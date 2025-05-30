package model.factions;

import constants.Emojis;
import enums.UpdateType;
import model.Game;
import model.Leader;
import model.Territory;
import model.TraitorCard;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.stream.IntStream;

public class HarkonnenFaction extends Faction {
    private boolean bonusCardBlocked;
    protected boolean nexusBetrayalTraitorNeeded;

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

    public void mulliganTraitorHand() {
        game.getTraitorDeck().addAll(traitorHand);
        traitorHand.clear();
        Collections.shuffle(game.getTraitorDeck());
        IntStream.range(0, 4).forEach(j -> game.drawCard("traitor deck", "Harkonnen"));
        game.getTurnSummary().publish(emoji + " mulliganed their Traitor Hand.");
    }

    public boolean isBonusCardBlocked() {
        return bonusCardBlocked;
    }

    public void setBonusCardBlocked(boolean bonusCardBlocked) {
        this.bonusCardBlocked = bonusCardBlocked;
    }

    public void keepCapturedLeader(String factionName, String leaderName) {
        Faction faction = game.getFaction(factionName);
        Leader leader = faction.getLeader(leaderName).orElseThrow();

        addLeader(leader);
        faction.removeLeader(leader);

        if (leader.getSkillCard() != null)
            game.getTurnSummary().publish(emoji + " has captured the " + faction.getEmoji() + " skilled leader, " + leaderName + " the " + leader.getSkillCard().name()+ ".");
        else
            game.getTurnSummary().publish(emoji + " has captured a Leader from " + faction.getEmoji());

        faction.getChat().publish(leaderName + " has been captured by the treacherous " + Emojis.HARKONNEN + "!");
        faction.getLedger().publish(leaderName + " has been captured by the treacherous " + Emojis.HARKONNEN + "!");
        ledger.publish("You have captured " + leaderName + ".");
        chat.reply("You kept " + leaderName);
    }

    public void killCapturedLeader(String factionName, String leaderName) {
        Faction faction = game.getFaction(factionName);
        Leader leader = faction.getLeader(leaderName).orElseThrow();
        faction.removeLeader(leader);

        addSpice(2, "killing " + leaderName);

        if (leader.getSkillCard() != null) {
            game.getLeaderSkillDeck().add(leader.getSkillCard());
            leader.removeSkillCard();
            game.getTurnSummary().publish(emoji + " has killed the " + faction.getEmoji() + " skilled leader, " + leaderName + ", for 2 " + Emojis.SPICE);
        } else
            game.getTurnSummary().publish(emoji + " has killed the " + faction.getEmoji() + " leader for 2 " + Emojis.SPICE);

        game.getLeaderTanks().add(leader);
        leader.setFaceDown(true);
        BTFaction bt = game.getBTFactionOrNull();
        if (bt != null) {
            bt.setUpdated(UpdateType.MISC_BACK_OF_SHIELD);
            bt.getChat().publish(leader.getEmoiNameAndValueString() + " is face down in the tanks.");
        }

        faction.getChat().publish(leader.getName() + " has been killed by the treacherous " + Emojis.HARKONNEN + "!");
        faction.getLedger().publish(leader.getName() + " has been killed by the treacherous " + Emojis.HARKONNEN + "!");
        setUpdated(UpdateType.MAP);
        chat.publish("You killed " + leaderName);
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

    public void nexusCardBetrayal(String traitorName) {
        LinkedList<TraitorCard> traitorDeck = game.getTraitorDeck();
        TraitorCard traitorCard = traitorHand.stream()
                .filter(t -> t.getName().equalsIgnoreCase(traitorName))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Harkonnen does not have " + traitorName + " as a Traitor."));
        removeTraitorCard(traitorCard);
        nexusBetrayalTraitorNeeded = true;
        traitorDeck.add(traitorCard);
        Collections.shuffle(traitorDeck);
        ledger.publish(traitorName + " has been shuffled back into the Traitor Deck.");
        game.getTurnSummary().publish(emoji + " loses " + traitorName + " and will draw a new Traitor in Mentat Pause.");
    }

    @Override
    public void performMentatPauseActions(boolean extortionTokenTriggered) {
        super.performMentatPauseActions(extortionTokenTriggered);
        if (nexusBetrayalTraitorNeeded) {
            TraitorCard newTraitor = game.getTraitorDeck().pop();
            addTraitorCard(newTraitor);
            ledger.publish(newTraitor.getEmojiNameAndStrengthString() + " is your new Traitor.");
            game.getTurnSummary().publish(Emojis.HARKONNEN + " has drawn a new Traitor.");
            nexusBetrayalTraitorNeeded = false;
        }
    }
}
