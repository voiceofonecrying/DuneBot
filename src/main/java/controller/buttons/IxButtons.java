package controller.buttons;

import constants.Emojis;
import controller.commands.IxCommands;
import enums.UpdateType;
import exceptions.ChannelNotFoundException;
import exceptions.InvalidGameStateException;
import controller.DiscordGame;
import model.*;
import model.factions.Faction;
import model.factions.IxFaction;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static controller.buttons.ButtonManager.deleteAllButtonsInChannel;

public class IxButtons implements Pressable {

    public static void press(ButtonInteractionEvent event, Game game, DiscordGame discordGame) throws ChannelNotFoundException, InvalidGameStateException, IOException {
        if (event.getComponentId().startsWith("ix-starting-card-")) startingCardSelected(event, discordGame, game);
        else if (event.getComponentId().equals("ix-confirm-start-reset")) resetStartingCardSelection(discordGame, game);
        else if (event.getComponentId().startsWith("ix-confirm-start-")) confirmStartingCard(event, discordGame, game);
        else if (event.getComponentId().startsWith("ix-card-to-reject-")) cardSelected(event, discordGame, game);
        else if (event.getComponentId().startsWith("ix-reject-reset")) chooseDifferentCard(discordGame, game);
        else if (event.getComponentId().startsWith("ix-reject-")) locationSelected(event, discordGame, game);
        else if (event.getComponentId().equals("ix-reset-card-selection")) chooseDifferentCard(discordGame, game);
        else if (event.getComponentId().startsWith("ix-confirm-reject-reset")) chooseDifferentCard(discordGame, game);
        else if (event.getComponentId().startsWith("ix-confirm-reject-technology-")) sendCardBackAndRequestTechnology(event, discordGame, game);
        else if (event.getComponentId().startsWith("ix-confirm-reject-")) sendCardBack(event, discordGame, game);
        else if (event.getComponentId().startsWith("hms-move-more-choices-")) hmsMoreChoices(event, game, discordGame);
        else if (event.getComponentId().startsWith("hms-move-sector-")) hmsFilterBySector(event, game, discordGame);
        else if (event.getComponentId().startsWith("hms-move-")) queueSectorButtons(event, game, discordGame);
        switch (event.getComponentId()) {
            case "hms-pass-movement" -> hmsPassMovement(event, game, discordGame);
            case "hms-reset-movement" -> hmsResetShipmentMovement(game, discordGame);
        }
    }

    public static void hmsSubPhase(DiscordGame discordGame, Game game) throws ChannelNotFoundException {
        discordGame.getTurnSummary().queueMessage(Emojis.IX + " to decide if they want to move the HMS.");
        Faction faction = game.getFaction("Ix");
        faction.getMovement().clear();
        faction.getMovement().setMoved(false);
        hmsQueueMovableTerritories(game, 0);
    }

    private static void hmsPassMovement(ButtonInteractionEvent event, Game game, DiscordGame discordGame) throws ChannelNotFoundException {
        discordGame.getTurnSummary().queueMessage(Emojis.IX + " does not move the HMS.");
        discordGame.queueMessage("You are not moving the HMS.");
        game.setIxHMSActionRequired(false);
        deleteAllButtonsInChannel(event.getMessageChannel());
        discordGame.pushGame();
    }

    private static void hmsResetShipmentMovement(Game game, DiscordGame discordGame) throws ChannelNotFoundException {
        discordGame.queueMessage("Starting over.");
        Faction faction = game.getFaction("Ix");
        faction.getMovement().clear();
        faction.getMovement().setMoved(false);
        hmsQueueMovableTerritories(game, 0);
        discordGame.queueDeleteMessage();
        discordGame.pushGame();
    }

    public static boolean isNotStronghold(Game game, String wholeTerritoryName){
        try {
            if (game.getTerritory(wholeTerritoryName).isStronghold()) return false;
        } catch (IllegalArgumentException e) {
            return true;
        }
        return true;
    }

    //    private static void hmsQueueMovableTerritories(ButtonInteractionEvent event, Game game, DiscordGame discordGame, boolean ornithopter) throws ChannelNotFoundException {
    public static void hmsQueueMovableTerritories(Game game, int startingIndex) {
        Faction faction = game.getFaction("Ix");
        Territory from = game.getTerritories().values().stream().filter(territory -> territory.getForces().stream().anyMatch(force -> force.getName().equals("Hidden Mobile Stronghold"))).findFirst().orElseThrow();

        faction.getMovement().setMovingFrom(from.getTerritoryName());
        int spacesCanMove = 6;
        Set<String> moveableTerritories = ShipmentAndMovementButtons.getAdjacentTerritoryNames(from.getTerritoryName().replaceAll("\\(.*\\)", "").strip(), spacesCanMove, game)
                .stream().filter(t -> isNotStronghold(game, t)).collect(Collectors.toSet());
        TreeSet<DuneChoice> moveToChoices = new TreeSet<>(Comparator.comparing(DuneChoice::getLabel));
        moveableTerritories.stream().map(t -> new DuneChoice("hms-move-" + t, t)).forEach(moveToChoices::add);

        List<DuneChoice> choices = new ArrayList<>();
        int maxButtons = 24;
        if (moveToChoices.size() - startingIndex > maxButtons) {
            int nextStartingIndex = startingIndex + maxButtons - 1; // Take one out for the "More choices" button
            choices.addAll(moveToChoices.stream().toList().subList(startingIndex, nextStartingIndex));
            String buttonId = "hms-move-more-choices-" + nextStartingIndex;
            choices.add(new DuneChoice("secondary", buttonId, "More choices"));
        } else {
            choices.addAll(moveToChoices.stream().toList().subList(startingIndex, moveToChoices.size()));
        }
        if (startingIndex == 0)
            choices.add(new DuneChoice("danger", "hms-pass-movement", "No move"));
        else
            choices.add(new DuneChoice("secondary", "hms-reset-movement", "Start over"));
        faction.getChat().publish("Where will you move the HMS? " + game.getFaction("Ix").getPlayer(), choices);
    }


    private static void hmsMoreChoices(ButtonInteractionEvent event, Game game, DiscordGame discordGame) {
        String moreChoices = "hms-move-more-choices-";
        int startingIndex = Integer.parseInt(event.getComponentId().replace(moreChoices, "").replace("-", " "));
        discordGame.queueDeleteMessage();
        discordGame.queueMessage("Getting more choices");
        hmsQueueMovableTerritories(game, startingIndex);
    }

    private static void queueSectorButtons(ButtonInteractionEvent event, Game game, DiscordGame discordGame) throws ChannelNotFoundException {
        String hmsMoveString = "hms-move-";
        String territoryName = event.getComponentId().replace(hmsMoveString, "").replace("-", " ");
        discordGame.queueMessage("You selected " + territoryName);
        Faction faction = game.getFaction("Ix");
        List<Territory> territory = game.getTerritories().values().stream()
                .filter(t -> t.getSector() != game.getStorm())
                .filter(t -> t.getTerritoryName().replaceAll("\\s*\\([^)]*\\)\\s*", "")
                .equalsIgnoreCase(territoryName)
        ).toList();

        if (territory.size() == 1) {
            faction.getMovement().setMovingTo(territory.getFirst().getTerritoryName());
            hmsExecuteFactionMovement(discordGame, game, faction);
            discordGame.pushGame();
            return;
        }

        List<Button> buttons = new LinkedList<>();

        for (Territory sector : territory) {
            if (sector.getSpice() > 0)
                buttons.add(Button.primary(hmsMoveString + "sector-" + sector.getTerritoryName(), sector.getSector() + " (spice sector)"));
            else
                buttons.add(Button.primary(hmsMoveString + "sector-" + sector.getTerritoryName(), String.valueOf(sector.getSector())));
        }
        buttons.sort(ShipmentAndMovementButtons.getButtonComparator());
        String backButtonId = "hms-reset-movement";

        discordGame.queueMessage(new MessageCreateBuilder()
                .setContent("Which sector?")
                .addActionRow(buttons)
                .addActionRow(Button.secondary(backButtonId, "Start Over"))
        );
    }

    private static void hmsFilterBySector(ButtonInteractionEvent event, Game game, DiscordGame discordGame) throws ChannelNotFoundException {
        String hmsMoveString = "hms-move-";
        String sector = event.getComponentId().replace(hmsMoveString + "sector-", "").replace("-", " ");
        discordGame.queueMessage("You selected sector " + sector);
        Faction faction = game.getFaction("Ix");
        Territory territory = game.getTerritories().values().stream().filter(t -> t.getTerritoryName().contains(
                sector)
        ).findFirst().orElseThrow();

        faction.getMovement().setMovingTo(territory.getTerritoryName());

//        queueForcesButtons(event, game, discordGame, faction, isShipment);
        hmsExecuteFactionMovement(discordGame, game, faction);
        discordGame.pushGame();
    }

    public static void hmsExecuteFactionMovement(DiscordGame discordGame, Game game, Faction faction) throws ChannelNotFoundException {
        Movement movement = faction.getMovement();
        String movingTo = movement.getMovingTo();
        Territory targetTerritory = game.getTerritory(movingTo);
//            IxCommands.moveHMS();
        for (Territory territory : game.getTerritories().values()) {
            territory.getForces().removeIf(f -> f.getName().equals("Hidden Mobile Stronghold"));
            game.removeTerritoryFromAnotherTerritory(game.getTerritory("Hidden Mobile Stronghold"), territory);
        }
//            IxCommands.placeHMS();
        targetTerritory.addForces("Hidden Mobile Stronghold", 1);
        game.putTerritoryInAnotherTerritory(game.getTerritory("Hidden Mobile Stronghold"), targetTerritory);
        game.setIxHMSActionRequired(false);
        discordGame.getTurnSummary().queueMessage(Emojis.IX + " moved the HMS to " + targetTerritory.getTerritoryName() + ".");
        discordGame.pushGame();
        movement.clear();
//        deleteAllButtonsInChannel(event.getMessageChannel());
        game.setUpdated(UpdateType.MAP);
    }

    private static void startingCardSelected(ButtonInteractionEvent event, DiscordGame discordGame, Game game) throws InvalidGameStateException {
        IxFaction ixFaction = (IxFaction) game.getFaction("Ix");
        if (game.isSetupFinished()) {
            throw new InvalidGameStateException("Setup phase is completed.");
        } else if (ixFaction.getTreacheryHand().size() <= 4) {
            throw new InvalidGameStateException("You have already selected your card.");
        }
        discordGame.queueMessage("You selected " + event.getComponentId().split("-")[4].trim() + ".");
        discordGame.queueDeleteMessage();
        IxCommands.confirmStartingCard(game, event.getComponentId().split("-")[4]);
    }

    private static void resetStartingCardSelection(DiscordGame discordGame, Game game) throws InvalidGameStateException {
        IxFaction ixFaction = (IxFaction) game.getFaction("Ix");
        if (game.isSetupFinished()) {
            throw new InvalidGameStateException("Setup phase is completed.");
        } else if (ixFaction.getTreacheryHand().size() <= 4) {
            throw new InvalidGameStateException("You have already selected your card.");
        }
        discordGame.queueMessage("Choose a different card.");
        discordGame.queueDeleteMessage();
        IxCommands.initialCardButtons(game);
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

    private static void cardSelected(ButtonInteractionEvent event, DiscordGame discordGame, Game game) throws InvalidGameStateException {
        int buttonTurn = Integer.parseInt(event.getComponentId().split("-")[4]);
        if (buttonTurn != game.getTurn()) {
            throw new InvalidGameStateException("Button is from turn " + buttonTurn);
        } else if (!game.getBidding().isIxRejectOutstanding()) {
            throw new InvalidGameStateException("You have already sent a card back.");
        }
        discordGame.queueMessage("You selected " + event.getComponentId().split("-")[6].trim() + ".");
        discordGame.queueDeleteMessage();
        IxCommands.sendBackLocationButtons(game, event.getComponentId().split("-")[6]);
    }

    private static void locationSelected(ButtonInteractionEvent event, DiscordGame discordGame, Game game) throws InvalidGameStateException {
        int buttonTurn = Integer.parseInt(event.getComponentId().split("-")[2]);
        if (buttonTurn != game.getTurn()) {
            throw new InvalidGameStateException("Button is from turn " + buttonTurn);
        } else if (!game.getBidding().isIxRejectOutstanding()) {
            throw new InvalidGameStateException("You have already sent a card back.");
        }
        discordGame.queueMessage("You selected to send it to the " + event.getComponentId().split("-")[4].trim() + ".");
        discordGame.queueDeleteMessage();
        IxCommands.confirmCardToSendBack(game, event.getComponentId().split("-")[3], event.getComponentId().split("-")[4]);
    }

    private static void chooseDifferentCard(DiscordGame discordGame, Game game) throws InvalidGameStateException {
        Bidding bidding = game.getBidding();
        if (!bidding.isIxRejectOutstanding()) {
            throw new InvalidGameStateException("You have already sent a card back.");
        }
        discordGame.queueMessage("Starting over.");
        discordGame.queueDeleteMessage();
        bidding.presentCardToRejectChoices(game);
    }

    private static void sendCardBackAndRequestTechnology(ButtonInteractionEvent event, DiscordGame discordGame, Game game) throws ChannelNotFoundException, InvalidGameStateException {
        if (!game.getBidding().isIxRejectOutstanding()) {
            throw new InvalidGameStateException("You have already sent a card back.");
        }
        discordGame.queueDeleteMessage();
        IxCommands.sendCardBackToDeck(event, discordGame, game, event.getComponentId().split("-")[4], event.getComponentId().split("-")[5], true);
    }

    private static void sendCardBack(ButtonInteractionEvent event, DiscordGame discordGame, Game game) throws ChannelNotFoundException, InvalidGameStateException {
        if (!game.getBidding().isIxRejectOutstanding()) {
            throw new InvalidGameStateException("You have already sent a card back.");
        }
        discordGame.queueDeleteMessage();
        IxCommands.sendCardBackToDeck(event, discordGame, game, event.getComponentId().split("-")[3], event.getComponentId().split("-")[4], false);
    }
}
