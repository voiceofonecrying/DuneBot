package model;

import caches.EcazAmbassadorCardsCache;

public record EcazAmbassador(String id) {
    public String name() {
        return EcazAmbassadorCardsCache.getName(id());
    }

    public String description() {
        return EcazAmbassadorCardsCache.getDescription(id());
    }
}
