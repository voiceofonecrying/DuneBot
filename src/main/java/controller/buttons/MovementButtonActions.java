package controller.buttons;

import controller.DiscordGame;
import exceptions.ChannelNotFoundException;
import exceptions.InvalidGameStateException;
import model.Game;
import model.Territory;
import model.factions.Faction;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;

import java.util.List;

public class MovementButtonActions {
    protected static void handleMovementAction(ButtonInteractionEvent event, DiscordGame discordGame, Game game, String action) throws ChannelNotFoundException, InvalidGameStateException {
        if (action.equals("pass")) pass(event, game, discordGame);
        else if (action.equals("start-over")) startOver(event, game, discordGame);
        else if (action.equals("stronghold")) presentStrongholdShippingChoices(event, game, discordGame);
        else if (action.equals("spice-blow")) presentSpiceBlowShippingChoices(event, discordGame, game);
        else if (action.equals("rock")) presentRockShippingChoices(event, discordGame, game);
        else if (action.equals("discovery-tokens")) presentDiscoveryShippingChoices(event, game, discordGame);
        else if (action.equals("other")) presentOtherShippingChoices(event, discordGame, game);
        else if (action.startsWith("territory-")) presentSectorChoices(event, game, discordGame);
        else if (action.startsWith("sector-")) filterBySector(event, game, discordGame);
        else if (action.startsWith("add-force-")) addRegularForces(event, game, discordGame);
        else if (action.equals("reset-forces")) resetForces(event, game, discordGame);
        else if (action.equals("execute")) execute(event, game, discordGame);
    }

    protected static void pass(ButtonInteractionEvent event, Game game, DiscordGame discordGame) throws ChannelNotFoundException {
        Faction faction = ButtonManager.getButtonPresser(event, game);
        ShipmentAndMovementButtons.deleteButtonsInChannelWithPrefix(event.getMessageChannel(), faction.getMovement().getChoicePrefix());
        faction.getMovement().pass(game, faction);
        discordGame.pushGame();
    }

    protected static void startOver(ButtonInteractionEvent event, Game game, DiscordGame discordGame) throws ChannelNotFoundException {
        Faction faction = ButtonManager.getButtonPresser(event, game);
        faction.getMovement().startOver(faction);
        ShipmentAndMovementButtons.deleteButtonsInChannelWithPrefix(event.getMessageChannel(), faction.getMovement().getChoicePrefix());
//        discordGame.queueDeleteMessage();
        discordGame.pushGame();
    }

    protected static void presentStrongholdShippingChoices(ButtonInteractionEvent event, Game game, DiscordGame discordGame) {
        Faction faction = ButtonManager.getButtonPresser(event, game);
        faction.getMovement().presentStrongholdChoices(game, faction);
        discordGame.queueDeleteMessage();
    }

    protected static void presentSpiceBlowShippingChoices(ButtonInteractionEvent event, DiscordGame discordGame, Game game) {
        Faction faction = ButtonManager.getButtonPresser(event, game);
        faction.getMovement().presentSpiceBlowChoices(game, faction);
        discordGame.queueDeleteMessage();
    }

    protected static void presentRockShippingChoices(ButtonInteractionEvent event, DiscordGame discordGame, Game game) {
        Faction faction = ButtonManager.getButtonPresser(event, game);
        faction.getMovement().presentRockChoices(game, faction);
        discordGame.queueDeleteMessage();
    }

    protected static void presentDiscoveryShippingChoices(ButtonInteractionEvent event, Game game, DiscordGame discordGame) {
        Faction faction = ButtonManager.getButtonPresser(event, game);
        faction.getMovement().presentDiscoveryTokenChoices(game, faction);
        discordGame.queueDeleteMessage();
    }

    protected static void presentOtherShippingChoices(ButtonInteractionEvent event, DiscordGame discordGame, Game game) {
        Faction faction = ButtonManager.getButtonPresser(event, game);
        faction.getMovement().presentNonSpiceNonRockChoices(game, faction);
        discordGame.queueDeleteMessage();
    }

    protected static void presentSectorChoices(ButtonInteractionEvent event, Game game, DiscordGame discordGame) throws ChannelNotFoundException {
        Faction faction = ButtonManager.getButtonPresser(event, game);
        String choicePrefix = faction.getMovement().getChoicePrefix();
        String aggregateTerritoryName = event.getComponentId().replace("territory-", "").replace(choicePrefix, "");
        List<Territory> territorySectors = game.getTerritories().getTerritorySectorsInStormOrder(aggregateTerritoryName);

        if (territorySectors.size() == 1) {
            faction.getMovement().setMovingTo(territorySectors.getFirst().getTerritoryName());
            presentForcesChoices(event, game, faction);
            discordGame.pushGame();
        } else {
            faction.getMovement().presentSectorChoices(faction, aggregateTerritoryName, territorySectors);
        }
        ShipmentAndMovementButtons.deleteButtonsInChannelWithPrefix(event.getMessageChannel(), choicePrefix);
    }

    protected static void filterBySector(ButtonInteractionEvent event, Game game, DiscordGame discordGame) throws ChannelNotFoundException {
        Faction faction = ButtonManager.getButtonPresser(event, game);
        String choicePrefix = faction.getMovement().getChoicePrefix();
        String territoryName = event.getComponentId().replace("sector-", "").replace(choicePrefix, "");
        faction.getMovement().setMovingTo(territoryName);
        presentForcesChoices(event, game, faction);
        discordGame.pushGame();
    }

    protected static void addRegularForces(ButtonInteractionEvent event, Game game, DiscordGame discordGame) throws ChannelNotFoundException {
        Faction faction = ButtonManager.getButtonPresser(event, game);
        String choicePrefix = faction.getMovement().getChoicePrefix();
        int addedForces = Integer.parseInt(event.getComponentId().replace("add-force-", "").replace(choicePrefix, ""));
        faction.getMovement().addRegularForces(addedForces);
        presentForcesChoices(event, game, faction);
        discordGame.pushGame();
    }

    protected static void resetForces(ButtonInteractionEvent event, Game game, DiscordGame discordGame) throws ChannelNotFoundException {
        Faction faction = ButtonManager.getButtonPresser(event, game);
        faction.getMovement().resetForces();
        presentForcesChoices(event, game, faction);
        discordGame.pushGame();
    }

    protected static void presentForcesChoices(ButtonInteractionEvent event, Game game, Faction faction) {
        String choicePrefix = faction.getMovement().getChoicePrefix();
        ShipmentAndMovementButtons.deleteButtonsInChannelWithPrefix(event.getMessageChannel(), choicePrefix);
        faction.getMovement().presentForcesChoices(game, faction);
    }

    protected static void execute(ButtonInteractionEvent event, Game game, DiscordGame discordGame) throws ChannelNotFoundException, InvalidGameStateException {
        Faction faction = ButtonManager.getButtonPresser(event, game);
        ShipmentAndMovementButtons.deleteButtonsInChannelWithPrefix(event.getMessageChannel(), faction.getMovement().getChoicePrefix());
        faction.getMovement().execute(game, faction);
        discordGame.pushGame();
    }
}
