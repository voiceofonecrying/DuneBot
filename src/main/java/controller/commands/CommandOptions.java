package controller.commands;

import com.google.gson.internal.LinkedTreeMap;
import enums.GameOption;
import model.*;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Collectors;

public class CommandOptions {
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
            .addChoice("Richese", "Richese");
    public static final OptionData faction = new OptionData(OptionType.STRING, "factionname", "The faction", true)
            .setAutoComplete(true);
    public static final OptionData otherFaction = new OptionData(OptionType.STRING, "other-factionname", "The Other faction", true)
            .setAutoComplete(true);
    public static final OptionData resourceName = new OptionData(OptionType.STRING, "resource", "The name of the resource", true);
    public static final OptionData value = new OptionData(OptionType.STRING, "value", "Set the initial value", true);
    public static final OptionData amount = new OptionData(OptionType.INTEGER, "amount", "Amount", true);
    public static final OptionData message = new OptionData(OptionType.STRING, "message", "Message for spice transactions", false);
    public static final OptionData password = new OptionData(OptionType.STRING, "password", "You really aren't allowed to run this command unless Voiceofonecrying lets you", true);
    public static final OptionData deck = new OptionData(OptionType.STRING, "deck", "The deck", true)
            .addChoice("Treachery Deck", "treachery deck")
            .addChoice("Traitor Deck", "traitor deck");
    public static final OptionData card = new OptionData(OptionType.STRING, "card", "The card.", true).setAutoComplete(true);
    public static final OptionData ixCard = new OptionData(OptionType.STRING, "ixcard", "The card.", true).setAutoComplete(true);
    public static final OptionData putBackCard = new OptionData(OptionType.STRING, "putbackcard", "The card.", true).setAutoComplete(true);
    public static final OptionData recipient = new OptionData(OptionType.STRING, "recipient", "The recipient", true).setAutoComplete(true);
    public static final OptionData bottom = new OptionData(OptionType.BOOLEAN, "bottom", "Place on bottom?", true);
    public static final OptionData traitor = new OptionData(OptionType.STRING, "traitor", "The name of the traitor", true).setAutoComplete(true);
    public static final OptionData territory = new OptionData(OptionType.STRING, "territory", "The name of the territory", true).setAutoComplete(true);
    public static final OptionData sector = new OptionData(OptionType.INTEGER, "sector", "The storm sector", true);
    public static final OptionData starred = new OptionData(OptionType.BOOLEAN, "starred", "Are they starred forces?", true);
    public static final OptionData paid = new OptionData(OptionType.BOOLEAN, "paid", "Is the action paid for?", true);

    public static final OptionData spent = new OptionData(OptionType.INTEGER, "spent", "How much was spent on the card.", true);
    public static final OptionData revived = new OptionData(OptionType.INTEGER, "revived", "How many are being revived.", true);
    public static final OptionData data = new OptionData(OptionType.STRING, "data", "What data to display", true)
            .addChoice("Territories", "territories")
            .addChoice("Decks and Discards", "dnd")
            .addChoice("Phase, Turn, and everything else", "etc")
            .addChoice("Faction Info", "factions");
    public static final OptionData isShipment = new OptionData(OptionType.BOOLEAN, "isshipment", "Is this placement a shipment?", true);
    public static final OptionData toTanks = new OptionData(OptionType.BOOLEAN, "totanks", "Remove these forces to the tanks (true) or to reserves (false)?", true);
    public static final OptionData leader = new OptionData(OptionType.STRING, "leadertokill", "The leader.", true).setAutoComplete(true);
    public static final OptionData reviveLeader = new OptionData(OptionType.STRING, "leadertorevive", "The leader.", true).setAutoComplete(true);
    public static final OptionData fromTerritory = new OptionData(OptionType.STRING, "from", "the territory.", true).setAutoComplete(true);
    public static final OptionData toTerritory = new OptionData(OptionType.STRING, "to", "Moving to this territory.", true).setAutoComplete(true);
    public static final OptionData starredAmount = new OptionData(OptionType.INTEGER, "starredamount", "Starred amount", true);
    public static final OptionData bgTerritories = new OptionData(OptionType.STRING, "bgterritories", "Territory to flip the BG force", true).setAutoComplete(true);
    public static final OptionData techTokens = new OptionData(OptionType.BOOLEAN, "techtokens", "Include Tech Tokens?", true);
    public static final OptionData sandTrout = new OptionData(OptionType.BOOLEAN, "sandtrout", "Include Sand Trout?", true);
    public static final OptionData cheapHeroTraitor = new OptionData(OptionType.BOOLEAN, "cheapherotraitor", "Include Cheap Hero Traitor card?", true);
    public static final OptionData expansionTreacheryCards = new OptionData(OptionType.BOOLEAN, "expansiontreacherycards", "Include expansion treachery cards?", true);
    public static final OptionData leaderSkills = new OptionData(OptionType.BOOLEAN, "leaderskills", "Include Leader skills?", true);
    public static final OptionData strongholdSkills = new OptionData(OptionType.BOOLEAN, "strongholdskills", "Include stronghold skills?", true);
    public static final OptionData token = new OptionData(OptionType.STRING, "token", "The Tech Token", true)
            .addChoice("Heighliners", "Heighliners")
            .addChoice("Spice Production", "Spice Production")
            .addChoice("Axlotl Tanks", "Axlotl Tanks");
    public static final OptionData factionLeader = new OptionData(OptionType.STRING, "factionleader", "The leader.", true).setAutoComplete(true);
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

    public static List<Command.Choice> getCommandChoices(CommandAutoCompleteInteractionEvent event, Game gameState) {
        String optionName = event.getFocusedOption().getName();
        String searchValue = event.getFocusedOption().getValue();

        List<Command.Choice> choices = new ArrayList<>();

        switch (optionName) {
            case "factionname", "other-factionname", "sender", "recipient", "paid-to-faction" -> choices = factions(gameState, searchValue);
            case "territory", "to" -> choices = territories(gameState, searchValue);
            case "traitor" -> choices = traitors(event, gameState, searchValue);
            case "card" -> choices = cardsInHand(event, gameState, searchValue);
            case "ixcard" -> choices = ixCardsInHand(gameState, searchValue);
            case "putbackcard" -> choices = cardsInMarket(gameState, searchValue);
            case "from" -> choices = fromTerritories(event, gameState, searchValue);
            case "bgterritories" -> choices = bgTerritories(gameState, searchValue);
            case "leadertokill", "factionleader"  -> choices = leaders(event, gameState, searchValue);
            case "leadertorevive" -> choices = reviveLeaders(gameState, searchValue);
            case "factionleaderskill" -> choices = factionLeaderSkill(event, gameState, searchValue);
            case "richese-card" -> choices = richeseCard(gameState, searchValue);
            case "bt-face-dancer" -> choices = btFaceDancers(gameState, searchValue);
            case "richese-black-market-card" -> choices = richeseBlackMarketCard(gameState, searchValue);
            case "add-game-option" -> choices = getAddGameOptions(gameState, searchValue);
            case "remove-game-option" -> choices = getRemoveGameOptions(gameState, searchValue);
        }

        return choices;
    }

    private static List<Command.Choice> factions(@NotNull Game gameState, String searchValue) {
        return gameState.getFactions().stream()
                .map(Faction::getName)
                .filter(factionName -> factionName.toLowerCase().matches(searchRegex(searchValue.toLowerCase())))
                .map(factionName -> new Command.Choice(factionName, factionName))
                .collect(Collectors.toList());
    }

    private static List<Command.Choice> territories(@NotNull Game gameState, String searchValue) {
        return gameState.getTerritories().values().stream()
                .map(Territory::getTerritoryName)
                .filter(territoryName -> territoryName.toLowerCase().matches(searchRegex(searchValue.toLowerCase())))
                .map(territoryName -> new Command.Choice(territoryName, territoryName))
                .limit(25)
                .collect(Collectors.toList());
    }

    private static List<Command.Choice> traitors(CommandAutoCompleteInteractionEvent event, Game gameState, String searchValue) {
        Faction faction = gameState.getFaction(event.getOptionsByName("factionname").get(0).getAsString());
        return faction.getTraitorHand().stream().map(TraitorCard::name)
                .filter(traitor -> traitor.toLowerCase().matches(searchRegex(searchValue.toLowerCase())))
                .map(traitor -> new Command.Choice(traitor, traitor))
                .collect(Collectors.toList());
    }

    private static List<Command.Choice> btFaceDancers(Game gameState, String searchValue) {
        if (!gameState.hasFaction("BT")) return new ArrayList<>();
        else {
            return gameState.getFaction("BT").getTraitorHand().stream()
                    .map(TraitorCard::name)
                    .filter(traitor -> traitor.toLowerCase().matches(searchRegex(searchValue.toLowerCase())))
                    .map(traitor -> new Command.Choice(traitor, traitor))
                    .collect(Collectors.toList());
        }
    }

    private static List<Command.Choice> cardsInHand(CommandAutoCompleteInteractionEvent event, Game gameState, String searchValue) {
        Faction faction = gameState.getFaction(event.getOptionsByName("factionname").get(0).getAsString());
        return faction.getTreacheryHand().stream().map(TreacheryCard::name)
                .filter(card -> card.toLowerCase().matches(searchRegex(searchValue.toLowerCase())))
                .map(card -> new Command.Choice(card, card))
                .collect(Collectors.toList());
    }

    private static List<Command.Choice> fromTerritories(CommandAutoCompleteInteractionEvent event, Game gameState, String searchValue) {
        Faction faction = gameState.getFaction(event.getOptionsByName("factionname").get(0).getAsString());
        List<Territory> territories = new LinkedList<>();
        for (Territory territory : gameState.getTerritories().values()) {
            if (territory.getForce(faction.getName()).getStrength() > 0 || territory.getForce(faction.getName() + "*").getStrength() > 0
            || (faction.getName().equals("BG") && territory.getForce("Advisor").getStrength() > 0)) {
                territories.add(territory);
            }
        }
        return territories.stream().map(Territory::getTerritoryName)
                .filter(territory -> territory.toLowerCase().matches(searchRegex(searchValue.toLowerCase())))
                .map(territory -> new Command.Choice(territory, territory))
                .collect(Collectors.toList());
    }


    private static List<Command.Choice> leaders(CommandAutoCompleteInteractionEvent event, Game gameState, String searchValue) {
        Faction faction = gameState.getFaction(event.getOptionsByName("factionname").get(0).getAsString());
        return faction.getLeaders().stream().map(Leader::name)
                .filter(leader -> leader.matches(searchRegex(searchValue)))
                .map(leader -> new Command.Choice(leader, leader))
                .collect(Collectors.toList());
    }

    private static List<Command.Choice> factionLeaderSkill(CommandAutoCompleteInteractionEvent event, Game gameState, String searchValue) {
        Faction faction = gameState.getFaction(event.getOptionsByName("factionname").get(0).getAsString());
        return faction.getLeaderSkillsHand().stream().map(LeaderSkillCard::name)
                .filter(leaderSkillCardName -> leaderSkillCardName.matches(searchRegex(searchValue)))
                .map(leaderSkillCardName -> new Command.Choice(leaderSkillCardName, leaderSkillCardName))
                .collect(Collectors.toList());
    }

    private static List<Command.Choice> reviveLeaders(Game gameState, String searchValue) {
        return gameState.getLeaderTanks().stream().map(Leader::name)
                .filter(leader -> leader.matches(searchRegex(searchValue)))
                .map(leader -> new Command.Choice(leader, leader))
                .collect(Collectors.toList());
    }

    private static List<Command.Choice> bgTerritories(Game gameState, String searchValue) {
        List<Territory> territories = new LinkedList<>();
        for (Territory territory : gameState.getTerritories().values()) {
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

    private static List<Command.Choice> ixCardsInHand(Game gameState, String searchValue) {
        Faction faction = gameState.getFaction("Ix");
        return faction.getTreacheryHand().stream().map(TreacheryCard::name)
                .filter(card -> card.toLowerCase().matches(searchRegex(searchValue.toLowerCase())))
                .map(card -> new Command.Choice(card, card))
                .collect(Collectors.toList());
    }

    private static List<Command.Choice> cardsInMarket(Game gameState, String searchValue) {
        return gameState.getMarket().stream().map(TreacheryCard::name)
                .filter(card -> card.toLowerCase().matches(searchRegex(searchValue.toLowerCase())))
                .map(card -> new Command.Choice(card, card))
                .collect(Collectors.toList());
    }

    private static List<Command.Choice> richeseCard(Game gameState, String searchValue) {
        if (gameState.hasFaction("Richese")) {
            Faction faction = gameState.getFaction("Richese");
            List<TreacheryCard> cards = convertRicheseCards(faction.getResource("cache").getValue());

            return cards.stream().map(TreacheryCard::name)
                    .filter(card -> card.toLowerCase().matches(searchRegex(searchValue.toLowerCase())))
                    .map(card -> new Command.Choice(card, card))
                    .collect(Collectors.toList());
        } else {
            return new ArrayList<>();
        }
    }

    private static List<Command.Choice> richeseBlackMarketCard(Game gameState, String searchValue) {
        if (gameState.hasFaction("Richese")) {
            Faction faction = gameState.getFaction("Richese");
            List<TreacheryCard> cards = faction.getTreacheryHand();

            return cards.stream().map(TreacheryCard::name)
                    .filter(card -> card.toLowerCase().matches(searchRegex(searchValue.toLowerCase())))
                    .map(card -> new Command.Choice(card, card))
                    .collect(Collectors.toList());
        } else {
            return new ArrayList<>();
        }
    }

    private static List<TreacheryCard> convertRicheseCards(Object rawList) {
        return ((ArrayList<LinkedTreeMap>) rawList).stream()
                .map(a -> new TreacheryCard((String)a.get("name"), (String)a.get("type")))
                .collect(Collectors.toList());
    }

    private static List<Command.Choice> getAddGameOptions(Game gameState, String searchValue) {
        Set<GameOption> selectedGameOptions = gameState.getGameOptions();
        Set<GameOption> allGameOptions = new HashSet<>(Arrays.asList(GameOption.values()));

        if (selectedGameOptions != null) {
            for (GameOption selectedGameOption : selectedGameOptions) {
                allGameOptions.remove(selectedGameOption);
            }
        }

        return gameOptionsToChoices(allGameOptions.stream().toList(), searchValue);
    }

    private static List<Command.Choice> getRemoveGameOptions(Game gameState, String searchValue) {
        Set<GameOption> gameOptions = gameState.getGameOptions();

        if (gameOptions != null) {
            return gameOptionsToChoices(gameOptions.stream().toList(), searchValue);
        } else {
            return new ArrayList<>();
        }
    }

    private static List<Command.Choice> gameOptionsToChoices(List<GameOption> list, String searchValue) {
        return list.stream().map(Enum::name)
                .filter(e -> e.toLowerCase().matches(searchRegex(searchValue.toLowerCase())))
                .map(e -> new Command.Choice(e, e))
                .toList();
    }
}
