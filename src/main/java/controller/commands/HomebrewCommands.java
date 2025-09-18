package controller.commands;

import controller.DiscordGame;
import exceptions.ChannelNotFoundException;
import model.Game;
import model.factions.Faction;
import model.factions.HomebrewFaction;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.concrete.Category;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static controller.commands.CommandOptions.message;

public class HomebrewCommands {
    public static List<CommandData> getCommands() {
        List<CommandData> commandData = new ArrayList<>();
        commandData.add(Commands.slash("homebrew", "Commands related to Homebrew Factions.").addSubcommands(
                new SubcommandData("set-homeworld-image", "Set the message link for homebrew homeworld image").addOptions(message),
                new SubcommandData("set-image-test", "Set the message link for homebrew image test").addOptions(message),
                new SubcommandData("image-test", "Write image diagnostics to mod-info")
        ));
        return commandData;
    }

    public static void runCommand(SlashCommandInteractionEvent event, DiscordGame discordGame, Game game) throws ChannelNotFoundException {
        String name = event.getSubcommandName();
        if (name == null) throw new IllegalArgumentException("Invalid command name: null");

        switch (name) {
            case "set-homeworld-image" -> setHomeworldImage(discordGame, game);
            case "set-image-test" -> setImageTest(discordGame, game);
            case "image-test" -> imageTest(discordGame, game);
        }
    }

    public static void setHomeworldImage(DiscordGame discordGame, Game game) throws ChannelNotFoundException {
        String messageLink = discordGame.required(message).getAsString();
        Faction faction = game.getFactions().stream().filter(f -> f instanceof HomebrewFaction).findFirst().orElse(null);
        if (faction == null) {
            game.getModInfo().publish("No homebrew factions in the game.");
            return;
        }
        HomebrewFaction homebrewFaction = (HomebrewFaction) faction;
        homebrewFaction.setHomeworldImageMessage(messageLink);
        discordGame.pushGame();
    }

    public static void setImageTest(DiscordGame discordGame, Game game) throws ChannelNotFoundException {
        String messageLink = discordGame.required(message).getAsString();
        Faction faction = game.getFactions().stream().filter(f -> f instanceof HomebrewFaction).findFirst().orElse(null);
        if (faction == null) {
            game.getModInfo().publish("No homebrew factions in the game.");
            return;
        }
        HomebrewFaction homebrewFaction = (HomebrewFaction) faction;
        homebrewFaction.setHomeworldImageLinkTest(messageLink);
        discordGame.pushGame();
    }

    public static void imageTest(DiscordGame discordGame, Game game) {
        Faction faction = game.getFactions().stream().filter(f -> f instanceof HomebrewFaction).findFirst().orElse(null);
        if (faction == null) {
            game.getModLedger().publish("No homebrew factions in the game.");
            return;
        }
        HomebrewFaction homebrewFaction = (HomebrewFaction) faction;
        String homebrewImageLinkTest = homebrewFaction.getHomeworldImageLinkTest();
        if (homebrewImageLinkTest == null) {
            game.getModLedger().publish("No homebrew image link test value.");
            return;
        }
        try {
            int numAttachments;
//            game.getModLedger().publish(ShowCommands.getHomeworldFactionImageUrl(discordGame, homebrewImageLinkTest));
            String serverId = homebrewImageLinkTest.replace("https://discord.com/channels/", "");
            int channelIdStart = serverId.indexOf("/") + 1;
            int channelIdEnd = serverId.indexOf("/", channelIdStart);
            String channelId = serverId.substring(channelIdStart, channelIdEnd);
            int messageIdStart = channelIdEnd + 1;
            String messageId = serverId.substring(messageIdStart);
            Category category = discordGame.getGameCategory();
            TextChannel channel = category.getTextChannels().stream().filter(c -> c.getId().equals(channelId)).findFirst().orElseThrow();
            Message msg = channel.retrieveMessageById(messageId).complete();
            numAttachments = msg.getAttachments().size();
            game.getModLedger().publish("Message link: " + homebrewImageLinkTest
                    + "\nChannel ID: " + channelId
                    + "\nMessage ID: " + messageId
                    + "\nNum attachments: " + numAttachments
            );
        } catch (Exception e) {
            game.getModLedger().publish(Arrays.toString(e.getStackTrace()));
        }

        try {
            game.getModLedger().publish(ShowCommands.getHomebrewFactionImageUrl(discordGame, homebrewImageLinkTest));
        } catch (Exception e) {
            game.getModLedger().publish(Arrays.toString(e.getStackTrace()));
        }
    }
}
