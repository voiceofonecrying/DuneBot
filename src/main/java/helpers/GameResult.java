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
    private List<String> assistantModerators;
    private String victoryType;
    private int turn;
    private List<Set<String>> winningFactions;
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
    private List<Set<String>> winningPlayers;
    private String predictedFaction;
    private String predictedPlayer;
    private String startingForum = "Discord";
    private String endingForum = "Discord";

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

    public String getArchiveDate() {
        return archiveDate;
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

    public String getDaysUntilArchive() {
        return daysUntilArchive;
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

    public List<String> getAssistantModerators() {
        return assistantModerators;
    }

    public void setAssistantModerators(List<String> assistantModerators) {
        this.assistantModerators = assistantModerators;
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

    public List<Set<String>> getWinningFactions() {
        return winningFactions;
    }

    public void setWinningFactions(List<Set<String>> winningFactions) {
        this.winningFactions = winningFactions;
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

    public List<Set<String>> getWinningPlayers() {
        return winningPlayers;
    }

    public void setWinningPlayers(List<Set<String>> winningPlayers) {
        this.winningPlayers = winningPlayers;
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

    public String getStartingForum() {
        return startingForum;
    }

    public void setStartingForum(String startingForum) {
        this.startingForum = startingForum;
    }

    public String getEndingForum() {
        return endingForum;
    }

    public void setEndingForum(String endingForum) {
        this.endingForum = endingForum;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GameResult that = (GameResult) o;
        return Objects.equals(gameName, that.gameName) && Objects.equals(messageID, that.messageID) && Objects.equals(archiveDate, that.archiveDate) && Objects.equals(gameStartDate, that.gameStartDate) && Objects.equals(gameEndDate, that.gameEndDate) && Objects.equals(gameDuration, that.gameDuration) && Objects.equals(daysUntilArchive, that.daysUntilArchive) && Objects.equals(moderator, that.moderator) && Objects.equals(victoryType, that.victoryType) && Objects.equals(turn, that.turn) && Objects.equals(winningFactions, that.winningFactions) && Objects.equals(atreides, that.atreides) && Objects.equals(bg, that.bg) && Objects.equals(bt, that.bt) && Objects.equals(choam, that.choam) && Objects.equals(ecaz, that.ecaz) && Objects.equals(emperor, that.emperor) && Objects.equals(fremen, that.fremen) && Objects.equals(guild, that.guild) && Objects.equals(harkonnen, that.harkonnen) && Objects.equals(ix, that.ix) && Objects.equals(moritani, that.moritani) && Objects.equals(richese, that.richese) && Objects.equals(winningPlayers, that.winningPlayers) && Objects.equals(predictedFaction, that.predictedFaction) && Objects.equals(predictedPlayer, that.predictedPlayer);
    }

    @Override
    public int hashCode() {
        return Objects.hash(gameName, messageID, archiveDate, gameStartDate, gameEndDate, gameDuration, daysUntilArchive, moderator, victoryType, turn, winningFactions, atreides, bg, bt, choam, ecaz, emperor, fremen, guild, harkonnen, ix, moritani, richese, winningPlayers, predictedFaction, predictedPlayer);
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

    public String getFactionForPlayer(String playerName) {
        List<String> factionNames = List.of("atreides", "bg", "bt", "choam", "ecaz", "emperor", "fremen", "guild", "harkonnen", "ix", "moritani", "richese");
        for (String f : factionNames) {
            String p = getFieldValue(f);
            if (p != null && p.equals(playerName))
                return f;
        }
        return null;
    }

    public boolean isFactionPlayer(String factionName, String playerName) {
        return playerName.equals(getFieldValue(factionName));
    }

    public boolean isWinningPlayer(String playerName) {
        return winningPlayers.stream().flatMap(Collection::stream).anyMatch(n -> n.equals(playerName));
    }

    public boolean isWinningFaction(String factionName) {
        return winningFactions.stream().flatMap(Collection::stream).anyMatch(n -> n.equals(factionName));
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
        List<String> allFactionWinners = winningFactions.stream().flatMap(Collection::stream).toList();
        gameRecord += String.join(",", allFactionWinners) + ",".repeat(Math.max(0, 1+ 6 - allFactionWinners.size()));
        List<String> allPlayerWinners = winningPlayers.stream().flatMap(Collection::stream).toList();
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
