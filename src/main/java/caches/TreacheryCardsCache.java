package caches;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public class TreacheryCardsCache {
    private static final String NAME = "TreacheryCards";
    private static final String CSV_PATH = "cards/treachery.csv";

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

    public static String getType(String cardName) {
        return getCardInfo(cardName).getOrDefault("Type", "");
    }

    public static String getColor(String cardName) {
        return getCardInfo(cardName).getOrDefault("Color", "");
    }

    public static String getDescription(String cardName) {
        return getCardInfo(cardName).getOrDefault("Description", "");
    }
}
