package controller.buttons;

import constants.Emojis;
import controller.Alliance;
import controller.commands.CommandManager;
import enums.UpdateType;
import exceptions.ChannelNotFoundException;
import controller.DiscordGame;
import exceptions.InvalidGameStateException;
import model.Game;
import model.Territory;
import model.factions.Faction;
import model.factions.MoritaniFaction;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

public class MoritaniButtons implements Pressable {

    public static void press(ButtonInteractionEvent event, Game game, DiscordGame discordGame) throws ChannelNotFoundException, IOException, InvalidGameStateException {

        if (event.getComponentId().startsWith("moritani-offer-alliance-")) offerAlliance(event, discordGame);
        else if (event.getComponentId().startsWith("moritani-place-terror-"))
            queueTerrorButtons(event, game, discordGame);
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
            case "moritani-pay-extortion" -> payExtortion(event, game, discordGame);
            case "moritani-pass-extortion" -> passExtortion(event, game, discordGame);
            case "moritani-robbery-draw" -> robberyDraw(event, game, discordGame);
            case "moritani-don't-place-terror" -> dontPlaceTerrorToken(discordGame);
        }
    }

    private static void removeTerrorToken(ButtonInteractionEvent event, Game game, DiscordGame discordGame) throws ChannelNotFoundException {
        String terror = event.getComponentId().split("-")[4];
        Territory territory = game.getTerritory(event.getComponentId().split("-")[3]);
        MoritaniFaction moritani = (MoritaniFaction) game.getFaction("Moritani");

        moritani.addSpice(4, "terror token returned to supply");

        moritani.getTerrorTokens().add(terror);
        territory.getTerrorTokens().removeIf(t -> t.equals(terror));
        game.setUpdated(UpdateType.MAP);
        discordGame.getTurnSummary().queueMessage(Emojis.MORITANI + " have removed a Terror Token from " + territory.getTerritoryName() + " for 4 " + Emojis.SPICE);
        discordGame.queueMessage("You removed " + terror + " from " + territory.getTerritoryName() + ".");
        discordGame.pushGame();
    }

    private static void dontPlaceTerrorToken(DiscordGame discordGame) {
        discordGame.queueDeleteMessage();
        discordGame.queueMessage("You will leave your Terror Tokens as they are.");
    }

    private static void passExtortion(ButtonInteractionEvent event, Game game, DiscordGame discordGame) throws ChannelNotFoundException {
        Faction faction = ButtonManager.getButtonPresser(event, game);
        String emojiName = faction.getEmoji().replace("<:", "").replaceAll(":.*>", "");
        long id = Long.parseLong(faction.getEmoji().replaceAll("<:.*:", "").replace(">", ""));
        event.getMessage().addReaction(Emoji.fromCustom(emojiName, id, false)).queue();
        discordGame.queueMessageToEphemeral("You choose not to pay Extortion.");
        if (event.getMessageChannel().retrieveMessageById(event.getMessageIdLong()).complete().getReactions().size() >= 5) {
            ((MoritaniFaction) game.getFaction("Moritani")).getTerrorTokens().add("Extortion");
            discordGame.queueMessage("Nobody wanted to pay the " + Emojis.SPICE + ", Extortion has been returned to " + Emojis.MORITANI);
            discordGame.queueDeleteMessage();
        }
        discordGame.pushGame();
    }

    private static void payExtortion(ButtonInteractionEvent event, Game game, DiscordGame discordGame) throws ChannelNotFoundException {
        Faction faction = ButtonManager.getButtonPresser(event, game);
        faction.subtractSpice(3, "remove Extortion from the game.");
        discordGame.queueMessage(faction.getEmoji() + " has paid 3 " + Emojis.SPICE + " to remove Extortion from the game.");
        discordGame.queueDeleteMessage();
        discordGame.pushGame();
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
        discordGame.queueMessage(Emojis.MORITANI + " Do not trigger their terror token.");
        discordGame.queueDeleteMessage();
    }

    private static void triggerTerrorToken(ButtonInteractionEvent event, Game game, DiscordGame discordGame) throws ChannelNotFoundException {
        if (!(ButtonManager.getButtonPresser(event, game) instanceof MoritaniFaction)) return;
        String terror = event.getComponentId().split("-")[3];
        Faction triggeringFaction = game.getFaction(event.getComponentId().split("-")[4]);
        MoritaniFaction moritani = (MoritaniFaction) game.getFaction("Moritani");
        Territory territory = game.getTerritories().values().stream().filter(t -> t.getTerrorTokens().contains(terror)).findFirst().orElseThrow();
        moritani.triggerTerrorToken(triggeringFaction, territory, terror);
        discordGame.queueMessage("You have triggered your " + terror + " token in " + territory.getTerritoryName());
        discordGame.pushGame();
        discordGame.queueDeleteMessage();
    }

    private static void placeTerrorToken(ButtonInteractionEvent event, Game game, DiscordGame discordGame) throws ChannelNotFoundException {
        MoritaniFaction moritaniFaction = (MoritaniFaction) game.getFaction("Moritani");
        String terror = event.getComponentId().split("-")[3];
        Territory territory = game.getTerritory(event.getComponentId().split("-")[4]);
        moritaniFaction.placeTerrorToken(territory, terror);
        discordGame.getTurnSummary().queueMessage("A " + Emojis.MORITANI + " terror token has been placed in " + territory.getTerritoryName());
        discordGame.pushGame();
        discordGame.queueMessage("The " + terror + " token has been sent to " + territory.getTerritoryName());
        discordGame.queueDeleteMessage();
    }

    private static void queueTerrorButtons(ButtonInteractionEvent event, Game game, DiscordGame discordGame) {
        MoritaniFaction moritaniFaction = (MoritaniFaction) game.getFaction("Moritani");
        Territory territory = game.getTerritory(event.getComponentId().split("-")[3]);
        moritaniFaction.sendTerrorTokenMessage(territory.getTerritoryName());
        discordGame.queueMessage("You selected " + territory.getTerritoryName());
        discordGame.queueDeleteMessage();
    }

    private static void denyAlliance(ButtonInteractionEvent event, Game game, DiscordGame discordGame) throws ChannelNotFoundException {
        discordGame.queueMessage("You have sent the emissary away empty-handed. Time to prepare for the worst.");
        discordGame.getMoritaniChat().queueMessage("Your ambassador has returned with news that no alliance will take place.");
        discordGame.queueDeleteMessage();
        Territory territory = game.getTerritory(event.getComponentId().split("-")[3]);
        String terror = event.getComponentId().split("-")[4];
        ((MoritaniFaction) game.getFaction("Moritani")).triggerTerrorToken(ButtonManager.getButtonPresser(event, game), territory, terror);
        discordGame.pushGame();
    }

    private static void acceptAlliance(ButtonInteractionEvent event, Game game, DiscordGame discordGame) throws ChannelNotFoundException {
        Faction faction = ButtonManager.getButtonPresser(event, game);
        discordGame.queueMessage("You have sent the emissary away with news of their new alliance!");
        discordGame.queueDeleteMessage();
        String terror = event.getComponentId().split("-")[4];
        MoritaniFaction moritani = (MoritaniFaction) game.getFaction("Moritani");

        Alliance.createAlliance(discordGame, moritani, faction);

        moritani.getTerrorTokens().add(terror);
        game.getTerritory(event.getComponentId().split("-")[3]).getTerrorTokens().removeIf(t -> t.equals(terror));
        discordGame.pushGame();
    }

    private static void offerAlliance(ButtonInteractionEvent event, DiscordGame discordGame) throws ChannelNotFoundException {
        String factionName = event.getComponentId().split("-")[3];
        String territory = event.getComponentId().split("-")[4];
        String terror = event.getComponentId().split("-")[5];
        List<Button> buttons = new LinkedList<>();
        buttons.add(Button.primary("moritani-accept-offer-" + territory + "-" + terror, "Yes"));
        buttons.add(Button.danger("moritani-deny-offer-" + territory + "-" + terror, "No"));
        String emoji  = discordGame.getGame().getFaction(factionName).getEmoji();
        discordGame.getTurnSummary().queueMessage(Emojis.MORITANI + " are offering an alliance to " + emoji + " in exchange for safety from their terror token!");
        discordGame.getFactionChat(factionName).queueMessage("An emissary of " + Emojis.MORITANI + " has offered an alliance with you!  Or else.  Do you accept?", buttons);
        discordGame.queueMessage("You have offered an alliance to " + emoji + " in exchange for safety from your terror token!");
        discordGame.queueDeleteMessage();
    }

    private static void giveCard(ButtonInteractionEvent event, Game game, DiscordGame discordGame) throws ChannelNotFoundException {
        String factionName = event.getComponentId().split("-")[4];
        String cardName = event.getComponentId().split("-")[5];
        String emoji = game.getFaction(factionName).getEmoji();
        game.transferCard("Moritani", factionName, cardName);
        discordGame.queueMessage("You have given " + cardName + " to " + emoji);
        discordGame.getTurnSummary().queueMessage(Emojis.MORITANI + " have given a " + Emojis.TREACHERY + " card to " + emoji + " with Sabotage.");
        discordGame.pushGame();
    }

    private static void dontGiveCard(ButtonInteractionEvent event, Game game, DiscordGame discordGame) throws ChannelNotFoundException {
        String factionName = event.getComponentId().split("-")[4];
        String emoji = game.getFaction(factionName).getEmoji();
        discordGame.queueMessage("You did not give a card to " + emoji);
        discordGame.getTurnSummary().queueMessage(Emojis.MORITANI + " did not give a " + Emojis.TREACHERY + " card to " + emoji + " with Sabotage.");
    }

    private static void sneakAttack(ButtonInteractionEvent event, Game game, DiscordGame discordGame) throws ChannelNotFoundException, InvalidGameStateException {
        Faction faction = ButtonManager.getButtonPresser(event, game);
        String[] components = event.getComponentId().split("-");
        int amount = Integer.parseInt(components[3]);
        String strongholdName = components[4];
        System.out.println("Placing " + amount + " in " + strongholdName);
        Territory territory = game.getTerritory(strongholdName);
        CommandManager.placeForces(territory, faction, amount, 0, false, true, discordGame, game, false);
        game.getTurnSummary().publish(amount + " " + Emojis.MORITANI_TROOP + " placed in " + strongholdName + " with Sneak Attack.");
        discordGame.queueDeleteMessage();
        discordGame.queueMessage("You placed " + amount + " " + Emojis.MORITANI_TROOP + " in " + strongholdName + " with Sneak Attack.");
        discordGame.pushGame();
    }
}