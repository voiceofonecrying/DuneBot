package model;

import java.text.MessageFormat;

public class BattlePlan {
    private final boolean aggressor;
    private final Leader leader;
    private final TreacheryCard cheapHero;
    private final boolean kwisatzHaderach;
    private final TreacheryCard weapon;
    private TreacheryCard defense;
    private final int wholeNumberDial;
    private final boolean plusHalfDial;
    private final int spice;
    private final int troopsNotDialed;
    private final int ecazTroopsForAlly;
    private final int homeworldDialAdvantage;
    private TreacheryCard opponentWeapon;
    private TreacheryCard opponentDefense;
    private Leader opponentLeader;
    private boolean inactivePoisonTooth;
    private boolean portableSnooperAdded;
    private boolean stoneBurnerNoKill;
    private boolean opponentStoneBurnerNoKill;

    public BattlePlan(boolean aggressor, Leader leader, TreacheryCard cheapHero, boolean kwisatzHaderach, TreacheryCard weapon, TreacheryCard defense, int wholeNumberDial, boolean plusHalfDial, int spice, int troopsNotDialed, int ecazTroopsForAlly, int homeworldDialAdvantage) {
        this.aggressor = aggressor;
        this.leader = leader;
        this.cheapHero = cheapHero;
        this.kwisatzHaderach = kwisatzHaderach;
        this.weapon = weapon;
        this.defense = defense;
        this.wholeNumberDial = wholeNumberDial;
        this.plusHalfDial = plusHalfDial;
        this.spice = spice;
        this.troopsNotDialed = troopsNotDialed;
        this.ecazTroopsForAlly = ecazTroopsForAlly;
        this.homeworldDialAdvantage = homeworldDialAdvantage;
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

    public boolean getPlusHalfDial() {
        return plusHalfDial;
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
        return defense;
    }

    public boolean isStoneBurnerNoKill() {
        return stoneBurnerNoKill;
    }

    public int getLeaderValue() {
        if (leader != null && leader.name().equals("Zoal"))
            return opponentLeader == null ? 0 : opponentLeader.value();
        return leader == null ? 0 : leader.value();
    }

    public int getLeaderContribution() {
        if (artilleryStrike() || !isLeaderAlive())
            return 0;
        return getLeaderValue() + (kwisatzHaderach ? 2 : 0);
    }

    public String getLeaderString() {
        String khString = kwisatzHaderach ? " + KH (2)" : "";
        String leaderString = "-";
        if (leader != null) leaderString = MessageFormat.format("{0} ({1}){2}",
                leader.name(), leader.name().equals("Zoal") ? "X" : leader.value(), khString);
        else if (cheapHero != null) leaderString = cheapHero.name() + "(0)" + khString;
        return "Leader: " + leaderString;
    }

    public int getDoubleBattleStrength() {
        if (stoneBurnerForTroops())
            return 2 * troopsNotDialed + 2 * homeworldDialAdvantage;
        int doubleBattleStrength = 2 * wholeNumberDial + 2 * homeworldDialAdvantage;
        if (plusHalfDial) doubleBattleStrength++;
        doubleBattleStrength += 2 * getLeaderContribution();
        doubleBattleStrength += 2 * Math.ceilDiv(ecazTroopsForAlly, 2);
        return doubleBattleStrength;
    }

    public String getTotalStrengthString() {
        int wholeNumber = getDoubleBattleStrength() / 2;
        return MessageFormat.format("{0}{1}", wholeNumber, plusHalfDial ? ".5" : "");
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
        if (weapon != null && weapon.name().equals("Stone Burner")) {
            stoneBurnerNoKill = true;
            return true;
        }
        return false;
    }

    public void restoreKillWithStoneBurner() {
        stoneBurnerNoKill = false;
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
        } else if (opponentWeapon != null && !opponentWeapon.name().equals("Stone Burner")) {
            if (defense == null)
                return false;
            else if (opponentWeapon.name().equals("Poison Blade") && !defense.name().equals("Shield Snooper"))
                return false;
            else if ((opponentWeapon.type().equals("Weapon - Poison") || opponentWeapon.name().equals("Chemistry"))
                    && !(defense.servesAsSnooper() || defense.name().equals("Chemistry")))
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
        return leader == null ? "" : leader.name();
    }

    public String getWeaponString() {
        return "Weapon: " + (weapon == null ? "-"
                : weapon.name()) + (inactivePoisonTooth ? " (not used)" : stoneBurnerNoKill ? " (leaders not killed)" : "");
    }

    public String getDefenseString() {
        return "Defense: " + (defense == null ? "-" : defense.name());
    }

    public String getDialString() {
        String dialString = "Dial: " + wholeNumberDial + (plusHalfDial ? ".5" : "");
        if (homeworldDialAdvantage != 0)
            dialString += "\nHomeworld advantage: " + homeworldDialAdvantage;
        return dialString;
    }

    public String getSpiceString() {
        return "Spice: " + spice;
    }

    public String getPlanMessage() {
        return getLeaderString() + "\n"
                + getWeaponString() + "\n"
                + getDefenseString() + "\n"
                + getDialString() + "\n"
                + getSpiceString();
    }

    public void revealOpponentBattlePlan(BattlePlan opponentPlan) {
        this.opponentLeader = opponentPlan.getLeader();
        this.opponentWeapon = opponentPlan.inactivePoisonTooth ? null : opponentPlan.getWeapon();
        if (opponentWeapon != null && opponentWeapon.name().equals("Mirror Weapon"))
            this.opponentWeapon = weapon;
        this.opponentDefense = opponentPlan.getDefense();
        this.opponentStoneBurnerNoKill = opponentPlan.isStoneBurnerNoKill();
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
            if (card.name().equals("Harass and Withdraw")) return true;
            if (card.name().equals("Reinforcements")) return true;
            return card.type().equals("Worthless Card");
        }
        return false;
    }

    public boolean weaponMustBeDiscarded(boolean loser) {
        return weapon != null & (loser || cardMustBeDiscarded(weapon));
    }

    public boolean defenseMustBeDiscarded(boolean loser) {
        return defense != null && (loser || cardMustBeDiscarded(defense));
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
        if (defense != null) return false;
        defense = new TreacheryCard("Portable Snooper");
        portableSnooperAdded = true;
        return true;
    }

    public void removePortableSnooper() {
        if (portableSnooperAdded) defense = null;
    }
}
