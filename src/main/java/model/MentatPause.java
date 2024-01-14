package model;

import constants.Emojis;
import exceptions.InvalidGameStateException;
import model.factions.Faction;
import model.factions.MoritaniFaction;

import java.util.ArrayList;
import java.util.List;

public class MentatPause {
    List<String> declinedExtortion = null;
    List<String> wouldPayExtortion = null;
    boolean extortionActive = false;

    public MentatPause(Game game) {
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
                f.subtractSpice(3);
                f.spiceMessage(3, Emojis.MORITANI + " Extortion", false);
                Faction moritani = game.getFaction("Moritani");
                moritani.addSpice(3);
                moritani.spiceMessage(3, f.getEmoji() + " paid Extortion", true);
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
