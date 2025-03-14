package model;

import constants.Emojis;
import enums.GameOption;
import exceptions.InvalidGameStateException;
import model.factions.*;
import net.dv8tion.jda.internal.utils.tuple.ImmutablePair;
import net.dv8tion.jda.internal.utils.tuple.Pair;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class BattlePlan {
    private final boolean aggressor;
    private final Leader leader;
    private final TreacheryCard cheapHero;
    private final boolean kwisatzHaderach;
    private final TreacheryCard weapon;
    private TreacheryCard defense;
    private TreacheryCard originalDefense;
    private final int wholeNumberDial;
    private final boolean plusHalfDial;
    private int spice;
    private final String wholeTerritoryName;
    private int regularDialed;
    private int specialDialed;
    private int regularNotDialed;
    private int specialNotDialed;
    private boolean dialedForcesSettled;
    private final boolean hasEcazAndAlly;
    private final int ecazTroopsForAlly;
    private final String dialFactionName;
    private final List<LeaderSkillCard> leaderSkillsInFront;
    private boolean carthagStrongholdCard;
    private final int homeworldDialAdvantage;
    private final int numStrongholdsOccupied;
    private final int numForcesInReserve;
    private int spiceBankerSupport;
    private boolean juiceOfSapho;
    private TreacheryCard opponentWeapon;
    private TreacheryCard opponentDefense;
    private Leader opponentLeader;
    private boolean opponentHasBureaucrat;
    private boolean inactivePoisonTooth;
    private boolean portableSnooperAdded;
    public boolean stoneBurnerNoKill;
    private boolean opponentStoneBurnerNoKill;
    private boolean canCallTraitor;
    private boolean declinedTraitor;
    private boolean willCallTraitor;
    private boolean harkCanCallTraitor;
    private boolean harkDeclinedTraitor;
    private boolean harkWillCallTraitor;
    private boolean leaderIsTraitor;
    private boolean opponentIsTraitor;

    public BattlePlan(Game game, Battle battle, Faction faction, boolean aggressor, Leader leader, TreacheryCard cheapHero, boolean kwisatzHaderach, TreacheryCard weapon, TreacheryCard defense, int wholeNumberDial, boolean plusHalfDial, int spice) throws InvalidGameStateException {
        this.wholeTerritoryName = battle.getWholeTerritoryName();
        this.numForcesInReserve = getNumForcesInReserve(game, faction);
        validatePlanInputs(game, battle, faction, leader, cheapHero, kwisatzHaderach, weapon, defense, spice);

        this.aggressor = aggressor;
        this.leader = leader;
        this.cheapHero = cheapHero;
        this.kwisatzHaderach = kwisatzHaderach;
        this.weapon = weapon;
        this.defense = defense;
        this.originalDefense = defense;
        this.wholeNumberDial = wholeNumberDial;
        this.plusHalfDial = plusHalfDial;
        this.dialedForcesSettled = true;
        this.hasEcazAndAlly = battle.hasEcazAndAlly() && (faction instanceof EcazFaction || faction.getAlly().equals("Ecaz"));
        this.ecazTroopsForAlly = hasEcazAndAlly ? battle.getForces().stream().filter(f -> f.getFactionName().equals("Ecaz")).map(Force::getStrength).findFirst().orElse(0) : 0;
        this.dialFactionName = hasEcazAndAlly && faction instanceof EcazFaction ? faction.getAlly() : faction.getName();
        calculateForcesDialedAndSpiceUsed(game, battle, faction, wholeNumberDial, plusHalfDial, spice);

        this.leaderSkillsInFront = getLeaderSkillsInFront(faction);
        // Handling of the hmsStrongholdProxy intentionally excluded here in case player initially selected Carthag but wants to change
        this.carthagStrongholdCard = game.hasGameOption(GameOption.STRONGHOLD_SKILLS) && wholeTerritoryName.equals("Carthag") && faction.hasStrongholdCard("Carthag");
        this.homeworldDialAdvantage = faction.homeworldDialAdvantage(game, battle.getTerritorySectors(game).getFirst());
        this.numStrongholdsOccupied = getNumStrongholdsOccupied(game, faction);
        this.spiceBankerSupport = 0;
        this.juiceOfSapho = false;
        this.stoneBurnerNoKill = false;

        game.getModInfo().publish(faction.getEmoji() + " battle plan for " + wholeTerritoryName + ":\n" + getPlanMessage(false));
        faction.getChat().publish("Your battle plan for " + wholeTerritoryName + " has been submitted:\n" + getPlanMessage(false));
        faction.getChat().publish(getForcesRemainingString());
        Faction opponent = aggressor ? battle.getDefender(game) : battle.getAggressor(game);
        this.canCallTraitor = false;
        this.declinedTraitor = false;
        this.willCallTraitor = false;
        this.harkCanCallTraitor = false;
        this.harkDeclinedTraitor = false;
        this.harkWillCallTraitor = false;
        this.leaderIsTraitor = false;
        this.opponentIsTraitor = false;
        presentEarlyTraitorChoices(game, faction, opponent, false);
    }

    public void presentEarlyTraitorChoices(Game game, Faction faction, Faction opponent, boolean isHarkonnenAllyPower) {
        if (faction instanceof BTFaction)
            return;
        if (game.getHomeworlds().containsValue(wholeTerritoryName) && !((HomeworldTerritory) game.getTerritory(wholeTerritoryName)).getNativeName().equals(faction.getName())) {
            faction.getChat().publish("You cannot call Traitor on " + wholeTerritoryName + ".");
            return;
        }
        List<String> eligibleTraitors = faction.getTraitorHand().stream().filter(t -> t.canBeCalledAgainst(opponent) && game.getLeaderTanks().stream().noneMatch(l -> l.getName().equals(t.getName()))).map(TraitorCard::getName).collect(Collectors.toList());
        if (!isHarkonnenAllyPower && faction.getAlly().equals("Harkonnen"))
            eligibleTraitors.add("one of " + Emojis.HARKONNEN + "'s traitors");
        String traitors = String.join(" or ", eligibleTraitors);
        if (!traitors.isEmpty()) {
            List<DuneChoice> choices = new ArrayList<>();
            choices.add(new DuneChoice("traitor-call-yes-turn-" + game.getTurn() + "-" + wholeTerritoryName, "Yes, I will call no matter what."));
            choices.add(new DuneChoice("traitor-call-no-turn-" + game.getTurn() + "-" + wholeTerritoryName, "No, arranged battle or not worth it."));
            choices.add(new DuneChoice("traitor-call-wait-turn-" + game.getTurn() + "-" + wholeTerritoryName, "Wait until I see battle wheels."));
            String forYourAlly = isHarkonnenAllyPower ? "for your ally " : "";
            String tag = isHarkonnenAllyPower ? " " + faction.getPlayer() : "";
            faction.getChat().publish("Will you call Traitor " + forYourAlly + "if " + opponent.getEmoji() + " plays " + traitors + "?" + tag, choices);
            if (isHarkonnenAllyPower)
                harkCanCallTraitor = true;
            else
                canCallTraitor = true;
        }
    }

    public void setLeaderIsTraitor(boolean leaderIsTraitor) {
        this.leaderIsTraitor = leaderIsTraitor;
    }

    public void setOpponentIsTraitor(boolean opponentIsTraitor) {
        this.opponentIsTraitor = opponentIsTraitor;
    }

    public Leader getLeader() {
        return leader;
    }

    public TreacheryCard getCheapHero() {
        return cheapHero;
    }

    public String getLeaderNameForTraitor() {
        String leaderNameForTraitor = "";
        if (getLeader() != null)
            leaderNameForTraitor = getLeader().getName();
        else if (getCheapHero() != null)
            leaderNameForTraitor = "Cheap Hero";
        return leaderNameForTraitor;
    }

    public int getWholeNumberDial() {
        return wholeNumberDial;
    }

    public int getRegularDialed() {
        return regularDialed;
    }

    public int getSpecialDialed() {
        return specialDialed;
    }

    public int getRegularNotDialed() {
        return regularNotDialed;
    }

    public int getSpecialNotDialed () {
        return specialNotDialed;
    }

    public int getNumForcesNotDialed() {
        return regularNotDialed + specialNotDialed;
    }

    public boolean isDialedForcesSettled() {
        return dialedForcesSettled;
    }

    public void setForcesDialed(int newRegularDialed, int newSpecialDialed) {
        dialedForcesSettled = true;
        int delta = regularDialed - newRegularDialed;
        regularDialed = newRegularDialed;
        regularNotDialed += delta;
        delta = specialDialed - newSpecialDialed;
        specialDialed = newSpecialDialed;
        specialNotDialed += delta;
    }

    private boolean isSpiceNeeded(Game game, Battle battle, Faction faction, boolean starred) {
        if (!starred && faction instanceof IxFaction)
            return false;
        if (starred && game.hasGameOption(GameOption.HOMEWORLDS) && faction instanceof EmperorFaction emperorFaction && emperorFaction.isSecundusHighThreshold())
            return false;
        else return !(faction instanceof FremenFaction) || battle.isFremenMustPay();
    }

    private void calculateForcesDialedAndSpiceUsed(Game game, Battle battle, Faction faction, int wholeNumberDial, boolean plusHalfDial, int spice) throws InvalidGameStateException {
        boolean arrakeenStrongholdCard = game.hasGameOption(GameOption.STRONGHOLD_SKILLS)
                && (wholeTerritoryName.equals("Arrakeen") && faction.hasStrongholdCard("Arrakeen")
                || wholeTerritoryName.equals("Hidden Mobile Stronghold") && faction.hasHmsStrongholdProxy("Arrakeen"));
        String factionName = (hasEcazAndAlly && faction instanceof EcazFaction) ? faction.getAlly() : faction.getName();
        boolean isFremen = faction instanceof FremenFaction;
        if (faction instanceof EcazFaction && hasEcazAndAlly && faction.getAlly().equals("Fremen"))
            isFremen = true;
        boolean isIx = faction instanceof IxFaction;
        if (faction instanceof EcazFaction && hasEcazAndAlly && faction.getAlly().equals("Ix"))
            isIx = true;
        boolean isEmperor = faction instanceof EmperorFaction;
        if (faction instanceof EcazFaction && hasEcazAndAlly && faction.getAlly().equals("Emperor"))
            isEmperor = true;
        boolean specialsNegated = isFremen && battle.isFedaykinNegated() || isEmperor && battle.isSardaukarNegated() || isIx && battle.isCyborgsNegated();
        specialNotDialed = battle.getForces().stream().filter(f -> f.getName().equals(factionName + "*")).findFirst().map(Force::getStrength).orElse(0);
        regularNotDialed = battle.getForces().stream().filter(f -> f.getName().equals(factionName)).findFirst().map(Force::getStrength).orElse(0);
        int noFieldNotUsed = 0;
        int numReserves = faction.getReservesStrength();
        int noFieldValue = battle.getForces().stream().filter(f -> f.getName().equals("NoField")).findFirst().map(Force::getStrength).orElse(0);
        if (faction instanceof RicheseFaction) {
            if (noFieldValue > numReserves)
                noFieldNotUsed = noFieldValue - numReserves;
            regularNotDialed += Math.min(noFieldValue, numReserves);
        }
        int spiceUsed = 0;
        int dialUsed = 0;
        specialDialed = 0;
        regularDialed = 0;
        if (arrakeenStrongholdCard)
            spice += 2;

        int doubleStrengthSpecial = specialsNegated ? 0 : specialNotDialed;
        int doubleStrengthSpecialWithSpice = 0;
        doubleStrengthSpecial = Math.min(wholeNumberDial / 2, doubleStrengthSpecial);
        if (isSpiceNeeded(game, battle, faction, true)) {
            doubleStrengthSpecial = Math.min(doubleStrengthSpecial, spice);
            doubleStrengthSpecialWithSpice = doubleStrengthSpecial;
            spiceUsed += doubleStrengthSpecial;
        }
        dialUsed += 2 * doubleStrengthSpecial;
        specialDialed += doubleStrengthSpecial;
        specialNotDialed -= doubleStrengthSpecial;

        int fullStrengthSpecial = specialNotDialed;
        fullStrengthSpecial = Math.min(wholeNumberDial - dialUsed, fullStrengthSpecial);
        int fullStregnthSpecialWithSpice = 0;
        if (specialsNegated) {
            if (isSpiceNeeded(game, battle, faction, true)) {
                fullStrengthSpecial = Math.min(fullStrengthSpecial, spice);
                fullStregnthSpecialWithSpice = fullStrengthSpecial;
                spiceUsed += fullStrengthSpecial;
            }
        } else if (isFremen && !isSpiceNeeded(game, battle, faction, true))
            fullStrengthSpecial = 0;
        dialUsed += fullStrengthSpecial;
        specialDialed += fullStrengthSpecial;
        specialNotDialed -= fullStrengthSpecial;

        int halfStrengthSpecial = specialNotDialed;
        halfStrengthSpecial = Math.min(wholeNumberDial - dialUsed, halfStrengthSpecial / 2) * 2;
        dialUsed += halfStrengthSpecial / 2;
        specialDialed += halfStrengthSpecial;
        specialNotDialed -= halfStrengthSpecial;

        if (faction instanceof EmperorFaction && battle.isEmperorCunning() && !specialsNegated) {
            int cunningSardaukar = Math.min(5, regularNotDialed);
            int doubleStrengthRegular = Math.min(cunningSardaukar, spice);
            doubleStrengthRegular = Math.min((wholeNumberDial - dialUsed) / 2, doubleStrengthRegular);
            spiceUsed += doubleStrengthRegular;
            dialUsed += 2 * doubleStrengthRegular;
            regularDialed += doubleStrengthRegular;
            regularNotDialed -= doubleStrengthRegular;

            int fullStrengthRegular = Math.min(wholeNumberDial - dialUsed, cunningSardaukar - doubleStrengthRegular);
            dialUsed += fullStrengthRegular;
            regularDialed += fullStrengthRegular;
            regularNotDialed -= fullStrengthRegular;
        }

        int fullStrengthRegular = regularNotDialed;
        if (faction instanceof IxFaction && !battle.isIxCunning())
            fullStrengthRegular = 0;
        fullStrengthRegular = Math.min(wholeNumberDial - dialUsed, fullStrengthRegular);
        if (isSpiceNeeded(game, battle, faction, false)) {
            fullStrengthRegular = Math.min(fullStrengthRegular, spice);
            spiceUsed += fullStrengthRegular;
        }
        dialUsed += fullStrengthRegular;
        regularDialed += fullStrengthRegular;
        regularNotDialed -= fullStrengthRegular;

        int halfStrengthRegular = regularNotDialed;
        halfStrengthRegular = Math.min(wholeNumberDial - dialUsed, halfStrengthRegular / 2) * 2;
        dialUsed += halfStrengthRegular / 2;
        regularDialed += halfStrengthRegular;
        regularNotDialed -= halfStrengthRegular;

        int spicedSpecials = doubleStrengthSpecialWithSpice + fullStregnthSpecialWithSpice;
        int unspicedSpecials = (doubleStrengthSpecial - doubleStrengthSpecialWithSpice) + (fullStrengthSpecial - fullStregnthSpecialWithSpice) + halfStrengthSpecial;
        if (!specialsNegated && spiceUsed < spice
                && (isEmperor || isFremen && isSpiceNeeded(game, battle, faction, true))) {
            int unspicedSpecialsToSwap = Math.min(spice - spiceUsed, unspicedSpecials);
            regularDialed += unspicedSpecialsToSwap;
            regularNotDialed -= unspicedSpecialsToSwap;
            specialDialed -= unspicedSpecialsToSwap;
            specialNotDialed += unspicedSpecialsToSwap;
            spiceUsed += unspicedSpecialsToSwap;
            unspicedSpecials -= unspicedSpecialsToSwap;
            int spicedSpecialsToSwap = Math.min(spice - spiceUsed, spicedSpecials);
            regularDialed += 2 * spicedSpecialsToSwap;
            regularNotDialed -= 2 * spicedSpecialsToSwap;
            specialDialed -= spicedSpecialsToSwap;
            specialNotDialed += spicedSpecialsToSwap;
            spiceUsed += spicedSpecialsToSwap;
            spicedSpecials -= spicedSpecialsToSwap;
        }

        if ((wholeNumberDial > dialUsed) || plusHalfDial) {
            int troopsNeeded = (wholeNumberDial - dialUsed) * 2 + (plusHalfDial ? 1 : 0);
            if (faction instanceof IxFaction && battle.isIxCunning())
                troopsNeeded = wholeNumberDial - dialUsed;
            regularDialed += troopsNeeded;
            regularNotDialed -= troopsNeeded;
        }
        if (regularNotDialed < 0 || specialNotDialed < 0) {
            if (noFieldNotUsed > 0)
                throw new InvalidGameStateException(faction.getName() + " has only " + numReserves + " forces in reserves to replace the " + noFieldValue + " No-Field");
            else
                throw new InvalidGameStateException(faction.getName() + " does not have enough troops in the territory.");
        }

        List<Pair<Integer, Integer>> pairsOfLossOptions = new ArrayList<>();
        pairsOfLossOptions.add(new ImmutablePair<>(regularDialed, specialDialed));
        int regularsSwappedIn = 0;
        int specialsSwappedOut = 0;
        if (isEmperor && !isSpiceNeeded(game, battle, faction, true)) {
            while (specialsNegated && (regularNotDialed - regularsSwappedIn) >= 2 && unspicedSpecials > 0) {
                regularsSwappedIn += 2;
                specialsSwappedOut++;
                unspicedSpecials--;
                pairsOfLossOptions.add(new ImmutablePair<>(regularDialed + regularsSwappedIn, specialDialed - specialsSwappedOut));
            }
            while (!specialsNegated && (regularNotDialed - regularsSwappedIn) >= 4 && unspicedSpecials > 0) {
                regularsSwappedIn += 4;
                specialsSwappedOut++;
                unspicedSpecials--;
                pairsOfLossOptions.add(new ImmutablePair<>(regularDialed + regularsSwappedIn, specialDialed - specialsSwappedOut));
            }
        } else if (isIx && battle.isIxCunning()) {
            while (unspicedSpecials > 0 && (regularNotDialed - regularsSwappedIn) >= 1) {
                regularsSwappedIn++;
                unspicedSpecials--;
                pairsOfLossOptions.add(new ImmutablePair<>(regularDialed + regularsSwappedIn, specialDialed - unspicedSpecials));
            }
            while (spicedSpecials > 0 && (regularNotDialed - regularsSwappedIn) >= 2) {
                regularsSwappedIn++;
                spicedSpecials--;
                pairsOfLossOptions.add(new ImmutablePair<>(regularDialed + regularsSwappedIn, specialDialed - spicedSpecials));
            }
        } else {
            while (specialsNegated && (regularNotDialed - regularsSwappedIn) >= 1 && unspicedSpecials > 0) {
                regularsSwappedIn += 1;
                specialsSwappedOut += 1;
                unspicedSpecials--;
                pairsOfLossOptions.add(new ImmutablePair<>(regularDialed + regularsSwappedIn, specialDialed - specialsSwappedOut));
            }
            while (specialsNegated && (regularNotDialed - regularsSwappedIn) >= 1 && spicedSpecials > 0) {
                regularsSwappedIn += 1;
                specialsSwappedOut += 1;
                spicedSpecials--;
                pairsOfLossOptions.add(new ImmutablePair<>(regularDialed + regularsSwappedIn, specialDialed - specialsSwappedOut));
            }
            while (!specialsNegated && (regularNotDialed - regularsSwappedIn) >= 2 && unspicedSpecials > 0) {
                regularsSwappedIn += 2;
                specialsSwappedOut += 1;
                unspicedSpecials--;
                pairsOfLossOptions.add(new ImmutablePair<>(regularDialed + regularsSwappedIn, specialDialed - specialsSwappedOut));
            }
            while (!specialsNegated && (regularNotDialed - regularsSwappedIn) >= 2 && spicedSpecials > 0) {
                regularsSwappedIn += 3;
                specialsSwappedOut += 1;
                spicedSpecials--;
                pairsOfLossOptions.add(new ImmutablePair<>(regularDialed + regularsSwappedIn, specialDialed - specialsSwappedOut));
            }
        }
        if (pairsOfLossOptions.size() > 1) {
            pairsOfLossOptions = pairsOfLossOptions.reversed();
            if (!(faction instanceof IxFaction) || battle.isIxCunning()) {
                int preferredLossesPair = 0;
                if (pairsOfLossOptions.get(1).getRight() == 1 && !specialsNegated)
                    preferredLossesPair = 1;
                int regularsToSwapIn = pairsOfLossOptions.get(preferredLossesPair).getLeft() - regularDialed;
                regularDialed += regularsToSwapIn;
                regularNotDialed -= regularsToSwapIn;
                int specialsToSwapOut = specialDialed - pairsOfLossOptions.get(preferredLossesPair).getRight();
                specialDialed -= specialsToSwapOut;
                specialNotDialed += specialsToSwapOut;
            }
            List<DuneChoice> choices = new ArrayList<>();
            for (Pair<Integer, Integer> p : pairsOfLossOptions) {
                String id = "battle-forces-dialed-" + faction.getName() + "-" + p.getLeft() + "-" + p.getRight();
                String label = p.getLeft() + " + " + p.getRight() + "*" + (specialDialed == p.getRight() ? " (Current)" : "");
                choices.add(new DuneChoice(id, label));
            }
            faction.getChat().publish("How would you like to take troop losses?", choices);
            dialedForcesSettled = false;
        }

        if (arrakeenStrongholdCard) {
            spiceUsed = Math.max(0, spiceUsed - 2);
            spice -= 2;
        }

        if (spice > spiceUsed)
            faction.getChat().publish("This dial can be supported with " + spiceUsed + " " + Emojis.SPICE + ", reducing from " + spice + ".");
        this.spice = spiceUsed;
    }

    public String getForcesRemainingString() {
        String forcesRemaining = "";
        if (regularNotDialed > 0) forcesRemaining += regularNotDialed + " " + Emojis.getForceEmoji(dialFactionName) + " ";
        if (specialNotDialed > 0) forcesRemaining += specialNotDialed + " " + Emojis.getForceEmoji(dialFactionName + "*") + " ";
        if (forcesRemaining.isEmpty()) forcesRemaining = "no " + Emojis.getFactionEmoji(dialFactionName) + " forces ";
        if (hasEcazAndAlly)
            forcesRemaining += Math.floorDiv(ecazTroopsForAlly, 2) + " " + Emojis.ECAZ_TROOP + " ";
        if (weapon != null && weapon.name().equals("Lasgun") && defense != null && defense.name().equals("Shield"))
            return "KABOOM! All forces will be sent to the tanks.";
        return "This will leave " + forcesRemaining + "in " + wholeTerritoryName + " if you win.";
    }

    public int getNumForcesInReserve() {
        return numForcesInReserve;
    }

    public int getEcazTroopsForAlly() {
        return ecazTroopsForAlly;
    }

    public int getSpice() {
        return spice;
    }

    public boolean hasKwisatzHaderach() {
        return kwisatzHaderach;
    }

    public TreacheryCard getWeapon() {
        return weapon;
    }

    public TreacheryCard getDefense() {
        return originalDefense;
    }

    public boolean isStoneBurnerNoKill() {
        return stoneBurnerNoKill;
    }

    public int getLeaderValue() {
        if (leader != null && leader.getName().equals("Zoal"))
            return opponentLeader == null ? 0 : opponentLeader.getValue();
        return leader == null ? 0 : leader.getValue();
    }

    public int getLeaderContribution() {
        if (artilleryStrike() || !isLeaderAlive())
            return 0;
        return getLeaderValue() + (kwisatzHaderach ? 2 : 0);
    }

    public boolean isSkillBehindAndLeaderAlive(String skillName) {
        return leader != null && leader.isPulledBehindShield() && isLeaderAlive() && leader.getSkillCard().name().equals(skillName);
    }

    public boolean isSkillInFront(String skillName) {
        return leaderSkillsInFront != null && leaderSkillsInFront.stream().anyMatch(s -> s.name().equals(skillName));
    }

    public boolean isSkillInFrontAndLeaderAlive(String skillName) {
        return leader != null && isLeaderAlive() && leaderSkillsInFront != null && leaderSkillsInFront.stream().anyMatch(s -> s.name().equals(skillName));
    }

    private boolean isOpponentWeaponPoison() {
        return opponentWeapon != null
                && (opponentWeapon.type().equals("Weapon - Poison")
                || opponentWeapon.name().equals("Chemistry")
                || opponentWeapon.name().equals("Poison Blade")
                || opponentWeapon.name().equals("Poison Tooth")
        );
    }

    private boolean isPoisonWeapon() {
        return weapon != null
                && (weapon.type().equals("Weapon - Poison")
                || weapon.name().equals("Chemistry")
                || weapon.name().equals("Poison Blade")
                || weapon.name().equals("Poison Tooth") && !inactivePoisonTooth
                || weapon.name().equals("Mirror Weapon") && isOpponentWeaponPoison()
        );
    }

    private boolean isOpponentWeaponProjectile() {
        return opponentWeapon != null
                && (opponentWeapon.type().equals("Weapon - Projectile")
                || opponentWeapon.name().equals("Weirding Way")
                || opponentWeapon.name().equals("Poison Blade")
        );
    }

    private boolean isProjectileWeapon() {
        return weapon != null
                && (weapon.type().equals("Weapon - Projectile")
                || weapon.name().equals("Weirding Way")
                || weapon.name().equals("Poison Blade")
                || weapon.name().equals("Mirror Weapon") && isOpponentWeaponProjectile()
        );
    }

    private boolean isSpecialForWeapon() {
        return weapon != null && (weapon.type().startsWith("Special") || weapon.type().equals("Spice Blow - Special"));
    }

    private boolean isPoisonDefense() {
        return defense != null && (defense.servesAsSnooper() || defense.name().equals("Chemistry"));
    }

    protected boolean isProjectileDefense() {
        return defense != null && (defense.servesAsShield() || defense.name().equals("Weirding Way"));
    }

    private boolean hasWorthlessCard() {
        return weapon != null && weapon.type().equals("Worthless Card") || originalDefense != null && originalDefense.type().equals("Worthless Card");
    }

    public String getLeaderString(boolean revealLeaderSkills) {
        String khString = kwisatzHaderach ? " + KH (2)" : "";
        String leaderString = "-";
        String leaderName = "";
        if (leader != null) {
            leaderName = leader.getName();
            leaderString = MessageFormat.format("{0} ({1}){2}",
                    leaderName, leaderName.equals("Zoal") ? "X" : leader.getValue(), khString);
        }
        else if (cheapHero != null) leaderString = cheapHero.name() + " (0)" + khString;

        if (isSkillBehindAndLeaderAlive("Killer Medic") && isPoisonDefense())
            leaderString += "\n  +3 for Killer Medic" + (revealLeaderSkills ? "" : " if " + leaderName + " survives");
        if (isSkillInFront("Killer Medic") && isPoisonDefense())
            leaderString += "\n  +1 for Killer Medic";
        if (isSkillBehindAndLeaderAlive("Master of Assassins") && isPoisonWeapon())
            leaderString += "\n  +3 for Master of Assassins" + (revealLeaderSkills ? "" : " if " + leaderName + " survives");
        if (isSkillInFront("Master of Assassins") && isPoisonWeapon())
            leaderString += "\n  +1 for Master of Assassins";
        if (isSkillBehindAndLeaderAlive("Mentat"))
            leaderString += "\n  +2 for Mentat" + (revealLeaderSkills ? "" : " if " + leaderName + " survives");
        if (isSkillBehindAndLeaderAlive("Planetologist") && isSpecialForWeapon())
            leaderString += "\n  +2 for Planetologist" + (revealLeaderSkills ? "" : " if " + leaderName + " survives");
        if (isSkillBehindAndLeaderAlive("Prana Bindu Adept") && isProjectileDefense())
            leaderString += "\n  +3 for Prana Bindu Adept" + (revealLeaderSkills ? "" : " if " + leaderName + " survives");
        if (isSkillInFront("Prana Bindu Adept") && isProjectileDefense())
            leaderString += "\n  +1 for Prana Bindu Adept";
        if (isSkillBehindAndLeaderAlive("Swordmaster of Ginaz") && isProjectileWeapon())
            leaderString += "\n  +3 for Swordmaster of Ginaz" + (revealLeaderSkills ? "" : " if " + leaderName + " survives");
        if (isSkillInFront("Swordmaster of Ginaz") && isProjectileWeapon())
            leaderString += "\n  +1 for Swordmaster of Ginaz";
        if (isSkillBehindAndLeaderAlive("Spice Banker"))
            leaderString += "\n  +" + spiceBankerSupport + " for Spice Banker" + (revealLeaderSkills ? "" : " if " + leaderName + " survives");
        if (isSkillBehindAndLeaderAlive("Warmaster") && hasWorthlessCard())
            leaderString += "\n  +3 for Warmaster" + (revealLeaderSkills ? "" : " if " + leaderName + " survives");
        if (isSkillInFront("Warmaster") && hasWorthlessCard())
            leaderString += "\n  +1 for Warmaster";
        if (revealLeaderSkills) {
            if (opponentHasBureaucrat && numStrongholdsOccupied > 0)
                leaderString += "\n -" + numStrongholdsOccupied + " for opponent Bureaucrat";
        }
        return "Leader: " + leaderString;
    }

    public int getDoubleBattleStrength() {
        int bonuses = homeworldDialAdvantage;
        if (numForcesInReserve >= 3 && (weapon != null && weapon.name().equals("Reinforcements") || defense != null && defense.name().equals("Reinforcements")))
            bonuses += 2;
        if (isSkillBehindAndLeaderAlive("Killer Medic") && isPoisonDefense())
            bonuses += 3;
        if (isSkillInFrontAndLeaderAlive("Killer Medic") && isPoisonDefense())
            bonuses += 1;
        if (isSkillBehindAndLeaderAlive("Master of Assassins") && isPoisonWeapon())
            bonuses += 3;
        if (isSkillInFrontAndLeaderAlive("Master of Assassins") && isPoisonWeapon())
            bonuses += 1;
        if (isSkillBehindAndLeaderAlive("Mentat"))
            bonuses += 2;
        if (isSkillBehindAndLeaderAlive("Planetologist") && isSpecialForWeapon())
            bonuses += 2;
        if (isSkillBehindAndLeaderAlive("Prana Bindu Adept") && isProjectileDefense())
            bonuses += 3;
        if (isSkillInFrontAndLeaderAlive("Prana Bindu Adept") && isProjectileDefense())
            bonuses += 1;
        if (isSkillBehindAndLeaderAlive("Spice Banker"))
            bonuses += spiceBankerSupport;
        if (isSkillBehindAndLeaderAlive("Swordmaster of Ginaz") && isProjectileWeapon())
            bonuses += 3;
        if (isSkillInFrontAndLeaderAlive("Swordmaster of Ginaz") && isProjectileWeapon())
            bonuses += 1;
        if (isSkillBehindAndLeaderAlive("Warmaster") && hasWorthlessCard())
            bonuses += 3;
        if (isSkillInFrontAndLeaderAlive("Warmaster") && hasWorthlessCard())
            bonuses += 1;
        if (opponentHasBureaucrat)
            bonuses -= numStrongholdsOccupied;
        if (stoneBurnerForTroops())
            return 2 * (regularNotDialed + specialNotDialed) + 2 * bonuses;
        int doubleBattleStrength = 2 * wholeNumberDial + 2 * bonuses;
        if (plusHalfDial) doubleBattleStrength++;
        doubleBattleStrength += 2 * getLeaderContribution();
        doubleBattleStrength += 2 * Math.ceilDiv(ecazTroopsForAlly, 2);
        return doubleBattleStrength;
    }

    public String getTotalStrengthString() {
        int wholeNumber = getDoubleBattleStrength() / 2;
        return MessageFormat.format("{0}{1}", wholeNumber, plusHalfDial ? ".5" : "");
    }

    public void addCarthagStrongholdPower() {
        carthagStrongholdCard = true;
    }

    private boolean artilleryStrike() {
        return opponentWeapon != null && opponentWeapon.name().equals("Artillery Strike")
                || weapon != null && weapon.name().equals("Artillery Strike");
    }

    private boolean poisonTooth() {
        return opponentWeapon != null && opponentWeapon.name().equals("Poison Tooth")
                || weapon != null && !inactivePoisonTooth && weapon.name().equals("Poison Tooth");
    }

    public boolean stoneBurnerForTroops() {
        if (aggressor && weapon != null && weapon.name().equals("Artillery Strike")
                || !aggressor && opponentWeapon != null && opponentWeapon.name().equals("Artillery Strike"))
            return false;
        return opponentWeapon != null && opponentWeapon.name().equals("Stone Burner")
                || weapon != null && weapon.name().equals("Stone Burner");
    }

    private boolean stoneBurnerKills() {
        return opponentWeapon != null && !opponentStoneBurnerNoKill && opponentWeapon.name().equals("Stone Burner")
                || weapon != null && !stoneBurnerNoKill && weapon.name().equals("Stone Burner");
    }

    public void dontKillWithStoneBurner() {
        if (weapon != null) {
            if (weapon.name().equals("Stone Burner") || opponentWeapon != null && opponentWeapon.name().equals("Stone Burner") && weapon.name().equals("Mirror Weapon")) {
                stoneBurnerNoKill = true;
            }
        }
    }

    private boolean carthagStrongholdPoisonDefense() {
        TreacheryCard effectiveWeapon = weapon;
        if (weapon != null && weapon.name().equals("Mirror Weapon")) effectiveWeapon = opponentWeapon;
        boolean nonPoisonWeapon = effectiveWeapon == null || (!effectiveWeapon.type().equals("Weapon - Poison") && !effectiveWeapon.name().equals("Chemistry")
                && !effectiveWeapon.name().equals("Poison Tooth") && !effectiveWeapon.name().equals("Poison Blade"));
        return carthagStrongholdCard && nonPoisonWeapon && defense != null && defense.servesAsShield();
    }

    public boolean isLeaderAlive() {
        if (leaderIsTraitor) {
            return false;
        } else if (opponentIsTraitor) {
            return true;
        } else if (isLasgunShieldExplosion()) {
            return false;
        } else if (stoneBurnerKills()) {
            return false;
        } else if (artilleryStrike()) {
            return defense != null && defense.servesAsShield();
        } else if (poisonTooth()) {
            return defense != null && defense.name().equals("Chemistry");
        } else if (opponentWeapon != null && !opponentWeapon.type().equals("Worthless Card") && !opponentWeapon.name().equals("Stone Burner")) {
            if (defense == null)
                return false;
            else if (opponentWeapon.name().equals("Poison Blade")
                    && !(defense.name().equals("Shield Snooper") || carthagStrongholdPoisonDefense()))
                return false;
            else if ((opponentWeapon.type().equals("Weapon - Poison") || opponentWeapon.name().equals("Chemistry"))
                    && !(defense.servesAsSnooper() || defense.name().equals("Chemistry") || carthagStrongholdPoisonDefense()))
                return false;
            else if ((opponentWeapon.type().equals("Weapon - Projectile") || opponentWeapon.name().equals("Weirding Way"))
                    && !(defense.servesAsShield() || defense.name().equals("Weirding Way")))
                return false;
            else
                return !opponentWeapon.name().equals("Lasgun");
        }
        return true;
    }

    public int combatWater() {
        return isLeaderAlive() || isLasgunShieldExplosion() || artilleryStrike() ? 0 : getLeaderValue();
    }

    public String getKilledLeaderString() {
        return leader == null ? "" : leader.getName();
    }

    public String getWeaponString() {
        if (inactivePoisonTooth)
            return "Weapon: ~~" + weapon.name() + "~~ (not used)";
        return "Weapon: " + (weapon == null ? "-"
                : weapon.name()) + (stoneBurnerNoKill ? " (leaders not killed)" : "");
    }

    public String getDefenseString() {
        return "Defense: " + (originalDefense == null ? "-" : originalDefense.name() + (carthagStrongholdPoisonDefense() ? " + Snooper from Carthag stronghold card" : ""));
    }

    public String getDialString() {
        String dialString = "Dial: " + wholeNumberDial + (plusHalfDial ? ".5" : "");
        if (ecazTroopsForAlly != 0 && !stoneBurnerForTroops())
            dialString += " + " + Math.ceilDiv(ecazTroopsForAlly, 2) + " " + Emojis.ECAZ_TROOP + " support";
        if (homeworldDialAdvantage != 0)
            dialString += "\n  +" + homeworldDialAdvantage + " for Homeworld advantage";
        if (numForcesInReserve >= 3 && (weapon != null && weapon.name().equals("Reinforcements") || defense != null && defense.name().equals("Reinforcements")))
            dialString += "\n  +2 for Reinforcements";
        return dialString;
    }

    public String getSpiceString() {
        return "Spice: " + spice + (spiceBankerSupport > 0 ? " + " + spiceBankerSupport + " for Spice Banker" : "");
    }

    public String getPlanMessage(boolean revealLeaderSkills) {
        return getLeaderString(revealLeaderSkills) + "\n"
                + getWeaponString() + "\n"
                + getDefenseString() + "\n"
                + getDialString() + "\n"
                + getSpiceString();
    }

    public int getSpiceBankerSupport() {
        return spiceBankerSupport;
    }

    public void setSpiceBankerSupport(int spiceBankerSupport) {
        this.spiceBankerSupport = spiceBankerSupport;
    }

    public boolean isJuiceOfSapho() {
        return juiceOfSapho;
    }

    public void setJuiceOfSapho(boolean juiceOfSapho) {
        this.juiceOfSapho = juiceOfSapho;
    }

    public void revealOpponentBattlePlan(BattlePlan opponentPlan) {
        opponentLeader = opponentPlan.getLeader();
        opponentWeapon = opponentPlan.inactivePoisonTooth ? null : opponentPlan.getWeapon();
        if (opponentWeapon != null) {
            if (opponentWeapon.type().equals("Worthless Card")
                    || opponentWeapon.type().startsWith("Special")
                    || opponentWeapon.type().equals("Spice Blow - Special"))
                opponentWeapon = null;
            else if (opponentWeapon.name().equals("Mirror Weapon"))
                opponentWeapon = stoneBurnerNoKill ? null : weapon;
        }
        opponentDefense = opponentPlan.getDefense();
        if (isSkillInFront("Diplomat") && (defense == null && weapon != null && weapon.type().equals("Worthless Card") || defense != null && defense.type().equals("Worthless Card")))
            defense = opponentDefense;
        if (opponentPlan.isSkillBehindAndLeaderAlive("Bureaucrat"))
            opponentHasBureaucrat = true;
        opponentStoneBurnerNoKill = opponentPlan.isStoneBurnerNoKill();
    }

    public boolean isOpponentHasBureaucrat() {
        return opponentHasBureaucrat;
    }

    public boolean isLasgunShieldExplosion() {
        return ((defense != null && defense.servesAsShield() || opponentDefense != null && opponentDefense.servesAsShield())
                && (weapon != null && weapon.name().equals("Lasgun") || opponentWeapon != null && opponentWeapon.name().equals("Lasgun")));
    }

    private boolean cardMustBeDiscarded(TreacheryCard card) {
        if (card != null) {
            if (card.name().equals("Artillery Strike")) return true;
            if (card.name().equals("Mirror Weapon")) return true;
            if (card.name().equals("Poison Tooth") && !inactivePoisonTooth) return true;
            if (card.name().equals("Portable Snooper")) return true;
            if (card.name().equals("Stone Burner")) return true;
            if (card.type().equals("Spice Blow - Special")) return true;
            if (card.type().startsWith("Special")) return true;
            return card.type().equals("Worthless Card");
        }
        return false;
    }

    public boolean weaponMustBeDiscarded(boolean loser) {
        if (inactivePoisonTooth && !loser)
            return false;
        return weapon != null && (loser || cardMustBeDiscarded(weapon));
    }

    public boolean defenseMustBeDiscarded(boolean loser) {
        return originalDefense != null && (loser || cardMustBeDiscarded(originalDefense));
    }

    public boolean revokePoisonTooth() {
        if (weapon != null && weapon.name().equals("Poison Tooth")) {
            inactivePoisonTooth = true;
            return true;
        }
        return false;
    }

    public void restorePoisonTooth() {
        inactivePoisonTooth = false;
    }

    public boolean isInactivePoisonTooth() {
        return inactivePoisonTooth;
    }

    public boolean addPortableSnooper() {
        if (originalDefense != null) return false;
        originalDefense = new TreacheryCard("Portable Snooper");
        defense = originalDefense;
        portableSnooperAdded = true;
        return true;
    }

    public void removePortableSnooper() {
        if (portableSnooperAdded) {
            originalDefense = null;
            defense = null;
        }
    }

    private int getNumForcesInReserve(Game game, Faction faction) {
        int numForcesInReserve = faction.getTotalReservesStrength();
        Territory territoryWithNoField = game.getTerritories().values().stream().filter(Territory::hasRicheseNoField).findFirst().orElse(null);
        if (faction instanceof RicheseFaction && territoryWithNoField != null && territoryWithNoField.getTerritoryName().contains(wholeTerritoryName))
            numForcesInReserve = Math.max(0, numForcesInReserve - territoryWithNoField.getRicheseNoField());
        return numForcesInReserve;
    }

    private int getNumStrongholdsOccupied(Game game, Faction faction) {
        List<Territory> strongholds = game.getTerritories().values().stream().filter(Territory::isStronghold).toList();
        int numStrongholdsOccupied = strongholds.stream().filter(t -> t.getForces().stream().anyMatch(f -> f.getFactionName().equals(faction.getName()) && f.getStrength() > 0) || (faction instanceof RicheseFaction) && t.hasRicheseNoField()).toList().size();
        if (faction instanceof BGFaction)
            numStrongholdsOccupied = strongholds.stream().filter(t -> t.getForces().stream().anyMatch(f -> f.getName().equals("BG") && f.getStrength() > 0)).toList().size();
        return numStrongholdsOccupied;
    }

    private List<LeaderSkillCard> getLeaderSkillsInFront(Faction faction) {
        return faction.getSkilledLeaders().stream()
                .filter(l -> !(faction instanceof HarkonnenFaction)
                        || l.getName().equals("Feyd Rautha")
                        || l.getName().equals("Beast Rabban")
                        || l.getName().equals("Piter de Vries")
                        || l.getName().equals("Cpt. Iakin Nefud")
                        || l.getName().equals("Umman Kudu"))
                .filter(l -> !l.isPulledBehindShield())
                .map(Leader::getSkillCard)
                .toList();
    }

    private void validatePlanInputs(Game game, Battle battle, Faction faction, Leader leader, TreacheryCard cheapHero, boolean kwisatzHaderach, TreacheryCard weapon, TreacheryCard defense, int spice) throws InvalidGameStateException {
        if (leader != null && cheapHero != null)
            throw new InvalidGameStateException(faction.getName() + " cannot play both a leader and " + cheapHero.name());
        if (leader != null && !faction.getLeaders().contains(leader))
            throw new InvalidGameStateException(faction.getName() + " does not have " + leader.getName());
        if (cheapHero != null && !faction.hasTreacheryCard(cheapHero.name()))
            throw new InvalidGameStateException(faction.getName() + " does not have " + cheapHero.name());
        List<String> battleTerritoryNames = battle.getTerritorySectors(game).stream().map(Territory::getTerritoryName).toList();
        if (leader == null && cheapHero == null && !faction.getLeaders().stream()
                .filter(l -> !l.getName().equals("Kwisatz Haderach") && (l.getBattleTerritoryName() == null || battleTerritoryNames.stream().anyMatch(n -> n.equals(l.getBattleTerritoryName()))))
                .toList().isEmpty())
            throw new InvalidGameStateException(faction.getName() + " must play a leader or a Cheap Hero");
        if (kwisatzHaderach) {
            if (leader == null && cheapHero == null)
                throw new InvalidGameStateException("A leader or Cheap Hero must be played to use the Kwisatz Haderach");
            if (!(faction instanceof AtreidesFaction))
                throw new InvalidGameStateException("Only Atreides can have the Kwisatz Haderach");
            if (!((AtreidesFaction) faction).isHasKH())
                throw new InvalidGameStateException("Only " + ((AtreidesFaction) faction).getForcesLost() + " Atreides forces killed in battle. 7 required for Kwisatz Haderach");
            if (faction.getLeader("Kwisatz Haderach").isEmpty())
                throw new InvalidGameStateException("Atreides has lost Kwisatz Haderach to the tanks");
        }

        int spiceFromAlly = 0;
        if (faction.hasAlly())
            spiceFromAlly = game.getFaction(faction.getAlly()).getBattleSupport();
        if (spice > (faction.getSpice() + spiceFromAlly))
            throw new InvalidGameStateException(faction.getName() + " does not have " + spice + " spice");
        boolean isNotPlanetologist = leader == null || leader.getSkillCard() == null || !leader.getSkillCard().name().equals("Planetologist");
        if (weapon != null) {
            if (!faction.hasTreacheryCard(weapon.name()))
                throw new InvalidGameStateException(faction.getName() + " does not have " + weapon.name());
            else if (weapon.name().equals("Chemistry") && (defense == null || !defense.type().startsWith("Defense")))
                throw new InvalidGameStateException("Chemistry can only be played as a weapon when playing another Defense");
            else if (weapon.name().equals("Harass and Withdraw")
                    && isNotPlanetologist
                    && (faction.getHomeworld().equals(wholeTerritoryName) || (faction instanceof EmperorFaction emperor && emperor.getSecondHomeworld().equals(wholeTerritoryName))))
                throw new InvalidGameStateException("Harass and Withdraw cannot be used on your Homeworld");
            else if (weapon.name().equals("Reinforcements")
                    && isNotPlanetologist
                    && numForcesInReserve < 3)
                throw new InvalidGameStateException("There must be at least 3 forces in reserves to use Reinformcements");
            else if (isNotPlanetologist && weapon.isGreenSpecialCard() && !weapon.name().equals("Harass and Withdraw") && !weapon.name().equals("Reinforcements"))
                throw new InvalidGameStateException(weapon.name() + " can only be played as a weapon if leader has Planetologist skill");
        }
        if (defense != null) {
            if (!faction.hasTreacheryCard(defense.name()))
                throw new InvalidGameStateException(faction.getName() + " does not have " + defense.name());
            else if (defense.name().equals("Weirding Way") && (weapon == null || !weapon.type().startsWith("Weapon")))
                throw new InvalidGameStateException("Weirding Way can only be played as a defense when playing another Weapon");
            else if (defense.name().equals("Harass and Withdraw") && (faction.getHomeworld().equals(wholeTerritoryName) || (faction instanceof EmperorFaction emperor && emperor.getSecondHomeworld().equals(wholeTerritoryName))))
                throw new InvalidGameStateException("Harass and Withdraw cannot be used on your Homeworld");
            else if (defense.name().equals("Reinforcements") && numForcesInReserve < 3)
                throw new InvalidGameStateException("There must be at least 3 forces in reserves to use Reinformcements");
        }
    }

    public boolean isCanCallTraitor() {
        return canCallTraitor;
    }

    public void setCanCallTraitor(boolean canCallTraitor) {
        this.canCallTraitor = canCallTraitor;
    }

    public boolean isDeclinedTraitor() {
        return declinedTraitor;
    }

    public void setDeclinedTraitor(boolean declinedTraitor) {
        this.declinedTraitor = declinedTraitor;
    }

    public boolean isWillCallTraitor() {
        return willCallTraitor;
    }

    public void setWillCallTraitor(boolean willCallTraitor) {
        this.willCallTraitor = willCallTraitor;
        canCallTraitor = false;
    }

    public boolean isHarkCanCallTraitor() {
        return harkCanCallTraitor;
    }

    public void setHarkCanCallTraitor(boolean harkCanCallTraitor) {
        this.harkCanCallTraitor = harkCanCallTraitor;
    }

    public boolean isHarkDeclinedTraitor() {
        return harkDeclinedTraitor;
    }

    public void setHarkDeclinedTraitor(boolean harkDeclinedTraitor) {
        this.harkDeclinedTraitor = harkDeclinedTraitor;
    }

    public boolean isHarkWillCallTraitor() {
        return harkWillCallTraitor;
    }

    public void setHarkWillCallTraitor(boolean harkWillCallTraitor) {
        this.harkWillCallTraitor = harkWillCallTraitor;
        harkCanCallTraitor = false;
    }
}
