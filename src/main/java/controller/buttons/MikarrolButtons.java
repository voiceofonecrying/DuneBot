package controller.buttons;

import controller.DiscordGame;
import controller.buttons.ButtonManager;
import controller.buttons.Pressable;
import controller.commands.ShowCommands;
import exceptions.ChannelNotFoundException;
import exceptions.InvalidGameStateException;
import model.Game;
import model.factions.Faction;
import model.factions.MikarrolFaction;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import java.awt.Color;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

import caches.EmojiCache;
import constants.Emojis;

import static controller.buttons.ShipmentAndMovementButtons.arrangeButtonsAndSend;
import static controller.buttons.ShipmentAndMovementButtons.getButtonComparator;

public class MikarrolButtons implements Pressable {
    // Buttons handled by this class must begin with "mikarrol"
    // And any button that begins with "mikarrol" must be handled by this class
    public static void press(ButtonInteractionEvent event, Game game, DiscordGame discordGame) throws ChannelNotFoundException, InvalidGameStateException, IOException {
        if (event.getComponentId().startsWith("mikarrol-discard-raid-card")) discardRaidedCard(event, game, discordGame);
        else if (event.getComponentId().equals("mikarrol-keep-raid-card")) keepRaidedCard(event, game, discordGame);
		else if (event.getComponentId().equals("mikarrol-raid")) raid(event, game, discordGame);
		else if (event.getComponentId().equals("mikarrol-decline-raid")) declineRaid(event, game, discordGame);
		else if (event.getComponentId().equals("mikarrol-finished-discarding")) finishDiscard(event, game, discordGame);
        else if (event.getComponentId().startsWith("mikarrol-discard-")) discard(event, game, discordGame);
		else if (event.getComponentId().startsWith("mikarrol-slavers-")) setSlavers(event, game, discordGame);
        else if (event.getComponentId().startsWith("mikarrol-ally-slavers-")) setAllySlavers(event, game, discordGame);
    }

	public static void sendSlaversButtons(MikarrolFaction mikarrol, Faction ally, boolean forAlly, DiscordGame discordGame, boolean isReply) throws ChannelNotFoundException, InvalidGameStateException {
		Faction primaryFaction = forAlly ? ally : (Faction)mikarrol;
		int currentAmount = forAlly ? mikarrol.getAllySlaversAmount() : mikarrol.getSlaversAmount();
		String forceEmoji = forAlly ? Emojis.getForceEmoji(ally.getName()) : Emojis.getForceEmoji("Mikarrol");
		String numButton = forAlly ? "mikarrol-ally-slavers-number" : "mikarrol-slavers-number";
		String resetButton = forAlly ? "mikarrol-ally-slavers-reset" : "mikarrol-slavers-reset";
		MessageCreateBuilder builder = new MessageCreateBuilder();
        EmbedBuilder embedBuilder = new EmbedBuilder().setColor(Color.DARK_GRAY);
        embedBuilder.setTitle(EmojiCache.tagEmojis("**Set Slavers Amount** " + mikarrol.getEmoji()));
        String infoChannelName = primaryFaction.getInfoChannelPrefix() + "-info";
        List<String> descriptionMessages = new ArrayList<>();
        if (primaryFaction.getTotalReservesStrength() > 0) {
            String message = "You are currently selling " + currentAmount + " " + forceEmoji + " at the end of this phase.";
            List<Button> buttons = new ArrayList<>();
            buttons.add(Button.primary(numButton, "Pick a number"));
            buttons.add(Button.secondary(resetButton, "Reset").withDisabled(currentAmount == 0));
            descriptionMessages.add(EmojiCache.tagEmojis(message));
            embedBuilder.appendDescription(String.join("\n", descriptionMessages));
            builder.addEmbeds(embedBuilder.build());
            builder.addComponents(ActionRow.of(buttons));
            if (isReply)
                discordGame.queueMessage(builder);
            else
                discordGame.queueMessage(infoChannelName, builder.build());
        }
	}

    private static void setSlavers(ButtonInteractionEvent event, Game game, DiscordGame discordGame) throws InvalidGameStateException, ChannelNotFoundException {
        MikarrolFaction faction = game.getMikarrolFaction();
        String forces = event.getComponentId().split("-")[2];
        switch (forces) {
            case "number" -> {
                TreeSet<Button> buttonList = new TreeSet<>(getButtonComparator());
                int limit = Math.min(faction.getTotalReservesStrength(), 20);
                for (int i = 0; i <= limit; i++)
                    buttonList.add(Button.primary("mikarrol-slavers-" + i + "-number", i + (i == faction.getSlaversAmount() ? " (Current)" : "")));
                arrangeButtonsAndSend("How many forces?", buttonList, discordGame);
                return;
            }
            case "reset" -> {
                faction.setSlaversAmount(0);
                faction.getChat().publish("You will not sell forces this phase.");
            }
            default -> {
                faction.setSlaversAmount(Integer.parseInt(forces));
                faction.getChat().publish("You will sell " + forces + " forces this phase.");
            }
        }

        sendSlaversButtons(faction, faction, false, discordGame, true);
        discordGame.pushGame();
    }

    private static void setAllySlavers(ButtonInteractionEvent event, Game game, DiscordGame discordGame) throws InvalidGameStateException, ChannelNotFoundException {
		Faction allyFaction = ButtonManager.getButtonPresser(event, game);
		if (allyFaction.getAlly() != "Mikarrol")
			throw new InvalidGameStateException("You are not currently allied with Mikarrol");
        String forces = event.getComponentId().split("-")[3];
        MikarrolFaction mikarrol = game.getMikarrolFaction();

        switch (forces) {
            case "number" -> {
                TreeSet<Button> buttonList = new TreeSet<>(getButtonComparator());
                int limit = Math.min(allyFaction.getTotalReservesStrength(), 20);
                for (int i = 0; i <= limit; i++)
                    buttonList.add(Button.primary("mikarrol-ally-slavers-" + i + "-number", i + (i == mikarrol.getAllySlaversAmount() ? " (Current)" : "")));
                arrangeButtonsAndSend("How many forces?", buttonList, discordGame);
                return;
            }
            case "reset" -> {
                mikarrol.setAllySlaversAmount(0);
                allyFaction.getChat().publish("You will not sell forces this phase.");
            }
            default -> {
                mikarrol.setAllySlaversAmount(Integer.parseInt(forces));
                allyFaction.getChat().publish("You will sell " + forces + " forces this phase.");
            }
        }

        sendSlaversButtons(mikarrol, allyFaction, true, discordGame, true);
        discordGame.pushGame();
    }

    private static void keepRaidedCard(ButtonInteractionEvent event, Game game, DiscordGame discordGame) throws InvalidGameStateException, ChannelNotFoundException {
        discordGame.queueDeleteMessage();
        game.getMikarrolFaction().keepRaidedCard();
        discordGame.pushGame();
    }

    private static void discardRaidedCard(ButtonInteractionEvent event, Game game, DiscordGame discordGame) throws InvalidGameStateException, ChannelNotFoundException {
        discordGame.queueDeleteMessage();
        game.getMikarrolFaction().discardRaidedCard();
        discordGame.pushGame();
    }

    public static void raid(ButtonInteractionEvent event, Game game, DiscordGame discordGame) throws InvalidGameStateException, ChannelNotFoundException {
        discordGame.queueDeleteMessage();
        game.getMikarrolFaction().raid();
        discordGame.pushGame();
    }

    public static void declineRaid(ButtonInteractionEvent event, Game game, DiscordGame discordGame) throws ChannelNotFoundException, InvalidGameStateException, IOException {
        discordGame.queueDeleteMessage();
        game.getMikarrolFaction().declineRaid();
        discordGame.pushGame();
    }

    public static void discard(ButtonInteractionEvent event, Game game, DiscordGame discordGame ) throws ChannelNotFoundException {
        String cardName = event.getComponentId().split("-")[2];
        discordGame.queueDeleteMessage();
        game.getMikarrolFaction().discard(cardName);
        discordGame.pushGame();
    }

    public static void finishDiscard(ButtonInteractionEvent event, Game game, DiscordGame discordGame) throws ChannelNotFoundException {
        discordGame.queueDeleteMessage();
        game.getMikarrolFaction().returnToNormalHandSize();
        discordGame.pushGame();
    }
}
