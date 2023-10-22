package controller.buttons;

import constants.Emojis;
import controller.commands.CommandManager;
import controller.commands.ShowCommands;
import enums.GameOption;
import enums.UpdateType;
import exceptions.ChannelNotFoundException;
import model.DiscordGame;
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

    public static void press(ButtonInteractionEvent event, Game game, DiscordGame discordGame) throws ChannelNotFoundException, IOException {

        if (event.getComponentId().startsWith("moritani-offer-alliance-")) offerAlliance(event, game, discordGame);
        else if (event.getComponentId().startsWith("moritani-place-terror-"))
            queueTerrorButtons(event, game, discordGame);
        else if (event.getComponentId().startsWith("moritani-terror-selected-"))
            placeTerrorToken(event, game, discordGame);
        else if (event.getComponentId().startsWith("moritani-trigger-terror-"))
            triggerTerrorToken(event, game, discordGame);
        else if (event.getComponentId().startsWith("moritani-remove-terror-"))
            removeTerrorToken(event, game, discordGame);
        else if (event.getComponentId().startsWith("moritani-deny-offer-")) denyAlliance(event, game, discordGame);
        switch (event.getComponentId()) {
            case "moritani-accept-offer" -> acceptAlliance(event, game, discordGame);
            case "moritani-don't-trigger-terror" -> dontTrigger(event, game, discordGame);
            case "moritani-pay-extortion" -> payExtortion(event, game, discordGame);
            case "moritani-pass-extortion" -> passExtortion(event, game, discordGame);
        }

    }

    private static void removeTerrorToken(ButtonInteractionEvent event, Game game, DiscordGame discordGame) throws ChannelNotFoundException {
        String terror = event.getComponentId().split("-")[4];
        Territory territory = game.getTerritory(event.getComponentId().split("-")[3]);
        MoritaniFaction moritani = (MoritaniFaction) game.getFaction("Moritani");

        moritani.addSpice(4);
        CommandManager.spiceMessage(discordGame, 4, moritani.getSpice(), "Moritani", "terror token returned to supply", true);

        moritani.getTerrorTokens().add(terror);
        territory.getTerrorTokens().removeIf(t -> t.equals(terror));
        discordGame.getTurnSummary().queueMessage(Emojis.MORITANI + " have removed a Terror Token from " + territory.getTerritoryName() + " for 4 " + Emojis.SPICE);
        discordGame.pushGame();
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
        faction.subtractSpice(3);
        discordGame.queueMessage(faction.getEmoji() + " has paid 3 " + Emojis.SPICE + " to remove Extortion from the game.");
        discordGame.queueDeleteMessage();
        discordGame.pushGame();
    }

    private static void dontTrigger(ButtonInteractionEvent event, Game game, DiscordGame discordGame) {
        if (!ButtonManager.getButtonPresser(event, game).getName().equals("Moritani")) return;
        discordGame.queueMessage(Emojis.MORITANI + " Do not trigger their terror token.");
        discordGame.queueDeleteMessage();
    }

    private static void triggerTerrorToken(ButtonInteractionEvent event, Game game, DiscordGame discordGame) throws ChannelNotFoundException, IOException {
        if (!ButtonManager.getButtonPresser(event, game).getName().equals("Moritani")) return;
        String terror = event.getComponentId().split("-")[3];
        Faction triggeringFaction = game.getFaction(event.getComponentId().split("-")[4]);
        MoritaniFaction moritani = (MoritaniFaction) game.getFaction("Moritani");
        Territory territory = game.getTerritories().values().stream().filter(t -> t.getTerrorTokens().contains(terror)).findFirst().orElseThrow();
        moritani.triggerTerrorToken(game, discordGame, triggeringFaction, territory, terror);
        discordGame.pushGame();
        discordGame.queueDeleteMessage();
    }

    private static void placeTerrorToken(ButtonInteractionEvent event, Game game, DiscordGame discordGame) throws ChannelNotFoundException {
        MoritaniFaction moritaniFaction = (MoritaniFaction) game.getFaction("Moritani");
        String terror = event.getComponentId().split("-")[3];
        Territory territory = game.getTerritory(event.getComponentId().split("-")[4]);
        moritaniFaction.placeTerrorToken(game, territory, terror);
        discordGame.getTurnSummary().queueMessage("A " + Emojis.MORITANI + " terror token has been placed in " + territory.getTerritoryName());
        discordGame.pushGame();
        discordGame.queueMessage("The " + terror + " token has been sent to " + territory.getTerritoryName());
        discordGame.queueDeleteMessage();
    }

    private static void queueTerrorButtons(ButtonInteractionEvent event, Game game, DiscordGame discordGame) throws ChannelNotFoundException {
        MoritaniFaction moritaniFaction = (MoritaniFaction) game.getFaction("Moritani");
        Territory territory = game.getTerritory(event.getComponentId().split("-")[3]);
        moritaniFaction.sendTerrorTokenMessage(discordGame, territory.getTerritoryName());
        discordGame.queueDeleteMessage();
    }

    private static void denyAlliance(ButtonInteractionEvent event, Game game, DiscordGame discordGame) throws ChannelNotFoundException, IOException {
        discordGame.queueMessage("You have sent the emissary away empty-handed. Time to prepare for the worst.");
        discordGame.getMoritaniChat().queueMessage("Your ambassador has returned with news that no alliance will take place.");
        discordGame.queueDeleteMessage();
        Territory territory = game.getTerritory(event.getComponentId().split("-")[3]);
        String terror = event.getComponentId().split("-")[4];
        ((MoritaniFaction) game.getFaction("Moritani")).triggerTerrorToken(game, discordGame, ButtonManager.getButtonPresser(event, game), territory, terror);
    }

    private static void acceptAlliance(ButtonInteractionEvent event, Game game, DiscordGame discordGame) throws ChannelNotFoundException, IOException {
        Faction faction = ButtonManager.getButtonPresser(event, game);
        discordGame.queueMessage("You have sent the emissary away with news of their new alliance!");
        discordGame.queueDeleteMessage();
        String terror = event.getComponentId().split("-")[4];

        String oldAlly = faction.getAlly();
        String moritaniOldAlly = game.getFaction("Moritani").getAlly();

        faction.setAlly("Moritani");
        game.getFaction("Moritani").setAlly(faction.getName());
        if (oldAlly != null) game.getFaction(oldAlly).setAlly(null);
        if (moritaniOldAlly != null) game.getFaction(moritaniOldAlly).setAlly(null);
        discordGame.getTurnSummary().queueMessage(Emojis.MORITANI + " and " + faction.getEmoji() + " have forsaken former political ties and formed an alliance!");
        MoritaniFaction moritani = (MoritaniFaction) game.getFaction("Moritani");
        moritani.getTerrorTokens().add(terror);
        game.getTerritory(event.getComponentId().split("-")[3]).getTerrorTokens().removeIf(t -> t.equals(terror));
        discordGame.pushGame();
        if (game.hasGameOption(GameOption.MAP_IN_FRONT_OF_SHIELD))
            game.setUpdated(UpdateType.MAP);
        else
            ShowCommands.showBoard(discordGame, game);
    }

    private static void offerAlliance(ButtonInteractionEvent event, Game game, DiscordGame discordGame) throws ChannelNotFoundException {
        String factionName = event.getComponentId().split("-")[3];
        String territory = event.getComponentId().split("-")[4];
        String terror = event.getComponentId().split("-")[5];
        List<Button> buttons = new LinkedList<>();
        buttons.add(Button.primary("moritani-accept-offer-" + territory + "-" + terror, "Yes"));
        buttons.add(Button.danger("moritani-deny-offer-" + territory + "-" + terror, "No"));
        discordGame.getFactionChat(factionName).queueMessage("An emissary of " + Emojis.MORITANI + " has offered an alliance with you!  Or else.  Do you accept?", buttons);
        discordGame.queueMessage(Emojis.MORITANI + " are offering an alliance in exchange for safety from their terror token!");
        discordGame.queueDeleteMessage();
    }
}