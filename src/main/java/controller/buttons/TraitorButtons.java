package controller.buttons;

import controller.DiscordGame;
import controller.commands.SetupCommands;
import exceptions.ChannelNotFoundException;
import exceptions.InvalidGameStateException;
import model.Battle;
import model.Game;
import model.factions.*;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.components.ActionComponent;

import java.io.IOException;
import java.util.List;

public class TraitorButtons {
    // Buttons handled by this class must begin with "traitor"
    // And any button that begins with "traitor" must be handled by this class
    public static void press(ButtonInteractionEvent event, Game game, DiscordGame discordGame) throws ChannelNotFoundException, InvalidGameStateException, IOException {
        if (event.getComponentId().startsWith("traitor-selection-")) selectTraitor(event, game, discordGame);
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

    public static void callTraitor(ButtonInteractionEvent event, Game game, DiscordGame discordGame) throws ChannelNotFoundException, InvalidGameStateException {
        Faction faction = ButtonManager.getButtonPresser(event, game);
        String[] params = event.getComponentId().replace("traitor-call-", "").split("-");
        String response = params[0];
        int turn = Integer.parseInt(params[2]);
        String wholeTerritoryName = params[3];
        discordGame.queueDeleteMessage();
        Battle battle = game.getBattles().getCurrentBattle();
        switch (response) {
            case "wait" -> {
                discordGame.queueMessage("You will wait for battle wheels to decide.");
                if (battle.mightCallTraitor(game, faction, turn, wholeTerritoryName))
                    discordGame.pushGame();
            }
            case "yes" -> {
                if (battle.isResolutionPublished())
                    discordGame.queueMessage("You will call Traitor.");
                else
                    discordGame.queueMessage("You will call Traitor if possible.");
                battle.willCallTraitor(game, faction, true, turn, wholeTerritoryName);
                deleteTraitorCallButtonsInChannel(event.getMessageChannel());
                discordGame.pushGame();
            }
            case "no" -> {
                discordGame.queueMessage("You will not call Traitor.");
                battle.willCallTraitor(game, faction, false, turn, wholeTerritoryName);
                deleteTraitorCallButtonsInChannel(event.getMessageChannel());
                discordGame.pushGame();
            }
        }
    }

    public static void deleteTraitorCallButtonsInChannel(MessageChannel channel) {
        List<Message> messages = channel.getHistoryAround(channel.getLatestMessageId(), 100).complete().getRetrievedHistory();
        List<Message> messagesToDelete = messages.stream().filter(message -> message.getButtons().stream().map(ActionComponent::getId).anyMatch(id -> id != null && id.startsWith("traitor-call"))).toList();
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
