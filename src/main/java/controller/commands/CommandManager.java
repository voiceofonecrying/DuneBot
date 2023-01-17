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
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
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

import static controller.Initializers.getCSVFile;

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
                    case "moveforces" -> moveForces(event, discordGame, gameState);
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
                    case "advancegame" -> advanceGame(discordGame, gameState);
                    case "placehms" -> placeHMS(event, discordGame, gameState);
                    case "movehms" -> moveHMS(event, discordGame, gameState);
                    case "expansionchoices" -> expansionChoices(event, discordGame, gameState);
                    case "assigntechtoken" -> assignTechToken(event, discordGame, gameState);
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
                case "factionname", "sender", "recipient" -> event.replyChoices(CommandOptions.factions(gameState, searchValue)).queue();
                case "territory", "to" -> event.replyChoices(CommandOptions.territories(gameState, searchValue)).queue();
                case "traitor" -> event.replyChoices(CommandOptions.traitors(event, gameState, searchValue)).queue();
                case "card" -> event.replyChoices(CommandOptions.cardsInHand(event, gameState, searchValue)).queue();
                case "ixcard" -> event.replyChoices(CommandOptions.ixCardsInHand(event, gameState, searchValue)).queue();
                case "putbackcard" -> event.replyChoices(CommandOptions.cardsInMarket(event, gameState, searchValue)).queue();
                case "from" -> event.replyChoices(CommandOptions.fromTerritories(event, gameState, searchValue)).queue();
                case "bgterritories" -> event.replyChoices(CommandOptions.bgTerritories(gameState, searchValue)).queue();
                case "leadertokill" -> event.replyChoices(CommandOptions.leaders(event, gameState, searchValue)).queue();
                case "leadertorevive" -> event.replyChoices(CommandOptions.reviveLeaders(event, gameState, searchValue)).queue();
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
        OptionData allFactions = new OptionData(OptionType.STRING, "faction", "The faction", true)
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
        OptionData amount = new OptionData(OptionType.INTEGER, "amount", "Amount", true);
        OptionData message = new OptionData(OptionType.STRING, "message", "Message for spice transactions", false);
        OptionData password = new OptionData(OptionType.STRING, "password", "You really aren't allowed to run this command unless Voiceofonecrying lets you", true);
        OptionData deck = new OptionData(OptionType.STRING, "deck", "The deck", true)
                .addChoice("Treachery Deck", "treachery deck")
                .addChoice("Traitor Deck", "traitor deck");
        OptionData card = new OptionData(OptionType.STRING, "card", "The card.", true).setAutoComplete(true);
        OptionData ixCard = new OptionData(OptionType.STRING, "ixcard", "The card.", true).setAutoComplete(true);
        OptionData putBackCard = new OptionData(OptionType.STRING, "putbackcard", "The card.", true).setAutoComplete(true);
        OptionData recipient = new OptionData(OptionType.STRING, "recipient", "The recipient", true).setAutoComplete(true);
        OptionData bottom = new OptionData(OptionType.BOOLEAN, "bottom", "Place on bottom?", true);
        OptionData traitor = new OptionData(OptionType.STRING, "traitor", "The name of the traitor", true).setAutoComplete(true);
        OptionData territory = new OptionData(OptionType.STRING, "territory", "The name of the territory", true).setAutoComplete(true);
        OptionData sector = new OptionData(OptionType.INTEGER, "sector", "The storm sector", true);
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
        OptionData leader = new OptionData(OptionType.STRING, "leadertokill", "The leader.", true).setAutoComplete(true);
        OptionData reviveLeader = new OptionData(OptionType.STRING, "leadertorevive", "The leader.", true).setAutoComplete(true);
        OptionData fromTerritory = new OptionData(OptionType.STRING, "from", "the territory.", true).setAutoComplete(true);
        OptionData toTerritory = new OptionData(OptionType.STRING, "to", "Moving to this territory.", true).setAutoComplete(true);
        OptionData starredAmount = new OptionData(OptionType.INTEGER, "starredamount", "Starred amount", true);
        OptionData bgTerritories = new OptionData(OptionType.STRING, "bgterritories", "Territory to flip the BG force", true).setAutoComplete(true);
        OptionData techTokens = new OptionData(OptionType.BOOLEAN, "techtokens", "Include Tech Tokens?", true);
        OptionData sandTrout = new OptionData(OptionType.BOOLEAN, "sandtrout", "Include Sand Trout?", true);
        OptionData cheapHeroTraitor = new OptionData(OptionType.BOOLEAN, "cheapherotraitor", "Include Cheap Hero Traitor card?", true);
        OptionData expansionTreacheryCards = new OptionData(OptionType.BOOLEAN, "expansiontreacherycards", "Include expansion treachery cards?", true);
        OptionData leaderSkills = new OptionData(OptionType.BOOLEAN, "leaderskills", "Include Leader skills?", true);
        OptionData strongholdSkills = new OptionData(OptionType.BOOLEAN, "strongholdskills", "Include stronghold skills?", true);
        OptionData token = new OptionData(OptionType.STRING, "token", "The Tech Token", true)
                .addChoice("Heighliners", "Heighliners")
                .addChoice("Spice Production", "Spice Production")
                .addChoice("Axlotl Tanks", "Axlotl Tanks");


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
        commandData.add(Commands.slash("transfercard", "Move a card from one faction's hand to another").addOptions(faction, card, recipient));
        commandData.add(Commands.slash("putback", "Used for the Ixian ability to put a treachery card on the top or bottom of the deck.").addOptions(putBackCard, bottom));
        commandData.add(Commands.slash("advancegame", "Send the game to the next phase, turn, or card (in bidding round"));
        commandData.add(Commands.slash("ixhandselection", "Only use this command to select the Ix starting treachery card").addOptions(ixCard));
        commandData.add(Commands.slash("selecttraitor", "Select a starting traitor from hand.").addOptions(faction, traitor));
        commandData.add(Commands.slash("placeforces", "Place forces from reserves onto the surface").addOptions(faction, amount, isShipment, starred, territory));
        commandData.add(Commands.slash("moveforces", "Move forces from one territory to another").addOptions(faction, fromTerritory, toTerritory, amount, starredAmount));
        commandData.add(Commands.slash("removeforces", "Remove forces from the board.").addOptions(faction, amount, toTanks, starred, fromTerritory));
        commandData.add(Commands.slash("awardbid", "Designate that a card has been won by a faction during bidding phase.").addOptions(faction, spent));
        commandData.add(Commands.slash("reviveforces", "Revive forces for a faction.").addOptions(faction, revived, starred));
        commandData.add(Commands.slash("display", "Displays some element of the game to the mod.").addOptions(data));
        commandData.add(Commands.slash("setstorm", "Sets the storm to an initial sector.").addOptions(sector));
        commandData.add(Commands.slash("killleader", "Send a leader to the tanks.").addOptions(faction, leader));
        commandData.add(Commands.slash("reviveleader", "Revive a leader from the tanks.").addOptions(faction, reviveLeader));
        commandData.add(Commands.slash("bgflip", "Flip BG forces to advisor or fighter.").addOptions(bgTerritories));
        commandData.add(Commands.slash("mute", "Toggle mute for all bot messages."));
        commandData.add(Commands.slash("bribe", "Record a bribe transaction").addOptions(faction, recipient, amount));
        commandData.add(Commands.slash("expansionchoices", "Configure rules for a game before it starts.").addOptions(techTokens, sandTrout, cheapHeroTraitor, expansionTreacheryCards, leaderSkills, strongholdSkills));
        commandData.add(Commands.slash("placehms", "Starting position for Hidden Mobile Stronghold").addOptions(territory));
        commandData.add(Commands.slash("movehms", "Move Hidden Mobile Stronghold to another territory").addOptions(territory));
        commandData.add(Commands.slash("assigntechtoken", "Assign a Tech Token to a Faction (taking it away from previous owner)").addOptions(faction, token));

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
        if (gameState.getTurn() != 0) {
            modInfo.sendMessage("The game has already started, you can't add more factions!").queue();
            return;
        }
        if (gameState.getFactions().size() >= 6) {
            modInfo.sendMessage("This game is already full!").queue();
            return;
        }
        String factionName = event.getOption("faction").getAsString();
        if (gameState.hasFaction(factionName)) {
            modInfo.sendMessage("This faction has already been taken!").queue();
            return;
        }

        gameState.addFaction(new Faction(factionName, event.getOption("player").getAsUser().getAsMention(), event.getOption("player").getAsMember().getNickname(), gameState));

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

        if (resourceName.toLowerCase().equals("spice")) {
            gameState.getFaction(factionName).subtractSpice(amount);
            writeFactionInfo(discordGame, gameState.getFaction(factionName));
            discordGame.pushGameState();
            return;
        }

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

    public String drawSpiceCard(Game gameState, boolean discardA) {
        LinkedList<SpiceCard> deck = gameState.getSpiceDeck();
        LinkedList<SpiceCard> discard = discardA ? gameState.getSpiceDiscardA() : gameState.getSpiceDiscardB();
        StringBuilder message = new StringBuilder();
        if (deck.isEmpty()) {
            deck.addAll(gameState.getSpiceDiscardA());
            deck.addAll(gameState.getSpiceDiscardB());
            Collections.shuffle(deck);
            gameState.getSpiceDiscardA().clear();
            gameState.getSpiceDiscardB().clear();
        }
        SpiceCard drawn = deck.pop();
        if (gameState.getTurn() == 1 && drawn.name().equals("Shai-Hulud")) {
            deck.add(drawn);
            Collections.shuffle(deck);
            drawSpiceCard(gameState, discardA);
        }
        discard.add(drawn);
        message.append(drawn.name());
        if (gameState.getStorm() == drawn.sector()) message.append(" (blown away by the storm!)");
        else if (drawn.name().equals("Shai-Hulud")) message.append(", ").append(drawSpiceCard(gameState, discardA));
        else gameState.getTerritories().get(drawn.name()).addSpice(drawn.spice());
        return message.toString();
    }

    public void drawCard(SlashCommandInteractionEvent event, DiscordGame discordGame, Game gameState) throws ChannelNotFoundException {
        Faction faction = gameState.getFaction(event.getOption("factionname").getAsString());
        faction.addTreacheryCard(gameState.getTreacheryDeck().pop());
        discordGame.pushGameState();
    }

    public void drawCard(Game gameState, String deckName, String faction) {
        switch (deckName) {
            case "traitor deck" -> gameState.getFaction(faction).getTraitorHand().add(gameState.getTraitorDeck().pollLast());
            case "treachery deck" -> gameState.getFaction(faction).getTreacheryHand().add(gameState.getTreacheryDeck().pollLast());
        }
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
        writeFactionInfo(discordGame, faction);
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

    public void putBack(SlashCommandInteractionEvent event, DiscordGame discordGame, Game gameState) throws ChannelNotFoundException {
        LinkedList<TreacheryCard> market = gameState.getMarket();
        int i = 0;
        boolean found = false;
        for (; i < market.size(); i++) {
            if (market.get(i).name().contains(event.getOption("putbackcard").getAsString())) {
                if (!event.getOption("bottom").getAsBoolean()) gameState.getTreacheryDeck().addLast(market.get(i));
                else gameState.getTreacheryDeck().addFirst(market.get(i));
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
        if (gameState.hasFaction("Atreides")) {
            discordGame.sendMessage("atreides-chat","The first card up for bid is <:treachery:991763073281040518> " + gameState.getMarket().peek().name() + " <:treachery:991763073281040518>");
        }
        discordGame.pushGameState();
    }

    public void ixHandSelection(SlashCommandInteractionEvent event, DiscordGame discordGame, Game gameState) throws ChannelNotFoundException {
        List<TreacheryCard> hand = gameState.getFaction("Ix").getTreacheryHand();
        Collections.shuffle(hand);
        TreacheryCard card = hand.stream().filter(treacheryCard -> treacheryCard.name().equals(event.getOption("ixcard").getAsString())).findFirst().orElseThrow();
        for (TreacheryCard treacheryCard : hand) {
            if (treacheryCard.equals(card)) continue;
            gameState.getTreacheryDeck().add(treacheryCard);
        }
        hand.removeIf(treacheryCard -> !treacheryCard.equals(card));
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
                int cardNumber = gameState.getMarketSize() - market.size();
                message.append("R").append(gameState.getTurn()).append(":C").append(cardNumber + 1).append("\n");
                int firstBid = Math.ceilDiv(gameState.getStormMovement(), 3) + cardNumber;
                for (int i = 0; i < gameState.getFactions().size(); i++) {
                    int playerPosition = (firstBid + i + 1) % gameState.getFactions().size();
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
            drawCard(gameState, "treachery deck", "Harkonnen");
            discordGame.sendMessage("turn-summary", winner.getEmoji() + " draws another card from the <:treachery:991763073281040518> deck.");
        }
        winner.subtractSpice(spent);
        spiceMessage(discordGame, spent, winner.getName(), "R" +
                gameState.getTurn() + ":C" + (gameState.getMarketSize() - gameState.getMarket().size()), false);
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
        gameState.getLeaderTanks().add(faction.removeLeader(event.getOption("leadertokill").getAsString()));
        discordGame.pushGameState();
    }

    public void reviveLeader(SlashCommandInteractionEvent event, DiscordGame discordGame, Game gameState) throws ChannelNotFoundException {
        Faction faction = gameState.getFaction(event.getOption("factionname").getAsString());
        faction.getLeaders().add(gameState.removeLeaderFromTanks(event.getOption("leadertorevive").getAsString()));
        discordGame.pushGameState();
    }

    public void revival(SlashCommandInteractionEvent event, DiscordGame discordGame, Game gameState) throws ChannelNotFoundException {
        String star = event.getOption("starred").getAsBoolean() ? "*" : "";
        Faction faction = gameState.getFaction(event.getOption("factionname").getAsString());
        if (star.equals("")) faction.getReserves().addStrength(event.getOption("revived").getAsInt());
        else faction.getSpecialReserves().addStrength(event.getOption("revived").getAsInt());
        faction.subtractSpice(2 * event.getOption("revived").getAsInt());
        spiceMessage(discordGame, 2 * event.getOption("revived").getAsInt(), faction.getName(), "Revivals", false);
        if (gameState.hasFaction("BT")) {
            gameState.getFaction("BT").addSpice(2 * event.getOption("revived").getAsInt());
            spiceMessage(discordGame, 2 * event.getOption("revived").getAsInt(), "bt", faction.getEmoji() + " revivals", true);
            writeFactionInfo(discordGame, gameState.getFaction("BT"));
        }
        writeFactionInfo(discordGame, faction);
        discordGame.pushGameState();
    }

    public void advanceGame(DiscordGame discordGame, Game gameState) throws ChannelNotFoundException {
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
                    drawGameBoard(discordGame, gameState);
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
                    drawCard(gameState, "traitor deck", "BT");
                    drawCard(gameState, "traitor deck", "BT");
                    drawCard(gameState, "traitor deck", "BT");
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
                            drawCard(gameState, "treachery deck", "Ix");
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
                           for (Territory territory : territories.values()) {
                               if (territory.isRock() || territory.getSector() != gameState.getStorm()) continue;
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
                               List<Force> toRemove = new LinkedList<>();
                               for (Force force : forces) {
                                   if (force.getName().contains("Fremen") && fremenSpecialCase) continue;
                                   int lost = force.getStrength();
                                   if (force.getName().contains("Fremen") && lost > 1) {
                                       lost /= 2;
                                       force.setStrength(lost);
                                       forces.add(force);
                                   } else toRemove.add(force);
                                   gameState.getTanks().stream().filter(force1 -> force1.getName().equals(force.getName())).findFirst().orElseThrow().addStrength(force.getStrength());
                                   discordGame.sendMessage("turn-summary",
                                           gameState.getFaction(force.getName().replace("*", "")).getEmoji() + " lost " +
                                                   lost + " forces to the storm in " + territory.getTerritoryName());
                               }
                               forces.removeAll(toRemove);
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
                    discordGame.sendMessage("turn-summary", "A: " + drawSpiceCard(gameState, true));
                    discordGame.sendMessage("turn-summary", "B: " + drawSpiceCard(gameState, false));
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
                        gameState.getFaction("CHOAM").addSpice((2 * factions.size() * multiplier) - choamGiven);
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
                        if (faction.getName().equals("Rich")) cardsUpForBid--;
                    }
                    if (gameState.hasFaction("Ix")) {
                        discordGame.sendMessage("ix-chat", "Please select a card to put back to top or bottom.");
                    }
                    countMessage.append("There will be ").append(cardsUpForBid).append(" <:treachery:991763073281040518> cards up for bid this round.");
                    discordGame.sendMessage("turn-summary", countMessage.toString());
                    gameState.setMarketSize(cardsUpForBid);
                    if (gameState.hasFaction("Ix")) {
                        cardsUpForBid++;
                        discordGame.sendMessage("turn-summary", gameState.getFaction("Ix").getEmoji() + " to place a card back on the top or bottom of the deck.");
                    }
                    for (int i = 0; i < cardsUpForBid; i++) {
                        gameState.getMarket().add(gameState.getTreacheryDeck().pop());
                        if (gameState.hasFaction("Ix")) {
                            discordGame.sendMessage("ix-chat", "<:treachery:991763073281040518> " +
                                    gameState.getMarket().get(i).name() + " <:treachery:991763073281040518>");
                        }
                    }
                    if (gameState.hasFaction("Atreides") && !gameState.hasFaction("Ix")) {
                        discordGame.sendMessage("atreides-chat","The first card up for bid is <:treachery:991763073281040518> " + gameState.getMarket().peek().name() + " <:treachery:991763073281040518>");
                    }
                    StringBuilder message = new StringBuilder();
                    message.append("R").append(gameState.getTurn()).append(":C1\n");
                    int firstBid = Math.ceilDiv(gameState.getStorm(), 3);
                    for (int i = 0; i < factions.size(); i++) {
                        int playerPosition = firstBid + i > factions.size() - 1 ? firstBid + i - factions.size() : firstBid + i;
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
                            if (gameState.getForceFromTanks(faction.getName()).getStrength() == 0
                                    && gameState.getForceFromTanks(faction.getName() + "*").getStrength() == 0) continue;
                            revived++;
                            if (gameState.getForceFromTanks(faction.getName() + "*").getStrength() > 0 && !revivedStar) {
                                Force force = gameState.getForceFromTanks(faction.getName() + "*");
                                force.setStrength(force.getStrength() - 1);
                                revivedStar = true;
                                faction.getSpecialReserves().setStrength(faction.getSpecialReserves().getStrength() + 1);
                            } else if (gameState.getForceFromTanks(faction.getName()).getStrength() > 0) {
                                Force force = gameState.getForceFromTanks(faction.getName());
                                force.setStrength(force.getStrength() - 1);
                                faction.getReserves().setStrength(faction.getReserves().getStrength() + 1);
                            }
                        }
                        if (revived > 0) {
                            message.append(gameState.getFaction(faction.getName()).getEmoji()).append(": ").append(revived).append("\n");
                        }
                    }
                    discordGame.sendMessage("turn-summary", message.toString());
                    gameState.advancePhase();
                }
                //6. Shipment and Movement
                case 6 -> {
                    discordGame.sendMessage("turn-summary","Turn " + gameState.getTurn() + " Shipment and Movement Phase:");
                    if (gameState.hasFaction("Atreides")) {
                        discordGame.sendMessage("atreides-info", "You see visions of " + gameState.getSpiceDeck().peek().name() + " in your future.");
                    }
                    if(gameState.hasFaction("BG")) {
                       for (Territory territory : gameState.getTerritories().values()) {
                           if (territory.getForce("Advisor").getStrength() > 0) discordGame.sendMessage("turn-summary",gameState
                                   .getFaction("BG").getEmoji() + " to decide whether to flip their advisors in " + territory.getTerritoryName());
                       }
                    }
                    gameState.advancePhase();
                }
                //TODO: 7. Battle
                case 7 -> {
                    discordGame.sendMessage("turn-summary","Turn " + gameState.getTurn() + " Battle Phase:");
                    StringBuilder battleMessage = new StringBuilder();
                    for (Territory territory : gameState.getTerritories().values()) {
                        if (territory.getForces().size() < 2) continue;
                        int fightingFactions = territory.getForces().size();
                        if (territory.getForce("Advisor").getStrength() > 0) fightingFactions -= 1;
                        for (Force force : territory.getForces()) {
                            for (Force otherForce : territory.getForces()) {
                                if (force.equals(otherForce)) continue;
                                if (force.getName().equals(otherForce.getName().replace("*", ""))) fightingFactions -= 1;
                            }
                        }
                        if (fightingFactions < 2) continue;
                        if (battleMessage.isEmpty()) battleMessage.append("The following battles will take place this turn:\n");
                        battleMessage.append("In ").append(territory.getTerritoryName()).append(": ");
                        for (Force force : territory.getForces()) {
                            if (force.getName().contains("*") || force.getName().equals("Advisor")) continue;
                            battleMessage.append(gameState.getFaction(force.getName()).getEmoji());
                        }
                    }

                    if (battleMessage.isEmpty()) discordGame.sendMessage("turn-summary", "There are no battles this turn.");
                    else discordGame.sendMessage("turn-summary", battleMessage.toString());

                    gameState.advancePhase();

                }
                //TODO: 8. Spice Harvest
                case 8 -> {
                    discordGame.sendMessage("turn-summary", "Turn " + gameState.getTurn() + " Spice Harvest Phase:");
                   Map<String, Territory> territories = gameState.getTerritories();
                   for (Territory territory : territories.values()) {
                       if (territory.getForces().size() != 1) continue;
                       if (territory.getForces().get(0).getName().equals("Advisor")) {
                           int strength = territory.getForces().get(0).getStrength();
                           territory.getForces().clear();
                           territory.getForces().add(new Force("BG", strength));
                           discordGame.sendMessage("turn-summary", "Advisors are alone in " + territory.getTerritoryName() + " and have flipped to fighters.");
                       }
                   }

                   for (Faction faction : gameState.getFactions()) {
                       faction.setHasMiningEquipment(false);
                       if (territories.get("Arrakeen").getForces().stream().anyMatch(force -> force.getName().contains(faction.getName()))) {
                           faction.addSpice(2);
                           discordGame.sendMessage("turn-summary", gameState.getFaction(faction.getName()).getEmoji() + " collects 2 <:spice4:991763531798167573> from Arrakeen");
                           faction.setHasMiningEquipment(true);
                       }
                       if (territories.get("Carthag").getForces().stream().anyMatch(force -> force.getName().contains(faction.getName()))) {
                           faction.addSpice(2);
                           discordGame.sendMessage("turn-summary", gameState.getFaction(faction.getName()).getEmoji() + " collects 2 <:spice4:991763531798167573> from Carthag");
                           faction.setHasMiningEquipment(true);
                       }
                       if (territories.get("Tuek's Sietch").getForces().stream().anyMatch(force -> force.getName().contains(faction.getName()))) {
                           discordGame.sendMessage("turn-summary", gameState.getFaction(faction.getName()).getEmoji() + " collects 1 <:spice4:991763531798167573> from Tuek's Sietch");
                           faction.addSpice(1);
                       }
                   }

                    for (Territory territory: territories.values()) {
                        if (territory.getSpice() == 0 || territory.getForces().size() == 0) continue;
                        int totalStrength = 0;
                        Faction faction = gameState.getFaction(territory.getForces().stream().filter(force -> !force.getName().equals("Advisor")).findFirst().orElseThrow().getName().replace("*", ""));
                        for (Force force : territory.getForces()) {
                            if (force.getName().equals("Advisor")) continue;
                            totalStrength += force.getStrength();
                        }
                        int multiplier = faction.hasMiningEquipment() ? 3 : 2;
                        int spice = Math.min(multiplier * totalStrength, territory.getSpice());
                        faction.addSpice(spice);
                        territory.setSpice(territory.getSpice() - spice);
                        discordGame.sendMessage("turn-summary", gameState.getFaction(faction.getName()).getEmoji() + " collects " + spice + " <:spice4:991763531798167573> from " + territory.getTerritoryName());
                    }
                    gameState.advancePhase();
                }
                //TODO: 9. Mentat Pause
                case 9 -> {
                    discordGame.sendMessage("turn-summary", "Turn " + gameState.getTurn() + " Mentat Pause Phase:");
                    for (Faction faction : gameState.getFactions()) {
                        if (faction.getFrontOfShieldSpice() > 0) {
                            discordGame.sendMessage("turn-summary", faction.getEmoji() + " collects " +
                                    faction.getFrontOfShieldSpice() + " <:spice4:991763531798167573> from front of shield.");
                            spiceMessage(discordGame,  faction.getFrontOfShieldSpice(), faction.getName(), "front of shield", true);
                            faction.addSpice(faction.getFrontOfShieldSpice());
                            faction.setFrontOfShieldSpice(0);
                        }
                        writeFactionInfo(discordGame, faction);
                    }
                    gameState.advanceTurn();
                }
            }
            discordGame.pushGameState();
            drawGameBoard(discordGame, gameState);
        }
    }

    public void selectTraitor(SlashCommandInteractionEvent event, DiscordGame discordGame, Game gameState) throws ChannelNotFoundException {
        Faction faction = gameState.getFaction(event.getOption("factionname").getAsString());
        TraitorCard traitor = faction.getTraitorHand().stream().filter(traitorCard -> traitorCard.name().toLowerCase()
                .contains(event.getOption("traitor").getAsString().toLowerCase())).findFirst().orElseThrow();
        for (TraitorCard card : faction.getTraitorHand()) {
            if (!card.equals(traitor)) gameState.getTraitorDeck().add(card);
        }
        faction.getTraitorHand().clear();
        faction.getTraitorHand().add(traitor);
        writeFactionInfo(discordGame, faction);
        discordGame.pushGameState();
    }

    public void placeForces(SlashCommandInteractionEvent event, DiscordGame discordGame, Game gameState) throws ChannelNotFoundException {
        boolean star = event.getOption("starred").getAsBoolean();
        Territory territory = gameState.getTerritories().get(event.getOption("territory").getAsString());
        Faction faction = gameState.getFaction(event.getOption("factionname").getAsString());
        int amount = event.getOption("amount").getAsInt();

        Force reserves = star ? faction.getSpecialReserves() : faction.getReserves();
        if (reserves.getStrength() < amount) {
            discordGame.sendMessage("mod-info", "This faction does not have enough forces in reserves!");
            return;
        }
        reserves.setStrength(reserves.getStrength() - amount);

        Force force = territory.getForce(reserves.getName());
        if (force.getStrength() == 0) territory.getForces().add(force);
        force.addStrength(amount);

        if (event.getOption("isshipment").getAsBoolean()) {
            int cost = territory.isStronghold() ? 1 : 2;
            cost *= faction.getName().equals("Guild") ? Math.ceilDiv(amount, 2) : amount;
            faction.subtractSpice(cost);
            spiceMessage(discordGame, cost, faction.getName(), "shipment to " + territory.getTerritoryName(), false);
            if (gameState.hasFaction("Guild") && !(faction.getName().equals("Guild") || faction.getName().equals("Fremen"))) {
                gameState.getFaction("Guild").addSpice(cost);
                spiceMessage(discordGame, cost, "guild", faction.getEmoji() + " shipment", true);
                writeFactionInfo(discordGame, gameState.getFaction("Guild"));
            }
            writeFactionInfo(discordGame, faction);
        }
        if (territory.getForce("BG").getStrength() > 0) {
            discordGame.sendMessage("turn-summary", gameState.getFaction("BG").getEmoji() + " to decide whether to flip their forces in " + territory.getTerritoryName());
        }
        discordGame.pushGameState();
        drawGameBoard(discordGame, gameState);
    }

    public void moveForces(SlashCommandInteractionEvent event, DiscordGame discordGame, Game gameState) throws ChannelNotFoundException {
        Faction faction = gameState.getFaction(event.getOption("factionname").getAsString());
        Territory from = gameState.getTerritories().get(event.getOption("from").getAsString());
        Territory to = gameState.getTerritories().get(event.getOption("to").getAsString());
        int amount = event.getOption("amount").getAsInt();
        int starredAmount = event.getOption("starredamount").getAsInt();

        from.setForceStrength(faction.getName(), from.getForce(faction.getName()).getStrength() - amount);
        from.setForceStrength(faction.getName() + "*", from.getForce(faction.getName() + "*").getStrength() - starredAmount);

        to.setForceStrength(faction.getName(), to.getForce(faction.getName()).getStrength() + amount);
        to.setForceStrength(faction.getName() + "*", to.getForce(faction.getName() + "*").getStrength() + starredAmount);

        drawGameBoard(discordGame, gameState);
        discordGame.pushGameState();
    }

    public void removeForces(SlashCommandInteractionEvent event, DiscordGame discordGame, Game gameState) throws ChannelNotFoundException {
        Territory territory = gameState.getTerritories().get(event.getOption("from").getAsString());
        Faction faction = gameState.getFaction(event.getOption("factionname").getAsString());
        int amount = event.getOption("amount").getAsInt();
        String starred = event.getOption("starred").getAsBoolean() ? "*" : "";
        Force force = territory.getForce(faction.getName() + starred);
        if (force.getStrength() > amount) {
            force.setStrength(force.getStrength() - amount);
        } else if (force.getStrength() < amount) {
            discordGame.sendMessage("mod-info","You are trying to remove more forces than this faction has in this territory! Please check your info and try again.");
            return;
        } else {
            territory.getForces().remove(force);
        }

        if (event.getOption("totanks").getAsBoolean()) {
            gameState.getForceFromTanks(faction.getName()).addStrength(amount);
        } else {
            if (starred.equals("*")) faction.getSpecialReserves().addStrength(amount);
            else faction.getReserves().addStrength(amount);
        }
        discordGame.pushGameState();
        if (event.getOption("totanks").getAsBoolean()) drawGameBoard(discordGame, gameState);
    }

    public void setStorm(SlashCommandInteractionEvent event, DiscordGame discordGame, Game gameState) throws ChannelNotFoundException {
        gameState.setStorm(event.getOption("sector").getAsInt());
        discordGame.sendMessage("turn-summary","The storm has been initialized to sector " + event.getOption("sector").getAsInt());
        if (gameState.hasTechTokens()) {
            List<TechToken> techTokens = new LinkedList<>();
            if (gameState.hasFaction("BT")) {
                gameState.getFaction("BT").getTechTokens().add(new TechToken("Axlotl Tanks"));
            } else techTokens.add(new TechToken("Axlotl Tanks"));
            if (gameState.hasFaction("Ix")) {
                gameState.getFaction("Ix").getTechTokens().add(new TechToken("Heighliners"));
            } else techTokens.add(new TechToken("Heighliners"));
            if (gameState.hasFaction("Fremen")) {
                gameState.getFaction("Fremen").getTechTokens().add(new TechToken("Spice Production"));
            } else techTokens.add(new TechToken("Spice Production"));
            if (!techTokens.isEmpty()) {
                Collections.shuffle(techTokens);
                for (int i = 0; i < techTokens.size(); i++) {
                    Faction faction = gameState.getFactions().get((Math.ceilDiv(gameState.getStorm(), 3) - 1 + i) % 6);
                    faction.getTechTokens().add(techTokens.get(i));
                }
            }
        }
        discordGame.pushGameState();
        drawGameBoard(discordGame, gameState);
    }

    public void writeFactionInfo(DiscordGame discordGame, Faction faction) throws ChannelNotFoundException {

        String emoji = faction.getEmoji();
        List<TraitorCard> traitors = faction.getTraitorHand();
        StringBuilder traitorString = new StringBuilder();
        if (faction.getName().equals("BT")) traitorString.append("\n__Face Dancers:__\n");
        else traitorString.append("\n__Traitors:__\n");
        for (TraitorCard traitor : traitors) {
            if (traitor.name().equals("Cheap Hero")) {
                traitorString.append("Cheap Hero (0)\n");
                continue;
            }
            String traitorEmoji = discordGame.getGameState().getFaction(traitor.factionName()).getEmoji();
            traitorString.append(traitorEmoji).append(" ").append(traitor.name()).append("(").append(traitor.strength()).append(")");
            traitorString.append("\n");
        }
        for (TextChannel channel : discordGame.getTextChannels()) {
            if (channel.getName().equals(faction.getName().toLowerCase() + "-info")) {
                discordGame.sendMessage(channel.getName(), emoji + "**Faction Info**" + emoji + "\n__Spice:__ " +
                        faction.getSpice() +
                        traitorString);

                for (TreacheryCard treachery : faction.getTreacheryHand()) {
                    discordGame.sendMessage(channel.getName(), "<:treachery:991763073281040518> " + treachery.name() + " <:treachery:991763073281040518>");
                }
            }
        }

    }

    public void expansionChoices(SlashCommandInteractionEvent event, DiscordGame discordGame, Game gameState) {

        if (event.getOption("techtokens").getAsBoolean()) {
            gameState.setTechTokens(true);
        }

        if (event.getOption("sandtrout").getAsBoolean()) {
            gameState.getSpiceDeck().add(new SpiceCard("Sandtrout", -1, 0));
        }

        if (event.getOption("cheapherotraitor").getAsBoolean()) {
            gameState.getTraitorDeck().add(new TraitorCard("Cheap Hero", "Any", 0));
        }

        if (event.getOption("expansiontreacherycards").getAsBoolean()) {
            CSVParser csvParser = getCSVFile("ExpansionTreacheryCards.csv");
            for (CSVRecord csvRecord : csvParser) {
                gameState.getTreacheryDeck().add(new TreacheryCard(csvRecord.get(0), csvRecord.get(1)));
            }
        }

        if (event.getOption("leaderskills").getAsBoolean()) {
            gameState.setLeaderSkills(true);
        }

        if (event.getOption("strongholdskills").getAsBoolean()) {
            gameState.setStrongholdSkills(true);
        }

        discordGame.pushGameState();
    }

    public void bgFlip(SlashCommandInteractionEvent event, DiscordGame discordGame, Game gameState) throws ChannelNotFoundException {
        Territory territory = gameState.getTerritories().get(event.getOption("bgterritories").getAsString());
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

    public void placeHMS(SlashCommandInteractionEvent event, DiscordGame discordGame, Game gameState) throws ChannelNotFoundException {
        Territory territory = gameState.getTerritories().get(event.getOption("territory").getAsString());
        territory.getForces().add(new Force("Hidden Mobile Stronghold", 1));
        discordGame.pushGameState();
        drawGameBoard(discordGame, gameState);
    }

    public void moveHMS(SlashCommandInteractionEvent event, DiscordGame discordGame, Game gameState) throws ChannelNotFoundException {
        for (Territory territory : gameState.getTerritories().values()) {
            territory.getForces().removeIf(force -> force.getName().equals("Hidden Mobile Stronghold"));
        }
        placeHMS(event, discordGame, gameState);
    }

    public void assignTechToken(SlashCommandInteractionEvent event, DiscordGame discordGame, Game gameState) throws ChannelNotFoundException {
        for (Faction faction : gameState.getFactions()) {
            faction.getTechTokens().removeIf(techToken -> techToken.getName().equals(event.getOption("token").getAsString()));
        }
        gameState.getFaction(event.getOption("factionname").getAsString()).getTechTokens().add(new TechToken(event.getOption("token").getAsString()));
        discordGame.sendMessage("turn-summary", event.getOption("token").getAsString() + " has been transferred to " + gameState.getFaction(event.getOption("factionname").getAsString()).getEmoji());
        drawGameBoard(discordGame, gameState);
        discordGame.pushGameState();
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

            //Place Tech Tokens
            for (int i = 0; i < gameState.getFactions().size(); i++) {
                Faction faction = gameState.getFactions().get(i);
                if (faction.getTechTokens().isEmpty()) continue;
                int offset = 0;
                for (TechToken token : faction.getTechTokens()) {
                    BufferedImage tokenImage = ImageIO.read(boardComponents.get(token.getName()));
                    tokenImage = resize(tokenImage, 50, 50);
                    Point coordinates = Initializers.getDrawCoordinates("tech token " + i);
                    Point coordinatesOffset = new Point(coordinates.x + offset, coordinates.y);
                    board = overlay(board, tokenImage, coordinatesOffset, 1);
                    offset += 50;
                }
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

            //Place sigils
            for (int i = 1; i <= gameState.getFactions().size(); i++) {
                BufferedImage sigil = ImageIO.read(boardComponents.get(gameState.getFactions().get(i - 1).getName() + " Sigil"));
                coordinates = Initializers.getDrawCoordinates("sigil " + i);
                sigil = resize(sigil, 50, 50);
                board = overlay(board, sigil, coordinates, 1);
            }

            //Place forces
            for (Territory territory : gameState.getTerritories().values()) {
                if (territory.getForces().size() == 0 && territory.getSpice() == 0) continue;
                if (territory.getTerritoryName().equals("Hidden Mobile Stronghold")) continue;
                int offset = 0;
                int i = 0;

                if (territory.getSpice() != 0) {
                    i = 1;
                    int spice = territory.getSpice();
                    while (spice != 0) {
                        if (spice >= 10) {
                            BufferedImage spiceImage = ImageIO.read(boardComponents.get("10 Spice"));
                            spiceImage = resize(spiceImage, 25,25);
                            Point spicePlacement = Initializers.getPoints(territory.getTerritoryName()).get(0);
                            Point spicePlacementOffset = new Point(spicePlacement.x + offset, spicePlacement.y - offset);
                            board = overlay(board, spiceImage, spicePlacementOffset, 1);
                            spice -= 10;
                        } else if (spice >= 8) {
                            BufferedImage spiceImage = ImageIO.read(boardComponents.get("8 Spice"));
                            spiceImage = resize(spiceImage, 25,25);
                            Point spicePlacement = Initializers.getPoints(territory.getTerritoryName()).get(0);
                            Point spicePlacementOffset = new Point(spicePlacement.x + offset, spicePlacement.y - offset);
                            board = overlay(board, spiceImage, spicePlacementOffset, 1);
                            spice -= 8;
                        } else if (spice >= 6) {
                            BufferedImage spiceImage = ImageIO.read(boardComponents.get("6 Spice"));
                            spiceImage = resize(spiceImage, 25,25);
                            Point spicePlacement = Initializers.getPoints(territory.getTerritoryName()).get(0);
                            Point spicePlacementOffset = new Point(spicePlacement.x + offset, spicePlacement.y - offset);
                            board = overlay(board, spiceImage, spicePlacementOffset, 1);
                            spice -= 6;
                        } else if (spice >= 5) {
                            BufferedImage spiceImage = ImageIO.read(boardComponents.get("5 Spice"));
                            spiceImage = resize(spiceImage, 25,25);
                            Point spicePlacement = Initializers.getPoints(territory.getTerritoryName()).get(0);
                            Point spicePlacementOffset = new Point(spicePlacement.x + offset, spicePlacement.y - offset);
                            board = overlay(board, spiceImage, spicePlacementOffset, 1);
                            spice -= 5;
                        } else if (spice >= 2) {
                            BufferedImage spiceImage = ImageIO.read(boardComponents.get("2 Spice"));
                            spiceImage = resize(spiceImage, 25,25);
                            Point spicePlacement = Initializers.getPoints(territory.getTerritoryName()).get(0);
                            Point spicePlacementOffset = new Point(spicePlacement.x + offset, spicePlacement.y - offset);
                            board = overlay(board, spiceImage, spicePlacementOffset, 1);
                            spice -= 2;
                        } else {
                            BufferedImage spiceImage = ImageIO.read(boardComponents.get("1 Spice"));
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
                        BufferedImage hms = ImageIO.read(boardComponents.get("Hidden Mobile Stronghold"));
                        hms = resize(hms, 150,100);
                        List<Force> hmsForces = gameState.getTerritories().get("Hidden Mobile Stronghold").getForces();
                        int forceOffset = 0;
                        for (Force f : hmsForces) {
                            BufferedImage forceImage = buildForceImage(boardComponents, f.getName(), f.getStrength());
                            hms = overlay(hms, forceImage, new Point(40,20 + forceOffset), 1);
                            forceOffset += 30;
                        }
                        Point forcePlacement = Initializers.getPoints(territory.getTerritoryName()).get(i);
                        Point forcePlacementOffset = new Point(forcePlacement.x - 55, forcePlacement.y + offset + 5);
                        board = overlay(board, hms, forcePlacementOffset, 1);
                        continue;
                    }
                    BufferedImage forceImage = buildForceImage(boardComponents, force.getName(), force.getStrength());
                    Point forcePlacement = Initializers.getPoints(territory.getTerritoryName()).get(i);
                    Point forcePlacementOffset = new Point(forcePlacement.x, forcePlacement.y + offset);
                    board = overlay(board, forceImage, forcePlacementOffset, 1);
                    i++;
                    if (i == Initializers.getPoints(territory.getTerritoryName()).size()) {
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
               for (Territory territory: territories.values()) {
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
                    message.append("**").append(faction.getName()).append(":**\nPlayer: ").append(faction.getUserName()).append("\n");
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
