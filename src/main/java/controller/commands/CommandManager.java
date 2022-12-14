package controller.commands;

import controller.Initializers;
import exceptions.ChannelNotFoundException;
import exceptions.InvalidGameStateException;
import io.github.cdimascio.dotenv.Dotenv;
import model.*;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.channel.concrete.Category;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.guild.GuildReadyEvent;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.utils.FileUpload;
import org.jetbrains.annotations.NotNull;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.*;
import java.util.List;

public class CommandManager extends ListenerAdapter {

    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        List<Role> roles = event.getMember().getRoles();
        boolean isGameMaster = false;
        for (Role role : roles) {
            if (role.getName().equals("Game Master") || role.getName().equals("Dungeon Master")) {
                isGameMaster = true;
                break;
            }
        }
        if (!isGameMaster) {
            event.reply("You are not a Game Master!").setEphemeral(true).queue();
            return;
        }

        String name = event.getName();
        event.deferReply(true).queue();

        try {
            if (name.equals("newgame")) newGame(event);
            else if (name.equals("clean")) clean(event);
            else {
                DiscordGame discordGame = new DiscordGame(event);
                Game gameState = discordGame.getGameState();

                switch (name) {
                    case "addfaction" -> addFaction(event, discordGame, gameState);
                    case "newfactionresource" -> newFactionResource(event, discordGame, gameState);
                    case "resourceaddorsubtract" -> resourceAddOrSubtract(event, discordGame, gameState);
                    case "removeresource" -> removeResource(event, discordGame, gameState);
                    case "draw" -> drawCard(event, discordGame, gameState);
                    case "discard" -> discard(event, discordGame, gameState);
                    case "transfercard" -> transferCard(event, discordGame, gameState);
                    case "putback" -> putBack(event, discordGame, gameState);
                    case "ixhandselection" -> ixHandSelection(event, discordGame, gameState);
                    case "selecttraitor" -> selectTraitor(event, discordGame, gameState);
                    case "placeforces" -> placeForces(event, discordGame, gameState);
                    case "removeforces" -> removeForces(event, discordGame, gameState);
                    case "display" -> displayGameState(event, discordGame, gameState);
                    case "reviveforces" -> revival(event, discordGame, gameState);
                    case "awardbid" -> awardBid(event, discordGame, gameState);
                    case "killleader" -> killLeader(event, discordGame, gameState);
                    case "reviveleader" -> reviveLeader(event, discordGame, gameState);
                    case "setstorm" -> setStorm(event, discordGame, gameState);
                    case "bgflip" -> bgFlip(event, discordGame, gameState);
                    case "bribe" -> bribe(event, discordGame, gameState);
                    case "mute" -> mute(discordGame, gameState);
                    case "advancegame" -> advanceGame(event, discordGame, gameState);
                }
            }
            event.getHook().editOriginal("Command Done").queue();
        } catch (ChannelNotFoundException e) {
            e.printStackTrace();
            event.getHook().editOriginal("Channel not found!").queue();
        } catch (Exception e) {
            event.getHook().editOriginal("An error occurred!").queue();
            e.printStackTrace();
        }
    }

    @Override
    public void onCommandAutoCompleteInteraction(@NotNull CommandAutoCompleteInteractionEvent event) {
        String optionName = event.getFocusedOption().getName();
        String searchValue = event.getFocusedOption().getValue();
        DiscordGame discordGame = new DiscordGame(event);

        try {
            Game gameState = discordGame.getGameState();
            switch (optionName) {
                case "factionname" -> event.replyChoices(CommandOptions.factions(gameState, searchValue)).queue();
            }
        } catch (ChannelNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void onGuildReady(@NotNull GuildReadyEvent event) {

        OptionData gameName = new OptionData(OptionType.STRING, "name", "e.g. 'Dune Discord #5: The Tortoise and the Hajr'", true);
        OptionData gameRole = new OptionData(OptionType.ROLE, "gamerole", "The role you created for the players of this game", true);
        OptionData modRole = new OptionData(OptionType.ROLE, "modrole", "The role you created for the mod(s) of this game", true);
        OptionData user = new OptionData(OptionType.USER, "player", "The player for the faction", true);
        OptionData allFactions = new OptionData(OptionType.STRING, "factionname", "The faction", true)
                .addChoice("Atreides", "Atreides")
                .addChoice("Harkonnen", "Harkonnen")
                .addChoice("Emperor", "Emperor")
                .addChoice("Fremen", "Fremen")
                .addChoice("Spacing Guild", "Guild")
                .addChoice("Bene Gesserit", "BG")
                .addChoice("Ixian", "Ix")
                .addChoice("Tleilaxu", "BT")
                .addChoice("CHOAM", "CHOAM")
                .addChoice("Richese", "Rich");
        OptionData faction = new OptionData(OptionType.STRING, "factionname", "The faction", true)
                .setAutoComplete(true);
        OptionData resourceName = new OptionData(OptionType.STRING, "resource", "The name of the resource", true);
        OptionData value = new OptionData(OptionType.STRING, "value", "Set the initial value", true);
        OptionData amount = new OptionData(OptionType.INTEGER, "amount", "Amount to be added or subtracted (e.g. -3, 4)", true);
        OptionData message = new OptionData(OptionType.STRING, "message", "Message for spice transactions", false);
        OptionData password = new OptionData(OptionType.STRING, "password", "You really aren't allowed to run this command unless Voiceofonecrying lets you", true);
        OptionData deck = new OptionData(OptionType.STRING, "deck", "The deck", true)
                .addChoice("Treachery Deck", "treachery_deck")
                .addChoice("Traitor Deck", "traitor_deck");
        OptionData card = new OptionData(OptionType.STRING, "card", "The card.", true);
        OptionData sender = new OptionData(OptionType.STRING, "sender", "The one giving the card", true)
                .addChoice("Atreides", "Atreides")
                .addChoice("Harkonnen", "Harkonnen")
                .addChoice("Emperor", "Emperor")
                .addChoice("Fremen", "Fremen")
                .addChoice("Spacing Guild", "Guild")
                .addChoice("Bene Gesserit", "BG")
                .addChoice("Ixian", "Ix")
                .addChoice("Tleilaxu", "BT")
                .addChoice("CHOAM", "CHOAM")
                .addChoice("Richese", "Rich");
        OptionData recipient = new OptionData(OptionType.STRING, "recipient", "The recipient", true)
                .addChoice("Atreides", "Atreides")
                .addChoice("Harkonnen", "Harkonnen")
                .addChoice("Emperor", "Emperor")
                .addChoice("Fremen", "Fremen")
                .addChoice("Spacing Guild", "Guild")
                .addChoice("Bene Gesserit", "BG")
                .addChoice("Ixian", "Ix")
                .addChoice("Tleilaxu", "BT")
                .addChoice("CHOAM", "CHOAM")
                .addChoice("Richese", "Rich");
        OptionData bottom = new OptionData(OptionType.BOOLEAN, "bottom", "Place on bottom?", true);
        OptionData traitor = new OptionData(OptionType.STRING, "traitor", "The name of the traitor", true);
        OptionData territory = new OptionData(OptionType.STRING, "mostlikelyterritories", "The name of the territory (more 'important' territories).")
                .addChoice("Cielago North", "Cielago North")
                .addChoice("Cielago South", "Cielago South")
                .addChoice("False Wall South", "False Wall South")
                .addChoice("South Mesa", "South Mesa")
                .addChoice("False Wall East", "False Wall East")
                .addChoice("The Minor Erg", "The Minor Erg")
                .addChoice("Tuek's Sietch", "Tuek's Sietch")
                .addChoice("Red Chasm", "Red Chasm")
                .addChoice("Imperial Basin", "Imperial Basin")
                .addChoice("Old Gap", "Old Gap")
                .addChoice("Sihaya Ridge", "Sihaya Ridge")
                .addChoice("Arrakeen", "Arrakeen")
                .addChoice("Broken Land", "Broken Land")
                .addChoice("Carthag", "Carthag")
                .addChoice("Hagga Basin", "Hagga Basin")
                .addChoice("Rock Outcroppings", "Rock Outcroppings")
                .addChoice("Sietch Tabr", "Sietch Tabr")
                .addChoice("Funeral Plain", "Funeral Plain")
                .addChoice("The Great Flat", "The Great Flat")
                .addChoice("False Wall West", "False Wall West")
                .addChoice("Habbanya Erg", "Habbanya Erg")
                .addChoice("Habbanya Ridge Flat", "Habbanya Ridge Flat")
                .addChoice("Habbanya Sietch", "Habbanya Sietch")
                .addChoice("Wind Pass North", "Wind Pass North")
                .addChoice("Polar Sink", "Polar Sink");
        OptionData otherTerritory = new OptionData(OptionType.STRING, "otherterritories", "Added for completeness, less likely to use.")
                .addChoice("Cielago Depression","Cielago Depression")
                .addChoice("Meridian", "Meridian")
                .addChoice("Cielago East", "Cielago East")
                .addChoice("Harg Pass", "Harg Pass")
                .addChoice("Pasty Mesa", "Pasty Mesa")
                .addChoice("Gara Kulon", "Gara Kulon")
                .addChoice("Basin", "Basin")
                .addChoice("Hole in the Rock", "Hole in the Rock")
                .addChoice("Rim Wall West", "Rim Wall West")
                .addChoice("Arsunt", "Arsunt")
                .addChoice("Tsimpo", "Tsimpo")
                .addChoice("Plastic Basin", "Plastic Basin")
                .addChoice("Bight of the Cliff", "Bight of the Cliff")
                .addChoice("Wind Pass", "Wind Pass")
                .addChoice("The Greater Flat", "The Greater Flat")
                .addChoice("Cielago West", "Cielago West")
                .addChoice("Shield Wall", "Shield Wall");
        OptionData sector = new OptionData(OptionType.INTEGER, "sector", "The storm sector");
        OptionData starred = new OptionData(OptionType.BOOLEAN, "starred", "Are they starred forces?", true);
        OptionData spent = new OptionData(OptionType.INTEGER, "spent", "How much was spent on the card.", true);
        OptionData revived = new OptionData(OptionType.INTEGER, "revived", "How many are being revived.", true);
        OptionData data = new OptionData(OptionType.STRING, "data", "What data to display", true)
                .addChoice("Territories", "territories")
                .addChoice("Decks and Discards", "dnd")
                .addChoice("Phase, Turn, and everything else", "etc")
                .addChoice("Faction Info", "factions");
        OptionData isShipment = new OptionData(OptionType.BOOLEAN, "isshipment", "Is this placement a shipment?", true);
        OptionData toTanks = new OptionData(OptionType.BOOLEAN, "totanks", "Remove these forces to the tanks (true) or to reserves (false)?", true);
        OptionData leader = new OptionData(OptionType.STRING, "leader", "The leader.", true);

        //add new slash command definitions to commandData list
        List<CommandData> commandData = new ArrayList<>();
        commandData.add(Commands.slash("clean", "FOR TEST ONLY: DO NOT RUN").addOptions(password));
        commandData.add(Commands.slash("newgame", "Creates a new Dune game instance.").addOptions(gameName, gameRole, modRole));
        commandData.add(Commands.slash("addfaction", "Register a user to a faction in a game").addOptions(allFactions, user));
        commandData.add(Commands.slash("newfactionresource", "Initialize a new resource for a faction").addOptions(faction, resourceName, value));
        commandData.add(Commands.slash("resourceaddorsubtract", "Performs basic addition and subtraction of numerical resources for factions").addOptions(faction, resourceName, amount, message));
        commandData.add(Commands.slash("removeresource", "Removes a resource category entirely (Like if you want to remove a Tech Token from a player)").addOptions(faction, resourceName));
        commandData.add(Commands.slash("draw", "Draw a card from the top of a deck.").addOptions(deck, faction));
        commandData.add(Commands.slash("discard", "Move a card from a faction's hand to the discard pile").addOptions(faction, card));
        commandData.add(Commands.slash("transfercard", "Move a card from one faction's hand to another").addOptions(sender, card, recipient));
        commandData.add(Commands.slash("putback", "Used for the Ixian ability to put a treachery card on the top or bottom of the deck.").addOptions(card, bottom));
        commandData.add(Commands.slash("advancegame", "Send the game to the next phase, turn, or card (in bidding round"));
        commandData.add(Commands.slash("ixhandselection", "Only use this command to select the Ix starting treachery card").addOptions(card));
        commandData.add(Commands.slash("selecttraitor", "Select a starting traitor from hand.").addOptions(faction, traitor));
        commandData.add(Commands.slash("placeforces", "Place forces from reserves onto the surface").addOptions(faction, amount, isShipment, starred, territory, otherTerritory, sector));
        commandData.add(Commands.slash("removeforces", "Remove forces from the board.").addOptions(faction, amount, toTanks, starred, territory, otherTerritory, sector));
        commandData.add(Commands.slash("awardbid", "Designate that a card has been won by a faction during bidding phase.").addOptions(faction, spent));
        commandData.add(Commands.slash("reviveforces", "Revive forces for a faction.").addOptions(faction, revived, starred));
        commandData.add(Commands.slash("display", "Displays some element of the game to the mod.").addOptions(data));
        commandData.add(Commands.slash("setstorm", "Sets the storm to an initial sector.").addOptions(sector));
        commandData.add(Commands.slash("killleader", "Send a leader to the tanks.").addOptions(faction, leader));
        commandData.add(Commands.slash("reviveleader", "Revive a leader from the tanks.").addOptions(faction, leader));
        commandData.add(Commands.slash("bgflip", "Flip BG forces to advisor or fighter.").addOptions(territory, otherTerritory, sector));
        commandData.add(Commands.slash("mute", "Toggle mute for all bot messages."));
        commandData.add(Commands.slash("bribe", "Record a bribe transaction").addOptions(faction, recipient, amount));

        event.getGuild().updateCommands().addCommands(commandData).queue();
    }

    public void newGame(SlashCommandInteractionEvent event) throws ChannelNotFoundException {
        Role gameRole = event.getOption("gamerole").getAsRole();
        Role modRole = event.getOption("modrole").getAsRole();
        String name = event.getOption("name").getAsString();
        event.getGuild()
                .createCategory(name)
                .addPermissionOverride(event.getGuild().getPublicRole(), null, EnumSet.of(Permission.VIEW_CHANNEL))
                .addPermissionOverride(event.getMember(), EnumSet.of(Permission.VIEW_CHANNEL), null)
                .addPermissionOverride(modRole, EnumSet.of(Permission.VIEW_CHANNEL), null)
                .addPermissionOverride(gameRole, EnumSet.of(Permission.VIEW_CHANNEL), null)
                .complete();

        Category category = event.getGuild().getCategoriesByName(name, true).get(0);

        category.createTextChannel("bot-data")
                .addPermissionOverride(gameRole, null, EnumSet.of(Permission.VIEW_CHANNEL)).complete();
        category.createTextChannel("chat")
                        .addPermissionOverride(event.getGuild().getRolesByName("Observer", true).get(0), EnumSet.of(Permission.VIEW_CHANNEL), null).complete();
        category.createTextChannel("turn-summary")
                .addPermissionOverride(gameRole, EnumSet.of(Permission.VIEW_CHANNEL), EnumSet.of(Permission.MESSAGE_SEND))
                .addPermissionOverride(event.getGuild().getRolesByName("Observer", true).get(0), EnumSet.of(Permission.VIEW_CHANNEL), EnumSet.of(Permission.MESSAGE_SEND)).complete();
        category.createTextChannel("game-actions")
                .addPermissionOverride(event.getGuild().getRolesByName("Observer", true).get(0), EnumSet.of(Permission.VIEW_CHANNEL), EnumSet.of(Permission.MESSAGE_SEND)).complete();
        category.createTextChannel("bribes")
                .addPermissionOverride(event.getGuild().getRolesByName("Observer", true).get(0), EnumSet.of(Permission.VIEW_CHANNEL), EnumSet.of(Permission.MESSAGE_SEND)).complete();
        category.createTextChannel("bidding-phase")
                .addPermissionOverride(event.getGuild().getRolesByName("Observer", true).get(0), EnumSet.of(Permission.VIEW_CHANNEL), EnumSet.of(Permission.MESSAGE_SEND)).complete();
        category.createTextChannel("rules")
                .addPermissionOverride(event.getGuild().getRolesByName("Observer", true).get(0), EnumSet.of(Permission.VIEW_CHANNEL), EnumSet.of(Permission.MESSAGE_SEND)).complete();
        category.createTextChannel("pre-game-voting")
                .addPermissionOverride(event.getGuild().getRolesByName("Observer", true).get(0), EnumSet.of(Permission.VIEW_CHANNEL), EnumSet.of(Permission.MESSAGE_SEND)).complete();
        category.createTextChannel("mod-info")
                .addPermissionOverride(gameRole, null, EnumSet.of(Permission.VIEW_CHANNEL))
                .addPermissionOverride(event.getMember(), EnumSet.of(Permission.VIEW_CHANNEL), null).complete();

        DiscordGame discordGame = new DiscordGame(event, category);
        discordGame.getTextChannel("rules").sendMessage("""
            <:DuneRulebook01:991763013814198292>  Dune rulebook: https://www.gf9games.com/dunegame/wp-content/uploads/Dune-Rulebook.pdf
            <:weirding:991763071775297681>  Dune FAQ Nov 20: https://www.gf9games.com/dune/wp-content/uploads/2020/11/Dune-FAQ-Nov-2020.pdf
            <:ix:991763319406997514> <:bt:991763325576810546>  Ixians & Tleilaxu Rules: https://www.gf9games.com/dunegame/wp-content/uploads/2020/09/IxianAndTleilaxuRulebook.pdf
            <:choam:991763324624703538> <:rich:991763318467465337> CHOAM & Richese Rules: https://www.gf9games.com/dune/wp-content/uploads/2021/11/CHOAM-Rulebook-low-res.pdf""").queue();

        Game game = new Game();
        game.setGameRole(gameRole.getName());
        game.setModRole(modRole.getName());
        game.setMute(false);
        discordGame.setGameState(game);
        discordGame.pushGameState();
    }

    public void addFaction(SlashCommandInteractionEvent event, DiscordGame discordGame, Game gameState) throws ChannelNotFoundException {
        TextChannel modInfo = discordGame.getTextChannel("mod-info");
        if (!gameState.getResource("turn").getValue().equals(0)) {
            modInfo.sendMessage("The game has already started, you can't add more factions!").queue();
            return;
        }
        if (gameState.getFactions().size() >= 6) {
            modInfo.sendMessage("This game is already full!").queue();
            return;
        }
        String factionName = event.getOption("factionname").getAsString();
        if (gameState.hasFaction(factionName)) {
            modInfo.sendMessage("This faction has already been taken!").queue();
            return;
        }

        new Faction(FactionName.valueOf(factionName), event.getOption("player").getAsUser().getAsMention(), event.getOption("player").getAsMember().getNickname(), gameState);

        Category game = discordGame.getGameCategory();
        discordGame.pushGameState();
        game.createTextChannel(factionName.toLowerCase() + "-info").addPermissionOverride(event.getOption("player").getAsMember(), EnumSet.of(Permission.VIEW_CHANNEL), EnumSet.of(Permission.MESSAGE_SEND))
                .addPermissionOverride(event.getMember(), EnumSet.of(Permission.VIEW_CHANNEL), null)
                .addPermissionOverride(event.getGuild().getRolesByName(gameState.getGameRole(), true).get(0), null, EnumSet.of(Permission.VIEW_CHANNEL)).queue();

        game.createTextChannel(factionName.toLowerCase() + "-chat").addPermissionOverride(event.getOption("player").getAsMember(), EnumSet.of(Permission.VIEW_CHANNEL), null)
                .addPermissionOverride(event.getGuild().getRolesByName(gameState.getGameRole(), true).get(0), null, EnumSet.of(Permission.VIEW_CHANNEL))
                .addPermissionOverride(event.getMember(), EnumSet.of(Permission.VIEW_CHANNEL), null).queue();

    }

    public void newFactionResource(SlashCommandInteractionEvent event, DiscordGame discordGame, Game gameState) throws ChannelNotFoundException {
        gameState.getFaction(event.getOption("factionname").getAsString())
                .addResource(new Resource(event.getOption("resource").getAsString(),
                event.getOption("value").getAsString()));
        discordGame.pushGameState();
    }

    public void resourceAddOrSubtract(SlashCommandInteractionEvent event, DiscordGame discordGame, Game gameState) throws ChannelNotFoundException, InvalidGameStateException {
        String factionName = event.getOption("factionname").getAsString();
        String resourceName = event.getOption("resource").getAsString();
        int amount = event.getOption("amount").getAsInt();

        Resource resource = gameState.getFaction(factionName).getResource(resourceName);

        if (resource instanceof IntegerResource) {
            ((IntegerResource) resource).addValue(amount);
        } else {
            throw new InvalidGameStateException("Resource is not numeric");
        }

        writeFactionInfo(discordGame, gameState.getFaction(factionName));
        discordGame.pushGameState();
    }

    public void removeResource(SlashCommandInteractionEvent event, DiscordGame discordGame, Game gameState) throws ChannelNotFoundException {
        gameState.getFaction(event.getOption("factionname").getAsString()).removeResource(event.getOption("resource").getAsString());
        discordGame.pushGameState();
    }

    public void drawCard(SlashCommandInteractionEvent event, DiscordGame discordGame, Game gameState) throws ChannelNotFoundException {
        Faction faction = gameState.getFaction(event.getOption("factionname").getAsString());
        faction.addTreacheryCard(gameState.getTreacheryDeck().pop());
        discordGame.pushGameState();
    }

    public String drawCard(Game gameState, String deckName, String faction) {
        switch (deckName) {
            case "spice deck" -> {
                LinkedList<SpiceCard> deck = gameState.getSpiceDeck();
                LinkedList<SpiceCard> a = gameState.getSpiceDiscardA();
                LinkedList<SpiceCard> b = gameState.getSpiceDiscardB();
                if (deck.isEmpty()) {
                    deck.addAll(a);
                    deck.addAll(b);
                    a.clear();
                    b.clear();
                    Collections.shuffle(deck);
                }
                SpiceCard drawn = deck.pop();
                if (gameState.getTurn() == 1 && drawn.name().equals("Shai-Hulud")) {
                    deck.add(drawn);
                    Collections.shuffle(deck);
                    drawCard(gameState, deckName, faction);
                }
                if (faction.equals("A: ")) a.add(drawn);
                else b.add(drawn);
                if (!drawn.name().equals("Shai-Hulud") && gameState.getStorm() != drawn.sector()) {
                   gameState.getTerritories().get(drawn.name()).addSpice(drawn.spice());
                }
                if (drawn.name().equals("Shai-Hulud")) return drawn.name() + ", " + drawCard(gameState, deckName, faction);
            }
            case "traitor deck" -> gameState.getFaction(faction).getTraitorHand().add(gameState.getTraitorDeck().pop());
            case "treachery deck" -> gameState.getFaction(faction).getTreacheryHand().add(gameState.getTreacheryDeck().pop());
        }
        return "";
    }

    public void discard(SlashCommandInteractionEvent event, DiscordGame discordGame, Game gameState) throws ChannelNotFoundException {
        Faction faction = gameState.getFaction(event.getOption("factionname").getAsString());
        List<TreacheryCard> hand = faction.getTreacheryHand();
        int i = 0;
        for (; i < hand.size(); i++) {
            String card = hand.get(i).name();
            if (card.toLowerCase().contains(event.getOption("card").getAsString().toLowerCase())) {
                gameState.getTreacheryDiscard().add(hand.get(i));
                break;
            }
        }
        hand.remove(i);
        writeFactionInfo(discordGame, faction));
        discordGame.pushGameState();
    }

    public void transferCard(SlashCommandInteractionEvent event, DiscordGame discordGame, Game gameState) throws ChannelNotFoundException {
        Faction giver = gameState.getFaction(event.getOption("sender").getAsString());
        Faction receiver = gameState.getFaction(event.getOption("recipient").getAsString());
        List<TreacheryCard> giverHand = giver.getTreacheryHand();
        List<TreacheryCard> receiverHand = receiver.getTreacheryHand();

        if ((receiver.getHandLimit() == receiverHand.size())) {
            event.getUser().openPrivateChannel().queue(privateChannel -> privateChannel.sendMessage("The recipient's hand is full!").queue());
            return;
        }
        int i = 0;

        boolean cardFound = false;
        for (; i < giverHand.size(); i++) {
            String card = giverHand.get(i).name();
            if (card.toLowerCase().contains(event.getOption("card").getAsString())) {
                cardFound = true;
                receiverHand.add(giverHand.get(i));
                break;
            }
        }
        if (!cardFound) {
            event.getUser().openPrivateChannel().queue(privateChannel -> privateChannel.sendMessage("Could not find that card!").queue());
            return;
        }
        giverHand.remove(i);
        writeFactionInfo(discordGame, giver);
        writeFactionInfo(discordGame, receiver);
        discordGame.pushGameState();
    }

    public void putBack(SlashCommandInteractionEvent event, DiscordGame discordGame, Game gameState) {
        LinkedList<TreacheryCard> market = gameState.getMarket();
        int i = 0;
        boolean found = false;
        for (; i < market.size(); i++) {
            if (market.get(i).name().contains(event.getOption("card").getAsString())) {
                if (!event.getOption("bottom").getAsBoolean()) gameState.getTreacheryDeck().addFirst(market.get(i));
                else gameState.getTreacheryDeck().addLast(market.get(i));
                found = true;
                break;
            }
        }
        if (!found) {
            event.getUser().openPrivateChannel().queue(privateChannel -> privateChannel.sendMessage("Card not found, are you sure it's there?").queue());
            return;
        }
        market.remove(i);
        Collections.shuffle(market);
        discordGame.pushGameState();
    }

    public void ixHandSelection(SlashCommandInteractionEvent event, DiscordGame discordGame, Game gameState) throws ChannelNotFoundException {
        List<TreacheryCard> hand = gameState.getFaction("Ix").getTreacheryHand();
        Collections.shuffle(hand);
        for (TreacheryCard treacheryCard : hand) {
            if (treacheryCard.name().toLowerCase().contains(event.getOption("card").getAsString())) continue;
            gameState.getTreacheryDeck().addFirst(treacheryCard);
        }
        int shift = 0;
        int length = hand.size() - 1;
        for (int i = 0; i < length; i++) {
            if (hand.get(0).name().toLowerCase().contains(event.getOption("card").getAsString())) {
                shift = 1;
                continue;
            }
            hand.remove(shift);
        }
        writeFactionInfo(discordGame, gameState.getFaction("Ix"));
        discordGame.pushGameState();
    }

    public void awardBid(SlashCommandInteractionEvent event, DiscordGame discordGame, Game gameState) throws ChannelNotFoundException {
        Faction winner = gameState.getFaction(event.getOption("factionname").getAsString());
        List<TreacheryCard> winnerHand = winner.getTreacheryHand();
        int spent = event.getOption("spent").getAsInt();
        LinkedList<TreacheryCard> market = gameState.getMarket();
        if (winner.getHandLimit() == winnerHand.size()) {
            event.getUser().openPrivateChannel().queue(privateChannel -> privateChannel.sendMessage("Player's hand is full, they cannot bid on this card!").queue());
            return;
        }
        if (market.size() > 0) {
            winnerHand.add(market.pop());
            discordGame.sendMessage("turn-summary", winner.getEmoji() + " wins card up for bid for " + spent + " <:spice4:991763531798167573>");
        } else {
            discordGame.sendMessage("mod-info", "No more cards up for bid.");
            return;
        }
        if (gameState.hasFaction("Atreides") && market.size() > 0) {
            discordGame.sendMessage("atreides-info", "The next card up for bid is <:treachery:991763073281040518> "
                            + market.get(0).name() + " <:treachery:991763073281040518>");
        }

        if (market.size() > 0) {
                StringBuilder message = new StringBuilder();
                int cardNumber = (int) gameState.getResource("market_size").getValue() - market.size();
                message.append("R").append(gameState.getResource("turn").getValue()).append(":C").append(cardNumber + 1).append("\n");
                int firstBid = Math.ceilDiv((int) gameState.getResource("storm").getValue(), 3) + 1 + cardNumber;
                for (int i = 0; i < gameState.getFactions().size(); i++) {
                    int playerPosition = (firstBid + i) % 6;
                    if (playerPosition == 0) playerPosition = 6;
                    List<Faction> turnOrder = gameState.getFactions();
                    Faction faction = turnOrder.get(playerPosition);
                    List<TreacheryCard> hand = faction.getTreacheryHand();
                    if (faction.getHandLimit() > hand.size()) {
                        message.append(faction.getEmoji()).append(":");
                        if (i == 0) message.append(" ").append(faction.getPlayer());
                        message.append("\n");
                    }
                }
                discordGame.sendMessage("bidding-phase", message.toString());
        }
        if (winner.getName().equals("Harkonnen") && winnerHand.size() < winner.getHandLimit()) {
            drawCard(gameState, "treachery_deck", "Harkonnen");
            discordGame.sendMessage("turn-summary", winner.getEmoji() + " draws another card from the <:treachery:991763073281040518> deck.");
        }
        winner.subtractSpice(spent);
        spiceMessage(discordGame, spent, winner.getName(), "R" +
                gameState.getResource("turn").getValue() + ":C" + (gameState.getMarketSize() - gameState.getMarket().size()), false);
        writeFactionInfo(discordGame, winner);
        if (gameState.hasFaction("Emperor") && !winner.getName().equals("Emperor")) {
            gameState.getFaction("Emperor").addSpice(spent);
            discordGame.sendMessage("turn-summary", gameState.getFaction("Emperor").getEmoji() + " is paid " + spent + " <:spice4:991763531798167573>");
            writeFactionInfo(discordGame, gameState.getFaction("Emperor"));
        }
        spiceMessage(discordGame, spent, "emperor", "R" +
                gameState.getTurn() + ":C" + (gameState.getMarketSize() - gameState.getMarket().size()), true);
        discordGame.pushGameState();
    }

    public void spiceMessage(DiscordGame discordGame, int amount, String faction, String message, boolean plus) throws ChannelNotFoundException {
        String plusSign = plus ? "+" : "-";
        for (TextChannel channel : discordGame.getTextChannels()) {
            if (channel.getName().equals(faction.toLowerCase() + "-info")) {
                discordGame.sendMessage(channel.getName(), plusSign + amount + "<:spice4:991763531798167573> for " + message);
            }
        }
    }

    public void killLeader(SlashCommandInteractionEvent event, DiscordGame discordGame, Game gameState) throws ChannelNotFoundException {
        Faction faction = gameState.getFaction(event.getOption("factionname").getAsString());
        gameState.getLeaderTanks().add(faction.removeLeader(event.getOption("leader").getAsString().toLowerCase()));
        discordGame.pushGameState();
    }

    public void reviveLeader(SlashCommandInteractionEvent event, DiscordGame discordGame, Game gameState) throws ChannelNotFoundException {
        Faction faction = gameState.getFaction(event.getOption("factionname").getAsString());
        faction.getLeaders().add(gameState.removeLeaderFromTanks(event.getOption("leader").getAsString().toLowerCase()));
        discordGame.pushGameState();
    }

    public void revival(SlashCommandInteractionEvent event, DiscordGame discordGame, Game gameState) throws ChannelNotFoundException {
        String star = event.getOption("starred").getAsBoolean() ? "*" : "";
        Faction faction = gameState.getFaction(event.getOption("factionname").getAsString());
        if (star.equals("")) faction.getReserves().addStrength(event.getOption("revived").getAsInt());
        else faction.getSpecialReserves().addStrength(event.getOption("revived").getAsInt());
        spiceMessage(discordGame, 2 * event.getOption("revived").getAsInt(), faction.getName(), "Revivals", false);
        if (gameState.hasFaction("BT")) {
            gameState.getFaction("BT").addSpice(2 * event.getOption("revived").getAsInt());
            spiceMessage(discordGame, 2 * event.getOption("revived").getAsInt(), "bt", faction.getEmoji() + " revivals", true);
            writeFactionInfo(discordGame, gameState.getFaction("BT"));
        }
        writeFactionInfo(discordGame, faction));
        discordGame.pushGameState();
    }

    public void advanceGame(SlashCommandInteractionEvent event, DiscordGame discordGame, Game gameState) throws ChannelNotFoundException {
        Category game = discordGame.getGameCategory();

        //Turn 0 is for the set-up for play section from the rules page 6.
        if (gameState.getTurn() == 0) {
            switch (gameState.getPhase()) {
                //1. Positions
                case 0 -> {
                    Collections.shuffle(gameState.getTreacheryDeck());
                    Collections.shuffle(gameState.getSpiceDeck());
                    Collections.shuffle(gameState.getFactions());
                    gameState.advancePhase();
                    //If Bene Gesserit are present, time to make a prediction
                    if (gameState.hasFaction("BG")) {
                        discordGame.sendMessage("bg-chat", "Please make your secret prediction.");
                    }
                }
                //2. Traitors
                case 1 -> {
                    Collections.shuffle(gameState.getTraitorDeck());
                    for (Faction faction : gameState.getFactions()) {
                        if (!faction.getName().equals("BT")) {
                            for (int j = 0; j < 4; j++) {
                                drawCard(gameState, "traitor deck", faction.getName());
                            }
                            writeFactionInfo(discordGame, faction);
                        }
                    }
                    for (TextChannel channel : discordGame.getTextChannels()) {
                            if (channel.getName().contains("-chat") && !channel.getName().contains("game") &&
                                    !channel.getName().contains("harkonnen") && !channel.getName().contains("bt")) discordGame.sendMessage(channel.getName(), "Please select your traitor.");
                        }


                        gameState.advancePhase();
                    //If Bene Tleilax are not present, advance past the Face Dancers draw
                    if (!gameState.hasFaction("BT")) {
                        gameState.advancePhase();
                    }
                    discordGame.sendMessage("turn-summary", "2. Traitors are being selected.");
                }
                //Bene Tleilax to draw Face Dancers
                case 2 -> {
                    Collections.shuffle(gameState.getTraitorDeck());
                    drawCard(gameState, "traitor_deck", "BT");
                    drawCard(gameState, "traitor_deck", "BT");
                    drawCard(gameState, "traitor_deck", "BT");
                    writeFactionInfo(discordGame, gameState.getFaction("BT"));
                    gameState.advancePhase();
                    discordGame.sendMessage("turn-summary", "2b. Bene Tleilax have drawn their Face Dancers.");
                }
                //3. Spice, 4. Forces
                case 3 -> {
                    if (gameState.hasFaction("Fremen")) {
                        discordGame.sendMessage("fremen-chat", "Please distribute 10 forces between Sietch Tabr, False Wall South, and False Wall West");
                    }
                    gameState.advancePhase();
                    //If BG is not present, advance past the next step
                    if (!gameState.hasFaction("BG")) {
                        gameState.advancePhase();
                    }
                    discordGame.sendMessage("turn-summary", "3. Spice has been allocated.\n4. Forces are being placed on the board.");
                }
                case 4 -> {
                    if (gameState.hasFaction("BG")) {
                        discordGame.sendMessage("bg-chat", "Please choose where to place your advisor.");
                    }

                    gameState.advancePhase();
                    //If Ix is not present, advance past the next step
                    if (!gameState.hasFaction("Ix")) {
                        gameState.advancePhase();
                    }
                }
                //Ix to select from starting treachery cards
                case 5 -> {
                    if (gameState.hasFaction("Ix")) {
                        int toDraw = gameState.getFactions().size();
                        if (gameState.hasFaction("Harkonnen")) toDraw++;
                        for (int i = 0; i < toDraw; i++) {
                            drawCard(gameState, "treachery_deck", "Ix");
                        }
                        writeFactionInfo(discordGame, gameState.getFaction("Ix"));
                        discordGame.sendMessage("ix-chat", "Please select one treachery card to keep in your hand.");
                        discordGame.sendMessage("turn-summary", "Ix is selecting their starting treachery card.");
                    }
                    gameState.advancePhase();
                }
                //5. Treachery
                case 6 -> {
                    for (Faction faction : gameState.getFactions()) {
                        if (!faction.getName().equals("Ix")) drawCard(gameState, "treachery deck", faction.getName());
                        if (faction.getName().equals("Harkonnen")) drawCard(gameState, "treachery deck", faction.getName());
                        writeFactionInfo(discordGame, faction);
                    }
                    gameState.advancePhase();
                    discordGame.sendMessage("turn-summary", "5. Treachery cards are being dealt.");
                }
                //6. Turn Marker (prompt for dial for First Storm)
                case 7 -> {
                    discordGame.sendMessage(gameState.getFactions().get(0).getName().toLowerCase() + "-chat", "Please submit your dial for initial storm position.");
                    discordGame.sendMessage(gameState.getFactions().get(gameState.getFactions().size() - 1).getName().toLowerCase() + "-chat", "Please submit your dial for initial storm position.");
                    gameState.setStormMovement(new Random().nextInt(6) + 1);
                    gameState.advanceTurn();
                    discordGame.sendMessage("turn-summary", "6. Turn Marker is set to turn 1.  The game is beginning!  Initial storm is being calculated...");
                }
            }
            discordGame.pushGameState();
        }
        else {
            switch (gameState.getPhase()) {
                //1. Storm Phase
                case 1 -> {
                    discordGame.sendMessage("turn-summary", "Turn " + gameState.getTurn() + " Storm Phase:");
                    Map<String, Territory> territories = gameState.getTerritories();
                   if (gameState.getTurn() != 1) {
                       discordGame.sendMessage("turn-summary", "The storm moves " + gameState.getStormMovement() + " sectors this turn.");
                       for (int i = 0; i < gameState.getStormMovement(); i++) {
                           gameState.setStorm(gameState.getStorm() + 1); 
                           if (gameState.getStorm() == 19) gameState.setStorm(1);
                           for (String territoryName : territories.keySet()) {
                               Territory territory = territories.get(territoryName); 
                               if (!territory.isRock() && territory.getSector() == gameState.getStorm()) {
                                   List<Force> forces = territory.getForces();
                                   boolean fremenSpecialCase = false;
                                   //Defaults to play "optimally", destorying Fremen regular forces over Fedaykin
                                   if (forces.contains("Fremen") && forces.contains("Fremen*")) {
                                       fremenSpecialCase = true;
                                       int fremenForces = 0;
                                       int fremenFedaykin = 0;
                                       for (Force force : forces) {
                                           if (force.getName().equals("Fremen")) fremenForces = force.getStrength();
                                           if (force.getName().equals("Fremen*")) fremenFedaykin = force.getStrength();
                                       }
                                       int lost = (fremenForces + fremenFedaykin) / 2;
                                       if (lost < fremenForces) {
                                           for (Force force : forces) {
                                               if (force.getName().equals("Fremen")) force.setStrength(force.getStrength() - lost);
                                           }
                                       } else if (lost > fremenForces) {
                                           forces.removeIf(force -> force.getName().equals("Fremen"));
                                           for (Force force : forces) {
                                               if (force.getName().equals("Fremen*")) force.setStrength(fremenFedaykin - lost + fremenForces);
                                           }
                                       }
                                       discordGame.sendMessage("turn-summary",gameState.getFaction("Fremen").getEmoji() + " lost " + lost +
                                                       " forces to the storm in " + territory.getTerritoryName());
                                   }
                                   for (Force force : forces) {
                                       if (force.getName().contains("Fremen") && fremenSpecialCase) continue;
                                       int lost = force.getStrength();
                                       forces.remove(force);
                                       if (force.getName().contains("Fremen") && lost > 1) {
                                           lost /= 2;
                                           force.setStrength(lost);
                                           forces.add(force);
                                       }
                                       gameState.getTanks().stream().filter(force1 -> force1.getName().equals(force.getName())).findFirst().orElseThrow().addStrength(force.getStrength());
                                       discordGame.sendMessage("turn-summary",
                                               gameState.getFaction(force.getName().replace("*", "")).getEmoji() + " lost " +
                                                       lost + " forces to the storm in " + territory);
                                   }

                               }
                               territory.setSpice(0);
                           }
                       }
                   }
                   if (gameState.hasFaction("Fremen")) {
                       gameState.setStormMovement(new Random().nextInt(6) + 1);
                       discordGame.sendMessage("fremen-chat", "The storm will move " + gameState.getStormMovement() + " sectors next turn.");

                   }
                   gameState.advancePhase();
                }
                //2. Spice Blow and Nexus
                case 2 -> {
                    discordGame.sendMessage("turn-summary", "Turn " + gameState.getTurn() + " Spice Blow Phase:");
                    discordGame.sendMessage("turn-summary", "A: " + drawCard(gameState, "spice deck", "A"));
                    discordGame.sendMessage("turn-summary", "B: " + drawCard(gameState, "spice deck", "B"));
                    gameState.advancePhase();
                }
                //3. Choam Charity
                case 3 -> {
                    discordGame.sendMessage("turn-summary", "Turn " + gameState.getTurn() + " CHOAM Charity Phase:");
                    int multiplier = 1;
                    if (gameState.getResources().stream().anyMatch(resource -> resource.getName().equals("inflation token"))) {
                        if (gameState.getResources().stream().filter(resource -> resource.getName().equals("inflation token")).findFirst().orElseThrow().getValue().equals("cancel")) {
                            discordGame.sendMessage("turn-summary","CHOAM Charity is cancelled!");
                            gameState.advancePhase();
                            break;
                        } else {
                            multiplier = 2;
                        }
                    }

                    int choamGiven = 0;
                    List<Faction> factions = gameState.getFactions();
                    if (factions.stream().anyMatch(f -> f.getName().equals("CHOAM"))) discordGame.sendMessage("turn-summary",
                            gameState.getFaction("CHOAM").getEmoji() + " receives " +
                            gameState.getFactions().size() * 2 * multiplier + " <:spice4:991763531798167573> in dividends from their many investments."
                    );
                    for (Faction faction : factions) {
                        if (faction.getName().equals("CHOAM")) continue;
                        int spice = faction.getSpice();
                        if (faction.getName().equals("BG")) {
                            choamGiven += 2 * multiplier;
                            faction.addSpice(2 * multiplier);
                            discordGame.sendMessage("turn-summary", faction.getEmoji() + " have received " + 2 * multiplier + " <:spice4:991763531798167573> in CHOAM Charity.");
                            spiceMessage(discordGame, 2 * multiplier, faction.getName(), "CHOAM Charity", true);
                        }
                        else if (spice < 2) {
                            int charity = (2 * multiplier) - (spice * multiplier);
                            choamGiven += charity;
                            faction.addSpice(charity);
                            discordGame.sendMessage("turn-summary",
                                    faction.getEmoji() + " have received " + charity + " <:spice4:991763531798167573> in CHOAM Charity."
                            );
                            spiceMessage(discordGame, charity, faction.getName(), "CHOAM Charity", true);
                        }
                        else continue;
                        writeFactionInfo(discordGame, faction);
                    }
                    if (gameState.hasFaction("CHOAM")) {
                        gameState.getFaction("CHOAM").addSpice((10 * multiplier) - choamGiven);
                        spiceMessage(discordGame, gameState.getFactions().size() * 2 * multiplier, "choam", "CHOAM Charity", true);
                        discordGame.sendMessage("turn-summary",
                                gameState.getFaction("CHOAM").getEmoji() + " has paid " + choamGiven + " <:spice4:991763531798167573> to factions in need."
                        );
                        spiceMessage(discordGame, choamGiven, "choam", "CHOAM Charity given", false);
                    }
                    gameState.advancePhase();
                }
                //4. Bidding
                case 4 -> {
                    discordGame.sendMessage("turn-summary", "Turn " + gameState.getTurn() + " Bidding Phase:");
                    int cardsUpForBid = 0;
                    List<Faction> factions = gameState.getFactions();
                    StringBuilder countMessage = new StringBuilder();
                    countMessage.append("<:treachery:991763073281040518>Number of Treachery Cards<:treachery:991763073281040518>\n");
                    for (Faction faction : factions) {
                        int length = faction.getTreacheryHand().size();
                        countMessage.append(faction.getEmoji()).append(": ").append(length).append("\n");
                        if (faction.getHandLimit() > length) cardsUpForBid++;
                        if (faction.getName().equals("Ix")) cardsUpForBid++;
                        if (faction.getName().equals("Rich")) cardsUpForBid--;
                    }
                    if (gameState.hasFaction("Ix")) {
                        discordGame.sendMessage("ix-chat", "Please select a card to put back to top or bottom.");
                    }
                    countMessage.append("There will be ").append(cardsUpForBid).append(" <:treachery:991763073281040518> cards up for bid this round.");
                    discordGame.sendMessage("turn-summary", countMessage.toString());
                    gameState.setMarketSize(cardsUpForBid);
                    for (int i = 0; i < cardsUpForBid; i++) {
                        gameState.getMarket().add(gameState.getTreacheryDeck().pop());
                        if (gameState.hasFaction("Ix")) {
                            discordGame.sendMessage("ix-chat", "<:treachery:991763073281040518> " +
                                    gameState.getMarket().peek().name() + " <:treachery:991763073281040518>");
                        }
                    }
                    if (gameState.hasFaction("Atreides")) {
                        discordGame.sendMessage("atreides-chat","The first card up for bid is <:treachery:991763073281040518> " + gameState.getMarket().peek().name() + " <:treachery:991763073281040518>");
                    }
                    StringBuilder message = new StringBuilder();
                    message.append("R").append(gameState.getTurn()).append(":C1\n");
                    int firstBid = Math.ceilDiv(gameState.getStorm(), 3) + 1;
                    for (int i = 0; i < factions.size(); i++) {
                        int playerPosition = firstBid + i > 6 ? firstBid + i - 6 : firstBid + i;
                        Faction faction = gameState.getFactions().get(playerPosition);
                        if (faction.getHandLimit() > faction.getTreacheryHand().size()) message.append(faction.getEmoji()).append(":");
                                if (i == 0) message.append(" ").append(faction.getPlayer());
                                message.append("\n");
                    }
                    discordGame.sendMessage("bidding-phase", message.toString());
                    gameState.advancePhase();
                }
                //5. Revival
                case 5 -> {
                    if (gameState.getMarket().size() > 0) {
                        int marketLength = gameState.getMarket().size();
                        discordGame.sendMessage("turn-summary", "There were " + marketLength + " cards not bid on this round that are placed back on top of the <:treachery:991763073281040518> deck.");
                        while (!gameState.getMarket().isEmpty()) {
                            gameState.getTreacheryDeck().add(gameState.getMarket().pop());
                        }
                    }
                    gameState.setMarketSize(0);
                    discordGame.sendMessage("turn-summary", "Turn " + gameState.getTurn() + " Revival Phase:");
                    List<Faction> factions = gameState.getFactions();
                    StringBuilder message = new StringBuilder();
                    message.append("Free Revivals:\n");
                    for (Faction faction : factions) {
                        int revived = 0;
                        boolean revivedStar = false;
                        for (int i = faction.getFreeRevival(); i > 0; i--) {
                            if (gameState.getTanks().getInt(faction) == 0
                                    && (gameState.getTanks().isNull(faction + "*") || gameState.getTanks().getInt(faction + "*") == 0)) continue;
                            revived++;
                            if (!gameState.getTanks().isNull(faction + "*") && gameState.getTanks().getInt(faction + "*") != 0 && !revivedStar) {
                                int starred = gameState.getTanks().getInt(faction + "*");
                                gameState.getTanks().remove(faction + "*");
                                if (starred > 1) gameState.getTanks().put(faction + "*", starred - 1);
                                revivedStar = true;
                                int reserves = gameState.getFaction(faction).getJSONObject("resources").getInt("reserves*");
                                gameState.getFaction(faction).getJSONObject("resources").remove("reserves*");
                                gameState.getFaction(faction).getJSONObject("resources").put("reserves*", reserves + 1);
                            } else if (gameState.getTanks().getInt(faction) != 0) {
                                int forces = gameState.getTanks().getInt(faction);
                                gameState.getTanks().remove(faction);
                                gameState.getTanks().put(faction, forces - 1);
                                int reserves = gameState.getFaction(faction).getJSONObject("resources").getInt("reserves");
                                gameState.getFaction(faction).getJSONObject("resources").remove("reserves");
                                gameState.getFaction(faction).getJSONObject("resources").put("reserves", reserves + 1);
                            }
                        }
                        if (revived > 0) {
                            message.append(gameState.getFaction(faction).getEmoji()).append(": ").append(revived).append("\n");
                        }
                    }
                    discordGame.sendMessage("turn-summary", message.toString());
                    gameState.advancePhase();
                }
                //6. Shipment and Movement
                case 6 -> {
                    discordGame.sendMessage("turn-summary","Turn " + gameState.getTurn() + " Shipment and Movement Phase:");
                    if (gameState.hasFaction("Atreides")) {
                        discordGame.sendMessage("atreides-info", "You see visions of " + gameState.getDeck("spice_deck").getString(gameState.getDeck("spice_deck").length() - 1) + " in your future.");
                    }
                    if(!gameState.getFactions().isNull("BG")) {
                       for (String territoryName : gameState.getGameBoard().keySet()) {
                           JSONObject territory = gameState.getTerritory(territoryName);
                           if (!territory.getForces().keySet().contains("Advisor")) continue;
                           discordGame.sendMessage("turn-summary",gameState.getFaction("BG").getEmoji() + " to decide whether to flip their advisors in " + territory.getString("territory_name"));
                       }
                    }
                    gameState.advancePhase();
                }
                //TODO: 7. Battle
                case 7 -> {
                    discordGame.sendMessage("turn-summary","Turn " + gameState.getTurn() + " Battle Phase:");
                    gameState.advancePhase();

                }
                //TODO: 8. Spice Harvest
                case 8 -> {
                    discordGame.sendMessage("turn-summary", "Turn " + gameState.getTurn() + " Spice Harvest Phase:");
                   JSONObject territories = gameState.getTerritories();
                   //This is hacky, but I add spice to Arrakeen, Carthag, and Tuek's, then if it is not collected by the following algorithm, it is removed.
                    territories.getJSONObject("Arrakeen").remove("spice");
                    territories.getJSONObject("Carthag").remove("spice");
                    territories.getJSONObject("Tuek's Sietch").remove("spice");
                    territories.getJSONObject("Arrakeen").put("spice", 2);
                    territories.getJSONObject("Carthag").put("spice", 2);
                    territories.getJSONObject("Tuek's Sietch").put("spice", 1);
                    for (String territoryName : territories.keySet()) {
                        JSONObject territory = territories.getJSONObject(territoryName);
                        if (territory.getInt("spice") == 0 || territory.getForces().length() == 0) continue;
                        int spice = territory.getInt("spice");
                        territory.remove("spice");
                        Set<String> factions = territory.getForces().keySet();
                        for (Faction faction : factions) {
                            int forces = territory.getForces().getInt(faction);
                            forces += territory.getForces().isNull(faction + "*") ? 0 : territory.getForces().getInt(faction + "*");
                            int toCollect = 0;
                            if (faction.equals("BG") && factions.size() > 1) continue;
                            //If the faction has mining equipment, collect 3 spice per force.
                            if ((!territories.getJSONObject("Arrakeen").getForces().isNull(faction) || !territories.getJSONObject("Carthag").getForces().isNull(faction) && !faction.equals("BG")) ||
                                    (faction.equals("BG") && (territories.getJSONObject("Arrakeen").getForces().length() < 2 && !territories.getJSONObject("Arrakeen").getForces().isNull("BG")) ||
                                            (territories.getJSONObject("Carthag").getForces().length() < 2 && !territories.getJSONObject("Carthag").getForces().isNull("BG")))) {
                                toCollect += forces * 3;
                            } else toCollect += forces * 2;
                            if (spice < toCollect) {
                                toCollect = spice;
                                spice = 0;
                            } else spice -= toCollect;
                            territory.put("spice", spice);
                            int factionSpice = gameState.getFaction(faction).getJSONObject("resources").getInt("spice");
                            gameState.getFaction(faction).getJSONObject("resources").remove("spice");
                            gameState.getFaction(faction).getJSONObject("resources").put("spice", factionSpice + toCollect);
                            discordGame.sendMessage("turn-summary", gameState.getFaction(faction).getEmoji() + " collects " + toCollect + " <:spice4:991763531798167573> from " + territoryName);
                        }

                    }
                    territories.getJSONObject("Arrakeen").remove("spice");
                    territories.getJSONObject("Carthag").remove("spice");
                    territories.getJSONObject("Tuek's Sietch").remove("spice");
                    territories.getJSONObject("Arrakeen").put("spice", 0);
                    territories.getJSONObject("Carthag").put("spice", 0);
                    territories.getJSONObject("Tuek's Sietch").put("spice", 0);
                    gameState.advancePhase();
                }
                //TODO: 9. Mentat Pause
                case 9 -> {
                    discordGame.sendMessage("turn-summary", "Turn " + gameState.getTurn() + " Mentat Pause Phase:");
                    for (Faction faction : gameState.getFactions().keySet()) {
                        if (!gameState.getFaction(faction).getJSONObject("resources").getJSONObject("front_of_shield").isNull("spice")) {
                            discordGame.sendMessage("turn-summary", gameState.getFaction(faction).getEmoji() + " collects " +
                                    gameState.getFaction(faction).getJSONObject("resources").getJSONObject("front_of_shield").getInt("spice") + " <:spice4:991763531798167573> from front of shield.");
                            spiceMessage(discordGame,  gameState.getFaction(faction).getJSONObject("resources").getJSONObject("front_of_shield").getInt("spice"), faction, "front of shield", true);
                            gameState.getFaction(faction).getJSONObject("resources").put("spice", gameState.getFaction(faction).getJSONObject("resources").getInt("spice") +
                                    gameState.getFaction(faction).getJSONObject("resources").getJSONObject("front_of_shield").getInt("spice"));
                            gameState.getFaction(faction).getJSONObject("resources").getJSONObject("front_of_shield").remove("spice");
                        }
                        writeFactionInfo(event, gameState, discordGame, faction);
                    }
                    gameState.advanceTurn();
                }
            }
            discordGame.pushGameState();
            drawGameBoard(discordGame, gameState);
        }
    }

//    public void selectTraitor(SlashCommandInteractionEvent event, DiscordGame discordGame, Game gameState) throws ChannelNotFoundException {
//        JSONObject faction = gameState.getFaction(event.getOption("factionname").getAsString());
//        for (int i = 0; i < 4; i++) {
//            if (!faction.getJSONObject("resources").getJSONArray("traitors").getString(i).toLowerCase().contains(event.getOption("traitor").getAsString().toLowerCase())) {
//                gameState.getDeck("traitor_deck").put(faction.getJSONObject("resources").getJSONArray("traitors").get(i));
//                String traitor = faction.getJSONObject("resources").getJSONArray("traitors").getString(i);
//                faction.getJSONObject("resources").getJSONArray("traitors").put(i, "~~" + traitor + "~~");
//            }
//        }
//        writeFactionInfo(event, gameState, discordGame, event.getOption("factionname").getAsString());
//        discordGame.pushGameState();
//    }
//
//    public String getTerritoryString(SlashCommandInteractionEvent event, Game gameState) {
//        String sector = event.getOption("sector") == null ? "" : "(" + event.getOption("sector").getAsString() + ")";
//        String territory = "";
//        if (event.getOption("mostlikelyterritories") == null && event.getOption("otherterritories") == null) {
//            event.getChannel().sendMessage("You have to select a territory.").queue();
//            return null;
//        } else if (event.getOption("mostlikelyterritories") == null) {
//            territory = event.getOption("otherterritories").getAsString();
//        } else {
//            territory = event.getOption("mostlikelyterritories").getAsString();
//        }
//        if (gameState.getTerritories().isNull(territory + sector)) {
//            event.getChannel().sendMessage("Territory does not exist in that sector. Check your sector number and try again.").queue();
//            return null;
//        }
//        return territory + sector;
//    }
//
//    public void placeForces(SlashCommandInteractionEvent event, DiscordGame discordGame, Game gameState) throws ChannelNotFoundException {
//        String star = "";
//        String territory = getTerritoryString(event, gameState);
//
//        if (event.getOption("starred") != null && event.getOption("starred").getAsBoolean()) {
//            star = "*";
//        }
//        int reserves = gameState.getFaction(event.getOption("factionname").getAsString()).getJSONObject("resources").getInt("reserves" + star);
//        if (reserves < event.getOption("amount").getAsInt()) {
//            discordGame.sendMessage("mod-info", "This faction does not have enough forces in reserves!");
//            return;
//        }
//        gameState.getFaction(event.getOption("factionname").getAsString()).getJSONObject("resources").remove("reserves" + star);
//        gameState.getFaction(event.getOption("factionname").getAsString()).getJSONObject("resources").put("reserves" + star, reserves - event.getOption("amount").getAsInt());
//        int previous = 0;
//
//        if (!gameState.getTerritories().getJSONObject(territory)
//                .getForces().isNull(event.getOption("factionname").getAsString() + star)) {
//            previous = gameState.getTerritories().getJSONObject(territory).getForces().getInt(event.getOption("factionname").getAsString() + star);
//        }
//
//        if (event.getOption("isshipment").getAsBoolean()) {
//            int cost = gameState.getTerritory(territory).getBoolean("is_stronghold") ? 1 : 2;
//            cost *= event.getOption("factionname").getAsString().equals("Guild") ? Math.ceilDiv(event.getOption("amount").getAsInt(), 2) : event.getOption("amount").getAsInt();
//            int spice = gameState.getFaction(event.getOption("factionname").getAsString()).getJSONObject("resources").getInt("spice");
//            gameState.getFaction(event.getOption("factionname").getAsString()).getJSONObject("resources").remove("spice");
//            if (spice < cost) {
//                discordGame.sendMessage("mod-info","This faction doesn't have the resources to make this shipment!");
//                return;
//            }
//            gameState.getFaction(event.getOption("factionname").getAsString()).getJSONObject("resources").put("spice", spice - cost);
//            spiceMessage(discordGame, cost, event.getOption("factionname").getAsString(), "shipment to " + territory, false);
//            if (gameState.getFaction("Guild") != null && !(event.getOption("factionname").getAsString().equals("Guild") || event.getOption("factionname").getAsString().equals("Fremen"))) {
//                spice = gameState.getFaction("Guild").getJSONObject("resources").getInt("spice");
//                gameState.getFaction("Guild").getJSONObject("resources").remove("spice");
//                gameState.getFaction("Guild").getJSONObject("resources").put("spice", spice + event.getOption("amount").getAsInt());
//                spiceMessage(discordGame, cost, "guild", gameState.getFaction(event.getOption("factionname").getAsString()).getEmoji() + " shipment", true);
//                writeFactionInfo(event, gameState, discordGame, "Guild");
//            }
//            writeFactionInfo(event, gameState, discordGame, event.getOption("factionname").getAsString());
//        }
//        if (gameState.getTerritory(territory).getForces().keySet().contains("BG")) {
//            discordGame.sendMessage("turn-summary", gameState.getFaction("BG").getEmoji() + " to decide whether to flip their forces in " + territory);
//        }
//        gameState.getTerritories().getJSONObject(territory).getForces().put(event.getOption("factionname").getAsString() + star, event.getOption("amount").getAsInt() + previous);
//        discordGame.pushGameState();
//        drawGameBoard(discordGame, gameState);
//    }
//
//    public void removeForces(SlashCommandInteractionEvent event, DiscordGame discordGame, Game gameState) throws ChannelNotFoundException {
//        String sector = event.getOption("sector") == null ? "" : "(" + event.getOption("sector").getAsString() + ")";
//        String territoryName = "";
//
//        if (event.getOption("mostlikelyterritories") == null && event.getOption("otherterritories") == null) {
//            discordGame.sendMessage("mod-info", "You have to select a territory.");
//            return;
//        } else if (event.getOption("mostlikelyterritories") == null) {
//            territoryName = event.getOption("otherterritories").getAsString();
//        } else {
//            territoryName = event.getOption("mostlikelyterritories").getAsString();
//        }
//        if (gameState.getTerritories().isNull(territoryName + sector)) {
//            discordGame.sendMessage("mod-info","Territory does not exist in that sector. Check your sector number and try again.");
//            return;
//        }
//        JSONObject territory = gameState.getTerritory(territoryName + sector);
//        String starred = event.getOption("starred").getAsBoolean() ? "*" : "";
//        int forces = territory.getForces().getInt(event.getOption("factionname").getAsString() + starred);
//        territory.getForces().remove(event.getOption("factionname").getAsString() + starred);
//        if (forces > event.getOption("amount").getAsInt()) {
//            territory.getForces().put(event.getOption("factionname").getAsString() + starred, forces - event.getOption("amount").getAsInt());
//        } else if (forces < event.getOption("amount").getAsInt()) {
//            discordGame.sendMessage("mod-info","You are trying to remove more forces than this faction has in this territory! Please check your info and try again.");
//            return;
//        }
//
//        if (event.getOption("totanks").getAsBoolean()) {
//            int tanks = gameState.getTanks().getInt(event.getOption("factionname").getAsString() + starred);
//            gameState.getTanks().remove(event.getOption("factionname").getAsString() + starred);
//            gameState.getTanks().put(event.getOption("factionname").getAsString() + starred, tanks + event.getOption("amount").getAsInt());
//        } else {
//            int reserves = gameState.getFaction(event.getOption("factionname").getAsString()).getJSONObject("resources").getInt("reserves" + starred);
//            gameState.getFaction(event.getOption("factionname").getAsString()).getJSONObject("resources").remove("reserves" + starred);
//            gameState.getFaction(event.getOption("factionname").getAsString()).getJSONObject("resources").put("reserves" + starred, reserves + event.getOption("amount").getAsInt());
//        }
//        discordGame.pushGameState();
//        if (event.getOption("totanks").getAsBoolean()) drawGameBoard(discordGame, gameState);
//    }
//
//    public void setStorm(SlashCommandInteractionEvent event, DiscordGame discordGame, Game gameState) throws ChannelNotFoundException {
//        gameState.getResources().remove("storm");
//        try {
//            gameState.getResources().put("storm", event.getOption("sector").getAsInt());
//        } catch (NullPointerException e) {
//            discordGame.sendMessage("mod-info", "No storm sector was selected.");
//            return;
//        }
//        discordGame.sendMessage("turn-summary","The storm has been initialized to sector " + event.getOption("sector").getAsInt());
//        discordGame.pushGameState();
//        drawGameBoard(discordGame, gameState);
//    }

    public void writeFactionInfo(DiscordGame discordGame, Faction faction) throws ChannelNotFoundException {

        String emoji = faction.getEmoji();
        List<TraitorCard> traitors = faction.getTraitorHand();
        StringBuilder traitorString = new StringBuilder();
        if (faction.getName().equals("BT")) traitorString.append("\n__Face Dancers:__\n");
        else traitorString.append("\n__Traitors:__\n");
        for (TraitorCard traitor : traitors) {
            traitorString.append(traitor.name());
            traitorString.append("\n");
        }
        for (TextChannel channel : discordGame.getTextChannels()) {
            if (channel.getName().equals(faction.getName().toLowerCase() + "-info")) {
                discordGame.sendMessage(channel.getName(), emoji + "**Faction Info**" + emoji + "\n__Spice:__ " +
                        faction.getResource("spice").getValue() +
                        traitorString);

                for (TreacheryCard treachery : faction.getTreacheryHand()) {
                    discordGame.sendMessage(channel.getName(), "<:treachery:991763073281040518> " + treachery.name() + " <:treachery:991763073281040518>");
                }
            }
        }

    }

    public void bgFlip(SlashCommandInteractionEvent event, DiscordGame discordGame, Game gameState) throws ChannelNotFoundException {
        Territory territory = getTerritoryString(event, gameState);
        int strength = 0;
        String found = "";
        for (Force force : territory.getForces()) {
            if (force.getName().equals("BG") || force.getName().equals("Advisor")) {
               strength = force.getStrength();
               found = force.getName();
            }
        }
        territory.getForces().removeIf(force -> force.getName().equals("BG") || force.getName().equals("Advisor"));
        if (found.equals("Advisor")) territory.getForces().add(new Force("BG", strength));
        else if (found.equals("BG")) territory.getForces().add(new Force("Advisor", strength));
        else {
            discordGame.sendMessage("mod-info","No Bene Gesserit were found in that territory.");
            return;
        }
        discordGame.pushGameState();
        drawGameBoard(discordGame, gameState);
    }

    private Territory getTerritoryString(SlashCommandInteractionEvent event, Game gameState) {
        if (event.getOption("mostlikelyterritories") != null) return gameState.getTerritories().get(event.getOption("mostlikelyterritories").getAsString());
        else if (event.getOption("otherterritories") != null) return gameState.getTerritories().get(event.getOption("otherterritories").getAsString());
        else throw new IllegalStateException("No territory was selected.");
    }

    public void drawGameBoard(DiscordGame discordGame, Game gameState) throws ChannelNotFoundException {
        if (gameState.getMute()) return;
        //Load png resources into a hashmap.
        HashMap<String, File> boardComponents = new HashMap<>();
        URL dir = getClass().getClassLoader().getResource("Board Components");
        try {
            for (File file : new File(dir.toURI()).listFiles()) {
                boardComponents.put(file.getName().replace(".png", ""), file);
            }
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }


        try {
            BufferedImage board = ImageIO.read(boardComponents.get("Board"));

            //Place sigils
            for (int i = 1; i <= gameState.getFactions().size(); i++) {
                BufferedImage sigil = ImageIO.read(boardComponents.get(gameState.getFactions().get(i).getName() + " Sigil"));
                Point coordinates = Initializers.getDrawCoordinates("sigil " + i);
                sigil = resize(sigil, 50, 50);
                board = overlay(board, sigil, coordinates, 1);
            }

            //Place turn, phase, and storm markers
            BufferedImage turnMarker = ImageIO.read(boardComponents.get("Turn Marker"));
            turnMarker = resize(turnMarker, 55, 55);
            int turn = gameState.getTurn() == 0 ? 1 : gameState.getTurn();
            float angle = (turn * 36) + 74f;
            turnMarker = rotateImageByDegrees(turnMarker, angle);
            Point coordinates = Initializers.getDrawCoordinates("turn " + gameState.getTurn());
            board = overlay(board, turnMarker, coordinates, 1);
            BufferedImage phaseMarker = ImageIO.read(boardComponents.get("Phase Marker"));
            phaseMarker = resize(phaseMarker, 50, 50);
            coordinates = Initializers.getDrawCoordinates("phase " + (gameState.getPhase() - 1));
            board = overlay(board, phaseMarker, coordinates, 1);
            BufferedImage stormMarker = ImageIO.read(boardComponents.get("storm"));
            stormMarker = resize(stormMarker, 172, 96);
            stormMarker = rotateImageByDegrees(stormMarker, -(gameState.getStorm() * 20));
            board = overlay(board, stormMarker, Initializers.getDrawCoordinates("storm " + gameState.getStorm()), 1);


            //Place forces
            for (String territoryName : gameState.getTerritories().keySet()) {
                Territory territory = gameState.getTerritories().get(territoryName);
                if (territory.getForces().size() == 0 && territory.getSpice() == 0) continue;
                int offset = 0;
                int i = 0;

                if (territory.getSpice() != 0) {
                    i = 1;
                    int spice = territory.getSpice();
                    while (spice != 0) {
                        if (spice >= 10) {
                            BufferedImage spiceImage = ImageIO.read(boardComponents.get("10 Spice"));
                            spiceImage = resize(spiceImage, 25,25);
                            Point spicePlacement = Initializers.getPoints(territoryName).get(0);
                            Point spicePlacementOffset = new Point(spicePlacement.x + offset, spicePlacement.y - offset);
                            board = overlay(board, spiceImage, spicePlacementOffset, 1);
                            spice -= 10;
                        } else if (spice >= 5) {
                            BufferedImage spiceImage = ImageIO.read(boardComponents.get("5 Spice"));
                            spiceImage = resize(spiceImage, 25,25);
                            Point spicePlacement = Initializers.getPoints(territoryName).get(0);
                            Point spicePlacementOffset = new Point(spicePlacement.x + offset, spicePlacement.y - offset);
                            board = overlay(board, spiceImage, spicePlacementOffset, 1);
                            spice -= 5;
                        } else if (spice >= 2) {
                            BufferedImage spiceImage = ImageIO.read(boardComponents.get("2 Spice"));
                            spiceImage = resize(spiceImage, 25,25);
                            Point spicePlacement = Initializers.getPoints(territoryName).get(0);
                            Point spicePlacementOffset = new Point(spicePlacement.x + offset, spicePlacement.y - offset);
                            board = overlay(board, spiceImage, spicePlacementOffset, 1);
                            spice -= 2;
                        } else {
                            BufferedImage spiceImage = ImageIO.read(boardComponents.get("1 Spice"));
                            spiceImage = resize(spiceImage, 25,25);
                            Point spicePlacement = Initializers.getPoints(territoryName).get(0);
                            Point spicePlacementOffset = new Point(spicePlacement.x + offset, spicePlacement.y - offset);
                            board = overlay(board, spiceImage, spicePlacementOffset, 1);
                            spice -= 1;
                        }
                        offset += 15;
                    }
                }
                offset = 0;
                for (Force force : territory.getForces()) {
                    BufferedImage forceImage = buildForceImage(boardComponents, force.getName(), force.getStrength());
                    Point forcePlacement = Initializers.getPoints(territoryName).get(i);
                    Point forcePlacementOffset = new Point(forcePlacement.x, forcePlacement.y + offset);
                    board = overlay(board, forceImage, forcePlacementOffset, 1);
                    i++;
                    if (i == Initializers.getPoints(territoryName).size()) {
                        offset += 20;
                        i = 0;
                    }
                }
            }

            //Place tanks forces
            int i = 0;
            int offset = 0;
            for (Force force : gameState.getTanks()) {
                if (force.getStrength() == 0) continue;
                BufferedImage forceImage = buildForceImage(boardComponents, force.getName(), force.getStrength());

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
            for (Leader leader : gameState.getLeaderTanks()) {
                BufferedImage leaderImage = ImageIO.read(boardComponents.get(leader.name()));
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
            discordGame.getTextChannel("turn-summary")
                    .sendFiles(boardFileUpload)
                    .queue();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public BufferedImage buildForceImage(HashMap<String, File> boardComponents, String force, int strength) throws IOException {
        BufferedImage forceImage = !force.equals("Advisor") ? ImageIO.read(boardComponents.get(force.replace("*", "") + " Troop")) : ImageIO.read(boardComponents.get("BG Advisor"));
        forceImage = resize(forceImage, 47, 29);
        if (force.contains("*")) {
            BufferedImage star = ImageIO.read(boardComponents.get("star"));
            star = resize(star, 8, 8);
            forceImage = overlay(forceImage, star, new Point(20, 7), 1);
        }
        if (strength > 9) {
            BufferedImage oneImage = ImageIO.read(boardComponents.get("1"));
            BufferedImage digitImage = ImageIO.read(boardComponents.get(String.valueOf(strength - 10)));
            oneImage = resize(oneImage, 12, 12);
            digitImage = resize(digitImage, 12,12);
            forceImage = overlay(forceImage, oneImage, new Point(28, 14), 1);
            forceImage = overlay(forceImage, digitImage, new Point(36, 14), 1);
        } else {
            BufferedImage numberImage = ImageIO.read(boardComponents.get(String.valueOf(strength)));
            numberImage = resize(numberImage, 12, 12);
            forceImage = overlay(forceImage, numberImage, new Point(30,14), 1);

        }
        return forceImage;
    }

    public BufferedImage overlay(BufferedImage board, BufferedImage piece, Point coordinates, float alpha) {

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

    public BufferedImage rotateImageByDegrees(BufferedImage img, double angle) {
        double rads = Math.toRadians(angle);
        double sin = Math.abs(Math.sin(rads)), cos = Math.abs(Math.cos(rads));
        int w = img.getWidth();
        int h = img.getHeight();
        int newWidth = (int) Math.floor(w * cos + h * sin);
        int newHeight = (int) Math.floor(h * cos + w * sin);

        BufferedImage rotated = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = rotated.createGraphics();
        AffineTransform at = new AffineTransform();
        at.translate((newWidth - w) / 2, (newHeight - h) / 2);

        int x = w / 2;
        int y = h / 2;

        at.rotate(rads, x, y);
        g2d.setTransform(at);
        g2d.drawImage(img, 0, 0, null);
        g2d.setColor(Color.RED);
        g2d.drawRect(0, 0, newWidth - 1, newHeight - 1);
        g2d.dispose();

        return rotated;
    }

    public static BufferedImage resize(BufferedImage img, int newW, int newH) {
        Image tmp = img.getScaledInstance(newW, newH, Image.SCALE_SMOOTH);
        BufferedImage dimg = new BufferedImage(newW, newH, BufferedImage.TYPE_INT_ARGB);

        Graphics2D g2d = dimg.createGraphics();
        g2d.drawImage(tmp, 0, 0, null);
        g2d.dispose();

        return dimg;
    }

    public void bribe(SlashCommandInteractionEvent event, DiscordGame discordGame, Game gameState) throws ChannelNotFoundException {
        Faction faction = gameState.getFaction(event.getOption("factionname").getAsString());
        Faction recipient = gameState.getFaction(event.getOption("recipient").getAsString());
        int amount = event.getOption("amount").getAsInt();

        if (faction.getSpice() < amount) {
            discordGame.getTextChannel("mod-info").sendMessage("Faction does not have enough spice to pay the bribe!").queue();
            return;
        }
        faction.subtractSpice(amount);
        spiceMessage(discordGame, amount, faction.getName(), "bribe to " + recipient.getEmoji(), false);
        recipient.addFrontOfShieldSpice(amount);
        writeFactionInfo(discordGame, faction);
        writeFactionInfo(discordGame, recipient);
        discordGame.pushGameState();
    }

    public void mute(DiscordGame discordGame, Game gameState) {
        gameState.setMute(!gameState.getMute());
        discordGame.pushGameState();
    }

    public void displayGameState(SlashCommandInteractionEvent event, DiscordGame discordGame, Game gameState) throws ChannelNotFoundException {
        TextChannel channel = discordGame.getTextChannel("mod-info");
        switch (event.getOption("data").getAsString()) {
            case "territories" -> {
               Map<String, Territory> territories = gameState.getTerritories();
               for (String territoryName : territories.keySet()) {
                   Territory territory = territories.get(territoryName);
                   if (territory.getSpice() == 0 && !territory.isStronghold() && territory.getForces().isEmpty()) continue;
                   discordGame.sendMessage(channel.getName(), "**" + territory.getTerritoryName() + "** \n" +
                           "Spice: " + territory.getSpice() + "\nForces: " + territory.getForces().toString());
               }
            }
            case "dnd" -> {
                discordGame.sendMessage("mod-info", gameState.getTreacheryDeck().toString());
                discordGame.sendMessage("mod-info", gameState.getTreacheryDiscard().toString());
                discordGame.sendMessage("mod-info", gameState.getSpiceDeck().toString());
                discordGame.sendMessage("mod-info", gameState.getSpiceDiscardA().toString());
                discordGame.sendMessage("mod-info", gameState.getSpiceDiscardB().toString());
                discordGame.sendMessage("mod-info", gameState.getLeaderSkillDeck().toString());
                discordGame.sendMessage("mod-info", gameState.getTraitorDeck().toString());
                discordGame.sendMessage("mod-info", gameState.getMarket().toString());
            }
            case "factions" -> {
                for (Faction faction: gameState.getFactions()) {
                    StringBuilder message = new StringBuilder();
                    message.append("**" + faction.getName()).append(":**\nPlayer: ").append(faction.getUserName()).append("\n");
                    message.append("spice: ").append(faction.getSpice()).append("\nTreachery Cards: ").append(faction.getTreacheryHand())
                            .append("\nTraitors:").append(faction.getTraitorHand()).append("\nLeaders: ").append(faction.getLeaders()).append("\n");
                    for (Resource resource : faction.getResources()) {
                        message.append(resource.getName()).append(": ").append(resource.getValue()).append("\n");
                    }
                    discordGame.sendMessage(channel.getName(), message.toString());
                }
            }
        }
        drawGameBoard(discordGame, gameState);
    }

    public void clean(SlashCommandInteractionEvent event) {
        if (!event.getOption("password").getAsString().equals(Dotenv.configure().load().get("PASSWORD"))) {
            event.getChannel().sendMessage("You have attempted the forbidden command.\n\n...Or you're Voiceofonecrying " +
                    "and you fat-fingered the password").queue();
            return;
        }
        List<Category> categories = event.getGuild().getCategories();
        for (Category category : categories) {
            //if (!category.getName().startsWith("test")) continue;
            category.delete().complete();
        }
        List<TextChannel> channels = event.getGuild().getTextChannels();
        for (TextChannel channel : channels) {
            if (//!channel.getName().startsWith("test") ||
            channel.getName().equals("general")) continue;
            channel.delete().complete();
        }
    }
}
