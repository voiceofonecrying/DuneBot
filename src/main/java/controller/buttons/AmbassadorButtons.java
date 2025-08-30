package controller.buttons;

import controller.DiscordGame;
import exceptions.ChannelNotFoundException;
import exceptions.InvalidGameStateException;
import model.Game;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;

import java.io.IOException;

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
        else if (action.startsWith("territory-")) MovementButtonActions.presentSectorChoices(event, game, discordGame);
        else if (action.startsWith("sector-")) MovementButtonActions.filterBySector(event, game, discordGame);
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
        else if (action.startsWith("territory-")) MovementButtonActions.presentSectorChoices(event, game, discordGame);
        else if (action.startsWith("sector-")) MovementButtonActions.filterBySector(event, game, discordGame);
        else if (action.startsWith("add-force-")) MovementButtonActions.addRegularForces(event, game, discordGame);
        else if (action.equals("reset-forces")) MovementButtonActions.resetForces(event, game, discordGame);
        else if (action.equals("execute")) MovementButtonActions.execute(event, game, discordGame);
    }
}
