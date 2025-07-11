package model.factions;

import constants.Emojis;
import enums.GameOption;
import enums.UpdateType;
import exceptions.InvalidGameStateException;
import model.*;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class EmperorFaction extends Faction {
    private final int secundusHighThreshold;
    private final int secundusLowThreshold;
    private final String secondHomeworld;
    private boolean isSecundusHighThreshold;

    public EmperorFaction(String player, String userName) throws IOException {
        super("Emperor", player, userName);

        this.spice = 10;
        this.freeRevival = 1;
        this.emoji = Emojis.EMPEROR;
        this.highThreshold = 5;
        this.lowThreshold = 4;
        this.homeworld = "Kaitain";
        this.secondHomeworld = "Salusa Secundus";
        this.occupiedIncome = 2;
        this.secundusHighThreshold = 2;
        this.secundusLowThreshold = 2;
        this.isSecundusHighThreshold = true;
    }

    @Override
    public void joinGame(@NotNull Game game) {
        super.joinGame(game);
        Territory kaitain = game.getTerritories().addHomeworld(game, homeworld, name);
        kaitain.addForces(name, 15);
        Territory salusaSecundus = game.getTerritories().addHomeworld(game, secondHomeworld, name);
        salusaSecundus.addForces(name + "*", 5);
        game.getHomeworlds().put(name, homeworld);
        game.getHomeworlds().put(name + "*", secondHomeworld);
    }

    @Override
    public void setGame(Game game) {
        super.setGame(game);
        getSecondHomeworldTerritory().setGame(game);
    }

    @Override
    public String forcesStringWithZeroes(int numForces, int numSpecialForces) {
        return numForces + " " + Emojis.getForceEmoji(name) + " " + numSpecialForces + " " + Emojis.getForceEmoji(name + "*");
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
        game.getTerritory(homeworld).removeForces(game, name + "*", amount);
    }

    private int getSpecialStrengthOnSalusaSecundus() {
        return game.getTerritory(secondHomeworld).getForceStrength(name + "*");
    }

    private int getRegularStrengthOnSalusaSecundus() {
        return game.getTerritory(secondHomeworld).getForceStrength(name);
    }

    private void removeRegularForcesFromSalusaSecundus(int amount) {
        game.getTerritory(secondHomeworld).removeForces(game, name, amount);
    }

    private void removeSpecialForcesFromSalusaSecundus(int amount) {
        game.getTerritory(secondHomeworld).removeForces(game, name + "*", amount);
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

    public void presentKaitainHighThresholdChoices() {
        if (game.hasGameOption(GameOption.HOMEWORLDS)) {
            if (isHighThreshold) {
                List<DuneChoice> choices = treacheryHand.stream().map(card -> new DuneChoice("emperor-discard-" + card.name(), card.name())).collect(Collectors.toList());
                if (choices.isEmpty()) {
                    chat.publish("You have no " + Emojis.TREACHERY + " cards for Kaitain High Threshold.");
                } else {
                    choices.add(new DuneChoice("secondary", "emperor-finished-discarding", "Done"));
                    chat.publish("Would you like to pay 2 " + Emojis.SPICE + " to discard a " + Emojis.TREACHERY + " card with Kaitain High Threshold? " + player, choices);
                }
            } else
                chat.publish("You are at Low Threshold and cannot use Kaitain High Threshold ability.");
        }
    }

    public void kaitainHighDiscard(String cardName) {
        discard(cardName, "and pays 2 " + Emojis.SPICE + " with Kaitain High Threshold ability");
        subtractSpice(2, " paid to discard " + cardName);
        chat.reply("You discarded " + cardName + ".");
        presentKaitainHighThresholdChoices();
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
        boolean emperorCanPayForOneSardaukar = isStarNotRevived() && starsInTanks;
        return game.getTleilaxuTanks().getForceStrength(name) +
                (emperorCanPayForOneSardaukar ? 1 : 0);
    }

    @Override
    protected String paidRevivalMessage() {
        boolean starsInTanks = game.getTleilaxuTanks().getForceStrength(name + "*") > 0;
        boolean emperorCanPayForOneSardaukar = isStarNotRevived() && starsInTanks;
        String sardaukarString = emperorCanPayForOneSardaukar ? " including 1 " + Emojis.EMPEROR_SARDAUKAR : "";
        return "Would you like to purchase additional revivals" + sardaukarString + "? " + player;
    }

    @Override
    public String getNoRevivableForcesMessage() {
        boolean starsInTanks = game.getTleilaxuTanks().getForceStrength(name + "*") > 0;
        if (starsInTanks)
            return emoji + " has no revivable forces in the tanks";
        else
            return emoji + " has no forces in the tanks";
    }

    /**
     * Just revive the leader. Calling function handles payment and messaging.
     *
     * @param isPaid indicates if faction must pay spice for the revival
     * @param numForces the number of forces to revive
     */
    @Override
    public void reviveForces(boolean isPaid, int numForces) {
        paidRevivalTBD = false;
        if (numForces == 0)
            game.getTurnSummary().publish(emoji + " does not purchase additional revivals.");
        else if (isStarNotRevived() && game.getTleilaxuTanks().getForceStrength("Emperor*") > 0) {
            game.reviveForces(this, isPaid, numForces - 1, 1);
            starRevived = true;
        } else
            game.reviveForces(this, isPaid, numForces, 0);
    }

    public void presentAllyRevivalChoices() {
        if (ally != null) {
            Faction allyFaction = game.getFaction(ally);
            int revivableForces = allyFaction.getRevivableForces();
            if (revivableForces == 0) {
                chat.publish("Your ally " + allyFaction.getEmoji() + " has no revivable forces for you to pay for.");
            } else {
                paidRevivalTBD = true;
                List<DuneChoice> choices = new ArrayList<>();
                IntStream.rangeClosed(0, 3).forEachOrdered(i -> {
                    DuneChoice choice = new DuneChoice("revival-emp-ally-" + i, "" + i);
                    choice.setDisabled(spice < allyFaction.revivalCost(i, 0) || i > revivableForces);
                    choices.add(choice);
                });
                chat.publish("Would you like to purchase additional revivals for " + allyFaction.getEmoji() + "? " + player, choices);
            }
        }
    }

    public void reviveAllyForces(int numForces) throws InvalidGameStateException {
        paidRevivalTBD = false;
        if (!hasAlly())
            throw new InvalidGameStateException("Emperor does not have an ally.");
        Faction allyFaction = game.getFaction(ally);
        if (numForces == 0) {
            chat.publish("You will not revive any extra forces for your ally.");
            game.getTurnSummary().publish(Emojis.EMPEROR + " does not purchase extra revivals for " + allyFaction.getEmoji());
            return;
        }
        int cost;
        String revivalString;
        if (allyFaction instanceof IxFaction) {
            int numCyborgsInTanks = game.getTleilaxuTanks().getForceStrength("Ix*");
            int numCyborgsToRevive = Math.min(Math.min(numForces, numCyborgsInTanks), spice / 3);
            int numSuboidsInTanks = game.getTleilaxuTanks().getForceStrength("Ix");
            int numSuboidsToRevive = Math.min(numForces - numCyborgsToRevive, numSuboidsInTanks);
            game.reviveForces(allyFaction, false, numSuboidsToRevive, numCyborgsToRevive, true);
            cost = allyFaction.revivalCost(numSuboidsToRevive, numCyborgsToRevive);
            revivalString = " to revive " + allyFaction.forcesString(numSuboidsToRevive, numCyborgsToRevive);
        } else {
            game.reviveForces(allyFaction, false, numForces, 0, true);
            cost = allyFaction.revivalCost(numForces, 0);
            revivalString = " to revive " + allyFaction.forcesString(numForces, 0);
        }
        subtractSpice(cost, allyFaction.getEmoji() + " revivals");
        String costString = cost + " " + Emojis.SPICE;
        BTFaction bt = game.getBTFactionOrNull();
        if (bt != null) {
            costString += " to " + bt.getEmoji();
            bt.addSpice(cost, allyFaction.getEmoji() + " revivals, " + Emojis.EMPEROR + " alliance power");
        }
        chat.publish("Your revival request for your ally has been submitted to the " + Emojis.BT);
        game.getTurnSummary().publish(Emojis.EMPEROR + " pays " + costString + revivalString);
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
