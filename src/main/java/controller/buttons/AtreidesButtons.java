package controller.buttons;

import constants.Emojis;
import controller.DiscordGame;
import controller.commands.ShowCommands;
import exceptions.ChannelNotFoundException;
import exceptions.InvalidGameStateException;
import model.Game;
import model.factions.AtreidesFaction;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;

import java.io.IOException;

public class AtreidesButtons implements Pressable {
    public static void press(ButtonInteractionEvent event, Game game, DiscordGame discordGame) throws ChannelNotFoundException, IOException, InvalidGameStateException {
        // Buttons handled by this class must begin with "atreides"
        // And any button that begins with "atreides" must be handled by this class
        if (event.getComponentId().startsWith("atreides-ally-battle-prescience-")) allyBattlePrescience(event, game, discordGame);
        else if (event.getComponentId().startsWith("atreides-ally-treachery-prescience-")) allyTreacheryCardPrescience(event, game, discordGame);
        else if (event.getComponentId().startsWith("atreides-ht-placement-")) htPlacement(event, game, discordGame);
    }

    private static void allyBattlePrescience(ButtonInteractionEvent event, Game game, DiscordGame discordGame) throws ChannelNotFoundException {
        AtreidesFaction atreides = (AtreidesFaction) ButtonManager.getButtonPresser(event, game);
        String action = event.getComponentId().replace("atreides-ally-battle-prescience-", "");
        atreides.setDenyingAllyBattlePrescience(action.equals("no"));
        ShowCommands.sendAllianceActions(discordGame, game, atreides, true);
        discordGame.queueDeleteMessage();
        discordGame.pushGame();
    }

    private static void allyTreacheryCardPrescience(ButtonInteractionEvent event, Game game, DiscordGame discordGame) throws ChannelNotFoundException {
        AtreidesFaction atreides = (AtreidesFaction) ButtonManager.getButtonPresser(event, game);
        String action = event.getComponentId().replace("atreides-ally-treachery-prescience-", "");
        atreides.setGrantingAllyTreacheryPrescience(action.equals("yes"));
        ShowCommands.sendAllianceActions(discordGame, game, atreides, true);
        discordGame.queueDeleteMessage();
        discordGame.pushGame();
    }

    private static void htPlacement(ButtonInteractionEvent event, Game game, DiscordGame discordGame) throws ChannelNotFoundException, InvalidGameStateException {
        AtreidesFaction atreides = (AtreidesFaction) ButtonManager.getButtonPresser(event, game);
        int turn = Integer.parseInt(event.getComponentId().split("-")[4]);
        if (turn != game.getTurn())
            throw new InvalidGameStateException("It is not turn " + turn);
        String yesOrNo = event.getComponentId().split("-")[3];
        String territoryName = event.getComponentId().split("-")[5];
        if (yesOrNo.equals("yes")) {
            discordGame.queueMessage("You will place 1 " + Emojis.ATREIDES_TROOP + " in " + territoryName);
            atreides.placeForceFromReserves(game, game.getTerritory(territoryName), 1, false);
            game.getTurnSummary().publish(atreides.getEmoji() + " places 1 " + Emojis.ATREIDES_TROOP + " in " + territoryName + " with Caladan High Treshold.");
        } else {
            discordGame.queueMessage("You will not place 1 " + Emojis.ATREIDES_TROOP + " in " + territoryName);
            game.getTurnSummary().publish(atreides.getEmoji() + " does not place 1 " + Emojis.ATREIDES_TROOP + " with Caladan High Treshold.");
        }
        discordGame.queueDeleteMessage();
        discordGame.pushGame();
    }
}
