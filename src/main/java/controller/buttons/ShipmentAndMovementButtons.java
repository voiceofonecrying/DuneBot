package controller.buttons;

import constants.Emojis;
import controller.channels.TurnSummary;
import enums.UpdateType;
import exceptions.ChannelNotFoundException;
import exceptions.InvalidOptionException;
import model.DiscordGame;
import model.Game;
import model.Movement;
import model.Territory;
import model.factions.Faction;
import model.factions.RicheseFaction;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;

import java.io.IOException;
import java.util.*;

import static controller.buttons.ButtonManager.deleteAllButtonsInChannel;

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
        else if (event.getComponentId().startsWith("planetologist-add-force-"))
            planetologistAddForces(event, game, discordGame);
        else if (event.getComponentId().startsWith("planetologist-add-special-force-"))
            planetologistAddSpecialForces(event, game, discordGame);
        else if (event.getComponentId().startsWith("moving-from-")) queueMovableTerritories(event, game, discordGame, false);
        else if (event.getComponentId().startsWith("ornithopter-moving-from-")) ornithopterMovement(event, game, discordGame);
        else if (event.getComponentId().startsWith("move-sector-")) filterBySector(event, game, discordGame, false);
        else if (event.getComponentId().startsWith("move-")) queueSectorButtons(event, game, discordGame, false);
        else if (event.getComponentId().startsWith("richese-no-field-ship-"))
            richeseNoFieldShip(event, game, discordGame);
        else if (event.getComponentId().startsWith("support-")) supportAlly(event, game, discordGame);
        switch (event.getComponentId()) {
            case "shipment" -> queueShippingButtons(event, game, discordGame);
            case "pass-shipment" -> passShipment(event, game, discordGame);
            case "pass-movement" -> passMovement(event, game, discordGame);
            case "stronghold" -> queueStrongholdShippingButtons(game, discordGame);
            case "spice-blow" -> queueSpiceBlowShippingButtons(discordGame);
            case "rock" -> queueRockShippingButtons(discordGame);
            case "other" -> queueOtherShippingButtons(discordGame);
            case "reset-shipping-forces" -> resetForces(event, game, discordGame, true);
            case "reset-shipment" -> resetShipmentMovement(event, game, discordGame, true);
            case "execute-shipment" -> executeShipment(event, game, discordGame, false);
            case "karama-execute-shipment" -> karamaExecuteShipment(event, game, discordGame);
            case "reset-moving-forces" -> resetForces(event, game, discordGame, false);
            case "reset-movement" -> resetShipmentMovement(event, game, discordGame, false);
            case "execute-movement" -> executeMovement(event, game, discordGame);
            case "juice-of-sapho-first" -> playJuiceOfSapho(event, game, discordGame, false);
            case "juice-of-sapho-last" -> playJuiceOfSapho(event, game, discordGame, true);
            case "juice-of-sapho-don't-play" -> juiceOfSaphoDontPlay(event, game, discordGame);
            case "guild-take-turn" -> guildTakeTurn(game, discordGame);
            case "guild-wait-last" -> guildWaitLast(game, discordGame);
            case "guild-defer" -> guildDefer(game, discordGame);
            case "richese-no-field-move" -> richeseNoFieldMove(event, game, discordGame);
            case "guild-cross-ship" -> crossShip(game, discordGame);
            case "guild-ship-to-reserves" -> shipToReserves(game, discordGame);
            case "hajr" -> hajr(event, game, discordGame, true);
            case "Ornithopter" -> hajr(event, game, discordGame, false);
        }

    }

    private static void karamaExecuteShipment(ButtonInteractionEvent event, Game game, DiscordGame discordGame) throws ChannelNotFoundException, IOException {
        Faction faction = ButtonManager.getButtonPresser(event, game);
        game.getTreacheryDiscard().add(faction.removeTreacheryCard("Karama"));
        discordGame.getTurnSummary().queueMessage(faction.getEmoji() + " discards Karama to ship at " + Emojis.GUILD + " rates.");
        executeShipment(event, game, discordGame, true);
    }

    private static void ornithopterMovement(ButtonInteractionEvent event, Game game, DiscordGame discordGame) throws ChannelNotFoundException {
        Faction faction = ButtonManager.getButtonPresser(event, game);
        game.getTreacheryDiscard().add(faction.removeTreacheryCard("Ornithopter"));
        discordGame.getTurnSummary().queueMessage(faction.getEmoji() + " use Ornithopter to move 3 spaces with their move.");
        queueMovableTerritories(event, game, discordGame, true);
        discordGame.pushGame();
    }

    private static void planetologistAddSpecialForces(ButtonInteractionEvent event, Game game, DiscordGame discordGame) throws ChannelNotFoundException {
        Faction faction = ButtonManager.getButtonPresser(event, game);
        if (!event.getComponentId().split("-")[4].equals(faction.getMovement().getSecondMovingFrom())) {
            faction.getMovement().setSecondSpecialForce(0);
            faction.getMovement().setSecondForce(0);
        }
        faction.getMovement().setSecondMovingFrom(event.getComponentId().split("-")[4]);
        faction.getMovement().setSecondSpecialForce(faction.getMovement().getSecondSpecialForce() + Integer.parseInt(event.getComponentId().split("-")[5]));
        queueForcesButtons(event, game, discordGame, faction, false);

    }

    private static void planetologistAddForces(ButtonInteractionEvent event, Game game, DiscordGame discordGame) throws ChannelNotFoundException {
        Faction faction = ButtonManager.getButtonPresser(event, game);
        if (!event.getComponentId().split("-")[3].equals(faction.getMovement().getSecondMovingFrom())) {
            faction.getMovement().setSecondSpecialForce(0);
            faction.getMovement().setSecondForce(0);
        }
        faction.getMovement().setSecondMovingFrom(event.getComponentId().split("-")[3]);
        faction.getMovement().setSecondForce(faction.getMovement().getSecondForce() + Integer.parseInt(event.getComponentId().split("-")[4]));
        queueForcesButtons(event, game, discordGame, faction, false);


    }

    private static void hajr(ButtonInteractionEvent event, Game game, DiscordGame discordGame, boolean hajr) throws ChannelNotFoundException, InvalidOptionException, IOException {
        Faction faction = ButtonManager.getButtonPresser(event, game);
        if (hajr) game.getTreacheryDiscard().add(faction.removeTreacheryCard("Hajr "));
        else game.getTreacheryDiscard().add(faction.removeTreacheryCard("Ornithopter"));
        String hajrOrOrnithopter = hajr ? "Hajr" : "Ornithopter";
        discordGame.getTurnSummary().queueMessage(faction.getEmoji() + " discards " + hajrOrOrnithopter + " to move again.");
        faction.getMovement().execute(discordGame, game, faction);
        deleteAllButtonsInChannel(event.getMessageChannel());
        queueMovementButtons(game, faction, discordGame);
        faction.setUpdated(UpdateType.MISC_BACK_OF_SHIELD);
        discordGame.pushGame();
    }

    private static void setCrossShipFrom(ButtonInteractionEvent event, Game game, DiscordGame discordGame) throws ChannelNotFoundException {
        Faction faction = ButtonManager.getButtonPresser(event, game);
        String from = game.getTerritory(event.getComponentId().replace("cross-ship-from-", "")).getTerritoryName();
        faction.getShipment().setCrossShipFrom(from);
        queueShippingButtons(event, game, discordGame);
        game.getFaction("Guild").getShipment().setToReserves(false);
        discordGame.pushGame();
    }

    private static void shipToReserves(Game game, DiscordGame discordGame) throws ChannelNotFoundException {
        TreeSet<Button> buttons = new TreeSet<>(getButtonComparator());
        for (Territory territory : game.getTerritories().values()) {
            if (territory.hasForce("Guild")) buttons.add(Button.primary("ship-to-reserves-" + territory.getTerritoryName(), territory.getTerritoryName()));
        }
        arrangeButtonsAndSend("Where would you like to ship to reserves from?", buttons, discordGame);
        game.getFaction("Guild").getShipment().setToReserves(true);
        discordGame.pushGame();
    }

    private static void crossShip(Game game, DiscordGame discordGame) throws ChannelNotFoundException {
        TreeSet<Button> buttons = new TreeSet<>(getButtonComparator());
        for (Territory territory : game.getTerritories().values()) {
            if (territory.hasForce("Guild")) buttons.add(Button.primary("cross-ship-from-" + territory.getTerritoryName(), territory.getTerritoryName()));
        }
        arrangeButtonsAndSend("Where would you like to ship from?", buttons, discordGame);
        game.getFaction("Guild").getShipment().setToReserves(true);
        discordGame.pushGame();
    }

    private static void supportAlly(ButtonInteractionEvent event, Game game, DiscordGame discordGame) throws ChannelNotFoundException {
        Faction faction = ButtonManager.getButtonPresser(event, game);
        if (event.getComponentId().equals("support-max")) {
            game.getFaction(faction.getAlly()).setAllySpiceShipment(faction.getSpice());
            discordGame.getFactionChat(faction.getAlly()).queueMessage("Your ally will support your shipment this turn up to " + game.getFaction(faction.getAlly()).getAllySpiceShipment() + " " + Emojis.SPICE + "!");
            discordGame.queueMessage("You have offered your ally all of your spice to ship with.");
            discordGame.pushGame();
        } else if (event.getComponentId().equals("support-number")){
            TreeSet<Button> buttonList = new TreeSet<>(getButtonComparator());
            int limit = Math.min(faction.getSpice(), 40);
            for (int i = 0; i < limit; i++) {
                buttonList.add(Button.primary("support-" + (i + 1), Integer.toString(i + 1)));
            }
            arrangeButtonsAndSend("How much would you like to offer in support?", buttonList, discordGame);
        } else {
            game.getFaction(faction.getAlly()).setAllySpiceShipment(Integer.parseInt(event.getComponentId().replace("support-", "")));
            discordGame.getFactionChat(faction.getAlly()).queueMessage("Your ally will support your shipment this turn up to " + game.getFaction(faction.getAlly()).getAllySpiceShipment() + " " + Emojis.SPICE + "!");
            discordGame.queueMessage("You have offered your ally " + event.getComponentId().replace("support-", "") + " " + Emojis.SPICE + " to ship with.");
            discordGame.pushGame();
        }
    }


    private static void guildDefer(Game game, DiscordGame discordGame) throws ChannelNotFoundException {
        game.getTurnOrder().pollFirst();
        discordGame.queueMessage("You will defer this turn.");
        discordGame.getTurnSummary().queueMessage(Emojis.GUILD + " does not ship at this time.");
        sendShipmentMessage(game.getTurnOrder().peekFirst(), discordGame, game);
        discordGame.pushGame();
        discordGame.queueDeleteMessage();
    }

    private static void guildWaitLast(Game game, DiscordGame discordGame) throws ChannelNotFoundException {
        game.getTurnOrder().addLast("Guild");
        discordGame.queueMessage("You will take your turn last.");
        game.getTurnOrder().pollFirst();
        sendShipmentMessage(game.getTurnOrder().peekFirst(), discordGame, game);
        discordGame.pushGame();
        discordGame.queueDeleteMessage();
    }

    private static void guildTakeTurn(Game game, DiscordGame discordGame) throws ChannelNotFoundException {
        discordGame.queueMessage("You will take your turn now.");
        sendShipmentMessage("Guild", discordGame, game);
        discordGame.getTurnSummary().queueMessage(Emojis.GUILD + " will take their turn next.");
        discordGame.queueDeleteMessage();
        discordGame.pushGame();
    }
    private static void juiceOfSaphoDontPlay(ButtonInteractionEvent event, Game game, DiscordGame discordGame) throws ChannelNotFoundException {
        Faction faction = ButtonManager.getButtonPresser(event, game);
        if (faction.getTreacheryHand().stream().noneMatch(treacheryCard -> treacheryCard.name().equals("Juice of Sapho"))) {
            discordGame.queueMessageToEphemeral("These buttons were not intended for you");
            return;
        }
        discordGame.getTurnSummary().queueMessage("Juice of Sapho is not played at this time.");
        discordGame.queueMessage("You will not play Juice of Sapho this turn.");
        game.getTurnOrder().pollFirst();
        if (game.hasFaction("Guild")) {
            game.getTurnOrder().addFirst("Guild");
            queueGuildTurnOrderButtons(discordGame, game);
        }
        else sendShipmentMessage(game.getTurnOrder().peekFirst(), discordGame, game);
        discordGame.pushGame();
        discordGame.queueDeleteMessage();
    }

    private static void playJuiceOfSapho(ButtonInteractionEvent event, Game game, DiscordGame discordGame, boolean last) throws ChannelNotFoundException {
        Faction faction = ButtonManager.getButtonPresser(event, game);
        if (faction.getTreacheryHand().stream().noneMatch(treacheryCard -> treacheryCard.name().equals("Juice of Sapho"))) {
            discordGame.queueMessageToEphemeral("These buttons were not intended for you");
            return;
        }
        game.getTurnOrder().pollFirst();
        String lastFirst = last ? "last" : "first";
        game.getTurnOrder().remove(faction.getName());
        if (last) {
            game.getTurnOrder().addLast(faction.getName());
            game.getTurnOrder().addLast("juice-of-sapho-last");

        }
        else {
            game.getTurnOrder().addFirst(faction.getName());
        }
        game.getTreacheryDiscard().add(faction.removeTreacheryCard("Juice of Sapho"));
        discordGame.queueMessageToEphemeral("Button pressed.  You will go " + lastFirst + " this turn.");
        discordGame.getTurnSummary().queueMessage(faction.getEmoji() + " plays Juice of Sapho to ship and move " + lastFirst + " this turn.");
        if (last && game.hasFaction("Guild") && !faction.getName().equals("Guild")) {
            game.getTurnOrder().addFirst("Guild");
            queueGuildTurnOrderButtons(discordGame, game);
        }
        else sendShipmentMessage(game.getTurnOrder().peekFirst(), discordGame, game);
        discordGame.pushGame();
        discordGame.queueDeleteMessage();
    }

    private static void passMovement(ButtonInteractionEvent event, Game game, DiscordGame discordGame) throws ChannelNotFoundException {
        Faction faction = ButtonManager.getButtonPresser(event, game);
        discordGame.queueMessage("Shipment and movement complete.");
        TurnSummary turnSummary = discordGame.getTurnSummary();
        turnSummary.queueMessage(faction.getEmoji() + " does not move.");
        game.getTurnOrder().pollFirst();
        if (game.hasFaction("Guild") && !game.getFaction("Guild").getShipment().hasShipped() && !game.getTurnOrder().contains("Guild")) {
            game.getTurnOrder().addFirst("Guild");
            queueGuildTurnOrderButtons(discordGame, game);
        }
        else if (!game.getTurnOrder().isEmpty()){
            sendShipmentMessage(game.getTurnOrder().peekFirst(), discordGame, game);
        } else {
            discordGame.queueMessage("mod-info", "Everyone has taken their turn, please run advance.");
            discordGame.pushGame();
            return;
        }
        if (game.getTurnOrder().size() > 1 && game.getTurnOrder().peekLast().equals("Guild")) {
            turnSummary.queueMessage(Emojis.GUILD + " does not ship at this time");
        }

        discordGame.pushGame();
        deleteAllButtonsInChannel(event.getMessageChannel());
    }

    private static void executeMovement(ButtonInteractionEvent event, Game game, DiscordGame discordGame) throws ChannelNotFoundException, InvalidOptionException, IOException {
        Faction faction = ButtonManager.getButtonPresser(event, game);
        Movement movement = faction.getMovement();
        movement.execute(discordGame, game, faction);
        game.getTurnOrder().pollFirst();
        if (game.hasFaction("Guild") && !game.getFaction("Guild").getShipment().hasShipped() && !game.getTurnOrder().contains("Guild")) {
            game.getTurnOrder().addFirst("Guild");
            queueGuildTurnOrderButtons(discordGame, game);
        }
        else if (!game.getTurnOrder().isEmpty()){
            sendShipmentMessage(game.getTurnOrder().peekFirst(), discordGame, game);
        } else {
            discordGame.queueMessage("mod-info", "Everyone has taken their turn, please run advance.");
            discordGame.pushGame();
            return;
        }
        if (!game.getTurnOrder().isEmpty() && game.getTurnOrder().peekLast().equals("Guild") && game.getTurnOrder().size() > 1) {
            discordGame.getTurnSummary().queueMessage(Emojis.GUILD + " does not ship at this time");
        }
        deleteAllButtonsInChannel(event.getMessageChannel());
        discordGame.queueMessage("Shipment and movement complete.");
        discordGame.pushGame();
    }

    private static void queueMovableTerritories(ButtonInteractionEvent event, Game game, DiscordGame discordGame, boolean ornithopter) throws ChannelNotFoundException {
        Faction faction = ButtonManager.getButtonPresser(event, game);
        Territory from = game.getTerritory(event.getComponentId().replace("moving-from-", "").replace("ornithopter-", ""));
        faction.getMovement().setMovingFrom(from.getTerritoryName());
        int spacesCanMove = 1;
        if (faction.getName().equals("Fremen") || (faction.getName().equals("Ix") && !Objects.equals(from.getForce(faction.getSpecialReserves().getName()).getName(), ""))) spacesCanMove = 2;
        if (ornithopter || game.getTerritory("Arrakeen").getActiveFactions(game).stream().anyMatch(f -> f.getName().equals(faction.getName())) ||
                game.getTerritory("Carthag").getActiveFactions(game).stream().anyMatch(f -> f.getName().equals(faction.getName()))) spacesCanMove = 3;
        if (!faction.getSkilledLeaders().isEmpty() && faction.getSkilledLeaders().get(0).skillCard().name().equals("Planetologist") && spacesCanMove < 3) spacesCanMove++;
        Set<String> moveableTerritories = getAdjacentTerritoryNames(from.getTerritoryName().replaceAll("\\(.*\\)", "").strip(), spacesCanMove, game);
        TreeSet<Button> moveToButtons = new TreeSet<>(Comparator.comparing(Button::getLabel));

        for (String territory : moveableTerritories) {
            moveToButtons.add(Button.primary("move-" + territory, territory));
        }

        arrangeButtonsAndSend("Where will your forces move to?", moveToButtons, discordGame);

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

    private static void executeShipment(ButtonInteractionEvent event, Game game, DiscordGame discordGame, boolean karama) throws ChannelNotFoundException, IOException {
        Faction faction = ButtonManager.getButtonPresser(event, game);
        int spice = faction.getShipment().getForce() + faction.getShipment().getSpecialForce();
        if (faction.getName().equals("Fremen")) spice = 0;
        if (faction.getShipment().isNoField()) spice = 1;
        spice *= game.getTerritory(faction.getShipment().getTerritoryName()).isStronghold() ? 1 : 2;
        if (faction.getName().equals("Guild") || (faction.hasAlly() && faction.getAlly().equals("Guild")) || karama) spice = Math.ceilDiv(spice, 2);
        if (spice > faction.getSpice() + faction.getAllySpiceShipment()) {
            discordGame.queueMessage("You cannot afford this shipment.");
            return;
        }
        faction.getShipment().execute(discordGame, game, faction, karama);
        discordGame.queueMessage("Shipment complete.");
        deleteAllButtonsInChannel(event.getMessageChannel());
        queueMovementButtons(game, faction, discordGame);
    }

    private static void resetShipmentMovement(ButtonInteractionEvent event, Game game, DiscordGame discordGame, boolean isShipment) throws ChannelNotFoundException {
        Faction faction = ButtonManager.getButtonPresser(event, game);
        if (isShipment){
            faction.getShipment().clear();
            faction.getShipment().setShipped(false);
            queueShippingButtons(event, game, discordGame);
        } else {
            faction.getMovement().clear();
            faction.getMovement().setMoved(false);
            queueMovementButtons(game, faction, discordGame);
        }
        discordGame.queueDeleteMessage();
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
            faction.getMovement().setSecondForce(0);
            faction.getMovement().setSecondSpecialForce(0);
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
        MessageCreateBuilder messageCreateBuilder = new MessageCreateBuilder()
                .addContent(
                        "The No-Field ship is ready to depart for Arrakis!" +
                                "\n**You are shipping your " + event.getComponentId().replace("richese-no-field-ship-", "") + " no-field token to \n" +
                                faction.getShipment().getTerritoryName() + "\nFor " + spice + " " + Emojis.SPICE + "\n\nYou have " + faction.getSpice() + " " + Emojis.SPICE + " to spend.**"
                ).addActionRow(
                        Button.success("execute-shipment", "Confirm Shipment"),
                        Button.danger("reset-shipping-forces", "Reset forces"),
                        Button.danger("reset-shipment", "Start Over")
                );

        discordGame.queueMessage(messageCreateBuilder);
        faction.getShipment().setNoField(true);
        faction.getShipment().setForce(Integer.parseInt(event.getComponentId().replace("richese-no-field-ship-", "")));
        discordGame.pushGame();
    }

    private static void queueForcesButtons(ButtonInteractionEvent event, Game game, DiscordGame discordGame, Faction faction, boolean isShipment) throws ChannelNotFoundException {

        TreeSet<Button> forcesButtons = new TreeSet<>(getButtonComparator());
        String shipOrMove = isShipment ? "shipment" : "movement";
        int buttonLimitForces = isShipment ? faction.getReserves().getStrength() - faction.getShipment().getForce() :
                game.getTerritory(faction.getMovement().getMovingFrom()).getForce(faction.getName()).getStrength() - faction.getMovement().getForce();
        if (faction.getName().equals("BG") && !isShipment && game.getTerritory(faction.getMovement().getMovingFrom()).hasForce("Advisor"))
            buttonLimitForces = game.getTerritory(faction.getMovement().getMovingFrom()).getForce("Advisor").getStrength();
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
            if (faction.getName().equals("Guild") || (faction.hasAlly() && faction.getAlly().equals("Guild"))) spice = Math.ceilDiv(spice, 2);
            String specialForces = faction.getSpecialReserves().getName().equals("") ? "" : "\n" + faction.getShipment().getSpecialForce() + " " + Emojis.getForceEmoji(faction.getName() + "*");

            String message = "Use buttons below to add forces to your shipment." +
                    "\n**Currently shipping:\n" + faction.getShipment().getForce() +  " " + Emojis.getForceEmoji(faction.getName())
                    + specialForces + "\n to " + territory + "\n for " + spice + " " + Emojis.SPICE + "\n\nYou have " +
                    faction.getSpice() + " " + Emojis.SPICE + " to spend.**";
            if (!forcesButtons.isEmpty()) {
                arrangeButtonsAndSend(message, forcesButtons, discordGame);
            } else {
                discordGame.queueMessage(message);
            };

            if (faction.getName().equals("Richese")) {
                MessageCreateBuilder noFieldButtonMessage = new MessageCreateBuilder()
                        .setContent("no-field options:");
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
                noFieldButtonMessage.addActionRow(noFieldButtons);
                discordGame.getRicheseChat().queueMessage(noFieldButtonMessage);
            }
            List<Button> finalizeButtons = new LinkedList<>();

            Button execute = Button.success("execute-shipment", "Confirm Shipment");


            if (faction.getShipment().hasShipped()) execute = execute.asDisabled();
            finalizeButtons.add(execute);
            finalizeButtons.add(Button.danger("reset-shipping-forces", "Reset forces"));
            finalizeButtons.add(Button.danger("reset-shipment", "Start Over"));
            if (faction.getTreacheryHand().stream().anyMatch(treacheryCard -> treacheryCard.name().equals("Karama"))) finalizeButtons.add(Button.secondary("karama-execute-shipment", "Confirm Shipment (Use Karama for Guild rates)"));

            discordGame.queueMessage(
                    event.getMessageChannel().getName(), new MessageCreateBuilder()
                            .setContent("Finalize or start over:")
                            .addActionRow(finalizeButtons));
        }
        else {
            if (faction.getName().equals("Richese") && game.getTerritories().get(faction.getMovement().getMovingFrom()).hasRicheseNoField() && !faction.getMovement().isMovingNoField()) forcesButtons.add(Button.primary("richese-no-field-move", "+1 no-field token"));
            String specialForces = faction.getSpecialReserves().getName().equals("") ? "" : "\n" + faction.getMovement().getSpecialForce() + " " + Emojis.getForceEmoji(faction.getName() + "*");
            String noField = faction.getMovement().isMovingNoField() ? "\n" + game.getTerritory(faction.getMovement().getMovingFrom()).getRicheseNoField() + " No-Field token" : "";

            String message = "Use buttons below to add forces to your movement." +
                    "\n**Currently moving:\n" + faction.getMovement().getForce() + " " + Emojis.getForceEmoji(faction.getName()) + specialForces + noField + "\n to " + faction.getMovement().getMovingTo() + "**";
            if (faction.getTreacheryHand().stream().anyMatch(treacheryCard -> treacheryCard.name().equals("Hajr "))) forcesButtons.add(Button.secondary("hajr", "Confirm Movement and play Hajr"));
            if (faction.getTreacheryHand().stream().anyMatch(treacheryCard -> treacheryCard.name().equals("Ornithopter"))) forcesButtons.add(Button.secondary("Ornithopter", "Confirm Movement and play Ornithopter"));

            forcesButtons.add(Button.success("execute-movement", "Confirm Movement"));
            forcesButtons.add(Button.danger("reset-moving-forces", "Reset forces"));
            forcesButtons.add(Button.danger("reset-movement", "Start Over"));

            arrangeButtonsAndSend(message, forcesButtons, discordGame);

            if (!faction.getSkilledLeaders().isEmpty() && faction.getSkilledLeaders().get(0).skillCard().name().equals("Planetologist")) {
                int spacesCanMove = 1;
                if (faction.getName().equals("Fremen")) spacesCanMove = 2;
                if (game.getTerritory("Arrakeen").getActiveFactions(game).stream().anyMatch(f -> f.getName().equals(faction.getName())) ||
                        game.getTerritory("Carthag").getActiveFactions(game).stream().anyMatch(f -> f.getName().equals(faction.getName()))) spacesCanMove = 3;
                for (Territory territory : game.getTerritories().values()) {
                    if (territory.getTerritoryName().equals(faction.getMovement().getMovingFrom())) continue;
                    if (!territory.hasForce(faction.getName()) && !territory.hasForce(faction.getName() + "*")) continue;

                    Set<String> territoriesInRange = getAdjacentTerritoryNames(territory.getTerritoryName().replaceAll("\\(.*\\)", "").strip(), spacesCanMove, game);
                    if (!territoriesInRange.contains(faction.getMovement().getMovingTo().replaceAll("\\(.*\\)", "").strip())) continue;

                    TreeSet<Button> secondForcesButtons = new TreeSet<>(getButtonComparator());
                    int secondForcesButtonLimit = territory.getForce(faction.getName()).getStrength();
                    if (faction.getName().equals("BG")  && game.getTerritory(faction.getMovement().getMovingFrom()).hasForce("Advisor"))
                        secondForcesButtonLimit = territory.getForce("Advisor").getStrength();
                    int secondSpecialForcesButtonLimit = territory.getForce(faction.getName() + "*").getStrength();

                    if (territory.getTerritoryName().equals(faction.getMovement().getSecondMovingFrom())) {
                        secondForcesButtonLimit -= faction.getMovement().getSecondForce();
                        secondSpecialForcesButtonLimit -= faction.getMovement().getSecondSpecialForce();
                    }

                    for (int i = 0; i < secondForcesButtonLimit; i++) {
                        secondForcesButtons.add(Button.primary("planetologist-add-force-" + territory.getTerritoryName() + "-" + (i + 1), "+" + (i + 1) + " regular"));
                    }
                    for (int i = 0; i < secondSpecialForcesButtonLimit; i++) {
                        secondForcesButtons.add(Button.primary("planetologist-add-special-force-" + territory.getTerritoryName() + "-" + (i + 1), "+" + (i + 1) + " starred"));
                    }

                    arrangeButtonsAndSend("Second force that can move from " + territory.getTerritoryName() + " using Planetologist ability.", secondForcesButtons, discordGame);

                }
                String secondSpecialForcesMessage = Emojis.getForceEmoji(faction.getName() + "*").equals(" force ") ? "" : "\n" + faction.getMovement().getSecondSpecialForce() + " " + Emojis.getForceEmoji(faction.getName() + "*");
                String planetologistMessage =
                        "**Currently moving (Planetologist):\n" + faction.getMovement().getSecondForce() + " " + Emojis.getForceEmoji(faction.getName()) + secondSpecialForcesMessage + "\n from " + faction.getMovement().getSecondMovingFrom() + "**";
                if (!faction.getMovement().getSecondMovingFrom().equals("")) {
                    discordGame.getFactionChat(faction.getName()).queueMessage(planetologistMessage);
                }
            }
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

        discordGame.queueMessage(new MessageCreateBuilder()
                .setContent("Which sector?")
                .addActionRow(buttons)
                .addActionRow(Button.secondary(backButtonId, "back"))
        );
    }

    private static void queueOtherShippingButtons(DiscordGame discordGame) {
        MessageCreateBuilder message = new MessageCreateBuilder()
                .setContent("Which territory?")
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
                        Button.danger("pass-shipment", "Pass Shipment"));
        discordGame.queueMessage(message);
        discordGame.queueDeleteMessage();
    }

    private static void queueRockShippingButtons(DiscordGame discordGame) {
        MessageCreateBuilder message = new MessageCreateBuilder()
                .setContent("Which rock territory?")
                .addActionRow(Button.primary("ship-false-wall-south", "False Wall South"),
                        Button.primary("ship-pasty-mesa", "Pasty Mesa"),
                        Button.primary("ship-false-wall-east", "False Wall East"),
                        Button.primary("ship-shield-wall", "Shield Wall"),
                        Button.primary("ship-rim-wall-west", "Rim Wall West"))
                .addActionRow(
                        Button.primary("ship-plastic-basin", "Plastic Basin"),
                        Button.primary("ship-false-wall-west", "False Wall West"),
                        Button.secondary("reset-shipment", "back"),
                        Button.danger("pass-shipment", "Pass Shipment"));

        discordGame.queueMessage(message);
        discordGame.queueDeleteMessage();
    }

    private static void queueSpiceBlowShippingButtons(DiscordGame discordGame) {
        MessageCreateBuilder message = new MessageCreateBuilder()
                .setContent("Which spice blow territory?")
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
                        Button.danger("pass-shipment", "Pass Shipment"));

        discordGame.queueMessage(message);
        discordGame.queueDeleteMessage();
    }

    private static void queueStrongholdShippingButtons(Game game, DiscordGame discordGame) {
        MessageCreateBuilder message = new MessageCreateBuilder().setContent("Which stronghold?");
        List<Button> strongholds = List.of(Button.primary("ship-arrakeen", "Arrakeen"),
                        Button.primary("ship-carthag", "Carthag"),
                        Button.primary("ship-sietch-tabr", "Sietch Tabr"),
                        Button.primary("ship-tuek's-sietch", "Tuek's Sietch"),
                        Button.primary("ship-habbanya-sietch", "Habbanya Sietch"));
        for (Button stronghold : strongholds) {
            if (game.getTerritory(stronghold.getLabel()).isAftermathToken()) stronghold = stronghold.asDisabled();
        }
        message.addActionRow(strongholds);

        if (game.hasFaction("Ix")) message.addActionRow(Button.primary("ship-hidden-mobile-stronghold", "Hidden Mobile Stronghold"));
        message.addActionRow(Button.secondary("reset-shipment", "back"),
                Button.danger("pass-shipment", "Pass Shipment"));

        discordGame.queueMessage(message);
        discordGame.queueDeleteMessage();
    }

    private static void passShipment(ButtonInteractionEvent event, Game game, DiscordGame discordGame) throws ChannelNotFoundException {
        Faction faction = ButtonManager.getButtonPresser(event, game);
        discordGame.getTurnSummary().queueMessage(faction.getEmoji() + " does not ship.");
        faction.getShipment().clear();
        discordGame.pushGame();
        deleteAllButtonsInChannel(event.getMessageChannel());
        queueMovementButtons(game, faction, discordGame);
    }

    private static void queueShippingButtons(ButtonInteractionEvent event, Game game, DiscordGame discordGame) throws ChannelNotFoundException {
        Faction faction = ButtonManager.getButtonPresser(event, game);

        discordGame.queueMessage(
                new MessageCreateBuilder().setContent("What part of Arrakis would you like to ship to?")
                        .addActionRow(Button.primary("stronghold", "Stronghold"),
                                Button.primary("spice-blow", "Spice Blow Territories"),
                                Button.primary("rock", "Rock Territories"),
                                Button.primary("other", "Somewhere else"),
                                Button.danger("pass-shipment", "I don't want to ship."))
        );

        if (faction.getName().equals("Guild") && faction.getShipment().getCrossShipFrom().isEmpty()) {
            List<Button> buttons = new LinkedList<>();
            buttons.add(Button.primary("guild-cross-ship", "Cross ship"));
            buttons.add(Button.primary("guild-ship-to-reserves", "Ship to reserves"));
            discordGame.getGuildChat().queueMessage("Special options for " + Emojis.GUILD + ":", buttons);
        }

        if (faction.hasAlly()) {
            List<Button> buttons = new LinkedList<>();
            buttons.add(Button.primary("support-max", "Support ally (no limits)"));
            buttons.add(Button.primary("support-number", "Support ally (specific amount)"));
            discordGame.getFactionChat(faction.getAlly()).queueMessage("Use buttons below to support your ally's shipment", buttons);
        }
        discordGame.queueDeleteMessage();
    }

    private static void queueMovementButtons(Game game, Faction faction, DiscordGame discordGame) throws ChannelNotFoundException {
        String message = "Use the following buttons to perform your move.";

        TreeSet<Button> movingFromButtons = new TreeSet<>(Comparator.comparing(Button::getLabel));

        for (Territory territory : game.getTerritories().values()){
            if (territory.getForces().stream().anyMatch(force -> force.getFactionName().equals(faction.getName()))
                    || (faction.getName().equals("Richese") && territory.hasRicheseNoField())
                    || (faction.getName().equals("BG") && territory.hasForce("Advisor"))) {
                movingFromButtons.add(Button.primary("moving-from-" + territory.getTerritoryName(), territory.getTerritoryName()));
                if (faction.getTreacheryHand().stream().anyMatch(treacheryCard -> treacheryCard.name().equals("Ornithopter"))) movingFromButtons.add(Button.primary("ornithopter-moving-from-" + territory.getTerritoryName(), territory.getTerritoryName() + " (use Ornithopter)"));
            }
        }
        movingFromButtons.add(Button.danger("pass-movement", "No move"));

        arrangeButtonsAndSend(message, movingFromButtons, discordGame);
    }

    private static void arrangeButtonsAndSend(String message, TreeSet<Button> buttons, DiscordGame discordGame) {
        List<MessageCreateBuilder> messagesToQueue = new LinkedList<>();
        MessageCreateBuilder messageCreateBuilder = new MessageCreateBuilder();
        messageCreateBuilder.setContent(message);
        int count = 0;
        while (!buttons.isEmpty()) {
            List<Button> actionRow = new LinkedList<>();
            for (int i = 0; i < 5; i++) {
                if (!buttons.isEmpty()) actionRow.add(buttons.pollFirst());
            }
            messageCreateBuilder.addActionRow(actionRow);
            count++;
            if (count == 5 || buttons.isEmpty()) {
                messagesToQueue.add(messageCreateBuilder);
                messageCreateBuilder = new MessageCreateBuilder()
                        .addContent("cont.");
                count = 0;
            }
        }

        messagesToQueue.forEach(discordGame::queueMessage);
    }

    public static void sendShipmentMessage(String factionName, DiscordGame discordGame, Game game) throws ChannelNotFoundException {
        List<Button> buttons = new LinkedList<>();
        buttons.add(Button.primary( "shipment", "Begin a ship action"));
        buttons.add(Button.danger( "pass-shipment", "Pass shipment"));
        discordGame.getFactionChat(factionName).queueMessage("Use buttons to perform Shipment and Movement actions on your turn." + " " + game.getFaction(factionName).getPlayer(), buttons);
    }

    public static void queueGuildTurnOrderButtons(DiscordGame discordGame, Game game) throws ChannelNotFoundException {
        Button takeTurn = Button.primary("guild-take-turn", "Take turn next.");
        Button defer = Button.primary("guild-defer", "Defer turn.");
        Button last = Button.primary("guild-wait-last", "Take turn last.");
        if (game.getTurnOrder().size() == 1) {
            defer = defer.asDisabled();
            last = last.asDisabled();
        }

        if (game.getTurnOrder().getLast().equals("juice-of-sapho-last")) {
            last = last.asDisabled();
            if (game.getTurnOrder().size() == 3) defer = defer.asDisabled();
        }

        List<Button> buttons = new LinkedList<>();
        buttons.add(takeTurn);
        buttons.add(defer);
        buttons.add(last);
        discordGame.getGuildChat().queueMessage("Use buttons to take your turn out of order.", buttons);
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