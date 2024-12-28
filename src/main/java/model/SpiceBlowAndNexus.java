package model;

import constants.Emojis;
import enums.GameOption;
import model.factions.Faction;
import model.factions.FremenFaction;
import net.dv8tion.jda.internal.utils.tuple.Pair;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SpiceBlowAndNexus {
    private int numDecksDrawn;
    private boolean fremenRidesComplete = true;
    private boolean harvesterResolved = true;
    private boolean thumperResolved = true;
    private boolean thumperWasPlayed;

    SpiceBlowAndNexus(Game game) throws IOException {
        checkOnThumper(game, "A");
        nextStep(game);
    }

    public boolean nextStep(Game game) throws IOException {
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
            Collections.shuffle(game.getQuotes().get(2));
            game.getTurnSummary().publish(game.getQuotes().get(2).removeFirst());
            game.getTurnSummary().publish("**Turn " + game.getTurn() + " Spice Blow Phase**");
            game.setPhaseForWhispers("Turn " + game.getTurn() + " Spice Blow Phase\n");
            Pair<SpiceCard, Integer> spiceBlow = game.drawSpiceBlow("A", thumperWasPlayed);
            thumperWasPlayed = false;
            numDecksDrawn++;
            checkOnHarvester(game, spiceBlow);
            fremenRidesComplete = fremen == null || !fremen.hasRidesRemaining();
        } else if (numDecksDrawn == 1 && fremenRidesComplete && harvesterResolved) {
            if (game.hasGameOption(GameOption.THUMPER_ON_DECK_B))
                checkOnThumper(game, "B");
            if (thumperResolved) {
                Pair<SpiceCard, Integer> spiceBlow = game.drawSpiceBlow("B", thumperWasPlayed);
                numDecksDrawn++;
                checkOnHarvester(game, spiceBlow);
                fremenRidesComplete = fremen == null || !fremen.hasRidesRemaining();
            }
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

    public void checkOnThumper(Game game, String deck) {
        if (game.getTurn() >= 2) {
            for (Faction faction : game.getFactions()) {
                if (faction.hasTreacheryCard("Thumper")) {
                    thumperResolved = false;
                    List<DuneChoice> choices = new ArrayList<>();
                    choices.add(new DuneChoice("spiceblow-thumper-yes", "Yes"));
                    choices.add(new DuneChoice("secondary", "spiceblow-thumper-no", "No"));
                    String territoryName = game.getSpiceDiscardA().getLast().name();
                    if (deck.equals("B"))
                        territoryName = game.getSpiceDiscardB().getLast().name();
                    faction.getChat().publish("Would you like to play Thumper in " + territoryName + "? " + faction.getPlayer(), choices);
                }
            }
        }
    }

    public boolean isThumperActive() {
        return !thumperResolved;
    }

    public void playThumper(Game game, Faction faction) {
        thumperWasPlayed = true;
        thumperResolved = true;
        faction.discard("Thumper", "to summon Shai-Hulud");
        String territoryName = game.getSpiceDiscardA().getLast().name();
        if (numDecksDrawn == 1)
            territoryName = game.getSpiceDiscardB().getLast().name();
        game.getTurnSummary().publish(game.getTerritory(territoryName).shaiHuludAppears(game, "Shai-Hulud", true));
        faction.getChat().reply("You will play Thumper in " + territoryName + ".");
    }

    public void declineThumper(Game game, Faction faction) {
        thumperWasPlayed = false;
        thumperResolved = true;
        String territoryName = game.getSpiceDiscardA().getLast().name();
        if (numDecksDrawn == 1)
            territoryName = game.getSpiceDiscardB().getLast().name();
        faction.getChat().reply("You will not play Thumper in " + territoryName + ".");
    }

    public boolean isPhaseComplete() {
        return numDecksDrawn == 2 && fremenRidesComplete && harvesterResolved;
    }
}
