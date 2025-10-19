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
    private final List<String> sectorNames;
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
    private boolean emperorCunning;
    private boolean ixCunning;

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
    private String rihaniDeciphererFaction;
    private int rihaniDeciphererExectedTraitors;
    private String techTokenReceiver;
    private int expectedTechTokens;
    private String harkonnenCapturedLeader;
    private String harkonnenLeaderVictim;
    private boolean auditorMustBeResolved;
    private boolean assassinationMustBeResolved;
    private List<String> cardsForAudit;

    public Battle(Game game, List<Territory> territorySectors, List<Faction> battleFactionsInStormOrder) {
        this.wholeTerritoryName = territorySectors.getFirst().getAggregateTerritoryName();
        this.sectorNames = territorySectors.stream().map(Territory::getTerritoryName).toList();
        this.factionNames = new ArrayList<>();
        battleFactionsInStormOrder.forEach(f -> factionNames.add(f.getName()));
        this.ecazAllyName = battleFactionsInStormOrder.stream().filter(f -> f instanceof EcazFaction).map(Faction::getAlly).findFirst().orElse(null);
        if (factionNames.get(0).equals("Ecaz") && factionNames.get(1).equals(ecazAllyName) || factionNames.get(0).equals(ecazAllyName) && factionNames.get(1).equals("Ecaz"))
            factionNames.add(factionNames.remove(1));
        this.forces = aggregateForces(territorySectors, battleFactionsInStormOrder);
        this.resolutionPublished = false;
        this.fedaykinNegated = false;
        this.sardaukarNegated = battleFactionsInStormOrder.stream().anyMatch(f -> f instanceof FremenFaction);
        EmperorFaction emperor = game.getEmperorFactionOrNull();
        if (emperor != null && emperor.isSecundusOccupied())
            this.sardaukarNegated = true;
        this.cyborgsNegated = false;
        this.fremenMustPay = false;
        this.emperorCunning = false;
        this.ixCunning = false;
        try {
            this.ixCunning = game.getBattles().isIxCunning();
        } catch (InvalidGameStateException ignored) {
        }
        this.overrideDecisions = false;
        this.hmsStrongholdCardTBD = DecisionStatus.NA;
        this.spiceBankerTBD = DecisionStatus.NA;
        this.juiceOfSaphoTBD = DecisionStatus.NA;
        this.portableSnooperTBD = DecisionStatus.NA;
        this.stoneBurnerTBD = DecisionStatus.NA;
        this.mirrorWeaponStoneBurnerTBD = DecisionStatus.NA;
        this.poisonToothTBD = DecisionStatus.NA;
        this.diplomatMustBeResolved = false;
        this.auditorMustBeResolved = false;
        this.assassinationMustBeResolved = false;
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

    public List<Territory> getTerritorySectors(Game game) {
        return sectorNames.stream().map(game::getTerritory).toList();
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

    public void presentEcazAllyChoice(Game game) {
        Faction ecaz = game.getEcazFaction();
        Faction chooser = ecaz;
        String ecazLabel = "You";
        String allyLabel = "Your ally";
        String chatMessage = "Who will provide leader and " + Emojis.TREACHERY + " cards in your alliance's battle? " + ecaz.getPlayer();
        String tsMessage = Emojis.ECAZ + " must choose who will fight for their alliance.";
        if (!ecaz.isHighThreshold()) {
            Faction opponent = getAggressor(game);
            if (getAggressor(game) instanceof EcazFaction || ecaz.getAlly().equals(getAggressorName()))
                opponent = getDefender(game);
            chooser = opponent;
            ecazLabel = "Ecaz";
            allyLabel = "Their ally";
            chatMessage = Emojis.ECAZ + " is at Low Threshold.\nWho will provide leader and " + Emojis.TREACHERY + " cards against you? " + opponent.getPlayer();
            tsMessage = opponent.getEmoji() + " must choose who will fight for the " + Emojis.ECAZ + " alliance.";
        }
        List<DuneChoice> choices = List.of(
                new DuneChoice("primary", "battle-choose-combatant-Ecaz", ecazLabel, Emojis.ECAZ, false),
                new DuneChoice("primary", "battle-choose-combatant-" + ecaz.getAlly(), allyLabel, game.getFaction(ecaz.getAlly()).getEmoji(), false)
        );
        chooser.getChat().publish(chatMessage, choices);
        game.getTurnSummary().publish(tsMessage);
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
        for (Territory t : getTerritorySectors(game)) {
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
        Faction ecaz = game.getEcazFactionOrNull();
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
        String message = "Your free dial advantage has been negated by Karama.";
        if (battlePlanRemoved)
            message += "\nYou must submit a new battle plan.";
        Faction fremen = game.getFremenFaction();
        fremen.getChat().publish(message + " " + fremen.getPlayer());
    }

    private void removePreviouslySubmittedLeaderFromTerritory(Faction faction) {
        faction.getLeaders().stream().filter(l -> l.getBattleTerritoryName() != null && l.getBattleTerritoryName().equals(getWholeTerritoryName())).forEach(l -> l.setBattleTerritoryName(null));
    }

    public void removeBattlePlan(Faction faction) throws InvalidGameStateException {
        if (!getAggressorName().equals(faction.getName()) && !getDefenderName().equals(faction.getName()))
            throw new InvalidGameStateException("You are not in the current battle.");

        removePreviouslySubmittedLeaderFromTerritory(faction);

        boolean planIsForAggressor = false;
        if (getAggressorName().equals(faction.getName()))
            planIsForAggressor = true;
        else if (!getDefenderName().equals(faction.getName()))
            throw new InvalidGameStateException(faction.getName() + " is not in this battle.");

        if (planIsForAggressor)
            aggressorBattlePlan = null;
        else
            defenderBattlePlan = null;
    }

    public String setBattlePlan(Game game, Faction faction, String leaderName, boolean kwisatzHaderach, String dial, int spice, String weaponName, String defenseName) throws InvalidGameStateException {
        if (!getAggressorName().equals(faction.getName()) && !getDefenderName().equals(faction.getName()))
            throw new InvalidGameStateException("You are not in the current battle.");
        if (resolutionPublished)
            throw new InvalidGameStateException("Battle results have already been pulbished.");

        removePreviouslySubmittedLeaderFromTerritory(faction);
        Leader leader = null;
        TreacheryCard cheapHero = null;
        if (leaderName.startsWith("Cheap"))
            cheapHero = faction.getTreacheryHand().stream().filter(f -> f.name().equals(leaderName)).findFirst().orElseThrow();
        else if (!leaderName.equals("None")) {
            leader = faction.getLeaders().stream().filter(l -> l.getName().equals(leaderName)).findFirst().orElseThrow();
            Territory battleTerritory = getTerritorySectors(game).stream()
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
        if (resolutionPublished)
            throw new InvalidGameStateException("Battle results have already been pulbished.");
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
                List<DuneChoice> choices = strongholdNames.stream().map(strongholdName -> new DuneChoice("battle-hms-stronghold-power-" + strongholdName, strongholdName)).collect(Collectors.toList());
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
                DuneChoice choice = new DuneChoice("battle-spice-banker-" + i, Integer.toString(i));
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
        if (!battlePlan.isDialedForcesSettled())
            game.getGameActions().publish(Emojis.getFactionEmoji(factionName) + " battle plan submitted.");
        battlePlan.setForcesDialed(regularDialed, specialDialed);
        String emojiFactionName = factionName.equals("Ecaz") ? ecazAllyName : factionName;
        Faction emojiFaction = game.getFaction(emojiFactionName);
        String planUpdatedString = "Battle plan updated to dial " + emojiFaction.forcesStringWithZeroes(regularDialed, specialDialed);
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

    protected boolean aggressorCallsTraitor(Game game) {
        String opponentLeader = defenderBattlePlan.getLeaderNameForTraitor();
        if (defenderBattlePlan.hasKwisatzHaderach())
            return false;
        else if (aggressorBattlePlan.isWillCallTraitor() && getAggressor(game).hasTraitor(opponentLeader))
            return true;
        else
            return getAggressor(game).getAlly().equals("Harkonnen") && aggressorBattlePlan.isHarkWillCallTraitor() && game.getHarkonnenFaction().hasTraitor(opponentLeader);
    }

    protected boolean defenderCallsTraitor(Game game) {
        String opponentLeader = aggressorBattlePlan.getLeaderNameForTraitor();
        if (aggressorBattlePlan.hasKwisatzHaderach())
            return false;
        else if (defenderBattlePlan.isWillCallTraitor() && getDefender(game).hasTraitor(opponentLeader))
            return true;
        else
            return getDefender(game).getAlly().equals("Harkonnen") && defenderBattlePlan.isHarkWillCallTraitor() && game.getHarkonnenFaction().hasTraitor(opponentLeader);
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
        String troopFactionName = hasEcazAndAlly() && faction.getName().equals("Ecaz") ? ecazAllyName : faction.getName();
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
        boolean battleIsOnAHomeworld =  game.getHomeworlds().containsValue(wholeTerritoryName);

        if (battlePlan.getLeader() != null && !battlePlan.isLeaderAlive()) {
            resolution += emojis + " loses " + battlePlan.getKilledLeaderString() + " to the tanks\n";
            if (executeResolution)
                game.killLeader(faction, battlePlan.getKilledLeaderString());
        } else if (battlePlan.getLeader() != null && battlePlan.getLeader().getName().equals("Duke Vidal")) {
            if (!(faction instanceof BTFaction) || faction.getAlly().equals("Ecaz")) {
                resolution += emojis + " sets Duke Vidal aside\n";
                String btVidalMessage = "If Duke Vidal was a Ghola, he should be assigned back to " + Emojis.BT;
                if (faction instanceof BTFaction)
                    resolution += btVidalMessage + "\n";
                if (executeResolution) {
                    game.releaseDukeVidal(false);
                    if (faction instanceof BTFaction)
                        game.getModInfo().publish(btVidalMessage + " " + game.getModOrRoleMention());
                }
            }
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
                if (battlePlan.isSkillBehindAndLeaderAlive("Diplomat") && !battleIsOnAHomeworld) {
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
            if (!opponentCallsTraitor) {
                if (canHarassAndWithdraw(faction, weapon, defense)) {
                    if (faction instanceof EcazFaction && hasEcazAndAlly()) {
                        ecazForcesWithdrawn = Math.floorDiv(battlePlan.getEcazTroopsForAlly(), 2);
                        resolution += harassAndWithdraw(game, faction, ecazForcesWithdrawn, 0, executeResolution);
                    } else {
                        resolution += harassAndWithdraw(game, faction, regularForcesNotDialed, specialForcesNotDialed, executeResolution);
                        regularForcesTotal -= regularForcesNotDialed;
                        specialForcesTotal -= specialForcesNotDialed;
                    }
                }
                if (isLasgunShieldExplosion) {
                    int homeworldLasgunShieldLosses = 0;
                    if (faction.getHomeworld().equals(wholeTerritoryName))
                        homeworldLasgunShieldLosses = faction.homeworldDialAdvantage(game, game.getTerritory(wholeTerritoryName));
                    else if (faction instanceof EmperorFaction emperor && emperor.getSecondHomeworld().equals(wholeTerritoryName))
                        homeworldLasgunShieldLosses = faction.homeworldDialAdvantage(game, game.getTerritory(wholeTerritoryName));
                    if (homeworldLasgunShieldLosses != 0) {
                        if (regularForcesTotal - regularForcesDialed >= homeworldLasgunShieldLosses) {
                            regularForcesTotal = regularForcesDialed + homeworldLasgunShieldLosses;
                            specialForcesTotal = specialForcesDialed;
                        } else {
                            int regularsLostToLasgunShield = regularForcesTotal - regularForcesDialed;
                            if (specialForcesTotal - specialForcesDialed > homeworldLasgunShieldLosses - regularsLostToLasgunShield)
                                specialForcesTotal = specialForcesDialed + homeworldLasgunShieldLosses - regularsLostToLasgunShield;
                        }
                    }
                }
            }
            resolution += killForces(game, troopFaction, regularForcesTotal, specialForcesTotal, regularForcesTotal, executeResolution);
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
                            faction.withdrawForces(savedRegularForces, savedSpecialForces, getTerritorySectors(game), "Suk Graduate");
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
                            faction.withdrawForces(savedRegular, savedStarred, getTerritorySectors(game), "Suk Graduate");
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
            resolution += killForces(game, troopFaction, regularForcesDialed, specialForcesDialed, regularForcesTotal, executeResolution);
        }
        if (hasEcazAndAlly() && troopFactionName.equals(ecazAllyName)) {
            Faction ecaz = game.getEcazFaction();
            if (isLoser) {
                int ecazForcesnotDialed = Math.floorDiv(battlePlan.getEcazTroopsForAlly(), 2);
                int ecazForcesToKill = battlePlan.getEcazTroopsForAlly() - ecazForcesWithdrawn;
                if (faction instanceof EcazFaction && battlePlan.isSkillBehindAndLeaderAlive("Diplomat") && !battleIsOnAHomeworld) {
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
                resolution += killForces(game, ecaz, ecazForcesToKill, 0, 0, executeResolution);
            } else if (!callsTraitor) {
                int ecazForces = Math.ceilDiv(battlePlan.getEcazTroopsForAlly(), 2);
                if (battlePlan.isSkillBehindAndLeaderAlive("Suk Graduate")) {
                    savedRegularForces = Math.min(ecazForces, 3);
                    ecazForces -= savedRegularForces;
                    resolution += Emojis.ECAZ + " saves " + savedRegularForces + " " + Emojis.ECAZ_TROOP;
                    resolution += " and may leave 1 in the territory with Suk Graduate\n";
                    if (executeResolution) {
                        turnSummary.publish(faction.getEmoji() + " leaves 1 " + Emojis.ECAZ_TROOP + " in " + wholeTerritoryName + ", may return it to reserves.");
                        ecaz.withdrawForces(2, 0, getTerritorySectors(game), "Suk Graduate");
                    }
                } else if (battlePlan.isSkillInFront("Suk Graduate")) {
                    if (ecazForces > 0) {
                        ecazForces--;
                        resolution += Emojis.ECAZ + " returns 1 " + Emojis.ECAZ_TROOP + " to reserves with Suk Graduate\n";
                        if (executeResolution)
                            ecaz.withdrawForces(1, 0, getTerritorySectors(game), "Suk Graduate");
                    }
                }
                resolution += killForces(game, ecaz, ecazForces, 0, 0, executeResolution);
            }
        }
        boolean successfulTraitor = callsTraitor && !bothCallTraitor(game);
        resolution += handleReinforcements(game, faction, successfulTraitor, battlePlan, executeResolution);

        List<String> discards = new ArrayList<>();
        resolution += handleCheapHeroDiscard(faction, successfulTraitor, battlePlan, executeResolution, discards);
        resolution += handleWeaponDiscard(faction, successfulTraitor, isLoser, battlePlan, executeResolution, discards);
        resolution += handleDefenseDiscard(faction, successfulTraitor, isLoser, battlePlan, executeResolution, discards);
        if (!discards.isEmpty() && isLoser && !isLasgunShieldExplosion && !bothCallTraitor(game)) {
            if (executeResolution) {
                List<DuneChoice> choices = new ArrayList<>(discards.stream().map(d -> new DuneChoice("battle-retain-discard-" + d, d)).toList());
                choices.add(new DuneChoice("secondary", "battle-retain-discard-None", "No, leave in discard"));
                faction.getChat().publish("Would you like to retain a discarded " + Emojis.TREACHERY + " as " + Emojis.MORITANI + " ally? " + faction.getPlayer(), choices);
            } else
                resolution += faction.getEmoji() + " may retain a discarded " + Emojis.TREACHERY + " as " + Emojis.MORITANI + " ally.\n";
        }
        resolution += handleJuiceofSaphoDiscard(faction, successfulTraitor, battlePlan, executeResolution);

        resolution += handleSpicePayments(game, faction, successfulTraitor, battlePlan, executeResolution);

        resolution += handleRihaniDecipherer(game, faction, isLoser, battlePlan, executeResolution);

        Territory spiceTerritory = getTerritorySectors(game).stream().filter(t -> t.getSpice() > 0).findFirst().orElse(null);
        resolution += handleSandmaster(game, spiceTerritory, isLoser, battlePlan, executeResolution);
        resolution += handleSmuggler(game, faction, spiceTerritory, battlePlan, executeResolution);

        if (!isLasgunShieldExplosion && !bothCallTraitor(game)) {
            resolution += handleTechTokens(game, faction, isLoser, opponentFaction, executeResolution);
            resolution += handleCombatWater(game, faction, isLoser, executeResolution);
            resolution += handleJacurutuSietchSpice(game, faction, isLoser, opponentFaction, opponentBattlePlan, executeResolution);
            resolution += handleSietchTabrStrongholdCard(game, faction, isLoser, opponentBattlePlan, executeResolution);
        }
        resolution += handleTueksSietchStrongholdCard(game, faction, battlePlan, executeResolution);
        if (faction instanceof HarkonnenFaction harkonnen && battlePlan.isLeaderAlive())
            resolution += handleCapturedLeaderReturn(harkonnen, battlePlan, executeResolution);

        Faction winner = isAggressorWin(game) ? getAggressor(game) : getDefender(game);
        Faction loser = isAggressorWin(game) ? getDefender(game) : getAggressor(game);
        resolution += handleHarkonnenLeaderCapture(game, winner, loser, isLoser, executeResolution);
        if (!isLoser && winner instanceof AtreidesFaction atreides) {
            if (game.hasGameOption(GameOption.HOMEWORLDS) && atreides.isHighThreshold() && !wholeTerritoryName.equals("Caladan")
                    && regularForcesTotal - regularForcesDialed > 0 && atreides.getReservesStrength() > 0) {
                if (executeResolution) {
                    Territory territoryWithAtreidesForce = getTerritorySectors(game).stream().filter(s -> s.getForceStrength("Atreides") > 0).findAny().orElseThrow();
                    List<DuneChoice> choices = new ArrayList<>();
                    choices.add(new DuneChoice("atreides-ht-placement-yes-" + game.getTurn() + "-" + territoryWithAtreidesForce.getTerritoryName(), "Yes"));
                    choices.add(new DuneChoice("atreides-ht-placement-no-" + game.getTurn() + "-" + territoryWithAtreidesForce.getTerritoryName(), "No"));
                    atreides.getChat().publish("Would you like to place 1 " + Emojis.ATREIDES_TROOP + " from reserves in " + territoryWithAtreidesForce.getTerritoryName() + " with Caladan High Threshold? " + atreides.getPlayer(), choices);
                } else {
                    resolution += Emojis.ATREIDES + " may add 1 " + Emojis.ATREIDES_TROOP + " from reserves to " + wholeTerritoryName + " with Caladan High Threshold\n";
                }
            }
        }

        if (!resolution.isEmpty()) resolution = "\n" + resolution;
        return resolution;
    }

    private String killForces(Game game, Faction faction, int regularLeftToKill, int starredLeftToKill, int regularTotalForIx, boolean executeResolution) {
        String resolution = "";
        if (regularLeftToKill > 0 || starredLeftToKill > 0) {
            int cyborgReplacements = Math.min(starredLeftToKill, regularTotalForIx - regularLeftToKill);
            if (executeResolution) {
                if (faction instanceof IxFaction) {
                    regularLeftToKill += cyborgReplacements;
                    starredLeftToKill -= cyborgReplacements;
                }
                for (Territory t : getTerritorySectors(game)) {
                    if (regularLeftToKill == 0 && starredLeftToKill == 0)
                        break;
                    int regularPresent = t.getForceStrength(faction.getName());
                    int starredPresent = t.getForceStrength(faction.getName() + "*");
                    int regularToKillNow = Math.min(regularLeftToKill, regularPresent);
                    int starredToKillNow = Math.min(starredLeftToKill, starredPresent);
                    regularLeftToKill -= regularToKillNow;
                    starredLeftToKill -= starredToKillNow;
                    if (regularToKillNow > 0 || starredToKillNow > 0)
                        game.removeForcesAndReportToTurnSummary(t.getTerritoryName(), faction, regularToKillNow, starredToKillNow, true, true);
                }
            } else {
                resolution += faction.getEmoji() + " loses " + faction.forcesString(regularLeftToKill, starredLeftToKill) + " to the tanks\n";
                if (faction instanceof IxFaction && starredLeftToKill > 0 && regularTotalForIx - regularLeftToKill > 0)
                    resolution += faction.getEmoji() + " may send " + cyborgReplacements + " " + Emojis.IX_SUBOID + " to the tanks instead of " + cyborgReplacements + " " + Emojis.IX_CYBORG + "\n";
            }
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
            faction.withdrawForces(regularForcesNotDialed, specialForcesNotDialed, getTerritorySectors(game), "Harass and Withdraw");
        else
            resolution += faction.getEmoji() + " returns " + faction.forcesString(regularForcesNotDialed, specialForcesNotDialed) +  " to reserves with Harass and Withdraw\n";
        return resolution;
    }

    private String handleReinforcements(Game game, Faction faction, boolean successfulTraitor, BattlePlan battlePlan, boolean executeResolution) {
        String resolution = "";
        TreacheryCard weapon = battlePlan.getWeapon();
        TreacheryCard defense = battlePlan.getDefense();
        if (!successfulTraitor && (weapon != null && weapon.name().equals("Reinforcements") || defense != null && defense.name().equals("Reinforcements"))) {
            if (executeResolution) {
                if (battlePlan.getNumForcesInReserve() < 3)
                    game.getModInfo().publish("Reinforcements requires 3 forces in reserves. Sending all reserves to the tanks. " + game.getModOrRoleMention());
                int reservesLeftToKill = 3;
                Territory homeworld = game.getTerritory(faction.getHomeworld());
                int reservesToKillNow = Math.min(reservesLeftToKill, homeworld.getForceStrength(faction.getName()));
                if (reservesToKillNow > 0)
                    game.removeForcesAndReportToTurnSummary(homeworld.getTerritoryName(), faction, reservesToKillNow, 0, true, true);
                reservesLeftToKill -= reservesToKillNow;
                if (faction instanceof EmperorFaction emperor) {
                    Territory secondHomeworld = game.getTerritory(emperor.getSecondHomeworld());
                    reservesToKillNow = Math.min(reservesLeftToKill, secondHomeworld.getForceStrength(faction.getName()));
                    if (reservesToKillNow > 0)
                        game.removeForcesAndReportToTurnSummary(secondHomeworld.getTerritoryName(), faction, reservesToKillNow, 0, true, true);
                    reservesLeftToKill -= reservesToKillNow;
                }
                reservesToKillNow = Math.min(reservesLeftToKill, homeworld.getForceStrength(faction.getName() + "*"));
                if (reservesToKillNow > 0)
                    game.removeForcesAndReportToTurnSummary(homeworld.getTerritoryName(), faction, 0, reservesToKillNow, true, true);
                reservesLeftToKill -= reservesToKillNow;
                if (faction instanceof EmperorFaction emperor) {
                    Territory secondHomeworld = game.getTerritory(emperor.getSecondHomeworld());
                    reservesToKillNow = Math.min(reservesLeftToKill, secondHomeworld.getForceStrength(faction.getName() + "*"));
                    if (reservesToKillNow > 0)
                        game.removeForcesAndReportToTurnSummary(secondHomeworld.getTerritoryName(), faction, 0, reservesToKillNow, true, true);
                }
            } else {
                resolution += faction.getEmoji() + " must send 3 forces from reserves to the tanks for Reinforcements\n";
            }
        }
        return resolution;
    }

    private String handleCheapHeroDiscard(Faction faction, boolean successfulTraitor, BattlePlan battlePlan, boolean executeResolution, List<String> discards) {
        String resolution = "";
        if (battlePlan.getCheapHero() != null) {
            if (successfulTraitor) {
                if (!executeResolution)
                    resolution += faction.getEmoji() + " may discard " + battlePlan.getCheapHero().name() + "\n";
            } else {
                if (executeResolution)
                    faction.discard(battlePlan.getCheapHero().name());
                else
                    resolution += faction.getEmoji() + " discards " + battlePlan.getCheapHero().name() + "\n";
                if (faction.getAlly().equals("Moritani"))
                    discards.add(battlePlan.getCheapHero().name());
            }
        }
        return resolution;
    }

    private String handleWeaponDiscard(Faction faction, boolean successfulTraitor, boolean isLoser, BattlePlan battlePlan, boolean executeResolution, List<String> discards) {
        String resolution = "";
        if (battlePlan.weaponMustBeDiscarded(isLoser)) {
            if (successfulTraitor) {
                if (!executeResolution)
                    resolution += faction.getEmoji() + " may discard " + battlePlan.getWeapon().name() + "\n";
            } else {
                if (executeResolution)
                    faction.discard(battlePlan.getWeapon().name());
                else
                    resolution += faction.getEmoji() + " discards " + battlePlan.getWeapon().name() + "\n";
                if (faction.getAlly().equals("Moritani"))
                    discards.add(battlePlan.getWeapon().name());
            }
        }
        return resolution;
    }

    private String handleDefenseDiscard(Faction faction, boolean successfulTraitor, boolean isLoser, BattlePlan battlePlan, boolean executeResolution, List<String> discards) {
        String resolution = "";
        if (battlePlan.defenseMustBeDiscarded(isLoser)) {
            if (successfulTraitor) {
                if (!executeResolution)
                    resolution += faction.getEmoji() + " may discard " + battlePlan.getDefense().name() + "\n";
            } else {
                if (executeResolution)
                    faction.discard(battlePlan.getDefense().name());
                else
                    resolution += faction.getEmoji() + " discards " + battlePlan.getDefense().name() + "\n";
                if (faction.getAlly().equals("Moritani"))
                    discards.add(battlePlan.getDefense().name());
            }
        }
        return resolution;
    }

    private String handleJuiceofSaphoDiscard(Faction faction, boolean successfulTraitor, BattlePlan battlePlan, boolean executeResolution) {
        String resolution = "";
        if (!successfulTraitor && battlePlan.isJuiceOfSapho()) {
            if (executeResolution && faction.hasTreacheryCard("Juice of Sapho"))
                faction.discard("Juice of Sapho");
            else
                resolution += faction.getEmoji() + " discards Juice of Sapho\n";
        }
        return resolution;
    }

    private String handleSpicePayments(Game game, Faction faction, boolean successfulTraitor, BattlePlan battlePlan, boolean executeResolution) {
        String resolution = "";
        DuneTopic turnSummary = game.getTurnSummary();
        if (!successfulTraitor && battlePlan.getSpice() > 0) {
            int spiceFromAlly = 0;
            if (faction.hasAlly())
                spiceFromAlly = Math.min(game.getFaction(faction.getAlly()).getBattleSupport(), battlePlan.getSpice());
            int spiceFromArrakeenStrongholdCard = battlePlan.getArrakeenStrongholdCardSpice();

            if (executeResolution) {
                faction.subtractSpice(battlePlan.getSpice() - spiceFromAlly, "combat spice");
                turnSummary.publish(faction.getEmoji() + " loses " + (battlePlan.getSpice() - spiceFromAlly) + " " + Emojis.SPICE + " combat spice.");
            } else {
                resolution += faction.getEmoji() + " loses " + (battlePlan.getSpice() - spiceFromAlly) + " " + Emojis.SPICE + " combat spice\n";
                if (spiceFromArrakeenStrongholdCard > 0)
                    resolution += spiceFromArrakeenStrongholdCard + " " + Emojis.SPICE + " provided by Spice Bank for Arrakeen Stronghold Card.\n";
            }

            if (spiceFromAlly > 0) {
                Faction allyFaction = game.getFaction(faction.getAlly());
                if (executeResolution) {
                    allyFaction.subtractSpice(spiceFromAlly, "ally support");
                    turnSummary.publish(allyFaction.getEmoji() + " loses " + spiceFromAlly + " " + Emojis.SPICE + " ally support.");
                } else
                    resolution += allyFaction.getEmoji() + " loses " + spiceFromAlly + " " + Emojis.SPICE + " ally support\n";
            }

            Faction choam = game.getCHOAMFactionOrNull();
            if (!(faction instanceof ChoamFaction) && choam != null) {
                int choamEligibleSpice = battlePlan.getSpice();
                if (faction.getAlly().equals("CHOAM"))
                    choamEligibleSpice -= spiceFromAlly;
                if (spiceFromArrakeenStrongholdCard > 0)
                    choamEligibleSpice += spiceFromArrakeenStrongholdCard;
                if (choamEligibleSpice > 1) {
                    if (executeResolution) {
                        choam.addSpice(Math.floorDiv(choamEligibleSpice, 2), "combat spice");
                        turnSummary.publish(choam.getEmoji() + " gains " + Math.floorDiv(choamEligibleSpice, 2) + " " + Emojis.SPICE + " combat spice.");
                    } else
                        resolution += choam.getEmoji() + " gains " + Math.floorDiv(choamEligibleSpice, 2) + " " + Emojis.SPICE + " combat spice\n";
                }
            }
        }
        if (!successfulTraitor && battlePlan.getSpiceBankerSupport() > 0) {
            if (executeResolution) {
                faction.subtractSpice(battlePlan.getSpiceBankerSupport(), "combat spice");
                turnSummary.publish(faction.getEmoji() + " loses " + battlePlan.getSpiceBankerSupport() + " " + Emojis.SPICE + " spent on Spice Banker\n");
            } else
                resolution += faction.getEmoji() + " loses " + battlePlan.getSpiceBankerSupport() + " " + Emojis.SPICE + " spent on Spice Banker\n";
        }
        return resolution;
    }

    private String handleRihaniDecipherer(Game game, Faction faction, boolean isLoser, BattlePlan battlePlan, boolean executeResolution) {
        String resolution = "";
        if (!isLoser) {
            LinkedList<TraitorCard> traitorDeck = game.getTraitorDeck();
            if (battlePlan.isSkillInFront("Rihani Decipherer")) {
                if (executeResolution) {
                    Collections.shuffle(traitorDeck);
                    TraitorCard traitor1 = traitorDeck.getFirst();
                    TraitorCard traitor2 = traitorDeck.get(1);
                    faction.getChat().publish(traitor1.getEmojiNameAndStrengthString() + " and " + traitor2.getEmojiNameAndStrengthString() + " are in the Traitor deck.");
                    game.getTurnSummary().publish(faction.getEmoji() + " has been shown 2 Traitor cards for Rihani Decipherer.");
                } else
                    resolution += faction.getEmoji() + " may peek at 2 random cards in the Traitor Deck with Rihani Decipherer\n";
            } else if (battlePlan.isSkillBehindAndLeaderAlive("Rihani Decipherer")) {
                if (executeResolution) {
                    rihaniDeciphererFaction = faction.getName();
                    rihaniDeciphererExectedTraitors = faction.getTraitorHand().size();
                    faction.drawTwoTraitorsWithRihaniDecipherer("Rihani Decipherer");
                } else
                    resolution += faction.getEmoji() + " may draw 2 Traitor Cards and keep one of them with Rihani Decipherer\n";
            }
        }
        return resolution;
    }

    private String handleSandmaster(Game game, Territory spiceTerritory, boolean isLoser, BattlePlan battlePlan, boolean executeResolution) {
        String resolution = "";
        if (!isLoser) {
            if (spiceTerritory != null && battlePlan.isSkillBehindAndLeaderAlive("Sandmaster")) {
                if (executeResolution) {
                    spiceTerritory.addSpice(game, 3);
                    game.getTurnSummary().publish("3 " + Emojis.SPICE + " were added to " + spiceTerritory.getTerritoryName() + " with Sandmaster.");
                } else
                    resolution += "3 " + Emojis.SPICE + " will be added to " + spiceTerritory.getTerritoryName() + " with Sandmaster\n";
            }
        }
        return resolution;
    }

    private String handleSmuggler(Game game, Faction faction, Territory spiceTerritory, BattlePlan battlePlan, boolean executeResolution) {
        String resolution = "";
        if (spiceTerritory != null && battlePlan.isSkillBehindAndLeaderAlive("Smuggler")) {
            int spiceToTake = Math.min(spiceTerritory.getSpice(), battlePlan.getLeaderValue());
            String territoryName = spiceTerritory.getTerritoryName();
            if (executeResolution) {
                spiceTerritory.setSpice(spiceTerritory.getSpice() - spiceToTake);
                faction.addSpice(spiceToTake, "Smuggler in " + territoryName);
                game.getTurnSummary().publish(faction.getEmoji() + " took " + spiceToTake + " " + Emojis.SPICE + " from " + territoryName + " with Smuggler.");
            } else
                resolution += faction.getEmoji() + " will take " + spiceToTake + " " + Emojis.SPICE + " from " + territoryName + " with Smuggler";
        }
        return resolution;
    }

    private String handleTechTokens(Game game, Faction faction, boolean isLoser, Faction opponentFaction, boolean executeResolution) throws InvalidGameStateException {
        String resolution = "";
        if (isLoser) {
            List<TechToken> techTokens = faction.getTechTokens();
            if (techTokens.size() == 1) {
                String ttName = techTokens.getFirst().getName();
                String ttEmoji = Emojis.getTechTokenEmoji(ttName);
                if (executeResolution) {
                    faction.removeTechToken(ttName);
                    opponentFaction.addTechToken(ttName);
                    game.getTurnSummary().publish(opponentFaction.getEmoji() + " takes " + ttEmoji + " from " + faction.getEmoji());
                } else
                    resolution += faction.getEmoji() + " loses " + ttEmoji + " to " + opponentFaction.getEmoji() + "\n";
            } else if (techTokens.size() > 1) {
                if (executeResolution) {
                    techTokenReceiver = opponentFaction.getName();
                    expectedTechTokens = opponentFaction.getTechTokens().size() + 1;
                    List<DuneChoice> choices = new ArrayList<>();
                    techTokens.forEach(tt -> choices.add(new DuneChoice("battle-take-tech-token-" + tt.getName(), tt.getName())));
                    opponentFaction.getChat().publish("Which Tech Token would you like to take? " + opponentFaction.getPlayer(), choices);
                    game.getTurnSummary().publish(opponentFaction.getEmoji() + " must choose which Tech Token to take from " + faction.getEmoji());
                } else {
                    String ttString = String.join(" or ", techTokens.stream().map(TechToken::getName).map(Emojis::getTechTokenEmoji).toList());
                    resolution += faction.getEmoji() + " loses " + ttString + " to " + opponentFaction.getEmoji() + "\n";
                }
            }
        }
        return resolution;
    }

    private String handleCombatWater(Game game, Faction faction, boolean isLoser, boolean executeResolution) {
        String resolution = "";
        if (!isLoser) {
            int combatWater = getAggressorBattlePlan().combatWater() + getDefenderBattlePlan().combatWater();
            if (combatWater > 0) {
                if (executeResolution) {
                    game.getTurnSummary().publish(faction.getEmoji() + " gains " + combatWater + " " + Emojis.SPICE + " combat water.");
                    faction.addSpice(combatWater, "combat water");
                } else
                    resolution += faction.getEmoji() + " gains " + combatWater + " " + Emojis.SPICE + " combat water\n";
            }
        }
        return resolution;
    }

    private String handleJacurutuSietchSpice(Game game, Faction faction, boolean isLoser, Faction opponentFaction, BattlePlan opponentBattlePlan, boolean executeResolution) {
        String resolution = "";
        if (!isLoser && getWholeTerritoryName().equals("Jacurutu Sietch")) {
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
            if (!forcesString.isEmpty()) {
                int spiceGained = opponentRegularNotDialed + opponentSpecialNotDialed + ecazForcesNotDialed;
                if (executeResolution) {
                    faction.addSpice(spiceGained, "Jacurutu Sietch");
                    game.getTurnSummary().publish(faction.getEmoji() + " gains " + spiceGained + " " + Emojis.SPICE + " for" + forcesString + " not dialed.");
                } else
                    resolution += faction.getEmoji() + " gains " + spiceGained + " " + Emojis.SPICE + " for" + forcesString + " not dialed.\n";
            }
        }
        return resolution;
    }

    private String handleSietchTabrStrongholdCard(Game game, Faction faction, boolean isLoser, BattlePlan opponentBattlePlan, boolean executeResolution) {
        String resolution = "";
        int dialSpice = opponentBattlePlan.getWholeNumberDial();
        if (!isLoser && strongholdCardApplies(game, "Sietch Tabr", faction) && dialSpice > 0) {
            if (executeResolution) {
                game.getTurnSummary().publish(faction.getEmoji() + " gains " + dialSpice + " " + Emojis.SPICE + " for Sietch Tabr stronghold card.");
                faction.addSpice(dialSpice, "Sietch Tabr Stronghold Card");
            } else
                resolution += faction.getEmoji() + " gains " + dialSpice + " " + Emojis.SPICE + " for Sietch Tabr stronghold card\n";
        }
        return resolution;
    }

    private String handleTueksSietchStrongholdCard(Game game, Faction faction, BattlePlan battlePlan, boolean executeResolution) {
        String resolution = "";
        if (strongholdCardApplies(game, "Tuek's Sietch", faction)) {
            int worthlessCardSpice = 0;
            if (battlePlan.getWeapon() != null && battlePlan.getWeapon().type().equals("Worthless Card")) worthlessCardSpice += 2;
            if (battlePlan.getDefense() != null && battlePlan.getDefense().type().equals("Worthless Card")) worthlessCardSpice += 2;
            if (worthlessCardSpice > 0) {
                if (executeResolution) {
                    game.getTurnSummary().publish(faction.getEmoji() + " gains " + worthlessCardSpice + " " + Emojis.SPICE + " for Tuek's Sietch stronghold card.");
                    faction.addSpice(worthlessCardSpice, "Tuek's Sietch Stronghold Card");
                } else
                    resolution += faction.getEmoji() + " gains " + worthlessCardSpice + " " + Emojis.SPICE + " for Tuek's Sietch stronghold card\n";
            }
        }
        return resolution;
    }

    private String handleCapturedLeaderReturn(HarkonnenFaction harkonnen, BattlePlan battlePlan, boolean executeResolution) {
        String resolution = "";
        String leaderFaction = "Harkonnen";
        Leader leader = battlePlan.getLeader();
        if (leader != null) {
            leader = harkonnen.getLeader(leader.getName()).orElseThrow();
            leaderFaction = leader.getOriginalFactionName();
        }
        if (!leaderFaction.equals("Harkonnen")) {
            if (executeResolution)
                harkonnen.returnCapturedLeader(leader.getName());
            else
                resolution += Emojis.HARKONNEN + " returns " + leader.getName() + " to " + Emojis.getFactionEmoji(leaderFaction) + "\n";
        }
        return resolution;
    }

    protected String handleHarkonnenLeaderCapture(Game game, Faction winner, Faction loser, boolean isLoser, boolean executeResolution) throws InvalidGameStateException {
        String resolution = "";
        BattlePlan loserBattlePlan = isAggressorWin(game) ? defenderBattlePlan : aggressorBattlePlan;
        if (!isLoser && winner instanceof HarkonnenFaction harkonnen) {
            if (executeResolution) {
                String loserKilledLeader = loserBattlePlan.getLeader() == null || loserBattlePlan.isLeaderAlive() ? null : loserBattlePlan.getLeader().getName();
                List<Leader> eligibleLeaders = new ArrayList<>(loser.getLeaders().stream()
                        .filter(l -> !l.getName().equals(loserKilledLeader))
                        .filter(l -> l.getBattleTerritoryName() == null || sectorNames.contains(l.getBattleTerritoryName()))
                        .filter(l -> !(l.getName().equals("Kwisatz Haderach") || l.getName().equals("Auditor"))).toList());
                if (eligibleLeaders.isEmpty()) {
                    game.getTurnSummary().publish(loser.getEmoji() + " has no eligible leaders to capture.");
                } else {
                    Collections.shuffle(eligibleLeaders);
                    Leader leader = eligibleLeaders.getFirst();
                    harkonnenCapturedLeader = leader.getName();
                    harkonnenLeaderVictim = loser.getName();
                    List<DuneChoice> choices = new ArrayList<>();
                    choices.add(new DuneChoice("battle-harkonnen-keep-captured-leader", "Keep"));
                    choices.add(new DuneChoice("battle-harkonnen-kill-captured-leader", "Kill for 2 spice"));
                    choices.add(new DuneChoice("secondary", "battle-harkonnen-return-captured-leader", "No capture"));
                    harkonnen.getChat().publish("Will you keep or kill " + leader.getName() + "? " + harkonnen.getPlayer(), choices);
                }
            } else
                resolution += Emojis.HARKONNEN + " captures a " + loser.getEmoji() + " leader\n";
        }
        return resolution;
    }

    public boolean strongholdCardApplies(Game game, String stronghold, Faction faction) {
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
        else if (faction.getAlly().equals("Harkonnen") && game.getHarkonnenFaction().hasTraitor(opponentLeader))
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
        if (executeResolution)
            populateCardsForAudit(game);
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

        RicheseFaction richeseFaction = game.getRicheseFactionOrNull();
        Integer noFieldValue = getForces().stream().filter(f -> f.getName().equals("NoField")).map(Force::getStrength).findFirst().orElse(null);
        if (noFieldValue != null) {
            resolution += MessageFormat.format("{0} reveals {1} to be {2} {3}\n\n", Emojis.RICHESE, Emojis.NO_FIELD, noFieldValue, Emojis.RICHESE_TROOP);
            if (executeResolution && richeseFaction != null)
                richeseFaction.revealNoField();
        }

        cardsForAudit = new ArrayList<>();
        if (executeResolution)
            populateCardsForAudit(game);
        resolution += getAggressorEmojis(game) + "\n";
        resolution += aggressorBattlePlan.getPlanMessage(true) + "\n\n";
        resolution += getDefenderEmojis(game) + "\n";
        resolution += defenderBattlePlan.getPlanMessage(true) + "\n\n";
        resolution += getWinnerString(game) + "\n";

        resolution += factionBattleResults(game, true, executeResolution);
        resolution += factionBattleResults(game, false, executeResolution);
        resolution += lasgunShieldCarnage(game, executeResolution);
        resolution += checkAuditor(game, aggressorBattlePlan, defender, executeResolution);
        resolution += checkAuditor(game, defenderBattlePlan, aggressor, executeResolution);

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
            resolutionDecisions += portableSnooperDecision(aggressor, aggressorBattlePlan, defenderBattlePlan, publishToTurnSummary);
        resolutionDecisions += stoneBurnerDecision(aggressor, true, aggressorBattlePlan, defenderBattlePlan, publishToTurnSummary);
        resolutionDecisions += poisonToothDecision(aggressor, aggressorBattlePlan, publishToTurnSummary);
        if (saphoHasBeenAuctioned)
            resolutionDecisions += juiceOfSaphoDecision(defender, defenderBattlePlan, aggressorBattlePlan, publishToTurnSummary);
        if (portableSnooperHasBeenAuctioned)
            resolutionDecisions += portableSnooperDecision(defender, defenderBattlePlan, aggressorBattlePlan, publishToTurnSummary);
        resolutionDecisions += stoneBurnerDecision(defender, false, defenderBattlePlan, aggressorBattlePlan, publishToTurnSummary);
        resolutionDecisions += poisonToothDecision(defender, defenderBattlePlan, publishToTurnSummary);
        if (!resolutionDecisions.isEmpty())
            resolution += "\n" + resolutionDecisions;

        if (executeResolution) {
            if (isAggressorWin(game))
                checkForAssassination(game, getDefender(game), getAggressor(game), aggressorBattlePlan);
            else
                checkForAssassination(game, getAggressor(game), getDefender(game), defenderBattlePlan);
        } else {
            DuneTopic resultsChannel = publishToTurnSummary ? game.getTurnSummary() : game.getModInfo();
            resultsChannel.publish(resolution);

            checkForTraitorCall(game, getAggressor(game), aggressorBattlePlan, getDefender(game), defenderBattlePlan, publishToTurnSummary, executeResolution);
            checkForTraitorCall(game, getDefender(game), defenderBattlePlan, getAggressor(game), aggressorBattlePlan, publishToTurnSummary, executeResolution);
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

    public void betrayHarkTraitorAndResolve(Game game, boolean publishToTurnSummary, int turn, String territoryName) throws InvalidGameStateException {
        Faction factionWithHarkonnenNexusCard = game.getFactions().stream().filter(f -> f.getNexusCard() != null && f.getNexusCard().name().equals("Harkonnen")).findFirst().orElseThrow();
        game.discardNexusCard(factionWithHarkonnenNexusCard);
        game.getTurnSummary().publish(factionWithHarkonnenNexusCard.getEmoji() + " cancels the " + Emojis. HARKONNEN + " Traitor call!");

        boolean harkonnenCalledTraitor = false;
        if (aggressorCallsTraitor(game)) {
            if (getAggressor(game) instanceof HarkonnenFaction harkonnen && aggressorBattlePlan.isWillCallTraitor()) {
                aggressorBattlePlan.setWillCallTraitor(false);
                harkonnen.nexusCardBetrayal(defenderBattlePlan.getLeaderNameForTraitor());
                harkonnenCalledTraitor = true;
            } else if (getAggressor(game).getAlly() != null && getAggressor(game).getAlly().equals("Harkonnen") && aggressorBattlePlan.isHarkWillCallTraitor()) {
                aggressorBattlePlan.setHarkWillCallTraitor(false);
                game.getHarkonnenFaction().nexusCardBetrayal(defenderBattlePlan.getLeaderNameForTraitor());
                harkonnenCalledTraitor = true;
            }
        }
        if (defenderCallsTraitor(game)) {
            if (getDefender(game) instanceof HarkonnenFaction harkonnen && defenderBattlePlan.isWillCallTraitor()) {
                defenderBattlePlan.setWillCallTraitor(false);
                harkonnen.nexusCardBetrayal(aggressorBattlePlan.getLeaderNameForTraitor());
                harkonnenCalledTraitor = true;
            } else if (getDefender(game).getAlly() != null && getDefender(game).getAlly().equals("Harkonnen") && defenderBattlePlan.isHarkWillCallTraitor()) {
                defenderBattlePlan.setHarkWillCallTraitor(false);
                game.getHarkonnenFaction().nexusCardBetrayal(aggressorBattlePlan.getLeaderNameForTraitor());
                harkonnenCalledTraitor = true;
            }
        }
        if (!harkonnenCalledTraitor)
            throw new InvalidGameStateException("Harkonnen did not call Traitor.");
        printBattleResolution(game, true, false);
        resolveBattle(game, publishToTurnSummary, turn, territoryName);
    }

    private void checkForAssassination(Game game, Faction faction, Faction opponent, BattlePlan opponentPlan) {
        if (faction instanceof MoritaniFaction moritani && !opponentPlan.isWillCallTraitor() && !opponentPlan.isHarkWillCallTraitor() && opponentPlan.getLeader() != null && opponentPlan.isLeaderAlive()) {
            if (moritani.canAssassinate(opponent, opponentPlan.getLeader())) {
                assassinationMustBeResolved = true;
                List<DuneChoice> choices = new ArrayList<>();
                choices.add(new DuneChoice("moritani-assassinate-traitor-yes", "Yes"));
                choices.add(new DuneChoice("moritani-assassinate-traitor-no", "No"));
                String victim = moritani.getTraitorHand().getFirst().getName();
                if (game.getLeaderTanks().stream().anyMatch(l -> l.getName().equals(victim)))
                    moritani.getChat().publish(victim + " is in the tanks. Would you like to assassinate and draw a new Traitor in Mentat Pause? " + moritani.getPlayer(), choices);
                else
                    moritani.getChat().publish("Would you like to assassinate " + victim + "? " + moritani.getPlayer(), choices);
            }
        }
    }

    private void checkForTraitorCall(Game game, Faction faction, BattlePlan battlePlan, Faction opponent, BattlePlan opponentPlan, boolean publishToTurnSummary, boolean executeResolution) {
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

        checkForTraitorCall(game, faction, faction, battlePlan, opponent, opponentPlan, false, modInfo, publishToTurnSummary, executeResolution);
        if (faction.getAlly().equals("Harkonnen"))
            checkForTraitorCall(game, game.getHarkonnenFaction(), faction, battlePlan, opponent, opponentPlan, true, modInfo, publishToTurnSummary, executeResolution);
    }

    private void checkForTraitorCall(Game game, Faction faction, Faction combatant, BattlePlan battlePlan, Faction opponent, BattlePlan opponentPlan, boolean isHarkonnenAllyPower, DuneTopic modInfo, boolean publishToTurnSummary, boolean executeResolution) {
        String opponentLeader = opponentPlan.getLeaderNameForTraitor();
        String forYourAlly = isHarkonnenAllyPower ? "for your ally " : "";
        if (faction.hasTraitor(opponentLeader)) {
            if (executeResolution)
                faction.useTraitor(opponentLeader);
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
                if (isHarkonnenAllyPower)
                    battlePlan.setHarkCanCallTraitor(true);
                else
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
        boolean resolutionChecked = false;
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
                    resolutionChecked = true;
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
                resolutionChecked = true;
            } else if (!resolutionPublished && plan.isCanCallTraitor()) {
                plan.setWillCallTraitor(true);
                game.getModInfo().publish(faction.getEmoji() + " will call Traitor in " + wholeTerritoryName + " if possible.");
                if (faction.getAlly().equals("Harkonnen")) {
                    plan.presentEarlyTraitorChoices(game, game.getHarkonnenFaction(), opponent, true);
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
        if (resolutionPublished && !resolutionChecked)
            checkIfResolvable(game);
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
                plan.presentEarlyTraitorChoices(game, game.getHarkonnenFaction(), opponent, true);
                if (plan.isHarkCanCallTraitor())
                    game.getModInfo().publish(Emojis.HARKONNEN + " can call Traitor for ally " + faction.getEmoji() + " in " + wholeTerritoryName + ".");
                else
                    game.getModInfo().publish(Emojis.HARKONNEN + " cannot call Traitor for ally " + faction.getEmoji() + " in " + wholeTerritoryName + ".");
            }
        }
        return plan.isHarkCanCallTraitor();
    }

    private String lasgunShieldCarnage(Game game, boolean executeResolution) {
        List<Territory> allTerritorySectors = game.getTerritories().values().stream().filter(t -> t.getTerritoryName().startsWith(wholeTerritoryName)).toList();
        String carnage = "";
        if (aggressorBattlePlan.isLasgunShieldExplosion() && !aggressorCallsTraitor(game) && !defenderCallsTraitor(game)) {
            Territory noFieldTerritory = allTerritorySectors.stream().filter(Territory::hasRicheseNoField).findFirst().orElse(null);
            String ambassador = allTerritorySectors.stream().map(Territory::getEcazAmbassador).filter(Objects::nonNull).findFirst().orElse(null);
            if (executeResolution) {
                if (noFieldTerritory != null)
                    game.getRicheseFaction().revealNoField();
                for (Territory t : allTerritorySectors) {
                    for (Force f : nonCombatantForcesInSector(game, t)) {
                        int regular = f.getName().equals(f.getFactionName()) ? f.getStrength() : 0;
                        int starred = f.getName().equals(f.getFactionName() + "*") ? f.getStrength() : 0;
                        if (regular > 0 || starred > 0)
                            game.removeForcesAndReportToTurnSummary(t.getTerritoryName(), game.getFaction(f.getFactionName()), regular, starred, true, true);
                        else if (f.getName().equals("Advisor"))
                            game.removeAdvisorsAndReportToTurnSummary(t.getTerritoryName(), game.getBGFaction(), f.getStrength(), true);
                    }
                }
                if (ambassador != null) {
                    Territory territoryWithAmbassador = allTerritorySectors.stream().filter(t -> t.getEcazAmbassador() != null).findFirst().orElseThrow();
                    game.getEcazFaction().returnAmbassadorToSuppy(territoryWithAmbassador, ambassador);
                }
                allTerritorySectors.stream().filter(t -> t.getSpice() > 0).forEach(t -> game.getTurnSummary().publish(t.lasgunShieldDestroysSpice()));
            } else {
                if (noFieldTerritory != null)
                    carnage += Emojis.RICHESE + " reveals " + Emojis.NO_FIELD + " to be " + noFieldTerritory.getRicheseNoField() + " " + Emojis.RICHESE_TROOP + " and loses them to the tanks\n";
                String otherForces = nonCombatantForces(game, allTerritorySectors);
                if (!otherForces.isEmpty())
                    carnage += otherForces;
                if (ambassador != null)
                    carnage += Emojis.ECAZ + " " + ambassador + " ambassador returned to supply\n";
                carnage += String.join("", allTerritorySectors.stream().filter(s -> s.getSpice() > 0).map(s -> s.getSpice() + " " + Emojis.SPICE + " destroyed in " + s.getTerritoryName() + "\n").toList());
            }
        }
        if (!carnage.isEmpty())
            carnage = "\nLasgun-Shield carnage:\n" + carnage;
        return carnage;
    }

    private String checkAuditor(Game game, BattlePlan battlePlan, Faction opponent, boolean executeResolution) {
        String resolution = "";
        if (battlePlan.getLeader() != null && battlePlan.getLeader().getName().equals("Auditor")) {
            int numCards = battlePlan.isLeaderAlive() ? 2 : 1;
            if (executeResolution) {
                int spice = Math.min(numCards, cardsForAudit.size());
                if (spice == 0) {
                    game.getTurnSummary().publish(opponent.getEmoji() + " has no cards that can be audited.");
                } else {
                    List<DuneChoice> choices = new ArrayList<>();
                    choices.add(new DuneChoice("battle-cancel-audit-yes", "Yes"));
                    choices.add(new DuneChoice("battle-cancel-audit-no", "No"));
                    opponent.getChat().publish("Will you pay " + spice + " " + Emojis.SPICE + " to cancel the audit? " + opponent.getPlayer(), choices);
                    auditorMustBeResolved = true;
                }
            } else
                resolution += MessageFormat.format(
                        "{0} may audit {1} {2} cards not used in the battle unless {3} pays to cancel the audit.\n",
                        Emojis.CHOAM, numCards, Emojis.TREACHERY, opponent.getEmoji());
        }
        return resolution;
    }

    private String nonCombatantForces(Game game, List<Territory> allTerritorySectors) {
        return String.join("", allTerritorySectors.stream().map(t -> nonCombatantForcesInSectorString(game, t)).toList());
    }

    private String nonCombatantForcesInSectorString(Game game, Territory t) {
        List<Force> nonCombatantForces = nonCombatantForcesInSector(game, t);
        return String.join("", nonCombatantForces.stream().map(f -> Emojis.getFactionEmoji(f.getFactionName()) + " loses " + f.getStrength() + " " + Emojis.getForceEmoji(f.getName()) + " in " + t.getTerritoryName() + " to the tanks\n").toList());
    }

    private List<Force> nonCombatantForcesInSector(Game game, Territory t) {
        boolean battleSector = getTerritorySectors(game).contains(t);
        List<Force> nonCombatantForces = new ArrayList<>(t.getForces().stream().filter(f -> !battleSector || !factionNames.contains(f.getFactionName())).toList());
        nonCombatantForces.sort((a, b) -> {
            if (a.getFactionName().equals(b.getFactionName()))
                return a.getName().compareTo(b.getName());
            else
                return a.getFactionName().compareTo(b.getFactionName());
        });
        return nonCombatantForces;
    }

    public boolean isEmperorCunning() {
        return emperorCunning;
    }

    public void emperorNexusCunning(Game game, boolean useNexusCard) {
        Faction emperor = game.getEmperorFaction();
        emperorCunning = useNexusCard;
        if (useNexusCard) {
            game.discardNexusCard(emperor);
            game.getTurnSummary().publish(Emojis.EMPEROR + " may count up to 5 " + Emojis.EMPEROR_TROOP + " as " + Emojis.EMPEROR_SARDAUKAR + " in this battle.");
            emperor.getChat().reply("You played the " + Emojis.EMPEROR + " Nexus Card. Up to 5 " + Emojis.EMPEROR_TROOP + " will count as " + Emojis.EMPEROR_SARDAUKAR);
        } else
            emperor.getChat().reply("You will not play the " + Emojis.EMPEROR + " Nexus Card.");
    }

    public boolean isIxCunning() {
        return ixCunning;
    }

    public void setIxCunning(boolean ixCunning) {
        this.ixCunning = ixCunning;
    }

    private String juiceOfSaphoDecision(Faction faction, BattlePlan battlePlan, BattlePlan opponentBattlePlan, boolean publishToTurnSummary) {
        String decisionAnnouncement = "";
        if (juiceOfSaphoTBD != DecisionStatus.CLOSED && battlePlan.getDefense() == null) {
            if (faction.hasTreacheryCard("Juice of Sapho") && battlePlan.getDoubleBattleStrength() == opponentBattlePlan.getDoubleBattleStrength()) {
                juiceOfSaphoTBD = DecisionStatus.OPEN;
                if (publishToTurnSummary) {
                    List<DuneChoice> choices = new LinkedList<>();
                    choices.add(new DuneChoice("battle-juice-of-sapho-add", "Yes, add it"));
                    choices.add(new DuneChoice("battle-juice-of-sapho-don't-add", "No, keep it out"));
                    faction.getChat().publish("Will you play Juice of Sapho to become the aggressor in " + wholeTerritoryName + "? " + faction.getPlayer(), choices);
                } else
                    decisionAnnouncement += faction.getEmoji() + " may play Juice of Sapho.\n";
            }
        }
        return decisionAnnouncement;
    }

    private String portableSnooperDecision(Faction faction, BattlePlan battlePlan, BattlePlan opponentPlan, boolean publishToTurnSummary) {
        String decisionAnnouncement = "";
        TreacheryCard opponentWeapon = opponentPlan.getWeapon();
        boolean isPoisonWeapon = opponentWeapon != null && opponentWeapon.isStoppedBySnooper();
        if (portableSnooperTBD != DecisionStatus.CLOSED && battlePlan.getDefense() == null && isPoisonWeapon) {
            if (faction.hasTreacheryCard("Portable Snooper")) {
                portableSnooperTBD = DecisionStatus.OPEN;
                if (publishToTurnSummary) {
                    List<DuneChoice> choices = new LinkedList<>();
                    choices.add(new DuneChoice("battle-portable-snooper-add", "Yes, add it"));
                    choices.add(new DuneChoice("battle-portable-snooper-don't-add", "No, keep it out"));
                    faction.getChat().publish("Will you play Portable Snooper in " + wholeTerritoryName + "? " + faction.getPlayer(), choices);
                } else
                    decisionAnnouncement += faction.getEmoji() + " may play Portable Snooper.\n";
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
            decisionAnnouncement += faction.getEmoji() + " must decide if they will use Poison Tooth.\n";
            if (publishToTurnSummary) {
                List<DuneChoice> choices = new LinkedList<>();
                choices.add(new DuneChoice("battle-poison-tooth-keep", "Yes, kill"));
                choices.add(new DuneChoice("battle-poison-tooth-remove", "No, don't kill with it"));
                faction.getChat().publish("Will you use Poison Tooth from your plan in " + wholeTerritoryName + "? " + faction.getPlayer(), choices);
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
        juiceOfSaphoTBD = DecisionStatus.CLOSED;
        turnSummaryString += outcomeDifferences(game, wasAggressorLeaderAlive, wasDefenderLeaderAlive, false, oldResolutionString, combatWaterBefore);
        game.getTurnSummary().publish(turnSummaryString);
    }

    public void juiceOfSaphoDontAdd(Game game) {
        juiceOfSaphoTBD = DecisionStatus.CLOSED;
        checkIfResolvable(game);
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
        portableSnooperTBD = DecisionStatus.CLOSED;
        turnSummaryString += outcomeDifferences(game, wasAggressorLeaderAlive, wasDefenderLeaderAlive, false, oldResolutionString, combatWaterBefore);
        game.getTurnSummary().publish(turnSummaryString);
    }

    public void portableSnooperDontAdd(Game game) {
        portableSnooperTBD = DecisionStatus.CLOSED;
        checkIfResolvable(game);
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
        checkIfResolvable(game);
    }

    public void removePoisonTooth(Game game, Faction faction) throws InvalidGameStateException {
        boolean wasAggressorLeaderAlive = aggressorBattlePlan.isLeaderAlive();
        boolean wasDefenderLeaderAlive = defenderBattlePlan.isLeaderAlive();
        String oldResolutionString = getWinnerString(game);
        int combatWaterBefore = aggressorBattlePlan.combatWater() + defenderBattlePlan.combatWater();
        if (getAggressorName().equals(faction.getName())) {
            if (!aggressorBattlePlan.revokePoisonTooth())
                throw new InvalidGameStateException(faction.getName() + " did not play Poison Tooth");
            defenderBattlePlan.revealOpponentBattlePlan(aggressorBattlePlan);
        } else if (getDefenderName().equals(faction.getName())) {
            if (!defenderBattlePlan.revokePoisonTooth())
                throw new InvalidGameStateException(faction.getName() + " did not play Poison Tooth");
            aggressorBattlePlan.revealOpponentBattlePlan(defenderBattlePlan);
        } else {
            throw new InvalidGameStateException(faction.getName() + " is not in this battle.");
        }
        String turnSummaryString = faction.getEmoji() + " does not use Poison Tooth.\n";
        poisonToothTBD = DecisionStatus.CLOSED;
        turnSummaryString += outcomeDifferences(game, wasAggressorLeaderAlive, wasDefenderLeaderAlive, true, oldResolutionString, combatWaterBefore);
        game.getTurnSummary().publish(turnSummaryString);
    }

    public void keepPoisonTooth(Game game, Faction faction) {
        game.getTurnSummary().publish(faction.getEmoji() + " will use Poison Tooth.");
        poisonToothTBD = DecisionStatus.CLOSED;
        checkIfResolvable(game);
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
        List<String> openIssues = new ArrayList<>();
        if (juiceOfSaphoTBD == DecisionStatus.OPEN)
            openIssues.add("Juice of Sapho");
        if (portableSnooperTBD == DecisionStatus.OPEN)
            openIssues.add("Portable Snooper");
        if (stoneBurnerTBD == DecisionStatus.OPEN)
            openIssues.add("Stone Burner");
        if (mirrorWeaponStoneBurnerTBD == DecisionStatus.OPEN)
            openIssues.add("Mirror Weapon as Stone Burner");
        if (poisonToothTBD == DecisionStatus.OPEN)
            openIssues.add("Poison Tooth");
        if (aggressorBattlePlan.isCanCallTraitor() || aggressorBattlePlan.isHarkCanCallTraitor())
            openIssues.add("Aggressor Traitor Call");
        if (defenderBattlePlan.isCanCallTraitor() || defenderBattlePlan.isHarkCanCallTraitor())
            openIssues.add("Defender Traitor Call");

        if (openIssues.isEmpty() || overrideDecisions) {
            String resolveString = "Would you like the bot to resolve the battle? " + game.getModOrRoleMention();
            List<DuneChoice> choices = new ArrayList<>();
            Faction harkBetrayer = getHarkonnenNexusBetrayalFaction(game);
            if (harkBetrayer != null) {
                choices.add(new DuneChoice("battle-harkonnen-betrayal-resolve-turn-" + game.getTurn() + "-" + wholeTerritoryName, "Resolve with Hark Betrayal"));
                game.getModInfo().publish(harkBetrayer.getEmoji() + " may play " + Emojis.HARKONNEN + " Nexus Card Betrayal to cancel the Traitor call.");
            }
            choices.add(new DuneChoice("battle-resolve-turn-" + game.getTurn() + "-" + wholeTerritoryName, "Yes"));
            choices.add(new DuneChoice("secondary", "battle-dont-resolve", "No"));
            game.getModInfo().reply(resolveString, choices);
        } else
            game.getModInfo().reply("The following must be decided before the battle can be resolved:\n  " + String.join(", ", openIssues));
    }

    protected Faction getHarkonnenNexusBetrayalFaction(Game game) {
        HarkonnenFaction harkonnen = game.getHarkonnenFactionOrNull();
        if (harkonnen == null)
            return null;
        Faction factionWithHarkonnenNexusCard = game.getFactions().stream().filter(f -> f.getNexusCard() != null && f.getNexusCard().name().equals("Harkonnen")).findFirst().orElse(null);
        if (factionWithHarkonnenNexusCard == null)
            return null;
        if (factionWithHarkonnenNexusCard instanceof HarkonnenFaction)
            return null;
        if (harkonnen.hasAlly() && harkonnen.getAlly().equals(factionWithHarkonnenNexusCard.getName()))
            return null;

        if (getAggressor(game) instanceof HarkonnenFaction && aggressorBattlePlan.isWillCallTraitor() && aggressorCallsTraitor(game))
            return factionWithHarkonnenNexusCard;
        if (harkonnen.hasAlly() && harkonnen.getAlly().equals(getAggressorName()) && aggressorBattlePlan.isHarkWillCallTraitor() && aggressorCallsTraitor(game))
            return factionWithHarkonnenNexusCard;
        if (getDefender(game) instanceof HarkonnenFaction && defenderBattlePlan.isWillCallTraitor() && defenderCallsTraitor(game))
            return factionWithHarkonnenNexusCard;
        if (harkonnen.hasAlly() && harkonnen.getAlly().equals(getDefenderName()) && defenderBattlePlan.isHarkWillCallTraitor() && defenderCallsTraitor(game))
            return factionWithHarkonnenNexusCard;
        return null;
    }

    public boolean isDiplomatMustBeResolved() {
        return diplomatMustBeResolved;
    }

    public boolean isRihaniDeciphererMustBeResolved(Game game) {
        if (rihaniDeciphererFaction == null)
            return false;
        return game.getFaction(rihaniDeciphererFaction).getTraitorHand().size() != rihaniDeciphererExectedTraitors;
    }

    public boolean isTechTokenMustBeResolved(Game game) {
        if (techTokenReceiver == null)
            return false;
        return game.getFaction(techTokenReceiver).getTechTokens().size() != expectedTechTokens;
    }

    public String getHarkonnenLeaderVictim() {
        return harkonnenLeaderVictim;
    }

    public String getHarkonnenCapturedLeader() {
        return harkonnenCapturedLeader;
    }

    public void returnHarkonnenCapturedLeader(Game game) {
        harkonnenCapturedLeader = null;
        game.getTurnSummary().publish(Emojis.HARKONNEN + " chooses not to capture a leader.");
    }

    public boolean isHarkonnenCaptureMustBeResolved(Game game) {
        if (harkonnenCapturedLeader == null)
            return false;
        return game.getFaction(harkonnenLeaderVictim).getLeader(harkonnenCapturedLeader).isPresent();
    }

    public boolean isAuditorMustBeResolved() {
        return auditorMustBeResolved;
    }

    public boolean isAssassinationMustBeResolved() {
        return assassinationMustBeResolved;
    }

    public void populateCardsForAudit(Game game) {
        Faction aggressor = getAggressor(game);
        Faction defender = getDefender(game);
        Faction choam = null;
        BattlePlan choamPlan = null;
        Faction auditedFaction = null;
        BattlePlan auditedPlan = null;
        if (aggressor instanceof ChoamFaction) {
            choam = aggressor;
            choamPlan = aggressorBattlePlan;
            auditedFaction = defender;
            auditedPlan = defenderBattlePlan;
        } else if (defender instanceof ChoamFaction) {
            choam = defender;
            choamPlan = defenderBattlePlan;
            auditedFaction = aggressor;
            auditedPlan = aggressorBattlePlan;
        }
        if (choam != null && choamPlan.getLeader() != null && choamPlan.getLeader().getName().equals("Auditor")) {
            cardsForAudit = new ArrayList<>();
            List<String> cards = auditedFaction.getTreacheryHand().stream().map(TreacheryCard::name).toList();
            boolean cheapHeroFound = false;
            boolean weaponFound = false;
            boolean defenseFound = false;
            for (String card : cards) {
                if (!cheapHeroFound && auditedPlan.getCheapHero() != null && card.equals(auditedPlan.getCheapHero().name())) {
                    cheapHeroFound = true;
                } else if (!weaponFound && auditedPlan.getWeapon() != null && card.equals(auditedPlan.getWeapon().name())) {
                    weaponFound = true;
                } else if (!defenseFound && auditedPlan.getDefense() != null && card.equals(auditedPlan.getDefense().name())) {
                    defenseFound = true;
                } else {
                    cardsForAudit.add(card);
                }
            }
        }
    }

    public void cancelAudit(Game game, boolean cancel) throws InvalidGameStateException {
        Faction aggressor = getAggressor(game);
        Faction defender = getDefender(game);
        Faction choam;
        BattlePlan choamPlan;
        Faction auditedFaction;
        if (aggressor instanceof ChoamFaction) {
            choam = aggressor;
            choamPlan = aggressorBattlePlan;
            auditedFaction = defender;
        } else if (defender instanceof ChoamFaction) {
            choam = defender;
            choamPlan = defenderBattlePlan;
            auditedFaction = aggressor;
        } else {
            throw new InvalidGameStateException("CHOAM is not in this battle.");
        }

        int auditAmount = choamPlan.isLeaderAlive() ? 2 : 1;
        auditAmount = Math.min(auditAmount, cardsForAudit.size());
        if (cancel) {
            auditedFaction.subtractSpice(auditAmount, "cancel audit");
            choam.addSpice(auditAmount, auditedFaction.getEmoji() + " canceled audit");
            game.getTurnSummary().publish(auditedFaction.getEmoji() + " paid " + Emojis.CHOAM + " " + auditAmount + " " + Emojis.SPICE + " to cancel the audit.");
        } else {
            game.getTurnSummary().publish(auditedFaction.getEmoji() + " accepts audit from " + Emojis.CHOAM);
            List<String> cardsToShow = new ArrayList<>();
            if (cardsForAudit != null) {
                Collections.shuffle(cardsForAudit);
                cardsToShow = cardsForAudit.subList(0, Math.min(auditAmount, cardsForAudit.size())).stream().map(c -> Emojis.TREACHERY + " " + c + " " + Emojis.TREACHERY).toList();
            }
            if (cardsToShow.isEmpty()) {
                choam.getChat().publish(auditedFaction.getEmoji() + " has no " + Emojis.TREACHERY + " cards not played in the battle.");
                auditedFaction.getChat().publish("You had no " + Emojis.TREACHERY + " cards not played in the battle for " + choam.getEmoji() + " to audit.");
            } else {
                choam.getChat().publish(auditedFaction.getEmoji() + " has " + String.join(" and ", cardsToShow));
                auditedFaction.getChat().publish(choam.getEmoji() + " audited " + String.join(" and ", cardsToShow));
            }
        }
        auditorMustBeResolved = false;
    }

    public void assassinate(Game game, boolean kill) throws InvalidGameStateException {
        MoritaniFaction moritani = game.getMoritaniFaction();
        if (kill)
            moritani.assassinateTraitor();
        else
            moritani.getChat().reply("You did not assassinate " + moritani.getTraitorHand().getFirst().getName() + ".");
        assassinationMustBeResolved = false;
    }
}
