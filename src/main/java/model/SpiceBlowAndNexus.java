package model;

import constants.Emojis;
import model.factions.Faction;
import model.factions.FremenFaction;
import net.dv8tion.jda.internal.utils.tuple.Pair;

import java.util.ArrayList;
import java.util.List;

public class SpiceBlowAndNexus {
    private int numDecksDrawn;
    private boolean fremenRidesComplete = true;
    private boolean harvesterResolved = true;
    private boolean thumperResolved = true;
    private boolean thumperWasPlayed;

    SpiceBlowAndNexus(Game game) {
        game.getTurnSummary().publish("**Turn " + game.getTurn() + " Spice Blow Phase**");
        game.setPhaseForWhispers("Turn " + game.getTurn() + " Spice Blow Phase\n");
        checkOnThumper(game);
        nextStep(game);
    }

    public boolean nextStep(Game game) {
        FremenFaction fremen = null;
        fremenRidesComplete = true;
        if (game.hasFaction("Fremen")) {
            fremen = (FremenFaction) game.getFaction("Fremen");
            if (fremen.hasRidesRemaining()) {
                fremenRidesComplete = false;
                fremen.presentNextWormRideChoices();
            }
        }
        if (numDecksDrawn == 0 && thumperResolved) {
            Pair<SpiceCard, Integer> spiceBlow = game.drawSpiceBlow("A", thumperWasPlayed);
            thumperWasPlayed = false;
            numDecksDrawn++;
            checkOnHarvester(game, spiceBlow);
            fremenRidesComplete = fremen == null || !fremen.hasRidesRemaining();
        } else if (numDecksDrawn == 1 && fremenRidesComplete && harvesterResolved) {
            Pair<SpiceCard, Integer> spiceBlow = game.drawSpiceBlow("B");
            numDecksDrawn++;
            checkOnHarvester(game, spiceBlow);
            fremenRidesComplete = fremen == null || !fremen.hasRidesRemaining();
        }
        return isPhaseComplete();
    }

    public void checkOnHarvester(Game game, Pair<SpiceCard, Integer> spiceBlow) {
        SpiceCard spiceCard = spiceBlow.getLeft();
        int spiceMultiplier = spiceBlow.getRight();
        if (game.getStorm() == spiceCard.sector())
            return;
        for (Faction faction : game.getFactions()) {
            if (faction.hasTreacheryCard("Harvester")) {
                harvesterResolved = false;
                List<DuneChoice> choices = new ArrayList<>();
                choices.add(new DuneChoice("play-harvester-yes-" + spiceCard.spice() + "-" + spiceMultiplier + "-" + spiceCard.name(), "Yes"));
                choices.add(new DuneChoice("secondary", "play-harvester-no", "No"));
                faction.getChat().publish("Would you like to play Harvester to double the " + Emojis.SPICE + " Blow? " + faction.getPlayer(), choices);
            }
        }
    }

    public boolean isHarvesterActive() {
        return !harvesterResolved;
    }

    public void resolveHarvester() {
        this.harvesterResolved = true;
    }

    public void checkOnThumper(Game game) {
        if (game.getTurn() >= 2) {
            for (Faction faction : game.getFactions()) {
                if (faction.hasTreacheryCard("Thumper")) {
                    thumperResolved = false;
                    List<DuneChoice> choices = new ArrayList<>();
                    choices.add(new DuneChoice("spiceblow-thumper-yes-" + "A", "Yes"));
                    choices.add(new DuneChoice("secondary", "spiceblow-thumper-no", "No"));
                    String territoryName = game.getSpiceDiscardA().getLast().name();
                    faction.getChat().publish("Would you like to play Thumper in " + territoryName + "? " + faction.getPlayer(), choices);
                }
            }
        }
    }

    public boolean isThumperActive() {
        return !thumperResolved;
    }

    public void resolveThumper(boolean thumperWasPlayed) {
        this.thumperWasPlayed = thumperWasPlayed;
        this.thumperResolved = true;
    }

    public boolean isPhaseComplete() {
        return numDecksDrawn == 2 && fremenRidesComplete && harvesterResolved;
    }
}
