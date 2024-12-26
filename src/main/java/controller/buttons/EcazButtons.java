package controller.buttons;

import constants.Emojis;
import controller.Alliance;
import controller.commands.RunCommands;
import exceptions.ChannelNotFoundException;
import controller.DiscordGame;
import exceptions.InvalidGameStateException;
import model.DuneChoice;
import model.Game;
import model.Territory;
import model.factions.EcazFaction;
import model.factions.Faction;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class EcazButtons implements Pressable {


    public static void press(ButtonInteractionEvent event, Game game, DiscordGame discordGame) throws ChannelNotFoundException, IOException, InvalidGameStateException {
        // Buttons handled by this class must begin with "ecaz"
        // And any button that begins with "ecaz" must be handled by this class
        if (event.getComponentId().startsWith("ecaz-offer-alliance-")) offerAlliance(event, discordGame, game);
        else if (event.getComponentId().startsWith("ecaz-bg-trigger-")) bgAmbassadorTrigger(event, game, discordGame);
        else if (event.getComponentId().startsWith("ecaz-choam-discard-")) choamDiscard(event, game, discordGame);
        else if (event.getComponentId().startsWith("ecaz-fremen-move-from-")) fremenMoveFrom(event, game, discordGame);
        else if (event.getComponentId().startsWith("ecaz-ix-discard-")) ixDiscard(event, game, discordGame);
        else if (event.getComponentId().startsWith("ecaz-richese-buy-")) richeseBuyCard(event, game, discordGame);
        else if (event.getComponentId().startsWith("ecaz-bt-leader-")) btLeader(event, game, discordGame);
        else if (event.getComponentId().startsWith("ecaz-bt-which-revival-")) btWhichRevival(event, game, discordGame);
        else if (event.getComponentId().startsWith("ecaz-place-ambassador-"))
            queueAmbassadorButtons(event, game, discordGame);
        else if (event.getComponentId().startsWith("ecaz-ambassador-selected-"))
            sendAmbassador(event, game, discordGame);
        else if (event.getComponentId().startsWith("ecaz-trigger-ambassador-"))
            triggerAmbassador(event, game, discordGame);
        switch (event.getComponentId()) {
            case "ecaz-get-vidal" -> getDukeVidal(game, discordGame);
            case "ecaz-accept-offer" -> acceptAlliance(event, game, discordGame);
            case "ecaz-deny-offer" -> denyAlliance(discordGame, game);
            case "ecaz-don't-trigger-ambassador" -> dontTrigger(event, game, discordGame);
            case "ecaz-no-more-ambassadors" -> noMoreAmbassadors(discordGame, game);
            case "ecaz-reset-ambassadors" -> resetAmbassadors(discordGame);
        }

    }

    private static void resetAmbassadors(DiscordGame discordGame) throws ChannelNotFoundException {
        EcazFaction ecazFaction = (EcazFaction) discordGame.getGame().getFaction("Ecaz");
        ecazFaction.sendAmbassadorLocationMessage(1);
    }

    private static void noMoreAmbassadors(DiscordGame discordGame, Game game) throws InvalidGameStateException, ChannelNotFoundException, IOException {
        discordGame.queueMessage("Finished sending ambassadors.");
        game.getRevival().ecazAmbassadorsComplete();
        RunCommands.advance(discordGame, game);
        discordGame.queueDeleteMessage();
        discordGame.pushGame();
    }

    private static void dontTrigger(ButtonInteractionEvent event, Game game, DiscordGame discordGame) throws ChannelNotFoundException {
        if (!(ButtonManager.getButtonPresser(event, game) instanceof EcazFaction)) return;
        discordGame.getTurnSummary().queueMessage(Emojis.ECAZ + " do not trigger their ambassador token.");
        discordGame.queueMessage("You will not trigger your ambassador token.");
        discordGame.queueDeleteMessage();
    }

    private static void triggerAmbassador(ButtonInteractionEvent event, Game game, DiscordGame discordGame) throws ChannelNotFoundException {
        if (!(ButtonManager.getButtonPresser(event, game) instanceof EcazFaction)) {
            discordGame.queueMessage("You are not " + Emojis.ECAZ);
            return;
        }
        String ambassador = event.getComponentId().split("-")[3];
        Faction triggeringFaction = game.getFaction(event.getComponentId().split("-")[4]);
        EcazFaction ecaz = (EcazFaction) game.getFaction("Ecaz");
        ecaz.triggerAmbassador(triggeringFaction, ambassador);
        discordGame.queueDeleteMessage();
        discordGame.queueMessage("You have triggered your " + ambassador + " ambassador!");
        discordGame.pushGame();
    }

    private static void sendAmbassador(ButtonInteractionEvent event, Game game, DiscordGame discordGame) throws ChannelNotFoundException {
        discordGame.queueDeleteMessage();
        EcazFaction ecazFaction = (EcazFaction) game.getFaction("Ecaz");
        String ambassador = event.getComponentId().split("-")[3];
        Territory territory = game.getTerritory(event.getComponentId().split("-")[4]);
        int cost = Integer.parseInt(event.getComponentId().split("-")[5]);
        if (cost > ecazFaction.getSpice()) {
            discordGame.queueMessage("You can't afford to send your ambassador.");
            return;
        }
        ecazFaction.subtractSpice(cost, " ambassador to " + territory.getTerritoryName());
        ecazFaction.placeAmbassador(territory, ambassador);
        game.getTurnSummary().publish(Emojis.ECAZ + " has sent the " + ambassador + " Ambassador to " + territory.getTerritoryName() + ".");
        discordGame.pushGame();
        discordGame.queueMessage("The " + ambassador + " ambassador has been sent to " + territory.getTerritoryName());
        ecazFaction.sendAmbassadorLocationMessage(cost + 1);
    }

    private static void queueAmbassadorButtons(ButtonInteractionEvent event, Game game, DiscordGame discordGame) {
        EcazFaction ecazFaction = (EcazFaction) game.getFaction("Ecaz");
        Territory territory = game.getTerritory(event.getComponentId().split("-")[3]);
        int cost = Integer.parseInt(event.getComponentId().split("-")[4]);
        if (cost > ecazFaction.getSpice()) {
            discordGame.queueMessage("You can't afford to send your ambassador.");
            return;
        }
        ecazFaction.sendAmbassadorMessage(territory.getTerritoryName(), cost);
        discordGame.queueMessage("You selected " + territory.getTerritoryName());
    }

    private static void bgAmbassadorTrigger(ButtonInteractionEvent event, Game game, DiscordGame discordGame) throws ChannelNotFoundException {
        EcazFaction faction = (EcazFaction) game.getFaction("Ecaz");
        discordGame.queueMessage("Your Bene Gesserit ambassador will be used for the " + event.getComponentId().split("-")[3] + " effect.");
        discordGame.queueDeleteMessage();
        faction.triggerAmbassador(game.getFaction(event.getComponentId().split("-")[4]), event.getComponentId().split("-")[3]);
        discordGame.pushGame();
    }

    private static void choamDiscard(ButtonInteractionEvent event, Game game, DiscordGame discordGame) throws ChannelNotFoundException {
        EcazFaction faction = (EcazFaction) game.getFaction("Ecaz");
        String cardName = event.getComponentId().split("-")[3];
        discordGame.queueDeleteMessage();
        if (cardName.equals("finished")) {
            discordGame.queueMessage("You are finished discarding.");
        } else {
            discordGame.queueMessage("You are discarding " + cardName + " for 3 " + Emojis.SPICE);
            faction.discard(cardName);
            faction.addSpice(3, "discard " + cardName + " with CHOAM ambassador.");
            faction.presentCHOAMAmbassadorDiscardChoices();
            discordGame.pushGame();
        }
    }

    private static void fremenMoveFrom(ButtonInteractionEvent event, Game game, DiscordGame discordGame) throws ChannelNotFoundException {
        EcazFaction faction = (EcazFaction) game.getFaction("Ecaz");
        String territoryName = event.getComponentId().split("-")[4];
        discordGame.queueDeleteMessage();
        discordGame.queueMessage("You will move from " + territoryName + ".");
        faction.getMovement().setMovingFrom(territoryName);
        ShipmentAndMovementButtons.queueShippingButtons(event, game, discordGame, true);
        discordGame.pushGame();
    }

    private static void ixDiscard(ButtonInteractionEvent event, Game game, DiscordGame discordGame) throws ChannelNotFoundException {
        EcazFaction faction = (EcazFaction) game.getFaction("Ecaz");
        String cardName = event.getComponentId().split("-")[3];
        discordGame.queueDeleteMessage();
        if (cardName.equals("finished")) {
            discordGame.queueMessage("You will not discard and draw a new card.");
        } else {
            faction.discard(cardName);
            game.drawTreacheryCard("Ecaz", true, true);
            discordGame.queueMessage("You discarded " + cardName + " and drew " + faction.getTreacheryHand().getLast().name());
            discordGame.pushGame();
        }
    }

    private static void richeseBuyCard(ButtonInteractionEvent event, Game game, DiscordGame discordGame) throws ChannelNotFoundException {
        EcazFaction faction = (EcazFaction) game.getFaction("Ecaz");
        String response = event.getComponentId().split("-")[3];
        discordGame.queueDeleteMessage();
        if (response.equals("no")) {
            discordGame.queueMessage("You will not buy a card.");
            game.getTurnSummary().publish(Emojis.ECAZ + " does not buy a card with their Richese ambassador.");
        } else {
            faction.buyCardWithRicheseAmbassador();
            discordGame.queueMessage("You bought " + faction.getTreacheryHand().getLast().name() + ".");
            discordGame.pushGame();
        }
    }

    private static void btLeader(ButtonInteractionEvent event, Game game, DiscordGame discordGame) throws ChannelNotFoundException {
        EcazFaction faction = (EcazFaction) game.getFaction("Ecaz");
        String leaderName = event.getComponentId().split("-")[3];
        discordGame.queueDeleteMessage();
        discordGame.queueMessage("You chose " + leaderName);
        faction.reviveLeaderWithBTAmbassador(leaderName);
        discordGame.pushGame();
    }

    private static void btWhichRevival(ButtonInteractionEvent event, Game game, DiscordGame discordGame) throws ChannelNotFoundException {
        EcazFaction faction = (EcazFaction) game.getFaction("Ecaz");
        String revivalType = event.getComponentId().split("-")[4];
        discordGame.queueDeleteMessage();
        if (revivalType.equals("leader"))
            faction.presentLeaderChoicesWithBTAmbassador();
        else
            faction.reviveForcesWithBTAmbassador();
        discordGame.queueMessage("You chose to revive " + (revivalType.equals("leader") ? "a Leader" : Emojis.ECAZ_TROOP));
        discordGame.pushGame();
    }

    private static void denyAlliance(DiscordGame discordGame, Game game) {
        discordGame.queueMessage("You have sent the Ambassador away empty-handed.");
        game.getFaction("Ecaz").getChat().publish("Your ambassador has returned with news that no alliance will take place.");
        discordGame.queueDeleteMessage();
    }

    private static void acceptAlliance(ButtonInteractionEvent event, Game game, DiscordGame discordGame) throws ChannelNotFoundException {
        Faction faction = ButtonManager.getButtonPresser(event, game);
        discordGame.queueMessage("You have sent the Ambassador away with news of their new alliance!");
        discordGame.queueDeleteMessage();

        Alliance.createAlliance(discordGame, game.getFaction("Ecaz"), faction);

        discordGame.pushGame();
    }


    private static void offerAlliance(ButtonInteractionEvent event, DiscordGame discordGame, Game game) {
        String factionName = event.getComponentId().replace("ecaz-offer-alliance-", "");
        Faction faction = game.getFaction(factionName);
        List<DuneChoice> choices = new ArrayList<>();
        choices.add(new DuneChoice("ecaz-accept-offer", "Yes"));
        choices.add(new DuneChoice("danger", "ecaz-deny-offer", "No"));
        game.getTurnSummary().publish(Emojis.ECAZ + " have offered alliance to " + faction.getEmoji());
        faction.getChat().publish("An ambassador of " + Emojis.ECAZ + " has approached you to offer a formal alliance.  Do you accept?", choices);
        discordGame.queueMessage("Your ambassador has been sent to negotiate an alliance.");
        discordGame.queueDeleteMessage();
    }

    private static void getDukeVidal(Game game, DiscordGame discordGame) throws ChannelNotFoundException {
        Faction ecaz = game.getFaction("Ecaz");
        if (ecaz.getLeader("Duke Vidal").isPresent()) return;
        ecaz.addLeader(game.getDukeVidal());
        discordGame.pushGame();
        discordGame.queueMessage("Duke Vidal has been returned to you!");
        discordGame.queueDeleteMessage();
    }

}
