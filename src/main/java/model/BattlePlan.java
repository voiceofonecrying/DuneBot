package model;

import constants.Emojis;

import java.text.MessageFormat;
import java.util.List;

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
    private final int spice;
    private int regularDialed;
    private int specialDialed;
    private int troopsNotDialed;
    private final int ecazTroopsForAlly;
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
    private boolean stoneBurnerNoKill;
    private boolean opponentStoneBurnerNoKill;

    public BattlePlan(boolean aggressor, Leader leader, TreacheryCard cheapHero, boolean kwisatzHaderach, TreacheryCard weapon, TreacheryCard defense, int wholeNumberDial, boolean plusHalfDial, int spice, int troopsNotDialed, int ecazTroopsForAlly, List<LeaderSkillCard> leaderSkillsInFront, boolean carthagStrongholdCard, int homeworldDialAdvantage, int numStrongholdsOccupied, int numForcesInReserve) {
        this.aggressor = aggressor;
        this.leader = leader;
        this.cheapHero = cheapHero;
        this.kwisatzHaderach = kwisatzHaderach;
        this.weapon = weapon;
        this.defense = defense;
        this.originalDefense = defense;
        this.wholeNumberDial = wholeNumberDial;
        this.plusHalfDial = plusHalfDial;
        this.spice = spice;
        this.troopsNotDialed = troopsNotDialed;
        this.ecazTroopsForAlly = ecazTroopsForAlly;
        this.leaderSkillsInFront = leaderSkillsInFront;
        this.carthagStrongholdCard = carthagStrongholdCard;
        this.homeworldDialAdvantage = homeworldDialAdvantage;
        this.numStrongholdsOccupied = numStrongholdsOccupied;
        this.numForcesInReserve = numForcesInReserve;
        this.spiceBankerSupport = 0;
        this.juiceOfSapho = false;
        this.stoneBurnerNoKill = false;
    }

    public Leader getLeader() {
        return leader;
    }

    public TreacheryCard getCheapHero() {
        return cheapHero;
    }

    public int getWholeNumberDial() {
        return wholeNumberDial;
    }

    public int getRegularDialed() {
        return regularDialed;
    }

    public void setRegularDialed(int regularDialed) {
        this.regularDialed = regularDialed;
    }

    public int getSpecialDialed() {
        return specialDialed;
    }

    public void setSpecialDialed(int specialDialed) {
        this.specialDialed = specialDialed;
    }

    public void setForcesDialed(int regularDialed, int specialDialed, int notDialed) {
        this.regularDialed = regularDialed;
        this.specialDialed = specialDialed;
        this.troopsNotDialed = notDialed;
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

    private boolean isProjectileDefense() {
        return defense != null && (defense.servesAsShield() || defense.name().equals("Weirding Way"));
    }

    private boolean hasWorthlessCard() {
        return weapon != null && weapon.type().equals("Worthless Card") || originalDefense != null && originalDefense.type().equals("Worthless Card");
    }

    public String getLeaderString(boolean revealLeaderSkills) {
        String khString = kwisatzHaderach ? " + KH (2)" : "";
        String leaderString = "-";
        if (leader != null) leaderString = MessageFormat.format("{0} ({1}){2}",
                leader.getName(), leader.getName().equals("Zoal") ? "X" : leader.getValue(), khString);
        else if (cheapHero != null) leaderString = cheapHero.name() + "(0)" + khString;

        if (revealLeaderSkills) {
            if (isSkillBehindAndLeaderAlive("Killer Medic") && isPoisonDefense())
                leaderString += "\n  +3 for Killer Medic";
            if (isSkillInFront("Killer Medic") && isPoisonDefense())
                leaderString += "\n  +1 for Killer Medic";
            if (isSkillBehindAndLeaderAlive("Master of Assassins") && isPoisonWeapon())
                leaderString += "\n  +3 for Master of Assassins";
            if (isSkillInFront("Master of Assassins") && isPoisonWeapon())
                leaderString += "\n  +1 for Master of Assassins";
            if (isSkillBehindAndLeaderAlive("Mentat"))
                leaderString += "\n  +2 for Mentat";
            if (isSkillBehindAndLeaderAlive("Planetologist") && isSpecialForWeapon())
                leaderString += "\n  +2 for Planetologist";
            if (isSkillBehindAndLeaderAlive("Prana Bindu Adept") && isProjectileDefense())
                leaderString += "\n  +3 for Prana Bindu Adept";
            if (isSkillInFront("Prana Bindu Adept") && isProjectileDefense())
                leaderString += "\n  +1 for Prana Bindu Adept";
            if (isSkillBehindAndLeaderAlive("Swordmaster of Ginaz") && isProjectileWeapon())
                leaderString += "\n  +3 for Swordmaster of Ginaz";
            if (isSkillInFront("Swordmaster of Ginaz") && isProjectileWeapon())
                leaderString += "\n  +1 for Swordmaster of Ginaz";
            if (isSkillBehindAndLeaderAlive("Spice Banker"))
                leaderString += "\n  +" + spiceBankerSupport + " for Spice Banker";
            if (isSkillBehindAndLeaderAlive("Warmaster") && hasWorthlessCard())
                leaderString += "\n  +3 for Warmaster";
            if (isSkillInFront("Warmaster") && hasWorthlessCard())
                leaderString += "\n  +1 for Warmaster";
            if (opponentHasBureaucrat && numStrongholdsOccupied > 0)
                leaderString += "\n -" + numStrongholdsOccupied + " for opponent Bureaucrat";
        }
        return "Leader: " + leaderString;
    }

    public int getDoubleBattleStrength() {
        int bonuses = homeworldDialAdvantage + spiceBankerSupport;
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
            return 2 * troopsNotDialed + 2 * bonuses;
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
        this.carthagStrongholdCard = true;
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

    public boolean dontKillWithStoneBurner() {
        if (weapon != null) {
            if (weapon.name().equals("Stone Burner") || opponentWeapon != null && opponentWeapon.name().equals("Stone Burner") && weapon.name().equals("Mirror Weapon")) {
                stoneBurnerNoKill = true;
                return true;
            }
        }
        return false;
    }

    public void restoreKillWithStoneBurner() {
        stoneBurnerNoKill = false;
    }

    private boolean carthagStrongholdPoisonDefense() {
        TreacheryCard effectiveWeapon = weapon;
        if (weapon != null && weapon.name().equals("Mirror Weapon")) effectiveWeapon = opponentWeapon;
        boolean nonPoisonWeapon = effectiveWeapon == null || (!effectiveWeapon.type().equals("Weapon - Poison") && !effectiveWeapon.name().equals("Chemistry")
                && !effectiveWeapon.name().equals("Poison Tooth") && !effectiveWeapon.name().equals("Poison Blade"));
        return carthagStrongholdCard && nonPoisonWeapon && defense != null && defense.servesAsShield();
    }

    public boolean isLeaderAlive() {
        if (isLasgunShieldExplosion()) {
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
        return "Weapon: " + (weapon == null ? "-"
                : weapon.name()) + (inactivePoisonTooth ? " (not used)" : stoneBurnerNoKill ? " (leaders not killed)" : "");
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
                opponentWeapon = weapon;
        }
        opponentDefense = opponentPlan.getDefense();
        if (isSkillInFront("Diplomat") && (defense == null && weapon.type().equals("Worthless Card") || defense.type().equals("Worthless Card")))
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
        return weapon != null & (loser || cardMustBeDiscarded(weapon));
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

    public String checkAuditor(String opponentEmoji) {
        String message = "";
        if (getLeader() != null && getLeader().getName().equals("Auditor")) {
            int numCards = isLeaderAlive() ? 2 : 1;
            message = MessageFormat.format(
                    "{0} may audit {1} {2} cards not used in the battle unless {3} cancels the audit for {1} {4}\n",
                    Emojis.CHOAM, numCards, Emojis.TREACHERY, opponentEmoji, Emojis.SPICE);
            // When automatic resolution is supported, give opponent buttons for their choice here.
        }
        return message;
    }
}
