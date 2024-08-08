package model;

import constants.Emojis;
import exceptions.InvalidGameStateException;
import model.factions.Faction;
import model.factions.MoritaniFaction;
import model.topics.DuneTopic;

import java.util.ArrayList;
import java.util.List;

public class MentatPause {
    List<String> declinedExtortion = null;
    List<String> wouldPayExtortion = null;
    boolean extortionTokenTriggered = false;
    boolean extortionActive = false;

    public void triggerExtortionToken() {
        extortionTokenTriggered = true;
    }

    public void startPhase(Game game) {
        DuneTopic turnSummary = game.getTurnSummary();
        turnSummary.publish("Turn " + game.getTurn() + " Mentat Pause Phase:");
        game.setPhaseForWhispers("Turn " + game.getTurn() + " Mentat Pause Phase\n");
        game.getFactions().forEach(faction -> faction.performMentatPauseActions(extortionTokenTriggered));
        if (extortionTokenTriggered)
            startExtortion(game);
    }

    public void endPhase() throws InvalidGameStateException {
        if (extortionActive)
            throw new InvalidGameStateException("Extortion is not complete.");
    }

    public void startExtortion(Game game) {
        game.getTurnSummary().publish("The Extortion token will be returned to " + Emojis.MORITANI + " unless someone pays 3 " + Emojis.SPICE + " to remove it from the game.");
        declinedExtortion = new ArrayList<>();
        game.getFactions().stream()
                .filter(f -> f.getSpice() < 3 && !(f instanceof MoritaniFaction))
                .forEach(f -> declinedExtortion.add(f.getName()));
        wouldPayExtortion = new ArrayList<>();
        extortionActive = true;
    }

    public boolean isExtortionInactive() {
        return !extortionActive;
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
