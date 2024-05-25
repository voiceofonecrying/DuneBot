package controller.buttons;

import constants.Emojis;
import controller.commands.CommandManager;
import exceptions.ChannelNotFoundException;
import controller.DiscordGame;
import model.Game;
import model.factions.EmperorFaction;
import model.factions.Faction;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;

public class RevivalButtons implements Pressable {
    public static void press(ButtonInteractionEvent event, Game game, DiscordGame discordGame) throws ChannelNotFoundException {
        if (!event.getComponentId().startsWith("revive-")) return;

        int revival = Integer.parseInt(event.getComponentId().replace("revive-", ""));
        Faction faction = ButtonManager.getButtonPresser(event, game);
        if (revival == 0) {
            discordGame.queueMessage("You will not revive any extra forces.");
            return;
        }
        if (faction instanceof EmperorFaction && !faction.isStarRevived() && game.getForceFromTanks("Emperor*").getStrength() > 0)
            CommandManager.reviveForces(faction, true, revival - 1, 1, game, discordGame);
        else
            CommandManager.reviveForces(faction, true, revival, 0, game, discordGame);
        discordGame.queueMessage("Your revival request has been submitted to the " + Emojis.BT);
    }
}
