package model;

import org.json.JSONObject;

public class Faction extends JSONObject {

    public Faction(String name, String emoji, String user) {
        this.put("name", name);
        this.put("emoji", emoji);
        this.put("player", user);
        this.put("resources", new JSONObject());
    }

    public Faction(JSONObject j) {
        super(j.toString());
    }

    public String getName() {
        return this.getString("name");
    }

    public void setName(String name) {
        this.put("name", name);
    }

    public String getEmoji() {
        return this.getString("emoji");
    }

    public void setEmoji(String emoji) {
        this.put("emoji", emoji);
    }

    public JSONObject getResources() {
        return this.getJSONObject("resources");
    }

    public void addResource(Resource resource) {
        this.getResources().put(resource.getName(), resource.getValue());
    }

    public Resource getResource(String name) {
        if (getResources().get(name) == null) return null;
        Object value = getResources().get(name);
        return new Resource<>(name, value);
    }
}