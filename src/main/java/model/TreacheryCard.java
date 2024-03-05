package model;

import caches.TreacheryCardsCache;

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
