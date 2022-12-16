package controller.commands;

import model.Faction;
import model.Game;
import model.Territory;
import model.TraitorCard;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.CommandAutoCompleteInteraction;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
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
}
