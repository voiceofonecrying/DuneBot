package model;

import constants.Emojis;
import enums.GameOption;
import enums.UpdateType;
import exceptions.InvalidGameStateException;
import model.factions.*;
import model.topics.DuneTopic;

import java.text.MessageFormat;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class Battle {
    private final String wholeTerritoryName;
    private final List<Territory> territorySectors;
    private final List<String> factionNames;
    private final List<Force> forces;
    private final String ecazAllyName;
    private BattlePlan aggressorBattlePlan;
    private BattlePlan defenderBattlePlan;
    private boolean resolutionPublished;
    private boolean fedaykinNegated;
    private boolean sardaukarNegated;
    private boolean cyborgsNegated;
    private boolean fremenMustPay;

    public enum DecisionStatus {
        NA,
        OPEN,
        CLOSED
    }
    private boolean overrideDecisions;
    private DecisionStatus hmsStrongholdCardTBD;
    private String hmsStrongholdCardFactionEmoji;
    private DecisionStatus spiceBankerTBD;
    private String spiceBankerFactionEmoji;
    private DecisionStatus juiceOfSaphoTBD;
    private DecisionStatus portableSnooperTBD;
    private DecisionStatus stoneBurnerTBD;
    private DecisionStatus mirrorWeaponStoneBurnerTBD;
    private DecisionStatus poisonToothTBD;
    private boolean diplomatMustBeResolved;

    public Battle(Game game, List<Territory> territorySectors, List<Faction> battleFactionsInStormOrder) {
        this.wholeTerritoryName = territorySectors.getFirst().getAggregateTerritoryName();
        this.territorySectors = territorySectors;
        this.factionNames = new ArrayList<>();
        battleFactionsInStormOrder.forEach(f -> factionNames.add(f.getName()));
        this.ecazAllyName = battleFactionsInStormOrder.stream().filter(f -> f instanceof EcazFaction).map(Faction::getAlly).findFirst().orElse(null);
        if (factionNames.get(0).equals("Ecaz") && factionNames.get(1).equals(ecazAllyName) || factionNames.get(0).equals(ecazAllyName) && factionNames.get(1).equals("Ecaz"))
            factionNames.add(factionNames.remove(1));
        this.forces = aggregateForces(territorySectors, battleFactionsInStormOrder);
        this.resolutionPublished = false;
        this.fedaykinNegated = false;
        this.sardaukarNegated = battleFactionsInStormOrder.stream().anyMatch(f -> f instanceof FremenFaction);
        try {
            EmperorFaction emperor = (EmperorFaction) game.getFaction("Emperor");
            if (emperor.isSecundusOccupied())
                this.sardaukarNegated = true;
        } catch (IllegalArgumentException ignored) {}
        this.cyborgsNegated = false;
        this.fremenMustPay = false;
        this.overrideDecisions = false;
        this.hmsStrongholdCardTBD = DecisionStatus.NA;
        this.spiceBankerTBD = DecisionStatus.NA;
        this.juiceOfSaphoTBD = DecisionStatus.NA;
        this.portableSnooperTBD = DecisionStatus.NA;
        this.stoneBurnerTBD = DecisionStatus.NA;
        this.mirrorWeaponStoneBurnerTBD = DecisionStatus.NA;
        this.poisonToothTBD = DecisionStatus.NA;
        this.diplomatMustBeResolved = false;
    }

    public List<Force> aggregateForces(List<Territory> territorySectors, List<Faction> factions) {
        List<Force> forces = new ArrayList<>();
        for (Faction f: factions) {
            Optional<Integer> optInt;
            optInt = territorySectors.stream().map(t -> t.getForceStrength(f.getName() + "*")).reduce(Integer::sum);
            int totalSpecialStrength = optInt.orElse(0);
            if (totalSpecialStrength > 0) forces.add(new Force(f.getName() + "*", totalSpecialStrength));
            optInt  = territorySectors.stream().map(t -> t.getForceStrength(f.getName())).reduce(Integer::sum);
            int totalForceStrength = optInt.orElse(0);
            if (totalForceStrength > 0) forces.add(new Force(f.getName(), totalForceStrength));
            Integer noField = territorySectors.stream().map(Territory::getRicheseNoField).filter(Objects::nonNull).findFirst().orElse(null);
            if (noField != null && f.getName().equals("Richese")) {
                forces.add(new Force("NoField", noField));
            }
        }
        return forces;
    }

    public String getWholeTerritoryName() {
        return wholeTerritoryName;
    }

    public List<Territory> getTerritorySectors() {
        return territorySectors;
    }

    public List<String> getFactionNames() {
        return factionNames;
    }

    public List<Faction> getFactions(Game game) {
        return factionNames.stream().map(game::getFaction).toList();
    }

    public String getAggressorName() {
        return factionNames.getFirst();
    }

    public Faction getAggressor(Game game) {
        return game.getFaction(factionNames.getFirst());
    }

    public void setEcazCombatant(Game game, String combatant) {
        Faction combatantFaction = game.getFaction(combatant);
        if (combatantFaction != null) {
            String ally = combatantFaction.getAlly();
            if (combatantFaction instanceof EcazFaction || ally.equals("Ecaz")) {
                factionNames.remove(ally);
                factionNames.add(ally);
            }
        }
    }

    public String getDefenderName() {
        return factionNames.get(1);
    }

    public Faction getDefender(Game game) {
        return game.getFaction(factionNames.get(1));
    }

    public boolean hasEcazAndAlly() {
        return factionNames.stream().anyMatch(f -> f.equals("Ecaz"))
                && factionNames.stream().anyMatch(f -> f.equals(ecazAllyName));
    }

    public boolean aggressorMustChooseOpponent() {
        int numFactions = factionNames.size();
        if (hasEcazAndAlly())
            numFactions--;
        return numFactions > 2;
    }

    public List<Force> getForces() {
        return forces;
    }

    private boolean inCurrentBattle(Force force) {
        return factionNames.stream().anyMatch(n -> n.equals(force.getFactionName()));
    }

    public boolean isResolved(Game game) {
        Set<String> factionsLeft = new HashSet<>();
        for (Territory t : territorySectors) {
            Territory territory = game.getTerritory(t.getTerritoryName());
            territory.getForces().stream()
                    .filter(force -> !(force.getName().equalsIgnoreCase("Advisor")))
                    .filter(force -> !(force.getName().equalsIgnoreCase("Hidden Mobile Stronghold")))
                    .filter(this::inCurrentBattle)
                    .forEach(force -> factionsLeft.add(force.getFactionName()));
            if (territory.hasRicheseNoField()) factionsLeft.add("Richese");
        }
        return factionsLeft.size() <= 1 || factionsLeft.size() == 2 && hasEcazAndAlly() && factionsLeft.contains("Ecaz") && factionsLeft.contains(ecazAllyName);
    }

    public String getFactionsMessage(Game game) {
        StringBuilder message = new StringBuilder();
        String vs = "";
        boolean ecazInBattle = factionNames.stream().anyMatch(f -> f.equals("Ecaz"));
        boolean ecazAllyInBattle = getFactions(game).stream().anyMatch(f -> f.getAlly().equals("Ecaz"));
        boolean ecazAllyComplete = false;
        for (Faction f : getFactions(game)) {
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

    public String getForcesMessage(Game game) {
        boolean ecazInBattle = factionNames.stream().anyMatch(f -> f.equals("Ecaz"));
        boolean ecazAllyInBattle = getFactions(game).stream().anyMatch(f -> f.getAlly().equals("Ecaz"));
        boolean ecazAllyComplete = false;
        List<String> factionForceStrings = new ArrayList<>();
        for (Faction f : getFactions(game)) {
            if (ecazAllyComplete && (f.getName().equals("Ecaz") || f.getAlly().equals("Ecaz"))) continue;
            String factionForceString = getFactionForceMessage(f.getName());
            if (ecazAllyInBattle && !ecazAllyComplete && f.getName().equals("Ecaz") && f.hasAlly()) {
                factionForceString += getFactionForceMessage(f.getAlly());
                ecazAllyComplete = true;
            } else if (ecazInBattle && !ecazAllyComplete && f.getAlly().equals("Ecaz")) {
                factionForceString += getFactionForceMessage("Ecaz");
                ecazAllyComplete = true;
            }
            factionForceStrings.add(factionForceString);
        }
        return String.join("vs ", factionForceStrings).trim();
    }

    public void negateSpecialForces(Game game, Faction targetFaction) throws InvalidGameStateException {
        String targetFactionName = targetFaction.getName();
        Faction ecaz = null;
        try {
            ecaz = game.getFaction("Ecaz");
        } catch (IllegalArgumentException ignored) {}
        boolean aggressorNegated = targetFactionName.equals(getAggressorName())
                || ecaz != null && ecaz.getAlly().equals(targetFactionName) && getAggressorName().equals("Ecaz");
        boolean defenderNegated = targetFactionName.equals(getDefenderName())
                || ecaz != null && ecaz.getAlly().equals(targetFactionName) && getDefenderName().equals("Ecaz");
        if (!aggressorNegated && !defenderNegated)
            throw new InvalidGameStateException(targetFactionName + " is not in the current battle.");

        switch (targetFaction) {
            case FremenFaction ignored -> fedaykinNegated = true;
            case EmperorFaction ignored -> sardaukarNegated = true;
            case IxFaction ignored -> cyborgsNegated = true;
            default -> {}
        }

        boolean battlePlanRemoved = false;
        if (aggressorNegated && aggressorBattlePlan != null) {
            aggressorBattlePlan = null;
            battlePlanRemoved = true;
        } else if (defenderNegated && defenderBattlePlan != null) {
            defenderBattlePlan = null;
            battlePlanRemoved = true;
        }
        String message = "Your " + Emojis.getForceEmoji(targetFactionName + "*") + " advantage has been negated by Karama.";
        if (battlePlanRemoved)
            message += "\nYou must submit a new battle plan.";
        targetFaction.getChat().publish(message + " " + targetFaction.getPlayer());
    }

    public boolean isFremenMustPay() {
        return fremenMustPay;
    }

    public boolean isFedaykinNegated() {
        return fedaykinNegated;
    }

    public boolean isSardaukarNegated() {
        return sardaukarNegated;
    }

    public boolean isCyborgsNegated() {
        return cyborgsNegated;
    }

    public void karamaFremenMustPay(Game game) throws InvalidGameStateException {
        boolean aggressorKaramad = getAggressor(game) instanceof FremenFaction;
        boolean defenderKaramad = getDefender(game) instanceof FremenFaction;
        if (!aggressorKaramad && !defenderKaramad)
            throw new InvalidGameStateException("Fremen are not in the current battle.");

        fremenMustPay = true;

        boolean battlePlanRemoved = false;
        if (aggressorKaramad && aggressorBattlePlan != null) {
            aggressorBattlePlan = null;
            battlePlanRemoved = true;
        } else if (defenderKaramad && defenderBattlePlan != null) {
            defenderBattlePlan = null;
            battlePlanRemoved = true;
        }
        Faction fremen = game.getFaction("Fremen");
        String message = "Your free dial advantage has been negated by Karama.";
        if (battlePlanRemoved)
            message += "\nYou must submit a new battle plan.";
        fremen.getChat().publish(message + " " + fremen.getPlayer());
    }

    private void removePreviouslySubmittedLeaderFromTerritory(Faction faction) {
        faction.getLeaders().stream().filter(l -> l.getBattleTerritoryName() != null && l.getBattleTerritoryName().equals(getWholeTerritoryName())).forEach(l -> l.setBattleTerritoryName(null));
    }

    public String setBattlePlan(Game game, Faction faction, String leaderName, boolean kwisatzHaderach, String dial, int spice, String weaponName, String defenseName) throws InvalidGameStateException {
        if (!getAggressorName().equals(faction.getName()) && !getDefenderName().equals(faction.getName()))
            throw new InvalidGameStateException("You are not in the current battle.");

        removePreviouslySubmittedLeaderFromTerritory(faction);
        Leader leader = null;
        TreacheryCard cheapHero = null;
        if (leaderName.startsWith("Cheap"))
            cheapHero = faction.getTreacheryHand().stream().filter(f -> f.name().equals(leaderName)).findFirst().orElseThrow();
        else if (!leaderName.equals("None")) {
            leader = faction.getLeaders().stream().filter(l -> l.getName().equals(leaderName)).findFirst().orElseThrow();
            Territory battleTerritory = getTerritorySectors().stream()
                    .filter(t -> t.getTotalForceCount(faction) > 0)
                    .findAny().orElseThrow();
            leader.setBattleTerritoryName(battleTerritory.getTerritoryName());
            faction.setUpdated(UpdateType.MISC_BACK_OF_SHIELD);
        }

        String returnString = "";
        if (faction instanceof AtreidesFaction atreidesFaction) {
            if (kwisatzHaderach && atreidesFaction.getForcesLost() < 7) {
                kwisatzHaderach = false;
                returnString += "Only " + ((AtreidesFaction) faction).getForcesLost() + " " + Emojis.getForceEmoji("Atreides") + " killed in battle. KH has been omitted from the battle plan.\n";
            } else if (kwisatzHaderach && leader == null && cheapHero == null) {
                kwisatzHaderach = false;
                returnString += "You must play a leader or a Cheap Hero to use Kwisatz Haderach. KH has been omitted from the battle plan.\n";
            }
        } else if (kwisatzHaderach) {
            kwisatzHaderach = false;
            returnString += "You are not " + Emojis.ATREIDES + ". KH has been omitted from the battle plan.\n";
        }

        int decimalPoint = dial.indexOf(".");
        int wholeNumberDial;
        boolean plusHalfDial = false;
        if (decimalPoint == -1)
            wholeNumberDial = Integer.parseInt(dial);
        else {
            wholeNumberDial = decimalPoint == 0 ? 0 : Integer.parseInt(dial.substring(0, decimalPoint));
            if (dial.length() == decimalPoint + 2 && dial.substring(decimalPoint + 1).equals("5"))
                plusHalfDial = true;
            else
                throw new InvalidGameStateException(dial + " is not a valid dial");
        }

        TreacheryCard weapon = null;
        if (!weaponName.equals("None")) {
            weapon = faction.getTreacheryHand().stream().filter(c -> c.name().equals(weaponName)).findFirst().orElseThrow();
        }
        TreacheryCard defense = null;
        if (!defenseName.equals("None")) {
            defense = faction.getTreacheryHand().stream().filter(c -> c.name().equals(defenseName)).findFirst().orElseThrow();
        }

        setBattlePlan(game, faction, leader, cheapHero, kwisatzHaderach, wholeNumberDial, plusHalfDial, spice, weapon, defense);
        return returnString;
    }

    public BattlePlan setBattlePlan(Game game, Faction faction, Leader leader, TreacheryCard cheapHero, boolean kwisatzHaderach, int wholeNumberDial, boolean plusHalfDial, int spice, TreacheryCard weapon, TreacheryCard defense) throws InvalidGameStateException {
        int numFactionsExpected = hasEcazAndAlly() ? 3 : 2;
        if (factionNames.size() != numFactionsExpected)
            throw new InvalidGameStateException("Combatants not determined yet.");
//        if (ecazAllyToBeChosen && (faction instanceof EcazFaction || faction.getAlly().equals("Ecaz")))
//            throw new InvalidGameStateException("Ecaz must choose their alliance combatant.");

        boolean planIsForAggressor = false;
        if (getAggressorName().equals(faction.getName()))
            planIsForAggressor = true;
        else if (!getDefenderName().equals(faction.getName()))
            throw new InvalidGameStateException(faction.getName() + " is not in this battle.");

        BattlePlan battlePlan = new BattlePlan(game, this, faction, planIsForAggressor, leader, cheapHero, kwisatzHaderach, weapon, defense, wholeNumberDial, plusHalfDial, spice);
        if (planIsForAggressor) {
            if (aggressorBattlePlan == null && battlePlan.isDialedForcesSettled() && game.getGameActions() != null)
                game.getGameActions().publish(faction.getEmoji() + " battle plan submitted.");
            aggressorBattlePlan = battlePlan;
        } else {
            if (defenderBattlePlan == null && battlePlan.isDialedForcesSettled() && game.getGameActions() != null)
                game.getGameActions().publish(faction.getEmoji() + " battle plan submitted.");
            defenderBattlePlan = battlePlan;
        }
        if (aggressorBattlePlan != null && defenderBattlePlan != null) {
            aggressorBattlePlan.revealOpponentBattlePlan(defenderBattlePlan);
            defenderBattlePlan.revealOpponentBattlePlan(aggressorBattlePlan);
            if (aggressorBattlePlan.isOpponentHasBureaucrat())
                aggressorBattlePlan.revealOpponentBattlePlan(defenderBattlePlan);
        }

        applyHMSStrongholdCard(game, faction, battlePlan);
        presentSpiceBankerChoices(game, faction, battlePlan, spice);
        return battlePlan;
    }

    private void applyHMSStrongholdCard(Game game, Faction faction, BattlePlan battlePlan) {
        if (game.hasGameOption(GameOption.STRONGHOLD_SKILLS) && getWholeTerritoryName().equals("Hidden Mobile Stronghold") && faction.hasStrongholdCard("Hidden Mobile Stronghold")) {
            List<String> strongholdNames = faction.getStrongholdCards().stream().map(StrongholdCard::name).filter(n -> !n.equals("Hidden Mobile Stronghold")).toList();
            if (strongholdNames.size() == 1) {
                faction.getChat().publish(strongholdNames.getFirst() + " Stronghold card will be applied in the HMS battle.");
                if (strongholdNames.getFirst().equals("Carthag"))
                    battlePlan.addCarthagStrongholdPower();
            } else if (strongholdNames.size() >= 2) {
                game.getModInfo().publish(faction.getEmoji() + " must select which Stronghold Card they want to apply in the HMS. Please wait to resolve the battle.");
                List<DuneChoice> choices = strongholdNames.stream().map(strongholdName -> new DuneChoice("hmsstrongholdpower-" + strongholdName, strongholdName)).collect(Collectors.toList());
                faction.getChat().publish("Which Stronghold Card would you like to use in the HMS battle?", choices);
                hmsStrongholdCardFactionEmoji = faction.getEmoji();
                hmsStrongholdCardTBD = DecisionStatus.OPEN;
            }
        }
    }

    public void hmsCardDecisionMade() {
        hmsStrongholdCardTBD = DecisionStatus.CLOSED;
    }

    public boolean isHMSCardDecisionOpen() {
        return hmsStrongholdCardTBD == DecisionStatus.OPEN;
    }

    public String getHmsStrongholdCardFactionEmoji() {
        return hmsStrongholdCardFactionEmoji;
    }

    public void setHMSStrongholdCard(Faction faction, String strongholdCard) throws InvalidGameStateException {
        faction.setHmsStrongholdProxy(new StrongholdCard(strongholdCard));
        if (strongholdCard.equals("Carthag")) {
            if (faction.getName().equals(getAggressorName()) && aggressorBattlePlan != null)
                aggressorBattlePlan.addCarthagStrongholdPower();
            else if (faction.getName().equals(getDefenderName()) && defenderBattlePlan != null)
                defenderBattlePlan.addCarthagStrongholdPower();
        }
        hmsStrongholdCardTBD = DecisionStatus.CLOSED;
    }

    private void presentSpiceBankerChoices(Game game, Faction faction, BattlePlan battlePlan, int spice) {
        int availableSpice = faction.getSpice() - spice;
        if (availableSpice > 0 && battlePlan.isSkillBehindAndLeaderAlive("Spice Banker")) {
            game.getModInfo().publish(faction.getEmoji() + " may spend spice to increase leader value with Spice Banker. Please wait to resolve the battle.");
            List<DuneChoice> choices = new ArrayList<>();
            IntStream.range(0, 4).forEachOrdered(i -> {
                DuneChoice choice = new DuneChoice("spicebanker-" + i, Integer.toString(i));
                choice.setDisabled(availableSpice < i);
                choices.add(choice);
            });
            faction.getChat().publish("How much would you like to spend with Spice Banker?", choices);
            spiceBankerFactionEmoji = faction.getEmoji();
            spiceBankerTBD = DecisionStatus.OPEN;
        }
    }

    public void spiceBankerDecisionMade() {
        spiceBankerTBD = DecisionStatus.CLOSED;
    }

    public boolean isSpiceBankerDecisionOpen() {
        return spiceBankerTBD == DecisionStatus.OPEN;
    }

    public String getSpiceBankerFactionEmoji() {
        return spiceBankerFactionEmoji;
    }

    public void setSpiceBankerSupport(Faction faction, int spice) {
        if (faction.getName().equals(getAggressorName()) && aggressorBattlePlan != null)
            aggressorBattlePlan.setSpiceBankerSupport(spice);
        else if (faction.getName().equals(getDefenderName()) && defenderBattlePlan != null)
            defenderBattlePlan.setSpiceBankerSupport(spice);
        spiceBankerTBD = DecisionStatus.CLOSED;
    }

    public String updateTroopsDialed(Game game, String factionName, int regularDialed, int specialDialed) throws InvalidGameStateException {
        BattlePlan battlePlan;
        if (getAggressorName().equals(factionName))
            battlePlan = aggressorBattlePlan;
        else if (getDefenderName().equals(factionName))
            battlePlan = defenderBattlePlan;
        else
            throw new InvalidGameStateException(factionName + " is not in the current battle.");
        if (!battlePlan.isDialedForcesSettled() && game.getGameActions() != null)
            game.getGameActions().publish(Emojis.getFactionEmoji(factionName) + " battle plan submitted.");
        battlePlan.setForcesDialed(regularDialed, specialDialed);
        String emojiFactionName = factionName.equals("Ecaz") ? ecazAllyName : factionName;
        String regularEmoji = Emojis.getForceEmoji(emojiFactionName);
        String starredEmoji = Emojis.getForceEmoji(emojiFactionName + "*");
        String planUpdatedString = "Battle plan updated to dial " + regularDialed + " " + regularEmoji + " " + specialDialed + " " + starredEmoji +".";
        String forcesRemainingString = battlePlan.getForcesRemainingString();
        return planUpdatedString + "\n" + forcesRemainingString;
    }

    public void addCarthagStrongholdPower(Faction faction) throws InvalidGameStateException {
        BattlePlan battlePlan;
        if (getAggressorName().equals(faction.getName()))
            battlePlan = aggressorBattlePlan;
        else if (getDefenderName().equals(faction.getName()))
            battlePlan = defenderBattlePlan;
        else
            throw new InvalidGameStateException(faction.getName() + " is not in this battle.");

        battlePlan.addCarthagStrongholdPower();
    }

    public boolean isNotResolvable() {
        return aggressorBattlePlan == null || defenderBattlePlan == null;
    }

    public String getAggressorEmojis(Game game) {
        Faction aggressor = getAggressor(game);
        StringBuilder resolution = new StringBuilder();
        resolution.append(aggressor.getEmoji());
        if (hasEcazAndAlly()) {
            if (aggressor instanceof EcazFaction)
                resolution.append(game.getFaction(aggressor.getAlly()).getEmoji());
            else if (aggressor.getAlly().equals("Ecaz"))
                resolution.append(Emojis.getFactionEmoji("Ecaz"));
        }
        return resolution.toString();
    }

    public String getDefenderEmojis(Game game) {
        Faction defender = getDefender(game);
        StringBuilder resolution = new StringBuilder();
        resolution.append(defender.getEmoji());
        if (hasEcazAndAlly()) {
            if (defender instanceof EcazFaction)
                resolution.append(game.getFaction(defender.getAlly()).getEmoji());
            else if (defender.getAlly().equals("Ecaz"))
                resolution.append(Emojis.getFactionEmoji("Ecaz"));
        }
        return resolution.toString();
    }

    public BattlePlan getAggressorBattlePlan() {
        return aggressorBattlePlan;
    }

    public BattlePlan getDefenderBattlePlan() {
        return defenderBattlePlan;
    }

    public boolean isAggressorWin(Game game) throws InvalidGameStateException {
        if (isNotResolvable())
            throw new InvalidGameStateException("Battle cannot be resolved yet. Missing battle plan(s).");
        if (bothCallTraitor(game))
            return false;
        if (aggressorCallsTraitor(game))
            return true;
        if (defenderCallsTraitor(game))
            return false;
        Faction defender = getDefender(game);
        if (defenderBattlePlan.isJuiceOfSapho()
                || (game.hasGameOption(GameOption.STRONGHOLD_SKILLS)
                && (wholeTerritoryName.equals("Habbanya Sietch") && defender.hasStrongholdCard("Habbanya Sietch")
                || wholeTerritoryName.equals("Hidden Mobile Stronghold") && defender.hasHmsStrongholdProxy("Habbanya Sietch"))))
            return aggressorBattlePlan.getDoubleBattleStrength() > defenderBattlePlan.getDoubleBattleStrength();
        return aggressorBattlePlan.getDoubleBattleStrength() >= defenderBattlePlan.getDoubleBattleStrength();
    }

    public String getWinnerEmojis(Game game) throws InvalidGameStateException {
        return isAggressorWin(game) ? getAggressorEmojis(game) : getDefenderEmojis(game);
    }

    private boolean aggressorCallsTraitor(Game game) {
        String opponentLeader = defenderBattlePlan.getLeaderNameForTraitor();
        if (aggressorBattlePlan.isWillCallTraitor() && getAggressor(game).hasTraitor(opponentLeader))
            return true;
        else
            return getAggressor(game).getAlly().equals("Harkonnen") && aggressorBattlePlan.isHarkWillCallTraitor() && game.getFaction("Harkonnen").hasTraitor(opponentLeader);
    }

    private boolean defenderCallsTraitor(Game game) {
        String opponentLeader = aggressorBattlePlan.getLeaderNameForTraitor();
        if (defenderBattlePlan.isWillCallTraitor() && getDefender(game).hasTraitor(opponentLeader))
            return true;
        else
            return getDefender(game).getAlly().equals("Harkonnen") && defenderBattlePlan.isHarkWillCallTraitor() && game.getFaction("Harkonnen").hasTraitor(opponentLeader);
    }

    private boolean bothCallTraitor(Game game) {
        return aggressorCallsTraitor(game) && defenderCallsTraitor(game);
    }

    private boolean eitherCallsTraitor(Game game) {
        return aggressorCallsTraitor(game) || defenderCallsTraitor(game);
    }

    private boolean neitherCallTraitor(Game game) {
        return !aggressorCallsTraitor(game) && !defenderCallsTraitor(game);
    }

    public String factionBattleResults(Game game, boolean isAggressor, boolean executeResolution) throws InvalidGameStateException {
        String resolution = "";
        Faction faction = isAggressor ? getAggressor(game) : getDefender(game);
        String troopFactionName = hasEcazAndAlly() && faction.getName().equals("Ecaz") ? game.getFaction("Ecaz").getAlly() : faction.getName();
        Faction troopFaction = game.getFaction(troopFactionName);
        String troopFactionEmoji = troopFaction.getEmoji();
        Faction opponentFaction = isAggressor ? getDefender(game) : getAggressor(game);
        BattlePlan aggressorPlan = getAggressorBattlePlan();
        BattlePlan defenderPlan = getDefenderBattlePlan();
        BattlePlan battlePlan = isAggressor ? aggressorPlan : defenderPlan;
        BattlePlan opponentBattlePlan = isAggressor ? defenderPlan : aggressorPlan;
        boolean isLasgunShieldExplosion = battlePlan.isLasgunShieldExplosion() && neitherCallTraitor(game);
        boolean callsTraitor = isAggressor ? aggressorCallsTraitor(game) : defenderCallsTraitor(game);
        if (callsTraitor) {
            battlePlan.setOpponentIsTraitor(true);
            opponentBattlePlan.setLeaderIsTraitor(true);
        }
        boolean opponentCallsTraitor = isAggressor ? defenderCallsTraitor(game) : aggressorCallsTraitor(game);
        if (opponentCallsTraitor) {
            opponentBattlePlan.setOpponentIsTraitor(true);
            battlePlan.setLeaderIsTraitor(true);
        }
        String emojis = faction.getEmoji();
        boolean isLoser = (isAggressor != isAggressorWin(game) || isLasgunShieldExplosion) && !callsTraitor || bothCallTraitor(game);
        String wholeTerritoryName = getWholeTerritoryName();

        if (battlePlan.getLeader() != null && !battlePlan.isLeaderAlive()) {
            resolution += emojis + " loses " + battlePlan.getKilledLeaderString() + " to the tanks\n";
            if (executeResolution)
                game.killLeader(faction, battlePlan.getKilledLeaderString());
        }
        if (!callsTraitor && isLasgunShieldExplosion && battlePlan.hasKwisatzHaderach()) {
            resolution += emojis + " loses Kwisatz Haderach to the tanks\n";
            if (executeResolution)
                game.killLeader(faction, "Kwisatz Haderach");
        }
        int regularForcesDialed = battlePlan.getRegularDialed();
        int specialForcesDialed = battlePlan.getSpecialDialed();
        int regularForcesTotal = getForces().stream()
                .filter(f -> f.getName().equals(troopFactionName))
                .mapToInt(Force::getStrength).findFirst().orElse(0);
        if (faction instanceof RicheseFaction)
            regularForcesTotal += getForces().stream()
                    .filter(f -> f.getName().equals("NoField"))
                    .mapToInt(Force::getStrength).findFirst().orElse(0);
        int specialForcesTotal = getForces().stream()
                .filter(f -> f.getName().equals(troopFactionName + "*"))
                .mapToInt(Force::getStrength).findFirst().orElse(0);
        int specialForcesNotDialed = specialForcesTotal - specialForcesDialed;
        int regularForcesNotDialed = regularForcesTotal - regularForcesDialed;
        int savedSpecialForces;
        int savedRegularForces;
        TreacheryCard weapon = battlePlan.getWeapon();
        TreacheryCard defense = battlePlan.getDefense();
        int ecazForcesWithdrawn = 0;
        DuneTopic turnSummary = game.getTurnSummary();
        if (isLoser) {
            if (!(faction instanceof EcazFaction && hasEcazAndAlly())) {
                if (battlePlan.isSkillBehindAndLeaderAlive("Diplomat")) {
                    int leaderValue = battlePlan.getLeaderValue();
                    savedSpecialForces = Math.min(specialForcesNotDialed, leaderValue);
                    savedRegularForces = Math.min(regularForcesNotDialed, leaderValue - savedSpecialForces);
                    if (savedRegularForces > 0 || savedSpecialForces > 0) {
                        specialForcesTotal -= savedSpecialForces;
                        specialForcesNotDialed -= savedSpecialForces;
                        regularForcesTotal -= savedRegularForces;
                        regularForcesNotDialed -= savedRegularForces;
                        resolution += faction.getEmoji() + " may retreat " + faction.forcesString(savedRegularForces, savedSpecialForces) + " to an empty adjacent non-stronghold with Diplomat\n";
                        if (executeResolution) {
                            diplomatMustBeResolved = true;
                            faction.getChat().publish("You may retreat " + faction.forcesString(savedRegularForces, savedSpecialForces) + " to an empty adjacent non-stronghold with Diplomat.\nPlease tell the mod where you would like to move them. " + faction.getPlayer());
                            game.getModInfo().publish(faction.getEmoji() + " retreat with Diplomat must be resolved. " + game.getModOrRoleMention());
                        }
                    }
                }
            }
            if (!opponentCallsTraitor && canHarassAndWithdraw(faction, weapon, defense)) {
                if (faction instanceof EcazFaction && hasEcazAndAlly()) {
                    ecazForcesWithdrawn = Math.floorDiv(battlePlan.getEcazTroopsForAlly(), 2);
                    resolution += harassAndWithdraw(game, faction, ecazForcesWithdrawn, 0, executeResolution);
                } else {
                    resolution += harassAndWithdraw(game, faction, regularForcesNotDialed, specialForcesNotDialed, executeResolution);
                    regularForcesTotal -= regularForcesNotDialed;
                    specialForcesTotal -= specialForcesNotDialed;
                }
            }
            resolution += killForces(game, troopFaction, regularForcesTotal, specialForcesTotal, executeResolution);
        } else if (!callsTraitor && (regularForcesDialed > 0 || specialForcesDialed > 0)) {
            if (!(faction instanceof EcazFaction && hasEcazAndAlly())) {
                String savedForceEmoji;
                if (battlePlan.isSkillBehindAndLeaderAlive("Suk Graduate")) {
                    savedSpecialForces = Math.min(specialForcesDialed, 3);
                    savedRegularForces = Math.min(regularForcesDialed, 3 - savedSpecialForces);
                    specialForcesDialed -= savedSpecialForces;
                    regularForcesDialed -= savedRegularForces;
                    if (savedRegularForces > 0 || savedSpecialForces > 0) {
                        resolution += troopFactionEmoji + " saves " + faction.forcesString(savedRegularForces, savedSpecialForces) + " and may leave 1 in the territory with Suk Graduate\n";
                        if (executeResolution) {
                            if (savedSpecialForces > 0) {
                                savedSpecialForces--;
                                savedForceEmoji = Emojis.getForceEmoji(troopFactionName + "*");
                            } else {
                                savedRegularForces--;
                                savedForceEmoji = Emojis.getForceEmoji(troopFactionName);
                            }
                            turnSummary.publish(faction.getEmoji() + " leaves 1 " + savedForceEmoji + " in " + wholeTerritoryName + ", may return it to reserves.");
                            faction.withdrawForces(game, savedRegularForces, savedSpecialForces, territorySectors, "Suk Graduate");
                        }
                    }
                } else if (battlePlan.isSkillInFront("Suk Graduate")) {
                    boolean savedForceIsStarred = false;
                    if (specialForcesDialed > 0) {
                        savedForceEmoji = Emojis.getForceEmoji(troopFactionName + "*");
                        specialForcesDialed--;
                        savedForceIsStarred = true;
                    } else {
                        savedForceEmoji = Emojis.getForceEmoji(troopFactionName);
                        regularForcesDialed--;
                    }
                    if (!savedForceEmoji.isEmpty()) {
                        resolution += troopFactionEmoji + " returns 1 " + savedForceEmoji + " to reserves with Suk Graduate\n";
                        if (executeResolution) {
                            int savedRegular = savedForceIsStarred ? 0 : 1;
                            int savedStarred = savedForceIsStarred ? 1 : 0;
                            faction.withdrawForces(game, savedRegular, savedStarred, territorySectors, "Suk Graduate");
                        }
                    }
                }
            }
            if (!opponentCallsTraitor && canHarassAndWithdraw(faction, weapon, defense)) {
                if (faction instanceof EcazFaction && hasEcazAndAlly()) {
                    ecazForcesWithdrawn = Math.floorDiv(battlePlan.getEcazTroopsForAlly(), 2);
                    resolution += harassAndWithdraw(game, faction, ecazForcesWithdrawn, 0, executeResolution);
                } else {
                    resolution += harassAndWithdraw(game, faction, regularForcesNotDialed, specialForcesNotDialed, executeResolution);
                }
            }
            resolution += killForces(game, troopFaction, regularForcesDialed, specialForcesDialed, executeResolution);
        }
        if (hasEcazAndAlly() && troopFactionName.equals(game.getFaction("Ecaz").getAlly())) {
            if (isLoser) {
                int ecazForcesnotDialed = Math.floorDiv(battlePlan.getEcazTroopsForAlly(), 2);
                int ecazForcesToKill = battlePlan.getEcazTroopsForAlly() - ecazForcesWithdrawn;
                if (faction instanceof EcazFaction && battlePlan.isSkillBehindAndLeaderAlive("Diplomat")) {
                    int leaderValue = battlePlan.getLeaderValue();
                    savedRegularForces = Math.min(ecazForcesnotDialed, leaderValue);
                    if (savedRegularForces > 0) {
                        ecazForcesToKill -= savedRegularForces;
                        resolution += faction.getEmoji() + " may retreat " + faction.forcesString(savedRegularForces, 0) + " to an empty adjacent non-stronghold with Diplomat\n";
                        if (executeResolution) {
                            diplomatMustBeResolved = true;
                            faction.getChat().publish("You may retreat " + faction.forcesString(savedRegularForces, 0) + " to an empty adjacent non-stronghold with Diplomat.\nPlease tell the mod where you would like to move them. " + faction.getPlayer());
                            game.getModInfo().publish(faction.getEmoji() + " retreat with Diplomat must be resolved. " + game.getModOrRoleMention());
                        }
                    }
                }
                resolution += killForces(game, game.getFaction("Ecaz"), ecazForcesToKill, 0, executeResolution);
            } else if (!callsTraitor) {
                int ecazForces = Math.ceilDiv(battlePlan.getEcazTroopsForAlly(), 2);
                if (battlePlan.isSkillBehindAndLeaderAlive("Suk Graduate")) {
                    savedRegularForces = Math.min(ecazForces, 3);
                    ecazForces -= savedRegularForces;
                    resolution += Emojis.ECAZ + " saves " + savedRegularForces + " " + Emojis.ECAZ_TROOP;
                    resolution += " and may leave 1 in the territory with Suk Graduate\n";
                    if (executeResolution) {
                        turnSummary.publish(faction.getEmoji() + " leaves 1 " + Emojis.ECAZ_TROOP + " in " + wholeTerritoryName + ", may return it to reserves.");
                        game.getFaction("Ecaz").withdrawForces(game, 2, 0, territorySectors, "Suk Graduate");
                    }
                } else if (battlePlan.isSkillInFront("Suk Graduate")) {
                    if (ecazForces > 0) {
                        ecazForces--;
                        resolution += Emojis.ECAZ + " returns 1 " + Emojis.ECAZ_TROOP + " to reserves with Suk Graduate\n";
                        if (executeResolution)
                            game.getFaction("Ecaz").withdrawForces(game, 1, 0, territorySectors, "Suk Graduate");
                    }
                }
                resolution += killForces(game, game.getFaction("Ecaz"), ecazForces, 0, executeResolution);
            }
        }
        resolution += handleReinforcements(game, faction, callsTraitor, battlePlan, executeResolution);
        resolution += handleCheapHeroDiscard(faction, callsTraitor, battlePlan, executeResolution);
        resolution += handleWeaponDiscard(faction, callsTraitor, isLoser, battlePlan, executeResolution);
        resolution += handleDefenseDiscard(faction, callsTraitor, isLoser, battlePlan, executeResolution);
        if (!callsTraitor && battlePlan.isJuiceOfSapho() && faction.hasTreacheryCard("Juice of Sapho"))
            resolution += emojis + " discards Juice of Sapho\n";
        if (!callsTraitor && battlePlan.getSpice() > 0) {
            int spiceFromAlly = 0;
            if (faction.hasAlly())
                spiceFromAlly = Math.min(game.getFaction(faction.getAlly()).getBattleSupport(), battlePlan.getSpice());
            resolution += emojis + " loses " + (battlePlan.getSpice() - spiceFromAlly) + " " + Emojis.SPICE + " combat spice";
            if (spiceFromAlly > 0)
                resolution += "\n" + Emojis.getFactionEmoji(faction.getAlly()) + " loses " + spiceFromAlly + " " + Emojis.SPICE + " ally support";
            if (!(faction instanceof ChoamFaction) && game.hasFaction("CHOAM")) {
                int choamEligibleSpice = battlePlan.getSpice();
                if (faction.getAlly().equals("CHOAM"))
                    choamEligibleSpice -= spiceFromAlly;
                if (choamEligibleSpice > 1)
                    resolution += MessageFormat.format(
                            "\n{0} gains {1} {2} combat spice",
                            Emojis.CHOAM, Math.floorDiv(choamEligibleSpice, 2), Emojis.SPICE
                    );
            }
            resolution += "\n";
        }
        if (!callsTraitor && battlePlan.getSpiceBankerSupport() > 0)
            resolution += emojis + " loses " + battlePlan.getSpiceBankerSupport() + " " + Emojis.SPICE + " spent on Spice Banker";

        Territory spiceTerritory = getTerritorySectors().stream().filter(t -> t.getSpice() > 0).findFirst().orElse(null);
        if (!isLoser) {
            if (battlePlan.isSkillInFront("Rihani Decipherer"))
                resolution += troopFactionEmoji + " may peek at 2 random cards in the Traitor Deck with Rihani Decipherer\n";
            else if (battlePlan.isSkillBehindAndLeaderAlive("Rihani Decipherer"))
                resolution += troopFactionEmoji + " may draw 2 Traitor Cards and keep one of them with Rihani Decipherer\n";

            if (spiceTerritory != null && battlePlan.isSkillBehindAndLeaderAlive("Sandmaster"))
                resolution += "3 " + Emojis.SPICE + " will be added to " + spiceTerritory + " with Sandmaster\n";
        }
        if (spiceTerritory != null && battlePlan.isSkillBehindAndLeaderAlive("Smuggler"))
            resolution += troopFactionEmoji + " gains " + Math.min(spiceTerritory.getSpice(), battlePlan.getLeaderValue()) + " " + Emojis.SPICE + " for Smuggler";
        if (!isLasgunShieldExplosion && !bothCallTraitor(game)) {
            if (isLoser) {
                List<TechToken> techTokens = faction.getTechTokens();
                if (techTokens.size() == 1)
                    resolution += emojis + " loses " + Emojis.getTechTokenEmoji(techTokens.getFirst().getName()) + "\n";
                else if (techTokens.size() > 1) {
                    resolution += emojis + " loses a Tech Token: ";
                    resolution += techTokens.stream().map(TechToken::getName).map(Emojis::getTechTokenEmoji).reduce("", String::concat);
                    resolution += "\n";
                }
            } else {
                int combatWater = aggressorPlan.combatWater() + defenderPlan.combatWater();
                if (combatWater > 0)
                    resolution += emojis + " gains " + combatWater + " " + Emojis.SPICE + " combat water\n";
                if (getWholeTerritoryName().equals("Jacurutu Sietch")) {
                    int opponentRegularNotDialed = opponentBattlePlan.getRegularNotDialed();
                    int opponentSpecialNotDialed = opponentBattlePlan.getSpecialNotDialed();
                    String forcesString = "";
                    if (opponentRegularNotDialed > 0)
                        forcesString += " " + opponentRegularNotDialed + " " + Emojis.getForceEmoji(opponentFaction.getName());
                    if (opponentSpecialNotDialed > 0)
                        forcesString += " " + opponentSpecialNotDialed + " " + Emojis.getForceEmoji(opponentFaction.getName() + "*");
                    int ecazForcesNotDialed = Math.floorDiv(opponentBattlePlan.getEcazTroopsForAlly(), 2);
                    if (ecazForcesNotDialed > 0)
                        forcesString += " " + ecazForcesNotDialed + " " + Emojis.ECAZ_TROOP;
                    if (!forcesString.isEmpty())
                        resolution += emojis + " gains " + (opponentRegularNotDialed + opponentSpecialNotDialed + ecazForcesNotDialed) + " " + Emojis.SPICE + " for" + forcesString + " not dialed.\n";
                }
                if (strongholdCardApplies(game, "Sietch Tabr", faction) && opponentBattlePlan.getWholeNumberDial() > 0)
                    resolution += emojis + " gains " + opponentBattlePlan.getWholeNumberDial() + " " + Emojis.SPICE + " for Sietch Tabr stronghold card\n";
            }
        }
        if (strongholdCardApplies(game, "Tuek's Sietch", faction)) {
            int worthlessCardSpice = 0;
            if (battlePlan.getWeapon() != null && battlePlan.getWeapon().type().equals("Worthless Card")) worthlessCardSpice += 2;
            if (battlePlan.getDefense() != null && battlePlan.getDefense().type().equals("Worthless Card")) worthlessCardSpice += 2;
            if (worthlessCardSpice > 0)
                resolution += emojis + " gains " + worthlessCardSpice + " " + Emojis.SPICE + " for Tuek's Sietch stronghold card\n";
        }
        Faction winner = isAggressorWin(game) ? getAggressor(game) : getDefender(game);
        Faction loser = isAggressorWin(game) ? getDefender(game) : getAggressor(game);
        if (!isLoser && winner instanceof HarkonnenFaction)
            resolution += Emojis.HARKONNEN + " captures a " + loser.getEmoji() + " leader\n";
        if (!isLoser && winner instanceof AtreidesFaction atreides) {
            if (game.hasGameOption(GameOption.HOMEWORLDS) && atreides.isHighThreshold() && !wholeTerritoryName.equals("Caladan")
                    && regularForcesTotal - regularForcesDialed > 0 && (atreides.getReservesStrength() > 0 || regularForcesDialed > 0)) {
                resolution += Emojis.ATREIDES + " may add 1 " + Emojis.ATREIDES_TROOP + " from reserves to " + wholeTerritoryName + " with Caladan High Threshold\n";
            }
        }

        if (!resolution.isEmpty()) resolution = "\n" + resolution;
        return resolution;
    }

    private String killForces(Game game, Faction faction, int regularLeftToKill, int starredLeftToKill, boolean executeResolution) {
        String resolution = "";
        if (regularLeftToKill > 0 || starredLeftToKill > 0) {
            if (executeResolution)
                for (Territory t : territorySectors) {
                    if (regularLeftToKill == 0 && starredLeftToKill == 0)
                        break;
                    int regularPresent = t.getForceStrength(faction.getName());
                    int starredPresent = t.getForceStrength(faction.getName() + "*");
                    int regularToKillNow = Math.min(regularLeftToKill, regularPresent);
                    int starredToKillNow = Math.min(starredLeftToKill, starredPresent);
                    regularLeftToKill -= regularToKillNow;
                    starredLeftToKill -= starredToKillNow;
                    if (regularToKillNow > 0 || starredToKillNow > 0)
                        game.removeForcesAndReportToTurnSummary(t.getTerritoryName(), faction, regularToKillNow, starredToKillNow, true);
                }
            else
                resolution += faction.getEmoji() + " loses " + faction.forcesString(regularLeftToKill, starredLeftToKill) + " to the tanks\n";
        }
        return resolution;
    }

    private boolean canHarassAndWithdraw(Faction faction, TreacheryCard weapon, TreacheryCard defense) {
        // Replace first line below if Lasgun-Shield takes precedence over returning forces to reserves
//        return (!isLasgunShieldExplosion && (weapon != null && weapon.name().equals("Harass and Withdraw") || defense != null && defense.name().equals("Harass and Withdraw"))
        return (weapon != null && weapon.name().equals("Harass and Withdraw") || defense != null && defense.name().equals("Harass and Withdraw"))
                && !faction.getHomeworld().equals(wholeTerritoryName) && !(faction instanceof EmperorFaction emperor && emperor.getSecondHomeworld().equals(wholeTerritoryName));
    }

    private String harassAndWithdraw(Game game, Faction faction, int regularForcesNotDialed, int specialForcesNotDialed, boolean executeResolution) {
        String resolution = "";
        if (executeResolution)
            faction.withdrawForces(game, regularForcesNotDialed, specialForcesNotDialed, territorySectors, "Harass and Withdraw");
        else
            resolution += faction.getEmoji() + " returns " + faction.forcesString(regularForcesNotDialed, specialForcesNotDialed) +  " to reserves with Harass and Withdraw\n";
        return resolution;
    }

    private String handleReinforcements(Game game, Faction faction, boolean callsTraitor, BattlePlan battlePlan, boolean executeResolution) {
        String resolution = "";
        TreacheryCard weapon = battlePlan.getWeapon();
        TreacheryCard defense = battlePlan.getDefense();
        if (!callsTraitor && (weapon != null && weapon.name().equals("Reinforcements") || defense != null && defense.name().equals("Reinforcements"))) {
            if (executeResolution) {
                if (battlePlan.getNumForcesInReserve() < 3)
                    game.getModInfo().publish("Reinforcements requires 3 forces in reserves. Sending all reserves to the tanks. " + game.getModOrRoleMention());
                int reservesLeftToKill = 3;
                Territory homeworld = game.getTerritory(faction.getHomeworld());
                int reservesToKillNow = Math.min(reservesLeftToKill, homeworld.getForceStrength(faction.getName()));
                if (reservesToKillNow > 0)
                    game.removeForcesAndReportToTurnSummary(homeworld.getTerritoryName(), faction, reservesToKillNow, 0, true);
                reservesLeftToKill -= reservesToKillNow;
                if (faction instanceof EmperorFaction emperor) {
                    Territory secondHomeworld = game.getTerritory(emperor.getSecondHomeworld());
                    reservesToKillNow = Math.min(reservesLeftToKill, secondHomeworld.getForceStrength(faction.getName()));
                    if (reservesToKillNow > 0)
                        game.removeForcesAndReportToTurnSummary(secondHomeworld.getTerritoryName(), faction, reservesToKillNow, 0, true);
                    reservesLeftToKill -= reservesToKillNow;
                }
                reservesToKillNow = Math.min(reservesLeftToKill, homeworld.getForceStrength(faction.getName() + "*"));
                if (reservesToKillNow > 0)
                    game.removeForcesAndReportToTurnSummary(homeworld.getTerritoryName(), faction, 0, reservesToKillNow, true);
                reservesLeftToKill -= reservesToKillNow;
                if (faction instanceof EmperorFaction emperor) {
                    Territory secondHomeworld = game.getTerritory(emperor.getSecondHomeworld());
                    reservesToKillNow = Math.min(reservesLeftToKill, secondHomeworld.getForceStrength(faction.getName() + "*"));
                    if (reservesToKillNow > 0)
                        game.removeForcesAndReportToTurnSummary(secondHomeworld.getTerritoryName(), faction, 0, reservesToKillNow, true);
                }
            } else {
                resolution += faction.getEmoji() + " must send 3 forces from reserves to the tanks for Reinforcements\n";
            }
        }
        return resolution;
    }

    private String handleCheapHeroDiscard(Faction faction, boolean callsTraitor, BattlePlan battlePlan, boolean executeResolution) {
        String resolution = "";
        if (!callsTraitor && battlePlan.getCheapHero() != null) {
            if (executeResolution)
                faction.discard(battlePlan.getCheapHero().name());
            else
                resolution += faction.getEmoji() + " discards " + battlePlan.getCheapHero().name() + "\n";
        }
        return resolution;
    }

    private String handleWeaponDiscard(Faction faction, boolean callsTraitor, boolean isLoser, BattlePlan battlePlan, boolean executeResolution) {
        String resolution = "";
        if (!callsTraitor && battlePlan.weaponMustBeDiscarded(isLoser)) {
            if (executeResolution)
                faction.discard(battlePlan.getWeapon().name());
            else
                resolution += faction.getEmoji() + " discards " + battlePlan.getWeapon().name() + "\n";
        }
        return resolution;
    }

    private String handleDefenseDiscard(Faction faction, boolean callsTraitor, boolean isLoser, BattlePlan battlePlan, boolean executeResolution) {
        String resolution = "";
        if (!callsTraitor && battlePlan.defenseMustBeDiscarded(isLoser)) {
            if (executeResolution)
                faction.discard(battlePlan.getDefense().name());
            else
                resolution += faction.getEmoji() + " discards " + battlePlan.getDefense().name() + "\n";
        }
        return resolution;
    }

    private boolean strongholdCardApplies(Game game, String stronghold, Faction faction) {
        return game.hasGameOption(GameOption.STRONGHOLD_SKILLS)
                && (getWholeTerritoryName().equals(stronghold) && faction.hasStrongholdCard(stronghold)
                || getWholeTerritoryName().equals("Hidden Mobile Stronghold") && faction.hasHmsStrongholdProxy(stronghold));
    }

    private String getWinnerString(Game game) throws InvalidGameStateException {
        String resolution = "";
        if (bothCallTraitor(game)) {
            resolution += getTraitorCallString(game, getAggressor(game), defenderBattlePlan.getLeaderNameForTraitor()) + "\n";
            resolution += getTraitorCallString(game, getDefender(game), aggressorBattlePlan.getLeaderNameForTraitor()) + "\n";
            resolution += "**Nobody wins**";
        } else if (aggressorCallsTraitor(game)) {
            resolution += getTraitorCallString(game, getAggressor(game), defenderBattlePlan.getLeaderNameForTraitor()) + "\n";
            resolution += getAggressorEmojis(game) + " **wins with no losses**";
        } else if (defenderCallsTraitor(game)) {
            resolution += getTraitorCallString(game, getDefender(game), aggressorBattlePlan.getLeaderNameForTraitor()) + "\n";
            resolution += getDefenderEmojis(game) + " **wins with no losses**";
        } else if (aggressorBattlePlan.isLasgunShieldExplosion()) {
            resolution += "**KABOOM!**";
        } else {
            BattlePlan winnerPlan = isAggressorWin(game) ? aggressorBattlePlan : defenderBattlePlan;
            BattlePlan loserPlan = isAggressorWin(game) ? defenderBattlePlan : aggressorBattlePlan;
            resolution += MessageFormat.format("{0} **wins {1} - {2}**",
                    getWinnerEmojis(game), winnerPlan.getTotalStrengthString(), loserPlan.getTotalStrengthString()
            );
            if (winnerPlan.getTotalStrengthString().equals(loserPlan.getTotalStrengthString())
                    && !isAggressorWin(game)) {
                if (winnerPlan.isJuiceOfSapho())
                    resolution += getWinnerEmojis(game) + " wins tie due to Juice of Sapho.";
                else
                    resolution += getWinnerEmojis(game) + " wins tie due to Habbanya Sietch stronghold card.";
            }
        }
        return resolution;
    }

    public String getTraitorCallString(Game game, Faction faction, String opponentLeader) throws InvalidGameStateException {
        if (faction.hasTraitor(opponentLeader))
            return faction.getEmoji() + " calls Traitor against " + opponentLeader + "!";
        else if (faction.getAlly().equals("Harkonnen") && game.getFaction("Harkonnen").hasTraitor(opponentLeader))
            return Emojis.HARKONNEN + " calls Traitor against " + opponentLeader + "!";
        else
            throw new InvalidGameStateException("Traitor not found.");
    }

    public void battleResolution(Game game, boolean publishToTurnSummary, boolean playedJuiceOfSapho, boolean noKillStoneBurner, boolean portableSnooper, boolean noPoisonTooth, boolean aggressorCallsTraitor, boolean defenderCallsTraitor, boolean overrideDecisions) throws InvalidGameStateException {
        BattlePlan aggressorPlan = getAggressorBattlePlan();
        BattlePlan defenderPlan = getDefenderBattlePlan();
        if (aggressorPlan == null || defenderPlan == null)
            throw new InvalidGameStateException("Battle cannot be resolved yet. Missing battle plan(s).");
        boolean persistOverride = true;
        if (isSpiceBankerDecisionOpen()) {
            if (overrideDecisions) {
                game.getModInfo().publish(getSpiceBankerFactionEmoji() + " Spice Banker decision has been overridden.");
                spiceBankerTBD = DecisionStatus.CLOSED;
                persistOverride = false;
            } else if (publishToTurnSummary)
                throw new InvalidGameStateException(getSpiceBankerFactionEmoji() + " must decide on Spice Banker");
        }
        if (isHMSCardDecisionOpen()) {
            if (overrideDecisions) {
                game.getModInfo().publish(getHmsStrongholdCardFactionEmoji() + " HMS Stronghold Card decision has been overridden.");
                hmsStrongholdCardTBD = DecisionStatus.CLOSED;
                persistOverride = false;
            } else if (publishToTurnSummary)
                throw new InvalidGameStateException(getHmsStrongholdCardFactionEmoji() + " must decide on HMS Stronghold Card");
        }
        if (persistOverride)
            this.overrideDecisions = overrideDecisions;

        if (aggressorCallsTraitor) {
            aggressorPlan.setWillCallTraitor(true);
            aggressorPlan.setHarkWillCallTraitor(true);
        }
        if (defenderCallsTraitor) {
            defenderPlan.setWillCallTraitor(true);
            defenderPlan.setHarkWillCallTraitor(true);
        }

        defenderPlan.setJuiceOfSapho(playedJuiceOfSapho);

        boolean reRevealBattlePlans = false;
        if (noKillStoneBurner) {
            aggressorPlan.dontKillWithStoneBurner();
            defenderPlan.dontKillWithStoneBurner();
            reRevealBattlePlans = true;
        }

        if (portableSnooper) {
            if (getAggressor(game).hasTreacheryCard("Portable Snooper"))
                aggressorPlan.addPortableSnooper();
            if (getDefender(game).hasTreacheryCard("Portable Snooper"))
                defenderPlan.addPortableSnooper();
            reRevealBattlePlans = true;
        }

        if (noPoisonTooth) {
            aggressorPlan.revokePoisonTooth();
            defenderPlan.revokePoisonTooth();
            reRevealBattlePlans = true;
        }

        if (reRevealBattlePlans) {
            aggressorPlan.revealOpponentBattlePlan(defenderPlan);
            defenderPlan.revealOpponentBattlePlan(aggressorPlan);
            if (aggressorPlan.isOpponentHasBureaucrat())
                aggressorPlan.revealOpponentBattlePlan(defenderPlan);
        }

        printBattleResolution(game, publishToTurnSummary);
        if (!publishToTurnSummary && (overrideDecisions || !isSpiceBankerDecisionOpen() && !isHMSCardDecisionOpen())) {
            String publishChoiceId = "battle-publish-resolution-turn-" + game.getTurn() + "-" + wholeTerritoryName;
            game.getModInfo().publish("Use this button to publish the above resolution to turn summary.", List.of(new DuneChoice(publishChoiceId, "Publish")));
        }
    }

    public void printBattleResolution(Game game, boolean publishToTurnSummary, int turn, String territoryName) throws InvalidGameStateException {
        if (game.getTurn() != turn)
            throw new InvalidGameStateException("It is not turn " + turn);
        if (!wholeTerritoryName.equals(territoryName))
            throw new InvalidGameStateException("The current battle is not in " + territoryName);
        printBattleResolution(game, publishToTurnSummary);
    }

    public void printBattleResolution(Game game, boolean publishToTurnSummary) throws InvalidGameStateException {
        printBattleResolution(game, publishToTurnSummary, false);
    }

    public void printBattleResolution(Game game, boolean publishToTurnSummary, boolean executeResolution) throws InvalidGameStateException {
        resolutionPublished = publishToTurnSummary;
        Faction aggressor = getAggressor(game);
        Faction defender = getDefender(game);
        String wholeTerritoryName = getWholeTerritoryName();
        String resolution = "";
        if (eitherCallsTraitor(game))
            resolution += "**TRAITOR!**\n\n";
        resolution += MessageFormat.format("{0} **vs {1} in {2}**\n\n",
                getAggressorEmojis(game), getDefenderEmojis(game), wholeTerritoryName
        );

        RicheseFaction richeseFaction = null;
        if (game.hasFaction("Richese"))
            richeseFaction = (RicheseFaction) game.getFaction("Richese");
        Integer noFieldValue = getForces().stream().filter(f -> f.getName().equals("NoField")).map(Force::getStrength).findFirst().orElse(null);
        if (noFieldValue != null) {
            resolution += MessageFormat.format("{0} reveals {1} to be {2} {3}\n\n", Emojis.RICHESE, Emojis.NO_FIELD, noFieldValue, Emojis.RICHESE_TROOP);
            if (executeResolution && richeseFaction != null)
                richeseFaction.revealNoField(game);
        }

        resolution += getAggressorEmojis(game) + "\n";
        resolution += aggressorBattlePlan.getPlanMessage(true) + "\n\n";
        resolution += getDefenderEmojis(game) + "\n";
        resolution += defenderBattlePlan.getPlanMessage(true) + "\n\n";
        resolution += getWinnerString(game) + "\n";

        resolution += factionBattleResults(game, true, executeResolution);
        resolution += factionBattleResults(game, false, executeResolution);

        String carnage = lasgunShieldCarnage(game);
        if (!carnage.isEmpty())
            resolution += "\nLasgun-Shield carnage:\n" + carnage;
        resolution += aggressorBattlePlan.checkAuditor(defender.getEmoji());
        resolution += defenderBattlePlan.checkAuditor(aggressor.getEmoji());

        if (isSpiceBankerDecisionOpen() && !publishToTurnSummary)
            resolution += "\nBattle cannot be resolved yet.\n" + spiceBankerFactionEmoji + " must decide on Spice Banker\n";
        if (isHMSCardDecisionOpen() && !publishToTurnSummary)
            resolution += "\nBattle cannot be resolved yet.\n" + hmsStrongholdCardFactionEmoji + " must decide on HMS Stronghold Card\n";

        String resolutionDecisions = "";
        boolean saphoHasBeenAuctioned = false;
        boolean portableSnooperHasBeenAuctioned = false;
        if (richeseFaction != null) {
            saphoHasBeenAuctioned = richeseFaction.getTreacheryCardCache().stream().noneMatch(c -> c.name().equals("Juice of Sapho"));
            portableSnooperHasBeenAuctioned = richeseFaction.getTreacheryCardCache().stream().noneMatch(c -> c.name().equals("Portable Snooper"));
        }
        if (portableSnooperHasBeenAuctioned)
            resolutionDecisions += portableSnooperDecision(aggressor, aggressorBattlePlan, publishToTurnSummary);
        resolutionDecisions += stoneBurnerDecision(aggressor, true, aggressorBattlePlan, defenderBattlePlan, publishToTurnSummary);
        resolutionDecisions += poisonToothDecision(aggressor, aggressorBattlePlan, publishToTurnSummary);
        if (saphoHasBeenAuctioned)
            resolutionDecisions += juiceOfSaphoDecision(defender, defenderBattlePlan, publishToTurnSummary);
        if (portableSnooperHasBeenAuctioned)
            resolutionDecisions += portableSnooperDecision(defender, defenderBattlePlan, publishToTurnSummary);
        resolutionDecisions += stoneBurnerDecision(defender, false, defenderBattlePlan, aggressorBattlePlan, publishToTurnSummary);
        resolutionDecisions += poisonToothDecision(defender, defenderBattlePlan, publishToTurnSummary);
        if (!resolutionDecisions.isEmpty())
            resolution += "\n" + resolutionDecisions;

        if (!executeResolution) {
            DuneTopic resultsChannel = publishToTurnSummary ? game.getTurnSummary() : game.getModInfo();
            resultsChannel.publish(resolution);

            checkForTraitorCall(game, getAggressor(game), aggressorBattlePlan, getDefender(game), defenderBattlePlan, publishToTurnSummary);
            checkForTraitorCall(game, getDefender(game), defenderBattlePlan, getAggressor(game), aggressorBattlePlan, publishToTurnSummary);
            if (publishToTurnSummary)
                checkIfResolvable(game);
        }
    }

    public void resolveBattle(Game game, boolean publishToTurnSummary, int turn, String territoryName) throws InvalidGameStateException {
        if (game.getTurn() != turn)
            throw new InvalidGameStateException("It is not turn " + turn);
        if (!wholeTerritoryName.equals(territoryName))
            throw new InvalidGameStateException("The current battle is not in " + territoryName);
        printBattleResolution(game, publishToTurnSummary, true);
    }

    private void checkForTraitorCall(Game game, Faction faction, BattlePlan battlePlan, Faction opponent, BattlePlan opponentPlan, boolean publishToTurnSummary) {
        DuneTopic modInfo = game.getModInfo();
        if (faction instanceof BTFaction) {
            if (!publishToTurnSummary)
                modInfo.publish(faction.getEmoji() + " does not call Traitors.");
            battlePlan.setCanCallTraitor(false);
            return;
        } else if (battlePlan.isDeclinedTraitor()) {
            if (!publishToTurnSummary)
                modInfo.publish(faction.getEmoji() + " declined calling Traitor in " + wholeTerritoryName + ".");
            return;
        } else if (opponentPlan.hasKwisatzHaderach()) {
            if (!publishToTurnSummary)
                modInfo.publish(faction.getEmoji() + " cannot call Traitor against Kwisatz Haderach.");
            battlePlan.setCanCallTraitor(false);
            return;
        }

        checkForTraitorCall(game, faction, faction, battlePlan, opponent, opponentPlan, false, modInfo, publishToTurnSummary);
        if (faction.getAlly().equals("Harkonnen"))
            checkForTraitorCall(game, game.getFaction("Harkonnen"), faction, battlePlan, opponent, opponentPlan, true, modInfo, publishToTurnSummary);
    }

    private void checkForTraitorCall(Game game, Faction faction, Faction combatant, BattlePlan battlePlan, Faction opponent, BattlePlan opponentPlan, boolean isHarkonnenAllyPower, DuneTopic modInfo, boolean publishToTurnSummary) {
        String opponentLeader = opponentPlan.getLeaderNameForTraitor();
        String forYourAlly = isHarkonnenAllyPower ? "for your ally " : "";
        if (faction.hasTraitor(opponentLeader)) {
            if (publishToTurnSummary) {
                if (!isHarkonnenAllyPower && battlePlan.isWillCallTraitor()) {
                    faction.getChat().publish(opponentLeader + " has betrayed " + opponent.getEmoji() + " for you!");
                } else if (isHarkonnenAllyPower && battlePlan.isHarkWillCallTraitor()) {
                    faction.getChat().publish(opponentLeader + " has betrayed " + opponent.getEmoji() + " for your ally!");
                    combatant.getChat().publish(opponentLeader + " has betrayed " + opponent.getEmoji() + " for " + Emojis.HARKONNEN + " and you!");
                } else if (!isHarkonnenAllyPower && battlePlan.isCanCallTraitor() || isHarkonnenAllyPower && battlePlan.isHarkCanCallTraitor()) {
                    List<DuneChoice> choices = new ArrayList<>();
                    choices.add(new DuneChoice("traitor-call-yes-turn-" + game.getTurn() + "-" + wholeTerritoryName, "Yes"));
                    choices.add(new DuneChoice("traitor-call-no-turn-" + game.getTurn() + "-" + wholeTerritoryName, "No"));
                    faction.getChat().publish("Will you call Traitor " + forYourAlly + "against " + opponentLeader + " in " + wholeTerritoryName + "? " + faction.getPlayer(), choices);
                }
            } else if (!isHarkonnenAllyPower && battlePlan.isWillCallTraitor() || isHarkonnenAllyPower && battlePlan.isHarkWillCallTraitor()) {
                modInfo.publish(faction.getEmoji() + " will call Traitor in " + wholeTerritoryName + ".");
            } else if (!isHarkonnenAllyPower && battlePlan.isDeclinedTraitor() || isHarkonnenAllyPower && battlePlan.isHarkDeclinedTraitor()) {
                modInfo.publish(faction.getEmoji() + " declined Traitor call in " + wholeTerritoryName + ".");
            } else {
                modInfo.publish(faction.getEmoji() + " can call Traitor against " + opponentLeader + " in " + wholeTerritoryName + ".");
                battlePlan.setCanCallTraitor(true);
            }
        } else if (!publishToTurnSummary) {
            modInfo.publish(faction.getEmoji() + " cannot call Traitor in " + wholeTerritoryName + ".");
            if (isHarkonnenAllyPower)
                battlePlan.setHarkCanCallTraitor(false);
            else
                battlePlan.setCanCallTraitor(false);
        }
    }

    public boolean isResolutionPublished() {
        return resolutionPublished;
    }

    public void willCallTraitor(Game game, Faction faction, boolean willCallTraitor, int turn, String wholeTerritoryForTraitor) throws InvalidGameStateException {
        if (turn != game.getTurn())
            throw new InvalidGameStateException("It is no longer turn " + turn);
        if (!wholeTerritoryForTraitor.equals(wholeTerritoryName))
            throw new InvalidGameStateException("The current battle is not in " + wholeTerritoryForTraitor);

        BattlePlan plan;
        BattlePlan opponentPlan;
        Faction opponent;
        if (faction == getAggressor(game) || faction instanceof HarkonnenFaction && faction.getAlly().equals(getAggressorName())) {
            plan = aggressorBattlePlan;
            opponentPlan = defenderBattlePlan;
            opponent = getDefender(game);
        } else if (faction == getDefender(game) || faction instanceof HarkonnenFaction && faction.getAlly().equals(getDefenderName())) {
            plan = defenderBattlePlan;
            opponentPlan = aggressorBattlePlan;
            opponent = getAggressor(game);
        } else
            throw new InvalidGameStateException(faction.getName() + " does not have a battle plan for this battle.");

        boolean isHarkonnenAllyPower = faction instanceof HarkonnenFaction && faction != getAggressor(game) && faction != getDefender(game);
        if (isHarkonnenAllyPower) {
            String harkAllyEmoji = Emojis.getFactionEmoji(faction.getAlly());
            if (willCallTraitor) {
                if (resolutionPublished && faction.hasTraitor(opponentPlan.getLeaderNameForTraitor())) {
                    game.getModInfo().publish(faction.getEmoji() + " calls Traitor for " + harkAllyEmoji + " in " + wholeTerritoryName + "!");
                    plan.setHarkWillCallTraitor(true);
                    printBattleResolution(game, true);
                } else if (!resolutionPublished && plan.isHarkCanCallTraitor()) {
                    plan.setHarkWillCallTraitor(true);
                    game.getModInfo().publish(faction.getEmoji() + " will call Traitor for " + harkAllyEmoji + " in " + wholeTerritoryName + " if possible.");
                } else {
                    faction.getChat().publish("You cannot call Traitor for " + harkAllyEmoji + ".");
                }
                plan.setHarkCanCallTraitor(false);
            } else if (plan.isHarkCanCallTraitor()) {
                plan.setHarkDeclinedTraitor(true);
                game.getModInfo().publish(faction.getEmoji() + " declines calling Traitor for " + harkAllyEmoji + " in " + wholeTerritoryName + ".");
                plan.setHarkCanCallTraitor(false);
            }
        } else if (willCallTraitor) {
            if (resolutionPublished && faction.hasTraitor(opponentPlan.getLeaderNameForTraitor())) {
                game.getModInfo().publish(faction.getEmoji() + " calls Traitor in " + wholeTerritoryName + "!");
                plan.setWillCallTraitor(true);
                printBattleResolution(game, true);
            } else if (!resolutionPublished && plan.isCanCallTraitor()) {
                plan.setWillCallTraitor(true);
                game.getModInfo().publish(faction.getEmoji() + " will call Traitor in " + wholeTerritoryName + " if possible.");
                if (faction.getAlly().equals("Harkonnen")) {
                    plan.presentEarlyTraitorChoices(game, game.getFaction("Harkonnen"), opponent, true);
                    if (plan.isHarkCanCallTraitor())
                        game.getModInfo().publish(Emojis.HARKONNEN + " can call Traitor for ally " + faction.getEmoji() + " in " + wholeTerritoryName + ".");
                }
            } else {
                faction.getChat().publish("You cannot call Traitor.");
            }
            plan.setCanCallTraitor(false);
        } else if (plan.isCanCallTraitor()) {
            plan.setDeclinedTraitor(true);
            game.getModInfo().publish(faction.getEmoji() + " declines calling Traitor in " + wholeTerritoryName + ".");
            plan.setCanCallTraitor(false);
        }
    }

    public boolean mightCallTraitor(Game game, Faction faction, int turn, String wholeTerritoryForTraitor) throws InvalidGameStateException {
        if (turn != game.getTurn())
            throw new InvalidGameStateException("It is no longer turn " + turn);
        if (!wholeTerritoryForTraitor.equals(wholeTerritoryName))
            throw new InvalidGameStateException("The current battle is not in " + wholeTerritoryForTraitor);

        BattlePlan plan;
        Faction opponent;
        if (faction == getAggressor(game) || faction instanceof HarkonnenFaction && faction.getAlly().equals(getAggressorName())) {
            plan = aggressorBattlePlan;
            opponent = getDefender(game);
        } else if (faction == getDefender(game) || faction instanceof HarkonnenFaction && faction.getAlly().equals(getDefenderName())) {
            plan = defenderBattlePlan;
            opponent = getAggressor(game);
        } else
            throw new InvalidGameStateException(faction.getName() + " does not have a battle plan for this battle.");

        if (!resolutionPublished) {
            game.getModInfo().publish(faction.getEmoji() + " will wait for battle wheels  to decide whether to call Traitor in " + wholeTerritoryName);
            if (faction.getAlly().equals("Harkonnen")) {
                plan.presentEarlyTraitorChoices(game, game.getFaction("Harkonnen"), opponent, true);
                if (plan.isHarkCanCallTraitor())
                    game.getModInfo().publish(Emojis.HARKONNEN + " can call Traitor for ally " + faction.getEmoji() + " in " + wholeTerritoryName + ".");
                else
                    game.getModInfo().publish(Emojis.HARKONNEN + " cannot call Traitor for ally " + faction.getEmoji() + " in " + wholeTerritoryName + ".");
            }
        }
        return plan.isHarkCanCallTraitor();
    }

    private String lasgunShieldCarnage(Game game) {
        List<Territory> allTerritorySectors = game.getTerritories().values().stream().filter(t -> t.getTerritoryName().startsWith(wholeTerritoryName)).toList();
        String carnage = "";
        if (aggressorBattlePlan.isLasgunShieldExplosion() && !aggressorCallsTraitor(game) && !defenderCallsTraitor(game)) {
            String otherForces = nonCombatantForces(allTerritorySectors);
            if (!otherForces.isEmpty())
                carnage += otherForces;
            Territory noFieldTerritory = allTerritorySectors.stream().filter(Territory::hasRicheseNoField).findFirst().orElse(null);
            if (noFieldTerritory != null)
                carnage += Emojis.RICHESE + " reveals " + Emojis.NO_FIELD + " to be " + noFieldTerritory.getRicheseNoField() + " " + Emojis.RICHESE_TROOP + " and loses them to the tanks\n";
            String ambassador = allTerritorySectors.stream().map(Territory::getEcazAmbassador).filter(Objects::nonNull).findFirst().orElse(null);
            if (ambassador != null)
                carnage += Emojis.ECAZ + " " + ambassador + " ambassador returned to supply\n";
            carnage += String.join("", allTerritorySectors.stream().filter(s -> s.getSpice() > 0).map(s -> s.getSpice() + " " + Emojis.SPICE + " destroyed in " + s.getTerritoryName() + "\n").toList());
        }
        return carnage;
    }

    private String nonCombatantForces(List<Territory> allTerritorySectors) {
        return String.join("", allTerritorySectors.stream().map(this::nonCombatantForcesInSector).toList());
    }

    private String nonCombatantForcesInSector(Territory t) {
        boolean battleSector = territorySectors.contains(t);
        List<Force> nonCombatantForces = new ArrayList<>(t.getForces().stream().filter(f -> !battleSector || !factionNames.contains(f.getFactionName())).toList());
        nonCombatantForces.sort((a, b) -> {
            if (a.getFactionName().equals(b.getFactionName()))
                return a.getName().compareTo(b.getName());
            else
                return a.getFactionName().compareTo(b.getFactionName());
        });
        return String.join("", nonCombatantForces.stream().map(f -> Emojis.getFactionEmoji(f.getFactionName()) + " loses " + f.getStrength() + " " + Emojis.getForceEmoji(f.getName()) + " in " + t.getTerritoryName() + " to the tanks\n").toList());
    }

    private String juiceOfSaphoDecision(Faction faction, BattlePlan battlePlan, boolean publishToTurnSummary) {
        String decisionAnnouncement = "";
        if (juiceOfSaphoTBD != DecisionStatus.CLOSED && battlePlan.getDefense() == null) {
            juiceOfSaphoTBD = DecisionStatus.OPEN;
            boolean factionHasJuiceOfSapho = faction.hasTreacheryCard("Juice of Sapho");
//            String ifTheyHaveIt = publishToTurnSummary ? " if they have it" : "";
            if (!publishToTurnSummary && factionHasJuiceOfSapho)
//                decisionAnnouncement += faction.getEmoji() + " may play Juice of Sapho" + ifTheyHaveIt + ".\n";
                decisionAnnouncement += faction.getEmoji() + " may play Juice of Sapho.\n";
            if (publishToTurnSummary && factionHasJuiceOfSapho) {
                List<DuneChoice> choices = new LinkedList<>();
                choices.add(new DuneChoice("battle-juice-of-sapho-add", "Yes, add it"));
                choices.add(new DuneChoice("battle-juice-of-sapho-don't-add", "No, keep it out"));
                faction.getChat().publish("Will you play Juice of Sapho to become the aggressor in " + wholeTerritoryName + "? " + faction.getPlayer(), choices);
            }
        }
        return decisionAnnouncement;
    }

    private String portableSnooperDecision(Faction faction, BattlePlan battlePlan, boolean publishToTurnSummary) {
        String decisionAnnouncement = "";
        if (portableSnooperTBD != DecisionStatus.CLOSED && battlePlan.getDefense() == null) {
            portableSnooperTBD = DecisionStatus.OPEN;
            boolean factionHasPortableSnooper = faction.hasTreacheryCard("Portable Snooper");
//            String ifTheyHaveIt = publishToTurnSummary ? " if they have it" : "";
            if (!publishToTurnSummary && factionHasPortableSnooper)
//                decisionAnnouncement += faction.getEmoji() + " may play Portable Snooper" + ifTheyHaveIt + ".\n";
                decisionAnnouncement += faction.getEmoji() + " may play Portable Snooper.\n";
            if (publishToTurnSummary && factionHasPortableSnooper) {
                List<DuneChoice> choices = new LinkedList<>();
                choices.add(new DuneChoice("battle-portable-snooper-add", "Yes, add it"));
                choices.add(new DuneChoice("battle-portable-snooper-don't-add", "No, keep it out"));
                faction.getChat().publish("Will you play Portable Snooper in " + wholeTerritoryName + "? " + faction.getPlayer(), choices);
            }
        }
        return decisionAnnouncement;
    }

    private String stoneBurnerDecision(Faction faction, boolean isAggressor, BattlePlan battlePlan, BattlePlan opponentPlan, boolean publishToTurnSummary) {
        String decisionAnnouncement = "";
        boolean askNow = false;
        boolean playedMirrorWeapon = battlePlan.getWeapon() != null && battlePlan.getWeapon().name().equals("Mirror Weapon");
        if (stoneBurnerTBD != DecisionStatus.CLOSED && battlePlan.getWeapon() != null && battlePlan.getWeapon().name().equals("Stone Burner")) {
            stoneBurnerTBD = DecisionStatus.OPEN;
            if (isAggressor || mirrorWeaponStoneBurnerTBD != DecisionStatus.OPEN)
                askNow = true;
        } else if (mirrorWeaponStoneBurnerTBD != DecisionStatus.CLOSED && playedMirrorWeapon
                && opponentPlan.getWeapon() != null && opponentPlan.getWeapon().name().equals("Stone Burner")) {
            mirrorWeaponStoneBurnerTBD = DecisionStatus.OPEN;
            if (isAggressor || stoneBurnerTBD != DecisionStatus.OPEN)
                askNow = true;
        }
        if (askNow) {
            String weapon = battlePlan.getWeapon().name() + (playedMirrorWeapon ? " (Stone Burner)" : "");
            decisionAnnouncement += faction.getEmoji() + " must decide if they will kill both leaders with " + weapon + ".\n";
            if (publishToTurnSummary) {
                List<DuneChoice> choices = new LinkedList<>();
                choices.add(new DuneChoice("battle-stone-burner-kill", "Yes, kill them both"));
                choices.add(new DuneChoice("battle-stone-burner-no-kill", "No, let them live"));
                faction.getChat().publish("Will you kill both leaders in " + wholeTerritoryName + "? " + faction.getPlayer(), choices);
            }
        }
        return decisionAnnouncement;
    }

    private String poisonToothDecision(Faction faction, BattlePlan battlePlan, boolean publishToTurnSummary) {
        String decisionAnnouncement = "";
        if (poisonToothTBD != DecisionStatus.CLOSED && battlePlan.getWeapon() != null && battlePlan.getWeapon().name().equals("Poison Tooth")) {
            poisonToothTBD = DecisionStatus.OPEN;
            decisionAnnouncement += faction.getEmoji() + " must decide if they will remove Poison Tooth.\n";
            if (publishToTurnSummary) {
                List<DuneChoice> choices = new LinkedList<>();
                choices.add(new DuneChoice("battle-poison-tooth-remove", "Yes, remove it"));
                choices.add(new DuneChoice("battle-poison-tooth-keep", "No, keep it in"));
                faction.getChat().publish("Will you remove Poison Tooth from your plan in " + wholeTerritoryName + "? " + faction.getPlayer(), choices);
            }
        }
        return decisionAnnouncement;
    }

    public void juiceOfSaphoAdd(Game game, Faction faction) throws InvalidGameStateException {
        boolean wasAggressorLeaderAlive = aggressorBattlePlan.isLeaderAlive();
        boolean wasDefenderLeaderAlive = defenderBattlePlan.isLeaderAlive();
        String oldResolutionString = getWinnerString(game);
        int combatWaterBefore = aggressorBattlePlan.combatWater() + defenderBattlePlan.combatWater();
        if (getDefenderName().equals(faction.getName())) {
            defenderBattlePlan.setJuiceOfSapho(true);
            aggressorBattlePlan.revealOpponentBattlePlan(defenderBattlePlan);
        } else {
            throw new InvalidGameStateException(faction.getName() + " is not the defender in this battle.");
        }
        String turnSummaryString = faction.getEmoji() + " played Juice of Sapho to become the aggressor.\n";
        turnSummaryString += outcomeDifferences(game, wasAggressorLeaderAlive, wasDefenderLeaderAlive, false, oldResolutionString, combatWaterBefore);
        game.getTurnSummary().publish(turnSummaryString);
        juiceOfSaphoTBD = DecisionStatus.CLOSED;
    }

    public void juiceOfSaphoDontAdd() {
        juiceOfSaphoTBD = DecisionStatus.CLOSED;
    }

    public void portableSnooperAdd(Game game, Faction faction) throws InvalidGameStateException {
        boolean wasAggressorLeaderAlive = aggressorBattlePlan.isLeaderAlive();
        boolean wasDefenderLeaderAlive = defenderBattlePlan.isLeaderAlive();
        String oldResolutionString = getWinnerString(game);
        int combatWaterBefore = aggressorBattlePlan.combatWater() + defenderBattlePlan.combatWater();
        if (getAggressorName().equals(faction.getName())) {
            if (!aggressorBattlePlan.addPortableSnooper())
                throw new InvalidGameStateException(faction.getName() + " cannot add Portable Snooper");
            defenderBattlePlan.revealOpponentBattlePlan(aggressorBattlePlan);
        } else if (getDefenderName().equals(faction.getName())) {
            if (!defenderBattlePlan.addPortableSnooper())
                throw new InvalidGameStateException(faction.getName() + " cannot add Portable Snooper");
            aggressorBattlePlan.revealOpponentBattlePlan(defenderBattlePlan);
        } else {
            throw new InvalidGameStateException(faction.getName() + " is not in this battle.");
        }
        String turnSummaryString = faction.getEmoji() + " added Portable Snooper to their Battle Plan.\n";
        turnSummaryString += outcomeDifferences(game, wasAggressorLeaderAlive, wasDefenderLeaderAlive, false, oldResolutionString, combatWaterBefore);
        game.getTurnSummary().publish(turnSummaryString);
        portableSnooperTBD = DecisionStatus.CLOSED;
    }

    public void portableSnooperDontAdd() {
        portableSnooperTBD = DecisionStatus.CLOSED;
    }

    public void stoneBurnerNoKill(Game game, Faction faction) throws InvalidGameStateException {
        boolean wasAggressorLeaderAlive = aggressorBattlePlan.isLeaderAlive();
        boolean wasDefenderLeaderAlive = defenderBattlePlan.isLeaderAlive();
        String oldResolutionString = getWinnerString(game);
        int combatWaterBefore = aggressorBattlePlan.combatWater() + defenderBattlePlan.combatWater();
        String nextDecision = "";
        if (getAggressorName().equals(faction.getName())) {
            String aggressorWeapon = aggressorBattlePlan.getWeapon().name();
            Faction defender = getDefender(game);
            if (aggressorWeapon.equals("Stone Burner")) {
                stoneBurnerTBD = DecisionStatus.CLOSED;
                if (mirrorWeaponStoneBurnerTBD == DecisionStatus.OPEN)
                    nextDecision = stoneBurnerDecision(defender, false, defenderBattlePlan, aggressorBattlePlan, true);
                else
                    aggressorBattlePlan.dontKillWithStoneBurner();
            } else if (aggressorWeapon.equals("Mirror Weapon")) {
                mirrorWeaponStoneBurnerTBD = DecisionStatus.CLOSED;
                if (stoneBurnerTBD == DecisionStatus.OPEN)
                    nextDecision = stoneBurnerDecision(defender, false, defenderBattlePlan, aggressorBattlePlan, true);
                else
                    defenderBattlePlan.dontKillWithStoneBurner();
            } else {
                throw new InvalidGameStateException(faction.getName() + " did not use Stone Burner or Mirror Weapon against Stone Burner");
            }
            aggressorBattlePlan.revealOpponentBattlePlan(defenderBattlePlan);
            defenderBattlePlan.revealOpponentBattlePlan(aggressorBattlePlan);
        } else if (getDefenderName().equals(faction.getName())) {
            String defenderWeapon = defenderBattlePlan.getWeapon().name();
            if (defenderWeapon.equals("Stone Burner")) {
                stoneBurnerTBD = DecisionStatus.CLOSED;
                defenderBattlePlan.dontKillWithStoneBurner();
            } else if (defenderWeapon.equals("Mirror Weapon")) {
                mirrorWeaponStoneBurnerTBD = DecisionStatus.CLOSED;
                aggressorBattlePlan.dontKillWithStoneBurner();
            } else {
                throw new InvalidGameStateException(faction.getName() + " did not use Stone Burner or Mirror Weapon against Stone Burner");
            }
            aggressorBattlePlan.revealOpponentBattlePlan(defenderBattlePlan);
            defenderBattlePlan.revealOpponentBattlePlan(aggressorBattlePlan);
        } else {
            throw new InvalidGameStateException(faction.getName() + " is not in this battle.");
        }
        String turnSummaryString = faction.getEmoji() + " does not kill both leaders.\n";
        if (nextDecision.isEmpty())
            turnSummaryString += outcomeDifferences(game, wasAggressorLeaderAlive, wasDefenderLeaderAlive, true, oldResolutionString, combatWaterBefore);
        else
            turnSummaryString += "\n" + nextDecision;
        game.getTurnSummary().publish(turnSummaryString);
    }

    public void stoneBurnerKill(Game game, Faction faction) {
        game.getTurnSummary().publish(faction.getEmoji() + " kills both leaders.");
        stoneBurnerTBD = DecisionStatus.CLOSED;
        mirrorWeaponStoneBurnerTBD = DecisionStatus.CLOSED;
    }

    public void removePoisonTooth(Game game, Faction faction) throws InvalidGameStateException {
        boolean wasAggressorLeaderAlive = aggressorBattlePlan.isLeaderAlive();
        boolean wasDefenderLeaderAlive = defenderBattlePlan.isLeaderAlive();
        String oldResolutionString = getWinnerString(game);
        int combatWaterBefore = aggressorBattlePlan.combatWater() + defenderBattlePlan.combatWater();
        if (getAggressorName().equals(faction.getName())) {
            if (!aggressorBattlePlan.revokePoisonTooth())
                throw new InvalidGameStateException(faction.getName() + " did not use Poison Tooth");
            defenderBattlePlan.revealOpponentBattlePlan(aggressorBattlePlan);
        } else if (getDefenderName().equals(faction.getName())) {
            if (!defenderBattlePlan.revokePoisonTooth())
                throw new InvalidGameStateException(faction.getName() + " did not use Poison Tooth");
            aggressorBattlePlan.revealOpponentBattlePlan(defenderBattlePlan);
        } else {
            throw new InvalidGameStateException(faction.getName() + " is not in this battle.");
        }
        String turnSummaryString = faction.getEmoji() + " removed Poison Tooth from their Battle Plan.\n";
        turnSummaryString += outcomeDifferences(game, wasAggressorLeaderAlive, wasDefenderLeaderAlive, true, oldResolutionString, combatWaterBefore);
        game.getTurnSummary().publish(turnSummaryString);
        poisonToothTBD = DecisionStatus.CLOSED;
    }

    public void keepPoisonTooth(Game game, Faction faction) {
        game.getTurnSummary().publish(faction.getEmoji() + " kept Poison Tooth in their Battle Plan.");
        poisonToothTBD = DecisionStatus.CLOSED;
    }

    public String outcomeDifferences(Game game, boolean wasAggressorLeaderAlive, boolean wasDefenderLeaderAlive, boolean announceStillDead, String oldResolutionString, int combatWaterBefore) throws InvalidGameStateException {
        String changes = "";
        String newResolutionString = getWinnerString(game);
        if (!newResolutionString.equals(oldResolutionString))
            changes += "\n" + newResolutionString;
        if (aggressorBattlePlan.getLeader() != null || aggressorBattlePlan.getCheapHero() != null && aggressorBattlePlan.hasKwisatzHaderach()) {
            if (aggressorBattlePlan.isLeaderAlive() && !wasAggressorLeaderAlive)
                changes += "\n" + getAggressor(game).getEmoji() + " " + aggressorBattlePlan.getLeaderString(false) + " survives.";
            else if (announceStillDead)
                changes += "\n" + getAggressor(game).getEmoji() + " " + aggressorBattlePlan.getLeaderString(false) + " still dies.";
        }
        if (defenderBattlePlan.getLeader() != null || defenderBattlePlan.getCheapHero() != null && defenderBattlePlan.hasKwisatzHaderach()) {
            if (defenderBattlePlan.isLeaderAlive() && !wasDefenderLeaderAlive)
                changes += "\n" + getDefender(game).getEmoji() + " " + defenderBattlePlan.getLeaderString(false) + " survives.";
            else if (announceStillDead)
                changes += "\n" + getDefender(game).getEmoji() + " " + defenderBattlePlan.getLeaderString(false) + " still dies.";
        }
        int combatWaterNow = aggressorBattlePlan.combatWater() + defenderBattlePlan.combatWater();
        if (combatWaterNow != combatWaterBefore)
            changes += "\n" + getWinnerEmojis(game) + " gains " + combatWaterNow + " " + Emojis.SPICE + " combat water\n";
        checkIfResolvable(game);
        return changes;
    }

    public void checkIfResolvable(Game game) {
        boolean resolvable = true;
        List<String> openIssues = new ArrayList<>();
        if (juiceOfSaphoTBD == DecisionStatus.OPEN) {
            openIssues.add("Juice of Sapho");
            resolvable = false;
        }
        if (portableSnooperTBD == DecisionStatus.OPEN) {
            openIssues.add("Portable Snooper");
            resolvable = false;
        }
        if (stoneBurnerTBD == DecisionStatus.OPEN) {
            openIssues.add("Stone Burner");
            resolvable = false;
        }
        if (mirrorWeaponStoneBurnerTBD == DecisionStatus.OPEN) {
            openIssues.add("Mirror Weapon as Stone Burner");
            resolvable = false;
        }
        if (poisonToothTBD == DecisionStatus.OPEN) {
            openIssues.add("Poison Tooth");
            resolvable = false;
        }
        if (aggressorBattlePlan.isCanCallTraitor() || aggressorBattlePlan.isHarkCanCallTraitor()) {
            openIssues.add("Aggressor Traitor Call");
            resolvable = false;
        }
        if (defenderBattlePlan.isCanCallTraitor() || defenderBattlePlan.isHarkCanCallTraitor()) {
            openIssues.add("Defender Traitor Call");
            resolvable = false;
        }

        if (resolvable)
            game.getModInfo().publish("The battle can be resolved.");
        else if (overrideDecisions)
            game.getModInfo().publish("The battle can be resolved with your override.");
        else
            game.getModInfo().publish("The following must be decided before the battle can be resolved:\n  " + String.join(", ", openIssues));
    }

    public boolean isDiplomatMustBeResolved() {
        return diplomatMustBeResolved;
    }
}
