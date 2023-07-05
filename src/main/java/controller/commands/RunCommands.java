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

    public static void runCommand(SlashCommandInteractionEvent event, DiscordGame discordGame, Game game) throws ChannelNotFoundException, IOException {
        String name = event.getSubcommandName();
        if (name == null) throw new IllegalArgumentException("Invalid command name: null");

        switch (name) {
            case "advance" -> advance(event, discordGame, game);
            case "bidding" -> bidding(discordGame, game);
            case "update-stronghold-skills" -> updateStrongholdSkills(discordGame, game);
        }
    }

    public static void advance(SlashCommandInteractionEvent event, DiscordGame discordGame, Game game) throws ChannelNotFoundException, IOException {
        if (game.getTurn() == 0) {
            discordGame.sendMessage("mod-info", "Please complete setup first.");
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
            startBiddingPhase(discordGame, game);
            game.advanceSubPhase();
        } else if (phase == 4 && subPhase == 2) {
            cardCountsInBiddingPhase(discordGame, game);
            game.advanceSubPhase();
        } else if (phase == 4 && subPhase == 3) {
            finishBiddingPhase(discordGame, game);
            game.advancePhase();
        } else if (phase == 5) {
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
            startMentatPause(event, discordGame, game);
            game.advanceTurn();
        }

        discordGame.pushGame();
    }

    public static void startStormPhase(DiscordGame discordGame, Game game) throws ChannelNotFoundException {
        discordGame.sendMessage("turn-summary", "Turn " + game.getTurn() + " Storm Phase:");
        if (game.getTurn() != 1) {
            discordGame.sendMessage("turn-summary",
                    "The storm would move " +
                    game.getStormMovement() +
                    " sectors this turn. Weather Control and Family Atomics may be played at this time.");
        } else {
            discordGame.sendMessage("mod-info", "Run advance to complete turn 1 storm phase.");
        }
        for (Faction faction : game.getFactions()) {
            for (TreacheryCard card : faction.getTreacheryHand()) {
                if (card.name().trim().equalsIgnoreCase("Weather Control")) {
                    discordGame.sendMessage(faction.getName().toLowerCase() + "-chat", "" + faction.getPlayer().toString() + " will you play Weather Control?");
                } else if (card.name().trim().equalsIgnoreCase("Family Atomics")) {
                    discordGame.sendMessage(faction.getName().toLowerCase() + "-chat", "" + faction.getPlayer().toString() + " will you play Family Atomics?");
                }
            }
        }
    }

    public static String stormTroops(Territory territory, Game game) {
        StringBuilder message = new StringBuilder();
        List<Force> fremenForces = territory.getForces().stream()
                .filter(f -> f.getFactionName().equalsIgnoreCase("Fremen"))
                .toList();

        List<Force> nonFremenForces = territory.getForces().stream()
                .filter(f -> !fremenForces.contains(f))
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
            discordGame.sendMessage("turn-summary", "The storm moves " + game.getStormMovement() + " sectors this turn.");

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

            if (!message.isEmpty())
                discordGame.sendMessage("turn-summary", message.toString());

            ShowCommands.showBoard(discordGame, game);
        }

        game.setStormMovement(new Random().nextInt(6) + 1);
        if (game.hasFaction("Fremen")) {
            discordGame.sendMessage("fremen-chat", "The storm will move " + game.getStormMovement() + " sectors next turn.");
        }
    }

    public static void spiceBlow(DiscordGame discordGame, Game game) throws ChannelNotFoundException {
        discordGame.sendMessage("turn-summary", "Turn " + game.getTurn() + " Spice Blow Phase:");
    }

    public static void choamCharity(DiscordGame discordGame, Game game) throws ChannelNotFoundException, IOException {
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

    public static void startBiddingPhase(DiscordGame discordGame, Game game) throws ChannelNotFoundException {
        discordGame.sendMessage("turn-summary", "Turn " + game.getTurn() + " Bidding Phase:");
        game.setBidOrder(new ArrayList<>());
        game.setBidCardNumber(0);
        game.setBidCard(null);
        game.getFactions().forEach(faction -> {
            faction.setBid("");
            faction.setMaxBid(0);
        });
        discordGame.sendMessage("mod-info", "Run black market bid (if exists), then advance the game.");
    }

    public static void cardCountsInBiddingPhase(DiscordGame discordGame, Game game) throws ChannelNotFoundException {
        StringBuilder message = new StringBuilder();

        message.append(MessageFormat.format(
                "{0}Number of Treachery Cards{0}\n",
                Emojis.TREACHERY
        ));

        List<Faction> factions = game.getFactions();

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

    public static void finishBiddingPhase(DiscordGame discordGame, Game game) throws ChannelNotFoundException {
        if (game.getBidCard() != null) {
            discordGame.sendMessage("turn-summary", "Card up for bid is placed on top of the Treachery Deck");
            game.getTreacheryDeck().addFirst(game.getBidCard());
            game.setBidCard(null);
        }
        discordGame.sendMessage("mod-info", "Bidding phase ended. Run advance to start revivals.");
    }

    public static void bidding(DiscordGame discordGame, Game game) throws ChannelNotFoundException {
        updateBidOrder(game);
        List<String> bidOrder = game.getEligibleBidOrder();

        if (bidOrder.size() == 0) {
            discordGame.sendMessage("bidding-phase", "All hands are full.");
            discordGame.sendMessage("mod-info", "If a player discards now, execute '/run bidding' again.");
        } else {
            game.incrementBidCardNumber();

            List<TreacheryCard> treacheryDeck = game.getTreacheryDeck();

            if (treacheryDeck.isEmpty()) {
                List<TreacheryCard> treacheryDiscard = game.getTreacheryDiscard();
                discordGame.sendMessage("turn-summary", "The Treachery Deck has been replenished from the Discard Pile");
                treacheryDeck.addAll(treacheryDiscard);
                Collections.shuffle(treacheryDeck);
                treacheryDiscard.clear();
            }

            TreacheryCard bidCard = treacheryDeck.remove(0);

            game.setBidCard(bidCard);
            game.setBidLeader("");
            game.setCurrentBid(0);

            for (Faction faction : game.getFactions()) {
                faction.setMaxBid(0);
                faction.setAutoBid(false);
                faction.setBid("");
            }

            AtreidesCommands.sendAtreidesCardPrescience(discordGame, game, bidCard);

            Faction firstBidFaction = game.getFaction(bidOrder.get(bidOrder.size() - 1 ));

            game.setCurrentBidder(firstBidFaction.getName());

            createBidMessage(discordGame, game, bidOrder, firstBidFaction);

            discordGame.pushGame();
        }
    }

    public static boolean createBidMessage(DiscordGame discordGame, Game game, List<String> bidOrder, Faction currentBidder) throws ChannelNotFoundException {
        StringBuilder message = new StringBuilder();



        if (!currentBidder.getBid().equals("pass") && !currentBidder.getBid().equals("")) {
            game.setCurrentBid(Integer.parseInt(currentBidder.getBid()));
            game.setBidLeader(currentBidder.getName());
        }

        message.append(
                MessageFormat.format(
                        "R{0}:C{1}\n",
                        game.getTurn(), game.getBidCardNumber()
                )
        );

        boolean tag = bidOrder.get(bidOrder.size() - 1).equals(currentBidder.getName());
        for (String factionName : bidOrder) {
            Faction f = game.getFaction(factionName);
            if (tag) {
                if (f.getName().equals(game.getBidLeader())) {
                    discordGame.sendMessage("bidding-phase", message.toString());
                    discordGame.sendMessage("bidding-phase", f.getEmoji() + " has the top bid.");
                    return true;
                }
                game.setCurrentBidder(f.getName());
                message.append(f.getEmoji()).append(" - ").append(f.getPlayer()).append("\n");
                tag = false;
            } else {
                message.append(f.getEmoji()).append(" - ").append(f.getBid()).append("\n");
            }
            if (currentBidder.getName().equals(f.getName())) tag = true;
        }

        discordGame.sendMessage("bidding-phase", message.toString());
        return false;
    }

    public static void updateBidOrder(Game game) {
        List<String> bidOrder;

        if (game.getBidOrder().isEmpty()) {
            List<Faction> factions = game.getFactions();

            int firstBid = Math.ceilDiv(game.getStorm(), 3) % factions.size();

            List<Faction> bidOrderFactions = new ArrayList<>();

            bidOrderFactions.addAll(factions.subList(firstBid, factions.size()));
            bidOrderFactions.addAll(factions.subList(0, firstBid));
            bidOrder = bidOrderFactions.stream().map(Faction::getName).collect(Collectors.toList());
        } else {
            bidOrder = game.getBidOrder();
            List<String> eligibleBidOrder = game.getEligibleBidOrder();
            while (!bidOrder.get(0).equalsIgnoreCase(eligibleBidOrder.get(0))) {
                bidOrder.add(bidOrder.remove(0));
            }
            bidOrder.add(bidOrder.remove(0));
        }

        String firstFaction = bidOrder.get(0);
        String faction = firstFaction;
        while (game.getFaction(faction).getHandLimit() <= game.getFaction(faction).getTreacheryHand().size()) {
            faction = bidOrder.remove(0);
            bidOrder.add(faction);
            if (faction.equalsIgnoreCase(firstFaction)) {
                break;
            }

        }

        game.setBidOrder(bidOrder);
    }

    public static void startRevivalPhase(DiscordGame discordGame, Game game) throws ChannelNotFoundException, IOException {
        discordGame.sendMessage("turn-summary", "Turn " + game.getTurn() + " Revival Phase:");
        List<Faction> factions = game.getFactions();
        StringBuilder message = new StringBuilder();
        boolean nonBTRevival = false;

        for (Faction faction : factions) {
            int revived = 0;
            boolean revivedStar = false;
            for (int i = faction.getFreeRevival(); i > 0; i--) {
                if (game.getForceFromTanks(faction.getName()).getStrength() == 0
                        && game.getForceFromTanks(faction.getName() + "*").getStrength() == 0) continue;
                revived++;
                if (game.getForceFromTanks(faction.getName() + "*").getStrength() > 0 && !revivedStar) {
                    Force force = game.getForceFromTanks(faction.getName() + "*");
                    force.setStrength(force.getStrength() - 1);
                    revivedStar = true;
                    faction.getSpecialReserves().setStrength(faction.getSpecialReserves().getStrength() + 1);
                } else if (game.getForceFromTanks(faction.getName()).getStrength() > 0) {
                    Force force = game.getForceFromTanks(faction.getName());
                    force.setStrength(force.getStrength() - 1);
                    faction.getReserves().setStrength(faction.getReserves().getStrength() + 1);
                }
            }
            if (revived > 0) {
                if (!faction.getName().equals("BT")) nonBTRevival = true;
                if (message.isEmpty()) message.append("Free Revivals:\n");
                message.append(game.getFaction(faction.getName()).getEmoji()).append(": ").append(revived).append("\n");
            }
        }
        if (!message.isEmpty()) discordGame.sendMessage("turn-summary", message.toString());
        if (nonBTRevival && game.hasGameOption(GameOption.TECH_TOKENS)) TechToken.addSpice(game, discordGame, "Axlotl Tanks");

        ShowCommands.showBoard(discordGame, game);
    }

    public static void startShipmentPhase(DiscordGame discordGame, Game game) throws ChannelNotFoundException, IOException {

        if (game.hasGameOption(GameOption.TECH_TOKENS)) TechToken.collectSpice(game, discordGame, "Axlotl Tanks");

        discordGame.sendMessage("turn-summary","Turn " + game.getTurn() + " Shipment and Movement Phase:");
        if (game.hasFaction("Atreides")) {
            discordGame.sendMessage("atreides-chat", "You see visions of " + game.getSpiceDeck().peek().name() + " in your future.");
        }
        if(game.hasFaction("BG")) {
            StringBuilder message = new StringBuilder();
            for (Territory territory : game.getTerritories().values()) {
                if (territory.getForce("Advisor").getStrength() > 0) {
                    message.append(game.getFaction("BG").getEmoji()).append(" to decide whether to flip their advisors in ").append(territory.getTerritoryName()).append("\n");
                }
            }
            if (!message.isEmpty()) discordGame.sendMessage("game-actions", message.append(game.getFaction("BG").getPlayer()).toString());
        }
        ShowCommands.showBoard(discordGame, game);
    }

    public static void startBattlePhase(DiscordGame discordGame, Game game) throws ChannelNotFoundException, IOException {

        if (game.hasGameOption(GameOption.TECH_TOKENS)) TechToken.collectSpice(game, discordGame, "Heighliners");

        discordGame.sendMessage("turn-summary","Turn " + game.getTurn() + " Battle Phase:");

        // Get list of territories with multiple factions
        List<Pair<Territory, List<Faction>>> battles = new ArrayList<>();
        for (Territory territory : game.getTerritories().values()) {
            List<Force> forces = territory.getForces();
            Set<String> factionNames = forces.stream()
                    .filter(force -> !(force.getName().equalsIgnoreCase("Advisor")))
                    .map(Force::getFactionName)
                    .collect(Collectors.toSet());

            if (game.hasFaction("Richese") && territory.hasRicheseNoField())
                factionNames.add("Richese");

            List<Faction> factions = factionNames.stream()
                    .sorted(Comparator.comparingInt(game::getFactionTurnIndex))
                    .map(game::getFaction)
                    .toList();

            if (factions.size() > 1 && !territory.getTerritoryName().equalsIgnoreCase("Polar Sink")) {
                battles.add(new ImmutablePair<>(territory, factions));
            }
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

            discordGame.sendMessage("turn-summary",
                    "The following battles will take place this turn:\n" + battleMessages
            );
        } else {
            discordGame.sendMessage("turn-summary", "There are no battles this turn.");
        }
        ShowCommands.showBoard(discordGame, game);
    }

    public static void startSpiceHarvest(DiscordGame discordGame, Game game) throws ChannelNotFoundException, IOException {
        discordGame.sendMessage("turn-summary", "Turn " + game.getTurn() + " Spice Harvest Phase:");
        Map<String, Territory> territories = game.getTerritories();
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

        for (Faction faction : game.getFactions()) {
            faction.setHasMiningEquipment(false);
            if (territories.get("Arrakeen").getForces().stream().anyMatch(force -> force.getName().contains(faction.getName()))) {
                faction.addSpice(2);
                CommandManager.spiceMessage(discordGame, 2, faction.getName(), "for Arrakeen", true);
                discordGame.sendMessage("turn-summary", game.getFaction(faction.getName()).getEmoji() +
                        " collects 2 " + Emojis.SPICE + " from Arrakeen");
                faction.setHasMiningEquipment(true);
                factionsWithChanges.add(faction);
            }
            if (territories.get("Carthag").getForces().stream().anyMatch(force -> force.getName().contains(faction.getName()))) {
                faction.addSpice(2);
                CommandManager.spiceMessage(discordGame, 2, faction.getName(), "for Carthag", true);
                discordGame.sendMessage("turn-summary", game.getFaction(faction.getName()).getEmoji() +
                        " collects 2 " + Emojis.SPICE + " from Carthag");
                faction.setHasMiningEquipment(true);
                factionsWithChanges.add(faction);
            }
            if (territories.get("Tuek's Sietch").getForces().stream().anyMatch(force -> force.getName().contains(faction.getName()))) {
                discordGame.sendMessage("turn-summary", game.getFaction(faction.getName()).getEmoji() +
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
            Faction faction = game.getFaction(territory.getForces().stream().filter(force -> !force.getName().equals("Advisor")).findFirst().orElseThrow().getName().replace("*", ""));
            for (Force force : territory.getForces()) {
                if (force.getName().equals("Advisor")) continue;
                totalStrength += force.getStrength();
            }
            int multiplier = faction.hasMiningEquipment() ? 3 : 2;
            int spice = Math.min(multiplier * totalStrength, territory.getSpice());
            faction.addSpice(spice);
            CommandManager.spiceMessage(discordGame, spice, faction.getName(), "for Spice Blow", true);
            factionsWithChanges.add(faction);
            if (game.hasGameOption(GameOption.TECH_TOKENS) && game.hasGameOption(GameOption.ALTERNATE_SPICE_PRODUCTION)
                    && (!faction.getName().equals("Fremen") || game.hasGameOption(GameOption.FREMEN_TRIGGER_ALTERNATE_SPICE_PRODUCTION))) altSpiceProductionTriggered = true;
            territory.setSpice(territory.getSpice() - spice);
            discordGame.sendMessage("turn-summary", game.getFaction(faction.getName()).getEmoji() +
                    " collects " + spice + " " + Emojis.SPICE + " from " + territory.getTerritoryName());
        }

        for (Faction faction : factionsWithChanges) ShowCommands.writeFactionInfo(discordGame, faction);

        if (altSpiceProductionTriggered) {
            TechToken.addSpice(game, discordGame, "Spice Production");
            TechToken.collectSpice(game, discordGame, "Spice Production");
        }

        ShowCommands.showBoard(discordGame, game);
    }

    public static void startMentatPause(SlashCommandInteractionEvent event, DiscordGame discordGame, Game game) throws ChannelNotFoundException, IOException {
        discordGame.sendMessage("turn-summary", "Turn " + game.getTurn() + " Mentat Pause Phase:");
        for (Faction faction : game.getFactions()) {
            if (faction.getFrontOfShieldSpice() > 0) {
                discordGame.sendMessage("turn-summary", faction.getEmoji() + " collects " +
                        faction.getFrontOfShieldSpice() + " " + Emojis.SPICE + " from front of shield.");
                CommandManager.spiceMessage(discordGame,  faction.getFrontOfShieldSpice(), faction.getName(), "front of shield", true);
                faction.addSpice(faction.getFrontOfShieldSpice());
                faction.setFrontOfShieldSpice(0);
                ShowCommands.writeFactionInfo(discordGame, faction);
            }
            for (TreacheryCard card : faction.getTreacheryHand()) {
                if (card.name().trim().equalsIgnoreCase("Weather Control")) {
                    discordGame.sendMessage("mod-info", "" + faction.getEmoji() + " has Weather Control.");
                } else if (card.name().trim().equalsIgnoreCase("Family Atomics")) {
                    discordGame.sendMessage("mod-info", "" + faction.getEmoji() + " has Family Atomics.");
                }
            }
        }

        updateStrongholdSkills(discordGame, game);

        ShowCommands.refreshFrontOfShieldInfo(event, discordGame, game);
    }

    public static void updateStrongholdSkills(DiscordGame discordGame, Game game) throws ChannelNotFoundException {
        if (game.hasStrongholdSkills()) {
            for (Faction faction : game.getFactions()) {
                faction.removeResource("strongholdCard");
            }

            List<Territory> strongholds = game.getTerritories().values().stream()
                    .filter(Territory::isStronghold)
                    .filter(t -> t.countActiveFactions(game) == 1)
                    .toList();

            for (Territory stronghold : strongholds) {
                Faction faction = stronghold.getActiveFactions(game).get(0);
                faction.addResource(new StringResource("strongholdCard", stronghold.getTerritoryName()));
                discordGame.sendMessage("turn-summary", MessageFormat.format("{0} controls {1}{2}{1}",
                        stronghold.getActiveFactions(game).get(0).getEmoji(), Emojis.WORM,
                        stronghold.getTerritoryName()));
            }

            discordGame.pushGame();
        }
    }
}
