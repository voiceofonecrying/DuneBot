package controller.buttons;

import constants.Emojis;
import controller.Alliance;
import exceptions.ChannelNotFoundException;
import controller.DiscordGame;
import exceptions.InvalidGameStateException;
import model.Game;
import model.Territory;
import model.factions.Faction;
import model.factions.MoritaniFaction;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;

import java.io.IOException;

public class MoritaniButtons implements Pressable {
    public static void press(ButtonInteractionEvent event, Game game, DiscordGame discordGame) throws ChannelNotFoundException, IOException, InvalidGameStateException {
        // Buttons handled by this class must begin with "moritani"
        // And any button that begins with "moritani" must be handled by this class
        if (event.getComponentId().startsWith("moritani-offer-alliance-")) offerAlliance(event, discordGame, game);
        else if (event.getComponentId().startsWith("moritani-place-terror-"))
            queueTerrorButtons(event, game, discordGame);
        else if (event.getComponentId().startsWith("moritani-move-terror-"))
            queueTerrorInTerritoriesButtons(event, game, discordGame);
        else if (event.getComponentId().startsWith("moritani-move-to-"))
            moveTerrorToken(event, game, discordGame);
        else if (event.getComponentId().startsWith("moritani-terror-selected-"))
            placeTerrorToken(event, game, discordGame);
        else if (event.getComponentId().startsWith("moritani-trigger-terror-"))
            triggerTerrorToken(event, game, discordGame);
        else if (event.getComponentId().startsWith("moritani-remove-terror-"))
            removeTerrorToken(event, game, discordGame);
        else if (event.getComponentId().startsWith("moritani-accept-offer-"))
            acceptAlliance(event, game, discordGame);
        else if (event.getComponentId().startsWith("moritani-deny-offer-"))
            denyAlliance(event, game, discordGame);
        else if (event.getComponentId().startsWith("moritani-robbery-rob-"))
            robberyRob(event, game, discordGame);
        else if (event.getComponentId().startsWith("moritani-robbery-discard-"))
            robberyDiscard(event, game, discordGame);
        else if (event.getComponentId().startsWith("moritani-sabotage-give-card-"))
            giveCard(event, game, discordGame);
        else if (event.getComponentId().startsWith("moritani-sabotage-no-card-"))
            dontGiveCard(event, game, discordGame);
        else if (event.getComponentId().startsWith("moritani-sneak-attack-"))
            sneakAttack(event, game, discordGame);
        switch (event.getComponentId()) {
            case "moritani-don't-trigger-terror" -> dontTrigger(event, game, discordGame);
            case "moritani-robbery-draw" -> robberyDraw(event, game, discordGame);
            case "moritani-don't-place-terror", "moritani-no-move" -> dontPlaceOrMoveTerrorToken(discordGame);
            case "moritani-move-option" -> queueMoveButtons(game, discordGame);
        }
    }

    private static void removeTerrorToken(ButtonInteractionEvent event, Game game, DiscordGame discordGame) throws ChannelNotFoundException {
        String territoryName = event.getComponentId().split("-")[3];
        String terror = event.getComponentId().split("-")[4];
        game.getMoritaniFaction().removeTerrorTokenWithHighThreshold(territoryName, terror);
        discordGame.pushGame();
    }

    private static void dontPlaceOrMoveTerrorToken(DiscordGame discordGame) throws ChannelNotFoundException {
        discordGame.queueDeleteMessage();
        discordGame.queueMessage("You will leave your Terror Tokens as they were.");
        discordGame.getTurnSummary().queueMessage(Emojis.MORITANI + " leaves their Terror Tokens as they were.");
    }

    private static void robberyRob(ButtonInteractionEvent event, Game game, DiscordGame discordGame) throws ChannelNotFoundException {
        Faction faction = ButtonManager.getButtonPresser(event, game);
        if (faction instanceof MoritaniFaction moritani) {
            discordGame.queueMessage("You chose to steal half their spice rounded up.");
            String triggeringFactionName = event.getComponentId().split("-")[3];
            moritani.robberyRob(triggeringFactionName);
            discordGame.queueDeleteMessage();
            discordGame.pushGame();
        }
    }

    private static void robberyDraw(ButtonInteractionEvent event, Game game, DiscordGame discordGame) throws ChannelNotFoundException {
        Faction faction = ButtonManager.getButtonPresser(event, game);
        if (faction instanceof MoritaniFaction moritani) {
            discordGame.queueMessage("You chose to draw a card.");
            moritani.robberyDraw();
            discordGame.queueDeleteMessage();
            discordGame.pushGame();
        }
    }

    private static void robberyDiscard(ButtonInteractionEvent event, Game game, DiscordGame discordGame) throws ChannelNotFoundException {
        Faction faction = ButtonManager.getButtonPresser(event, game);
        if (faction instanceof MoritaniFaction moritani) {
            String cardToDiscard = event.getComponentId().split("-")[3];
            discordGame.queueMessage("You chose to discard " + cardToDiscard);
            moritani.robberyDiscard(cardToDiscard);
            discordGame.queueDeleteMessage();
            discordGame.pushGame();
        }
    }

    private static void dontTrigger(ButtonInteractionEvent event, Game game, DiscordGame discordGame) {
        if (!(ButtonManager.getButtonPresser(event, game) instanceof MoritaniFaction)) return;
        game.getTurnSummary().publish(Emojis.MORITANI + " do not trigger their terror token.");
        discordGame.queueMessage("You will not trigger your terror token.");
        discordGame.queueDeleteMessage();
    }

    private static void triggerTerrorToken(ButtonInteractionEvent event, Game game, DiscordGame discordGame) throws ChannelNotFoundException, InvalidGameStateException {
        String terror = event.getComponentId().split("-")[3];
        Faction triggeringFaction = game.getFaction(event.getComponentId().split("-")[4]);
        Territory territory = game.getTerritories().values().stream().filter(t -> t.getTerrorTokens().contains(terror)).findFirst().orElseThrow();
        discordGame.queueDeleteMessage();
        game.getMoritaniFaction().triggerTerrorToken(triggeringFaction, territory, terror);
        discordGame.pushGame();
    }


    private static void queueMoveButtons(Game game, DiscordGame discordGame) {
        discordGame.queueDeleteMessage();
        game.getMoritaniFaction().presentTerrorTokenMoveChoices();
    }

    private static void moveTerrorToken(ButtonInteractionEvent event, Game game, DiscordGame discordGame) throws ChannelNotFoundException, InvalidGameStateException {
        Territory toTerritory = game.getTerritory(event.getComponentId().split("-")[3]);
        String terror = event.getComponentId().split("-")[4];
        Territory fromTerritory = game.getTerritory(event.getComponentId().split("-")[5]);
        discordGame.queueDeleteMessage();
        game.getMoritaniFaction().moveTerrorToken(toTerritory, terror);
        discordGame.getTurnSummary().queueMessage("The " + Emojis.MORITANI + " terror token in " + fromTerritory.getTerritoryName() + " has been moved to " + toTerritory.getTerritoryName());
        discordGame.pushGame();
    }

    private static void placeTerrorToken(ButtonInteractionEvent event, Game game, DiscordGame discordGame) throws ChannelNotFoundException, InvalidGameStateException {
        String terror = event.getComponentId().split("-")[3];
        Territory territory = game.getTerritory(event.getComponentId().split("-")[4]);
        game.getMoritaniFaction().placeTerrorToken(territory, terror);
        discordGame.queueDeleteMessage();
        discordGame.pushGame();
    }

    private static void queueTerrorButtons(ButtonInteractionEvent event, Game game, DiscordGame discordGame) {
        Territory territory = game.getTerritory(event.getComponentId().split("-")[3]);
        discordGame.queueDeleteMessage();
        game.getMoritaniFaction().sendTerrorTokenMessage(territory.getTerritoryName());
    }

    private static void queueTerrorInTerritoriesButtons(ButtonInteractionEvent event, Game game, DiscordGame discordGame) {
        String token = event.getComponentId().split("-")[3];
        Territory territory = game.getTerritory(event.getComponentId().split("-")[4]);
        discordGame.queueDeleteMessage();
        game.getMoritaniFaction().presentTerrorTokenMoveDestinations(token, territory);
    }

    private static void denyAlliance(ButtonInteractionEvent event, Game game, DiscordGame discordGame) throws ChannelNotFoundException, InvalidGameStateException {
        discordGame.queueDeleteMessage();
        String territoryName = event.getComponentId().split("-")[3];
        String terror = event.getComponentId().split("-")[4];
        Faction faction = ButtonManager.getButtonPresser(event, game);
        faction.denyTerrorAlliance(territoryName, terror);
        discordGame.pushGame();
    }

    private static void acceptAlliance(ButtonInteractionEvent event, Game game, DiscordGame discordGame) throws ChannelNotFoundException, InvalidGameStateException {
        discordGame.queueDeleteMessage();
        String territoryName = event.getComponentId().split("-")[3];
        String terror = event.getComponentId().split("-")[4];
        Faction faction = ButtonManager.getButtonPresser(event, game);
        faction.acceptTerrorAlliance(game.getMoritaniFaction(), territoryName, terror);
        Alliance.createAlliance(discordGame, game.getMoritaniFaction(), faction);
        discordGame.pushGame();
    }

    private static void offerAlliance(ButtonInteractionEvent event, DiscordGame discordGame, Game game) throws InvalidGameStateException {
        discordGame.queueDeleteMessage();
        String factionName = event.getComponentId().split("-")[3];
        String territory = event.getComponentId().split("-")[4];
        String terror = event.getComponentId().split("-")[5];
        game.getMoritaniFaction().presentTerrorAllianceChoices(factionName, territory, terror);
    }

    private static void giveCard(ButtonInteractionEvent event, Game game, DiscordGame discordGame) throws ChannelNotFoundException {
        String factionName = event.getComponentId().split("-")[4];
        String cardName = event.getComponentId().split("-")[5];
        String emoji = game.getFaction(factionName).getEmoji();
        discordGame.queueDeleteMessage();
        discordGame.queueMessage("You have given " + cardName + " to " + emoji);
        discordGame.getTurnSummary().queueMessage(Emojis.MORITANI + " has given a " + Emojis.TREACHERY + " card to " + emoji + " with Sabotage.");
        game.transferCard("Moritani", factionName, cardName);
        discordGame.pushGame();
    }

    private static void dontGiveCard(ButtonInteractionEvent event, Game game, DiscordGame discordGame) throws ChannelNotFoundException {
        String factionName = event.getComponentId().split("-")[4];
        String emoji = game.getFaction(factionName).getEmoji();
        discordGame.queueDeleteMessage();
        discordGame.queueMessage("You did not give a card to " + emoji);
        discordGame.getTurnSummary().queueMessage(Emojis.MORITANI + " did not give a " + Emojis.TREACHERY + " card to " + emoji + " with Sabotage.");
    }

    private static void sneakAttack(ButtonInteractionEvent event, Game game, DiscordGame discordGame) throws ChannelNotFoundException, InvalidGameStateException {
        Faction faction = ButtonManager.getButtonPresser(event, game);
        String[] components = event.getComponentId().split("-");
        int amount = Integer.parseInt(components[3]);
        String strongholdName = components[4];
        Territory territory = game.getTerritory(strongholdName);
        discordGame.queueDeleteMessage();
        discordGame.queueMessage("You placed " + amount + " " + Emojis.MORITANI_TROOP + " in " + strongholdName + " with Sneak Attack.");
        faction.placeForces(territory, amount, 0, false, true, true, game, false, false);
        game.getTurnSummary().publish(amount + " " + Emojis.MORITANI_TROOP + " placed in " + strongholdName + " with Sneak Attack.");
        discordGame.pushGame();
    }
}