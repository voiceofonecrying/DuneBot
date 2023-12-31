package model;

import caches.StrongholdCardsCache;

public record StrongholdCard(String name) {
    public String description() {
        return StrongholdCardsCache.getDescription(name());
    }
}
