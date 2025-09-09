package controller.commands;

import controller.DiscordGame;
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
    public static final OptionData gameName = new OptionData(OptionType.STRING, "name", "e.g. 'Dune Discord #5: The Tortoise and the Hajr'", true);
    public static final OptionData gameRole = new OptionData(OptionType.ROLE, "gamerole", "The role you created for the players of this game", true);
    public static final OptionData modRole = new OptionData(OptionType.ROLE, "modrole", "The role you created for the mod(s) of this game", true);
    public static final OptionData teamModSwitch = new OptionData(OptionType.BOOLEAN, "enable-team-mod", "True enables team mod, False disables", true);
    public static final OptionData user = new OptionData(OptionType.USER, "player", "The player for the faction", true);
    public static final OptionData allFactions = new OptionData(OptionType.STRING, "faction", "The faction", true)
            .addChoice("Atreides", "Atreides")
            .addChoice("Harkonnen", "Harkonnen")
            .addChoice("Emperor", "Emperor")
            .addChoice("Fremen", "Fremen")
            .addChoice("Guild", "Guild")
            .addChoice("BG", "BG")
            .addChoice("Ixian", "Ix")
            .addChoice("BT", "BT")
            .addChoice("CHOAM", "CHOAM")
            .addChoice("Richese", "Richese")
            .addChoice("Ecaz", "Ecaz")
            .addChoice("Moritani", "Moritani");
    public static final OptionData faction = new OptionData(OptionType.STRING, "factionname", "The faction", true)
            .setAutoComplete(true);
    public static final OptionData factionOrTanks = new OptionData(OptionType.STRING, "faction-or-tanks", "Holder of the leader to kill or flip face down", true)
            .setAutoComplete(true);
    public static final OptionData otherFaction = new OptionData(OptionType.STRING, "other-factionname", "The Other faction", true)
            .setAutoComplete(true);
    public static final OptionData karamaFaction = new OptionData(OptionType.STRING, "karama-faction", "The faction playing Karama", true)
            .setAutoComplete(true);
    public static final OptionData starredForcesFaction = new OptionData(OptionType.STRING, "starred-forces-faction", "The faction whose starred forces will be negated", true)
            .setAutoComplete(true);
    public static final OptionData otherWinnerFaction = new OptionData(OptionType.STRING, "other-winner", "The allied faction that also won", false)
            .setAutoComplete(true);
    public static final OptionData whisperFaction = new OptionData(OptionType.STRING, "whisper-recipient", "The faction you want to whisper to. Omit in -whisper threads to reply.", false)
            .setAutoComplete(true);
    public static final OptionData homebrewFactionName = new OptionData(OptionType.STRING, "name", "The name of the homebrew faction", true);
    public static final OptionData dotPosition = new OptionData(OptionType.INTEGER, "dot-position", "1 = dot in sector 1, then the others in storm order", true);
    public static final OptionData turn = new OptionData(OptionType.INTEGER, "turn", "The turn number.", true);
    public static final OptionData guildSpecialWin = new OptionData(OptionType.BOOLEAN, "guild-special", "Was this a Guild special victory condition?", false);
    public static final OptionData fremenSpecialWin = new OptionData(OptionType.BOOLEAN, "fremen-special", "Was this a Fremen special victory condition?", false);
    public static final OptionData bgPredictionWin = new OptionData(OptionType.BOOLEAN, "bg-prediction", "Was this a BG prediction win?", false);
    public static final OptionData ecazOccupyWin = new OptionData(OptionType.BOOLEAN, "ecaz-occupy", "Was this an Ecaz-ally occupy win?", false);
    public static final OptionData amount = new OptionData(OptionType.INTEGER, "amount", "Amount", true);
    public static final OptionData message = new OptionData(OptionType.STRING, "message", "Message for spice transactions", true);
    public static final OptionData reason = new OptionData(OptionType.STRING, "reason", "description of the bribe", false);
    public static final OptionData card = new OptionData(OptionType.STRING, "card", "The card.", true).setAutoComplete(true);
    public static final OptionData allyCard = new OptionData(OptionType.STRING, "ally-card", "The ally's card to swap for.", true).setAutoComplete(true);
    public static final OptionData discardCard = new OptionData(OptionType.STRING, "card-discard", "The card.", true).setAutoComplete(true);

    public static final OptionData ixCard = new OptionData(OptionType.STRING, "ixcard", "The card.", true).setAutoComplete(true);
    public static final OptionData putBackCard = new OptionData(OptionType.STRING, "putbackcard", "The card.", true).setAutoComplete(true);
    public static final OptionData topOrBottom = new OptionData(OptionType.STRING, "top-or-bottom", "Top or Bottom of treachery deck", true)
            .addChoice("Top", "top")
            .addChoice("Bottom", "bottom");
    public static final OptionData recipient = new OptionData(OptionType.STRING, "recipient", "The recipient", true).setAutoComplete(true);
    public static final OptionData traitor = new OptionData(OptionType.STRING, "traitor", "The name of the traitor", true).setAutoComplete(true);
    public static final OptionData territory = new OptionData(OptionType.STRING, "territory", "The name of the territory", true).setAutoComplete(true);
    public static final OptionData sandTerritory = new OptionData(OptionType.STRING, "sand-territory", "The name of the territory", true).setAutoComplete(true);
    public static final OptionData hmsTerritory = new OptionData(OptionType.STRING, "hms-territory", "The name of the territory", true).setAutoComplete(true);
    public static final OptionData dialOne = new OptionData(OptionType.INTEGER, "dial-one", "The dial of the first player", true);
    public static final OptionData dialTwo = new OptionData(OptionType.INTEGER, "dial-two", "The dial of the second player", true);
    public static final OptionData starred = new OptionData(OptionType.BOOLEAN, "starred", "Are they starred forces?", true);
    public static final OptionData paid = new OptionData(OptionType.BOOLEAN, "paid", "Is the action paid for?", true);
    public static final OptionData firstWorm = new OptionData(OptionType.BOOLEAN, "first-worm", "Is this the first worm on the spice deck?", false);

    public static final OptionData spent = new OptionData(OptionType.INTEGER, "spent", "How much was spent on the card.", true);
    public static final OptionData revived = new OptionData(OptionType.INTEGER, "revived", "How many are being revived.", true);
    public static final OptionData sectors = new OptionData(OptionType.INTEGER, "sectors", "Number of sectors to move storm", true);
    public static final OptionData sector = new OptionData(OptionType.INTEGER, "sector", "The sector on the board", true);
    public static final OptionData data = new OptionData(OptionType.STRING, "data", "What data to display", true)
            .addChoice("Territories and Tanks", "territories")
            .addChoice("Treachery Deck and Discard", "treachery")
            .addChoice("Spice Deck and Discards", "spice")
            .addChoice("Other Decks and Discards", "dnd")
            .addChoice("Phase, Turn, and everything else", "etc")
            .addChoice("Faction Info", "factions");
    public static final OptionData isShipment = new OptionData(OptionType.BOOLEAN, "is-shipment", "Is this placement a shipment?", true);
    public static final OptionData canTrigger = new OptionData(OptionType.BOOLEAN, "can-trigger", "Can this placement trigger Ambassadors and Terror Tokens?", true);
    public static final OptionData ecazAllyNoField = new OptionData(OptionType.BOOLEAN, "is-ecaz-ally", "True if Ecaz ally shipped the No-Field", false);
    public static final OptionData toTanks = new OptionData(OptionType.BOOLEAN, "to-tanks", "Remove these forces to the tanks (true) or to reserves (false)?", true);
    public static final OptionData killedInBattle = new OptionData(OptionType.BOOLEAN, "killed-in-battle", "For Atreides KH counter", true);
    public static final OptionData leader = new OptionData(OptionType.STRING, "leadertokill", "The leader.", true).setAutoComplete(true);
    public static final OptionData faceDown = new OptionData(OptionType.BOOLEAN, "face-down", "Put leader face down in the tanks", false);
    public static final OptionData reviveLeader = new OptionData(OptionType.STRING, "leader-to-revive", "The leader.", true).setAutoComplete(true);
    public static final OptionData revivalCost = new OptionData(OptionType.INTEGER, "revival-cost", "How much spent to revive if different than leader value", false);
    public static final OptionData combatLeader = new OptionData(OptionType.STRING, "combat-leader", "The leader or None.", true).setAutoComplete(true);
    public static final OptionData removeLeader = new OptionData(OptionType.STRING, "leader-to-remove", "The leader incorrectly in a territory.", true).setAutoComplete(true);
    public static final OptionData combatDial = new OptionData(OptionType.STRING, "combat-dial", "The dial on the battle wheel.", true);
    public static final OptionData combatSpice = new OptionData(OptionType.INTEGER, "combat-spice", "Spice used for backing troops", true);
    public static final OptionData weapon = new OptionData(OptionType.STRING, "weapon", "Weapon or Worthless.", true).setAutoComplete(true);
    public static final OptionData defense = new OptionData(OptionType.STRING, "defense", "Defense or Worthless.", true).setAutoComplete(true);
    public static final OptionData hmsStrongoldCard = new OptionData(OptionType.STRING, "hms-stronghold-card", "The Stronghold Card power to use for HMS", true).setAutoComplete(true);
    public static final OptionData spiceBankerPayment = new OptionData(OptionType.INTEGER, "spice-banker-payment", "Spice spent for Spice Banker support", true);
    public static final OptionData deactivatePoisonTooth = new OptionData(OptionType.BOOLEAN, "deactivate-poison-tooth", "Allow battle plan resolution with Poison Tooth not used (default = False)", false);
    public static final OptionData addPortableSnooper = new OptionData(OptionType.BOOLEAN, "add-portable-snooper", "Allow battle plan resolution with Portable Snooper added (default = False)", false);
    public static final OptionData stoneBurnerDoesNotKill = new OptionData(OptionType.BOOLEAN, "stone-burner-does-not-kill", "Prevent Stone Burner from killing leaders (default = False)", false);
    public static final OptionData useJuiceOfSapho = new OptionData(OptionType.BOOLEAN, "use-juice-of-sapho", "Use Juice of Sapho", false);
    public static final OptionData aggressorTraitor = new OptionData(OptionType.BOOLEAN, "aggressor-traitor", "Aggressor calls Traitor", false);
    public static final OptionData defenderTraitor = new OptionData(OptionType.BOOLEAN, "defender-traitor", "Defender calls Traitor", false);
    public static final OptionData forceResolution = new OptionData(OptionType.BOOLEAN, "force-resolution", "Override outstanding player decisions and print resolution", false);
    public static final OptionData fromTerritory = new OptionData(OptionType.STRING, "from", "The territory.", true).setAutoComplete(true);
    public static final OptionData toTerritory = new OptionData(OptionType.STRING, "to", "Moving to this territory.", true).setAutoComplete(true);
    public static final OptionData homeworld = new OptionData(OptionType.STRING, "homeworld", "The homeworld.", true).setAutoComplete(true);
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
    public static final OptionData atreidesKaramad = new OptionData(OptionType.BOOLEAN, "prescience-blocked", "If someone Karama blocked Atreides card prescience", false);
    public static final OptionData paidToFaction =
            new OptionData(OptionType.STRING, "paid-to-faction", "Which faction is bidding paid to.", false)
                    .setAutoComplete(true);
    public static final OptionData harkonnenKaramad = new OptionData(OptionType.BOOLEAN, "no-bonus-card", "If someone Karamas Harkonnen's bonus card", false);
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
    public static final OptionData originalSixFactions = waitingListOptionData("o6", "Choose Preferred for an Original 6 factions game.");
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

    public static final OptionData months = new OptionData(OptionType.INTEGER, "months", "List players from games that finished within this many months");
    public static final OptionData numFastGamesForAverageDuration = new OptionData(OptionType.INTEGER, "fastest-games", "Number of fastest games (default = 3)");
    public static final OptionData minTurnsForAverageDuration = new OptionData(OptionType.INTEGER, "min-turns", "Minimum number of turns (default = 3)");

    public static final OptionData clockDirection =
            new OptionData(OptionType.STRING, "clock-direction", "Clockwise is default", false)
                    .addChoice("Clockwise", "CW")
                    .addChoice("Counterclockwise", "CCW");

    public static final OptionData ecazAmbassadorsInSupply =
            new OptionData(OptionType.STRING, "ecaz-ambassador-in-supply", "Ecaz Ambassador Token in Ecaz supply", true)
                    .setAutoComplete(true);
    public static final OptionData strongholdWithoutAmbassador = new OptionData(OptionType.STRING, "stronghold-without-ambassador", "The stronghold to receive the ambassador", true).setAutoComplete(true);
    public static final OptionData ambassadorCost = new OptionData(OptionType.INTEGER, "ambassador-cost", "Cost for placing the ambassador", false);

    public static final OptionData ecazAmbassadorsOnMap =
            new OptionData(OptionType.STRING, "ecaz-ambassador-on-map", "Ecaz Ambassador Token on the map", true)
                    .setAutoComplete(true);

    public static final OptionData moritaniTerrorTokenInSupply =
            new OptionData(OptionType.STRING, "moritani-terror-token-in-supply", "Moritani Terror Token in Moritani supply", true)
                    .setAutoComplete(true);

    public static final OptionData moritaniTerrorTokenOnMap =
            new OptionData(OptionType.STRING, "moritani-terror-token-on-map", "Moritani Terror Token on the map", true)
                    .setAutoComplete(true);

    public static final OptionData toPlayer =
            new OptionData(OptionType.BOOLEAN, "to-player", "Return to the player's supply", true);

    public static final OptionData showFactions =
            new OptionData(OptionType.BOOLEAN, "show-factions", "Show factions in each game", false);
    public static final OptionData showUserNames =
            new OptionData(OptionType.BOOLEAN, "show-user-names", "Show user names in each game", false);

    public static final OptionData forcePublish =
            new OptionData(OptionType.BOOLEAN, "force-publish", "Publish stats even if no new games were added", false);
    public static final OptionData statsFileOnly =
            new OptionData(OptionType.BOOLEAN, "stats-file-only", "Publish only the JSON file in parsed-results", false);

    public static List<Command.Choice> getCommandChoices(CommandAutoCompleteInteractionEvent event, DiscordGame discordGame, Game game) throws ChannelNotFoundException {
        String optionName = event.getFocusedOption().getName();
        String searchValue = event.getFocusedOption().getValue();

        List<Command.Choice> choices = new ArrayList<>();

        switch (optionName) {
            case "factionname", "other-factionname", "sender", "recipient", "paid-to-faction", "karama-faction", "other-winner", "whisper-recipient" ->
                    choices = factions(game, searchValue);
            case "faction-or-tanks" -> choices = factionsOrTanks(game, searchValue);
            case "starred-forces-faction" -> choices = starredForcesFactions(game, searchValue);
            case "territory", "to" -> choices = territories(game, searchValue);
            case "sand-territory" -> choices = sandTerritories(game, searchValue);
            case "homeworld" -> choices = homeworldNames(game, searchValue);
            case "hms-territory" -> choices = hmsTerritories(game, searchValue);
            case "traitor" -> choices = traitors(event, game, searchValue);
            case "card" -> choices = cardsInHand(event, game, searchValue);
            case "ally-card" -> choices = allyCardsInHand(game, searchValue);
            case "card-discard" -> choices = cardsInDiscard(game, searchValue);
            case "ixcard" -> choices = ixCardsInHand(game, searchValue);
            case "putbackcard" -> choices = cardsInMarket(game, searchValue);
            case "from" -> choices = fromTerritories(event, game, searchValue);
            case "bgterritories" -> choices = bgTerritories(game, searchValue);
            case "factionleader" -> choices = leaders(event, game, searchValue);
            case "leadertokill" -> choices = leadersToKillOrFlip(event, game, searchValue);
            case "leader-to-revive" -> choices = reviveLeaders(event, game, searchValue);
            case "combat-leader" -> choices = combatLeaders(event, discordGame, game, searchValue);
            case "leader-to-remove" -> choices = removeLeaders(event, game, searchValue);
            case "weapon" -> choices = weapon(event, discordGame, searchValue);
            case "defense" -> choices = defense(event, discordGame, searchValue);
            case "hms-stronghold-card" -> choices = hmsStrongoldCardChoices(discordGame, searchValue);
            case "factionleaderskill" -> choices = factionLeaderSkill(event, game, searchValue);
            case "richese-card" -> choices = richeseCard(game, searchValue);
            case "bt-face-dancer" -> choices = btFaceDancers(game, searchValue);
            case "richese-black-market-card" -> choices = richeseBlackMarketCard(game, searchValue);
            case "add-game-option" -> choices = getAddGameOptions(game, searchValue);
            case "remove-game-option" -> choices = getRemoveGameOptions(game, searchValue);
            case "returning" -> choices = nonHarkLeaders(game, searchValue);
            case "game-state" -> choices = getGameStates(discordGame, searchValue);
            case "ecaz-ambassador-in-supply" -> choices = getEcazAmbassadorsInSupply(discordGame, searchValue);
            case "stronghold-without-ambassador" -> choices = strongholdWithoutAmbassaddor(game, searchValue);
            case "ecaz-ambassador-on-map" -> choices = getEcazAmbassadorsOnMap(discordGame, searchValue);
            case "moritani-terror-token-in-supply" -> choices = getMoritaniTerrorTokensInSupply(discordGame, searchValue);
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

    private static List<Command.Choice> factionsOrTanks(@NotNull Game game, String searchValue) {
        List<Command.Choice> choices = factions(game, searchValue);
        if (!game.getLeaderTanks().isEmpty())
            choices.add(new Command.Choice("Tanks", "Tanks"));
        return choices;
    }

    private static List<Command.Choice> starredForcesFactions(@NotNull Game game, String searchValue) {
        return game.getFactions().stream()
                .filter(f -> f instanceof EmperorFaction || f instanceof FremenFaction || f instanceof IxFaction)
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

    private static List<Command.Choice> sandTerritories(@NotNull Game game, String searchValue) {
        return game.getTerritories().values().stream()
                .filter(t -> !t.isRock())
                .filter(t -> !(t instanceof HomeworldTerritory))
                .filter(t -> !t.isDiscoveryToken())
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
                    .filter(t -> !(t instanceof HomeworldTerritory))
                    .map(Territory::getTerritoryName)
                    .filter(territoryName -> territoryName.toLowerCase().matches(searchRegex(searchValue.toLowerCase())))
                    .map(territoryName -> new Command.Choice(territoryName, territoryName))
                    .limit(25)
                    .collect(Collectors.toList());

        Set<String> moveableTerritories = ShipmentAndMovementButtons.getAdjacentTerritoryNames("Hidden Mobile Stronghold", 3, game)
                .stream().filter(t -> game.getTerritories().isNotStronghold(t)).collect(Collectors.toSet());
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

    private static List<Command.Choice> homeworldNames(@NotNull Game game, String searchValue) {
        return game.getTerritories().values().stream()
                .filter(t -> t instanceof HomeworldTerritory)
                .map(Territory::getTerritoryName)
                .filter(territoryName -> territoryName.toLowerCase().matches(searchRegex(searchValue.toLowerCase())))
                .map(territoryName -> new Command.Choice(territoryName, territoryName))
                .limit(25)
                .collect(Collectors.toList());
    }

    private static List<Command.Choice> traitors(CommandAutoCompleteInteractionEvent event, Game game, String searchValue) {
        Faction faction;
        if (event.getSubcommandName() != null && event.getSubcommandName().equals("nexus-card-lose-traitor"))
            faction = game.getHarkonnenFaction();
        else
            faction = game.getFaction(event.getOptionsByName("factionname").getFirst().getAsString());
        return faction.getTraitorHand().stream().map(TraitorCard::getName)
                .filter(traitor -> traitor.toLowerCase().matches(searchRegex(searchValue.toLowerCase())))
                .map(traitor -> new Command.Choice(traitor, traitor))
                .collect(Collectors.toList());
    }

    private static List<Command.Choice> btFaceDancers(Game game, String searchValue) {
        BTFaction bt = game.getBTFactionOrNull();
        if (bt == null)
            return new ArrayList<>();
        else {
            return bt.getTraitorHand().stream()
                    .map(TraitorCard::getName)
                    .filter(traitor -> traitor.toLowerCase().matches(searchRegex(searchValue.toLowerCase())))
                    .map(traitor -> new Command.Choice(traitor, traitor))
                    .collect(Collectors.toList());
        }
    }

    private static List<Command.Choice> cardsInHand(CommandAutoCompleteInteractionEvent event, Game game, String searchValue) {
        Faction faction;
        if (event.getSubcommandName() != null && event.getSubcommandName().equals("robbery-discard"))
            faction = game.getMoritaniFaction();
        else if (event.getSubcommandName() != null && event.getSubcommandName().equals("swap-card-with-ally"))
            faction = game.getCHOAMFaction();
        else
            faction = game.getFaction(event.getOptionsByName("factionname").getFirst().getAsString());
        return faction.getTreacheryHand().stream().map(TreacheryCard::name)
                .filter(card -> card.toLowerCase().matches(searchRegex(searchValue.toLowerCase())))
                .map(card -> new Command.Choice(card, card))
                .collect(Collectors.toList());
    }

    private static List<Command.Choice> allyCardsInHand(Game game, String searchValue) {
        Faction faction = game.getFaction(game.getCHOAMFaction().getAlly());
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
        Faction faction = game.getFaction(event.getOptionsByName("factionname").getFirst().getAsString());
        List<Territory> territories = game.getTerritories().values().stream().filter(territory -> territory.getTotalForceCount(faction) > 0).toList();
        return territories.stream().map(Territory::getTerritoryName)
                .filter(territory -> territory.toLowerCase().matches(searchRegex(searchValue.toLowerCase())))
                .map(territory -> new Command.Choice(territory, territory))
                .collect(Collectors.toList());
    }

    private static List<Command.Choice> leaders(CommandAutoCompleteInteractionEvent event, Game game, String searchValue) {
        Faction faction = game.getFaction(event.getOptionsByName("factionname").getFirst().getAsString());
        return faction.getLeaders().stream().map(Leader::getName)
                .filter(leader -> leader.matches(searchRegex(searchValue)))
                .map(leader -> new Command.Choice(leader, leader))
                .collect(Collectors.toList());
    }

    private static List<Command.Choice> leadersToKillOrFlip(CommandAutoCompleteInteractionEvent event, Game game, String searchValue) {
        String holder = event.getOptionsByName("faction-or-tanks").getFirst().getAsString();
        if (holder.equals("Tanks")) {
            return game.getLeaderTanks().stream().map(Leader::getName)
                    .filter(leader -> leader.matches(searchRegex(searchValue)))
                    .map(leader -> new Command.Choice(leader, leader))
                    .collect(Collectors.toList());
        } else {
            Faction faction = game.getFaction(holder);
            return faction.getLeaders().stream().map(Leader::getName)
                    .filter(leader -> leader.matches(searchRegex(searchValue)))
                    .map(leader -> new Command.Choice(leader, leader))
                    .collect(Collectors.toList());
        }
    }

    private static List<Command.Choice> factionLeaderSkill(CommandAutoCompleteInteractionEvent event, Game game, String searchValue) {
        Faction faction = game.getFaction(event.getOptionsByName("factionname").getFirst().getAsString());
        return faction.getLeaderSkillsHand().stream().map(LeaderSkillCard::name)
                .filter(leaderSkillCardName -> leaderSkillCardName.matches(searchRegex(searchValue)))
                .map(leaderSkillCardName -> new Command.Choice(leaderSkillCardName, leaderSkillCardName))
                .collect(Collectors.toList());
    }

    private static List<Command.Choice> nonHarkLeaders(Game game, String searchValue) {
        Faction faction = game.getHarkonnenFaction();
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

    private static List<Command.Choice> reviveLeaders(CommandAutoCompleteInteractionEvent event, Game game, String searchValue) {
        Faction faction = game.getFaction(event.getOptionsByName("factionname").getFirst().getAsString());
        if (faction instanceof BTFaction)
            return game.getLeaderTanks().stream()
                    .filter(l -> l.getName().matches(searchRegex(searchValue)))
                    .filter(l -> !(l.getName().equals("Kwisatz Haderach") || l.getName().equals("Auditor")))
                    .map(l -> new Command.Choice(l.getNameAndValueString(), l.getName()))
                    .collect(Collectors.toList());
        else
            return game.getLeaderTanks().stream()
                    .filter(l -> l.getName().matches(searchRegex(searchValue)))
                    .filter(l -> l.getOriginalFactionName().equals(faction.getName()))
                    .map(l -> new Command.Choice(l.getNameAndValueString(), l.getName()))
                    .collect(Collectors.toList());
    }

    private static List<Command.Choice> combatLeaders(CommandAutoCompleteInteractionEvent event, DiscordGame discordGame, Game game, String searchValue) throws ChannelNotFoundException {
        Faction faction = discordGame.getFactionByPlayer(event.getUser().toString());
        Battles battles = null;
        try {
            battles = discordGame.getGame().getBattles();
        } catch (InvalidGameStateException e) {
            // Battle territory not found. Continue without filtering leaders in other territories.
        }
        List<String> battleTerritoryNames = battles == null ? new ArrayList<>() : battles.getCurrentBattle().getTerritorySectors(game).stream().map(Territory::getTerritoryName).toList();
        List<Command.Choice> choices = new ArrayList<>();
        if (faction.getLeaders().stream().filter(leader -> !leader.getName().equals("Kwisatz Haderach")).toList().isEmpty()) choices.add(new Command.Choice("None", "None"));
        choices.addAll(faction.getLeaders().stream()
                .filter(leader -> leader.getBattleTerritoryName() == null || battleTerritoryNames.stream().anyMatch(n -> n.equals(leader.getBattleTerritoryName())))
                .filter(l -> l.getSkillCard() == null || l.isPulledBehindShield())
                .filter(leader -> !leader.getName().equals("Kwisatz Haderach"))
                .filter(leader -> leader.getName().matches(searchRegex(searchValue)))
                .map(leader -> new Command.Choice(leader.getNameAndValueString(), leader.getName()))
                .toList()
        );
        choices.addAll(faction.getTreacheryHand().stream()
                .map(TreacheryCard::name)
                .filter(card -> card.startsWith("Cheap Hero"))
                .filter(card -> card.toLowerCase().matches(searchRegex(searchValue.toLowerCase())))
                .map(card -> new Command.Choice(card + " (0)", card))
                .toList()
        );
        if (choices.isEmpty())
            choices.add(new Command.Choice("None", "None"));
        return choices;
    }

    private static List<Command.Choice> removeLeaders(CommandAutoCompleteInteractionEvent event, Game game, String searchValue) {
        Faction faction = game.getFaction(event.getOptionsByName("factionname").getFirst().getAsString());
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
            if (faction.getTotalReservesStrength() >= 3)
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
        if (faction.getTotalReservesStrength() >= 3)
            choices.addAll(faction.getTreacheryHand().stream()
                    .map(TreacheryCard::name)
                    .filter(name -> name.equals("Reinforcements"))
                    .filter(card -> card.toLowerCase().matches(searchRegex(searchValue.toLowerCase())))
                    .map(card -> new Command.Choice(card, card))
                    .toList()
            );
        return choices;
    }

    private static List<Command.Choice> hmsStrongoldCardChoices(DiscordGame discordGame, String searchValue) throws ChannelNotFoundException {
        Faction faction = discordGame.getGame().getFactions().stream().filter(f -> f.hasStrongholdCard("Hidden Mobile Stronghold")).findAny().orElse(null);
        List<Command.Choice> choices = new ArrayList<>();
        if (faction != null)
            choices.addAll(faction.getStrongholdCards().stream().map(StrongholdCard::name)
                    .filter(name -> !name.equals("Hidden Mobile Stronghold"))
                    .filter(card -> card.toLowerCase().matches(searchRegex(searchValue.toLowerCase())))
                    .map(card -> new Command.Choice(card, card))
                    .toList()
            );
        return choices;
    }

    private static List<Command.Choice> bgTerritories(Game game, String searchValue) {
        List<Territory> territories = game.getTerritories().values().stream()
                .filter(territory -> territory.getForceStrength("Advisor") > 0 || territory.getForceStrength("BG") > 0)
                .toList();
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
        IxFaction faction = game.getIxFaction();
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
        if (game.hasRicheseFaction()) {
            List<TreacheryCard> cards = game.getRicheseFaction().getTreacheryCardCache();
            return cards.stream().map(TreacheryCard::name)
                    .filter(card -> card.toLowerCase().matches(searchRegex(searchValue.toLowerCase())))
                    .map(card -> new Command.Choice(card, card))
                    .collect(Collectors.toList());
        } else {
            return new ArrayList<>();
        }
    }

    private static List<Command.Choice> richeseBlackMarketCard(Game game, String searchValue) {
        if (game.hasRicheseFaction()) {
            List<TreacheryCard> cards = game.getRicheseFaction().getTreacheryHand();
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

    private static List<Command.Choice> getEcazAmbassadorsInSupply(DiscordGame discordGame, String searchValue) throws ChannelNotFoundException {
        Game game = discordGame.getGame();
        return game.getEcazFaction().getAmbassadorSupply().stream().map(a -> new Command.Choice(a, a))
                .filter(a -> a.getName().toLowerCase().matches(searchRegex(searchValue.toLowerCase())))
                .toList();
    }

    private static List<Command.Choice> strongholdWithoutAmbassaddor(@NotNull Game game, String searchValue) {
        return game.getTerritories().values().stream()
                .filter(Territory::isStronghold)
                .filter(t -> !t.hasEcazAmbassador())
                .map(Territory::getTerritoryName)
                .filter(territoryName -> territoryName.toLowerCase().matches(searchRegex(searchValue.toLowerCase())))
                .map(territoryName -> new Command.Choice(territoryName, territoryName))
                .collect(Collectors.toList());
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

    private static List<Command.Choice> getMoritaniTerrorTokensInSupply(DiscordGame discordGame, String searchValue) throws ChannelNotFoundException {
        Game game = discordGame.getGame();
        return game.getMoritaniFaction().getTerrorTokens().stream().map(tt -> new Command.Choice(tt, tt))
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
