package Model;

import Model.CompositeKeys.LeaderId;

import jakarta.persistence.*;

@Entity
@IdClass(LeaderId.class)
@Table(name = "leader")
public class Leader {
    @Id
    @Column(name = "GAME_ID")
    private int gameId;
    @Id
    @Column(name = "LEADER_NAME")
    private int leaderName;
    @Column(name = "BATTLE_STRENGTH")
    private int battleStrength;
    @Column(name = "FACTION_NAME")
    private String factionName;
    @Column(name = "TRAITOR_FOR")
    private String traitorFor;
    @Column(name = "IS_ALIVE")
    private boolean isAlive;

    public Leader(int gameId, int leaderName, int battleStrength, String factionName, String traitorFor, boolean isAlive) {
        this.gameId = gameId;
        this.leaderName = leaderName;
        this.battleStrength = battleStrength;
        this.factionName = factionName;
        this.traitorFor = traitorFor;
        this.isAlive = isAlive;
    }

    public Leader() {}

}

