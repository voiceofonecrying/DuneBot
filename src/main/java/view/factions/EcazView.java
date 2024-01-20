package view.factions;

import controller.DiscordGame;
import exceptions.ChannelNotFoundException;
import model.factions.EcazFaction;
import net.dv8tion.jda.api.entities.MessageEmbed;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class EcazView extends FactionView {
    protected EcazFaction faction;
    public EcazView(DiscordGame discordGame, EcazFaction faction) throws ChannelNotFoundException {
        super(discordGame, faction);
        this.faction = faction;
    }

    @Override
    protected List<MessageEmbed.Field> additionalSummaryFields() {
        if (faction.getLoyalLeader() != null) {
            return Collections.singletonList(
                    new MessageEmbed.Field(
                            "Loyal Leader",
                            discordGame.tagEmojis(faction.getLoyalLeader().name()),
                            true
                    )
            );
        }

        return new ArrayList<>();
    }
}
