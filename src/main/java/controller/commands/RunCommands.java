package controller.commands;

import constants.Emojis;
import controller.DiscordGame;
import controller.buttons.IxButtons;
import controller.buttons.ShipmentAndMovementButtons;
import controller.channels.DiscordChannel;
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

import java.io.IOException;
import java.text.MessageFormat;
import java.util.*;

public class RunCommands {
    public static List<CommandData> getCommands() {
        List<CommandData> commandData = new ArrayList<>();
        commandData.add(
                Commands.slash("run", "Commands related to playing through phases and turns.").addSubcommands(
                        new SubcommandData("advance", "Continue to the next phase of the game."),
                        new SubcommandData("bidding", "Run a regular bidding for a card"),
                        new SubcommandData("battle", "Run the next battle"),
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
            case "battle" -> BattleCommands.setupBattle(discordGame, game);
            case "update-stronghold-skills" -> updateStrongholdSkillsCommand(discordGame, game);
        }
    }

    public static void advance(DiscordGame discordGame, Game game) throws ChannelNotFoundException, IOException, InvalidGameStateException {
        if (game.getTurn() == 0) {
            discordGame.getModInfo().queueMessage("Please complete setup first.");
            return;
        }

        if (game.getPhase() == 10) {
            game.endMentatPause();
            game.advanceTurn();
            game.setTurnSummary(discordGame.getTurnSummary());
            game.getTurnSummary().publish(game.getModOrRoleMention());
            game.getTurnSummary().publish(game.getGameRoleMention());
            game.updateStrongholdSkills();
        }

        int phase = game.getPhase();
        int subPhase = game.getSubPhase();

        sendQuote(discordGame, game, phase);

        boolean ixInGame = game.hasFaction("Ix");
        boolean hmsUnderStorm = game.getTerritories().values().stream().anyMatch(territory -> territory.getForces().stream().filter(force -> force.getName().equals("Hidden Mobile Stronghold")).anyMatch(force -> territory.getSector() == game.getStorm()));
        for (Territory territory : game.getTerritories().values()) {
            for (Force force : territory.getForces()) {
                if (force.getName().equals("Hidden Mobile Stronghold")) {
                    if (territory.getSector() == game.getStorm()) hmsUnderStorm = true;
                }
            }
        }
        boolean ixCanMoveHMS = game.ixCanMoveHMS();
        boolean ixHMSActionRequired = game.isIxHMSActionRequired();
        if (phase == 1 && subPhase == 1) {
            if (game.getTurn() == 1 || !ixCanMoveHMS || hmsUnderStorm) {
                game.advanceSubPhase();
                subPhase = game.getSubPhase();
                if (ixInGame && !ixCanMoveHMS) {
                    discordGame.getTurnSummary().queueMessage(Emojis.IX + " do not control the HMS. It cannot be moved.");
                } else if (hmsUnderStorm) {
                    discordGame.getTurnSummary().queueMessage("The HMS is under the storm and cannot be moved.");
                }
            } else {
                ixHMSActionRequired = true;
            }
        }
        game.setIxHMSActionRequired(ixHMSActionRequired);

        if (phase == 1 && subPhase == 1) {
            IxButtons.hmsSubPhase(discordGame, game);
            game.advanceSubPhase();
        } else if (phase == 1 && subPhase == 2) {
            if (game.isIxHMSActionRequired()) throw new InvalidGameStateException("Ix must choose to move the HMS before advancing.");
            startStormPhase(discordGame, game);
            game.advanceSubPhase();
        } else if (phase == 1 && subPhase == 3) {
            endStormPhase(discordGame, game);
            game.advancePhase();
        } else if (phase == 2) {
            spiceBlow(discordGame, game);
            game.advancePhase();
        } else if (phase == 3) {
            game.choamCharity();
            game.advancePhase();
        } else if (phase == 4 && subPhase == 1) {
            if (startBiddingPhase(discordGame, game)) {
                game.advanceSubPhase();
                game.getBidding().cardCountsInBiddingPhase(game);
            }
            game.advanceSubPhase();
        } else if (phase == 4 && subPhase == 2) {
            game.getBidding().cardCountsInBiddingPhase(game);
            game.advanceSubPhase();
        } else if (phase == 4 && subPhase == 3) {
            if (game.getBidding().finishBiddingPhase(game)) {
                game.advancePhase();
                game.startRevival();
                if (!game.getRevival().isRecruitsDecisionNeeded())
                    discordGame.getModInfo().queueMessage("Bidding phase ended. Run advance to start revivals.");
            }
        } else if (phase == 5 && subPhase == 1) {
            if (game.getRevival().performPreSteps(game)) {
                startRevivingForces(discordGame, game);
                game.advanceSubPhase();
            }
            game.advanceSubPhase();
        } else if (phase == 5 && subPhase == 2) {
            if (game.getRevival().performPreSteps(game)) {
                startRevivingForces(discordGame, game);
                game.advanceSubPhase();
            }
        } else if (phase == 5 && subPhase == 3) {
            game.endRevival();
            discordGame.getModInfo().queueMessage("Revival phase has ended. Run advance to start shipment and movement.");
            game.advancePhase();
        } else if (phase == 6) {
            startShipmentPhase(discordGame, game);
            game.advancePhase();
        } else if (phase == 7) {
            startBattlePhase(game);
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
        discordGame.getTurnSummary().queueMessage(game.getQuotes().get(phase).removeFirst());
    }

    public static void startStormPhase(DiscordGame discordGame, Game game) throws ChannelNotFoundException {
        game.startStormPhase();

        if (game.getTerritories().get("Ecological Testing Station") != null && game.getTerritory("Ecological Testing Station").countActiveFactions() == 1) {
            Faction faction = game.getTerritory("Ecological Testing Station").getActiveFactions(game).getFirst();
            discordGame.getFactionChat(faction.getName()).queueMessage("What have the ecologists at the testing station discovered about the storm movement? " + faction.getPlayer(),
                    List.of(Button.primary("storm-1", "-1"), Button.secondary("storm0", "0"), Button.primary("storm1", "+1")));
        }
        if (game.getTurn() == 1) {
            discordGame.getModInfo().queueMessage("Run advance to complete turn 1 storm phase.");
        }
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
                if (territoriesInStorm.stream().anyMatch(Territory::hasRicheseNoField)) {
                    RicheseFaction richese = (RicheseFaction) game.getFaction("Richese");
                    richese.revealNoField(game);
                }

                List<Territory> territoriesWithTroops = territoriesInStorm.stream()
                        .filter(t -> t.countFactions() > 0).toList();

                List<Territory> territoriesWithSpice = territoriesInStorm.stream()
                        .filter(t -> t.getSpice() > 0).toList();

                for (Territory territory : territoriesWithTroops) {
                    message.append(territory.stormTroops(game));
                }

                territoriesWithSpice.stream().map(Territory::stormRemoveSpice).forEach(message::append);
            }

            if (!message.isEmpty()) {
                turnSummary.queueMessage(message.toString());
            }
            game.setUpdated(UpdateType.MAP);
        }
        ShowCommands.showBoard(discordGame, game);

        int stormMovement;
        ArrayList<Integer> stormDeck = game.getStormDeck();
        stormMovement = stormDeck == null ? new Random().nextInt(6) + 1 : stormDeck.get(new Random().nextInt(stormDeck.size()));
        game.setStormMovement(stormMovement);
        if (game.hasFaction("Fremen")) {
            discordGame.getFremenChat().queueMessage("The storm will move " + game.getStormMovement() + " sectors next turn.");
        }
    }

    public static void spiceBlow(DiscordGame discordGame, Game game) throws ChannelNotFoundException {
        discordGame.getTurnSummary().queueMessage("Turn " + game.getTurn() + " Spice Blow Phase:");
        game.setPhaseForWhispers("Turn " + game.getTurn() + " Spice Blow Phase\n");
    }

    public static boolean startBiddingPhase(DiscordGame discordGame, Game game) throws ChannelNotFoundException {
        discordGame.getTurnSummary().queueMessage("Turn " + game.getTurn() + " Bidding Phase:");
        game.setPhaseForWhispers("Turn " + game.getTurn() + " Bidding Phase\n");
        game.startBidding();
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

    public static void bidding(DiscordGame discordGame, Game game) throws ChannelNotFoundException, InvalidGameStateException {
        game.getBidding().bidding(game);
        discordGame.pushGame();
    }

    public static void startRevivingForces(DiscordGame discordGame, Game game) throws ChannelNotFoundException, InvalidGameStateException {
        Revival revival = game.getRevival();
        if (revival.isRecruitsDecisionNeeded())
            throw new InvalidGameStateException(revival.getRecruitsHolder() + " must decide if they will play recruits before the game can be advanced.");
        boolean btWasHighThreshold = true;
        try {
            BTFaction bt = (BTFaction) game.getFaction("BT");
            List<String> factionsNeedingLimits = bt.getFactionsNeedingRevivalLimit();
            if (!factionsNeedingLimits.isEmpty()) {
                String names = String.join(", ", factionsNeedingLimits);
                throw new InvalidGameStateException("BT must set revival limits for the following factions before the game can be advanced.\n" + names);
            }
            btWasHighThreshold = !game.hasGameOption(GameOption.HOMEWORLDS) || bt.isHighThreshold();
        } catch (IllegalArgumentException e) {
            // BT are not in the game
        }
        TurnSummary turnSummary = discordGame.getTurnSummary();
        turnSummary.queueMessage("Turn " + game.getTurn() + " Revival Phase:");
        game.setPhaseForWhispers("Turn " + game.getTurn() + " Revival Phase\n");
        List<Faction> factions = game.getFactions();
        StringBuilder message = new StringBuilder();
        boolean nonBTRevival = false;
        int factionsWithRevivals = 0;
        for (Faction faction : factions) {
            int numFreeRevived = faction.performFreeRevivals();
            if (numFreeRevived > 0) {
                factionsWithRevivals++;
                if (!(faction instanceof BTFaction))
                    nonBTRevival = true;
            }
            faction.presentPaidRevivalChoices(numFreeRevived);
        }

        if (btWasHighThreshold && factionsWithRevivals > 0) {
            Faction btFaction = game.getFaction("BT");
            message.append(btFaction.getEmoji())
                    .append(" receives ")
                    .append(factionsWithRevivals)
                    .append(Emojis.SPICE)
                    .append(" from free revivals\n");
            btFaction.addSpice(factionsWithRevivals, "for free revivals");
        }

        if (!message.isEmpty()) {
            turnSummary.queueMessage(message.toString());
        }
        if (nonBTRevival && game.hasGameOption(GameOption.TECH_TOKENS))
            TechToken.addSpice(game, TechToken.AXLOTL_TANKS);

        if (game.hasFaction("Ecaz")) {
            EcazFaction ecaz = (EcazFaction) game.getFaction("Ecaz");
            ecaz.sendAmbassadorLocationMessage(1);
        }

        game.setUpdated(UpdateType.MAP);
    }

    public static void startShipmentPhase(DiscordGame discordGame, Game game) throws ChannelNotFoundException {
        if (game.hasGameOption(GameOption.TECH_TOKENS)) TechToken.collectSpice(game, TechToken.AXLOTL_TANKS);

        discordGame.getTurnSummary().queueMessage("Turn " + game.getTurn() + " Shipment and Movement Phase:");
        game.setPhaseForWhispers("Turn " + game.getTurn() + " Shipment and Movement Phase\n");
        game.getTurnOrder().clear();
        for (Faction faction : game.getFactions()) {
            game.getTurnOrder().add(faction.getName());
            faction.getShipment().clear();
            faction.getMovement().clear();
            faction.getShipment().setShipped(false);
            faction.getMovement().setMoved(false);
        }
        while (game.getFactionTurnIndex(game.getTurnOrder().getFirst()) != 0)
            game.getTurnOrder().addFirst(game.getTurnOrder().pollLast());
        game.getTurnOrder().removeIf(name -> name.equals("Guild"));
        List<Faction> factions = game.getFactionsWithTreacheryCard("Juice of Sapho");
        Faction saphoFaction = null;
        if (!factions.isEmpty()) {
            saphoFaction = factions.getFirst();
            saphoFaction.getShipment().setMayPlaySapho(true);
        }
        if (saphoFaction != null &&
                (!game.getTurnOrder().getFirst().equals(factions.getFirst().getName()) || game.hasFaction("Guild"))) {
            String message = "Do you want to play Juice of Sapho to ship and move first? " + saphoFaction.getPlayer();
            if (!game.getTurnOrder().getLast().equals(saphoFaction.getName()))
                message += "\nIf not, you will have the option to play it to go last on your turn.";
            else if (game.hasFaction("Guild"))
                message += "\nIf not, you will have the option to play it to go last if " + Emojis.GUILD + " defers to you.";
            List<Button> buttons = List.of(
                    Button.primary("juice-of-sapho-first", "Yes, go first"),
                    Button.secondary("juice-of-sapho-don't-play", "No"));
            discordGame.getFactionChat(saphoFaction).queueMessage(message, buttons);
            game.getTurnOrder().addFirst("juice-of-sapho-hold");
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
            Faction bgFaction = game.getFaction("BG");
            String bgPlayer = bgFaction.getPlayer();
            for (Territory territory : game.getTerritories().values()) {
                if (territory.getTerritoryName().equals("Polar Sink")) continue;
                StringBuilder message = new StringBuilder();
                if (territory.getForceStrength("Advisor") > 0) {
                    String bgAllyName = bgFaction.getAlly();
                    if (!bgAllyName.isEmpty()) {
                        Faction bgAlly = game.getFaction(bgAllyName);
                        if (territory.getTotalForceCount(bgAlly) > 0 && !bgAllyName.equals("Ecaz"))
                            continue;
                    }
                    if (territory.getSector() == game.getStorm()) {
                        discordGame.queueMessage("game-actions", territory.getTerritoryName() + " is under the storm. Ask the mod to flip for you if the game allows it. " + bgPlayer);
                        continue;
                    }
                    discordGame.getTurnSummary().queueMessage(game.getFaction("BG").getEmoji() + " to decide whether to flip their advisors in " + territory.getTerritoryName());
                    discordGame.getBGChat().queueMessage(new MessageCreateBuilder().setContent(
                                    message.append("Will you flip to fighters in ").append(territory.getTerritoryName()).append("? ").append(bgPlayer).toString())
                            .addActionRow(Button.primary("bg-flip-" + territory.getTerritoryName(), "Flip"), Button.secondary("bg-dont-flip-" + territory.getTerritoryName(), "Don't flip")));
                }
            }
        }
        game.setUpdated(UpdateType.MAP_ALSO_IN_TURN_SUMMARY);
    }

    public static void startBattlePhase(Game game) {
        game.startBattlePhase();
        game.setUpdated(UpdateType.MAP_ALSO_IN_TURN_SUMMARY);
    }

    public static void startSpiceHarvest(DiscordGame discordGame, Game game) throws ChannelNotFoundException, InvalidGameStateException {
        game.endBattlePhase();
        TurnSummary turnSummary = discordGame.getTurnSummary();
        if (game.hasFaction("Moritani")) {
            MoritaniFaction moritani = (MoritaniFaction) game.getFaction("Moritani");
            if (moritani.getLeaders().removeIf(leader -> leader.getName().equals("Duke Vidal")))
                turnSummary.queueMessage("Duke Vidal has left the " + Emojis.MORITANI + " services... for now.");
            if (game.hasGameOption(GameOption.HOMEWORLDS) && moritani.isHighThreshold())
                moritani.sendTerrorTokenHighThresholdMessage();
        }
        turnSummary.queueMessage("Turn " + game.getTurn() + " Spice Harvest Phase:");
        game.setPhaseForWhispers("Turn " + game.getTurn() + " Spice Harvest Phase\n");
        Map<String, Territory> territories = game.getTerritories();
        for (Territory territory : territories.values()) {
            if (territory.countActiveFactions() == 0 && territory.hasForce("Advisor")) {
                BGFaction bg = (BGFaction) game.getFaction("BG");
                bg.flipForces(territory);
                turnSummary.queueMessage("Advisors are alone in " + territory.getTerritoryName() + " and have flipped to fighters.");
            }
        }

        for (Faction faction : game.getFactions()) {
            faction.setHasMiningEquipment(false);
            if (territories.get("Arrakeen").hasActiveFaction(faction)) {
                faction.addSpice(2, "for Arrakeen");
                turnSummary.queueMessage(faction.getEmoji() + " collects 2 " + Emojis.SPICE + " from Arrakeen");
                faction.setHasMiningEquipment(true);
            }
            if (territories.get("Carthag").hasActiveFaction(faction)) {
                faction.addSpice(2, "for Carthag");
                turnSummary.queueMessage(faction.getEmoji() + " collects 2 " + Emojis.SPICE + " from Carthag");
                faction.setHasMiningEquipment(true);
            }
            if (territories.get("Tuek's Sietch").hasActiveFaction(faction)) {
                turnSummary.queueMessage(faction.getEmoji() + " collects 1 " + Emojis.SPICE + " from Tuek's Sietch");
                faction.addSpice(1, "for Tuek's Sietch");
            }
            if (territories.get("Cistern") != null && territories.get("Cistern").hasActiveFaction(faction)) {
                faction.addSpice(2, "for Cistern");
                turnSummary.queueMessage(faction.getEmoji() + " collects 2 " + Emojis.SPICE + " from Cistern");
                faction.setHasMiningEquipment(true);
            }
        }

        for (Faction faction : game.getFactions()) {
            Territory homeworld = game.getTerritory(faction.getHomeworld());
            if (homeworld.getForces().stream().anyMatch(force -> !force.getFactionName().equals(faction.getName()))) {
                Faction occupyingFaction = homeworld.getActiveFactions(game).getFirst();
                if (game.hasGameOption(GameOption.HOMEWORLDS) && occupyingFaction instanceof HarkonnenFaction harkonnenFaction && occupyingFaction.isHighThreshold() && !harkonnenFaction.hasTriggeredHT()) {
                    faction.addSpice(2, "for High Threshold advantage");
                    harkonnenFaction.setTriggeredHT(true);
                }
                turnSummary.queueMessage(occupyingFaction.getEmoji() + " collects " + faction.getOccupiedIncome() + " " + Emojis.SPICE + " for occupying " + faction.getHomeworld());
                occupyingFaction.addSpice(faction.getOccupiedIncome(), "for occupying " + faction.getHomeworld());
            }
        }

        boolean altSpiceProductionTriggered = false;
        Territory orgiz = territories.get("Orgiz Processing Station");
        boolean orgizActive = orgiz != null && orgiz.getActiveFactions(game).size() == 1;
        for (Territory territory : territories.values()) {
            if (territory.getSpice() == 0 || territory.countActiveFactions() == 0) continue;
            if (orgizActive) {
                Faction orgizFaction = orgiz.getActiveFactions(game).getFirst();
                orgizFaction.addSpice(1, "for Orgiz Processing Station");
                territory.setSpice(territory.getSpice() - 1);
                turnSummary.queueMessage(orgizFaction.getEmoji() + " collects 1 " + Emojis.SPICE + " from " + territory.getTerritoryName() + " with Orgiz Processing Station");
            }

            Faction faction = territory.getActiveFactions(game).getFirst();
            int spice = faction.getSpiceCollectedFromTerritory(territory);
            if (faction instanceof FremenFaction && faction.isHomeworldOccupied()) {
                faction.getOccupier().addSpice(Math.floorDiv(spice, 2),
                        "From " + Emojis.FREMEN + " " + Emojis.SPICE + " collection (occupied advantage).");
                turnSummary.queueMessage(game.getFaction(faction.getName()).getEmoji() +
                        " collects " + Math.floorDiv(spice, 2) + " " + Emojis.SPICE + " from " + Emojis.FREMEN + " collection at " + territory.getTerritoryName());
                spice = Math.ceilDiv(spice, 2);
            }
            faction.addSpice(spice, "for Spice Blow");
            territory.setSpice(territory.getSpice() - spice);

            if (game.hasGameOption(GameOption.TECH_TOKENS) && game.hasGameOption(GameOption.ALTERNATE_SPICE_PRODUCTION)
                    && (!(faction instanceof FremenFaction) || game.hasGameOption(GameOption.FREMEN_TRIGGER_ALTERNATE_SPICE_PRODUCTION)))
                altSpiceProductionTriggered = true;
            turnSummary.queueMessage(game.getFaction(faction.getName()).getEmoji() +
                    " collects " + spice + " " + Emojis.SPICE + " from " + territory.getTerritoryName());
            if (game.hasGameOption(GameOption.HOMEWORLDS) && faction instanceof HarkonnenFaction harkonnenFaction && faction.isHighThreshold() && !harkonnenFaction.hasTriggeredHT()) {
                faction.addSpice(2, "for High Threshold advantage");
                harkonnenFaction.setTriggeredHT(true);
            }
        }
        if (game.hasFaction("Harkonnen")) ((HarkonnenFaction)game.getFaction("Harkonnen")).setTriggeredHT(false);

        for (Territory territory : territories.values()) {
            if (territory.getDiscoveryToken() == null || territory.countActiveFactions() == 0 || territory.isDiscovered()) continue;
            Faction faction = territory.getActiveFactions(game).getFirst();
            List<Button> buttons = new LinkedList<>();
            buttons.add(Button.primary("reveal-discovery-token-" + territory.getTerritoryName(), "Yes"));
            buttons.add(Button.danger("don't-reveal-discovery-token", "No"));
            discordGame.getFactionChat(faction.getName()).queueMessage(faction.getPlayer() + "Would you like to reveal the discovery token at " + territory.getTerritoryName() + "? (" + territory.getDiscoveryToken() + ")", buttons);
        }

        if (altSpiceProductionTriggered) {
            TechToken.addSpice(game, TechToken.SPICE_PRODUCTION);
            TechToken.collectSpice(game, TechToken.SPICE_PRODUCTION);
        }

        game.setUpdated(UpdateType.MAP);
    }

    public static void startMentatPause(DiscordGame discordGame, Game game) throws ChannelNotFoundException {
        TurnSummary turnSummary = discordGame.getTurnSummary();
        turnSummary.publish("Turn " + game.getTurn() + " Mentat Pause Phase:");
        game.setPhaseForWhispers("Turn " + game.getTurn() + " Mentat Pause Phase\n");
        game.startMentatPause();
        for (Faction faction : game.getFactions()) {
            if (faction.getFrontOfShieldSpice() > 0) {
                turnSummary.queueMessage(faction.getEmoji() + " collects " +
                        faction.getFrontOfShieldSpice() + " " + Emojis.SPICE + " from front of shield.");
                faction.addSpice(faction.getFrontOfShieldSpice(), "front of shield");
                faction.setFrontOfShieldSpice(0);
            }
            for (TreacheryCard card : faction.getTreacheryHand()) {
                if (card.name().trim().equalsIgnoreCase("Weather Control")) {
                    discordGame.getModInfo().queueMessage(faction.getEmoji() + " has Weather Control.");
                } else if (card.name().trim().equalsIgnoreCase("Family Atomics")) {
                    discordGame.getModInfo().queueMessage(faction.getEmoji() + " has Family Atomics.");
                }
            }
            if (game.isExtortionTokenRevealed()) {
                if (!(faction instanceof MoritaniFaction)) {
                    DiscordChannel factionChat = discordGame.getFactionChat(faction);
                    if (faction.getSpice() >= 3) {
                        List<Button> buttons = new ArrayList<>();
                        buttons.add(Button.primary("extortion-pay", "Yes"));
                        buttons.add(Button.primary("extortion-dont-pay", "No"));
                        factionChat.queueMessage(MessageFormat.format(
                                "Will you pay {0} 3 {1} to remove the Extortion token from the game? " + faction.getPlayer(),
                                Emojis.MORITANI, Emojis.SPICE), buttons
                        );
                    } else {
                        factionChat.queueMessage("You do not have enough spice to pay Extortion.");
                    }
                }
            }
        }
        if (game.isExtortionTokenRevealed())
            turnSummary.queueMessage(MessageFormat.format(
                    "The Extortion token will be returned to {0} unless someone pays 3 {1} to remove it from the game.",
                    Emojis.MORITANI, Emojis.SPICE
            ));
        if (game.hasFaction("Moritani")) {
            MoritaniFaction moritani = (MoritaniFaction) game.getFaction("Moritani");
            moritani.sendTerrorTokenLocationMessage();
        }
    }

    public static void updateStrongholdSkillsCommand(DiscordGame discordGame, Game game) throws ChannelNotFoundException {
        game.updateStrongholdSkills();
        discordGame.pushGame();
    }
}
