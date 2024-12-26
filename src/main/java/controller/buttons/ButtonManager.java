package controller.buttons;

import constants.Emojis;
import controller.CommandCompletionGuard;
import controller.Queue;
import controller.commands.ShowCommands;
import exceptions.InvalidGameStateException;
import controller.DiscordGame;
import model.Game;
import model.MentatPause;
import model.factions.Faction;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import static controller.commands.ShowCommands.refreshChangedInfo;

public class ButtonManager extends ListenerAdapter {
    static boolean allowModButtonPress = false;
    static Set<Long> buttonMessageIds = new HashSet<>();

    public static void setAllowModButtonPress() {
        allowModButtonPress = true;
    }

    public static Faction getButtonPresser(ButtonInteractionEvent event, Game game) {
        try {
            if (allowModButtonPress) {
                String channelName = event.getChannel() instanceof ThreadChannel ?
                        event.getChannel().asThreadChannel().getParentMessageChannel().getName() :
                        event.getChannel().getName();

                return game.getFactions().stream()
                        .filter(f -> (f.getName().toLowerCase() + "-info").equals(channelName))
                        .findAny()
                        .orElseThrow();
            } else {
                return game.getFactions().stream()
                        .filter(f -> f.getPlayer().contains(Objects.requireNonNull(event.getMember()).getUser().getId()))
                        .findAny()
                        .orElseThrow();
            }
        } catch (Exception e) {
            System.out.println(Objects.requireNonNull(event.getMember()).getUser().getId());
            for (Faction faction : game.getFactions()) {
                System.out.println(faction.getPlayer());
            }
            throw e;
        }
    }

    public static void deleteAllButtonsInChannel(MessageChannel channel) {
        List<Message> messages = channel.getHistoryAround(channel.getLatestMessageId(), 100).complete().getRetrievedHistory();
        for (Message message : messages) {
            if (!message.getButtons().isEmpty()) {
                try {
                    message.delete().complete();
                } catch (Exception ignore) {}
            }
        }
    }

    @Override
    public void onButtonInteraction(@NotNull ButtonInteractionEvent event) {
        Long messageId = event.getMessageIdLong();
        if (buttonMessageIds.contains(messageId)) {
            event.reply("You already pressed a button! Please grab a chairdog, pour spice coffee, and wait for me to finish").queue();
            return;
        }
        buttonMessageIds.add(messageId);
        CommandCompletionGuard.incrementCommandCount();
        event.deferReply().queue();
        String categoryName = Objects.requireNonNull(DiscordGame.categoryFromEvent(event)).getName();
        CompletableFuture<Void> future = Queue.getFuture(categoryName);
        Queue.putFuture(categoryName, future
                .thenRunAsync(() -> runButtonCommand(event))
                .thenRunAsync(() -> buttonMessageIds.remove(messageId))
                .thenRunAsync(CommandCompletionGuard::decrementCommandCount));
    }

    private void runButtonCommand(@NotNull ButtonInteractionEvent event) {
        try {
            DiscordGame discordGame = new DiscordGame(event);
            Game game = discordGame.getGame();
            if (event.getComponentId().startsWith("bg"))
                BGButtons.press(event, game, discordGame);
            else if (event.getComponentId().startsWith("bt"))
                BTButtons.press(event, game, discordGame);
            else if (event.getComponentId().startsWith("emperor"))
                EmperorButtons.press(event, game, discordGame);
            else if (event.getComponentId().startsWith("ecaz"))
                EcazButtons.press(event, game, discordGame);
            else if (event.getComponentId().startsWith("fremen"))
                FremenButtons.press(event, game, discordGame);
            else if (event.getComponentId().startsWith("moritani"))
                MoritaniButtons.press(event, game, discordGame);
            else if (event.getComponentId().startsWith("storm"))
                StormButtons.press(event, game, discordGame);
            else if (event.getComponentId().startsWith("bidding"))
                BiddingButtons.press(event, game, discordGame);
            else {
                ShipmentAndMovementButtons.press(event, game, discordGame);
                BattleButtons.press(event, game, discordGame);
                SpiceCollectionButtons.press(event, game, discordGame);
                RevivalButtons.press(event, game, discordGame);
                FactionButtons.press(event, game, discordGame);
                IxButtons.press(event, game, discordGame);
                RicheseButtons.press(event, game, discordGame);
                ChoamButtons.press(event, game, discordGame);
                switch (event.getComponentId()) {
                    case "graphic" -> {
                        getButtonPresser(event, game).setGraphicDisplay(true);
                        discordGame.queueMessage("Graphic mode active");
                        discordGame.pushGame();
                        ShowCommands.drawFactionInfo(discordGame, game, getButtonPresser(event, game).getName());
                    }
                    case "text" -> {
                        getButtonPresser(event, game).setGraphicDisplay(false);
                        discordGame.queueMessage("Text mode active");
                        discordGame.pushGame();
                        ShowCommands.writeFactionInfo(discordGame, getButtonPresser(event, game));
                    }
                    case "charity-accept" -> {
                        getButtonPresser(event, game).setDecliningCharity(false);
                        discordGame.queueMessage("You will receive CHOAM charity if you have less than 2 " + Emojis.SPICE + ".");
                        discordGame.pushGame();
                        ShowCommands.writeFactionInfo(discordGame, getButtonPresser(event, game));
                    }
                    case "charity-decline" -> {
                        getButtonPresser(event, game).setDecliningCharity(true);
                        discordGame.queueMessage("You will not receive CHOAM charity even if you have less than 2 " + Emojis.SPICE + ".");
                        discordGame.pushGame();
                        ShowCommands.writeFactionInfo(discordGame, getButtonPresser(event, game));
                    }
                    case "extortion-pay" -> {
                        MentatPause mentatPause = game.getMentatPause();
                        if (mentatPause == null || mentatPause.isExtortionInactive())
                            discordGame.queueMessage("Extortion has already been resolved. You were willing to pay.");
                        else if (getButtonPresser(event, game).getSpice() >= 3) {
                            game.getMentatPause().factionWouldPayExtortion(game, getButtonPresser(event, game));
                            discordGame.queueMessage("You are willing to pay Extortion.");
                            discordGame.pushGame();
                        } else {
                            game.getMentatPause().factionDeclinesExtortion(game, getButtonPresser(event, game));
                            discordGame.queueMessage("You are willing to pay Extortion but do not have enough spice.");
                            discordGame.pushGame();
                        }
                    }
                    case "extortion-dont-pay" -> {
                        MentatPause mentatPause = game.getMentatPause();
                        if (mentatPause == null || mentatPause.isExtortionInactive())
                            discordGame.queueMessage("Extortion has already been resolved. You were not willing to pay.");
                        else {
                            game.getMentatPause().factionDeclinesExtortion(game, getButtonPresser(event, game));
                            discordGame.queueMessage("You will not pay Extortion.");
                            discordGame.pushGame();
                        }
                    }
                }
            }

            refreshChangedInfo(discordGame);
            discordGame.sendAllMessages();
            try {
                event.getMessage().delete().complete();
            } catch (Exception ignored) {}
        } catch (InvalidGameStateException e) {
            event.getHook().editOriginal(e.getMessage()).queue();
        } catch (Exception e) {
            event.getHook().editOriginal(e.getMessage()).queue();
            e.printStackTrace();
        }
    }
}
