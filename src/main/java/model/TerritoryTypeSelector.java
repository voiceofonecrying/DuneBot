package model;

import com.google.gson.JsonElement;
import io.gsonfire.TypeSelector;

public class TerritoryTypeSelector implements TypeSelector<Territory> {
    /**
     * @param readElement The json element that is being read
     * @return The class that should be used to deserialize the json element
     */
    @Override
    public Class<? extends Territory> getClassForElement(JsonElement readElement) {
        String territoryName = readElement.getAsJsonObject().get("territoryName").getAsString();
        Class<? extends Territory> territoryClass = Territory.class;

        switch (territoryName) {
            case "Caladan", "Wallach IX", "Tleilax", "Tupile", "Ecaz", "Kaitain", "Salusa Secundus",
                 "Southern Hemisphere", "Junction", "Giedi Prime", "Ix", "Grumman", "Richese" -> territoryClass = HomeworldTerritory.class;
        }

        return territoryClass;
    }
}
