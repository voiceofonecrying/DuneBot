package view.factions;

import controller.DiscordGame;
import exceptions.ChannelNotFoundException;
import model.factions.MoritaniFaction;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MoritaniView extends FactionView {
    protected MoritaniFaction faction;

    public MoritaniView(DiscordGame discordGame, MoritaniFaction faction) throws ChannelNotFoundException {
        super(discordGame, faction);
        this.faction = faction;
    }

    @Override
    protected List<MessageEmbed> additionalEmbedsTop() {
        if (!faction.getAssassinationTargets().isEmpty()) {
            EmbedBuilder assassinationTargetsBuilder = new EmbedBuilder()
                    .setTitle("Assassinated targets")
                    .setColor(faction.getColor());

            faction.getAssassinationTargets().stream()
                    .map(this::getAssassinationTargetField)
                    .forEach(assassinationTargetsBuilder::addField);

            return Collections.singletonList(assassinationTargetsBuilder.build());
        }

        return new ArrayList<>();
    }

    private MessageEmbed.Field getAssassinationTargetField(String assassinationTarget) {
        return new MessageEmbed.Field(
                discordGame.tagEmojis(assassinationTarget),
                "",
                false
        );
    }
}
