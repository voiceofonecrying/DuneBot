package controller.commands;

import model.Game;
import net.dv8tion.jda.api.interactions.commands.Command;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Spliterators;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class CommandOptions {
    public static List<Command.Choice> factions(@NotNull Game gameState, String search) {
        return gameState.getFactions().stream()
                .map(faction -> faction.getName())
                .filter(factionName -> factionName.toLowerCase().contains(search.toLowerCase()))
                .map(factionName -> new Command.Choice(factionName, factionName))
                .collect(Collectors.toList());
    }
}
