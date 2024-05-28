package model.factions;

import constants.Emojis;
import enums.UpdateType;
import model.Force;
import model.Game;
import model.Territory;
import model.TreacheryCard;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

public class RicheseFaction extends Faction {
    List<TreacheryCard> treacheryCardCache;
    Integer frontOfShieldNoField;
    List<Integer> behindShieldNoField;

    public RicheseFaction(String player, String userName, Game game) throws IOException {
        super("Richese", player, userName, game);

        setSpice(5);
        this.freeRevival = 2;
        this.emoji = Emojis.RICHESE;
        this.highThreshold = 10;
        this.lowThreshold = 9;
        this.occupiedIncome = 1;
        this.homeworld = "Richese";
        Territory richese = game.getTerritories().addHomeworld(homeworld);
        richese.addForce(new Force(name, 10));
        game.getHomeworlds().put(name, homeworld);
        this.behindShieldNoField = new ArrayList<>();
        this.behindShieldNoField.add(0);
        this.behindShieldNoField.add(3);
        this.behindShieldNoField.add(5);

        treacheryCardCache = new ArrayList<>();
        treacheryCardCache.add(new TreacheryCard("Ornithopter"));
        treacheryCardCache.add(new TreacheryCard("Residual Poison"));
        treacheryCardCache.add(new TreacheryCard("Semuta Drug"));
        treacheryCardCache.add(new TreacheryCard("Stone Burner"));
        treacheryCardCache.add(new TreacheryCard("Mirror Weapon"));
        treacheryCardCache.add(new TreacheryCard("Portable Snooper"));
        treacheryCardCache.add(new TreacheryCard("Distrans"));
        treacheryCardCache.add(new TreacheryCard("Juice of Sapho"));
        treacheryCardCache.add(new TreacheryCard("Karama"));
        treacheryCardCache.add(new TreacheryCard("Nullentropy Box"));
    }

    public boolean hasFrontOfShieldNoField() {
        return frontOfShieldNoField != null;
    }

    public Integer getFrontOfShieldNoField() {
        return frontOfShieldNoField;
    }

    public void setFrontOfShieldNoField(Integer frontOfShieldNoField) {
        if (!List.of(0, 3, 5).contains(frontOfShieldNoField)) {
            throw new IllegalArgumentException("Front of shield no field must be 0, 3, or 5");
        }

        this.frontOfShieldNoField = frontOfShieldNoField;

        setUpdated(UpdateType.MISC_FRONT_OF_SHIELD);
    }

    public TreacheryCard getTreacheryCardFromCache(String name) {
        return treacheryCardCache.stream()
                .filter(t -> t.name().equals(name))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Treachery card not found in cache"));
    }

    public TreacheryCard removeTreacheryCardFromCache(TreacheryCard card) {
        treacheryCardCache.remove(card);
        setUpdated(UpdateType.MISC_BACK_OF_SHIELD);
        return card;
    }

    public List<TreacheryCard> getTreacheryCardCache() {
        return treacheryCardCache;
    }

    /**
     * Get the total spice that would be collected from a territory.  This function does not actually add or subtract
     * spice.  It only calculates the total
     *
     * @param territory The territory to calculate the spice from
     * @return The total spice that would be collected from the territory
     */
    @Override
    public int getSpiceCollectedFromTerritory(Territory territory) {
        int multiplier = hasMiningEquipment() ? 3 : 2;
        int totalForces = territory.hasRicheseNoField() ? 1 : 0;
        if (territory.hasForce("Richese")) {
            totalForces += territory.getForce("Richese").getStrength();
        }

        return Math.min(multiplier * totalForces, territory.getSpice());
    }

    public void noFieldMessage(int noField, String territoryName) {
        getLedger().publish(
                MessageFormat.format(
                        "{0} {1} placed on {2}",
                        noField,
                        Emojis.NO_FIELD,
                        territoryName
                )
        );
    }
}
