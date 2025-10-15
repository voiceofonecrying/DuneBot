package view.factions;

import constants.Emojis;
import controller.DiscordGame;
import enums.ChoamInflationType;
import exceptions.ChannelNotFoundException;
import model.factions.ChoamFaction;
import net.dv8tion.jda.api.entities.MessageEmbed;

import java.util.ArrayList;
import java.util.List;

public class ChoamView extends FactionView {
    protected ChoamFaction faction;

    public ChoamView(DiscordGame discordGame, ChoamFaction faction) throws ChannelNotFoundException {
        super(discordGame, faction);
        this.faction = faction;
    }

    @Override
    public List<MessageEmbed.Field> sharedFrontOfShieldFields() {
        List<MessageEmbed.Field> fields = new ArrayList<>();

        // Only show inflation if it's been set and is current or future (not past)
        if (faction.getFirstInflationRound() > 0) {
            int currentTurn = game.getTurn();
            int nextTurn = currentTurn + 1;

            // Inflation affects two consecutive rounds: firstInflationRound and firstInflationRound + 1
            // Only show if at least one of those rounds is current or future
            if (faction.getFirstInflationRound() + 1 >= currentTurn) {
                ChoamInflationType currentInflation = faction.getInflationType(currentTurn);
                ChoamInflationType nextInflation = faction.getInflationType(nextTurn);

                List<String> inflationMessages = new ArrayList<>();

                if (currentInflation != null) {
                    inflationMessages.add(buildInflationMessage(currentTurn, currentInflation));
                }

                if (nextInflation != null && nextInflation != currentInflation) {
                    inflationMessages.add(buildInflationMessage(nextTurn, nextInflation));
                }

                if (!inflationMessages.isEmpty()) {
                    fields.add(new MessageEmbed.Field(
                            discordGame.tagEmojis(Emojis.CHOAM + " Inflation"),
                            discordGame.tagEmojis(String.join("\n", inflationMessages)),
                            false
                    ));
                }
            }
        }

        return fields;
    }

    private String buildInflationMessage(int round, ChoamInflationType inflation) {
        String message = "R" + round + ": **" + inflation + "**";
        if (inflation == ChoamInflationType.DOUBLE) {
            message += " (x2, NO BRIBES)";
        } else if (inflation == ChoamInflationType.CANCEL) {
            message += " (Cancelled)";
        }
        return message;
    }
}
