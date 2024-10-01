package controller.commands;

import constants.Emojis;
import controller.DiscordGame;
import controller.buttons.IxButtons;
import controller.buttons.ShipmentAndMovementButtons;
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
        if (game.isRobberyDiscardOutstanding())
            throw new InvalidGameStateException("Moritani must discard after Robbery before the game can advance.");

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
        } else if (phase == 2 && subPhase == 1) {
            game.startSpiceBlowPhase();
            game.advanceSubPhase();
        } else if (phase == 2 && subPhase == 2) {
            if (game.spiceBlowPhaseNextStep())
                game.advancePhase();
        } else if (phase == 3) {
            game.choamCharity();
            game.advancePhase();
        } else if (phase == 4 && subPhase == 1) {
            if (startBiddingPhase(game)) {
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
                game.getRevival().startRevivingForces(game);
                game.advanceSubPhase();
            }
            game.advanceSubPhase();
        } else if (phase == 5 && subPhase == 2) {
            if (game.getRevival().performPreSteps(game)) {
                game.getRevival().startRevivingForces(game);
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
            game.startBattlePhase();
            game.advancePhase();
        } else if (phase == 8) {
            game.startSpiceHarvest();
            game.advancePhase();
        } else if (phase == 9) {
            game.startMentatPause();
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

    public static boolean startBiddingPhase(Game game) {
        Bidding bidding = game.startBidding();
        return !bidding.isBlackMarketDecisionInProgress();
    }

    public static void bidding(DiscordGame discordGame, Game game) throws ChannelNotFoundException, InvalidGameStateException {
        game.getBidding().auctionNextCard(game);
        discordGame.pushGame();
    }

    public static void startShipmentPhase(DiscordGame discordGame, Game game) throws ChannelNotFoundException {
        if (game.hasGameOption(GameOption.TECH_TOKENS)) TechToken.collectSpice(game, TechToken.AXLOTL_TANKS);

        discordGame.getTurnSummary().queueMessage("**Turn " + game.getTurn() + " Shipment and Movement Phase**");
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
                if (nextCard.discoveryToken() == null)
                    discordGame.getAtreidesChat().queueMessage("You see visions of " + nextCard.name() + " in your future.");
                else
                    discordGame.getAtreidesChat().queueMessage("6 " + Emojis.SPICE + " will appear in " + nextCard.name() + " and destroy any forces and " + Emojis.SPICE + " there. A " + nextCard.discoveryToken() + " will appear in " + nextCard.tokenLocation());
            }
        }
        if (game.hasFaction("BG")) {
            Faction bgFaction = game.getFaction("BG");
            String bgPlayer = bgFaction.getPlayer();
            for (Territory territory : game.getTerritories().values()) {
                territory.flipAdvisorsIfAlone(game);
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

    public static void updateStrongholdSkillsCommand(DiscordGame discordGame, Game game) throws ChannelNotFoundException {
        game.updateStrongholdSkills();
        discordGame.pushGame();
    }
}
