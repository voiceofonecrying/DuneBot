package controller.commands;

import constants.Emojis;
import exceptions.ChannelNotFoundException;
import exceptions.InvalidOptionException;
import model.*;
import model.factions.Faction;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
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
            if (event.getComponentId().startsWith("ship-sector-")) filterBySector(event, game, discordGame, true);
            else if (event.getComponentId().startsWith("ship-")) queueSectorButtons(event, game, discordGame, true);
            else if (event.getComponentId().startsWith("add-force-shipment-")) addForces(event, game, discordGame, true);
            else if (event.getComponentId().startsWith("add-special-force-shipment-")) addSpecialForces(event, game, discordGame, true);
            else if (event.getComponentId().startsWith("add-force-movement-")) addForces(event, game, discordGame, false);
            else if (event.getComponentId().startsWith("add-special-force-movement-")) addSpecialForces(event, game, discordGame, false);
            else if (event.getComponentId().startsWith("moving-from-")) queueMoveableTerritories(event, game, discordGame);
            else if (event.getComponentId().startsWith("move-sector-")) filterBySector(event, game, discordGame, false);
            else if (event.getComponentId().startsWith("move-")) queueSectorButtons(event, game, discordGame, false);
            switch (event.getComponentId()) {
                case "shipment" -> queueShippingButtons(event);
                case "pass-shipment" -> passShipment(event, game, discordGame);
                case "stronghold" -> queueStrongholdShippingButtons(event, game);
                case "spice-blow" -> queueSpiceBlowShippingButtons(event);
                case "rock" -> queueRockShippingButtons(event);
                case "other" -> queueOtherShippingButtons(event);
                case "reset-shipping-forces" -> resetForces(event, game, discordGame, true);
                case "reset-shipment" -> resetMovement(event, game, discordGame, true);
                case "execute-shipment" -> executeShipment(event, game, discordGame);
                case "reset-moving-forces" -> resetForces(event, game, discordGame, false);
                case "reset-movement" -> resetMovement(event, game, discordGame, false);
                case "execute-movement" -> executeMovement(event, game, discordGame);
            }
        } catch (ChannelNotFoundException | IOException | InvalidOptionException e) {
            throw new RuntimeException(e);
        }
    }

    private void executeMovement(ButtonInteractionEvent event, Game game, DiscordGame discordGame) throws ChannelNotFoundException, InvalidOptionException, IOException {
        Faction faction = game.getFactions().stream().filter(f -> f.getPlayer().contains(event.getMember().getUser().getId())).findAny().get();
        Movement movement = faction.getMovement();
        movement.execute(discordGame, game, faction);
    }

    private void queueMoveableTerritories(ButtonInteractionEvent event, Game game, DiscordGame discordGame) throws ChannelNotFoundException {
        Faction faction = game.getFactions().stream().filter(f -> f.getPlayer().contains(event.getMember().getUser().getId())).findAny().get();
        Territory from = game.getTerritory(event.getComponentId().replace("moving-from-", ""));
        faction.getMovement().setMovingFrom(from.getTerritoryName());
        int spacesCanMove = 1;
        if (faction.getName().equals("Fremen") || (faction.getName().equals("Ix") && !Objects.equals(from.getForce(faction.getSpecialReserves().getName()).getName(), ""))) spacesCanMove = 2;
        if (game.getTerritory("Arrakeen").getActiveFactions(game).stream().anyMatch(f -> f.getName().equals(faction.getName())) ||
        game.getTerritory("Carthag").getActiveFactions(game).stream().anyMatch(f -> f.getName().equals(faction.getName()))) spacesCanMove = 3;
        Set<String> moveableTerritories = getAdjacentTerritoryNames(from.getTerritoryName(), spacesCanMove, game);
        moveableTerritories.remove(from.getTerritoryName());
        TreeSet<Button> moveToButtons = new TreeSet<>(Comparator.comparing(Button::getLabel));

        for (String territory : moveableTerritories) {
            moveToButtons.add(Button.primary("move-" + territory, territory));
        }

        WebhookMessageCreateAction<Message> message = event.getHook().sendMessage("Where will your forces move to?");
        List<MessageCreateRequest> messageList = arrangeButtonsInActionRow(message, moveToButtons, discordGame, event);

        for (MessageCreateRequest messageCreateRequest : messageList) {
            if (messageCreateRequest instanceof MessageCreateAction) {
                MessageCreateAction messageCreateAction = (MessageCreateAction) messageCreateRequest;
                messageCreateAction.queue();
            }
            else {
                WebhookMessageCreateAction<Message> messageCreateAction = (WebhookMessageCreateAction<Message>) messageCreateRequest;
                messageCreateAction.queue();
            }
        }
        discordGame.pushGame();
    }

    public Set<String> getAdjacentTerritoryNames(String territory, int spacesAway, Game game) {
        if (spacesAway == 0) return new HashSet<>();

        Set<String> adjacentTerritories = new HashSet<>(game.getAdjacencyList().get(territory));
        Set<String> second = new HashSet<>();
        for (String adjacentTerritory : adjacentTerritories) {
            second.addAll(getAdjacentTerritoryNames(adjacentTerritory, spacesAway - 1, game));
        }
        adjacentTerritories.addAll(second);
        return adjacentTerritories;
    }

    private void executeShipment(ButtonInteractionEvent event, Game game, DiscordGame discordGame) throws ChannelNotFoundException, IOException {
        Faction faction = game.getFactions().stream().filter(f -> f.getPlayer().contains(event.getMember().getUser().getId())).findAny().get();
        Shipment shipment = faction.getShipment();
        shipment.execute(discordGame, game, faction);
        queueMovementButtons(game, faction, event, discordGame);
    }

    private void resetMovement(ButtonInteractionEvent event, Game game, DiscordGame discordGame, boolean isShipment) throws ChannelNotFoundException {
        Faction faction = game.getFactions().stream().filter(f -> f.getPlayer().contains(event.getMember().getUser().getId())).findAny().get();
        if (isShipment){
            faction.getShipment().setForce(0);
            faction.getShipment().setSpecialForce(0);
            faction.getShipment().setTerritoryName("");
            queueShippingButtons(event);
        } else {
            faction.getMovement().setForce(0);
            faction.getMovement().setSpecialForce(0);
            faction.getMovement().setMovingFrom("");
            faction.getMovement().setMovingTo("");
            queueMovementButtons(game, faction, event, discordGame);
        }
        discordGame.pushGame();
    }

    private void resetForces(ButtonInteractionEvent event, Game game, DiscordGame discordGame, boolean isShipment) throws ChannelNotFoundException {
        Faction faction = game.getFactions().stream().filter(f -> f.getPlayer().contains(event.getMember().getUser().getId())).findAny().get();
        if (isShipment) {
            faction.getShipment().setForce(0);
            faction.getShipment().setSpecialForce(0);
            queueForcesButtons(event, game, discordGame, faction, true);
        } else {
            faction.getMovement().setForce(0);
            faction.getMovement().setSpecialForce(0);
            queueForcesButtons(event, game, discordGame, faction, false);
        }
        discordGame.pushGame();
    }

    private void addForces(ButtonInteractionEvent event, Game game, DiscordGame discordGame, boolean isShipment) throws ChannelNotFoundException {
        Faction faction = game.getFactions().stream().filter(f -> f.getPlayer().contains(event.getMember().getUser().getId())).findAny().get();
        if (isShipment) faction.getShipment().setForce((faction.getShipment().getForce() + Integer.parseInt(event.getComponentId().replace("add-force-shipment-", ""))));
        else faction.getMovement().setForce((faction.getMovement().getForce() + Integer.parseInt(event.getComponentId().replace("add-force-movement-", ""))));
        queueForcesButtons(event, game, discordGame, faction, isShipment);
        discordGame.pushGame();
    }

    private void addSpecialForces(ButtonInteractionEvent event, Game game, DiscordGame discordGame, boolean isShipment) throws ChannelNotFoundException {
        Faction faction = game.getFactions().stream().filter(f -> f.getPlayer().contains(event.getMember().getUser().getId())).findAny().get();
        if (isShipment) faction.getShipment().setSpecialForce((faction.getShipment().getSpecialForce() + Integer.parseInt(event.getComponentId().replace("add-special-force-shipment-", ""))));
        else faction.getMovement().setSpecialForce((faction.getMovement().getSpecialForce() + Integer.parseInt(event.getComponentId().replace("add-special-force-movement-", ""))));
        queueForcesButtons(event, game, discordGame, faction, isShipment);
        discordGame.pushGame();
    }

    private void filterBySector(ButtonInteractionEvent event, Game game, DiscordGame discordGame, boolean isShipment) throws ChannelNotFoundException {
        String shipmentOrMovement = isShipment ? "ship-" : "move-";
        Faction faction = game.getFactions().stream().filter(f -> f.getPlayer().contains(event.getMember().getUser().getId())).findAny().get();
        Territory territory = game.getTerritories().values().stream().filter(t -> t.getTerritoryName().contains(
                event.getComponentId().replace(shipmentOrMovement + "sector-", "").replace("-", " "))
        ).findFirst().orElseThrow();

        if (isShipment) faction.getShipment().setTerritoryName(territory.getTerritoryName());
        else faction.getMovement().setMovingTo(territory.getTerritoryName());

        queueForcesButtons(event, game, discordGame, faction, isShipment);
        discordGame.pushGame();
    }

    private void queueForcesButtons(ButtonInteractionEvent event, Game game, DiscordGame discordGame, Faction faction, boolean isShipment) throws ChannelNotFoundException {

        TreeSet<Button> forcesButtons = new TreeSet<>(Comparator.comparing(Button::getLabel));
        String shipOrMove = isShipment ? "shipment" : "movement";
        int buttonLimitForces = isShipment ? faction.getReserves().getStrength() - faction.getShipment().getForce() :
                game.getTerritory(faction.getMovement().getMovingFrom()).getForce(faction.getName()).getStrength() - faction.getMovement().getForce();
        int buttonLimitSpecialForces = isShipment ? faction.getSpecialReserves().getStrength() - faction.getShipment().getSpecialForce() :
                game.getTerritory(faction.getMovement().getMovingFrom()).getForce(faction.getName() + "*").getStrength() - faction.getMovement().getSpecialForce();

        for (int i = 0; i < buttonLimitForces; i++) {
            forcesButtons.add(Button.primary("add-force-" + shipOrMove + "-" + (i + 1), "+" + (i + 1) + " " + Emojis.getForceEmoji(faction.getName())));
        }
        for (int i = 0; i < buttonLimitSpecialForces; i++) {
            forcesButtons.add(Button.primary("add-special-force-" + shipOrMove + "-" + (i + 1), "+" + (i + 1) + " " + Emojis.getForceEmoji(faction.getName() + "*")));
        }

        if (isShipment) {
            int spice = faction.getShipment().getForce() + faction.getShipment().getSpecialForce();

            if (game.getTerritory(faction.getShipment().getTerritoryName()).isStronghold()) spice *= 2;

            if (faction.getName().equals("Fremen")) spice = 0;
            String specialForces = faction.getSpecialReserves().getName().equals("") ? "" : "\n" + faction.getShipment().getSpecialForce() + " " + Emojis.getForceEmoji(faction.getName() + "*");
            WebhookMessageCreateAction<Message> message = event.getHook().sendMessage("Use buttons below to add forces to your shipment." +
                    "\n**Currently shipping:\n" + faction.getShipment().getForce() +  " " + Emojis.getForceEmoji(faction.getName())
                    + specialForces + "\n to " + faction.getShipment().getTerritoryName() + "\n for " + spice + Emojis.SPICE + "\n\nYou have " + faction.getSpice() + Emojis.SPICE + " to spend.**");
            arrangeButtonsInActionRow(message, forcesButtons, discordGame, event);
            message.addActionRow(Button.success("execute-shipment", "Confirm Shipment"), Button.danger("reset-shipping-forces", "Reset forces"), Button.danger("reset-shipment", "Start Over"));
            message.queue();
        }
        else {
            String specialForces = faction.getSpecialReserves().getName().equals("") ? "" : "\n" + faction.getMovement().getSpecialForce() + " " + Emojis.getForceEmoji(faction.getName() + "*");
            WebhookMessageCreateAction<Message> message = event.getHook().sendMessage("Use buttons below to add forces to your movement." +
                    "\n**Currently moving:\n" + faction.getMovement().getForce() + " " + Emojis.getForceEmoji(faction.getName()) + specialForces + "\n to " + faction.getMovement().getMovingTo() + "**");
            arrangeButtonsInActionRow(message, forcesButtons, discordGame, event);
            message.addActionRow(Button.success("execute-movement", "Confirm Movement"), Button.danger("reset-moving-forces", "Reset forces"), Button.danger("reset-movement", "Start Over"));
            message.queue();
        }
    }

    private void queueSectorButtons(ButtonInteractionEvent event, Game game, DiscordGame discordGame, boolean isShipment) throws ChannelNotFoundException {
        String shipmentOrMovement = isShipment ? "ship-" : "move-";
        Faction faction = game.getFactions().stream().filter(f -> f.getPlayer().contains(event.getMember().getUser().getId())).findAny().get();
        List<Territory> territory = game.getTerritories().values().stream().filter(t -> t.getTerritoryName().replaceAll("\\s*\\([^\\)]*\\)\\s*", "").equalsIgnoreCase(
                event.getComponentId().replace(shipmentOrMovement, "").replace("-", " "))
        ).toList();

        if (territory.size() == 1) {
            if (isShipment) faction.getShipment().setTerritoryName(territory.get(0).getTerritoryName());
            else faction.getMovement().setMovingTo(territory.get(0).getTerritoryName());
            queueForcesButtons(event, game, discordGame, faction, isShipment);
            discordGame.pushGame();
            return;
        }

        List<Button> buttons = new LinkedList<>();

        for (Territory sector : territory) {
            if (sector.getSpice() > 0) buttons.add(Button.primary(shipmentOrMovement + "sector-" + sector.getTerritoryName(), sector.getSector() + " " + Emojis.SPICE));
            else buttons.add(Button.primary(shipmentOrMovement + "sector-" + sector.getTerritoryName(), String.valueOf(sector.getSector())));
        }
        buttons.sort(Comparator.comparing(Button::getLabel));
        String backButtonId = isShipment ? "reset-shipment" : "reset-movement";
        event.getHook().sendMessage("Which sector?")
                .addActionRow(buttons)
                .addActionRow(Button.secondary(backButtonId, "back")).queue();
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
                        Button.secondary("reset-shipment", "back"),
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
                        Button.secondary("reset-shipment", "back"),
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
                        Button.secondary("reset-shipment", "back"),
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
        message.addActionRow(Button.secondary("reset-shipment", "back"),
                Button.danger("pass-shipment", "Pass Shipment"));
        message.queue();
    }

    private void passShipment(ButtonInteractionEvent event, Game game, DiscordGame discordGame) throws ChannelNotFoundException {
        Faction faction = game.getFactions().stream().filter(f -> f.getPlayer().contains(event.getMember().getUser().getId())).findAny().get();
        discordGame.sendMessage("turn-summary", faction.getEmoji() + " does not ship.");
        queueMovementButtons(game, faction, event, discordGame);
    }

    private void queueShippingButtons(ButtonInteractionEvent event) throws ChannelNotFoundException {

        event.getHook().sendMessage("What part of Arrakis would you like to ship to?")
                .addActionRow(Button.primary("stronghold", "Stronghold"),
                        Button.primary("spice-blow", "Spice Blow Territories"),
                        Button.primary("rock", "Rock Territories"),
                        Button.primary("other", "Somewhere else"),
                        Button.danger("pass-shipment", "I don't want to ship.")).queue();

    }

    private void queueMovementButtons(Game game, Faction faction, ButtonInteractionEvent event, DiscordGame discordGame) throws ChannelNotFoundException {
         WebhookMessageCreateAction<Message> message = event.getHook().sendMessage("Use the following buttons to perform your move.");

         TreeSet<Button> movingFromButtons = new TreeSet<>(Comparator.comparing(Button::getLabel));

        for (Territory territory : game.getTerritories().values()){
            if (territory.getForces().stream().anyMatch(force -> force.getFactionName().equals(faction.getName()))) movingFromButtons.add(Button.primary("moving-from-" + territory.getTerritoryName(), territory.getTerritoryName()));
        }

        arrangeButtonsInActionRow(message, movingFromButtons, discordGame, event);
        message.addActionRow(Button.danger("pass-movement", "No move")).queue();
    }

    private List<MessageCreateRequest> arrangeButtonsInActionRow(WebhookMessageCreateAction<Message> message, TreeSet<Button> buttons, DiscordGame discordGame, ButtonInteractionEvent event) throws ChannelNotFoundException {
        List<MessageCreateRequest> list = new LinkedList<>();
        MessageCreateRequest messageCreateRequest = message;
        int count = 0;
        while (!buttons.isEmpty()) {
            List<Button> actionRow = new LinkedList<>();
            for (int i = 0; i < 5; i++) {
                if (!buttons.isEmpty()) actionRow.add(buttons.pollFirst());
            }
            messageCreateRequest.addActionRow(actionRow);
            count++;
            if (count == 5 || buttons.isEmpty()) {
                list.add(messageCreateRequest);
                messageCreateRequest = discordGame.prepareMessage(event.getMessageChannel().getName(), "cont.");
                count = 0;
            }
        }
        return list;
    }
}
