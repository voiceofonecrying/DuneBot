package controller.buttons;

import controller.commands.IxCommands;
import exceptions.ChannelNotFoundException;
import exceptions.InvalidGameStateException;
import model.DiscordGame;
import model.Game;
import model.factions.IxFaction;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;

public class IxButtons implements Pressable {

    public static void press(ButtonInteractionEvent event, Game game, DiscordGame discordGame) throws ChannelNotFoundException, InvalidGameStateException {
        if (event.getComponentId().startsWith("ix-starting-card-")) startingCardSelected(event, discordGame, game);
        else if (event.getComponentId().equals("ix-confirm-start-reset")) resetStartingCardSelection(discordGame, game);
        else if (event.getComponentId().startsWith("ix-confirm-start-")) confirmStartingCard(event, discordGame, game);
        else if (event.getComponentId().startsWith("ix-card-to-reject-")) cardSelected(event, discordGame, game);
        else if (event.getComponentId().startsWith("ix-reject-reset")) chooseDifferentCard(discordGame, game);
        else if (event.getComponentId().startsWith("ix-reject-")) locationSelected(event, discordGame, game);
        else if (event.getComponentId().equals("ix-reset-card-selection")) chooseDifferentCard(discordGame, game);
        else if (event.getComponentId().startsWith("ix-confirm-reject-reset")) chooseDifferentCard(discordGame, game);
        else if (event.getComponentId().startsWith("ix-confirm-reject-")) sendCardBack(event, discordGame, game);
    }

    private static void startingCardSelected(ButtonInteractionEvent event, DiscordGame discordGame, Game game) throws ChannelNotFoundException, InvalidGameStateException {
        IxFaction ixFaction = (IxFaction) game.getFaction("Ix");
        if (game.isSetupFinished()) {
            throw new InvalidGameStateException("Setup phase is completed.");
        } else if (ixFaction.getTreacheryHand().size() <= 4) {
            throw new InvalidGameStateException("You have already selected your card.");
        }
        discordGame.queueMessage("You selected " + event.getComponentId().split("-")[4].trim() + ".");
        discordGame.queueDeleteMessage();
        IxCommands.confirmStartingCard(discordGame, event.getComponentId().split("-")[4]);
    }

    private static void resetStartingCardSelection(DiscordGame discordGame, Game game) throws ChannelNotFoundException, InvalidGameStateException {
        IxFaction ixFaction = (IxFaction) game.getFaction("Ix");
        if (game.isSetupFinished()) {
            throw new InvalidGameStateException("Setup phase is completed.");
        } else if (ixFaction.getTreacheryHand().size() <= 4) {
            throw new InvalidGameStateException("You have already selected your card.");
        }
        discordGame.queueMessage("Choose a different card.");
        discordGame.queueDeleteMessage();
        IxCommands.initialCardButtons(discordGame, game);
    }

    private static void confirmStartingCard(ButtonInteractionEvent event, DiscordGame discordGame, Game game) throws ChannelNotFoundException, InvalidGameStateException {
        IxFaction ixFaction = (IxFaction) game.getFaction("Ix");
        if (game.isSetupFinished()) {
            throw new InvalidGameStateException("Setup phase is completed.");
        } else if (ixFaction.getTreacheryHand().size() <= 4) {
            throw new InvalidGameStateException("You have already selected your card.");
        }
        discordGame.queueMessage("You will keep " + event.getComponentId().split("-")[3].trim() + ".");
        discordGame.queueDeleteMessage();
        IxCommands.ixHandSelection(discordGame, game, event.getComponentId().split("-")[3]);
    }

    private static void cardSelected(ButtonInteractionEvent event, DiscordGame discordGame, Game game) throws ChannelNotFoundException, InvalidGameStateException {
        int buttonTurn = Integer.parseInt(event.getComponentId().split("-")[4]);
        if (buttonTurn != game.getTurn()) {
            throw new InvalidGameStateException("Button is from turn " + buttonTurn);
        } else if (!game.getBidding().isIxRejectOutstanding()) {
            throw new InvalidGameStateException("You have already sent a card back.");
        }
        discordGame.queueMessage("You selected " + event.getComponentId().split("-")[6].trim() + ".");
        discordGame.queueDeleteMessage();
        IxCommands.sendBackLocationButtons(discordGame, game, event.getComponentId().split("-")[6]);
    }

    private static void locationSelected(ButtonInteractionEvent event, DiscordGame discordGame, Game game) throws ChannelNotFoundException, InvalidGameStateException {
        int buttonTurn = Integer.parseInt(event.getComponentId().split("-")[2]);
        if (buttonTurn != game.getTurn()) {
            throw new InvalidGameStateException("Button is from turn " + buttonTurn);
        } else if (!game.getBidding().isIxRejectOutstanding()) {
            throw new InvalidGameStateException("You have already sent a card back.");
        }
        discordGame.queueMessage("You selected to send it to the " + event.getComponentId().split("-")[4].trim() + ".");
        discordGame.queueDeleteMessage();
        IxCommands.confirmCardToSendBack(discordGame, event.getComponentId().split("-")[3], event.getComponentId().split("-")[4]);
    }

    private static void chooseDifferentCard(DiscordGame discordGame, Game game) throws ChannelNotFoundException, InvalidGameStateException {
        if (!game.getBidding().isIxRejectOutstanding()) {
            throw new InvalidGameStateException("You have already sent a card back.");
        }
        discordGame.queueMessage("Starting over.");
        discordGame.queueDeleteMessage();
        IxCommands.cardToRejectButtons(discordGame, game);
    }

    private static void sendCardBack(ButtonInteractionEvent event, DiscordGame discordGame, Game game) throws ChannelNotFoundException, InvalidGameStateException {
        if (!game.getBidding().isIxRejectOutstanding()) {
            throw new InvalidGameStateException("You have already sent a card back.");
        }
        discordGame.queueDeleteMessage();
        IxCommands.sendCardBackToDeck(event, discordGame, game, event.getComponentId().split("-")[3], event.getComponentId().split("-")[4]);
    }
}
