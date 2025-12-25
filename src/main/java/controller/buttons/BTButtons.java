package controller.buttons;

import controller.commands.BTCommands;
import controller.commands.RunCommands;
import exceptions.ChannelNotFoundException;
import exceptions.InvalidGameStateException;
import helpers.MessageHelper;
import controller.DiscordGame;
import model.Game;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.components.buttons.Button;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class BTButtons implements Pressable {

    public static void press(ButtonInteractionEvent event, Game game, DiscordGame discordGame) throws ChannelNotFoundException, IOException, InvalidGameStateException {
        // Buttons handled by this class must begin with "bt"
        // And any button that begins with "bt" must be handled by this class
        if (event.getComponentId().startsWith("bt-revival-rate-set-")) btRevivalRateSet(event, game, discordGame);
        else if (event.getComponentId().equals("bt-revival-rate-no-change")) btRevivalLeaveRatesAsIs(event, game, discordGame);
        else if (event.getComponentId().startsWith("bt-ht-")) handleBTHighThresholdButtons(event, discordGame);
    }

    private static void btRevivalRateSet(ButtonInteractionEvent event, Game game, DiscordGame discordGame) throws ChannelNotFoundException, InvalidGameStateException, IOException {
        String factionName = event.getComponentId().split("-")[4];
        int revivalLimit = Integer.parseInt(event.getComponentId().split("-")[5]);
        BTCommands.setRevivalLimit(event.getMessageChannel(), discordGame, game, factionName, revivalLimit);
        discordGame.queueMessage("Revival limit for " + factionName + " has been set to " + revivalLimit);
    }

    private static void btRevivalLeaveRatesAsIs(ButtonInteractionEvent event, Game game, DiscordGame discordGame) throws ChannelNotFoundException, InvalidGameStateException, IOException {
        game.getBTFaction().leaveRevivalLimitsUnchanged();
        deleteRevivalButtonsInChannel(event.getMessageChannel());
        RunCommands.advance(discordGame, game);
    }

    public static void deleteRevivalButtonsInChannel(MessageChannel channel) {
        List<Message> messages = channel.getHistoryAround(channel.getLatestMessageId(), 50).complete().getRetrievedHistory();
        List<Message> messagesToDelete = new ArrayList<>();
        for (Message message : messages) {
            List<Button> buttons = MessageHelper.getButtons(message);
            for (Button button : buttons) {
                String id = button.getCustomId();
                if (id != null && id.startsWith("bt-revival-rate")) {
                    messagesToDelete.add(message);
                    break;
                }
            }
        }
        for (Message message : messagesToDelete) {
            try {
                message.delete().queue();
            } catch (Exception ignore) {}
        }
    }

    private static void handleBTHighThresholdButtons(ButtonInteractionEvent event, DiscordGame discordGame) throws ChannelNotFoundException, InvalidGameStateException, IOException {
        String action = event.getComponentId().replace("bt-ht-", "");
        MovementButtonActions.handleMovementAction(event, discordGame, action);
    }
}
