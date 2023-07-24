package controller.commands;

import constants.Emojis;
import exceptions.ChannelNotFoundException;
import model.DiscordGame;
import model.Game;
import model.Shipment;
import model.Territory;
import model.factions.Faction;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.requests.restaction.WebhookMessageCreateAction;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

public class ButtonManager extends ListenerAdapter {
    @Override
    public void onButtonInteraction(@NotNull ButtonInteractionEvent event){

        event.deferReply().queue();

        try {
            DiscordGame discordGame = new DiscordGame(event);
            Game game = discordGame.getGame();
            if (event.getComponentId().startsWith("ship-sector-")) filterBySector(event, game, discordGame);
            else if (event.getComponentId().startsWith("ship-")) queueSectorButtons(event, game, discordGame);
            else if (event.getComponentId().startsWith("add-force-shipment-")) addForceToShipment(event, game, discordGame);
            else if (event.getComponentId().startsWith("add-special-force-shipment-")) addSpecialForceToShipment(event, game, discordGame);
            switch (event.getComponentId()) {
                case "shipment" -> queueShippingButtons(event);
                case "pass-shipment" -> passShipment(event, game, discordGame);
                case "stronghold" -> queueStrongholdShippingButtons(event, game);
                case "spice-blow" -> queueSpiceBlowShippingButtons(event);
                case "rock" -> queueRockShippingButtons(event);
                case "other" -> queueOtherShippingButtons(event);
                case "reset-shipping-forces" -> resetShippingForces(event, game, discordGame);
                case "reset-shipment" -> resetShipment(event, game, discordGame);
                case "execute-shipment" -> executeShipment(event, game, discordGame);
            }
        } catch (ChannelNotFoundException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void executeShipment(ButtonInteractionEvent event, Game game, DiscordGame discordGame) throws ChannelNotFoundException, IOException {
        Faction faction = game.getFactions().stream().filter(f -> f.getPlayer().contains(event.getMember().getUser().getId())).findAny().get();
        Shipment shipment = faction.getShipment();
        shipment.execute(discordGame, game, faction);
        queueMovementButtons(game, faction, event);
    }

    private void resetShipment(ButtonInteractionEvent event, Game game, DiscordGame discordGame) throws ChannelNotFoundException {
        Faction faction = game.getFactions().stream().filter(f -> f.getPlayer().contains(event.getMember().getUser().getId())).findAny().get();
        faction.getShipment().setForce(0);
        faction.getShipment().setSpecialForce(0);
        faction.getShipment().setTerritoryName(null);
        queueShippingButtons(event);
        discordGame.pushGame();
    }

    private void resetShippingForces(ButtonInteractionEvent event, Game game, DiscordGame discordGame) throws ChannelNotFoundException {
        Faction faction = game.getFactions().stream().filter(f -> f.getPlayer().contains(event.getMember().getUser().getId())).findAny().get();
        faction.getShipment().setForce(0);
        faction.getShipment().setSpecialForce(0);
        queueForcesButtons(event, game, discordGame, faction);
        discordGame.pushGame();
    }

    private void addForceToShipment(ButtonInteractionEvent event, Game game, DiscordGame discordGame) throws ChannelNotFoundException {
        Faction faction = game.getFactions().stream().filter(f -> f.getPlayer().contains(event.getMember().getUser().getId())).findAny().get();
        faction.getShipment().setForce((faction.getShipment().getForce() + Integer.parseInt(event.getComponentId().replace("add-force-shipment-", ""))));
        queueForcesButtons(event, game, discordGame, faction);
        discordGame.pushGame();
    }

    private void addSpecialForceToShipment(ButtonInteractionEvent event, Game game, DiscordGame discordGame) throws ChannelNotFoundException {
        Faction faction = game.getFactions().stream().filter(f -> f.getPlayer().contains(event.getMember().getUser().getId())).findAny().get();
        faction.getShipment().setSpecialForce((faction.getShipment().getSpecialForce() + Integer.parseInt(event.getComponentId().replace("add-special-force-shipment-", ""))));
        queueForcesButtons(event, game, discordGame, faction);
        discordGame.pushGame();
    }

    private void filterBySector(ButtonInteractionEvent event, Game game, DiscordGame discordGame) throws ChannelNotFoundException {
        Faction faction = game.getFactions().stream().filter(f -> f.getPlayer().contains(event.getMember().getUser().getId())).findAny().get();
        Territory territory = game.getTerritories().values().stream().filter(t -> t.getTerritoryName().contains(
                event.getComponentId().replace("ship-sector-", "").replace("-", " "))
        ).findFirst().orElseThrow();

        faction.getShipment().setTerritoryName(territory.getTerritoryName());

        queueForcesButtons(event, game, discordGame, faction);
        discordGame.pushGame();
    }

    private void queueForcesButtons(ButtonInteractionEvent event, Game game, DiscordGame discordGame, Faction faction) {
        String specialForces = faction.getSpecialReserves().getName().equals("") ? "" : "\n" + faction.getShipment().getSpecialForce() + " " + Emojis.getForceEmoji(faction.getName() + "*");

        List<Button> forcesButtons = new LinkedList<>();

        for (int i = 0; i < faction.getReserves().getStrength() - faction.getShipment().getForce(); i++) {
            forcesButtons.add(Button.primary("add-force-shipment-" + (i + 1), (i + 1) + " " + Emojis.getForceEmoji(faction.getName())));
        }
        for (int i = 0; i < faction.getSpecialReserves().getStrength() - faction.getShipment().getSpecialForce(); i++) {
            forcesButtons.add(Button.primary("add-special-force-shipment-" + (i + 1), (i + 1) + " " + Emojis.getForceEmoji(faction.getName() + "*")));
        }

        WebhookMessageCreateAction<Message> message = event.getHook().sendMessage("Use buttons below to add forces to your shipment." +
                "\n**Currently shipping:\n" + faction.getShipment().getForce() +  " " + Emojis.getForceEmoji(faction.getName())
                + specialForces + "\n\n to " + faction.getShipment().getTerritoryName() + "**\n");

        if (!forcesButtons.isEmpty()) {
            if (forcesButtons.size() <= 5) message.addActionRow(forcesButtons);
            else if (forcesButtons.size() <= 10) message.addActionRow(forcesButtons.subList(0, 5)).addActionRow(forcesButtons.subList(5, forcesButtons.size()));
            else if (forcesButtons.size() <= 15) message.addActionRow(forcesButtons.subList(0, 5)).addActionRow(forcesButtons.subList(5, 10)).addActionRow(forcesButtons.subList(10, forcesButtons.size()));
            else message.addActionRow(forcesButtons.subList(0, 5)).addActionRow(forcesButtons.subList(5, 10)).addActionRow(forcesButtons.subList(10, 15)).addActionRow(forcesButtons.subList(15, forcesButtons.size()));
        }
        message.addActionRow(Button.success("execute-shipment", "Confirm Shipment"), Button.danger("reset-shipping-forces", "Reset forces"), Button.danger("reset-shipment", "Start Over"));
        message.queue();
    }

    private void queueSectorButtons(ButtonInteractionEvent event, Game game, DiscordGame discordGame) throws ChannelNotFoundException {
        Faction faction = game.getFactions().stream().filter(f -> f.getPlayer().contains(event.getMember().getUser().getId())).findAny().get();
        List<Territory> territory = game.getTerritories().values().stream().filter(t -> t.getTerritoryName().replaceAll("\\s*\\([^\\)]*\\)\\s*", "").equalsIgnoreCase(
                event.getComponentId().replace("ship-", "").replace("-", " "))
        ).toList();

        if (territory.size() == 1) {
            faction.getShipment().setTerritoryName(territory.get(0).getTerritoryName());
            queueForcesButtons(event, game, discordGame, faction);
            discordGame.pushGame();
            return;
        }

        List<Button> buttons = new LinkedList<>();

        for (Territory sector : territory) {
            if (sector.getSpice() > 0) buttons.add(Button.primary("ship-sector-" + sector.getTerritoryName(), sector.getSector() + " " + Emojis.SPICE));
            else buttons.add(Button.primary("ship-sector-" + sector.getTerritoryName(), String.valueOf(sector.getSector())));
        }
        buttons.sort(Comparator.comparing(Button::getLabel));
        event.getHook().sendMessage("Which sector?")
                .addActionRow(buttons)
                .addActionRow(Button.secondary("shipment", "back")).queue();
    }

    private void queueOtherShippingButtons(ButtonInteractionEvent event) {
        event.getHook().sendMessage("Which territory?")
                .addActionRow(Button.primary("ship-polar-sink", "Polar Sink"),
                        Button.primary("ship-cielago-depression", "Cielago Depression"),
                        Button.primary("ship-meridian", "Meridian"),
                        Button.primary("ship-cielago-east", "Cielago East"),
                        Button.primary("ship-harg-pass", "Harg Pass"))
                .addActionRow(
                        Button.primary("ship-gara-kulon", "Gara Kulon"),
                        Button.primary("ship-hole-in-the-rock", "Hole In The Rock"),
                        Button.primary("ship-basin", "Basin"),
                        Button.primary("ship-imperial-basin", "Imperial Basin"),
                        Button.primary("ship-arsunt", "Arsunt"))
                .addActionRow(
                        Button.primary("ship-tsimpo", "Tsimpo"),
                        Button.primary("ship-bight-of-the-cliff", "Bight Of The Cliff"),
                        Button.primary("ship-wind-pass", "Wind Pass"),
                        Button.primary("ship-the-greater-flat", "The Greater Flat"),
                        Button.primary("ship-cielago-west", "Cielago West"))
                .addActionRow(
                        Button.secondary("shipment", "back"),
                        Button.danger("pass-shipment", "Pass Shipment")).queue();
    }

    private void queueRockShippingButtons(ButtonInteractionEvent event) {
        event.getHook().sendMessage("Which rock territory?")
                .addActionRow(Button.primary("ship-false-wall-south", "False Wall South"),
                        Button.primary("ship-pasty-mesa", "Pasty Mesa"),
                        Button.primary("ship-false-wall-east", "False Wall East"),
                        Button.primary("ship-shield-wall", "Shield Wall"),
                        Button.primary("ship-rim-wall-west", "Rim Wall West"))
                .addActionRow(
                        Button.primary("ship-plastic-basin", "Plastic Basin"),
                        Button.primary("ship-false-wall-west", "False Wall West"),
                        Button.secondary("shipment", "back"),
                        Button.danger("pass-shipment", "Pass Shipment")).queue();
    }

    private void queueSpiceBlowShippingButtons(ButtonInteractionEvent event) {
        event.getHook().sendMessage("Which spice blow territory?")
                .addActionRow(Button.primary("ship-habbanya-ridge-flat", "Habbanya Ridge Flat"),
                        Button.primary("ship-cielago-south", "Cielago South"),
                        Button.primary("ship-broken-land", "Broken Land"),
                        Button.primary("ship-south-mesa", "South Mesa"),
                        Button.primary("ship-sihaya-ridge", "Sihaya Ridge"))
                .addActionRow(
                        Button.primary("ship-hagga-basin", "Hagga Basin"),
                        Button.primary("ship-red-chasm", "Red Chasm"),
                        Button.primary("ship-the-minor-erg", "The Minor Erg"),
                        Button.primary("ship-cielago-north", "Cielago North"),
                        Button.primary("ship-funeral-plain", "Funeral Plain"))
                .addActionRow(
                        Button.primary("ship-the-great-flat", "The Great Flat"),
                        Button.primary("ship-habbanya-erg", "Habbanya Erg"),
                        Button.primary("ship-old-gap", "Old Gap"),
                        Button.primary("ship-rock-outcroppings", "Rock Outcroppings"),
                        Button.primary("ship-wind-pass-north", "Wind Pass North"))
                .addActionRow(
                        Button.secondary("shipment", "back"),
                        Button.danger("pass-shipment", "Pass Shipment")).queue();
    }

    private void queueStrongholdShippingButtons(ButtonInteractionEvent event, Game game) {
        WebhookMessageCreateAction<Message> message = event.getHook().sendMessage("Which stronghold?")
                .addActionRow(Button.primary("ship-arrakeen", "Arrakeen"),
                        Button.primary("ship-carthag", "Carthag"),
                        Button.primary("ship-sietch-tabr", "Sietch Tabr"),
                        Button.primary("ship-tuek's-sietch", "Tuek's Sietch"),
                        Button.primary("ship-habbanya-sietch", "Habbanya Sietch"));

        if (game.hasFaction("Ix")) message.addActionRow(Button.primary("stronghold-ship-hms", "Hidden Mobile Stronghold"));
        message.addActionRow(Button.secondary("shipment", "back"),
                Button.danger("pass-shipment", "Pass Shipment"));
        message.queue();
    }

    private void passShipment(ButtonInteractionEvent event, Game game, DiscordGame discordGame) throws ChannelNotFoundException {
        Faction faction = game.getFactions().stream().filter(f -> f.getPlayer().contains(event.getMember().getUser().getId())).findAny().get();
        discordGame.sendMessage("turn-summary", faction.getEmoji() + " does not ship.");
        queueMovementButtons(game, faction, event);
    }

    private void queueShippingButtons(ButtonInteractionEvent event) throws ChannelNotFoundException {

        event.getHook().sendMessage("What part of Arrakis would you like to ship to?")
                .addActionRow(Button.primary("stronghold", "Stronghold"),
                        Button.primary("spice-blow", "Spice Blow Territories"),
                        Button.primary("rock", "Rock Territories"),
                        Button.primary("other", "Somewhere else"),
                        Button.danger("pass-shipment", "I don't want to ship.")).queue();

    }

    private void queueMovementButtons(Game game, Faction faction, ButtonInteractionEvent event) {
         WebhookMessageCreateAction<Message> message = event.getHook().sendMessage("Use the following buttons to perform your move.");

        for (Territory territory : game.getTerritories().values()){
            if (territory.getForces().stream().anyMatch(force -> force.getFactionName().equals(faction.getName()))) message.addActionRow(Button.primary(territory.getTerritoryName() + "_" + territory.getSector(), territory.getTerritoryName()));
        }
        message.addActionRow(Button.danger("pass-movement", "No move")).queue();
    }
}
