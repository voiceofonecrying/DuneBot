package controller.buttons;

import controller.commands.RunCommands;
import controller.commands.ShowCommands;
import exceptions.ChannelNotFoundException;
import exceptions.InvalidOptionException;
import exceptions.InvalidGameStateException;
import model.DiscordGame;
import model.Game;
import model.factions.Faction;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

public class ButtonManager extends ListenerAdapter {
    @Override
    public void onButtonInteraction(@NotNull ButtonInteractionEvent event) {

        event.deferReply().queue();

        try {
            DiscordGame discordGame = new DiscordGame(event);
            Game game = discordGame.getGame();
                ShipmentAndMovementButtons.press(event, game, discordGame);
                EcazButtons.press(event, game, discordGame);
                IxButtons.press(event, game, discordGame);
                switch (event.getComponentId()) {
                    case "graphic" -> {
                        getButtonPresser(event, game).setGraphicDisplay(true);
                        discordGame.pushGame();
                        event.getHook().sendMessage("Graphic mode active").queue();
                        ShowCommands.writeFactionInfo(discordGame, getButtonPresser(event, game));
                    }
                    case "text" -> {
                        getButtonPresser(event, game).setGraphicDisplay(false);
                        discordGame.pushGame();
                        event.getHook().sendMessage("Text mode active").queue();
                        ShowCommands.writeFactionInfo(discordGame, getButtonPresser(event, game));
                    }
                }
        } catch (InvalidGameStateException e) {
            event.getHook().editOriginal(e.getMessage()).queue();
        } catch (Exception e) {
            event.getHook().editOriginal(e.getMessage()).queue();
            e.printStackTrace();
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
