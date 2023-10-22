package controller.commands;

import constants.Emojis;
import controller.Initializers;
import enums.GameOption;
import enums.UpdateType;
import exceptions.ChannelNotFoundException;
import model.*;
import model.factions.*;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageHistory;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.utils.FileUpload;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import utils.CardImages;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.URL;
import java.text.MessageFormat;
import java.util.List;
import java.util.*;

import static controller.commands.CommandOptions.faction;

public class ShowCommands {
    public static List<CommandData> getCommands() {
        List<CommandData> commandData = new ArrayList<>();
        commandData.add(
                Commands.slash("show", "Show parts of the game.").addSubcommands(
                        new SubcommandData("board", "Show the map in the turn summary"),
                        new SubcommandData("faction-info", "Print Faction Information in their Private Channel")
                                .addOptions(faction),
                        new SubcommandData("front-of-shields", "Refresh the #front-of-shield channel")
                )
        );

        return commandData;
    }

    public static void runCommand(SlashCommandInteractionEvent event, DiscordGame discordGame, Game game) throws ChannelNotFoundException, IOException {
        String name = event.getSubcommandName();
        if (name == null) throw new IllegalArgumentException("Invalid command name: null");

        switch (name) {
            case "board" -> showBoard(discordGame, game);
            case "faction-info" -> showFactionInfo(discordGame);
            case "front-of-shields" -> refreshFrontOfShieldInfo(discordGame, game);
        }
    }

    public static void showBoard(DiscordGame discordGame, Game game) throws ChannelNotFoundException, IOException {
        if (game.getMute()) return;
        discordGame.getTurnSummary().queueMessage(drawGameBoard(game));
        if (game.hasGameOption(GameOption.MAP_IN_FRONT_OF_SHIELD)) {
            game.setUpdated(UpdateType.MAP);
        }
    }

    public static void showFactionInfo(DiscordGame discordGame) throws ChannelNotFoundException, IOException {
        String factionName = discordGame.required(faction).getAsString();
        if (discordGame.getGame().getFaction(factionName).isGraphicDisplay())
            drawFactionInfo(discordGame, discordGame.getGame(), factionName);
        else writeFactionInfo(discordGame, factionName);
    }

    private static BufferedImage getResourceImage(String name) throws IOException {
        URL file = ShowCommands.class.getClassLoader().getResource("Board Components/" + name + ".png");
        if (file == null) file = ShowCommands.class.getClassLoader().getResource("Board Components/" + name + ".jpg");
        assert file != null;
        return ImageIO.read(file);
    }

    private static FileUpload getResourceFile(String name) throws IOException {
        URL resourceURL = ShowCommands.class.getClassLoader()
                .getResource("Board Components/" + name + ".png");

        if (resourceURL == null) throw new FileNotFoundException("File not found: " + name);

        InputStream inputStream = resourceURL.openStream();
        return FileUpload.fromData(inputStream, name + ".png");
    }

    public static void drawFactionInfo(DiscordGame discordGame, Game game, String factionName) throws IOException, ChannelNotFoundException {
        if (game.getMute()) return;

        MessageChannel infoChannel = discordGame.getTextChannel(factionName.toLowerCase() + "-info");
        MessageHistory messageHistory = MessageHistory.getHistoryFromBeginning(infoChannel).complete();

        List<Message> messages = messageHistory.getRetrievedHistory();

        messages.forEach(discordGame::queueDeleteMessage);

        Faction faction = game.getFaction(factionName);
        BufferedImage table = getResourceImage(faction.getHomeworld());
        if (factionName.equals("Emperor")) {
            table = getResourceImage(((EmperorFaction) faction).getSecondHomeworld());
        }
        table = resize(table, 5000, 5000);

        //Place reserves
        int reserves = faction.getReserves().getStrength();
        int specialReserves = faction.getSpecialReserves().getStrength();

        if (reserves > 0) {
            BufferedImage reservesImage = buildForceImage(faction.getName(), reserves);
            reservesImage = resize(reservesImage, 353, 218);
            table = overlay(table, reservesImage, new Point(300, 200), 1);
        }
        if (specialReserves > 0) {
            BufferedImage specialReservesImage = buildForceImage(faction.getName() + "*", specialReserves);
            specialReservesImage = resize(specialReservesImage, 353, 218);
            table = overlay(table, specialReservesImage, new Point(300, 375), 1);
        }

        //Place spice
        int spice = faction.getSpice();
        int offset = 0;
        Point spicePlacement = new Point(1000, 200);
        while (spice != 0) {
            if (spice >= 10) {
                BufferedImage spiceImage = getResourceImage("10 Spice");
                spiceImage = resize(spiceImage, 200, 200);
                Point spicePlacementOffset = new Point(spicePlacement.x + offset, spicePlacement.y);
                table = overlay(table, spiceImage, spicePlacementOffset, 1);
                spice -= 10;
            } else if (spice == 5) {
                BufferedImage spiceImage = getResourceImage("5 Spice");
                spiceImage = resize(spiceImage, 200, 200);
                Point spicePlacementOffset = new Point(spicePlacement.x + offset, spicePlacement.y);
                table = overlay(table, spiceImage, spicePlacementOffset, 1);
                spice -= 5;
            } else if (spice >= 2) {
                BufferedImage spiceImage = getResourceImage("2 Spice");
                spiceImage = resize(spiceImage, 200, 200);
                Point spicePlacementOffset = new Point(spicePlacement.x + offset, spicePlacement.y);
                table = overlay(table, spiceImage, spicePlacementOffset, 1);
                spice -= 2;
            } else {
                BufferedImage spiceImage = getResourceImage("1 Spice");
                spiceImage = resize(spiceImage, 200, 200);
                Point spicePlacementOffset = new Point(spicePlacement.x + offset, spicePlacement.y);
                table = overlay(table, spiceImage, spicePlacementOffset, 1);
                spice -= 1;
            }
            offset += 120;
        }

        //Place Ornithopter token
        if (faction.hasOrnithoperToken()) {
            BufferedImage ornithopterImage = getResourceImage("Ornithopter");
            ornithopterImage = resize(ornithopterImage, 250, 250);
            table = overlay(table, ornithopterImage, new Point(1200 + offset, 200), 1);
        }

        offset = 0;

        //Place leaders
        for (Leader leader : faction.getLeaders()) {
            BufferedImage leaderImage = getResourceImage(leader.name());
            if (!leader.name().equals("Kwisatz Haderach")) leaderImage = resize(leaderImage, 500, 500);
            else leaderImage = resize(leaderImage, 500, 301);
            Point leaderPoint = new Point(300, 750 + offset);
            table = overlay(table, leaderImage, leaderPoint, 1);
            offset += 450;
        }

        offset = 0;

        //Place Treachery Cards
        int count = 0;
        int offsetY = 0;
        for (TreacheryCard treacheryCard : faction.getTreacheryHand()) {
            Optional<FileUpload> image = CardImages.getTreacheryCardImage(discordGame.getEvent().getGuild(), treacheryCard.name());
            if (image.isPresent()) {
                BufferedImage cardImage = ImageIO.read(image.get().getData());
                cardImage = resize(cardImage, 988, 1376);
                Point cardPoint = new Point(1250 + offset, 1250 + offsetY);
                table = overlay(table, cardImage, cardPoint, 1);
                offset += 900;
                count++;
                if (count == 4) {
                    offset = 0;
                    offsetY = 1000;
                }
            }
        }

        offset = 0;

        //Place Traitor Cards
        for (TraitorCard traitorCard : faction.getTraitorHand()) {
            Optional<FileUpload> image = CardImages.getTraitorImage(discordGame.getEvent().getGuild(), traitorCard.name());
            if (image.isPresent()) {
                BufferedImage cardImage = ImageIO.read(image.get().getData());
                cardImage = resize(cardImage, 988, 1376);
                Point cardPoint = new Point(1050 + offset, 3500);
                table = overlay(table, cardImage, cardPoint, 1);
                offset += 900;
            }
        }

        //Place KH counter card
        if (faction.getName().equals("Atreides")) {
            BufferedImage KHCounterImage = getResourceImage("KH Counter");
            int x;
            int y;
            switch (((AtreidesFaction)faction).getForcesLost()) {
                case 0 -> {
                    x = 275;
                    y = 575;
                }
                case 1 -> {
                    x = 585;
                    y = 575;
                }
                case 2 -> {
                    x = 890;
                    y = 575;
                }
                case 3 -> {
                    x = 1195;
                    y = 575;
                }
                case 4 -> {
                    x = 275;
                    y = 900;
                }
                case 5 -> {
                    x = 585;
                    y = 900;
                }
                case 6 -> {
                    x = 890;
                    y = 900;
                }
                default  -> {
                    x = 1195;
                    y = 900;
                }
            }
            BufferedImage KHCounter = getResourceImage("KH token");
            KHCounterImage = overlay(KHCounterImage, KHCounter, new Point(x, y), 1);
            table = overlay(table, resize(KHCounterImage, 988, 1376), new Point(1100 + offset, 3500), 1);
        }

        //Place Homeworld Card
        if (game.hasGameOption(GameOption.HOMEWORLDS)) {
            String lowHigh = faction.isHighThreshold() ? "High" : "Low";
            Optional<FileUpload> image = CardImages.getHomeworldImage(discordGame.getEvent().getGuild(), faction.getHomeworld() + " " + lowHigh);
            if (image.isPresent()) {
                BufferedImage cardImage = ImageIO.read(image.get().getData());
                cardImage = resize(cardImage, 988, 1376);
                Point cardPoint = new Point(4500, 750);
                table = overlay(table, cardImage, cardPoint, 1);
            }
            if (factionName.equals("Emperor")) {
                EmperorFaction emperor = (EmperorFaction) faction;
                lowHigh = emperor.isSecundusHighThreshold() ? "High" : "Low";
                image = CardImages.getHomeworldImage(discordGame.getEvent().getGuild(), emperor.getSecondHomeworld() + " " + lowHigh);
                if (image.isPresent()) {
                    BufferedImage cardImage = ImageIO.read(image.get().getData());
                    cardImage = resize(cardImage, 988, 1376);
                    Point cardPoint = new Point(4500, 2250);
                    table = overlay(table, cardImage, cardPoint, 1);
                }

            }
        }

        //Place nexus card if any
        if (faction.getNexusCard() != null) {
            Optional<FileUpload> image = CardImages.getNexusImage(discordGame.getEvent().getGuild(), faction.getNexusCard().name());
            if (image.isPresent()) {
                BufferedImage cardImage = ImageIO.read(image.get().getData());
                cardImage = resize(cardImage, 988, 1376);
                Point cardPoint = new Point(750 + offset, 3500);
                table = overlay(table, cardImage, cardPoint, 1);
            }
        }

        //BG Prediction
        if (faction.getName().equals("BG")) {
            offset += 900;
            Optional<FileUpload> image = CardImages.getPredictionImage(discordGame.getEvent().getGuild(), "Turn " + ((BGFaction) faction).getPredictionRound());
            if (image.isPresent()) {
                BufferedImage cardImage = ImageIO.read(image.get().getData());
                cardImage = resize(cardImage, 988, 1376);
                Point cardPoint = new Point(750 + offset, 3500);
                table = overlay(table, cardImage, cardPoint, 1);
            }
            offset += 900;
            image = CardImages.getPredictionImage(discordGame.getEvent().getGuild(), ((BGFaction) faction).getPredictionFactionName());
            if (image.isPresent()) {
                BufferedImage cardImage = ImageIO.read(image.get().getData());
                cardImage = resize(cardImage, 988, 1376);
                Point cardPoint = new Point(750 + offset, 3500);
                table = overlay(table, cardImage, cardPoint, 1);
            }
        }

        offset = 0;

        //Ecaz ambassadors
        if (faction.getName().equals("Ecaz")) {
            for (String ambassador : ((EcazFaction)faction).getAmbassadorSupplyList()) {
                BufferedImage ambassadorImage = getResourceImage(ambassador + " Ambassador");
                table = overlay(table, resize(ambassadorImage, 300, 300), new Point(750 + offset, 2250), 1);
                offset += 330;
            }
        }

        //Moritani Terror Tokens
        if (faction.getName().equals("Moritani")) {
            for (String terrorToken : ((MoritaniFaction)faction).getTerrorTokens()) {
                BufferedImage terrorTokenImage = getResourceImage(terrorToken);
                table = overlay(table, resize(terrorTokenImage, 300, 300), new Point(750 + offset, 2250), 1);
                offset += 330;
            }
        }


        ByteArrayOutputStream boardOutputStream = new ByteArrayOutputStream();
        ImageIO.write(table, "png", boardOutputStream);

        FileUpload boardFileUpload = FileUpload.fromData(boardOutputStream.toByteArray(), "behind shield.png");

        discordGame.queueMessage(faction.getName().toLowerCase() + "-info", "Faction Info", boardFileUpload);

        sendInfoButtons(game, discordGame, faction);
    }

    public static void sendInfoButtons(Game game, DiscordGame discordGame, Faction faction) throws ChannelNotFoundException {
        discordGame.queueMessage(faction.getName().toLowerCase() + "-info", new MessageCreateBuilder().addContent("Use these buttons to take the corresponding actions.")
                .addActionRow(Button.secondary("graphic", "-info channel graphic mode"), Button.secondary("text", "-info channel text mode")).build());

        if (faction.hasAlly()) {
            if (game.getFaction(faction.getAlly()).getAllySpiceBidding() > faction.getSpice()) game.getFaction(faction.getAlly()).setAllySpiceBidding(faction.getSpice());
            if (game.getFaction(faction.getAlly()).getAllySpiceShipment() > faction.getSpice()) game.getFaction(faction.getAlly()).setAllySpiceShipment(faction.getSpice());
            discordGame.queueMessage(faction.getName().toLowerCase() + "-info", new MessageCreateBuilder().addContent("You are currently offering " + game.getFaction(faction.getAlly()).getAllySpiceShipment() + " " + Emojis.SPICE + " to your ally for shipping.\nSet ally shipping support:")
                    .addActionRow(Button.primary("support-max", "Max support"),
                            Button.secondary("support-number", "Pick a number"), Button.danger("support-reset", "Reset support")).build());
            discordGame.queueMessage(faction.getName().toLowerCase() + "-info", new MessageCreateBuilder().addContent("You are currently offering " + game.getFaction(faction.getAlly()).getAllySpiceBidding() + " " + Emojis.SPICE + " to your ally for bidding.\nSet ally bidding support:")
                    .addActionRow(Button.primary("bid-support-max", "Max support"),
                            Button.secondary("bid-support-number", "Pick a number"), Button.danger("bid-support-reset", "Reset support")).build());
        }
    }

    private static FileUpload drawGameBoard(Game game) throws IOException {
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(
                Objects.requireNonNull(Faction.class.getClassLoader().getResourceAsStream("Leaders.csv"))
        ));
        CSVParser csvParser = CSVParser.parse(bufferedReader, CSVFormat.EXCEL);

        HashMap<String, String> leaderToFaction = new HashMap<>();

        for (CSVRecord csvRecord : csvParser) {
            leaderToFaction.put(csvRecord.get(1), csvRecord.get(0));
        }

        BufferedImage board = getResourceImage("Board");

        //If Homeworlds are in play, concatenate homeworlds under the board.
        if (game.hasGameOption(GameOption.HOMEWORLDS)) {
            BufferedImage homeworlds = new BufferedImage(1, 1024, BufferedImage.TYPE_INT_ARGB);
            for (Faction faction : game.getFactions()) {
                BufferedImage homeworld = getResourceImage(faction.getHomeworld());
                int offset = 0;
                for (Force force : game.getTerritory(faction.getHomeworld()).getForces()) {
                    BufferedImage forceImage = buildForceImage(force.getName(), force.getStrength());
                    forceImage = resize(forceImage, 376, 232);
                    Point forcePlacement = new Point(500, 200 + offset);
                    homeworld = overlay(homeworld, forceImage, forcePlacement, 1);
                    offset += 240;
                }
                offset = 0;
                homeworlds = concatenateHorizontally(homeworlds, homeworld);
                if (faction.getName().equals("Emperor")) {
                    BufferedImage salusa = getResourceImage("Salusa Secundus");
                    for (Force force : game.getTerritory(((EmperorFaction) faction).getSecondHomeworld()).getForces()) {
                        BufferedImage forceImage = buildForceImage(force.getName(), force.getStrength());
                        forceImage = resize(forceImage, 376, 232);
                        Point forcePlacement = new Point(500, 200 + offset);
                        salusa = overlay(salusa, forceImage, forcePlacement, 1);
                        offset += 240;
                    }
                    homeworlds = concatenateHorizontally(homeworlds, salusa);
                }
            }
            homeworlds = resize(homeworlds, board.getWidth(), 200);
            board = concatenateVertically(board, homeworlds);
        }

        // Add border to show where Fremen can ship
        if (game.hasFaction("Fremen")) {
            BufferedImage fremenShipBorder = getResourceImage("Fremen Ship Border");
            fremenShipBorder = resize(fremenShipBorder, 461, 716);
            Point coordinates = new Point(316, 475);
            board = overlay(board, fremenShipBorder, coordinates, 1);
        }

        //Place destroyed Shield Wall
        if (game.isShieldWallDestroyed()) {
            BufferedImage brokenShieldWallImage = getResourceImage("Shield Wall Destroyed");
            brokenShieldWallImage = resize(brokenShieldWallImage, 256, 231);
            Point coordinates = Initializers.getDrawCoordinates("shield wall");
            board = overlay(board, brokenShieldWallImage, coordinates, 1);
        }

        //Place turn, phase, and storm markers
        BufferedImage turnMarker = getResourceImage("Turn Marker");
        turnMarker = resize(turnMarker, 55, 55);
        int turn = game.getTurn() == 0 ? 1 : game.getTurn();
        float angle = (turn * 36) + 74f;
        turnMarker = rotateImageByDegrees(turnMarker, angle);
        Point coordinates = Initializers.getDrawCoordinates("turn " + game.getTurn());
        board = overlay(board, turnMarker, coordinates, 1);
        BufferedImage phaseMarker = getResourceImage("Phase Marker");
        phaseMarker = resize(phaseMarker, 50, 50);
        coordinates = Initializers.getDrawCoordinates("phase " + (game.getPhase()));
        board = overlay(board, phaseMarker, coordinates, 1);

        //Place Tech Tokens
        for (int i = 0; i < game.getFactions().size(); i++) {
            Faction faction = game.getFactions().get(i);
            if (faction.getTechTokens().isEmpty()) continue;
            int offset = 0;
            for (TechToken token : faction.getTechTokens()) {
                BufferedImage tokenImage = getResourceImage(token.getName());
                tokenImage = resize(tokenImage, 50, 50);
                coordinates = Initializers.getDrawCoordinates("tech token " + i);
                Point coordinatesOffset = new Point(coordinates.x + offset, coordinates.y);
                board = overlay(board, tokenImage, coordinatesOffset, 1);
                offset += 50;
            }
        }



        //Place pieces in territories
        for (Territory territory : game.getTerritories().values()) {
            if (territory.getForces().isEmpty() && territory.getSpice() == 0
                    && !territory.hasRicheseNoField() && territory.getEcazAmbassador() == null
                    && !territory.isAftermathToken() && !territory.hasTerrorToken()
                    && territory.getDiscoveryToken() == null) continue;
            if (territory.getTerritoryName().matches("Hidden Mobile Stronghold|Cistern|Ecological Testing Station|Shrine|Orgiz Processing Station")) continue;
            if (game.getHomeworlds().containsValue(territory.getTerritoryName())) continue;
            int offset = 0;
            int i = 0;

            if (territory.isAftermathToken()) {
                BufferedImage aftermath = getResourceImage("Aftermath");
                aftermath = resize(aftermath, 50, 50);
                Point placement = Initializers.getPoints(territory.getTerritoryName()).get(0);
                Point placementCorner = new Point(placement.x, placement.y);
                board = overlay(board, aftermath, placementCorner, 1);
            }

            if (territory.getEcazAmbassador() != null) {
                BufferedImage ambassador = resize(getResourceImage(territory.getEcazAmbassador() + " Ambassador"), 40, 40);
                Point placement = Initializers.getPoints(territory.getTerritoryName()).get(1);
                Point placementCorner = new Point(placement.x + 40, placement.y);
                board = overlay(board, ambassador, placementCorner, 1);
            }

            if (!territory.getTerrorTokens().isEmpty()) {
                for (int j = 0; j < territory.getTerrorTokens().size(); j++) {
                    BufferedImage terrorToken = getResourceImage("Terror Token");
                    terrorToken = resize(terrorToken, 40, 40);
                    Point placement = Initializers.getPoints(territory.getTerritoryName()).get(1);
                    Point placementCorner = new Point(placement.x - 20 + offset, placement.y);
                    board = overlay(board, terrorToken, placementCorner, 1);
                    offset += 20;
                }
            }
            offset = 0;

            if (territory.getSpice() != 0) {
                i = 1;
                int spice = territory.getSpice();
                while (spice != 0) {
                    if (spice >= 10) {
                        BufferedImage spiceImage = getResourceImage("10 Spice");
                        spiceImage = resize(spiceImage, 25, 25);
                        Point spicePlacement = Initializers.getPoints(territory.getTerritoryName()).get(0);
                        Point spicePlacementOffset = new Point(spicePlacement.x + offset, spicePlacement.y - offset);
                        board = overlay(board, spiceImage, spicePlacementOffset, 1);
                        spice -= 10;
                    } else if (spice >= 8) {
                        BufferedImage spiceImage = getResourceImage("8 Spice");
                        spiceImage = resize(spiceImage, 25, 25);
                        Point spicePlacement = Initializers.getPoints(territory.getTerritoryName()).get(0);
                        Point spicePlacementOffset = new Point(spicePlacement.x + offset, spicePlacement.y - offset);
                        board = overlay(board, spiceImage, spicePlacementOffset, 1);
                        spice -= 8;
                    } else if (spice >= 6) {
                        BufferedImage spiceImage = getResourceImage("6 Spice");
                        spiceImage = resize(spiceImage, 25, 25);
                        Point spicePlacement = Initializers.getPoints(territory.getTerritoryName()).get(0);
                        Point spicePlacementOffset = new Point(spicePlacement.x + offset, spicePlacement.y - offset);
                        board = overlay(board, spiceImage, spicePlacementOffset, 1);
                        spice -= 6;
                    } else if (spice == 5) {
                        BufferedImage spiceImage = getResourceImage("5 Spice");
                        spiceImage = resize(spiceImage, 25, 25);
                        Point spicePlacement = Initializers.getPoints(territory.getTerritoryName()).get(0);
                        Point spicePlacementOffset = new Point(spicePlacement.x + offset, spicePlacement.y - offset);
                        board = overlay(board, spiceImage, spicePlacementOffset, 1);
                        spice -= 5;
                    } else if (spice >= 2) {
                        BufferedImage spiceImage = getResourceImage("2 Spice");
                        spiceImage = resize(spiceImage, 25, 25);
                        Point spicePlacement = Initializers.getPoints(territory.getTerritoryName()).get(0);
                        Point spicePlacementOffset = new Point(spicePlacement.x + offset, spicePlacement.y - offset);
                        board = overlay(board, spiceImage, spicePlacementOffset, 1);
                        spice -= 2;
                    } else {
                        BufferedImage spiceImage = getResourceImage("1 Spice");
                        spiceImage = resize(spiceImage, 25, 25);
                        Point spicePlacement = Initializers.getPoints(territory.getTerritoryName()).get(0);
                        Point spicePlacementOffset = new Point(spicePlacement.x + offset, spicePlacement.y - offset);
                        board = overlay(board, spiceImage, spicePlacementOffset, 1);
                        spice -= 1;
                    }
                    offset += 15;
                }
            }
            offset = 0;
            if (territory.getDiscoveryToken() != null) {
                getResourceImage(territory.getDiscoveryToken());
                BufferedImage discoveryToken;
                if (territory.isDiscovered()) {
                    discoveryToken = getResourceImage(territory.getDiscoveryToken());
                    discoveryToken = resize(discoveryToken, 500, 500);
                    for (Force force : game.getTerritory(territory.getDiscoveryToken()).getForces()) {
                        BufferedImage forceImage = buildForceImage(force.getName(), force.getStrength());
                        forceImage = resize(forceImage, 376, 232);
                        Point forcePlacement = new Point(250, 150);
                        Point forcePlacementOffset = new Point(forcePlacement.x, forcePlacement.y + offset);
                        discoveryToken = overlay(discoveryToken, forceImage, forcePlacementOffset, 1);
                        offset += 100;
                    }
                }
                else {
                    if (territory.isRock()) discoveryToken = getResourceImage("Smuggler Token");
                    else discoveryToken = getResourceImage("Hiereg Token");
                }
                discoveryToken = resize(discoveryToken, 60, 60);
                board = overlay(board, discoveryToken, Initializers.getPoints(territory.getTerritoryName()).get(i), 1);
                i++;
            }
            for (Force force : territory.getForces()) {
                if (force.getName().equals("Hidden Mobile Stronghold")) {
                    BufferedImage hms = getResourceImage("Hidden Mobile Stronghold");
                    hms = resize(hms, 150, 100);
                    List<Force> hmsForces = game.getTerritories().get("Hidden Mobile Stronghold").getForces();
                    int forceOffset = 0;
                    for (Force f : hmsForces) {
                        BufferedImage forceImage = buildForceImage(f.getName(), f.getStrength());
                        hms = overlay(hms, forceImage, new Point(40, 20 + forceOffset), 1);
                        forceOffset += 30;
                    }
                    Point forcePlacement = Initializers.getPoints(territory.getTerritoryName()).get(i);
                    Point forcePlacementOffset = new Point(forcePlacement.x - 55, forcePlacement.y + offset + 5);
                    board = overlay(board, hms, forcePlacementOffset, 1);
                    continue;
                }
                BufferedImage forceImage = buildForceImage(force.getName(), force.getStrength());
                Point forcePlacement = Initializers.getPoints(territory.getTerritoryName()).get(i);
                Point forcePlacementOffset = new Point(forcePlacement.x, forcePlacement.y + offset);
                board = overlay(board, forceImage, forcePlacementOffset, 1);
                i++;
                if (i == Initializers.getPoints(territory.getTerritoryName()).size()) {
                    offset += 20;
                    i = 0;
                }
            }

            if (game.hasFaction("Richese") && territory.hasRicheseNoField()) {
                BufferedImage noFieldImage = resize(getResourceImage("No-Field Hidden"), 30, 30);
                Point noFieldPlacement = Initializers.getPoints(territory.getTerritoryName())
                        .get(i);
                board = overlay(board, noFieldImage, noFieldPlacement, 1);
            }

            //Storm overlay
            BufferedImage stormOverlay = getResourceImage("Storm Overlay");
            stormOverlay = rotateImageByDegrees(stormOverlay, -(game.getStorm() * 20) + 20);
            board = overlay(board, stormOverlay, Initializers.calculateStormCoordinates(game.getStorm()), .15F);
            BufferedImage stormMarker = getResourceImage("storm");
            stormMarker = resize(stormMarker, 172, 96);
            stormMarker = rotateImageByDegrees(stormMarker, -(game.getStorm() * 20));
            board = overlay(board, stormMarker, Initializers.getDrawCoordinates("storm " + game.getStorm()), 1);

            //Place sigils
            for (int k = 1; k <= game.getFactions().size(); k++) {
                Faction faction = game.getFactions().get(k - 1);
                BufferedImage sigil = getResourceImage(faction.getName() + " Sigil");
                coordinates = Initializers.getDrawCoordinates("sigil " + k);
                sigil = resize(sigil, 50, 50);
                board = overlay(board, sigil, coordinates, 1);

                // Check for alliances
                if (faction.hasAlly()) {
                    BufferedImage allySigil =
                            getResourceImage(faction.getAlly() + " Sigil");
                    coordinates = Initializers.getDrawCoordinates("ally " + k);
                    allySigil = resize(allySigil, 40, 40);
                    board = overlay(board, allySigil, coordinates, 1);
                }
            }
        }

        //Place tanks forces
        int i = 0;
        int offset = 0;
        for (Force force : game.getTanks()) {
            if (force.getStrength() == 0) continue;
            BufferedImage forceImage = buildForceImage(force.getName(), force.getStrength());

            Point tanksCoordinates = Initializers.getPoints("Forces Tanks").get(i);
            Point tanksOffset = new Point(tanksCoordinates.x, tanksCoordinates.y - offset);

            board = overlay(board, forceImage, tanksOffset, 1);
            i++;
            if (i > 1) {
                offset += 30;
                i = 0;
            }
        }

        //Place tanks leaders
        i = 0;
        offset = 0;
        for (Leader leader : game.getLeaderTanks()) {
            BufferedImage leaderImage;
            if (leader.faceDown()) {
                leaderImage = getResourceImage(leaderToFaction.get(leader.name()) + " Sigil");
            } else {
                leaderImage = getResourceImage(leader.name());
            }
            if (!leader.name().equals("Kwisatz Haderach")) leaderImage = resize(leaderImage, 70, 70);
            else leaderImage = resize(leaderImage, 70, 42);
            Point tanksCoordinates = Initializers.getPoints("Leaders Tanks").get(i);
            Point tanksOffset = new Point(tanksCoordinates.x, tanksCoordinates.y - offset);
            board = overlay(board, leaderImage, tanksOffset, 1);
            i++;
            if (i > Initializers.getPoints("Leaders Tanks").size() - 1) {
                offset += 70;
                i = 0;
            }
        }

        ByteArrayOutputStream boardOutputStream = new ByteArrayOutputStream();
        ImageIO.write(board, "png", boardOutputStream);

        return FileUpload.fromData(boardOutputStream.toByteArray(), "board.png");
    }

    private static BufferedImage buildForceImage(String force, int strength) throws IOException {
        BufferedImage forceImage = !force.equals("Advisor") ? getResourceImage(force.replace("*", "") + " Troop") : getResourceImage("BG Advisor");
        forceImage = resize(forceImage, 47, 29);
        if (force.contains("*")) {
            BufferedImage star = getResourceImage("star");
            star = resize(star, 8, 8);
            forceImage = overlay(forceImage, star, new Point(20, 7), 1);
        }
        if (strength == 20) {
            BufferedImage twoImage = getResourceImage("2");
            BufferedImage zeroImage = getResourceImage("0");
            twoImage = resize(twoImage, 12, 12);
            zeroImage = resize(zeroImage, 12, 12);
            forceImage = overlay(forceImage, twoImage, new Point(28, 14), 1);
            forceImage = overlay(forceImage, zeroImage, new Point(36, 14), 1);
        } else if (strength > 9) {
            BufferedImage oneImage = getResourceImage("1");
            BufferedImage digitImage = getResourceImage(String.valueOf(strength - 10));
            oneImage = resize(oneImage, 12, 12);
            digitImage = resize(digitImage, 12, 12);
            forceImage = overlay(forceImage, oneImage, new Point(28, 14), 1);
            forceImage = overlay(forceImage, digitImage, new Point(36, 14), 1);
        } else {
            BufferedImage numberImage = getResourceImage(String.valueOf(strength));
            numberImage = resize(numberImage, 12, 12);
            forceImage = overlay(forceImage, numberImage, new Point(30, 14), 1);

        }
        return forceImage;
    }

    private static BufferedImage resize(BufferedImage img, int newW, int newH) {
        Image tmp = img.getScaledInstance(newW, newH, Image.SCALE_SMOOTH);
        BufferedImage dimg = new BufferedImage(newW, newH, BufferedImage.TYPE_INT_ARGB);

        Graphics2D g2d = dimg.createGraphics();
        g2d.drawImage(tmp, 0, 0, null);
        g2d.dispose();

        return dimg;
    }

    private static BufferedImage overlay(BufferedImage board, BufferedImage piece, Point coordinates, float alpha) {

        int compositeRule = AlphaComposite.SRC_OVER;
        AlphaComposite ac;
        ac = AlphaComposite.getInstance(compositeRule, alpha);
        BufferedImage overlay = new BufferedImage(board.getWidth(), board.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = overlay.createGraphics();
        g.drawImage(board, 0, 0, null);
        g.setComposite(ac);
        g.drawImage(piece, coordinates.x - (piece.getWidth() / 2), coordinates.y - (piece.getHeight() / 2), null);
        g.setComposite(ac);
        g.dispose();

        return overlay;
    }

    private static BufferedImage concatenateHorizontally(BufferedImage img1, BufferedImage img2) {
        int offset = 2;
        int width = img1.getWidth() + img2.getWidth() + offset;
        int height = Math.max(img1.getHeight(), img2.getHeight()) + offset;
        BufferedImage newImage = new BufferedImage(width, height,
                BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = newImage.createGraphics();
        Color oldColor = g2.getColor();
        g2.setPaint(Color.BLACK);
        g2.fillRect(0, 0, width, height);
        g2.setColor(oldColor);
        g2.drawImage(img1, null, 0, 0);
        g2.drawImage(img2, null, img1.getWidth() + offset, 0);
        g2.dispose();
        return newImage;
    }

    private static BufferedImage concatenateVertically(BufferedImage img1, BufferedImage img2) {
        int offset = 2;
        int height = img1.getHeight() + img2.getHeight() + offset;
        int width = Math.max(img1.getWidth(), img2.getWidth()) + offset;
        BufferedImage newImage = new BufferedImage(width, height,
                BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = newImage.createGraphics();
        Color oldColor = g2.getColor();
        g2.setPaint(Color.BLACK);
        g2.fillRect(0, 0, width, height);
        g2.setColor(oldColor);
        g2.drawImage(img1, null, 0, 0);
        g2.drawImage(img2, null, 0, img1.getHeight() + offset);
        g2.dispose();
        return newImage;
    }

    private static BufferedImage rotateImageByDegrees(BufferedImage img, double angle) {
        double rads = Math.toRadians(angle);
        double sin = Math.abs(Math.sin(rads)), cos = Math.abs(Math.cos(rads));
        int w = img.getWidth();
        int h = img.getHeight();
        int newWidth = (int) Math.floor(w * cos + h * sin);
        int newHeight = (int) Math.floor(h * cos + w * sin);

        BufferedImage rotated = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = rotated.createGraphics();
        AffineTransform at = new AffineTransform();
        at.translate((double) (newWidth - w) / 2, (double) (newHeight - h) / 2);

        int x = w / 2;
        int y = h / 2;

        at.rotate(rads, x, y);
        g2d.setTransform(at);
        g2d.drawImage(img, 0, 0, null);
        g2d.dispose();

        return rotated;
    }

    public static void writeFactionInfo(DiscordGame discordGame, String factionName) throws ChannelNotFoundException, IOException {
        writeFactionInfo(discordGame, discordGame.getGame().getFaction(factionName));
    }

    /**
     * Writes the faction info to the faction-info channel
     *
     * @param discordGame The Discord Game
     * @param faction     The Faction whose info to write
     * @throws ChannelNotFoundException if the channel is not found
     * @throws IOException              if the image cannot be written
     */
    public static void writeFactionInfo(DiscordGame discordGame, Faction faction) throws ChannelNotFoundException, IOException {
        MessageChannel infoChannel = discordGame.getTextChannel(faction.getName().toLowerCase() + "-info");
        MessageHistory messageHistory = MessageHistory.getHistoryFromBeginning(infoChannel).complete();

        List<Message> messages = messageHistory.getRetrievedHistory();

        messages.forEach(discordGame::queueDeleteMessage);

        String emoji = faction.getEmoji();
        List<TraitorCard> traitors = faction.getTraitorHand();
        String infoChannelName = faction.getName().toLowerCase() + "-info";
        StringBuilder factionSpecificString = new StringBuilder();
        String nexusCard = faction.getNexusCard() == null ? "" : "\nNexus Card:\n" + Emojis.TRANSPARENT_WORM + faction.getNexusCard().name() + Emojis.TRANSPARENT_WORM;

        if (faction.getName().equalsIgnoreCase("bg")) {
            BGFaction bg = (BGFaction) faction;
            factionSpecificString.append("\n__Prediction:__ ")
                    .append(bg.getPredictionFactionName())
                    .append(" Turn ")
                    .append(bg.getPredictionRound());
        } else if (faction.getName().equalsIgnoreCase("ecaz")) {
            EcazFaction ecaz = (EcazFaction) faction;
            factionSpecificString.append(ecaz.getAmbassadorSupply());
        } else if (faction.getName().equalsIgnoreCase("moritani")) {
            MoritaniFaction moritani = (MoritaniFaction) faction;
            factionSpecificString.append(moritani.getTerrorTokenMessage());
        }
        StringBuilder traitorString = new StringBuilder();
        if (faction.getName().equals("BT")) traitorString.append("\n__Face Dancers:__\n");
        else traitorString.append("\n__Traitors:__\n");
        for (TraitorCard traitor : traitors) {
            if (traitor.name().equals("Cheap Hero")) {
                traitorString.append("Cheap Hero (0)\n");
                continue;
            }
            String traitorEmoji = discordGame.getGame().getFaction(traitor.factionName()).getEmoji();
            traitorString.append(traitorEmoji).append(" ").append(traitor.name()).append("(").append(traitor.strength()).append(")");
            traitorString.append("\n");
        }
        StringBuilder reservesString = new StringBuilder();
        reservesString.append("\n__Reserves:__ ").append(faction.getReserves().getStrength());
        if (faction.getName().equalsIgnoreCase("Fremen"))
            reservesString.append("\n__Fedaykin Reserves:__ ").append(faction.getSpecialReserves().getStrength());
        if (faction.getName().equalsIgnoreCase("Emperor"))
            reservesString.append("\n__Sardaukar Reserves:__ ").append(faction.getSpecialReserves().getStrength());
        if (faction.getName().equalsIgnoreCase("Ix"))
            reservesString.append("\n__Cyborg Reserves:__ ").append(faction.getSpecialReserves().getStrength());

        StringBuilder ornithopter = new StringBuilder();
        if (faction.hasOrnithoperToken()) ornithopter.append("\nOrnithopter Token\n");
        MessageCreateBuilder builder = new MessageCreateBuilder()
                .addContent(emoji + "**Faction Info**" + emoji + "\n__Spice:__ " +
                        faction.getSpice() +
                        reservesString +
                        ornithopter +
                        nexusCard +
                        factionSpecificString +
                        traitorString);
        for (Leader leader : faction.getLeaders()) {
            builder = builder.addFiles(getResourceFile(leader.name()));
        }

        MessageCreateData data = builder.build();

        discordGame.queueMessage(infoChannelName, data);

        StringBuilder homeworld = new StringBuilder();
        if (faction.getGame().hasGameOption(GameOption.HOMEWORLDS)) {
            MessageCreateBuilder homeworldMessageBuilder = new MessageCreateBuilder();
            homeworld.append(faction.getHomeworld());
            String lowHigh = faction.isHighThreshold() ? "High" : "Low";
            homeworldMessageBuilder.addContent(faction.getHomeworld()).addFiles(CardImages.getHomeworldImage(discordGame.getEvent().getGuild(), faction.getHomeworld() + " " + lowHigh).get());
            if (faction.getName().equals("Emperor")) {
                lowHigh = ((EmperorFaction) faction).isSecundusHighThreshold() ? "High" : "Low";
                homeworldMessageBuilder.addFiles(CardImages.getHomeworldImage(discordGame.getEvent().getGuild(), "Salusa Secundus " + lowHigh).get());
            }
            discordGame.queueMessage(infoChannelName, homeworldMessageBuilder);
        }

        List<TreacheryCard> treacheryCards = faction.getTreacheryHand();
        if (treacheryCards.isEmpty()) return;

        MessageCreateBuilder treacheryCardMessageBuilder = new MessageCreateBuilder();
        StringBuilder treacheryString = new StringBuilder();
        treacheryString.append("\n__Treachery Cards:__\n");
        for (TreacheryCard treachery : treacheryCards) {
            treacheryString.append(Emojis.TREACHERY)
                    .append(" ")
                    .append(treachery.name())
                    .append("\n");

            Optional<FileUpload> image = CardImages.getTreacheryCardImage(discordGame.getEvent().getGuild(), treachery.name());
            if (image.isPresent())
                treacheryCardMessageBuilder = treacheryCardMessageBuilder.addFiles(image.get());
        }

        treacheryCardMessageBuilder.addContent(treacheryString.toString());
        discordGame.queueMessage(infoChannelName, treacheryCardMessageBuilder.build());
        sendInfoButtons(discordGame.getGame(), discordGame, faction);
    }

    public static void refreshFrontOfShieldInfo(DiscordGame discordGame, Game game) throws ChannelNotFoundException, IOException {
        MessageChannel frontOfShieldChannel = discordGame.getTextChannel("front-of-shield");
        MessageHistory messageHistory = MessageHistory.getHistoryFromBeginning(frontOfShieldChannel).complete();

        List<Message> messages = messageHistory.getRetrievedHistory();

        messages.forEach(discordGame::queueDeleteMessage);
        for (Faction faction : game.getFactions()) {
            StringBuilder message = new StringBuilder();
            List<FileUpload> uploads = new ArrayList<>();

            message.append(
                    MessageFormat.format(
                            "{0}{1} Info{0}\n",
                            faction.getEmoji(), faction.getName()
                    )
            );

            if (game.hasGameOption(GameOption.SPICE_PUBLIC)) {
                message.append("Back of shield Spice: ")
                        .append(faction.getSpice())
                        .append(" " + Emojis.SPICE + "\n");
            }

            if (faction.getFrontOfShieldSpice() > 0) {
                message.append("Front of shield spice: ")
                        .append(faction.getFrontOfShieldSpice())
                        .append(" " + Emojis.SPICE + "\n");
            }

            if (game.hasGameOption(GameOption.TREACHERY_CARD_COUNT_PUBLIC)) {
                message.append("Treachery Cards: ")
                        .append(faction.getTreacheryHand().size())
                        .append("\n");
            }

            if (faction.getName().equalsIgnoreCase("Richese") && ((RicheseFaction) faction).hasFrontOfShieldNoField()) {
                message.append(((RicheseFaction) faction).getFrontOfShieldNoField())
                        .append(" No-Field Token\n");
            }

            if (faction.getName().equalsIgnoreCase("Ecaz")) {
                message.append(((EcazFaction) faction).getLoyalLeader().name()).append(" is loyal to " + Emojis.ECAZ + "\n");
            }

            if (faction.getName().equalsIgnoreCase("Moritani")) {
                message.append("Assassinated targets:\n").append(((MoritaniFaction) faction).getAssassinationTargets().toString());
            }

            if (game.hasLeaderSkills()) {
                List<Leader> skilledLeaders = faction.getSkilledLeaders();

                for (Leader leader : skilledLeaders) {
                    message.append(
                            MessageFormat.format(
                                    "{0} is a {1}\n",
                                    leader.name(), leader.skillCard().name()
                            )
                    );

                    Optional<FileUpload> fileUpload = CardImages
                            .getLeaderSkillImage(discordGame.getEvent().getGuild(), leader.skillCard().name());

                    fileUpload.ifPresent(uploads::add);
                }
            }

            if (game.hasStrongholdSkills()) {
                for (StrongholdCard strongholdCard : faction.getStrongholdCards()) {
                    String strongholdName = strongholdCard.name();
                    message.append(strongholdName).append(" Stronghold Skill\n");

                    Optional<FileUpload> fileUpload = CardImages
                            .getStrongholdImage(discordGame.getEvent().getGuild(), strongholdName);

                    fileUpload.ifPresent(uploads::add);
                }
            }

            if (uploads.isEmpty()) {
                discordGame.queueMessage("front-of-shield", message.toString());
            } else {
                discordGame.queueMessage("front-of-shield", message.toString(), uploads);
            }
        }
        if (game.hasGameOption(GameOption.MAP_IN_FRONT_OF_SHIELD)) {
            String mapFilename = "game-map.png";
            FileUpload newMap = drawGameBoard(game).setName(mapFilename);
            discordGame.queueMessage("front-of-shield", "", newMap);
        }
    }

    public static void refreshChangedInfo(DiscordGame discordGame) throws ChannelNotFoundException, IOException {
        Game game = discordGame.getGame();
        boolean frontOfShieldModified = false;
        for (Faction faction : game.getFactions()) {
            Set<UpdateType> updateTypes = faction.getUpdateTypes();
            if (
                    updateTypes.contains(UpdateType.MISC_BACK_OF_SHIELD) ||
                            updateTypes.contains(UpdateType.SPICE_BACK) ||
                            updateTypes.contains(UpdateType.TREACHERY_CARDS)
            ) {
                if (faction.isGraphicDisplay()) drawFactionInfo(discordGame, game, faction.getName());
                else writeFactionInfo(discordGame, faction);
            }

            if (updateTypes.contains(UpdateType.MISC_FRONT_OF_SHIELD)) {
                frontOfShieldModified = true;
            }

            if (game.hasGameOption(GameOption.SPICE_PUBLIC) && updateTypes.contains(UpdateType.SPICE_BACK)) {
                frontOfShieldModified = true;
            }

            if (game.hasGameOption(GameOption.TREACHERY_CARD_COUNT_PUBLIC) &&
                    updateTypes.contains(UpdateType.TREACHERY_CARDS)) {
                frontOfShieldModified = true;
            }

            if (game.hasGameOption(GameOption.MAP_IN_FRONT_OF_SHIELD) &&
                    updateTypes.contains(UpdateType.MAP)) {
                frontOfShieldModified = true;
            }
        }
        if (game.getUpdateTypes().contains(UpdateType.MAP)) {
            frontOfShieldModified = true;
        }

        if (frontOfShieldModified) {
            refreshFrontOfShieldInfo(discordGame, game);
        }
    }
}
