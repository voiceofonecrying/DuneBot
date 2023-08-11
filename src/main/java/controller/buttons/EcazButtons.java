package controller.buttons;

import constants.Emojis;
import exceptions.ChannelNotFoundException;
import model.DiscordGame;
import model.Game;
import model.Leader;
import model.Territory;
import model.factions.EcazFaction;
import model.factions.Faction;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;

public class EcazButtons implements Pressable {


    public static void press(ButtonInteractionEvent event, Game game, DiscordGame discordGame) throws ChannelNotFoundException {

        if (event.getComponentId().startsWith("ecaz-offer-alliance-")) offerAlliance(event, game, discordGame);
        else if (event.getComponentId().startsWith("ecaz-bg-trigger-")) bgAmbassadorTrigger(event, game, discordGame);
        else if (event.getComponentId().startsWith("ecaz-place-ambassador-")) queueAmbassadorButtons(event, game, discordGame);
        else if (event.getComponentId().startsWith("ecaz-ambassador-selected-")) sendAmbassador(event, game, discordGame);
        switch (event.getComponentId()) {
            case "ecaz-get-vidal" -> getDukeVidal(event, game, discordGame);
            case "ecaz-accept-offer" -> acceptAlliance(event, game, discordGame);
            case "ecaz-deny-offer" -> denyAlliance(event, game, discordGame);
        }

    }

    private static void sendAmbassador(ButtonInteractionEvent event, Game game, DiscordGame discordGame) throws ChannelNotFoundException {
        EcazFaction ecazFaction = (EcazFaction) game.getFaction("Ecaz");
        String ambassador = event.getComponentId().split("-")[3];
        Territory territory = game.getTerritory(event.getComponentId().split("-")[4]);
        int cost = Integer.parseInt(event.getComponentId().split("-")[5]);
        if (cost > ecazFaction.getSpice()) {
            event.getHook().sendMessage("You can't afford to send your ambassador.").queue();
            return;
        }
        ecazFaction.placeAmbassador(territory, ambassador);
        discordGame.sendMessage("turn-summary", "An " + Emojis.ECAZ + " Ambassador has been sent to " + territory.getTerritoryName());
        discordGame.pushGame();
        ecazFaction.sendAmbassadorLocationMessage(game, discordGame, cost + 1);
    }

    private static void queueAmbassadorButtons(ButtonInteractionEvent event, Game game, DiscordGame discordGame) throws ChannelNotFoundException {
        EcazFaction ecazFaction = (EcazFaction) game.getFaction("Ecaz");
        Territory territory = game.getTerritory(event.getComponentId().split("-")[3]);
        int cost = Integer.parseInt(event.getComponentId().split("-")[4]);
        if (cost > ecazFaction.getSpice()) {
            event.getHook().sendMessage("You can't afford to send your ambassador.").queue();
            return;
        }
        ecazFaction.sendAmbassadorMessage(game, discordGame, territory.getTerritoryName(), cost);
    }

    private static void bgAmbassadorTrigger(ButtonInteractionEvent event, Game game, DiscordGame discordGame) throws ChannelNotFoundException {
        EcazFaction faction = (EcazFaction) game.getFaction("Ecaz");
        faction.triggerAmbassador(game, discordGame, game.getFaction(event.getComponentId().split("-")[4]), event.getComponentId().split("-")[3]);
        discordGame.pushGame();
    }

    private static void denyAlliance(ButtonInteractionEvent event, Game game, DiscordGame discordGame) {
        event.getHook().sendMessage("You have sent the Ambassador away empty-handed.").queue();
    }

    private static void acceptAlliance(ButtonInteractionEvent event, Game game, DiscordGame discordGame) throws ChannelNotFoundException {
        Faction faction = ButtonManager.getButtonPresser(event, game);
        event.getHook().sendMessage("You have sent the Ambassador away with news of their new alliance!").queue();

        String oldAlly = faction.getAlly();
        String oldEcazAlly = game.getFaction("Ecaz").getAlly();
        faction.setAlly("Ecaz");
        game.getFaction("Ecaz").setAlly(faction.getName());
        if (oldAlly != null) game.getFaction(oldAlly).setAlly(null);
        if (oldEcazAlly != null) game.getFaction(oldEcazAlly).setAlly(null);
        discordGame.sendMessage("turn-summary", Emojis.ECAZ + " and " + faction.getEmoji() + " have forsaken any prior allegiances and are now allies!");
        discordGame.pushGame();
    }



    private static void offerAlliance(ButtonInteractionEvent event, Game game, DiscordGame discordGame) throws ChannelNotFoundException {
        discordGame.prepareMessage(event.getComponentId().replace("ecaz-offer-alliance-", "").toLowerCase() + "-chat", "An ambassador of Ecaz has approached you to offer a formal alliance.  Do you accept?")
                .addActionRow(Button.primary("ecaz-accept-offer", "Yes"), Button.danger("ecaz-deny-offer", "No")).queue();
        event.getHook().sendMessage("Your ambassador has been sent to negotiate an alliance.").queue();
    }

    private static void getDukeVidal(ButtonInteractionEvent event, Game game, DiscordGame discordGame) throws ChannelNotFoundException {
        Faction ecaz = game.getFaction("Ecaz");
        if (!ecaz.getLeader("Duke Vidal").isEmpty()) return;
        ecaz.addLeader(new Leader("Duke Vidal", 6, null, false));
        discordGame.pushGame();
        event.getHook().sendMessage("Duke Vidal has been returned to you!").queue();
    }

}
