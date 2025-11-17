package model;

import caches.TreacheryCardsCache;
import constants.Emojis;

public record TreacheryCard(String name) {
    public String type() {
        return TreacheryCardsCache.getType(name());
    }

    public String color() {
        return TreacheryCardsCache.getColor(name());
    }

    public String description() {
        return TreacheryCardsCache.getDescription(name());
    }

    public String prettyNameAndDescription() {
        return Emojis.TREACHERY + " **" + name + "** _" + type() + "_";
    }

    public boolean servesAsPoisonWeapon() {
        return type().equals("Weapon - Poison") || name().equals("Chemistry") || name().equals("Poison Blade");
    }

    public boolean isStoppedBySnooper() {
        return type().equals("Weapon - Poison") || name().equals("Chemistry");
    }

    public boolean servesAsShield() {
        return type().equals("Defense - Projectile") || name().equals("Shield Snooper");
    }

    public boolean servesAsSnooper() {
        return type().equals("Defense - Poison") || name().equals("Shield Snooper");
    }

    public boolean isGreenSpecialCard() {
        return type().startsWith("Special") || type().equals("Spice Blow - Special");
    }
}
