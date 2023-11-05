package controller.commands;

import constants.Emojis;
import controller.DiscordGame;
import controller.buttons.ShipmentAndMovementButtons;
import controller.channels.FactionChat;
import controller.channels.TurnSummary;
import enums.GameOption;
import enums.UpdateType;
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
        game.removeGameOption(GameOption.SUMMARY_THREAD_PER_TURN);
        if (game.getTurn() == 0) {
            discordGame.getModInfo().queueMessage("Please complete setup first.");
            return;
        }

        if (game.getPhase() == 10) {
            game.advanceTurn();
            game.setTurnSummary(discordGame.getTurnSummary());
        }

        int phase = game.getPhase();
        int subPhase = game.getSubPhase();

        sendQuote(discordGame, game, phase);

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
            if (game.hasFaction("BT")) {
                btSetRevivalRates(discordGame, game);
            }
            game.advanceSubPhase();
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
            game.advancePhase();
        }

        discordGame.pushGame();
    }

    private static void sendQuote(DiscordGame discordGame, Game game, int phase) throws IOException, ChannelNotFoundException {
        if (game.getSubPhase() != 1) return;
        if (game.getQuotes().get(phase).isEmpty()) return;
        Collections.shuffle(game.getQuotes().get(phase));
        discordGame.getTurnSummary().queueMessage(game.getQuotes().get(phase).remove(0));
    }

    public static void startStormPhase(DiscordGame discordGame, Game game) throws ChannelNotFoundException {
        game.startStormPhase();

        if (game.getTerritories().get("Ecological Testing Station") != null && game.getTerritory("Ecological Testing Station").countActiveFactions() == 1) {
            Faction faction = game.getTerritory("Ecological Testing Station").getActiveFactions(game).get(0);
            discordGame.getFactionChat(faction.getName()).queueMessage("What have the ecologists at the testing station discovered about the storm movement?",
                    List.of(Button.primary("storm-1", "-1"), Button.secondary("storm0", "0"), Button.primary("storm1", "+1")));
        }
        if (game.getTurn() == 1) {
            discordGame.getModInfo().queueMessage("Run advance to complete turn 1 storm phase.");
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

        if (!fremenForces.isEmpty())
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
                        .filter(t -> !t.getForces().isEmpty()).toList();

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
            if (game.hasGameOption(GameOption.MAP_IN_FRONT_OF_SHIELD))
                game.setUpdated(UpdateType.MAP);
            ShowCommands.showBoard(discordGame, game);
        }

        game.setStormMovement(new Random().nextInt(6) + 1);
        if (game.hasFaction("Fremen")) {
            discordGame.getFremenChat().queueMessage("The storm will move " + game.getStormMovement() + " sectors next turn.");
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
            int plusOne = (game.hasGameOption(GameOption.HOMEWORLDS) && !game.getFaction("CHOAM").isHighThreshold()) ? 1 : 0;
            turnSummary.queueMessage(
                    game.getFaction("CHOAM").getEmoji() + " receives " +
                            ((game.getFactions().size() * 2 * multiplier) + plusOne) +
                            " " + Emojis.SPICE + " in dividends from their many investments."
            );
        }
        for (Faction faction : factions) {
            if (faction.getName().equals("CHOAM")) continue;
            int spice = faction.getSpice();
            if (faction.getName().equals("BG")) {
                int charity = multiplier * 2;
                choamGiven += charity;
                if (game.hasGameOption(GameOption.HOMEWORLDS) && !faction.isHighThreshold()) charity++;
                faction.addSpice(charity);
                turnSummary.queueMessage(faction.getEmoji() + " have received " +
                        2 * multiplier + " " + Emojis.SPICE + " in CHOAM Charity.");
                faction.spiceMessage(2 * multiplier, "CHOAM Charity", true);
            } else if (spice < 2) {
                int charity = multiplier * (2 - spice);
                choamGiven += charity;
                if (game.hasGameOption(GameOption.HOMEWORLDS) && !faction.isHighThreshold()) charity++;
                faction.addSpice(charity);
                turnSummary.queueMessage(
                        faction.getEmoji() + " have received " + charity + " " + Emojis.SPICE +
                                " in CHOAM Charity."
                );
                if (game.hasGameOption(GameOption.TECH_TOKENS) && !game.hasGameOption(GameOption.ALTERNATE_SPICE_PRODUCTION))
                    TechToken.addSpice(game, TechToken.SPICE_PRODUCTION);
                faction.spiceMessage(charity, "CHOAM Charity", true);
            }
        }
        if (game.hasFaction("CHOAM")) {
            Faction choamFaction = game.getFaction("CHOAM");
            int plusOne = (game.hasGameOption(GameOption.HOMEWORLDS) && !choamFaction.isHighThreshold()) ? 1 : 0;
            choamFaction.addSpice(((2 * factions.size() * multiplier) + plusOne) - choamGiven);
            choamFaction.spiceMessage(game.getFactions().size() * 2 * multiplier, "CHOAM Charity", true);
            turnSummary.queueMessage(
                    choamFaction.getEmoji() + " has paid " + choamGiven +
                            " " + Emojis.SPICE + " to factions in need."
            );
            choamFaction.spiceMessage(choamGiven, "CHOAM Charity given", false);
        }
        if (game.hasGameOption(GameOption.TECH_TOKENS) && !game.hasGameOption(GameOption.ALTERNATE_SPICE_PRODUCTION))
            TechToken.collectSpice(game, TechToken.SPICE_PRODUCTION);
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
            richeseFaction = (RicheseFaction) game.getFaction("Richese");
            if (richeseFaction.getTreacheryHand().isEmpty()) {
                discordGame.getModInfo().queueMessage(Emojis.RICHESE + " has no cards for black market. Automatically advancing to regular bidding.");
                return true;
            } else {
                RicheseCommands.askBlackMarket(discordGame, game);
                discordGame.getModInfo().queueMessage(Emojis.RICHESE + " has been given buttons for black market.");
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
            discordGame.getModInfo().queueMessage("All hands are full. If a player discards now, execute '/run bidding' again. Otherwise, '/run advance' to end bidding.");
        } else if (bidding.isRicheseCacheCardOutstanding()) {
            RicheseCommands.cacheCard(discordGame, game);
            discordGame.getModInfo().queueMessage(Emojis.RICHESE + " has been given buttons for selling their cache card.");
        } else {
            discordGame.getModInfo().queueMessage("Start running commands to bid and then advance when all the bidding is done.");
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
            discordGame.getTurnSummary().queueMessage(numCardsReturned + " cards were returned to top of the Treachery Deck");
        }

        if (bidding.isRicheseCacheCardOutstanding()) {
            discordGame.getModInfo().queueMessage("Auction the " + Emojis.RICHESE + " cache card. Then /run advance again to end bidding.");
            return false;
        }

        if (game.hasFaction("Emperor") && game.hasGameOption(GameOption.HOMEWORLDS) && game.getFaction("Emperor").isHighThreshold()) {
            Faction emperor = game.getFaction("Emperor");
            List<Button> buttons = new LinkedList<>();
            for (TreacheryCard card : emperor.getTreacheryHand()) {
                buttons.add(Button.primary("emperor-discard-" + card.name(), card.name()));
            }
            buttons.add(Button.secondary("emperor-finished-discarding", "Done"));
            discordGame.getEmperorChat().queueMessage("Use these buttons to discard " + Emojis.TREACHERY + " from hand at the cost of 2 " + Emojis.SPICE + " per card.", buttons);
        }
        game.endBidding();
        discordGame.getModInfo().queueMessage("Bidding phase ended. Run advance to start revivals.");
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
            String message = MessageFormat.format(
                    "{0} {1} cards have been shown to {2}",
                    bidding.getMarket().size(), Emojis.TREACHERY, Emojis.IX
            );
            IxCommands.cardToReject(discordGame, game);
            bidding.setMarketShownToIx(true);
            turnSummary.queueMessage(message);
            discordGame.pushGame();
        } else {
            bidding.updateBidOrder(game);
            List<String> bidOrder = bidding.getEligibleBidOrder(game);

            if (bidOrder.isEmpty()) {
                discordGame.queueMessage("bidding-phase", "All hands are full.");
                discordGame.getModInfo().queueMessage("All hands are full. If a player discards now, execute '/run bidding' again. Otherwise, '/run advance' to end bidding.");
            } else {
                if (bidding.isTreacheryDeckReshuffled()) {
                    turnSummary.queueMessage(MessageFormat.format(
                            "There were only {0} left in the {1} deck. The {1} deck has been replenished from the discard pile.",
                            bidding.getNumCardsFromOldDeck(), Emojis.TREACHERY
                    ));
                }
                TreacheryCard bidCard = bidding.nextBidCard(game);
                AtreidesCommands.sendAtreidesCardPrescience(discordGame, game, bidCard);
                Faction factionBeforeFirstToBid = game.getFaction(bidOrder.get(bidOrder.size() - 1));
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
        FactionChat btChat = discordGame.getBTChat();
        btChat.queueMessage("Please set revival rates for each faction." + game.getFaction("BT").getPlayer());

        for (Faction faction : game.getFactions()) {
            if (faction.getName().equals("BT")) continue;
            List<Button> buttons = new LinkedList<>();
            buttons.add(Button.primary("bt-revival-rate-set-" + faction.getName() + "-3", "3"));
            buttons.add(Button.primary("bt-revival-rate-set-" + faction.getName() + "-4", "4"));
            buttons.add(Button.primary("bt-revival-rate-set-" + faction.getName() + "-5", "5"));
            btChat.queueMessage(faction.getEmoji(), buttons);
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
            if (game.hasGameOption(GameOption.HOMEWORLDS) && !faction.isHighThreshold()) freeRevivals++;

            for (int i = freeRevivals; i > 0; i--) {
                if (game.getForceFromTanks(faction.getName()).getStrength() == 0
                        && game.getForceFromTanks(faction.getName() + "*").getStrength() == 0) continue;
                revived++;
                if (game.getForceFromTanks(faction.getName() + "*").getStrength() > 0 && !revivedStar) {
                    if (faction.getName().equals("Emperor") && game.hasGameOption(GameOption.HOMEWORLDS) && !((EmperorFaction)faction).isSecundusHighThreshold()) {
                        revived--;
                        i++;
                        revivedStar = true;
                        continue;
                    }
                    if (faction.getName().equals("Fremen") && faction.isHighThreshold()) {
                        List<Button> buttons = new LinkedList<>();
                        for (Territory territory : game.getTerritories().values()) {
                            if (!territory.getActiveFactionNames().contains("Fremen")) continue;
                            buttons.add(Button.primary("fremen-ht-" + territory.getTerritoryName(), territory.getTerritoryName()));
                        }
                        buttons.add(Button.danger("fremen-cancel", "Don't use HT advantage"));
                        discordGame.getFremenChat().queueMessage("You are at high threshold, where would you like to place your revived " + Emojis.FREMEN_FEDAYKIN + "?", buttons);
                    }
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
                else if (game.hasFaction("BT") && game.hasGameOption(GameOption.HOMEWORLDS) && game.getFaction("BT").isHighThreshold()) {
                    discordGame.getBTChat().queueMessage("You are at high threshold, you may place your revived " + Emojis.BT_TROOP + " anywhere on Arrakis or on any homeworld. " + game.getFaction("BT").getPlayer());
                }
                if (message.isEmpty()) message.append("Free Revivals:\n");
                message.append(game.getFaction(faction.getName()).getEmoji()).append(": ").append(revived).append("\n");
                if (game.getForceFromTanks(faction.getName()).getStrength() > 0 && revived < 3) {
                    List<Button> buttons = new LinkedList<>();
                    for (int i = 0; i <= faction.getMaxRevival() - revived; i++) {
                        Button button = Button.primary("revive-" + i, Integer.toString(i));
                        if ((!(faction.getName().equals("BT") || faction.getAlly().equals("BT")) && faction.getSpice() < i * 2) || faction.getSpice() < i)
                            button = button.asDisabled();
                        buttons.add(button);
                    }

                    discordGame.getFactionChat(faction.getName()).queueMessage(faction.getPlayer() + " Would you like to purchase additional revivals?", buttons);
                }
            }

        }

        if (factionsWithRevivals > 0 && game.hasFaction("BT") && game.getFaction("BT").isHighThreshold()) {
            Faction btFaction = game.getFaction("BT");
            btFaction.addSpice(factionsWithRevivals);
            message.append(btFaction.getEmoji())
                    .append(" receives ")
                    .append(factionsWithRevivals)
                    .append(Emojis.SPICE)
                    .append(" from free revivals\n");
            btFaction.spiceMessage(factionsWithRevivals, "for free revivals", true);
        }

        flipToHighThresholdIfApplicable(discordGame, game);

        if (!message.isEmpty()) {
            turnSummary.queueMessage(message.toString());
        }
        if (nonBTRevival && game.hasGameOption(GameOption.TECH_TOKENS))
            TechToken.addSpice(game, TechToken.AXLOTL_TANKS);

        if (game.hasFaction("Ecaz")) {
            EcazFaction ecaz = (EcazFaction) game.getFaction("Ecaz");
            ecaz.sendAmbassadorLocationMessage(game, discordGame, 1);
        }

        if (factionsWithRevivals > 0 && game.hasGameOption(GameOption.MAP_IN_FRONT_OF_SHIELD)) {
            game.setUpdated(UpdateType.MAP);
        }
        if (game.hasGameOption(GameOption.MAP_IN_FRONT_OF_SHIELD))
            game.setUpdated(UpdateType.MAP);
        else
            ShowCommands.showBoard(discordGame, game);
    }

    public static void flipToHighThresholdIfApplicable(DiscordGame discordGame, Game game) throws ChannelNotFoundException {
        if (!game.hasGameOption(GameOption.HOMEWORLDS)) return;
        for (Faction faction : game.getFactions()) {
            if (faction.getName().equals("Emperor")) {
                EmperorFaction emperor = (EmperorFaction) faction;
                if (!emperor.isHighThreshold() && emperor.getReserves().getStrength() > emperor.getLowThreshold()) {
                    discordGame.getTurnSummary().queueMessage(faction.getHomeworld() + " has flipped to High Threshold");
                    emperor.setHighThreshold(true);
                }
                if (!emperor.isSecundusHighThreshold() && emperor.getSpecialReserves().getStrength() > emperor.getSecundusLowThreshold()) {
                    discordGame.getTurnSummary().queueMessage(emperor.getSecondHomeworld() + " has flipped to High Threshold");
                    emperor.setSecundusHighThreshold(true);
                }
            } else if (!faction.isHighThreshold() && faction.getReserves().getStrength() + faction.getSpecialReserves().getStrength() > faction.getLowThreshold()) {
                discordGame.getTurnSummary().queueMessage(faction.getHomeworld() + " has flipped to High Threshold");
                faction.setHighThreshold(true);
            }
        }
    }

    public static void startShipmentPhase(DiscordGame discordGame, Game game) throws ChannelNotFoundException, IOException {

        if (game.hasGameOption(GameOption.TECH_TOKENS)) TechToken.collectSpice(game, TechToken.AXLOTL_TANKS);

        discordGame.getTurnSummary().queueMessage("Turn " + game.getTurn() + " Shipment and Movement Phase:");
        game.getTurnOrder().clear();
        for (Faction faction : game.getFactions()) {
            game.getTurnOrder().add(faction.getName());
            faction.getShipment().clear();
            faction.getMovement().clear();
            faction.getShipment().setShipped(false);
            faction.getMovement().setMoved(false);
            faction.setAllySpiceShipment(0);
        }
        while (game.getFactionTurnIndex(game.getTurnOrder().getFirst()) != 0)
            game.getTurnOrder().addFirst(game.getTurnOrder().pollLast());
        game.getTurnOrder().removeIf(name -> name.equals("Guild"));
        if (game.hasFaction("Richese") && ((RicheseFaction) game.getFaction("Richese")).getTreacheryCardCache().stream().noneMatch(treacheryCard -> treacheryCard.name().equals("Juice of Sapho")) &&
                game.getTreacheryDiscard().stream().noneMatch(treacheryCard -> treacheryCard.name().equals("Juice of Sapho"))) {
            game.getTurnOrder().addFirst("juice-of-sapho-hold");
            discordGame.prepareMessage("game-actions", "Juice of Sapho is in play. Use buttons to play Juice of Sapho to be " +
                    "considered first or last this shipment and movement phase.").addActionRow(Button.primary("juice-of-sapho-first", "Go first this phase."),
                    Button.primary("juice-of-sapho-last", "Go last this phase."), Button.secondary("juice-of-sapho-don't-play", "Don't play Juice of Sapho this phase.")).queue();
        } else if (game.hasFaction("Guild")) {
            game.getTurnOrder().addFirst("Guild");
            ShipmentAndMovementButtons.queueGuildTurnOrderButtons(discordGame, game);
        } else ShipmentAndMovementButtons.sendShipmentMessage(game.getTurnOrder().peekFirst(), discordGame, game);
        if (game.hasFaction("Atreides") && game.getFaction("Atreides").isHighThreshold()) {
            SpiceCard nextCard = game.getSpiceDeck().peek();
            if (nextCard != null) {
                discordGame.getAtreidesChat().queueMessage("You see visions of " + nextCard.name() + " in your future.");
            }
        }
        if (game.hasFaction("BG")) {
            String bgPlayer = game.getFaction("BG").getPlayer();
            for (Territory territory : game.getTerritories().values()) {
                if (territory.getTerritoryName().equals("Polar Sink")) continue;
                StringBuilder message = new StringBuilder();
                if (territory.getForce("Advisor").getStrength() > 0) {
                    if (territory.getSector() == game.getStorm()) {
                        discordGame.queueMessage("game-actions", territory.getTerritoryName() + " is under the storm. Ask the mod to flip for you if the game allows it. " + bgPlayer);
                        continue;
                    }
                    discordGame.queueMessage("game-actions", new MessageCreateBuilder().setContent(
                                    message.append(game.getFaction("BG").getEmoji()).append(" to decide whether to flip their advisors in ").append(territory.getTerritoryName()).append("\n").append(bgPlayer).toString())
                            .addActionRow(Button.primary("bg-flip-" + territory.getTerritoryName(), "Flip"), Button.secondary("bg-dont-flip-" + territory.getTerritoryName(), "Don't flip")));
                }
            }
        }
        ShowCommands.showBoard(discordGame, game);
    }

    public static void startBattlePhase(DiscordGame discordGame, Game game) throws ChannelNotFoundException, IOException {
        if (game.hasGameOption(GameOption.TECH_TOKENS)) TechToken.collectSpice(game, TechToken.HEIGHLINERS);
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
        if (dukeVidalCount >= 2 && game.getLeaderTanks().stream().noneMatch(leader -> leader.name().equals("Duke Vidal")) && !(game.hasFaction("Ecaz") && game.getFaction("Ecaz").isHomeworldOccupied())) {
            for (Faction faction : game.getFactions()) {
                if (faction.getLeader("Duke Vidal").isEmpty()) continue;
                faction.removeLeader("Duke Vidal");
                if (faction.getName().equals("Ecaz")) {
                    discordGame.getEcazChat().queueMessage("Duke Vidal has left to fight for the " + Emojis.MORITANI + "!");
                }
                if (faction.getName().equals("Harkonnen")) {
                    discordGame.getHarkonnenChat().queueMessage("Duke Vidal has escaped to fight for the " + Emojis.MORITANI + "!");
                }
            }
            ((MoritaniFaction) game.getFaction("Moritani")).getDukeVidal();
            discordGame.getMoritaniChat().queueMessage("Duke Vidal has come to fight for you!");
        }

        if (!battles.isEmpty()) {
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
        if (game.hasFaction("Moritani") && game.getFaction("Moritani").getLeaders().removeIf(leader -> leader.name().equals("Duke Vidal"))) turnSummary.queueMessage("Duke Vidal has left the " + Emojis.MORITANI + " services... for now.");
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
                faction.spiceMessage(2, "for Arrakeen", true);
                turnSummary.queueMessage(faction.getEmoji() + " collects 2 " + Emojis.SPICE + " from Arrakeen");
                faction.setHasMiningEquipment(true);
            }
            if (territories.get("Carthag").hasActiveFaction(faction)) {
                faction.addSpice(2);
                faction.spiceMessage(2, "for Carthag", true);
                turnSummary.queueMessage(faction.getEmoji() + " collects 2 " + Emojis.SPICE + " from Carthag");
                faction.setHasMiningEquipment(true);
            }
            if (territories.get("Tuek's Sietch").hasActiveFaction(faction)) {
                turnSummary.queueMessage(faction.getEmoji() + " collects 1 " + Emojis.SPICE + " from Tuek's Sietch");
                faction.addSpice(1);
                faction.spiceMessage(1, "for Tuek's Sietch", true);
            }
            if (territories.get("Cistern") != null && territories.get("Cistern").hasActiveFaction(faction)) {
                faction.addSpice(2);
                faction.spiceMessage(2, "for Cistern", true);
                turnSummary.queueMessage(faction.getEmoji() + " collects 2 " + Emojis.SPICE + " from Cistern");
                faction.setHasMiningEquipment(true);
            }

            Territory homeworld = game.getTerritory(faction.getHomeworld());
            if (homeworld.getForces().stream().anyMatch(force -> !force.getFactionName().equals(faction.getName()))) {
                Faction occupyingFaction = homeworld.getActiveFactions(game).get(0);
                if (game.hasGameOption(GameOption.HOMEWORLDS) && occupyingFaction.getName().equals("Harkonnen") && occupyingFaction.isHighThreshold() && !((HarkonnenFaction)occupyingFaction).hasTriggeredHT()) {
                    faction.addSpice(2);
                    faction.spiceMessage(2, "for High Threshold advantage", true);
                    ((HarkonnenFaction)faction).setTriggeredHT(true);
                }
                turnSummary.queueMessage(occupyingFaction.getEmoji() + " collects " + faction.getOccupiedIncome() + " from " + faction.getHomeworld());
                occupyingFaction.addSpice(faction.getOccupiedIncome());
            }
        }

        boolean altSpiceProductionTriggered = false;
        boolean orgizActive = territories.get("Orgiz Processing Station") != null && territories.get("Orgiz Processing Station").getActiveFactions(game).size() == 1;
        Territory orgiz = territories.get("Orgiz Processing Station");
        for (Territory territory : territories.values()) {
            if (territory.getSpice() == 0 || territory.countActiveFactions() == 0) continue;
            Faction faction = territory.getActiveFactions(game).get(0);

            int spice = faction.getSpiceCollectedFromTerritory(territory);
            if (faction.getName().equals("Fremen") && faction.isHomeworldOccupied()) {
                faction.getOccupier().addSpice(Math.floorDiv(spice, 2));
                faction.getOccupier().spiceMessage(Math.floorDiv(spice, 2),
                        "From " + Emojis.FREMEN + " " + Emojis.SPICE + " collection (occupied advantage).", true);
                turnSummary.queueMessage(game.getFaction(faction.getName()).getEmoji() +
                        " collects " + Math.floorDiv(spice, 2) + " " + Emojis.SPICE + " from " + Emojis.FREMEN + " collection at " + territory.getTerritoryName());
                spice = Math.ceilDiv(spice, 2);
            }
            faction.addSpice(spice);
            faction.spiceMessage(spice, "for Spice Blow", true);
            if (orgizActive) {
                faction.subtractSpice(1);
                faction.spiceMessage(1, "for Orgiz Processing Station", false);
                orgiz.getActiveFactions(game).get(0).addSpice(1);
                orgiz.getActiveFactions(game).get(0).spiceMessage(1, "for Orgiz Processing Station", true);
                spice--;
            }
            territory.setSpice(territory.getSpice() - spice);

            if (game.hasGameOption(GameOption.TECH_TOKENS) && game.hasGameOption(GameOption.ALTERNATE_SPICE_PRODUCTION)
                    && (!faction.getName().equals("Fremen") || game.hasGameOption(GameOption.FREMEN_TRIGGER_ALTERNATE_SPICE_PRODUCTION)))
                altSpiceProductionTriggered = true;
            turnSummary.queueMessage(game.getFaction(faction.getName()).getEmoji() +
                    " collects " + spice + " " + Emojis.SPICE + " from " + territory.getTerritoryName());
            if (game.hasGameOption(GameOption.HOMEWORLDS) && faction.getName().equals("Harkonnen") && faction.isHighThreshold() && !((HarkonnenFaction)faction).hasTriggeredHT()) {
                faction.addSpice(2);
                faction.spiceMessage(2, "for High Threshold advantage", true);
                ((HarkonnenFaction)faction).setTriggeredHT(true);
            }
            if (orgizActive) {
                turnSummary.queueMessage(orgiz.getActiveFactions(game).get(0).getEmoji() +
                        " collects 1 " + Emojis.SPICE + " from " + territory.getTerritoryName() + " Because of Orgiz Processing Station");
            }
        }
        if (game.hasFaction("Harkonnen")) ((HarkonnenFaction)game.getFaction("Harkonnen")).setTriggeredHT(false);

        for (Territory territory : territories.values()) {
            if (territory.getDiscoveryToken() == null || territory.countActiveFactions() == 0) continue;
            Faction faction = territory.getActiveFactions(game).get(0);
            List<Button> buttons = new LinkedList<>();
            buttons.add(Button.primary("reveal-discovery-token-" + territory.getTerritoryName(), "Yes"));
            buttons.add(Button.danger("don't-reveal-discovery-token", "No"));
            discordGame.getFactionChat(faction.getName()).queueMessage("Would you like to reveal the discovery token at " + territory.getTerritoryName() + "? (" + territory.getDiscoveryToken() + ")", buttons);
        }

        if (altSpiceProductionTriggered) {
            TechToken.addSpice(game, TechToken.SPICE_PRODUCTION);
            TechToken.collectSpice(game, TechToken.SPICE_PRODUCTION);
        }

        if (game.hasGameOption(GameOption.MAP_IN_FRONT_OF_SHIELD))
            game.setUpdated(UpdateType.MAP);
        else
            ShowCommands.showBoard(discordGame, game);
    }

    public static void startMentatPause(DiscordGame discordGame, Game game) throws ChannelNotFoundException {
        TurnSummary turnSummary = discordGame.getTurnSummary();
        turnSummary.queueMessage("Turn " + game.getTurn() + " Mentat Pause Phase:");
        for (Faction faction : game.getFactions()) {
            if (faction.getFrontOfShieldSpice() > 0) {
                turnSummary.queueMessage(faction.getEmoji() + " collects " +
                        faction.getFrontOfShieldSpice() + " " + Emojis.SPICE + " from front of shield.");
                faction.addSpice(faction.getFrontOfShieldSpice());
                faction.spiceMessage(faction.getFrontOfShieldSpice(), "front of shield", true);
                faction.setFrontOfShieldSpice(0);
            }
            for (TreacheryCard card : faction.getTreacheryHand()) {
                if (card.name().trim().equalsIgnoreCase("Weather Control")) {
                    discordGame.getModInfo().queueMessage(faction.getEmoji() + " has Weather Control.");
                } else if (card.name().trim().equalsIgnoreCase("Family Atomics")) {
                    discordGame.getModInfo().queueMessage(faction.getEmoji() + " has Family Atomics.");
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
