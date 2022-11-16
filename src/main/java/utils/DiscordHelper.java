package utils;

import net.dv8tion.jda.api.entities.channel.concrete.Category;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

public class DiscordHelper {
    public static Category getGameCategory(SlashCommandInteractionEvent event) {
        return event.getChannel().asTextChannel().getParentCategory();
    }
}
