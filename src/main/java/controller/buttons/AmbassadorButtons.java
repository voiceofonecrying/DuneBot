package controller.buttons;

import controller.DiscordGame;
import exceptions.ChannelNotFoundException;
import exceptions.InvalidGameStateException;
import model.DuneChoice;
import model.Game;
import model.Territory;
import model.factions.Faction;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

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
        else if (action.equals("start-over")) MovementButtonActions.startOver(event, game, discordGame);
        else if (action.equals("stronghold")) MovementButtonActions.presentStrongholdShippingChoices(event, game, discordGame);
        else if (action.equals("spice-blow")) MovementButtonActions.presentSpiceBlowShippingChoices(event, discordGame, game);
        else if (action.equals("rock")) MovementButtonActions.presentRockShippingChoices(event, discordGame, game);
        else if (action.equals("discovery-tokens")) MovementButtonActions.presentDiscoveryShippingChoices(event, game, discordGame);
        else if (action.equals("other")) MovementButtonActions.presentOtherShippingChoices(event, discordGame, game);
        else if (action.startsWith("territory-")) presentSectorChoices(event, game, discordGame);
        else if (action.startsWith("sector-")) filterBySector(event, game, discordGame);
        else if (action.startsWith("add-force-")) MovementButtonActions.addRegularForces(event, game, discordGame);
        else if (action.equals("reset-forces")) MovementButtonActions.resetForces(event, game, discordGame);
        else if (action.equals("execute")) MovementButtonActions.execute(event, game, discordGame);
    }

    private static void handleFremenAmbassadorButtons(ButtonInteractionEvent event, Game game, DiscordGame discordGame) throws ChannelNotFoundException, InvalidGameStateException {
        String action = event.getComponentId().replace("ambassador-fremen-", "");
        if (action.equals("pass")) MovementButtonActions.pass(event, game, discordGame);
        else if (action.equals("start-over")) MovementButtonActions.startOver(event, game, discordGame);
        else if (action.equals("stronghold")) MovementButtonActions.presentStrongholdShippingChoices(event, game, discordGame);
        else if (action.equals("spice-blow")) MovementButtonActions.presentSpiceBlowShippingChoices(event, discordGame, game);
        else if (action.equals("rock")) MovementButtonActions.presentRockShippingChoices(event, discordGame, game);
        else if (action.equals("discovery-tokens")) MovementButtonActions.presentDiscoveryShippingChoices(event, game, discordGame);
        else if (action.equals("other")) MovementButtonActions.presentOtherShippingChoices(event, discordGame, game);
        else if (action.startsWith("territory-")) presentSectorChoices(event, game, discordGame);
        else if (action.startsWith("sector-")) filterBySector(event, game, discordGame);
        else if (action.startsWith("add-force-")) MovementButtonActions.addRegularForces(event, game, discordGame);
        else if (action.equals("reset-forces")) MovementButtonActions.resetForces(event, game, discordGame);
        else if (action.equals("execute")) MovementButtonActions.execute(event, game, discordGame);
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
            MovementButtonActions.presentForcesChoices(event, game, discordGame, faction);
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
        MovementButtonActions.presentForcesChoices(event, game, discordGame, faction);
        discordGame.pushGame();
    }
}
