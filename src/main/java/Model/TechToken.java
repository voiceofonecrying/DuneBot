package Model;

import javax.persistence.*;

@Entity
@Table(name = "tech_token")
@IdClass(TechTokenId.class)
public class TechToken {
    @Id
    @Column(name = "GAME_ID")
    private int gameId;
    @Id
    @Column(name = "TECH_NAME")
    private String techName;
    @Column(name = "OWNED_BY")
    private String ownedBy;

    public TechToken(int gameId, String techName, String ownedBy) {
        this.gameId = gameId;
        this.techName = techName;
        this.ownedBy = ownedBy;
    }

    public TechToken() {}

}
