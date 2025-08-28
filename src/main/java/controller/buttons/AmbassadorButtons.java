package controller.buttons;

import constants.Emojis;
import controller.DiscordGame;
import enums.MoveType;
import exceptions.ChannelNotFoundException;
import exceptions.InvalidGameStateException;
import model.Game;
import model.factions.EcazFaction;
import model.factions.Faction;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;

import java.io.IOException;

public class AmbassadorButtons {
    public static void press(ButtonInteractionEvent event, Game game, DiscordGame discordGame) throws ChannelNotFoundException, IOException, InvalidGameStateException {
        // Buttons handled by this class must begin with "ambassador"
        // And any button that begins with "ambassador" must be handled by this class
        if (event.getComponentId().startsWith("ambassador-guild-")) handleGuildAmbassadorButtons(event, game, discordGame);
        else if (event.getComponentId().startsWith("ambassador-fremen-")) handleFremenAmbassadorButtons(event, game, discordGame);
    }

    private static void handleGuildAmbassadorButtons(ButtonInteractionEvent event, Game game, DiscordGame discordGame) throws ChannelNotFoundException, InvalidGameStateException, IOException {
        String action = event.getComponentId().replace("ambassador-guild-", "");
        if (action.equals("pass-shipment")) passGuildAmbassador(event, game, discordGame);
        else if (action.equals("reset-shipment")) resetGuildAmbassador(event, game, discordGame);
        else if (action.equals("stronghold")) ShipmentAndMovementButtons.presentStrongholdShippingChoices(event, game, discordGame);
        else if (action.equals("spice-blow")) ShipmentAndMovementButtons.presentSpiceBlowShippingChoices(event, discordGame, game);
        else if (action.equals("rock")) ShipmentAndMovementButtons.presentRockShippingChoices(event, discordGame, game);
        else if (action.equals("discovery-tokens")) ShipmentAndMovementButtons.presentDiscoveryShippingChoices(event, game, discordGame);
        else if (action.equals("other")) ShipmentAndMovementButtons.presentOtherShippingChoices(event, discordGame, game);
        else if (action.startsWith("ship-sector-")) ShipmentAndMovementButtons.filterBySectorWithPrefix(event, game, discordGame, true);
        else if (action.startsWith("ship-")) ShipmentAndMovementButtons.presentSectorChoices(event, game, discordGame, true);
        else if (action.startsWith("add-force-shipment-")) ShipmentAndMovementButtons.presentAddForcesChoices(event, game, discordGame, true);
        else if (action.equals("reset-shipping-forces")) ShipmentAndMovementButtons.resetForcesWithPrefix(event, game, discordGame, true);
        else if (action.equals("execute-shipment")) ShipmentAndMovementButtons.executeShipmentWithPrefixes(event, game, discordGame, true);
    }

    private static void passGuildAmbassador(ButtonInteractionEvent event, Game game, DiscordGame discordGame) throws ChannelNotFoundException {
        Faction faction = ButtonManager.getButtonPresser(event, game);
        faction.getChat().reply("You will not ship with your Guild ambassador.");
        game.getTurnSummary().publish(Emojis.ECAZ + " does not ship with their Guild ambassador.");
        faction.getShipment().clear();
        faction.getMovement().setMoveType(MoveType.TBD);
        discordGame.pushGame();
    }

    private static void resetGuildAmbassador(ButtonInteractionEvent event, Game game, DiscordGame discordGame) throws ChannelNotFoundException {
        Faction faction = ButtonManager.getButtonPresser(event, game);
        faction.getShipment().clear();
        faction.getShipment().setShipped(false);
        ((EcazFaction) faction).presentGuildAmbassadorDestinationChoices();
        ShipmentAndMovementButtons.deleteButtonsInChannelWithPrefix(event.getMessageChannel(), "ambassador-guild-");
//        discordGame.queueDeleteMessage();
        discordGame.pushGame();
    }

    private static void handleFremenAmbassadorButtons(ButtonInteractionEvent event, Game game, DiscordGame discordGame) throws ChannelNotFoundException, InvalidGameStateException {
        String action = event.getComponentId().replace("ambassador-fremen-", "");
        if (action.equals("pass-shipment")) passFremenAmbassador(event, game, discordGame);
        else if (action.equals("reset-shipment")) resetFremenAmbassador(event, game, discordGame);
        else if (action.equals("stronghold")) ShipmentAndMovementButtons.presentStrongholdShippingChoices(event, game, discordGame);
        else if (action.equals("spice-blow")) ShipmentAndMovementButtons.presentSpiceBlowShippingChoices(event, discordGame, game);
        else if (action.equals("rock")) ShipmentAndMovementButtons.presentRockShippingChoices(event, discordGame, game);
        else if (action.equals("discovery-tokens")) ShipmentAndMovementButtons.presentDiscoveryShippingChoices(event, game, discordGame);
        else if (action.equals("other")) ShipmentAndMovementButtons.presentOtherShippingChoices(event, discordGame, game);
        else if (action.startsWith("ship-sector-")) ShipmentAndMovementButtons.filterBySectorWithPrefix(event, game, discordGame, true);
        else if (action.startsWith("ship-")) ShipmentAndMovementButtons.presentSectorChoices(event, game, discordGame, true);
        else if (action.startsWith("add-force-movement-")) ShipmentAndMovementButtons.presentAddForcesChoices(event, game, discordGame, false);
        else if (action.equals("reset-moving-forces")) ShipmentAndMovementButtons.resetForcesWithPrefix(event, game, discordGame, true);
        else if (action.equals("execute-movement")) ShipmentAndMovementButtons.executeFremenAmbassador(event, game, discordGame);
    }

    private static void passFremenAmbassador(ButtonInteractionEvent event, Game game, DiscordGame discordGame) throws ChannelNotFoundException {
        Faction faction = ButtonManager.getButtonPresser(event, game);
        game.getTurnSummary().publish(faction.getEmoji() + " does not ride the worm.");
        faction.getMovement().clear();
        faction.getMovement().setMoveType(MoveType.TBD);
//        ((FremenFaction) faction).setWormRideActive(false);
        discordGame.queueMessage("You will not ride the worm.");
        ShipmentAndMovementButtons.deleteButtonsInChannelWithPrefix(event.getMessageChannel(), "ambassador-fremen-");
        discordGame.pushGame();
    }

    private static void resetFremenAmbassador(ButtonInteractionEvent event, Game game, DiscordGame discordGame) throws ChannelNotFoundException {
        Faction faction = ButtonManager.getButtonPresser(event, game);
        faction.getMovement().clear();
        faction.getMovement().setMoved(false);
        ((EcazFaction) faction).presentFremenAmbassadorRideFromChoices();
        ShipmentAndMovementButtons.deleteButtonsInChannelWithPrefix(event.getMessageChannel(), "ambassador-fremen-");
//        discordGame.queueDeleteMessage();
        discordGame.pushGame();
    }
}
