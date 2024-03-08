package view.factions;

import controller.DiscordGame;
import exceptions.ChannelNotFoundException;
import model.EcazAmbassador;
import model.factions.EcazFaction;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import utils.CardImages;

import java.util.ArrayList;
import java.util.List;

public class EcazView extends FactionView {
    protected EcazFaction faction;
    public EcazView(DiscordGame discordGame, EcazFaction faction) throws ChannelNotFoundException {
        super(discordGame, faction);
        this.faction = faction;
    }

    @Override
    protected List<MessageEmbed.Field> additionalSummaryFields() {
        List<MessageEmbed.Field> returnList = new ArrayList<>();

        if (faction.getLoyalLeader() != null) {
            returnList.add(
                    new MessageEmbed.Field(
                            "Loyal Leader",
                            discordGame.tagEmojis(faction.getLoyalLeader().getName()),
                            true
                    )
            );
        }

        return returnList;
    }

    @Override
    protected List<MessageEmbed> additionalEmbedsBottom() {
        return faction.getAmbassadorSupply().stream()
                .map(this::getAmbassadorEmbed)
                .toList();
    }

    private MessageEmbed getAmbassadorEmbed(String id) {
        EcazAmbassador ambassador = new EcazAmbassador(id);
        EmbedBuilder embedBuilder = new EmbedBuilder()
                .setTitle(ambassador.name() + " Ambassador in Supply")
                .setColor(faction.getColor())
                .setDescription(ambassador.description())
                .setThumbnail(CardImages.getEcazAmbassadorImageLink(discordGame.getEvent().getGuild(), ambassador.name()));

        return embedBuilder.build();
    }
}
