package controller.buttons;

import controller.DiscordGame;
import controller.commands.RunCommands;
import exceptions.ChannelNotFoundException;
import exceptions.InvalidGameStateException;
import model.Game;
import model.factions.BTFaction;
import model.factions.Faction;
import model.factions.HarkonnenFaction;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;

import java.io.IOException;

public class FactionButtons {
    public static void press(ButtonInteractionEvent event, Game game, DiscordGame discordGame) throws ChannelNotFoundException, InvalidGameStateException, IOException {
        if (event.getComponentId().startsWith("traitorselection-")) selectTraitor(event, game, discordGame);
    }

    public static void selectTraitor(ButtonInteractionEvent event, Game game, DiscordGame discordGame) throws ChannelNotFoundException, InvalidGameStateException, IOException {
        Faction faction = ButtonManager.getButtonPresser(event, game);
        String traitorName = event.getComponentId().split("-")[1];
        faction.selectTraitor(traitorName);
        discordGame.queueMessage("You selected " + traitorName);
        if (game.getFactions().stream().anyMatch(f -> !(f instanceof HarkonnenFaction) && !(f instanceof BTFaction) && f.getTraitorHand().size() != 1)) {
            discordGame.pushGame();
        } else {
            game.getModInfo().publish("All traitors have been selected. Game is auto-advancing.");
            RunCommands.advance(discordGame, game);
        }
    }
}
