package controller.buttons;

import constants.Emojis;
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
        if (event.getComponentId().startsWith("revive-emp-ally-")) reviveEmperorAlly(event, discordGame, game);
        else if (event.getComponentId().startsWith("revive-")) revive(event, discordGame, game);
        else if (event.getComponentId().startsWith("revive*-")) reviveCyborgs(event, discordGame, game);
        else if (event.getComponentId().startsWith("recruits-yes")) playRecruits(event, discordGame, game);
        else if (event.getComponentId().startsWith("recruits-no")) dontPlayRecruits(discordGame, game);
    }

    private static void reviveEmperorAlly(ButtonInteractionEvent event, DiscordGame discordGame, Game game) throws ChannelNotFoundException, InvalidGameStateException {
        int numForces = Integer.parseInt(event.getComponentId().replace("revive-emp-ally-", ""));
        EmperorFaction emperor = (EmperorFaction) game.getFaction("Emperor");
        if (numForces == 0) {
            discordGame.queueMessage("You will not revive any extra forces for your ally.");
        } else {
            emperor.reviveAllyForces(numForces);
            discordGame.queueMessage("Your revival request for your ally has been submitted to the " + Emojis.BT);
            discordGame.pushGame();
        }
    }

    private static void revive(ButtonInteractionEvent event, DiscordGame discordGame, Game game) throws ChannelNotFoundException {
        int numForces = Integer.parseInt(event.getComponentId().replace("revive-", ""));
        Faction faction = ButtonManager.getButtonPresser(event, game);
        faction.reviveForces(true, numForces);
        if (numForces == 0) {
            discordGame.queueMessage("You will not revive any extra forces.");
        } else {
            discordGame.queueMessage("Your revival request has been submitted to the " + Emojis.BT);
            discordGame.pushGame();
        }
    }

    private static void reviveCyborgs(ButtonInteractionEvent event, DiscordGame discordGame, Game game) throws ChannelNotFoundException, InvalidGameStateException {
        int revival = Integer.parseInt(event.getComponentId().split("-")[1]);
        int freeRevived = Integer.parseInt(event.getComponentId().split("-")[2]);
        Faction faction = ButtonManager.getButtonPresser(event, game);
        if (revival == 0) {
            discordGame.queueMessage("You will not revive any extra " + Emojis.IX_CYBORG + ".");
        } else {
            game.reviveForces(faction, true, 0, revival);
            discordGame.queueMessage("You will revive " + revival + " " + Emojis.IX_CYBORG + ".");
            discordGame.pushGame();
        }
        game.getRevival().setCyborgRevivalComplete(true);
        faction.presentPaidRevivalChoices(freeRevived + revival);
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
