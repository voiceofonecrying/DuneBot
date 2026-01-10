package controller.buttons;

import constants.Emojis;
import controller.DiscordGame;
import exceptions.ChannelNotFoundException;
import exceptions.InvalidGameStateException;
import model.Game;
import model.factions.Faction;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;

import java.io.IOException;

public class AmbassadorButtons {
    public static void press(ButtonInteractionEvent event, DiscordGame discordGame) throws ChannelNotFoundException, IOException, InvalidGameStateException {
        // Buttons handled by this class must begin with "ambassador"
        // And any button that begins with "ambassador" must be handled by this class
        if (event.getComponentId().startsWith("ambassador-trigger-ally-")) triggerAmbassadorForAlly(event, discordGame);
        else if (event.getComponentId().startsWith("ambassador-trigger-")) triggerAmbassador(event, discordGame);
        else if (event.getComponentId().startsWith("ambassador-dont-trigger-")) dontTriggerAmbassador(event, discordGame);
        else if (event.getComponentId().startsWith("ambassador-fremen-")) handleFremenAmbassadorButtons(event, discordGame);
        else if (event.getComponentId().startsWith("ambassador-guild-")) handleGuildAmbassadorButtons(event, discordGame);
        else if (event.getComponentId().startsWith("ambassador-choam-discard-")) choamDiscard(event, discordGame);
    }

    private static void triggerAmbassadorForAlly(ButtonInteractionEvent event, DiscordGame discordGame) throws ChannelNotFoundException {
        Game game = discordGame.getGame();
        discordGame.queueDeleteMessage();
        String ambassador = event.getComponentId().split("-")[3];
        Faction triggeringFaction = game.getFaction(event.getComponentId().split("-")[4]);
        game.getEcazFaction().triggerAmbassador(triggeringFaction, ambassador, true);
        discordGame.pushGame();
    }

    private static void triggerAmbassador(ButtonInteractionEvent event, DiscordGame discordGame) throws ChannelNotFoundException {
        Game game = discordGame.getGame();
        discordGame.queueDeleteMessage();
        String ambassador = event.getComponentId().split("-")[2];
        Faction triggeringFaction = game.getFaction(event.getComponentId().split("-")[3]);
        game.getEcazFaction().triggerAmbassador(triggeringFaction, ambassador, false);
        discordGame.pushGame();
    }

    private static void dontTriggerAmbassador(ButtonInteractionEvent event, DiscordGame discordGame) throws ChannelNotFoundException {
        String ambassador = event.getComponentId().split("-")[3];
        String triggeringFactionName = event.getComponentId().split("-")[4];
        discordGame.getTurnSummary().queueMessage(Emojis.ECAZ + " do not trigger their " + ambassador + " Ambassador against " + Emojis.getFactionEmoji(triggeringFactionName) + ".");
        discordGame.queueMessage("You will not trigger your " + ambassador + " Ambassador against " + Emojis.getFactionEmoji(triggeringFactionName) + ".");
        discordGame.queueDeleteMessage();
    }

    private static void handleFremenAmbassadorButtons(ButtonInteractionEvent event, DiscordGame discordGame) throws ChannelNotFoundException, InvalidGameStateException, IOException {
        String action = event.getComponentId().replace("ambassador-fremen-", "");
        MovementButtonActions.handleMovementAction(event, discordGame, action);
    }

    private static void handleGuildAmbassadorButtons(ButtonInteractionEvent event, DiscordGame discordGame) throws ChannelNotFoundException, InvalidGameStateException, IOException {
        String action = event.getComponentId().replace("ambassador-guild-", "");
        MovementButtonActions.handleMovementAction(event, discordGame, action);
    }

    private static void choamDiscard(ButtonInteractionEvent event, DiscordGame discordGame) throws ChannelNotFoundException {
        Game game = discordGame.getGame();
        Faction faction = ButtonManager.getButtonPresser(event, game);
        String cardName = event.getComponentId().split("-")[3];
        discordGame.queueDeleteMessage();
        faction.discardWithCHOAMAmbassador(cardName);
        discordGame.pushGame();
    }
}
