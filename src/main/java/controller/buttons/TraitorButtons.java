package controller.buttons;

import controller.DiscordGame;
import controller.commands.SetupCommands;
import exceptions.ChannelNotFoundException;
import exceptions.InvalidGameStateException;
import helpers.MessageHelper;
import model.Battle;
import model.Game;
import model.factions.*;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.components.ActionComponent;

import java.io.IOException;
import java.util.List;

public class TraitorButtons implements Pressable {
    // Buttons handled by this class must begin with "traitor"
    // And any button that begins with "traitor" must be handled by this class
    public static void press(ButtonInteractionEvent event, Game game, DiscordGame discordGame) throws ChannelNotFoundException, InvalidGameStateException, IOException {
        if (event.getComponentId().startsWith("traitor-selection-")) selectTraitor(event, game, discordGame);
        else if (event.getComponentId().startsWith("traitor-call-card-")) selectTraitorForBattle(event, game, discordGame);
        else if (event.getComponentId().startsWith("traitor-call-")) callTraitor(event, game, discordGame);
        else if (event.getComponentId().startsWith("traitor-discard-")) discardTraitor(event, game, discordGame);
        else if (event.getComponentId().startsWith("traitor-reveal-and-discard-")) revealAndDiscardTraitor(event, game, discordGame);
    }

    public static void selectTraitor(ButtonInteractionEvent event, Game game, DiscordGame discordGame) throws ChannelNotFoundException, InvalidGameStateException, IOException {
        Faction faction = ButtonManager.getButtonPresser(event, game);
        String traitorName = event.getComponentId().replace("traitor-selection-", "");
        faction.selectTraitor(traitorName);
        discordGame.queueMessage("You selected " + traitorName);
        if (game.getFactions().stream().anyMatch(f -> !(f instanceof HarkonnenFaction) && !(f instanceof BTFaction) && f.getTraitorHand().size() != 1)) {
            discordGame.pushGame();
        } else {
            game.getModInfo().publish("All traitors have been selected. Game is auto-advancing.");
            SetupCommands.advance(event.getGuild(), discordGame, game);
        }
    }

    private static Battle currentBattle(Game game) throws InvalidGameStateException {
        if (game.getBattles() == null || game.getBattles().getCurrentBattle() == null)
            throw new InvalidGameStateException("There is no current battle.");
        return game.getBattles().getCurrentBattle();
    }

    public static void callTraitor(ButtonInteractionEvent event, Game game, DiscordGame discordGame) throws ChannelNotFoundException, InvalidGameStateException {
        Faction faction = ButtonManager.getButtonPresser(event, game);
        String[] params = event.getComponentId().substring("traitor-call-".length()).split("-", 4);
        if (params.length != 4 || !params[1].equals("turn"))
            throw new InvalidGameStateException("Invalid traitor-call button.");
        String response = params[0];
        if (!List.of("wait", "yes", "no").contains(response))
            throw new InvalidGameStateException("Invalid traitor response: " + response);
        int turn;
        try {
            turn = Integer.parseInt(params[2]);
        } catch (NumberFormatException e) {
            throw new InvalidGameStateException("Invalid turn in traitor-call button.");
        }
        String wholeTerritoryName = params[3];
        Battle battle = currentBattle(game);
        battle.validateTraitorCall(game, faction, turn, wholeTerritoryName);

        // Remove old prompts before Battle publishes replacement choices.
        deleteTraitorCallButtonsInChannel(event.getMessageChannel());
        switch (response) {
            case "wait" -> {
                discordGame.queueMessage("You will wait for battle wheels to decide.");
                battle.mightCallTraitor(game, faction, turn, wholeTerritoryName);
            }
            case "yes" -> battle.willCallTraitor(game, faction, true, turn, wholeTerritoryName);
            case "no" -> battle.willCallTraitor(game, faction, false, turn, wholeTerritoryName);
        }
        discordGame.pushGame();
    }

    private static void selectTraitorForBattle(ButtonInteractionEvent event, Game game, DiscordGame discordGame) throws ChannelNotFoundException, InvalidGameStateException {
        Faction faction = ButtonManager.getButtonPresser(event, game);
        String token = event.getComponentId().substring("traitor-call-card-".length());
        Battle battle = currentBattle(game);
        battle.validateTraitorCardChoice(game, faction, token);
        deleteTraitorCallButtonsInChannel(event.getMessageChannel());
        battle.selectTraitorForBattle(game, faction, token);
        discordGame.pushGame();
    }

    public static void deleteTraitorCallButtonsInChannel(MessageChannel channel) {
        List<Message> messages = channel.getHistoryAround(channel.getLatestMessageId(), 100).complete().getRetrievedHistory();
        List<Message> messagesToDelete = messages.stream().filter(message -> MessageHelper.getButtons(message).stream().map(ActionComponent::getCustomId).anyMatch(id -> id != null && id.startsWith("traitor-call-"))).toList();
        messagesToDelete.forEach(message -> message.delete().complete());
    }

    public static void discardTraitor(ButtonInteractionEvent event, Game game, DiscordGame discordGame) throws ChannelNotFoundException {
        discordGame.queueDeleteMessage();
        Faction faction = ButtonManager.getButtonPresser(event, game);
        String traitorName = event.getComponentId().replace("traitor-discard-", "");
        faction.discardTraitor(traitorName, false);
        discordGame.queueMessage("You discarded " + traitorName);
        discordGame.pushGame();
    }

    public static void revealAndDiscardTraitor(ButtonInteractionEvent event, Game game, DiscordGame discordGame) throws ChannelNotFoundException {
        discordGame.queueDeleteMessage();
        Faction faction = ButtonManager.getButtonPresser(event, game);
        String traitorName = event.getComponentId().replace("traitor-reveal-and-discard-", "");
        faction.discardTraitor(traitorName, true);
        discordGame.queueMessage("You revealed and discarded " + traitorName);
        discordGame.pushGame();
    }
}
