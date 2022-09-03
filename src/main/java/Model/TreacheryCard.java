package Model;

public class TreacheryCard {

    private int gameId;
    private String cardName;
    private String location;

    public TreacheryCard(int gameId, String cardName, String location) {
        this.gameId = gameId;
        this.cardName = cardName;
        this.location = location;
    }

    public TreacheryCard() {}

}