package Model;

import Model.CompositeKeys.TreacheryCardId;

import jakarta.persistence.*;

@Entity
@Table(name = "treachery_card")
@IdClass(TreacheryCardId.class)
public class TreacheryCard {
    @Id
    @Column(name = "GAME_ID")
    private int gameId;
    @Id
    @Column(name = "CARD_NAME")
    private String cardName;
    @Column(name = "LOCATION")
    private String location;

    public TreacheryCard(int gameId, String cardName, String location) {
        this.gameId = gameId;
        this.cardName = cardName;
        this.location = location;
    }

    public TreacheryCard() {}

}