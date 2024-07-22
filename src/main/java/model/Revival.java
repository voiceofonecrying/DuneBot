package model;

import constants.Emojis;
import exceptions.InvalidGameStateException;
import model.factions.*;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class Revival {
    private String recruitsHolder;
    private boolean recruitsAsked;
    private boolean recruitsDeclined;
    private boolean recruitsInPlay;
    private boolean btAskedAboutLimits;
    private boolean cyborgRevivalComplete;

    public Revival(Game game) throws InvalidGameStateException {
        List<Faction> factions = game.getFactionsWithTreacheryCard("Recruits");
        recruitsAsked = false;
        recruitsDeclined = false;
        recruitsInPlay = false;
        btAskedAboutLimits = false;
        cyborgRevivalComplete = false;
        if (!factions.isEmpty()) {
            recruitsHolder = factions.getFirst().getName();
            askAboutRecruits(game);
        }
    }

    /**
     * Perform pre-steps for revival and allow auto-advance to free revivals if BT are not in the game and Recruits is not played.
     *
     * @return true if pre-steps are completed and the game can advance, false if waiting on Recruits or BT to set revival limits.
     */
    public boolean performPreSteps(Game game) throws InvalidGameStateException {
        if (recruitsAsked && !recruitsDeclined && !recruitsInPlay)
            throw new InvalidGameStateException(recruitsHolder + " must decide if they will play Recruits before the game can be advanced.");
        else if (btAskedAboutLimits) {
            BTFaction bt = (BTFaction) game.getFaction("BT");
            List<String> factionsNeedingLimits = bt.getFactionsNeedingRevivalLimit();
            if (factionsNeedingLimits.isEmpty()) {
                return true;
            } else {
                String names = String.join(", ", factionsNeedingLimits);
                throw new InvalidGameStateException("BT must set revival limits for the following factions before the game can be advanced.\n" + names);
            }
        }
        else if (!recruitsInPlay)
            return !askAboutRevivalLimits(game);
        else
            return true;
    }

    public void askAboutRecruits(Game game) throws InvalidGameStateException {
        Faction factionWithRecruits;
        if (recruitsHolder == null)
            throw new InvalidGameStateException("No faction holds Recruits. Noboday can be asked about playing it.");
        factionWithRecruits = game.getFaction(recruitsHolder);
        List<DuneChoice> choices = new LinkedList<>();
        choices.add(new DuneChoice("recruits-yes", "Yes"));
        choices.add(new DuneChoice("recruits-no", "No"));
        factionWithRecruits.getChat().publish("Will you play Recruits? " + factionWithRecruits.getPlayer(), choices);
        recruitsAsked = true;
    }

    /**
     * Gives BT player choices for revival limits and tags the player.
     *
     * @return true if BT needs to set any revival limits, otherwise false.
     */
    public boolean askAboutRevivalLimits(Game game) {
        if (game.getTurn() > 1 && game.hasFaction("BT")) {
            BTFaction bt = (BTFaction) game.getFaction("BT");
            bt.getChat().publish("Please set revival rates for each faction." + bt.getPlayer());
            List<String> limitsNotNeededMessages = new ArrayList<>();
            game.getFactions().stream().filter(faction -> !(faction instanceof BTFaction)).forEach(faction -> {
                TleilaxuTanks tanks = game.getTleilaxuTanks();
                int regularInTanks = tanks.getForceStrength(faction.getName());
                int starredInTanks = tanks.getForceStrength(faction.getName() + "*");
                int starredForRevival = 0;
                if (faction instanceof EmperorFaction || faction instanceof FremenFaction)
                    starredForRevival = starredInTanks == 0 ? 0 : 1;
                if (faction instanceof ChoamFaction) {
                    limitsNotNeededMessages.add(faction.getEmoji() + " defies " + Emojis.BT + " attempts to constrain their revivals!");
                } else if (regularInTanks + starredForRevival > 3) {
                    List<DuneChoice> choices = new LinkedList<>();
                    choices.add(new DuneChoice("bt-revival-rate-set-" + faction.getName() + "-3", "3" + (faction.getMaxRevival() == 3 ? " (no change)" : "")));
                    choices.add(new DuneChoice("bt-revival-rate-set-" + faction.getName() + "-4", "4" + (faction.getMaxRevival() == 4 ? " (no change)" : "")));
                    choices.add(new DuneChoice("bt-revival-rate-set-" + faction.getName() + "-5", "5" + (faction.getMaxRevival() == 5 ? " (no change)" : "")));
                    bt.getChat().publish(faction.getEmoji(), choices);
                    bt.addFactionNeedingRevivalLimit(faction.getName());
                } else {
                    if (regularInTanks + starredInTanks == 0) {
                        limitsNotNeededMessages.add(faction.getEmoji() + " has no forces in the tanks.");
                    } else {
                        String troopsInTanks = "";
                        if (regularInTanks > 0)
                            troopsInTanks += regularInTanks + " " + Emojis.getForceEmoji(faction.getName()) + " ";
                        if (starredForRevival > 0)
                            troopsInTanks += starredForRevival + " " + Emojis.getForceEmoji(faction.getName() + "*") + " ";
                        limitsNotNeededMessages.add(faction.getEmoji() + " has only " + troopsInTanks + " revivable forces.");
                    }
                }
            });
            String leaveAllTheSameMessage = String.join("\n", limitsNotNeededMessages);
            leaveAllTheSameMessage += "\n\nOr leave all the same (any changes made above will be retained)";
            bt.getChat().publish(leaveAllTheSameMessage, List.of(new DuneChoice("secondary", "bt-revival-rate-no-change", "Leave limits as they are")));
            btAskedAboutLimits = true;
            return !bt.hasSetAllRevivalLimits();
        }
        return false;
    }

    public String getRecruitsHolder() {
        return recruitsHolder;
    }

    public void declineRecruits() {
        recruitsDeclined = true;
    }

    public void playRecruits() {
        recruitsInPlay = true;
    }

    public boolean isRecruitsDecisionNeeded() {
        return recruitsAsked && !recruitsDeclined && !recruitsInPlay;
    }

    public boolean isRecruitsInPlay() {
        return recruitsInPlay;
    }

    public void setRecruitsInPlay(boolean recruitsInPlay) {
        this.recruitsInPlay = recruitsInPlay;
    }

    public boolean isCyborgRevivalComplete() {
        return cyborgRevivalComplete;
    }

    public void setCyborgRevivalComplete(boolean cyborgRevivalComplete) {
        this.cyborgRevivalComplete = cyborgRevivalComplete;
    }
}
