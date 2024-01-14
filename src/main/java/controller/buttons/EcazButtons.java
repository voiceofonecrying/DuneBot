package controller.buttons;

import constants.Emojis;
import controller.Alliance;
import controller.channels.FactionChat;
import exceptions.ChannelNotFoundException;
import controller.DiscordGame;
import model.Game;
import model.Leader;
import model.Territory;
import model.factions.EcazFaction;
import model.factions.Faction;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;

import java.util.LinkedList;
import java.util.List;

public class EcazButtons implements Pressable {


    public static void press(ButtonInteractionEvent event, Game game, DiscordGame discordGame) throws ChannelNotFoundException {

        if (event.getComponentId().startsWith("ecaz-offer-alliance-")) offerAlliance(event, discordGame);
        else if (event.getComponentId().startsWith("ecaz-bg-trigger-")) bgAmbassadorTrigger(event, game, discordGame);
        else if (event.getComponentId().startsWith("ecaz-place-ambassador-"))
            queueAmbassadorButtons(event, game, discordGame);
        else if (event.getComponentId().startsWith("ecaz-ambassador-selected-"))
            sendAmbassador(event, game, discordGame);
        else if (event.getComponentId().startsWith("ecaz-trigger-ambassador-"))
            triggerAmbassador(event, game, discordGame);
        switch (event.getComponentId()) {
            case "ecaz-get-vidal" -> getDukeVidal(game, discordGame);
            case "ecaz-accept-offer" -> acceptAlliance(event, game, discordGame);
            case "ecaz-deny-offer" -> denyAlliance(discordGame);
            case "ecaz-don't-trigger-ambassador" -> dontTrigger(event, game, discordGame);
            case "ecaz-no-more-ambassadors" -> noMoreAmbassadors(event, discordGame);
            case "ecaz-reset-ambassadors" -> resetAmbassadors(discordGame);
        }

    }

    private static void resetAmbassadors(DiscordGame discordGame) throws ChannelNotFoundException {
        EcazFaction ecazFaction = (EcazFaction) discordGame.getGame().getFaction("Ecaz");
        ecazFaction.sendAmbassadorLocationMessage(discordGame.getGame(), discordGame, 1);
    }

    private static void noMoreAmbassadors(ButtonInteractionEvent event, DiscordGame discordGame) {
        discordGame.queueMessage("Finished sending ambassadors.");
        discordGame.queueDeleteMessage();
        ButtonManager.deleteAllButtonsInChannel(event.getChannel());
    }

    private static void dontTrigger(ButtonInteractionEvent event, Game game, DiscordGame discordGame) throws ChannelNotFoundException {
        if (!(ButtonManager.getButtonPresser(event, game) instanceof EcazFaction)) return;
        discordGame.getTurnSummary().queueMessage(Emojis.ECAZ + " do not trigger their ambassador token.");
        discordGame.queueMessage("You will not trigger your ambassador token.");
        discordGame.queueDeleteMessage();
    }

    private static void triggerAmbassador(ButtonInteractionEvent event, Game game, DiscordGame discordGame) throws ChannelNotFoundException {
        if (!(ButtonManager.getButtonPresser(event, game) instanceof EcazFaction)) return;
        String ambassador = event.getComponentId().split("-")[3];
        Faction triggeringFaction = game.getFaction(event.getComponentId().split("-")[4]);
        EcazFaction ecaz = (EcazFaction) game.getFaction("Ecaz");
        ecaz.triggerAmbassador(game, discordGame, triggeringFaction, ambassador);
        discordGame.pushGame();
    }

    private static void sendAmbassador(ButtonInteractionEvent event, Game game, DiscordGame discordGame) throws ChannelNotFoundException {
        EcazFaction ecazFaction = (EcazFaction) game.getFaction("Ecaz");
        String ambassador = event.getComponentId().split("-")[3];
        Territory territory = game.getTerritory(event.getComponentId().split("-")[4]);
        int cost = Integer.parseInt(event.getComponentId().split("-")[5]);
        if (cost > ecazFaction.getSpice()) {
            discordGame.queueMessage("You can't afford to send your ambassador.");
            return;
        }
        ecazFaction.subtractSpice(cost);
        ecazFaction.spiceMessage(cost, " ambassador to " + territory.getTerritoryName(), false);
        ecazFaction.placeAmbassador(game, territory, ambassador);
        discordGame.getTurnSummary().queueMessage("An " + Emojis.ECAZ + " Ambassador has been sent to " + territory.getTerritoryName());
        discordGame.pushGame();
        discordGame.queueMessage("The " + ambassador + " ambassador has been sent to " + territory.getTerritoryName());
        ecazFaction.sendAmbassadorLocationMessage(game, discordGame, cost + 1);
    }

    private static void queueAmbassadorButtons(ButtonInteractionEvent event, Game game, DiscordGame discordGame) throws ChannelNotFoundException {
        EcazFaction ecazFaction = (EcazFaction) game.getFaction("Ecaz");
        Territory territory = game.getTerritory(event.getComponentId().split("-")[3]);
        int cost = Integer.parseInt(event.getComponentId().split("-")[4]);
        if (cost > ecazFaction.getSpice()) {
            discordGame.queueMessage("You can't afford to send your ambassador.");
            return;
        }
        ecazFaction.sendAmbassadorMessage(discordGame, territory.getTerritoryName(), cost);
    }

    private static void bgAmbassadorTrigger(ButtonInteractionEvent event, Game game, DiscordGame discordGame) throws ChannelNotFoundException {
        EcazFaction faction = (EcazFaction) game.getFaction("Ecaz");
        discordGame.queueMessage("Your Bene Gesserit token will be used for the " + event.getComponentId().split("-")[3] + " effect.");
        discordGame.queueDeleteMessage();
        faction.triggerAmbassador(game, discordGame, game.getFaction(event.getComponentId().split("-")[4]), event.getComponentId().split("-")[3]);
        discordGame.pushGame();
    }

    private static void denyAlliance(DiscordGame discordGame) throws ChannelNotFoundException {
        discordGame.queueMessage("You have sent the Ambassador away empty-handed.");
        discordGame.getEcazChat().queueMessage("Your ambassador has returned with news that no alliance will take place.");
        discordGame.queueDeleteMessage();
    }

    private static void acceptAlliance(ButtonInteractionEvent event, Game game, DiscordGame discordGame) throws ChannelNotFoundException {
        Faction faction = ButtonManager.getButtonPresser(event, game);
        discordGame.queueMessage("You have sent the Ambassador away with news of their new alliance!");
        discordGame.queueDeleteMessage();

        Alliance.createAlliance(discordGame, game.getFaction("Ecaz"), faction);

        discordGame.pushGame();
    }


    private static void offerAlliance(ButtonInteractionEvent event, DiscordGame discordGame) throws ChannelNotFoundException {
        List<Button> buttons = new LinkedList<>();
        buttons.add(Button.primary("ecaz-accept-offer", "Yes"));
        buttons.add(Button.danger("ecaz-deny-offer", "No"));
        FactionChat chatChannel = discordGame.getFactionChat(event.getComponentId().replace("ecaz-offer-alliance-", ""));
        chatChannel.queueMessage("An ambassador of Ecaz has approached you to offer a formal alliance.  Do you accept?", buttons);
        discordGame.queueMessage("Your ambassador has been sent to negotiate an alliance.");
        discordGame.queueDeleteMessage();
    }

    private static void getDukeVidal(Game game, DiscordGame discordGame) throws ChannelNotFoundException {
        Faction ecaz = game.getFaction("Ecaz");
        if (ecaz.getLeader("Duke Vidal").isPresent()) return;
        ecaz.addLeader(new Leader("Duke Vidal", 6, null, false));
        discordGame.pushGame();
        discordGame.queueMessage("Duke Vidal has been returned to you!");
        discordGame.queueDeleteMessage();
    }

}
