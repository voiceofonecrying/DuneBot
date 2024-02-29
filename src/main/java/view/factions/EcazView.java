package view.factions;

import controller.DiscordGame;
import exceptions.ChannelNotFoundException;
import model.factions.EcazFaction;
import net.dv8tion.jda.api.entities.MessageEmbed;

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

        String emojis = faction.getAmbassadorSupplyEmojis();
        returnList.add(new MessageEmbed.Field(
                "Ambassador Supply",
                emojis.isEmpty() ? "Empty" : discordGame.tagEmojis(emojis),
                true));

        return returnList;
    }
}
