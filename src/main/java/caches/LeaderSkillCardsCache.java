package caches;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public class LeaderSkillCardsCache {
    private static final String NAME = "LeaderSkillCards";
    private static final String CSV_PATH = "cards/leader-skills.csv";

    static {
        try {
            CardCache.loadCardMap(NAME, CSV_PATH);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static List<String> getNames() {
        return CardCache.getNames(NAME);
    }

    public static Map<String, String> getCardInfo(String cardName) {
        return CardCache.getCardInfo(NAME, cardName);
    }

    public static String getDescription(String cardName) {
        if (cardName == null)
            return "";
        return getCardInfo(cardName).getOrDefault("Description", "");
    }

    public static String getInBattleDescription(String cardName) {
        if (cardName == null)
            return "";
        return getCardInfo(cardName).getOrDefault("In Battle Description", "");
    }
}
