package controller.buttons;

import constants.Emojis;
import controller.commands.CommandManager;
import controller.commands.RunCommands;
import exceptions.ChannelNotFoundException;
import controller.DiscordGame;
import exceptions.InvalidGameStateException;
import model.Game;
import model.factions.EmperorFaction;
import model.factions.Faction;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;

import java.io.IOException;

public class RevivalButtons implements Pressable {
    public static void press(ButtonInteractionEvent event, Game game, DiscordGame discordGame) throws ChannelNotFoundException, InvalidGameStateException, IOException {
        if (event.getComponentId().startsWith("recruits-yes")) playRecruits(event, discordGame, game);
        else if (event.getComponentId().startsWith("recruits-no")) dontPlayRecruits(discordGame, game);

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

    private static void playRecruits(ButtonInteractionEvent event, DiscordGame discordGame, Game game) throws InvalidGameStateException, ChannelNotFoundException, IOException {
        discordGame.queueMessage("You will play Recruits to raise all revival limits to 7.");
        game.getRevival().playRecruits();

        Faction faction = ButtonManager.getButtonPresser(event, game);
        faction.removeTreacheryCard("Recruits");
        // Maybe do the following in game.setRecruitsInPlay(true);
        game.getTurnSummary().publish(faction.getName() + " plays Recruits to raise all revival limits to 7!");
        try {
            Faction bt = game.getFaction("BT");
            bt.getChat().publish("Recruits has been played. All revival limits are set to 7.");
        } catch (IllegalArgumentException ignored) {}

        discordGame.queueDeleteMessage();
        RunCommands.advance(discordGame, game);
    }

    private static void dontPlayRecruits(DiscordGame discordGame, Game game) throws InvalidGameStateException, ChannelNotFoundException, IOException {
        discordGame.queueMessage("You will not play Recruits.");
        discordGame.queueDeleteMessage();
        game.getRevival().declineRecruits();
        RunCommands.advance(discordGame, game);
    }
}
