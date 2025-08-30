package controller.buttons;

import controller.DiscordGame;
import enums.MoveType;
import exceptions.ChannelNotFoundException;
import exceptions.InvalidGameStateException;
import model.DuneChoice;
import model.Game;
import model.Movement;
import model.Territory;
import model.factions.EcazFaction;
import model.factions.Faction;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.TreeSet;

public class AmbassadorButtons {
    public static void press(ButtonInteractionEvent event, Game game, DiscordGame discordGame) throws ChannelNotFoundException, IOException, InvalidGameStateException {
        // Buttons handled by this class must begin with "ambassador"
        // And any button that begins with "ambassador" must be handled by this class
        if (event.getComponentId().startsWith("ambassador-guild-")) handleGuildAmbassadorButtons(event, game, discordGame);
        else if (event.getComponentId().startsWith("ambassador-fremen-")) handleFremenAmbassadorButtons(event, game, discordGame);
    }

    private static void handleGuildAmbassadorButtons(ButtonInteractionEvent event, Game game, DiscordGame discordGame) throws ChannelNotFoundException, InvalidGameStateException {
        String action = event.getComponentId().replace("ambassador-guild-", "");
        if (action.equals("pass")) MovementButtonActions.pass(event, game, discordGame);
        else if (action.equals("start-over")) resetGuildAmbassador(event, game, discordGame);
        else if (action.equals("stronghold")) MovementButtonActions.presentStrongholdShippingChoices(event, game, discordGame);
        else if (action.equals("spice-blow")) MovementButtonActions.presentSpiceBlowShippingChoices(event, discordGame, game);
        else if (action.equals("rock")) MovementButtonActions.presentRockShippingChoices(event, discordGame, game);
        else if (action.equals("discovery-tokens")) MovementButtonActions.presentDiscoveryShippingChoices(event, game, discordGame);
        else if (action.equals("other")) MovementButtonActions.presentOtherShippingChoices(event, discordGame, game);
        else if (action.startsWith("territory-")) presentSectorChoices(event, game, discordGame);
        else if (action.startsWith("sector-")) filterBySector(event, game, discordGame);
        else if (action.startsWith("add-force-")) addRegularForces(event, game, discordGame);
        else if (action.equals("reset-forces")) resetForces(event, game, discordGame);
        else if (action.equals("execute")) execute(event, game, discordGame);
    }

    private static void resetGuildAmbassador(ButtonInteractionEvent event, Game game, DiscordGame discordGame) throws ChannelNotFoundException {
        Faction faction = ButtonManager.getButtonPresser(event, game);
        faction.getMovement().clear();
        ((EcazFaction) faction).presentGuildAmbassadorDestinationChoices();
        ShipmentAndMovementButtons.deleteButtonsInChannelWithPrefix(event.getMessageChannel(), "ambassador-guild-");
//        discordGame.queueDeleteMessage();
        discordGame.pushGame();
    }

    private static void handleFremenAmbassadorButtons(ButtonInteractionEvent event, Game game, DiscordGame discordGame) throws ChannelNotFoundException, InvalidGameStateException {
        String action = event.getComponentId().replace("ambassador-fremen-", "");
        if (action.equals("pass")) MovementButtonActions.pass(event, game, discordGame);
        else if (action.equals("start-over")) resetFremenAmbassador(event, game, discordGame);
        else if (action.equals("stronghold")) MovementButtonActions.presentStrongholdShippingChoices(event, game, discordGame);
        else if (action.equals("spice-blow")) MovementButtonActions.presentSpiceBlowShippingChoices(event, discordGame, game);
        else if (action.equals("rock")) MovementButtonActions.presentRockShippingChoices(event, discordGame, game);
        else if (action.equals("discovery-tokens")) MovementButtonActions.presentDiscoveryShippingChoices(event, game, discordGame);
        else if (action.equals("other")) MovementButtonActions.presentOtherShippingChoices(event, discordGame, game);
        else if (action.startsWith("territory-")) presentSectorChoices(event, game, discordGame);
        else if (action.startsWith("sector-")) filterBySector(event, game, discordGame);
        else if (action.startsWith("add-force-")) addRegularForces(event, game, discordGame);
        else if (action.equals("reset-forces")) resetForces(event, game, discordGame);
        else if (action.equals("execute")) execute(event, game, discordGame);
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

    protected static void presentSectorChoices(ButtonInteractionEvent event, Game game, DiscordGame discordGame) throws ChannelNotFoundException {
        Faction faction = ButtonManager.getButtonPresser(event, game);
        String choicePrefix = faction.getMovement().getChoicePrefix();
        String aggregateTerritoryName = event.getComponentId().replace("territory-", "")
                .replace(choicePrefix, "");
        List<Territory> territory = new ArrayList<>(game.getTerritories().values().stream().filter(t -> t.getTerritoryName().replaceAll("\\s*\\([^)]*\\)\\s*", "").equalsIgnoreCase(aggregateTerritoryName)).toList());
        territory.sort(Comparator.comparingInt(Territory::getSector));
        if (aggregateTerritoryName.equals("Cielago North") || aggregateTerritoryName.equals("Cielago Depression") || aggregateTerritoryName.equals("Meridian")) {
            territory.addFirst(territory.removeLast());
        }

        if (territory.size() == 1) {
            faction.getMovement().setMovingTo(territory.getFirst().getTerritoryName());
            presentForcesChoices(event, game, discordGame, faction);
            ShipmentAndMovementButtons.deleteButtonsInChannelWithPrefix(event.getMessageChannel(), choicePrefix);
            discordGame.pushGame();
            return;
        }

        List<DuneChoice> choices = new ArrayList<>();
        for (Territory sector : territory) {
            int sectorNameStart = sector.getTerritoryName().indexOf("(");
            String sectorName = sector.getTerritoryName().substring(sectorNameStart + 1, sector.getTerritoryName().length() - 1);
            String spiceString = "";
            if (sector.getSpice() > 0)
                spiceString = " (" + sector.getSpice() + " spice)";
            choices.add(new DuneChoice(choicePrefix + "sector-" + sector.getTerritoryName(), sector.getSector() + " - " + sectorName + spiceString));
        }
        choices.add(new DuneChoice("secondary", choicePrefix + "start-over", "Start over"));
        ShipmentAndMovementButtons.deleteButtonsInChannelWithPrefix(event.getMessageChannel(), choicePrefix);
        faction.getChat().reply("Which sector of " + aggregateTerritoryName + "?", choices);
    }

    private static void filterBySector(ButtonInteractionEvent event, Game game, DiscordGame discordGame) throws ChannelNotFoundException {
        Faction faction = ButtonManager.getButtonPresser(event, game);
        String choicePrefix = faction.getMovement().getChoicePrefix();
        Territory territory = game.getTerritories().values().stream().filter(t -> t.getTerritoryName().contains(
                        event.getComponentId().replace("sector-", "")
                                .replace(choicePrefix, "")
                                .replace("-", " ")
                )
        ).findFirst().orElseThrow();
        faction.getMovement().setMovingTo(territory.getTerritoryName());
        presentForcesChoices(event, game, discordGame, faction);
        discordGame.pushGame();
    }

    private static void addRegularForces(ButtonInteractionEvent event, Game game, DiscordGame discordGame) throws ChannelNotFoundException {
        Faction faction = ButtonManager.getButtonPresser(event, game);
        String choicePrefix = faction.getMovement().getChoicePrefix();
        int currentForces = faction.getMovement().getForce();
        int addedForces = Integer.parseInt(event.getComponentId().replace("add-force-", "").replace(choicePrefix, ""));
        faction.getMovement().setForce(currentForces + addedForces);
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
        forcesButtons.add(Button.danger(choicePrefix + "start-over", "Start over"));
        ShipmentAndMovementButtons.arrangeButtonsAndSend(message, forcesButtons, discordGame);
    }

    protected static void resetForces(ButtonInteractionEvent event, Game game, DiscordGame discordGame) throws ChannelNotFoundException {
        Faction faction = ButtonManager.getButtonPresser(event, game);
        faction.getMovement().setForce(0);
        faction.getMovement().setSpecialForce(0);
        presentForcesChoices(event, game, discordGame, faction);
        discordGame.pushGame();
    }

    private static void execute(ButtonInteractionEvent event, Game game, DiscordGame discordGame) throws ChannelNotFoundException, InvalidGameStateException {
        Faction faction = ButtonManager.getButtonPresser(event, game);
        ShipmentAndMovementButtons.deleteButtonsInChannelWithPrefix(event.getMessageChannel(), faction.getMovement().getChoicePrefix());
        faction.getMovement().execute(game, faction);
        discordGame.pushGame();
    }
}
