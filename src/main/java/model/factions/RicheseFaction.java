package model.factions;

import constants.Emojis;
import enums.UpdateType;
import model.Game;
import model.Territory;
import model.TreacheryCard;

import java.io.IOException;
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
        Territory richese = game.getTerritories().addHomeworld(game, homeworld, name);
        richese.addForces(name, 20);
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
}
