package view.factions;

import controller.DiscordGame;
import exceptions.ChannelNotFoundException;
import model.TraitorCard;
import model.factions.BTFaction;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;

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
    protected List<MessageEmbed> additionalEmbedsTop() {
        if (!faction.getRevealedFaceDancers().isEmpty()) {
            EmbedBuilder faceDancerBuilder = new EmbedBuilder()
                    .setColor(faction.getColor());
            List<String> revealedFDs = faction.getRevealedFaceDancers().stream().map(TraitorCard::getEmojiAndNameString).toList();
            faceDancerBuilder.addField("Revealed Face Dancers", discordGame.tagEmojis(String.join("\n", revealedFDs)), false);
            return Collections.singletonList(faceDancerBuilder.build());
        }
        return new ArrayList<>();
    }
}
