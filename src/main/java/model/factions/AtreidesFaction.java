package model.factions;

import constants.Emojis;
import enums.UpdateType;
import model.Game;
import model.Leader;
import model.SpiceCard;
import model.Territory;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

public class AtreidesFaction extends Faction {
    private int forcesLost;
    private boolean cardPrescienceBlocked;
    private boolean denyingAllyBattlePrescience;
    private boolean grantingAllyTreacheryPrescience;

    public AtreidesFaction(String player, String userName) throws IOException {
        super("Atreides", player, userName);

        setSpice(10);
        this.freeRevival = 2;
        this.hasMiningEquipment = true;
        this.emoji = Emojis.ATREIDES;
        this.highThreshold = 6;
        this.lowThreshold = 5;
        this.occupiedIncome = 2;
        this.homeworld = "Caladan";

        this.forcesLost = 0;
        this.grantingAllyTreacheryPrescience = false;
    }

    @Override
    public void joinGame(@NotNull Game game) {
        super.joinGame(game);
        Territory caladan = game.getTerritories().addHomeworld(game, homeworld, name);
        caladan.addForces(name, 10);
        game.getHomeworlds().put(name, homeworld);
        game.getTerritory("Arrakeen").addForces("Atreides", 10);
    }

    /**
     * @return The number of forces lost in battle by the Atreides player
     */
    public int getForcesLost() {
        return forcesLost;
    }

    /**
     * @param forcesLost The number of forces lost in battle by the Atreides player
     */
    public void setForcesLost(int forcesLost) {
        if (this.forcesLost >= 7)
            return;
        this.forcesLost = forcesLost;
        if (this.forcesLost >= 7) {
            addLeader(new Leader("Kwisatz Haderach", 2, "Atreides", null, false));
            game.getTurnSummary().publish("The sleeper has awakened! " + Emojis.ATREIDES + " Paul Muad'Dib! Muad'Dib! Muad'Dib!");
        }
        setUpdated(UpdateType.MISC_BACK_OF_SHIELD);
    }

    /**
     * @param forceLost The number of forces lost in battle by the Atreides player
     */
    public void addForceLost(int forceLost) {
        setForcesLost(this.forcesLost + forceLost);
        setUpdated(UpdateType.MISC_BACK_OF_SHIELD);
    }

    /**
     * @return Whether the Atreides player has earned the Kwisatz Haderach
     */
    public boolean isHasKH() {
        return forcesLost >= 7;
    }

    public void giveSpiceDeckPrescience() {
        if (isHighThreshold()) {
            SpiceCard nextCard = game.getSpiceDeck().peek();
            if (nextCard != null) {
                if (nextCard.discoveryToken() == null)
                    chat.publish("You see visions of " + nextCard.name() + " in your future.");
                else
                    chat.publish("6 " + Emojis.SPICE + " will appear in " + nextCard.name() + " and destroy any forces and " + Emojis.SPICE + " there. A " + nextCard.discoveryToken() + " will appear in " + nextCard.tokenLocation());
            }
        }
    }

    public boolean isCardPrescienceBlocked() {
        return cardPrescienceBlocked;
    }

    public void setCardPrescienceBlocked(boolean cardPrescienceBlocked) {
        this.cardPrescienceBlocked = cardPrescienceBlocked;
    }

    public boolean isDenyingAllyBattlePrescience() {
        return denyingAllyBattlePrescience;
    }

    public void setDenyingAllyBattlePrescience(boolean denyingAllyBattlePrescience) {
        this.denyingAllyBattlePrescience = denyingAllyBattlePrescience;
        ledger.publish("You are " + (denyingAllyBattlePrescience ? "denying " : "granting ") + "Battle Prescience to " + Emojis.getFactionEmoji(ally));
        setUpdated(UpdateType.MISC_BACK_OF_SHIELD);
    }

    public boolean isGrantingAllyTreacheryPrescience() {
        return grantingAllyTreacheryPrescience;
    }

    public void setGrantingAllyTreacheryPrescience(boolean grantingAllyTreacheryPrescience) {
        this.grantingAllyTreacheryPrescience = grantingAllyTreacheryPrescience;
        ledger.publish("You are " + (grantingAllyTreacheryPrescience ? "granting " : "denying ") + Emojis.TREACHERY + " Prescience to " + Emojis.getFactionEmoji(ally));
        setUpdated(UpdateType.MISC_BACK_OF_SHIELD);
    }
}
