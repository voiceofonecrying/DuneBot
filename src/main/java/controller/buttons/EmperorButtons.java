package controller.buttons;

import constants.Emojis;
import exceptions.ChannelNotFoundException;
import controller.DiscordGame;
import model.Game;
import model.TreacheryCard;
import model.factions.EmperorFaction;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

public class EmperorButtons implements Pressable {
    public static void press(ButtonInteractionEvent event, Game game, DiscordGame discordGame) throws ChannelNotFoundException, IOException {

        if (event.getComponentId().startsWith("emperor-discard-")) discardCard(event, game, discordGame);
        if (event.getComponentId().equals("emperor-finished-discarding")) finishedDiscarding(discordGame);

    }

    private static void finishedDiscarding(DiscordGame discordGame) {
        discordGame.queueMessage("You have discarded all that you want to discard.");
        discordGame.queueDeleteMessage();
    }

    private static void discardCard(ButtonInteractionEvent event, Game game, DiscordGame discordGame) throws ChannelNotFoundException {
        String cardName = event.getComponentId().split("-")[2];
        EmperorFaction emperor = (EmperorFaction)game.getFaction("Emperor");
        emperor.kaitainHighDiscard(cardName);
        discordGame.queueDeleteMessage();

        List<Button> buttons = new LinkedList<>();
        for (TreacheryCard card : emperor.getTreacheryHand()) {
            buttons.add(Button.primary("emperor-discard-" + card.name(), card.name()));
        }
        buttons.add(Button.secondary("emperor-finished-discarding", "Done"));
        discordGame.getEmperorChat().queueMessage("Use these buttons to discard " + Emojis.TREACHERY + " from hand at the cost of 2 " + Emojis.SPICE + " per card.", buttons);
        discordGame.pushGame();
    }
}
