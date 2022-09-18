package model;

import org.json.JSONObject;

public class Territory extends JSONObject {

    public Territory(String territoryName, int sector, boolean isRock, boolean isStronghold) {
        this.put("territory_name", territoryName);
        this.put("sector", sector);
        this.put("is_rock", isRock);
        this.put("is_stronghold", isStronghold);
        this.put("forces", new JSONObject());
        this.put("spice", 0);
    }
}
