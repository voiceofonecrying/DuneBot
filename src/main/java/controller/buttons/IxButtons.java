package controller.buttons;

import constants.Emojis;
import controller.channels.FactionChat;
import controller.commands.CommandManager;
import controller.commands.IxCommands;
import controller.commands.ShowCommands;
import enums.GameOption;
import enums.UpdateType;
import exceptions.ChannelNotFoundException;
import exceptions.InvalidGameStateException;
import controller.DiscordGame;
import exceptions.InvalidOptionException;
import model.Force;
import model.Game;
import model.Movement;
import model.Territory;
import model.factions.Faction;
import model.factions.IxFaction;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;

import java.io.IOException;
import java.util.*;

import static controller.buttons.ButtonManager.deleteAllButtonsInChannel;

public class IxButtons implements Pressable {

    public static void press(ButtonInteractionEvent event, Game game, DiscordGame discordGame) throws ChannelNotFoundException, InvalidGameStateException, InvalidOptionException, IOException {
        if (event.getComponentId().startsWith("ix-starting-card-")) startingCardSelected(event, discordGame, game);
        else if (event.getComponentId().equals("ix-confirm-start-reset")) resetStartingCardSelection(discordGame, game);
        else if (event.getComponentId().startsWith("ix-confirm-start-")) confirmStartingCard(event, discordGame, game);
        else if (event.getComponentId().startsWith("ix-card-to-reject-")) cardSelected(event, discordGame, game);
        else if (event.getComponentId().startsWith("ix-reject-reset")) chooseDifferentCard(discordGame, game);
        else if (event.getComponentId().startsWith("ix-reject-")) locationSelected(event, discordGame, game);
        else if (event.getComponentId().equals("ix-reset-card-selection")) chooseDifferentCard(discordGame, game);
        else if (event.getComponentId().startsWith("ix-confirm-reject-reset")) chooseDifferentCard(discordGame, game);
        else if (event.getComponentId().startsWith("ix-confirm-reject-")) sendCardBack(event, discordGame, game);
        else if (event.getComponentId().startsWith("hms-move-sector-")) hmsFilterBySector(event, game, discordGame, false);
        else if (event.getComponentId().startsWith("hms-move-")) queueSectorButtons(event, game, discordGame, false);
        switch (event.getComponentId()) {
            case "hms-pass-movement" -> hmsPassMovement(event, game, discordGame);
            case "hms-reset-movement" -> hmsResetShipmentMovement(game, discordGame, false);
        }
    }

    public static void hmsSubPhase(DiscordGame discordGame, Game game) throws ChannelNotFoundException {
        discordGame.getTurnSummary().queueMessage(Emojis.IX + " to decide if they want to move the HMS. " + game.getFaction("Ix").getPlayer());
        hmsQueueMovableTerritories(game, discordGame);
    }

    private static void hmsPassMovement(ButtonInteractionEvent event, Game game, DiscordGame discordGame) throws ChannelNotFoundException {
        discordGame.getTurnSummary().queueMessage(Emojis.IX + " does not move the HMS.");
        discordGame.queueMessage("You are not moving the HMS.");
        deleteAllButtonsInChannel(event.getMessageChannel());
        discordGame.pushGame();
    }

    private static void hmsResetShipmentMovement(Game game, DiscordGame discordGame, boolean isShipment) throws ChannelNotFoundException {
        discordGame.queueMessage("Starting over.");
//        Faction faction = ButtonManager.getButtonPresser(event, game);
        Faction faction = game.getFaction("Ix");
        if (isShipment) {
            faction.getShipment().clear();
            faction.getShipment().setShipped(false);
//            queueShippingButtons(event, game, discordGame);
        } else {
            faction.getMovement().clear();
            faction.getMovement().setMoved(false);
//            IxCommands.hmsQueueMovementButtons(game, discordGame);
            hmsQueueMovableTerritories(game, discordGame);
        }
        discordGame.queueDeleteMessage();
        discordGame.pushGame();
    }

    //    private static void hmsQueueMovableTerritories(ButtonInteractionEvent event, Game game, DiscordGame discordGame, boolean ornithopter) throws ChannelNotFoundException {
    public static void hmsQueueMovableTerritories(Game game, DiscordGame discordGame) throws ChannelNotFoundException {
//        Faction faction = ButtonManager.getButtonPresser(event, game);
        Faction faction = game.getFaction("Ix");
//        Territory from = game.getTerritory(event.getComponentId().replace("moving-from-", "").replace("ornithopter-", ""));
        Territory from = game.getTerritories().values().stream().filter(territory -> territory.getForces().stream().filter(force -> force.getName().equals("Hidden Mobile Stronghold")).findFirst().isPresent()).findFirst().orElse(null);

        faction.getMovement().setMovingFrom(from.getTerritoryName());
        int spacesCanMove = 1;
//        if (faction.getName().equals("Fremen") || (faction.getName().equals("Ix") && !Objects.equals(from.getForce(faction.getSpecialReserves().getName()).getName(), "")))
//            spacesCanMove = 2;
//        if (ornithopter || game.getTerritory("Arrakeen").getActiveFactions(game).stream().anyMatch(f -> f.getName().equals(faction.getName())) ||
//                game.getTerritory("Carthag").getActiveFactions(game).stream().anyMatch(f -> f.getName().equals(faction.getName())))
            spacesCanMove = 3;
//        if (!faction.getSkilledLeaders().isEmpty() && faction.getSkilledLeaders().get(0).skillCard().name().equals("Planetologist") && spacesCanMove < 3)
//            spacesCanMove++;
        Set<String> moveableTerritories = ShipmentAndMovementButtons.getAdjacentTerritoryNames(from.getTerritoryName().replaceAll("\\(.*\\)", "").strip(), spacesCanMove, game);
        TreeSet<Button> moveToButtons = new TreeSet<>(Comparator.comparing(Button::getLabel));

        moveToButtons.add(Button.danger("hms-pass-movement", "No move"));
        for (String territory : moveableTerritories) {
            moveToButtons.add(Button.primary("hms-move-" + territory, territory));
        }

//        discordGame.getIxChat().queueMessage("Where will you move the HMS?", moveToButtons);
        arrangeButtonsAndSend("Where will your forces move to?", moveToButtons, discordGame);

//        discordGame.pushGame();
    }

    private static void queueSectorButtons(ButtonInteractionEvent event, Game game, DiscordGame discordGame, boolean isShipment) throws ChannelNotFoundException, InvalidOptionException, IOException {
//        String shipmentOrMovement = isShipment ? "ship-" : "move-";
        String shipmentOrMovement = "hms-move-";
        String territoryName = event.getComponentId().replace(shipmentOrMovement, "").replace("-", " ");
        discordGame.queueMessage("You selected " + territoryName);
//        Faction faction = ButtonManager.getButtonPresser(event, game);
        Faction faction = game.getFaction("Ix");
        List<Territory> territory = game.getTerritories().values().stream().filter(t -> t.getTerritoryName().replaceAll("\\s*\\([^\\)]*\\)\\s*", "").equalsIgnoreCase(
                territoryName)
        ).toList();

        if (territory.size() == 1) {
            if (isShipment) faction.getShipment().setTerritoryName(territory.get(0).getTerritoryName());
            else faction.getMovement().setMovingTo(territory.get(0).getTerritoryName());
//            queueForcesButtons(event, game, discordGame, faction, isShipment);
            hmsExecuteFactionMovement(event, discordGame, game, faction);
            discordGame.pushGame();
            return;
        }

        List<Button> buttons = new LinkedList<>();

        for (Territory sector : territory) {
            if (sector.getSpice() > 0)
                buttons.add(Button.primary(shipmentOrMovement + "sector-" + sector.getTerritoryName(), sector.getSector() + " (spice sector)"));
            else
                buttons.add(Button.primary(shipmentOrMovement + "sector-" + sector.getTerritoryName(), String.valueOf(sector.getSector())));
        }
        buttons.sort(ShipmentAndMovementButtons.getButtonComparator());
        String backButtonId = isShipment ? "reset-shipment" : "hms-reset-movement";

        discordGame.queueMessage(new MessageCreateBuilder()
                .setContent("Which sector?")
                .addActionRow(buttons)
                .addActionRow(Button.secondary(backButtonId, "back"))
        );
    }

    private static void hmsFilterBySector(ButtonInteractionEvent event, Game game, DiscordGame discordGame, boolean isShipment) throws ChannelNotFoundException, InvalidOptionException, IOException {
        String shipmentOrMovement = isShipment ? "ship-" : "hms-move-";
        String sector = event.getComponentId().replace(shipmentOrMovement + "sector-", "").replace("-", " ");
        discordGame.queueMessage("You selected sector " + sector);
//        Faction faction = ButtonManager.getButtonPresser(event, game);
        Faction faction = game.getFaction("Ix");
        Territory territory = game.getTerritories().values().stream().filter(t -> t.getTerritoryName().contains(
                sector)
        ).findFirst().orElseThrow();

        if (isShipment) faction.getShipment().setTerritoryName(territory.getTerritoryName());
        else faction.getMovement().setMovingTo(territory.getTerritoryName());

//        queueForcesButtons(event, game, discordGame, faction, isShipment);
        hmsExecuteFactionMovement(event, discordGame, game, faction);
        discordGame.pushGame();
    }

    public static void hmsExecuteFactionMovement(ButtonInteractionEvent event, DiscordGame discordGame, Game game, Faction faction) throws ChannelNotFoundException, InvalidOptionException, IOException {
        Movement movement = faction.getMovement();
        String movingFrom = movement.getMovingFrom();
        String movingTo = movement.getMovingTo();
        boolean movingNoField = movement.isMovingNoField();
//        int force = movement.getForce();
        int force = 1;
        int specialForce = movement.getSpecialForce();
        int secondForce = movement.getSecondForce();
        int secondSpecialForce = movement.getSecondSpecialForce();
        String secondMovingFrom = movement.getSecondMovingFrom();
        Territory from = game.getTerritory(movingFrom);
        Territory to = game.getTerritory(movingTo);
        if (movingNoField) {
            to.setRicheseNoField(from.getRicheseNoField());
            from.setRicheseNoField(null);
            discordGame.getTurnSummary().queueMessage(Emojis.RICHESE + " move their No-Field token to " + to.getTerritoryName());
        }
        if (force != 0 || specialForce != 0) {
//            CommandManager.moveForces(faction, from, to, force, specialForce, discordGame, game);
//            IxCommands.moveHMS();
            for (Territory territory : game.getTerritories().values()) {
                territory.getForces().removeIf(f -> f.getName().equals("Hidden Mobile Stronghold"));
                game.removeTerritoryFromAnotherTerritory(game.getTerritory("Hidden Mobile Stronghold"), territory);
            }
//            IxCommands.placeHMS();
            Territory targetTerritory = to;
            targetTerritory.getForces().add(new Force("Hidden Mobile Stronghold", 1));
            game.putTerritoryInAnotherTerritory(game.getTerritory("Hidden Mobile Stronghold"), targetTerritory);
            discordGame.getTurnSummary().queueMessage(Emojis.IX + " moved the HMS to " + targetTerritory.getTerritoryName() + ".");
            discordGame.pushGame();
        }
        if (secondForce != 0 || secondSpecialForce != 0) {
            discordGame.getTurnSummary().queueMessage(faction.getEmoji() + " use Planetologist to move another force to " + movingTo);
            CommandManager.moveForces(faction, game.getTerritory(secondMovingFrom), to, secondForce, secondSpecialForce, discordGame, game);
        }
        movement.clear();
        deleteAllButtonsInChannel(event.getMessageChannel());
        if (game.hasGameOption(GameOption.MAP_IN_FRONT_OF_SHIELD))
            game.setUpdated(UpdateType.MAP);
        else
            ShowCommands.showBoard(discordGame, game);
    }

    public static void arrangeButtonsAndSend(String message, TreeSet<Button> buttons, DiscordGame discordGame) throws ChannelNotFoundException {
        List<MessageCreateBuilder> messagesToQueue = new LinkedList<>();
        MessageCreateBuilder messageCreateBuilder = new MessageCreateBuilder();
        messageCreateBuilder.setContent(message);
        int count = 0;
        while (!buttons.isEmpty()) {
            List<Button> actionRow = new LinkedList<>();
            for (int i = 0; i < 5; i++) {
                if (!buttons.isEmpty()) actionRow.add(buttons.pollFirst());
            }
            messageCreateBuilder.addActionRow(actionRow);
            count++;
            if (count == 5 || buttons.isEmpty()) {
                messagesToQueue.add(messageCreateBuilder);
                messageCreateBuilder = new MessageCreateBuilder()
                        .addContent("cont.");
                count = 0;
            }
        }

        FactionChat ixChat = discordGame.getIxChat();
        for (MessageCreateBuilder mcb : messagesToQueue) {
            ixChat.queueMessage(mcb);
        }
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
