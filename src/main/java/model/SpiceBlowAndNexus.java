package model;

import constants.Emojis;
import enums.GameOption;
import enums.Ruleset;
import model.factions.Faction;
import model.factions.FremenFaction;

import static controller.commands.CommandOptions.territory;
import net.dv8tion.jda.internal.utils.tuple.Pair;

import static controller.commands.CommandOptions.territory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SpiceBlowAndNexus {
    private int numDecksDrawn;
    private boolean fremenRidesComplete = true;
    private boolean harvesterResolved = true;
    private boolean thumperResolved = true;
    private boolean miningLaserResolved = true;
    private boolean spiceHeistResolved = true;
    private boolean thumperWasPlayed;
    private SpiceCard spiceCard;
    private DoubleSpiceCard doubleSpiceCard;
    private int spiceMultiplier;

    SpiceBlowAndNexus(Game game) throws IOException {
        checkOnMiningLaser(game);
        checkOnThumper(game, "A");
        nextStep(game);
    }

    public boolean nextStep(Game game) throws IOException {
        FremenFaction fremen = game.getFremenFactionOrNull();
        boolean isDouble = game.getRuleset() == Ruleset.GF9_KH;
        fremenRidesComplete = true;
        if (fremen != null && fremen.hasRidesRemaining()) {
            fremenRidesComplete = false;
            fremen.presentNextWormRideChoices();
        }
        if (isDouble && numDecksDrawn == 0 && thumperResolved && miningLaserResolved) {
            List<String> spiceBlowQuotes = game.getQuotes().get(2);
            if (!spiceBlowQuotes.isEmpty()) {
                Collections.shuffle(spiceBlowQuotes);
                game.getTurnSummary().publish(spiceBlowQuotes.removeFirst());
            }
            game.getTurnSummary().publish("**Turn " + game.getTurn() + " Spice Blow Phase**");
            game.setPhaseForWhispers("Turn " + game.getTurn() + " Spice Blow Phase\n");
            Pair<DoubleSpiceCard, Integer> spiceBlow = game.drawDoubleSpiceBlow(thumperWasPlayed);
            doubleSpiceCard = spiceBlow.getLeft();
            spiceMultiplier = spiceBlow.getRight();
            thumperWasPlayed = false;
            // checkOnHarvester(game); - not available for double spice for now
            numDecksDrawn = 2;
            fremenRidesComplete = fremen == null || !fremen.hasRidesRemaining();
        } else if (numDecksDrawn == 0 && thumperResolved && miningLaserResolved) {
            List<String> spiceBlowQuotes = game.getQuotes().get(2);
            if (!spiceBlowQuotes.isEmpty()) {
                Collections.shuffle(spiceBlowQuotes);
                game.getTurnSummary().publish(spiceBlowQuotes.removeFirst());
            }
            game.getTurnSummary().publish("**Turn " + game.getTurn() + " Spice Blow Phase**");
            game.setPhaseForWhispers("Turn " + game.getTurn() + " Spice Blow Phase\n");
            Pair<SpiceCard, Integer> spiceBlow = game.drawSpiceBlow("A", thumperWasPlayed);
            spiceCard = spiceBlow.getLeft();
            spiceMultiplier = spiceBlow.getRight();
            thumperWasPlayed = false;
            numDecksDrawn++;
            checkOnHarvester(game);
            fremenRidesComplete = fremen == null || !fremen.hasRidesRemaining();
            if (game.hasGameOption(GameOption.THUMPER_ON_DECK_B))
                checkOnThumper(game, "B");
        } else if (numDecksDrawn == 1 && fremenRidesComplete && harvesterResolved) {
            if (thumperResolved) {
                Pair<SpiceCard, Integer> spiceBlow = game.drawSpiceBlow("B", thumperWasPlayed);
                spiceCard = spiceBlow.getLeft();
                spiceMultiplier = spiceBlow.getRight();
                numDecksDrawn++;
                checkOnHarvester(game);
                checkOnSpiceHeist(game);
                fremenRidesComplete = fremen == null || !fremen.hasRidesRemaining();
            }
        }
        return isPhaseComplete();
    }

    public boolean isMiningLaserActive() {
        return !miningLaserResolved;
    }

    public boolean isThumperActive() {
        return !thumperResolved;
    }

    public boolean isHarvesterActive() {
        return !harvesterResolved;
    }

    public boolean isSpiceHeistActive() {
        return !spiceHeistResolved;
    }

    private void checkOnMiningLaser(Game game) {
        for (Faction faction : game.getFactions()) {
            if (faction.hasTreacheryCard("Mining Laser")) {
                miningLaserResolved = false;
                List<DuneChoice> choices = new ArrayList<>();
                for (Territory territory : game.getTerritories().values()) {
                    String territoryName = territory.getTerritoryName();
                    List<String> sietches = List.of("Tuek's Sietch", "Habbanya Sietch", "Sietch Tabr");
                    if (sietches.contains(territoryName) && territory.hasActiveFaction(faction)) {
                        miningLaserResolved = false;
                        choices.add(new DuneChoice("iduali-mining-laser-yes-" + territoryName, territoryName));
                    }
                }

                if (!miningLaserResolved) {
                    choices.add(new DuneChoice("secondary", "iduali-mining-laser-no", "No"));
                    faction.getChat().publish("Would you like to play Mining Laser? " + faction.getPlayer(), choices);
                }
                break;
                // Only one faction could have Mining Laser at a time
            }
        }
    }

    public void playMiningLaser(Game game, Faction faction, Territory territory) {
        miningLaserResolved = true;
        String territoryName = territory.getTerritoryName();
        faction.getChat().reply("You will play Mining Laser in " + territoryName);
        faction.addSpice(10, "Experimental mining in " + territoryName);
        territory.setRock(false);
        territory.setMiningDamageToken(true);
        faction.removeTreacheryCardWithoutDiscard("Mining Laser");
        game.getTurnSummary().publish(territoryName + " has been sabotaged and is now vulnerable to Storm.\nMining Laser has been removed from the game.");
    }

    public void declineMiningLaser(Game game, Faction faction) {
        miningLaserResolved = true;
        faction.getChat().reply("You will not play Mining Laser.");
    }

    public void checkOnHarvester(Game game) {
        if (game.getStorm() == spiceCard.sector())
            return;
        for (Faction faction : game.getFactions()) {
            if (faction.hasTreacheryCard("Harvester")) {
                harvesterResolved = false;
                List<DuneChoice> choices = new ArrayList<>();
                choices.add(new DuneChoice("spiceblow-harvester-yes", "Yes"));
                choices.add(new DuneChoice("secondary", "spiceblow-harvester-no", "No"));
                faction.getChat().publish("Would you like to play Harvester to double the " + spiceCard.spice() * spiceMultiplier + " " + Emojis.SPICE + " Blow in " + spiceCard.name() + "? " + faction.getPlayer(), choices);
            }
        }
    }

    public void playHarvester(Game game, Faction faction) {
        this.harvesterResolved = true;
        String territoryName = spiceCard.name();
        int spice = spiceCard.spice();
        int blowSize = spice * spiceMultiplier;
        faction.discard("Harvester", "in " + territoryName + " to double the " + Emojis.SPICE + " Blow in " + territoryName);
        game.getTerritories().get(territoryName).addSpice(game, blowSize);
        game.getTurnSummary().publish(blowSize + " more " + Emojis.SPICE + " has been spotted in " + territoryName);
        faction.getChat().reply("You will play Harvester in " + territoryName);
    }

    public void declineHarvester(Faction faction) {
        this.harvesterResolved = true;
        faction.getChat().reply("You will not play Harvester");
    }

    public void checkOnThumper(Game game, String deck) {
        if (game.getTurn() >= 2) {
            for (Faction faction : game.getFactions()) {
                if (faction.hasTreacheryCard("Thumper")) {
                    thumperResolved = false;
                    List<DuneChoice> choices = new ArrayList<>();
                    choices.add(new DuneChoice("spiceblow-thumper-yes", "Yes"));
                    choices.add(new DuneChoice("secondary", "spiceblow-thumper-no", "No"));
                    String territoryName;
                    if (game.getRuleset() == Ruleset.GF9_KH)
                        territoryName = game.getDoubleSpiceDiscard().getLast().firstTerritoryName() + " and " + game.getDoubleSpiceDiscard().getLast().secondTerritoryName();
                    else if (deck.equals("B"))
                        territoryName = game.getSpiceDiscardB().getLast().name();
                    else
                        territoryName = game.getSpiceDiscardA().getLast().name();

                    faction.getChat().publish("Would you like to play Thumper in " + territoryName + "? " + faction.getPlayer(), choices);
                }
            }
        }
    }

    public void playThumper(Game game, Faction faction) {
        thumperWasPlayed = true;
        thumperResolved = true;
        faction.discard("Thumper", "to summon Shai-Hulud");
        String territoryName = game.getSpiceDiscardA().getLast().name();
        if (numDecksDrawn == 1)
            territoryName = game.getSpiceDiscardB().getLast().name();
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

    public void checkOnSpiceHeist(Game game) {
        for (Faction faction : game.getFactions()) {
            if (faction.hasTreacheryCard("Spice Heist")) {
                spiceHeistResolved = false;
                List<DuneChoice> choices = new ArrayList<>();
                choices.add(new DuneChoice("spiceblow-spice-heist-yes", "Yes"));
                choices.add(new DuneChoice("secondary", "spiceblow-spice-heist-no", "No"));
                faction.getChat().publish("Would you like to play Spice Heist to move " + Emojis.SPICE + "? " + faction.getPlayer(), choices);
                break;
            }
        }
    }

    public void playSpiceHeist(Game game, Faction faction) {
        this.spiceHeistResolved = true;
        faction.discard("Spice Heist");
        faction.getChat().reply("You will play Spice Heist. Where will the spice be taken, and from which territory?");
    }

    public void declineSpiceHeist(Faction faction) {
        this.spiceHeistResolved = true;
        faction.getChat().reply("You will not play Spice Heist");
    }

    public boolean isPhaseComplete() {
        return numDecksDrawn == 2 && fremenRidesComplete && harvesterResolved && spiceHeistResolved;
    }
}
