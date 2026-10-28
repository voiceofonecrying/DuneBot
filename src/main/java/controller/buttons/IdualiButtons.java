package controller.buttons;

import constants.Emojis;
import exceptions.ChannelNotFoundException;
import controller.DiscordGame;
import controller.buttons.ButtonManager;
import controller.buttons.MovementButtonActions;
import controller.buttons.Pressable;
import controller.buttons.ShipmentAndMovementButtons;
import controller.commands.SetupCommands;
import enums.MoveType;
import exceptions.InvalidGameStateException;
import model.Game;
import model.Territories;
import model.Territory;
import model.factions.Faction;
import model.factions.IdualiFaction;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;

import java.io.IOException;
import java.util.List;
import java.util.TreeSet;

import static controller.buttons.ShipmentAndMovementButtons.arrangeButtonsAndSend;
import static controller.buttons.ShipmentAndMovementButtons.getButtonComparator;

public class IdualiButtons implements Pressable {
    public static void press(ButtonInteractionEvent event, Game game, DiscordGame discordGame) throws ChannelNotFoundException, InvalidGameStateException, IOException {
        // Buttons handled by this class must begin with "iduali"
        // And any button that begins with "iduali" must be handled by this class
        if (event.getComponentId().startsWith("iduali-place-territory-")) placeTerritory(event, game, discordGame);
        else if (event.getComponentId().equals("iduali-cave")) caveMove(event, game, discordGame);
        else if (event.getComponentId().startsWith("iduali-cave-from-")) setCaveFrom(event, game, discordGame);
        else if (event.getComponentId().startsWith("iduali-ride-")) handleIdualiRideButtons(event, discordGame);
    }

    private static void caveMove(ButtonInteractionEvent event, Game game, DiscordGame discordGame) throws ChannelNotFoundException {
        Faction faction = ButtonManager.getButtonPresser(event, game);
        TreeSet<Button> buttons = new TreeSet<>(getButtonComparator());
        for (Territory territory : game.getTerritories().values()) {
            if (territory.getActiveFactions(game).contains(faction))
                buttons.add(Button.primary("iduali-cave-from-" + territory.getTerritoryName(), territory.getTerritoryName()));
        }
        faction.getMovement().setMoveType(MoveType.IDUALI_CAVE);
        arrangeButtonsAndSend("Where will you start the cave movement from?", buttons, discordGame);
        discordGame.pushGame();
    }

    private static void setCaveFrom(ButtonInteractionEvent event, Game game, DiscordGame discordGame) throws ChannelNotFoundException {
        IdualiFaction faction = (IdualiFaction) ButtonManager.getButtonPresser(event, game);
        String from = game.getTerritory(event.getComponentId().replace("iduali-cave-from-", "")).getTerritoryName();
        faction.getMovement().setCaveFrom(from);
         faction.getMovement().setMovingFrom(from);
        ShipmentAndMovementButtons.queueCaveDestinations(game, discordGame, faction);
        discordGame.pushGame();
    }

// private static void setCaveFrom(
//         ButtonInteractionEvent event,
//         Game game,
//         DiscordGame discordGame) throws ChannelNotFoundException {

//     IdualiFaction faction = (IdualiFaction) ButtonManager.getButtonPresser(event, game);
//     String from = event.getComponentId().substring("iduali-cave-from-".length());

//     faction.getMovement().setCaveFrom(from);
//     faction.getMovement().setMovingFrom(from);

//     ShipmentAndMovementButtons.queueCaveDestinations(
//             game, discordGame, faction);
// }

    private static void placeTerritory(ButtonInteractionEvent event, Game game, DiscordGame discordGame) throws ChannelNotFoundException, InvalidGameStateException, IOException {
        String prefix = "iduali-place-territory-";
        String[] parts = event.getComponentId()
            .substring(prefix.length())
            .split("-", 3);

        String territory = parts[0];
        IdualiFaction iduali = game.getIdualiFaction();

        if ("reset".equals(territory)) {
            iduali.resetPlacingTerritories();
            discordGame.queueDeleteMessage();
            iduali.presentTerritoriesChoices();
            discordGame.pushGame();
            return;
        }

        if (parts.length == 1) {
            TreeSet<Button> buttons = new TreeSet<>(getButtonComparator());
            List<Integer> usedSectors = iduali.getPlacedSectors();
            for (int i = 1; i <= 18; i++) {
                if (!usedSectors.contains(i))
                    buttons.add(Button.primary(prefix + territory + "-" + i, String.valueOf(i)));
            }

            arrangeButtonsAndSend("Which planet Sector?", buttons, discordGame);
            return;
        }

        int sector;
        try {
            sector = Integer.parseInt(parts[1]);
        } catch (NumberFormatException e) {
            throw new InvalidGameStateException("Invalid sector: " + parts[1]);
        }

        if (sector < 1 || sector > 18)
            throw new InvalidGameStateException("Sector must be between 1 and 18.");

        List<String> sectorTerritories = game.getTerritories().getEquatorialTerritoryNamesInSector(sector);
        String adjacentTerritory;

        if (parts.length == 2) {
            if (sectorTerritories.isEmpty())
                throw new InvalidGameStateException("There are no eligible adjacent territories in sector " + sector + ".");

            if (sectorTerritories.size() == 1) {
                adjacentTerritory = sectorTerritories.getFirst();
            } else {
                TreeSet<Button> buttons = new TreeSet<>(getButtonComparator());
                for (String t : sectorTerritories)
                    buttons.add(Button.primary(prefix + territory + "-" + sector + "-" + t, t));
                arrangeButtonsAndSend("Which territory in Sector " + sector + " is adjacent in the Northern Hemisphere?", buttons, discordGame);
                return;
            }
        } else {
            adjacentTerritory = parts[2];
            if (!sectorTerritories.contains(adjacentTerritory))
                throw new InvalidGameStateException("Invalid adjacent territory for sector " + sector + ".");
        }

        int territoriesPlaced = iduali.placeTerritory(territory, sector, adjacentTerritory);

        discordGame.queueDeleteMessage();
        discordGame.queueMessage(territory + " is to the South of " + adjacentTerritory);
        discordGame.pushGame();

        if (territoriesPlaced == 3)
            SetupCommands.advance(event.getGuild(), discordGame, game);
        else
            iduali.presentTerritoriesChoices();
    }

    private static void handleIdualiRideButtons(ButtonInteractionEvent event, DiscordGame discordGame) throws ChannelNotFoundException, InvalidGameStateException, IOException {
        String action = event.getComponentId().replace("iduali-ride-", "");
        MovementButtonActions.handleMovementAction(event, discordGame, action);
    }
}

