package controller.buttons;

import constants.Emojis;
import exceptions.ChannelNotFoundException;
import exceptions.InvalidOptionException;
import model.*;
import model.factions.Faction;
import model.factions.RicheseFaction;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonInteraction;
import net.dv8tion.jda.api.requests.restaction.MessageCreateAction;
import net.dv8tion.jda.api.requests.restaction.WebhookMessageCreateAction;
import net.dv8tion.jda.api.utils.messages.MessageCreateRequest;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.*;

public class ButtonManager extends ListenerAdapter {
    @Override
    public void onButtonInteraction(@NotNull ButtonInteractionEvent event){

        event.deferReply().queue();

        try {
            DiscordGame discordGame = new DiscordGame(event);
            Game game = discordGame.getGame();
                ShipmentAndMovementButtons.press(event, game, discordGame);
                EcazButtons.press(event, game, discordGame);

        } catch (ChannelNotFoundException | IOException | InvalidOptionException e) {
            throw new RuntimeException(e);
        }
    }

    public static Faction getButtonPresser(ButtonInteractionEvent event, Game game) {
        try {
        return game.getFactions().stream().filter(f -> f.getPlayer().contains(event.getMember().getUser().getId())).findAny().get();
        } catch (Exception e) {
            System.out.println(event.getMember().getUser().getId());
            for (Faction faction : game.getFactions()) {
                System.out.println(faction.getPlayer());
            }
            throw e;
        }
    }
}
