package controller.buttons;

import constants.Emojis;
import controller.commands.CommandManager;
import exceptions.ChannelNotFoundException;
import exceptions.InvalidOptionException;
import model.DiscordGame;
import model.Game;
import model.Territory;
import model.TreacheryCard;
import model.factions.Faction;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;

import java.io.IOException;
import java.util.ArrayList;

public class SpiceCollectionButtons  implements Pressable{

        public static void press(ButtonInteractionEvent event, Game game, DiscordGame discordGame) throws ChannelNotFoundException, IOException, InvalidOptionException {

            if (event.getComponentId().startsWith("reveal-discovery-token-")) revealDiscoveryToken(event, game, discordGame);
            else if (event.getComponentId().equals("don't-reveal-discovery-token")) dontRevealDiscoveryToken(event, game, discordGame);


        }

    private static void dontRevealDiscoveryToken(ButtonInteractionEvent event, Game game, DiscordGame discordGame) {
            discordGame.queueMessage("You won't reveal the discovery token.");
            discordGame.queueDeleteMessage();
    }

    private static void revealDiscoveryToken(ButtonInteractionEvent event, Game game, DiscordGame discordGame) throws ChannelNotFoundException {
            Faction faction = ButtonManager.getButtonPresser(event, game);
            Territory territory = game.getTerritory(event.getComponentId().split("-")[3]);
            String token = territory.getDiscoveryToken();

            switch (token) {
                case "Jacurutu Sietch" -> {
                    addDiscoveryTokenTerritory(game, territory, new Territory("Jacurutu Sietch", territory.getSector(), true, true, false));
                    discordGame.getTurnSummary().queueMessage("Jacurutu Sietch has been discovered in " + territory.getTerritoryName() + "!");
                    territory.setDiscovered(true);
                }
                case "Cistern" -> {
                    addDiscoveryTokenTerritory(game, territory, new Territory("Cistern", territory.getSector(), true, false, false));
                    discordGame.getTurnSummary().queueMessage("A Cistern has been discovered in " + territory.getTerritoryName() + "!");
                    territory.setDiscovered(true);
                }
                case "Ecological Testing Station" -> {
                    addDiscoveryTokenTerritory(game, territory, new Territory("Ecological Testing Station", territory.getSector(), true, false, false));
                    discordGame.getTurnSummary().queueMessage("An Ecological Testing Station has been discovered in " + territory.getTerritoryName() + "!");
                    territory.setDiscovered(true);
                }
                case "Shrine" -> {
                    addDiscoveryTokenTerritory(game, territory, new Territory("Shrine", territory.getSector(), true, false, false));
                    discordGame.getTurnSummary().queueMessage("A Shrine has been discovered in " + territory.getTerritoryName() + "!");
                    territory.setDiscovered(true);
                }
                case "Orgiz Processing Station" -> {
                    addDiscoveryTokenTerritory(game, territory, new Territory("Orgiz Processing Station", territory.getSector(), true, true, false));
                    discordGame.getTurnSummary().queueMessage("The Orgiz Processing Station has been discovered in " + territory.getTerritoryName() + "!");
                    territory.setDiscovered(true);
                }
                case "Treachery Card Stash" -> {
                    TreacheryCard card = game.getTreacheryDeck().pollFirst();
                    faction.addTreacheryCard(card);
                    if (faction.getTreacheryHand().size() > faction.getHandLimit()) {
                        discordGame.getFactionChat(faction.getName()).queueMessage("Your hand is over the limit. Please select a card to discard.");
                    }
                    discordGame.getFactionLedger(faction).queueMessage(card.name() + " found in " + territory.getTerritoryName());
                    discordGame.getTurnSummary().queueMessage(faction.getEmoji() + " has discovered a " + Emojis.TREACHERY + " stash in " + territory.getTerritoryName() + "!");
                    territory.setDiscoveryToken(null);
                    territory.setDiscovered(false);

                }
                case "Spice Stash" -> {
                    faction.addSpice(7);
                    CommandManager.spiceMessage(discordGame, 7, faction.getSpice() + 7, faction.getName(), Emojis.SPICE + " stash discovery token", true);
                    discordGame.getTurnSummary().queueMessage(faction.getEmoji() + " has discovered a " + Emojis.SPICE + " stash in " + territory.getTerritoryName() + "!");
                    territory.setDiscoveryToken(null);
                    territory.setDiscovered(false);
                }
                case "Ornithopter" -> {
                    faction.setOrnithoperToken(true);
                    discordGame.getFactionLedger(faction).queueMessage("Ornithopter found in " + territory.getTerritoryName());
                    discordGame.getTurnSummary().queueMessage(faction.getEmoji() + " has discovered an Ornithopter in " + territory.getTerritoryName() + "!");
                    territory.setDiscoveryToken(null);
                    territory.setDiscovered(false);
                }
            }
            discordGame.pushGame();
            discordGame.queueDeleteMessage();
    }

    private static void addDiscoveryTokenTerritory(Game game, Territory territory, Territory token) {
            game.getTerritories().put(token.getTerritoryName(), token);
            game.getAdjacencyList().put(token.getTerritoryName(), new ArrayList<>());
            game.getAdjacencyList().get(territory.getTerritoryName().replaceAll("\\(.*\\)", "").strip()).add(token.getTerritoryName());
            game.getAdjacencyList().get(token.getTerritoryName()).add(territory.getTerritoryName());
    }
}
