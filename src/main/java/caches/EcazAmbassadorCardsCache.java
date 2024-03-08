package caches;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public class EcazAmbassadorCardsCache {
    private static final String NAME = "EcazEmbassador";
    private static final String CSV_PATH = "cards/ecaz.csv";

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

    public static String getName(String id) {
        return getCardInfo(id).get("Name");
    }

    public static String getDescription(String id) {
        return getCardInfo(id).getOrDefault("Description", "");
    }
}
