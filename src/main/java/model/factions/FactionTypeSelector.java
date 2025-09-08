package model.factions;

import com.google.gson.JsonElement;
import io.gsonfire.TypeSelector;

public class FactionTypeSelector implements TypeSelector<Faction> {
    /**
     * @param readElement The json element that is being read
     * @return The class that should be used to deserialize the json element
     */
    @Override
    public Class<? extends Faction> getClassForElement(JsonElement readElement) {
        String factionName = readElement.getAsJsonObject().get("name").getAsString();
        Class<? extends Faction> factionClass = Faction.class;

        switch (factionName.toUpperCase()) {
            case "ATREIDES" -> factionClass = AtreidesFaction.class;
            case "BG" -> factionClass = BGFaction.class;
            case "BT" -> factionClass = BTFaction.class;
            case "CHOAM" -> factionClass = ChoamFaction.class;
            case "EMPEROR" -> factionClass = EmperorFaction.class;
            case "FREMEN" -> factionClass = FremenFaction.class;
            case "GUILD" -> factionClass = GuildFaction.class;
            case "HARKONNEN" -> factionClass = HarkonnenFaction.class;
            case "IX" -> factionClass = IxFaction.class;
            case "RICHESE" -> factionClass = RicheseFaction.class;
            case "ECAZ" -> factionClass = EcazFaction.class;
            case "MORITANI" -> factionClass = MoritaniFaction.class;
            default -> factionClass = HomebrewFaction.class;
        }

        return factionClass;
    }
}
