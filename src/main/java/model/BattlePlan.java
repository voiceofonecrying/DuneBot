package model;

public class BattlePlan {
    Leader leader;
    TreacheryCard cheapHero;
    boolean kwisatzHaderach;
    int wholeNumberDial;
    boolean plusHalfDial;
    int spice;
    TreacheryCard weapon;
    TreacheryCard defense;

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

    public String getPlanMessage() {
        String khString = (kwisatzHaderach ? " + KH (2)" : "");
        String leaderString = "-";
        if (leader != null) leaderString = leader.name() + " (" + leader.value() + ")" + khString;
        else if (cheapHero != null) leaderString = cheapHero.name() + "(0)" + khString;

        return "Leader: " + leaderString + "\n"
                + "Weapon: " + (weapon == null ? "-" : weapon.name()) + "\n"
                + "Defense: " + (defense == null ? "-" : defense.name()) + "\n"
                + "Dial: " + wholeNumberDial + (plusHalfDial ? ".5" : "") + "\n"
                + "Spice: " + spice;
    }
}
