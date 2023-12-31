package caches;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public class HomeworldCardsCache {
    private static final String NAME = "HomeworldCards";
    private static final String CSV_PATH = "cards/homeworlds.csv";

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

    public static String getFactionName(String cardName) {
        return getCardInfo(cardName).getOrDefault("Faction", "");
    }

    public static int getHighLowerThreshold(String cardName) {
        return Integer.parseInt(getCardInfo(cardName).getOrDefault("High Lower Threshold", "0"));
    }

    public static int getHighUpperThreshold(String cardName) {
        return Integer.parseInt(getCardInfo(cardName).getOrDefault("High Upper Threshold", "0"));
    }

    public static String getHighDescription(String cardName) {
        return getCardInfo(cardName).getOrDefault("High Description", "");
    }

    public static int getLowLowerThreshold(String cardName) {
        return Integer.parseInt(getCardInfo(cardName).getOrDefault("Low Lower Threshold", "0"));
    }

    public static int getLowUpperThreshold(String cardName) {
        return Integer.parseInt(getCardInfo(cardName).getOrDefault("Low Upper Threshold", "0"));
    }

    public static String getLowDescription(String cardName) {
        return getCardInfo(cardName).getOrDefault("Low Description", "");
    }

    public static String getOccupiedDescription(String cardName) {
        return getCardInfo(cardName).getOrDefault("Occupied Description", "");
    }

    public static int getOccupiedSpice(String cardName) {
        return Integer.parseInt(getCardInfo(cardName).getOrDefault("Occupied Spice", "0"));
    }

    public static int getHighBattleExplosion(String cardName) {
        return Integer.parseInt(getCardInfo(cardName).getOrDefault("High Battle Explosion", "0"));
    }

    public static int getLowBattleExplosion(String cardName) {
        return Integer.parseInt(getCardInfo(cardName).getOrDefault("Low Battle Explosion", "0"));
    }

    public static int getLowRevivalCharity(String cardName) {
        return Integer.parseInt(getCardInfo(cardName).getOrDefault("Low Revival Charity", "0"));
    }
}
