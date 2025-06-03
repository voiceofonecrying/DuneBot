package controller.buttons;

import constants.Emojis;
import controller.commands.RunCommands;
import controller.commands.SetupCommands;
import controller.commands.ShowCommands;
import exceptions.ChannelNotFoundException;
import controller.DiscordGame;
import exceptions.InvalidGameStateException;
import model.Game;
import model.Territory;
import model.factions.BGFaction;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;

import java.io.IOException;

import static controller.commands.BGCommands.advise;

public class BGButtons implements Pressable {

    public static void press(ButtonInteractionEvent event, Game game, DiscordGame discordGame) throws ChannelNotFoundException, IOException, InvalidGameStateException {
        // Buttons handled by this class must begin with "bg"
        // And any button that begins with "bg" must be handled by this class
        if (event.getComponentId().startsWith("bg-advise-")) advise(discordGame, game, game.getTerritory(event.getComponentId().split("-")[2]), 1);
        else if (event.getComponentId().startsWith("bg-dont-advise-")) dontAdvise(event, discordGame);
        else if (event.getComponentId().startsWith("bg-flip-")) bgFlip(event, game, discordGame);
        else if (event.getComponentId().startsWith("bg-dont-flip-")) dontFlip(event, game, discordGame);
        else if (event.getComponentId().startsWith("bg-ht")) advise(discordGame, game, game.getTerritory("Polar Sink"), 2);
        else if (event.getComponentId().startsWith("bg-ally-voice-")) allyVoice(event, game, discordGame);
        else if (event.getComponentId().startsWith("bg-prediction-faction-")) predictionFaction(event, game, discordGame);
        else if (event.getComponentId().startsWith("bg-prediction-turn-")) predictionTurn(event, game, discordGame);
    }

    private static void bgFlip(ButtonInteractionEvent event, Game game, DiscordGame discordGame) throws ChannelNotFoundException, IOException, InvalidGameStateException {
        discordGame.getTurnSummary().queueMessage(Emojis.BG + " flip in " + event.getComponentId().split("-")[2]);
        Territory territory = game.getTerritory(event.getComponentId().split("-")[2]);
        game.getBGFaction().flipForces(territory);
        discordGame.queueMessageToEphemeral("You will flip.");
        discordGame.queueDeleteMessage();
        if (game.getPhase() == 7 && game.allFactionsHaveMoved()) {
            RunCommands.advance(discordGame, game);
            discordGame.getModInfo().queueMessage("Everyone has taken their turn. Game is auto-advancing to battle phase.");
            return;
        }
        discordGame.pushGame();
    }

    private static void dontFlip(ButtonInteractionEvent event, Game game, DiscordGame discordGame) throws ChannelNotFoundException, InvalidGameStateException, IOException {
        String territoryName = event.getComponentId().split("-")[3];
        game.getBGFaction().dontFlipFighters(game, territoryName);
        discordGame.queueDeleteMessage();
        if (game.getPhase() == 7 && game.allFactionsHaveMoved()) {
            RunCommands.advance(discordGame, game);
            discordGame.getModInfo().queueMessage("Everyone has taken their turn. Game is auto-advancing to battle phase.");
            return;
        }
        discordGame.pushGame();
    }

    private static void dontAdvise(ButtonInteractionEvent event, DiscordGame discordGame) {
        discordGame.queueMessage(Emojis.BG + " Don't advise in " + event.getComponentId().split("-")[3]);
        discordGame.queueDeleteMessage();
    }

    private static void allyVoice(ButtonInteractionEvent event, Game game, DiscordGame discordGame) throws ChannelNotFoundException {
        BGFaction bg = (BGFaction) ButtonManager.getButtonPresser(event, game);
        String action = event.getComponentId().replace("bg-ally-voice-", "");
        bg.setDenyingAllyVoice(action.equals("no"));
        ShowCommands.sendAllianceActions(discordGame, game, bg, true);
        discordGame.queueDeleteMessage();
        discordGame.pushGame();
    }

    private static void predictionFaction(ButtonInteractionEvent event, Game game, DiscordGame discordGame) throws ChannelNotFoundException {
        BGFaction bg = (BGFaction) ButtonManager.getButtonPresser(event, game);
        String factionName = event.getComponentId().replace("bg-prediction-faction-", "");
        bg.presentPredictedTurnChoices(factionName);
        discordGame.queueDeleteMessage();
        discordGame.pushGame();
    }

    private static void predictionTurn(ButtonInteractionEvent event, Game game, DiscordGame discordGame) throws ChannelNotFoundException, InvalidGameStateException, IOException {
        BGFaction bg = (BGFaction) ButtonManager.getButtonPresser(event, game);
        int turn = Integer.parseInt(event.getComponentId().replace("bg-prediction-turn-", ""));
        bg.setPredictionRound(turn);
        SetupCommands.advance(event.getGuild(), discordGame, game);
        discordGame.queueDeleteMessage();
    }
}
