package controller.buttons;

import constants.Emojis;
import enums.UpdateType;
import exceptions.ChannelNotFoundException;
import controller.DiscordGame;
import model.Game;
import model.Territories;
import model.Territory;
import model.TreacheryCard;
import model.factions.Faction;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;

import java.util.Objects;

public class SpiceCollectionButtons  implements Pressable{

        public static void press(ButtonInteractionEvent event, Game game, DiscordGame discordGame) throws ChannelNotFoundException {

            if (event.getComponentId().startsWith("reveal-discovery-token-")) revealDiscoveryToken(event, game, discordGame);
            else if (event.getComponentId().equals("don't-reveal-discovery-token")) dontRevealDiscoveryToken(discordGame);


        }

    private static void dontRevealDiscoveryToken(DiscordGame discordGame) {
            discordGame.queueMessage("You won't reveal the discovery token.");
            discordGame.queueDeleteMessage();
    }

    private static void revealDiscoveryToken(ButtonInteractionEvent event, Game game, DiscordGame discordGame) throws ChannelNotFoundException {
            Faction faction = ButtonManager.getButtonPresser(event, game);
            Territory territory = game.getTerritory(event.getComponentId().split("-")[3]);
            String token = territory.getDiscoveryToken();

            Territories territories = game.getTerritories();
            switch (token) {
                case "Jacurutu Sietch" -> {
                    Territory jacurutuSietch = territories.addDiscoveryToken("Jacurutu Sietch", true);
                    game.putTerritoryInAnotherTerritory(jacurutuSietch, territory);
                    discordGame.getTurnSummary().queueMessage("Jacurutu Sietch has been discovered in " + territory.getTerritoryName() + "!");
                    territory.setDiscovered(true);
                }
                case "Cistern" -> {
                    Territory cistern = territories.addDiscoveryToken("Cistern", false);
                    game.putTerritoryInAnotherTerritory(cistern, territory);
                    discordGame.getTurnSummary().queueMessage("A Cistern has been discovered in " + territory.getTerritoryName() + "!");
                    territory.setDiscovered(true);
                }
                case "Ecological Testing Station" -> {
                    Territory ecologicalTestingStation = territories.addDiscoveryToken("Ecological Testing Station", false);
                    game.putTerritoryInAnotherTerritory(ecologicalTestingStation, territory);
                    discordGame.getTurnSummary().queueMessage("An Ecological Testing Station has been discovered in " + territory.getTerritoryName() + "!");
                    territory.setDiscovered(true);
                }
                case "Shrine" -> {
                    Territory shrine = territories.addDiscoveryToken("Shrine", false);
                    game.putTerritoryInAnotherTerritory(shrine, territory);
                    discordGame.getTurnSummary().queueMessage("A Shrine has been discovered in " + territory.getTerritoryName() + "!");
                    territory.setDiscovered(true);
                }
                case "Orgiz Processing Station" -> {
                    Territory orgizProcessingStation = territories.addDiscoveryToken("Orgiz Processing Station", false);
                    game.putTerritoryInAnotherTerritory(orgizProcessingStation, territory);
                    discordGame.getTurnSummary().queueMessage("The Orgiz Processing Station has been discovered in " + territory.getTerritoryName() + "!");
                    territory.setDiscovered(true);
                }
                case "Treachery Card Stash" -> {
                    TreacheryCard card = game.getTreacheryDeck().pollLast();
                    faction.addTreacheryCard(card);
                    if (faction.getTreacheryHand().size() > faction.getHandLimit()) {
                        discordGame.getFactionChat(faction.getName()).queueMessage("Your hand is over the limit. Please select a card to discard.");
                    }
                    discordGame.getFactionLedger(faction).queueMessage(Objects.requireNonNull(card).name() + " found in " + territory.getTerritoryName());
                    discordGame.getTurnSummary().queueMessage(faction.getEmoji() + " has discovered a " + Emojis.TREACHERY + " stash in " + territory.getTerritoryName() + "!");
                    territory.setDiscoveryToken(null);
                    territory.setDiscovered(false);

                }
                case "Spice Stash" -> {
                    faction.addSpice(7, Emojis.SPICE + " stash discovery token");
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

            game.setUpdated(UpdateType.MAP);
    }
}
