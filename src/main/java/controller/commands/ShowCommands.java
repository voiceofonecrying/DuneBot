package controller.commands;

import constants.Emojis;
import controller.DiscordGame;
import enums.GameOption;
import enums.UpdateType;
import exceptions.ChannelNotFoundException;
import exceptions.InvalidGameStateException;
import model.*;
import model.factions.*;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageHistory;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu;
import net.dv8tion.jda.api.utils.FileUpload;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import utils.CardImages;
import view.factions.FactionView;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.URL;
import java.util.List;
import java.util.*;
import java.util.stream.Collectors;

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

    public static void runCommand(SlashCommandInteractionEvent event, DiscordGame discordGame, Game game) throws ChannelNotFoundException, IOException, InvalidGameStateException {
        String name = event.getSubcommandName();
        if (name == null) throw new IllegalArgumentException("Invalid command name: null");

        game.modExecutedACommand(event.getUser().getAsMention());
        switch (name) {
            case "board" -> showBoard(discordGame, game);
            case "faction-info" -> showFactionInfoHandler(discordGame);
            case "front-of-shields" -> refreshFrontOfShieldInfo(discordGame, game);
        }
    }

    public static void showBoard(DiscordGame discordGame, Game game) throws ChannelNotFoundException, IOException {
        if (game.getMute()) return;
        discordGame.getTurnSummary().queueMessage(drawGameBoard(game));
        game.setUpdated(UpdateType.MAP);
    }
    public static void showFactionInfoHandler(DiscordGame discordGame) throws ChannelNotFoundException, IOException, InvalidGameStateException {
        showFactionInfo(discordGame.required(faction).getAsString(), discordGame);
    }

    public static void showFactionInfo(String factionName, DiscordGame discordGame) throws ChannelNotFoundException, IOException, InvalidGameStateException {
        if (discordGame.getGame().getFaction(factionName).isGraphicDisplay())
            drawFactionInfo(discordGame, discordGame.getGame(), factionName);
        else writeFactionInfo(discordGame, factionName);
    }

    private static BufferedImage getSigilImage(Faction faction) throws IOException {
        if (faction instanceof HomebrewFaction hbFaction)
            return getResourceImage(hbFaction.getFactionProxy() + " Sigil");
        return getResourceImage(faction.getName() + " Sigil");
    }

    private static BufferedImage getHomeworldImage(Faction faction) throws IOException {
        if (faction instanceof HomebrewFaction hbFaction)
            return getResourceImage(hbFaction.getHomeworldProxy());
        return getResourceImage(faction.getHomeworld());
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

    public static void drawFactionInfo(DiscordGame discordGame, Game game, String factionName) throws IOException, ChannelNotFoundException, InvalidGameStateException {
        if (game.getMute()) return;

        Faction faction = game.getFaction(factionName);

        MessageChannel infoChannel = discordGame.getTextChannel(faction.getInfoChannelPrefix() + "-info");
        MessageHistory messageHistory = MessageHistory.getHistoryFromBeginning(infoChannel).complete();

        List<Message> messages = messageHistory.getRetrievedHistory();

        messages.forEach(discordGame::queueDeleteMessage);

        BufferedImage table = getHomeworldImage(faction);
        if (faction instanceof EmperorFaction emperorFaction) {
            table = getResourceImage(emperorFaction.getSecondHomeworld());
        }
        table = resize(table, 5000, 5000);

        //Place reserves
        int reserves = faction.getReservesStrength();
        int specialReserves = faction.getSpecialReservesStrength();

        if (reserves > 0) {
            BufferedImage reservesImage = buildForceImage(game, new Force(faction.getName(), reserves));
            reservesImage = resize(reservesImage, 353, 218);
            table = overlay(table, reservesImage, new Point(300, 200), 1);
        }
        if (specialReserves > 0) {
            BufferedImage specialReservesImage = buildForceImage(game, new Force(faction.getName() + "*", specialReserves));
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

        //Place leaders
        StringBuilder leadersInTerritories = new StringBuilder();
        int numLeaders = faction.getLeaders().size();
        offset = (numLeaders - 1) * 450;
        for (Leader leader : faction.getLeaders()) {
            if (faction instanceof HomebrewFaction)
                break;
            BufferedImage leaderImage = getResourceImage(leader.getName());
            if (!leader.getName().equals("Kwisatz Haderach")) leaderImage = resize(leaderImage, 500, 500);
            else leaderImage = resize(leaderImage, 500, 301);
            Point leaderPoint = new Point(300, 750 + offset);
            table = overlay(table, leaderImage, leaderPoint, 1);
            offset -= 450;
            if (leader.getBattleTerritoryName() != null)
                leadersInTerritories.append(leader.getName()).append(" is in ").append(leader.getBattleTerritoryName()).append("\n");
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
            if (!traitorCard.getName().equals("Cheap Hero") && game.getFaction(traitorCard.getFactionName()) instanceof HomebrewFaction)
                traitorCard = new TraitorCard("Cheap Hero",  "Any", 0);
            Optional<FileUpload> image = CardImages.getTraitorImage(discordGame.getEvent().getGuild(), traitorCard.getName());
            if (image.isPresent()) {
                BufferedImage cardImage = ImageIO.read(image.get().getData());
                cardImage = resize(cardImage, 988, 1376);
                Point cardPoint = new Point(1050 + offset, 3500);
                table = overlay(table, cardImage, cardPoint, 1);
                offset += 900;
            }
        }

        //Place KH counter card
        if (faction instanceof AtreidesFaction atreidesFaction) {
            BufferedImage KHCounterImage = getResourceImage("KH Counter");
            int x;
            int y;
            switch (atreidesFaction.getForcesLost()) {
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
            String homeworldName = faction.getHomeworld();
            Optional<FileUpload> image = CardImages.getHomeworldImage(discordGame.getEvent().getGuild(), homeworldName + " " + lowHigh);
            if (image.isPresent()) {
                BufferedImage cardImage = ImageIO.read(image.get().getData());
                cardImage = resize(cardImage, 988, 1376);
                Point cardPoint = new Point(4500, 750);
                table = overlay(table, cardImage, cardPoint, 1);
            }
            if (faction instanceof EmperorFaction emperor) {
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

        String nexusCard = "";
        //Place nexus card if any
        if (faction.getNexusCard() != null) {
            Optional<FileUpload> image = CardImages.getNexusImage(discordGame.getEvent().getGuild(), faction.getNexusCard().name());
            if (image.isPresent()) {
                BufferedImage cardImage = ImageIO.read(image.get().getData());
                cardImage = resize(cardImage, 988, 1376);
                Point cardPoint = new Point(750 + offset, 3500);
                table = overlay(table, cardImage, cardPoint, 1);
            } else
                nexusCard = Emojis.NEXUS + " " + faction.getNexusCard().name();
        }

        //BG Prediction
        if (faction instanceof BGFaction bgFaction) {
            offset += 900;
            Optional<FileUpload> image = CardImages.getPredictionImage(discordGame.getEvent().getGuild(), "Turn " + bgFaction.getPredictionRound());
            if (image.isPresent()) {
                BufferedImage cardImage = ImageIO.read(image.get().getData());
                cardImage = resize(cardImage, 988, 1376);
                Point cardPoint = new Point(750 + offset, 3500);
                table = overlay(table, cardImage, cardPoint, 1);
            }
            offset += 900;
            image = CardImages.getPredictionImage(discordGame.getEvent().getGuild(), bgFaction.getPredictionFactionName());
            if (image.isPresent()) {
                BufferedImage cardImage = ImageIO.read(image.get().getData());
                cardImage = resize(cardImage, 988, 1376);
                Point cardPoint = new Point(750 + offset, 3500);
                table = overlay(table, cardImage, cardPoint, 1);
            }
        }

        offset = 0;

        //Ecaz ambassadors
        if (faction instanceof EcazFaction ecazFaction) {
            for (String ambassador : ecazFaction.getAmbassadorSupply()) {
                BufferedImage ambassadorImage = getResourceImage(ambassador + " Ambassador");
                table = overlay(table, resize(ambassadorImage, 300, 300), new Point(750 + offset, 2250), 1);
                offset += 330;
            }
        }

        //Moritani Terror Tokens
        if (faction instanceof MoritaniFaction moritaniFaction) {
            for (String terrorToken : moritaniFaction.getTerrorTokens()) {
                BufferedImage terrorTokenImage = getResourceImage(terrorToken);
                table = overlay(table, resize(terrorTokenImage, 300, 300), new Point(750 + offset, 2250), 1);
                offset += 330;
            }
        }

        table = resize(table, 1024, 1024);
        ByteArrayOutputStream boardOutputStream = new ByteArrayOutputStream();
        ImageIO.write(table, "png", boardOutputStream);

        FileUpload boardFileUpload = FileUpload.fromData(boardOutputStream.toByteArray(), "behind shield.png");

        String infoChannelName = faction.getInfoChannelPrefix() + "-info";
        discordGame.queueMessage(infoChannelName, "Faction Info", boardFileUpload);
        if (!nexusCard.isEmpty())
            discordGame.queueMessage(infoChannelName, "__Nexus Card:__\n" + Emojis.NEXUS + faction.getNexusCard().name());
        if (faction instanceof HomebrewFaction)
            discordGame.queueMessage(infoChannelName, "__Leaders:__\n" + String.join("\n", faction.getLeaders().stream().map(Leader::getEmoiNameAndValueString).toList()));
        List<String> homebrewTraitors = new ArrayList<>();
        for (TraitorCard traitorCard : faction.getTraitorHand())
            if (!traitorCard.getName().equals("Cheap Hero") && game.getFaction(traitorCard.getFactionName()) instanceof HomebrewFaction)
                homebrewTraitors.add("Cheap Hero (0) Traitor above is really " + traitorCard.getEmojiNameAndStrengthString());
        if (!homebrewTraitors.isEmpty())
            discordGame.queueMessage(infoChannelName, "__Traitors:__\n" + String.join("\n", homebrewTraitors));
        if (!leadersInTerritories.isEmpty())
            discordGame.queueMessage(infoChannelName, leadersInTerritories.toString());
        if (faction instanceof MoritaniFaction moritani)
            discordGame.queueMessage(infoChannelName, moritani.getTerrorTokenMessage(true));
        else if (faction instanceof RicheseFaction richese)
            writeRicheseCardCache(discordGame, infoChannelName, richese);
        else if (faction instanceof BTFaction)
            writeFaceDownLeaders(discordGame, game, infoChannelName);

        sendInfoButtons(game, discordGame, faction);
    }

    public static void sendInfoButtons(Game game, DiscordGame discordGame, Faction faction) throws ChannelNotFoundException, InvalidGameStateException, IOException {
        String infoChannelName = faction.getInfoChannelPrefix() + "-info";
        if (faction.isGraphicDisplay())
            discordGame.queueMessage(infoChannelName, new MessageCreateBuilder().addActionRow(Button.secondary("text", "Try Text mode. It's easier to read!")).build());
        else
            discordGame.queueMessage(infoChannelName, new MessageCreateBuilder().addActionRow(Button.secondary("graphic", "Try Graphic mode. It's cool and beautiful!")).build());

        sendCharityAction(discordGame, faction, false);
        if (faction instanceof RicheseFaction) {
            Territory territory = game.getTerritories().values().stream()
                    .filter(Territory::hasRicheseNoField)
                    .findFirst().orElse(null);
            if (territory != null && (game.getPhase() < 8 || game.allFactionsHaveMoved())) {
                int noField = territory.getRicheseNoField();
                Button button = Button.success("richese-reveal-no-field", "Reveal " + noField + " No-Field in " + territory.getTerritoryName());
                button = button.withEmoji(Emoji.fromFormatted(discordGame.tagEmojis(Emojis.NO_FIELD)));
                discordGame.queueMessage(infoChannelName, new MessageCreateBuilder().addActionRow(List.of(button)).build());
            }
        }

        StringSelectMenu.Builder playCardMenu = StringSelectMenu.create("play-card-menu-" + faction.getName()).setPlaceholder("Play a card.").setRequiredRange(1,1).setDefaultValues("0");
        if (faction.getTreacheryHand().stream().anyMatch(treacheryCard -> treacheryCard.name().equals("Truthtrance"))) {
            playCardMenu.addOption("Play Truthtrance to ask someone a yes/no question.", "Truthtrance");
        }

        if (faction.getTreacheryHand().stream().anyMatch(treacheryCard -> treacheryCard.name().equals("Tleilaxu Ghola"))) {
            playCardMenu.addOption("Play Tleilaxu Ghola to revive 5 forces for free", "Tleilaxu Ghola-forces");
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(
                    Objects.requireNonNull(Faction.class.getClassLoader().getResourceAsStream("Leaders.csv"))
            ));
            for (CSVRecord csvRecord : CSVParser.parse(bufferedReader, CSVFormat.EXCEL)) {
                if (csvRecord.get(0).equals(faction.getName()) && game.getLeaderTanks().stream().anyMatch(l -> l.getName().equals(csvRecord.get(1)))) {
                    playCardMenu.addOption("Play Tleilaxu Ghola to revive " + csvRecord.get(1) + " for free", "Tleilaxu Ghola-leader-" + csvRecord.get(1));
                }
            }
        }

//        if (faction.getTreacheryHand().stream().anyMatch(treacheryCard -> treacheryCard.name().equals("Amal"))) {
//            playCardMenu.addOption("Play Amal to halve all players' spice totals", "Amal");
//        }

        sendAllySpiceSupportButtons(discordGame, game, faction, false);
        sendAllianceActions(discordGame, game, faction, false);
        if (game.isInBiddingPhase()) {
            sendBiddingActions(discordGame, game, faction, false);
            Bidding bidding = game.getBidding();
            boolean isRicheseCard = bidding.isRicheseCacheCard() || bidding.isBlackMarketCard();
            if (faction.getTreacheryHand().stream().anyMatch(treacheryCard -> treacheryCard.name().equals("Karama")) && faction.getTreacheryHand().size() != faction.getHandLimit() && !isRicheseCard) {
                playCardMenu.addOption("Use Karama to buy this card for free.", "Karama-buy");
            }
        }
        sendCardInterruptActions(discordGame, faction, playCardMenu);
    }

    public static void sendCharityAction(DiscordGame discordGame, Faction faction, boolean isReply) throws ChannelNotFoundException {
        MessageCreateBuilder builder = new MessageCreateBuilder();
        EmbedBuilder embedBuilder = new EmbedBuilder().setColor(Color.DARK_GRAY);
        embedBuilder.setTitle("**Charity Option**");
        embedBuilder.appendDescription("You may decline CHOAM charity to hide the fact you are low on spice.");
        builder.addEmbeds(embedBuilder.build());
        String infoChannelName = faction.getInfoChannelPrefix() + "-info";
        if (faction.getSpice() < 2 && !(faction instanceof BGFaction) && !(faction instanceof ChoamFaction)) {
            if (faction.isDecliningCharity()) {
                builder = builder.addActionRow(Button.success("faction-charity-accept", "Accept CHOAM charity"));
                if (isReply)
                    discordGame.queueMessage(builder);
                else
                    discordGame.queueMessage(infoChannelName, builder.build());
            } else {
                builder = builder.addActionRow(Button.danger("faction-charity-decline", "Decline CHOAM charity"));
                if (isReply)
                    discordGame.queueMessage(builder);
                else
                    discordGame.queueMessage(infoChannelName, builder.build());
            }
        }
    }

    public static void sendAllySpiceSupportButtons(DiscordGame discordGame, Game game, Faction faction, boolean isReply) throws ChannelNotFoundException {
        if (faction.hasAlly()) {
            MessageCreateBuilder builder = new MessageCreateBuilder();
            EmbedBuilder embedBuilder = new EmbedBuilder().setColor(Color.DARK_GRAY);
            embedBuilder.setTitle(discordGame.tagEmojis("**Set Ally " + Emojis.SPICE + " Support**"));
            String infoChannelName = faction.getInfoChannelPrefix() + "-info";
            String allyEmoji = game.getFaction(faction.getAlly()).getEmoji();
            List<String> descriptionMessages = new ArrayList<>();
            if (faction.getSpice() > 0 && !faction.isAllySpiceFinishedForTurn()) {
                String message = "You are currently offering " + faction.getSpiceForAlly() + " " + Emojis.SPICE + " to " + allyEmoji + " for bidding and shipping.";
                List<Button> buttons = new ArrayList<>();
                buttons.add(Button.primary("ally-support-number", "Pick a number").withDisabled(faction.getSpice() == 0));
//                if (faction.getSpice() >= 4)
//                    buttons.add(Button.secondary("ally-support-" + Math.floorDiv(faction.getSpice(), 4) + "-quarter", "1/4 (" + Math.floorDiv(faction.getSpice(), 4) + ")").withDisabled(faction.getSpiceForAlly() == Math.floorDiv(faction.getSpice(), 4)));
                if (faction.getSpice() >= 2)
                    buttons.add(Button.secondary("ally-support-" + Math.floorDiv(faction.getSpice(), 2) + "-half", "Half (" + Math.floorDiv(faction.getSpice(), 2) + ")").withDisabled(faction.getSpiceForAlly() == Math.floorDiv(faction.getSpice(), 2)));
                buttons.add(Button.secondary("ally-support-" + faction.getSpice() + "-max", "Max (" + faction.getSpice() + ")").withDisabled(faction.getSpiceForAlly() == faction.getSpice()));
                buttons.add(Button.secondary("ally-support-reset", "Reset support (0)").withDisabled(faction.getSpiceForAlly() == 0));
                switch (faction) {
                    case GuildFaction guild -> {
                        if (guild.isAllySpiceForShipping()) {
                            message = "You are currently offering " + faction.getSpiceForAlly() + " " + Emojis.SPICE + " to " + allyEmoji + " for bidding AND shipping.";
                            if (faction.getSpiceForAlly() > 0)
                                message += "\nShipping " + Emojis.SPICE + " will go to the bank, not to you.";
                            buttons.add(Button.success("ally-support-noshipping", "Don't support shipping"));
                        } else {
                            message = "You are currently offering " + faction.getSpiceForAlly() + " " + Emojis.SPICE + " to " + allyEmoji + " for bidding ONLY.";
                            buttons.add(Button.danger("ally-support-shipping", "Support shipping"));
                        }
                    }
                    case ChoamFaction choam -> {
                        if (choam.isAllySpiceForBattle()) {
                            message = "You are currently offering " + faction.getSpiceForAlly() + " " + Emojis.SPICE + " to " + allyEmoji + " for bidding, shipping, AND battles.";
                            if (faction.getSpiceForAlly() > 0)
                                message += "\nCombat " + Emojis.SPICE + " will go to the bank, not to you.";
                            buttons.add(Button.success("ally-support-nobattles", "Don't support battles"));
                        } else {
                            message = "You are currently offering " + faction.getSpiceForAlly() + " " + Emojis.SPICE + " to " + allyEmoji + " for bidding and shipping ONLY";
                            buttons.add(Button.danger("ally-support-battles", "Support battles"));
                        }
                    }
                    case EmperorFaction ignored ->
                            message = "You are currently offering " + faction.getSpiceForAlly() + " " + Emojis.SPICE + " to " + allyEmoji + " for bidding, shipping, and battles";
                    default -> {
                    }
                }
                descriptionMessages.add(discordGame.tagEmojis(message));
                embedBuilder.appendDescription(String.join("\n", descriptionMessages));
                builder.addEmbeds(embedBuilder.build());
                builder.addActionRow(buttons);
                if (isReply)
                    discordGame.queueMessage(builder);
                else
                    discordGame.queueMessage(infoChannelName, builder.build());
            }
        }
    }

    public static void sendAllianceActions(DiscordGame discordGame, Game game, Faction faction, boolean isReply) throws ChannelNotFoundException {
        if (faction.hasAlly()) {
            MessageCreateBuilder builder = new MessageCreateBuilder();
            EmbedBuilder embedBuilder = new EmbedBuilder().setColor(Color.DARK_GRAY);
            embedBuilder.setTitle("**Alliance Actions**");
            String infoChannelName = faction.getInfoChannelPrefix() + "-info";
            String allyEmoji = game.getFaction(faction.getAlly()).getEmoji();
            List<String> descriptionMessages = new ArrayList<>();
            switch (faction) {
                // Also BT, Fremen, Guild, Richese
                // Future - Moritani, Ix, Ecaz
                case AtreidesFaction atreides -> {
                    List<Button> buttons = new ArrayList<>();
                    descriptionMessages.add("You are currently " + (atreides.isGrantingAllyTreacheryPrescience() ? "granting " : "denying ") + Emojis.TREACHERY + " Prescience to " + allyEmoji);
                    if (atreides.isGrantingAllyTreacheryPrescience())
                        buttons.add(Button.danger("atreides-ally-treachery-prescience-no", "Deny Bidding Prescience"));
                    else
                        buttons.add(Button.success("atreides-ally-treachery-prescience-yes", "Grant Bidding Prescience"));
                    descriptionMessages.add("You are currently " + (atreides.isDenyingAllyBattlePrescience() ? "denying " : "granting ") + "Battle Prescience to " + allyEmoji);
                    if (atreides.isDenyingAllyBattlePrescience())
                        buttons.add(Button.success("atreides-ally-battle-prescience-yes", "Grant Battle Prescience"));
                    else
                        buttons.add(Button.danger("atreides-ally-battle-prescience-no", "Deny Battle Prescience"));
                    builder.addActionRow(buttons);
                }
                case BGFaction bg -> {
                    descriptionMessages.add("You are currently " + (bg.isDenyingAllyVoice() ? "denying " : "granting ") + "The Voice to " + allyEmoji);
                    if (bg.isDenyingAllyVoice())
                        builder.addActionRow(List.of(Button.success("bg-ally-voice-yes", "Grant The Voice")));
                    else
                        builder.addActionRow(List.of(Button.danger("bg-ally-voice-no", "Deny The Voice")));
                }
                default -> {
                }
            }
            if (faction instanceof AtreidesFaction || faction instanceof BGFaction) {
                embedBuilder.appendDescription(discordGame.tagEmojis(String.join("\n", descriptionMessages)));
                builder.addEmbeds(embedBuilder.build());
                if (isReply)
                    discordGame.queueMessage(builder);
                else
                    discordGame.queueMessage(infoChannelName, builder.build());
            }
        }
    }

    public static void sendBiddingActions(DiscordGame discordGame, Game game, Faction faction, boolean isReply) throws ChannelNotFoundException, InvalidGameStateException {
        MessageCreateBuilder builder = new MessageCreateBuilder();
        EmbedBuilder embedBuilder = new EmbedBuilder().setColor(Color.DARK_GRAY);
        embedBuilder.setTitle("**Bidding Actions**");
        if (faction.getMaxBid() > 0)
            embedBuilder.appendDescription("Your current max bid is " + faction.getMaxBid() + (faction.isUseExactBid() ? ", Exact Bid" : ", Incremental"));
        else if (faction.getMaxBid() == -1)
            embedBuilder.appendDescription("You will pass.");
        else if (faction.isAutoBid() || faction.isAutoBidTurn())
            embedBuilder.appendDescription("You will auto-pass.");
        else
            embedBuilder.appendDescription("You have not set a bid or pass.");
        embedBuilder.appendDescription("\nYou will " + (faction.isOutbidAlly() ? "" : "not ") + "outbid your ally.");
        embedBuilder.appendDescription("\n\nTo bid, select a single number from the list.\nAlso select +1 for an Incremental bid.\nPass One Time logs a single pass for you.\nAuto-Pass will be reset with the next card.\nAuto-Pass (Whole Round) will be reset next turn.\nOutbid Ally policy will persist until you change it.\n\nYou don't have to wait to be tagged to bid or pass.\nPlease use Auto-Passing whenever possible.");
        builder.addEmbeds(embedBuilder.build());

        StringSelectMenu.Builder menu = StringSelectMenu.create("bidding-menu-" + faction.getName()).setPlaceholder("Select your max bid").setRequiredRange(1,2).setDefaultValues("0").addOption("Bid +1 up to your max bid instead of exactly the max", "auto-increment");
        int maxPossibleBid = faction.getSpice();
        if (faction.hasAlly())
            maxPossibleBid += game.getFaction(faction.getAlly()).getSpiceForAlly();
        for (int i=game.getBidding().getCurrentBid() + 1; i<=Math.min(maxPossibleBid, 24 + game.getBidding().getCurrentBid()); i++) {
            String optionString = String.valueOf(i);
            if (faction.hasAlly() && i == game.getFaction(faction.getAlly()).getSpiceForAlly()) {
                optionString += " (Ally support limit)";
            }
            menu.addOption(optionString, String.valueOf(i));
        }
        builder.addActionRow(menu.build());

        List<Button> buttons = new ArrayList<>();
        buttons.add(Button.primary("bidding-pass", "Pass One Time"));
        if (faction.isAutoBid())
            buttons.add(Button.secondary("bidding-auto-pass", "Disable Auto-Pass"));
        else
            buttons.add(Button.primary("bidding-auto-pass", "Enable Auto-Pass"));
        if (faction.isAutoBidTurn())
            buttons.add(Button.secondary("bidding-turn-pass", "Disable Auto-Pass (Whole Round)"));
        else
            buttons.add(Button.primary("bidding-turn-pass", "Enable Auto-Pass (Whole Round)"));
        if (faction.isOutbidAlly())
            buttons.add(Button.success("bidding-toggle-outbid-ally", "Don't Outbid Ally"));
        else
            buttons.add(Button.danger("bidding-toggle-outbid-ally", "Allow Outbidding Ally"));
        builder.addActionRow(buttons);

        String infoChannelName = faction.getInfoChannelPrefix() + "-info";
        if (isReply)
            discordGame.queueMessage(builder);
        else
            discordGame.queueMessage(infoChannelName, builder.build());
    }

    public static void updateBiddingActions(DiscordGame discordGame, Game game, Faction faction) throws InvalidGameStateException, ChannelNotFoundException {
        String infoChannelName = faction.getInfoChannelPrefix() + "-info";
        TextChannel channel = discordGame.getTextChannel(infoChannelName);
        List<Message> messages = channel.getHistoryAround(channel.getLatestMessageId(), 100).complete().getRetrievedHistory();
        List<Message> messagesToDelete = messages.stream().filter(message -> message.getEmbeds().stream().anyMatch(e -> e.getTitle() != null && e.getTitle().equals("**Bidding Actions**"))).toList();
        for (Message message : messagesToDelete) {
            try {
                message.delete().complete();
            } catch (Exception ignore) {}
        }
        sendBiddingActions(discordGame, game, faction, false);
    }

    public static void sendCardInterruptActions(DiscordGame discordGame, Faction faction, StringSelectMenu.Builder playCardMenu) throws ChannelNotFoundException {
        if (!playCardMenu.getOptions().isEmpty()) {
            MessageCreateBuilder builder = new MessageCreateBuilder();
            EmbedBuilder embedBuilder = new EmbedBuilder().setColor(Color.DARK_GRAY);
            embedBuilder.setTitle("**Treachery Card Actions**");
            embedBuilder.appendDescription("You may play a Treachery Card for a bot-supported action.");
            builder.addEmbeds(embedBuilder.build());
            String infoChannelName = faction.getInfoChannelPrefix() + "-info";
            builder.addActionRow(playCardMenu.build());
            discordGame.queueMessage(infoChannelName, builder.build());
        }
    }

    public static FileUpload drawGameBoard(Game game) throws IOException {
        BufferedImage board = getResourceImage("Board");

        //If Homeworlds are in play, concatenate homeworlds under the board.
        if (game.hasGameOption(GameOption.HOMEWORLDS)) {
            BufferedImage homeworlds = new BufferedImage(1, 1024, BufferedImage.TYPE_INT_ARGB);
            int sigilWidth = game.hasEmperorFaction() ? 350 : 300;
            for (Faction faction : game.getFactions()) {
                BufferedImage homeworld = getHomeworldImage(faction);
                int offset = 0;
                BufferedImage sigil = getSigilImage(faction);
                sigil = resize(sigil, sigilWidth, 250);
                Point sigilPlacement = new Point(500, 850);
                Point kaitainSigilPlacement = new Point(920, 950);
                homeworld = overlay(homeworld, sigil, faction instanceof EmperorFaction ? kaitainSigilPlacement : sigilPlacement, 1);
                for (Force force : game.getTerritory(faction.getHomeworld()).getForces()) {
                    BufferedImage forceImage = buildForceImage(game, force);
                    forceImage = resize(forceImage, 376, 232);
                    Point forcePlacement = new Point(500, 175 + offset);
                    if (faction instanceof EmperorFaction)
                        forcePlacement = new Point(920, 275 + offset);
                    homeworld = overlay(homeworld, forceImage, forcePlacement, 1);
                    offset += 240;
                }
                offset = 0;
                homeworlds = concatenateHorizontally(homeworlds, homeworld);
                if (faction instanceof EmperorFaction emperorFaction) {
                    BufferedImage salusa = getResourceImage("Salusa Secundus");
                    salusa = overlay(salusa, sigil, sigilPlacement, 1);
                    for (Force force : game.getTerritory(emperorFaction.getSecondHomeworld()).getForces()) {
                        BufferedImage forceImage = buildForceImage(game, force);
                        forceImage = resize(forceImage, 376, 232);
                        Point forcePlacement = new Point(500, 175 + offset);
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
        if (game.hasFremenFaction()) {
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
        int markerTurn = game.getTurn() == 0 ? 1 : (game.getTurn() % 10);
        float angle = markerTurn * 36 + 74f;
        turnMarker = rotateImageByDegrees(turnMarker, angle);
        Point coordinates = Initializers.getDrawCoordinates("turn " + markerTurn);
        board = overlay(board, turnMarker, coordinates, 1);
        BufferedImage phaseMarker = getResourceImage("Phase Marker");
        phaseMarker = resize(phaseMarker, 50, 50);
        coordinates = Initializers.getDrawCoordinates("phase " + (game.getPhaseForTracker()));
        board = overlay(board, phaseMarker, coordinates, 1);

        //Place pieces in territories
        for (Territory territory : game.getTerritories().values()) {
            if (territory.getForces().isEmpty() && territory.getSpice() == 0
                    && !territory.hasRicheseNoField() && territory.getEcazAmbassador() == null
                    && !territory.isAftermathToken() && !territory.hasTerrorToken()
                    && territory.getDiscoveryToken() == null) continue;
            if (territory.isDiscoveryToken() || territory instanceof HomeworldTerritory || territory.getTerritoryName().equals("Hidden Mobile Stronghold"))
                continue;
            int offset = 0;
            int i = 0;

            if (territory.isAftermathToken()) {
                BufferedImage aftermath = getResourceImage("Atomics");
                aftermath = resize(aftermath, 50, 50);
                Point placement = Initializers.getPoints(territory.getTerritoryName()).getFirst();
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
                    if (territory.getTerritoryName().equals("Jacurutu Sietch")) continue;
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
                        Point spicePlacement = Initializers.getPoints(territory.getTerritoryName()).getFirst();
                        Point spicePlacementOffset = new Point(spicePlacement.x + offset, spicePlacement.y - offset);
                        board = overlay(board, spiceImage, spicePlacementOffset, 1);
                        spice -= 10;
                    } else if (spice >= 8) {
                        BufferedImage spiceImage = getResourceImage("8 Spice");
                        spiceImage = resize(spiceImage, 25, 25);
                        Point spicePlacement = Initializers.getPoints(territory.getTerritoryName()).getFirst();
                        Point spicePlacementOffset = new Point(spicePlacement.x + offset, spicePlacement.y - offset);
                        board = overlay(board, spiceImage, spicePlacementOffset, 1);
                        spice -= 8;
                    } else if (spice >= 6) {
                        BufferedImage spiceImage = getResourceImage("6 Spice");
                        spiceImage = resize(spiceImage, 25, 25);
                        Point spicePlacement = Initializers.getPoints(territory.getTerritoryName()).getFirst();
                        Point spicePlacementOffset = new Point(spicePlacement.x + offset, spicePlacement.y - offset);
                        board = overlay(board, spiceImage, spicePlacementOffset, 1);
                        spice -= 6;
                    } else if (spice == 5) {
                        BufferedImage spiceImage = getResourceImage("5 Spice");
                        spiceImage = resize(spiceImage, 25, 25);
                        Point spicePlacement = Initializers.getPoints(territory.getTerritoryName()).getFirst();
                        Point spicePlacementOffset = new Point(spicePlacement.x + offset, spicePlacement.y - offset);
                        board = overlay(board, spiceImage, spicePlacementOffset, 1);
                        spice -= 5;
                    } else if (spice >= 2) {
                        BufferedImage spiceImage = getResourceImage("2 Spice");
                        spiceImage = resize(spiceImage, 25, 25);
                        Point spicePlacement = Initializers.getPoints(territory.getTerritoryName()).getFirst();
                        Point spicePlacementOffset = new Point(spicePlacement.x + offset, spicePlacement.y - offset);
                        board = overlay(board, spiceImage, spicePlacementOffset, 1);
                        spice -= 2;
                    } else {
                        BufferedImage spiceImage = getResourceImage("1 Spice");
                        spiceImage = resize(spiceImage, 25, 25);
                        Point spicePlacement = Initializers.getPoints(territory.getTerritoryName()).getFirst();
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
                    for (String ignored : game.getTerritory(territory.getDiscoveryToken()).getTerrorTokens()) {
                        BufferedImage terrorToken = getResourceImage("Terror Token");
                        terrorToken = resize(terrorToken, 250, 250);
                        Point tokenPlacement = new Point(250, 150);
                        Point tokenPlacementOffset = new Point(tokenPlacement.x, tokenPlacement.y + offset);
                        discoveryToken = overlay(discoveryToken, terrorToken, tokenPlacementOffset, 1);
                        offset += 100;
                    }

                    for (Force force : game.getTerritory(territory.getDiscoveryToken()).getForces()) {
                        BufferedImage forceImage = buildForceImage(game, force);
                        forceImage = resize(forceImage, 376, 232);
                        Point forcePlacement = new Point(250, 150);
                        Point forcePlacementOffset = new Point(forcePlacement.x, forcePlacement.y + offset);
                        discoveryToken = overlay(discoveryToken, forceImage, forcePlacementOffset, 1);
                        offset += 100;
                    }

                    if (game.getTerritory(territory.getDiscoveryToken()).hasRicheseNoField()) {
                        BufferedImage noFieldImage = resize(getResourceImage("No-Field Hidden"), 240, 240);
                        Point forcePlacement = new Point(250, 150);
                        Point forcePlacementOffset = new Point(forcePlacement.x, forcePlacement.y + offset);
                        discoveryToken = overlay(discoveryToken, noFieldImage, forcePlacementOffset, 1);
//                        offset += 100;
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
            offset = 0;
            for (Force force : territory.getForces()) {
                if (territory.getTerritoryName().equals("Jacurutu Sietch")) continue;
                if (force.getName().equals("Hidden Mobile Stronghold")) {
                    BufferedImage hms = getResourceImage("Hidden Mobile Stronghold");
                    hms = resize(hms, 150, 100);
                    List<Force> hmsForces = game.getTerritories().get("Hidden Mobile Stronghold").getForces();
                    int forceOffset = 0;
                    for (Force f : hmsForces) {
                        BufferedImage forceImage = buildForceImage(game, f);
                        hms = overlay(hms, forceImage, new Point(40, 20 + forceOffset), 1);
                        forceOffset += 30;
                    }
                    Point forcePlacement = Initializers.getPoints(territory.getTerritoryName()).get(i);
                    int hmsRotation = game.getHmsRotation();
                    int xOffset = -55;
                    int yOffset = offset + 5;
                    if (hmsRotation == 180) xOffset = 55;
                    else if (hmsRotation == 90) {
                        xOffset = 0;
                        yOffset = offset - 55;
                    } else if (hmsRotation == 270) {
                        xOffset = 0;
                        yOffset = offset + 55;
                    }
                    Point forcePlacementOffset = new Point(forcePlacement.x +xOffset, forcePlacement.y + yOffset);
                    hms = rotateImageByDegrees(hms, hmsRotation);
                    board = overlay(board, hms, forcePlacementOffset, 1);
                    continue;
                }
                BufferedImage forceImage = buildForceImage(game, force);
                Point forcePlacement = Initializers.getPoints(territory.getTerritoryName()).get(i);
                Point forcePlacementOffset = new Point(forcePlacement.x, forcePlacement.y + offset);
                board = overlay(board, forceImage, forcePlacementOffset, 1);
                i++;
                if (i == Initializers.getPoints(territory.getTerritoryName()).size()) {
                    offset += 20;
                    i = 0;
                }
            }

            if (territory.hasRicheseNoField() && !territory.isDiscoveryToken()) {
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
                BufferedImage sigil;
                sigil = getSigilImage(faction);
                coordinates = Initializers.getDrawCoordinates("sigil " + k);
                sigil = resize(sigil, 50, 50);
                board = overlay(board, sigil, coordinates, 1);

                // Check for alliances
                if (faction.hasAlly()) {
                    BufferedImage allySigil = getSigilImage(game.getFaction(faction.getAlly()));
                    coordinates = Initializers.getDrawCoordinates("ally " + k);
                    allySigil = resize(allySigil, 40, 40);
                    board = overlay(board, allySigil, coordinates, 1);
                }
            }
        }

        //Place tanks forces
        int i = 0;
        int offset = 0;
        for (Force force : game.getTleilaxuTanks().getForces()) {
            if (force.getStrength() == 0) continue;
            BufferedImage forceImage = buildForceImage(game, force);

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
            Faction leaderFaction = game.getFaction(leader.getOriginalFactionName());
            BufferedImage leaderImage;
            if (leader.isFaceDown() || leaderFaction instanceof HomebrewFaction) {
                leaderImage = getSigilImage(leaderFaction);
            } else {
                leaderImage = getResourceImage(leader.getName());
            }
            if (!leader.getName().equals("Kwisatz Haderach")) leaderImage = resize(leaderImage, 70, 70);
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

        //Place Tech Tokens
        for (int x = 0; x < game.getFactions().size(); x++) {
            Faction faction = game.getFactions().get(x);
            if (faction.getTechTokens().isEmpty()) continue;
            int ttoffset = 0;
            for (TechToken token : faction.getTechTokens()) {
                BufferedImage tokenImage = getResourceImage(token.getName());
                tokenImage = resize(tokenImage, 50, 50);
                coordinates = Initializers.getDrawCoordinates("tech token " + x);
                Point coordinatesOffset = new Point(coordinates.x + ttoffset, coordinates.y);
                board = overlay(board, tokenImage, coordinatesOffset, 1);
                ttoffset += 50;
            }
        }

        if (game.hasEcazFaction() || game.hasMoritaniFaction()) {
            Leader dukeVidal = game.getDukeVidal();
            boolean someoneHasVidal = game.getFactions().stream().anyMatch(f -> f.getLeaders().stream().anyMatch(l -> l.getName().equals("Duke Vidal")));
            boolean vidalIsInTheTanks = game.getLeaderTanks().stream().anyMatch(l -> l.getName().equals("Duke Vidal"));
            if (!someoneHasVidal && !vidalIsInTheTanks) {
                BufferedImage leaderImage = getResourceImage(dukeVidal.getName());
                leaderImage = resize(leaderImage, 70, 70);
                board = overlay(board, leaderImage, new Point(904, 46), 1);
            }
        }

        ByteArrayOutputStream boardOutputStream = new ByteArrayOutputStream();
        ImageIO.write(board, "png", boardOutputStream);

        return FileUpload.fromData(boardOutputStream.toByteArray(), "board.png");
    }

    private static BufferedImage buildForceImage(Game game, Force force) throws IOException {
        String forceName = force.getName();
        int strength = force.getStrength();
        Faction faction = game.getFaction(force.getFactionName());
        if (faction instanceof HomebrewFaction hbFaction)
            forceName = hbFaction.getFactionProxy();
        BufferedImage forceImage = !forceName.equals("Advisor") ? getResourceImage(forceName.replace("*", "") + " Troop") : getResourceImage("BG Advisor");
        forceImage = resize(forceImage, 47, 29);
        if (forceName.contains("*")) {
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

    public static void writeFactionInfo(DiscordGame discordGame, String factionName) throws ChannelNotFoundException, IOException, InvalidGameStateException {
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
    public static void writeFactionInfo(DiscordGame discordGame, Faction faction) throws ChannelNotFoundException, IOException, InvalidGameStateException {
        MessageChannel infoChannel = discordGame.getTextChannel(faction.getInfoChannelPrefix() + "-info");
        MessageHistory messageHistory = MessageHistory.getHistoryFromBeginning(infoChannel).complete();

        List<Message> messages = messageHistory.getRetrievedHistory();

        messages.forEach(discordGame::queueDeleteMessage);

        String emoji = faction.getEmoji();
        List<TraitorCard> traitors = faction.getTraitorHand();
        String infoChannelName = faction.getInfoChannelPrefix() + "-info";
        StringBuilder factionSpecificString = new StringBuilder();
        String nexusCard = faction.getNexusCard() == null ? "" : "\n__Nexus Card:__\n" + Emojis.NEXUS + faction.getNexusCard().name();

        switch (faction) {
            case BGFaction bg -> factionSpecificString.append("\n__Prediction:__ ")
                    .append(bg.getPredictionFactionName())
                    .append(" Turn ")
                    .append(bg.getPredictionRound());
            case EcazFaction ecaz -> factionSpecificString.append(ecaz.getAmbassadorSupplyInfoMessage());
            case MoritaniFaction moritani -> factionSpecificString.append(moritani.getTerrorTokenMessage(false));
            case AtreidesFaction atreides -> factionSpecificString.append("\n__KH Counter:__ ").append(Math.min(7, atreides.getForcesLost()));
            default -> {}
        }
        String traitorString = ((faction instanceof BTFaction) ? "\n__Face Dancers:__\n" : "\n__Traitors:__\n") +
                String.join("\n", traitors.stream().map(TraitorCard::getEmojiNameAndStrengthString).toList()) + "\n";
        String reservesString = "\n__Reserves:__ " + FactionView.getTaggedReservesString(discordGame, faction);

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
        StringBuilder leadersInTerritories = new StringBuilder();
        for (Leader leader : faction.getLeaders()) {
            if (!(faction instanceof HomebrewFaction))
                builder = builder.addFiles(getResourceFile(leader.getName()));
            if (leader.getBattleTerritoryName() != null)
                leadersInTerritories.append(leader.getName()).append(" is in ").append(leader.getBattleTerritoryName()).append("\n");
        }
        discordGame.queueMessage(infoChannelName, builder.build());
        if (faction instanceof HomebrewFaction && !faction.getLeaders().isEmpty())
            discordGame.queueMessage(infoChannelName, "__Leaders:__\n" + String.join("\n", faction.getLeaders().stream().map(Leader::getEmoiNameAndValueString).toList()));

        if (!leadersInTerritories.isEmpty())
            discordGame.queueMessage(infoChannelName, leadersInTerritories.toString());

        if (faction.getGame().hasGameOption(GameOption.HOMEWORLDS)) {
            MessageCreateBuilder homeworldMessageBuilder = new MessageCreateBuilder();
            String lowHigh = faction.isHighThreshold() ? "High" : "Low";
            String homeworldName = faction.getHomeworld();
            Optional<FileUpload> cardImage = CardImages.getHomeworldImage(discordGame.getEvent().getGuild(), homeworldName + " " + lowHigh);
            if (cardImage.isPresent()) homeworldMessageBuilder.addContent(faction.getHomeworld()).addFiles(cardImage.get());
            else homeworldMessageBuilder.addContent(faction.getHomeworld() + " " + lowHigh);
            if (faction instanceof EmperorFaction emperorFaction) {
                lowHigh = emperorFaction.isSecundusHighThreshold() ? "High" : "Low";
                cardImage = CardImages.getHomeworldImage(discordGame.getEvent().getGuild(), "Salusa Secundus " + lowHigh);
                if (cardImage.isPresent()) homeworldMessageBuilder.addFiles(cardImage.get());
                else homeworldMessageBuilder.addContent("Salusa Secundus " + lowHigh);
            }
            discordGame.queueMessage(infoChannelName, homeworldMessageBuilder);
        }

        List<TreacheryCard> treacheryCards = faction.getTreacheryHand();
        if (!treacheryCards.isEmpty()) {
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

            if (faction instanceof RicheseFaction richese)
                writeRicheseCardCache(discordGame, infoChannelName, richese);
            else if (faction instanceof BTFaction)
                writeFaceDownLeaders(discordGame, faction.getGame(), infoChannelName);
        }
        sendInfoButtons(discordGame.getGame(), discordGame, faction);
    }

    private static void writeRicheseCardCache(DiscordGame discordGame, String infoChannelName, RicheseFaction richese) throws ChannelNotFoundException {
        List<TreacheryCard> treacheryCards = richese.getTreacheryCardCache();
        if (!treacheryCards.isEmpty()) {
            MessageCreateBuilder treacheryCardMessageBuilder = new MessageCreateBuilder();
            StringBuilder treacheryString = new StringBuilder();
            treacheryString.append("\n__" + Emojis.RICHESE + " Card Cache:__\n");
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
        }
    }

    private static void writeFaceDownLeaders(DiscordGame discordGame, Game game, String infoChannelName) throws ChannelNotFoundException {
        List<Leader> faceDownLeaders = game.getLeaderTanks().stream().filter(Leader::isFaceDown).toList();
        if (!faceDownLeaders.isEmpty()) {
            List<String> leadersNamesAndEmojis = faceDownLeaders.stream().map(Leader::getEmoiNameAndValueString).toList();
            String leadersString = "\n__Face Down Leaders:__\n" + String.join("\n", leadersNamesAndEmojis);

            MessageCreateBuilder faceDownLeadersBuilder = new MessageCreateBuilder();
            faceDownLeadersBuilder.addContent(leadersString);
            discordGame.queueMessage(infoChannelName, faceDownLeadersBuilder.build());
        }
    }

    public static void refreshFrontOfShieldInfo(DiscordGame discordGame, Game game) throws ChannelNotFoundException, IOException {
        MessageChannel frontOfShieldChannel = discordGame.getTextChannel("front-of-shield");
        MessageHistory messageHistory = MessageHistory.getHistoryFromBeginning(frontOfShieldChannel).complete();

        List<Message> messages = messageHistory.getRetrievedHistory();

        messages.forEach(discordGame::queueDeleteMessage);

        FileUpload newMap = drawGameBoard(game).setName("game-map.png");
        MessageCreateBuilder builder = new MessageCreateBuilder();
        builder.addFiles(newMap);

        EmbedBuilder embedBuilder = new EmbedBuilder().setColor(Color.BLACK);
        String biddingMarketSize = "";
        try {
            Bidding bidding = game.getBidding();
            biddingMarketSize = ":convenience_store: " + (bidding.getMarket().size() + (bidding.getBidCard() != null ? 1 : 0)) + " ";
        } catch (InvalidGameStateException ignored) {}
        String deckSizes = discordGame.tagEmojis(biddingMarketSize + Emojis.TREACHERY + " " + game.getTreacheryDeck().size()
                        + " :wastebasket: " + game.getTreacheryDiscard().size()
                        + " " + Emojis.SPICE + " " + game.getSpiceDeck().size());
        embedBuilder.addField("Deck Sizes", deckSizes, false);

        List<String> discoveryTokensLocations = game.getTerritories().values().stream().filter(t -> t.getDiscoveryToken() != null && t.isDiscovered()).map(t -> t.getDiscoveryToken() + " is in " + t.getTerritoryName()).toList();
        List<String> undiscoveredTokensLocations = game.getTerritories().values().stream().filter(t -> t.getDiscoveryToken() != null && !t.isDiscovered()).map(t -> "A " + (t.isRock() ? "Smuggler" : "Hiereg") + " token is in " + t.getTerritoryName()).collect(Collectors.toList());
        String discoveryString = String.join("\n", discoveryTokensLocations);
        String undiscoveredString = String.join("\n", undiscoveredTokensLocations);
        if (!discoveryTokensLocations.isEmpty() || !undiscoveredTokensLocations.isEmpty())
            embedBuilder.addField("Discovery Token Locations", String.join("\n", List.of(discoveryString, undiscoveredString)), true);

        List<String> ambassadorLocations = game.getTerritories().values().stream().filter(Territory::hasEcazAmbassador).map(t -> Emojis.getFactionEmoji(t.getEcazAmbassador()) + " is in " + t.getTerritoryName()).toList();
        if (!ambassadorLocations.isEmpty())
            embedBuilder.addField(discordGame.tagEmojis(Emojis.ECAZ + " Ambassador Locations"), discordGame.tagEmojis(String.join("\n", ambassadorLocations)), false);

        builder.addEmbeds(embedBuilder.build());
        discordGame.queueMessage("front-of-shield", builder);

        for (Faction faction : game.getFactions()) {
            FactionView factionView = FactionView.factory(discordGame, faction);
            builder = factionView.getPublicMessage();
            discordGame.queueMessage("front-of-shield", builder);
        }
    }

    public static void refreshChangedInfo(DiscordGame discordGame) throws ChannelNotFoundException, IOException, InvalidGameStateException {
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

            if (updateTypes.contains(UpdateType.TREACHERY_CARDS)) {
                frontOfShieldModified = true;
            }

            if (updateTypes.contains(UpdateType.MAP)) {
                frontOfShieldModified = true;
            }
        }
        if (game.getUpdateTypes().contains(UpdateType.MAP)) {
            frontOfShieldModified = true;
        }

        if (game.getUpdateTypes().contains(UpdateType.MAP_ALSO_IN_TURN_SUMMARY)) {
            frontOfShieldModified = true;
            discordGame.getTurnSummary().queueMessage(drawGameBoard(game));
        }

        if (frontOfShieldModified) {
            refreshFrontOfShieldInfo(discordGame, game);
        }
    }
}
