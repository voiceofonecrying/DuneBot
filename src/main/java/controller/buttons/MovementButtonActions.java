package controller.buttons;

import controller.DiscordGame;
import exceptions.ChannelNotFoundException;
import model.Game;
import model.factions.Faction;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;

public class MovementButtonActions {
    protected static void pass(ButtonInteractionEvent event, Game game, DiscordGame discordGame) throws ChannelNotFoundException {
        Faction faction = ButtonManager.getButtonPresser(event, game);
        ShipmentAndMovementButtons.deleteButtonsInChannelWithPrefix(event.getMessageChannel(), faction.getMovement().getChoicePrefix());
        faction.getMovement().pass(game, faction);
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
}
