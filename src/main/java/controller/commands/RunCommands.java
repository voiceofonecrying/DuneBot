package controller.commands;

import constants.Emojis;
import controller.buttons.ShipmentAndMovementButtons;
import controller.channels.FactionChat;
import controller.channels.TurnSummary;
import enums.GameOption;
import exceptions.ChannelNotFoundException;
import exceptions.InvalidGameStateException;
import model.*;
import model.factions.*;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.*;
import java.util.stream.Collectors;

public class RunCommands {
    public static List<CommandData> getCommands() {
        List<CommandData> commandData = new ArrayList<>();
        commandData.add(
                Commands.slash("run", "Commands related to playing through phases and turns.").addSubcommands(
                        new SubcommandData("advance", "Continue to the next phase of the game."),
                        new SubcommandData("bidding", "Run a regular bidding for a card"),
                        new SubcommandData("update-stronghold-skills", "Updates the Stronghold skill cards.")
                )
        );

        return commandData;
    }

    public static void runCommand(SlashCommandInteractionEvent event, DiscordGame discordGame, Game game) throws ChannelNotFoundException, InvalidGameStateException, IOException {
        String name = event.getSubcommandName();
        if (name == null) throw new IllegalArgumentException("Invalid command name: null");

        switch (name) {
            case "advance" -> advance(discordGame, game);
            case "bidding" -> bidding(discordGame, game);
            case "update-stronghold-skills" -> updateStrongholdSkillsCommand(discordGame, game);
        }
    }

    public static void advance(DiscordGame discordGame, Game game) throws ChannelNotFoundException, IOException, InvalidGameStateException {
        if (game.getTurn() == 0) {
            discordGame.queueMessage("mod-info", "Please complete setup first.");
            return;
        }
        int phase = game.getPhase();
        int subPhase = game.getSubPhase();

        if (phase == 1 && subPhase == 1) {
            startStormPhase(discordGame, game);
            game.advanceSubPhase();
        } else if (phase == 1 && subPhase == 2) {
            endStormPhase(discordGame, game);
            game.advancePhase();
        } else if (phase == 2) {
            spiceBlow(discordGame, game);
            game.advancePhase();
        } else if (phase == 3) {
            choamCharity(discordGame, game);
            game.advancePhase();
        } else if (phase == 4 && subPhase == 1) {
            if (startBiddingPhase(discordGame, game)) {
                game.advanceSubPhase();
                cardCountsInBiddingPhase(discordGame, game);
            }
            game.advanceSubPhase();
        } else if (phase == 4 && subPhase == 2) {
            cardCountsInBiddingPhase(discordGame, game);
            game.advanceSubPhase();
        } else if (phase == 4 && subPhase == 3) {
            if (finishBiddingPhase(discordGame, game))
                game.advancePhase();
        } else if (phase == 5 && subPhase == 1) {
            game.advanceSubPhase();
            if (!game.hasFaction("BT")) {
                startRevivalPhase(discordGame, game);
            } else btSetRevivalRates(discordGame, game);
        } else if (phase == 5 && subPhase == 2) {
            startRevivalPhase(discordGame, game);
            game.advancePhase();
        } else if (phase == 6) {
            startShipmentPhase(discordGame, game);
            game.advancePhase();
        } else if (phase == 7) {
            startBattlePhase(discordGame, game);
            game.advancePhase();
        } else if (phase == 8) {
            startSpiceHarvest(discordGame, game);
            game.advancePhase();
        } else if (phase == 9) {
            startMentatPause(discordGame, game);
            game.advanceTurn();
        }

        discordGame.pushGame();
    }

    public static void startStormPhase(DiscordGame discordGame, Game game) throws ChannelNotFoundException {
        TurnSummary turnSummary = discordGame.getTurnSummary();
        turnSummary.queueMessage("Turn " + game.getTurn() + " Storm Phase:");
        boolean atomicsEligible = false;
        boolean nobodyHoldsAtomics = true;
        for (Faction faction : game.getFactions()) {
            boolean isNearShieldWall = false;
            if (faction.isNearShieldWall()) {
                isNearShieldWall = true;
                atomicsEligible = true;
            }
            for (TreacheryCard card : faction.getTreacheryHand()) {
                if (card.name().trim().equalsIgnoreCase("Weather Control")) {
                    FactionChat chatChannel = new FactionChat(discordGame, faction.getName());
                    chatChannel.queueMessage(faction.getPlayer() + " will you play Weather Control?");
                } else if (card.name().trim().equalsIgnoreCase("Family Atomics")) {
                    nobodyHoldsAtomics = false;
                    if (isNearShieldWall) {
                        FactionChat chatChannel = new FactionChat(discordGame, faction.getName());
                        chatChannel.queueMessage(faction.getPlayer() + " will you play Family Atomics?");
                    }
                }
            }
        }
        if (atomicsEligible && nobodyHoldsAtomics) {
            boolean atomicsStillInGame = false;
            for (TreacheryCard card: game.getTreacheryDeck()) {
                if (card.name().trim().equalsIgnoreCase("Family Atomics")) {
                    atomicsStillInGame = true;
                    break;
                }
            }
            for (TreacheryCard card: game.getTreacheryDiscard()) {
                if (atomicsStillInGame) {
                    break;
                }
                if (card.name().trim().equalsIgnoreCase("Family Atomics")) {
                    atomicsStillInGame = true;
                    break;
                }
            }
            if (!atomicsStillInGame) {
                atomicsEligible = false;
            }
        }
        if (game.getTurn() != 1) {
            turnSummary.queueMessage(
                    "The storm would move " +
                    game.getStormMovement() +
                    " sectors this turn. Weather Control " +
                    (atomicsEligible ? "and Family Atomics " : "") +
                    "may be played at this time.");
            if (atomicsEligible && game.getStorm() >= 5 && game.getStorm() <= 9) {
                turnSummary.queueMessage("(Check if storm position prevents use of Family Atomics.)");
            }
        } else {
            discordGame.queueMessage("mod-info", "Run advance to complete turn 1 storm phase.");
        }
    }

    public static String stormTroops(Territory territory, Game game) {
        StringBuilder message = new StringBuilder();
        List<Force> fremenForces = territory.getForces().stream()
                .filter(f -> f.getFactionName().equalsIgnoreCase("Fremen"))
                .toList();

        List<Force> nonFremenForces = territory.getForces().stream()
                .filter(f -> !fremenForces.contains(f))
                .filter(force -> !(force.getName().equalsIgnoreCase("Hidden Mobile Stronghold")))
                .toList();

        if (fremenForces.size() > 0)
            message.append(stormTroopsFremen(territory, fremenForces, game));

        for (Force force : nonFremenForces) {
            message.append(stormRemoveTroops(territory, force, force.getStrength(), game));
        }

        return message.toString();
    }

    public static String stormTroopsFremen(Territory territory, List<Force> forces, Game game) {
        StringBuilder message = new StringBuilder();

        int totalTroops = forces.stream().mapToInt(Force::getStrength).sum();
        int totalLostTroops = Math.ceilDiv(totalTroops, 2);

        Force regularForce = territory.getForce("Fremen");
        Force fedaykin = territory.getForce("Fremen*");

        int lostRegularForces = Math.min(regularForce.getStrength(), totalLostTroops);
        totalLostTroops -= lostRegularForces;
        int lostFedaykin = Math.min(fedaykin.getStrength(), totalLostTroops);

        if (lostRegularForces > 0)
            message.append(stormRemoveTroops(territory, regularForce, lostRegularForces, game));

        if (lostFedaykin > 0)
            message.append(stormRemoveTroops(territory, fedaykin, lostFedaykin, game));

        return message.toString();
    }

    public static String stormRemoveTroops(Territory territory, Force force, int strength, Game game) {
        territory.setForceStrength(force.getName(), force.getStrength() - strength);
        game.addToTanks(force.getName(), strength);

        return MessageFormat.format(
                "{0} lose {1} {2} to the storm in {3}\n",
                game.getFaction(force.getFactionName()).getEmoji(),
                strength, Emojis.getForceEmoji(force.getName()),
                territory.getTerritoryName()
        );
    }

    public static String stormRemoveSpice(Territory territory) {
        String message = MessageFormat.format(
                "{0} {1} in {2} was blown away by the storm\n",
                territory.getSpice(), Emojis.SPICE, territory.getTerritoryName()
        );
        territory.setSpice(0);
        return message;
    }

    public static void endStormPhase(DiscordGame discordGame, Game game) throws ChannelNotFoundException, IOException {
        Map<String, Territory> territories = game.getTerritories();
        if (game.getTurn() != 1) {
            TurnSummary turnSummary = discordGame.getTurnSummary();
            turnSummary.queueMessage("The storm moves " + game.getStormMovement() + " sectors this turn.");

            StringBuilder message = new StringBuilder();
            for (int i = 0; i < game.getStormMovement(); i++) {
                game.advanceStorm(1);

                List<Territory> territoriesInStorm = territories.values().stream()
                        .filter(t ->
                                t.getSector() == game.getStorm() &&
                                !t.isRock()
                        ).toList();

                List<Territory> territoriesWithTroops = territoriesInStorm.stream()
                        .filter(t -> t.getForces().size() > 0).toList();

                List<Territory> territoriesWithSpice = territoriesInStorm.stream()
                        .filter(t -> t.getSpice() > 0).toList();

                for (Territory territory : territoriesWithTroops) {
                    message.append(stormTroops(territory, game));
                }

                for (Territory territory : territoriesWithSpice) {
                    message.append(stormRemoveSpice(territory));
                }
            }

            if (!message.isEmpty()) {
                turnSummary.queueMessage(message.toString());
            }
            ShowCommands.showBoard(discordGame, game);
        }

        game.setStormMovement(new Random().nextInt(6) + 1);
        if (game.hasFaction("Fremen")) {
            FactionChat chatChannel = new FactionChat(discordGame, "Fremen");
            chatChannel.queueMessage("The storm will move " + game.getStormMovement() + " sectors next turn.");
        }
    }

    public static void spiceBlow(DiscordGame discordGame, Game game) throws ChannelNotFoundException {
        discordGame.getTurnSummary().queueMessage("Turn " + game.getTurn() + " Spice Blow Phase:");
    }

    public static void choamCharity(DiscordGame discordGame, Game game) throws ChannelNotFoundException {
        TurnSummary turnSummary = discordGame.getTurnSummary();
        turnSummary.queueMessage("Turn " + game.getTurn() + " CHOAM Charity Phase:");
        int multiplier = 1;

        if (game.hasFaction("CHOAM")) {
            multiplier = ((ChoamFaction) game.getFaction("CHOAM")).getChoamMultiplier(game.getTurn());

            if (multiplier == 0) {
                turnSummary.queueMessage("CHOAM Charity is cancelled!");
                return;
            }
        }

        int choamGiven = 0;
        List<Faction> factions = game.getFactions();
        if (game.hasFaction("CHOAM")) {
            turnSummary.queueMessage(
                    game.getFaction("CHOAM").getEmoji() + " receives " +
                            game.getFactions().size() * 2 * multiplier +
                            " " + Emojis.SPICE + " in dividends from their many investments."
            );
        }
        for (Faction faction : factions) {
            if (faction.getName().equals("CHOAM")) continue;
            int spice = faction.getSpice();
            if (faction.getName().equals("BG")) {
                choamGiven += 2 * multiplier;
                faction.addSpice(2 * multiplier);
                turnSummary.queueMessage(faction.getEmoji() + " have received " +
                        2 * multiplier + " " + Emojis.SPICE + " in CHOAM Charity.");
                CommandManager.spiceMessage(discordGame, 2 * multiplier, faction.getName(), "CHOAM Charity", true);
            }
            else if (spice < 2) {
                int charity = multiplier * (2 - spice);
                choamGiven += charity;
                faction.addSpice(charity);
                turnSummary.queueMessage(
                        faction.getEmoji() + " have received " + charity + " " + Emojis.SPICE +
                                " in CHOAM Charity."
                );
                if (game.hasGameOption(GameOption.TECH_TOKENS) && !game.hasGameOption(GameOption.ALTERNATE_SPICE_PRODUCTION)) TechToken.addSpice(game, discordGame, "Spice Production");
                CommandManager.spiceMessage(discordGame, charity, faction.getName(), "CHOAM Charity", true);
            }
        }
        if (game.hasFaction("CHOAM")) {
            Faction choamFaction = game.getFaction("CHOAM");
            choamFaction.addSpice((2 * factions.size() * multiplier) - choamGiven);
            CommandManager.spiceMessage(discordGame, game.getFactions().size() * 2 * multiplier, "choam", "CHOAM Charity", true);
            turnSummary.queueMessage(
                    choamFaction.getEmoji() + " has paid " + choamGiven +
                            " " + Emojis.SPICE + " to factions in need."
            );
            CommandManager.spiceMessage(discordGame, choamGiven, "choam", "CHOAM Charity given", false);
        }
        if (game.hasGameOption(GameOption.TECH_TOKENS) && !game.hasGameOption(GameOption.ALTERNATE_SPICE_PRODUCTION)) TechToken.collectSpice(game, discordGame, "Spice Production");
    }

    public static boolean startBiddingPhase(DiscordGame discordGame, Game game) throws ChannelNotFoundException {
        discordGame.getTurnSummary().queueMessage("Turn " + game.getTurn() + " Bidding Phase:");
        game.startBidding();
        game.getFactions().forEach(faction -> {
            faction.setBid("");
            faction.setMaxBid(0);
        });
        RicheseFaction richeseFaction;
        try {
            richeseFaction = (RicheseFaction)game.getFaction("Richese");
            if (richeseFaction.getTreacheryHand().isEmpty()) {
                discordGame.queueMessage("mod-info", Emojis.RICHESE + " has no cards for black market. Automatically advancing to regular bidding.");
                return true;
            } else {
                RicheseCommands.askBlackMarket(discordGame, game);
                discordGame.queueMessage("mod-info", Emojis.RICHESE + " has been given buttons for black market.");
                return false;
            }
        } catch (IllegalArgumentException e) {
            return true;
        }
    }

    public static void cardCountsInBiddingPhase(DiscordGame discordGame, Game game) throws ChannelNotFoundException, InvalidGameStateException {
        Bidding bidding = game.getBidding();
        if (bidding.getBidCard() != null) {
            throw new InvalidGameStateException("The black market card must be awarded before advancing.");
        }
        StringBuilder message = new StringBuilder();
        message.append(MessageFormat.format(
                "{0}Number of Treachery Cards{0}\n",
                Emojis.TREACHERY
        ));
        message.append(
                game.getFactions().stream().map(
                        f -> MessageFormat.format(
                                "{0}: {1}\n", f.getEmoji(), f.getTreacheryHand().size()
                        )
                ).collect(Collectors.joining())
        );
        int numCardsForBid = bidding.populateMarket(game);
        message.append(
                MessageFormat.format(
                        "{0} cards will be pulled from the {1} deck for bidding.",
                        numCardsForBid, Emojis.TREACHERY
                )
        );
        if (game.hasFaction("Ix")) {
            message.append(
                    MessageFormat.format(
                            "\n{0} will send one of them back to the deck.",
                            Emojis.IX
                    )
            );
        }
        discordGame.getTurnSummary().queueMessage(message.toString());
        if (numCardsForBid == 0) {
            discordGame.queueMessage("mod-info", "All hands are full. If a player discards now, execute '/run bidding' again. Otherwise, '/run advance' to end bidding.");
        } else if (bidding.isRicheseCacheCardOutstanding()) {
            RicheseCommands.cacheCard(discordGame, game);
            discordGame.queueMessage("mod-info", Emojis.RICHESE + " has been given buttons for selling their cache card.");
        } else {
            discordGame.queueMessage("mod-info", "Start running commands to bid and then advance when all the bidding is done.");
        }
    }

    public static boolean finishBiddingPhase(DiscordGame discordGame, Game game) throws ChannelNotFoundException, InvalidGameStateException {
        Bidding bidding = game.getBidding();
        if (bidding.getBidCard() == null && !bidding.getMarket().isEmpty()) {
            throw new InvalidGameStateException("Use /run bidding to auction the next card.");
        } else if (bidding.getBidCard() == null && bidding.isRicheseCacheCardOutstanding()) {
            throw new InvalidGameStateException(Emojis.RICHESE + " cache card must be completed before ending bidding.");
        } else if (bidding.getBidCard() != null && !bidding.isCardFromMarket()) {
            throw new InvalidGameStateException("Card up for bid is not from bidding market.");
        }

        if (bidding.getBidCard() != null && bidding.isCardFromMarket()) {
            int numCardsReturned = bidding.moveMarketToDeck(game);
            discordGame.getTurnSummary().queueMessage("" + numCardsReturned + " cards were returned to top of the Treachery Deck");
        }

        if (bidding.isRicheseCacheCardOutstanding()) {
            discordGame.queueMessage("mod-info", "Auction the " + Emojis.RICHESE + " cache card. Then /run advance again to end bidding.");
            return false;
        }
        game.endBidding();
        discordGame.queueMessage("mod-info", "Bidding phase ended. Run advance to start revivals.");
        return true;
    }

    public static void bidding(DiscordGame discordGame, Game game) throws ChannelNotFoundException, InvalidGameStateException {
        Bidding bidding = game.getBidding();
        if (bidding.getBidCard() != null) {
            throw new InvalidGameStateException("There is already a card up for bid.");
        } else if (bidding.getNumCardsForBid() == 0) {
            throw new InvalidGameStateException("Use /run advance.");
        } else if (bidding.getBidCardNumber() != 0 && bidding.getBidCardNumber() == bidding.getNumCardsForBid()) {
            throw new InvalidGameStateException("All cards for this round have already been bid on.");
        } else if (bidding.isIxRejectOutstanding()) {
            throw new InvalidGameStateException(Emojis.IX + " must send a " + Emojis.TREACHERY + " card back to the deck.");
        }

        TurnSummary turnSummary = discordGame.getTurnSummary();
        if (!bidding.isMarketShownToIx() && game.hasFaction("Ix")) {
            StringBuilder message = new StringBuilder();
            message.append(
                    MessageFormat.format(
                            "{0} {1} cards have been shown to {2}",
                            bidding.getMarket().size(), Emojis.TREACHERY, Emojis.IX
                    )
            );
            IxCommands.cardToReject(discordGame, game);
            bidding.setMarketShownToIx(true);
            turnSummary.queueMessage(message.toString());
            discordGame.pushGame();
        } else  {
            bidding.updateBidOrder(game);
            List<String> bidOrder = bidding.getEligibleBidOrder(game);

            if (bidOrder.isEmpty()) {
                discordGame.queueMessage("bidding-phase", "All hands are full.");
                discordGame.queueMessage("mod-info", "All hands are full. If a player discards now, execute '/run bidding' again. Otherwise, '/run advance' to end bidding.");
            } else  {
                if (bidding.isTreacheryDeckReshuffled()) {
                    turnSummary.queueMessage(MessageFormat.format(
                            "There were only {0} left in the {1} deck. The {1} deck has been replenished from the discard pile.",
                            bidding.getNumCardsFromOldDeck(), Emojis.TREACHERY
                    ));
                }
                TreacheryCard bidCard = bidding.nextBidCard(game);
                AtreidesCommands.sendAtreidesCardPrescience(discordGame, game, bidCard);
                Faction factionBeforeFirstToBid = game.getFaction(bidOrder.get(bidOrder.size() - 1 ));
                bidding.setCurrentBidder(factionBeforeFirstToBid.getName());
                discordGame.queueMessage("bidding-phase",
                        MessageFormat.format("{0} You may now place your bids for R{1}:C{2}",
                                game.getGameRoleMention(), game.getTurn(), bidding.getBidCardNumber()
                        )
                );
                createBidMessage(discordGame, game);
                bidding.advanceBidder(game);
                discordGame.pushGame();
            }
        }
    }

    public static void createBidMessage(DiscordGame discordGame, Game game) throws ChannelNotFoundException, InvalidGameStateException {
        createBidMessage(discordGame, game, true);
    }

    public static boolean createBidMessage(DiscordGame discordGame, Game game, boolean tag) throws ChannelNotFoundException, InvalidGameStateException {
        Bidding bidding = game.getBidding();
        String nextBidderName = bidding.getNextBidder(game);
        List<String> bidOrder = bidding.getEligibleBidOrder(game);
        StringBuilder message = new StringBuilder();
        message.append(
                MessageFormat.format(
                        "R{0}:C{1}",
                        game.getTurn(), bidding.getBidCardNumber()
                )
        );
        if (bidding.isSilentAuction()) {
            message.append(" (Silent Auction)");
        } else if (bidding.isRicheseBidding()) {
            message.append(" (Once Around)");
        }
        message.append("\n");

        for (String factionName : bidOrder) {
            Faction f = game.getFaction(factionName);
            if (factionName.equals(nextBidderName) && tag) {
                if (f.getName().equals(bidding.getBidLeader())) {
                    discordGame.queueMessage("bidding-phase", message.toString());
                    discordGame.queueMessage("bidding-phase", f.getEmoji() + " has the top bid.");
                    return true;
                }
                message.append(f.getEmoji()).append(" - ").append(f.getPlayer()).append("\n");
            } else {
                message.append(f.getEmoji()).append(" - ").append(f.getBid()).append("\n");
            }
        }

        discordGame.queueMessage("bidding-phase", message.toString());
        return false;
    }

    public static void btSetRevivalRates(DiscordGame discordGame, Game game) throws ChannelNotFoundException {
        FactionChat chatChannel = new FactionChat(discordGame, "BT");
        chatChannel.queueMessage("Please set revival rates for each faction." + game.getFaction("BT").getPlayer());

        for (Faction faction : game.getFactions()) {
            if (faction.getName().equals("BT")) continue;
            List<Button> buttons = new LinkedList<>();
            buttons.add(Button.primary("bt-revival-rate-set-" + faction.getName() + "-3", "3"));
            buttons.add(Button.primary("bt-revival-rate-set-" + faction.getName() + "-4", "4"));
            buttons.add(Button.primary("bt-revival-rate-set-" + faction.getName() + "-5", "5"));
            chatChannel.queueMessage(faction.getEmoji(), buttons);
        }

    }

    public static void startRevivalPhase(DiscordGame discordGame, Game game) throws ChannelNotFoundException, IOException {
        TurnSummary turnSummary = discordGame.getTurnSummary();
        turnSummary.queueMessage("Turn " + game.getTurn() + " Revival Phase:");
        List<Faction> factions = game.getFactions();
        StringBuilder message = new StringBuilder();
        boolean nonBTRevival = false;
        int factionsWithRevivals = 0;

        for (Faction faction : factions) {
            int revived = 0;
            boolean revivedStar = false;
            int freeRevivals = faction.hasAlly() && faction.getAlly().equals("Fremen") ? 3 : faction.getFreeRevival();

            for (int i = freeRevivals; i > 0; i--) {
                if (game.getForceFromTanks(faction.getName()).getStrength() == 0
                        && game.getForceFromTanks(faction.getName() + "*").getStrength() == 0) continue;
                revived++;
                if (game.getForceFromTanks(faction.getName() + "*").getStrength() > 0 && !revivedStar) {
                    Force force = game.getForceFromTanks(faction.getName() + "*");
                    force.setStrength(force.getStrength() - 1);
                    revivedStar = true;
                    faction.addSpecialReserves(1);
                } else if (game.getForceFromTanks(faction.getName()).getStrength() > 0) {
                    Force force = game.getForceFromTanks(faction.getName());
                    force.setStrength(force.getStrength() - 1);
                    faction.addReserves(1);
                }
            }
            if (revived > 0) {
                factionsWithRevivals++;
                if (!faction.getName().equals("BT")) nonBTRevival = true;
                if (message.isEmpty()) message.append("Free Revivals:\n");
                message.append(game.getFaction(faction.getName()).getEmoji()).append(": ").append(revived).append("\n");
                if (game.getForceFromTanks(faction.getName()).getStrength() > 0 && revived < 3) {
                    List<Button> buttons = new LinkedList<>();
                    for (int i = 0; i <= faction.getMaxRevival() - revived; i++) {
                        Button button = Button.primary("revive-" + i, Integer.toString(i));
                        if ((!(faction.getName().equals("BT") || faction.getAlly().equals("BT")) && faction.getSpice() < i * 2) || faction.getSpice() < i) button = button.asDisabled();
                        buttons.add(button);
                    }

                    FactionChat chatChannel = new FactionChat(discordGame, faction.getName());
                    chatChannel.queueMessage(faction.getPlayer() + " Would you like to purchase additional revivals?", buttons);
                }
            }
        }

        if (factionsWithRevivals > 0 && game.hasFaction("BT")) {
            Faction btFaction = game.getFaction("BT");
            btFaction.addSpice(factionsWithRevivals);
            message.append(btFaction.getEmoji())
                    .append(" receives ")
                    .append(factionsWithRevivals)
                    .append(Emojis.SPICE)
                    .append(" from free revivals\n");
            CommandManager.spiceMessage(discordGame, factionsWithRevivals, "BT", "for free revivals", true);
        }

        if (!message.isEmpty()) {
            turnSummary.queueMessage(message.toString());
        }
        if (nonBTRevival && game.hasGameOption(GameOption.TECH_TOKENS)) TechToken.addSpice(game, discordGame, "Axlotl Tanks");

        if (game.hasFaction("Ecaz")) {
            EcazFaction ecaz = (EcazFaction) game.getFaction("Ecaz");
            ecaz.sendAmbassadorLocationMessage(game, discordGame, 1);
        }

        ShowCommands.showBoard(discordGame, game);
    }

    public static void startShipmentPhase(DiscordGame discordGame, Game game) throws ChannelNotFoundException, IOException {

        if (game.hasGameOption(GameOption.TECH_TOKENS)) TechToken.collectSpice(game, discordGame, "Axlotl Tanks");

        discordGame.getTurnSummary().queueMessage("Turn " + game.getTurn() + " Shipment and Movement Phase:");
        game.getTurnOrder().clear();
        for (Faction faction : game.getFactions()) {
            game.getTurnOrder().add(faction.getName());
            faction.getShipment().clear();
            faction.getMovement().clear();
            faction.getShipment().setShipped(false);
            faction.getMovement().setMoved(false);
        }
        while (game.getFactionTurnIndex(game.getTurnOrder().getFirst()) != 0) game.getTurnOrder().addFirst(game.getTurnOrder().pollLast());
        game.getTurnOrder().removeIf(name -> name.equals("Guild"));
        if (game.hasFaction("Richese") && !((RicheseFaction)game.getFaction("Richese")).getTreacheryCardCache().stream().anyMatch(treacheryCard -> treacheryCard.name().equals("Juice of Sapho")) &&
                    !game.getTreacheryDiscard().stream().anyMatch(treacheryCard -> treacheryCard.name().equals("Juice of Sapho"))) {
                game.getTurnOrder().addFirst("juice-of-sapho-hold");
                discordGame.prepareMessage("game-actions", "Juice of Sapho is in play. Use buttons to play Juice of Sapho to be " +
                        "considered first or last this shipment and movement phase.").addActionRow(Button.primary("juice-of-sapho-first", "Go first this phase."),
                        Button.primary("juice-of-sapho-last", "Go last this phase."), Button.secondary("juice-of-sapho-don't-play", "Don't play Juice of Sapho this phase.")).queue();
            }
        else if (game.hasFaction("Guild")) {
            game.getTurnOrder().addFirst("Guild");
            ShipmentAndMovementButtons.queueGuildTurnOrderButtons(discordGame, game);
        }
        else ShipmentAndMovementButtons.sendShipmentMessage(game.getTurnOrder().peekFirst(), discordGame, game);
        if (game.hasFaction("Atreides")) {
            SpiceCard nextCard = game.getSpiceDeck().peek();
            if (nextCard != null) {
                FactionChat chatChannel = new FactionChat(discordGame, "Atreides");
                chatChannel.queueMessage("You see visions of " + nextCard.name() + " in your future.");
            }
        }
        if(game.hasFaction("BG")) {
            StringBuilder message = new StringBuilder();
            for (Territory territory : game.getTerritories().values()) {
                if (territory.getForce("Advisor").getStrength() > 0) {
                    discordGame.queueMessage("game-actions", new MessageCreateBuilder().setContent(
                            message.append(game.getFaction("BG").getEmoji()).append(" to decide whether to flip their advisors in ").append(territory.getTerritoryName()).append("\n").append(game.getFaction("BG").getPlayer()).toString())
                            .addActionRow(Button.primary("bg-flip-" + territory.getTerritoryName(), "Flip"), Button.secondary("bg-dont-flip-" + territory.getTerritoryName(), "Don't flip")));
                }
            }
        }
        ShowCommands.showBoard(discordGame, game);
    }

    public static void startBattlePhase(DiscordGame discordGame, Game game) throws ChannelNotFoundException, IOException {
        if (game.hasGameOption(GameOption.TECH_TOKENS)) TechToken.collectSpice(game, discordGame, "Heighliners");
        TurnSummary turnSummary = discordGame.getTurnSummary();
        turnSummary.queueMessage("Turn " + game.getTurn() + " Battle Phase:");

        // Get list of territories with multiple factions
        List<Pair<Territory, List<Faction>>> battles = new ArrayList<>();
        int dukeVidalCount = 0;
        for (Territory territory : game.getTerritories().values()) {
            List<Force> forces = territory.getForces();
            Set<String> factionNames = forces.stream()
                    .filter(force -> !(force.getName().equalsIgnoreCase("Advisor")))
                    .filter(force -> !(force.getName().equalsIgnoreCase("Hidden Mobile Stronghold")))
                    .map(Force::getFactionName)
                    .collect(Collectors.toSet());

            if (game.hasFaction("Richese") && territory.hasRicheseNoField())
                factionNames.add("Richese");
            if (game.hasFaction("Moritani") && territory.isStronghold() && forces.size() > 1 && forces.stream().anyMatch(force -> force.getFactionName().equals("Moritani"))
            && forces.stream().noneMatch(force -> force.getFactionName().equals("Ecaz"))) dukeVidalCount++;

            List<Faction> factions = factionNames.stream()
                    .sorted(Comparator.comparingInt(game::getFactionTurnIndex))
                    .map(game::getFaction)
                    .toList();

            if (factions.size() > 1 && !territory.getTerritoryName().equalsIgnoreCase("Polar Sink")) {
                battles.add(new ImmutablePair<>(territory, factions));
            }
        }
        if (dukeVidalCount >= 2 && game.getLeaderTanks().stream().noneMatch(leader -> leader.name().equals("Duke Vidal"))) {
            for (Faction faction : game.getFactions()) {
                if (faction.getLeader("Duke Vidal").isEmpty()) continue;
                faction.removeLeader("Duke Vidal");
                if (faction.getName().equals("Ecaz")) {
                    FactionChat ecazChat = new FactionChat(discordGame, "Ecaz");
                    ecazChat.queueMessage("Duke Vidal has left to fight for the " + Emojis.MORITANI + "!");
                }
                if (faction.getName().equals("Harkonnen")) {
                    FactionChat harkonnenChat = new FactionChat(discordGame, "Harkonnen");
                    harkonnenChat.queueMessage("Duke Vidal has escaped to fight for the " + Emojis.MORITANI + "!");
                }
            }
            ((MoritaniFaction)game.getFaction("Moritani")).getDukeVidal();
            FactionChat moritaniChat = new FactionChat(discordGame, "Moritani");
            moritaniChat.queueMessage("Duke Vidal has come to fight for you!");
        }

        if(battles.size() > 0) {
            String battleMessages = battles.stream()
                    .sorted(Comparator
                            .comparingInt(o -> game.getFactionTurnIndex(o.getRight().get(0).getName()))
                    ).map((battle) ->
                            MessageFormat.format("{0} in {1}",
                                    battle.getRight().stream()
                                            .map(Faction::getEmoji)
                                            .collect(Collectors.joining(" vs ")),
                                    battle.getLeft().getTerritoryName()
                            )
                    ).collect(Collectors.joining("\n"));
            turnSummary.queueMessage("The following battles will take place this turn:\n" + battleMessages);
        } else {
            turnSummary.queueMessage("There are no battles this turn.");
        }
        ShowCommands.showBoard(discordGame, game);
    }

    public static void startSpiceHarvest(DiscordGame discordGame, Game game) throws ChannelNotFoundException, IOException {
        TurnSummary turnSummary = discordGame.getTurnSummary();
        turnSummary.queueMessage("Turn " + game.getTurn() + " Spice Harvest Phase:");
        Map<String, Territory> territories = game.getTerritories();
        for (Territory territory : territories.values()) {
            if (territory.getForces().size() != 1) continue;
            if (territory.countActiveFactions() == 0 && territory.hasForce("Advisor")) {
                BGFaction bg = (BGFaction) game.getFaction("BG");
                bg.flipForces(territory);
                turnSummary.queueMessage("Advisors are alone in " + territory.getTerritoryName() + " and have flipped to fighters.");
            }
        }

        for (Faction faction : game.getFactions()) {
            faction.setHasMiningEquipment(false);
            if (territories.get("Arrakeen").hasActiveFaction(faction)) {
                faction.addSpice(2);
                CommandManager.spiceMessage(discordGame, 2, faction.getName(), "for Arrakeen", true);
                turnSummary.queueMessage(faction.getEmoji() + " collects 2 " + Emojis.SPICE + " from Arrakeen");
                faction.setHasMiningEquipment(true);
            }
            if (territories.get("Carthag").hasActiveFaction(faction)) {
                faction.addSpice(2);
                CommandManager.spiceMessage(discordGame, 2, faction.getName(), "for Carthag", true);
                turnSummary.queueMessage(faction.getEmoji() + " collects 2 " + Emojis.SPICE + " from Carthag");
                faction.setHasMiningEquipment(true);
            }
            if (territories.get("Tuek's Sietch").hasActiveFaction(faction)) {
                turnSummary.queueMessage(faction.getEmoji() + " collects 1 " + Emojis.SPICE + " from Tuek's Sietch");
                faction.addSpice(1);
                CommandManager.spiceMessage(discordGame, 1, faction.getName(), "for Tuek's Sietch", true);
            }
        }

        boolean altSpiceProductionTriggered = false;
        for (Territory territory: territories.values()) {
            if (territory.getSpice() == 0 || territory.countActiveFactions() == 0) continue;
            Faction faction = territory.getActiveFactions(game).get(0);

            int spice = faction.getSpiceCollectedFromTerritory(territory);

            faction.addSpice(spice);
            territory.setSpice(territory.getSpice() - spice);

            CommandManager.spiceMessage(discordGame, spice, faction.getName(), "for Spice Blow", true);
            if (game.hasGameOption(GameOption.TECH_TOKENS) && game.hasGameOption(GameOption.ALTERNATE_SPICE_PRODUCTION)
                    && (!faction.getName().equals("Fremen") || game.hasGameOption(GameOption.FREMEN_TRIGGER_ALTERNATE_SPICE_PRODUCTION)))
                altSpiceProductionTriggered = true;
            turnSummary.queueMessage(game.getFaction(faction.getName()).getEmoji() +
                    " collects " + spice + " " + Emojis.SPICE + " from " + territory.getTerritoryName());
        }

        if (altSpiceProductionTriggered) {
            TechToken.addSpice(game, discordGame, "Spice Production");
            TechToken.collectSpice(game, discordGame, "Spice Production");
        }

        ShowCommands.showBoard(discordGame, game);
    }

    public static void startMentatPause(DiscordGame discordGame, Game game) throws ChannelNotFoundException {
        TurnSummary turnSummary = discordGame.getTurnSummary();
        turnSummary.queueMessage("Turn " + game.getTurn() + " Mentat Pause Phase:");
        for (Faction faction : game.getFactions()) {
            if (faction.getFrontOfShieldSpice() > 0) {
                turnSummary.queueMessage(faction.getEmoji() + " collects " +
                        faction.getFrontOfShieldSpice() + " " + Emojis.SPICE + " from front of shield.");
                CommandManager.spiceMessage(discordGame,  faction.getFrontOfShieldSpice(), faction.getName(), "front of shield", true);
                faction.addSpice(faction.getFrontOfShieldSpice());
                faction.setFrontOfShieldSpice(0);
            }
            for (TreacheryCard card : faction.getTreacheryHand()) {
                if (card.name().trim().equalsIgnoreCase("Weather Control")) {
                    discordGame.queueMessage("mod-info", faction.getEmoji() + " has Weather Control.");
                } else if (card.name().trim().equalsIgnoreCase("Family Atomics")) {
                    discordGame.queueMessage("mod-info", faction.getEmoji() + " has Family Atomics.");
                }
            }
        }
        if (game.hasFaction("Moritani")) {
            MoritaniFaction moritani = (MoritaniFaction) game.getFaction("Moritani");
            moritani.sendTerrorTokenLocationMessage(game, discordGame);
        }

        updateStrongholdSkills(discordGame, game);
    }

    public static void updateStrongholdSkillsCommand(DiscordGame discordGame, Game game) throws ChannelNotFoundException {
        updateStrongholdSkills(discordGame, game);
        discordGame.pushGame();
    }

    public static void updateStrongholdSkills(DiscordGame discordGame, Game game) throws ChannelNotFoundException {
        if (game.hasStrongholdSkills()) {
            game.getFactions().forEach(Faction::removeAllStrongholdCards);

            List<Territory> strongholds = game.getTerritories().values().stream()
                    .filter(Territory::isStronghold)
                    .filter(t -> t.countActiveFactions() == 1)
                    .toList();

            for (Territory stronghold : strongholds) {
                Faction faction = stronghold.getActiveFactions(game).get(0);
                faction.addStrongholdCard(new StrongholdCard(stronghold.getTerritoryName()));
                discordGame.getTurnSummary().queueMessage(MessageFormat.format("{0} controls {1}{2}{1}",
                        stronghold.getActiveFactions(game).get(0).getEmoji(), Emojis.WORM,
                        stronghold.getTerritoryName()));
            }
        }
    }
}
