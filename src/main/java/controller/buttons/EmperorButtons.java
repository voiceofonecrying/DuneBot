package controller.buttons;

import constants.Emojis;
import exceptions.ChannelNotFoundException;
import controller.DiscordGame;
import model.DuneChoice;
import model.Game;
import model.TreacheryCard;
import model.factions.EmperorFaction;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

public class EmperorButtons implements Pressable {
    public static void press(ButtonInteractionEvent event, Game game, DiscordGame discordGame) throws ChannelNotFoundException {

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

        List<DuneChoice> choices = emperor.getTreacheryHand().stream().map(card -> new DuneChoice("emperor-discard-" + card.name(), card.name())).collect(Collectors.toList());
        choices.add(new DuneChoice("secondary", "emperor-finished-discarding", "Done"));
        emperor.getChat().publish("Use these buttons to discard " + Emojis.TREACHERY + " from hand at the cost of 2 " + Emojis.SPICE + " per card.", choices);
        discordGame.pushGame();
    }
}
