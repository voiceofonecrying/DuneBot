package controller.buttons;

import constants.Emojis;
import controller.commands.SetupCommands;
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
        // Buttons handled by this class must begin with "ix"
        // And any button that begins with "ix" must be handled by this class
        if (event.getComponentId().startsWith("ix-starting-card-")) startingCardSelected(event, discordGame, game);
        else if (event.getComponentId().equals("ix-confirm-start-reset")) resetStartingCardSelection(discordGame, game);
        else if (event.getComponentId().startsWith("ix-confirm-start-")) confirmStartingCard(event, discordGame, game);
        else if (event.getComponentId().startsWith("ix-card-to-reject-")) cardSelected(event, discordGame, game);
        else if (event.getComponentId().startsWith("ix-reject-reset")) chooseDifferentCard(discordGame, game);
        else if (event.getComponentId().startsWith("ix-reject-")) locationSelected(event, discordGame, game);
        else if (event.getComponentId().startsWith("ix-confirm-reject-technology-")) sendCardBackAndRequestTechnology(event, discordGame, game);
        else if (event.getComponentId().startsWith("ix-confirm-reject-")) sendCardBack(event, discordGame, game);
        else if (event.getComponentId().startsWith("ix-request-technology-")) requestTechnology(event, discordGame, game);
        else if (event.getComponentId().startsWith("ix-hms-move-more-choices-")) hmsMoreChoices(event, game, discordGame);
        else if (event.getComponentId().startsWith("ix-hms-move-sector-")) hmsFilterBySector(event, game, discordGame);
        else if (event.getComponentId().startsWith("ix-hms-move-")) queueSectorButtons(event, game, discordGame);
        switch (event.getComponentId()) {
            case "ix-hms-pass-movement" -> hmsPassMovement(event, game, discordGame);
            case "ix-hms-reset-movement" -> hmsResetShipmentMovement(game, discordGame);
        }
    }

    public static void hmsSubPhase(DiscordGame discordGame, Game game) throws ChannelNotFoundException {
        discordGame.getTurnSummary().queueMessage(Emojis.IX + " to decide if they want to move the HMS.");
        IxFaction faction = game.getIxFaction();
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
        IxFaction faction = game.getIxFaction();
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
        IxFaction faction = game.getIxFaction();
        Territory from = game.getTerritories().values().stream().filter(territory -> territory.getForces().stream().anyMatch(force -> force.getName().equals("Hidden Mobile Stronghold"))).findFirst().orElseThrow();

        faction.getMovement().setMovingFrom(from.getTerritoryName());
        int spacesCanMove = 3;
        Set<String> moveableTerritories = ShipmentAndMovementButtons.getAdjacentTerritoryNames(from.getTerritoryName().replaceAll("\\(.*\\)", "").strip(), spacesCanMove, game)
                .stream().filter(t -> isNotStronghold(game, t)).collect(Collectors.toSet());
        TreeSet<DuneChoice> moveToChoices = new TreeSet<>(Comparator.comparing(DuneChoice::getLabel));
        moveableTerritories.stream().map(t -> new DuneChoice("ix-hms-move-" + t, t)).forEach(moveToChoices::add);

        List<DuneChoice> choices = new ArrayList<>();
        int maxButtons = 24;
        if (moveToChoices.size() - startingIndex > maxButtons) {
            int nextStartingIndex = startingIndex + maxButtons - 1; // Take one out for the "More choices" button
            choices.addAll(moveToChoices.stream().toList().subList(startingIndex, nextStartingIndex));
            String buttonId = "ix-hms-move-more-choices-" + nextStartingIndex;
            choices.add(new DuneChoice("secondary", buttonId, "More choices"));
        } else {
            choices.addAll(moveToChoices.stream().toList().subList(startingIndex, moveToChoices.size()));
        }
        if (startingIndex == 0)
            choices.add(new DuneChoice("danger", "ix-hms-pass-movement", "No move"));
        else
            choices.add(new DuneChoice("secondary", "ix-hms-reset-movement", "Start over"));
        faction.getChat().publish("Where will you move the HMS? " + game.getIxFaction().getPlayer(), choices);
    }


    private static void hmsMoreChoices(ButtonInteractionEvent event, Game game, DiscordGame discordGame) {
        String moreChoices = "ix-hms-move-more-choices-";
        int startingIndex = Integer.parseInt(event.getComponentId().replace(moreChoices, "").replace("-", " "));
        discordGame.queueDeleteMessage();
        discordGame.queueMessage("Getting more choices");
        hmsQueueMovableTerritories(game, startingIndex);
    }

    private static void queueSectorButtons(ButtonInteractionEvent event, Game game, DiscordGame discordGame) throws ChannelNotFoundException {
        String hmsMoveString = "ix-hms-move-";
        String territoryName = event.getComponentId().replace(hmsMoveString, "").replace("-", " ");
        discordGame.queueMessage("You selected " + territoryName);
        IxFaction faction = game.getIxFaction();
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
        String backButtonId = "ix-hms-reset-movement";

        discordGame.queueMessage(new MessageCreateBuilder()
                .setContent("Which sector?")
                .addActionRow(buttons)
                .addActionRow(Button.secondary(backButtonId, "Start Over"))
        );
    }

    private static void hmsFilterBySector(ButtonInteractionEvent event, Game game, DiscordGame discordGame) throws ChannelNotFoundException {
        String hmsMoveString = "ix-hms-move-";
        String sector = event.getComponentId().replace(hmsMoveString + "sector-", "").replace("-", " ");
        discordGame.queueMessage("You selected sector " + sector);
        IxFaction faction = game.getIxFaction();
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
        String cardName = event.getComponentId().split("-")[4];
        game.getIxFaction().presentConfirmStartingCardChoices(cardName);
        discordGame.queueDeleteMessage();
    }

    private static void resetStartingCardSelection(DiscordGame discordGame, Game game) throws InvalidGameStateException {
        game.getIxFaction().presentStartingCardChoices();
        discordGame.queueDeleteMessage();
    }

    private static void confirmStartingCard(ButtonInteractionEvent event, DiscordGame discordGame, Game game) throws ChannelNotFoundException, InvalidGameStateException, IOException {
        String cardName = event.getComponentId().split("-")[3];
        game.getIxFaction().startingCard(cardName);
        discordGame.queueDeleteMessage();
        SetupCommands.advance(event.getGuild(), discordGame, game);
    }

    private static void cardSelected(ButtonInteractionEvent event, DiscordGame discordGame, Game game) throws InvalidGameStateException {
        String cardName = event.getComponentId().split("-")[6];
        int buttonTurn = Integer.parseInt(event.getComponentId().split("-")[4]);
        game.getBidding().presentRejectedCardLocationChoices(game, cardName, buttonTurn);
        discordGame.queueDeleteMessage();
    }

    private static void locationSelected(ButtonInteractionEvent event, DiscordGame discordGame, Game game) throws InvalidGameStateException {
        String cardName = event.getComponentId().split("-")[3];
        String location = event.getComponentId().split("-")[4];
        int buttonTurn = Integer.parseInt(event.getComponentId().split("-")[2]);
        game.getBidding().presentRejectConfirmationChoices(game, cardName, location, buttonTurn);
        discordGame.queueDeleteMessage();
    }

    private static void chooseDifferentCard(DiscordGame discordGame, Game game) throws InvalidGameStateException {
        game.getBidding().presentCardToRejectChoices(game);
        discordGame.queueDeleteMessage();
    }

    private static void sendCardBackAndRequestTechnology(ButtonInteractionEvent event, DiscordGame discordGame, Game game) throws InvalidGameStateException, ChannelNotFoundException {
        String cardName = event.getComponentId().split("-")[4];
        String location = event.getComponentId().split("-")[5];
        game.getBidding().sendCardBack(game, cardName, location, true);
        discordGame.queueDeleteMessage();
        discordGame.pushGame();
    }

    private static void sendCardBack(ButtonInteractionEvent event, DiscordGame discordGame, Game game) throws InvalidGameStateException, ChannelNotFoundException {
        String cardName = event.getComponentId().split("-")[3];
        String location = event.getComponentId().split("-")[4];
        game.getBidding().sendCardBack(game, cardName, location, false);
        discordGame.queueDeleteMessage();
        discordGame.pushGame();
    }

    private static void requestTechnology(ButtonInteractionEvent event, DiscordGame discordGame, Game game) throws InvalidGameStateException, ChannelNotFoundException {
        String yesOrNo = event.getComponentId().split("-")[3];
        game.getBidding().requestTechnologyOrAuction(game, game.getIxFaction(), yesOrNo.equals("yes"));
        discordGame.queueDeleteMessage();
        discordGame.pushGame();
    }
}
