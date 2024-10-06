package controller.buttons;

import constants.Emojis;
import controller.channels.TurnSummary;
import controller.commands.CommandManager;
import enums.GameOption;
import enums.UpdateType;
import exceptions.ChannelNotFoundException;
import exceptions.InvalidGameStateException;
import controller.DiscordGame;
import model.*;
import model.factions.*;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;

import java.io.IOException;
import java.util.*;

public class ShipmentAndMovementButtons implements Pressable {


    public static void press(ButtonInteractionEvent event, Game game, DiscordGame discordGame) throws ChannelNotFoundException, IOException, InvalidGameStateException {

        if (event.getComponentId().startsWith("ship-sector-")) filterBySector(event, game, discordGame, true);
        else if (event.getComponentId().startsWith("ship-to-reserves-")) {
            queueForcesButtons(event, game, discordGame, game.getFaction("Guild"), true, false, false, false, false);
            discordGame.pushGame();
        }
        else if (event.getComponentId().startsWith("cross-ship-from-")) setCrossShipFrom(event, game, discordGame);
        else if (event.getComponentId().startsWith("stronghold")) queueStrongholdShippingButtons(event, game, discordGame);
        else if (event.getComponentId().startsWith("spice-blow")) queueSpiceBlowShippingButtons(event, discordGame, game);
        else if (event.getComponentId().startsWith("rock")) queueRockShippingButtons(event, discordGame, game);
        else if (event.getComponentId().startsWith("discovery-tokens")) queueDiscoveryShippingButtons(event, game, discordGame);
        else if (event.getComponentId().startsWith("other")) queueOtherShippingButtons(event, discordGame, game);
        else if (event.getComponentId().startsWith("pass-shipment")) passShipment(event, game, discordGame);
        else if (event.getComponentId().startsWith("reset-shipment")) resetShipmentMovement(event, game, discordGame, true);
        else if (event.getComponentId().startsWith("reset-shipping-forces")) resetForces(event, game, discordGame, true);
        else if (event.getComponentId().startsWith("execute-shipment")) executeShipment(event, game, discordGame, false);
        else if (event.getComponentId().startsWith("pass-movement")) passMovement(event, game, discordGame);
        else if (event.getComponentId().startsWith("reset-movement")) resetShipmentMovement(event, game, discordGame, false);
        else if (event.getComponentId().startsWith("reset-moving-forces")) resetForces(event, game, discordGame, false);
        else if (event.getComponentId().startsWith("ship-")) queueSectorButtons(event, game, discordGame, true);
        else if (event.getComponentId().startsWith("add-force-shipment-")) addForces(event, game, discordGame, true);
        else if (event.getComponentId().startsWith("add-special-force-shipment-"))
            addSpecialForces(event, game, discordGame, true);
        else if (event.getComponentId().startsWith("execute-movement")) executeMovement(event, game, discordGame);
        else if (event.getComponentId().startsWith("add-force-movement-")) addForces(event, game, discordGame, false);
        else if (event.getComponentId().startsWith("add-special-force-movement-"))
            addSpecialForces(event, game, discordGame, false);
        else if (event.getComponentId().startsWith("planetologist-add-force-"))
            planetologistAddForces(event, game, discordGame);
        else if (event.getComponentId().startsWith("planetologist-add-special-force-"))
            planetologistAddSpecialForces(event, game, discordGame);
        else if (event.getComponentId().startsWith("moving-from-"))
            queueMovableTerritories(event, game, discordGame, false);
        else if (event.getComponentId().startsWith("ornithopter-moving-from-"))
            ornithopterMovement(event, game, discordGame, false);
        else if (event.getComponentId().startsWith("ornithopter-token-moving-from-"))
            ornithopterMovement(event, game, discordGame, true);
        else if (event.getComponentId().startsWith("move-sector-")) filterBySector(event, game, discordGame, false);
        else if (event.getComponentId().startsWith("move-")) queueSectorButtons(event, game, discordGame, false);
        else if (event.getComponentId().startsWith("richese-no-field-ship-"))
            richeseNoFieldShip(event, game, discordGame, false);
        else if (event.getComponentId().startsWith("richese-ally-no-field-ship-"))
            richeseNoFieldShip(event, game, discordGame, true);
        else if (event.getComponentId().startsWith("support-")) refreshInfo(event, game, discordGame);
        else if (event.getComponentId().startsWith("bid-support-")) refreshInfo(event, game, discordGame);
        switch (event.getComponentId()) {
            case "shipment" -> queueShippingButtons(event, game, discordGame);
            case "homeworlds" -> queueHomeworldShippingButtons(event, game, discordGame);
            case "karama-execute-shipment" -> karamaExecuteShipment(event, game, discordGame);
            case "juice-of-sapho-first" -> playJuiceOfSapho(event, game, discordGame, false);
            case "juice-of-sapho-last" -> playJuiceOfSapho(event, game, discordGame, true);
            case "juice-of-sapho-don't-play" -> juiceOfSaphoDontPlay(event, game, discordGame);
            case "guild-take-turn" -> guildTakeTurn(game, discordGame);
            case "guild-wait-last" -> guildWaitLast(game, discordGame);
            case "guild-defer" -> guildDefer(game, discordGame);
            case "richese-no-field-move" -> richeseNoFieldMove(event, game, discordGame);
            case "guild-cross-ship" -> crossShip(event, game, discordGame);
            case "guild-ship-to-reserves" -> shipToReserves(game, discordGame);
            case "hajr" -> hajr(event, game, discordGame, true);
            case "Ornithopter" -> hajr(event, game, discordGame, false);
        }
    }

    public static void refreshInfo(ButtonInteractionEvent event, Game game, DiscordGame discordGame) throws ChannelNotFoundException {
        Faction faction = ButtonManager.getButtonPresser(event, game);
        faction.setUpdated(UpdateType.MISC_BACK_OF_SHIELD);
        discordGame.pushGame();
    }

    public static void deleteShipMoveButtonsInChannel(MessageChannel channel) {
        List<Message> messages = channel.getHistoryAround(channel.getLatestMessageId(), 100).complete().getRetrievedHistory();
        List<Message> messagesToDelete = new ArrayList<>();
        for (Message message : messages) {
            if (message.getContentRaw().contains("Where would you like to place "))
                continue;
            List<Button> buttons = message.getButtons();
            for (Button button : buttons) {
                String id = button.getId();
                if (id != null && (id.startsWith("pass-shipment") ||
                        id.startsWith("reset-shipment") ||
                        id.startsWith("add-force") ||
                        id.startsWith("add-special-force") ||
                        id.startsWith("planetologist-add-force") ||
                        id.startsWith("planetologist-add-special-force") ||
                        id.startsWith("guild-cross-ship") ||
                        id.startsWith("pass-movement") ||
                        id.startsWith("reset-movement") ||
                        id.startsWith("move-") ||
                        id.equals("hajr") ||
                        id.equals("Ornithopter") ||
                        id.startsWith("ornithopter") ||
                        id.startsWith("richese-no-field") ||
                        id.startsWith("richese-ally-no-field"))) {
                    messagesToDelete.add(message);
                    break;
                }
            }
        }
        for (Message message : messagesToDelete) {
            try {
                message.delete().complete();
            } catch (Exception ignore) {}
        }
    }

    private static void karamaExecuteShipment(ButtonInteractionEvent event, Game game, DiscordGame discordGame) throws ChannelNotFoundException, InvalidGameStateException {
        Faction faction = ButtonManager.getButtonPresser(event, game);
        game.getTreacheryDiscard().add(faction.removeTreacheryCard("Karama"));
        discordGame.getTurnSummary().queueMessage(faction.getEmoji() + " discards Karama to ship at " + Emojis.GUILD + " rates.");
        executeShipment(event, game, discordGame, true);
    }

    private static void ornithopterMovement(ButtonInteractionEvent event, Game game, DiscordGame discordGame, boolean isToken) throws ChannelNotFoundException {
        Faction faction = ButtonManager.getButtonPresser(event, game);
        if (isToken) {
            faction.setOrnithoperToken(false);
            discordGame.getTurnSummary().queueMessage(faction.getEmoji() + " use Ornithopter Discovery Token to move 3 spaces with their move.");

        } else {
            game.getTreacheryDiscard().add(faction.removeTreacheryCard("Ornithopter"));
            discordGame.getTurnSummary().queueMessage(faction.getEmoji() + " use Ornithopter " + Emojis.TREACHERY + " to move 3 spaces with their move.");
        }
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
        queueForcesButtons(event, game, discordGame, faction, false, false, false, false, false);
        discordGame.pushGame();
    }

    private static void planetologistAddForces(ButtonInteractionEvent event, Game game, DiscordGame discordGame) throws ChannelNotFoundException {
        Faction faction = ButtonManager.getButtonPresser(event, game);
        if (!event.getComponentId().split("-")[3].equals(faction.getMovement().getSecondMovingFrom())) {
            faction.getMovement().setSecondSpecialForce(0);
            faction.getMovement().setSecondForce(0);
        }
        faction.getMovement().setSecondMovingFrom(event.getComponentId().split("-")[3]);
        faction.getMovement().setSecondForce(faction.getMovement().getSecondForce() + Integer.parseInt(event.getComponentId().split("-")[4]));
        queueForcesButtons(event, game, discordGame, faction, false, false, false, false, false);
        discordGame.pushGame();
    }

    private static void hajr(ButtonInteractionEvent event, Game game, DiscordGame discordGame, boolean hajr) throws ChannelNotFoundException {
        Faction faction = ButtonManager.getButtonPresser(event, game);
        if (hajr) game.getTreacheryDiscard().add(faction.removeTreacheryCard("Hajr"));
        else game.getTreacheryDiscard().add(faction.removeTreacheryCard("Ornithopter"));
        String hajrOrOrnithopter = hajr ? "Hajr" : "Ornithopter";
        discordGame.getTurnSummary().queueMessage(faction.getEmoji() + " discards " + hajrOrOrnithopter + " to move again.");
        game.executeFactionMovement(faction);
        deleteShipMoveButtonsInChannel(event.getMessageChannel());
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
            if (territory.hasForce("Guild"))
                buttons.add(Button.primary("ship-to-reserves-" + territory.getTerritoryName(), territory.getTerritoryName()));
        }
        arrangeButtonsAndSend("Where would you like to ship to reserves from?", buttons, discordGame);
        game.getFaction("Guild").getShipment().setToReserves(true);
        discordGame.pushGame();
    }

    private static void crossShip(ButtonInteractionEvent event, Game game, DiscordGame discordGame) throws ChannelNotFoundException {
        Faction faction = ButtonManager.getButtonPresser(event, game);
        TreeSet<Button> buttons = new TreeSet<>(getButtonComparator());
        for (Territory territory : game.getTerritories().values()) {
            if (territory.getActiveFactions(game).contains(faction))
                buttons.add(Button.primary("cross-ship-from-" + territory.getTerritoryName(), territory.getTerritoryName()));
        }
        arrangeButtonsAndSend("Where would you like to ship from?", buttons, discordGame);
        game.getFaction("Guild").getShipment().setToReserves(true);
        discordGame.pushGame();
    }

    private static void guildDefer(Game game, DiscordGame discordGame) throws ChannelNotFoundException {
        game.getTurnOrder().pollFirst();
        discordGame.queueMessage("You will defer to " + Emojis.getFactionEmoji(Objects.requireNonNull(game.getTurnOrder().peekFirst())));
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
        faction.getShipment().setMayPlaySapho(true);
        discordGame.queueMessage("You will not play Juice of Sapho to go first.");
        game.getTurnOrder().pollFirst();
        if (game.hasFaction("Guild")) {
            game.getTurnOrder().addFirst("Guild");
            queueGuildTurnOrderButtons(discordGame, game);
        } else sendShipmentMessage(game.getTurnOrder().peekFirst(), discordGame, game);
        discordGame.pushGame();
        discordGame.queueDeleteMessage();
    }

    private static void playJuiceOfSapho(ButtonInteractionEvent event, Game game, DiscordGame discordGame, boolean last) throws ChannelNotFoundException {
        Faction faction = ButtonManager.getButtonPresser(event, game);
        if (faction.getTreacheryHand().stream().noneMatch(treacheryCard -> treacheryCard.name().equals("Juice of Sapho"))) {
            discordGame.queueMessageToEphemeral("These buttons were not intended for you");
            return;
        }
        faction.getShipment().setMayPlaySapho(false);
        game.getTurnOrder().pollFirst();
        String lastFirst = last ? "last" : "first";
        game.getTurnOrder().remove(faction.getName());
        if (last) {
            game.getTurnOrder().addLast(faction.getName());
            game.getTurnOrder().addLast("juice-of-sapho-last");

        } else {
            game.getTurnOrder().addFirst(faction.getName());
        }
        game.getTreacheryDiscard().add(faction.removeTreacheryCard("Juice of Sapho"));
        discordGame.queueMessage("You will go " + lastFirst + " this turn.");
        discordGame.getTurnSummary().queueMessage(faction.getEmoji() + " plays Juice of Sapho to ship and move " + lastFirst + " this turn.");
        if (last && game.hasFaction("Guild") && !(faction instanceof GuildFaction)) {
            game.getTurnOrder().addFirst("Guild");
            queueGuildTurnOrderButtons(discordGame, game);
        } else sendShipmentMessage(game.getTurnOrder().peekFirst(), discordGame, game);
        discordGame.pushGame();
        discordGame.queueDeleteMessage();
    }

    private static boolean isGuildNeedsToShip(Game game) {
        return game.hasFaction("Guild") && !game.getFaction("Guild").getShipment().hasShipped() && !game.getTurnOrder().contains("Guild");
    }

    private static void passMovement(ButtonInteractionEvent event, Game game, DiscordGame discordGame) throws ChannelNotFoundException {
        Faction faction = ButtonManager.getButtonPresser(event, game);
        TurnSummary turnSummary = discordGame.getTurnSummary();
        if (event.getComponentId().contains("-enter-discovery-token")) {
            discordGame.queueMessage("You will not enter the Discovery Token.");
            turnSummary.queueMessage(faction.getEmoji() + " does not enter the Discovery Token.");
        } else {
            discordGame.queueMessage("Shipment and movement complete.");
            turnSummary.queueMessage(faction.getEmoji() + " does not move.");
            game.getTurnOrder().pollFirst();
            if (game.getTurnOrder().size() == 1 && game.getTurnOrder().getFirst().equals("juice-of-sapho-last"))
                game.getTurnOrder().removeFirst();
            if (isGuildNeedsToShip(game)) {
                game.getTurnOrder().addFirst("Guild");
                queueGuildTurnOrderButtons(discordGame, game);
            } else if (!game.getTurnOrder().isEmpty()) {
                sendShipmentMessage(game.getTurnOrder().peekFirst(), discordGame, game);
            } else {
                discordGame.getModInfo().queueMessage("Everyone has taken their turn, please run advance.");
                discordGame.pushGame();
                return;
            }
            if (game.getTurnOrder().size() > 1 && Objects.requireNonNull(game.getTurnOrder().peekLast()).equals("Guild")) {
                turnSummary.queueMessage(Emojis.GUILD + " does not ship at this time");
            }
        }

        discordGame.pushGame();
        deleteShipMoveButtonsInChannel(event.getMessageChannel());
    }

    private static void executeMovement(ButtonInteractionEvent event, Game game, DiscordGame discordGame) throws ChannelNotFoundException {
        Faction faction = ButtonManager.getButtonPresser(event, game);
        game.executeFactionMovement(faction);
        if (event.getComponentId().contains("-fremen-ride")) {
            if (faction instanceof EcazFaction)
                discordGame.queueMessage("Movement with Fremen ambasssador complete.");
            else if (faction instanceof FremenFaction fremen){
                discordGame.queueMessage("Fremen ride complete.");
                fremen.setWormRideActive(false);
            }
        } else if (event.getComponentId().contains("-enter-discovery-token")) {
            discordGame.queueMessage("Movement into Discovery Token complete.");
        } else {
            game.getTurnOrder().pollFirst();
            if (game.getTurnOrder().size() == 1 && game.getTurnOrder().getFirst().equals("juice-of-sapho-last"))
                game.getTurnOrder().removeFirst();
            if (isGuildNeedsToShip(game)) {
                game.getTurnOrder().addFirst("Guild");
                queueGuildTurnOrderButtons(discordGame, game);
            } else if (!game.getTurnOrder().isEmpty()) {
                sendShipmentMessage(game.getTurnOrder().peekFirst(), discordGame, game);
            } else {
                discordGame.getModInfo().queueMessage("Everyone has taken their turn, please run advance.");
                discordGame.pushGame();
                return;
            }
            if (!game.getTurnOrder().isEmpty() && Objects.requireNonNull(game.getTurnOrder().peekLast()).equals("Guild") && game.getTurnOrder().size() > 1) {
                discordGame.getTurnSummary().queueMessage(Emojis.GUILD + " does not ship at this time");
            }
            discordGame.queueMessage("Shipment and movement complete.");
        }
        deleteShipMoveButtonsInChannel(event.getMessageChannel());
        discordGame.pushGame();
    }

    private static void queueMovableTerritories(ButtonInteractionEvent event, Game game, DiscordGame discordGame, boolean ornithopter) throws ChannelNotFoundException {
        Faction faction = ButtonManager.getButtonPresser(event, game);
        Territory from = game.getTerritory(event.getComponentId().replace("moving-from-", "").replace("ornithopter-", ""));
        String territoryName = from.getTerritoryName();
        faction.getMovement().setMovingFrom(territoryName);
        int spacesCanMove = 1;
        if (faction instanceof FremenFaction || (faction instanceof IxFaction && from.getForceStrength("Ix*") > 0))
            spacesCanMove = 2;
        if (ornithopter || game.getTerritory("Arrakeen").getActiveFactions(game).stream().anyMatch(f -> f.getName().equals(faction.getName())) ||
                game.getTerritory("Carthag").getActiveFactions(game).stream().anyMatch(f -> f.getName().equals(faction.getName())))
            spacesCanMove = 3;
        if (!faction.getSkilledLeaders().isEmpty() && faction.getSkilledLeaders().getFirst().getSkillCard().name().equals("Planetologist") && spacesCanMove < 3)
            spacesCanMove++;
        Set<String> moveableTerritories;
        if (territoryName.equals("Kaitain"))
            moveableTerritories = Set.of("Salusa Secundus");
        else if (territoryName.equals("Salusa Secundus"))
            moveableTerritories = Set.of("Kaitain");
        else
            moveableTerritories = getAdjacentTerritoryNames(territoryName.replaceAll("\\(.*\\)", "").strip(), spacesCanMove, game);
        TreeSet<Button> moveToButtons = new TreeSet<>(Comparator.comparing(Button::getLabel));
        moveableTerritories.stream().map(territory -> moveToTerritoryButton(game, faction, territory)).forEach(moveToButtons::add);
        arrangeButtonsAndSend("Where will your forces move to?", moveToButtons, discordGame);
        discordGame.pushGame();
    }

    private static Button moveToTerritoryButton(Game game, Faction faction, String territoryName) {
        String labelSuffix = "-" + territoryName;
        Button button = Button.primary("move" + labelSuffix, territoryName);
        List<Territory> sectors = game.getTerritories().values().stream().filter(s -> s.getTerritoryName().startsWith(territoryName)).toList();
        return button.withDisabled(sectors.stream().anyMatch(s -> s.factionMayNotEnter(game, faction, false)));
    }

    public static Set<String> getAdjacentTerritoryNames(String territory, int spacesAway, Game game) {
        if (spacesAway == 0) return new HashSet<>();

        List<String> adjacency = game.getAdjacencyList().get(territory);
        if (adjacency == null) return new HashSet<>();
        Set<String> adjacentTerritories = new HashSet<>(adjacency);
        Set<String> second = new HashSet<>();
        for (String adjacentTerritory : adjacentTerritories) {
            second.addAll(getAdjacentTerritoryNames(adjacentTerritory, spacesAway - 1, game));
        }
        adjacentTerritories.addAll(second);
        return adjacentTerritories;
    }

    public static void executeFactionShipment(DiscordGame discordGame, Game game, Faction faction, boolean karama, boolean guildAmbassador) throws ChannelNotFoundException, InvalidGameStateException {
        Shipment shipment = faction.getShipment();
        String territoryName = shipment.getTerritoryName();
        int noField = shipment.getNoField();
        int force = shipment.getForce();
        int specialForce = shipment.getSpecialForce();
        String crossShipFrom = shipment.getCrossShipFrom();
        Territory territory = game.getTerritory(territoryName);
        if (shipment.isToReserves()) {
            game.removeForces(territoryName, faction, force, specialForce, false);
            int spice = Math.ceilDiv(force, 2);
            faction.subtractSpice(spice, "shipment from " + territory.getTerritoryName() + " back to reserves");
            discordGame.getTurnSummary().queueMessage(Emojis.GUILD + " ship " + force + " " + Emojis.getForceEmoji("Guild") + " from " + territoryName + " to reserves. for " + spice + " " + Emojis.SPICE + " paid to the bank.");
        } else {
            if (territory.factionMustMoveOut(game, faction))
                faction.getMovement().setMustMoveOutOf(territoryName);
            if (noField >= 0) {
                RicheseFaction richese = (RicheseFaction) game.getFaction("Richese");
                richese.shipNoField(faction, territory, noField, karama, !crossShipFrom.isEmpty(), force);
                if (force > 0)
                    CommandManager.placeForces(territory, faction, force, 0, true, false, discordGame, game, karama);
                if (game.hasFaction("Ecaz"))
                    ((EcazFaction) game.getFaction("Ecaz")).checkForAmbassadorTrigger(territory, faction);
                if (game.hasFaction("Moritani"))
                    ((MoritaniFaction) game.getFaction("Moritani")).checkForTerrorTrigger(territory, faction, force + specialForce + 1);
            } else if (!crossShipFrom.isEmpty()) {
                game.removeForces(crossShipFrom, faction, force, 0, false);
                CommandManager.placeForces(territory, faction, force, specialForce, true, true, discordGame, game, false);
                discordGame.getTurnSummary().queueMessage(faction.getEmoji() + " cross shipped from " + crossShipFrom + " to " + territoryName);
            } else if (force > 0 || specialForce > 0)
                CommandManager.placeForces(territory, faction, force, specialForce, !guildAmbassador, true, true, discordGame, game, karama);
        }
        game.setUpdated(UpdateType.MAP);
        shipment.clear();
        if (guildAmbassador)
            shipment.setShipped(false);
    }

    private static void executeShipment(ButtonInteractionEvent event, Game game, DiscordGame discordGame, boolean karama) throws ChannelNotFoundException, InvalidGameStateException {
        Faction faction = ButtonManager.getButtonPresser(event, game);
        int totalForces = faction.getShipment().getForce() + faction.getShipment().getSpecialForce();
        Territory territory = game.getTerritory(faction.getShipment().getTerritoryName());
        boolean guildAmbassador = event.getComponentId().contains("-guild-ambassador");
        int spice = game.shipmentCost(faction, totalForces, territory, karama || guildAmbassador);
        int spiceFromAlly = 0;
        if (faction.hasAlly()) {
            spiceFromAlly = game.getFaction(faction.getAlly()).getShippingSupport();
        }
        if (spice > faction.getSpice() + spiceFromAlly)
            throw new InvalidGameStateException("You cannot afford this shipment.");
        executeFactionShipment(discordGame, game, faction, karama, guildAmbassador);
        if (guildAmbassador) {
            discordGame.queueMessage("Shipment with Guild ambassador complete.");
        } else {
            discordGame.queueMessage("Shipment complete.");
            faction.resetAllySpiceSupportAfterShipping(game);
            queueMovementButtons(game, faction, discordGame);
        }
        deleteShipMoveButtonsInChannel(event.getMessageChannel());
        discordGame.pushGame();
    }

    private static void resetShipmentMovement(ButtonInteractionEvent event, Game game, DiscordGame discordGame, boolean isShipment) throws ChannelNotFoundException {
        Faction faction = ButtonManager.getButtonPresser(event, game);
        if (isShipment) {
            boolean fremenRide = event.getComponentId().contains("-fremen-ride");
            boolean shaiHuludPlacement = event.getComponentId().contains("-place-shai-hulud");
            boolean greatMakerPlacement = event.getComponentId().contains("-place-great-maker");
            boolean guildAmbassador = event.getComponentId().contains("-guild-ambassador");
            if (fremenRide) {
                discordGame.queueMessage("Starting over");
                String fromTerritory = faction.getMovement().getMovingFrom();
                faction.getMovement().clear();
                faction.getMovement().setMoved(false);
                if (faction instanceof FremenFaction fremen)
                    fremen.presentWormRideChoices(fromTerritory);
                else if (faction instanceof EcazFaction ecaz)
                    ecaz.presentFremenAmbassadorRideFromChoices();
            } else if (shaiHuludPlacement || greatMakerPlacement) {
                discordGame.queueMessage("Starting over");
                String fromTerritory = faction.getMovement().getMovingFrom();
                faction.getMovement().clear();
                faction.getMovement().setMoved(false);
                ((FremenFaction) faction).presentWormPlacementChoices(fromTerritory, shaiHuludPlacement ? "Shai-Hulud" : "Great Maker");
            } else {
                faction.getShipment().clear();
                faction.getShipment().setShipped(false);
                if (guildAmbassador) {
                    discordGame.queueMessage("Starting over");
                    ((EcazFaction) faction).presentGuildAmbassadorDestinationChoices();
                }
                else
                    queueShippingButtons(event, game, discordGame);
            }
        } else {
            faction.getMovement().clear();
            faction.getMovement().setMoved(false);
            queueMovementButtons(game, faction, discordGame);
        }
        deleteShipMoveButtonsInChannel(event.getMessageChannel());
        discordGame.queueDeleteMessage();
        discordGame.pushGame();
    }

    private static void resetForces(ButtonInteractionEvent event, Game game, DiscordGame discordGame, boolean isShipment) throws ChannelNotFoundException {
        boolean fremenRide = event.getComponentId().contains("-fremen-ride");
        boolean guildAmbassador = event.getComponentId().contains("-guild-ambassador");
        boolean enterDiscoveryToken = event.getComponentId().contains("-enter-discovery-token");
        Faction faction = ButtonManager.getButtonPresser(event, game);
        if (isShipment && !fremenRide) {
            faction.getShipment().setForce(0);
            faction.getShipment().setSpecialForce(0);
            faction.getShipment().setNoField(-1);
            queueForcesButtons(event, game, discordGame, faction, true, false, guildAmbassador, false, false);
        } else {
            faction.getMovement().setForce(0);
            faction.getMovement().setSpecialForce(0);
            faction.getMovement().setSecondForce(0);
            faction.getMovement().setSecondSpecialForce(0);
            faction.getMovement().setSecondMovingFrom("");
            faction.getMovement().setMovingNoField(false);
            queueForcesButtons(event, game, discordGame, faction, false, false, false, fremenRide, enterDiscoveryToken);
        }
        discordGame.pushGame();
    }

    private static void addForces(ButtonInteractionEvent event, Game game, DiscordGame discordGame, boolean isShipment) throws ChannelNotFoundException {
        boolean fremenRide = event.getComponentId().contains("-fremen-ride");
        boolean guildAmbassador = event.getComponentId().contains("-guild-ambassador");
        boolean enterDiscoveryToken = event.getComponentId().contains("-enter-discovery-token");
        Faction faction = ButtonManager.getButtonPresser(event, game);
        if (isShipment)
            faction.getShipment().setForce((faction.getShipment().getForce() + Integer.parseInt(event.getComponentId().replace("add-force-shipment-", "").replace("guild-ambassador-", ""))));
        else
            faction.getMovement().setForce((faction.getMovement().getForce() + Integer.parseInt(event.getComponentId().replace("add-force-movement-", "").replace("fremen-ride-", "").replace("enter-discovery-token-", ""))));
        queueForcesButtons(event, game, discordGame, faction, isShipment, false, guildAmbassador, fremenRide, enterDiscoveryToken);
        discordGame.pushGame();
    }

    private static void addSpecialForces(ButtonInteractionEvent event, Game game, DiscordGame discordGame, boolean isShipment) throws ChannelNotFoundException {
        boolean fremenRide = event.getComponentId().contains("-fremen-ride");
        boolean enterDiscoveryToken = event.getComponentId().contains("-enter-discovery-token");
        Faction faction = ButtonManager.getButtonPresser(event, game);
        if (isShipment)
            faction.getShipment().setSpecialForce((faction.getShipment().getSpecialForce() + Integer.parseInt(event.getComponentId().replace("add-special-force-shipment-", ""))));
        else
            faction.getMovement().setSpecialForce((faction.getMovement().getSpecialForce() + Integer.parseInt(event.getComponentId().replace("add-special-force-movement-", "").replace("fremen-ride-", "").replace("enter-discovery-token-", ""))));
        queueForcesButtons(event, game, discordGame, faction, isShipment, false, false, fremenRide, enterDiscoveryToken);
        discordGame.pushGame();
    }

    private static void filterBySector(ButtonInteractionEvent event, Game game, DiscordGame discordGame, boolean isShipment) throws ChannelNotFoundException {
        boolean fremenRide = event.getComponentId().contains("-fremen-ride");
        boolean shaiHuludPlacement = event.getComponentId().contains("-place-shai-hulud");
        boolean greatMakerPlacement = event.getComponentId().contains("-place-great-maker");
        boolean guildAmbassador = event.getComponentId().contains("-guild-ambassador");
        boolean enterDiscoveryToken = event.getComponentId().contains("-enter-discovery-token");
        String shipmentOrMovement = isShipment ? "ship-" : "move-";
        Faction faction = ButtonManager.getButtonPresser(event, game);
        Territory territory = game.getTerritories().values().stream().filter(t -> t.getTerritoryName().contains(
                event.getComponentId().replace(shipmentOrMovement + "sector-", "")
                        .replace("fremen-ride-", "")
                        .replace("place-shai-hulud-", "")
                        .replace("place-great-maker-", "")
                        .replace("guild-ambassador-", "")
                        .replace("enter-discovery-token-", "")
                        .replace("-", " ")
                )
        ).findFirst().orElseThrow();

        if (shaiHuludPlacement || greatMakerPlacement) {
            String wormName = shaiHuludPlacement ? "Shai-Hulud" : "Great Maker";
            String territoryName = territory.getTerritoryName();
            game.placeShaiHulud(territoryName, wormName, false);
            ((FremenFaction) game.getFaction("Fremen")).wormWasPlaced();
            discordGame.queueMessage("You placed " + wormName + " in " + territoryName);
            discordGame.pushGame();
            return;
        }
        else if (isShipment && !fremenRide) faction.getShipment().setTerritoryName(territory.getTerritoryName());
        else faction.getMovement().setMovingTo(territory.getTerritoryName());

        queueForcesButtons(event, game, discordGame, faction, isShipment, false, guildAmbassador, fremenRide, enterDiscoveryToken);
        discordGame.pushGame();
    }

    private static void richeseNoFieldShip(ButtonInteractionEvent event, Game game, DiscordGame discordGame, boolean isAlly) throws ChannelNotFoundException {
        deleteShipMoveButtonsInChannel(event.getMessageChannel());
        Faction faction = game.getFaction("Richese");
        String componentId = "richese-no-field-ship-";
        if (isAlly) {
            faction = game.getFaction(faction.getAlly());
            componentId = "richese-ally-no-field-ship-";
        }
        int spice = game.getTerritory(faction.getShipment().getTerritoryName()).costToShipInto();
        MessageCreateBuilder messageCreateBuilder = new MessageCreateBuilder()
                .addContent(
                        "\nCurrently shipping:\n" + event.getComponentId().replace(componentId, "") + " " + Emojis.NO_FIELD + " to " +
                                faction.getShipment().getTerritoryName() + " for " + spice + " " + Emojis.SPICE + "\n\nYou have " + faction.getSpice() + " " + Emojis.SPICE + " to spend."
                ).addActionRow(
                        Button.success("execute-shipment", "Confirm Shipment"),
                        Button.danger("reset-shipping-forces", "Reset forces"),
                        Button.danger("reset-shipment", "Start over")
                );

        faction.getShipment().setNoField(Integer.parseInt(event.getComponentId().replace(componentId, "")));
        if (game.hasGameOption(GameOption.HOMEWORLDS) && !isAlly && faction.isHighThreshold())
            queueForcesButtons(event, game, discordGame, faction, true, true, false, false, false);
        else
            discordGame.queueMessage(messageCreateBuilder);
        discordGame.pushGame();
    }

    private static void queueForcesButtons(ButtonInteractionEvent event, Game game, DiscordGame discordGame, Faction faction, boolean isShipment, boolean hasNoField, boolean guildAmbassador, boolean fremenRide, boolean enterDiscoveryToken) throws ChannelNotFoundException {
        deleteShipMoveButtonsInChannel(event.getMessageChannel());

        TreeSet<Button> forcesButtons = new TreeSet<>(getButtonComparator());
        int buttonLimitForces = isShipment && !fremenRide ? faction.getReservesStrength() - faction.getShipment().getForce() :
                game.getTerritory(faction.getMovement().getMovingFrom()).getForceStrength(faction.getName()) - faction.getMovement().getForce();
        if (guildAmbassador)
            buttonLimitForces = Math.min(4, buttonLimitForces);
        if (faction instanceof BGFaction && !isShipment && game.getTerritory(faction.getMovement().getMovingFrom()).hasForce("Advisor"))
            buttonLimitForces = game.getTerritory(faction.getMovement().getMovingFrom()).getForceStrength("Advisor");
        int buttonLimitSpecialForces = isShipment && !fremenRide ? faction.getSpecialReservesStrength() - faction.getShipment().getSpecialForce() :
                game.getTerritory(faction.getMovement().getMovingFrom()).getForceStrength(faction.getName() + "*") - faction.getMovement().getSpecialForce();

        if (faction.getShipment().isToReserves()) {
            if (event.getComponentId().startsWith("ship-to-reserves-")) {
                faction.getShipment().setToReserves(true);
                faction.getShipment().setTerritoryName(event.getComponentId().replace("ship-to-reserves-", ""));
            }
            buttonLimitForces = game.getTerritory(faction.getShipment().getTerritoryName()).getForceStrength("Guild") - faction.getShipment().getForce();
        }

        if (!faction.getShipment().getCrossShipFrom().isEmpty()) {
            buttonLimitForces = game.getTerritory(faction.getShipment().getCrossShipFrom()).getForceStrength(faction.getName()) - faction.getShipment().getForce();
            buttonLimitSpecialForces = game.getTerritory(faction.getShipment().getCrossShipFrom()).getForceStrength(faction.getName() + "*") - faction.getShipment().getSpecialForce();
        }

        if (fremenRide)
            isShipment = false;
        String shipOrMove = isShipment ? "shipment" : "movement";
        String buttonSuffix = "";
        if (fremenRide)
            buttonSuffix = "-fremen-ride";
        if (guildAmbassador)
            buttonSuffix += "-guild-ambassador";
        if (enterDiscoveryToken)
            buttonSuffix += "-enter-discovery-token";
        shipOrMove += buttonSuffix + "-";
        for (int i = 0; i < buttonLimitForces; i++) {
            forcesButtons.add(Button.primary("add-force-" + shipOrMove + (i + 1), "Add " + (i + 1) + " troop"));
        }
        for (int i = 0; i < buttonLimitSpecialForces; i++) {
            forcesButtons.add(Button.primary("add-special-force-" + shipOrMove + (i + 1), "Add " + (i + 1) + " * troop"));
        }

        if (isShipment) {
            int spice = faction.getShipment().getForce() + faction.getShipment().getSpecialForce();
            if (faction.getShipment().getNoField() >= 0) spice++;

            String territory = faction.getShipment().getTerritoryName();
            if (faction.getShipment().isToReserves()) territory = "reserves from " + territory;
            if (!faction.getShipment().getCrossShipFrom().isEmpty())
                territory = territory + " cross shipping from " + faction.getShipment().getCrossShipFrom();

            if (game.getTerritory(faction.getShipment().getTerritoryName()).costToShipInto() == 2 && !faction.getShipment().isToReserves())
                spice *= 2;

            if (faction instanceof FremenFaction || guildAmbassador)
                spice = 0;
            if (faction instanceof GuildFaction || (faction.hasAlly() && faction.getAlly().equals("Guild")))
                spice = Math.ceilDiv(spice, 2);
            String specialForces = faction.hasStarredForces() ? " " + faction.getShipment().getSpecialForce() + " " + Emojis.getForceEmoji(faction.getName() + "*") : "";
            String noFieldMessage = faction.getShipment().getNoField() >= 0 ? "\n" + faction.getShipment().getNoField() + " " + Emojis.NO_FIELD + "\n": "";
            String currentlyShipping = "Currently shipping:\n**" + faction.getShipment().getForce() + " " + Emojis.getForceEmoji(faction.getName()) +
                    specialForces + noFieldMessage + "** to " + territory +
                    (guildAmbassador ? "" : " for " + spice + " " + Emojis.SPICE + "\n\nYou have " + faction.getSpice() + " " + Emojis.SPICE + " to spend.");
            String message = "Use buttons below to add forces to your shipment. " + currentlyShipping;

            if (!forcesButtons.isEmpty()) {
                arrangeButtonsAndSend(message, forcesButtons, discordGame);
            } else {
                message = "You have no troops in reserves to ship. ";
                if (faction.getShipment().getForce() != 0 || faction.getShipment().getSpecialForce() != 0 || faction.getShipment().getNoField() != 0)
                    message += currentlyShipping;
                discordGame.queueMessage(message);
            }

            if (faction instanceof RicheseFaction || faction.getAlly().equals("Richese")) {
                RicheseFaction richese = (RicheseFaction) game.getFaction("Richese");
                MessageCreateBuilder noFieldButtonMessage = new MessageCreateBuilder()
                        .setContent("No-Field options:");
                TreeSet<Button> noFieldButtons = new TreeSet<>(getButtonComparator());
                List<Integer> noFields = new LinkedList<>();
                noFields.add(0);
                noFields.add(3);
                noFields.add(5);
                noFields.removeIf(integer -> Objects.equals(integer, richese.getFrontOfShieldNoField()) || integer.equals(richese.getShipment().getNoField()));
                for (int noField : noFields) {
                    if (faction instanceof RicheseFaction)
                        noFieldButtons.add(Button.primary("richese-no-field-ship-" + noField, "Ship " + noField + " No-Field token."));
                    else
                        noFieldButtons.add(Button.primary("richese-ally-no-field-ship-" + noField, "Ship " + noField + " No-Field token."));
                }
                noFieldButtonMessage.addActionRow(noFieldButtons);
                discordGame.getFactionChat(faction).queueMessage(noFieldButtonMessage);
            }
            List<Button> finalizeButtons = new LinkedList<>();

            boolean disableConfirmButton = true;
            Shipment shipment = faction.getShipment();
            if (shipment.getForce() > 0 || shipment.getSpecialForce() > 0 || hasNoField)
                disableConfirmButton = false;
            Button execute = Button.success("execute-shipment" + (guildAmbassador ? "-guild-ambassador" : ""), "Confirm Shipment").withDisabled(disableConfirmButton);

            if (faction.getShipment().hasShipped()) execute = execute.asDisabled();
            finalizeButtons.add(execute);
            finalizeButtons.add(Button.danger("reset-shipping-forces" + buttonSuffix, "Reset forces"));
            finalizeButtons.add(Button.danger("reset-shipment" + buttonSuffix, "Start over"));
            if (!guildAmbassador && !enterDiscoveryToken && faction.hasTreacheryCard("Karama"))
                finalizeButtons.add(Button.secondary("karama-execute-shipment", "Confirm Shipment (Use Karama for Guild rates)").withDisabled(disableConfirmButton));

            discordGame.getFactionChat(faction.getName()).queueMessage(new MessageCreateBuilder()
                    .setContent("Finalize or start over:")
                    .addActionRow(finalizeButtons));
        } else {
            Movement movement = faction.getMovement();
            if (faction instanceof RicheseFaction && game.getTerritories().get(faction.getMovement().getMovingFrom()).hasRicheseNoField() && !faction.getMovement().isMovingNoField())
                forcesButtons.add(Button.primary("richese-no-field-move", "+1 No-Field token")
                        .withDisabled(game.hasGameOption(GameOption.HOMEWORLDS) && !faction.isHighThreshold()));
            String specialForces = faction.hasStarredForces() ? " " + faction.getMovement().getSpecialForce() + " " + Emojis.getForceEmoji(faction.getName() + "*") : "";
            String noField = faction.getMovement().isMovingNoField() ? "\n" + game.getTerritory(faction.getMovement().getMovingFrom()).getRicheseNoField() + " No-Field token" : "";

            String message = "Use buttons below to add forces to your ";
            message += fremenRide ? "ride." : "movement.";
            message += " Currently moving:\n**" + faction.getMovement().getForce() + " " + Emojis.getForceEmoji(faction.getName()) + specialForces + noField + "** to " + faction.getMovement().getMovingTo();
            if (movement.isMovingNoField() || movement.getForce() != 0 || movement.getSpecialForce() != 0 || movement.getSecondForce() != 0 || movement.getSecondSpecialForce() != 0) {
                if (!fremenRide && !enterDiscoveryToken && faction.hasTreacheryCard("Hajr"))
                    forcesButtons.add(Button.secondary("hajr", "Confirm Movement and play Hajr"));
                if (!fremenRide && !enterDiscoveryToken && faction.hasTreacheryCard("Ornithopter"))
                    forcesButtons.add(Button.secondary("Ornithopter", "Confirm Movement and play Ornithopter"));
                forcesButtons.add(Button.success("execute-movement" + buttonSuffix, "Confirm Movement"));
                forcesButtons.add(Button.danger("reset-moving-forces" + buttonSuffix, "Reset forces"));
            }
            if (enterDiscoveryToken)
                forcesButtons.add(Button.danger("pass-movement-enter-discovery-token", "Don't enter"));
            else
                forcesButtons.add(Button.danger(fremenRide ? "reset-shipment-fremen-ride" : "reset-movement" + buttonSuffix, "Start over"));

            arrangeButtonsAndSend(message, forcesButtons, discordGame);

            if (!fremenRide && !faction.getSkilledLeaders().isEmpty() && faction.getSkilledLeaders().getFirst().getSkillCard().name().equals("Planetologist")) {
                int spacesCanMove = 1;
                if (faction instanceof FremenFaction) spacesCanMove = 2;
                if (game.getTerritory("Arrakeen").getActiveFactions(game).stream().anyMatch(f -> f.getName().equals(faction.getName())) ||
                        game.getTerritory("Carthag").getActiveFactions(game).stream().anyMatch(f -> f.getName().equals(faction.getName())))
                    spacesCanMove = 3;
                for (Territory territory : game.getTerritories().values()) {
                    if (territory.getTerritoryName().equals(faction.getMovement().getMovingFrom())) continue;
                    String secondMovingFrom = faction.getMovement().getSecondMovingFrom();
                    if (!secondMovingFrom.isEmpty() && !territory.getTerritoryName().equals(secondMovingFrom)) continue;
                    if (!territory.hasForce(faction.getName()) && !territory.hasForce(faction.getName() + "*"))
                        continue;

                    Set<String> territoriesInRange = getAdjacentTerritoryNames(territory.getTerritoryName().replaceAll("\\(.*\\)", "").strip(), spacesCanMove, game);
                    if (!territoriesInRange.contains(faction.getMovement().getMovingTo().replaceAll("\\(.*\\)", "").strip()))
                        continue;

                    TreeSet<Button> secondForcesButtons = new TreeSet<>(getButtonComparator());
                    int secondForcesButtonLimit = territory.getForceStrength(faction.getName());
                    if (faction instanceof BGFaction && game.getTerritory(faction.getMovement().getMovingFrom()).hasForce("Advisor"))
                        secondForcesButtonLimit = territory.getForceStrength("Advisor");
                    int secondSpecialForcesButtonLimit = territory.getForceStrength(faction.getName() + "*");

                    if (territory.getTerritoryName().equals(secondMovingFrom)) {
                        secondForcesButtonLimit -= faction.getMovement().getSecondForce();
                        secondSpecialForcesButtonLimit -= faction.getMovement().getSecondSpecialForce();
                    }

                    for (int i = 0; i < secondForcesButtonLimit; i++) {
                        secondForcesButtons.add(Button.primary("planetologist-add-force-" + territory.getTerritoryName() + "-" + (i + 1), "Add " + (i + 1) + " troop"));
                    }
                    for (int i = 0; i < secondSpecialForcesButtonLimit; i++) {
                        secondForcesButtons.add(Button.primary("planetologist-add-special-force-" + territory.getTerritoryName() + "-" + (i + 1), "Add " + (i + 1) + " * troop"));
                    }
                    String secondSpecialForces = faction.hasStarredForces() ? " " + faction.getMovement().getSecondSpecialForce() + " " + Emojis.getForceEmoji(faction.getName() + "*") : "";
                    String planetologistMessage = "Second group can move from " + territory.getTerritoryName() + " using Planetologist ability.";
                    planetologistMessage +=
                            " Currently moving:\n**" + faction.getMovement().getSecondForce() + " " + Emojis.getForceEmoji(faction.getName()) + secondSpecialForces + "** from " + secondMovingFrom + " to " + faction.getMovement().getMovingTo();
                    arrangeButtonsAndSend(planetologistMessage, secondForcesButtons, discordGame);
                }
            }
        }
    }

    private static void richeseNoFieldMove(ButtonInteractionEvent event, Game game, DiscordGame discordGame) throws ChannelNotFoundException {
        RicheseFaction richese = (RicheseFaction) game.getFaction("Richese");
        richese.getMovement().setMovingNoField(true);
        queueForcesButtons(event, game, discordGame, richese, false, false, false, false, false);
        discordGame.pushGame();
    }

    private static void queueSectorButtons(ButtonInteractionEvent event, Game game, DiscordGame discordGame, boolean isShipment) throws ChannelNotFoundException {
        String buttonSuffix = "";
        boolean fremenRide = event.getComponentId().contains("-fremen-ride");
        if (fremenRide)
            buttonSuffix = "-fremen-ride";
        boolean shaiHuludPlacement = event.getComponentId().contains("-place-shai-hulud");
        if (shaiHuludPlacement)
            buttonSuffix = "-place-shai-hulud";
        boolean greatMakerPlacement = event.getComponentId().contains("-place-great-maker");
        if (greatMakerPlacement)
            buttonSuffix = "-place-great-maker";
        boolean guildAmbassador = event.getComponentId().contains("-guild-ambassador");
        if (guildAmbassador)
            buttonSuffix += "-guild-ambassador";
        String shipmentOrMovement = isShipment ? "ship-" : "move-";
        Faction faction = ButtonManager.getButtonPresser(event, game);
        List<Territory> territory = game.getTerritories().values().stream().filter(t -> t.getTerritoryName().replaceAll("\\s*\\([^)]*\\)\\s*", "").equalsIgnoreCase(
                event.getComponentId().replace(shipmentOrMovement, "")
                        .replace("fremen-ride-", "")
                        .replace("place-shai-hulud-", "")
                        .replace("place-great-maker-", "")
                        .replace("guild-ambassador-", "")
                        .replace("-", " ")
                )
        ).toList();

        if (territory.size() == 1) {
            if (shaiHuludPlacement || greatMakerPlacement) {
                String wormName = shaiHuludPlacement ? "Shai-Hulud" : "Great Maker";
                String territoryName = territory.getFirst().getTerritoryName();
                game.placeShaiHulud(territoryName, wormName, false);
                ((FremenFaction) game.getFaction("Fremen")).wormWasPlaced();
                discordGame.queueMessage("You placed " + wormName + " in " + territoryName);
                discordGame.pushGame();
                return;
            }
            if (isShipment && !fremenRide) faction.getShipment().setTerritoryName(territory.getFirst().getTerritoryName());
            else faction.getMovement().setMovingTo(territory.getFirst().getTerritoryName());
            queueForcesButtons(event, game, discordGame, faction, isShipment, false, guildAmbassador, fremenRide, false);
            deleteShipMoveButtonsInChannel(event.getMessageChannel());
            discordGame.pushGame();
            return;
        }

        List<Button> buttons = new LinkedList<>();

        for (Territory sector : territory) {
            if (sector.getSpice() > 0)
                buttons.add(Button.primary(shipmentOrMovement + "sector" + buttonSuffix + "-" + sector.getTerritoryName(), sector.getSector() + " (spice sector)"));
            else
                buttons.add(Button.primary(shipmentOrMovement + "sector" + buttonSuffix + "-" + sector.getTerritoryName(), String.valueOf(sector.getSector())));
        }
        buttons.sort(getButtonComparator());
        String backButtonId = isShipment ? "reset-shipment" : "reset-movement";

        deleteShipMoveButtonsInChannel(event.getMessageChannel());
        discordGame.queueMessage(new MessageCreateBuilder()
                .setContent("Which sector?")
                .addActionRow(buttons)
                .addActionRow(Button.secondary(backButtonId + buttonSuffix, "Start over"))
        );
    }

    private static void queueOtherShippingButtons(ButtonInteractionEvent event, DiscordGame discordGame, Game game) {
        String buttonSuffix = event.getComponentId().replace("other", "");
        boolean fremenRide = event.getComponentId().contains("-fremen-ride");
        boolean wormPlacement = event.getComponentId().contains("-place-shai-hulud") || event.getComponentId().contains("-place-great-maker");
        Faction faction = ButtonManager.getButtonPresser(event, game);
        Button passButton = Button.danger("pass-shipment" + buttonSuffix, fremenRide ? "No ride" : "Pass Shipment");
        if (wormPlacement)
            passButton = Button.secondary("pass-shipment" + buttonSuffix, "Keep it in " + faction.getMovement().getMovingFrom());
        MessageCreateBuilder message = new MessageCreateBuilder()
                .setContent("Which territory?")
                .addActionRow(shipToTerritoryButton(game, faction, buttonSuffix, "Polar Sink"),
                        shipToTerritoryButton(game, faction, buttonSuffix, "Cielago Depression"),
                        shipToTerritoryButton(game, faction, buttonSuffix, "Meridian"),
                        shipToTerritoryButton(game, faction, buttonSuffix, "Cielago East"),
                        shipToTerritoryButton(game, faction, buttonSuffix, "Harg Pass"))
                .addActionRow(
                        shipToTerritoryButton(game, faction, buttonSuffix, "Gara Kulon"),
                        shipToTerritoryButton(game, faction, buttonSuffix, "Hole In The Rock"),
                        shipToTerritoryButton(game, faction, buttonSuffix, "Basin"),
                        shipToTerritoryButton(game, faction, buttonSuffix, "Imperial Basin"),
                        shipToTerritoryButton(game, faction, buttonSuffix, "Arsunt"))
                .addActionRow(
                        shipToTerritoryButton(game, faction, buttonSuffix, "Tsimpo"),
                        shipToTerritoryButton(game, faction, buttonSuffix, "Bight Of The Cliff"),
                        shipToTerritoryButton(game, faction, buttonSuffix, "Wind Pass"),
                        shipToTerritoryButton(game, faction, buttonSuffix, "The Greater Flat"),
                        shipToTerritoryButton(game, faction, buttonSuffix, "Cielago West"))
                .addActionRow(
                        Button.secondary("reset-shipment" + buttonSuffix, "Start over"),
                        passButton);
        discordGame.queueMessage(message);
        discordGame.queueDeleteMessage();
    }

    private static void queueRockShippingButtons(ButtonInteractionEvent event, DiscordGame discordGame, Game game) {
        String buttonSuffix = event.getComponentId().replace("rock", "");
        boolean fremenRide = event.getComponentId().contains("-fremen-ride");
        Faction faction = ButtonManager.getButtonPresser(event, game);
        MessageCreateBuilder message = new MessageCreateBuilder()
                .setContent("Which rock territory?")
                .addActionRow(shipToTerritoryButton(game, faction, buttonSuffix, "False Wall South"),
                        shipToTerritoryButton(game, faction, buttonSuffix, "Pasty Mesa"),
                        shipToTerritoryButton(game, faction, buttonSuffix, "False Wall East"),
                        shipToTerritoryButton(game, faction, buttonSuffix, "Shield Wall"),
                        shipToTerritoryButton(game, faction, buttonSuffix, "Rim Wall West"))
                .addActionRow(
                        shipToTerritoryButton(game, faction, buttonSuffix, "Plastic Basin"),
                        shipToTerritoryButton(game, faction, buttonSuffix, "False Wall West"),
                        Button.secondary("reset-shipment" + buttonSuffix, "Start over"),
                        Button.danger("pass-shipment" + buttonSuffix, fremenRide ? "No ride" : "Pass Shipment"));

        discordGame.queueMessage(message);
        discordGame.queueDeleteMessage();
    }

    private static void queueSpiceBlowShippingButtons(ButtonInteractionEvent event, DiscordGame discordGame, Game game) {
        String buttonSuffix = event.getComponentId().replace("spice-blow", "");
        boolean fremenRide = event.getComponentId().contains("-fremen-ride");
        boolean wormPlacement = event.getComponentId().contains("-place-shai-hulud") || event.getComponentId().contains("-place-great-maker");
        Faction faction = ButtonManager.getButtonPresser(event, game);
        Button passButton = Button.danger("pass-shipment" + buttonSuffix, fremenRide ? "No ride" : "Pass Shipment");
        if (wormPlacement)
            passButton = Button.secondary("pass-shipment" + buttonSuffix, "Keep it in " + faction.getMovement().getMovingFrom());
        MessageCreateBuilder message = new MessageCreateBuilder()
                .setContent("Which spice blow territory?")
                .addActionRow(shipToTerritoryButton(game, faction, buttonSuffix, "Habbanya Ridge Flat"),
                        shipToTerritoryButton(game, faction, buttonSuffix, "Cielago South"),
                        shipToTerritoryButton(game, faction, buttonSuffix, "Broken Land"),
                        shipToTerritoryButton(game, faction, buttonSuffix, "South Mesa"),
                        shipToTerritoryButton(game, faction, buttonSuffix, "Sihaya Ridge"))
                .addActionRow(
                        shipToTerritoryButton(game, faction, buttonSuffix, "Hagga Basin"),
                        shipToTerritoryButton(game, faction, buttonSuffix, "Red Chasm"),
                        shipToTerritoryButton(game, faction, buttonSuffix, "The Minor Erg"),
                        shipToTerritoryButton(game, faction, buttonSuffix, "Cielago North"),
                        shipToTerritoryButton(game, faction, buttonSuffix, "Funeral Plain"))
                .addActionRow(
                        shipToTerritoryButton(game, faction, buttonSuffix, "The Great Flat"),
                        shipToTerritoryButton(game, faction, buttonSuffix, "Habbanya Erg"),
                        shipToTerritoryButton(game, faction, buttonSuffix, "Old Gap"),
                        shipToTerritoryButton(game, faction, buttonSuffix, "Rock Outcroppings"),
                        shipToTerritoryButton(game, faction, buttonSuffix, "Wind Pass North"))
                .addActionRow(
                        Button.secondary("reset-shipment" + buttonSuffix, "Start over"),
                        passButton);

        discordGame.queueMessage(message);
        discordGame.queueDeleteMessage();
    }

    private static void queueHomeworldShippingButtons(ButtonInteractionEvent event, Game game, DiscordGame discordGame) {
        Faction faction = ButtonManager.getButtonPresser(event, game);
        List<Button> buttons = new LinkedList<>();

        for (Faction f : game.getFactions()) {
            if (faction.getName().equals(f.getName())) continue;
            buttons.add(shipToTerritoryButton(game, faction, "", f.getHomeworld()));
            if (f instanceof EmperorFaction emperorFaction)
                buttons.add(shipToTerritoryButton(game, faction, "", emperorFaction.getSecondHomeworld()));
        }
        buttons.add(Button.secondary("reset-shipment", "Start over"));

        MessageCreateBuilder message = new MessageCreateBuilder()
                .setContent("Which Homeworld?");
        if (buttons.size() > 5) {
            message.addActionRow(buttons.subList(0, 5));
            message.addActionRow(buttons.subList(5, buttons.size()));
        } else {
            message.addActionRow(buttons);
        }
        discordGame.queueMessage(message);
        discordGame.queueDeleteMessage();
    }

    private static void queueDiscoveryShippingButtons(ButtonInteractionEvent event, Game game, DiscordGame discordGame) {
        String buttonSuffix = event.getComponentId().replace("discovery-tokens", "");
        boolean fremenRide = event.getComponentId().contains("-fremen-ride");
        Faction faction = ButtonManager.getButtonPresser(event, game);
        List<Button> buttons = new ArrayList<>();
        for (Territory territory : game.getTerritories().values()) {
            if (territory.isDiscovered()) {
                buttons.add(shipToTerritoryButton(game, faction, buttonSuffix, territory.getDiscoveryToken()));
            }
        }
        buttons.add(Button.secondary("reset-shipment" + buttonSuffix, "Start over"));
        buttons.add(Button.danger("pass-shipment" + buttonSuffix, fremenRide ? "No ride" : "Pass Shipment"));

        MessageCreateBuilder message = new MessageCreateBuilder()
                .setContent("Which Discovery Token?");
        if (buttons.size() > 5) {
            message.addActionRow(buttons.subList(0, 5));
            message.addActionRow(buttons.subList(5, buttons.size()));
        } else {
            message.addActionRow(buttons);
        }
        discordGame.queueMessage(message);
        discordGame.queueDeleteMessage();
    }

    private static boolean validStronghold(Game game, Territory t, Faction faction, boolean fremenRide) {
        if (!t.isStronghold())
            return false;
        else if (t.getTerritoryName().equals("Hidden Mobile Stronghold"))
            return faction instanceof IxFaction || fremenRide && game.hasFaction("Ix");
        else
            return true;
    }

    private static void queueStrongholdShippingButtons(ButtonInteractionEvent event, Game game, DiscordGame discordGame) {
        Faction faction = ButtonManager.getButtonPresser(event, game);
        String buttonSuffix = event.getComponentId().replace("stronghold", "");
        boolean fremenRide = event.getComponentId().contains("-fremen-ride");
        MessageCreateBuilder message = new MessageCreateBuilder().setContent("Which stronghold?");
        List<Button> strongholds = new ArrayList<>();
        for (Territory t : game.getTerritories().values()) {
            if (validStronghold(game, t, faction, fremenRide)) {
                Button button = shipToTerritoryButton(game, faction, buttonSuffix, t.getTerritoryName());
                strongholds.add(button);
            }
        }
        strongholds.sort(Comparator.comparing(Button::getLabel));
        if (strongholds.size() > 5) {
            message.addActionRow(strongholds.subList(0, 5));
            message.addActionRow(strongholds.subList(5, strongholds.size()));
        } else {
            message.addActionRow(strongholds);
        }

        message.addActionRow(Button.secondary("reset-shipment" + buttonSuffix, "Start over"),
                Button.danger("pass-shipment" + buttonSuffix, fremenRide ? "No ride" : "Pass Shipment"));

        discordGame.queueMessage(message);
        discordGame.queueDeleteMessage();
    }

    private static Button shipToTerritoryButton(Game game, Faction faction, String buttonSuffix, String territoryName) {
        String labelSuffix = "-" + territoryName;
        Button button = Button.primary("ship" + buttonSuffix + labelSuffix, territoryName);
        List<Territory> sectors = game.getTerritories().values().stream().filter(s -> s.getTerritoryName().startsWith(territoryName)).toList();
        return button.withDisabled(sectors.stream().anyMatch(s -> s.factionMayNotEnter(game, faction, !buttonSuffix.contains("fremen-ride"))));
    }

    private static void passShipment(ButtonInteractionEvent event, Game game, DiscordGame discordGame) throws ChannelNotFoundException {
        Faction faction = ButtonManager.getButtonPresser(event, game);
        String buttonSuffix = event.getComponentId().replace("pass-shipment", "");
        switch (buttonSuffix) {
            case "-fremen-ride" -> {
                game.getTurnSummary().publish(faction.getEmoji() + " does not ride the worm.");
                faction.getMovement().clear();
                ((FremenFaction) faction).setWormRideActive(false);
                discordGame.queueMessage("You will not ride the worm.");
                deleteShipMoveButtonsInChannel(event.getMessageChannel());
            }
            case "-place-shai-hulud", "-place-great-maker" -> {
                String wormName = buttonSuffix.equals("-place-shai-hulud") ? "Shai-Hulud" : "Great Maker";
                discordGame.queueMessage("You will keep " + wormName + " in " + faction.getMovement().getMovingFrom() + ".");
                game.placeShaiHulud(faction.getMovement().getMovingFrom(), wormName, false);
                ((FremenFaction) game.getFaction("Fremen")).wormWasPlaced();
                deleteShipMoveButtonsInChannel(event.getMessageChannel());
            }
            case "-guild-ambassador" -> {
                discordGame.queueMessage("You will not ship with your Guild ambassador.");
                game.getTurnSummary().publish(Emojis.ECAZ + " does not ship with their Guild ambassador.");
                faction.getShipment().clear();
            }
            default -> {
                game.getTurnSummary().publish(faction.getEmoji() + " does not ship.");
                faction.resetAllySpiceSupportAfterShipping(game);
                faction.getShipment().clear();
                deleteShipMoveButtonsInChannel(event.getMessageChannel());
                queueMovementButtons(game, faction, discordGame);
            }
        }
        discordGame.pushGame();
    }

    public static void queueShippingButtons(ButtonInteractionEvent event, Game game, DiscordGame discordGame) throws ChannelNotFoundException {
        queueShippingButtons(event, game, discordGame, false);
    }

    public static void queueShippingButtons(ButtonInteractionEvent event, Game game, DiscordGame discordGame, boolean fremenRide) throws ChannelNotFoundException {
        Faction faction = ButtonManager.getButtonPresser(event, game);
        String buttonSuffix = fremenRide ? "-fremen-ride" : "";
        List<Button> buttons = new LinkedList<>();
        buttons.add(Button.primary("stronghold" + buttonSuffix, "Stronghold"));
        buttons.add(Button.primary("spice-blow" + buttonSuffix, "Spice Blow Territories"));
        buttons.add(Button.primary("rock" + buttonSuffix, "Rock Territories"));
        if (game.hasGameOption(GameOption.HOMEWORLDS) && !fremenRide)
            buttons.add(Button.primary("homeworlds", "Homeworlds"));
        boolean revealedDiscoveryTokenOnMap = game.getTerritories().values().stream().anyMatch(Territory::isDiscovered);
        if (game.hasGameOption(GameOption.DISCOVERY_TOKENS) && revealedDiscoveryTokenOnMap) buttons.add(Button.primary("discovery-tokens" + buttonSuffix, "Discovery Tokens"));
        buttons.add(Button.primary("other" + buttonSuffix, "Somewhere else"));
        buttons.add(Button.danger("pass-shipment" + buttonSuffix, fremenRide ? "No ride" : "I don't want to ship."));

        MessageCreateBuilder messageCreateBuilder = new MessageCreateBuilder()
                .setContent(fremenRide ? "Where would you like to ride to?" : "What part of Arrakis would you like to ship to?");
        int i = 0;
        while (i + 5 < buttons.size()) {
            messageCreateBuilder.addActionRow(buttons.subList(i, i + 5));
            i += 5;
        }
        if (i < buttons.size()) {
            messageCreateBuilder.addActionRow(buttons.subList(i, buttons.size()));
        }
        discordGame.queueMessage(messageCreateBuilder);

        if (faction instanceof GuildFaction && faction.getShipment().getCrossShipFrom().isEmpty()) {
            List<Button> guildButtons = new LinkedList<>();
            guildButtons.add(Button.primary("guild-cross-ship", "Cross ship"));
            guildButtons.add(Button.primary("guild-ship-to-reserves", "Ship to reserves"));
            discordGame.getGuildChat().queueMessage("Special options for " + Emojis.GUILD + ":", guildButtons);
        }
        if (faction.getAlly().equals("Guild") && faction.getShipment().getCrossShipFrom().isEmpty()) {
            List<Button> guildButtons = new LinkedList<>();
            guildButtons.add(Button.primary("guild-cross-ship", "Cross ship"));
            discordGame.getFactionChat(faction).queueMessage("Special option for " + Emojis.GUILD + " ally :", guildButtons);
        }

        discordGame.queueDeleteMessage();
    }

    private static void queueMovementButtons(Game game, Faction faction, DiscordGame discordGame) {
        String message = "Use the following buttons to perform your move.";
        String mustMoveOutOf = faction.getMovement().getMustMoveOutOf();
        if (mustMoveOutOf != null && !mustMoveOutOf.isEmpty()) {
            message += "\nYou must move out of " + mustMoveOutOf;
            if (faction.hasTreacheryCard("Hajr"))
                message += "\nIf you need to move into " + mustMoveOutOf + " and then move out with Hajr, please ask the mod for help.";
            if (faction.hasTreacheryCard("Ornithopter"))
                message += "\nIf you need to move into " + mustMoveOutOf + " and then move out with Ornithopter, please ask the mod for help.";
        }

        TreeSet<Button> movingFromButtons = new TreeSet<>(Comparator.comparing(Button::getLabel));

        for (Territory territory : game.getTerritories().values()) {
            String territoryName = territory.getTerritoryName();
            if (territory instanceof HomeworldTerritory &&
                    (!(faction instanceof EmperorFaction) || !territoryName.equals("Kaitain") && !territoryName.equals("Salusa Secundus")))
                continue;
            int count = territory.getTotalForceCount(faction);
            if (game.hasGameOption(GameOption.HOMEWORLDS) && territory.hasRicheseNoField() && faction instanceof RicheseFaction && !faction.isHighThreshold())
                count--;
            if (count > 0) {
                boolean disabled = false;
                if (mustMoveOutOf != null && !mustMoveOutOf.isEmpty())
                    disabled = !territoryName.equals(mustMoveOutOf);
                movingFromButtons.add(Button.primary("moving-from-" + territoryName, territoryName).withDisabled(disabled));
                if (faction.hasTreacheryCard("Ornithopter"))
                    movingFromButtons.add(Button.primary("ornithopter-moving-from-" + territoryName, territoryName + " (use Ornithopter)").withDisabled(disabled));
                if (faction.hasOrnithoperToken())
                    movingFromButtons.add(Button.primary("ornithopter-token-moving-from-" + territoryName, territoryName + " (use Ornithopter Token)").withDisabled(disabled));
            }
        }
        movingFromButtons.add(Button.danger("pass-movement", "No move"));

        arrangeButtonsAndSend(message, movingFromButtons, discordGame);
    }

    public static void arrangeButtonsAndSend(String message, TreeSet<Button> buttons, DiscordGame discordGame) {
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
        Faction faction = game.getFaction(factionName);
        if (faction.getReservesStrength() == 0 && faction.getSpecialReservesStrength() == 0 && !(faction instanceof RicheseFaction) && !faction.getAlly().equals("Richese") && !(faction instanceof GuildFaction) && !faction.getAlly().equals("Guild")) {
            List<Button> passButton = List.of(Button.danger("pass-shipment", "Pass shipment"));
            discordGame.getFactionChat(factionName).queueMessage("You have no troops in reserves to ship.", passButton);
            return;
        }
        List<Button> buttons = new LinkedList<>();
        buttons.add(Button.primary("shipment", "Begin a ship action"));
        buttons.add(Button.danger("pass-shipment", "Pass shipment"));
        boolean lastFaction = game.getTurnOrder().size() == 1 && !isGuildNeedsToShip(game);
        if (faction.getShipment().isMayPlaySapho())
            buttons.add(Button.secondary("juice-of-sapho-last", "Play Juice of Sapho to go last").withDisabled(lastFaction));
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
        discordGame.getGuildChat().queueMessage("Use buttons to take your turn out of order. "
                + game.getFaction("Guild").getPlayer(), buttons);
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