package caches;

import helpers.CSVTools;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class CardCache {
    private static final Map<String, Map<String, Map<String, String>>> cardMaps = new HashMap<>();

    public static void loadCardMap(String name, String path) throws IOException {
        CSVParser csvParser = CSVTools.getParser(path);

        cardMaps.put(
                name,
                csvParser.stream().map(CSVRecord::toMap).collect(Collectors.toMap(v -> v.get("Name").toLowerCase(), v -> v))
        );

        csvParser.close();
    }

    private static Map<String, Map<String, String>> getCardMap(String name) {
        return cardMaps.get(name);
    }
    public static Map<String, String> getCardInfo(String name, String cardName) {
        return getCardMap(name).getOrDefault(cardName.toLowerCase(), new HashMap<>());
    }

    public static List<String> getNames(String name) {
        return List.copyOf(getCardMap(name).keySet());
    }
}
