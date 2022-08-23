package Model;

public class SpiceCard {

    private int gameId;
    private String cardName;
    private int sector;
    private String location;
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
