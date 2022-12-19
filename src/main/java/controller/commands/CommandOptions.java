package controller.commands;

import model.*;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.Command;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

public class CommandOptions {
    public static List<Command.Choice> factions(@NotNull Game gameState, String searchValue) {
        return gameState.getFactions().stream()
                .map(Faction::getName)
                .filter(factionName -> factionName.toLowerCase().matches(searchRegex(searchValue.toLowerCase())))
                .map(factionName -> new Command.Choice(factionName, factionName))
                .collect(Collectors.toList());
    }

    public static List<Command.Choice> territories(@NotNull Game gameState, String searchValue) {
        return gameState.getTerritories().values().stream()
                .map(Territory::getTerritoryName)
                .filter(territoryName -> territoryName.toLowerCase().matches(searchRegex(searchValue.toLowerCase())))
                .map(territoryName -> new Command.Choice(territoryName, territoryName))
                .limit(25)
                .collect(Collectors.toList());
    }

    public static List<Command.Choice> traitors(CommandAutoCompleteInteractionEvent event, Game gameState, String searchValue) {
        Faction faction = gameState.getFaction(event.getOptionsByName("factionname").get(0).getAsString());
        return faction.getTraitorHand().stream().map(TraitorCard::name)
                .filter(traitor -> traitor.toLowerCase().matches(searchRegex(searchValue.toLowerCase())))
                .map(traitor -> new Command.Choice(traitor, traitor))
                .collect(Collectors.toList());
    }

    public static List<Command.Choice> cardsInHand(CommandAutoCompleteInteractionEvent event, Game gameState, String searchValue) {
        Faction faction = gameState.getFaction(event.getOptionsByName("factionname").get(0).getAsString());
        return faction.getTreacheryHand().stream().map(TreacheryCard::name)
                .filter(card -> card.toLowerCase().matches(searchRegex(searchValue.toLowerCase())))
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
                .filter(territory -> territory.toLowerCase().matches(searchRegex(searchValue.toLowerCase())))
                .map(territory -> new Command.Choice(territory, territory))
                .collect(Collectors.toList());
    }


    public static List<Command.Choice> leaders(CommandAutoCompleteInteractionEvent event, Game gameState, String searchValue) {
        Faction faction = gameState.getFaction(event.getOptionsByName("factionname").get(0).getAsString());
        return faction.getLeaders().stream().map(Leader::name)
                .filter(leader -> leader.matches(searchRegex(searchValue)))
                .map(leader -> new Command.Choice(leader, leader))
                .collect(Collectors.toList());
    }

    public static List<Command.Choice> reviveLeaders(CommandAutoCompleteInteractionEvent event, Game gameState, String searchValue) {
        return gameState.getLeaderTanks().stream().map(Leader::name)
                .filter(leader -> leader.matches(searchRegex(searchValue)))
                .map(leader -> new Command.Choice(leader, leader))
                .collect(Collectors.toList());
    }

    public static List<Command.Choice> bgTerritories(Game gameState, String searchValue) {
        List<Territory> territories = new LinkedList<>();
        for (Territory territory : gameState.getTerritories().values()) {
            if (territory.getForce("Advisor").getStrength() > 0 || territory.getForce("BG").getStrength() > 0) {
                territories.add(territory);
            }
        }
        return territories.stream().map(Territory::getTerritoryName)
                .filter(territory -> territory.toLowerCase().matches(searchRegex(searchValue.toLowerCase())))
                .map(territory -> new Command.Choice(territory, territory))
                .collect(Collectors.toList());
    }

    private static String searchRegex(String searchValue) {
        StringBuilder searchRegex = new StringBuilder();
        searchRegex.append(".*");

        for (char c : searchValue.toCharArray()) {
            searchRegex.append(c).append(".*");
        }
        return searchRegex.toString();
    }
}
