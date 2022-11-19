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
        return StreamSupport.stream(
                Spliterators.spliteratorUnknownSize(gameState.getFactions(), 0),
                        false)
                .filter(faction -> faction.toLowerCase().contains(search.toLowerCase()))
                .map(faction -> new Command.Choice(faction, faction))
                .collect(Collectors.toList());
    }
}
