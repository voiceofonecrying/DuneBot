package controller.buttons;

import constants.Emojis;
import controller.commands.CommandManager;
import controller.commands.ShowCommands;
import exceptions.ChannelNotFoundException;
import model.DiscordGame;
import model.Game;
import model.Leader;
import model.Territory;
import model.factions.EcazFaction;
import model.factions.Faction;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;

import java.io.IOException;

public class EcazButtons implements Pressable {


    public static void press(ButtonInteractionEvent event, Game game, DiscordGame discordGame) throws ChannelNotFoundException, IOException {

        if (event.getComponentId().startsWith("ecaz-offer-alliance-")) offerAlliance(event, game, discordGame);
        else if (event.getComponentId().startsWith("ecaz-bg-trigger-")) bgAmbassadorTrigger(event, game, discordGame);
        else if (event.getComponentId().startsWith("ecaz-place-ambassador-")) queueAmbassadorButtons(event, game, discordGame);
        else if (event.getComponentId().startsWith("ecaz-ambassador-selected-")) sendAmbassador(event, game, discordGame);
        else if (event.getComponentId().startsWith("ecaz-trigger-ambassador-")) triggerAmbassador(event, game, discordGame);
        switch (event.getComponentId()) {
            case "ecaz-get-vidal" -> getDukeVidal(event, game, discordGame);
            case "ecaz-accept-offer" -> acceptAlliance(event, game, discordGame);
            case "ecaz-deny-offer" -> denyAlliance(event, game, discordGame);
            case "ecaz-don't-trigger-ambassador" -> dontTrigger(event, game, discordGame);
        }

    }

    private static void dontTrigger(ButtonInteractionEvent event, Game game, DiscordGame discordGame) {
        if (!ButtonManager.getButtonPresser(event, game).getName().equals("Ecaz")) return;
        discordGame.queueMessage(Emojis.ECAZ + " Do not trigger their ambassador token.");
        discordGame.queueDeleteMessage();
    }

    private static void triggerAmbassador(ButtonInteractionEvent event, Game game, DiscordGame discordGame) throws ChannelNotFoundException {
        if (!ButtonManager.getButtonPresser(event, game).getName().equals("Ecaz")) return;
        String ambassador = event.getComponentId().split("-")[3];
        Faction triggeringFaction = game.getFaction(event.getComponentId().split("-")[4]);
        EcazFaction ecaz = (EcazFaction) game.getFaction("Ecaz");
        ecaz.triggerAmbassador(game, discordGame, triggeringFaction, ambassador);
        discordGame.pushGame();
    }

    private static void sendAmbassador(ButtonInteractionEvent event, Game game, DiscordGame discordGame) throws ChannelNotFoundException, IOException {
        EcazFaction ecazFaction = (EcazFaction) game.getFaction("Ecaz");
        String ambassador = event.getComponentId().split("-")[3];
        Territory territory = game.getTerritory(event.getComponentId().split("-")[4]);
        int cost = Integer.parseInt(event.getComponentId().split("-")[5]);
        if (cost > ecazFaction.getSpice()) {
            discordGame.queueMessage("You can't afford to send your ambassador.");
            return;
        }
        ecazFaction.subtractSpice(cost);
        CommandManager.spiceMessage(discordGame, cost, ecazFaction.getName(), " ambassador to " + territory.getTerritoryName(), false);
        ecazFaction.placeAmbassador(territory, ambassador);
        discordGame.queueMessage("turn-summary", "An " + Emojis.ECAZ + " Ambassador has been sent to " + territory.getTerritoryName());
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

    private static void denyAlliance(ButtonInteractionEvent event, Game game, DiscordGame discordGame) throws ChannelNotFoundException {
        discordGame.queueMessage("You have sent the Ambassador away empty-handed.");
        discordGame.queueMessage("ecaz-chat", "Your ambassador has returned with news that no alliance will take place.");
        discordGame.queueDeleteMessage();
    }

    private static void acceptAlliance(ButtonInteractionEvent event, Game game, DiscordGame discordGame) throws ChannelNotFoundException, IOException {
        Faction faction = ButtonManager.getButtonPresser(event, game);
        discordGame.queueMessage("You have sent the Ambassador away with news of their new alliance!");
        discordGame.queueDeleteMessage();

        faction.setAlly("Ecaz");
        game.getFaction("Ecaz").setAlly(faction.getName());
        discordGame.queueMessage("turn-summary", Emojis.ECAZ + " and " + faction.getEmoji() + " have formed an alliance!");
        discordGame.pushGame();
        ShowCommands.showBoard(discordGame, game);
    }



    private static void offerAlliance(ButtonInteractionEvent event, Game game, DiscordGame discordGame) throws ChannelNotFoundException {
        discordGame.queueMessage(event.getComponentId().replace("ecaz-offer-alliance-", "").toLowerCase() + "-chat",
                new MessageCreateBuilder().setContent("An ambassador of Ecaz has approached you to offer a formal alliance.  Do you accept?")
                        .addActionRow(Button.primary("ecaz-accept-offer", "Yes"), Button.danger("ecaz-deny-offer", "No"))
                );
        discordGame.queueMessage("Your ambassador has been sent to negotiate an alliance.");
        discordGame.queueDeleteMessage();
    }

    private static void getDukeVidal(ButtonInteractionEvent event, Game game, DiscordGame discordGame) throws ChannelNotFoundException, IOException {
        Faction ecaz = game.getFaction("Ecaz");
        if (!ecaz.getLeader("Duke Vidal").isEmpty()) return;
        ecaz.addLeader(new Leader("Duke Vidal", 6, null, false));
        discordGame.pushGame();
        discordGame.queueMessage("Duke Vidal has been returned to you!");
        discordGame.queueDeleteMessage();
    }

}
