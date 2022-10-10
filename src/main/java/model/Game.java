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

    public int getTurn() {
        return this.getResources().getInt("turn");
    }

    public int getPhase() {
        return this.getResources().getInt("phase");
    }

    public void advancePhase() {
        int i = this.getPhase();
        this.getResources().remove("phase");
        this.getResources().put("phase", ++i);
    }
    public void advanceTurn() {
        int i = this.getTurn();
        this.getResources().remove("turn");
        this.getResources().remove("phase");
        this.getResources().put("turn", ++i);
        this.getResources().put("phase", 1);
    }


    public JSONArray getDeck(String deck) {
        return this.getJSONObject("game_state").getJSONObject("game_resources").getJSONArray(deck);
    }

    public JSONObject getFaction(String name) {
        return this.getJSONObject("game_state").getJSONObject("factions").getJSONObject(name);
    }

}
