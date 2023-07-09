package model.factions;

import com.google.gson.internal.LinkedTreeMap;
import constants.Emojis;
import model.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class RicheseFaction extends Faction {
    List <TreacheryCard> treacheryCardCache;
    Integer frontOfShieldNoField;

    public RicheseFaction(String player, String userName, Game game) throws IOException {
        super("Richese", player, userName, game);

        setSpice(5);
        this.freeRevival = 2;
        this.reserves = new Force("Richese", 20);
        this.emoji = Emojis.RICHESE;

        treacheryCardCache = new ArrayList<>();
        treacheryCardCache.add(new TreacheryCard("Ornithopter", "Special - Movement"));
        treacheryCardCache.add(new TreacheryCard("Residual Poison", "Special"));
        treacheryCardCache.add(new TreacheryCard("Semuta Drug", "Special"));
        treacheryCardCache.add(new TreacheryCard("Stone Burner", "Weapon - Special"));
        treacheryCardCache.add(new TreacheryCard("Mirror Weapon", "Weapon - Special"));
        treacheryCardCache.add(new TreacheryCard("Portable Snooper", "Defense - Poison"));
        treacheryCardCache.add(new TreacheryCard("Distrans", "Special"));
        treacheryCardCache.add(new TreacheryCard("Juice of Sapho", "Special"));
        treacheryCardCache.add(new TreacheryCard("Karama", "Special"));
        treacheryCardCache.add(new TreacheryCard("Nullentropy Box", "Special"));
    }

    public void migrateRichese() {
        // Saving modified values to make sure the migration does not cause updates to be triggered
        boolean frontOfShieldModified = isFrontOfShieldModified();
        boolean backOfShieldModified = isBackOfShieldModified();

        if (hasResource("frontOfShieldNoField")) {
            frontOfShieldNoField = Math.round(Float.parseFloat(getResource("frontOfShieldNoField").getValue().toString()));
            removeResource("frontOfShieldNoField");
        }

        if (hasResource("cache")) {
            treacheryCardCache = new ArrayList<>();

            List<LinkedTreeMap> rawList = (ArrayList<LinkedTreeMap>) getResource("cache").getValue();

            for (LinkedTreeMap linkedTreeMap : rawList) {
                treacheryCardCache.add(new TreacheryCard(
                        (String) linkedTreeMap.get("name"), (String) linkedTreeMap.get("type")
                ));
            }

            removeResource("cache");
        }

        // Making sure the migration does not cause updates to be triggered
        setFrontOfShieldModified(frontOfShieldModified);
        setBackOfShieldModified(backOfShieldModified);
    }

    public boolean hasFrontOfShieldNoField() {
        migrateRichese();
        return frontOfShieldNoField != null;
    }

    public Integer getFrontOfShieldNoField() {
        migrateRichese();
        return frontOfShieldNoField;
    }

    public void setFrontOfShieldNoField(Integer frontOfShieldNoField) {
        migrateRichese();
        if (!List.of(0, 3, 5).contains(frontOfShieldNoField)) {
            throw new IllegalArgumentException("Front of shield no field must be 0, 3, or 5");
        }

        this.frontOfShieldNoField = frontOfShieldNoField;

        setFrontOfShieldModified();
    }

    public TreacheryCard getTreacheryCardFromCache(String name) {
        migrateRichese();
        return treacheryCardCache.stream()
                .filter(t -> t.name().equals(name))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Treachery card not found in cache"));
    }

    public TreacheryCard removeTreacheryCardFromCache(TreacheryCard card) {
        migrateRichese();
        treacheryCardCache.remove(card);
        setBackOfShieldModified();
        return card;
    }

    public List<TreacheryCard> getTreacheryCardCache() {
        migrateRichese();
        return treacheryCardCache;
    }
}
