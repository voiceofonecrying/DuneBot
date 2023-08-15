package controller.buttons;

import exceptions.ChannelNotFoundException;
import exceptions.InvalidOptionException;
import model.DiscordGame;
import model.Game;
import model.factions.Faction;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

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
