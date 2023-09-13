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
import java.util.*;
import java.util.List;

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
        drawGameBoard(discordGame, game);
    }

    public static void showFactionInfo(DiscordGame discordGame) throws ChannelNotFoundException, IOException {
        String factionName = discordGame.required(faction).getAsString();
        if (discordGame.getGame().getFaction(factionName).isGraphicDisplay()) drawFactionInfo(discordGame, discordGame.getGame(), factionName);
        else writeFactionInfo(discordGame, factionName);
    }

    private static BufferedImage getResourceImage(String name) throws IOException {
        URL file = ShowCommands.class.getClassLoader().getResource("Board Components/" + name + ".png");
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
        BufferedImage table = getResourceImage("behind shield");

        //Place reserves
        int reserves = faction.getReserves().getStrength();
        int specialReserves = faction.getSpecialReserves().getStrength();

        if (reserves > 0) {
            BufferedImage reservesImage = buildForceImage(faction.getName(), reserves);
            table = overlay(table, reservesImage, new Point(80, 50), 1);
        }
        if (specialReserves > 0) {
            BufferedImage specialReservesImage = buildForceImage(faction.getName() + "*", specialReserves);
            table = overlay(table, specialReservesImage, new Point(80, 90), 1);
        }

        //Place spice
        int spice = faction.getSpice();
        int offset = 0;
        Point spicePlacement = new Point(150, 50);
        while (spice != 0) {
            if (spice >= 10) {
                BufferedImage spiceImage = getResourceImage("10 Spice");
                spiceImage = resize(spiceImage, 25,25);
                Point spicePlacementOffset = new Point(spicePlacement.x + offset, spicePlacement.y);
                table = overlay(table, spiceImage, spicePlacementOffset, 1);
                spice -= 10;
            } else if (spice == 5) {
                BufferedImage spiceImage = getResourceImage("5 Spice");
                spiceImage = resize(spiceImage, 25,25);
                Point spicePlacementOffset = new Point(spicePlacement.x + offset, spicePlacement.y);
                table = overlay(table, spiceImage, spicePlacementOffset, 1);
                spice -= 5;
            } else if (spice >= 2) {
                BufferedImage spiceImage = getResourceImage("2 Spice");
                spiceImage = resize(spiceImage, 25,25);
                Point spicePlacementOffset = new Point(spicePlacement.x + offset, spicePlacement.y);
                table = overlay(table, spiceImage, spicePlacementOffset, 1);
                spice -= 2;
            } else {
                BufferedImage spiceImage = getResourceImage("1 Spice");
                spiceImage = resize(spiceImage, 25,25);
                Point spicePlacementOffset = new Point(spicePlacement.x + offset, spicePlacement.y);
                table = overlay(table, spiceImage, spicePlacementOffset, 1);
                spice -= 1;
            }
            offset += 15;
        }

        offset = 0;

        //Place leaders
        for (Leader leader : faction.getLeaders()) {
            BufferedImage leaderImage = getResourceImage(leader.name());
            leaderImage = resize(leaderImage, 70, 70);
            Point leaderPoint = new Point(150 + offset, 100);
            table = overlay(table, leaderImage, leaderPoint, 1);
            offset += 50;
        }

        offset = 0;

        //Place Treachery Cards
        for (TreacheryCard treacheryCard : faction.getTreacheryHand()) {
            Optional<FileUpload> image = CardImages.getTreacheryCardImage(discordGame.getEvent().getGuild(), treacheryCard.name());
            if (image.isPresent()) {
                BufferedImage cardImage = ImageIO.read(image.get().getData());
                cardImage = resize(cardImage, 107, 150);
                Point cardPoint = new Point(100 + offset, 250);
                table = overlay(table, cardImage, cardPoint, 1);
                offset += 100;
        }
        }

        offset = 0;

        //Place Traitor Cards
        for (TraitorCard traitorCard : faction.getTraitorHand()) {
            Optional<FileUpload> image = CardImages.getTraitorImage(discordGame.getEvent().getGuild(), traitorCard.name());
            if (image.isPresent()) {
                BufferedImage cardImage = ImageIO.read(image.get().getData());
                cardImage = resize(cardImage, 107, 150);
                Point cardPoint = new Point(100 + offset, 450);
                table = overlay(table, cardImage, cardPoint, 1);
                offset += 100;
            }
        }


        ByteArrayOutputStream boardOutputStream = new ByteArrayOutputStream();
        ImageIO.write(table, "png", boardOutputStream);

        FileUpload boardFileUpload = FileUpload.fromData(boardOutputStream.toByteArray(), "behind shield.png");

        discordGame.queueMessage(faction.getName().toLowerCase() + "-info", "Faction Info", boardFileUpload);

        discordGame.queueMessage(faction.getName().toLowerCase() + "-info", new MessageCreateBuilder().addContent("Use these buttons to take the corresponding actions.")
                .addActionRow(Button.secondary("graphic", "-info channel graphic mode"), Button.secondary("text", "-info channel text mode")).build());


    }
    private static void drawGameBoard(DiscordGame discordGame, Game game) throws ChannelNotFoundException, IOException {
        if (game.getMute()) return;

        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(
                Objects.requireNonNull(Faction.class.getClassLoader().getResourceAsStream("Leaders.csv"))
        ));
        CSVParser csvParser = CSVParser.parse(bufferedReader, CSVFormat.EXCEL);

        HashMap<String, String> leaderToFaction = new HashMap<>();

        for (CSVRecord csvRecord : csvParser) {
            leaderToFaction.put(csvRecord.get(1), csvRecord.get(0));
        }

        BufferedImage board = getResourceImage("Board");

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
        BufferedImage stormMarker = getResourceImage("storm");
        stormMarker = resize(stormMarker, 172, 96);
        stormMarker = rotateImageByDegrees(stormMarker, -(game.getStorm() * 20));
        board = overlay(board, stormMarker, Initializers.getDrawCoordinates("storm " + game.getStorm()), 1);

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

        //Place sigils
        for (int i = 1; i <= game.getFactions().size(); i++) {
            Faction faction = game.getFactions().get(i - 1);
            BufferedImage sigil = getResourceImage(faction.getName() + " Sigil");
            coordinates = Initializers.getDrawCoordinates("sigil " + i);
            sigil = resize(sigil, 50, 50);
            board = overlay(board, sigil, coordinates, 1);

            // Check for alliances
            if (faction.hasAlly()) {
                BufferedImage allySigil =
                        getResourceImage(faction.getAlly() + " Sigil");
                coordinates = Initializers.getDrawCoordinates("ally " + i);
                allySigil = resize(allySigil, 40, 40);
                board = overlay(board, allySigil, coordinates, 1);
            }
        }


        //Place pieces in territories
        for (Territory territory : game.getTerritories().values()) {
            if (territory.getForces().isEmpty() && territory.getSpice() == 0
                    && !territory.hasRicheseNoField() && territory.getEcazAmbassador() == null
                    && !territory.isAftermathToken() && !territory.hasTerrorToken()) continue;
            if (territory.getTerritoryName().equals("Hidden Mobile Stronghold")) continue;
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
                BufferedImage ambassadorSigil = getResourceImage(territory.getEcazAmbassador() + " Sigil");
                BufferedImage ambassadorBackground = getResourceImage("Ambassador");
                BufferedImage ambassador = overlay(
                        resize(ambassadorBackground, 40, 40),
                        resize(ambassadorSigil, 22, 22),
                        new Point(20,20),
                        1
                );
                Point placement = Initializers.getPoints(territory.getTerritoryName()).get(1);
                Point placementCorner = new Point(placement.x + 40, placement.y);
                board = overlay(board, ambassador, placementCorner, 1);
            }

            if (territory.getTerrorToken() != null) {
                BufferedImage terrorToken = getResourceImage("Terror Token");
                terrorToken = resize(terrorToken, 40, 40);
                Point placement = Initializers.getPoints(territory.getTerritoryName()).get(1);
                Point placementCorner = new Point(placement.x - 20, placement.y);
                board = overlay(board, terrorToken, placementCorner, 1);
            }

            if (territory.getSpice() != 0) {
                i = 1;
                int spice = territory.getSpice();
                while (spice != 0) {
                    if (spice >= 10) {
                        BufferedImage spiceImage = getResourceImage("10 Spice");
                        spiceImage = resize(spiceImage, 25,25);
                        Point spicePlacement = Initializers.getPoints(territory.getTerritoryName()).get(0);
                        Point spicePlacementOffset = new Point(spicePlacement.x + offset, spicePlacement.y - offset);
                        board = overlay(board, spiceImage, spicePlacementOffset, 1);
                        spice -= 10;
                    } else if (spice >= 8) {
                        BufferedImage spiceImage = getResourceImage("8 Spice");
                        spiceImage = resize(spiceImage, 25,25);
                        Point spicePlacement = Initializers.getPoints(territory.getTerritoryName()).get(0);
                        Point spicePlacementOffset = new Point(spicePlacement.x + offset, spicePlacement.y - offset);
                        board = overlay(board, spiceImage, spicePlacementOffset, 1);
                        spice -= 8;
                    } else if (spice >= 6) {
                        BufferedImage spiceImage = getResourceImage("6 Spice");
                        spiceImage = resize(spiceImage, 25,25);
                        Point spicePlacement = Initializers.getPoints(territory.getTerritoryName()).get(0);
                        Point spicePlacementOffset = new Point(spicePlacement.x + offset, spicePlacement.y - offset);
                        board = overlay(board, spiceImage, spicePlacementOffset, 1);
                        spice -= 6;
                    } else if (spice == 5) {
                        BufferedImage spiceImage = getResourceImage("5 Spice");
                        spiceImage = resize(spiceImage, 25,25);
                        Point spicePlacement = Initializers.getPoints(territory.getTerritoryName()).get(0);
                        Point spicePlacementOffset = new Point(spicePlacement.x + offset, spicePlacement.y - offset);
                        board = overlay(board, spiceImage, spicePlacementOffset, 1);
                        spice -= 5;
                    } else if (spice >= 2) {
                        BufferedImage spiceImage = getResourceImage("2 Spice");
                        spiceImage = resize(spiceImage, 25,25);
                        Point spicePlacement = Initializers.getPoints(territory.getTerritoryName()).get(0);
                        Point spicePlacementOffset = new Point(spicePlacement.x + offset, spicePlacement.y - offset);
                        board = overlay(board, spiceImage, spicePlacementOffset, 1);
                        spice -= 2;
                    } else {
                        BufferedImage spiceImage = getResourceImage("1 Spice");
                        spiceImage = resize(spiceImage, 25,25);
                        Point spicePlacement = Initializers.getPoints(territory.getTerritoryName()).get(0);
                        Point spicePlacementOffset = new Point(spicePlacement.x + offset, spicePlacement.y - offset);
                        board = overlay(board, spiceImage, spicePlacementOffset, 1);
                        spice -= 1;
                    }
                    offset += 15;
                }
            }
            offset = 0;
            for (Force force : territory.getForces()) {
                if (force.getName().equals("Hidden Mobile Stronghold")) {
                    BufferedImage hms = getResourceImage("Hidden Mobile Stronghold");
                    hms = resize(hms, 150,100);
                    List<Force> hmsForces = game.getTerritories().get("Hidden Mobile Stronghold").getForces();
                    int forceOffset = 0;
                    for (Force f : hmsForces) {
                        BufferedImage forceImage = buildForceImage(f.getName(), f.getStrength());
                        hms = overlay(hms, forceImage, new Point(40,20 + forceOffset), 1);
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
            leaderImage = resize(leaderImage, 70,70);
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

        FileUpload boardFileUpload = FileUpload.fromData(boardOutputStream.toByteArray(), "board.png");
        discordGame.queueMessage("turn-summary", boardFileUpload);
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
            zeroImage = resize(zeroImage, 12,12);
            forceImage = overlay(forceImage, twoImage, new Point(28, 14), 1);
            forceImage = overlay(forceImage, zeroImage, new Point(36, 14), 1);
        } else if (strength > 9) {
            BufferedImage oneImage = getResourceImage("1");
            BufferedImage digitImage = getResourceImage(String.valueOf(strength - 10));
            oneImage = resize(oneImage, 12, 12);
            digitImage = resize(digitImage, 12,12);
            forceImage = overlay(forceImage, oneImage, new Point(28, 14), 1);
            forceImage = overlay(forceImage, digitImage, new Point(36, 14), 1);
        } else {
            BufferedImage numberImage = getResourceImage(String.valueOf(strength));
            numberImage = resize(numberImage, 12, 12);
            forceImage = overlay(forceImage, numberImage, new Point(30,14), 1);

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
        g.drawImage(piece, coordinates.x - (piece.getWidth()/2), coordinates.y - (piece.getHeight()/2), null);
        g.setComposite(ac);
        g.dispose();

        return overlay;
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
     * @param discordGame The Discord Game
     * @param faction The Faction whose info to write
     * @throws ChannelNotFoundException if the channel is not found
     * @throws IOException if the image cannot be written
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
        if (faction.getName().equalsIgnoreCase("Fremen")) reservesString.append("\n__Fedaykin Reserves:__ ").append(faction.getSpecialReserves().getStrength());
        if (faction.getName().equalsIgnoreCase("Emperor")) reservesString.append("\n__Sardaukar Reserves:__ ").append(faction.getSpecialReserves().getStrength());
        if (faction.getName().equalsIgnoreCase("Ix")) reservesString.append("\n__Cyborg Reserves:__ ").append(faction.getSpecialReserves().getStrength());

        MessageCreateBuilder builder = new MessageCreateBuilder()
                .addContent(emoji + "**Faction Info**" + emoji + "\n__Spice:__ " +
                        faction.getSpice() +
                        reservesString +
                        nexusCard +
                        factionSpecificString +
                        traitorString);
        for (Leader leader : faction.getLeaders()) {
            builder = builder.addFiles(getResourceFile(leader.name()));
        }

        MessageCreateData data = builder.build();

        discordGame.queueMessage(infoChannelName, data);

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
        discordGame.queueMessage(faction.getName().toLowerCase() + "-info", new MessageCreateBuilder().addContent("Use these buttons to take the corresponding actions.")
                .addActionRow(Button.secondary("graphic", "-info channel graphic mode"), Button.secondary("text", "-info channel text mode")).build());
    }

    public static void refreshFrontOfShieldInfo(DiscordGame discordGame, Game game) throws ChannelNotFoundException {
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
                        .append(" " + Emojis.SPICE +"\n");
            }

            if (game.hasGameOption(GameOption.TREACHERY_CARD_COUNT_PUBLIC)) {
                message.append("Treachery Cards: ")
                        .append(faction.getTreacheryHand().size())
                        .append("\n");
            }

            if (faction.getName().equalsIgnoreCase("Richese") && ((RicheseFaction)faction).hasFrontOfShieldNoField()) {
                message.append(((RicheseFaction)faction).getFrontOfShieldNoField())
                        .append(" No-Field Token\n");
            }

            if (faction.getName().equalsIgnoreCase("Ecaz")) {
                message.append(((EcazFaction)faction).getLoyalLeader().name()).append(" is loyal to " + Emojis.ECAZ + "\n");
            }

            if (faction.getName().equalsIgnoreCase("Moritani")) {
                message.append("Assassinated targets:\n").append(((MoritaniFaction)faction).getAssassinationTargets().toString());
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
        }

        if (frontOfShieldModified) {
            refreshFrontOfShieldInfo(discordGame, game);
        }
    }
}
