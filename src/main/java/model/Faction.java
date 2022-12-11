package model;

import java.util.ArrayList;
import java.util.List;

public class Faction extends GameFactionBase {
    private final String name;
    private String emoji;
    private final String player;
    private final String userName;
    private final int handLimit;

    public Faction(String name, String player, String userName) {
        super();

        if (name.equals("Harkonnen")) this.handLimit = 8;
        else if (name.equals("CHOAM")) this.handLimit = 5;
        else this.handLimit = 4;
        this.name = name;
        this.player = player;
        this.userName = userName;
    }

    public String getName() {
        return name;
    }

    public String getEmoji() {
        return emoji;
    }

    public void setEmoji(String emoji) {
        this.emoji = emoji;
    }

    public String getPlayer() {
        return player;
    }

    public String getUserName() {
        return userName;
    }

    public int getHandLimit() {
        return handLimit;
    }
}