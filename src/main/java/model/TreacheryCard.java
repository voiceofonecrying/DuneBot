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
}
