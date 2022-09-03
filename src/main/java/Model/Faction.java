package Model;

import java.util.ArrayList;
import java.util.List;

public class Faction {
    int gameId;
    String id;
    String name;
    String emoji;
    List<Resource> resources;

    public Faction(int gameId, String id, String name, String emoji) {
        this.gameId = gameId;
        this.id = id;
        this.name = name;
        this.emoji = emoji;
        this.resources = new ArrayList<>();
    }

    public int getGameId() {
        return gameId;
    }

    public void setGameId(int gameId) {
        this.gameId = gameId;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmoji() {
        return emoji;
    }

    public void setEmoji(String emoji) {
        this.emoji = emoji;
    }
}