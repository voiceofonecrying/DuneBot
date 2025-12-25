package controller.buttons;

import controller.commands.SetupCommands;
import exceptions.ChannelNotFoundException;
import exceptions.InvalidGameStateException;
import controller.DiscordGame;
import model.*;
import model.factions.IxFaction;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;
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
        else if (event.getComponentId().startsWith("ix-hms-move-sector-")) hmsFilterBySector(event, game, discordGame);
        else if (event.getComponentId().startsWith("ix-hms-move-")) queueSectorButtons(event, game, discordGame);
        else if (event.getComponentId().startsWith("ix-hms-placement-")) handleIxHMSPlacementButtons(event, discordGame);
        switch (event.getComponentId()) {
            case "ix-hms-pass-movement" -> hmsPassMovement(event, game, discordGame);
            case "ix-hms-reset-movement" -> hmsResetShipmentMovement(game, discordGame);
        }
    }

    public static void hmsSubPhase(Game game) {
        game.getIxFaction().startHMSMovement();
        hmsQueueMovableTerritories(game);
    }

    private static void hmsPassMovement(ButtonInteractionEvent event, Game game, DiscordGame discordGame) throws ChannelNotFoundException {
        discordGame.queueMessage("HMS movement complete.");
        game.getIxFaction().endHMSMovement();
        deleteAllButtonsInChannel(event.getMessageChannel());
        discordGame.pushGame();
    }

    private static void hmsResetShipmentMovement(Game game, DiscordGame discordGame) throws ChannelNotFoundException {
        discordGame.queueMessage("Starting over.");
        hmsQueueMovableTerritories(game);
        discordGame.queueDeleteMessage();
        discordGame.pushGame();
    }

    //    private static void hmsQueueMovableTerritories(ButtonInteractionEvent event, Game game, DiscordGame discordGame, boolean ornithopter) throws ChannelNotFoundException {
    public static void hmsQueueMovableTerritories(Game game) {
        IxFaction faction = game.getIxFaction();
        faction.getMovement().clear();
        Territory from = faction.getTerritoryWithHMS();

        faction.getMovement().setMovingFrom(from.getTerritoryName());
        Set<String> moveableTerritories = ShipmentAndMovementButtons.getAdjacentTerritoryNames(from.getTerritoryName().replaceAll("\\(.*\\)", "").strip(), 1, game)
                .stream().filter(t -> game.getTerritories().isNotStronghold(t)).collect(Collectors.toSet());
        TreeSet<DuneChoice> moveToChoices = new TreeSet<>(Comparator.comparing(DuneChoice::getLabel));
        moveableTerritories.stream().map(t -> new DuneChoice("ix-hms-move-" + t, t)).forEach(moveToChoices::add);

        List<DuneChoice> choices = new ArrayList<>(moveToChoices.stream().toList());
        choices.add(new DuneChoice("secondary", "ix-hms-pass-movement", "End HMS movement"));
        faction.getChat().publish("You can move the HMS " + faction.getHMSMoves() + " territories. Choose the next territory. " + game.getIxFaction().getPlayer(), choices);
    }

    private static void queueSectorButtons(ButtonInteractionEvent event, Game game, DiscordGame discordGame) throws ChannelNotFoundException, InvalidGameStateException {
        discordGame.queueDeleteMessage();
        String hmsMoveString = "ix-hms-move-";
        String territoryName = event.getComponentId().replace(hmsMoveString, "").replace("-", " ");
        IxFaction faction = game.getIxFaction();
        List<Territory> territory = game.getTerritories().values().stream()
                .filter(t -> t.getSector() != game.getStorm())
                .filter(t -> t.getTerritoryName().replaceAll("\\s*\\([^)]*\\)\\s*", "")
                .equalsIgnoreCase(territoryName)
        ).toList();

        if (territory.size() == 1) {
            faction.getMovement().setMovingTo(territory.getFirst().getTerritoryName());
            hmsExecuteFactionMovement(discordGame, game, faction);
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
                .setContent("Which sector of " + territoryName + "?")
                .addComponents(ActionRow.of(buttons))
                .addComponents(ActionRow.of(Button.secondary(backButtonId, "Start Over")))
        );
    }

    private static void hmsFilterBySector(ButtonInteractionEvent event, Game game, DiscordGame discordGame) throws ChannelNotFoundException, InvalidGameStateException {
        discordGame.queueDeleteMessage();
        String hmsMoveString = "ix-hms-move-";
        String sector = event.getComponentId().replace(hmsMoveString + "sector-", "").replace("-", " ");
        IxFaction faction = game.getIxFaction();
        Territory territory = game.getTerritories().values().stream().filter(t -> t.getTerritoryName().contains(sector)).findFirst().orElseThrow();
        faction.getMovement().setMovingTo(territory.getTerritoryName());
        hmsExecuteFactionMovement(discordGame, game, faction);
    }

    public static void hmsExecuteFactionMovement(DiscordGame discordGame, Game game, IxFaction faction) throws ChannelNotFoundException, InvalidGameStateException {
        Movement movement = faction.getMovement();
        String movingTo = movement.getMovingTo();
        faction.moveHMSOneTerritory(movingTo);
        discordGame.queueMessage("Moving the HMS to " + movingTo);
        movement.clear();
        if (faction.getHMSMoves() > 0)
            hmsQueueMovableTerritories(game);
        else
            faction.endHMSMovement();
        discordGame.pushGame();
    }

    private static void handleIxHMSPlacementButtons(ButtonInteractionEvent event, DiscordGame discordGame) throws ChannelNotFoundException, InvalidGameStateException, IOException {
        String action = event.getComponentId().replace("ix-hms-placement-", "");
        MovementButtonActions.handleMovementAction(event, discordGame, action);
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
