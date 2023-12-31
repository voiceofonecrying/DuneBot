package model;

import caches.HomeworldCardsCache;

public record HomeworldCard(String name) {

    public String factionName() {
        return HomeworldCardsCache.getFactionName(name());
    }

    public int highLowerThreshold() {
        return HomeworldCardsCache.getHighLowerThreshold(name());
    }

    public int highUpperThreshold() {
        return HomeworldCardsCache.getHighUpperThreshold(name());
    }

    public String highDescription() {
        return HomeworldCardsCache.getHighDescription(name());
    }

    public int lowLowerThreshold() {
        return HomeworldCardsCache.getLowLowerThreshold(name());
    }

    public int lowUpperThreshold() {
        return HomeworldCardsCache.getLowUpperThreshold(name());
    }

    public String lowDescription() {
        return HomeworldCardsCache.getLowDescription(name());
    }

    public String occupiedDescription() {
        return HomeworldCardsCache.getOccupiedDescription(name());
    }

    public int occupiedSpice() {
        return HomeworldCardsCache.getOccupiedSpice(name());
    }

    public int highBattleExplosion() {
        return HomeworldCardsCache.getHighBattleExplosion(name());
    }

    public int lowBattleExplosion() {
        return HomeworldCardsCache.getLowBattleExplosion(name());
    }

    public int lowRevivalCharity() {
        return HomeworldCardsCache.getLowRevivalCharity(name());
    }
}
