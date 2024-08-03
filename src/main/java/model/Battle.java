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
    private boolean fedaykinNegated;
    private boolean sardaukarNegated;
    private boolean cyborgsNegated;
    private boolean fremenMustPay;

    public enum DecisionStatus {
        NA,
        OPEN,
        CLOSED
    }
    private DecisionStatus hmsStrongholdCardTBD;
    private String hmsStrongholdCardFactionEmoji;
    private DecisionStatus spiceBankerTBD;
    private String spiceBankerFactionEmoji;
    private DecisionStatus juiceOfSaphoTBD;
    private DecisionStatus portableSnooperTBD;
    private DecisionStatus stoneBurnerTBD;
    public DecisionStatus mirrorWeaponStoneBurnerTBD;
    private DecisionStatus poisonToothTBD;

    public Battle(Game game, String wholeTerritoryName, List<Territory> territorySectors, List<Faction> battleFactionsInStormOrder, List<Force> forces, String ecazAllyName) {
        this.wholeTerritoryName = wholeTerritoryName;
        this.territorySectors = territorySectors;
        this.factionNames = new ArrayList<>();
        battleFactionsInStormOrder.forEach(f -> factionNames.add(f.getName()));
        if (factionNames.get(0).equals("Ecaz") && factionNames.get(1).equals(ecazAllyName) || factionNames.get(0).equals(ecazAllyName) && factionNames.get(1).equals("Ecaz"))
            factionNames.add(factionNames.remove(1));
        this.forces = forces;
        this.ecazAllyName = ecazAllyName;
        this.fedaykinNegated = false;
        this.sardaukarNegated = factionNames.stream().anyMatch(n -> n.equals("Emperor")) && factionNames.stream().anyMatch(n -> n.equals("Fremen"));
        try {
            EmperorFaction emperor = (EmperorFaction) game.getFaction("Emperor");
            if (emperor.isSecundusOccupied())
                this.sardaukarNegated = true;
        } catch (IllegalArgumentException ignored) {}
        this.cyborgsNegated = false;
        this.fremenMustPay = false;
        this.hmsStrongholdCardTBD = DecisionStatus.NA;
        this.spiceBankerTBD = DecisionStatus.NA;
        this.juiceOfSaphoTBD = DecisionStatus.NA;
        this.portableSnooperTBD = DecisionStatus.NA;
        this.stoneBurnerTBD = DecisionStatus.NA;
        this.mirrorWeaponStoneBurnerTBD = DecisionStatus.NA;
        this.poisonToothTBD = DecisionStatus.NA;
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

    public String getEcazAllyName() {
        return ecazAllyName;
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

        if (factionsLeft.size() <= 1) return true;
        List<String> namesList = factionsLeft.stream().toList();
        return factionsLeft.size() == 2 && hasEcazAndAlly()
                && (namesList.get(0).equals("Ecaz") && namesList.get(1).equals(ecazAllyName)
                || namesList.get(0).equals(ecazAllyName) && namesList.get(1).equals("Ecaz"));
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
        StringBuilder message = new StringBuilder();
        String vs = "";
        boolean ecazInBattle = factionNames.stream().anyMatch(f -> f.equals("Ecaz"));
        boolean ecazAllyInBattle = getFactions(game).stream().anyMatch(f -> f.getAlly().equals("Ecaz"));
        boolean ecazAllyComplete = false;
        for (Faction f : getFactions(game)) {
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
            aggressorBattlePlan = battlePlan;
        } else {
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

    public String updateTroopsDialed(String factionName, int regularDialed, int specialDialed) throws InvalidGameStateException {
        BattlePlan battlePlan;
        if (getAggressorName().equals(factionName))
            battlePlan = aggressorBattlePlan;
        else if (getDefenderName().equals(factionName))
            battlePlan = defenderBattlePlan;
        else
            throw new InvalidGameStateException(factionName + " is not in the current battle.");
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

    public String factionBattleResults(Game game, boolean isAggressor) throws InvalidGameStateException {
        String resolution = "";
        Faction faction = isAggressor ? getAggressor(game) : getDefender(game);
        String troopFactionName = hasEcazAndAlly() && faction.getName().equals("Ecaz") ? game.getFaction("Ecaz").getAlly() : faction.getName();
        String troopFactionEmoji = Emojis.getFactionEmoji(troopFactionName);
        BattlePlan aggressorPlan = getAggressorBattlePlan();
        BattlePlan defenderPlan = getDefenderBattlePlan();
        BattlePlan battlePlan = isAggressor ? aggressorPlan : defenderPlan;
        BattlePlan opponentBattlePlan = isAggressor ? defenderPlan : aggressorPlan;
        boolean isLasgunShieldExplosion = battlePlan.isLasgunShieldExplosion();
        String emojis = isAggressor ? getAggressor(game).getEmoji() : getDefender(game).getEmoji();
        boolean loser = isAggressor != isAggressorWin(game) || isLasgunShieldExplosion;
        String wholeTerritoryName = getWholeTerritoryName();

        if (battlePlan.getLeader() != null && !battlePlan.isLeaderAlive())
            resolution += emojis + " loses " + battlePlan.getKilledLeaderString() + " to the tanks\n";
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
        if (loser) {
            if (!(faction instanceof EcazFaction)) {
                if (battlePlan.isSkillBehindAndLeaderAlive("Diplomat")) {
                    int leaderValue = battlePlan.getLeaderValue();
                    savedSpecialForces = Math.min(specialForcesNotDialed, leaderValue);
                    savedRegularForces = Math.min(regularForcesNotDialed, leaderValue - savedSpecialForces);
                    specialForcesTotal -= savedSpecialForces;
                    specialForcesNotDialed -= savedSpecialForces;
                    regularForcesTotal -= savedRegularForces;
                    regularForcesNotDialed -= savedRegularForces;
                    resolution += troopFactionEmoji + " may have";
                    if (savedRegularForces > 0)
                        resolution += " " + savedRegularForces + " " + Emojis.getForceEmoji(troopFactionName);
                    if (savedSpecialForces > 0)
                        resolution += " " + savedSpecialForces + " " + Emojis.getForceEmoji(troopFactionName + "*");
                    resolution += " retreat to an empty adjacent non-stronghold with Diplomat\n";
                }
            }
            if ((weapon != null && weapon.name().equals("Harass and Withdraw") || defense != null && defense.name().equals("Harass and Withdraw"))
                    && !faction.getHomeworld().equals(wholeTerritoryName) && !(faction instanceof EmperorFaction emperor && emperor.getSecondHomeworld().equals(wholeTerritoryName))) {
                resolution += troopFactionEmoji + " returns";
                if (regularForcesNotDialed > 0)
                    resolution += " " + regularForcesNotDialed + " " + Emojis.getForceEmoji(troopFactionName);
                if (specialForcesNotDialed > 0)
                    resolution += " " + specialForcesNotDialed + " " + Emojis.getForceEmoji(troopFactionName + "*");
                regularForcesTotal -= regularForcesNotDialed;
                specialForcesTotal -= specialForcesNotDialed;
                resolution += " to reserves with Harass and Withdraw\n";
            }
            String troopLosses = troopFactionEmoji + " loses ";
            if (regularForcesTotal > 0)
                troopLosses += regularForcesTotal + " " + Emojis.getForceEmoji(troopFactionName) + " ";
            if (specialForcesTotal > 0)
                troopLosses += specialForcesTotal + " " + Emojis.getForceEmoji(troopFactionName + "*") + " ";
            troopLosses += "to the tanks\n";
            if (regularForcesTotal > 0 || specialForcesTotal > 0)
                resolution += troopLosses;
        } else if (regularForcesDialed > 0 || specialForcesDialed > 0) {
            if (!(faction instanceof EcazFaction)) {
                if (battlePlan.isSkillBehindAndLeaderAlive("Suk Graduate")) {
                    savedSpecialForces = Math.min(specialForcesDialed, 3);
                    savedRegularForces = Math.min(regularForcesDialed, 3 - savedSpecialForces);
                    specialForcesDialed -= savedSpecialForces;
                    regularForcesDialed -= savedRegularForces;
                    resolution += troopFactionEmoji + " saves";
                    if (savedRegularForces > 0)
                        resolution += " " + savedRegularForces + " " + Emojis.getForceEmoji(troopFactionName);
                    if (savedSpecialForces > 0)
                        resolution += " " + savedSpecialForces + " " + Emojis.getForceEmoji(troopFactionName + "*");
                    resolution += " and may leave 1 in the territory with Suk Graduate\n";
                } else if (battlePlan.isSkillInFront("Suk Graduate")) {
                    String savedTroopEmoji;
                    if (specialForcesDialed > 0) {
                        savedTroopEmoji = Emojis.getForceEmoji(troopFactionName + "*");
                        specialForcesDialed--;
                    } else {
                        savedTroopEmoji = Emojis.getForceEmoji(troopFactionName);
                        regularForcesDialed--;
                    }
                    if (!savedTroopEmoji.isEmpty())
                        resolution += troopFactionEmoji + " returns 1 " + savedTroopEmoji + " to reserves with Suk Graduate\n";
                }
            }
            if ((weapon != null && weapon.name().equals("Harass and Withdraw") || defense != null && defense.name().equals("Harass and Withdraw"))
                    && !faction.getHomeworld().equals(wholeTerritoryName) && !(faction instanceof EmperorFaction emperor && emperor.getSecondHomeworld().equals(wholeTerritoryName))) {
                resolution += troopFactionEmoji + " returns";
                if (regularForcesNotDialed > 0)
                    resolution += " " + regularForcesNotDialed + " " + Emojis.getForceEmoji(troopFactionName);
                if (specialForcesNotDialed > 0)
                    resolution += " " + specialForcesNotDialed + " " + Emojis.getForceEmoji(troopFactionName + "*");
                resolution += " to reserves with Harass and Withdraw\n";
            }
            if (regularForcesDialed > 0 || specialForcesDialed > 0) {
                resolution += troopFactionEmoji + " loses";
                if (regularForcesDialed > 0)
                    resolution += " " + regularForcesDialed + " " + Emojis.getForceEmoji(troopFactionName);
                if (specialForcesDialed > 0)
                    resolution += " " + specialForcesDialed + " " + Emojis.getForceEmoji(troopFactionName + "*");
                resolution += " to the tanks\n";
            }
        }
        if (hasEcazAndAlly() && (loser || !battlePlan.stoneBurnerForTroops()) && troopFactionName.equals(game.getFaction("Ecaz").getAlly())) {
            int ecazForces = Math.ceilDiv(battlePlan.getEcazTroopsForAlly(), 2);
            if (!loser && (faction instanceof EcazFaction)) {
                if (battlePlan.isSkillBehindAndLeaderAlive("Suk Graduate")) {
                    savedRegularForces = Math.min(ecazForces, 3);
                    ecazForces -= savedRegularForces;
                    resolution += Emojis.ECAZ + " saves " + savedRegularForces + " " + Emojis.ECAZ_TROOP;
                    resolution += " and may leave 1 in the territory with Suk Graduate\n";
                } else if (battlePlan.isSkillInFront("Suk Graduate")) {
                    if (ecazForces > 0) {
                        ecazForces--;
                        resolution += Emojis.ECAZ + " returns 1 " + Emojis.ECAZ_TROOP + " to reserves with Suk Graduate\n";
                    }
                }
            }
            resolution += Emojis.ECAZ + " loses ";
            resolution += loser ? battlePlan.getEcazTroopsForAlly() : ecazForces;
            resolution += " " + Emojis.ECAZ_TROOP;
            resolution += " to the tanks\n";
        }
        if (battlePlan.getNumForcesInReserve() >= 3 && (weapon != null && weapon.name().equals("Reinforcements") || defense != null && defense.name().equals("Reinforcements")))
            resolution += emojis + " must send 3 forces from reserves to the tanks for Reinforcements\n";
        if (battlePlan.getCheapHero() != null)
            resolution += emojis + " discards " + battlePlan.getCheapHero().name() + "\n";
        if (battlePlan.weaponMustBeDiscarded(loser))
            resolution += emojis + " discards " + battlePlan.getWeapon().name() + "\n";
        if (battlePlan.defenseMustBeDiscarded(loser))
            resolution += emojis + " discards " + battlePlan.getDefense().name() + "\n";
        if (battlePlan.isJuiceOfSapho() && faction.hasTreacheryCard("Juice of Sapho"))
            resolution += emojis + " discards Juice of Sapho\n";
        if (battlePlan.getSpice() > 0) {
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
        if (battlePlan.getSpiceBankerSupport() > 0)
            resolution += emojis + " loses " + battlePlan.getSpiceBankerSupport() + " " + Emojis.SPICE + " spent on Spice Banker";

        Territory spiceTerritory = getTerritorySectors().stream().filter(t -> t.getSpice() > 0).findFirst().orElse(null);
        if (!loser) {
            if (battlePlan.isSkillInFront("Rihani Decipherer"))
                resolution += troopFactionEmoji + " may peek at 2 random cards in the Traitor Deck with Rihani Decipherer\n";
            else if (battlePlan.isSkillBehindAndLeaderAlive("Rihani Decipherer"))
                resolution += troopFactionEmoji + " may draw 2 Traitor Cards and keep one of them with Rihani Decipherer\n";

            if (spiceTerritory != null && battlePlan.isSkillBehindAndLeaderAlive("Sandmaster"))
                resolution += "3 " + Emojis.SPICE + " will be added to " + spiceTerritory + " with Sandmaster\n";
        }
        if (spiceTerritory != null && battlePlan.isSkillBehindAndLeaderAlive("Smuggler"))
            resolution += troopFactionEmoji + " gains " + Math.min(spiceTerritory.getSpice(), battlePlan.getLeaderValue()) + " " + Emojis.SPICE + " for Smuggler";
        if (!isLasgunShieldExplosion) {
            if (loser) {
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
                if (game.hasGameOption(GameOption.STRONGHOLD_SKILLS)) {
                    String territoryNamne = getWholeTerritoryName();
                    String sietchTabr = "Sietch Tabr";
                    String tueksSietch = "Tuek's Sietch";
                    String hms = "Hidden Mobile Stronghold";
                    String worthlessCardType = "Worthless Card";
                    if (territoryNamne.equals(sietchTabr) && faction.hasStrongholdCard(sietchTabr)
                            || territoryNamne.equals(hms) && faction.hasHmsStrongholdProxy(sietchTabr)) {
                        if (opponentBattlePlan.getWholeNumberDial() > 0)
                            resolution += emojis + " gains " + opponentBattlePlan.getWholeNumberDial() + " " + Emojis.SPICE + " for Sietch Tabr stronghold card\n";
                    } else if (territoryNamne.equals(tueksSietch) && faction.hasStrongholdCard(tueksSietch)
                            || territoryNamne.equals(hms) && faction.hasHmsStrongholdProxy(tueksSietch)) {
                        int worthlessCardSpice = 0;
                        if (battlePlan.getWeapon() != null && battlePlan.getWeapon().type().equals(worthlessCardType)) worthlessCardSpice += 2;
                        if (battlePlan.getDefense() != null && battlePlan.getDefense().type().equals(worthlessCardType)) worthlessCardSpice += 2;
                        if (worthlessCardSpice > 0)
                            resolution += emojis + " gains " + worthlessCardSpice + " " + Emojis.SPICE + " for Tuek's Sietch stronghold card\n";
                    }
                }
            }
        }
        Faction winner = isAggressorWin(game) ? getAggressor(game) : getDefender(game);
        Faction loserFaction = isAggressorWin(game) ? getDefender(game) : getAggressor(game);
        if (winner instanceof HarkonnenFaction)
            resolution += Emojis.HARKONNEN + " captures a " + loserFaction.getEmoji() + " leader";
        boolean atreidesWin = winner instanceof AtreidesFaction || winner instanceof EcazFaction && hasEcazAndAlly() && winner.getAlly().equals("Atreides");
        if (!loser && atreidesWin) {
            Faction atreides = game.getFaction("Atreides");
            if (game.hasGameOption(GameOption.HOMEWORLDS) && atreides.isHighThreshold() && !wholeTerritoryName.equals("Caladan")
                    && regularForcesTotal - regularForcesDialed > 0 && (atreides.getReservesStrength() > 0 || regularForcesDialed > 0)) {
                resolution += Emojis.ATREIDES + " may add 1 " + Emojis.ATREIDES_TROOP + " from reserves to " + wholeTerritoryName + " with Caladan High Threshold\n";
            }
        }

        if (!resolution.isEmpty()) resolution = "\n" + resolution;
        return resolution;
    }

    private String getWinnerString(Game game) throws InvalidGameStateException {
        String resolution = "";
        if (aggressorBattlePlan.isLasgunShieldExplosion())
            resolution += "**KABOOM!**";
        else {
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

    public void printBattleResolution(Game game, boolean publishToTurnSummary) throws InvalidGameStateException {
        Faction aggressor = getAggressor(game);
        Faction defender = getDefender(game);
        String wholeTerritoryName = getWholeTerritoryName();
        String resolution = MessageFormat.format("{0} **vs {1} in {2}**\n\n",
                getAggressorEmojis(game), getDefenderEmojis(game), wholeTerritoryName
        );
        Integer noFieldValue = getForces().stream().filter(f -> f.getName().equals("NoField")).map(Force::getStrength).findFirst().orElse(null);
        if (noFieldValue != null)
            resolution += MessageFormat.format("{0} reveals {1} to be {2} {3}\n\n", Emojis.RICHESE, Emojis.NO_FIELD, noFieldValue, Emojis.RICHESE_TROOP);
        resolution += getAggressorEmojis(game) + "\n";
        resolution += aggressorBattlePlan.getPlanMessage(true) + "\n\n";
        resolution += getDefenderEmojis(game) + "\n";
        resolution += defenderBattlePlan.getPlanMessage(true) + "\n\n";
        resolution += getWinnerString(game) + "\n";
        resolution += factionBattleResults(game, true);
        resolution += factionBattleResults(game, false);
        resolution += aggressorBattlePlan.checkAuditor(defender.getEmoji());
        resolution += defenderBattlePlan.checkAuditor(aggressor.getEmoji());

        if (isSpiceBankerDecisionOpen() && !publishToTurnSummary)
            resolution += "\nBattle cannot be resolved yet.\n" + spiceBankerFactionEmoji + " must decide on Spice Banker\n";
        if (isHMSCardDecisionOpen() && !publishToTurnSummary)
            resolution += "\nBattle cannot be resolved yet.\n" + hmsStrongholdCardFactionEmoji + " must decide on HMS Stronghold Card\n";

        String resolutionDecisions = "";
        RicheseFaction richeseFaction;
        boolean saphoHasBeenAuctioned = false;
        boolean portableSnooperHasBeenAuctioned = false;
        if (game.hasFaction("Richese")) {
            richeseFaction = (RicheseFaction) game.getFaction("Richese");
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

        DuneTopic resultsChannel = publishToTurnSummary ? game.getTurnSummary() : game.getModInfo();
        resultsChannel.publish(resolution);
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
        return changes;
    }
}
