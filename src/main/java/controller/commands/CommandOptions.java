package controller.commands;

import controller.DiscordGame;
import controller.buttons.IxButtons;
import controller.buttons.ShipmentAndMovementButtons;
import enums.GameOption;
import exceptions.ChannelNotFoundException;
import exceptions.InvalidGameStateException;
import model.*;
import model.factions.*;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageHistory;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class CommandOptions {

    public static final OptionData book = new OptionData(OptionType.STRING, "book", "Which book to quote.")
            .addChoice("Dune", "Dune.txt")
            .addChoice("Dune Messiah", "Messiah.txt")
            .addChoice("Children of Dune", "Children.txt")
            .addChoice("God Emperor of Dune", "GeoD.txt")
            .addChoice("Heretics of Dune", "Heretics.txt")
            .addChoice("Chapterhouse: Dune", "Chapterhouse.txt");

    public static final OptionData lines = new OptionData(OptionType.INTEGER, "lines", "How many lines long the quotation will be.", true);
    public static final OptionData startingLine = new OptionData(OptionType.INTEGER, "starting-line", "Where you want the quote to start (not random). The nth result if used with search.");
    public static final OptionData search = new OptionData(OptionType.STRING, "search", "Will include the search term if it finds a match.");

    public static final OptionData gameName = new OptionData(OptionType.STRING, "name", "e.g. 'Dune Discord #5: The Tortoise and the Hajr'", true);
    public static final OptionData gameRole = new OptionData(OptionType.ROLE, "gamerole", "The role you created for the players of this game", true);
    public static final OptionData modRole = new OptionData(OptionType.ROLE, "modrole", "The role you created for the mod(s) of this game", true);
    public static final OptionData user = new OptionData(OptionType.USER, "player", "The player for the faction", true);
    public static final OptionData allFactions = new OptionData(OptionType.STRING, "faction", "The faction", true)
            .addChoice("Atreides", "Atreides")
            .addChoice("Harkonnen", "Harkonnen")
            .addChoice("Emperor", "Emperor")
            .addChoice("Fremen", "Fremen")
            .addChoice("Spacing Guild", "Guild")
            .addChoice("Bene Gesserit", "BG")
            .addChoice("Ixian", "Ix")
            .addChoice("Tleilaxu", "BT")
            .addChoice("CHOAM", "CHOAM")
            .addChoice("Richese", "Richese")
            .addChoice("Ecaz", "Ecaz")
            .addChoice("Moritani", "Moritani");
    public static final OptionData faction = new OptionData(OptionType.STRING, "factionname", "The faction", true)
            .setAutoComplete(true);
    public static final OptionData otherFaction = new OptionData(OptionType.STRING, "other-factionname", "The Other faction", true)
            .setAutoComplete(true);
    public static final OptionData karamaFaction = new OptionData(OptionType.STRING, "karama-faction", "The faction playing Karama", true)
            .setAutoComplete(true);
    public static final OptionData turn = new OptionData(OptionType.INTEGER, "turn", "The turn number.", true);
    public static final OptionData amount = new OptionData(OptionType.INTEGER, "amount", "Amount", true);
    public static final OptionData message = new OptionData(OptionType.STRING, "message", "Message for spice transactions", true);
    public static final OptionData reason = new OptionData(OptionType.STRING, "reason", "description of the bribe", false);
    public static final OptionData deck = new OptionData(OptionType.STRING, "deck", "The deck", true)
            .addChoice("Treachery Deck", "treachery deck")
            .addChoice("Traitor Deck", "traitor deck");
    public static final OptionData card = new OptionData(OptionType.STRING, "card", "The card.", true).setAutoComplete(true);
    public static final OptionData discardCard = new OptionData(OptionType.STRING, "card-discard", "The card.", true).setAutoComplete(true);

    public static final OptionData ixCard = new OptionData(OptionType.STRING, "ixcard", "The card.", true).setAutoComplete(true);
    public static final OptionData putBackCard = new OptionData(OptionType.STRING, "putbackcard", "The card.", true).setAutoComplete(true);
    public static final OptionData topOrBottom = new OptionData(OptionType.STRING, "top-or-bottom", "Top or Bottom of treachery deck", true)
            .addChoice("Top", "top")
            .addChoice("Bottom", "bottom");
    public static final OptionData recipient = new OptionData(OptionType.STRING, "recipient", "The recipient", true).setAutoComplete(true);
    public static final OptionData traitor = new OptionData(OptionType.STRING, "traitor", "The name of the traitor", true).setAutoComplete(true);
    public static final OptionData territory = new OptionData(OptionType.STRING, "territory", "The name of the territory", true).setAutoComplete(true);
    public static final OptionData hmsTerritory = new OptionData(OptionType.STRING, "hms-territory", "The name of the territory", true).setAutoComplete(true);
    public static final OptionData dialOne = new OptionData(OptionType.INTEGER, "dial-one", "The dial of the first player", true);
    public static final OptionData dialTwo = new OptionData(OptionType.INTEGER, "dial-two", "The dial of the second player", true);
    public static final OptionData starred = new OptionData(OptionType.BOOLEAN, "starred", "Are they starred forces?", true);
    public static final OptionData paid = new OptionData(OptionType.BOOLEAN, "paid", "Is the action paid for?", true);

    public static final OptionData spent = new OptionData(OptionType.INTEGER, "spent", "How much was spent on the card.", true);
    public static final OptionData revived = new OptionData(OptionType.INTEGER, "revived", "How many are being revived.", true);
    public static final OptionData sectors = new OptionData(OptionType.INTEGER, "sectors", "Number of sectors to move storm", true);
    public static final OptionData data = new OptionData(OptionType.STRING, "data", "What data to display", true)
            .addChoice("Territories", "territories")
            .addChoice("Decks and Discards", "dnd")
            .addChoice("Phase, Turn, and everything else", "etc")
            .addChoice("Faction Info", "factions");
    public static final OptionData isShipment = new OptionData(OptionType.BOOLEAN, "is-shipment", "Is this placement a shipment?", true);
    public static final OptionData canTrigger = new OptionData(OptionType.BOOLEAN, "can-trigger", "Can this placement trigger Ambassadors and Terror Tokens?", true);
    public static final OptionData toTanks = new OptionData(OptionType.BOOLEAN, "totanks", "Remove these forces to the tanks (true) or to reserves (false)?", true);
    public static final OptionData leader = new OptionData(OptionType.STRING, "leadertokill", "The leader.", true).setAutoComplete(true);
    public static final OptionData reviveLeader = new OptionData(OptionType.STRING, "leadertorevive", "The leader.", true).setAutoComplete(true);
    public static final OptionData combatLeader = new OptionData(OptionType.STRING, "combat-leader", "The leader or None.", true).setAutoComplete(true);
    public static final OptionData removeLeader = new OptionData(OptionType.STRING, "leader-to-remove", "The leader incorrectly in a territory.", true).setAutoComplete(true);
    public static final OptionData combatDial = new OptionData(OptionType.STRING, "combat-dial", "The dial on the battle wheel.", true);
    public static final OptionData combatSpice = new OptionData(OptionType.INTEGER, "combat-spice", "Spice used for backing troops", true);
    public static final OptionData weapon = new OptionData(OptionType.STRING, "weapon", "Weapon or Worthless.", true).setAutoComplete(true);
    public static final OptionData defense = new OptionData(OptionType.STRING, "defense", "Defense or Worthless.", true).setAutoComplete(true);
    public static final OptionData deactivatePoisonTooth = new OptionData(OptionType.BOOLEAN, "deactivate-poison-tooth", "Allow battle plan resolution with Poison Tooth not used (default = False)", false);
    public static final OptionData addPortableSnooper = new OptionData(OptionType.BOOLEAN, "add-portable-snooper", "Allow battle plan resolution with Portable Snooper added (default = False)", false);
    public static final OptionData stoneBurnerKills = new OptionData(OptionType.BOOLEAN, "stone-burner-kills", "Select whether Stone Burner kills leaders (default = False)", false);
    public static final OptionData useJuiceOfSapho = new OptionData(OptionType.BOOLEAN, "use-juice-of-sapho", "Use Juice of Sapho", false);
    public static final OptionData fromTerritory = new OptionData(OptionType.STRING, "from", "the territory.", true).setAutoComplete(true);
    public static final OptionData toTerritory = new OptionData(OptionType.STRING, "to", "Moving to this territory.", true).setAutoComplete(true);
    public static final OptionData starredAmount = new OptionData(OptionType.INTEGER, "starredamount", "Starred amount", true);
    public static final OptionData bgTerritories = new OptionData(OptionType.STRING, "bgterritories", "Territory to flip the BG force", true).setAutoComplete(true);
    public static final OptionData token = new OptionData(OptionType.STRING, "token", "The Tech Token", true)
            .addChoice(TechToken.HEIGHLINERS, TechToken.HEIGHLINERS)
            .addChoice(TechToken.SPICE_PRODUCTION, TechToken.SPICE_PRODUCTION)
            .addChoice(TechToken.AXLOTL_TANKS, TechToken.AXLOTL_TANKS);
    public static final OptionData factionLeader = new OptionData(OptionType.STRING, "factionleader", "The leader.", true).setAutoComplete(true);
    public static final OptionData nonHarkLeader = new OptionData(OptionType.STRING, "returning", "Leader to return.", true).setAutoComplete(true);
    public static final OptionData factionLeaderSkill =
            new OptionData(OptionType.STRING, "factionleaderskill", "Leader Skill available to the faction", true)
                    .setAutoComplete(true);
    public static final OptionData spiceBlowDeck = new OptionData(OptionType.STRING, "spice-blow-deck", "Which deck to discard spice blow to (A or B)", true)
            .addChoice("A", "A")
            .addChoice("B", "B");
    public static final OptionData richeseCard =
            new OptionData(OptionType.STRING, "richese-card", "Select Richese card to bid on.", true)
                    .setAutoComplete(true);
    public static final OptionData richeseBidType =
            new OptionData(OptionType.STRING, "richese-bid-type", "Type of bidding for a Richese card.", true)
                    .addChoice("OnceAroundCW", "OnceAroundCW")
                    .addChoice("OnceAroundCCW", "OnceAroundCCW")
                    .addChoice("Silent", "Silent");
    public static final OptionData richeseBlackMarketCard =
            new OptionData(OptionType.STRING, "richese-black-market-card", "Select Richese Black Market card to bid on.", true)
                    .setAutoComplete(true);
    public static final OptionData richeseBlackMarketBidType =
            new OptionData(OptionType.STRING, "richese-black-market-bid-type", "Type of bidding for a Richese Black Market card.", true)
                    .addChoice("OnceAroundCW", "OnceAroundCW")
                    .addChoice("OnceAroundCCW", "OnceAroundCCW")
                    .addChoice("Silent", "Silent")
                    .addChoice("Normal", "Normal");

    public static final OptionData paidToFaction =
            new OptionData(OptionType.STRING, "paid-to-faction", "Which faction is bidding paid to.", false)
                    .setAutoComplete(true);
    public static final OptionData richeseNoFields =
            new OptionData(OptionType.INTEGER, "richese-no-fields", "Value of Richese No-Fields token.", true)
                    .addChoice("0 No-Field", 0)
                    .addChoice("3 No-Field", 3)
                    .addChoice("5 No-Field", 5);
    public static final OptionData btFaceDancer =
            new OptionData(OptionType.STRING, "bt-face-dancer", "Select BT Face Dancer", true)
                    .setAutoComplete(true);

    public static final OptionData addGameOption =
            new OptionData(OptionType.STRING, "add-game-option", "Game option to add", true)
                    .setAutoComplete(true);

    public static final OptionData removeGameOption =
            new OptionData(OptionType.STRING, "remove-game-option", "Game option to remove", true)
                    .setAutoComplete(true);

    public static final OptionData incrementOrExact =
            new OptionData(OptionType.BOOLEAN, "use-exact", "Set true for exact, false for increment.", true);

    public static final OptionData autoPass =
            new OptionData(OptionType.BOOLEAN, "enabled", "Set to true if you want auto-pass enabled.", true);

    public static final OptionData autoPassAfterMax =
            new OptionData(OptionType.BOOLEAN, "auto-pass-after-max", "Set to true if you want to automatically pass after your max bid is exceeded.", false);

    public static final OptionData outbidAlly =
            new OptionData(OptionType.BOOLEAN, "outbid-ally", "You will pass if your ally is highest bidder unless set to true.");

    public static final OptionData holdgameReason = new OptionData(OptionType.STRING, "reason-for-hold", "Tell the mod why you are putting a hold on the game", true);

    public static final OptionData frontOfShield =
            new OptionData(OptionType.BOOLEAN, "front-of-shield", "Set to true for front of shield (default false)");
    public static final OptionData choamInflationType =
            new OptionData(OptionType.STRING, "choam-inflation-type", "Type of CHOAM inflation.", true)
                    .addChoice("Double", "DOUBLE")
                    .addChoice("Cancel", "CANCEL");

    private static OptionData waitingListOptionData(String name, String description) {
        return new OptionData(OptionType.STRING, name, description, true)
                .addChoice("Either way is okay", "Maybe")
                .addChoice("Preferred", "Yes")
                .addChoice("Don't want", "No");
    }
    public static final OptionData slowGame = waitingListOptionData("slow-game", "Do you want to play a slow speed game?");
    public static final OptionData midGame = waitingListOptionData("mid-game", "Do you want to play a normal speed game?");
    public static final OptionData fastGame = waitingListOptionData("fast-game", "Do you want to play a fast speed game?");
    public static final OptionData ixianstleilaxuExpansion = waitingListOptionData("ix-bt", "Do you want to with the Ixians & Tleilaxu expansion content?");
    public static final OptionData choamricheseExpansion = waitingListOptionData("choam-rich", "Do you want to play with the Choam & Richese expansion content?");
    public static final OptionData ecazmoritaniExpansion = waitingListOptionData("ecaz-moritani", "Do you want to play with the Ecaz & Moritani expansion content?");
    public static final OptionData leaderSkills = waitingListOptionData("leader-skills", "Do you want to play with leader skills?");
    public static final OptionData strongholdCards = waitingListOptionData("stronghold-cards", "Do you want to play with stronghold cards?");
    public static final OptionData homeworlds = waitingListOptionData("homeworlds", "Do you want to play with homeworlds?");

    public static final OptionData gameState =
            new OptionData(OptionType.STRING, "game-state", "Select a game state to rewind to.", true)
                    .setAutoComplete(true);

    public static final OptionData buttonId =
            new OptionData(OptionType.STRING, "button-id", "ID for the button", true);

    public static final OptionData buttonName =
            new OptionData(OptionType.STRING, "button-name", "Name for the button", false);

    public static final OptionData months = new OptionData(OptionType.INTEGER, "months", "List players from games that finished within this many months (default = 1)");

    public static final OptionData clockDirection =
            new OptionData(OptionType.STRING, "clock-direction", "Clockwise is default", false)
                    .addChoice("Clockwise", "CW")
                    .addChoice("Counterclockwise", "CCW");

    public static final OptionData ecazAmbassadorsOnMap =
            new OptionData(OptionType.STRING, "ecaz-ambassador-on-map", "Ecaz Embassador Token on the Map", true)
                    .setAutoComplete(true);

    public static final OptionData moritaniTerrorTokenOnMap =
            new OptionData(OptionType.STRING, "moritani-terror-token-on-map", "Moritani Terror Token on the Map", true)
                    .setAutoComplete(true);

    public static final OptionData toHand =
            new OptionData(OptionType.BOOLEAN, "to-hand", "Move to hand (default=false)", false);

    public static List<Command.Choice> getCommandChoices(CommandAutoCompleteInteractionEvent event, DiscordGame discordGame, Game game) throws ChannelNotFoundException {
        String optionName = event.getFocusedOption().getName();
        String searchValue = event.getFocusedOption().getValue();

        List<Command.Choice> choices = new ArrayList<>();

        switch (optionName) {
            case "factionname", "other-factionname", "sender", "recipient", "paid-to-faction", "karama-faction" ->
                    choices = factions(game, searchValue);
            case "territory", "to" -> choices = territories(game, searchValue);
            case "hms-territory" -> choices = hmsTerritories(game, searchValue);
            case "traitor" -> choices = traitors(event, game, searchValue);
            case "card" -> choices = cardsInHand(event, game, searchValue);
            case "card-discard" -> choices = cardsInDiscard(game, searchValue);
            case "ixcard" -> choices = ixCardsInHand(game, searchValue);
            case "putbackcard" -> choices = cardsInMarket(game, searchValue);
            case "from" -> choices = fromTerritories(event, game, searchValue);
            case "bgterritories" -> choices = bgTerritories(game, searchValue);
            case "leadertokill", "factionleader" -> choices = leaders(event, game, searchValue);
            case "leadertorevive" -> choices = reviveLeaders(game, searchValue);
            case "combat-leader" -> choices = combatLeaders(event, discordGame, searchValue);
            case "leader-to-remove" -> choices = removeLeaders(event, game, searchValue);
            case "weapon" -> choices = weapon(event, discordGame, searchValue);
            case "defense" -> choices = defense(event, discordGame, searchValue);
            case "factionleaderskill" -> choices = factionLeaderSkill(event, game, searchValue);
            case "richese-card" -> choices = richeseCard(game, searchValue);
            case "bt-face-dancer" -> choices = btFaceDancers(game, searchValue);
            case "richese-black-market-card" -> choices = richeseBlackMarketCard(game, searchValue);
            case "add-game-option" -> choices = getAddGameOptions(game, searchValue);
            case "remove-game-option" -> choices = getRemoveGameOptions(game, searchValue);
            case "returning" -> choices = nonHarkLeaders(game, searchValue);
            case "game-state" -> choices = getGameStates(discordGame, searchValue);
            case "ecaz-ambassador-on-map" -> choices = getEcazAmbassadorsOnMap(discordGame, searchValue);
            case "moritani-terror-token-on-map" -> choices = getMoritaniTerrorTokensOnMap(discordGame, searchValue);
        }

        return choices;
    }

    private static List<Command.Choice> factions(@NotNull Game game, String searchValue) {
        return game.getFactions().stream()
                .map(Faction::getName)
                .filter(factionName -> factionName.toLowerCase().matches(searchRegex(searchValue.toLowerCase())))
                .map(factionName -> new Command.Choice(factionName, factionName))
                .collect(Collectors.toList());
    }

    private static List<Command.Choice> territories(@NotNull Game game, String searchValue) {
        return game.getTerritories().values().stream()
                .map(Territory::getTerritoryName)
                .filter(territoryName -> territoryName.toLowerCase().matches(searchRegex(searchValue.toLowerCase())))
                .map(territoryName -> new Command.Choice(territoryName, territoryName))
                .limit(25)
                .collect(Collectors.toList());
    }

    private static List<Command.Choice> hmsTerritories(@NotNull Game game, String searchValue) {
        List<Command.Choice> returnlist = new ArrayList<>();
        Territories allTerritories = game.getTerritories();
        allTerritories.values().stream().filter(t1 -> t1.getForces().stream().anyMatch(force -> force.getName().equals("Hidden Mobile Stronghold"))).findFirst().ifPresent(t1 -> returnlist.add(new Command.Choice(t1.getTerritoryName() + " - Current, select for no move", t1.getTerritoryName())));
        if (returnlist.isEmpty())
            // Initial placement of HMS
            return allTerritories.values().stream()
                    .filter(t -> !t.isStronghold())
                    .map(Territory::getTerritoryName)
                    .filter(tn -> !game.getHomeworlds().containsValue(tn))
                    .filter(territoryName -> territoryName.toLowerCase().matches(searchRegex(searchValue.toLowerCase())))
                    .map(territoryName -> new Command.Choice(territoryName, territoryName))
                    .limit(25)
                    .collect(Collectors.toList());

        Set<String> moveableTerritories = ShipmentAndMovementButtons.getAdjacentTerritoryNames("Hidden Mobile Stronghold", 3, game)
                .stream().filter(t -> IxButtons.isNotStronghold(game, t)).collect(Collectors.toSet());
        List<Territory> territories = new ArrayList<>();
        for (String territoryName : moveableTerritories) {
            territories.addAll(game.getTerritories().values().stream()
                    .filter(t -> t.getSector() != game.getStorm())
                    .filter(t -> t.getTerritoryName().replaceAll("\\s*\\([^)]*\\)\\s*", "")
                            .equalsIgnoreCase(territoryName)
                    ).toList());
        }

        returnlist.addAll(territories.stream()
                .filter(t -> !t.isStronghold())
                .filter(t -> t.getSector() != game.getStorm())
                .map(Territory::getTerritoryName)
                .filter(territoryName -> territoryName.toLowerCase().matches(searchRegex(searchValue.toLowerCase())))
                .map(territoryName -> new Command.Choice(territoryName, territoryName))
                .limit(24)
                .toList());
        return returnlist;
    }

    private static List<Command.Choice> traitors(CommandAutoCompleteInteractionEvent event, Game game, String searchValue) {
        Faction faction = game.getFaction(event.getOptionsByName("factionname").get(0).getAsString());
        return faction.getTraitorHand().stream().map(TraitorCard::name)
                .filter(traitor -> traitor.toLowerCase().matches(searchRegex(searchValue.toLowerCase())))
                .map(traitor -> new Command.Choice(traitor, traitor))
                .collect(Collectors.toList());
    }

    private static List<Command.Choice> btFaceDancers(Game game, String searchValue) {
        if (!game.hasFaction("BT")) return new ArrayList<>();
        else {
            return game.getFaction("BT").getTraitorHand().stream()
                    .map(TraitorCard::name)
                    .filter(traitor -> traitor.toLowerCase().matches(searchRegex(searchValue.toLowerCase())))
                    .map(traitor -> new Command.Choice(traitor, traitor))
                    .collect(Collectors.toList());
        }
    }

    private static List<Command.Choice> cardsInHand(CommandAutoCompleteInteractionEvent event, Game game, String searchValue) {
        Faction faction = game.getFaction(event.getOptionsByName("factionname").get(0).getAsString());
        return faction.getTreacheryHand().stream().map(TreacheryCard::name)
                .filter(card -> card.toLowerCase().matches(searchRegex(searchValue.toLowerCase())))
                .map(card -> new Command.Choice(card, card))
                .collect(Collectors.toList());
    }

    private static List<Command.Choice> cardsInDiscard(Game game, String searchValue) {
        return game.getTreacheryDiscard().stream().map(TreacheryCard::name)
                .filter(card -> card.toLowerCase().matches(searchRegex(searchValue.toLowerCase())))
                .map(card -> new Command.Choice(card, card))
                .collect(Collectors.toList());
    }

    private static List<Command.Choice> fromTerritories(CommandAutoCompleteInteractionEvent event, Game game, String searchValue) {
        Faction faction = game.getFaction(event.getOptionsByName("factionname").get(0).getAsString());
        List<Territory> territories = new LinkedList<>();
        for (Territory territory : game.getTerritories().values()) {
            if (territory.getForce(faction.getName()).getStrength() > 0 || territory.getForce(faction.getName() + "*").getStrength() > 0
                    || (faction instanceof BGFaction && territory.getForce("Advisor").getStrength() > 0)) {
                territories.add(territory);
            }
        }
        return territories.stream().map(Territory::getTerritoryName)
                .filter(territory -> territory.toLowerCase().matches(searchRegex(searchValue.toLowerCase())))
                .map(territory -> new Command.Choice(territory, territory))
                .collect(Collectors.toList());
    }

    private static List<Command.Choice> leaders(CommandAutoCompleteInteractionEvent event, Game game, String searchValue) {
        Faction faction = game.getFaction(event.getOptionsByName("factionname").get(0).getAsString());
        return faction.getLeaders().stream().map(Leader::getName)
                .filter(leader -> leader.matches(searchRegex(searchValue)))
                .map(leader -> new Command.Choice(leader, leader))
                .collect(Collectors.toList());
    }

    private static List<Command.Choice> factionLeaderSkill(CommandAutoCompleteInteractionEvent event, Game game, String searchValue) {
        Faction faction = game.getFaction(event.getOptionsByName("factionname").get(0).getAsString());
        return faction.getLeaderSkillsHand().stream().map(LeaderSkillCard::name)
                .filter(leaderSkillCardName -> leaderSkillCardName.matches(searchRegex(searchValue)))
                .map(leaderSkillCardName -> new Command.Choice(leaderSkillCardName, leaderSkillCardName))
                .collect(Collectors.toList());
    }

    private static List<Command.Choice> nonHarkLeaders(Game game, String searchValue) {
        Faction faction = game.getFaction("Harkonnen");
        List<String> harkLeaders = new LinkedList<>();
        harkLeaders.add("Feyd Rautha");
        harkLeaders.add("Beast Rabban");
        harkLeaders.add("Piter de Vries");
        harkLeaders.add("Cpt. Iakin Nefud");
        harkLeaders.add("Umman Kudu");
        return faction.getLeaders().stream().map(Leader::getName)
                .filter(leader -> !harkLeaders.contains(leader))
                .filter(leader -> leader.matches(searchRegex(searchValue)))
                .map(leader -> new Command.Choice(leader, leader))
                .collect(Collectors.toList());
    }

    private static List<Command.Choice> reviveLeaders(Game game, String searchValue) {
        return game.getLeaderTanks().stream().map(Leader::getName)
                .filter(leader -> leader.matches(searchRegex(searchValue)))
                .map(leader -> new Command.Choice(leader, leader))
                .collect(Collectors.toList());
    }

    private static List<Command.Choice> combatLeaders(CommandAutoCompleteInteractionEvent event, DiscordGame discordGame, String searchValue) throws ChannelNotFoundException {
        Faction faction = discordGame.getFactionByPlayer(event.getUser().toString());
        Battles battles = null;
        try {
            battles = discordGame.getGame().getBattles();
        } catch (InvalidGameStateException e) {
            // Battle territory not found. Continue without filtering leaders in other territories.
        }
        List<String> battleTerritoryNames = battles == null ? new ArrayList<>() : battles.getCurrentBattle().getTerritorySectors().stream().map(Territory::getTerritoryName).toList();
        List<Command.Choice> choices = new ArrayList<>();
        if (faction.getLeaders().stream().filter(leader -> !leader.getName().equals("Kwisatz Haderach")).toList().isEmpty()) choices.add(new Command.Choice("None", "None"));
        choices.addAll(faction.getLeaders().stream()
                .filter(leader -> leader.getBattleTerritoryName() == null || battleTerritoryNames.stream().anyMatch(n -> n.equals(leader.getBattleTerritoryName())))
                .filter(l -> l.getSkillCard() == null || l.isPulledBehindShield())
                .map(Leader::getName)
                .filter(leader -> !leader.equals("Kwisatz Haderach"))
                .filter(leader -> leader.matches(searchRegex(searchValue)))
                .map(leader -> new Command.Choice(leader, leader))
                .toList()
        );
        choices.addAll(faction.getTreacheryHand().stream()
                .map(TreacheryCard::name)
                .filter(card -> card.startsWith("Cheap Hero"))
                .filter(card -> card.toLowerCase().matches(searchRegex(searchValue.toLowerCase())))
                .map(card -> new Command.Choice(card, card))
                .toList()
        );
        return choices;
    }

    private static List<Command.Choice> removeLeaders(CommandAutoCompleteInteractionEvent event, Game game, String searchValue) {
        Faction faction = game.getFaction(event.getOptionsByName("factionname").get(0).getAsString());
        return faction.getLeaders().stream()
                .filter(l -> l.getBattleTerritoryName() != null)
                .filter(l -> l.getName().matches(searchRegex(searchValue)))
                .map(l -> new Command.Choice(l.getName() + " from " + l.getBattleTerritoryName(), l.getName()))
                .collect(Collectors.toList());
    }

    private static List<Command.Choice> weapon(CommandAutoCompleteInteractionEvent event, DiscordGame discordGame, String searchValue) throws ChannelNotFoundException {
        Faction faction = discordGame.getFactionByPlayer(event.getUser().toString());
        List<Command.Choice> choices = new ArrayList<>();
        choices.add(new Command.Choice("None", "None"));
        choices.addAll(faction.getTreacheryHand().stream()
                .filter(c -> c.type().startsWith("Weapon") || c.type().equals("Worthless Card"))
                .map(TreacheryCard::name)
                .filter(card -> card.toLowerCase().matches(searchRegex(searchValue.toLowerCase())))
                .map(card -> new Command.Choice(card, card))
                .toList()
        );
        choices.addAll(faction.getTreacheryHand().stream()
                .map(TreacheryCard::name)
                .filter(name -> name.equals("Chemistry"))
                .filter(card -> card.toLowerCase().matches(searchRegex(searchValue.toLowerCase())))
                .map(card -> new Command.Choice("Chemistry (only with another Defense)", card))
                .toList()
        );
        if (faction.hasSkill("Planetologist")) {
            choices.addAll(faction.getTreacheryHand().stream()
                    .filter(c -> c.type().startsWith("Special") && !c.name().startsWith("Cheap Hero") || c.type().equals("Spice Blow - Special"))
                    .map(TreacheryCard::name)
                    .filter(card -> card.toLowerCase().matches(searchRegex(searchValue.toLowerCase())))
                    .map(card -> new Command.Choice(card, card))
                    .toList()
            );
        } else {
            choices.addAll(faction.getTreacheryHand().stream()
                    .map(TreacheryCard::name)
                    .filter(name -> name.equals("Harass and Withdraw"))
                    .filter(card -> card.toLowerCase().matches(searchRegex(searchValue.toLowerCase())))
                    .map(card -> new Command.Choice("Harass and Withdraw (not on your Homeworld)", card))
                    .toList()
            );
            if (faction.getReserves().getStrength() + faction.getSpecialReserves().getStrength() >= 3)
                choices.addAll(faction.getTreacheryHand().stream()
                        .map(TreacheryCard::name)
                        .filter(name -> name.equals("Reinforcements"))
                        .filter(card -> card.toLowerCase().matches(searchRegex(searchValue.toLowerCase())))
                        .map(card -> new Command.Choice(card, card))
                        .toList()
                );
        }
        return choices;
    }

    private static List<Command.Choice> defense(CommandAutoCompleteInteractionEvent event, DiscordGame discordGame, String searchValue) throws ChannelNotFoundException {
        Faction faction = discordGame.getFactionByPlayer(event.getUser().toString());
        List<Command.Choice> choices = new ArrayList<>();
        choices.add(new Command.Choice("None", "None"));
        choices.addAll(faction.getTreacheryHand().stream()
                .filter(c -> c.type().startsWith("Defense") || c.type().equals("Worthless Card"))
                .map(TreacheryCard::name)
                .filter(card -> card.toLowerCase().matches(searchRegex(searchValue.toLowerCase())))
                .map(card -> new Command.Choice(card, card))
                .toList()
        );
        choices.addAll(faction.getTreacheryHand().stream()
                .map(TreacheryCard::name)
                .filter(name -> name.equals("Weirding Way"))
                .filter(card -> card.toLowerCase().matches(searchRegex(searchValue.toLowerCase())))
                .map(card -> new Command.Choice("Weirding Way (only with another Weapon)", card))
                .toList()
        );
        choices.addAll(faction.getTreacheryHand().stream()
                .map(TreacheryCard::name)
                .filter(name -> name.equals("Harass and Withdraw"))
                .filter(card -> card.toLowerCase().matches(searchRegex(searchValue.toLowerCase())))
                .map(card -> new Command.Choice("Harass and Withdraw (not on your Homeworld)", card))
                .toList()
        );
        if (faction.getReserves().getStrength() + faction.getSpecialReserves().getStrength() >= 3)
            choices.addAll(faction.getTreacheryHand().stream()
                    .map(TreacheryCard::name)
                    .filter(name -> name.equals("Reinforcements"))
                    .filter(card -> card.toLowerCase().matches(searchRegex(searchValue.toLowerCase())))
                    .map(card -> new Command.Choice(card, card))
                    .toList()
            );
        return choices;
    }

    private static List<Command.Choice> bgTerritories(Game game, String searchValue) {
        List<Territory> territories = new LinkedList<>();
        for (Territory territory : game.getTerritories().values()) {
            if (territory.getForce("Advisor").getStrength() > 0 || territory.getForce("BG").getStrength() > 0) {
                territories.add(territory);
            }
        }
        return territories.stream().map(Territory::getTerritoryName)
                .filter(territory -> territory.toLowerCase().matches(searchRegex(searchValue.toLowerCase())))
                .map(territory -> new Command.Choice(territory, territory))
                .collect(Collectors.toList());
    }

    private static String searchRegex(String searchValue) {
        StringBuilder searchRegex = new StringBuilder();
        searchRegex.append(".*");

        for (char c : searchValue.toCharArray()) {
            searchRegex.append(c).append(".*");
        }
        return searchRegex.toString();
    }

    private static List<Command.Choice> ixCardsInHand(Game game, String searchValue) {
        Faction faction = game.getFaction("Ix");
        return faction.getTreacheryHand().stream().map(TreacheryCard::name)
                .filter(card -> card.toLowerCase().matches(searchRegex(searchValue.toLowerCase())))
                .map(card -> new Command.Choice(card, card))
                .collect(Collectors.toList());
    }

    private static List<Command.Choice> cardsInMarket(Game game, String searchValue) {
        try {
            return game.getBidding().getMarket().stream().map(TreacheryCard::name)
                    .filter(card -> card.toLowerCase().matches(searchRegex(searchValue.toLowerCase())))
                    .map(card -> new Command.Choice(card, card))
                    .collect(Collectors.toList());
        } catch (InvalidGameStateException e) {
            return new LinkedList<>();
        }
    }

    private static List<Command.Choice> richeseCard(Game game, String searchValue) {
        if (game.hasFaction("Richese")) {
            RicheseFaction faction = (RicheseFaction) game.getFaction("Richese");
            List<TreacheryCard> cards = faction.getTreacheryCardCache();

            return cards.stream().map(TreacheryCard::name)
                    .filter(card -> card.toLowerCase().matches(searchRegex(searchValue.toLowerCase())))
                    .map(card -> new Command.Choice(card, card))
                    .collect(Collectors.toList());
        } else {
            return new ArrayList<>();
        }
    }

    private static List<Command.Choice> richeseBlackMarketCard(Game game, String searchValue) {
        if (game.hasFaction("Richese")) {
            Faction faction = game.getFaction("Richese");
            List<TreacheryCard> cards = faction.getTreacheryHand();

            return cards.stream().map(TreacheryCard::name)
                    .filter(card -> card.toLowerCase().matches(searchRegex(searchValue.toLowerCase())))
                    .map(card -> new Command.Choice(card, card))
                    .collect(Collectors.toList());
        } else {
            return new ArrayList<>();
        }
    }

    private static List<Command.Choice> getAddGameOptions(Game game, String searchValue) {
        Set<GameOption> selectedGameOptions = game.getGameOptions();
        Set<GameOption> allGameOptions = new HashSet<>(Arrays.asList(GameOption.values()));

        if (selectedGameOptions != null) {
            for (GameOption selectedGameOption : selectedGameOptions) {
                allGameOptions.remove(selectedGameOption);
            }
        }

        return gameOptionsToChoices(allGameOptions.stream().toList(), searchValue);
    }

    private static List<Command.Choice> getRemoveGameOptions(Game game, String searchValue) {
        Set<GameOption> gameOptions = game.getGameOptions();

        if (gameOptions != null) {
            return gameOptionsToChoices(gameOptions.stream().toList(), searchValue);
        } else {
            return new ArrayList<>();
        }
    }

    private static List<Command.Choice> getGameStates(DiscordGame discordGame, String searchValue) throws ChannelNotFoundException {
        MessageChannel botDataChannel = discordGame.getTextChannel("bot-data");

        String latestMessageId = botDataChannel.getLatestMessageId();
        MessageHistory messageHistory = botDataChannel.getHistoryBefore(latestMessageId, 25).complete();
        List<Message> messages = messageHistory.getRetrievedHistory();


        return IntStream.range(0, messageHistory.size())
                .filter(i -> messages.get(i).getContentDisplay().toLowerCase().matches(searchRegex(searchValue.toLowerCase())))
                .mapToObj(i -> new Command.Choice(
                        StringUtils.left(i + " - " + messages.get(i).getTimeCreated() + " - " +
                                messages.get(i).getContentDisplay(), 100),
                        messages.get(i).getId()
                ))
                .toList();
    }

    private static List<Command.Choice> gameOptionsToChoices(List<GameOption> list, String searchValue) {
        return list.stream().map(Enum::name)
                .filter(e -> e.toLowerCase().matches(searchRegex(searchValue.toLowerCase())))
                .map(e -> new Command.Choice(e, e))
                .toList();
    }

    private static List<Command.Choice> getEcazAmbassadorsOnMap(DiscordGame discordGame, String searchValue) throws ChannelNotFoundException {
        Game game = discordGame.getGame();

        return game.getTerritories().values().stream().filter(t -> t.getEcazAmbassador() != null)
                .map(t -> new Command.Choice(
                        t.getEcazAmbassador() + " in " + t.getTerritoryName(),
                        t.getEcazAmbassador()
                ))
                .filter(t -> t.getName().toLowerCase().matches(searchRegex(searchValue.toLowerCase())))
                .toList();
    }

    private static List<Command.Choice> getMoritaniTerrorTokensOnMap(DiscordGame discordGame, String searchValue) throws ChannelNotFoundException {
        Game game = discordGame.getGame();

        return game.getTerritories().values().stream().filter(t -> t.getTerrorTokens() != null  && !t.getTerrorTokens().isEmpty())
                .flatMap(t -> t.getTerrorTokens().stream().map(tt -> new Command.Choice(
                        tt + " in " + t.getTerritoryName(),
                        tt
                )))
                .filter(t -> t.getName().toLowerCase().matches(searchRegex(searchValue.toLowerCase())))
                .toList();
    }

}
