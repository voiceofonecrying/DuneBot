package controller.commands;

import constants.Emojis;
import controller.Alliance;
import controller.DiscordGame;
import controller.Queue;
import controller.channels.TurnSummary;
import enums.GameOption;
import enums.UpdateType;
import exceptions.ChannelNotFoundException;
import exceptions.InvalidGameStateException;
import exceptions.InvalidOptionException;
import model.*;
import model.factions.*;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.channel.concrete.Category;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel;
import net.dv8tion.jda.api.events.guild.GuildReadyEvent;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import org.jetbrains.annotations.NotNull;
import templates.ChannelPermissions;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static controller.commands.CommandOptions.*;
import static controller.commands.ShowCommands.refreshChangedInfo;
import static controller.commands.ShowCommands.showFactionInfo;

public class CommandManager extends ListenerAdapter {

    public static void awardTopBidder(DiscordGame discordGame, Game game) throws ChannelNotFoundException, InvalidGameStateException {
        Bidding bidding = game.getBidding();
        String winnerName = bidding.getBidLeader();
        if (winnerName.isEmpty()) {
            if (bidding.isRicheseCacheCard() || bidding.isBlackMarketCard())
                assignAndPayForCard(discordGame, game, "Richese", "", 0);
            else
                throw new InvalidGameStateException("There is no top bidder for this card.");
        } else {
            String paidToFactionName = "Bank";
            if ((bidding.isRicheseCacheCard() || bidding.isBlackMarketCard()) && !winnerName.equals("Richese"))
                paidToFactionName = "Richese";
            else if (!winnerName.equals("Emperor"))
                paidToFactionName = "Emperor";
            int spentValue = bidding.getCurrentBid();
            assignAndPayForCard(discordGame, game, winnerName, paidToFactionName, spentValue);
        }
    }

    public static void assignAndPayForCard(DiscordGame discordGame, Game game, String winnerName, String paidToFactionName, int spentValue) throws ChannelNotFoundException, InvalidGameStateException {
        Bidding bidding = game.getBidding();
        if (bidding.getBidCard() == null) {
            throw new InvalidGameStateException("There is no card up for bid.");
        }
        Faction winner = game.getFaction(winnerName);
        List<TreacheryCard> winnerHand = winner.getTreacheryHand();
        if ((!winner.hasAlly() && winner.getSpice() < spentValue) || (winner.hasAlly() && winner.getSpice() + winner.getAllySpiceBidding() < spentValue)) {
            throw new InvalidGameStateException(winner.getEmoji() + " does not have enough spice to buy the card.");
        } else if (winnerHand.size() >= winner.getHandLimit()) {
            throw new InvalidGameStateException(winner.getEmoji() + " already has a full hand.");
        }

        String currentCard = MessageFormat.format(
                "R{0}:C{1}",
                game.getTurn(),
                bidding.getBidCardNumber()
        );
        int allySupport = Math.min(winner.getAllySpiceBidding(), spentValue);

        String allyString = winner.hasAlly() && winner.getAllySpiceBidding() > 0 ? "(" + allySupport + " from " + game.getFaction(winner.getAlly()).getEmoji() + ")" : "";

        TurnSummary turnSummary = discordGame.getTurnSummary();
        turnSummary.queueMessage(
                MessageFormat.format(
                        "{0} wins {1} for {2} {3} {4}",
                        winner.getEmoji(),
                        currentCard,
                        spentValue,
                        Emojis.SPICE,
                        allyString
                )
        );

        // Winner pays for the card
        winner.subtractSpice(spentValue - allySupport);
        winner.setAllySpiceBidding(Math.max(winner.getAllySpiceBidding() - spentValue, 0));
        winner.spiceMessage(spentValue - allySupport, currentCard, false);
        if (winner.hasAlly()) {
            game.getFaction(winner.getAlly()).subtractSpice(allySupport);
            game.getFaction(winner.getAlly()).spiceMessage(allySupport, currentCard + " (ally support)", false);
        }

        if (game.hasFaction(paidToFactionName)) {
            int spicePaid = spentValue;
            Faction paidToFaction = game.getFaction(paidToFactionName);

            if (paidToFaction instanceof EmperorFaction && game.hasGameOption(GameOption.HOMEWORLDS)
                    && !paidToFaction.isHighThreshold()) {
                spicePaid = Math.ceilDiv(spentValue, 2);
                if (paidToFaction.isHomeworldOccupied()) {
                    Faction occupier = paidToFaction.getOccupier();
                    occupier.addSpice(Math.floorDiv(spentValue, 2));
                    occupier.spiceMessage(Math.floorDiv(spentValue, 2), "Tribute from " + Emojis.EMPEROR + " for " + currentCard, true);
                    turnSummary.queueMessage(
                            MessageFormat.format(
                                    "{0} is paid {1} {2} for {3} (homeworld occupied)",
                                    paidToFaction.getOccupier().getEmoji(),
                                    Math.floorDiv(spentValue, 2),
                                    Emojis.SPICE,
                                    currentCard
                            )
                    );
                }
            }

            if (paidToFaction instanceof RicheseFaction && paidToFaction.isHomeworldOccupied()) {
                spicePaid = Math.ceilDiv(spentValue, 2);
                Faction occupier = paidToFaction.getOccupier();
                occupier.addSpice(Math.floorDiv(spentValue, 2));
                occupier.spiceMessage(Math.floorDiv(spentValue, 2), "Tribute from " + Emojis.EMPEROR + " for " + currentCard, true);
                turnSummary.queueMessage(
                        MessageFormat.format(
                                "{0} is paid {1} {2} for {3} (homeworld occupied)",
                                paidToFaction.getOccupier().getEmoji(),
                                Math.floorDiv(spentValue, 2),
                                Emojis.SPICE,
                                currentCard
                        )
                );
            }

            paidToFaction.addSpice(spicePaid);
            paidToFaction.spiceMessage(spicePaid, currentCard, true);

            turnSummary.queueMessage(
                    MessageFormat.format(
                            "{0} is paid {1} {2} for {3}",
                            paidToFaction.getEmoji(),
                            spicePaid,
                            Emojis.SPICE,
                            currentCard
                    )
            );
        }

        winner.addTreacheryCard(bidding.getBidCard());
        discordGame.getFactionLedger(winnerName).queueMessage(
                "Received " + bidding.getBidCard().name() +
                        " from bidding. (R" + game.getTurn() + ":C" + bidding.getBidCardNumber() + ")");
        bidding.clearBidCardInfo(winnerName);

        // Harkonnen draw an additional card
        if (winner instanceof HarkonnenFaction && winnerHand.size() < winner.getHandLimit() && !winner.isHomeworldOccupied()) {
            if (game.drawTreacheryCard("Harkonnen")) {
                turnSummary.queueMessage(MessageFormat.format(
                        "The {0} deck was empty and has been replenished from the discard pile.",
                        Emojis.TREACHERY
                ));
            }

            turnSummary.queueMessage(MessageFormat.format(
                    "{0} draws another card from the {1} deck.",
                    winner.getEmoji(), Emojis.TREACHERY
            ));

            TreacheryCard addedCard = winner.getLastTreacheryCard();
            discordGame.getHarkonnenLedger().queueMessage(
                    "Received " + addedCard.name() + " as an extra card. (" + currentCard + ")"
            );

        } else if (winner instanceof HarkonnenFaction && winner.isHomeworldOccupied() && winner.getOccupier().hasAlly()) {
            discordGame.getModInfo().queueMessage("Harkonnen occupier or ally may draw one from the deck (you must do this for them).");
            discordGame.getTurnSummary().queueMessage("Giedi Prime is occupied by " + winner.getOccupier().getName() + ", they or their ally may draw an additional card from the deck.");
        } else if (winner instanceof HarkonnenFaction && winner.isHomeworldOccupied() && winner.getOccupier().getTreacheryHand().size() < winner.getOccupier().getHandLimit()) {
            game.drawCard("treachery deck", winner.getOccupier().getName());
            turnSummary.queueMessage(MessageFormat.format(
                    "Giedi Prime is occupied, {0} draws another card from the {1} deck instead of {2}.",
                    winner.getEmoji(), Emojis.TREACHERY, Emojis.HARKONNEN
            ));
        }

        if (bidding.getMarket().isEmpty() && bidding.getBidCardNumber() == bidding.getNumCardsForBid() - 1 && bidding.isRicheseCacheCardOutstanding()) {
            RicheseCommands.cacheCard(discordGame, game);
            discordGame.getModInfo().queueMessage(Emojis.RICHESE + " has been asked to select the last card of the turn.");
        }
        discordGame.pushGame();
    }

    public static void reviveForces(Faction faction, boolean isPaid, int revivedValue, int starredAmountValue, Game game, DiscordGame discordGame) throws ChannelNotFoundException {
        game.reviveForces(faction, isPaid, revivedValue, starredAmountValue);
        RunCommands.flipToHighThresholdIfApplicable(discordGame, game);
        discordGame.pushGame();
    }

    public static void revival(boolean starred, Faction faction, boolean isPaid, int revivedValue, Game game, DiscordGame discordGame) throws ChannelNotFoundException {
        int regularValue = starred ? 0 : revivedValue;
        int starredValue = starred ? revivedValue : 0;
        reviveForces(faction, isPaid, regularValue, starredValue, game, discordGame);
    }

    private static void bgFlipMessageAndButtons(DiscordGame discordGame, Game game, String territoryName) throws ChannelNotFoundException {
        List<Button> buttons = new LinkedList<>();
        buttons.add(Button.primary("bg-flip-" + territoryName, "Flip"));
        buttons.add(Button.secondary("bg-dont-flip-" + territoryName, "Don't Flip"));
        discordGame.getTurnSummary().queueMessage(Emojis.BG + " to decide whether they want to flip to " + Emojis.BG_ADVISOR + " in " + territoryName);
        discordGame.getBGChat().queueMessage("Will you flip to " + Emojis.BG_ADVISOR + " in " + territoryName + "? " + game.getFaction("BG").getPlayer(), buttons);
    }

    private static void ecazTriggerMessageAndButtons(DiscordGame discordGame, Game game, String ambassador, Faction targetFaction, String territoryName) throws ChannelNotFoundException {
        List<Button> buttons = new LinkedList<>();
        buttons.add(Button.primary("ecaz-trigger-ambassador-" + ambassador + "-" + targetFaction.getName(), "Trigger"));
        buttons.add(Button.danger("ecaz-don't-trigger-ambassador", "Don't Trigger"));
        discordGame.getTurnSummary().queueMessage(Emojis.ECAZ + " has an opportunity to trigger their " + ambassador + " ambassador.");
        discordGame.getEcazChat().queueMessage("Will you trigger your " + ambassador + " ambassador against " + targetFaction.getName() + " in " + territoryName + "? " + game.getFaction("Ecaz").getPlayer(), buttons);
    }

    public static void placeForces(Territory targetTerritory, Faction targetFaction, int amountValue, int starredAmountValue, boolean isShipment, boolean canTrigger, DiscordGame discordGame, Game game, boolean karama) throws ChannelNotFoundException {
        Force reserves = targetFaction.getReserves();
        Force specialReserves = targetFaction.getSpecialReserves();
        TurnSummary turnSummary = discordGame.getTurnSummary();

        if (amountValue > 0)
            placeForceInTerritory(discordGame, game, targetTerritory, targetFaction, amountValue, false);
        if (starredAmountValue > 0)
            placeForceInTerritory(discordGame, game, targetTerritory, targetFaction, starredAmountValue, true);

        if (isShipment) {
            targetFaction.getShipment().setShipped(true);
            int costPerForce = targetTerritory.isStronghold() || targetTerritory.getTerritoryName().matches("Cistern|Ecological Testing Station|Shrine|Orgiz Processing Station") ? 1 : 2;
            int baseCost = costPerForce * (amountValue + starredAmountValue);
            int cost;

            if (targetFaction instanceof GuildFaction || (targetFaction.hasAlly() && targetFaction.getAlly().equals("Guild")) || karama) {
                cost = Math.ceilDiv(baseCost, 2);
            } else if (targetFaction instanceof FremenFaction &&
                    !game.getHomeworlds().containsValue(targetTerritory.getTerritoryName())) {
                cost = 0;
            } else {
                cost = baseCost;
            }

            StringBuilder message = new StringBuilder();

            message.append(targetFaction.getEmoji())
                    .append(": ");

            if (amountValue > 0) {
                message.append(MessageFormat.format("{0} {1} ", amountValue, Emojis.getForceEmoji(reserves.getName())));
            }

            if (starredAmountValue > 0) {
                message.append(MessageFormat.format("{0} {1} ", starredAmountValue, Emojis.getForceEmoji(specialReserves.getName())));
            }

            message.append(
                    MessageFormat.format("placed on {0}",
                            targetTerritory.getTerritoryName()
                    )
            );

            if (cost > 0) {
                message.append(
                        MessageFormat.format(" for {0} {1}",
                                cost, Emojis.SPICE
                        )
                );
                int support = 0;
                if (targetFaction.getAllySpiceShipment() > 0) {
                    support = Math.min(targetFaction.getAllySpiceShipment(), cost);
                    Faction allyFaction = game.getFaction(targetFaction.getAlly());
                    allyFaction.subtractSpice(support);
                    allyFaction.spiceMessage(support, targetFaction.getEmoji() + " shipment support", false);
                    message.append(MessageFormat.format(" ({0} from {1})", support, game.getFaction(targetFaction.getAlly()).getEmoji()));
                    targetFaction.setAllySpiceShipment(0);
                }

                targetFaction.subtractSpice(cost - support);
                targetFaction.spiceMessage(cost - support, "shipment to " + targetTerritory.getTerritoryName(), false);

                if (game.hasFaction("Guild") && !(targetFaction instanceof GuildFaction) && !karama) {
                    Faction guildFaction = game.getFaction("Guild");
                    guildFaction.addSpice(cost);
                    message.append(" paid to ")
                            .append(Emojis.GUILD);
                    guildFaction.spiceMessage(cost, targetFaction.getEmoji() + " shipment", true);
                }

            }

            if (
                    !(targetFaction instanceof GuildFaction) &&
                            !(targetFaction instanceof FremenFaction) &&
                            game.hasGameOption(GameOption.TECH_TOKENS)
            ) {
                TechToken.addSpice(game, TechToken.HEIGHLINERS);
            }

            if (game.hasFaction("BG") && targetTerritory.hasActiveFaction(game.getFaction("BG")) && !(targetFaction instanceof BGFaction)) {
                bgFlipMessageAndButtons(discordGame, game, targetTerritory.getTerritoryName());
            }
            BGCommands.presentAdvisorButtons(discordGame, game, targetFaction, targetTerritory);
            turnSummary.queueMessage(message.toString());
        }

        if (canTrigger) {
            if (targetTerritory.getEcazAmbassador() != null && !(targetFaction instanceof EcazFaction)
                    && !targetFaction.getName().equals(targetTerritory.getEcazAmbassador())
                    && !(game.getFaction("Ecaz").hasAlly()
                    && game.getFaction("Ecaz").getAlly().equals(targetFaction.getName()))) {
                ecazTriggerMessageAndButtons(discordGame, game, targetTerritory.getEcazAmbassador(), targetFaction, targetTerritory.getTerritoryName());
            }

            if (!targetTerritory.getTerrorTokens().isEmpty() && !(targetFaction instanceof MoritaniFaction)
                    && (!(game.getFaction("Moritani").hasAlly()
                    && game.getFaction("Moritani").getAlly().equals(targetFaction.getName())))) {
                if (!game.getFaction("Moritani").isHighThreshold() && amountValue + starredAmountValue < 3) {
                    turnSummary.queueMessage(Emojis.MORITANI + " are at low threshold and may not trigger their Terror Token at this time");
                } else {
                    ((MoritaniFaction)game.getFaction("Moritani")).sendTerrorTokenTriggerMessage(game, discordGame, targetTerritory, targetFaction);
                }
            }
        }
    }

    /**
     * Places a force from the reserves into a territory.
     * Reports removal from reserves to ledger.
     * Switches homeworld to low threshold if applicable.
     *
     * @param discordGame The DiscordGame instance.
     * @param game      The Game instance.
     * @param territory The territory to place the force in.
     * @param faction   The faction that owns the force.
     * @param amount    The number of forces to place.
     * @param special   Whether the force is a special reserve.
     */
    public static void placeForceInTerritory(DiscordGame discordGame, Game game, Territory territory, Faction faction, int amount, boolean special) throws ChannelNotFoundException {
        String forceName;

        if (special) {
            // Are Sardaukar being reported to ledger?
            faction.removeSpecialReserves(amount);
            forceName = faction.getSpecialReserves().getName();
        } else {
            faction.removeReserves(amount);
            forceName = faction.getReserves().getName();
            if (faction instanceof BGFaction && territory.hasForce("Advisor")) {
                // Also need to report Advisors to ledger
                int advisors = territory.getForce("Advisor").getStrength();
                territory.getForces().add(new Force("BG", advisors));
                territory.removeForce("Advisor");
            }
        }
        discordGame.getFactionLedger(faction).queueMessage(
                MessageFormat.format("{0} {1} removed from reserves.", amount, Emojis.getForceEmoji(faction.getName() + (special ? "*" : ""))));
        Force territoryForce = territory.getForce(forceName);
        territory.setForceStrength(forceName, territoryForce.getStrength() + amount);

        if (game.hasGameOption(GameOption.HOMEWORLDS)) {
            Force reserves = faction.getReserves();
            Force specialReserves = faction.getSpecialReserves();
            if (faction instanceof EmperorFaction emperorFaction) {
                if (reserves.getStrength() < faction.getHighThreshold() && faction.isHighThreshold()) {
                    discordGame.getTurnSummary().queueMessage(faction.getEmoji() + " is now at Low Threshold.");
                    faction.setHighThreshold(false);
                } else if (specialReserves.getStrength() < emperorFaction.getSecundusHighThreshold() && emperorFaction.isSecundusHighThreshold()) {
                    discordGame.getTurnSummary().queueMessage(faction.getEmoji() + " Salusa Secundus is now at Low Threshold.");
                    emperorFaction.setSecundusHighThreshold(false);
                }
            } else if (reserves.getStrength() + specialReserves.getStrength() < faction.getHighThreshold() && faction.isHighThreshold()) {
                discordGame.getTurnSummary().queueMessage(faction.getEmoji() + " is now at Low Threshold.");
                faction.setHighThreshold(false);
            }
        }
        game.setUpdated(UpdateType.MAP);
    }

    public static void moveForces(Faction targetFaction, Territory from, Territory to, int amountValue, int starredAmountValue, DiscordGame discordGame, Game game) throws ChannelNotFoundException, InvalidOptionException {

        int fromForceStrength = from.getForce(targetFaction.getName()).getStrength();
        if (targetFaction instanceof BGFaction && from.getForce("Advisor").getStrength() > 0)
            fromForceStrength = from.getForce("Advisor").getStrength();
        int fromStarredForceStrength = from.getForce(targetFaction.getName() + "*").getStrength();

        if (fromForceStrength < amountValue || fromStarredForceStrength < starredAmountValue) {
            throw new InvalidOptionException("Not enough forces in territory.");
        }
        if (targetFaction.hasAlly() && to.hasActiveFaction(game.getFaction(targetFaction.getAlly())) &&
                (!(targetFaction instanceof EcazFaction && to.getActiveFactions(game).stream().anyMatch(f -> f.getName().equals(targetFaction.getAlly())))
                        && !(targetFaction.getAlly().equals("Ecaz") && to.getActiveFactions(game).stream().anyMatch(f -> f instanceof EcazFaction)))) {
            throw new InvalidOptionException("You cannot move into a territory with your ally.");
        }

        StringBuilder message = new StringBuilder();

        message.append(targetFaction.getEmoji())
                .append(": ");

        if (amountValue > 0) {
            String forceName = targetFaction.getName();
            String targetForceName = targetFaction.getName();
            if (targetFaction instanceof BGFaction && from.hasForce("Advisor")) {
                forceName = "Advisor";
                if (to.hasForce("Advisor")) targetForceName = "Advisor";
            }
            from.setForceStrength(forceName, fromForceStrength - amountValue);
            to.setForceStrength(targetForceName, to.getForce(targetForceName).getStrength() + amountValue);

            message.append(
                    MessageFormat.format("{0} {1} ",
                            amountValue, Emojis.getForceEmoji(from.getForce(forceName).getName())
                    )
            );
        }

        if (starredAmountValue > 0) {
            from.setForceStrength(targetFaction.getName() + "*", fromStarredForceStrength - starredAmountValue);
            to.setForceStrength(targetFaction.getName() + "*",
                    to.getForce(targetFaction.getName() + "*").getStrength() + starredAmountValue);

            message.append(
                    MessageFormat.format("{0} {1} ",
                            starredAmountValue, Emojis.getForceEmoji(from.getForce(targetFaction.getName() + "*").getName())
                    )
            );

        }

        message.append(
                MessageFormat.format("moved from {0} to {1}",
                        from.getTerritoryName(), to.getTerritoryName()
                )
        );
        TurnSummary turnSummary = discordGame.getTurnSummary();
        turnSummary.queueMessage(message.toString());

        if (game.hasFaction("BG") && to.hasActiveFaction(game.getFaction("BG")) && !(targetFaction instanceof BGFaction)) {
            bgFlipMessageAndButtons(discordGame, game, to.getTerritoryName());
        }
        if (game.getTerritory(to.getTerritoryName()).getEcazAmbassador() != null && !(targetFaction instanceof EcazFaction)
                && !targetFaction.getName().equals(game.getTerritory(to.getTerritoryName()).getEcazAmbassador())
                && !(game.getFaction("Ecaz").hasAlly()
                && game.getFaction("Ecaz").getAlly().equals(targetFaction.getName()))) {
            ecazTriggerMessageAndButtons(discordGame, game, to.getEcazAmbassador(), targetFaction, to.getTerritoryName());
        }

        if (!to.getTerrorTokens().isEmpty() && !(targetFaction instanceof MoritaniFaction)
                && !(game.getFaction("Moritani").hasAlly()
                && game.getFaction("Moritani").getAlly().equals(targetFaction.getName()))) {
            if (!game.getFaction("Moritani").isHighThreshold() && amountValue + starredAmountValue < 3) {
                turnSummary.queueMessage(Emojis.MORITANI + " are at low threshold and may not trigger their Terror Token at this time");
            } else {
                ((MoritaniFaction)game.getFaction("Moritani")).sendTerrorTokenTriggerMessage(game, discordGame, to, targetFaction);
            }
        }
        game.setUpdated(UpdateType.MAP);
    }

    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        String name = event.getName();
        event.deferReply(true).queue();
        Member member = event.getMember();

        List<Role> roles = member == null ? new ArrayList<>() : member.getRoles();

        try {
            if (name.equals("new-game") && roles.stream().anyMatch(role -> role.getName().equals("Moderators"))) {
                newGame(event);
                event.getHook().editOriginal("Command Done.").queue();
            } else if (name.equals("waiting-list")) {
                waitingList(event);
                event.getHook().editOriginal("Command Done.").queue();
            } else if (name.equals("reports")) {
                String result = ReportsCommands.runCommand(event);
                event.getHook().editOriginal(result).queue();
            } else if (name.equals("random-dune-quote")) {
                randomDuneQuote(event);
            } else {
                String categoryName = Objects.requireNonNull(DiscordGame.categoryFromEvent(event)).getName();
                CompletableFuture<Void> future = Queue.getFuture(categoryName);
                Queue.putFuture(categoryName, future.thenRunAsync(() -> runGameCommand(event)));
            }
        } catch (Exception e) {
            event.getHook().editOriginal(e.getMessage()).queue();
            e.printStackTrace();
        }
    }

    private void runGameCommand(@NotNull SlashCommandInteractionEvent event) {
        String ephemeralMessage = "";

        try {
            Member member = event.getMember();
            String name = event.getName();
            List<Role> roles = member == null ? new ArrayList<>() : member.getRoles();
            DiscordGame discordGame = new DiscordGame(event);
            Game game = discordGame.getGame();

            if (roles.stream().noneMatch(role -> role.getName().equals(game.getModRole()) ||
                    role.getName().equals(game.getGameRole()) && name.startsWith("player"))) {
                event.getHook().editOriginal("You do not have permission to use this command.").queue();
                return;
            }

            if (game.isOnHold()) {
                if (name.equals("remove-hold")) {
                    game.setOnHold(false);
                    discordGame.getTurnSummary().queueMessage("The hold has been resolved. Gameplay may proceed.");
                    discordGame.pushGame();
                    discordGame.sendAllMessages();
                    event.getHook().editOriginal("Command Done.").queue();
                } else {
                    event.getHook().editOriginal("The game is on hold. Please wait for the mod to resolve the issue.").queue();
                }
                return;
            }

            switch (name) {
                case "gamestate" -> GameStateCommands.runCommand(event, discordGame, game);
                case "show" -> ShowCommands.runCommand(event, discordGame, game);
                case "setup" -> SetupCommands.runCommand(event, discordGame, game);
                case "run" -> RunCommands.runCommand(event, discordGame, game);
                case "battle" -> BattleCommands.runCommand(event, discordGame, game);
                case "richese" -> RicheseCommands.runCommand(event, discordGame, game);
                case "bt" -> BTCommands.runCommand(event, discordGame, game);
                case "ecaz" -> EcazCommands.runCommand(event, discordGame, game);
                case "hark" -> HarkCommands.runCommand(event, discordGame, game);
                case "choam" -> ChoamCommands.runCommand(event, discordGame, game);
                case "ix" -> IxCommands.runCommand(event, discordGame, game);
                case "moritani" -> MoritaniCommands.runCommand(event, discordGame, game);
                case "bg" -> BGCommands.runCommand(event, discordGame, game);
                case "atreides" -> AtreidesCommands.runCommand(event, discordGame, game);
                case "player" -> ephemeralMessage = PlayerCommands.runCommand(event, discordGame, game);
                case "draw-treachery-card" -> drawTreacheryCard(discordGame, game);
                case "shuffle-treachery-deck" -> shuffleTreacheryDeck(discordGame, game);
                case "discard" -> discard(discordGame, game);
                case "transfer-card" -> transferCard(discordGame, game);
                case "transfer-card-from-discard" -> transferCardFromDiscard(discordGame, game);
                case "place-forces" -> placeForcesEventHandler(discordGame, game);
                case "move-forces" -> moveForcesEventHandler(discordGame, game);
                case "remove-forces" -> removeForcesEventHandler(discordGame, game);
                case "display" -> displayGameState(discordGame, game);
                case "revive-forces" -> reviveForcesEventHandler(discordGame, game);
                case "award-bid" -> awardBid(event, discordGame, game);
                case "award-top-bidder" -> awardTopBidder(discordGame, game);
                case "kill-leader" -> killLeader(discordGame, game);
                case "revive-leader" -> reviveLeader(discordGame, game);
                case "set-storm" -> setStorm(discordGame, game);
                case "set-storm-movement" -> setStormMovement(discordGame, game);
                case "bribe" -> bribe(discordGame, game);
                case "mute" -> mute(discordGame, game);
                case "assign-tech-token" -> assignTechToken(discordGame, game);
                case "draw-spice-blow" -> drawSpiceBlow(discordGame, game);
                case "create-alliance" -> createAlliance(discordGame, game);
                case "remove-alliance" -> removeAlliance(discordGame, game);
                case "set-spice-in-territory" -> setSpiceInTerritory(discordGame, game);
                case "destroy-shield-wall" -> destroyShieldWall(discordGame, game);
                case "add-spice" -> addSpice(discordGame, game);
                case "remove-spice" -> removeSpice(discordGame, game);
                case "reassign-faction" -> reassignFaction(discordGame, game);
                case "reassign-mod" -> reassignMod(event, discordGame, game);
                case "draw-nexus-card" -> drawNexusCard(discordGame, game);
                case "discard-nexus-card" -> discardNexusCard(discordGame, game);
                case "moritani-assassinate-leader" -> assassinateLeader(discordGame, game);
                case "game-result" -> ReportsCommands.gameResult(event, discordGame, game);
            }

            if (!(name.equals("setup") && Objects.requireNonNull(event.getSubcommandName()).equals("faction")))
                refreshChangedInfo(discordGame);
            discordGame.sendAllMessages();

            if (ephemeralMessage.isEmpty()) ephemeralMessage = "Command Done.";
            event.getHook().editOriginal(ephemeralMessage).queue();
        } catch (InvalidGameStateException e) {
            event.getHook().editOriginal(e.getMessage()).queue();
        } catch (Exception e) {
            event.getHook().editOriginal(e.getMessage()).queue();
            e.printStackTrace();
        }
    }

    private List<String> getQuotesFromBook(String bookName) throws IOException {
        try (InputStream stream = getClass().getClassLoader().getResourceAsStream(bookName)) {
            byte[] allBytes = Objects.requireNonNull(stream).readAllBytes();
            return new ArrayList<>(Arrays.stream(new String(allBytes, StandardCharsets.UTF_8).split("((?<=[.?!]))")).toList());
        }
    }

    private void randomDuneQuote(SlashCommandInteractionEvent event) throws IOException {
        int lines = DiscordGame.required(CommandOptions.lines, event).getAsInt();
        List<String> quotes;
        if (DiscordGame.optional(search, event) == null) {
            if (DiscordGame.optional(book, event) == null) {
                quotes = getQuotesFromBook("Dune Books/Dune.txt");
                Random random = new Random();
                int start = DiscordGame.optional(startingLine, event) != null ? DiscordGame.required(startingLine, event).getAsInt() : random.nextInt(quotes.size() - lines + 1);
                StringBuilder quote = new StringBuilder();

                for (int i = 0; i < lines; i++) {
                    quote.append(quotes.get(start + i));
                }
                event.getMessageChannel().sendMessage(quote + "\n\n(Line: " + start + ")").queue();

            } else {
                quotes = getQuotesFromBook("Dune Books/" + DiscordGame.required(book, event).getAsString());
                Random random = new Random();
                int start = DiscordGame.optional(startingLine, event) != null ? DiscordGame.required(startingLine, event).getAsInt() : random.nextInt(quotes.size() - lines + 1);
                StringBuilder quote = new StringBuilder();

                for (int i = 0; i < lines; i++) {
                    quote.append(quotes.get(start + i));
                }
                event.getMessageChannel().sendMessage(quote + "\n\n(Line: " + start + ")").queue();
            }
        } else {
            String search = DiscordGame.required(CommandOptions.search, event).getAsString();
            quotes = new ArrayList<>();
            quotes.addAll(getQuotesFromBook("Dune Books/Dune.txt"));
            quotes.addAll(getQuotesFromBook("Dune Books/Messiah.txt"));
            quotes.addAll(getQuotesFromBook("Dune Books/Children.txt"));
            quotes.addAll(getQuotesFromBook("Dune Books/GeoD.txt"));
            quotes.addAll(getQuotesFromBook("Dune Books/Heretics.txt"));
            quotes.addAll(getQuotesFromBook("Dune Books/Chapterhouse.txt"));
            List<String> matched = new LinkedList<>();

            for (int i = 0; i < quotes.size() - lines; i+=lines) {
                StringBuilder candidate = new StringBuilder();
                for (int j = 0; j < lines; j++) {
                    candidate.append(quotes.get(i + j));
                }
                if (candidate.toString().contains(search)) matched.add(candidate.toString());
            }
            if (matched.isEmpty()) {
                event.getHook().sendMessage("No results.").queue();
                return;
            }
            Random random = new Random();
            int start = DiscordGame.optional(startingLine, event) != null ? DiscordGame.required(startingLine, event).getAsInt() - 1 : random.nextInt(matched.size() - 1);

            event.getMessageChannel().sendMessage(matched.get(start) + "\n (Match " + (start + 1) + " of " + matched.size() + " for search term: '" + search + "')").queue();
        }
    }

    @Override
    public void onCommandAutoCompleteInteraction(@NotNull CommandAutoCompleteInteractionEvent event) {
        CompletableFuture.runAsync(() -> runCommandAutoCompleteInteraction(event));
    }

    private void runCommandAutoCompleteInteraction(@NotNull CommandAutoCompleteInteractionEvent event) {
        DiscordGame discordGame = new DiscordGame(event);

        try {
            Game game = discordGame.getGame();
            event.replyChoices(CommandOptions.getCommandChoices(event, discordGame, game)).queue();
        } catch (ChannelNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public static List<CommandData> getAllCommands() {
        //add new slash command definitions to commandData list
        List<CommandData> commandData = new ArrayList<>();
        commandData.add(Commands.slash("new-game", "Creates a new Dune game instance.").addOptions(gameName, gameRole, modRole));
        commandData.add(Commands.slash("draw-treachery-card", "Draw a card from the top of a deck.").addOptions(faction));
        commandData.add(Commands.slash("shuffle-treachery-deck", "Shuffle the treachery deck."));
        commandData.add(Commands.slash("discard", "Move a card from a faction's hand to the discard pile").addOptions(faction, card));
        commandData.add(Commands.slash("transfer-card", "Move a card from one faction's hand to another").addOptions(faction, card, recipient));
        commandData.add(Commands.slash("transfer-card-from-discard", "Move a card from the discard to a faction's hand").addOptions(faction, discardCard));
        commandData.add(Commands.slash("place-forces", "Place forces from reserves onto the surface").addOptions(faction, amount, starredAmount, isShipment, canTrigger, territory));
        commandData.add(Commands.slash("move-forces", "Move forces from one territory to another").addOptions(faction, fromTerritory, toTerritory, amount, starredAmount));
        commandData.add(Commands.slash("remove-forces", "Remove forces from the board.").addOptions(faction, amount, starredAmount, toTanks, fromTerritory));
        commandData.add(Commands.slash("award-bid", "Designate that a card has been won by a faction during bidding phase.").addOptions(faction, spent, paidToFaction));
        commandData.add(Commands.slash("award-top-bidder", "Designate that a card has been won by the top bidder during bidding phase and pay spice recipient."));
        commandData.add(Commands.slash("revive-forces", "Revive forces for a faction.").addOptions(faction, revived, starredAmount, paid));
        commandData.add(Commands.slash("display", "Displays some element of the game to the mod.").addOptions(data));
        commandData.add(Commands.slash("set-storm", "Sets the storm to an initial sector.").addOptions(dialOne, dialTwo));
        commandData.add(Commands.slash("set-storm-movement", "Override the storm movement").addOptions(sectors));
        commandData.add(Commands.slash("kill-leader", "Send a leader to the tanks.").addOptions(faction, leader));
        commandData.add(Commands.slash("revive-leader", "Revive a leader from the tanks.").addOptions(faction, reviveLeader));
        commandData.add(Commands.slash("mute", "Toggle mute for all bot messages."));
        commandData.add(Commands.slash("remove-hold", "Remove the hold and allow gameplay to proceed."));
        commandData.add(Commands.slash("bribe", "Record a bribe transaction").addOptions(faction, recipient, amount, reason));
        commandData.add(Commands.slash("assign-tech-token", "Assign a Tech Token to a Faction (taking it away from previous owner)").addOptions(faction, token));
        commandData.add(Commands.slash("draw-spice-blow", "Draw the spice blow").addOptions(spiceBlowDeck));
        commandData.add(Commands.slash("create-alliance", "Create an alliance between two factions")
                .addOptions(faction, otherFaction));
        commandData.add(Commands.slash("remove-alliance", "Remove alliance (only on faction of the alliance needs to be selected)")
                .addOptions(faction));
        commandData.add(Commands.slash("set-spice-in-territory", "Set the spice amount for a territory")
                .addOptions(territory, amount));
        commandData.add(Commands.slash("destroy-shield-wall", "Destroy the shield wall"));

        commandData.add(Commands.slash("add-spice", "Add spice to a faction").addOptions(faction, amount, message, frontOfShield));
        commandData.add(Commands.slash("remove-spice", "Remove spice from a faction").addOptions(faction, amount, message, frontOfShield));
        commandData.add(Commands.slash("reassign-faction", "Assign the faction to a different player").addOptions(faction, user));
        commandData.add(Commands.slash("reassign-mod", "Assign yourself as the mod to be tagged"));
        commandData.add(Commands.slash("draw-nexus-card", "Draw a nexus card.").addOptions(faction));
        commandData.add(Commands.slash("discard-nexus-card", "Discard a nexus card.").addOptions(faction));
        commandData.add(Commands.slash("moritani-assassinate-leader", "Assassinate leader ability"));
        commandData.add(Commands.slash("random-dune-quote", "Will dispense a random line of text from the specified book.").addOptions(lines, book, startingLine, search));
        commandData.add(Commands.slash("game-result", "Generate the game-results message for this game.").addOptions(faction, otherWinnerFaction, guildSpecialWin, fremenSpecialWin, bgPredictionWin, ecazOccupyWin));

        commandData.addAll(GameStateCommands.getCommands());
        commandData.addAll(ShowCommands.getCommands());
        commandData.addAll(SetupCommands.getCommands());
        commandData.addAll(RunCommands.getCommands());
        commandData.addAll(BattleCommands.getCommands());
        commandData.addAll(RicheseCommands.getCommands());
        commandData.addAll(BTCommands.getCommands());
        commandData.addAll(HarkCommands.getCommands());
        commandData.addAll(ChoamCommands.getCommands());
        commandData.addAll(IxCommands.getCommands());
        commandData.addAll(BGCommands.getCommands());
        commandData.addAll(AtreidesCommands.getCommands());
        commandData.addAll(EcazCommands.getCommands());
        commandData.addAll(MoritaniCommands.getCommands());
        commandData.addAll(ReportsCommands.getCommands());

        List<CommandData> commandDataWithPermissions = commandData.stream()
                .map(command -> command.setDefaultPermissions(
                        DefaultMemberPermissions.enabledFor(Permission.MANAGE_CHANNEL)
                ))
                .collect(Collectors.toList());

        commandDataWithPermissions.addAll(PlayerCommands.getCommands());
        commandDataWithPermissions.add(Commands.slash("waiting-list", "Add an entry to the waiting list")
                .addOptions(slowGame, midGame, fastGame, originalSixFactions, ixianstleilaxuExpansion, choamricheseExpansion, ecazmoritaniExpansion, leaderSkills, strongholdCards, homeworlds));

        return commandDataWithPermissions;
    }

    @Override
    public void onGuildReady(@NotNull GuildReadyEvent event) {
        event.getGuild().updateCommands().addCommands(getAllCommands()).complete();
    }

    public void newGame(SlashCommandInteractionEvent event) throws ChannelNotFoundException, IOException {
        Role gameRoleValue = Objects.requireNonNull(event.getOption(gameRole.getName())).getAsRole();
        Role modRoleValue = Objects.requireNonNull(event.getOption(modRole.getName())).getAsRole();
        Role observerRole = Objects.requireNonNull(event.getGuild()).getRolesByName("Observer", true).get(0);
        Role pollBot = event.getGuild().getRolesByName("EasyPoll", true).get(0);
        String name = Objects.requireNonNull(event.getOption(gameName.getName())).getAsString();

        // Create category and set base permissions to deny everything for everyone except the mod role.
        // The channel permissions assume that this is set this way.
        event.getGuild()
                .createCategory(name)
                .addPermissionOverride(modRoleValue, ChannelPermissions.all, null)
                .addPermissionOverride(event.getGuild().getPublicRole(), null, ChannelPermissions.all)
                .addPermissionOverride(gameRoleValue, null, ChannelPermissions.all)
                .addPermissionOverride(observerRole, null, ChannelPermissions.all)
                .complete();

        Category category = event.getGuild().getCategoriesByName(name, true).get(0);

        category.createTextChannel("chat")
                .addPermissionOverride(
                        observerRole,
                        ChannelPermissions.readWriteMinimumAllow,
                        ChannelPermissions.readWriteMinimumDeny
                )
                .addPermissionOverride(
                        gameRoleValue,
                        ChannelPermissions.readWriteAllow,
                        ChannelPermissions.readWriteDeny
                )
                .complete();

        // Not including Observer in pre-game-voting because there's no way to stop someone from adding to an
        // existing emoji reaction.
        category.createTextChannel("pre-game-voting")
                .addPermissionOverride(
                        gameRoleValue,
                        ChannelPermissions.readAndReactAllow,
                        ChannelPermissions.readAndReactDeny
                )
                .addPermissionOverride(
                        pollBot,
                        ChannelPermissions.pollBotAllow,
                        ChannelPermissions.pollBotDeny
                )
                .complete();

        TextChannel fosChannel = category.createTextChannel("front-of-shield")
                .addPermissionOverride(
                        observerRole,
                        ChannelPermissions.readAndReactAllow,
                        ChannelPermissions.readAndReactDeny
                )
                .addPermissionOverride(
                        gameRoleValue,
                        ChannelPermissions.readAndReactAllow,
                        ChannelPermissions.readAndReactDeny
                )
                .complete();
        fosChannel.createThreadChannel("turn-0-summary", true)
                .setInvitable(false)
                .setAutoArchiveDuration(ThreadChannel.AutoArchiveDuration.TIME_3_DAYS)
                .complete();

        String[] readWriteChannels = {"game-actions", "bribes", "bidding-phase", "rules"};
        for (String channel : readWriteChannels) {
            category.createTextChannel(channel)
                    .addPermissionOverride(
                            observerRole,
                            ChannelPermissions.readAndReactAllow,
                            ChannelPermissions.readAndReactDeny
                    )
                    .addPermissionOverride(
                            gameRoleValue,
                            ChannelPermissions.readWriteAllow,
                            ChannelPermissions.readWriteDeny
                    )
                    .complete();
        }

        String[] modChannels = {"bot-data", "mod-info"};
        for (String channel : modChannels) {
            category.createTextChannel(channel).complete();
        }

        DiscordGame discordGame = new DiscordGame(category);
        discordGame.queueMessage("rules", MessageFormat.format(
                """
                        {0}  Dune rulebook: https://www.gf9games.com/dunegame/wp-content/uploads/Dune-Rulebook.pdf
                        {1}  Dune FAQ Nov 20: https://www.gf9games.com/dune/wp-content/uploads/2020/11/Dune-FAQ-Nov-2020.pdf
                        {2} {3}  Ixians & Tleilaxu Rules: https://www.gf9games.com/dunegame/wp-content/uploads/2020/09/IxianAndTleilaxuRulebook.pdf
                        {4} {5} CHOAM & Richese Rules: https://www.gf9games.com/dune/wp-content/uploads/2021/11/CHOAM-Rulebook-low-res.pdf
                        {6} {7} Ecaz & Moritani Rules: https://www.gf9games.com/dune/wp-content/uploads/EcazMoritani-Rulebook-LOWRES.pdf""",
                Emojis.DUNE_RULEBOOK,
                Emojis.WEIRDING,
                Emojis.IX, Emojis.BT,
                Emojis.CHOAM, Emojis.RICHESE,
                Emojis.ECAZ, Emojis.MORITANI
        ));

        Game game = new Game();
        game.setGameRole(gameRoleValue.getName());
        game.setGameRoleMention(gameRoleValue.getAsMention());
        game.setModRole(modRoleValue.getName());
        game.setMod(event.getUser().getAsMention());
        game.setMute(false);
        discordGame.setGame(game);
        discordGame.pushGame();
        discordGame.sendAllMessages();
    }

    public static List<String> findPlayerTags(String message) {
        List<String> players = new ArrayList<>();
        int startChar = message.indexOf("<@");
        while (startChar != -1) {
            int endChar = message.substring(startChar).indexOf(">");
            if (endChar != -1) {
                players.add(message.substring(startChar, startChar + endChar + 1));
            }
            message = message.substring(startChar + endChar + 1);
            startChar = message.indexOf("<@");
        }
        return players;
    }

    /**
     * Add Spice to a player.  Spice can be added behind the shield or in front, with an optional message.
     *
     * @param discordGame The discord game object.
     * @param game        The game object.
     * @throws ChannelNotFoundException If the channel is not found.
     */
    public void addSpice(DiscordGame discordGame, Game game) throws ChannelNotFoundException {
        addOrRemoveSpice(discordGame, game, true);
    }

    /**
     * Remove Spice from a player.  Spice can be removed from behind the shield or in front, with an optional message.
     *
     * @param discordGame The discord game object.
     * @param game        The game object.
     * @throws ChannelNotFoundException If the channel is not found.
     */
    public void removeSpice(DiscordGame discordGame, Game game) throws ChannelNotFoundException {
        addOrRemoveSpice(discordGame, game, false);
    }

    /**
     * Add or Remove Spice from a player.  This can be behind the shield or in front, with an optional message.
     *
     * @param discordGame The discord game object.
     * @param game        The game object.
     * @param add         True to add spice, false to remove spice.
     * @throws ChannelNotFoundException If the channel is not found.
     */
    public void addOrRemoveSpice(DiscordGame discordGame, Game game, boolean add) throws ChannelNotFoundException {
        String factionName = discordGame.required(faction).getAsString();
        int amountValue = discordGame.required(amount).getAsInt();
        String messageValue = discordGame.required(message).getAsString();
        boolean toFrontOfShield = discordGame.optional(frontOfShield) != null && discordGame.required(frontOfShield).getAsBoolean();

        Faction faction = game.getFaction(factionName);
        if (toFrontOfShield) {
            if (add) {
                faction.addFrontOfShieldSpice(amountValue);
            } else {
                faction.subtractFrontOfShieldSpice(amountValue);
            }
        } else {
            if (add) {
                faction.addSpice(amountValue);
            } else {
                faction.subtractSpice(amountValue);
            }
        }
        String frontOfShieldMessage = add ? "to front of shield" : "from front of shield";

        discordGame.getTurnSummary().queueMessage(
                MessageFormat.format(
                        "{0} {1} {2} {3} {4} {5}",
                        faction.getEmoji(),
                        add ? "gains" : "loses",
                        amountValue, Emojis.SPICE,
                        toFrontOfShield ? frontOfShieldMessage : "",
                        messageValue
                )
        );

        if (!toFrontOfShield)
            faction.spiceMessage(amountValue, messageValue, add);

        discordGame.pushGame();
    }

    public void drawSpiceBlow(DiscordGame discordGame, Game game) throws ChannelNotFoundException {
        String spiceBlowDeckName = discordGame.required(spiceBlowDeck).getAsString();
        game.drawSpiceBlow(spiceBlowDeckName);
        discordGame.pushGame();
    }

    private void drawNexusCard(DiscordGame discordGame, Game game) throws ChannelNotFoundException, IOException {
        Faction faction = game.getFaction(discordGame.required(CommandOptions.faction).getAsString());
        boolean discarded = false;
        if (faction.getNexusCard() != null) {
            discarded = true;
            game.getNexusDiscard().add(faction.getNexusCard());
        }
        faction.setNexusCard(game.getNexusDeck().pollFirst());
        if (discarded)
            game.getTurnSummary().publish(faction.getEmoji() + " has replaced their Nexus Card.");
        else
            game.getTurnSummary().publish(faction.getEmoji() + " has drawn a Nexus Card.");
        showFactionInfo(discordGame);
        discordGame.pushGame();
    }

    public static void discardNexusCard(DiscordGame discordGame, Game game, Faction faction) throws ChannelNotFoundException, IOException {
        game.getNexusDiscard().add(faction.getNexusCard());
        faction.setNexusCard(null);
        game.getTurnSummary().publish(faction.getEmoji() + " has discarded a Nexus Card.");
        showFactionInfo(discordGame);
        discordGame.pushGame();
    }

    private void discardNexusCard(DiscordGame discordGame, Game game) throws ChannelNotFoundException, IOException {
        Faction faction = game.getFaction(discordGame.required(CommandOptions.faction).getAsString());
        discardNexusCard(discordGame, game, faction);
    }

    public void drawTreacheryCard(DiscordGame discordGame, Game game) throws ChannelNotFoundException {
        String factionName = discordGame.required(faction).getAsString();
        game.drawTreacheryCard(factionName);
        List<TreacheryCard> treacheryHand = game.getFaction(factionName).getTreacheryHand();
        TreacheryCard cardDrawn = treacheryHand.get(treacheryHand.size() - 1);
        discordGame.getFactionLedger(factionName).queueMessage(cardDrawn.name() + " drawn from deck.");
        discordGame.pushGame();
    }

    public void shuffleTreacheryDeck(DiscordGame discordGame, Game game) throws ChannelNotFoundException {
        game.shuffleTreacheryDeck();
        discordGame.pushGame();
    }

    public void discard(DiscordGame discordGame, Game game) throws ChannelNotFoundException {
        String factionName = discordGame.required(faction).getAsString();
        Faction faction = game.getFaction(factionName);

        String cardName = discordGame.required(card).getAsString();
        TreacheryCard treacheryCard = faction.removeTreacheryCard(cardName);

        game.getTreacheryDiscard().add(treacheryCard);
        discordGame.getTurnSummary().queueMessage(faction.getEmoji() + " discards " + cardName);
        discordGame.getFactionLedger(factionName).queueMessage(cardName + " discarded from hand.");

        if (game.hasGameOption(GameOption.HOMEWORLDS) && game.hasFaction("Ecaz") && game.getFaction("Ecaz").isHighThreshold() && (treacheryCard.type().contains("Weapon - Poison") || treacheryCard.name().equals("Poison Blade"))) {
            game.getFaction("Ecaz").addSpice(3);
            game.getFaction("Ecaz").spiceMessage(3, "Poison weapon was discarded", true);
            discordGame.getTurnSummary().queueMessage(Emojis.ECAZ + " gain 3 " + Emojis.SPICE + " for the discarded poison weapon");
        }
        discordGame.pushGame();
    }

    public void transferCard(DiscordGame discordGame, Game game) throws ChannelNotFoundException {
        game.transferCard(
                discordGame.required(faction).getAsString(),
                discordGame.required(recipient).getAsString(),
                discordGame.required(card).getAsString()
        );
        discordGame.pushGame();
    }

    public void transferCardFromDiscard(DiscordGame discordGame, Game game) throws ChannelNotFoundException, InvalidGameStateException {
        Faction receiver = game.getFaction(discordGame.required(faction).getAsString());
        String cardName = discordGame.required(discardCard).getAsString();

        TreacheryCard card = game.getTreacheryDiscard().stream()
                .filter(c -> c.name().equalsIgnoreCase(cardName))
                .findFirst()
                .orElseThrow(() -> new InvalidGameStateException("Card not found in discard pile."));

        receiver.addTreacheryCard(card);
        game.getTreacheryDiscard().remove(card);
        discordGame.getFactionLedger(receiver).queueMessage("Received " + cardName + " from discard.");

        discordGame.pushGame();
    }

    public void awardBid(SlashCommandInteractionEvent event, DiscordGame discordGame, Game game) throws ChannelNotFoundException, InvalidGameStateException {
        String winnerName = discordGame.required(faction).getAsString();
        String paidToFactionName = event.getOption("paid-to-faction", "Bank", OptionMapping::getAsString);
        int spentValue = discordGame.required(spent).getAsInt();
        assignAndPayForCard(discordGame, game, winnerName, paidToFactionName, spentValue);
    }

    public void killLeader(DiscordGame discordGame, Game game) throws ChannelNotFoundException {
        Faction targetFaction = game.getFaction(discordGame.required(faction).getAsString());
        String leaderName = discordGame.required(leader).getAsString();
        game.getLeaderTanks().add(targetFaction.removeLeader(leaderName));
        String message = leaderName + " was sent to the tanks.";
        discordGame.getFactionLedger(targetFaction).queueMessage(message);
        discordGame.getTurnSummary().queueMessage(targetFaction.getEmoji() + " " + message);
        discordGame.pushGame();
    }

    public void reviveLeader(DiscordGame discordGame, Game game) throws ChannelNotFoundException {
        Faction targetFaction = game.getFaction(discordGame.required(faction).getAsString());
        String leaderToRevive = discordGame.required(reviveLeader).getAsString();

        Leader leader = game.removeLeaderFromTanks(leaderToRevive);
        targetFaction.addLeader(leader);
        leader.setBattleTerritoryName(null);
        String message = leaderToRevive + " was revived from the tanks.";
        discordGame.getFactionLedger(targetFaction).queueMessage(message);
        discordGame.getTurnSummary().queueMessage(targetFaction.getEmoji() + " " + message);
        discordGame.pushGame();
    }

    /**
     * Revive forces from the tanks
     *
     * @param discordGame the discord game
     * @param game        the game
     */
    public void reviveForcesEventHandler(DiscordGame discordGame, Game game) throws ChannelNotFoundException {
        Faction targetFaction = game.getFaction(discordGame.required(faction).getAsString());
        boolean isPaid = discordGame.required(paid).getAsBoolean();
        int revivedValue = discordGame.required(revived).getAsInt();
        int starredAmountValue = discordGame.required(starredAmount).getAsInt();
        reviveForces(targetFaction, isPaid, revivedValue, starredAmountValue, game, discordGame);
    }

    /**
     * Place forces in a territory
     *
     * @param discordGame the discord game
     * @param game        the game
     */
    public void placeForcesEventHandler(DiscordGame discordGame, Game game) throws ChannelNotFoundException {
        Territory targetTerritory = game.getTerritories().get(discordGame.required(territory).getAsString());
        Faction targetFaction = game.getFaction(discordGame.required(faction).getAsString());
        int amountValue = discordGame.required(amount).getAsInt();
        int starredAmountValue = discordGame.required(starredAmount).getAsInt();
        boolean isShipment = discordGame.required(CommandOptions.isShipment).getAsBoolean();
        boolean canTrigger = discordGame.required(CommandOptions.canTrigger).getAsBoolean();
        placeForces(targetTerritory, targetFaction, amountValue, starredAmountValue, isShipment, canTrigger, discordGame, game, false);
        discordGame.pushGame();
    }

    public void moveForcesEventHandler(DiscordGame discordGame, Game game) throws ChannelNotFoundException, InvalidOptionException {
        Faction targetFaction = game.getFaction(discordGame.required(faction).getAsString());
        Territory from = game.getTerritories().get(discordGame.required(fromTerritory).getAsString());
        Territory to = game.getTerritories().get(discordGame.required(toTerritory).getAsString());
        int amountValue = discordGame.required(amount).getAsInt();
        int starredAmountValue = discordGame.required(starredAmount).getAsInt();

        moveForces(targetFaction, from, to, amountValue, starredAmountValue, discordGame, game);
        discordGame.pushGame();
    }

    public void removeForcesEventHandler(DiscordGame discordGame, Game game) throws ChannelNotFoundException, InvalidGameStateException {
        String territoryName = discordGame.required(fromTerritory).getAsString();
        Faction targetFaction = game.getFaction(discordGame.required(faction).getAsString());
        int amountValue = discordGame.required(amount).getAsInt();
        int specialAmount = discordGame.required(starredAmount).getAsInt();
        if (amountValue < 0 || specialAmount < 0)
            throw new InvalidGameStateException("Negative numbers are invalid.");
        if (amountValue == 0 && specialAmount == 0) {
//            throw new InvalidGameStateException("Both force amounts cannot be 0.");
            RunCommands.flipToHighThresholdIfApplicable(discordGame, game);
            discordGame.pushGame();
            return;
        }
        boolean isToTanks = discordGame.required(toTanks).getAsBoolean();

        game.removeForces(territoryName, targetFaction, amountValue, specialAmount, isToTanks);
        String forcesString = "";
        if (amountValue > 0) forcesString += MessageFormat.format("{0} {1} ", amountValue, Emojis.getForceEmoji(targetFaction.getName()));
        if (specialAmount > 0) forcesString += MessageFormat.format("{0} {1} ", specialAmount, Emojis.getForceEmoji(targetFaction.getName() + "*"));
        discordGame.getTurnSummary().queueMessage(MessageFormat.format(
                "{0}in {1} were sent to {2}.", forcesString, territoryName, (isToTanks ? "the tanks" : "reserves")
        ));
        RunCommands.flipToHighThresholdIfApplicable(discordGame, game);
        discordGame.pushGame();
    }

    private void assassinateLeader(DiscordGame discordGame, Game game) throws ChannelNotFoundException {
        MoritaniFaction moritani = (MoritaniFaction) game.getFaction("Moritani");
        String assassinated = moritani.getTraitorHand().get(0).name();
        moritani.getAssassinationTargets().add(assassinated);
        moritani.getTraitorHand().clear();
        moritani.setUpdated(UpdateType.MISC_FRONT_OF_SHIELD);
        for (Faction faction : game.getFactions()) {
            Optional<Leader> optLeader = faction.getLeaders().stream().filter(leader1 -> leader1.getName().equals(assassinated)).findFirst();
            if (optLeader.isPresent()) {
                game.getLeaderTanks().add(faction.removeLeader(assassinated));
                Leader leader = optLeader.get();
                int spiceGained = leader.getName().equals("Zoal") ? 3 : leader.getValue();
                moritani.addSpice(spiceGained);
                moritani.spiceMessage(spiceGained, "assassination of " + assassinated, true);
                break;
            }
        }
        discordGame.getTurnSummary().queueMessage(Emojis.MORITANI + " have assassinated " + assassinated + "!");
        moritani.setNewAssassinationTargetNeeded(true);
        discordGame.pushGame();
    }

    public void setStorm(DiscordGame discordGame, Game game) throws ChannelNotFoundException, IOException {
        int stormDialOne = discordGame.required(dialOne).getAsInt();
        int stormDialTwo = discordGame.required(dialTwo).getAsInt();
        game.setInitialStorm(stormDialOne, stormDialTwo);
        discordGame.pushGame();
        ShowCommands.showBoard(discordGame, game);
    }

    public void setStormMovement(DiscordGame discordGame, Game game) throws ChannelNotFoundException {
        int stormMovement = discordGame.required(sectors).getAsInt();
        game.setStormMovement(stormMovement);

        discordGame.pushGame();
    }

    public void assignTechToken(DiscordGame discordGame, Game game) throws ChannelNotFoundException {
        for (Faction f : game.getFactions()) {
            if (f.getTechTokens().removeIf(
                    techToken -> techToken.getName().equals(discordGame.required(token).getAsString())))
                discordGame.getFactionLedger(f).queueMessage(
                        discordGame.required(token).getAsString() + " was sent to " + game.getFaction(discordGame.required(faction).getAsString()).getEmoji());
        }
        game.getFaction(discordGame.required(faction).getAsString())
                .getTechTokens().add(new TechToken(discordGame.required(token).getAsString()));
        discordGame.getFactionLedger(discordGame.required(faction).getAsString()).queueMessage(discordGame.required(token).getAsString() + " transferred to you.");
        discordGame.getTurnSummary().queueMessage(
                discordGame.required(token).getAsString() + " has been transferred to " +
                        game.getFaction(discordGame.required(faction).getAsString()).getEmoji());
        game.setUpdated(UpdateType.MAP);
        discordGame.pushGame();
    }

    public void bribe(DiscordGame discordGame, Game game) throws ChannelNotFoundException {
        Faction fromFaction = game.getFaction(discordGame.required(faction).getAsString());
        Faction recipientFaction = game.getFaction(discordGame.required(recipient).getAsString());
        int amountValue = discordGame.required(amount).getAsInt();

        if (amountValue != 0) {
            if (fromFaction.getSpice() < amountValue) {
                discordGame.getModInfo().queueMessage("Faction does not have enough spice to pay the bribe!");
                return;
            }
            fromFaction.subtractSpice(amountValue);
            fromFaction.spiceMessage(amountValue, "bribe to " + recipientFaction.getEmoji(), false);
            discordGame.getTurnSummary().queueMessage(
                    MessageFormat.format(
                            "{0} places {1} {2} in front of {3} shield.",
                            fromFaction.getEmoji(), amountValue, Emojis.SPICE, recipientFaction.getEmoji()
                    )
            );

            recipientFaction.addFrontOfShieldSpice(amountValue);
        } else {
            discordGame.getTurnSummary().queueMessage(
                    MessageFormat.format(
                            "{0} bribes {2}. {1} TBD or NA.",
                            fromFaction.getEmoji(), Emojis.SPICE, recipientFaction.getEmoji()
                    )
            );
        }

        String message = MessageFormat.format("{0} {1}",
                fromFaction.getEmoji(), recipientFaction.getEmoji());
        if (discordGame.optional(reason) != null) {
            message += "\n" + discordGame.optional(reason).getAsString();
        }
        discordGame.queueMessage("bribes", message);
        discordGame.pushGame();
    }

    public void mute(DiscordGame discordGame, Game game) throws ChannelNotFoundException {
        game.setMute(!game.getMute());

        discordGame.pushGame();
    }

    public void displayGameState(DiscordGame discordGame, Game game) throws ChannelNotFoundException {
        TextChannel channel = discordGame.getTextChannel("mod-info");
        switch (discordGame.required(data).getAsString()) {
            case "territories" -> {
                Map<String, Territory> territories = game.getTerritories();
                for (Territory territory : territories.values()) {
                    if (territory.getSpice() == 0 && !territory.isStronghold() && territory.getForces().isEmpty())
                        continue;
                    discordGame.queueMessage(channel.getName(), "**" + territory.getTerritoryName() + "** \n" +
                            "Spice: " + territory.getSpice() + "\nForces: " + territory.getForces().toString());
                }
            }
            case "dnd" -> {
                discordGame.getModInfo().queueMessage(game.getTreacheryDeck().toString());
                discordGame.getModInfo().queueMessage(game.getTreacheryDiscard().toString());
                discordGame.getModInfo().queueMessage(game.getSpiceDeck().toString());
                discordGame.getModInfo().queueMessage(game.getSpiceDiscardA().toString());
                discordGame.getModInfo().queueMessage(game.getSpiceDiscardB().toString());
                discordGame.getModInfo().queueMessage(game.getLeaderSkillDeck().toString());
                discordGame.getModInfo().queueMessage(game.getTraitorDeck().toString());
                try {
                    Bidding bidding = game.getBidding();
                    discordGame.getModInfo().queueMessage(bidding.getMarket().toString());
                } catch (InvalidGameStateException e) {
                    discordGame.getModInfo().queueMessage("No bidding state. Game is not in bidding phase.");
                }
            }
            case "factions" -> {
                for (Faction faction : game.getFactions()) {
                    String message = "**" + faction.getName() + ":**\nPlayer: " + faction.getUserName() + "\n" +
                            "spice: " + faction.getSpice() + "\nTreachery Cards: " + faction.getTreacheryHand() +
                            "\nTraitors:" + faction.getTraitorHand() + "\nLeaders: " + faction.getLeaders() + "\n";
                    discordGame.queueMessage(channel.getName(), message);
                }
            }
        }
        game.setUpdated(UpdateType.MAP);
    }

    public void createAlliance(DiscordGame discordGame, Game game) throws ChannelNotFoundException, IOException {
        Faction faction1 = game.getFaction(discordGame.required(faction).getAsString());
        Faction faction2 = game.getFaction(discordGame.required(otherFaction).getAsString());

        Alliance.createAlliance(discordGame, faction1, faction2);

        discordGame.pushGame();
    }

    public void removeAlliance(DiscordGame discordGame, Game game) throws ChannelNotFoundException {
        Faction targetFaction = game.getFaction(discordGame.required(faction).getAsString());

        game.removeAlliance(targetFaction);

        discordGame.pushGame();
    }


    public void setSpiceInTerritory(DiscordGame discordGame, Game game) throws ChannelNotFoundException {
        String territoryName = discordGame.required(territory).getAsString();
        int amountValue = discordGame.required(amount).getAsInt();

        game.getTerritories().get(territoryName).setSpice(amountValue);
        game.setUpdated(UpdateType.MAP);
        discordGame.pushGame();
    }

    public void destroyShieldWall(DiscordGame discordGame, Game game) throws ChannelNotFoundException, InvalidGameStateException {
        Faction factionWithAtomics;
        try {
            factionWithAtomics = game.getFactionWithAtomics();
        } catch (NoSuchElementException e) {
            throw new InvalidGameStateException("No faction holds Family Atomics.");
        }

        if (!factionWithAtomics.isNearShieldWall()) {
            throw new InvalidGameStateException(factionWithAtomics.getEmoji() + " is not in position to use Family Atomics.");
        } else {
            String message = game.breakShieldWall(factionWithAtomics);
            discordGame.getTurnSummary().queueMessage(message);
            game.setUpdated(UpdateType.MAP);
            discordGame.pushGame();
        }
    }

    public void reassignFaction(DiscordGame discordGame, Game game) throws ChannelNotFoundException {
        String factionName = discordGame.required(faction).getAsString();
        String playerName = discordGame.required(user).getAsUser().getAsMention();
        Member player = discordGame.required(user).getAsMember();

        if (player == null) throw new IllegalArgumentException("Not a valid user");

        String userName = player.getNickname();

        Faction faction = game.getFaction(factionName);
        faction.setPlayer(playerName);
        faction.setUserName(userName);

        discordGame.pushGame();
    }

    public void reassignMod(SlashCommandInteractionEvent event, DiscordGame discordGame, Game game) throws ChannelNotFoundException {
        game.setMod(event.getUser().getAsMention());
        List<Role> roles = Objects.requireNonNull(event.getGuild()).getRolesByName(game.getGameRole(), false);
        if (!roles.isEmpty()) {
            game.setGameRoleMention(roles.get(0).getAsMention());
        }
        discordGame.pushGame();
    }

    public String waitingListItemResult(String itemEmoji, String choice) {
        return switch (choice) {
            case "Yes" -> itemEmoji + " :white_check_mark:";
            case "Maybe" -> itemEmoji + " :ok:";
            default -> itemEmoji + " :no_entry_sign:";
        };
    }

    public void waitingList(SlashCommandInteractionEvent event) {
        String userTag = event.getUser().getId();
        TextChannel textChannel = Objects.requireNonNull(event.getGuild()).getTextChannelsByName("waiting-list", true).get(0);
        String message = "Speed: ";
        message += waitingListItemResult(":scooter:", DiscordGame.required(slowGame, event).getAsString());
        message += " -- ";
        message += waitingListItemResult(":blue_car:", DiscordGame.required(midGame, event).getAsString());
        message += " -- ";
        message += waitingListItemResult(":race_car:", DiscordGame.required(fastGame, event).getAsString());
        message += "\nExpansions: ";
        message += waitingListItemResult("O6", DiscordGame.required(originalSixFactions, event).getAsString());
        message += " -- ";
        message += waitingListItemResult("<:bt:991763325576810546> <:ix:991763319406997514>", DiscordGame.required(ixianstleilaxuExpansion, event).getAsString());
        message += " -- ";
        message += waitingListItemResult("<:choam:991763324624703538> <:rich:991763318467465337>", DiscordGame.required(choamricheseExpansion, event).getAsString());
        message += " -- ";
        message += waitingListItemResult("<:ecaz:1142126129105346590> <:moritani:1142126199775182879>", DiscordGame.required(ecazmoritaniExpansion, event).getAsString());
        message += "\nOptions: ";
        message += waitingListItemResult("<:weirding:991763071775297681>", DiscordGame.required(leaderSkills, event).getAsString());
        message += " -- ";
        message += waitingListItemResult(":european_castle:", DiscordGame.required(strongholdCards, event).getAsString());
        message += " -- ";
        message += waitingListItemResult(":ringed_planet:", DiscordGame.required(homeworlds, event).getAsString());
        message += "\nUser: <@" + userTag + ">";
        textChannel.sendMessage(message).queue();
        // textChannel.sendMessage("Speed: :turtle: " + event.getOption(slowGame.getName()).getAsBoolean() + " :racehorse: " + event.getOption(midGame.getName()).getAsBoolean() + " :race_car: " + event.getOption(fastGame.getName()).getAsBoolean() + "\nExpansions: <:bt:991763325576810546> <:ix:991763319406997514>  " + event.getOption(ixianstleilaxuExpansion.getName()).getAsBoolean() + " <:choam:991763324624703538> <:rich:991763318467465337> " + event.getOption(choamricheseExpansion.getName()).getAsBoolean() + " :ecaz: :moritani: " + event.getOption(ecazmoritaniExpansion.getName()).getAsBoolean() + "\nOptions: Leader Skills " + event.getOption(leaderSkills.getName()).getAsBoolean() + " Stronghold Cards " + event.getOption(strongholdCards.getName()).getAsBoolean() + "\nUser: <@" + userTag + ">").queue();
    }
}
