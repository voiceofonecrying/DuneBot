package Model;

import Model.CompositeKeys.FactionId;

import jakarta.persistence.*;

@Entity
@Table(name = "faction_info")
@IdClass(FactionId.class)
public class Faction {
    @Id
    @Column(name = "GAME_ID")
    int gameId;
    @Id
    @Column(name = "DISCORD_ID")
    String discordId;
    @Column(name = "FACTION_NAME")
    String name;
    @Column(name = "SPICE")
    int spice;
    @Column(name = "LOST_FORCES")
    int lostForces;

    public Faction(String discordId, String name, int spice, int lostForces) {
        this.discordId = discordId;
        this.name = name;
        this.spice = spice;
        this.lostForces = lostForces;
    }

    public Faction() {}

    public int getGameId() {
        return gameId;
    }

    public void setGameId(int gameId) {
        this.gameId = gameId;
    }

    public String getDiscordId() {
        return discordId;
    }

    public void setDiscordId(String discordId) {
        this.discordId = discordId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getSpice() {
        return spice;
    }

    public void setSpice(int spice) {
        this.spice = spice;
    }

    public int getLostForces() {
        return lostForces;
    }

    public void setLostForces(int lostForces) {
        this.lostForces = lostForces;
    }
}