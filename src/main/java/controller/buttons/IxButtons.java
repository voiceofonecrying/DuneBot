package controller.buttons;

import constants.Emojis;
import exceptions.InvalidGameStateException;
import controller.commands.CommandManager;
import controller.commands.ShowCommands;
import controller.commands.IxCommands;
import exceptions.ChannelNotFoundException;
import model.DiscordGame;
import model.Game;
import model.Leader;
import model.Territory;
import model.factions.IxFaction;
import model.factions.Faction;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;

public class IxButtons implements Pressable {

    public static void press(ButtonInteractionEvent event, Game game, DiscordGame discordGame) throws ChannelNotFoundException, InvalidGameStateException {
        if (event.getComponentId().startsWith("ix-starting-card-")) startingCardSelected(event, discordGame, game);
        else if (event.getComponentId().equals("ix-confirm-reset")) resetStartingCardSelection(event, discordGame, game);
        else if (event.getComponentId().startsWith("ix-confirm-")) confirmStartingCard(event, discordGame, game);
        else if (event.getComponentId().startsWith("ix-card-to-reject-")) cardSelected(event, discordGame, game);
        else if (event.getComponentId().startsWith("ix-reject-")) locationSelected(event, discordGame, game);
        else if (event.getComponentId().equals("ix-reset-card-selection")) chooseDifferentCard(event, discordGame, game);
    }

    private static void startingCardSelected(ButtonInteractionEvent event, DiscordGame discordGame, Game game) throws ChannelNotFoundException, InvalidGameStateException {
        IxFaction ixFaction = (IxFaction) game.getFaction("Ix");
        if (game.isSetupFinished()) {
            throw new InvalidGameStateException("Setup phase is completed.");
        } else if (ixFaction.getTreacheryHand().size() <= 4) {
            throw new InvalidGameStateException("You have already selected your card.");
        }
        discordGame.queueMessage("You selected " + event.getComponentId().split("-")[4].trim() + ".");
        IxCommands.confirmStartingCard(discordGame, game, event.getComponentId().split("-")[4]);
    }

    private static void resetStartingCardSelection(ButtonInteractionEvent event, DiscordGame discordGame, Game game) throws ChannelNotFoundException, InvalidGameStateException {
        IxFaction ixFaction = (IxFaction) game.getFaction("Ix");
        if (game.isSetupFinished()) {
            throw new InvalidGameStateException("Setup phase is completed.");
        } else if (ixFaction.getTreacheryHand().size() <= 4) {
            throw new InvalidGameStateException("You have already selected your card.");
        }
        discordGame.queueMessage("Choosing a different card.");
        IxCommands.initialCardButtons(discordGame, game);
    }

    private static void confirmStartingCard(ButtonInteractionEvent event, DiscordGame discordGame, Game game) throws ChannelNotFoundException, InvalidGameStateException {
        IxFaction ixFaction = (IxFaction) game.getFaction("Ix");
        if (game.isSetupFinished()) {
            throw new InvalidGameStateException("Setup phase is completed.");
        } else if (ixFaction.getTreacheryHand().size() <= 4) {
            throw new InvalidGameStateException("You have already selected your card.");
        }
        discordGame.queueMessage("You will keep " + event.getComponentId().split("-")[2].trim() + ".");
        IxCommands.ixHandSelection(discordGame, game, event.getComponentId().split("-")[2]);
    }

    private static void cardSelected(ButtonInteractionEvent event, DiscordGame discordGame, Game game) throws ChannelNotFoundException, InvalidGameStateException {
        int buttonTurn = Integer.parseInt(event.getComponentId().split("-")[4]);
        if (buttonTurn != game.getTurn()) {
            throw new InvalidGameStateException("Button is from turn " + buttonTurn);
        } else if (!game.getBidding().isIxRejectOutstanding()) {
            throw new InvalidGameStateException("You have already sent a card back.");
        }
        discordGame.queueMessage("You selected " + event.getComponentId().split("-")[6].trim() + ".");
        IxCommands.sendBackLocationButtons(discordGame, game, event.getComponentId().split("-")[6]);
    }

    private static void locationSelected(ButtonInteractionEvent event, DiscordGame discordGame, Game game) throws ChannelNotFoundException, InvalidGameStateException {
        int buttonTurn = Integer.parseInt(event.getComponentId().split("-")[2]);
        if (buttonTurn != game.getTurn()) {
            throw new InvalidGameStateException("Button is from turn " + buttonTurn);
        } else if (!game.getBidding().isIxRejectOutstanding()) {
            throw new InvalidGameStateException("You have already sent a card back.");
        }
        IxCommands.sendCardBackToDeck(event, discordGame, game, event.getComponentId().split("-")[3], event.getComponentId().split("-")[4]);
    }

    private static void chooseDifferentCard(ButtonInteractionEvent event, DiscordGame discordGame, Game game) throws ChannelNotFoundException, InvalidGameStateException {
        if (!game.getBidding().isIxRejectOutstanding()) {
            throw new InvalidGameStateException("You have already sent a card back.");
        }
        discordGame.queueMessage("Choosing a different card.");
        IxCommands.cardToRejectButtons(discordGame, game);
    }
}
