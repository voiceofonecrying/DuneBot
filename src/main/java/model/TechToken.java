package model;

public class TechToken {

    private int gameId;
    private String techName;
    private String ownedBy;

    public TechToken(int gameId, String techName, String ownedBy) {
        this.gameId = gameId;
        this.techName = techName;
        this.ownedBy = ownedBy;
    }

    public TechToken() {}

}
