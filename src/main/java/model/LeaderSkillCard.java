package model;

import caches.LeaderSkillCardsCache;

public record LeaderSkillCard(String name) {

    public String description() {
        return LeaderSkillCardsCache.getDescription(name());
    }

    public String inBattleDescription() {
        return LeaderSkillCardsCache.getInBattleDescription(name());
    }
}
