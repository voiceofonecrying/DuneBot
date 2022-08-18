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
    @Column(name = "SECTOR")
    private int sector;
    @Column(name = "LOCATION")
    private String location;
    @Column(name = "SPICE")
    private int spice;

    public SpiceCard(int gameId, String cardName, int sector, String location, int spice) {
        this.gameId = gameId;
        this.cardName = cardName;
        this.sector = sector;
        this.location = location;
        this.spice = spice;
    }

    public SpiceCard() {}

}
