package model.factions;

import constants.Emojis;
import model.Game;
import model.Leader;
import model.Territory;
import model.TraitorCard;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

public class HarkonnenFaction extends Faction {
    private Boolean hasTriggeredHT;
    private boolean bonusCardBlocked;

    public HarkonnenFaction(String player, String userName) throws IOException {
        super("Harkonnen", player, userName);

        this.spice = 10;
        this.freeRevival = 2;
        this.hasMiningEquipment = true;
        this.emoji = Emojis.HARKONNEN;
        this.highThreshold = 7;
        this.lowThreshold = 6;
        this.occupiedIncome = 2;
        this.homeworld = "Giedi Prime";
        this.hasTriggeredHT = false;
        this.handLimit = 8;
    }

    @Override
    public void joinGame(@NotNull Game game) {
        super.joinGame(game);
        game.getTerritories().get("Carthag").addForces("Harkonnen", 10);
        Territory giediPrime = game.getTerritories().addHomeworld(game, homeworld, name);
        giediPrime.addForces(name, 10);
        game.getHomeworlds().put(name, homeworld);
    }

    public boolean hasTriggeredHT() {
        if (hasTriggeredHT == null) hasTriggeredHT = false;
        return hasTriggeredHT;
    }

    public void setTriggeredHT(boolean hasTriggeredHT) {
        this.hasTriggeredHT = hasTriggeredHT;
    }

    public boolean isBonusCardBlocked() {
        return bonusCardBlocked;
    }

    public void setBonusCardBlocked(boolean bonusCardBlocked) {
        this.bonusCardBlocked = bonusCardBlocked;
    }

    public void returnCapturedLeader(String leaderName) {
        Leader leader = getLeaders().stream().filter(l -> l.getName().equals(leaderName)).findFirst().orElseThrow();
        removeLeader(leader);
        ledger.publish(leader.getName() + " has returned to " + Emojis.getFactionEmoji(leader.getOriginalFactionName()));
        Faction opponentFaction = game.getFaction(leader.getOriginalFactionName());
        opponentFaction.addLeader(leader);
        opponentFaction.getLedger().publish(leader.getName() + " has returned to you.");
        game.getTurnSummary().publish(Emojis.HARKONNEN + " has returned " + leader.getName() + " to " + Emojis.getFactionEmoji(leader.getOriginalFactionName()));
    }

    @Override
    public void performMentatPauseActions(boolean extortionTokenTriggered) {
        super.performMentatPauseActions(extortionTokenTriggered);
        if (traitorHand.size() == 3) {
            TraitorCard newTraitor = game.getTraitorDeck().pop();
            addTraitorCard(newTraitor);
            ledger.publish(newTraitor.getEmojiNameAndStrengthString() + " is your new Traitor.");
            game.getTurnSummary().publish(Emojis.HARKONNEN + " has drawn a new Traitor.");
        }
    }
}
