package controller.buttons;

import constants.Emojis;
import controller.commands.CommandManager;
import exceptions.ChannelNotFoundException;
import model.DiscordGame;
import model.Game;
import model.factions.Faction;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;

public class RevivalButtons implements Pressable {
    public static void press(ButtonInteractionEvent event, Game game, DiscordGame discordGame) throws ChannelNotFoundException {
        if (!event.getComponentId().contains("revive-")) return;

        int revival = Integer.parseInt(event.getComponentId().replace("revive-", ""));
        Faction faction = ButtonManager.getButtonPresser(event, game);
        if (revival == 0) {
            discordGame.queueMessage("You will not revive any extra forces.");
            ButtonManager.deleteAllButtonsInChannel(event.getChannel());
            return;
        }
        CommandManager.revival(false, faction, true, revival, game, discordGame);
        discordGame.queueMessage("Your revival request has been submitted to the " + Emojis.BT);
        ButtonManager.deleteAllButtonsInChannel(event.getChannel());
    }
}
