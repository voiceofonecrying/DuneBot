package view.factions;

import constants.Emojis;
import controller.DiscordGame;
import exceptions.ChannelNotFoundException;
import model.factions.RicheseFaction;
import net.dv8tion.jda.api.entities.MessageEmbed;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RicheseView extends FactionView {
    protected RicheseFaction faction;
    public RicheseView(DiscordGame discordGame, RicheseFaction faction) throws ChannelNotFoundException {
        super(discordGame, faction);
        this.faction = faction;
    }

    @Override
    protected List<MessageEmbed.Field> additionalSummaryFields() {
        if (faction.hasFrontOfShieldNoField()) {
            return Collections.singletonList(
                    new MessageEmbed.Field(
                            "No-Field",
                            discordGame.tagEmojis(faction.getFrontOfShieldNoField() + " " + Emojis.NO_FIELD),
                            true
                    )
            );
        }

        return new ArrayList<>();
    }
}
