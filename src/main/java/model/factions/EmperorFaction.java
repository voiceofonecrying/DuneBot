package model.factions;

import constants.Emojis;
import enums.GameOption;
import enums.UpdateType;
import model.Force;
import model.Game;
import model.Territory;

import java.io.IOException;
import java.text.MessageFormat;

public class EmperorFaction extends Faction {
    private final int secundusHighThreshold;
    private final int secundusLowThreshold;
    private final String secondHomeworld;
    private boolean isSecundusHighThreshold;

    public EmperorFaction(String player, String userName, Game game) throws IOException {
        super("Emperor", player, userName, game);

        setSpice(10);
        this.freeRevival = 1;
        this.emoji = Emojis.EMPEROR;
        this.highThreshold = 5;
        this.lowThreshold = 4;
        this.homeworld = "Kaitain";
        this.secondHomeworld = "Salusa Secundus";
        game.getTerritories().put("Kaitain", new Territory("Kaitain", -1, false, false, false));
        game.getTerritory("Kaitain").addForce(new Force("Emperor", 15));
        game.getTerritories().put("Salusa Secundus", new Territory("Salusa Secundus", -1, false, false, false));
        game.getTerritory("Salusa Secundus").addForce(new Force("Emperor*", 5));
        game.getHomeworlds().put(getName(), homeworld);
        game.getHomeworlds().put(getName() + "*", secondHomeworld);
        this.occupiedIncome = 2;
        this.secundusHighThreshold = 2;
        this.secundusLowThreshold = 2;
        this.isSecundusHighThreshold = true;
    }

    /**
     * Adds forces from a Territory to the reserves or tanks
     *
     * @param territoryName The name of the Territory.
     * @param amount        The amount of the force.
     * @param isSpecial     Whether the force is special or not.
     * @param toTanks       Whether the force is going to the tanks or not.
     */
    @Override
    public void removeForces(String territoryName, int amount, boolean isSpecial, boolean toTanks) {
        String forceName = getName() + (isSpecial ? "*" : "");
        removeForces(territoryName, forceName, amount, toTanks, isSpecial, forceName);
    }

    public String getSecondHomeworld() {
        return secondHomeworld;
    }

    public boolean isSecundusHighThreshold() {
        return isSecundusHighThreshold;
    }

    @Override
    public void checkForHighThreshold() {
        if (!game.hasGameOption(GameOption.HOMEWORLDS)) return;
        if (!isHighThreshold && getReserves().getStrength() + getSpecialReservesOnKaitain().getStrength() > lowThreshold) {
            game.getTurnSummary().publish(homeworld + " has flipped to High Threshold");
            isHighThreshold = true;
        }
        if (!isSecundusHighThreshold && getSpecialReserves().getStrength() > secundusLowThreshold) {
            game.getTurnSummary().publish(secondHomeworld + " has flipped to High Threshold");
            isSecundusHighThreshold = true;
        }
    }

    @Override
    public void checkForLowThreshold() {
        if (!game.hasGameOption(GameOption.HOMEWORLDS)) return;
        if (isHighThreshold && getReserves().getStrength() + getSpecialReservesOnKaitain().getStrength() < highThreshold) {
            game.getTurnSummary().publish(homeworld + " has flipped to Low Threshold.");
            isHighThreshold = false;
        }
        if (isSecundusHighThreshold && getSpecialReserves().getStrength() < secundusHighThreshold) {
            game.getTurnSummary().publish("Salusa Secundus has flipped to Low Threshold.");
            isSecundusHighThreshold = false;
        }
    }

    @Override
    public void removeReserves(int amount) {
        int kaitainAmount = getReserves().getStrength();
        int secundusAmount = getRegularReservesOnSalusaSecundus().getStrength();
        if (amount > kaitainAmount + secundusAmount) {
            // Produce exception as would also result from Faction::removeReserves
            super.removeReserves(amount);
        } else {
            getReserves().removeStrength(Math.min(amount, kaitainAmount));
            if (amount > kaitainAmount)
                getRegularReservesOnSalusaSecundus().removeStrength(amount - kaitainAmount);
        }
        setUpdated(UpdateType.MISC_BACK_OF_SHIELD);
    }

    @Override
    public Force getSpecialReserves() {
        if (this.specialReserves != null) return this.specialReserves;
        return getGame().getTerritory(getSecondHomeworld()).getForce("Emperor*");
    }

    public Force getSpecialReservesOnKaitain() {
        return getGame().getTerritory(getHomeworld()).getForce("Emperor*");
    }

    public Force getRegularReservesOnSalusaSecundus() {
        return getGame().getTerritory(getSecondHomeworld()).getForce("Emperor");
    }

    @Override
    public void addSpecialReserves(int amount) {
        if (specialReserves != null) {
            getSpecialReserves().addStrength(amount);
        } else {
            Territory territory = getGame().getTerritory(getSecondHomeworld());
            territory.setForceStrength("Emperor*", territory.getForce("Emperor*").getStrength() + amount);
        }
        setUpdated(UpdateType.MISC_BACK_OF_SHIELD);
    }

    @Override
    public void removeSpecialReserves(int amount) {
        int secundusAmount = getSpecialReserves().getStrength();
        int kaitainAmount = getSpecialReservesOnKaitain().getStrength();
        if (amount > secundusAmount + kaitainAmount) {
            // Produce exception as would also result from Faction::removeReserves
            super.removeSpecialReserves(amount);
        } else {
            getSpecialReserves().removeStrength(Math.min(amount, secundusAmount));
            if (amount > secundusAmount)
                getSpecialReservesOnKaitain().removeStrength(amount - secundusAmount);
        }
        setUpdated(UpdateType.MISC_BACK_OF_SHIELD);
    }

    @Override
    public int getReservesStrength() {
        return getReserves().getStrength() + getRegularReservesOnSalusaSecundus().getStrength();
    }

    @Override
    public int getSpecialReservesStrength() {
        return getSpecialReserves().getStrength() + getSpecialReservesOnKaitain().getStrength();
    }

    public void kaitainHighDiscard(String cardName) {
        getGame().getTreacheryDiscard().add(removeTreacheryCard(cardName));
        getLedger().publish(cardName + " discarded");
        subtractSpice(2);
        spiceMessage(2, " paid to discard", false);
        getGame().getTurnSummary().publish(MessageFormat.format(
                "{0} paid 2 {1} to discard {2} (Kaitain High Threshold ability)",
                Emojis.EMPEROR, Emojis.SPICE, cardName.trim()
        ));
    }
}
