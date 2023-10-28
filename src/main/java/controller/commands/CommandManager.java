package controller.commands;

import constants.Emojis;
import controller.DiscordGame;
import controller.Queue;
import controller.channels.TurnSummary;
import enums.GameOption;
import enums.UpdateType;
import exceptions.ChannelNotFoundException;
import exceptions.InvalidGameStateException;
import exceptions.InvalidOptionException;
import model.*;
import model.factions.EmperorFaction;
import model.factions.Faction;
import model.factions.MoritaniFaction;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageHistory;
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
import net.dv8tion.jda.api.utils.TimeUtil;
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

            if (paidToFaction.getName().equals("Emperor") && game.hasGameOption(GameOption.HOMEWORLDS)
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

            if (paidToFaction.getName().equals("Richese") && paidToFaction.isHomeworldOccupied()) {
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

            paidToFaction.spiceMessage(spicePaid, currentCard, true);
            game.getFaction(paidToFaction.getName()).addSpice(spicePaid);

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
        if (winner.getName().equals("Harkonnen") && winnerHand.size() < winner.getHandLimit() && !winner.isHomeworldOccupied()) {
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

        } else if (winner.getName().equals("Harkonnen") && winner.isHomeworldOccupied() && winner.getOccupier().hasAlly()) {
            discordGame.getModInfo().queueMessage("Harkonnen occupier or ally may draw one from the deck (you must do this for them).");
            discordGame.getTurnSummary().queueMessage("Giedi Prime is occupied by " + winner.getOccupier().getName() + ", they or their ally may draw an additional card from the deck.");
        } else if (winner.getName().equals("Harkonnen") && winner.isHomeworldOccupied() && winner.getOccupier().getTreacheryHand().size() < winner.getOccupier().getHandLimit()) {
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

    public static void revival(boolean starred, Faction faction, boolean isPaid, int revivedValue, Game game, DiscordGame discordGame) throws ChannelNotFoundException {
        String star = starred ? "*" : "";

        int revivalCost;

        if (faction.getName().equalsIgnoreCase("CHOAM")) revivalCost = revivedValue;
        else if (faction.getName().equalsIgnoreCase("BT")) revivalCost = revivedValue;
        else revivalCost = revivedValue * 2;

        if (star.isEmpty()) faction.addReserves(revivedValue);
        else faction.addSpecialReserves(revivedValue);

        Force force = game.getForceFromTanks(faction.getName() + star);
        force.setStrength(force.getStrength() - revivedValue);

        if (isPaid) {
            faction.subtractSpice(revivalCost);
            faction.spiceMessage(revivalCost, "Revivals", false);
            if (game.hasFaction("BT") && !faction.getName().equalsIgnoreCase("BT")) {
                Faction btFaction = game.getFaction("BT");
                btFaction.addSpice(revivalCost);
                btFaction.spiceMessage(revivalCost, faction.getEmoji() + " revivals", true);
            }
        }
        discordGame.getFactionLedger(faction).queueMessage(revivedValue + " " + Emojis.getForceEmoji(faction.getName() + star) + " returned to reserves.");
        String costString = isPaid ? " for " + revivalCost + " " + Emojis.SPICE : "";
        discordGame.getTurnSummary().queueMessage(faction.getEmoji() + " revives " + revivedValue + " " + Emojis.getForceEmoji(faction.getName() + star) + costString);
        RunCommands.flipToHighThresholdIfApplicable(discordGame, game);
        if (game.hasGameOption(GameOption.MAP_IN_FRONT_OF_SHIELD)) {
            faction.setUpdated(UpdateType.MAP);
        }
        discordGame.pushGame();
    }

    public static void placeForces(Territory targetTerritory, Faction targetFaction, int amountValue, int starredAmountValue, boolean isShipment, DiscordGame discordGame, Game game, boolean karama) throws ChannelNotFoundException {

        Force reserves = targetFaction.getReserves();
        Force specialReserves = targetFaction.getSpecialReserves();

        if (amountValue > 0) {
            placeForceInTerritory(targetTerritory, targetFaction, amountValue, false);
            discordGame.getFactionLedger(targetFaction).queueMessage(
                    MessageFormat.format("{0} {1} removed from reserves.", amountValue, Emojis.getForceEmoji(targetFaction.getName())));
        }

        if (starredAmountValue > 0) {
            placeForceInTerritory(targetTerritory, targetFaction, starredAmountValue, true);
            discordGame.getFactionLedger(targetFaction).queueMessage(
                    MessageFormat.format("{0} {1} removed from reserves.", starredAmountValue, Emojis.getForceEmoji(targetFaction.getName() + "*")));
        }

        if (isShipment) {
            targetFaction.getShipment().setShipped(true);
            int costPerForce = targetTerritory.isStronghold() || targetTerritory.getTerritoryName().matches("Cistern|Ecological Testing Station|Shrine|Orgiz Processing Station") ? 1 : 2;
            int baseCost = costPerForce * (amountValue + starredAmountValue);
            int cost;

            if (targetFaction.getName().equalsIgnoreCase("Guild") || (targetFaction.hasAlly() && targetFaction.getAlly().equals("Guild")) || karama) {
                cost = Math.ceilDiv(baseCost, 2);
            } else if (targetFaction.getName().equalsIgnoreCase("Fremen")) {
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

                if (game.hasFaction("Guild") && !targetFaction.getName().equals("Guild") && !karama) {
                    Faction guildFaction = game.getFaction("Guild");
                    guildFaction.addSpice(cost);
                    message.append(" paid to ")
                            .append(Emojis.GUILD);
                    guildFaction.spiceMessage(cost, targetFaction.getEmoji() + " shipment", true);
                }

            }

            if (
                    !targetFaction.getName().equalsIgnoreCase("Guild") &&
                            !targetFaction.getName().equalsIgnoreCase("Fremen") &&
                            game.hasGameOption(GameOption.TECH_TOKENS)
            ) {
                TechToken.addSpice(game, discordGame, "Heighliners");
            }

            TurnSummary turnSummary = discordGame.getTurnSummary();
            if (game.hasFaction("BG") && !(targetFaction.getName().equals("BG") || targetFaction.getName().equals("Fremen"))
                    && !(game.hasGameOption(GameOption.HOMEWORLDS) && !game.getFaction("BG").isHighThreshold()
                    && !game.getHomeworlds().containsValue(targetTerritory.getTerritoryName()))) {
                List<Button> buttons = new LinkedList<>();
                String territoryName = targetTerritory.getTerritoryName();
                buttons.add(Button.primary("bg-advise-" + territoryName, "Advise"));
                buttons.add(Button.secondary("bg-advise-Polar Sink", "Advise to Polar Sink"));
                buttons.add(Button.secondary("bg-ht", "Advise 2 to Polar Sink"));
                buttons.add(Button.danger("bg-dont-advise-" + territoryName, "No"));
                discordGame.getBGChat().queueMessage(Emojis.BG + " Would you like to advise the shipment to " + territoryName + "?" + game.getFaction("BG").getPlayer(), buttons);
            }
            turnSummary.queueMessage(message.toString());

            if (game.hasFaction("BG") && targetTerritory.hasActiveFaction(game.getFaction("BG")) && !targetFaction.getName().equals("BG")) {
                List<Button> buttons = new LinkedList<>();
                buttons.add(Button.primary("bg-flip-" + targetTerritory.getTerritoryName(), "Flip"));
                buttons.add(Button.secondary("bg-dont-flip-" + targetTerritory.getTerritoryName(), "Don't Flip"));
                turnSummary.queueMessage(Emojis.BG + " to decide whether they want to flip to " + Emojis.BG_ADVISOR + " in " + targetTerritory.getTerritoryName() + game.getFaction("BG").getPlayer(), buttons);
            }
            if (targetTerritory.getEcazAmbassador() != null && !targetFaction.getName().equals("Ecaz")
                    && !targetFaction.getName().equals(targetTerritory.getEcazAmbassador())
                    && !(game.getFaction("Ecaz").hasAlly()
                    && game.getFaction("Ecaz").getAlly().equals(targetFaction.getName()))) {
                List<Button> buttons = new LinkedList<>();
                buttons.add(Button.primary("ecaz-trigger-ambassador-" + targetTerritory.getEcazAmbassador() + "-" + targetFaction.getName(), "Trigger"));
                buttons.add(Button.danger("ecaz-don't-trigger-ambassador", "Don't Trigger"));
                turnSummary.queueMessage(Emojis.ECAZ + " has an opportunity to trigger their ambassador now." + game.getFaction("Ecaz").getPlayer(), buttons);
            }

            if (!targetTerritory.getTerrorTokens().isEmpty() && !targetFaction.getName().equals("Moritani")
                    && (!(game.getFaction("Moritani").hasAlly()
                    && game.getFaction("Moritani").getAlly().equals(targetFaction.getName())))) {
                if (!game.getFaction("Moritani").isHighThreshold() && amountValue + starredAmountValue < 3) {
                    turnSummary.queueMessage(Emojis.MORITANI + " are at low threshold and may not trigger their Terror Token at this time");
                } else {
                    ((MoritaniFaction)game.getFaction("Moritani")).sendTerrorTokenTriggerMessage(game, discordGame, targetTerritory, targetFaction);
                }
            }
        }
        if (game.hasGameOption(GameOption.HOMEWORLDS) && (reserves.getStrength() + specialReserves.getStrength() < targetFaction.getHighThreshold()) && targetFaction.isHighThreshold()) {
            discordGame.getTurnSummary().queueMessage(targetFaction.getEmoji() + " is now at Low Threshold.");
            targetFaction.setHighThreshold(false);
        }
        if (game.hasGameOption(GameOption.MAP_IN_FRONT_OF_SHIELD)) {
            game.setUpdated(UpdateType.MAP);
        }
    }

    /**
     * Places a force from the reserves into a territory.
     *
     * @param territory The territory to place the force in.
     * @param faction   The faction that owns the force.
     * @param amount    The number of forces to place.
     * @param special   Whether the force is a special reserve.
     */
    public static void placeForceInTerritory(Territory territory, Faction faction, int amount, boolean special) {
        String forceName;

        if (special) {
            faction.removeSpecialReserves(amount);
            forceName = faction.getSpecialReserves().getName();
        } else {
            faction.removeReserves(amount);
            forceName = faction.getReserves().getName();
            if (faction.getName().equals("BG") && territory.hasForce("Advisor")) {
                int advisors = territory.getForce("Advisor").getStrength();
                territory.getForces().add(new Force("BG", advisors));
                territory.removeForce("Advisor");
            }
        }
        Force territoryForce = territory.getForce(forceName);
        territory.setForceStrength(forceName, territoryForce.getStrength() + amount);
    }

    public static void moveForces(Faction targetFaction, Territory from, Territory to, int amountValue, int starredAmountValue, DiscordGame discordGame, Game game) throws ChannelNotFoundException, InvalidOptionException {

        int fromForceStrength = from.getForce(targetFaction.getName()).getStrength();
        if (targetFaction.getName().equals("BG") && from.getForce("Advisor").getStrength() > 0)
            fromForceStrength = from.getForce("Advisor").getStrength();
        int fromStarredForceStrength = from.getForce(targetFaction.getName() + "*").getStrength();

        if (fromForceStrength < amountValue || fromStarredForceStrength < starredAmountValue) {
            throw new InvalidOptionException("Not enough forces in territory.");
        }
        if (targetFaction.hasAlly() && to.hasActiveFaction(game.getFaction(targetFaction.getAlly())) &&
                (!(targetFaction.getName().equals("Ecaz") && to.getActiveFactions(game).stream().anyMatch(f -> f.getName().equals(targetFaction.getAlly())))
                        && !(targetFaction.getAlly().equals("Ecaz") && to.getActiveFactions(game).stream().anyMatch(f -> f.getName().equals("Ecaz"))))) {
            throw new InvalidOptionException("You cannot move into a territory with your ally.");
        }

        StringBuilder message = new StringBuilder();

        message.append(targetFaction.getEmoji())
                .append(": ");

        if (amountValue > 0) {
            String forceName = targetFaction.getName();
            String targetForceName = targetFaction.getName();
            if (targetFaction.getName().equals("BG") && from.hasForce("Advisor")) {
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

        if (game.hasFaction("BG") && to.hasActiveFaction(game.getFaction("BG")) && !targetFaction.getName().equals("BG")) {
            List<Button> buttons = new LinkedList<>();
            buttons.add(Button.primary("bg-flip-" + to.getTerritoryName(), "Flip"));
            buttons.add(Button.secondary("bg-dont-flip-" + to.getTerritoryName(), "Don't Flip"));
            turnSummary.queueMessage(Emojis.BG + " to decide whether they want to flip to " + Emojis.BG_ADVISOR + " in " + to.getTerritoryName() + game.getFaction("BG").getPlayer(), buttons);
        }
        if (game.getTerritory(to.getTerritoryName()).getEcazAmbassador() != null && !targetFaction.getName().equals("Ecaz")
                && !targetFaction.getName().equals(game.getTerritory(to.getTerritoryName()).getEcazAmbassador())
                && !(game.getFaction("Ecaz").hasAlly()
                && game.getFaction("Ecaz").getAlly().equals(targetFaction.getName()))) {
            List<Button> buttons = new LinkedList<>();
            buttons.add(Button.primary("ecaz-trigger-ambassador-" + to.getEcazAmbassador() + "-" + targetFaction.getName(), "Trigger"));
            buttons.add(Button.danger("ecaz-don't-trigger-ambassador", "Don't Trigger"));
            turnSummary.queueMessage(Emojis.ECAZ + " has an opportunity to trigger their ambassador now." + game.getFaction("Ecaz").getPlayer(), buttons);
        }

        if (!to.getTerrorTokens().isEmpty() && !targetFaction.getName().equals("Moritani")
                && !(game.getFaction("Moritani").hasAlly()
                && game.getFaction("Moritani").getAlly().equals(targetFaction.getName()))) {
            if (!game.getFaction("Moritani").isHighThreshold() && amountValue + starredAmountValue < 3) {
                turnSummary.queueMessage(Emojis.MORITANI + " are at low threshold and may not trigger their Terror Token at this time");
            } else {
                ((MoritaniFaction)game.getFaction("Moritani")).sendTerrorTokenTriggerMessage(game, discordGame, to, targetFaction);
            }
        }
        if (game.hasGameOption(GameOption.MAP_IN_FRONT_OF_SHIELD)) {
            game.setUpdated(UpdateType.MAP);
        }
    }

    public static void removeForces(String territoryName, Faction targetFaction, int amountValue, int specialAmount, boolean isToTanks, Game game, DiscordGame discordGame) throws ChannelNotFoundException {
        targetFaction.removeForces(territoryName, amountValue, false, isToTanks);
        if (specialAmount > 0) targetFaction.removeForces(territoryName, specialAmount, true, isToTanks);
        if (game.hasGameOption(GameOption.HOMEWORLDS) && game.getHomeworlds().containsValue(territoryName)) {
            Faction homeworldFaction = game.getFactions().stream().filter(f -> f.getHomeworld().equals(territoryName) || (f.getName().equals("Emperor") && territoryName.equals("Salusa Secundus"))).findFirst().get();
            if (territoryName.equals("Salusa Secundus") && ((EmperorFaction) homeworldFaction).getSecundusHighThreshold() > game.getTerritory("Salusa Secundus").getForce("Emperor*").getStrength() && ((EmperorFaction) homeworldFaction).isSecundusHighThreshold()) {
                ((EmperorFaction) homeworldFaction).setSecundusHighThreshold(false);
                discordGame.getTurnSummary().queueMessage("Salusa Secundus has flipped to low threshold.");

            } else if (homeworldFaction.isHighThreshold() && homeworldFaction.getHighThreshold() > game.getTerritory(territoryName).getForce(faction.getName()).getStrength() + game.getTerritory(territoryName).getForce(faction.getName() + "*").getStrength()) {
                homeworldFaction.setHighThreshold(false);
                discordGame.getTurnSummary().queueMessage(homeworldFaction.getHomeworld() + " has flipped to low threshold.");
            }

            if (territoryName.equals("Ecaz") && game.getFaction("Ecaz").isHomeworldOccupied()) {
                for (Faction faction1 : game.getFactions()) {
                    faction1.getLeaders().removeIf(leader1 -> leader1.name().equals("Duke Vidal"));
                }
                game.getFaction("Ecaz").getOccupier().getLeaders().add(new Leader("Duke Vidal", 6, null, false));
                discordGame.getTurnSummary().queueMessage("Duke Vidal has left to work for " + game.getFaction("Ecaz").getOccupier().getEmoji() + " (planet Ecaz occupied)");
            }
        }
        if (game.hasGameOption(GameOption.MAP_IN_FRONT_OF_SHIELD)) {
            game.setUpdated(UpdateType.MAP);
        }
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
            }  else if (name.equals("num-games-per-player")) {
                    String result = numGamesPerPlayer(event);
                    event.getHook().editOriginal(result).queue();
            } else if (name.equals("random-dune-quote")) {
                randomDuneQuote(event);
            } else {
                String categoryName = DiscordGame.categoryFromEvent(event).getName();
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
                case "richese" -> RicheseCommands.runCommand(event, discordGame, game);
                case "bt" -> BTCommands.runCommand(event, discordGame, game);
                case "hark" -> HarkCommands.runCommand(event, discordGame, game);
                case "choam" -> ChoamCommands.runCommand(event, discordGame, game);
                case "ix" -> IxCommands.runCommand(event, discordGame, game);
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
                case "revive-forces" -> revivalHandler(discordGame, game);
                case "award-bid" -> awardBid(event, discordGame, game);
                case "award-top-bidder" -> awardTopBidder(discordGame, game);
                case "kill-leader" -> killLeader(discordGame, game);
                case "revive-leader" -> reviveLeader(discordGame, game);
                case "set-storm" -> setStorm(discordGame, game);
                case "bribe" -> bribe(discordGame, game);
                case "mute" -> mute(discordGame, game);
                case "assign-tech-token" -> assignTechToken(discordGame, game);
                case "draw-spice-blow" -> drawSpiceBlow(discordGame, game);
                case "create-alliance" -> createAlliance(discordGame, game);
                case "remove-alliance" -> removeAlliance(discordGame, game);
                case "set-spice-in-territory" -> setSpiceInTerritory(discordGame, game);
                case "destroy-shield-wall" -> destroyShieldWall(discordGame, game);
                case "weather-control-storm" -> weatherControlStorm(discordGame, game);
                case "add-spice" -> addSpice(discordGame, game);
                case "remove-spice" -> removeSpice(discordGame, game);
                case "reassign-faction" -> reassignFaction(discordGame, game);
                case "reassign-mod" -> reassignMod(event, discordGame, game);
                case "draw-nexus-card" -> drawNexusCard(discordGame, game);
                case "discard-nexus-card" -> discardNexusCard(discordGame, game);
                case "moritani-assassinate-leader" -> assassinateLeader(discordGame, game);
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

    private void randomDuneQuote(SlashCommandInteractionEvent event) throws IOException {
        int lines = event.getOption("lines").getAsInt();
        if (event.getOption("search") == null) {
            if (event.getOption("book") == null) {
                InputStream stream = getClass().getClassLoader().getResourceAsStream("Dune Books/Dune.txt");
                List<String> quotes = Arrays.stream(new String(stream.readAllBytes(), StandardCharsets.UTF_8).split("((?<=\\.|\\?|!))")).toList();
                Random random = new Random();
                int start = event.getOption("starting-line") != null ? event.getOption("starting-line").getAsInt() : random.nextInt(quotes.size() - lines + 1);
                StringBuilder quote = new StringBuilder();

                for (int i = 0; i < lines; i++) {
                    quote.append(quotes.get(start + i));
                }
                event.getMessageChannel().sendMessage(quote + "\n\n(Line: " + start + ")").queue();

            } else {
                InputStream stream = getClass().getClassLoader().getResourceAsStream("Dune Books/" + event.getOption("book").getAsString());

                List<String> quotes = Arrays.stream(new String(stream.readAllBytes(), StandardCharsets.UTF_8).split("((?<=\\.|\\?|!))")).toList();
                Random random = new Random();
                int start = event.getOption("starting-line") != null ? event.getOption("starting-line").getAsInt() : random.nextInt(quotes.size() - lines + 1);
                StringBuilder quote = new StringBuilder();

                for (int i = 0; i < lines; i++) {
                    quote.append(quotes.get(start + i));
                }
                event.getMessageChannel().sendMessage(quote + "\n\n(Line: " + start + ")").queue();
            }
        } else {
            String search = event.getOption("search").getAsString();
            InputStream stream = getClass().getClassLoader().getResourceAsStream("Dune Books/Dune.txt");
            List<String> quotes = new ArrayList<>(Arrays.stream(new String(stream.readAllBytes(), StandardCharsets.UTF_8).split("((?<=\\.|\\?|!))")).toList());
            stream = getClass().getClassLoader().getResourceAsStream("Dune Books/Messiah.txt");
            quotes.addAll(Arrays.stream(new String(stream.readAllBytes(), StandardCharsets.UTF_8).split("((?<=\\.|\\?|!))")).toList());
            stream = getClass().getClassLoader().getResourceAsStream("Dune Books/Children.txt");
            quotes.addAll(Arrays.stream(new String(stream.readAllBytes(), StandardCharsets.UTF_8).split("((?<=\\.|\\?|!))")).toList());
            stream = getClass().getClassLoader().getResourceAsStream("Dune Books/GeoD.txt");
            quotes.addAll(Arrays.stream(new String(stream.readAllBytes(), StandardCharsets.UTF_8).split("((?<=\\.|\\?|!))")).toList());
            stream = getClass().getClassLoader().getResourceAsStream("Dune Books/Heretics.txt");
            quotes.addAll(Arrays.stream(new String(stream.readAllBytes(), StandardCharsets.UTF_8).split("((?<=\\.|\\?|!))")).toList());
            stream = getClass().getClassLoader().getResourceAsStream("Dune Books/Chapterhouse.txt");
            quotes.addAll(Arrays.stream(new String(stream.readAllBytes(), StandardCharsets.UTF_8).split("((?<=\\.|\\?|!))")).toList());
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
            int start = event.getOption("starting-line") != null ? event.getOption("starting-line").getAsInt() - 1 : random.nextInt(matched.size() - 1);

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

    @Override
    public void onGuildReady(@NotNull GuildReadyEvent event) {
        //add new slash command definitions to commandData list
        List<CommandData> commandData = new ArrayList<>();
        commandData.add(Commands.slash("new-game", "Creates a new Dune game instance.").addOptions(gameName, gameRole, modRole));
        commandData.add(Commands.slash("num-games-per-player", "Report on how many games players are in.").addOptions(months));
        commandData.add(Commands.slash("draw-treachery-card", "Draw a card from the top of a deck.").addOptions(faction));
        commandData.add(Commands.slash("shuffle-treachery-deck", "Shuffle the treachery deck."));
        commandData.add(Commands.slash("discard", "Move a card from a faction's hand to the discard pile").addOptions(faction, card));
        commandData.add(Commands.slash("transfer-card", "Move a card from one faction's hand to another").addOptions(faction, card, recipient));
        commandData.add(Commands.slash("transfer-card-from-discard", "Move a card from the discard to a faction's hand").addOptions(faction, discardCard));
        commandData.add(Commands.slash("place-forces", "Place forces from reserves onto the surface").addOptions(faction, amount, starredAmount, isShipment, territory));
        commandData.add(Commands.slash("move-forces", "Move forces from one territory to another").addOptions(faction, fromTerritory, toTerritory, amount, starredAmount));
        commandData.add(Commands.slash("remove-forces", "Remove forces from the board.").addOptions(faction, amount, starredAmount, toTanks, fromTerritory));
        commandData.add(Commands.slash("award-bid", "Designate that a card has been won by a faction during bidding phase.").addOptions(faction, spent, paidToFaction));
        commandData.add(Commands.slash("award-top-bidder", "Designate that a card has been won by the top bidder during bidding phase and pay spice recipient."));
        commandData.add(Commands.slash("revive-forces", "Revive forces for a faction.").addOptions(faction, revived, starred, paid));
        commandData.add(Commands.slash("display", "Displays some element of the game to the mod.").addOptions(data));
        commandData.add(Commands.slash("set-storm", "Sets the storm to an initial sector.").addOptions(dialOne, dialTwo));
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
        commandData.add(Commands.slash("weather-control-storm", "Override the storm movement").addOptions(sectors));

        commandData.add(Commands.slash("add-spice", "Add spice to a faction").addOptions(faction, amount, message, frontOfShield));
        commandData.add(Commands.slash("remove-spice", "Remove spice from a faction").addOptions(faction, amount, message, frontOfShield));
        commandData.add(Commands.slash("reassign-faction", "Assign the faction to a different player").addOptions(faction, user));
        commandData.add(Commands.slash("reassign-mod", "Assign yourself as the mod to be tagged"));
        commandData.add(Commands.slash("draw-nexus-card", "Draw a nexus card.").addOptions(faction));
        commandData.add(Commands.slash("discard-nexus-card", "Discard a nexus card.").addOptions(faction));
        commandData.add(Commands.slash("moritani-assassinate-leader", "Assassinate leader ability"));
        commandData.add(Commands.slash("random-dune-quote", "Will dispense a random line of text from the specified book.").addOptions(lines, book, startingLine, search));

        commandData.addAll(GameStateCommands.getCommands());
        commandData.addAll(ShowCommands.getCommands());
        commandData.addAll(SetupCommands.getCommands());
        commandData.addAll(RunCommands.getCommands());
        commandData.addAll(RicheseCommands.getCommands());
        commandData.addAll(BTCommands.getCommands());
        commandData.addAll(HarkCommands.getCommands());
        commandData.addAll(ChoamCommands.getCommands());
        commandData.addAll(IxCommands.getCommands());
        commandData.addAll(BGCommands.getCommands());
        commandData.addAll(AtreidesCommands.getCommands());

        List<CommandData> commandDataWithPermissions = commandData.stream()
                .map(command -> command.setDefaultPermissions(
                        DefaultMemberPermissions.enabledFor(Permission.MANAGE_CHANNEL)
                ))
                .collect(Collectors.toList());

        commandDataWithPermissions.addAll(PlayerCommands.getCommands());
        commandDataWithPermissions.add(Commands.slash("waiting-list", "Add an entry to the waiting list")
                .addOptions(slowGame, midGame, fastGame, ixianstleilaxuExpansion, choamricheseExpansion, ecazmoritaniExpansion, leaderSkills, strongholdCards));

        event.getGuild().updateCommands().addCommands(commandDataWithPermissions).queue();
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

    private static class PlayerGame {
        String player;
        List<String> games;
        int numGames;
        boolean onWaitingList;
        boolean recentlyFinished;
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

    private void addWaitingListPlayers(HashMap<String, List<String>> playerGamesMap, Category category) {
        Optional<TextChannel> optChannel = category.getTextChannels().stream()
                .filter(c -> c.getName().equalsIgnoreCase("waiting-list"))
                .findFirst();
        if (optChannel.isPresent()) {
            TextChannel waitingList = optChannel.get();
            MessageHistory messageHistory = MessageHistory.getHistoryFromBeginning(waitingList).complete();
            List<Message> messages = messageHistory.getRetrievedHistory();
            for (Message m : messages) {
                int startChar = m.getContentRaw().indexOf("User:");
                if (startChar == -1) continue;
                for (String player : findPlayerTags(m.getContentRaw().substring(startChar))) {
                    List<String> games = playerGamesMap.computeIfAbsent(player, k -> new ArrayList<>());
                    if (games.isEmpty()) {
                        games.add("waiting-list");
                    }
                }
            }
        }
    }

    private void addRecentlyFinishedPlayers(HashMap<String, List<String>> playerGamesMap, Category category, int monthsAgo) {
        Optional<TextChannel> optChannel = category.getTextChannels().stream()
                .filter(c -> c.getName().equalsIgnoreCase("game-results"))
                .findFirst();
        if (optChannel.isPresent()) {
            Calendar c = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
            c.add(Calendar.MONTH, -1 * monthsAgo);
            long timestamp = c.getTimeInMillis();
            String discordTimestamp = Long.toUnsignedString(TimeUtil.getDiscordTimestamp(timestamp));
            TextChannel gameResults = optChannel.get();
            MessageHistory messageHistory = MessageHistory.getHistoryAfter(gameResults, discordTimestamp).complete();
            List<Message> messages = messageHistory.getRetrievedHistory();
            for (Message m : messages) {
                for (String player : findPlayerTags(m.getContentRaw())) {
                    List<String> games = playerGamesMap.computeIfAbsent(player, k -> new ArrayList<>());
                    if (games.isEmpty()) {
                        games.add("recently-finished");
                    }
                }
            }
        }
    }

    private void addGamePlayers(HashMap<String, List<String>> playerGamesMap, Category category, String categoryName) {
        try {
            DiscordGame discordGame = new DiscordGame(category, false);
            for (Faction faction : discordGame.getGame().getFactions()) {
                String player = faction.getPlayer();
                List<String> games = playerGamesMap.computeIfAbsent(player, k -> new ArrayList<>());
                games.add(categoryName);
            }
        } catch (Exception e) {
            // category is not a Dune game
        }
    }

    private String playerMessage(PlayerGame playerGame) {
        StringBuilder message = new StringBuilder("    " + playerGame.numGames + " - " + playerGame.player);
        if (playerGame.numGames != 0) message.append(" (");
        String comma = "";
        for (String categoryName : playerGame.games) {
            String printName = categoryName.substring(0, Math.min(5, categoryName.length()));
            if (categoryName.startsWith("Discord ")) {
                try {
                    printName = "D" +  new Scanner(categoryName).useDelimiter("\\D+").nextInt();
                } catch (Exception e) {
                    // category does not follow Discord N pattern
                }
            }
            message.append(comma).append(printName);
            comma = ", ";
        }
        if (playerGame.numGames != 0) message.append(")");
        message.append("\n");
        return message.toString();
    }

    private String playerGamesMessage(List<PlayerGame> playerGames, String header) {
        StringBuilder message = new StringBuilder();
        if (!playerGames.isEmpty()) {
            Comparator<PlayerGame> numGamesComparator = Comparator.comparingInt(playerGame -> playerGame.numGames);
            playerGames.sort(numGamesComparator);
            message.append(header);
            for (PlayerGame playerGame : playerGames) {
                message.append(playerMessage(playerGame));
            }
        }
        return message.toString();
    }

    private String numGamesPerPlayer(SlashCommandInteractionEvent event) {
        String message = "**Number of games players are in**\n";
        HashMap<String, List<String>> playerGamesMap = new HashMap<>();
        OptionMapping optionMapping = event.getOption(months.getName());
        int monthsAgo = (optionMapping != null ? optionMapping.getAsInt() : 1);
        List<Category> categories = Objects.requireNonNull(event.getGuild()).getCategories();
        for (Category category : categories) {
            String categoryName = category.getName();
            if (categoryName.equalsIgnoreCase("staging area")) {
                addWaitingListPlayers(playerGamesMap, category);
            } else if (categoryName.equalsIgnoreCase("dune statistics")) {
                addRecentlyFinishedPlayers(playerGamesMap, category, monthsAgo);
            } else {
                addGamePlayers(playerGamesMap, category, categoryName);
            }
        }
        List<PlayerGame> waitingPlayerGames = new ArrayList<>();
        List<PlayerGame> finishedPlayerGames = new ArrayList<>();
        List<PlayerGame> activePlayerGames = new ArrayList<>();
        for (Map.Entry<String, List<String>> entry : playerGamesMap.entrySet()) {
            PlayerGame pg = new PlayerGame();
            pg.player = entry.getKey();
            pg.games = entry.getValue();
            if (pg.games.get(0).equals("waiting-list")) {
                pg.games.remove(0);
                pg.onWaitingList = true;
            } else if (pg.games.get(0).equals("recently-finished")) {
                pg.games.remove(0);
                if (pg.games.isEmpty()) {
                    pg.recentlyFinished = true;
                }
            }
            pg.numGames = pg.games.size();
            if (pg.onWaitingList) waitingPlayerGames.add(pg);
            else if (pg.recentlyFinished) finishedPlayerGames.add(pg);
            else activePlayerGames.add(pg);
        }
        message += playerGamesMessage(waitingPlayerGames, "On waiting list:\n");
        message += playerGamesMessage(finishedPlayerGames,
                "Finished in last " + monthsAgo + " month" + (monthsAgo == 1 ? "" : "s") + ", not in a game:\n");
        message += playerGamesMessage(activePlayerGames, "Currently playing:\n");
        return message;
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

    public void drawSpiceBlow(DiscordGame discordGame, Game game) throws ChannelNotFoundException, IOException {
        String spiceBlowDeckName = discordGame.required(spiceBlowDeck).getAsString();

        LinkedList<SpiceCard> deck = game.getSpiceDeck();
        LinkedList<SpiceCard> discard = spiceBlowDeckName.equalsIgnoreCase("A") ?
                game.getSpiceDiscardA() : game.getSpiceDiscardB();
        LinkedList<SpiceCard> wormsToReshuffle = new LinkedList<>();

        StringBuilder message = new StringBuilder();

        SpiceCard drawn;

        message.append("**Spice Deck ").append(spiceBlowDeckName).append("**\n");

        boolean shaiHuludSpotted = false;
        int spiceMultiplier = 1;

        do {
            if (deck.isEmpty()) {
                deck.addAll(game.getSpiceDiscardA());
                deck.addAll(game.getSpiceDiscardB());
                Collections.shuffle(deck);
                game.getSpiceDiscardA().clear();
                game.getSpiceDiscardB().clear();
                message.append("The Spice Deck is empty, and will be recreated from the Discard piles.\n");
            }

            drawn = deck.pop();
            boolean saveWormForReshuffle = false;
            if (drawn.name().equalsIgnoreCase("Shai-Hulud") || drawn.name().equalsIgnoreCase("Great Maker")) {
                if (game.getTurn() <= 1) {
                    saveWormForReshuffle = true;
                    message.append(drawn.name())
                            .append(" will be reshuffled back into deck.\n");
                } else if (!discard.isEmpty() && !shaiHuludSpotted) {
                    shaiHuludSpotted = true;

                    if (game.isSandtroutInPlay()) {
                        spiceMultiplier = 2;
                        game.setSandtroutInPlay(false);
                        message.append(drawn.name())
                                .append(" has been spotted! The next Shai-Hulud will cause a Nexus!\n");
                    } else {
                        SpiceCard lastCard = discard.getLast();
                        message.append(drawn.name())
                                .append(" has been spotted in ").append(lastCard.name()).append("!\n");
                        int spice = game.getTerritories().get(lastCard.name()).getSpice();
                        if (spice > 0) {
                            message.append(spice);
                            message.append(Emojis.SPICE);
                            message.append(" is eaten by the worm!\n");
                            game.getTerritories().get(lastCard.name()).setSpice(0);
                        }
                    }

                } else {
                    shaiHuludSpotted = true;
                    spiceMultiplier = 1;
                    message.append(drawn.name())
                            .append(" has been spotted!\n");
                }
            } else if (drawn.name().equalsIgnoreCase("Sandtrout")) {
                shaiHuludSpotted = true;
                message.append("Sandtrout has been spotted, and all alliances have ended!\n");
                game.getFactions().forEach(Faction::removeAlly);
                game.setSandtroutInPlay(true);
            } else {
                message.append("Spice has been spotted in ");
                message.append(drawn.name());
                message.append("!\n");
            }
            if (saveWormForReshuffle) {
                wormsToReshuffle.add(drawn);
            } else if (!drawn.name().equalsIgnoreCase("Sandtrout")) {
                discard.add(drawn);
            }
        } while (drawn.name().equalsIgnoreCase("Shai-Hulud") ||
                drawn.name().equalsIgnoreCase("Great Maker") ||
                drawn.name().equalsIgnoreCase("Sandtrout"));

        while (!wormsToReshuffle.isEmpty()) {
            deck.add(wormsToReshuffle.pop());
            if (wormsToReshuffle.isEmpty()) {
                Collections.shuffle(deck);
            }
        }

        if (game.getStorm() == drawn.sector()) message.append(" (blown away by the storm!)\n");
        if (drawn.discoveryToken() == null) game.getTerritories().get(drawn.name()).addSpice(drawn.spice() * spiceMultiplier);
        else {
            game.getTerritory(drawn.name()).setSpice(6 * spiceMultiplier);
            if (!game.getTerritory(drawn.name()).getForces().isEmpty()) {
                message.append("all forces in the territory were killed in the spice blow!\n");
                for (Force force : game.getTerritory(drawn.name()).getForces()) {
                    if (force.getName().contains("*")) removeForces(drawn.name(), game.getFaction(force.getFactionName()), 0, force.getStrength(),  true, game, discordGame);
                    else removeForces(drawn.name(), game.getFaction(force.getFactionName()), force.getStrength(), 0, true, game, discordGame);
                }
            }
            message.append(drawn.discoveryToken()).append(" has been placed in ").append(drawn.tokenLocation()).append("\n");
            if (drawn.discoveryToken().equals("Hiereg")) game.getTerritory(drawn.tokenLocation()).setDiscoveryToken(game.getHieregTokens().remove(0));
            else game.getTerritory(drawn.tokenLocation()).setDiscoveryToken(game.getSmugglerTokens().remove(0));
            game.getTerritory(drawn.tokenLocation()).setDiscovered(false);
            if (game.hasFaction("Guild") && drawn.discoveryToken().equals("Smuggler")) discordGame.getFactionChat("Guild")
                    .queueMessage("The discovery token at " + drawn.tokenLocation() + " is a(n) " + game.getTerritory(drawn.tokenLocation()).getDiscoveryToken());
            if (game.hasFaction("Fremen") && drawn.discoveryToken().equals("Hiereg")) discordGame.getFactionChat("Fremen")
                    .queueMessage("The discovery token at " + drawn.tokenLocation() + " is a(n) " + game.getTerritory(drawn.tokenLocation()).getDiscoveryToken());
        }
        if (game.getStorm() == drawn.sector()) game.getTerritory(drawn.name()).setSpice(0);

        discordGame.pushGame();
        discordGame.getTurnSummary().queueMessage(message.toString());
        if (game.hasGameOption(GameOption.MAP_IN_FRONT_OF_SHIELD))
            game.setUpdated(UpdateType.MAP);
        else
            ShowCommands.showBoard(discordGame, game);
    }

    private void drawNexusCard(DiscordGame discordGame, Game game) throws ChannelNotFoundException, IOException {
        Faction faction = game.getFaction(discordGame.required(CommandOptions.faction).getAsString());
        if (faction.getNexusCard() != null) {
            game.getNexusDiscard().add(faction.getNexusCard());
        }
        faction.setNexusCard(game.getNexusDeck().pollFirst());
        showFactionInfo(discordGame);
        discordGame.pushGame();
    }

    private void discardNexusCard(DiscordGame discordGame, Game game) throws ChannelNotFoundException, IOException {
        Faction faction = game.getFaction(discordGame.required(CommandOptions.faction).getAsString());
        game.getNexusDiscard().add(faction.getNexusCard());
        faction.setNexusCard(null);
        showFactionInfo(discordGame);
        discordGame.pushGame();
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
        game.getLeaderTanks().add(targetFaction.removeLeader(discordGame.required(leader).getAsString()));
        discordGame.getFactionLedger(targetFaction).queueMessage(discordGame.required(leader).getAsString() + " was sent to the tanks.");
        discordGame.pushGame();
    }

    public void reviveLeader(DiscordGame discordGame, Game game) throws ChannelNotFoundException {
        Faction targetFaction = game.getFaction(discordGame.required(faction).getAsString());
        String leaderToRevive = discordGame.required(reviveLeader).getAsString();
        targetFaction.addLeader(
                game.removeLeaderFromTanks(leaderToRevive)
        );
        discordGame.getFactionLedger(targetFaction).queueMessage(leaderToRevive + " was revived from the tanks.");
        discordGame.pushGame();
    }

    public void revivalHandler(DiscordGame discordGame, Game game) throws ChannelNotFoundException {
        boolean star = discordGame.required(starred).getAsBoolean();
        Faction targetFaction = game.getFaction(discordGame.required(faction).getAsString());
        boolean isPaid = discordGame.required(paid).getAsBoolean();
        int revivedValue = discordGame.required(revived).getAsInt();
        revival(star, targetFaction, isPaid, revivedValue, game, discordGame);
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
        placeForces(targetTerritory, targetFaction, amountValue, starredAmountValue, isShipment, discordGame, game, false);
        discordGame.pushGame();
    }

    public void moveForcesEventHandler(DiscordGame discordGame, Game game) throws ChannelNotFoundException, InvalidOptionException, IOException {
        Faction targetFaction = game.getFaction(discordGame.required(faction).getAsString());
        Territory from = game.getTerritories().get(discordGame.required(fromTerritory).getAsString());
        Territory to = game.getTerritories().get(discordGame.required(toTerritory).getAsString());
        int amountValue = discordGame.required(amount).getAsInt();
        int starredAmountValue = discordGame.required(starredAmount).getAsInt();

        moveForces(targetFaction, from, to, amountValue, starredAmountValue, discordGame, game);
        if (!game.hasGameOption(GameOption.MAP_IN_FRONT_OF_SHIELD))
            ShowCommands.showBoard(discordGame, game);
        discordGame.pushGame();
    }

    public void removeForcesEventHandler(DiscordGame discordGame, Game game) throws ChannelNotFoundException {
        String territoryName = discordGame.required(fromTerritory).getAsString();
        Faction targetFaction = game.getFaction(discordGame.required(faction).getAsString());
        int amountValue = discordGame.required(amount).getAsInt();
        int specialAmount = discordGame.required(starredAmount).getAsInt();
        boolean isToTanks = discordGame.required(toTanks).getAsBoolean();

        removeForces(territoryName, targetFaction, amountValue, specialAmount, isToTanks, game, discordGame);
        discordGame.pushGame();
    }

    private void assassinateLeader(DiscordGame discordGame, Game game) throws ChannelNotFoundException {
        MoritaniFaction moritani = (MoritaniFaction) game.getFaction("Moritani");
        String assassinated = moritani.getTraitorHand().get(0).name();
        moritani.getAssassinationTargets().add(assassinated);
        moritani.getTraitorHand().clear();
        for (Faction faction : game.getFactions()) {
            if (faction.getLeaders().stream().anyMatch(leader1 -> leader1.name().equals(assassinated))) {
                game.getLeaderTanks().add(faction.removeLeader(assassinated));
                break;
            }
        }
        discordGame.getTurnSummary().queueMessage(Emojis.MORITANI + " have assassinated " + assassinated + "!");
        moritani.getTraitorHand().add(game.getTraitorDeck().pollFirst());
        discordGame.pushGame();
    }

    public void setStorm(DiscordGame discordGame, Game game) throws ChannelNotFoundException, IOException {
        int stormDialOne = discordGame.required(dialOne).getAsInt();
        int stormDialTwo = discordGame.required(dialTwo).getAsInt();
        game.advanceStorm(stormDialOne + stormDialTwo);
        discordGame.getTurnSummary().queueMessage("The storm has been initialized to sector " + game.getStorm() + " (" + stormDialOne + " + " + stormDialTwo + ")");
        if (game.hasTechTokens()) {
            List<TechToken> techTokens = new LinkedList<>();
            if (game.hasFaction("BT")) {
                game.getFaction("BT").getTechTokens().add(new TechToken("Axlotl Tanks"));
            } else techTokens.add(new TechToken("Axlotl Tanks"));
            if (game.hasFaction("Ix")) {
                game.getFaction("Ix").getTechTokens().add(new TechToken("Heighliners"));
            } else techTokens.add(new TechToken("Heighliners"));
            if (game.hasFaction("Fremen")) {
                game.getFaction("Fremen").getTechTokens().add(new TechToken("Spice Production"));
            } else techTokens.add(new TechToken("Spice Production"));
            if (!techTokens.isEmpty()) {
                Collections.shuffle(techTokens);
                for (int i = 0; i < techTokens.size(); i++) {
                    int firstFactionIndex = (Math.ceilDiv(game.getStorm(), 3) + i) % 6;
                    for (int j = 0; j < 6; j++) {
                        Faction faction = game.getFactions().get((firstFactionIndex + j) % 6);
                        if (faction.getTechTokens().isEmpty()) {
                            faction.getTechTokens().add(techTokens.get(i));
                            break;
                        }
                    }
                }
            }
        }
        discordGame.pushGame();
        ShowCommands.showBoard(discordGame, game);
        if (game.hasGameOption(GameOption.MAP_IN_FRONT_OF_SHIELD))
            game.setUpdated(UpdateType.MAP);
    }

    public void assignTechToken(DiscordGame discordGame, Game game) throws ChannelNotFoundException, IOException {
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
        if (game.hasGameOption(GameOption.MAP_IN_FRONT_OF_SHIELD))
            game.setUpdated(UpdateType.MAP);
        else
            ShowCommands.showBoard(discordGame, game);
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

    public void displayGameState(DiscordGame discordGame, Game game) throws ChannelNotFoundException, IOException {
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
        if (game.hasGameOption(GameOption.MAP_IN_FRONT_OF_SHIELD))
            game.setUpdated(UpdateType.MAP);
        else
            ShowCommands.showBoard(discordGame, game);
    }

    public void createAlliance(DiscordGame discordGame, Game game) throws ChannelNotFoundException {
        Faction factionOne = game.getFaction(discordGame.required(faction).getAsString());
        Faction factionTwo = game.getFaction(discordGame.required(otherFaction).getAsString());

        removeAlliance(game, factionOne);
        removeAlliance(game, factionTwo);

        factionOne.setAlly(factionTwo.getName());
        factionTwo.setAlly(factionOne.getName());

        String threadName = MessageFormat.format(
                "{0} {1} Alliance",
                factionOne.getName(),
                factionTwo.getName()
        );

        discordGame.createPrivateThread(discordGame.getTextChannel("chat"), threadName, Arrays.asList(
                factionOne.getPlayer(), factionTwo.getPlayer()
        ));

        if (game.hasGameOption(GameOption.MAP_IN_FRONT_OF_SHIELD))
            game.setUpdated(UpdateType.MAP);
        discordGame.pushGame();
    }

    public void removeAlliance(DiscordGame discordGame, Game game) throws ChannelNotFoundException {
        Faction targetFaction = game.getFaction(discordGame.required(faction).getAsString());
        removeAlliance(game, targetFaction);

        discordGame.pushGame();
    }

    private void removeAlliance(Game game, Faction faction) {
        if (faction.hasAlly()) {
            game.getFaction(faction.getAlly()).removeAlly();
        }
        faction.removeAlly();
        if (game.hasGameOption(GameOption.MAP_IN_FRONT_OF_SHIELD))
            game.setUpdated(UpdateType.MAP);
    }

    public void setSpiceInTerritory(DiscordGame discordGame, Game game) throws ChannelNotFoundException {
        String territoryName = discordGame.required(territory).getAsString();
        int amountValue = discordGame.required(amount).getAsInt();

        game.getTerritories().get(territoryName).setSpice(amountValue);
        if (game.hasGameOption(GameOption.MAP_IN_FRONT_OF_SHIELD))
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
            if (game.hasGameOption(GameOption.MAP_IN_FRONT_OF_SHIELD))
                game.setUpdated(UpdateType.MAP);
            discordGame.pushGame();
        }
    }

    public void weatherControlStorm(DiscordGame discordGame, Game game) throws ChannelNotFoundException {
        int wcStormMovement = discordGame.required(sectors).getAsInt();
        game.setStormMovement(wcStormMovement);

        discordGame.pushGame();
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
        List<Role> roles = event.getGuild().getRolesByName(game.getGameRole(), false);
        if (!roles.isEmpty()) {
            game.setGameRoleMention(roles.get(0).getAsMention());
        }
        discordGame.pushGame();
    }

    public void waitingList(SlashCommandInteractionEvent event) {
        String userTag = event.getUser().getId();
        TextChannel textChannel = event.getGuild().getTextChannelsByName("waiting-list", true).get(0);
        String message = "";
        message += "Speed: :scooter: ";
        if (event.getOption(slowGame.getName()).getAsBoolean()) message += ":white_check_mark: -- :blue_car: ";
        else message += ":no_entry_sign: -- :blue_car: ";
        if (event.getOption(midGame.getName()).getAsBoolean()) message += ":white_check_mark: -- :race_car: ";
        else message += ":no_entry_sign: -- :race_car: ";
        if (event.getOption(fastGame.getName()).getAsBoolean())
            message += ":white_check_mark:\nExpansions: <:bt:991763325576810546> <:ix:991763319406997514> ";
        else message += ":no_entry_sign:\nExpansions: <:bt:991763325576810546> <:ix:991763319406997514> ";
        if (event.getOption(ixianstleilaxuExpansion.getName()).getAsBoolean())
            message += ":white_check_mark: -- <:choam:991763324624703538> <:rich:991763318467465337> ";
        else message += ":no_entry_sign: -- <:choam:991763324624703538> <:rich:991763318467465337> ";
        if (event.getOption(choamricheseExpansion.getName()).getAsBoolean())
            message += ":white_check_mark: -- <:ecaz:1142126129105346590> <:moritani:1142126199775182879> ";
        else message += ":no_entry_sign: -- <:ecaz:1142126129105346590> <:moritani:1142126199775182879> ";
        if (event.getOption(ecazmoritaniExpansion.getName()).getAsBoolean())
            message += ":white_check_mark:\nOptions: <:weirding:991763071775297681> ";
        else message += ":no_entry_sign:\nOptions: <:weirding:991763071775297681> ";
        if (event.getOption(leaderSkills.getName()).getAsBoolean())
            message += ":white_check_mark: -- :european_castle: ";
        else message += ":no_entry_sign: -- :european_castle: ";
        if (event.getOption(strongholdCards.getName()).getAsBoolean()) message += ":white_check_mark:\nUser: ";
        else message += ":no_entry_sign:\nUser: ";
        message += "<@" + userTag + ">";
        textChannel.sendMessage(message).queue();
        // textChannel.sendMessage("Speed: :turtle: " + event.getOption(slowGame.getName()).getAsBoolean() + " :racehorse: " + event.getOption(midGame.getName()).getAsBoolean() + " :race_car: " + event.getOption(fastGame.getName()).getAsBoolean() + "\nExpansions: <:bt:991763325576810546> <:ix:991763319406997514>  " + event.getOption(ixianstleilaxuExpansion.getName()).getAsBoolean() + " <:choam:991763324624703538> <:rich:991763318467465337> " + event.getOption(choamricheseExpansion.getName()).getAsBoolean() + " :ecaz: :moritani: " + event.getOption(ecazmoritaniExpansion.getName()).getAsBoolean() + "\nOptions: Leader Skills " + event.getOption(leaderSkills.getName()).getAsBoolean() + " Stronghold Cards " + event.getOption(strongholdCards.getName()).getAsBoolean() + "\nUser: <@" + userTag + ">").queue();
    }
}
