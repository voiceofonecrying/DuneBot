package model.factions;

import constants.Emojis;
import enums.GameOption;
import enums.UpdateType;
import model.Game;
import model.HomeworldTerritory;
import model.Territory;
import model.TleilaxuTanks;

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
        Territory kaitain = game.getTerritories().addHomeworld(game, homeworld, name);
        kaitain.addForces(name, 15);
        Territory salusaSecundus = game.getTerritories().addHomeworld(game, secondHomeworld, name);
        salusaSecundus.addForces(name + "*", 5);
        game.getHomeworlds().put(name, homeworld);
        game.getHomeworlds().put(name + "*", secondHomeworld);
        this.occupiedIncome = 2;
        this.secundusHighThreshold = 2;
        this.secundusLowThreshold = 2;
        this.isSecundusHighThreshold = true;
    }

    @Override
    public void setGame(Game game) {
        super.setGame(game);
        getSecondHomeworldTerritory().setGame(game);
    }

    @Override
    public boolean hasStarredForces() {
        return true;
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
        removeForces(territoryName, forceName, amount, toTanks, isSpecial);
    }

    public String getSecondHomeworld() {
        return secondHomeworld;
    }

    @Override
    public int homeworldDialAdvantage(Game game, Territory territory) {
        String territoryName = territory.getTerritoryName();
        if (game.hasGameOption(GameOption.HOMEWORLDS)) {
            if (homeworld.equals(territoryName))
                return isHighThreshold() ? 2 : 3;
            else if (territoryName.equals("Salusa Secundus"))
                return isSecundusHighThreshold() ? 3 : 2;
        }
        return 0;
    }

    public boolean isSecundusHighThreshold() {
        if (!game.hasGameOption(GameOption.HOMEWORLDS)) return true;
        return isSecundusHighThreshold;
    }

    private HomeworldTerritory getSecondHomeworldTerritory() {
        return (HomeworldTerritory) game.getTerritory(secondHomeworld);
    }

    @Override
    public void resetOccupation() {
        super.resetOccupation();
        getSecondHomeworldTerritory().resetOccupation();
    }

    public boolean isSecundusOccupied() {
        if (!game.hasGameOption(GameOption.HOMEWORLDS)) return false;
        return getSecondHomeworldTerritory().getOccupierName() != null;
    }

    @Override
    public void checkForHighThreshold() {
        if (!game.hasGameOption(GameOption.HOMEWORLDS)) return;
        if (getHomeworldTerritory().getOccupyingFaction() != null)
            isHighThreshold = false;
        else if (!isHighThreshold && super.getReservesStrength() + getSpecialStrengthOnKaitain() > lowThreshold) {
            game.getTurnSummary().publish(homeworld + " has flipped to High Threshold");
            isHighThreshold = true;
        }
        if (getSecondHomeworldTerritory().getOccupyingFaction() != null)
            isSecundusHighThreshold = false;
        else if (!isSecundusHighThreshold && getSpecialStrengthOnSalusaSecundus() > secundusLowThreshold) {
            game.getTurnSummary().publish(secondHomeworld + " has flipped to High Threshold");
            isSecundusHighThreshold = true;
        }
    }

    @Override
    public void checkForLowThreshold() {
        if (!game.hasGameOption(GameOption.HOMEWORLDS)) return;
        if (getHomeworldTerritory().getOccupyingFaction() != null)
            isHighThreshold = false;
        else if (isHighThreshold && super.getReservesStrength() + getSpecialStrengthOnKaitain() < highThreshold) {
            game.getTurnSummary().publish(homeworld + " has flipped to Low Threshold.");
            isHighThreshold = false;
        }
        if (getSecondHomeworldTerritory().getOccupyingFaction() != null)
            isSecundusHighThreshold = false;
        else if (isSecundusHighThreshold && getSpecialStrengthOnSalusaSecundus() < secundusHighThreshold) {
            game.getTurnSummary().publish("Salusa Secundus has flipped to Low Threshold.");
            isSecundusHighThreshold = false;
        }
    }

    @Override
    public void removeReserves(int amount) {
        int kaitainAmount = super.getReservesStrength();
        int secundusAmount = getRegularStrengthOnSalusaSecundus();
        if (amount > kaitainAmount + secundusAmount) {
            // Produce exception as would also result from Faction::removeReserves
            super.removeReserves(amount);
        } else {
            super.removeReserves(Math.min(amount, kaitainAmount));
            if (amount > kaitainAmount)
                removeRegularForcesFromSalusaSecundus(amount - kaitainAmount);
        }
        setUpdated(UpdateType.MISC_BACK_OF_SHIELD);
    }

    private int getSpecialStrengthOnKaitain() {
        return game.getTerritory(homeworld).getForceStrength(name + "*");
    }

    private void removeSpecialForcesFromKaitain(int amount) {
        game.getTerritory(homeworld).removeForces(name + "*", amount);
    }

    private int getSpecialStrengthOnSalusaSecundus() {
        return game.getTerritory(secondHomeworld).getForceStrength(name + "*");
    }

    private int getRegularStrengthOnSalusaSecundus() {
        return game.getTerritory(secondHomeworld).getForceStrength(name);
    }

    private void removeRegularForcesFromSalusaSecundus(int amount) {
        game.getTerritory(secondHomeworld).removeForces(name, amount);
    }

    private void removeSpecialForcesFromSalusaSecundus(int amount) {
        game.getTerritory(secondHomeworld).removeForces(name + "*", amount);
    }

    @Override
    public void addSpecialReserves(int amount) {
        Territory territory = getGame().getTerritory(getSecondHomeworld());
        territory.addForces(name + "*", amount);
        setUpdated(UpdateType.MISC_BACK_OF_SHIELD);
    }

    @Override
    public void removeSpecialReserves(int amount) {
        int secundusAmount = getGame().getTerritory(getSecondHomeworld()).getForceStrength(name + "*");
        int kaitainAmount = getGame().getTerritory(getHomeworld()).getForceStrength(name + "*");
        if (amount > secundusAmount + kaitainAmount) {
            // Produce exception as would also result from Faction::removeReserves
            super.removeSpecialReserves(amount);
        } else {
            removeSpecialForcesFromSalusaSecundus(Math.min(amount, secundusAmount));
            if (amount > secundusAmount)
                removeSpecialForcesFromKaitain(amount - secundusAmount);
        }
        setUpdated(UpdateType.MISC_BACK_OF_SHIELD);
    }

    @Override
    public int getReservesStrength() {
        return super.getReservesStrength() + getRegularStrengthOnSalusaSecundus();
    }

    @Override
    public int getSpecialReservesStrength() {
        return getSpecialStrengthOnSalusaSecundus() + getSpecialStrengthOnKaitain();
    }

    public void kaitainHighDiscard(String cardName) {
        getGame().getTreacheryDiscard().add(removeTreacheryCard(cardName));
        getLedger().publish(cardName + " discarded");
        subtractSpice(2, " paid to discard");
        getGame().getTurnSummary().publish(MessageFormat.format(
                "{0} paid 2 {1} to discard {2} (Kaitain High Threshold ability)",
                Emojis.EMPEROR, Emojis.SPICE, cardName.trim()
        ));
    }

    @Override
    public int countFreeStarredRevival() {
        int numStarRevived = 0;
        TleilaxuTanks tanks = game.getTleilaxuTanks();
        if (tanks.getForceStrength(name + "*") > 0) {
            if (!game.hasGameOption(GameOption.HOMEWORLDS) || isSecundusHighThreshold()) {
                numStarRevived++;
                starRevived = true;
            }
        }
        return numStarRevived;
    }

    @Override
    protected int getRevivableForces() {
        boolean starsInTanks = game.getTleilaxuTanks().getForceStrength(name + "*") > 0;
        boolean emperorCanPayForOneSardaukar = !isStarRevived() && starsInTanks;
        return game.getTleilaxuTanks().getForceStrength(name) +
                (emperorCanPayForOneSardaukar ? 1 : 0);
    }

    @Override
    protected String paidRevivalMessage() {
        boolean starsInTanks = game.getTleilaxuTanks().getForceStrength(name + "*") > 0;
        boolean emperorCanPayForOneSardaukar = !isStarRevived() && starsInTanks;
        String sardaukarString = emperorCanPayForOneSardaukar ? " including 1 " + Emojis.EMPEROR_SARDAUKAR : "";
        return "Would you like to purchase additional revivals" + sardaukarString + "? " + player;
    }

    @Override
    public void publishNoRevivableForcesMessage() {
        boolean starsInTanks = game.getTleilaxuTanks().getForceStrength(name + "*") > 0;
        if (starsInTanks)
            game.getTurnSummary().publish(emoji + " has no revivable forces in the tanks");
        else
            game.getTurnSummary().publish(emoji + " has no forces in the tanks");
    }

    @Override
    public void resetAllySpiceSupportAfterShipping(Game game) {
        // Emperor ally support remains active through Battle Phase
    }

    @Override
    public int getBattleSupport() {
        return getSpiceForAlly();
    }

    @Override
    public String getSpiceSupportPhasesString() {
        return " for bidding, shipping, and battles!";
    }
}
