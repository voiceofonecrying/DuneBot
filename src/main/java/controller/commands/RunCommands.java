package controller.commands;

import constants.Emojis;
import controller.DiscordGame;
import controller.buttons.IxButtons;
import controller.channels.TurnSummary;
import enums.UpdateType;
import exceptions.ChannelNotFoundException;
import exceptions.InvalidGameStateException;
import model.*;
import model.factions.*;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;

import java.io.IOException;
import java.util.*;

import static controller.commands.CommandOptions.atreidesKaramad;

public class RunCommands {
    public static List<CommandData> getCommands() {
        List<CommandData> commandData = new ArrayList<>();
        commandData.add(
                Commands.slash("run", "Commands related to playing through phases and turns.").addSubcommands(
                        new SubcommandData("advance", "Continue to the next phase of the game."),
                        new SubcommandData("bidding", "Run a regular bidding for a card").addOptions(atreidesKaramad),
                        new SubcommandData("battle", "Run the next battle"),
                        new SubcommandData("update-stronghold-skills", "Updates the Stronghold skill cards.")
                )
        );

        return commandData;
    }

    public static void runCommand(SlashCommandInteractionEvent event, DiscordGame discordGame, Game game) throws ChannelNotFoundException, InvalidGameStateException, IOException {
        String name = event.getSubcommandName();
        if (name == null) throw new IllegalArgumentException("Invalid command name: null");

        game.modExecutedACommand(event.getUser().getAsMention());
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
            game.getTurnSummary().publish(game.getModRoleMention());
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
            if (game.isIxHMSActionRequired())
                throw new InvalidGameStateException("Ix must choose to move the HMS before advancing.");
            game.startStormPhase();
            if (game.getTurn() == 1) {
                endStormPhase(discordGame, game);
                game.advancePhase();
                game.startSpiceBlowPhase();
                game.advanceSubPhase();
            } else
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
                    discordGame.getModInfo().queueMessage("Bidding phase has ended. Run advance to start revivals. " + game.getModOrRoleMention());
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
            if (game.endRevival(game)) {
                discordGame.getModInfo().queueMessage("Revival phase has ended. Run advance to start shipment and movement. " + game.getModOrRoleMention());
                game.advancePhase();
            }
        } else if (phase == 6) {
            game.startShipmentPhase();
            game.advancePhase();
        } else if (phase == 7) {
            if (!game.allFactionsHaveMoved())
                throw new InvalidGameStateException("Shipment and movement is still in progress.");
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
        if (game.getPhase() == 2) return; // SpiceBlowAndNexus will send its own quote
        if (game.getQuotes().get(phase).isEmpty()) return;
        Collections.shuffle(game.getQuotes().get(phase));
        discordGame.getTurnSummary().queueMessage(game.getQuotes().get(phase).removeFirst());
    }

    public static void endStormPhase(DiscordGame discordGame, Game game) throws ChannelNotFoundException, IOException {
        Map<String, Territory> territories = game.getTerritories();
        if (game.getTurn() == 1) {
            game.endStormPhaseTurn1();
        } else {
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

                territoriesInStorm.stream().filter(Territory::hasEcazAmbassador)
                        .forEach(t ->{
                            String ambassador = t.getEcazAmbassador();
                            t.removeEcazAmbassador();
                            ((EcazFaction) game.getFaction("Ecaz")).addAmbassadorToSupply(ambassador);
                            message.append(Emojis.ECAZ + " ")
                                    .append(ambassador)
                                    .append(" Ambassador removed from ")
                                    .append(t.getTerritoryName())
                                    .append(" and returned to supply.\n");
                        });

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
        if (game.hasFaction("Fremen"))
            game.getFaction("Fremen").getChat().publish("The storm will move " + game.getStormMovement() + " sectors next turn.");
    }

    public static boolean startBiddingPhase(Game game) {
        Bidding bidding = game.startBidding();
        return !bidding.isBlackMarketDecisionInProgress();
    }

    public static void bidding(DiscordGame discordGame, Game game) throws ChannelNotFoundException, InvalidGameStateException {
        boolean prescienceBlocked = discordGame.optional(atreidesKaramad) != null && discordGame.required(atreidesKaramad).getAsBoolean();
        game.getBidding().auctionNextCard(game, prescienceBlocked);
        discordGame.pushGame();
    }

    public static void updateStrongholdSkillsCommand(DiscordGame discordGame, Game game) throws ChannelNotFoundException {
        game.updateStrongholdSkills();
        discordGame.pushGame();
    }
}
