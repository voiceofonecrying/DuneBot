package model;

import constants.Emojis;
import exceptions.InvalidGameStateException;
import model.factions.AtreidesFaction;
import model.factions.EcazFaction;
import model.factions.Faction;

import java.util.*;

public class Battle {
    private final String wholeTerritoryName;
    private final List<Territory> territorySectors;
    private final List<Faction> factions;
    private List<Force> forces;
    private Faction aggressor;
    private BattlePlan aggressorBattlePlan;
    private Faction defender;
    private BattlePlan defenderBattlePlan;
    private boolean ecazAllyToBeChosen;

    public Battle(String wholeTerritoryName, List<Territory> territorySectors, List<Faction> battleFactionsInStormOrder) {
        this.wholeTerritoryName = wholeTerritoryName;
        this.territorySectors = territorySectors;
        this.factions = battleFactionsInStormOrder;
        this.forces = aggregateForces();
        this.ecazAllyToBeChosen = hasEcazAndAlly();
    }

    public String getWholeTerritoryName() {
        return wholeTerritoryName;
    }

    public List<Territory> getTerritorySectors() {
        return territorySectors;
    }

    public List<Faction> getFactions() {
        return factions;
    }

    public Faction getAggressor() {
        if (aggressor != null) return aggressor;
        return factions.get(0);
    }

    public void setAggressor(Faction aggressor) {
        this.aggressor = aggressor;
    }

    public Faction getDefender() {
        if (defender != null) return defender;
        return factions.get(1);
    }

    public void setDefender(Faction defender) {
        this.defender = defender;
    }

    public boolean hasEcazAndAlly() {
        return factions.stream().anyMatch(f -> f instanceof EcazFaction)
                && factions.stream().anyMatch(f -> f.getAlly().equals("Ecaz"));
    }

    public boolean isEcazAllyToBeChosen() {
        return ecazAllyToBeChosen;
    }

    public void setEcazAllyToBeChosen(boolean ecazAllyToBeChosen) {
        this.ecazAllyToBeChosen = ecazAllyToBeChosen;
    }

    public boolean aggressorMustChooseOpponent() {
        int numFactions = factions.size();
        if (hasEcazAndAlly())
            numFactions--;
        return numFactions > 2;
    }

    public List<Force> getForces() {
        return forces;
    }

    public boolean isResolved() {
        aggregateForces();
        int numIfResolved = hasEcazAndAlly() ? 2 : 1;
        Set<String> factionNamesRemaining = new HashSet<>();
        forces.stream().forEach(f -> factionNamesRemaining.add(f.getFactionName()));
        return numIfResolved == factionNamesRemaining.size();
    }

    public List<Force> aggregateForces() {
        forces = new ArrayList<>();
        for (Faction f: factions) {
            Optional<Integer> optInt;
            optInt = territorySectors.stream().map(t -> t.getForce(f.getName() + "*").getStrength()).reduce(Integer::sum);
            int totalSpecialStrength = optInt.orElse(0);
            if (totalSpecialStrength > 0) forces.add(new Force(f.getName() + "*", totalSpecialStrength));
            optInt  = territorySectors.stream().map(t -> t.getForce(f.getName()).getStrength()).reduce(Integer::sum);
            int totalForceStrength = optInt.orElse(0);
            if (totalForceStrength > 0) forces.add(new Force(f.getName(), totalForceStrength));
            boolean hasNoField = territorySectors.stream().anyMatch(Territory::hasRicheseNoField);
            if (hasNoField && f.getName().equals("Richese")) {
                forces.add(new Force("NoField", 1, "Richese"));
            }
        }
        return forces;
    }

    public String getFactionsMessage() {
        StringBuilder message = new StringBuilder();
        String vs = "";
        boolean ecazInBattle = factions.stream().anyMatch(f -> f.getName().equals("Ecaz"));
        boolean ecazAllyInBattle = factions.stream().anyMatch(f -> f.getAlly().equals("Ecaz"));
        boolean ecazAllyComplete = false;
        for (Faction f : factions) {
            if (ecazAllyComplete && (f.getName().equals("Ecaz") || f.getAlly().equals("Ecaz"))) continue;
            message.append(vs);
            message.append(f.getEmoji());
            if (ecazAllyInBattle && !ecazAllyComplete && f.getName().equals("Ecaz") && f.hasAlly()) {
                message.append(Emojis.getFactionEmoji(f.getAlly()));
                ecazAllyComplete = true;
            } else if (ecazInBattle && !ecazAllyComplete && f.getAlly().equals("Ecaz")) {
                message.append(Emojis.getFactionEmoji("Ecaz"));
                ecazAllyComplete = true;
            }
            vs = " vs ";
        }
        return message.toString().trim();
    }

    private String getFactionForceMessage(String factionName) {
        String message = "";
        Optional<Force> optForce;
        optForce = forces.stream().filter(faction -> faction.getName().equals(factionName)).findFirst();
        int regularForces = 0;
        if (optForce.isPresent()) regularForces = optForce.get().getStrength();
        optForce = forces.stream().filter(faction -> faction.getName().equals(factionName + "*")).findFirst();
        int specialForces = 0;
        if (optForce.isPresent()) specialForces = optForce.get().getStrength();
        boolean hasNoField = factionName.equals("Richese") && forces.stream().anyMatch(force -> force.getName().equals("NoField"));
        if (hasNoField) message += "1 " + Emojis.NO_FIELD + " ";
        if (regularForces > 0) message += regularForces + " " + Emojis.getForceEmoji(factionName) + " ";
        if (specialForces > 0) message += specialForces + " " + Emojis.getForceEmoji(factionName + "*") + " ";
        return message;
    }

    public String getForcesMessage() {
        StringBuilder message = new StringBuilder();
        String vs = "";
        boolean ecazInBattle = factions.stream().anyMatch(f -> f.getName().equals("Ecaz"));
        boolean ecazAllyInBattle = factions.stream().anyMatch(f -> f.getAlly().equals("Ecaz"));
        boolean ecazAllyComplete = false;
        for (Faction f : factions) {
            if (ecazAllyComplete && (f.getName().equals("Ecaz") || f.getAlly().equals("Ecaz"))) continue;
            message.append(vs);
            message.append(getFactionForceMessage(f.getName()));
            if (ecazAllyInBattle && !ecazAllyComplete && f.getName().equals("Ecaz") && f.hasAlly()) {
                message.append(getFactionForceMessage(f.getAlly()));
                ecazAllyComplete = true;
            } else if (ecazInBattle && !ecazAllyComplete && f.getAlly().equals("Ecaz")) {
                message.append(getFactionForceMessage("Ecaz"));
                ecazAllyComplete = true;
            }
            vs = "vs ";
        }
        return message.toString().trim();
    }

    private void validateDial(Faction faction, int wholeNumberDial, boolean plusHalfDial, int spice) throws InvalidGameStateException {
        String factionName = (hasEcazAndAlly() && faction instanceof EcazFaction) ? faction.getAlly() : faction.getName();
        int specialStrength = forces.stream().filter(f -> f.getName().equals(factionName + "*")).findFirst().map(Force::getStrength).orElse(0);
        int regularStrength = forces.stream().filter(f -> f.getName().equals(factionName)).findFirst().map(Force::getStrength).orElse(0);
        int spiceUsed = 0;
        int dialUsed = 0;
        int specialStrengthUsed = 0;
        int regularStrengthUsed = 0;
        while (spice - spiceUsed > 0 && wholeNumberDial - dialUsed >= 2 && specialStrength - specialStrengthUsed > 0) {
            dialUsed += 2;
            spiceUsed++;
            specialStrengthUsed++;
        }
        while (spice - spiceUsed == 0 && wholeNumberDial - dialUsed >= 1 && specialStrength - specialStrengthUsed > 0) {
            dialUsed++;
            specialStrengthUsed++;
        }
        while (spice - spiceUsed > 0 && wholeNumberDial - dialUsed >= 1 && regularStrength - regularStrengthUsed > 0) {
            dialUsed++;
            spiceUsed++;
            regularStrengthUsed++;
        }
        if ((wholeNumberDial > dialUsed) || plusHalfDial) {
            int troopsNeeded = (wholeNumberDial - dialUsed) * 2 + (plusHalfDial ? 1 : 0);
            regularStrengthUsed += troopsNeeded;
        }
        if (regularStrengthUsed > regularStrength || specialStrengthUsed > specialStrength)
            throw new InvalidGameStateException(faction.getEmoji() + " does not have enough troops in the territory.");
        if (spice > spiceUsed)
            throw new InvalidGameStateException(faction.getEmoji() + " is spending more spice than necessary. Only " + spiceUsed + " is required.");
    }

    public BattlePlan setBattlePlan(Faction faction, Leader leader, TreacheryCard cheapHero, boolean kwisatzHaderach, int wholeNumberDial, boolean plusHalfDial, int spice, TreacheryCard weapon, TreacheryCard defense) throws InvalidGameStateException {
        int numFactionsExpected = hasEcazAndAlly() ? 3 : 2;
        if (factions.size() != numFactionsExpected)
            throw new InvalidGameStateException("Combatants not determined yet.");
//        if (ecazAllyToBeChosen && (faction instanceof EcazFaction || faction.getAlly().equals("Ecaz")))
//            throw new InvalidGameStateException("Ecaz must choose their alliance combatant.");

        boolean planIsForAggressor = false;
        if (getAggressor().getName().equals(faction.getName()))
            planIsForAggressor = true;
        else if (!getDefender().getName().equals(faction.getName()))
            throw new InvalidGameStateException(faction.getEmoji() + " is not in this battle.");

        if (leader != null && cheapHero != null)
            throw new InvalidGameStateException(faction.getEmoji() + " cannot both a leader and " + cheapHero.name());
        if (leader != null && !faction.getLeaders().contains(leader))
            throw new InvalidGameStateException(faction.getEmoji() + " does not have " + leader);
        if (cheapHero != null && !faction.hasTreacheryCard(cheapHero.name()))
            throw new InvalidGameStateException(faction.getEmoji() + " does not have " + cheapHero.name());
        if (leader == null && cheapHero == null && !faction.getLeaders().stream().filter(l -> !l.name().equals("Kwisatz Haderach")).toList().isEmpty())
            throw new InvalidGameStateException(faction.getEmoji() + " must play a leader or a Cheap Hero");
        if (kwisatzHaderach) {
            if (leader == null && cheapHero == null)
                throw new InvalidGameStateException("A leader or Cheap Hero must be played to use the Kwisatz Haderach");
            if (!(faction instanceof AtreidesFaction))
                throw new InvalidGameStateException("Only " + Emojis.ATREIDES + " can have the Kwisatz Haderach");
            if (!((AtreidesFaction) faction).isHasKH())
                throw new InvalidGameStateException("Only " + ((AtreidesFaction) faction).getForcesLost() + " " + Emojis.getForceEmoji("Atreides") + " killed in battle. 7 required for Kwisatz Haderach");
        }

        if (spice > (faction.getSpice() + faction.getAllySpiceShipment()))
            throw new InvalidGameStateException(faction.getEmoji() + " does not have " + spice + " " + Emojis.SPICE);
        if (weapon != null && !faction.hasTreacheryCard(weapon.name()))
            throw new InvalidGameStateException(faction.getEmoji() + " does not have " + weapon.name());
        if (defense != null && !faction.hasTreacheryCard(defense.name()))
            throw new InvalidGameStateException(faction.getEmoji() + " does not have " + defense.name());

        validateDial(faction, wholeNumberDial, plusHalfDial, spice);

        BattlePlan battlePlan = new BattlePlan(leader, cheapHero, kwisatzHaderach, wholeNumberDial, plusHalfDial, spice, weapon, defense);
        if (planIsForAggressor) {
            aggressorBattlePlan = battlePlan;
        } else {
            defenderBattlePlan = battlePlan;
        }
        return battlePlan;
    }
}
