package view.factions;

import controller.DiscordGame;
import exceptions.ChannelNotFoundException;
import model.factions.EmperorFaction;
import net.dv8tion.jda.api.entities.MessageEmbed;

import java.util.List;

public class EmperorView extends FactionView {
    protected EmperorFaction faction;
    public EmperorView(DiscordGame discordGame, EmperorFaction faction) throws ChannelNotFoundException {
        super(discordGame, faction);
        this.faction = faction;
    }

    @Override
    protected List<MessageEmbed> getHomeworldEmbeds() {
        return List.of(
                getHomeworldEmbed(faction.getHomeworld(), faction.isHighThreshold()),
                getHomeworldEmbed("Salusa Secundus", faction.isSecundusHighThreshold())
        );
    }
}
