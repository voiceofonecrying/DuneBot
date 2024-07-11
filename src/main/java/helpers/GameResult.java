package helpers;

import java.lang.reflect.Field;
import java.util.Objects;

public class GameResult {
    private String gameName;
    private String messageID;
    private String archiveDate;
    private String gameStartDate;
    private String gameEndDate;
    private String gameDuration;
    private String daysUntilArchive;
    private String moderator;
    private String victoryType;
    private String turn;
    private String winner1Faction;
    private String winner2Faction;
    private String atreides;
    private String bg;
    private String bt;
    private String choam;
    private String ecaz;
    private String emperor;
    private String fremen;
    private String guild;
    private String harkonnen;
    private String ix;
    private String moritani;
    private String richese;
    private String winner1Player;
    private String winner2Player;
    private String predictedFaction;
    private String predictedPlayer;

    public String getGameName() {
        return gameName;
    }

    public void setGameName(String gameName) {
        this.gameName = gameName;
    }

    public String getMessageID() {
        return messageID;
    }

    public void setMessageID(String messageID) {
        this.messageID = messageID;
    }

    public void setArchiveDate(String archiveDate) {
        this.archiveDate = archiveDate;
    }

    public void setGameStartDate(String gameStartDate) {
        this.gameStartDate = gameStartDate;
    }

    public String getGameEndDate() {
        return gameEndDate;
    }

    public void setGameEndDate(String gameEndDate) {
        this.gameEndDate = gameEndDate;
    }

    public String getGameDuration() {
        return gameDuration;
    }

    public void setGameDuration(String gameDuration) {
        this.gameDuration = gameDuration;
    }

    public void setDaysUntilArchive(String daysUntilArchive) {
        this.daysUntilArchive = daysUntilArchive;
    }

    public String getModerator() {
        return moderator;
    }

    public void setModerator(String moderator) {
        this.moderator = moderator;
    }

    public String getVictoryType() {
        return victoryType;
    }

    public void setVictoryType(String victoryType) {
        this.victoryType = victoryType;
    }

    public String getTurn() {
        return turn;
    }

    public void setTurn(String turn) {
        this.turn = turn;
    }

    public String getWinner1Faction() {
        return winner1Faction;
    }

    public void setWinner1Faction(String winner1Faction) {
        this.winner1Faction = winner1Faction;
    }

    public String getWinner2Faction() {
        return winner2Faction;
    }

    public void setWinner2Faction(String winner2Faction) {
        this.winner2Faction = winner2Faction;
    }

    public String getAtreides() {
        return atreides;
    }

    public void setAtreides(String atreides) {
        this.atreides = atreides;
    }

    public String getBG() {
        return bg;
    }

    public void setBG(String bg) {
        this.bg = bg;
    }

    public String getBT() {
        return bt;
    }

    public void setBT(String BT) {
        this.bt = BT;
    }

    public String getCHOAM() {
        return choam;
    }

    public void setCHOAM(String CHOAM) {
        this.choam = CHOAM;
    }

    public String getEcaz() {
        return ecaz;
    }

    public void setEcaz(String ecaz) {
        this.ecaz = ecaz;
    }

    public String getEmperor() {
        return emperor;
    }

    public void setEmperor(String emperor) {
        this.emperor = emperor;
    }

    public String getFremen() {
        return fremen;
    }

    public void setFremen(String fremen) {
        this.fremen = fremen;
    }

    public String getGuild() {
        return guild;
    }

    public void setGuild(String guild) {
        this.guild = guild;
    }

    public String getHarkonnen() {
        return harkonnen;
    }

    public void setHarkonnen(String harkonnen) {
        this.harkonnen = harkonnen;
    }

    public String getIx() {
        return ix;
    }

    public void setIx(String ix) {
        this.ix = ix;
    }

    public String getMoritani() {
        return moritani;
    }

    public void setMoritani(String moritani) {
        this.moritani = moritani;
    }

    public String getRichese() {
        return richese;
    }

    public void setRichese(String richese) {
        this.richese = richese;
    }

    public String getWinner1Player() {
        return winner1Player;
    }

    public void setWinner1Player(String winner1Player) {
        this.winner1Player = winner1Player;
    }

    public String getWinner2Player() {
        return winner2Player;
    }

    public void setWinner2Player(String winner2Player) {
        this.winner2Player = winner2Player;
    }

    public String getPredictedFaction() {
        return predictedFaction;
    }

    public void setPredictedFaction(String predictedFaction) {
        this.predictedFaction = predictedFaction;
    }

    public String getPredictedPlayer() {
        return predictedPlayer;
    }

    public void setPredictedPlayer(String predictedPlayer) {
        this.predictedPlayer = predictedPlayer;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GameResult that = (GameResult) o;
        return Objects.equals(gameName, that.gameName) && Objects.equals(messageID, that.messageID) && Objects.equals(archiveDate, that.archiveDate) && Objects.equals(gameStartDate, that.gameStartDate) && Objects.equals(gameEndDate, that.gameEndDate) && Objects.equals(gameDuration, that.gameDuration) && Objects.equals(daysUntilArchive, that.daysUntilArchive) && Objects.equals(moderator, that.moderator) && Objects.equals(victoryType, that.victoryType) && Objects.equals(turn, that.turn) && Objects.equals(winner1Faction, that.winner1Faction) && Objects.equals(winner2Faction, that.winner2Faction) && Objects.equals(atreides, that.atreides) && Objects.equals(bg, that.bg) && Objects.equals(bt, that.bt) && Objects.equals(choam, that.choam) && Objects.equals(ecaz, that.ecaz) && Objects.equals(emperor, that.emperor) && Objects.equals(fremen, that.fremen) && Objects.equals(guild, that.guild) && Objects.equals(harkonnen, that.harkonnen) && Objects.equals(ix, that.ix) && Objects.equals(moritani, that.moritani) && Objects.equals(richese, that.richese) && Objects.equals(winner1Player, that.winner1Player) && Objects.equals(winner2Player, that.winner2Player) && Objects.equals(predictedFaction, that.predictedFaction) && Objects.equals(predictedPlayer, that.predictedPlayer);
    }

    @Override
    public int hashCode() {
        return Objects.hash(gameName, messageID, archiveDate, gameStartDate, gameEndDate, gameDuration, daysUntilArchive, moderator, victoryType, turn, winner1Faction, winner2Faction, atreides, bg, bt, choam, ecaz, emperor, fremen, guild, harkonnen, ix, moritani, richese, winner1Player, winner2Player, predictedFaction, predictedPlayer);
    }

    public String getFieldValue(String fieldName) {
        try {
            Field f = getClass().getDeclaredField(fieldName);
            return (String) f.get(this);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            return null;
        }
    }

    public void setFieldValue(String fieldName, Object value) throws IllegalAccessException {
        for (Field f : getClass().getDeclaredFields())
            if (f.getName().equals(fieldName))
                f.set(this, value);
    }

    public static String getHeader() {
        return "V1.0,Atreides,BG,BT,CHOAM,Ecaz,Emperor,Fremen,Guild,Harkonnen,Ix,Moritani,Rich,Turn,Win Type,Faction 1,Faction 2,Winner 1,Winner 2,Predicted Faction,Predicted Player,Mod,Game Start,Game End,Duration,Archived,Days Until Archive";
    }

    public String csvString() {
        String gameRecord = "\n\"" + gameName + "\",";
        gameRecord += (atreides == null ? "" : atreides) + ",";
        gameRecord += (bg == null ? "" : bg) + ",";
        gameRecord += (bt == null ? "" : bt) + ",";
        gameRecord += (choam == null ? "" : choam) + ",";
        gameRecord += (ecaz == null ? "" : ecaz) + ",";
        gameRecord += (emperor == null ? "" : emperor) + ",";
        gameRecord += (fremen == null ? "" : fremen) + ",";
        gameRecord += (guild == null ? "" : guild) + ",";
        gameRecord += (harkonnen == null ? "" : harkonnen) + ",";
        gameRecord += (ix == null ? "" : ix) + ",";
        gameRecord += (moritani == null ? "" : moritani) + ",";
        gameRecord += (richese == null ? "" : richese) + ",";
        gameRecord += turn + ",";
        gameRecord += (victoryType == null ? "" : victoryType) + ",";
        String winner = winner1Faction == null ? "" : winner1Faction;
        if (winner.equals("Richese"))
            winner = "Rich";
        gameRecord += winner + ",";
        winner = winner2Faction == null ? "" : winner2Faction;
        if (winner.equals("Richese"))
            winner = "Rich";
        gameRecord += winner + ",";
        gameRecord += (winner1Player == null ? "" : winner1Player) + ",";
        gameRecord += (winner2Player == null ? "" : winner2Player) + ",";
        gameRecord += (predictedFaction == null ? "" : predictedFaction) + ",";
        gameRecord += (predictedPlayer == null ? "" : predictedPlayer) + ",";
        gameRecord += (moderator == null ? "" : moderator) + ",";
        gameRecord += (gameStartDate == null ? "" : gameStartDate) + ",";
        gameRecord += (gameEndDate == null ? "" : gameEndDate) + ",";
        gameRecord += (gameDuration == null ? "" : gameDuration) + ",";
        gameRecord += (archiveDate == null ? "" : archiveDate) + ",";
        gameRecord += (daysUntilArchive == null ? "" : daysUntilArchive);
//            gameRecord += (daysUntilArchive == null ? "" : daysUntilArchive) + ",";
        return gameRecord;
    }
}
