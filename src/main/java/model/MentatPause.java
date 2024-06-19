package model;

import constants.Emojis;
import enums.ChoamInflationType;
import enums.UpdateType;
import exceptions.InvalidGameStateException;
import model.factions.BTFaction;
import model.factions.ChoamFaction;
import model.factions.Faction;
import model.factions.MoritaniFaction;

import java.util.ArrayList;
import java.util.List;

public class MentatPause {
    List<String> declinedExtortion = null;
    List<String> wouldPayExtortion = null;
    boolean extortionActive = false;

    public MentatPause(Game game) {
        List<DuneChoice> choices;
        for (Faction faction : game.getFactions()) {
            int spice = faction.getSpice();
            if (faction.isDecliningCharity() && spice < 2)
                faction.getChat().publish("You have only " + spice + " " + Emojis.SPICE + " but are declining CHOAM charity.\nYou must change this in your info channel if you want to receive charity. " + faction.getPlayer());
        }

        try {
            BTFaction bt = (BTFaction) game.getFaction("BT");
            bt.getChat().publish("Would you like to swap a Face Dancer? " + bt.getPlayer());
        } catch (IllegalArgumentException e) {
            // BT is not in the game
        }

        ChoamFaction choam = null;
        try {
            choam = (ChoamFaction) game.getFaction("CHOAM");
        } catch (IllegalArgumentException e) {
            // CHOAM is not in the game
        }
        if (choam != null) {
            if (choam.getFirstInflationRound() == 0) {
                choices = new ArrayList<>();
                choices.add(new DuneChoice("inflation-double", "Yes, Double side up"));
                choices.add(new DuneChoice("inflation-cancel", "Yes, Cancel side up"));
                choices.add(new DuneChoice("inflation-not-yet", "No"));
                choam.getChat().publish("Would you like to set inflation? " + choam.getPlayer(), choices);
            } else {
                int doubleRound = choam.getFirstInflationRound();
                if (choam.getFirstInflationType() == ChoamInflationType.CANCEL) doubleRound++;

                if (doubleRound == game.getTurn() + 1)
                    game.getTurnSummary().publish("No bribes may be made while the " + Emojis.CHOAM + " inflation token is Double side up.");
                else if (doubleRound == game.getTurn())
                    game.getTurnSummary().publish("Bribes may be made again. The Inflation Token is no longer Double side up.");
            }
        }

        try {
            MoritaniFaction moritani = (MoritaniFaction) game.getFaction("Moritani");
            if (moritani.isNewAssassinationTargetNeeded()) {
                moritani.getTraitorHand().add(game.getTraitorDeck().pollFirst());
                game.getTurnSummary().publish(Emojis.MORITANI + " have drawn a new traitor.");
                moritani.setUpdated(UpdateType.MISC_BACK_OF_SHIELD);
            }
            moritani.setNewAssassinationTargetNeeded(false);
        } catch (IllegalArgumentException e) {
            // Moritani is not in the game
        }

        if (game.isExtortionTokenRevealed())
            startExtortion(game);
    }

    public void endPhase() throws InvalidGameStateException {
        if (extortionActive)
            throw new InvalidGameStateException("Extortion is not complete.");
    }

    public void startExtortion(Game game) {
        declinedExtortion = new ArrayList<>();
        game.getFactions().stream()
                .filter(f -> f.getSpice() < 3 && !(f instanceof MoritaniFaction))
                .forEach(f -> declinedExtortion.add(f.getName()));
        wouldPayExtortion = new ArrayList<>();
        extortionActive = true;
    }

    public boolean isExtortionActive() {
        return extortionActive;
    }

    private void checkForExtortionPayment(Game game) {
        if (!extortionActive) return;
        for (Faction f : game.getFactionsInStormOrder()) {
            if (f instanceof MoritaniFaction) continue;
            if (wouldPayExtortion.contains(f.getName())) {
                game.getTurnSummary().publish(f.getEmoji() + " pays 3 " + Emojis.SPICE + " to remove the Extortion token from the game.");
                f.subtractSpice(3, Emojis.MORITANI + " Extortion");
                Faction moritani = game.getFaction("Moritani");
                moritani.addSpice(3, f.getEmoji() + " paid Extortion");
                extortionActive = false;
                break;
            } else if (!declinedExtortion.contains(f.getName())) {
                break;
            }
        }
    }

    public void factionDeclinesExtortion(Game game, Faction faction) {
        if (faction instanceof MoritaniFaction) return;
        declinedExtortion.add(faction.getName());
        if (declinedExtortion.size() == game.getFactions().size() - 1) {
            ((MoritaniFaction)game.getFaction("Moritani")).getTerrorTokens().add("Extortion");
            game.getTurnSummary().publish("No faction paid Extortion. The token returns to " + Emojis.MORITANI);
            extortionActive = false;
        } else {
            checkForExtortionPayment(game);
        }
    }

    public void factionWouldPayExtortion(Game game, Faction faction) {
        if (faction instanceof MoritaniFaction) return;
        wouldPayExtortion.add(faction.getName());
        checkForExtortionPayment(game);
    }
}
