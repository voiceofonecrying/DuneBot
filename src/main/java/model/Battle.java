package model;

import constants.Emojis;
import enums.GameOption;
import exceptions.InvalidGameStateException;
import model.factions.*;

import java.util.*;

public class Battle {
    private final String wholeTerritoryName;
    private final List<Territory> territorySectors;
    private final List<String> factionNames;
    private final List<Force> forces;
    private final String ecazAllyName;
    private BattlePlan aggressorBattlePlan;
    private BattlePlan defenderBattlePlan;
    private boolean fedaykinKaramad;
    private boolean sardaukarKaramad;
    private boolean cyborgsKaramad;
    private boolean fremenMustPay;

    public Battle(String wholeTerritoryName, List<Territory> territorySectors, List<Faction> battleFactionsInStormOrder, List<Force> forces, String ecazAllyName) {
        this.wholeTerritoryName = wholeTerritoryName;
        this.territorySectors = territorySectors;
        this.factionNames = new ArrayList<>();
        battleFactionsInStormOrder.forEach(f -> factionNames.add(f.getName()));
        if (factionNames.get(0).equals("Ecaz") && factionNames.get(1).equals(ecazAllyName) || factionNames.get(0).equals(ecazAllyName) && factionNames.get(1).equals("Ecaz"))
            factionNames.add(factionNames.remove(1));
        this.forces = forces;
        this.ecazAllyName = ecazAllyName;
        this.fedaykinKaramad = false;
        this.sardaukarKaramad = false;
        this.cyborgsKaramad = false;
        this.fremenMustPay = false;
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
        return factionNames.get(0);
    }

    public Faction getAggressor(Game game) {
        return game.getFaction(factionNames.get(0));
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

    private boolean isSpiceNeeded(Game game, Faction faction, boolean starred) {
        if (starred && game.hasGameOption(GameOption.HOMEWORLDS) && faction instanceof EmperorFaction emperorFaction && emperorFaction.isSecundusHighThreshold())
            return false;
        else return !(faction instanceof FremenFaction) || fremenMustPay;
    }

    public static class ForcesDialed {
        int regularForcesDialed;
        int specialForcesDialed;
        int spiceUsed;

        ForcesDialed(int regularForcesDialed, int specialForcesDialed, int spiceUsed) {
            this.regularForcesDialed = regularForcesDialed;
            this.specialForcesDialed = specialForcesDialed;
            this.spiceUsed = spiceUsed;
        }
    }

    public ForcesDialed getForcesDialed(Game game, Faction faction, int wholeNumberDial, boolean plusHalfDial, int spice) throws InvalidGameStateException {
        String factionName = (hasEcazAndAlly() && faction instanceof EcazFaction) ? faction.getAlly() : faction.getName();
        boolean isFremen = faction instanceof FremenFaction;
        boolean isIx = faction instanceof IxFaction;
        boolean isEmperor = faction instanceof EmperorFaction;
        boolean specialsKaramad = isFremen && fedaykinKaramad || isEmperor && sardaukarKaramad || isIx && cyborgsKaramad;
        int specialStrength = forces.stream().filter(f -> f.getName().equals(factionName + "*")).findFirst().map(Force::getStrength).orElse(0);
        int regularStrength = forces.stream().filter(f -> f.getName().equals(factionName)).findFirst().map(Force::getStrength).orElse(0);
        if (faction instanceof RicheseFaction)
            regularStrength += forces.stream().filter(f -> f.getName().equals("NoField")).findFirst().map(Force::getStrength).orElse(0);
        int spiceUsed = 0;
        int dialUsed = 0;
        int specialStrengthUsed = 0;
        int regularStrengthUsed = 0;
        if (specialsKaramad) {
            while (!isIx && (spice - spiceUsed > 0 || !isSpiceNeeded(game, faction, false)) && wholeNumberDial - dialUsed >= 1 && regularStrength - regularStrengthUsed > 0) {
                dialUsed++;
                if (isSpiceNeeded(game, faction, false)) spiceUsed++;
                regularStrengthUsed++;
            }
            while ((spice - spiceUsed > 0 || !isSpiceNeeded(game, faction, true)) && wholeNumberDial - dialUsed >= 1 && specialStrength - specialStrengthUsed > 0) {
                dialUsed++;
                if (isSpiceNeeded(game, faction, true)) spiceUsed++;
                specialStrengthUsed++;
            }
            if ((wholeNumberDial > dialUsed) || plusHalfDial) {
                int troopsNeeded = (wholeNumberDial - dialUsed) * 2 + (plusHalfDial ? 1 : 0);
                if (isIx)
                    regularStrengthUsed += troopsNeeded;
                else {
                    troopsNeeded -= Math.min(troopsNeeded, regularStrength - regularStrengthUsed);
                    regularStrengthUsed = regularStrength;
                    specialStrengthUsed += troopsNeeded;
                }
            }
        } else {
            while ((spice - spiceUsed > 0 || !isSpiceNeeded(game, faction, true)) && wholeNumberDial - dialUsed >= 2 && specialStrength - specialStrengthUsed > 0) {
                dialUsed += 2;
                if (isSpiceNeeded(game, faction, true)) spiceUsed++;
                specialStrengthUsed++;
            }
            while ((spice - spiceUsed == 0 && isSpiceNeeded(game, faction, true)) && wholeNumberDial - dialUsed >= 1 && specialStrength - specialStrengthUsed > 0) {
                dialUsed++;
                specialStrengthUsed++;
            }
            while (!isIx && (spice - spiceUsed > 0 || !isSpiceNeeded(game, faction, false)) && wholeNumberDial - dialUsed >= 1 && regularStrength - regularStrengthUsed > 0) {
                dialUsed++;
                if (isSpiceNeeded(game, faction, false)) spiceUsed++;
                regularStrengthUsed++;
            }
            if ((wholeNumberDial > dialUsed) || plusHalfDial) {
                int troopsNeeded = (wholeNumberDial - dialUsed) * 2 + (plusHalfDial ? 1 : 0);
                regularStrengthUsed += troopsNeeded;
            }
        }
        if (regularStrengthUsed > regularStrength || specialStrengthUsed > specialStrength)
            throw new InvalidGameStateException(faction.getEmoji() + " does not have enough troops in the territory.");
        while (faction instanceof EmperorFaction && spiceUsed < spice && specialStrengthUsed > 0 && regularStrength - regularStrengthUsed >= 2) {
            specialStrengthUsed--;
            regularStrengthUsed += 2;
            spiceUsed++;
        }
        return new ForcesDialed(regularStrengthUsed, specialStrengthUsed, spiceUsed);
    }

    public int numForcesNotDialed(ForcesDialed forcesDialed, Faction faction, int spice) {
        int regularForcesDialed = forcesDialed.regularForcesDialed;
        int specialForcesDialed = forcesDialed.specialForcesDialed;
        String factionName = (hasEcazAndAlly() && faction instanceof EcazFaction) ? faction.getAlly() : faction.getName();
        int specialStrength = forces.stream().filter(f -> f.getName().equals(factionName + "*")).findFirst().map(Force::getStrength).orElse(0);
        int regularStrength = forces.stream().filter(f -> f.getName().equals(factionName)).findFirst().map(Force::getStrength).orElse(0);
        int forcesNotDialed = specialStrength - specialForcesDialed + regularStrength - regularForcesDialed;
        if (specialForcesDialed > 0 && regularStrength - regularForcesDialed >= 2) {
            List<DuneChoice> choices = new ArrayList<>();
            int numStarsReplaced = 0;
            int swapRatio = 2;
            if (faction instanceof EmperorFaction) swapRatio = 3;
            int swappableSpecials = specialStrength;
            if (faction instanceof IxFaction) swappableSpecials -= spice;
            while (regularStrength - regularForcesDialed >= swapRatio * numStarsReplaced && swappableSpecials > 0) {
                int altRegularDialed = regularForcesDialed + numStarsReplaced * swapRatio;
                int altSpecialDialed = specialForcesDialed - numStarsReplaced;
                int altNotDialed = forcesNotDialed + numStarsReplaced;
                String id = "forcesdialed-" + faction.getName() + "-" + altRegularDialed + "-" + altSpecialDialed + "-" + altNotDialed;
                String label = altRegularDialed + " + " + altSpecialDialed + "*" + (numStarsReplaced == 0 ? " (Current)" : "");
                choices.add(new DuneChoice(id, label));
                numStarsReplaced++;
                if (altSpecialDialed == 0 || faction instanceof IxFaction && altSpecialDialed == specialStrength - spice)
                    break;
            }
            faction.getChat().publish("How would you like to take troop losses?", choices);
        }
        return forcesNotDialed;
    }

    public int homeworldDialAdvantage(Game game, Territory territory, Faction faction) {
        String territoryName = territory.getTerritoryName();
        if (game.hasGameOption(GameOption.HOMEWORLDS)) {
            if (faction.getHomeworld().equals(territoryName))
                return (faction instanceof EmperorFaction && !faction.isHighThreshold() || faction instanceof BGFaction && faction.isHighThreshold())
                        ? 3 : 2;
            else if (faction instanceof EmperorFaction emperorFaction && territoryName.equals("Salusa Secundus"))
                return emperorFaction.isSecundusHighThreshold() ? 3 : 2;
        }
        return 0;
    }

    private int getNumForcesInReserve(Game game, Faction faction) {
        int numForcesInReserve = faction.getReserves().getStrength() + faction.getSpecialReserves().getStrength();
        if (faction instanceof RicheseFaction) {
            numForcesInReserve -= game.getTerritories().values().stream().filter(Territory::hasRicheseNoField).map(Territory::getRicheseNoField).findFirst().orElse(0);
        } else if (faction instanceof EmperorFaction && game.hasGameOption(GameOption.HOMEWORLDS)) {
            numForcesInReserve = game.getTerritory("Kaitain").getForces().stream().filter(f -> f.getName().equals("Emperor")).map(Force::getStrength).findFirst().orElse(0);
            numForcesInReserve += game.getTerritory("Kaitain").getForces().stream().filter(f -> f.getName().equals("Emperor*")).map(Force::getStrength).findFirst().orElse(0);
            numForcesInReserve += game.getTerritory("Salusa Secundus").getForces().stream().filter(f -> f.getName().equals("Emperor")).map(Force::getStrength).findFirst().orElse(0);
            numForcesInReserve += game.getTerritory("Salusa Secundus").getForces().stream().filter(f -> f.getName().equals("Emperor*")).map(Force::getStrength).findFirst().orElse(0);
        }
        return numForcesInReserve;
    }

    private void presentJuiceOfSaphoChoices(Game game, Faction faction, boolean tag) {
        List<DuneChoice> choices = new ArrayList<>();
        choices.add(new DuneChoice("battlesapho-yes", "Yes"));
        choices.add(new DuneChoice("battlesapho-no", "No"));
        faction.getChat().publish("Do you want to play Juice of Sapho to be aggressor in the battle of " + wholeTerritoryName + "?" + (tag ? " " + faction.getPlayer() : ""), choices);
        game.getModInfo().publish(faction.getEmoji() + " may choose to play Juice of Sapho in " + wholeTerritoryName + ". Please wait to resolve the battle.");
    }

    public void checkJuiceOfSapho(Game game, Faction faction) {
        if (getDefenderName().equals(faction.getName())) {
            if (faction.hasTreacheryCard("Juice of Sapho"))
                presentJuiceOfSaphoChoices(game, faction, false);
            else if (hasEcazAndAlly()) {
                if (faction instanceof EcazFaction && game.getFaction(faction.getAlly()).hasTreacheryCard("Juice of Sapho"))
                    presentJuiceOfSaphoChoices(game, game.getFaction(faction.getAlly()), true);
                else if (faction.getAlly().equals("Ecaz") && game.getFaction("Ecaz").hasTreacheryCard("Juice of Sapho"))
                    presentJuiceOfSaphoChoices(game, game.getFaction("Ecaz"), true);
            }
        }
    }

    public void karamaSpecialForces(Faction targetFaction) throws InvalidGameStateException {
        String targetFactionName = targetFaction.getName();
        boolean aggressorKaramad = targetFactionName.equals(getAggressorName());
        boolean defenderKaramad = targetFactionName.equals(getDefenderName());
        if (!aggressorKaramad && !defenderKaramad)
            throw new InvalidGameStateException(targetFactionName + " is not in the current battle.");

        if (targetFaction instanceof FremenFaction)
            fedaykinKaramad = true;
        else if (targetFaction instanceof EmperorFaction)
            sardaukarKaramad = true;
        else if (targetFaction instanceof IxFaction)
            cyborgsKaramad = true;

        boolean battlePlanRemoved = false;
        if (aggressorKaramad && aggressorBattlePlan != null) {
            aggressorBattlePlan = null;
            battlePlanRemoved = true;
        } else if (defenderKaramad && defenderBattlePlan != null) {
            defenderBattlePlan = null;
            battlePlanRemoved = true;
        }
        String message = "Your " + Emojis.getForceEmoji(targetFactionName + "*") + " advantage has been negated by Karama.";
        if (battlePlanRemoved)
            message += "\nYou must submit a new battle plan.";
        targetFaction.getChat().publish(message + " " + targetFaction.getPlayer());
    }

    public void karamaFremenMustPay(Game game) throws InvalidGameStateException {
        boolean aggressorKaramad = getAggressor(game) instanceof FremenFaction;
        boolean defenderKaramad = getDefender(game) instanceof FremenFaction;
        if (!aggressorKaramad && !defenderKaramad)
            throw new InvalidGameStateException(Emojis.FREMEN + " is not in the current battle.");

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

    public String getForcesRemainingString(String factionName, int regularDialed, int specialDialed) {
        int specialStrength = forces.stream().filter(f -> f.getName().equals(factionName + "*")).findFirst().map(Force::getStrength).orElse(0);
        int regularStrength = forces.stream().filter(f -> f.getName().equals(factionName)).findFirst().map(Force::getStrength).orElse(0);
        int regularNotDialed = regularStrength - regularDialed;
        int specialNotDialed = specialStrength - specialDialed;
        String forcesRemaining = "";
        if (regularNotDialed > 0) forcesRemaining += regularNotDialed + " " + Emojis.getForceEmoji(factionName) + " ";
        if (specialNotDialed > 0) forcesRemaining += specialNotDialed + " " + Emojis.getForceEmoji(factionName + "*") + " ";
        if (forcesRemaining.isEmpty()) forcesRemaining = "no " + Emojis.getFactionEmoji(factionName) + " forces ";
        return "This will leave " + forcesRemaining + "in " + wholeTerritoryName + " if you win.";
    }

    public BattlePlan setBattlePlan(Game game, Faction faction, Leader leader, TreacheryCard cheapHero, boolean kwisatzHaderach, int wholeNumberDial, boolean plusHalfDial, int spice, TreacheryCard weapon, TreacheryCard defense) throws InvalidGameStateException {
        int actualSize = factionNames.size();
        int numFactionsExpected = hasEcazAndAlly() ? 3 : 2;
        if (actualSize != numFactionsExpected)
            throw new InvalidGameStateException("Combatants not determined yet.");
//        if (ecazAllyToBeChosen && (faction instanceof EcazFaction || faction.getAlly().equals("Ecaz")))
//            throw new InvalidGameStateException("Ecaz must choose their alliance combatant.");

        boolean planIsForAggressor = false;
        if (getAggressorName().equals(faction.getName()))
            planIsForAggressor = true;
        else if (!getDefenderName().equals(faction.getName()))
            throw new InvalidGameStateException(faction.getEmoji() + " is not in this battle.");

        if (leader != null && cheapHero != null)
            throw new InvalidGameStateException(faction.getEmoji() + " cannot play both a leader and " + cheapHero.name());
        if (leader != null && !faction.getLeaders().contains(leader))
            throw new InvalidGameStateException(faction.getEmoji() + " does not have " + leader.getName());
        if (cheapHero != null && !faction.hasTreacheryCard(cheapHero.name()))
            throw new InvalidGameStateException(faction.getEmoji() + " does not have " + cheapHero.name());
        if (leader == null && cheapHero == null && !faction.getLeaders().stream().filter(l -> !l.getName().equals("Kwisatz Haderach")).toList().isEmpty())
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
        boolean isNotPlanetologist = leader == null || leader.getSkillCard() == null || !leader.getSkillCard().name().equals("Planetologist");
        if (weapon != null) {
            if (!faction.hasTreacheryCard(weapon.name()))
                throw new InvalidGameStateException(faction.getEmoji() + " does not have " + weapon.name());
            else if (weapon.name().equals("Chemistry") && (defense == null || !defense.type().startsWith("Defense")))
                throw new InvalidGameStateException("Chemistry can only be played as a weapon when playing another Defense");
            else if (weapon.name().equals("Harass and Withdraw")
                    && isNotPlanetologist
                    && (faction.getHomeworld().equals(wholeTerritoryName) || (faction instanceof EmperorFaction emperor && emperor.getSecondHomeworld().equals(wholeTerritoryName))))
                throw new InvalidGameStateException("Harass and Withdraw cannot be used on your Homeworld");
            else if (weapon.name().equals("Reinforcements")
                    && isNotPlanetologist
                    && getNumForcesInReserve(game, faction) < 3)
                throw new InvalidGameStateException("There must be at least 3 forces in reserves to use Reinformcements");
            else if (isNotPlanetologist && weapon.isGreenSpecialCard() && !weapon.name().equals("Harass and Withdraw") && !weapon.name().equals("Reinforcements"))
                throw new InvalidGameStateException(weapon.name() + " can only be played as a weapon if leader has Planetologist skill");
        }
        if (defense != null) {
            if (!faction.hasTreacheryCard(defense.name()))
                throw new InvalidGameStateException(faction.getEmoji() + " does not have " + defense.name());
            else if (defense.name().equals("Weirding Way") && (weapon == null || !weapon.type().startsWith("Weapon")))
                throw new InvalidGameStateException("Weirding Way can only be played as a defense when playing another Weapon");
            else if (defense.name().equals("Harass and Withdraw") && (faction.getHomeworld().equals(wholeTerritoryName) || (faction instanceof EmperorFaction emperor && emperor.getSecondHomeworld().equals(wholeTerritoryName))))
                throw new InvalidGameStateException("Harass and Withdraw cannot be used on Homeworld");
            else if (defense.name().equals("Reinforcements") && getNumForcesInReserve(game, faction) < 3)
                throw new InvalidGameStateException("There must be at least 3 forces in reserves to use Reinformcements");
        }

        int spiceForValidation = spice;
        if (game.hasGameOption(GameOption.STRONGHOLD_SKILLS)
            && (wholeTerritoryName.equals("Arrakeen") && faction.hasStrongholdCard("Arrakeen")
                || wholeTerritoryName.equals("Hidden Mobile Stronghold") && faction.hasHmsStrongholdProxy("Arrakeen")))
            spiceForValidation += 2;
        ForcesDialed forcesDialed = getForcesDialed(game, faction, wholeNumberDial, plusHalfDial, spice);
        int troopsNotDialed = numForcesNotDialed(forcesDialed, faction, spiceForValidation);
        if (spice > forcesDialed.spiceUsed)
            faction.getChat().publish("This dial can be supported with " + forcesDialed.spiceUsed + " " + Emojis.SPICE + ", reducing from " + spice + ".");
        spice = forcesDialed.spiceUsed;

        int ecazTroops = 0;
        if (hasEcazAndAlly() && (faction instanceof EcazFaction || faction.getAlly().equals("Ecaz")))
            ecazTroops = forces.stream().filter(f -> f.getFactionName().equals("Ecaz")).map(Force::getStrength).findFirst().orElse(0);

        // Handling of the hmsStrongholdProxy intentionally excluded here in case player initially selected Carthag but wants to change
        boolean hasCarthagStrongholdPower = game.hasGameOption(GameOption.STRONGHOLD_SKILLS) && wholeTerritoryName.equals("Carthag") && faction.hasStrongholdCard("Carthag");
        List<LeaderSkillCard> leaderSkillsInFront = faction.getSkilledLeaders().stream()
                .filter(l -> !(faction instanceof HarkonnenFaction)
                        || l.getName().equals("Feyd Rautha")
                        || l.getName().equals("Beast Rabban")
                        || l.getName().equals("Piter de Vries")
                        || l.getName().equals("Cpt. Iakin Nefud")
                        || l.getName().equals("Umman Kudu"))
                .filter(l -> !l.isPulledBehindShield())
                .map(Leader::getSkillCard)
                .toList();
        List<Territory> strongholds = game.getTerritories().values().stream().filter(Territory::isStronghold).toList();
        int numStrongholdsOccupied = strongholds.stream().filter(t -> t.getForces().stream().anyMatch(f -> f.getFactionName().equals(faction.getName()) && f.getStrength() > 0) || (faction instanceof RicheseFaction) && t.hasRicheseNoField()).toList().size();
        if (faction instanceof BGFaction)
            numStrongholdsOccupied = strongholds.stream().filter(t -> t.getForces().stream().anyMatch(f -> f.getName().equals("BG") && f.getStrength() > 0)).toList().size();
        BattlePlan battlePlan = new BattlePlan(planIsForAggressor, leader, cheapHero, kwisatzHaderach, weapon, defense, wholeNumberDial, plusHalfDial, spice, troopsNotDialed, ecazTroops, leaderSkillsInFront, hasCarthagStrongholdPower, homeworldDialAdvantage(game, territorySectors.get(0), faction), numStrongholdsOccupied, getNumForcesInReserve(game, faction));
        battlePlan.setRegularDialed(forcesDialed.regularForcesDialed);
        battlePlan.setSpecialDialed(forcesDialed.specialForcesDialed);
        game.getModInfo().publish(faction.getEmoji() + " battle plan for " + wholeTerritoryName + ":\n" + battlePlan.getPlanMessage(false));
        faction.getChat().publish("Your battle plan for " + wholeTerritoryName + " has been submitted:\n" + battlePlan.getPlanMessage(false));
        String factionName = faction.getName();
        faction.getChat().publish(getForcesRemainingString(factionName, forcesDialed.regularForcesDialed, forcesDialed.specialForcesDialed));
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
        return battlePlan;
    }

    public String updateTroopsDialed(String factionName, int regularDialed, int specialDialed, int notDialed) throws InvalidGameStateException {
        if (getAggressorName().equals(factionName))
            aggressorBattlePlan.setForcesDialed(regularDialed, specialDialed, notDialed);
        else if (getDefenderName().equals(factionName))
            defenderBattlePlan.setForcesDialed(regularDialed, specialDialed, notDialed);
        else
            throw new InvalidGameStateException(factionName + " is not in the current battle.");
        String planUpdatedString = "Battle plan updated to dial " + regularDialed + " " + Emojis.getForceEmoji(factionName) + " " + specialDialed + " " + Emojis.getForceEmoji(factionName + "*") +".";
        String forcesRemainingString = getForcesRemainingString(factionName, regularDialed, specialDialed);
        return planUpdatedString + "\n" + forcesRemainingString;
    }

    public void addCarthagStrongholdPower(Faction faction) throws InvalidGameStateException {
        BattlePlan battlePlan;
        if (getAggressorName().equals(faction.getName()))
            battlePlan = aggressorBattlePlan;
        else if (getDefenderName().equals(faction.getName()))
            battlePlan = defenderBattlePlan;
        else
            throw new InvalidGameStateException(faction.getEmoji() + " is not in this battle.");

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
}
