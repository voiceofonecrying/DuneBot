package controller.buttons;

import constants.Emojis;
import controller.DiscordGame;
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
//        else if (event.getComponentId().startsWith("ambassador-fremen-")) handleFremenAmbassadorButtons(event, game, discordGame);
    }

    private static void handleGuildAmbassadorButtons(ButtonInteractionEvent event, Game game, DiscordGame discordGame) throws ChannelNotFoundException, InvalidGameStateException, IOException {
        String action = event.getComponentId().replace("ambassador-guild-", "");
        if (action.equals("pass-shipment")) passGuildAmbassador(event, game, discordGame);
        else if (action.equals("reset-shipment")) resetGuildAmbassador(event, game, discordGame);
        else if (action.equals("stronghold")) ShipmentAndMovementButtons.presentStrongholdShippingChoices(event, game, discordGame, "ambassador-guild-");
        else if (action.equals("spice-blow")) ShipmentAndMovementButtons.presentSpiceBlowShippingChoices(event, discordGame, game, "ambassador-guild-");
        else if (action.equals("rock")) ShipmentAndMovementButtons.presentRockShippingChoices(event, discordGame, game, "ambassador-guild-");
        else if (action.equals("discovery-tokens")) ShipmentAndMovementButtons.presentDiscoveryShippingChoices(event, game, discordGame, "ambassador-guild-");
        else if (action.equals("other")) ShipmentAndMovementButtons.presentOtherShippingChoices(event, discordGame, game, "ambassador-guild-");
        else if (action.startsWith("ship-sector-")) ShipmentAndMovementButtons.filterBySectorWithPrefix(event, game, discordGame, true);
        else if (action.startsWith("ship-")) ShipmentAndMovementButtons.presentSectorChoices(event, game, discordGame, true, "ambassador-guild-");
        else if (action.startsWith("add-force-shipment-")) ShipmentAndMovementButtons.presentAddForcesChoices(event, game, discordGame, true);
        else if (action.equals("reset-shipping-forces")) ShipmentAndMovementButtons.resetForcesWithPrefix(event, game, discordGame, true);
        else if (action.equals("execute-shipment")) ShipmentAndMovementButtons.executeShipmentWithPrefixes(event, game, discordGame, true);
    }

    private static void passGuildAmbassador(ButtonInteractionEvent event, Game game, DiscordGame discordGame) throws ChannelNotFoundException {
        Faction faction = ButtonManager.getButtonPresser(event, game);
        faction.getChat().reply("You will not ship with your Guild ambassador.");
        game.getTurnSummary().publish(Emojis.ECAZ + " does not ship with their Guild ambassador.");
        faction.getShipment().clear();
        discordGame.pushGame();
    }

    private static void resetGuildAmbassador(ButtonInteractionEvent event, Game game, DiscordGame discordGame) throws ChannelNotFoundException {
        Faction faction = ButtonManager.getButtonPresser(event, game);
        faction.getShipment().clear();
        faction.getShipment().setShipped(false);
        ((EcazFaction) faction).presentGuildAmbassadorDestinationChoices();
        ShipmentAndMovementButtons.deleteButtonsInChannelWithPrefix(event.getMessageChannel(), "ambassador-guild-");
        discordGame.queueDeleteMessage();
        discordGame.pushGame();
    }

//    private static void handleFremenAmbassadorButtons(ButtonInteractionEvent event, Game game, DiscordGame discordGame) throws ChannelNotFoundException {
//        Faction faction = ButtonManager.getButtonPresser(event, game);
//        String action = event.getComponentId().replace("ambassador-fremen-", "");
//    }
}
