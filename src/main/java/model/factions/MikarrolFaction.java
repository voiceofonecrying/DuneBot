package model.factions;

import constants.Emojis;
import controller.DiscordGame;
import exceptions.InvalidGameStateException;
import model.DuneChoice;
import model.Game;
import model.Territory;
import model.TleilaxuTanks;
import model.TreacheryCard;
import model.factions.EmperorFaction;
import model.factions.Faction;
import model.TleilaxuTanks;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

public class MikarrolFaction extends Faction {
    private boolean raidersBlocked;
    private int slaversAmount;
    private int allySlaversAmount;
    private String raidTarget;
    private String raidedCardName;

    public MikarrolFaction(String player, String userName) throws IOException {
        super("Mikarrol", player, userName);

        this.spice = 12;
        this.freeRevival = 2;
        this.emoji = Emojis.getFactionEmoji(name);
        this.forceEmoji = Emojis.getForceEmoji(name);
        this.highThreshold = 6;
        this.lowThreshold = 5;
        this.occupiedIncome = 2;
        this.slaversAmount = 0;
        this.allySlaversAmount = 0;
        this.homeworld = "Kublai XII";
        this.handLimit = 5;
    }

    @Override
    public void joinGame(@NotNull Game game) {
        super.joinGame(game);
        Territory kublai = game.getTerritories().addHomeworld(game, homeworld, name);
        kublai.addForces(name, 20);
        game.getHomeworlds().put(name, homeworld);
    }

    @Override
    public void performEndOfPhaseActions() {
        try {
            slavers();
            allySlavers();
		}
        catch(InvalidGameStateException i) {}
        returnToNormalHandSize();
	}

    @Override
    public void setAlly(String ally) {
        super.setAlly(ally);
        allySlaversAmount = 0;
    }

    public boolean isRaidersBlocked() {
        return raidersBlocked;
    }

    public int getSlaversAmount() {
        return slaversAmount;
    }

    public int getAllySlaversAmount() {
        return allySlaversAmount;
    }

    public void setRaidersBlocked(boolean raidersBlocked) {
        this.raidersBlocked = raidersBlocked;
    }

    public void setSlaversAmount(int slaversAmount) throws InvalidGameStateException {
        if (slaversAmount > getTotalReservesStrength())
            throw new InvalidGameStateException("You do not have enough forces in reserves");
        this.slaversAmount = slaversAmount;
    }

    public void setAllySlaversAmount(int slaversAmount) throws InvalidGameStateException {
        if (slaversAmount > game.getFaction(getAlly()).getTotalReservesStrength())
            throw new InvalidGameStateException("You do not have enough forces in reserves");
        this.allySlaversAmount = slaversAmount;
    }

    public void setInfiniteHand() {
        handLimit = 100;
    }

    public void returnToNormalHandSize() {
        if (handLimit != 100)
            return;

        if (treacheryHand.size() > 5) {
            Set<String> cardNames = new HashSet<>(treacheryHand.stream().map(TreacheryCard::name).toList());
            List<DuneChoice> choices = cardNames.stream().map(c -> new DuneChoice("mikarrol-discard-" + c, c)).collect(Collectors.toList());
            choices.add(new DuneChoice("secondary", "mikarrol-finished-discarding", "Done"));
            chat.publish("You must discard " + Emojis.TREACHERY + " cards until you have only 5 " + player, choices);
        }
        else {
            handLimit = 5;
            chat.publish("Your maximum hand size has returned to 5 at the end of the Battles phase.");
        }
    }

    public void offerRaidChoice(Faction loser) {
        List<TreacheryCard> eligibleCards = loser.getTreacheryHand();
        if (raidersBlocked) {
            raidersBlocked = false;
            return;
        }

        if (eligibleCards.isEmpty()) {
            game.getTurnSummary().publish(loser.getEmoji() + " has no " + Emojis.TREACHERY + " cards to raid.");
        } else {
            raidTarget = loser.getName();
            List<DuneChoice> choices = new ArrayList<>();
            choices.add(new DuneChoice("mikarrol-raid", "Raid"));
            choices.add(new DuneChoice("secondary", "mikarrol-decline-raid", "Pass"));
            chat.publish("Will you raid " + loser.getEmoji() + "? " + getPlayer(), choices);
        }
    }

    public void raid(String factionName) throws InvalidGameStateException {
        raidTarget = factionName;
        raid();
    }

    public void raid() throws InvalidGameStateException {
        if (raidTarget == null)
            throw new InvalidGameStateException("No faction is selected to raid currently.");
        Faction faction = game.getFaction(raidTarget);
        List<TreacheryCard> raidedHand = faction.getTreacheryHand();
        int cardCount = raidedHand.size();
        if (cardCount < 1) {
            chat.reply("Your raiders have returned, revealing the enemy had nothing to steal.");
            endRaid();
            return;
        }

        Random r = new Random();
        raidedCardName = raidedHand.get(r.nextInt(cardCount)).name();
        TreacheryCard raidedCard = faction.removeTreacheryCardWithoutDiscard(raidedCardName);
        chat.reply("You will raid for a treachery card");
        addTreacheryCard(raidedCard);
        game.getTurnSummary().publish(emoji + " has raided the treachery hand of " + faction.getEmoji() + ".");
        faction.getChat().publish(raidedCardName + " has been stolen by " + emoji + " raiders!");
        faction.getLedger().publish(raidedCardName + " stolen by " + emoji);
        ledger.publish("Your raiders have returned, stealing " + raidedCardName + " from " + faction.getEmoji() + "!");
        List<DuneChoice> choices = new ArrayList<>();
        choices.add(new DuneChoice("mikarrol-keep-raid-card", "Keep"));
        choices.add(new DuneChoice("secondary", "mikarrol-discard-raid-card", "Discard"));
        chat.publish("Will you keep " + raidedCardName + "? " + getPlayer(), choices);
    }

    public void keepRaidedCard() {
        chat.publish("You will keep " + raidedCardName + " from the raid");
        endRaid();
    }

    public void discardRaidedCard() {
        discard(raidedCardName, "Discarding card collected in raid on " + game.getFaction(raidTarget).getEmoji());
        chat.publish("You chose not to keep the raided " + raidedCardName);
        endRaid();
    }

    public void declineRaid() {
        game.getTurnSummary().publish(emoji + " will not raid " + game.getFaction(raidTarget).getEmoji() + ".");
        chat.publish("You will not raid");
        endRaid();
    }

    private void endRaid() {
        raidTarget = null;
        raidedCardName = null;
    }

    public void replenishment(boolean wonBattle) {
        int numForces = 2;
        int strengthInTanks = game.getTleilaxuTanks().getForceStrength("Mikarrol");
        if (strengthInTanks < 1)
            return;
        if (wonBattle)
            numForces = 4;
        if (isHighThreshold)
            ++numForces;
        if (numForces > strengthInTanks)
            numForces = strengthInTanks;
        game.reviveForces(this, false, numForces, 0);
        game.getTurnSummary().publish(emoji + " acquire " + numForces + " troops to replenish their forces after battle.");
    }

    public void slavers() throws InvalidGameStateException {
        if (slaversAmount < 1)
            return;
        if (slaversAmount > getTotalReservesStrength())
            slaversAmount = getTotalReservesStrength();
        addSpice(slaversAmount, " forces sold to slavers.");
        removeReserves(slaversAmount);
        game.getTleilaxuTanks().addForces(name, slaversAmount);
        game.getTurnSummary().publish(emoji + " sell " + slaversAmount + " forces to slavers.");
        chat.reply("You sold " + slaversAmount + " forces.");
        setSlaversAmount(0);
    }

    public void allySlavers() throws InvalidGameStateException {
        if (allySlaversAmount < 1 || !hasAlly())
            return;
        Faction allyFaction = game.getFaction(ally);
        if (allySlaversAmount > allyFaction.getTotalReservesStrength())
            allySlaversAmount = allyFaction.getTotalReservesStrength();
        int specialAmount = 0;
        int normalAmount = allySlaversAmount;
        if (allySlaversAmount > allyFaction.getReservesStrength()) {
            normalAmount = allyFaction.getReservesStrength();
            specialAmount = allyFaction.getSpecialReservesStrength();
        }

        allyFaction.removeReserves(normalAmount);
        game.getTleilaxuTanks().addForces(ally, normalAmount);
        if (specialAmount > 0) {
            if (allyFaction instanceof EmperorFaction) {
                EmperorFaction emp = (EmperorFaction)allyFaction;
                emp.removeSpecialReserves(specialAmount);
			} else
                allyFaction.removeSpecialReserves(specialAmount);
			game.getTleilaxuTanks().addForces(ally + "*", specialAmount);
        }
        allyFaction.addSpice(allySlaversAmount, " forces sold to slavers");
        game.getTurnSummary().publish(allyFaction.getEmoji() + " sell " + allySlaversAmount + " forces to slavers, through " + emoji + ".");
        allyFaction.getChat().reply("You sold " + allySlaversAmount + " forces.");
        setAllySlaversAmount(0);
    }
}
