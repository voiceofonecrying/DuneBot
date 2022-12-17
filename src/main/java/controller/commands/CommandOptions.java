package controller.commands;

import model.*;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.CommandAutoCompleteInteraction;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

public class CommandOptions {
    public static List<Command.Choice> factions(@NotNull Game gameState, String search) {
        return gameState.getFactions().stream()
                .map(Faction::getName)
                .filter(factionName -> factionName.toLowerCase().contains(search.toLowerCase()))
                .map(factionName -> new Command.Choice(factionName, factionName))
                .collect(Collectors.toList());
    }

    public static List<Command.Choice> territories(@NotNull Game gameState, String search) {
        return gameState.getTerritories().values().stream()
                .map(Territory::getTerritoryName)
                .filter(territoryName -> territoryName.toLowerCase().contains(search.toLowerCase()))
                .map(territoryName -> new Command.Choice(territoryName, territoryName))
                .limit(25)
                .collect(Collectors.toList());
    }

    public static List<Command.Choice> traitors(CommandAutoCompleteInteractionEvent event, Game gameState, String searchValue) {
        Faction faction = gameState.getFaction(event.getOptionsByName("factionname").get(0).getAsString());
        return faction.getTraitorHand().stream().map(TraitorCard::name)
                .filter(traitor -> traitor.toLowerCase().contains(searchValue.toLowerCase()))
                .map(traitor -> new Command.Choice(traitor, traitor))
                .collect(Collectors.toList());
    }

    public static List<Command.Choice> cardsInHand(CommandAutoCompleteInteractionEvent event, Game gameState, String searchValue) {
        Faction faction = gameState.getFaction(event.getOptionsByName("factionname").get(0).getAsString());
        return faction.getTreacheryHand().stream().map(TreacheryCard::name)
                .filter(card -> card.toLowerCase().contains(searchValue.toLowerCase()))
                .map(card -> new Command.Choice(card, card))
                .collect(Collectors.toList());
    }

    public static List<Command.Choice> fromTerritories(CommandAutoCompleteInteractionEvent event, Game gameState, String searchValue) {
        Faction faction = gameState.getFaction(event.getOptionsByName("factionname").get(0).getAsString());
        List<Territory> territories = new LinkedList<>();
        for (Territory territory : gameState.getTerritories().values()) {
            if (territory.getForce(faction.getName()).getStrength() > 0 || territory.getForce(faction.getName() + "*").getStrength() > 0
            || (faction.getName().equals("BG") && territory.getForce("Advisor").getStrength() > 0)) {
                territories.add(territory);
            }
        }
        return territories.stream().map(Territory::getTerritoryName)
                .filter(territory -> territory.toLowerCase().contains(searchValue.toLowerCase()))
                .map(territory -> new Command.Choice(territory, territory))
                .collect(Collectors.toList());
    }

    public static List<Command.Choice> toTerritories(CommandAutoCompleteInteractionEvent event, Game gameState, String searchValue) {
        Faction faction = gameState.getFaction(event.getOptionsByName("factionname").get(0).getAsString());
        Territory fromTerritory = gameState.getTerritories().get(event.getOptionsByName("from").get(0).getAsString());
        return territories(gameState, searchValue);
        //TODO: Add adjacency list to Territory model and make a BFS algorithm that makes a list of territories x territories away from the from territory.
    }

    public static List<Command.Choice> bgTerritories(Game gameState, String searchValue) {
        List<Territory> territories = new LinkedList<>();
        for (Territory territory : gameState.getTerritories().values()) {
            if (territory.getForce("Advisor").getStrength() > 0 || territory.getForce("BG").getStrength() > 0) {
                territories.add(territory);
            }
        }
        return territories.stream().map(Territory::getTerritoryName)
                .filter(territory -> territory.toLowerCase().contains(searchValue.toLowerCase()))
                .map(territory -> new Command.Choice(territory, territory))
                .collect(Collectors.toList());
    }
}
