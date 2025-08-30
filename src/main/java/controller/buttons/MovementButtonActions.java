package controller.buttons;

import controller.DiscordGame;
import enums.MoveType;
import exceptions.ChannelNotFoundException;
import exceptions.InvalidGameStateException;
import model.Game;
import model.Movement;
import model.Territory;
import model.factions.Faction;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;

import java.util.List;
import java.util.TreeSet;

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
            presentForcesChoices(event, game, discordGame, faction);
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
        presentForcesChoices(event, game, discordGame, faction);
        discordGame.pushGame();
    }

    protected static void addRegularForces(ButtonInteractionEvent event, Game game, DiscordGame discordGame) throws ChannelNotFoundException {
        Faction faction = ButtonManager.getButtonPresser(event, game);
        String choicePrefix = faction.getMovement().getChoicePrefix();
        int addedForces = Integer.parseInt(event.getComponentId().replace("add-force-", "").replace(choicePrefix, ""));
        faction.getMovement().addRegularForces(addedForces);
        presentForcesChoices(event, game, discordGame, faction);
        discordGame.pushGame();
    }

    protected static void resetForces(ButtonInteractionEvent event, Game game, DiscordGame discordGame) throws ChannelNotFoundException {
        Faction faction = ButtonManager.getButtonPresser(event, game);
        faction.getMovement().resetForces();
        presentForcesChoices(event, game, discordGame, faction);
        discordGame.pushGame();
    }

    protected static void presentForcesChoices(ButtonInteractionEvent event, Game game, DiscordGame discordGame, Faction faction) {
        boolean guildAmbassador = faction.getMovement().getMoveType() == MoveType.GUILD_AMBASSADOR;
        boolean fremenAmbassador = faction.getMovement().getMoveType() == MoveType.FREMEN_AMBASSADOR;
        boolean fremenRide = faction.getMovement().getMoveType() == MoveType.FREMEN_RIDE;
        String choicePrefix = faction.getMovement().getChoicePrefix();
        boolean wormRide = fremenRide || fremenAmbassador;
        ShipmentAndMovementButtons.deleteButtonsInChannelWithPrefix(event.getMessageChannel(), choicePrefix);

        TreeSet<Button> forcesButtons = new TreeSet<>(ShipmentAndMovementButtons.getButtonComparator());
        int buttonLimitForces = guildAmbassador ? faction.getReservesStrength() - faction.getMovement().getForce() :
                game.getTerritory(faction.getMovement().getMovingFrom()).getForceStrength(faction.getName()) - faction.getMovement().getForce();
        if (guildAmbassador)
            buttonLimitForces = Math.min(4, buttonLimitForces);
        int buttonLimitSpecialForces = guildAmbassador ? faction.getSpecialReservesStrength() - faction.getMovement().getSpecialForce() :
                game.getTerritory(faction.getMovement().getMovingFrom()).getForceStrength(faction.getName() + "*") - faction.getMovement().getSpecialForce();

        for (int i = 0; i < buttonLimitForces; i++) {
            forcesButtons.add(Button.primary(choicePrefix + "add-force-" + (i + 1), "Add " + (i + 1) + " troop")
                    .withDisabled(guildAmbassador && i + 1 + faction.getMovement().getForce() > 4));
        }
        for (int i = 0; i < buttonLimitSpecialForces; i++) {
            forcesButtons.add(Button.primary(choicePrefix + "add-special-force-" + (i + 1), "Add " + (i + 1) + " * troop")
                    .withDisabled(guildAmbassador && i + 1 + faction.getMovement().getSpecialForce() > 4));
        }

        Movement movement = faction.getMovement();
        String message = "Use buttons below to add forces to your ";
        if (wormRide)
            message += "ride. Currently moving:";
        else if (guildAmbassador)
            message += "shipment. Currently shipping:";
        else
            message += "movement. Currently moving:";
        message += "\n**" + faction.forcesStringWithZeroes(faction.getMovement().getForce(), faction.getMovement().getSpecialForce()) + "** to " + faction.getMovement().getMovingTo();
        if (movement.getForce() != 0 || movement.getSpecialForce() != 0) {
            String executeLabel = "Confirm Movement";
            if (guildAmbassador)
                executeLabel = "Confirm Shipment";
            forcesButtons.add(Button.success(choicePrefix + "execute", executeLabel));
            forcesButtons.add(Button.danger(choicePrefix + "reset-forces", "Reset forces"));
        }
        forcesButtons.add(Button.secondary(choicePrefix + "start-over", "Start over"));
        ShipmentAndMovementButtons.arrangeButtonsAndSend(message, forcesButtons, discordGame);
    }

    protected static void execute(ButtonInteractionEvent event, Game game, DiscordGame discordGame) throws ChannelNotFoundException, InvalidGameStateException {
        Faction faction = ButtonManager.getButtonPresser(event, game);
        ShipmentAndMovementButtons.deleteButtonsInChannelWithPrefix(event.getMessageChannel(), faction.getMovement().getChoicePrefix());
        faction.getMovement().execute(game, faction);
        discordGame.pushGame();
    }
}
