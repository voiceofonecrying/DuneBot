package helpers;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.util.*;

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
    private int turn;
    private List<Set<String>> winnerFactions;
    @Exclude
    private String winner1Faction;
    @Exclude
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
    private List<Set<String>> winnerPlayers;
    @Exclude
    private String winner1Player;
    @Exclude
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

    public int getTurn() {
        return turn;
    }

    public void setTurn(int turn) {
        this.turn = turn;
    }

    public String getWinner1Faction() {
        return winner1Faction;
    }

    public String getWinner2Faction() {
        return winner2Faction;
    }

    public List<Set<String>> getWinnerFactions() {
        return winnerFactions;
    }

    public void setWinnerFactions(List<Set<String>> winnerFactions) {
        this.winnerFactions = winnerFactions;
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

    public String getWinner2Player() {
        return winner2Player;
    }

    public List<Set<String>> getWinnerPlayers() {
        return winnerPlayers;
    }

    public void setWinnerPlayers(List<Set<String>> winnerPlayers) {
        this.winnerPlayers = winnerPlayers;
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
        if (fieldName == null || fieldName.isBlank())
            return null;
        MethodHandles.Lookup lookup = MethodHandles.lookup();
        MethodHandle methodHandle;
        try {
            methodHandle = lookup.findGetter(GameResult.class, fieldName, String.class);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
        try {
            return (String) methodHandle.invoke(this);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    public void setFieldValue(String fieldName, Object value) {
        if (fieldName == null || fieldName.isBlank())
            return;
        MethodHandles.Lookup lookup = MethodHandles.lookup();
        MethodHandle methodHandle;
        try {
            methodHandle = lookup.findSetter(GameResult.class, fieldName, String.class);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
        try {
            methodHandle.invoke(this, value);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    public boolean isPlayer(String playerName) {
        if (atreides != null && atreides.equals(playerName)) return true;
        else if (bg != null && bg.equals(playerName)) return true;
        else if (bt != null && bt.equals(playerName)) return true;
        else if (choam != null && choam.equals(playerName)) return true;
        else if (ecaz != null && ecaz.equals(playerName)) return true;
        else if (emperor != null && emperor.equals(playerName)) return true;
        else if (fremen != null && fremen.equals(playerName)) return true;
        else if (guild != null && guild.equals(playerName)) return true;
        else if (harkonnen != null && harkonnen.equals(playerName)) return true;
        else if (ix != null && ix.equals(playerName)) return true;
        else if (moritani != null && moritani.equals(playerName)) return true;
        else return richese != null && richese.equals(playerName);
    }

    public boolean isFactionPlayer(String factionName, String playerName) {
        return playerName.equals(getFieldValue(factionName));
    }

    public boolean isWinner(String playerName) {
        return winnerPlayers.stream().flatMap(Collection::stream).anyMatch(n -> n.equals(playerName));
//        return winner1Player.equals(playerName) || winner2Player != null && winner2Player.equals(playerName);
    }

    public boolean isFactionWinner(String factionName) {
        return winnerFactions.stream().flatMap(Collection::stream).anyMatch(n -> n.equals(factionName));
//        return winner1Faction.equals(factionName) || winner2Faction != null && winner2Faction.equals(factionName);
    }

    public static String getHeader() {
        return "V1.0,Atreides,BG,BT,CHOAM,Ecaz,Emperor,Fremen,Guild,Harkonnen,Ix,Moritani,Richese,Turn,Win Type,Faction 1,Faction 2,Faction 3,Faction 4,Faction 5,Faction 6,Winner 1,Winner 2,Winner 3,Winner 4,Winner 5,Winner 6,Predicted Faction,Predicted Player,Mod,Game Start,Game End,Duration,Archived,Days Until Archive";
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
        List<String> allFactionWinners = winnerFactions.stream().flatMap(Collection::stream).toList();
        gameRecord += String.join(",", allFactionWinners) + ",".repeat(Math.max(0, 1+ 6 - allFactionWinners.size()));
        List<String> allPlayerWinners = winnerPlayers.stream().flatMap(Collection::stream).toList();
        gameRecord += String.join(",", allPlayerWinners) + ",".repeat(Math.max(0, 1 + 6 - allPlayerWinners.size()));
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
