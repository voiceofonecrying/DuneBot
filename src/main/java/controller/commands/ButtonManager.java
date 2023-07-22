package controller.commands;

import exceptions.ChannelNotFoundException;
import model.DiscordGame;
import model.Game;
import model.Territory;
import model.factions.Faction;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.requests.restaction.WebhookMessageCreateAction;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

public class ButtonManager extends ListenerAdapter {
    @Override
    public void onButtonInteraction(@NotNull ButtonInteractionEvent event){

        event.deferReply().queue();

        try {
            DiscordGame discordGame = new DiscordGame(event);
            Game game = discordGame.getGame();
            switch (event.getComponentId()) {
                case "shipment" -> shipForces(event, game);
                case "pass-shipment" -> passShipment(event, game, discordGame);
            }
        } catch (ChannelNotFoundException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void passShipment(ButtonInteractionEvent event, Game game, DiscordGame discordGame) throws ChannelNotFoundException {
        Faction faction = game.getFactions().stream().filter(f -> f.getPlayer().contains(event.getMember().getUser().getId())).findAny().get();
        discordGame.sendMessage("turn-summary", faction.getEmoji() + " does not ship.");
        queueMovementButtons(game, faction, event);
    }

    private void shipForces(ButtonInteractionEvent event, Game game) throws ChannelNotFoundException {

        event.getHook().sendMessage("What part of Arrakis would you like to ship to?")
                .addActionRow(Button.primary("stronghold", "Stronghold"),
                        Button.primary("spice-blow", "Spice Blow"),
                        Button.primary("rock", "Rock Territories"),
                        Button.primary("other", "Somewhere else"),
                        Button.danger("pass-shipment", "I don't want to ship.")).queue();

    }

    private void queueMovementButtons(Game game, Faction faction, ButtonInteractionEvent event) throws ChannelNotFoundException {
         WebhookMessageCreateAction<Message> message = event.getHook().sendMessage("Use the following buttons to perform your move.");

        for (Territory territory : game.getTerritories().values()){
            if (territory.getForces().stream().anyMatch(force -> force.getFactionName().equals(faction.getName()))) message.addActionRow(Button.primary(territory.getTerritoryName() + "_" + territory.getSector(), territory.getTerritoryName()));
        }
        message.addActionRow(Button.danger("pass-movement", "No move")).queue();
    }
}
