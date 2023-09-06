package caches;

import java.util.HashMap;
import java.util.Map;

public class GameCache {
    static Map<String, String> gameJson = new HashMap<>();

    public static String getGameJson(String gameName) {
        return gameJson.get(gameName);
    }

    public static void setGameJson(String gameName, String json) {
        gameJson.put(gameName, json);
    }

    public static void clearGameJson(String gameName) {
        gameJson.remove(gameName);
    }

    public static boolean hasGameJson(String gameName) {
        return gameJson.containsKey(gameName);
    }
}
