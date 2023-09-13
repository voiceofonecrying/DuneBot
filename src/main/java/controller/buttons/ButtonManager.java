package controller.buttons;

import controller.Queue;
import controller.commands.ShowCommands;
import exceptions.InvalidGameStateException;
import model.DiscordGame;
import model.Game;
import model.factions.Faction;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import static controller.commands.ShowCommands.refreshChangedInfo;

public class ButtonManager extends ListenerAdapter {
    @Override
    public void onButtonInteraction(@NotNull ButtonInteractionEvent event) {
        event.deferReply().queue();
        String categoryName = event.getChannel().asTextChannel().getParentCategory().getName();
        CompletableFuture<Void> future = Queue.getFuture(categoryName);
        Queue.putFuture(categoryName, future.thenRunAsync(() -> runButtonCommand(event)));
    }

    private void runButtonCommand(@NotNull ButtonInteractionEvent event) {
        try {
            DiscordGame discordGame = new DiscordGame(event);
            Game game = discordGame.getGame();
            ShipmentAndMovementButtons.press(event, game, discordGame);
            RevivalButtons.press(event, game, discordGame);
            IxButtons.press(event, game, discordGame);
            BTButtons.press(event, game, discordGame);
            RicheseButtons.press(event, game, discordGame);
            EcazButtons.press(event, game, discordGame);
            MoritaniButtons.press(event, game, discordGame);
            BGButtons.press(event, game, discordGame);
            switch (event.getComponentId()) {
                case "graphic" -> {
                    getButtonPresser(event, game).setGraphicDisplay(true);
                    discordGame.queueMessage("Graphic mode active");
                    discordGame.pushGame();
                    ShowCommands.drawFactionInfo(discordGame, game,  getButtonPresser(event, game).getName());
                }
                case "text" -> {
                    getButtonPresser(event, game).setGraphicDisplay(false);
                    discordGame.queueMessage("Text mode active");
                    discordGame.pushGame();
                    ShowCommands.writeFactionInfo(discordGame, getButtonPresser(event, game));
                }
            }
            refreshChangedInfo(discordGame);
            discordGame.sendAllMessages();
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
    public static void deleteAllButtonsInChannel(MessageChannel channel) {
        List<Message> messages = channel.getHistoryAround(channel.getLatestMessageId(), 100).complete().getRetrievedHistory();
        for (Message message : messages) {
            if (!message.getButtons().isEmpty()) message.delete().complete();
        }
    }
}
