package Model;

import org.json.JSONObject;

public class Faction extends JSONObject {

    public Faction(String id, String name, String emoji) {
        this.put("id", id);
        this.put("name", name);
        this.put("emoji", emoji);
        this.put("resources", new JSONObject());
    }

    public String getId() {
        return this.getString("id");
    }

    public void setId(String id) {
        this.put("id", id);
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