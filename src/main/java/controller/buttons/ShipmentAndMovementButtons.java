package controller.buttons;

import constants.Emojis;
import controller.channels.TurnSummary;
import controller.commands.RunCommands;
import controller.commands.SetupCommands;
import enums.GameOption;
import enums.MoveType;
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
import java.util.stream.Collectors;

public class ShipmentAndMovementButtons implements Pressable {


    public static void press(ButtonInteractionEvent event, Game game, DiscordGame discordGame) throws ChannelNotFoundException, IOException, InvalidGameStateException {

        if (event.getComponentId().startsWith("ship-sector-")) filterBySector(event, game, discordGame, true);
        else if (event.getComponentId().startsWith("ship-to-reserves-")) {
            queueForcesButtons(event, game, discordGame, game.getGuildFaction(), true, false, false, false, false);
            discordGame.pushGame();
        }
        else if (event.getComponentId().startsWith("cross-ship-from-")) setCrossShipFrom(event, game, discordGame);
        else if (event.getComponentId().startsWith("stronghold")) queueStrongholdShippingButtons(event, game, discordGame);
        else if (event.getComponentId().startsWith("spice-blow")) queueSpiceBlowShippingButtons(event, discordGame, game);
        else if (event.getComponentId().startsWith("rock")) queueRockShippingButtons(event, discordGame, game);
        else if (event.getComponentId().startsWith("homeworlds")) queueHomeworldShippingButtons(event, game, discordGame);
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
        else if (event.getComponentId().startsWith("execute-movement-fremen-ride")) executeFremenRide(event, game, discordGame);
        else if (event.getComponentId().startsWith("execute-movement-enter-discovery-token")) executeEnterDiscoveryToken(event, game, discordGame);
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
        else if (event.getComponentId().startsWith("guild-defer-to-")) guildDeferTo(event, game, discordGame);
        switch (event.getComponentId()) {
            case "shipment" -> queueShippingButtons(event, game, discordGame);
            case "karama-execute-shipment" -> karamaExecuteShipment(event, game, discordGame);
            case "juice-of-sapho-first" -> playJuiceOfSapho(event, game, discordGame, false);
            case "juice-of-sapho-last" -> playJuiceOfSapho(event, game, discordGame, true);
            case "juice-of-sapho-don't-play" -> juiceOfSaphoDontPlay(event, game, discordGame);
            case "guild-take-turn" -> guildTakeTurn(game, discordGame);
            case "guild-wait-last" -> guildWaitLast(game, discordGame);
            case "guild-defer" -> guildDefer(game, discordGame);
            case "guild-select" -> guildSelect(game, discordGame);
            case "richese-no-field-move" -> richeseNoFieldMove(event, game, discordGame);
            case "guild-cross-ship" -> crossShip(event, game, discordGame);
            case "guild-ship-to-reserves" -> shipToReserves(game, discordGame);
            case "hajr" -> hajr(event, game, discordGame, true);
            case "Ornithopter" -> hajr(event, game, discordGame, false);
        }
    }

    public static void deleteButtonsInChannelWithPrefix(MessageChannel channel, String choicePrefix) {
        List<Message> messages = channel.getHistoryAround(channel.getLatestMessageId(), 100).complete().getRetrievedHistory();
        List<Message> messagesToDelete = new ArrayList<>();
        for (Message message : messages) {
            List<Button> buttons = message.getButtons();
            for (Button button : buttons) {
                String id = button.getId();
                if (id != null && (id.startsWith(choicePrefix))) {
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
                        id.startsWith("reset-shipping-forces") ||
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

    private static void karamaExecuteShipment(ButtonInteractionEvent event, Game game, DiscordGame discordGame) throws ChannelNotFoundException, InvalidGameStateException, IOException {
        Faction faction = ButtonManager.getButtonPresser(event, game);
        faction.discard("Karama", "to ship at " + Emojis.GUILD + " rates");
        executeShipment(event, game, discordGame, true);
    }

    private static void ornithopterMovement(ButtonInteractionEvent event, Game game, DiscordGame discordGame, boolean isToken) throws ChannelNotFoundException {
        Faction faction = ButtonManager.getButtonPresser(event, game);
        if (isToken) {
            faction.setOrnithoperToken(false);
            discordGame.getTurnSummary().queueMessage(faction.getEmoji() + " use Ornithopter Discovery Token to move 3 spaces with their move.");

        } else {
            faction.discard("Ornithopter", "to move 3 spaces with their move");
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

    private static void hajr(ButtonInteractionEvent event, Game game, DiscordGame discordGame, boolean hajr) throws ChannelNotFoundException, InvalidGameStateException {
        Faction faction = ButtonManager.getButtonPresser(event, game);
        faction.discard(hajr ? "Hajr" : "Ornithopter", "to move again");
        faction.executeMovement();
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
        game.getGuildFaction().getShipment().setToReserves(false);
        discordGame.pushGame();
    }

    private static void shipToReserves(Game game, DiscordGame discordGame) throws ChannelNotFoundException {
        TreeSet<Button> buttons = new TreeSet<>(getButtonComparator());
        for (Territory territory : game.getTerritories().values()) {
            if (territory.hasForce("Guild"))
                buttons.add(Button.primary("ship-to-reserves-" + territory.getTerritoryName(), territory.getTerritoryName()));
        }
        arrangeButtonsAndSend("Where would you like to ship to reserves from?", buttons, discordGame);
        game.getGuildFaction().getShipment().setToReserves(true);
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
        game.getGuildFaction().getShipment().setToReserves(true);
        discordGame.pushGame();
    }

    private static void guildDefer(Game game, DiscordGame discordGame) throws ChannelNotFoundException, InvalidGameStateException {
        game.guildDefer();
        discordGame.pushGame();
        discordGame.queueDeleteMessage();
    }

    private static void guildDeferTo(ButtonInteractionEvent event, Game game, DiscordGame discordGame) throws ChannelNotFoundException, InvalidGameStateException {
        String factionToDeferTo = event.getComponentId().replace("guild-defer-to-", "");
        game.guildDeferUntilAfter(factionToDeferTo);
        discordGame.pushGame();
        discordGame.queueDeleteMessage();
    }

    private static void guildSelect(Game game, DiscordGame discordGame) {
        game.promptGuildToSelectFactionToDeferTo();
        discordGame.queueDeleteMessage();
    }

    private static void guildWaitLast(Game game, DiscordGame discordGame) throws ChannelNotFoundException {
        game.guildWaitLast();
        discordGame.pushGame();
        discordGame.queueDeleteMessage();
    }

    private static void guildTakeTurn(Game game, DiscordGame discordGame) throws ChannelNotFoundException {
        game.guildTakeTurn();
        discordGame.queueDeleteMessage();
        discordGame.pushGame();
    }

    private static void juiceOfSaphoDontPlay(ButtonInteractionEvent event, Game game, DiscordGame discordGame) throws ChannelNotFoundException, InvalidGameStateException {
        Faction faction = ButtonManager.getButtonPresser(event, game);
        discordGame.queueMessage("You will not play Juice of Sapho to go first.");
        game.juiceOfSaphoDontPlay(faction);
        discordGame.pushGame();
        discordGame.queueDeleteMessage();
    }

    private static void playJuiceOfSapho(ButtonInteractionEvent event, Game game, DiscordGame discordGame, boolean last) throws ChannelNotFoundException, InvalidGameStateException {
        Faction faction = ButtonManager.getButtonPresser(event, game);
        game.playJuiceOfSapho(faction, last);
        discordGame.queueMessage("You will go " + (last ? "last" : "first") + " this turn.");
        discordGame.pushGame();
        discordGame.queueDeleteMessage();
    }

    private static void passMovement(ButtonInteractionEvent event, Game game, DiscordGame discordGame) throws ChannelNotFoundException, InvalidGameStateException, IOException {
        Faction faction = ButtonManager.getButtonPresser(event, game);
        TurnSummary turnSummary = discordGame.getTurnSummary();
        if (event.getComponentId().contains("-enter-discovery-token")) {
            discordGame.queueMessage("You will not enter the Discovery Token.");
            turnSummary.queueMessage(faction.getEmoji() + " does not enter the Discovery Token.");
        } else {
            discordGame.queueMessage("Shipment and movement complete.");
            turnSummary.queueMessage(faction.getEmoji() + " does not move.");
            game.completeCurrentFactionMovement();
            if (game.allFactionsHaveMoved()) {
                RunCommands.advance(discordGame, game);
                discordGame.getModInfo().queueMessage("Everyone has taken their turn. Game is auto-advancing to battle phase.");
                return;
            }
        }

        discordGame.pushGame();
        deleteShipMoveButtonsInChannel(event.getMessageChannel());
    }

    private static void executeMovement(ButtonInteractionEvent event, Game game, DiscordGame discordGame) throws ChannelNotFoundException, InvalidGameStateException, IOException {
        Faction faction = ButtonManager.getButtonPresser(event, game);
        faction.executeMovement();
        game.completeCurrentFactionMovement();
        discordGame.queueMessage("Shipment and movement complete.");
        if (game.allFactionsHaveMoved()) {
            RunCommands.advance(discordGame, game);
            discordGame.getModInfo().queueMessage("Everyone has taken their turn. Game is auto-advancing to battle phase.");
            return;
        }
        deleteShipMoveButtonsInChannel(event.getMessageChannel());
        discordGame.pushGame();
    }

    private static void executeFremenRide(ButtonInteractionEvent event, Game game, DiscordGame discordGame) throws ChannelNotFoundException, InvalidGameStateException {
        Faction faction = ButtonManager.getButtonPresser(event, game);
        faction.executeMovement();
        if (faction instanceof EcazFaction) {
            discordGame.queueMessage("Movement with Fremen ambasssador complete.");
        } else if (faction instanceof FremenFaction fremen) {
            discordGame.queueMessage("Fremen ride complete.");
            fremen.setWormRideActive(false);
        }
        deleteShipMoveButtonsInChannel(event.getMessageChannel());
        discordGame.pushGame();
    }

    protected static void executeFremenAmbassador(ButtonInteractionEvent event, Game game, DiscordGame discordGame) throws ChannelNotFoundException, InvalidGameStateException {
        Faction faction = ButtonManager.getButtonPresser(event, game);
        faction.executeMovement();
        faction.getChat().reply("Movement with Fremen Ambassador complete.");
        deleteButtonsInChannelWithPrefix(event.getMessageChannel(), faction.getMovement().getChoicePrefix());
        faction.getMovement().setMoveType(MoveType.TBD);
        discordGame.pushGame();
    }

    private static void executeEnterDiscoveryToken(ButtonInteractionEvent event, Game game, DiscordGame discordGame) throws ChannelNotFoundException, InvalidGameStateException {
        Faction faction = ButtonManager.getButtonPresser(event, game);
        faction.executeMovement();
        discordGame.queueMessage("Movement into Discovery Token complete.");
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
        return button.withDisabled(sectors.stream().anyMatch(s -> s.factionMayNotEnter(game, faction, false, false)));
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

    private static void executeShipment(ButtonInteractionEvent event, Game game, DiscordGame discordGame, boolean karama) throws ChannelNotFoundException, InvalidGameStateException, IOException {
        Faction faction = ButtonManager.getButtonPresser(event, game);
        boolean guildAmbassador = event.getComponentId().contains("-guild-ambassador");
        boolean btHTPlacement = event.getComponentId().contains("-bt-ht");
        boolean startingForces = event.getComponentId().contains("-starting-forces");
        boolean hmsPlacement = event.getComponentId().contains("-hms-placement");
        if (startingForces) {
            if (faction.placeChosenStartingForces())
                SetupCommands.advance(event.getGuild(), discordGame, game);
            else
                discordGame.pushGame();
        } else if (hmsPlacement) {
            ((IxFaction) faction).placeHMS();
            SetupCommands.advance(event.getGuild(), discordGame, game);
        } else {
            faction.executeShipment(game, karama, guildAmbassador || btHTPlacement);
            if (guildAmbassador) {
                discordGame.queueMessage("Shipment with Guild ambassador complete.");
            } else if (btHTPlacement) {
                BTFaction bt = game.getBTFaction();
                discordGame.queueMessage("Placement of " + bt.getNumFreeRevivals() + " free revivals complete.");
                bt.setBtHTActive(false);
            } else {
                discordGame.queueMessage("Shipment complete.");
                queueMovementButtons(game, faction, discordGame);
            }
            discordGame.pushGame();
        }
        deleteShipMoveButtonsInChannel(event.getMessageChannel());
    }

    protected static void executeShipmentWithPrefixes(ButtonInteractionEvent event, Game game, DiscordGame discordGame, boolean karama) throws ChannelNotFoundException, InvalidGameStateException, IOException {
        Faction faction = ButtonManager.getButtonPresser(event, game);
        boolean guildAmbassador = faction.getMovement().getMoveType() == MoveType.GUILD_AMBASSADOR;
        boolean fremenAmbassador = faction.getMovement().getMoveType() == MoveType.FREMEN_AMBASSADOR;
        boolean btHTPlacement = faction.getMovement().getMoveType() == MoveType.BT_HT;
        boolean startingForces = faction.getMovement().getMoveType() == MoveType.STARTING_FORCES;
        boolean hmsPlacement = faction.getMovement().getMoveType() == MoveType.HMS_PLACEMENT;
        String choicePrefix = faction.getMovement().getChoicePrefix();
        if (startingForces) {
            if (faction.placeChosenStartingForces())
                SetupCommands.advance(event.getGuild(), discordGame, game);
            else
                discordGame.pushGame();
        } else if (hmsPlacement) {
            ((IxFaction) faction).placeHMS();
            SetupCommands.advance(event.getGuild(), discordGame, game);
        } else if (fremenAmbassador) {
            faction.executeMovement();
            faction.getChat().publish("Movement with Fremen ambasssador complete.");
            discordGame.pushGame();
        } else {
            faction.executeShipment(game, karama, guildAmbassador || btHTPlacement);
            if (guildAmbassador) {
                faction.getChat().reply("Shipment with Guild Ambassador complete.");
            } else if (btHTPlacement) {
                BTFaction bt = game.getBTFaction();
                discordGame.queueMessage("Placement of " + bt.getNumFreeRevivals() + " free revivals complete.");
                bt.setBtHTActive(false);
            } else {
                discordGame.queueMessage("Shipment complete.");
                queueMovementButtons(game, faction, discordGame);
            }
            discordGame.pushGame();
        }
        deleteButtonsInChannelWithPrefix(event.getMessageChannel(), choicePrefix);
        faction.getMovement().setMoveType(MoveType.TBD);
    }

    private static void resetShipmentMovement(ButtonInteractionEvent event, Game game, DiscordGame discordGame, boolean isShipment) throws ChannelNotFoundException, InvalidGameStateException {
        Faction faction = ButtonManager.getButtonPresser(event, game);
        if (isShipment) {
            boolean fremenRide = event.getComponentId().contains("-fremen-ride");
            boolean shaiHuludPlacement = event.getComponentId().contains("-place-shai-hulud");
            boolean greatMakerPlacement = event.getComponentId().contains("-place-great-maker");
            boolean guildAmbassador = event.getComponentId().contains("-guild-ambassador");
            boolean btHTPlacement = event.getComponentId().contains("-bt-ht");
            boolean startingForces = event.getComponentId().contains("-starting-forces");
            boolean hmsPlacement = event.getComponentId().contains("-hms-placement");
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
            } else if (startingForces) {
                faction.getShipment().clear();
                discordGame.queueMessage("Starting over");
                faction.presentStartingForcesChoices();
            } else if (hmsPlacement) {
                ((IxFaction) faction).presentHMSPlacementChoices();
            } else {
                faction.getShipment().clear();
                faction.getShipment().setShipped(false);
                if (guildAmbassador) {
                    discordGame.queueMessage("Starting over");
                    ((EcazFaction) faction).presentGuildAmbassadorDestinationChoices();
                } else if (btHTPlacement) {
                    discordGame.queueMessage("Starting over");
                    ((BTFaction) faction).presentHTChoices();
                } else
                    discordGame.queueMessage("Starting over");
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
        boolean startingForces = event.getComponentId().contains("-starting-forces");
        Faction faction = ButtonManager.getButtonPresser(event, game);
        if (isShipment && !fremenRide) {
            faction.getShipment().setForce(0);
            faction.getShipment().setSpecialForce(0);
            faction.getShipment().setNoField(-1);
            queueForcesButtons(event, game, discordGame, faction, true, false, guildAmbassador, false, false, startingForces);
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

    protected static void resetForcesWithPrefix(ButtonInteractionEvent event, Game game, DiscordGame discordGame, boolean isShipment) throws ChannelNotFoundException {
        Faction faction = ButtonManager.getButtonPresser(event, game);
        boolean fremenRide = faction.getMovement().getMoveType() == MoveType.FREMEN_RIDE;
        boolean fremenAmbassador = faction.getMovement().getMoveType() == MoveType.FREMEN_AMBASSADOR;
        boolean enterDiscoveryToken = faction.getMovement().getMoveType() == MoveType.ENTER_DISCOVERY_TOKEN;
        boolean startingForces = faction.getMovement().getMoveType() == MoveType.STARTING_FORCES;
        if (isShipment && !fremenRide && !fremenAmbassador) {
            faction.getShipment().setForce(0);
            faction.getShipment().setSpecialForce(0);
            faction.getShipment().setNoField(-1);
            presentForcesChoices(event, game, discordGame, faction, true, false, false, false, startingForces);
        } else {
            faction.getMovement().setForce(0);
            faction.getMovement().setSpecialForce(0);
            faction.getMovement().setSecondForce(0);
            faction.getMovement().setSecondSpecialForce(0);
            faction.getMovement().setSecondMovingFrom("");
            faction.getMovement().setMovingNoField(false);
            presentForcesChoices(event, game, discordGame, faction, false, false, fremenRide, enterDiscoveryToken, false);
        }
        discordGame.pushGame();
    }

    private static void addForces(ButtonInteractionEvent event, Game game, DiscordGame discordGame, boolean isShipment) throws ChannelNotFoundException {
        boolean fremenRide = event.getComponentId().contains("-fremen-ride");
        boolean guildAmbassador = event.getComponentId().contains("-guild-ambassador");
        boolean enterDiscoveryToken = event.getComponentId().contains("-enter-discovery-token");
        boolean startingForces = event.getComponentId().contains("-starting-forces");
        Faction faction = ButtonManager.getButtonPresser(event, game);
        if (isShipment)
            faction.getShipment().setForce((faction.getShipment().getForce() + Integer.parseInt(event.getComponentId().replace("add-force-shipment-", "").replace("guild-ambassador-", "").replace("starting-forces-", ""))));
        else
            faction.getMovement().setForce((faction.getMovement().getForce() + Integer.parseInt(event.getComponentId().replace("add-force-movement-", "").replace("fremen-ride-", "").replace("enter-discovery-token-", ""))));
        queueForcesButtons(event, game, discordGame, faction, isShipment, false, guildAmbassador, fremenRide, enterDiscoveryToken, startingForces);
        discordGame.pushGame();
    }

    protected static void presentAddForcesChoices(ButtonInteractionEvent event, Game game, DiscordGame discordGame, boolean isShipment) throws ChannelNotFoundException {
        Faction faction = ButtonManager.getButtonPresser(event, game);
        boolean fremenRide = faction.getMovement().getMoveType() == MoveType.FREMEN_RIDE;
        boolean enterDiscoveryToken = faction.getMovement().getMoveType() == MoveType.ENTER_DISCOVERY_TOKEN;
        boolean startingForces = faction.getMovement().getMoveType() == MoveType.STARTING_FORCES;
        String choicePrefix = faction.getMovement().getChoicePrefix();
        if (isShipment)
            faction.getShipment().setForce((faction.getShipment().getForce() + Integer.parseInt(event.getComponentId().replace("add-force-shipment-", "").replace(choicePrefix, ""))));
        else
            faction.getMovement().setForce((faction.getMovement().getForce() + Integer.parseInt(event.getComponentId().replace("add-force-movement-", "").replace(choicePrefix, ""))));
        presentForcesChoices(event, game, discordGame, faction, isShipment, false, fremenRide, enterDiscoveryToken, startingForces);
        discordGame.pushGame();
    }

    private static void addSpecialForces(ButtonInteractionEvent event, Game game, DiscordGame discordGame, boolean isShipment) throws ChannelNotFoundException {
        boolean fremenRide = event.getComponentId().contains("-fremen-ride");
        boolean enterDiscoveryToken = event.getComponentId().contains("-enter-discovery-token");
        boolean startingForces = event.getComponentId().contains("-starting-forces");
        Faction faction = ButtonManager.getButtonPresser(event, game);
        if (isShipment)
            faction.getShipment().setSpecialForce((faction.getShipment().getSpecialForce() + Integer.parseInt(event.getComponentId().replace("add-special-force-shipment-", "").replace("starting-forces-", ""))));
        else
            faction.getMovement().setSpecialForce((faction.getMovement().getSpecialForce() + Integer.parseInt(event.getComponentId().replace("add-special-force-movement-", "").replace("fremen-ride-", "").replace("enter-discovery-token-", ""))));
        queueForcesButtons(event, game, discordGame, faction, isShipment, false, false, fremenRide, enterDiscoveryToken, startingForces);
        discordGame.pushGame();
    }

    private static void filterBySector(ButtonInteractionEvent event, Game game, DiscordGame discordGame, boolean isShipment) throws ChannelNotFoundException, InvalidGameStateException {
        boolean fremenRide = event.getComponentId().contains("-fremen-ride");
        boolean shaiHuludPlacement = event.getComponentId().contains("-place-shai-hulud");
        boolean greatMakerPlacement = event.getComponentId().contains("-place-great-maker");
        boolean guildAmbassador = event.getComponentId().contains("-guild-ambassador");
        boolean btHTPlacement = event.getComponentId().contains("-bt-ht");
        boolean startingForces = event.getComponentId().contains("-starting-forces");
        boolean hmsPlacement = event.getComponentId().contains("-hms-placement");
        boolean enterDiscoveryToken = event.getComponentId().contains("-enter-discovery-token");
        String shipmentOrMovement = isShipment ? "ship-" : "move-";
        Faction faction = ButtonManager.getButtonPresser(event, game);
        Territory territory = game.getTerritories().values().stream().filter(t -> t.getTerritoryName().contains(
                event.getComponentId().replace(shipmentOrMovement + "sector-", "")
                        .replace("fremen-ride-", "")
                        .replace("place-shai-hulud-", "")
                        .replace("place-great-maker-", "")
                        .replace("guild-ambassador-", "")
                        .replace("bt-ht-", "")
                        .replace("starting-forces-", "")
                        .replace("hms-placement-", "")
                        .replace("enter-discovery-token-", "")
                        .replace("-", " ")
                )
        ).findFirst().orElseThrow();

        if (shaiHuludPlacement || greatMakerPlacement) {
            game.getFremenFaction().placeWorm(greatMakerPlacement, territory, false);
            discordGame.pushGame();
            return;
        } else if (btHTPlacement) {
            faction.getShipment().setTerritoryName(territory.getTerritoryName());
            BTFaction bt = game.getBTFaction();
            faction.getShipment().setForce(bt.getNumFreeRevivals());
            MessageCreateBuilder messageCreateBuilder = new MessageCreateBuilder()
                    .addContent("Currently sending your free revivals to " + territory.getTerritoryName() + "."
                    ).addActionRow(
                            Button.success("execute-shipment-bt-ht", "Confirm"),
                            Button.danger("reset-shipment-bt-ht", "Start over")
                    );
            discordGame.queueMessage(messageCreateBuilder);
            discordGame.pushGame();
            return;
        } else if (startingForces) {
            faction.getShipment().setTerritoryName(territory.getTerritoryName());
            if (faction instanceof FremenFaction)
                queueForcesButtons(event, game, discordGame, faction, true, false, false, false, false, true);
            else
                faction.presentStartingForcesExecutionChoices();
            discordGame.pushGame();
            return;
        } else if (hmsPlacement) {
            faction.getShipment().setTerritoryName(territory.getTerritoryName());
            ((IxFaction) faction).presentHMSPlacementExecutionChoices();
            discordGame.pushGame();
            return;
        } else if (isShipment && !fremenRide)
            faction.getShipment().setTerritoryName(territory.getTerritoryName());
        else
            faction.getMovement().setMovingTo(territory.getTerritoryName());

        queueForcesButtons(event, game, discordGame, faction, isShipment, false, guildAmbassador, fremenRide, enterDiscoveryToken);
        discordGame.pushGame();
    }

    protected static void filterBySectorWithPrefix(ButtonInteractionEvent event, Game game, DiscordGame discordGame, boolean isShipment) throws ChannelNotFoundException, InvalidGameStateException {
        Faction faction = ButtonManager.getButtonPresser(event, game);
        boolean fremenRide = faction.getMovement().getMoveType() == MoveType.FREMEN_RIDE;
        boolean shaiHuludPlacement = faction.getMovement().getMoveType() == MoveType.SHAI_HULUD_PLACEMENT;
        boolean greatMakerPlacement = faction.getMovement().getMoveType() == MoveType.GREAT_MAKER_PLACEMENT;
        boolean fremenAmbassador = faction.getMovement().getMoveType() == MoveType.FREMEN_AMBASSADOR;
        boolean btHTPlacement = faction.getMovement().getMoveType() == MoveType.BT_HT;
        boolean startingForces = faction.getMovement().getMoveType() == MoveType.STARTING_FORCES;
        boolean hmsPlacement = faction.getMovement().getMoveType() == MoveType.HMS_PLACEMENT;
        boolean enterDiscoveryToken = faction.getMovement().getMoveType() == MoveType.ENTER_DISCOVERY_TOKEN;
        String shipmentOrMovement = isShipment ? "ship-" : "move-";
        String choicePrefix = faction.getMovement().getChoicePrefix();
        Territory territory = game.getTerritories().values().stream().filter(t -> t.getTerritoryName().contains(
                        event.getComponentId().replace(shipmentOrMovement + "sector-", "")
                                .replace(choicePrefix, "")
                                .replace("-", " ")
                )
        ).findFirst().orElseThrow();

        if (shaiHuludPlacement || greatMakerPlacement) {
            game.getFremenFaction().placeWorm(greatMakerPlacement, territory, false);
            discordGame.pushGame();
            return;
        } else if (btHTPlacement) {
            faction.getShipment().setTerritoryName(territory.getTerritoryName());
            BTFaction bt = game.getBTFaction();
            faction.getShipment().setForce(bt.getNumFreeRevivals());
            MessageCreateBuilder messageCreateBuilder = new MessageCreateBuilder()
                    .addContent("Currently sending your free revivals to " + territory.getTerritoryName() + "."
                    ).addActionRow(
                            Button.success("bt-ht-execute-shipment", "Confirm"),
                            Button.danger("bt-ht-reset-shipment", "Start over")
                    );
            discordGame.queueMessage(messageCreateBuilder);
            discordGame.pushGame();
            return;
        } else if (startingForces) {
            faction.getShipment().setTerritoryName(territory.getTerritoryName());
            if (faction instanceof FremenFaction)
                presentForcesChoices(event, game, discordGame, faction, true, false, false, false, true);
            else
                faction.presentStartingForcesExecutionChoices();
            discordGame.pushGame();
            return;
        } else if (hmsPlacement) {
            faction.getShipment().setTerritoryName(territory.getTerritoryName());
            ((IxFaction) faction).presentHMSPlacementExecutionChoices();
            discordGame.pushGame();
            return;
        } else if (isShipment && !fremenRide && !fremenAmbassador)
            faction.getShipment().setTerritoryName(territory.getTerritoryName());
        else
            faction.getMovement().setMovingTo(territory.getTerritoryName());

        presentForcesChoices(event, game, discordGame, faction, isShipment, false, fremenRide, enterDiscoveryToken, false);
        discordGame.pushGame();
    }

    private static void richeseNoFieldShip(ButtonInteractionEvent event, Game game, DiscordGame discordGame, boolean isAlly) throws ChannelNotFoundException {
        deleteShipMoveButtonsInChannel(event.getMessageChannel());
        Faction faction = game.getRicheseFaction();
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

    private static void queueForcesButtons(ButtonInteractionEvent event, Game game, DiscordGame discordGame, Faction faction, boolean isShipment, boolean hasNoField, boolean guildAmbassador, boolean fremenRide, boolean enterDiscoveryToken) {
        queueForcesButtons(event, game, discordGame, faction, isShipment, hasNoField, guildAmbassador, fremenRide, enterDiscoveryToken, false);
    }

    private static void queueForcesButtons(ButtonInteractionEvent event, Game game, DiscordGame discordGame, Faction faction, boolean isShipment, boolean hasNoField, boolean guildAmbassador, boolean fremenRide, boolean enterDiscoveryToken, boolean isStartingForces) {
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
        if (isStartingForces && faction instanceof FremenFaction fremen) {
            int forcesInShipment = fremen.getShipment().getForce() + fremen.getShipment().getSpecialForce();
            buttonLimitForces = Math.min(10 - fremen.getStartingForcesPlaced() - forcesInShipment, buttonLimitForces);
            buttonLimitSpecialForces = Math.min(10 - fremen.getStartingForcesPlaced() - forcesInShipment, buttonLimitSpecialForces);
        }

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
        if (isStartingForces)
            buttonSuffix = "-starting-forces";
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
            String noFieldMessage = faction.getShipment().getNoField() >= 0 ? "\n" + faction.getShipment().getNoField() + " " + Emojis.NO_FIELD + "\n": "";
            String currentlyShipping = "Currently ";
            currentlyShipping += isStartingForces ? "placing" : "shipping";
            currentlyShipping += ":\n**" + faction.forcesStringWithZeroes(faction.getShipment().getForce(), faction.getShipment().getSpecialForce()) + noFieldMessage + "** to " + territory +
                    (guildAmbassador || isStartingForces ? "" : " for " + spice + " " + Emojis.SPICE + "\n\nYou have " + faction.getSpice() + " " + Emojis.SPICE + " to spend.");
            String message = "Use buttons below to add forces to your ";
            message += isStartingForces ? "placement. " : "shipment. ";
            message += currentlyShipping;

            if (!forcesButtons.isEmpty()) {
                arrangeButtonsAndSend(message, forcesButtons, discordGame);
            } else {
                message = isStartingForces ? "" : "You have no troops in reserves to ship. ";
                if (faction.getShipment().getForce() != 0 || faction.getShipment().getSpecialForce() != 0 || faction.getShipment().getNoField() != 0)
                    message += currentlyShipping;
                faction.getChat().reply(message, List.of(new DuneChoice("secondary", "reset-shipping-forces" + buttonSuffix, "Reset forces")));
            }

            if (faction instanceof RicheseFaction || faction.getAlly().equals("Richese")) {
                RicheseFaction richese = game.getRicheseFaction();
                String buttonPrefix = (faction instanceof RicheseFaction) ? "richese-no-field-ship-" : "richese-ally-no-field-ship-";
                List<DuneChoice> choices = new ArrayList<>();
                List<Integer> noFields = new LinkedList<>();
                noFields.add(0);
                noFields.add(3);
                noFields.add(5);
                for (int nf : noFields) {
                    DuneChoice choice = new DuneChoice(buttonPrefix + nf, "Ship " + nf + " No-Field token.");
                    Integer invalidNoField = richese.getFrontOfShieldNoField();
                    Territory noFieldTerritory = richese.getTerritoryWithNoFieldOrNull();
                    if (noFieldTerritory != null)
                        invalidNoField = noFieldTerritory.getRicheseNoField();
                    choice.setDisabled(invalidNoField != null && nf == invalidNoField || nf == richese.getShipment().getNoField());
                    choices.add(choice);
                }
                faction.getChat().publish("No-Field options:", choices);
            }

            List<DuneChoice> choices = new ArrayList<>();
            boolean disableConfirmButton = true;
            Shipment shipment = faction.getShipment();
            if (shipment.getForce() > 0 || shipment.getSpecialForce() > 0 || hasNoField)
                disableConfirmButton = false;
            String executeSuffix = "";
            if (guildAmbassador)
                executeSuffix = "-guild-ambassador";
            else if (isStartingForces)
                executeSuffix = "-starting-forces";
            DuneChoice execute = new DuneChoice("execute-shipment" + executeSuffix, "Confirm Shipment");
            execute.setDisabled(disableConfirmButton);

            if (!guildAmbassador && !isStartingForces && faction.getShipment().hasShipped())
                execute.setDisabled(true);
            choices.add(execute);
            choices.add(new DuneChoice("secondary", "reset-shipping-forces" + buttonSuffix, "Reset forces"));
            choices.add(new DuneChoice("secondary", "reset-shipment" + buttonSuffix, "Start over"));
            if (!guildAmbassador && !enterDiscoveryToken && faction.hasTreacheryCard("Karama")) {
                DuneChoice choice = new DuneChoice("secondary", "karama-execute-shipment", "Confirm Shipment (Use Karama for Guild rates)");
                choice.setDisabled(disableConfirmButton);
                choices.add(choice);
            }
            faction.getChat().publish("Finalize or start over:", choices);
        } else {
            Movement movement = faction.getMovement();
            if (faction instanceof RicheseFaction && game.getTerritories().get(faction.getMovement().getMovingFrom()).hasRicheseNoField() && !faction.getMovement().isMovingNoField())
                forcesButtons.add(Button.primary("richese-no-field-move", "+1 No-Field token")
                        .withDisabled(game.hasGameOption(GameOption.HOMEWORLDS) && !faction.isHighThreshold()));
            String noField = faction.getMovement().isMovingNoField() ? "\n" + game.getTerritory(faction.getMovement().getMovingFrom()).getRicheseNoField() + " No-Field token" : "";

            String message = "Use buttons below to add forces to your ";
            message += fremenRide ? "ride." : "movement.";
            message += " Currently moving:\n**" + faction.forcesStringWithZeroes(faction.getMovement().getForce(), faction.getMovement().getSpecialForce()) + noField + "** to " + faction.getMovement().getMovingTo();
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
                    String planetologistMessage = "Second group can move from " + territory.getTerritoryName() + " using Planetologist ability.";
                    planetologistMessage +=
                            " Currently moving:\n**" + faction.forcesStringWithZeroes(faction.getMovement().getSecondForce(), faction.getMovement().getSecondSpecialForce()) + "** from " + secondMovingFrom + " to " + faction.getMovement().getMovingTo();
                    arrangeButtonsAndSend(planetologistMessage, secondForcesButtons, discordGame);
                }
            }
        }
    }

    private static void presentForcesChoices(ButtonInteractionEvent event, Game game, DiscordGame discordGame, Faction faction, boolean isShipment, boolean hasNoField, boolean fremenRide, boolean enterDiscoveryToken, boolean isStartingForces) {
        boolean guildAmbassador = faction.getMovement().getMoveType() == MoveType.GUILD_AMBASSADOR;
        boolean fremenAmbassador = faction.getMovement().getMoveType() == MoveType.FREMEN_AMBASSADOR;
        String choicePrefix = faction.getMovement().getChoicePrefix();
        boolean wormRide = fremenRide || fremenAmbassador;
        deleteButtonsInChannelWithPrefix(event.getMessageChannel(), choicePrefix);

        TreeSet<Button> forcesButtons = new TreeSet<>(getButtonComparator());
        int buttonLimitForces = isShipment && !wormRide ? faction.getReservesStrength() - faction.getShipment().getForce() :
                game.getTerritory(faction.getMovement().getMovingFrom()).getForceStrength(faction.getName()) - faction.getMovement().getForce();
        if (guildAmbassador)
            buttonLimitForces = Math.min(4, buttonLimitForces);
        if (faction instanceof BGFaction && !isShipment && game.getTerritory(faction.getMovement().getMovingFrom()).hasForce("Advisor"))
            buttonLimitForces = game.getTerritory(faction.getMovement().getMovingFrom()).getForceStrength("Advisor");
        int buttonLimitSpecialForces = isShipment && !wormRide ? faction.getSpecialReservesStrength() - faction.getShipment().getSpecialForce() :
                game.getTerritory(faction.getMovement().getMovingFrom()).getForceStrength(faction.getName() + "*") - faction.getMovement().getSpecialForce();
        if (isStartingForces && faction instanceof FremenFaction fremen) {
            int forcesInShipment = fremen.getShipment().getForce() + fremen.getShipment().getSpecialForce();
            buttonLimitForces = Math.min(10 - fremen.getStartingForcesPlaced() - forcesInShipment, buttonLimitForces);
            buttonLimitSpecialForces = Math.min(10 - fremen.getStartingForcesPlaced() - forcesInShipment, buttonLimitSpecialForces);
        }

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

        if (wormRide)
            isShipment = false;
        String shipOrMove = isShipment ? "shipment-" : "movement-";
        for (int i = 0; i < buttonLimitForces; i++) {
            forcesButtons.add(Button.primary(choicePrefix + "add-force-" + shipOrMove + (i + 1), "Add " + (i + 1) + " troop")
                    .withDisabled(guildAmbassador && i + 1 + faction.getShipment().getForce() > 4));
        }
        for (int i = 0; i < buttonLimitSpecialForces; i++) {
            forcesButtons.add(Button.primary(choicePrefix + "add-special-force-" + shipOrMove + (i + 1), "Add " + (i + 1) + " * troop")
                    .withDisabled(guildAmbassador && i + 1 + faction.getShipment().getSpecialForce() > 4));
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
            String noFieldMessage = faction.getShipment().getNoField() >= 0 ? "\n" + faction.getShipment().getNoField() + " " + Emojis.NO_FIELD + "\n": "";
            String currentlyShipping = "Currently ";
            currentlyShipping += isStartingForces ? "placing" : "shipping";
            currentlyShipping += ":\n**" + faction.forcesStringWithZeroes(faction.getShipment().getForce(), faction.getShipment().getSpecialForce()) + noFieldMessage + "** to " + territory +
                    (guildAmbassador || isStartingForces ? "" : " for " + spice + " " + Emojis.SPICE + "\n\nYou have " + faction.getSpice() + " " + Emojis.SPICE + " to spend.");
            String message = "Use buttons below to add forces to your ";
            message += isStartingForces ? "placement. " : "shipment. ";
            message += currentlyShipping;

            if (!forcesButtons.isEmpty()) {
                arrangeButtonsAndSend(message, forcesButtons, discordGame);
            } else {
                message = isStartingForces ? "" : "You have no troops in reserves to ship. ";
                if (faction.getShipment().getForce() != 0 || faction.getShipment().getSpecialForce() != 0 || faction.getShipment().getNoField() != 0)
                    message += currentlyShipping;
                faction.getChat().reply(message, List.of(new DuneChoice("secondary", choicePrefix + "reset-shipping-forces", "Reset forces")));
            }

            if (faction instanceof RicheseFaction || faction.getAlly().equals("Richese")) {
                RicheseFaction richese = game.getRicheseFaction();
                String buttonPrefix = (faction instanceof RicheseFaction) ? "richese-no-field-ship-" : "richese-ally-no-field-ship-";
                List<DuneChoice> choices = new ArrayList<>();
                List<Integer> noFields = new LinkedList<>();
                noFields.add(0);
                noFields.add(3);
                noFields.add(5);
                for (int nf : noFields) {
                    DuneChoice choice = new DuneChoice(buttonPrefix + nf, "Ship " + nf + " No-Field token.");
                    Integer invalidNoField = richese.getFrontOfShieldNoField();
                    Territory noFieldTerritory = richese.getTerritoryWithNoFieldOrNull();
                    if (noFieldTerritory != null)
                        invalidNoField = noFieldTerritory.getRicheseNoField();
                    choice.setDisabled(invalidNoField != null && nf == invalidNoField || nf == richese.getShipment().getNoField());
                    choices.add(choice);
                }
                faction.getChat().publish("No-Field options:", choices);
            }

            List<DuneChoice> choices = new ArrayList<>();
            boolean disableConfirmButton = true;
            Shipment shipment = faction.getShipment();
            if (shipment.getForce() > 0 || shipment.getSpecialForce() > 0 || hasNoField)
                disableConfirmButton = false;
            String executePrefix = "";
            if (guildAmbassador || isStartingForces)
                executePrefix = faction.getMovement().getChoicePrefix();
            // Why is this not just using choicePrefix regardless of guildAmbassador or isStartingForces?
            DuneChoice execute = new DuneChoice(executePrefix + "execute-shipment", "Confirm Shipment");
            execute.setDisabled(disableConfirmButton);

            if (!guildAmbassador && !isStartingForces && faction.getShipment().hasShipped())
                execute.setDisabled(true);
            choices.add(execute);
            choices.add(new DuneChoice("secondary", choicePrefix + "reset-shipping-forces", "Reset forces"));
            choices.add(new DuneChoice("secondary", choicePrefix + "reset-shipment", "Start over"));
            if (!guildAmbassador && !enterDiscoveryToken && faction.hasTreacheryCard("Karama")) {
                DuneChoice choice = new DuneChoice("secondary", "karama-execute-shipment", "Confirm Shipment (Use Karama for Guild rates)");
                choice.setDisabled(disableConfirmButton);
                choices.add(choice);
            }
            faction.getChat().publish("Finalize or start over:", choices);
        } else {
            Movement movement = faction.getMovement();
            if (faction instanceof RicheseFaction && game.getTerritories().get(faction.getMovement().getMovingFrom()).hasRicheseNoField() && !faction.getMovement().isMovingNoField())
                forcesButtons.add(Button.primary("richese-no-field-move", "+1 No-Field token")
                        .withDisabled(game.hasGameOption(GameOption.HOMEWORLDS) && !faction.isHighThreshold()));
            String noField = faction.getMovement().isMovingNoField() ? "\n" + game.getTerritory(faction.getMovement().getMovingFrom()).getRicheseNoField() + " No-Field token" : "";

            String message = "Use buttons below to add forces to your ";
            message += wormRide ? "ride." : "movement.";
            message += " Currently moving:\n**" + faction.forcesStringWithZeroes(faction.getMovement().getForce(), faction.getMovement().getSpecialForce()) + noField + "** to " + faction.getMovement().getMovingTo();
            if (movement.isMovingNoField() || movement.getForce() != 0 || movement.getSpecialForce() != 0 || movement.getSecondForce() != 0 || movement.getSecondSpecialForce() != 0) {
                if (!wormRide && !enterDiscoveryToken && faction.hasTreacheryCard("Hajr"))
                    forcesButtons.add(Button.secondary("hajr", "Confirm Movement and play Hajr"));
                if (!wormRide && !enterDiscoveryToken && faction.hasTreacheryCard("Ornithopter"))
                    forcesButtons.add(Button.secondary("Ornithopter", "Confirm Movement and play Ornithopter"));
                forcesButtons.add(Button.success(choicePrefix + "execute-movement", "Confirm Movement"));
                forcesButtons.add(Button.danger(choicePrefix + "reset-moving-forces", "Reset forces"));
            }
            if (enterDiscoveryToken)
                forcesButtons.add(Button.danger("pass-movement-enter-discovery-token", "Don't enter"));
            else if (fremenRide || fremenAmbassador)
                forcesButtons.add(Button.danger(choicePrefix + "reset-shipment", "Start over"));
            else
                forcesButtons.add(Button.danger(choicePrefix + "reset-movement", "Start over"));

            arrangeButtonsAndSend(message, forcesButtons, discordGame);

            if (!wormRide && !faction.getSkilledLeaders().isEmpty() && faction.getSkilledLeaders().getFirst().getSkillCard().name().equals("Planetologist")) {
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
                    String planetologistMessage = "Second group can move from " + territory.getTerritoryName() + " using Planetologist ability.";
                    planetologistMessage +=
                            " Currently moving:\n**" + faction.forcesStringWithZeroes(faction.getMovement().getSecondForce(), faction.getMovement().getSecondSpecialForce()) + "** from " + secondMovingFrom + " to " + faction.getMovement().getMovingTo();
                    arrangeButtonsAndSend(planetologistMessage, secondForcesButtons, discordGame);
                }
            }
        }
    }

    private static void richeseNoFieldMove(ButtonInteractionEvent event, Game game, DiscordGame discordGame) throws ChannelNotFoundException {
        RicheseFaction richese = game.getRicheseFaction();
        richese.getMovement().setMovingNoField(true);
        queueForcesButtons(event, game, discordGame, richese, false, false, false, false, false);
        discordGame.pushGame();
    }

    private static void queueSectorButtons(ButtonInteractionEvent event, Game game, DiscordGame discordGame, boolean isShipment) throws ChannelNotFoundException, InvalidGameStateException {
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
            buttonSuffix = "-guild-ambassador";
        boolean btHTPlacement = event.getComponentId().contains("-bt-ht");
        if (btHTPlacement)
            buttonSuffix = "-bt-ht";
        boolean startingForces = event.getComponentId().contains("-starting-forces");
        if (startingForces)
            buttonSuffix = "-starting-forces";
        boolean hmsPlacement = event.getComponentId().contains("-hms-placement");
        if (hmsPlacement)
            buttonSuffix = "-hms-placement";
        String shipmentOrMovement = isShipment ? "ship-" : "move-";
        Faction faction = ButtonManager.getButtonPresser(event, game);
        String aggregateTerritoryName = event.getComponentId().replace(shipmentOrMovement, "")
                .replace("fremen-ride-", "")
                .replace("place-shai-hulud-", "")
                .replace("place-great-maker-", "")
                .replace("guild-ambassador-", "")
                .replace("bt-ht-", "")
                .replace("starting-forces-", "")
                .replace("hms-placement-", "")
                .replace("-", " ");
        List<Territory> territory = new ArrayList<>(game.getTerritories().values().stream().filter(t -> t.getTerritoryName().replaceAll("\\s*\\([^)]*\\)\\s*", "").equalsIgnoreCase(aggregateTerritoryName)).toList());
        territory.sort(Comparator.comparingInt(Territory::getSector));
        if (aggregateTerritoryName.equals("Cielago North") || aggregateTerritoryName.equals("Cielago Depression") || aggregateTerritoryName.equals("Meridian")) {
            territory.addFirst(territory.removeLast());
        }

        if (territory.size() == 1) {
            if (shaiHuludPlacement || greatMakerPlacement) {
                game.getFremenFaction().placeWorm(greatMakerPlacement, territory.getFirst(), false);
                discordGame.pushGame();
                return;
            }
            if (isShipment && !fremenRide) faction.getShipment().setTerritoryName(territory.getFirst().getTerritoryName());
            else faction.getMovement().setMovingTo(territory.getFirst().getTerritoryName());
            if (btHTPlacement)
                game.getBTFaction().presentHTExecutionChoices();
            else if (startingForces) {
                if (faction instanceof FremenFaction)
                    queueForcesButtons(event, game, discordGame, faction, isShipment, false, false, false, false, true);
                else
                    faction.presentStartingForcesExecutionChoices();
                discordGame.pushGame();
                return;
            } else if (hmsPlacement) {
                ((IxFaction) faction).presentHMSPlacementExecutionChoices();
                discordGame.pushGame();
            } else
                queueForcesButtons(event, game, discordGame, faction, isShipment, false, guildAmbassador, fremenRide, false);
            deleteShipMoveButtonsInChannel(event.getMessageChannel());
            discordGame.pushGame();
            return;
        }

        List<Button> buttons = new LinkedList<>();

        for (Territory sector : territory) {
            int sectorNameStart = sector.getTerritoryName().indexOf("(");
            String sectorName = sector.getTerritoryName().substring(sectorNameStart + 1, sector.getTerritoryName().length() - 1);
            if (sector.getSpice() > 0)
                buttons.add(Button.primary(shipmentOrMovement + "sector" + buttonSuffix + "-" + sector.getTerritoryName(), sector.getSector() + " - " + sectorName + " (" + sector.getSpice() + " spice)"));
            else
                buttons.add(Button.primary(shipmentOrMovement + "sector" + buttonSuffix + "-" + sector.getTerritoryName(), sector.getSector() + " - " + sectorName));
        }
        String backButtonId = isShipment ? "reset-shipment" : "reset-movement";

        deleteShipMoveButtonsInChannel(event.getMessageChannel());
        discordGame.queueMessage(new MessageCreateBuilder()
                .setContent("Which sector of " + aggregateTerritoryName + "?")
                .addActionRow(buttons)
                .addActionRow(Button.secondary(backButtonId + buttonSuffix, "Start over"))
        );
    }

    protected static void presentSectorChoices(ButtonInteractionEvent event, Game game, DiscordGame discordGame, boolean isShipment) throws ChannelNotFoundException, InvalidGameStateException {
        Faction faction = ButtonManager.getButtonPresser(event, game);
        boolean fremenRide = faction.getMovement().getMoveType() == MoveType.FREMEN_RIDE;
        boolean shaiHuludPlacement = faction.getMovement().getMoveType() == MoveType.SHAI_HULUD_PLACEMENT;
        boolean greatMakerPlacement = faction.getMovement().getMoveType() == MoveType.GREAT_MAKER_PLACEMENT;
        boolean fremenAmbassador = faction.getMovement().getMoveType() == MoveType.FREMEN_AMBASSADOR;
        boolean btHTPlacement = faction.getMovement().getMoveType() == MoveType.BT_HT;
        boolean startingForces = faction.getMovement().getMoveType() == MoveType.STARTING_FORCES;
        boolean hmsPlacement = faction.getMovement().getMoveType() == MoveType.HMS_PLACEMENT;
        String shipmentOrMovement = isShipment ? "ship-" : "move-";
        String choicePrefix = faction.getMovement().getChoicePrefix();
        String aggregateTerritoryName = event.getComponentId().replace(shipmentOrMovement, "")
                .replace(choicePrefix, "")
                .replace("-", " ");
        List<Territory> territory = new ArrayList<>(game.getTerritories().values().stream().filter(t -> t.getTerritoryName().replaceAll("\\s*\\([^)]*\\)\\s*", "").equalsIgnoreCase(aggregateTerritoryName)).toList());
        territory.sort(Comparator.comparingInt(Territory::getSector));
        if (aggregateTerritoryName.equals("Cielago North") || aggregateTerritoryName.equals("Cielago Depression") || aggregateTerritoryName.equals("Meridian")) {
            territory.addFirst(territory.removeLast());
        }

        if (territory.size() == 1) {
            if (shaiHuludPlacement || greatMakerPlacement) {
                game.getFremenFaction().placeWorm(greatMakerPlacement, territory.getFirst(), false);
                discordGame.pushGame();
                return;
            }
            if (isShipment && !fremenRide && !fremenAmbassador)
                faction.getShipment().setTerritoryName(territory.getFirst().getTerritoryName());
            else
                faction.getMovement().setMovingTo(territory.getFirst().getTerritoryName());
            if (btHTPlacement)
                game.getBTFaction().presentHTExecutionChoices();
            else if (startingForces) {
                if (faction instanceof FremenFaction)
                    queueForcesButtons(event, game, discordGame, faction, isShipment, false, false, false, false, true);
                else
                    faction.presentStartingForcesExecutionChoices();
                discordGame.pushGame();
                return;
            } else if (hmsPlacement) {
                ((IxFaction) faction).presentHMSPlacementExecutionChoices();
                discordGame.pushGame();
            } else
                presentForcesChoices(event, game, discordGame, faction, isShipment, false, fremenRide, false, false);
            deleteButtonsInChannelWithPrefix(event.getMessageChannel(), choicePrefix);
            discordGame.pushGame();
            return;
        }

        List<Button> buttons = new LinkedList<>();

        for (Territory sector : territory) {
            int sectorNameStart = sector.getTerritoryName().indexOf("(");
            String sectorName = sector.getTerritoryName().substring(sectorNameStart + 1, sector.getTerritoryName().length() - 1);
            if (sector.getSpice() > 0)
                buttons.add(Button.primary(choicePrefix + shipmentOrMovement + "sector" + "-" + sector.getTerritoryName(), sector.getSector() + " - " + sectorName + " (" + sector.getSpice() + " spice)"));
            else
                buttons.add(Button.primary(choicePrefix + shipmentOrMovement + "sector" + "-" + sector.getTerritoryName(), sector.getSector() + " - " + sectorName));
        }
        String backButtonId = isShipment ? "reset-shipment" : "reset-movement";

        deleteButtonsInChannelWithPrefix(event.getMessageChannel(), choicePrefix);
        discordGame.queueMessage(new MessageCreateBuilder()
                .setContent("Which sector of " + aggregateTerritoryName + "?")
                .addActionRow(buttons)
                .addActionRow(Button.secondary(choicePrefix + backButtonId, "Start over"))
        );
    }

    private static void queueOtherShippingButtons(ButtonInteractionEvent event, DiscordGame discordGame, Game game) {
        String buttonSuffix = event.getComponentId().replace("other", "");
        boolean startingForces = event.getComponentId().contains("-starting-forces");
        boolean hmsPlacement = event.getComponentId().contains("-hms-placement");
        Faction faction = ButtonManager.getButtonPresser(event, game);
        boolean initialPlacement = startingForces || hmsPlacement;
        List<String> otherTerritories = List.of(
                "Polar Sink", "Cielago Depression", "Meridian", "Cielago East", "Harg Pass",
                "Gara Kulon", "Hole In The Rock", "Basin", "Imperial Basin", "Arsunt",
                "Tsimpo", "Bight Of The Cliff", "Wind Pass", "The Greater Flat", "Cielago West"
        );
        List<DuneChoice> choices = otherTerritories.stream().map(s -> shipToTerritoryChoice(game, faction, buttonSuffix, s, initialPlacement)).collect(Collectors.toList());
        choices.add(new DuneChoice("secondary", "reset-shipment" + buttonSuffix, "Start over"));
        faction.getChat().reply("Which Territory?", choices);
        discordGame.queueDeleteMessage();
    }

    protected static void presentOtherShippingChoices(ButtonInteractionEvent event, DiscordGame discordGame, Game game) {
        Faction faction = ButtonManager.getButtonPresser(event, game);
        faction.getMovement().presentNonSpiceNonRockChoices(game, faction);
        discordGame.queueDeleteMessage();
    }

    private static void queueRockShippingButtons(ButtonInteractionEvent event, DiscordGame discordGame, Game game) {
        String buttonSuffix = event.getComponentId().replace("rock", "");
        boolean startingForces = event.getComponentId().contains("-starting-forces");
        boolean hmsPlacement = event.getComponentId().contains("-hms-placement");
        Faction faction = ButtonManager.getButtonPresser(event, game);
        boolean initialPlacement = startingForces || hmsPlacement;
        List<String> rockTerritories = List.of("False Wall South", "Pasty Mesa", "False Wall East", "Shield Wall", "Rim Wall West", "Plastic Basin", "False Wall West");
        List<DuneChoice> choices = rockTerritories.stream().map(s -> shipToTerritoryChoice(game, faction, buttonSuffix, s, initialPlacement)).collect(Collectors.toList());
        choices.add(new DuneChoice("secondary", "reset-shipment" + buttonSuffix, "Start over"));
        faction.getChat().reply("Which Rock Territory?", choices);
        discordGame.queueDeleteMessage();
    }

    protected static void presentRockShippingChoices(ButtonInteractionEvent event, DiscordGame discordGame, Game game) {
        Faction faction = ButtonManager.getButtonPresser(event, game);
        faction.getMovement().presentRockChoices(game, faction);
        discordGame.queueDeleteMessage();
    }

    private static void queueSpiceBlowShippingButtons(ButtonInteractionEvent event, DiscordGame discordGame, Game game) {
        String buttonSuffix = event.getComponentId().replace("spice-blow", "");
        boolean startingForces = event.getComponentId().contains("-starting-forces");
        boolean hmsPlacement = event.getComponentId().contains("-hms-placement");
        Faction faction = ButtonManager.getButtonPresser(event, game);
        boolean initialPlacement = startingForces || hmsPlacement;
        List<String> spiceBlowTerritories = List.of(
                "Habbanya Ridge Flat", "Cielago South", "Broken Land", "South Mesa", "Sihaya Ridge",
                "Hagga Basin", "Red Chasm", "The Minor Erg", "Cielago North", "Funeral Plain",
                "The Great Flat", "Habbanya Erg", "Old Gap", "Rock Outcroppings", "Wind Pass North"
        );
        List<DuneChoice> choices = spiceBlowTerritories.stream().map(s -> shipToTerritoryChoice(game, faction, buttonSuffix, s, initialPlacement)).collect(Collectors.toList());
        choices.add(new DuneChoice("secondary", "reset-shipment" + buttonSuffix, "Start over"));
        faction.getChat().reply("Which Spice Blow Territory?", choices);
        discordGame.queueDeleteMessage();
    }

    protected static void presentSpiceBlowShippingChoices(ButtonInteractionEvent event, DiscordGame discordGame, Game game) {
        Faction faction = ButtonManager.getButtonPresser(event, game);
        faction.getMovement().presentSpiceBlowChoices(game, faction);
        discordGame.queueDeleteMessage();
    }

    private static void queueHomeworldShippingButtons(ButtonInteractionEvent event, Game game, DiscordGame discordGame) {
        String buttonSuffix = event.getComponentId().replace("homeworlds", "");
        Faction faction = ButtonManager.getButtonPresser(event, game);
        List<DuneChoice> choices = new ArrayList<>();
        game.getFactions().stream().filter(f -> !faction.getName().equals(f.getName())).forEach(f -> {
            choices.add(shipToTerritoryChoice(game, faction, buttonSuffix, f.getHomeworld(), false));
            if (f instanceof EmperorFaction emperorFaction)
                choices.add(shipToTerritoryChoice(game, faction, buttonSuffix, emperorFaction.getSecondHomeworld(), false));
        });
        choices.add(new DuneChoice("secondary", "reset-shipment" + buttonSuffix, "Start over"));
        faction.getChat().reply("Which Homeworld?", choices);
        discordGame.queueDeleteMessage();
    }

    private static void queueDiscoveryShippingButtons(ButtonInteractionEvent event, Game game, DiscordGame discordGame) {
        String buttonSuffix = event.getComponentId().replace("discovery-tokens", "");
        Faction faction = ButtonManager.getButtonPresser(event, game);
        List<DuneChoice> choices = game.getTerritories().values().stream().filter(Territory::isDiscovered)
                .map(territory -> shipToTerritoryChoice(game, faction, buttonSuffix, territory.getDiscoveryToken(), false)).collect(Collectors.toList());
        choices.add(new DuneChoice("secondary", "reset-shipment" + buttonSuffix, "Start over"));
        faction.getChat().reply("Which Discovery Token?", choices);
        discordGame.queueDeleteMessage();
    }

    protected static void presentDiscoveryShippingChoices(ButtonInteractionEvent event, Game game, DiscordGame discordGame) {
        Faction faction = ButtonManager.getButtonPresser(event, game);
        faction.getMovement().presentDiscoveryTokenChoices(game, faction);
        discordGame.queueDeleteMessage();
    }

    private static void queueStrongholdShippingButtons(ButtonInteractionEvent event, Game game, DiscordGame discordGame) {
        Faction faction = ButtonManager.getButtonPresser(event, game);
        String buttonSuffix = event.getComponentId().replace("stronghold", "");
        boolean fremenRide = event.getComponentId().contains("-fremen-ride");
        boolean btHTPlacement = event.getComponentId().contains("-bt-ht");
        boolean startingForces = event.getComponentId().contains("-starting-forces");
        List<DuneChoice> choices = game.getTerritories().values().stream()
                .filter(t -> t.isValidStrongholdForShipmentFremenRideAndBTHT(faction, fremenRide || btHTPlacement))
                .map(t -> shipToTerritoryChoice(game, faction, buttonSuffix, t.getTerritoryName(), startingForces)).sorted(Comparator.comparing(DuneChoice::getLabel)).collect(Collectors.toList());
        choices.add(new DuneChoice("secondary", "reset-shipment" + buttonSuffix, "Start over"));
        faction.getChat().reply("Which Stronghold?", choices);
        discordGame.queueDeleteMessage();
    }

    protected static void presentStrongholdShippingChoices(ButtonInteractionEvent event, Game game, DiscordGame discordGame) {
        Faction faction = ButtonManager.getButtonPresser(event, game);
        faction.getMovement().presentStrongholdChoices(game, faction);
        discordGame.queueDeleteMessage();
    }

    protected static DuneChoice shipToTerritoryChoice(Game game, Faction faction, String buttonSuffix, String wholeTerritoryName, boolean isInitialPlacement) {
        String labelSuffix = "-" + wholeTerritoryName;
        DuneChoice choice = new DuneChoice("ship" + buttonSuffix + labelSuffix, wholeTerritoryName);
        List<Territory> sectors = game.getTerritories().values().stream().filter(s -> s.getTerritoryName().startsWith(wholeTerritoryName)).toList();
        choice.setDisabled(sectors.stream().anyMatch(s -> s.factionMayNotEnter(game, faction, !buttonSuffix.contains("fremen-ride"), isInitialPlacement)));
        return choice;
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
                game.getFremenFaction().placeWorm(buttonSuffix.equals("-place-great-maker"), game.getTerritory(faction.getMovement().getMovingFrom()), true);
                deleteShipMoveButtonsInChannel(event.getMessageChannel());
            }
            case "-guild-ambassador" -> {
                discordGame.queueMessage("You will not ship with your Guild ambassador.");
                game.getTurnSummary().publish(Emojis.ECAZ + " does not ship with their Guild ambassador.");
                faction.getShipment().clear();
            }
            case "-bt-ht" -> {
                discordGame.queueMessage("You will leave your free revivals on Tleilaxu.");
                game.getTurnSummary().publish(Emojis.BT + " leaves their free revivals on Tleilaxu.");
                faction.getShipment().clear();
                game.getBTFaction().setBtHTActive(false);
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

    public static void queueShippingButtons(ButtonInteractionEvent event, Game game, DiscordGame discordGame) {
        queueShippingButtons(event, game, discordGame, false);
    }

    public static void queueShippingButtons(ButtonInteractionEvent event, Game game, DiscordGame discordGame, boolean fremenRide) {
        Faction faction = ButtonManager.getButtonPresser(event, game);
        String buttonSuffix = fremenRide ? "-fremen-ride" : "";
        List<DuneChoice> choices = new LinkedList<>();
        choices.add(new DuneChoice("stronghold" + buttonSuffix, "Stronghold"));
        choices.add(new DuneChoice("spice-blow" + buttonSuffix, "Spice Blow Territories"));
        choices.add(new DuneChoice("rock" + buttonSuffix, "Rock Territories"));
        if (game.hasGameOption(GameOption.HOMEWORLDS) && !fremenRide)
            choices.add(new DuneChoice("homeworlds", "Homeworlds"));
        boolean revealedDiscoveryTokenOnMap = game.getTerritories().values().stream().anyMatch(Territory::isDiscovered);
        if (game.hasGameOption(GameOption.DISCOVERY_TOKENS) && revealedDiscoveryTokenOnMap)
            choices.add(new DuneChoice("discovery-tokens" + buttonSuffix, "Discovery Tokens"));
        choices.add(new DuneChoice("other" + buttonSuffix, "Somewhere else"));
        choices.add(new DuneChoice("danger", "pass-shipment" + buttonSuffix, fremenRide ? "No ride" : "I don't want to ship."));
        faction.getChat().reply(fremenRide ? "Where would you like to ride to?" : "Where would you like to ship to?", choices);

        choices = new ArrayList<>();
        if (faction instanceof GuildFaction && faction.getShipment().getCrossShipFrom().isEmpty()) {
            choices.add(new DuneChoice("guild-cross-ship", "Cross ship"));
            choices.add(new DuneChoice("guild-ship-to-reserves", "Ship to reserves"));
            faction.getChat().publish("Special options for " + Emojis.GUILD + ":", choices);
        } else if (faction.getAlly().equals("Guild") && faction.getShipment().getCrossShipFrom().isEmpty()) {
            choices.add(new DuneChoice("guild-cross-ship", "Cross ship"));
            faction.getChat().publish("Special option for " + Emojis.GUILD + " ally :", choices);
        }

        discordGame.queueDeleteMessage();
    }

    public static void presentShippingChoices(ButtonInteractionEvent event, Game game, DiscordGame discordGame, boolean fremenRide) {
        Faction faction = ButtonManager.getButtonPresser(event, game);
        boolean fremenAmbassador = faction.getMovement().getMoveType() == MoveType.FREMEN_AMBASSADOR;
        String choicePrefix = faction.getMovement().getChoicePrefix();
        boolean wormRide = fremenRide || fremenAmbassador;
        List<DuneChoice> choices = new LinkedList<>();
        choices.add(new DuneChoice(choicePrefix + "stronghold", "Stronghold"));
        choices.add(new DuneChoice(choicePrefix + "spice-blow", "Spice Blow Territories"));
        choices.add(new DuneChoice(choicePrefix + "rock", "Rock Territories"));
        if (game.hasGameOption(GameOption.HOMEWORLDS) && !wormRide)
            choices.add(new DuneChoice("homeworlds", "Homeworlds"));
        boolean revealedDiscoveryTokenOnMap = game.getTerritories().values().stream().anyMatch(Territory::isDiscovered);
        if (game.hasGameOption(GameOption.DISCOVERY_TOKENS) && revealedDiscoveryTokenOnMap)
            choices.add(new DuneChoice(choicePrefix + "discovery-tokens", "Discovery Tokens"));
        choices.add(new DuneChoice(choicePrefix + "other", "Somewhere else"));
        choices.add(new DuneChoice("danger", choicePrefix + "pass-shipment", wormRide ? "No ride" : "I don't want to ship."));
        faction.getChat().reply(wormRide ? "Where would you like to ride to from " + faction.getMovement().getMovingFrom() + "?" : "Where would you like to ship to?", choices);

        choices = new ArrayList<>();
        if (faction instanceof GuildFaction && faction.getShipment().getCrossShipFrom().isEmpty()) {
            choices.add(new DuneChoice("guild-cross-ship", "Cross ship"));
            choices.add(new DuneChoice("guild-ship-to-reserves", "Ship to reserves"));
            faction.getChat().publish("Special options for " + Emojis.GUILD + ":", choices);
        } else if (faction.getAlly().equals("Guild") && faction.getShipment().getCrossShipFrom().isEmpty()) {
            choices.add(new DuneChoice("guild-cross-ship", "Cross ship"));
            faction.getChat().publish("Special option for " + Emojis.GUILD + " ally :", choices);
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
                    (!game.hasGameOption(GameOption.HOMEWORLDS) || !(faction instanceof EmperorFaction) || !territoryName.equals("Kaitain") && !territoryName.equals("Salusa Secundus")))
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