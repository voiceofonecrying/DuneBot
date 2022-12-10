package model;

import controller.Initializers;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;

public class Game extends GameFactionBase {
    private String gameRole;
    private String modRole;
    private Boolean mute;

    private final List<Faction> factions;

    private final Map<String, Territory> territories;
    public Game() {
        super();

        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(
                Objects.requireNonNull(this.getClass().getClassLoader().getResourceAsStream("Territories.csv"))
        ));
        factions = new ArrayList<>();
        territories = new HashMap<>();
        try {
            CSVParser csvParser = CSVParser.parse(bufferedReader, CSVFormat.RFC4180.builder().setHeader().setSkipHeaderRecord(true).build());
            for (CSVRecord csvRecord : csvParser) {
                territories.put(csvRecord.get(0), new Territory(csvRecord.get(0), Integer.parseInt(csvRecord.get(1)), Boolean.parseBoolean(csvRecord.get(2)), Boolean.parseBoolean(csvRecord.get(3))));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public List<Faction> getFactions() {
        return factions;
    }

    public Map<String, Territory> getTerritories() {
        return territories;
    }

    public String getGameRole() {
        return gameRole;
    }

    public void setGameRole(String gameRole) {
        this.gameRole = gameRole;
    }

    public String getModRole() {
        return modRole;
    }

    public void setModRole(String modRole) {
        this.modRole = modRole;
    }

    public Boolean getMute() {
        return mute;
    }

    public void setMute(Boolean mute) {
        this.mute = mute;
    }

    private Optional<Faction> findFaction(String name) {
        return factions.stream()
                .filter(f -> f.getName().equals(name))
                .findFirst();
    }
    public Faction getFaction(String name) {
        return findFaction(name).get();
    }

    public Boolean hasFaction(String name) {
        return findFaction(name).isPresent();
    }

    public void addFaction(Faction faction) {
        factions.add(faction);
    }
}
