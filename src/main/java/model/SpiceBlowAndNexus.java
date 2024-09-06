package model;

import constants.Emojis;
import model.factions.Faction;
import model.factions.FremenFaction;
import net.dv8tion.jda.internal.utils.tuple.Pair;

import java.util.ArrayList;
import java.util.List;

public class SpiceBlowAndNexus {
    private boolean bothDecksDrawn;
    private boolean fremenRidesComplete = true;
    private boolean harvesterResolved = true;

    SpiceBlowAndNexus(Game game) {
        game.getTurnSummary().publish("**Turn " + game.getTurn() + " Spice Blow Phase**");
        game.setPhaseForWhispers("Turn " + game.getTurn() + " Spice Blow Phase\n");
        Pair<SpiceCard, Integer> spiceBlow = game.drawSpiceBlow("A");
        checkOnHarvester(game, spiceBlow);
        if (game.hasFaction("Fremen"))
            fremenRidesComplete = !((FremenFaction) game.getFaction("Fremen")).hasRidesRemaining();
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
        if (!bothDecksDrawn && fremenRidesComplete && harvesterResolved) {
            Pair<SpiceCard, Integer> spiceBlow = game.drawSpiceBlow("B");
            checkOnHarvester(game, spiceBlow);
            bothDecksDrawn = true;
            fremenRidesComplete = fremen == null || !fremen.hasRidesRemaining();
        }
        return bothDecksDrawn && fremenRidesComplete;
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

    public boolean isPhaseComplete() {
        return bothDecksDrawn && fremenRidesComplete && harvesterResolved;
    }
}
