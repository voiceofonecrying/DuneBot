package model.factions;

import constants.Emojis;
import model.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class RicheseFaction extends Faction {
    public RicheseFaction(String player, String userName, Game game) throws IOException {
        super("Richese", player, userName, game);

        setSpice(5);
        this.freeRevival = 2;
        this.reserves = new Force("Richese", 20);
        this.emoji = Emojis.RICHESE;
        this.resources.add(new IntegerResource("no field", 0, 0, 0));
        this.resources.add(new IntegerResource("no field", 3, 3, 3));
        this.resources.add(new IntegerResource("no field", 5, 5, 5));

        this.resources.add(new Resource<List<TreacheryCard>>("cache", new ArrayList<>()));
        List<TreacheryCard> cache = (List<TreacheryCard>) this.getResource("cache").getValue();
        cache.add(new TreacheryCard("Ornithopter", "Special - Movement"));
        cache.add(new TreacheryCard("Residual Poison", "Special"));
        cache.add(new TreacheryCard("Semuta Drug", "Special"));
        cache.add(new TreacheryCard("Stone Burner", "Weapon - Special"));
        cache.add(new TreacheryCard("Mirror Weapon", "Weapon - Special"));
        cache.add(new TreacheryCard("Portable Snooper", "Defense - Poison"));
        cache.add(new TreacheryCard("Distrans", "Special"));
        cache.add(new TreacheryCard("Juice of Sapho", "Special"));
        cache.add(new TreacheryCard("Karama", "Special"));
        cache.add(new TreacheryCard("Nullentropy Box", "Special"));
    }


}
