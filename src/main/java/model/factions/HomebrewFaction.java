package model.factions;

import constants.Colors;
import constants.Emojis;
import model.*;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Objects;

public class HomebrewFaction extends Faction{
    String factionProxy;
    String homeworldProxy;

    public HomebrewFaction(String name, String factionProxy, String homeworld, String player, String userName) throws IOException {
        super(name, player, userName);
        this.factionProxy = factionProxy;
        this.homeworld = homeworld;
        this.emoji = Emojis.getFactionEmoji(factionProxy);

        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(
                Objects.requireNonNull(Faction.class.getClassLoader().getResourceAsStream("cards/homeworlds.csv"))
        ));
        for (CSVRecord csvRecord : CSVParser.parse(bufferedReader, CSVFormat.EXCEL))
            if (csvRecord.get(1).equals(factionProxy))
                homeworldProxy = csvRecord.get(0);
    }

    @Override
    public void joinGame(@NotNull Game game) {
        super.joinGame(game);
        Territory hwTerritory = game.getTerritories().addHomeworld(game, homeworld, name);
        hwTerritory.addForces(name, 20);
        game.getHomeworlds().put(name, homeworld);
    }

    public String getFactionProxy() {
        return factionProxy;
    }

    public String getHomeworldProxy() {
        return homeworldProxy;
    }

    @Override
    public Color getColor() {
        return Colors.getFactionColor(factionProxy);
    }

    @Override
    protected HomeworldTerritory getHomeworldTerritory() {
        Territory territory = game.getTerritory(homeworld);
        if (!(territory instanceof HomeworldTerritory)) {
            game.getTerritories().remove(homeworld, territory);
            Territory hwt = game.getTerritories().addHomeworld(game, homeworld, name);
            territory.getForces().forEach(f -> hwt.addForces(f.getName(), f.getStrength()));
        }
        return (HomeworldTerritory) game.getTerritory(homeworld);
    }
}
