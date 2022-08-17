package Model;

import Model.CompositeKeys.SpiceCardId;

import jakarta.persistence.*;

@Entity
@IdClass(SpiceCardId.class)
@Table(name = "spice_card")
public class SpiceCard {
    @Id
    @Column(name = "GAME_ID")
    private int gameId;
    @Id
    @Column(name = "CARD_NAME")
    private String cardName;
    @Column(name = "LOCATION")
    private String location;

    public SpiceCard(int gameId, String cardName, String location) {
        this.gameId = gameId;
        this.cardName = cardName;
        this.location = location;
    }

    public SpiceCard() {}

}
