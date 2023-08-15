package controller.buttons;

import constants.Emojis;
import exceptions.ChannelNotFoundException;
import exceptions.InvalidOptionException;
import model.DiscordGame;
import model.Game;
import model.Movement;
import model.Territory;
import model.factions.Faction;
import model.factions.RicheseFaction;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.requests.restaction.MessageCreateAction;
import net.dv8tion.jda.api.requests.restaction.WebhookMessageCreateAction;
import net.dv8tion.jda.api.utils.messages.MessageCreateRequest;

import java.io.IOException;
import java.util.*;

public class ShipmentAndMovementButtons implements Pressable {
    
    
    public static void press(ButtonInteractionEvent event, Game game, DiscordGame discordGame) throws ChannelNotFoundException, IOException, InvalidOptionException {

        if (event.getComponentId().startsWith("ship-sector-")) filterBySector(event, game, discordGame, true);
        else if (event.getComponentId().startsWith("ship-to-reserves-")) queueForcesButtons(event, game, discordGame, game.getFaction("Guild"), true);
        else if (event.getComponentId().startsWith("cross-ship-from-")) setCrossShipFrom(event, game, discordGame);
        else if (event.getComponentId().startsWith("ship-")) queueSectorButtons(event, game, discordGame, true);
        else if (event.getComponentId().startsWith("add-force-shipment-")) addForces(event, game, discordGame, true);
        else if (event.getComponentId().startsWith("add-special-force-shipment-"))
            addSpecialForces(event, game, discordGame, true);
        else if (event.getComponentId().startsWith("add-force-movement-")) addForces(event, game, discordGame, false);
        else if (event.getComponentId().startsWith("add-special-force-movement-"))
            addSpecialForces(event, game, discordGame, false);
        else if (event.getComponentId().startsWith("moving-from-")) queueMoveableTerritories(event, game, discordGame);
        else if (event.getComponentId().startsWith("move-sector-")) filterBySector(event, game, discordGame, false);
        else if (event.getComponentId().startsWith("move-")) queueSectorButtons(event, game, discordGame, false);
        else if (event.getComponentId().startsWith("richese-no-field-ship-"))
            richeseNoFieldShip(event, game, discordGame);
        else if (event.getComponentId().startsWith("support-")) supportAlly(event, game, discordGame);
        switch (event.getComponentId()) {
            case "shipment" -> queueShippingButtons(event, game, discordGame);
            case "pass-shipment" -> passShipment(event, game, discordGame);
            case "pass-movement" -> passMovement(event, game, discordGame);
            case "stronghold" -> queueStrongholdShippingButtons(event, game);
            case "spice-blow" -> queueSpiceBlowShippingButtons(event);
            case "rock" -> queueRockShippingButtons(event);
            case "other" -> queueOtherShippingButtons(event);
            case "reset-shipping-forces" -> resetForces(event, game, discordGame, true);
            case "reset-shipment" -> resetShipmentMovement(event, game, discordGame, true);
            case "execute-shipment" -> executeShipment(event, game, discordGame);
            case "reset-moving-forces" -> resetForces(event, game, discordGame, false);
            case "reset-movement" -> resetShipmentMovement(event, game, discordGame, false);
            case "execute-movement" -> executeMovement(event, game, discordGame);
            case "juice-of-sapho-first" -> playJuiceOfSapho(event, game, discordGame, false);
            case "juice-of-sapho-last" -> playJuiceOfSapho(event, game, discordGame, true);
            case "juice-of-sapho-don't-play" -> juiceOfSaphoDontPlay(event, game, discordGame);
            case "guild-take-turn" -> guildTakeTurn(event, game, discordGame);
            case "guild-wait-last" -> guildWaitLast(event, game, discordGame);
            case "guild-defer" -> guildDefer(event, game, discordGame);
            case "richese-no-field-move" -> richeseNoFieldMove(event, game, discordGame);
            case "guild-cross-ship" -> crossShip(event, game, discordGame);
            case "guild-ship-to-reserves" -> shipToReserves(event, game, discordGame);
        }
    }

    private static void setCrossShipFrom(ButtonInteractionEvent event, Game game, DiscordGame discordGame) throws ChannelNotFoundException {
        Faction faction = ButtonManager.getButtonPresser(event, game);
        String from = game.getTerritory(event.getComponentId().replace("cross-ship-from-", "")).getTerritoryName();
        faction.getShipment().setCrossShipFrom(from);
        queueShippingButtons(event, game, discordGame);
        game.getFaction("Guild").getShipment().setToReserves(false);
        discordGame.pushGame();
    }

    private static void shipToReserves(ButtonInteractionEvent event, Game game, DiscordGame discordGame) throws ChannelNotFoundException {
        TreeSet<Button> buttons = new TreeSet<>(getButtonComparator());
        for (Territory territory : game.getTerritories().values()) {
            if (territory.hasForce("Guild")) buttons.add(Button.primary("ship-to-reserves-" + territory.getTerritoryName(), territory.getTerritoryName()));
        }
        arrangeButtonsAndSend(event.getHook().sendMessage("Where would you like to ship to reserves from?"), buttons, discordGame, event);
        game.getFaction("Guild").getShipment().setToReserves(true);
        discordGame.pushGame();
    }

    private static void crossShip(ButtonInteractionEvent event, Game game, DiscordGame discordGame) throws ChannelNotFoundException {
        TreeSet<Button> buttons = new TreeSet<>(getButtonComparator());
        for (Territory territory : game.getTerritories().values()) {
            if (territory.hasForce("Guild")) buttons.add(Button.primary("cross-ship-from-" + territory.getTerritoryName(), territory.getTerritoryName()));
        }
        arrangeButtonsAndSend(event.getHook().sendMessage("Where would you like to ship from?"), buttons, discordGame, event);
        game.getFaction("Guild").getShipment().setToReserves(true);
        discordGame.pushGame();
    }

    private static void supportAlly(ButtonInteractionEvent event, Game game, DiscordGame discordGame) throws ChannelNotFoundException {
        Faction faction = ButtonManager.getButtonPresser(event, game);
        if (event.getComponentId().equals("support-max")) {
            game.getFaction(faction.getAlly()).setAllySpiceShipment(faction.getSpice());
            discordGame.sendMessage(faction.getAlly().toLowerCase() + "-chat", "Your ally will support your shipment this turn up to " + game.getFaction(faction.getAlly()).getAllySpiceShipment() + " " + Emojis.SPICE + "!");
            event.getHook().sendMessage("You have offered your ally all of your spice to ship with.").queue();
            discordGame.pushGame();
        } else if (event.getComponentId().equals("support-number")){
            TreeSet<Button> buttonList = new TreeSet<>(getButtonComparator());
            int limit = Math.min(faction.getSpice(), 40);
            for (int i = 0; i < limit; i++) {
                buttonList.add(Button.primary("support-" + (i + 1), Integer.toString(i + 1)));
            }
            arrangeButtonsAndSend(event.getHook().sendMessage("How much would you like to offer in support?"), buttonList, discordGame, event);
        } else {
            game.getFaction(faction.getAlly()).setAllySpiceShipment(Integer.parseInt(event.getComponentId().replace("support-", "")));
            discordGame.sendMessage(faction.getAlly().toLowerCase() + "-chat", "Your ally will support your shipment this turn up to " + game.getFaction(faction.getAlly()).getAllySpiceShipment() + " " + Emojis.SPICE + "!");
            event.getHook().sendMessage("You have offered your ally " + event.getComponentId().replace("support-", "") + " " + Emojis.SPICE + " to ship with.").queue();
            discordGame.pushGame();
        }
    }


    private static void guildDefer(ButtonInteractionEvent event, Game game, DiscordGame discordGame) throws ChannelNotFoundException {
        if (game.getTurnOrder().peekFirst().equals("juice-of-sapho-hold")) {
            event.getHook().sendMessage("Juice of Sapho is in play, please wait so that the holding player has a chance to use it.").queue();
            return;
        }
        if (!game.getTurnOrder().peekFirst().equals("guild-hold")) {
            event.getHook().sendMessage("It is the middle of someone's turn, this is not the right time to press this button.").queue();
            return;
        }
        if (game.getTurnOrder().size() == 1 && game.getTurnOrder().peekFirst().equals("guild-hold")) {
            event.getHook().sendMessage("Everyone else has taken their turn, you can no longer defer!").queue();
            return;
        }
        game.getTurnOrder().remove("guild-hold");
        event.getHook().sendMessage("You will defer this turn.").queue();
        discordGame.sendMessage("turn-summary", Emojis.GUILD + " does not ship at this time.");
        discordGame.pushGame();
    }

    private static void guildWaitLast(ButtonInteractionEvent event, Game game, DiscordGame discordGame) throws ChannelNotFoundException {
        if (game.getTurnOrder().peekFirst().equals("juice-of-sapho-hold")) {
            event.getHook().sendMessage("Juice of Sapho is in play, please wait so that the holding player has a chance to use it.").queue();
            return;
        }
        if (game.getTurnOrder().peekLast().equals("juice-of-sapho-last")) {
            event.getHook().sendMessage("Juice of Sapho was played, you cannot go last.").queue();
            return;
        }
        if (game.getTurnOrder().peekLast().equals("Guild")) {
            event.getHook().sendMessage("You have already decided to go last, pressing it again won't do anything.").queue();
            return;
        }
        if (game.getFaction("Guild").getShipment().hasShipped()) {
            event.getHook().sendMessage("You have already shipped this turn.").queue();
        }
        game.getTurnOrder().addLast("Guild");
        game.getTurnOrder().remove("guild-hold");
        event.getHook().sendMessage("You will take your turn last.").queue();
        discordGame.pushGame();
    }

    private static void guildTakeTurn(ButtonInteractionEvent event, Game game, DiscordGame discordGame) throws ChannelNotFoundException {
        if (game.getTurnOrder().peekFirst().equals("juice-of-sapho-hold")) {
            event.getHook().sendMessage("Juice of Sapho is in play, please wait so that the holding player has a chance to use it.").queue();
            return;
        }
        if (!game.getTurnOrder().peekFirst().equals("guild-hold")) {
            event.getHook().sendMessage("It is the middle of someone's turn, this is not the right time to press this button.").queue();
            return;
        }
        if (game.getFaction("Guild").getShipment().hasShipped()) {
            event.getHook().sendMessage("You have already shipped this turn.").queue();
        }
        game.getTurnOrder().remove("Guild");
        game.getTurnOrder().addFirst("Guild");
        game.getTurnOrder().remove("guild-hold");
        event.getHook().sendMessage("You will take your turn now.").queue();
        sendShipmentMessage("Guild", discordGame, game);
        discordGame.sendMessage("turn-summary", Emojis.GUILD + " will take their turn next.");
        discordGame.pushGame();
    }
    private static void juiceOfSaphoDontPlay(ButtonInteractionEvent event, Game game, DiscordGame discordGame) throws ChannelNotFoundException {
        Faction faction = ButtonManager.getButtonPresser(event, game);
        if (!faction.getTreacheryHand().stream().anyMatch(treacheryCard -> treacheryCard.name().equals("Juice of Sapho"))) return;
        game.getTurnOrder().remove("juice-of-sapho-hold");
        discordGame.sendMessage("turn-summary", "Juice of Sapho is not played at this time.");
        event.getHook().sendMessage("You will not play Juice of Sapho this turn.").queue();
        discordGame.pushGame();
    }

    private static void playJuiceOfSapho(ButtonInteractionEvent event, Game game, DiscordGame discordGame, boolean last) throws ChannelNotFoundException {
        Faction faction = ButtonManager.getButtonPresser(event, game);
        if (!faction.getTreacheryHand().stream().anyMatch(treacheryCard -> treacheryCard.name().equals("Juice of Sapho"))) {
            event.getHook().sendMessage("These buttons were not intended for you").setEphemeral(true).queue();
        }
        String lastFirst = last ? "last" : "first";
        game.getTurnOrder().remove("juice-of-sapho-hold");
        game.getTurnOrder().remove("guild-hold");
        game.getTurnOrder().remove(faction.getName());
        if (last) {
            game.getTurnOrder().addLast(faction.getName());
            game.getTurnOrder().addLast("juice-of-sapho-last");

        }
        else {
            game.getTurnOrder().addFirst(faction.getName());
        }
        game.getTreacheryDiscard().add(faction.removeTreacheryCard("Juice of Sapho"));
        event.getHook().sendMessage("Button pressed.  You will go " + lastFirst + " this turn.").setEphemeral(true).queue();
        discordGame.sendMessage("turn-summary", faction.getEmoji() + " plays Juice of Sapho to ship and move " + lastFirst + " this turn.");
        discordGame.sendMessage(game.getTurnOrder().peekFirst().toLowerCase() + "-chat", "You are now going first.");
        sendShipmentMessage(game.getTurnOrder().peekFirst(), discordGame, game);
        if (game.hasFaction("Guild")) game.getTurnOrder().addFirst("guild-hold");
        discordGame.pushGame();
    }

    private static void passMovement(ButtonInteractionEvent event, Game game, DiscordGame discordGame) throws ChannelNotFoundException {
        Faction faction = ButtonManager.getButtonPresser(event, game);
        if (faction.getMovement().hasMoved()) {
            event.getHook().sendMessage("You have already moved this turn.").queue();
            return;
        }
        event.getHook().sendMessage("Shipment and movement complete.").queue();
        discordGame.sendMessage("turn-summary", faction.getEmoji() + " does not move.");
        game.getTurnOrder().pollFirst();
        if (!game.getTurnOrder().isEmpty()){
            sendShipmentMessage(game.getTurnOrder().peekFirst(), discordGame, game);
        } else {
            discordGame.sendMessage("mod-info", "Everyone has taken their turn, please run advance.");
            discordGame.pushGame();
            return;
        }
        if (game.hasFaction("Guild") && !game.getFaction("Guild").getShipment().hasShipped() && !game.getTurnOrder().contains("Guild")) {
            game.getTurnOrder().addFirst("guild-hold");
            queueGuildTurnOrderButtons(discordGame);
        }
        if (game.getTurnOrder().size() > 1 && game.getTurnOrder().peekLast().equals("Guild")) discordGame.sendMessage("turn-summary", Emojis.GUILD + " does not ship at this time");

        discordGame.pushGame();
    }

    private static void executeMovement(ButtonInteractionEvent event, Game game, DiscordGame discordGame) throws ChannelNotFoundException, InvalidOptionException, IOException {
        Faction faction = ButtonManager.getButtonPresser(event, game);
        if (faction.getMovement().hasMoved()) {
            event.getHook().sendMessage("You have already moved this turn.").queue();
            return;
        }
        Movement movement = faction.getMovement();
        movement.execute(discordGame, game, faction);
        game.getTurnOrder().pollFirst();
        if (game.getTurnOrder().isEmpty()) {
            if (game.hasFaction("Guild") && !game.getFaction("Guild").getShipment().hasShipped()) {
                game.getTurnOrder().addFirst("Guild");
                sendShipmentMessage(game.getTurnOrder().peekFirst(), discordGame, game);
            }
        } else {
            sendShipmentMessage(game.getTurnOrder().peekFirst(), discordGame, game);
            if (game.hasFaction("Guild") && !game.getFaction("Guild").getShipment().hasShipped() && !game.getTurnOrder().contains("Guild")) {
                game.getTurnOrder().addFirst("guild-hold");
                queueGuildTurnOrderButtons(discordGame);
            }
        }
        if (game.getTurnOrder().peekLast().equals("Guild") && game.getTurnOrder().size() > 1) discordGame.sendMessage("turn-summary", Emojis.GUILD + " does not ship at this time");
        event.getHook().sendMessage("Shipment and movement complete.").queue();
        discordGame.pushGame();
    }

    private static void queueMoveableTerritories(ButtonInteractionEvent event, Game game, DiscordGame discordGame) throws ChannelNotFoundException {
        Faction faction = ButtonManager.getButtonPresser(event, game);
        Territory from = game.getTerritory(event.getComponentId().replace("moving-from-", ""));
        faction.getMovement().setMovingFrom(from.getTerritoryName());
        int spacesCanMove = 1;
        if (faction.getName().equals("Fremen") || (faction.getName().equals("Ix") && !Objects.equals(from.getForce(faction.getSpecialReserves().getName()).getName(), ""))) spacesCanMove = 2;
        if (game.getTerritory("Arrakeen").getActiveFactions(game).stream().anyMatch(f -> f.getName().equals(faction.getName())) ||
                game.getTerritory("Carthag").getActiveFactions(game).stream().anyMatch(f -> f.getName().equals(faction.getName()))) spacesCanMove = 3;
        Set<String> moveableTerritories = getAdjacentTerritoryNames(from.getTerritoryName().replaceAll("\\(.*\\)", "").strip(), spacesCanMove, game);
        moveableTerritories.remove(from.getTerritoryName());
        TreeSet<Button> moveToButtons = new TreeSet<>(Comparator.comparing(Button::getLabel));

        for (String territory : moveableTerritories) {
            moveToButtons.add(Button.primary("move-" + territory, territory));
        }

        arrangeButtonsAndSend(event.getHook().sendMessage("Where will your forces move to?"), moveToButtons, discordGame, event);

        discordGame.pushGame();
    }

    public static Set<String> getAdjacentTerritoryNames(String territory, int spacesAway, Game game) {
        if (spacesAway == 0) return new HashSet<>();

        Set<String> adjacentTerritories = new HashSet<>(game.getAdjacencyList().get(territory));
        Set<String> second = new HashSet<>();
        for (String adjacentTerritory : adjacentTerritories) {
            second.addAll(getAdjacentTerritoryNames(adjacentTerritory, spacesAway - 1, game));
        }
        adjacentTerritories.addAll(second);
        return adjacentTerritories;
    }

    private static void executeShipment(ButtonInteractionEvent event, Game game, DiscordGame discordGame) throws ChannelNotFoundException, IOException {
        Faction faction = ButtonManager.getButtonPresser(event, game);
        if (faction.getShipment().hasShipped()) {
            event.getHook().sendMessage("You have already shipped this turn.").queue();
            return;
        }
        if (game.getTurnOrder().peekFirst().equals("guild-hold")) {
            event.getHook().sendMessage("Waiting on " + Emojis.GUILD + " to decide if they want to take their turn now.").queue();
            return;
        }
        if (game.getTurnOrder().peekFirst().equals("juice-of-sapho-hold")) {
            event.getHook().sendMessage("Juice of Sapho can still be played, please wait.").queue();
            return;
        }
        if (!faction.getName().equals(game.getTurnOrder().peekFirst())) {
            event.getHook().sendMessage("It is not your turn to ship.").queue();
            return;
        }
        int spice = faction.getShipment().getForce() + faction.getShipment().getSpecialForce();
        if (faction.getName().equals("Fremen")) spice = 0;
        if (faction.getShipment().isNoField()) spice = 1;
        spice *= game.getTerritory(faction.getShipment().getTerritoryName()).isStronghold() ? 1 : 2;
        if (faction.getName().equals("Guild")) spice = Math.ceilDiv(spice, 2);
        if (spice > faction.getSpice() + faction.getAllySpiceShipment()) {
            event.getHook().sendMessage("You cannot afford this shipment.").queue();
            return;
        }
        faction.getShipment().execute(discordGame, game, faction);
        event.getHook().sendMessage("Shipment complete.").queue();
        queueMovementButtons(game, faction, event, discordGame);
    }

    private static void resetShipmentMovement(ButtonInteractionEvent event, Game game, DiscordGame discordGame, boolean isShipment) throws ChannelNotFoundException {
        Faction faction = ButtonManager.getButtonPresser(event, game);
        if (isShipment){
            faction.getShipment().clear();
            queueShippingButtons(event, game, discordGame);
        } else {
            faction.getMovement().clear();
            queueMovementButtons(game, faction, event, discordGame);
        }
        discordGame.pushGame();
    }

    private static void resetForces(ButtonInteractionEvent event, Game game, DiscordGame discordGame, boolean isShipment) throws ChannelNotFoundException {
        Faction faction = ButtonManager.getButtonPresser(event, game);
        if (isShipment) {
            faction.getShipment().setForce(0);
            faction.getShipment().setSpecialForce(0);
            faction.getShipment().setNoField(false);
            queueForcesButtons(event, game, discordGame, faction, true);
        } else {
            faction.getMovement().setForce(0);
            faction.getMovement().setSpecialForce(0);
            faction.getMovement().setMovingNoField(false);
            queueForcesButtons(event, game, discordGame, faction, false);
        }
        discordGame.pushGame();
    }

    private static void addForces(ButtonInteractionEvent event, Game game, DiscordGame discordGame, boolean isShipment) throws ChannelNotFoundException {
        Faction faction = ButtonManager.getButtonPresser(event, game);
        if (isShipment) faction.getShipment().setForce((faction.getShipment().getForce() + Integer.parseInt(event.getComponentId().replace("add-force-shipment-", ""))));
        else faction.getMovement().setForce((faction.getMovement().getForce() + Integer.parseInt(event.getComponentId().replace("add-force-movement-", ""))));
        queueForcesButtons(event, game, discordGame, faction, isShipment);
        discordGame.pushGame();
    }

    private static void addSpecialForces(ButtonInteractionEvent event, Game game, DiscordGame discordGame, boolean isShipment) throws ChannelNotFoundException {
        Faction faction = ButtonManager.getButtonPresser(event, game);
        if (isShipment) faction.getShipment().setSpecialForce((faction.getShipment().getSpecialForce() + Integer.parseInt(event.getComponentId().replace("add-special-force-shipment-", ""))));
        else faction.getMovement().setSpecialForce((faction.getMovement().getSpecialForce() + Integer.parseInt(event.getComponentId().replace("add-special-force-movement-", ""))));
        queueForcesButtons(event, game, discordGame, faction, isShipment);
        discordGame.pushGame();
    }

    private static void filterBySector(ButtonInteractionEvent event, Game game, DiscordGame discordGame, boolean isShipment) throws ChannelNotFoundException {
        String shipmentOrMovement = isShipment ? "ship-" : "move-";
        Faction faction = ButtonManager.getButtonPresser(event, game);
        Territory territory = game.getTerritories().values().stream().filter(t -> t.getTerritoryName().contains(
                event.getComponentId().replace(shipmentOrMovement + "sector-", "").replace("-", " "))
        ).findFirst().orElseThrow();

        if (isShipment) faction.getShipment().setTerritoryName(territory.getTerritoryName());
        else faction.getMovement().setMovingTo(territory.getTerritoryName());

        queueForcesButtons(event, game, discordGame, faction, isShipment);
        discordGame.pushGame();
    }
    private static void richeseNoFieldShip(ButtonInteractionEvent event, Game game, DiscordGame discordGame) throws ChannelNotFoundException {
        Faction faction = game.getFaction("Richese");
        int spice = 1;
        if (!game.getTerritory(faction.getShipment().getTerritoryName()).isStronghold()) spice *= 2;

        WebhookMessageCreateAction<Message> message = event.getHook().sendMessage("The No-Field ship is ready to depart for Arrakis!" +
                "\n**You are shipping your " + event.getComponentId().replace("richese-no-field-ship-", "") + " no-field token to \n" +
                faction.getShipment().getTerritoryName() + "\nFor " + spice + " " + Emojis.SPICE + "\n\nYou have " + faction.getSpice() + " " + Emojis.SPICE + " to spend.**");
        message.addActionRow(Button.success("execute-shipment", "Confirm Shipment"), Button.danger("reset-shipping-forces", "Reset forces"), Button.danger("reset-shipment", "Start Over")).queue();
        faction.getShipment().setNoField(true);
        faction.getShipment().setForce(Integer.parseInt(event.getComponentId().replace("richese-no-field-ship-", "")));
        discordGame.pushGame();
    }

    private static void queueForcesButtons(ButtonInteractionEvent event, Game game, DiscordGame discordGame, Faction faction, boolean isShipment) throws ChannelNotFoundException {

        TreeSet<Button> forcesButtons = new TreeSet<>(getButtonComparator());
        String shipOrMove = isShipment ? "shipment" : "movement";
        int buttonLimitForces = isShipment ? faction.getReserves().getStrength() - faction.getShipment().getForce() :
                game.getTerritory(faction.getMovement().getMovingFrom()).getForce(faction.getName()).getStrength() - faction.getMovement().getForce();
        if (faction.getName().equals("BG") && !isShipment && game.getTerritory(faction.getMovement().getMovingFrom()).hasForce("Advisor")) buttonLimitForces = game.getTerritory(faction.getMovement().getMovingFrom()).getForce("Advisor").getStrength();
        int buttonLimitSpecialForces = isShipment ? faction.getSpecialReserves().getStrength() - faction.getShipment().getSpecialForce() :
                game.getTerritory(faction.getMovement().getMovingFrom()).getForce(faction.getName() + "*").getStrength() - faction.getMovement().getSpecialForce();

        if (faction.getShipment().isToReserves()) {
            if (event.getComponentId().startsWith("ship-to-reserves-")){
                faction.getShipment().setToReserves(true);
                faction.getShipment().setTerritoryName(event.getComponentId().replace("ship-to-reserves-", ""));
            }
            buttonLimitForces = game.getTerritory(faction.getShipment().getTerritoryName()).getForce("Guild").getStrength() - faction.getShipment().getForce();
        }

        if (!faction.getShipment().getCrossShipFrom().isEmpty()) {
            buttonLimitForces = game.getTerritory(faction.getShipment().getCrossShipFrom()).getForce("Guild").getStrength() - faction.getShipment().getForce();
        }

        for (int i = 0; i < buttonLimitForces; i++) {
            forcesButtons.add(Button.primary("add-force-" + shipOrMove + "-" + (i + 1), "+" + (i + 1) + " regular"));
        }
        for (int i = 0; i < buttonLimitSpecialForces; i++) {
            forcesButtons.add(Button.primary("add-special-force-" + shipOrMove + "-" + (i + 1), "+" + (i + 1) + " starred"));
        }

        if (isShipment) {
            int spice = faction.getShipment().getForce() + faction.getShipment().getSpecialForce();

            String territory = faction.getShipment().getTerritoryName();
            if (faction.getShipment().isToReserves()) territory = "reserves from " + territory;
            if (!faction.getShipment().getCrossShipFrom().isEmpty()) territory = territory + " cross shipping from " + faction.getShipment().getCrossShipFrom();

            if (!game.getTerritory(faction.getShipment().getTerritoryName()).isStronghold() && !faction.getShipment().isToReserves()) spice *= 2;

            if (faction.getName().equals("Fremen")) spice = 0;
            if (faction.getName().equals("Guild")) spice = Math.ceilDiv(spice, 2);
            String specialForces = faction.getSpecialReserves().getName().equals("") ? "" : "\n" + faction.getShipment().getSpecialForce() + " " + Emojis.getForceEmoji(faction.getName() + "*");
            WebhookMessageCreateAction<Message> message = event.getHook().sendMessage("Use buttons below to add forces to your shipment." +
                    "\n**Currently shipping:\n" + faction.getShipment().getForce() +  " " + Emojis.getForceEmoji(faction.getName())
                    + specialForces + "\n to " + territory + "\n for " + spice + " " + Emojis.SPICE + "\n\nYou have " + faction.getSpice() + " " + Emojis.SPICE + " to spend.**");

            if (!forcesButtons.isEmpty()) {
                arrangeButtonsAndSend(message, forcesButtons, discordGame, event);
            } else message.queue();
            if (faction.getName().equals("Richese")) {
                MessageCreateAction noFieldButtonMessage = discordGame.prepareMessage("richese-chat", "no-field options:");
                TreeSet<Button> noFieldButtons = new TreeSet<>(getButtonComparator());
                RicheseFaction richese = (RicheseFaction) faction;
                List<Integer> noFields = new LinkedList<>();
                noFields.add(0);
                noFields.add(3);
                noFields.add(5);
                noFields.removeIf(integer -> Objects.equals(integer, richese.getFrontOfShieldNoField()));
                for (int noField : noFields) {
                    if (richese.hasFrontOfShieldNoField() && richese.getFrontOfShieldNoField() == noField) continue;
                    noFieldButtons.add(Button.primary("richese-no-field-ship-" + noField, "Ship " + noField + " no-field token."));
                }
                noFieldButtonMessage.addActionRow(noFieldButtons).queue();
            }
            discordGame.prepareMessage(event.getMessageChannel().getName(), "Finalize or start over:")
                    .addActionRow(Button.success("execute-shipment", "Confirm Shipment"), Button.danger("reset-shipping-forces", "Reset forces"), Button.danger("reset-shipment", "Start Over")).queue();
        }
        else {
            if (faction.getName().equals("Richese") && game.getTerritories().get(faction.getMovement().getMovingFrom()).hasRicheseNoField() && !faction.getMovement().isMovingNoField()) forcesButtons.add(Button.primary("richese-no-field-move", "+1 no-field token"));
            String specialForces = faction.getSpecialReserves().getName().equals("") ? "" : "\n" + faction.getMovement().getSpecialForce() + " " + Emojis.getForceEmoji(faction.getName() + "*");
            String noField = faction.getMovement().isMovingNoField() ? "\n" + game.getTerritory(faction.getMovement().getMovingFrom()).getRicheseNoField() + " No-Field token" : "";
            WebhookMessageCreateAction<Message> message = event.getHook().sendMessage("Use buttons below to add forces to your movement." +
                    "\n**Currently moving:\n" + faction.getMovement().getForce() + " " + Emojis.getForceEmoji(faction.getName()) + specialForces + noField + "\n to " + faction.getMovement().getMovingTo() + "**");
            forcesButtons.add(Button.success("execute-movement", "Confirm Movement"));
            forcesButtons.add(Button.danger("reset-moving-forces", "Reset forces"));
            forcesButtons.add(Button.danger("reset-movement", "Start Over"));

            arrangeButtonsAndSend(message, forcesButtons, discordGame, event);
        }
        discordGame.pushGame();
    }
    private static void richeseNoFieldMove(ButtonInteractionEvent event, Game game, DiscordGame discordGame) throws ChannelNotFoundException {
        RicheseFaction richese = (RicheseFaction) game.getFaction("Richese");
        richese.getMovement().setMovingNoField(true);
        queueForcesButtons(event, game, discordGame, richese, false);
        discordGame.pushGame();
    }

    private static void queueSectorButtons(ButtonInteractionEvent event, Game game, DiscordGame discordGame, boolean isShipment) throws ChannelNotFoundException {
        String shipmentOrMovement = isShipment ? "ship-" : "move-";
        Faction faction = ButtonManager.getButtonPresser(event, game);
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
            if (sector.getSpice() > 0) buttons.add(Button.primary(shipmentOrMovement + "sector-" + sector.getTerritoryName(), sector.getSector() + " (spice sector)"));
            else buttons.add(Button.primary(shipmentOrMovement + "sector-" + sector.getTerritoryName(), String.valueOf(sector.getSector())));
        }
        buttons.sort(getButtonComparator());
        String backButtonId = isShipment ? "reset-shipment" : "reset-movement";
        event.getHook().sendMessage("Which sector?")
                .addActionRow(buttons)
                .addActionRow(Button.secondary(backButtonId, "back")).queue();
    }

    private static void queueOtherShippingButtons(ButtonInteractionEvent event) {
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

    private static void queueRockShippingButtons(ButtonInteractionEvent event) {
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

    private static void queueSpiceBlowShippingButtons(ButtonInteractionEvent event) {
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

    private static void queueStrongholdShippingButtons(ButtonInteractionEvent event, Game game) {
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

    private static void passShipment(ButtonInteractionEvent event, Game game, DiscordGame discordGame) throws ChannelNotFoundException {
        Faction faction = ButtonManager.getButtonPresser(event, game);
        if (faction.getShipment().hasShipped()) {
            event.getHook().sendMessage("You have already shipped this turn.").queue();
            return;
        }
        if (game.getTurnOrder().peekFirst().equals("guild-hold")) {
            event.getHook().sendMessage("Waiting on " + Emojis.GUILD + " to decide if they want to take their turn now.").queue();
            return;
        }
        if (game.getTurnOrder().peekFirst().equals("juice-of-sapho-hold")) {
            event.getHook().sendMessage("Juice of Sapho can still be played, please wait.").queue();
            return;
        }
        if (!faction.getName().equals(game.getTurnOrder().peekFirst())) {
            event.getHook().sendMessage("It is not your turn to ship.").queue();
            return;
        }
        discordGame.sendMessage("turn-summary", faction.getEmoji() + " does not ship.");
        faction.getShipment().clear();
        discordGame.pushGame();
        queueMovementButtons(game, faction, event, discordGame);
    }

    private static void queueShippingButtons(ButtonInteractionEvent event, Game game, DiscordGame discordGame) throws ChannelNotFoundException {
        Faction faction = ButtonManager.getButtonPresser(event, game);

        event.getHook().sendMessage("What part of Arrakis would you like to ship to?")
                .addActionRow(Button.primary("stronghold", "Stronghold"),
                        Button.primary("spice-blow", "Spice Blow Territories"),
                        Button.primary("rock", "Rock Territories"),
                        Button.primary("other", "Somewhere else"),
                        Button.danger("pass-shipment", "I don't want to ship.")).complete();

        if (faction.getName().equals("Guild") && faction.getShipment().getCrossShipFrom().isEmpty()) {
            discordGame.prepareMessage("guild-chat", "Special options for " + Emojis.GUILD + ":")
                    .addActionRow(
                            Button.primary("guild-cross-ship", "Cross ship"),
                            Button.primary("guild-ship-to-reserves", "Ship to reserves")
                            ).complete();
        }

        if (faction.hasAlly()) {
            discordGame.prepareMessage(faction.getAlly().toLowerCase() + "-chat", "Use buttons below to support your ally's shipment")
                    .addActionRow(Button.primary("support-max", "Support ally (no limits)"),
                            Button.primary("support-number", "Support ally (specific amount)")).queue();
        }
    }

    private static void queueMovementButtons(Game game, Faction faction, ButtonInteractionEvent event, DiscordGame discordGame) throws ChannelNotFoundException {
        WebhookMessageCreateAction<Message> message = event.getHook().sendMessage("Use the following buttons to perform your move.");

        TreeSet<Button> movingFromButtons = new TreeSet<>(Comparator.comparing(Button::getLabel));

        for (Territory territory : game.getTerritories().values()){
            if (territory.getForces().stream().anyMatch(force -> force.getFactionName().equals(faction.getName()))
                    || (faction.getName().equals("Richese") && territory.hasRicheseNoField())
                    || (faction.getName().equals("BG") && territory.hasForce("Advisor"))) movingFromButtons.add(Button.primary("moving-from-" + territory.getTerritoryName(), territory.getTerritoryName()));
        }
        movingFromButtons.add(Button.danger("pass-movement", "No move"));

        arrangeButtonsAndSend(message, movingFromButtons, discordGame, event);
    }

    private static void arrangeButtonsAndSend(WebhookMessageCreateAction<Message> message, TreeSet<Button> buttons, DiscordGame discordGame, ButtonInteractionEvent event) throws ChannelNotFoundException {
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
        for (MessageCreateRequest item : list) {
            if (item instanceof MessageCreateAction) {
                MessageCreateAction messageCreateAction = (MessageCreateAction) item;
                messageCreateAction.queue();
            }
            else {
                WebhookMessageCreateAction<Message> messageCreateAction = (WebhookMessageCreateAction<Message>) item;
                messageCreateAction.queue();
            }
        }
    }
    public static void sendShipmentMessage(String faction, DiscordGame discordGame, Game game) throws ChannelNotFoundException {
        discordGame.prepareMessage(faction.toLowerCase() + "-chat", "Use buttons to perform Shipment and Movement actions on your turn." + " " + game.getFaction(faction).getPlayer())
                .addActionRow(Button.primary( "shipment", "Begin a ship action"))
                .addActionRow(Button.danger( "pass-shipment", "Pass shipment")).queue();
    }

    public static void queueGuildTurnOrderButtons(DiscordGame discordGame) throws ChannelNotFoundException {
        discordGame.prepareMessage("guild-chat", "Use buttons to take your turn out of order.")
                .addActionRow(Button.primary("guild-take-turn", "Take turn next."),
                        Button.primary("guild-defer", "Defer turn."),
                        Button.primary("guild-wait-last", "Take turn last.")).queue();
    }

    public static Comparator<Button> getButtonComparator() {
        return (o1, o2) -> {
            String labelo1 = o1.getLabel();
            String labelo2 = o2.getLabel();
            labelo1 = labelo1.replaceAll("[^0-9]", "").isEmpty() ? "999" : labelo1.replaceAll("[^0-9]", "");
            labelo2 = labelo2.replaceAll("[^0-9]", "").isEmpty() ? "999" : labelo2.replaceAll("[^0-9]", "");
            int o1int = Integer.parseInt(labelo1);
            int o2int = Integer.parseInt(labelo2);
            if (o1int - o2int == 0) return 1;
            return o1int - o2int;
        };
    }
}