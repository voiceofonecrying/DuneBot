package model;

import org.json.JSONArray;
import org.json.JSONObject;

public class Game extends JSONObject {

    public Game(String s) {
        super(s);
    }

    public Game() {
        super();
    }

    public JSONObject getResources() {
        return this.getJSONObject("game_state").getJSONObject("game_resources");
    }

    public JSONArray getDeck(String deck) {
        return this.getJSONObject("game_state").getJSONObject("game_resources").getJSONArray(deck);
    }

    public JSONObject getFaction(String name) {
        return this.getJSONObject("game_state").getJSONObject("factions").getJSONObject(name);
    }

}
