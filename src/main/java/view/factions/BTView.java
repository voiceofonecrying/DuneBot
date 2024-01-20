package view.factions;

import controller.DiscordGame;
import exceptions.ChannelNotFoundException;
import model.TraitorCard;
import model.factions.BTFaction;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class BTView extends FactionView {
    protected BTFaction faction;
    public BTView(DiscordGame discordGame, BTFaction faction) throws ChannelNotFoundException {
        super(discordGame, faction);
        this.faction = faction;
    }

    @Override
    protected List<MessageEmbed> additionalEmbeds() {
        if (!faction.getRevealedFaceDancers().isEmpty()) {
            EmbedBuilder faceDancerBuilder = new EmbedBuilder()
                    .setTitle("Revealed Face Dancers")
                    .setColor(faction.getColor());

            faction.getRevealedFaceDancers().stream()
                    .map(this::getFaceDancerField)
                    .forEach(faceDancerBuilder::addField);

            return Collections.singletonList(faceDancerBuilder.build());
        }

        return new ArrayList<>();
    }

    private MessageEmbed.Field getFaceDancerField(TraitorCard faceDancer) {
        return new MessageEmbed.Field(
                MessageFormat.format(
                        "{0} {1}",
                        game.getFaction(faceDancer.factionName()).getEmoji(), faceDancer.name()
                ),
                null,
                false
        );
    }
}
