package controller.buttons;

import controller.DiscordGame;
import controller.commands.SetupCommands;
import exceptions.ChannelNotFoundException;
import exceptions.InvalidGameStateException;
import helpers.MessageHelper;
import model.Game;
import model.factions.Faction;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.components.buttons.Button;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MovementButtonActions {
    protected static void handleMovementAction(ButtonInteractionEvent event, DiscordGame discordGame, String action) throws ChannelNotFoundException, InvalidGameStateException, IOException {
        if (action.equals("pass")) pass(event, discordGame);
        else if (action.equals("start-over")) startOver(event, discordGame);
        else if (action.equals("stronghold")) presentStrongholdShippingChoices(event, discordGame);
        else if (action.equals("spice-blow")) presentSpiceBlowShippingChoices(event, discordGame);
        else if (action.equals("rock")) presentRockShippingChoices(event, discordGame);
        else if (action.equals("discovery-tokens")) presentDiscoveryShippingChoices(event, discordGame);
        else if (action.equals("other")) presentOtherShippingChoices(event, discordGame);
        else if (action.startsWith("territory-")) presentSectorChoices(event, discordGame);
        else if (action.startsWith("sector-")) filterBySector(event, discordGame);
        else if (action.startsWith("add-force-")) addRegularForces(event, discordGame);
        else if (action.startsWith("add-special-force-")) addSpecialForces(event, discordGame);
        else if (action.equals("reset-forces")) resetForces(event, discordGame);
        else if (action.equals("execute")) execute(event, discordGame);
    }

    protected static void pass(ButtonInteractionEvent event, DiscordGame discordGame) throws ChannelNotFoundException, InvalidGameStateException {
        Faction faction = ButtonManager.getButtonPresser(event, discordGame.getGame());
        deleteButtonsInChannelWithPrefix(event.getMessageChannel(), faction.getMovement().getChoicePrefix());
        faction.getMovement().pass();
        discordGame.pushGame();
    }

    protected static void startOver(ButtonInteractionEvent event, DiscordGame discordGame) throws ChannelNotFoundException {
        Faction faction = ButtonManager.getButtonPresser(event, discordGame.getGame());
        faction.getMovement().startOver();
        deleteButtonsInChannelWithPrefix(event.getMessageChannel(), faction.getMovement().getChoicePrefix());
//        discordGame.queueDeleteMessage();
        discordGame.pushGame();
    }

    protected static void presentStrongholdShippingChoices(ButtonInteractionEvent event, DiscordGame discordGame) throws ChannelNotFoundException, InvalidGameStateException {
        Faction faction = ButtonManager.getButtonPresser(event, discordGame.getGame());
        faction.getMovement().presentStrongholdChoices();
        discordGame.queueDeleteMessage();
    }

    protected static void presentSpiceBlowShippingChoices(ButtonInteractionEvent event, DiscordGame discordGame) throws ChannelNotFoundException {
        Faction faction = ButtonManager.getButtonPresser(event, discordGame.getGame());
        faction.getMovement().presentSpiceBlowChoices();
        discordGame.queueDeleteMessage();
    }

    protected static void presentRockShippingChoices(ButtonInteractionEvent event, DiscordGame discordGame) throws ChannelNotFoundException {
        Faction faction = ButtonManager.getButtonPresser(event, discordGame.getGame());
        faction.getMovement().presentRockChoices();
        discordGame.queueDeleteMessage();
    }

    protected static void presentDiscoveryShippingChoices(ButtonInteractionEvent event, DiscordGame discordGame) throws ChannelNotFoundException, InvalidGameStateException {
        Faction faction = ButtonManager.getButtonPresser(event, discordGame.getGame());
        faction.getMovement().presentDiscoveryTokenChoices();
        discordGame.queueDeleteMessage();
    }

    protected static void presentOtherShippingChoices(ButtonInteractionEvent event, DiscordGame discordGame) throws ChannelNotFoundException {
        Faction faction = ButtonManager.getButtonPresser(event, discordGame.getGame());
        faction.getMovement().presentNonSpiceNonRockChoices();
        discordGame.queueDeleteMessage();
    }

    protected static void presentSectorChoices(ButtonInteractionEvent event, DiscordGame discordGame) throws ChannelNotFoundException {
        Game game = discordGame.getGame();
        Faction faction = ButtonManager.getButtonPresser(event, game);
        String choicePrefix = faction.getMovement().getChoicePrefix();
        String aggregateTerritoryName = event.getComponentId().replace("territory-", "").replace(choicePrefix, "");
        if (faction.getMovement().processTerritory(aggregateTerritoryName))
            discordGame.pushGame();
        deleteButtonsInChannelWithPrefix(event.getMessageChannel(), choicePrefix);
    }

    protected static void filterBySector(ButtonInteractionEvent event, DiscordGame discordGame) throws ChannelNotFoundException {
        Faction faction = ButtonManager.getButtonPresser(event, discordGame.getGame());
        String choicePrefix = faction.getMovement().getChoicePrefix();
        String territoryName = event.getComponentId().replace("sector-", "").replace(choicePrefix, "");
        faction.getMovement().processSector(territoryName);
        discordGame.pushGame();
        deleteButtonsInChannelWithPrefix(event.getMessageChannel(), choicePrefix);
    }

    protected static void addRegularForces(ButtonInteractionEvent event, DiscordGame discordGame) throws ChannelNotFoundException {
        Faction faction = ButtonManager.getButtonPresser(event, discordGame.getGame());
        String choicePrefix = faction.getMovement().getChoicePrefix();
        int addedForces = Integer.parseInt(event.getComponentId().replace("add-force-", "").replace(choicePrefix, ""));
        faction.getMovement().addRegularForces(addedForces);
        deleteButtonsInChannelWithPrefix(event.getMessageChannel(), choicePrefix);
        discordGame.pushGame();
    }

    protected static void addSpecialForces(ButtonInteractionEvent event, DiscordGame discordGame) throws ChannelNotFoundException {
        Faction faction = ButtonManager.getButtonPresser(event, discordGame.getGame());
        String choicePrefix = faction.getMovement().getChoicePrefix();
        int addedForces = Integer.parseInt(event.getComponentId().replace("add-special-force-", "").replace(choicePrefix, ""));
        faction.getMovement().addSpecialForces(addedForces);
        deleteButtonsInChannelWithPrefix(event.getMessageChannel(), choicePrefix);
        discordGame.pushGame();
    }

    protected static void resetForces(ButtonInteractionEvent event, DiscordGame discordGame) throws ChannelNotFoundException {
        Faction faction = ButtonManager.getButtonPresser(event, discordGame.getGame());
        String choicePrefix = faction.getMovement().getChoicePrefix();
        faction.getMovement().resetForces();
        deleteButtonsInChannelWithPrefix(event.getMessageChannel(), choicePrefix);
        discordGame.pushGame();
    }

    protected static void execute(ButtonInteractionEvent event, DiscordGame discordGame) throws ChannelNotFoundException, InvalidGameStateException, IOException {
        Faction faction = ButtonManager.getButtonPresser(event, discordGame.getGame());
        deleteButtonsInChannelWithPrefix(event.getMessageChannel(), faction.getMovement().getChoicePrefix());
        if (faction.getMovement().execute())
            SetupCommands.advance(event.getGuild(), discordGame, discordGame.getGame());
        discordGame.pushGame();
    }

    public static void deleteButtonsInChannelWithPrefix(MessageChannel channel, String choicePrefix) {
        List<Message> messages = channel.getHistoryAround(channel.getLatestMessageId(), 100).complete().getRetrievedHistory();
        List<Message> messagesToDelete = new ArrayList<>();
        for (Message message : messages) {
            List<Button> buttons = MessageHelper.getButtons(message);
            for (Button button : buttons) {
                String id = button.getCustomId();
                if (id != null && (id.startsWith(choicePrefix))) {
                    messagesToDelete.add(message);
                    break;
                }
            }
        }
        for (Message message : messagesToDelete) {
            try {
                message.delete().complete();
            } catch (Exception ignore) {}
        }
    }
}
