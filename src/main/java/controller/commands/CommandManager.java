package controller.commands;

import constants.Emojis;
import enums.GameOption;
import exceptions.ChannelNotFoundException;
import exceptions.InvalidOptionException;
import exceptions.InvalidGameStateException;
import model.*;
import model.factions.Faction;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.channel.concrete.Category;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.guild.GuildReadyEvent;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import org.jetbrains.annotations.NotNull;
import templates.ChannelPermissions;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.*;
import java.util.stream.Collectors;

import static controller.commands.CommandOptions.*;
import static controller.commands.ShowCommands.refreshChangedInfo;

public class CommandManager extends ListenerAdapter {

    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        String name = event.getName();
        event.deferReply(true).queue();
        Member member = event.getMember();

        List<Role> roles = member == null ? new ArrayList<>() : member.getRoles();

        try {
            String ephemeralMessage = "";
            if (name.equals("newgame") && roles.stream().anyMatch(role -> role.getName().equals("Game Master"))) newGame(event);
            //else if (name.equals("clean")) clean(event); Leaving this command commented so that the command is ignored in production
            else {
                DiscordGame discordGame = new DiscordGame(event);
                Game game = discordGame.getGame();

                if (roles.stream().noneMatch(role ->
                        role.getName().equals(game.getModRole()) || (role.getName().equals(game.getGameRole()))
                && name.startsWith("player"))
                ) {
                    event.getHook().editOriginal("You do not have permission to use this command.").queue();
                    return;
                }

                switch (name) {
                    case "show" -> ShowCommands.runCommand(event, discordGame, game);
                    case "setup" -> SetupCommands.runCommand(event, discordGame, game);
                    case "run" -> RunCommands.runCommand(event, discordGame, game);
                    case "richese" -> RicheseCommands.runCommand(event, discordGame, game);
                    case "bt" -> BTCommands.runCommand(event, discordGame, game);
                    case "hark" -> HarkCommands.runCommand(event, discordGame, game);
                    case "choam" -> ChoamCommands.runCommand(event, discordGame, game);
                    case "player" -> ephemeralMessage = PlayerCommands.runCommand(event, discordGame, game);
                    case "draw" -> drawCard(discordGame, game);
                    case "discard" -> discard(discordGame, game);
                    case "transfercard" -> transferCard(discordGame, game);
                    case "putback" -> putBack(discordGame, game);
                    case "placeforces" -> placeForces(discordGame, game);
                    case "moveforces" -> moveForces(discordGame, game);
                    case "removeforces" -> removeForces(discordGame, game);
                    case "display" -> displayGameState(discordGame, game);
                    case "reviveforces" -> revival(discordGame, game);
                    case "awardbid" -> awardBid(event, discordGame, game);
                    case "killleader" -> killLeader(discordGame, game);
                    case "reviveleader" -> reviveLeader(discordGame, game);
                    case "setstorm" -> setStorm(discordGame, game);
                    case "bgflip" -> bgFlip(discordGame, game);
                    case "bribe" -> bribe(discordGame, game);
                    case "mute" -> mute(discordGame, game);
                    case "placehms" -> placeHMS(discordGame, game);
                    case "movehms" -> moveHMS(discordGame, game);
                    case "assigntechtoken" -> assignTechToken(discordGame, game);
                    case "draw-spice-blow" -> drawSpiceBlow(discordGame, game);
                    case "create-alliance" -> createAlliance(discordGame, game);
                    case "remove-alliance" -> removeAlliance(discordGame, game);
                    case "set-spice-in-territory" -> setSpiceInTerritory(discordGame, game);
                    case "destroy-shield-wall" -> destroyShieldWall(discordGame, game);
                    case "weather-control-storm" -> weatherControlStorm(discordGame, game);
                    case "add-spice" -> addSpice(discordGame, game);
                    case "remove-spice" -> removeSpice(discordGame, game);
                }

                refreshChangedInfo(discordGame);
            }

            if (ephemeralMessage.length() == 0) ephemeralMessage = "Command Done.";
            event.getHook().editOriginal(ephemeralMessage).queue();
        } catch (InvalidGameStateException e) {
            event.getHook().editOriginal(e.getMessage()).queue();
        } catch (Exception e) {
            event.getHook().editOriginal(e.getMessage()).queue();
            e.printStackTrace();
        }
    }

    @Override
    public void onCommandAutoCompleteInteraction(@NotNull CommandAutoCompleteInteractionEvent event) {
        DiscordGame discordGame = new DiscordGame(event);

        try {
            Game game = discordGame.getGame();
            event.replyChoices(CommandOptions.getCommandChoices(event, game)).queue();
        } catch (ChannelNotFoundException|IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void onGuildReady(@NotNull GuildReadyEvent event) {
        //add new slash command definitions to commandData list
        List<CommandData> commandData = new ArrayList<>();
        commandData.add(Commands.slash("clean", "FOR TEST ONLY: DO NOT RUN").addOptions(CommandOptions.password));
        commandData.add(Commands.slash("newgame", "Creates a new Dune game instance.").addOptions(CommandOptions.gameName, CommandOptions.gameRole, CommandOptions.modRole));
        commandData.add(Commands.slash("draw", "Draw a card from the top of a deck.").addOptions(CommandOptions.deck, faction));
        commandData.add(Commands.slash("discard", "Move a card from a faction's hand to the discard pile").addOptions(faction, CommandOptions.card));
        commandData.add(Commands.slash("transfercard", "Move a card from one faction's hand to another").addOptions(faction, CommandOptions.card, CommandOptions.recipient));
        commandData.add(Commands.slash("putback", "Used for the Ixian ability to put a treachery card on the top or bottom of the deck.").addOptions(CommandOptions.putBackCard, CommandOptions.bottom));
        commandData.add(Commands.slash("placeforces", "Place forces from reserves onto the surface").addOptions(faction, amount, CommandOptions.starredAmount, CommandOptions.isShipment, CommandOptions.territory));
        commandData.add(Commands.slash("moveforces", "Move forces from one territory to another").addOptions(faction, CommandOptions.fromTerritory, CommandOptions.toTerritory, amount, CommandOptions.starredAmount));
        commandData.add(Commands.slash("removeforces", "Remove forces from the board.").addOptions(faction, amount, CommandOptions.toTanks, CommandOptions.starred, CommandOptions.fromTerritory));
        commandData.add(Commands.slash("awardbid", "Designate that a card has been won by a faction during bidding phase.").addOptions(faction, CommandOptions.spent, CommandOptions.paidToFaction));
        commandData.add(Commands.slash("reviveforces", "Revive forces for a faction.").addOptions(faction, CommandOptions.revived, CommandOptions.starred, CommandOptions.paid));
        commandData.add(Commands.slash("display", "Displays some element of the game to the mod.").addOptions(CommandOptions.data));
        commandData.add(Commands.slash("setstorm", "Sets the storm to an initial sector.").addOptions(CommandOptions.dialOne, CommandOptions.dialTwo));
        commandData.add(Commands.slash("killleader", "Send a leader to the tanks.").addOptions(faction, CommandOptions.leader));
        commandData.add(Commands.slash("reviveleader", "Revive a leader from the tanks.").addOptions(faction, CommandOptions.reviveLeader));
        commandData.add(Commands.slash("bgflip", "Flip BG forces to advisor or fighter.").addOptions(CommandOptions.bgTerritories));
        commandData.add(Commands.slash("mute", "Toggle mute for all bot messages."));
        commandData.add(Commands.slash("bribe", "Record a bribe transaction").addOptions(faction, CommandOptions.recipient, amount, CommandOptions.reason));
        commandData.add(Commands.slash("placehms", "Starting position for Hidden Mobile Stronghold").addOptions(CommandOptions.territory));
        commandData.add(Commands.slash("movehms", "Move Hidden Mobile Stronghold to another territory").addOptions(CommandOptions.territory));
        commandData.add(Commands.slash("assigntechtoken", "Assign a Tech Token to a Faction (taking it away from previous owner)").addOptions(faction, CommandOptions.token));
        commandData.add(Commands.slash("draw-spice-blow", "Draw the spice blow").addOptions(CommandOptions.spiceBlowDeck));
        commandData.add(Commands.slash("create-alliance", "Create an alliance between two factions")
                .addOptions(faction, CommandOptions.otherFaction));
        commandData.add(Commands.slash("remove-alliance", "Remove alliance (only on faction of the alliance needs to be selected)")
                .addOptions(faction));
        commandData.add(Commands.slash("set-spice-in-territory", "Set the spice amount for a territory")
                .addOptions(CommandOptions.territory, amount));
        commandData.add(Commands.slash("destroy-shield-wall", "Destroy the shield wall"));
        commandData.add(Commands.slash("weather-control-storm", "Override the storm movement").addOptions(sectors));

        commandData.add(Commands.slash("add-spice", "Add spice to a faction").addOptions(faction, amount, message, frontOfShield));
        commandData.add(Commands.slash("remove-spice", "Remove spice from a faction").addOptions(faction, amount, message, frontOfShield));

        commandData.addAll(ShowCommands.getCommands());
        commandData.addAll(SetupCommands.getCommands());
        commandData.addAll(RunCommands.getCommands());
        commandData.addAll(RicheseCommands.getCommands());
        commandData.addAll(BTCommands.getCommands());
        commandData.addAll(HarkCommands.getCommands());
        commandData.addAll(ChoamCommands.getCommands());

        List<CommandData> commandDataWithPermissions = commandData.stream()
                .map(command -> command.setDefaultPermissions(
                        DefaultMemberPermissions.enabledFor(Permission.MANAGE_CHANNEL)
                ))
                .collect(Collectors.toList());

        commandDataWithPermissions.addAll(PlayerCommands.getCommands());

        event.getGuild().updateCommands().addCommands(commandDataWithPermissions).queue();
    }

    public void newGame(SlashCommandInteractionEvent event) throws ChannelNotFoundException, IOException {
        Role gameRoleValue = Objects.requireNonNull(event.getOption(gameRole.getName())).getAsRole();
        Role modRoleValue = Objects.requireNonNull(event.getOption(modRole.getName())).getAsRole();
        Role observerRole = Objects.requireNonNull(event.getGuild()).getRolesByName("Observer", true).get(0);
        Role pollBot = event.getGuild().getRolesByName("EasyPoll", true).get(0);
        String name = Objects.requireNonNull(event.getOption(gameName.getName())).getAsString();

        // Create category and set base permissions to deny everything for everyone except the mod role.
        // The channel permissions assume that this is set this way.
        event.getGuild()
                .createCategory(name)
                .addPermissionOverride(modRoleValue, ChannelPermissions.all, null)
                .addPermissionOverride(event.getGuild().getPublicRole(), null, ChannelPermissions.all)
                .addPermissionOverride(gameRoleValue, null, ChannelPermissions.all)
                .addPermissionOverride(observerRole, null, ChannelPermissions.all)
                .complete();

        Category category = event.getGuild().getCategoriesByName(name, true).get(0);

        category.createTextChannel("chat")
                .addPermissionOverride(
                        observerRole,
                        ChannelPermissions.readWriteAllow,
                        ChannelPermissions.readWriteDeny
                )
                .addPermissionOverride(
                        gameRoleValue,
                        ChannelPermissions.readWriteAllow,
                        ChannelPermissions.readWriteDeny
                )
                .complete();

        // Not including Observer in pre-game-voting because there's no way to stop someone from adding to an
        // existing emoji reaction.
        category.createTextChannel("pre-game-voting")
                .addPermissionOverride(
                        gameRoleValue,
                        ChannelPermissions.readAndReactAllow,
                        ChannelPermissions.readAndReactDeny
                )
                .addPermissionOverride(
                        pollBot,
                        ChannelPermissions.pollBotAllow,
                        ChannelPermissions.pollBotDeny
                )
                .complete();

        String[] readAndReactChannels  = {"front-of-shield", "turn-summary", "rules"};

        for (String channel : readAndReactChannels) {
            category.createTextChannel(channel)
                    .addPermissionOverride(
                            observerRole,
                            ChannelPermissions.readAndReactAllow,
                            ChannelPermissions.readAndReactDeny
                    )
                    .addPermissionOverride(
                            gameRoleValue,
                            ChannelPermissions.readAndReactAllow,
                            ChannelPermissions.readAndReactDeny
                    )
                    .complete();
        }

        String[] readWriteChannels = {"game-actions", "bribes", "bidding-phase"};
        for (String channel : readWriteChannels) {
            category.createTextChannel(channel)
                    .addPermissionOverride(
                            observerRole,
                            ChannelPermissions.readAndReactAllow,
                            ChannelPermissions.readAndReactDeny
                    )
                    .addPermissionOverride(
                            gameRoleValue,
                            ChannelPermissions.readWriteAllow,
                            ChannelPermissions.readWriteDeny
                    )
                    .complete();
        }

        String[] modChannels  = {"bot-data", "mod-info"};
        for (String channel : modChannels) {
            category.createTextChannel(channel).complete();
        }

        DiscordGame discordGame = new DiscordGame(category);

        discordGame.getTextChannel("rules").sendMessage(MessageFormat.format(
                """
            {0}  Dune rulebook: https://www.gf9games.com/dunegame/wp-content/uploads/Dune-Rulebook.pdf
            {1}  Dune FAQ Nov 20: https://www.gf9games.com/dune/wp-content/uploads/2020/11/Dune-FAQ-Nov-2020.pdf
            {2} {3}  Ixians & Tleilaxu Rules: https://www.gf9games.com/dunegame/wp-content/uploads/2020/09/IxianAndTleilaxuRulebook.pdf
            {4} {5} CHOAM & Richese Rules: https://www.gf9games.com/dune/wp-content/uploads/2021/11/CHOAM-Rulebook-low-res.pdf""",
                Emojis.DUNE_RULEBOOK,
                Emojis.WEIRDING,
                Emojis.IX, Emojis.BT,
                Emojis.CHOAM, Emojis.RICHESE
        )).queue();

        Game game = new Game();
        game.setGameRole(gameRoleValue.getName());
        game.setModRole(modRoleValue.getName());
        game.setMute(false);
        discordGame.setGame(game);
        discordGame.pushGame();
    }

    /**
     * Add Spice to a player.  Spice can be added behind the shield or in front, with an optional message.
     *
     * @param discordGame The discord game object.
     * @param game The game object.
     * @throws ChannelNotFoundException If the channel is not found.
     */
    public void addSpice(DiscordGame discordGame, Game game) throws ChannelNotFoundException {
        addOrRemoveSpice(discordGame, game, true);
    }

    /**
     * Remove Spice from a player.  Spice can be removed from behind the shield or in front, with an optional message.
     *
     * @param discordGame The discord game object.
     * @param game The game object.
     * @throws ChannelNotFoundException If the channel is not found.
     */
    public void removeSpice(DiscordGame discordGame, Game game) throws ChannelNotFoundException {
        addOrRemoveSpice(discordGame, game, false);
    }

    /**
     * Add or Remove Spice from a player.  This can be behind the shield or in front, with an optional message.
     *
     * @param discordGame The discord game object.
     * @param game The game object.
     * @param add True to add spice, false to remove spice.
     * @throws ChannelNotFoundException If the channel is not found.
     */
    public void addOrRemoveSpice(DiscordGame discordGame, Game game, boolean add) throws ChannelNotFoundException {
        String factionName = discordGame.required(faction).getAsString();
        int amountValue = discordGame.required(amount).getAsInt();
        String messageValue = discordGame.required(message).getAsString();
        boolean toFrontOfShield = discordGame.optional(frontOfShield) != null && discordGame.required(frontOfShield).getAsBoolean();

        Faction faction = game.getFaction(factionName);
        if (toFrontOfShield) {
            if (add) {
                faction.addFrontOfShieldSpice(amountValue);
            } else {
                faction.subtractFrontOfShieldSpice(amountValue);
            }
        } else {
            if (add) {
                faction.addSpice(amountValue);
            } else {
                faction.subtractSpice(amountValue);
            }
        }

        String frontOfShieldMessage = add ? "to front of shield" : "from front of shield";

        discordGame.sendMessage(
                "turn-summary",
                MessageFormat.format(
                        "{0} {1} {2} {3} {4} {5}",
                        faction.getEmoji(),
                        add ? "gains" : "loses",
                        amountValue, Emojis.SPICE,
                        toFrontOfShield ? frontOfShieldMessage : "",
                        messageValue
                )
        );

        if (!toFrontOfShield)
            spiceMessage(discordGame, amountValue, faction.getName(), messageValue, add);

        discordGame.pushGame();
    }

    public void drawSpiceBlow(DiscordGame discordGame, Game game) throws ChannelNotFoundException, IOException {
        String spiceBlowDeckName = discordGame.required(spiceBlowDeck).getAsString();

        LinkedList<SpiceCard> deck = game.getSpiceDeck();
        LinkedList<SpiceCard> discard = spiceBlowDeckName.equalsIgnoreCase("A") ?
                game.getSpiceDiscardA() : game.getSpiceDiscardB();
        LinkedList<SpiceCard> wormsToReshuffle = new LinkedList<>();

        StringBuilder message = new StringBuilder();

        SpiceCard drawn;

        message.append("**Spice Deck ").append(spiceBlowDeckName).append("**\n");

        boolean shaiHuludSpotted = false;
        int spiceMultiplier = 1;

        do {
            if (deck.isEmpty()) {
                deck.addAll(game.getSpiceDiscardA());
                deck.addAll(game.getSpiceDiscardB());
                Collections.shuffle(deck);
                game.getSpiceDiscardA().clear();
                game.getSpiceDiscardB().clear();
                message.append("The Spice Deck is empty, and will be recreated from the Discard piles.\n");
            }

            drawn = deck.pop();
            boolean saveWormForReshuffle = false;
            if (drawn.name().equalsIgnoreCase("Shai-Hulud")) {
                if (game.getTurn() <= 1) {
                    saveWormForReshuffle = true;
                    message.append("Shai-Hulud will be reshuffled back into deck.\n");
                } else if (discard.size() > 0 && !shaiHuludSpotted) {
                    shaiHuludSpotted = true;

                    if (game.isSandtroutInPlay()) {
                        spiceMultiplier = 2;
                        game.setSandtroutInPlay(false);
                        message.append("Shai-Hulud has been spotted! The next Shai-Hulud will cause a Nexus!\n");
                    } else {
                        SpiceCard lastCard = discard.getLast();
                        message.append("Shai-Hulud has been spotted in ").append(lastCard.name()).append("!\n");
                        int spice = game.getTerritories().get(lastCard.name()).getSpice();
                        if (spice > 0) {
                            message.append(spice);
                            message.append(Emojis.SPICE);
                            message.append(" is eaten by the worm!\n");
                            game.getTerritories().get(lastCard.name()).setSpice(0);
                        }
                    }

                } else {
                    shaiHuludSpotted = true;
                    spiceMultiplier = 1;
                    message.append("Shai-Hulud has been spotted!\n");
                }
            } else if (drawn.name().equalsIgnoreCase("Sandtrout")){
                shaiHuludSpotted = true;
                message.append("Sandtrout has been spotted, and all aliances have ended!\n");
                game.getFactions().forEach(Faction::removeAlly);
                game.setSandtroutInPlay(true);
            } else {
                message.append("Spice has been spotted in ");
                message.append(drawn.name());
                message.append("!\n");
            }
            if (saveWormForReshuffle) {
                wormsToReshuffle.add(drawn);
            } else if (!drawn.name().equalsIgnoreCase("Sandtrout")) {
                discard.add(drawn);
            }
        } while (drawn.name().equalsIgnoreCase("Shai-Hulud") ||
                drawn.name().equalsIgnoreCase("Sandtrout"));

        while (wormsToReshuffle.size() > 0) {
            deck.add(wormsToReshuffle.pop());
            if (wormsToReshuffle.size() == 0) {
                Collections.shuffle(deck);
            }
        }

        if (game.getStorm() == drawn.sector())
            message.append(" (blown away by the storm!)");
        else
            game.getTerritories().get(drawn.name()).addSpice(drawn.spice() * spiceMultiplier);

        discordGame.pushGame();

        discordGame.sendMessage("turn-summary", message.toString());
        ShowCommands.showBoard(discordGame, game);
    }

    public void drawCard(DiscordGame discordGame, Game game) throws ChannelNotFoundException {
        String factionName = discordGame.required(faction).getAsString();
        Faction faction = game.getFaction(factionName);
        faction.addTreacheryCard(game.getTreacheryDeck().pop());
        discordGame.pushGame();
    }

    public void discard(DiscordGame discordGame, Game game) throws ChannelNotFoundException {
        String factionName = discordGame.required(faction).getAsString();
        Faction faction = game.getFaction(factionName);

        String cardName = discordGame.required(card).getAsString();

        game.getTreacheryDiscard().add(faction.removeTreacheryCard(cardName));
        discordGame.pushGame();
    }

    public void transferCard(DiscordGame discordGame, Game game) throws ChannelNotFoundException {
        Faction giver = game.getFaction(discordGame.required(faction).getAsString());
        Faction receiver = game.getFaction(discordGame.required(recipient).getAsString());
        String cardName = discordGame.required(card).getAsString();

        receiver.addTreacheryCard(
                giver.removeTreacheryCard(cardName)
        );

        discordGame.pushGame();
    }

    public void putBack(DiscordGame discordGame, Game game) throws ChannelNotFoundException {
        String cardName = discordGame.required(putBackCard).getAsString();
        boolean isBottom = discordGame.required(bottom).getAsBoolean();
        LinkedList<TreacheryCard> market = game.getMarket();
        int i = 0;
        boolean found = false;
        for (; i < market.size(); i++) {
            if (market.get(i).name().contains(cardName)) {
                if (!isBottom) game.getTreacheryDeck().addLast(market.get(i));
                else game.getTreacheryDeck().addFirst(market.get(i));
                found = true;
                break;
            }
        }
        if (!found) {
            discordGame.sendMessage("mod-info", "Card not found, are you sure it's there?");
            return;
        }
        market.remove(i);
        Collections.shuffle(market);
        if (game.hasFaction("Atreides")) {
            discordGame.sendMessage("atreides-chat", MessageFormat.format(
                    "The first card up for bid is {0} {1} {0}",
                    Emojis.TREACHERY,
                    market.stream().findFirst().orElseThrow(() -> new RuntimeException("No cards in market!")).name()
            ));
        }
        discordGame.pushGame();
    }

    public void awardBid(SlashCommandInteractionEvent event, DiscordGame discordGame, Game game) throws ChannelNotFoundException, InvalidGameStateException {
        if (game.getBidCard() == null) {
            throw new InvalidGameStateException("There is no card up for bid.");
        }
        Faction winner = game.getFaction(discordGame.required(faction).getAsString());
        String paidToFactionName = event.getOption("paid-to-faction", "Bank", OptionMapping::getAsString);
        List<TreacheryCard> winnerHand = winner.getTreacheryHand();
        int spentValue = discordGame.required(spent).getAsInt();

        String currentCard = MessageFormat.format(
                "R{0}:C{1}",
                game.getTurn(),
                game.getBidCardNumber()
        );

        discordGame.sendMessage("turn-summary",
                MessageFormat.format(
                        "{0} wins {1} for {2} {3}",
                        winner.getEmoji(),
                        currentCard,
                        spentValue,
                        Emojis.SPICE
                )
        );

        // Winner pays for the card
        winner.subtractSpice(spentValue);
        spiceMessage(discordGame, spentValue, winner.getName(), currentCard, false);

        if (game.hasFaction(paidToFactionName)) {
            Faction paidToFaction = game.getFaction(paidToFactionName);
            spiceMessage(discordGame, spentValue, paidToFaction.getName(), currentCard, true);
            game.getFaction(paidToFaction.getName()).addSpice(spentValue);
            discordGame.sendMessage("turn-summary",
                    MessageFormat.format(
                            "{0} is paid {1} {2} for {3}",
                            paidToFaction.getEmoji(),
                            spentValue,
                            Emojis.SPICE,
                            currentCard
                    )
            );
        }

        winner.addTreacheryCard(game.getBidCard());

        game.clearBidCardInfo();

        // Harkonnen draw an additional card
        if (winner.getName().equals("Harkonnen") && winnerHand.size() < winner.getHandLimit()) {
            if (game.getTreacheryDeck().isEmpty()) {
                List<TreacheryCard> treacheryDiscard = game.getTreacheryDiscard();
                discordGame.sendMessage("turn-summary", "The Treachery Deck has been replenished from the Discard Pile");
                game.getTreacheryDeck().addAll(treacheryDiscard);
                treacheryDiscard.clear();
            }

            game.drawCard("treachery deck", "Harkonnen");
            discordGame.sendMessage("turn-summary", MessageFormat.format(
                    "{0} draws another card from the {1} deck.",
                    winner.getEmoji(), Emojis.TREACHERY
            ));
        }

        discordGame.pushGame();
    }

    public static void spiceMessage(DiscordGame discordGame, int amount, String faction, String message, boolean plus) throws ChannelNotFoundException {
        String plusSign = plus ? "+" : "-";
        for (TextChannel channel : discordGame.getTextChannels()) {
            if (channel.getName().equals(faction.toLowerCase() + "-info")) {
                discordGame.sendMessage(channel.getName(),
                        MessageFormat.format(
                                "{0}{1}{2} {3}",
                                plusSign,
                                amount,
                                Emojis.SPICE,
                                message
                        ));
            }
        }
    }

    public void killLeader(DiscordGame discordGame, Game game) throws ChannelNotFoundException {
        Faction targetFaction = game.getFaction(discordGame.required(faction).getAsString());
        game.getLeaderTanks().add(targetFaction.removeLeader(discordGame.required(leader).getAsString()));
        discordGame.pushGame();
    }

    public void reviveLeader(DiscordGame discordGame, Game game) throws ChannelNotFoundException {
        Faction targetFaction = game.getFaction(discordGame.required(faction).getAsString());
        targetFaction.addLeader(
                game.removeLeaderFromTanks(discordGame.required(reviveLeader).getAsString())
        );

        discordGame.pushGame();
    }

    public void revival(DiscordGame discordGame, Game game) throws ChannelNotFoundException {
        String star = discordGame.required(starred).getAsBoolean() ? "*" : "";
        Faction targetFaction = game.getFaction(discordGame.required(faction).getAsString());
        boolean isPaid = discordGame.required(paid).getAsBoolean();
        int revivedValue = discordGame.required(revived).getAsInt();

        int revivalCost;

        if (targetFaction.getName().equalsIgnoreCase("CHOAM")) revivalCost = revivedValue;
        else if (targetFaction.getName().equalsIgnoreCase("BT")) revivalCost = revivedValue;
        else revivalCost = revivedValue * 2;

        if (star.equals("")) targetFaction.addReserves(revivedValue);
        else targetFaction.addSpecialReserves(revivedValue);

        Force force = game.getForceFromTanks(targetFaction.getName() + star);
        force.setStrength(force.getStrength() - revivedValue);

        if (isPaid) {
            targetFaction.subtractSpice(revivalCost);
            spiceMessage(discordGame, revivalCost, targetFaction.getName(), "Revivals", false);
            if (game.hasFaction("BT") && !targetFaction.getName().equalsIgnoreCase("BT")) {
                game.getFaction("BT").addSpice(revivalCost);
                spiceMessage(discordGame, revivalCost, "bt", targetFaction.getEmoji() + " revivals", true);
            }
        }

        discordGame.pushGame();
    }

    /**
     * Place forces in a territory
     *
     * @param discordGame  the discord game
     * @param game         the game
     */
    public void placeForces(DiscordGame discordGame, Game game) throws ChannelNotFoundException {
        Territory targetTerritory = game.getTerritories().get(discordGame.required(territory).getAsString());
        Faction targetFaction = game.getFaction(discordGame.required(faction).getAsString());
        int amountValue = discordGame.required(amount).getAsInt();
        int starredAmountValue = discordGame.required(starredAmount).getAsInt();

        Force reserves = targetFaction.getReserves();
        Force specialReserves = targetFaction.getSpecialReserves();

        if (amountValue > 0) placeForceInTerritory(targetTerritory, targetFaction, amountValue, false);

        if (starredAmountValue > 0) placeForceInTerritory(targetTerritory, targetFaction, starredAmountValue, true);

        if (discordGame.required(isShipment).getAsBoolean()) {
            int costPerForce = targetTerritory.isStronghold() ? 1 : 2;
            int baseCost = costPerForce * (amountValue + starredAmountValue);
            int cost;

            if (targetFaction.getName().equalsIgnoreCase("Guild")) {
                cost = Math.ceilDiv(baseCost, 2);
            } else if (targetFaction.getName().equalsIgnoreCase("Fremen")) {
                cost = 0;
            } else {
                cost = baseCost;
            }

            StringBuilder message = new StringBuilder();

            message.append(targetFaction.getEmoji())
                            .append(": ");

            if (amountValue > 0) {
                message.append(MessageFormat.format("{0} {1} ", amountValue, Emojis.getForceEmoji(reserves.getName())));
            }

            if (starredAmountValue > 0) {
                message.append(MessageFormat.format("{0} {1} ", starredAmountValue, Emojis.getForceEmoji(specialReserves.getName())));
            }

            message.append(
                    MessageFormat.format("placed on {0}",
                            targetTerritory.getTerritoryName()
                    )
            );

            if (cost > 0) {
                message.append(
                        MessageFormat.format(" for {0} {1}",
                                cost, Emojis.SPICE
                        )
                );

                targetFaction.subtractSpice(cost);
                spiceMessage(discordGame, cost, targetFaction.getName(),
                        "shipment to " + targetTerritory.getTerritoryName(), false);

                if (game.hasFaction("Guild") && !targetFaction.getName().equals("Guild")) {
                    game.getFaction("Guild").addSpice(cost);
                    message.append(" paid to ")
                            .append(game.getFaction("Guild").getEmoji());
                    spiceMessage(discordGame, cost, "guild", targetFaction.getEmoji() + " shipment", true);
                }

            }

            if (
                    !targetFaction.getName().equalsIgnoreCase("Guild") &&
                            !targetFaction.getName().equalsIgnoreCase("Fremen") &&
                            game.hasGameOption(GameOption.TECH_TOKENS)
            ) {
                TechToken.addSpice(game, discordGame, "Heighliners");
            }

            discordGame.sendMessage("turn-summary", message.toString());
        }

        discordGame.pushGame();
    }

    /**
     * Places a force from the reserves into a territory.
     * @param territory The territory to place the force in.
     * @param faction The faction that owns the force.
     * @param amount The number of forces to place.
     * @param special Whether the force is a special reserve.
     */
    public void placeForceInTerritory(Territory territory, Faction faction, int amount, boolean special) {
        String forceName;

        if (special) {
            faction.removeSpecialReserves(amount);
            forceName = faction.getSpecialReserves().getName();
        } else {
            faction.removeReserves(amount);
            forceName = faction.getReserves().getName();
        }

        Force territoryForce = territory.getForce(forceName);
        territory.setForceStrength(forceName, territoryForce.getStrength() + amount);
    }

    public void moveForces(DiscordGame discordGame, Game game) throws ChannelNotFoundException, InvalidOptionException {
        Faction targetFaction = game.getFaction(discordGame.required(faction).getAsString());
        Territory from = game.getTerritories().get(discordGame.required(fromTerritory).getAsString());
        Territory to = game.getTerritories().get(discordGame.required(toTerritory).getAsString());
        int amountValue = discordGame.required(amount).getAsInt();
        int starredAmountValue = discordGame.required(starredAmount).getAsInt();

        int fromForceStrength = from.getForce(targetFaction.getName()).getStrength();
        int fromStarredForceStrength = from.getForce(targetFaction.getName() + "*").getStrength();

        if (fromForceStrength < amountValue || fromStarredForceStrength < starredAmountValue) {
            throw new InvalidOptionException("Not enough forces in territory.");
        }

        StringBuilder message = new StringBuilder();

        message.append(targetFaction.getEmoji())
                .append(": ");

        if (amountValue > 0) {
            from.setForceStrength(targetFaction.getName(), fromForceStrength - amountValue);
            to.setForceStrength(targetFaction.getName(), to.getForce(targetFaction.getName()).getStrength() + amountValue);

            message.append(
                    MessageFormat.format("{0} {1} ",
                            amountValue, Emojis.getForceEmoji(from.getForce(targetFaction.getName()).getName())
                    )
            );
        }

        if (starredAmountValue > 0) {
            from.setForceStrength(targetFaction.getName() + "*", fromStarredForceStrength - starredAmountValue);
            to.setForceStrength(targetFaction.getName() + "*",
                    to.getForce(targetFaction.getName() + "*").getStrength() + starredAmountValue);

            message.append(
                    MessageFormat.format("{0} {1} ",
                            starredAmountValue, Emojis.getForceEmoji(from.getForce(targetFaction.getName() + "*").getName())
                    )
            );

        }

        message.append(
                MessageFormat.format("moved from {0} to {1}",
                        from.getTerritoryName(), to.getTerritoryName()
                )
        );

        discordGame.sendMessage("turn-summary", message.toString());

        discordGame.pushGame();
    }

    public void removeForces(DiscordGame discordGame, Game game) throws ChannelNotFoundException {
        String territoryName = discordGame.required(fromTerritory).getAsString();
        Faction targetFaction = game.getFaction(discordGame.required(faction).getAsString());
        int amountValue = discordGame.required(amount).getAsInt();
        boolean isSpecial = discordGame.required(starred).getAsBoolean();
        boolean isToTanks = discordGame.required(toTanks).getAsBoolean();

        targetFaction.removeForces(territoryName, amountValue, isSpecial, isToTanks);

        discordGame.pushGame();
    }

    public void setStorm(DiscordGame discordGame, Game game) throws ChannelNotFoundException, IOException {
        int stormDialOne = discordGame.required(dialOne).getAsInt();
        int stormDialTwo = discordGame.required(dialTwo).getAsInt();
        game.advanceStorm(stormDialOne + stormDialTwo);
        discordGame.sendMessage("turn-summary","The storm has been initialized to sector " + game.getStorm() + " (" + stormDialOne + " + " + stormDialTwo + ")");
        if (game.hasTechTokens()) {
            List<TechToken> techTokens = new LinkedList<>();
            if (game.hasFaction("BT")) {
                game.getFaction("BT").getTechTokens().add(new TechToken("Axlotl Tanks"));
            } else techTokens.add(new TechToken("Axlotl Tanks"));
            if (game.hasFaction("Ix")) {
                game.getFaction("Ix").getTechTokens().add(new TechToken("Heighliners"));
            } else techTokens.add(new TechToken("Heighliners"));
            if (game.hasFaction("Fremen")) {
                game.getFaction("Fremen").getTechTokens().add(new TechToken("Spice Production"));
            } else techTokens.add(new TechToken("Spice Production"));
            if (!techTokens.isEmpty()) {
                Collections.shuffle(techTokens);
                for (int i = 0; i < techTokens.size(); i++) {
                    int firstFactionIndex = (Math.ceilDiv(game.getStorm(), 3) + i) % 6;
                    for (int j = 0; j < 6; j++) {
                        Faction faction = game.getFactions().get((firstFactionIndex + j) % 6);
                        if (faction.getTechTokens().isEmpty()) {
                            faction.getTechTokens().add(techTokens.get(i));
                            break;
                        }
                    }
                }
            }
        }
        discordGame.pushGame();
        ShowCommands.showBoard(discordGame, game);
    }

    public void bgFlip(DiscordGame discordGame, Game game) throws ChannelNotFoundException, IOException {
        Territory territory = game.getTerritories().get(discordGame.required(bgTerritories).getAsString());
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
        discordGame.pushGame();
        ShowCommands.showBoard(discordGame, game);
    }

    public void placeHMS(DiscordGame discordGame, Game game) throws ChannelNotFoundException, IOException {
        Territory targetTerritory = game.getTerritories().get(discordGame.required(territory).getAsString());
        targetTerritory.getForces().add(new Force("Hidden Mobile Stronghold", 1));
        discordGame.pushGame();
        ShowCommands.showBoard(discordGame, game);
    }

    public void moveHMS(DiscordGame discordGame, Game game) throws ChannelNotFoundException, IOException {
        for (Territory territory : game.getTerritories().values()) {
            territory.getForces().removeIf(force -> force.getName().equals("Hidden Mobile Stronghold"));
        }
        placeHMS(discordGame, game);
    }

    public void assignTechToken(DiscordGame discordGame, Game game) throws ChannelNotFoundException, IOException {
        for (Faction faction : game.getFactions()) {
            faction.getTechTokens().removeIf(
                    techToken -> techToken.getName().equals(discordGame.required(token).getAsString()));
        }
        game.getFaction(discordGame.required(faction).getAsString())
                .getTechTokens().add(new TechToken(discordGame.required(token).getAsString()));
        discordGame.sendMessage("turn-summary",
                discordGame.required(token).getAsString() + " has been transferred to " +
                        game.getFaction(discordGame.required(faction).getAsString()).getEmoji());
        ShowCommands.showBoard(discordGame, game);
        discordGame.pushGame();
    }

    public void bribe(DiscordGame discordGame, Game game) throws ChannelNotFoundException {
        Faction fromFaction = game.getFaction(discordGame.required(faction).getAsString());
        Faction recipientFaction = game.getFaction(discordGame.required(recipient).getAsString());
        int amountValue = discordGame.required(amount).getAsInt();

        if (fromFaction.getSpice() < amountValue) {
            discordGame.getTextChannel("mod-info").sendMessage("Faction does not have enough spice to pay the bribe!").queue();
            return;
        }
        fromFaction.subtractSpice(amountValue);
        spiceMessage(discordGame, amountValue, fromFaction.getName(), "bribe to " + recipientFaction.getEmoji(), false);

        discordGame.sendMessage(
                "turn-summary",
                MessageFormat.format(
                        "{0} places {1} {2} in front of {3} shield.",
                        fromFaction.getEmoji(), amountValue, Emojis.SPICE, recipientFaction.getEmoji()
                )
        );

        if (discordGame.optional(reason) != null) {
            discordGame.sendMessage("bribes",
                    MessageFormat.format("{0} {1}\n{2}",
                            fromFaction.getEmoji(), recipientFaction.getEmoji(),
                            discordGame.optional(reason).getAsString()
                    )
            );
        }

        recipientFaction.addFrontOfShieldSpice(amountValue);
        discordGame.pushGame();
    }

    public void mute(DiscordGame discordGame, Game game) throws ChannelNotFoundException {
        game.setMute(!game.getMute());

        discordGame.pushGame();
    }

    public void displayGameState(DiscordGame discordGame, Game game) throws ChannelNotFoundException, IOException {
        TextChannel channel = discordGame.getTextChannel("mod-info");
        switch (discordGame.required(data).getAsString()) {
            case "territories" -> {
               Map<String, Territory> territories = game.getTerritories();
               for (Territory territory: territories.values()) {
                   if (territory.getSpice() == 0 && !territory.isStronghold() && territory.getForces().isEmpty()) continue;
                   discordGame.sendMessage(channel.getName(), "**" + territory.getTerritoryName() + "** \n" +
                           "Spice: " + territory.getSpice() + "\nForces: " + territory.getForces().toString());
               }
            }
            case "dnd" -> {
                discordGame.sendMessage("mod-info", game.getTreacheryDeck().toString());
                discordGame.sendMessage("mod-info", game.getTreacheryDiscard().toString());
                discordGame.sendMessage("mod-info", game.getSpiceDeck().toString());
                discordGame.sendMessage("mod-info", game.getSpiceDiscardA().toString());
                discordGame.sendMessage("mod-info", game.getSpiceDiscardB().toString());
                discordGame.sendMessage("mod-info", game.getLeaderSkillDeck().toString());
                discordGame.sendMessage("mod-info", game.getTraitorDeck().toString());
                discordGame.sendMessage("mod-info", game.getMarket().toString());
            }
            case "factions" -> {
                for (Faction faction: game.getFactions()) {
                    String message = "**" + faction.getName() + ":**\nPlayer: " + faction.getUserName() + "\n" +
                            "spice: " + faction.getSpice() + "\nTreachery Cards: " + faction.getTreacheryHand() +
                            "\nTraitors:" + faction.getTraitorHand() + "\nLeaders: " + faction.getLeaders() + "\n";
                    discordGame.sendMessage(channel.getName(), message);
                }
            }
        }
        ShowCommands.showBoard(discordGame, game);
    }

    public void createAlliance(DiscordGame discordGame, Game game) throws ChannelNotFoundException {
        Faction factionOne = game.getFaction(discordGame.required(faction).getAsString());
        Faction factionTwo = game.getFaction(discordGame.required(otherFaction).getAsString());

        removeAlliance(game, factionOne);
        removeAlliance(game, factionTwo);

        factionOne.setAlly(factionTwo.getName());
        factionTwo.setAlly(factionOne.getName());

        String threadName = MessageFormat.format(
                "{0} {1} Alliance",
                factionOne.getName(),
                factionTwo.getName()
        );

        discordGame.createThread("chat", threadName, Arrays.asList(
                factionOne.getPlayer(), factionTwo.getPlayer()
        ));

        discordGame.pushGame();
    }

    public void removeAlliance(DiscordGame discordGame, Game game) throws ChannelNotFoundException {
        Faction targetFaction = game.getFaction(discordGame.required(faction).getAsString());
        removeAlliance(game, targetFaction);

        discordGame.pushGame();
    }

    private void removeAlliance(Game game, Faction faction) {
        if (faction.hasAlly()) {
            game.getFaction(faction.getAlly()).removeAlly();
        }
        faction.removeAlly();
    }

    public void setSpiceInTerritory(DiscordGame discordGame, Game game) throws ChannelNotFoundException {
        String territoryName = discordGame.required(territory).getAsString();
        int amountValue = discordGame.required(amount).getAsInt();

        game.getTerritories().get(territoryName).setSpice(amountValue);
        discordGame.pushGame();
    }

    public void destroyShieldWall(DiscordGame discordGame, Game game) throws ChannelNotFoundException, InvalidGameStateException {
        Faction factionWithAtomics = null;
        try {
            factionWithAtomics = game.getFactionWithAtomics();
        } catch (NoSuchElementException e) {
            throw new InvalidGameStateException("No faction holds Family Atomics.");
        }

        if (!factionWithAtomics.isNearShieldWall()) {
            throw new InvalidGameStateException(factionWithAtomics.getEmoji() + " is not in position to use Family Atomics.");
        } else {
            String message = game.breakShieldWall(factionWithAtomics);
            discordGame.sendMessage("turn-summary", message);
            discordGame.pushGame();
        }
    }

    public void weatherControlStorm(DiscordGame discordGame, Game game) throws ChannelNotFoundException {
        int wcStormMovement = discordGame.required(sectors).getAsInt();
        game.setStormMovement(wcStormMovement);

        discordGame.pushGame();
    }
}
