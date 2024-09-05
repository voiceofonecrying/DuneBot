package model;

import model.factions.FremenFaction;

public class SpiceBlowAndNexus {
    boolean bothDecksDrawn;
    boolean fremenRidesComplete = true;
    boolean harvesterResolved = true;

    SpiceBlowAndNexus(Game game) {
        game.getTurnSummary().publish("**Turn " + game.getTurn() + " Spice Blow Phase**");
        game.setPhaseForWhispers("Turn " + game.getTurn() + " Spice Blow Phase\n");
        game.drawSpiceBlow("A");
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
        if (!bothDecksDrawn && fremenRidesComplete) {
            game.drawSpiceBlow("B");
            bothDecksDrawn = true;
            fremenRidesComplete = fremen == null || !fremen.hasRidesRemaining();
        }
        return bothDecksDrawn && fremenRidesComplete;
    }

    public boolean isPhaseComplete() {
        return bothDecksDrawn && fremenRidesComplete && harvesterResolved;
    }
}
