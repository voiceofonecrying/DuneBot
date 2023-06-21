package controller.commands;

import constants.Emojis;
import enums.GameOption;
import exceptions.ChannelNotFoundException;
import model.*;
import model.factions.ChoamFaction;
import model.factions.Faction;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
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

    public static void runCommand(SlashCommandInteractionEvent event, DiscordGame discordGame, Game gameState) throws ChannelNotFoundException, IOException {
        String name = event.getSubcommandName();

        switch (name) {
            case "advance" -> advance(event, discordGame, gameState);
            case "bidding" -> bidding(event, discordGame, gameState);
            case "update-stronghold-skills" -> updateStrongholdSkills(event, discordGame, gameState);
        }
    }

    public static void advance(SlashCommandInteractionEvent event, DiscordGame discordGame, Game gameState) throws ChannelNotFoundException, IOException {
        if (gameState.getTurn() == 0) {
            discordGame.sendMessage("mod-info", "Please complete setup first.");
            return;
        }
        int phase = gameState.getPhase();
        int subPhase = gameState.getSubPhase();

        if (phase == 1 && subPhase == 1) {
            startStormPhase(event, discordGame, gameState);
            gameState.advanceSubPhase();
        } else if (phase == 1 && subPhase == 2) {
            endStormPhase(event, discordGame, gameState);
            gameState.advancePhase();
        } else if (phase == 2) {
            spiceBlow(event, discordGame, gameState);
            gameState.advancePhase();
        } else if (phase == 3) {
            choamCharity(event, discordGame, gameState);
            gameState.advancePhase();
        } else if (phase == 4 && subPhase == 1) {
            startBiddingPhase(event, discordGame, gameState);
            gameState.advanceSubPhase();
        } else if (phase == 4 && subPhase == 2) {
            cardCountsInBiddingPhase(event, discordGame, gameState);
            gameState.advanceSubPhase();
        } else if (phase == 4 && subPhase == 3) {
            finishBiddingPhase(event, discordGame, gameState);
            gameState.advancePhase();
        } else if (phase == 5) {
            startRevivalPhase(event, discordGame, gameState);
            gameState.advancePhase();
        } else if (phase == 6) {
            startShipmentPhase(event, discordGame, gameState);
            gameState.advancePhase();
        } else if (phase == 7) {
            startBattlePhase(event, discordGame, gameState);
            gameState.advancePhase();
        } else if (phase == 8) {
            startSpiceHarvest(event, discordGame, gameState);
            gameState.advancePhase();
        } else if (phase == 9) {
            startMentatPause(event, discordGame, gameState);
            gameState.advanceTurn();
        }

        discordGame.pushGameState();
    }

    public static void startStormPhase(SlashCommandInteractionEvent event, DiscordGame discordGame, Game gameState) throws ChannelNotFoundException {
        discordGame.sendMessage("turn-summary", "Turn " + gameState.getTurn() + " Storm Phase:");
        Map<String, Territory> territories = gameState.getTerritories();
        if (gameState.getTurn() != 1) {
            updateStrongholdSkills(event, discordGame, gameState);
            ShowCommands.refreshFrontOfShieldInfo(event, discordGame, gameState);

            discordGame.sendMessage("turn-summary", "The storm moves " + gameState.getStormMovement() + " sectors this turn.");

            StringBuilder message = new StringBuilder();
            for (int i = 0; i < gameState.getStormMovement(); i++) {
                gameState.advanceStorm(1);

                List<Territory> territoriesInStorm = territories.values().stream()
                        .filter(t ->
                                t.getSector() == gameState.getStorm() &&
                                !t.isRock()
                        ).toList();

                List<Territory> territoriesWithTroops = territoriesInStorm.stream()
                        .filter(t -> t.getForces().size() > 0).toList();

                List<Territory> territoriesWithSpice = territoriesInStorm.stream()
                        .filter(t -> t.getSpice() > 0).toList();

                for (Territory territory : territoriesWithTroops) {
                    message.append(stormTroops(territory, gameState));
                }

                for (Territory territory : territoriesWithSpice) {
                    message.append(stormRemoveSpice(territory));
                }
            }

            if (!message.isEmpty())
                discordGame.sendMessage("turn-summary", message.toString());

            ShowCommands.showBoard(discordGame, gameState);
        }

        gameState.setStormMovement(new Random().nextInt(6) + 1);
    }

    public static String stormTroops(Territory territory, Game gameState) {
        StringBuilder message = new StringBuilder();
        List<Force> fremenForces = territory.getForces().stream()
                .filter(f -> f.getFactionName().equalsIgnoreCase("Fremen"))
                .toList();

        List<Force> nonFremenForces = territory.getForces().stream()
                .filter(f -> !fremenForces.contains(f))
                .toList();

        if (fremenForces.size() > 0)
            message.append(stormTroopsFremen(territory, fremenForces, gameState));

        for (Force force : nonFremenForces) {
            message.append(stormRemoveTroops(territory, force, force.getStrength(), gameState));
        }

        return message.toString();
    }

    public static String stormTroopsFremen(Territory territory, List<Force> forces, Game gameState) {
        StringBuilder message = new StringBuilder();

        int totalTroops = forces.stream().mapToInt(f -> f.getStrength()).sum();
        int totalLostTroops = Math.ceilDiv(totalTroops, 2);

        Force regularForce = territory.getForce("Fremen");
        Force fedaykin = territory.getForce("Fremen*");

        int lostRegularForces = Math.min(regularForce.getStrength(), totalLostTroops);
        totalLostTroops -= lostRegularForces;
        int lostFedaykin = Math.min(fedaykin.getStrength(), totalLostTroops);

        if (lostRegularForces > 0)
            message.append(stormRemoveTroops(territory, regularForce, lostRegularForces, gameState));

        if (lostFedaykin > 0)
            message.append(stormRemoveTroops(territory, fedaykin, lostFedaykin, gameState));

        return message.toString();
    }

    public static String stormRemoveTroops(Territory territory, Force force, int strength, Game gameState) {
        territory.setForceStrength(force.getName(), force.getStrength() - strength);
        gameState.addToTanks(force.getName(), strength);

        return MessageFormat.format(
                "{0} lose {1} {2} to the storm in {3}\n",
                gameState.getFaction(force.getFactionName()).getEmoji(),
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

    public static void endStormPhase(SlashCommandInteractionEvent event, DiscordGame discordGame, Game gameState) throws ChannelNotFoundException {
        if (gameState.hasFaction("Fremen")) {
            discordGame.sendMessage("fremen-chat", "The storm will move " + gameState.getStormMovement() + " sectors next turn.");
        }
    }

    public static void spiceBlow(SlashCommandInteractionEvent event, DiscordGame discordGame, Game gameState) throws ChannelNotFoundException {
        discordGame.sendMessage("turn-summary", "Turn " + gameState.getTurn() + " Spice Blow Phase:");
    }

    public static void choamCharity(SlashCommandInteractionEvent event, DiscordGame discordGame, Game game) throws ChannelNotFoundException, IOException {
        discordGame.sendMessage("turn-summary", "Turn " + game.getTurn() + " CHOAM Charity Phase:");
        int multiplier = 1;

        if (game.hasFaction("CHOAM")) {
            multiplier = ((ChoamFaction) game.getFaction("CHOAM")).getChoamMultiplier(game.getTurn());

            if (multiplier == 0) {
                discordGame.sendMessage("turn-summary", "CHOAM Charity is cancelled!");
                return;
            }
        }

        int choamGiven = 0;
        List<Faction> factions = game.getFactions();
        if (game.hasFaction("CHOAM")) {
            discordGame.sendMessage("turn-summary",
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
                discordGame.sendMessage("turn-summary", faction.getEmoji() + " have received " +
                        2 * multiplier + " " + Emojis.SPICE + " in CHOAM Charity.");
                CommandManager.spiceMessage(discordGame, 2 * multiplier, faction.getName(), "CHOAM Charity", true);
            }
            else if (spice < 2) {
                int charity = multiplier * (2 - spice);
                choamGiven += charity;
                faction.addSpice(charity);
                discordGame.sendMessage("turn-summary",
                        faction.getEmoji() + " have received " + charity + " " + Emojis.SPICE +
                                " in CHOAM Charity."
                );
                if (game.hasGameOption(GameOption.TECH_TOKENS) && !game.hasGameOption(GameOption.ALTERNATE_SPICE_PRODUCTION)) TechToken.addSpice(game, discordGame, "Spice Production");
                CommandManager.spiceMessage(discordGame, charity, faction.getName(), "CHOAM Charity", true);
            }
            else continue;
            ShowCommands.writeFactionInfo(discordGame, faction);
        }
        if (game.hasFaction("CHOAM")) {
            Faction choamFaction = game.getFaction("CHOAM");
            choamFaction.addSpice((2 * factions.size() * multiplier) - choamGiven);
            CommandManager.spiceMessage(discordGame, game.getFactions().size() * 2 * multiplier, "choam", "CHOAM Charity", true);
            discordGame.sendMessage("turn-summary",
                    choamFaction.getEmoji() + " has paid " + choamGiven +
                            " " + Emojis.SPICE + " to factions in need."
            );
            CommandManager.spiceMessage(discordGame, choamGiven, "choam", "CHOAM Charity given", false);
            ShowCommands.writeFactionInfo(discordGame, choamFaction);
        }
        if (game.hasGameOption(GameOption.TECH_TOKENS) && !game.hasGameOption(GameOption.ALTERNATE_SPICE_PRODUCTION)) TechToken.collectSpice(game, discordGame, "Spice Production");
    }

    public static void startBiddingPhase(SlashCommandInteractionEvent event, DiscordGame discordGame, Game gameState) throws ChannelNotFoundException {
        discordGame.sendMessage("turn-summary", "Turn " + gameState.getTurn() + " Bidding Phase:");
        gameState.setBidOrder(new ArrayList<>());
        gameState.setBidCardNumber(0);
        gameState.setBidCard(null);
        gameState.getFactions().forEach(faction -> {
            faction.setBid("");
            faction.setMaxBid(0);
        });
        discordGame.sendMessage("mod-info", "Run black market bid (if exists), then advance the game.");
    }

    public static void cardCountsInBiddingPhase(SlashCommandInteractionEvent event, DiscordGame discordGame, Game gameState) throws ChannelNotFoundException {
        StringBuilder message = new StringBuilder();

        message.append(MessageFormat.format(
                "{0}Number of Treachery Cards{0}\n",
                Emojis.TREACHERY
        ));

        List<Faction> factions = gameState.getFactions();

        message.append(
                factions.stream().map(
                        f -> MessageFormat.format(
                                "{0}: {1}\n", f.getEmoji(), f.getTreacheryHand().size()
                        )
                ).collect(Collectors.joining())
        );

        int numCardsForBid = factions.stream()
                .filter(f -> f.getHandLimit() > f.getTreacheryHand().size())
                .toList().size();

        message.append(
                MessageFormat.format(
                        "There will be {0}{1} up for bid this round.",
                        numCardsForBid, Emojis.TREACHERY
                )
        );

        discordGame.sendMessage("turn-summary", message.toString());
        discordGame.sendMessage("mod-info", "Start running commands to bid and then advance when all the bidding is done.");
    }

    public static void finishBiddingPhase(SlashCommandInteractionEvent event, DiscordGame discordGame, Game gameState) throws ChannelNotFoundException {
        if (gameState.getBidCard() != null) {
            discordGame.sendMessage("turn-summary", "Card up for bid is placed on top of the Treachery Deck");
            gameState.getTreacheryDeck().addFirst(gameState.getBidCard());
            gameState.setBidCard(null);
        }
    }

    public static void bidding(SlashCommandInteractionEvent event, DiscordGame discordGame, Game gameState) throws ChannelNotFoundException {
        updateBidOrder(gameState);
        List<String> bidOrder = gameState.getBidOrder()
                .stream()
                .filter(f -> gameState.getFaction(f).getHandLimit() > gameState.getFaction(f).getTreacheryHand().size())
                .collect(Collectors.toList());

        if (bidOrder.size() == 0) {
            discordGame.sendMessage("bidding-phase", "All hands are full.");
            discordGame.sendMessage("mod-info", "If a player discards now, execute '/run bidding' again.");
        } else {
            gameState.incrementBidCardNumber();

            List<TreacheryCard> treacheryDeck = gameState.getTreacheryDeck();

            if (treacheryDeck.isEmpty()) {
                List<TreacheryCard> treacheryDiscard = gameState.getTreacheryDiscard();
                discordGame.sendMessage("turn-summary", "The Treachery Deck has been replenished from the Discard Pile");
                treacheryDeck.addAll(treacheryDiscard);
                Collections.shuffle(treacheryDeck);
                treacheryDiscard.clear();
            }

            TreacheryCard bidCard = treacheryDeck.remove(0);

            gameState.setBidCard(bidCard);

            gameState.setCurrentBid(0);

            for (Faction faction : gameState.getFactions()) {
                faction.setMaxBid(0);
                faction.setAutoBid(false);
                faction.setBid("");
            }

            AtreidesCommands.sendAtreidesCardPrescience(discordGame, gameState, bidCard);

            Faction firstBidFaction = gameState.getFaction(bidOrder.get(bidOrder.size() - 1 ));

            gameState.setCurrentBidder(firstBidFaction.getName());

            createBidMessage(discordGame, gameState, bidOrder, firstBidFaction);

            discordGame.pushGameState();
        }
    }

    public static boolean createBidMessage(DiscordGame discordGame, Game gameState, List<String> bidOrder, Faction currentBidder) throws ChannelNotFoundException {
        StringBuilder message = new StringBuilder();



        if (!currentBidder.getBid().equals("pass") && !currentBidder.getBid().equals("")) {
            gameState.setCurrentBid(Integer.parseInt(currentBidder.getBid()));
            gameState.setBidLeader(currentBidder.getName());
        }

        message.append(
                MessageFormat.format(
                        "R{0}:C{1}\n",
                        gameState.getTurn(), gameState.getBidCardNumber()
                )
        );

        boolean tag = bidOrder.get(bidOrder.size() - 1).equals(currentBidder.getName());
        for (String factionName : bidOrder) {
            Faction f = gameState.getFaction(factionName);
            if (tag) {
                if (f.getName().equals(gameState.getBidLeader())) {
                    discordGame.sendMessage("bidding-phase", message.toString());
                    discordGame.sendMessage("bidding-phase", f.getEmoji() + " has the top bid.");
                    return true;
                }
                gameState.setCurrentBidder(f.getName());
                message.append(f.getEmoji() + " - " + f.getPlayer() + "\n");
                tag = false;
            } else {
                message.append(f.getEmoji() + " - " + f.getBid() + "\n");
            }
            if (currentBidder.getName().equals(f.getName())) tag = true;
        }

        discordGame.sendMessage("bidding-phase", message.toString());
        return false;
    }

    public static void updateBidOrder(Game gameState) {
        List<String> bidOrder;

        if (gameState.getBidOrder().isEmpty()) {
            List<Faction> factions = gameState.getFactions();

            int firstBid = Math.ceilDiv(gameState.getStorm(), 3) % factions.size();

            List<Faction> bidOrderFactions = new ArrayList<>();

            bidOrderFactions.addAll(factions.subList(firstBid, factions.size()));
            bidOrderFactions.addAll(factions.subList(0, firstBid));
            bidOrder = bidOrderFactions.stream().map(Faction::getName).collect(Collectors.toList());
        } else {
            bidOrder = gameState.getBidOrder();
            bidOrder.add(bidOrder.remove(0));
        }

        String firstFaction = bidOrder.get(0);
        String faction = firstFaction;
        while (gameState.getFaction(faction).getHandLimit() <= gameState.getFaction(faction).getTreacheryHand().size()) {
            faction = bidOrder.remove(0);
            bidOrder.add(faction);
            if (faction.equalsIgnoreCase(firstFaction)) {
                break;
            }

        }

        gameState.setBidOrder(bidOrder);
    }

    public static void startRevivalPhase(SlashCommandInteractionEvent event, DiscordGame discordGame, Game gameState) throws ChannelNotFoundException {
        discordGame.sendMessage("turn-summary", "Turn " + gameState.getTurn() + " Revival Phase:");
        List<Faction> factions = gameState.getFactions();
        StringBuilder message = new StringBuilder();
        boolean nonBTRevival = false;

        for (Faction faction : factions) {
            int revived = 0;
            boolean revivedStar = false;
            for (int i = faction.getFreeRevival(); i > 0; i--) {
                if (gameState.getForceFromTanks(faction.getName()).getStrength() == 0
                        && gameState.getForceFromTanks(faction.getName() + "*").getStrength() == 0) continue;
                revived++;
                if (gameState.getForceFromTanks(faction.getName() + "*").getStrength() > 0 && !revivedStar) {
                    Force force = gameState.getForceFromTanks(faction.getName() + "*");
                    force.setStrength(force.getStrength() - 1);
                    revivedStar = true;
                    faction.getSpecialReserves().setStrength(faction.getSpecialReserves().getStrength() + 1);
                } else if (gameState.getForceFromTanks(faction.getName()).getStrength() > 0) {
                    Force force = gameState.getForceFromTanks(faction.getName());
                    force.setStrength(force.getStrength() - 1);
                    faction.getReserves().setStrength(faction.getReserves().getStrength() + 1);
                }
            }
            if (revived > 0) {
                if (!faction.getName().equals("BT")) nonBTRevival = true;
                if (message.isEmpty()) message.append("Free Revivals:\n");
                message.append(gameState.getFaction(faction.getName()).getEmoji()).append(": ").append(revived).append("\n");
            }
        }
        if (!message.isEmpty()) discordGame.sendMessage("turn-summary", message.toString());
        if (nonBTRevival && gameState.hasGameOption(GameOption.TECH_TOKENS)) TechToken.addSpice(gameState, discordGame, "Axlotl Tanks");

        ShowCommands.showBoard(discordGame, gameState);
    }

    public static void startShipmentPhase(SlashCommandInteractionEvent event, DiscordGame discordGame, Game gameState) throws ChannelNotFoundException, IOException {

        if (gameState.hasGameOption(GameOption.TECH_TOKENS)) TechToken.collectSpice(gameState, discordGame, "Axlotl Tanks");

        discordGame.sendMessage("turn-summary","Turn " + gameState.getTurn() + " Shipment and Movement Phase:");
        if (gameState.hasFaction("Atreides")) {
            discordGame.sendMessage("atreides-chat", "You see visions of " + gameState.getSpiceDeck().peek().name() + " in your future.");
        }
        if(gameState.hasFaction("BG")) {
            for (Territory territory : gameState.getTerritories().values()) {
                if (territory.getForce("Advisor").getStrength() > 0) discordGame.sendMessage("turn-summary",gameState
                        .getFaction("BG").getEmoji() + " to decide whether to flip their advisors in " + territory.getTerritoryName());
            }
        }
        ShowCommands.showBoard(discordGame, gameState);
    }

    public static void startBattlePhase(SlashCommandInteractionEvent event, DiscordGame discordGame, Game gameState) throws ChannelNotFoundException, IOException {

        if (gameState.hasGameOption(GameOption.TECH_TOKENS)) TechToken.collectSpice(gameState, discordGame, "Heighliners");

        discordGame.sendMessage("turn-summary","Turn " + gameState.getTurn() + " Battle Phase:");

        // Get list of territories with multiple factions
        List<Pair<Territory, List<Faction>>> battles = new ArrayList<>();
        for (Territory territory : gameState.getTerritories().values()) {
            List<Force> forces = territory.getForces();
            Set<String> factionNames = forces.stream()
                    .filter(force -> !(force.getName().equalsIgnoreCase("Advisor")))
                    .map(Force::getFactionName)
                    .collect(Collectors.toSet());

            if (gameState.hasFaction("Richese") && territory.hasRicheseNoField())
                factionNames.add("Richese");

            List<Faction> factions = factionNames.stream()
                    .sorted(Comparator.comparingInt(gameState::getFactionTurnIndex))
                    .map(gameState::getFaction)
                    .toList();

            if (factions.size() > 1 && !territory.getTerritoryName().equalsIgnoreCase("Polar Sink")) {
                battles.add(new ImmutablePair<>(territory, factions));
            }
        }

        if(battles.size() > 0) {
            String battleMessages = battles.stream()
                    .sorted(Comparator
                            .comparingInt(o -> gameState.getFactionTurnIndex(o.getRight().get(0).getName()))
                    ).map((battle) ->
                            MessageFormat.format("{0} in {1}",
                                    battle.getRight().stream()
                                            .map(Faction::getEmoji)
                                            .collect(Collectors.joining(" vs ")),
                                    battle.getLeft().getTerritoryName()
                            )
                    ).collect(Collectors.joining("\n"));

            discordGame.sendMessage("turn-summary",
                    "The following battles will take place this turn:\n" + battleMessages
            );
        } else {
            discordGame.sendMessage("turn-summary", "There are no battles this turn.");
        }
        ShowCommands.showBoard(discordGame, gameState);
    }

    public static void startSpiceHarvest(SlashCommandInteractionEvent event, DiscordGame discordGame, Game gameState) throws ChannelNotFoundException, IOException {
        discordGame.sendMessage("turn-summary", "Turn " + gameState.getTurn() + " Spice Harvest Phase:");
        Map<String, Territory> territories = gameState.getTerritories();
        for (Territory territory : territories.values()) {
            if (territory.getForces().size() != 1) continue;
            if (territory.getForces().get(0).getName().equals("Advisor")) {
                int strength = territory.getForces().get(0).getStrength();
                territory.getForces().clear();
                territory.getForces().add(new Force("BG", strength));
                discordGame.sendMessage("turn-summary", "Advisors are alone in " + territory.getTerritoryName() + " and have flipped to fighters.");
            }
        }

        Set<Faction> factionsWithChanges = new HashSet<>();

        for (Faction faction : gameState.getFactions()) {
            faction.setHasMiningEquipment(false);
            if (territories.get("Arrakeen").getForces().stream().anyMatch(force -> force.getName().contains(faction.getName()))) {
                faction.addSpice(2);
                CommandManager.spiceMessage(discordGame, 2, faction.getName(), "for Arrakeen", true);
                discordGame.sendMessage("turn-summary", gameState.getFaction(faction.getName()).getEmoji() +
                        " collects 2 " + Emojis.SPICE + " from Arrakeen");
                faction.setHasMiningEquipment(true);
                factionsWithChanges.add(faction);
            }
            if (territories.get("Carthag").getForces().stream().anyMatch(force -> force.getName().contains(faction.getName()))) {
                faction.addSpice(2);
                CommandManager.spiceMessage(discordGame, 2, faction.getName(), "for Carthag", true);
                discordGame.sendMessage("turn-summary", gameState.getFaction(faction.getName()).getEmoji() +
                        " collects 2 " + Emojis.SPICE + " from Carthag");
                faction.setHasMiningEquipment(true);
                factionsWithChanges.add(faction);
            }
            if (territories.get("Tuek's Sietch").getForces().stream().anyMatch(force -> force.getName().contains(faction.getName()))) {
                discordGame.sendMessage("turn-summary", gameState.getFaction(faction.getName()).getEmoji() +
                        " collects 1 " + Emojis.SPICE + " from Tuek's Sietch");
                faction.addSpice(1);
                CommandManager.spiceMessage(discordGame, 1, faction.getName(), "for Tuek's Sietch", true);
                factionsWithChanges.add(faction);
            }
        }

        boolean altSpiceProductionTriggered = false;
        for (Territory territory: territories.values()) {
            if (territory.getSpice() == 0 || territory.getForces().size() == 0) continue;
            int totalStrength = 0;
            Faction faction = gameState.getFaction(territory.getForces().stream().filter(force -> !force.getName().equals("Advisor")).findFirst().orElseThrow().getName().replace("*", ""));
            for (Force force : territory.getForces()) {
                if (force.getName().equals("Advisor")) continue;
                totalStrength += force.getStrength();
            }
            int multiplier = faction.hasMiningEquipment() ? 3 : 2;
            int spice = Math.min(multiplier * totalStrength, territory.getSpice());
            faction.addSpice(spice);
            CommandManager.spiceMessage(discordGame, spice, faction.getName(), "for Spice Blow", true);
            factionsWithChanges.add(faction);
            if (gameState.hasGameOption(GameOption.TECH_TOKENS) && gameState.hasGameOption(GameOption.ALTERNATE_SPICE_PRODUCTION)
                    && (!faction.getName().equals("Fremen") || gameState.hasGameOption(GameOption.FREMEN_TRIGGER_ALTERNATE_SPICE_PRODUCTION))) altSpiceProductionTriggered = true;
            territory.setSpice(territory.getSpice() - spice);
            discordGame.sendMessage("turn-summary", gameState.getFaction(faction.getName()).getEmoji() +
                    " collects " + spice + " " + Emojis.SPICE + " from " + territory.getTerritoryName());
        }

        for (Faction faction : factionsWithChanges) ShowCommands.writeFactionInfo(discordGame, faction);

        if (altSpiceProductionTriggered) {
            TechToken.addSpice(gameState, discordGame, "Spice Production");
            TechToken.collectSpice(gameState, discordGame, "Spice Production");
        }

        ShowCommands.showBoard(discordGame, gameState);
    }

    public static void startMentatPause(SlashCommandInteractionEvent event, DiscordGame discordGame, Game gameState) throws ChannelNotFoundException, IOException {
        discordGame.sendMessage("turn-summary", "Turn " + gameState.getTurn() + " Mentat Pause Phase:");
        for (Faction faction : gameState.getFactions()) {
            if (faction.getFrontOfShieldSpice() > 0) {
                discordGame.sendMessage("turn-summary", faction.getEmoji() + " collects " +
                        faction.getFrontOfShieldSpice() + " " + Emojis.SPICE + " from front of shield.");
                CommandManager.spiceMessage(discordGame,  faction.getFrontOfShieldSpice(), faction.getName(), "front of shield", true);
                faction.addSpice(faction.getFrontOfShieldSpice());
                faction.setFrontOfShieldSpice(0);
                ShowCommands.writeFactionInfo(discordGame, faction);
            }
        }
    }

    public static void updateStrongholdSkills(SlashCommandInteractionEvent event, DiscordGame discordGame, Game gameState) {
        if (gameState.hasStrongholdSkills()) {
            for (Faction faction : gameState.getFactions()) {
                faction.removeResource("strongholdCard");
            }

            List<Territory> strongholds = gameState.getTerritories().values().stream()
                    .filter(Territory::isStronghold)
                    .filter(t -> t.countActiveFactions(gameState) == 1)
                    .toList();

            for (Territory stronghold : strongholds) {
                Faction faction = stronghold.getActiveFactions(gameState).get(0);
                faction.addResource(new StringResource("strongholdCard", stronghold.getTerritoryName()));
            }

            discordGame.pushGameState();
        }
    }
}
