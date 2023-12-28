package model;

import java.text.MessageFormat;

public class BattlePlan {
    private final Leader leader;
    private final TreacheryCard cheapHero;
    private final boolean kwisatzHaderach;
    private final int wholeNumberDial;
    private final boolean plusHalfDial;
    private final int spice;
    private final TreacheryCard weapon;
    private final TreacheryCard defense;
    private TreacheryCard opponentWeapon;
    private Leader opponentLeader;

    public BattlePlan(Leader leader, TreacheryCard cheapHero, boolean kwisatzHaderach, int wholeNumberDial, boolean plusHalfDial, int spice, TreacheryCard weapon, TreacheryCard defense) {
        this.leader = leader;
        this.cheapHero = cheapHero;
        this.kwisatzHaderach = kwisatzHaderach;
        this.wholeNumberDial = wholeNumberDial;
        this.plusHalfDial = plusHalfDial;
        this.spice = spice;
        this.weapon = weapon;
        this.defense = defense;
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

    public int getSpice() {
        return spice;
    }

    public TreacheryCard getWeapon() {
        return weapon;
    }

    public TreacheryCard getDefense() {
        return defense;
    }

    public int getLeaderStrength() {
        if (leader != null && leader.name().equals("Zoal"))
            return opponentLeader == null ? 0 : opponentLeader.value();
        return leader == null ? 0 : leader.value();
    }

    public int getLeaderStrengthWithKH() {
        return getLeaderStrength() + (kwisatzHaderach ? 2 : 0);
    }

    public String getLeaderString() {
        String khString = kwisatzHaderach ? " + KH (2)" : "";
        String leaderString = "-";
        if (leader != null) leaderString = MessageFormat.format("{0} ({1}){2}",
                leader.name(), leader.name().equals("Zoal") ? "X" : leader.value(), khString);
        else if (cheapHero != null) leaderString = cheapHero.name() + "(0)" + khString;
        return "Leader: " + leaderString;
    }

    public boolean isLeaderAlive() {
        if (opponentWeapon != null) {
            if (defense == null)
                return false;
            else if ((opponentWeapon.type().equals("Weapon - Poison") || opponentWeapon.name().equals("Chemistry"))
                    && !(defense.type().equals("Defense - Poison") || defense.name().equals("Chemistry")))
                return false;
            else if ((opponentWeapon.type().equals("Weapon - Projectile") || opponentWeapon.name().equals("Weirding Way"))
                    && !(defense.type().equals("Defense - Projectile") || defense.name().equals("Weirding Way")))
                return false;
        }
        return true;
    }

    public int combatWater() {
        return isLeaderAlive() ? 0 : getLeaderStrength();
    }

    public String getKilledLeaderString() {
        return leader == null ? "" : leader.name();
    }

    public String getWeaponString() {
        return "Weapon: " + (weapon == null ? "-" : weapon.name());
    }

    public String getDefenseString() {
        return "Defense: " + (defense == null ? "-" : defense.name());
    }

    public String getDialString() {
        return "Dial: " + wholeNumberDial + (plusHalfDial ? ".5" : "");
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

    public void setOpponentWeaponAndLeader(TreacheryCard opponentWeapon, Leader opponentLeader) {
        this.opponentWeapon = opponentWeapon;
        this.opponentLeader = opponentLeader;
    }
}
