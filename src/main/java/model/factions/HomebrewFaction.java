package model.factions;

import constants.Colors;
import constants.Emojis;
import enums.GameOption;
import model.*;
import exceptions.InvalidGameStateException;
import java.awt.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

public class HomebrewFaction extends Faction{
    Boolean emojisSetup = false;
    String colorHexCode;
    String color;
    String factionProxy;
    String homeworldProxy;
    String colorProxy;
    String highDescription;
    String lowDescription;
    String occupiedDescription;
    int highBattleExplosion;
    int lowBattleExplosion;
    int lowRevivalCharity;
    List<HomebrewAbility> homebrewAbilities;

    private class HomebrewAbility {
        String type;
        String name;
        boolean targetsOpponent;
        boolean homeworldsOnly;
        boolean requiresHT;
        boolean alliance;
        List<HomebrewAbilityState> states = new LinkedList<>();
        HomebrewAbilityRoll roll;
        String currentState;

        public void setCurrentState(String newState) {
            currentState = newState;
        }

        public float getCurrentState() {
            HomebrewAbilityState state = states.stream()
                .filter(s -> currentState.equals(s.name))
                .findFirst()
                .orElse(null);

            return state.value;
        }
    }

    private class HomebrewAbilityState {
        String name;
        float value;
    }

    private class HomebrewAbilityRoll {
        int sides;
        int perForce;
        int minResult;
        float bonus;
    }

    public HomebrewFaction(String name, String player, String userName) throws IOException {
        this.homebrewAbilities = new LinkedList<>();
        super(name, player, userName);
    }

    public static class FactionSpecs {
        public static class LeaderSpecs {
            String name;
            int value;
        }

        Boolean emojisSetup;
        String factionProxy;
        String color;
        int spice;
        int handLimit = 4;
        int freeRevival = 1;
        int maxRevival = 3;
        List<LeaderSpecs> leaders;
        String homeworld;
        String homeworldProxy;
        int highThreshold = 12;
        String highDescription;
        int lowThreshold = 11;
        String lowDescription;
        String occupiedDescription;
        int occupiedIncome = 2;
        int highBattleExplosion;
        int lowBattleExplosion;
        int lowRevivalCharity;
        List<HomebrewAbility> abilities;

        public String getFactionProxy() {
            return factionProxy;
        }
    }

    public void initalizeFromSpecs(FactionSpecs specs) {
        if (specs.emojisSetup != null)
            emojisSetup = specs.emojisSetup;

        setFactionProxy(specs.factionProxy);
        if (specs.color != null)
            colorHexCode = specs.color;
        else {
            Color decodedColor = Colors.getFactionColor(specs.factionProxy);
            colorHexCode = String.format("#%02x%02x%02x", decodedColor.getRed(), decodedColor.getGreen(), decodedColor.getBlue());
        }
        homebrewAbilities = specs.abilities;
        spice = specs.spice;
        handLimit = specs.handLimit;
        freeRevival = specs.freeRevival;
        maxRevival = specs.maxRevival;
        String emojiFaction = emojisSetup ? name : factionProxy;
        for (FactionSpecs.LeaderSpecs ls : specs.leaders) {
            Leader leader = new Leader(ls.name, ls.value, name, emojiFaction,  null, false);
            leaders.add(leader);
            game.getTraitorDeck().add(new TraitorCard(ls.name, name, emojiFaction, ls.value));
        }
        homeworld = specs.homeworld;
        if (specs.homeworldProxy != null)
            homeworldProxy = specs.homeworldProxy;
        highThreshold = specs.highThreshold;
        highDescription = specs.highDescription;
        lowThreshold = specs.lowThreshold;
        lowDescription = specs.lowDescription;
        occupiedDescription = specs.occupiedDescription;
        occupiedIncome = specs.occupiedIncome;
        highBattleExplosion = specs.highBattleExplosion;
        lowBattleExplosion = specs.lowBattleExplosion;
        lowRevivalCharity = specs.lowRevivalCharity;

        Territory hwTerritory = game.getTerritories().addHomeworld(game, homeworld, name);
        hwTerritory.addForces(name, 20);
        game.getHomeworlds().put(name, homeworld);
    }

    public String getFactionProxy() {
        return factionProxy;
    }

    public void setFactionProxy(String factionProxy) {
        if (emojisSetup) {
            emoji = ":" + name.toLowerCase() + ":";
            forceEmoji = ":" + name.toLowerCase() + "_troop:";
            this.factionProxy = name;
        } else {
            this.factionProxy = factionProxy;
            emoji = Emojis.getFactionEmoji(factionProxy);
            forceEmoji = Emojis.getForceEmoji(factionProxy);

            game.getTraitorDeck().stream().filter(t -> t.getFactionName().equals(name)).forEach(t -> t.setEmojiFaction(factionProxy));
            for (Faction f : game.getFactions())
                f.getTraitorHand().stream().filter(t -> t.getFactionName().equals(name)).forEach(t -> t.setEmojiFaction(factionProxy));
        }

        HashMap<String, String> homeworldName = new HashMap<>();
        homeworldName.put("Atreides", "Caladan");
        homeworldName.put("BG", "Wallach IX");
        homeworldName.put("BT", "Tleilax");
        homeworldName.put("CHOAM", "Tupile");
        homeworldName.put("Ecaz", "Ecaz");
        homeworldName.put("Emperor", "Salusa Secundus");
        homeworldName.put("Fremen", "Southern Hemisphere");
        homeworldName.put("Guild", "Junction");
        homeworldName.put("Harkonnen", "Giedi Prime");
        homeworldName.put("Ix", "Ix");
        homeworldName.put("Moritani", "Grumman");
        homeworldName.put("Richese", "Richese");
        homeworldProxy = homeworldName.get(factionProxy);
    }

    public void addToken(Territory territory, String homebrewTokenName) throws InvalidGameStateException {
        territory.addHomebrewToken(game, homebrewTokenName);
        String message = "A " + emoji + " Token was placed in " + territory.getTerritoryName();
        game.getTurnSummary().publish(message);
    }

    public void removeToken(Territory territory, String homebrewTokenName) {
        territory.removeHomebrewToken(game, homebrewTokenName);
        game.getTurnSummary().publish(emoji + " Token has been removed from " + territory.getTerritoryName());
    }

    public String getHomeworldProxy() {
        return homeworldProxy;
    }

    public String getHighDescription() {
        return highDescription;
    }

    public String getLowDescription() {
        return lowDescription;
    }

    public String getOccupiedDescription() {
        return occupiedDescription;
    }

    public int getHighBattleExplosion() {
        return highBattleExplosion;
    }

    public int getLowBattleExplosion() {
        return lowBattleExplosion;
    }

    public int getLowRevivalCharity() {
        return lowRevivalCharity;
    }

    @Override
    public Color getColor() {
        return Color.decode(colorHexCode);
    }

    @Override
    protected HomeworldTerritory getHomeworldTerritory() {
        Territory territory = game.getTerritory(homeworld);
        if (!(territory instanceof HomeworldTerritory)) {
            game.getTerritories().remove(homeworld, territory);
            HomeworldTerritory hwt = game.getTerritories().addHomeworld(game, homeworld, name);
            territory.getForces().forEach(f -> hwt.callParentAddForces(f.getName(), f.getStrength()));
        }
        return (HomeworldTerritory) game.getTerritory(homeworld);
    }

    public List<HomebrewAbility> getJSONAbilities() {
        return homebrewAbilities;
    }

    public List<HomebrewAbility> getJSONAbilities(String type) {
        return homebrewAbilities.stream().filter(a -> a.type == type).collect(Collectors.toList());
    }

    public List<HomebrewAbility> getJSONBattleAbilities(Territory territory, boolean targetingOpponent, boolean forAlly) {
        return homebrewAbilities.stream().filter(a ->
            a.type.equals("battle") &&
            (targetingOpponent ? a.targetsOpponent : !a.targetsOpponent) &&
            (!a.homeworldsOnly || territory instanceof HomeworldTerritory) &&
            (!a.requiresHT || (isHighThreshold() && game.hasGameOption(GameOption.HOMEWORLDS))) &&
            (!forAlly || a.alliance)
        ).collect(Collectors.toList());
    }

    public void setAbilityState(String ability, String state) {
        homebrewAbilities.stream()
            .filter(a -> ability.equals(a.name))
            .forEach(a -> a.currentState = state);
    }

    public List<String> getJSONAbilityNames() {
        List<String> result = new ArrayList<>();
        for (HomebrewAbility ability : homebrewAbilities) {
            result.add(ability.name);
        }
        return result;
    }

    public List<String> getJSONAbilityStateNames() {
        List<String> result = new ArrayList<>();
        for (HomebrewAbility ability : homebrewAbilities) {
            for (HomebrewAbilityState state : ability.states) {
                result.add(state.name);
            }
        }
        return result;
    }

    private BattlePlanBonus calculateDialBonus(Territory territory, int forcesDialed, boolean targetingOpponent, boolean forAlly) {
        float bonus = 0;
        StringBuilder description = new StringBuilder();

        for (HomebrewAbility ability : getJSONBattleAbilities(territory, targetingOpponent, forAlly)) {
            float abilityBonus = ability.getCurrentState();
            abilityBonus += getBattleAbilityRoll(ability, forcesDialed);
            bonus += abilityBonus;
            if (abilityBonus == 0)
                continue;
            String plus = abilityBonus > 0 ? "+" : "";
            String formattedValue = abilityBonus % 1 == 0 ?
                String.valueOf((int)abilityBonus) :
                String.valueOf(abilityBonus);
            description.append("\n ")
                .append(plus)
                .append(formattedValue)
                .append(" for ")
                .append(ability.name);
            if (forAlly) {
                description.append(" (")
                    .append(name)
                    .append(" alliance ability)");
            }
        }

        BattlePlanBonus result = new BattlePlanBonus(bonus, description.toString());
        if (!forAlly && hasAlly()) {
            BattlePlanBonus allyBonus = targetingOpponent ?
                game.getFaction(ally).getAllianceBattleOpponentDialPenalty(territory, forcesDialed) :
                game.getFaction(ally).getAllianceBattleDialBonus(territory, forcesDialed);
            result = result.add(allyBonus);
        }

        return result;
    }

    private float getBattleAbilityRoll(HomebrewAbility ability, int forcesDialed) {
        if (ability.roll == null || "inactive".equalsIgnoreCase(ability.currentState))
            return 0;
        float result = 0;
        for (int i = 1; i <= ability.roll.perForce * forcesDialed; i++) {
            Random dice = new Random();
            int rolled = dice.nextInt(ability.roll.sides) + 1;
            if (rolled >= ability.roll.minResult) {
                result += ability.roll.bonus;
            }
        }
        double doubled = Math.floor(result * 2);
        return (float)doubled / 2;
    }

    @Override
    public BattlePlanBonus getBattleDialBonus(Territory territory, int forcesDialed) {
        return calculateDialBonus(territory, forcesDialed, false, false);
    }

    @Override
    public BattlePlanBonus getAllianceBattleDialBonus(Territory territory, int forcesDialed) {
        return calculateDialBonus(territory, forcesDialed, false, true);
    }

    @Override
    public BattlePlanBonus getBattleOpponentDialPenalty(Territory territory, int forcesDialed) {
        return calculateDialBonus(territory, forcesDialed, true, false);
    }

    @Override
    public BattlePlanBonus getAllianceBattleOpponentDialPenalty(Territory territory, int forcesDialed) {
        return calculateDialBonus(territory, forcesDialed, true, true);
    }
}
