package model;

import constants.Emojis;
import enums.GameOption;
import enums.UpdateType;
import exceptions.InvalidGameStateException;
import model.factions.*;
import model.topics.DuneTopic;

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
    private boolean ecazAskedAboutAmbassadors;
    private boolean ecazAmbassadorsToBePlaced;

    public Revival(Game game) throws InvalidGameStateException {
        List<Faction> factions = game.getFactionsWithTreacheryCard("Recruits");
        recruitsAsked = false;
        recruitsDeclined = false;
        recruitsInPlay = false;
        btAskedAboutLimits = false;
        cyborgRevivalComplete = false;
        ecazAskedAboutAmbassadors = false;
        ecazAmbassadorsToBePlaced = false;
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
        choices.add(new DuneChoice("revival-recruits-yes", "Yes"));
        choices.add(new DuneChoice("revival-recruits-no", "No"));
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
            bt.getChat().publish("Please set revival rates for each faction. " + bt.getPlayer());
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
                } else if (regularInTanks + starredInTanks == 0) {
                    limitsNotNeededMessages.add(faction.getEmoji() + " has no forces in the tanks.");
                } else {
                    limitsNotNeededMessages.add(faction.getEmoji() + " has only " + faction.forcesString(regularInTanks, starredForRevival) + " revivable forces.");
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

    public void startRevivingForces(Game game) throws InvalidGameStateException {
        if (isRecruitsDecisionNeeded())
            throw new InvalidGameStateException(recruitsHolder + " must decide if they will play recruits before the game can be advanced.");
        boolean btWasHighThreshold = false;
        try {
            BTFaction bt = (BTFaction) game.getFaction("BT");
            List<String> factionsNeedingLimits = bt.getFactionsNeedingRevivalLimit();
            if (!factionsNeedingLimits.isEmpty()) {
                String names = String.join(", ", factionsNeedingLimits);
                throw new InvalidGameStateException("BT must set revival limits for the following factions before the game can be advanced.\n" + names);
            }
            btWasHighThreshold = !game.hasGameOption(GameOption.HOMEWORLDS) || bt.isHighThreshold();
        } catch (IllegalArgumentException e) {
            // BT are not in the game
        }
        DuneTopic turnSummary = game.getTurnSummary();
        turnSummary.publish("**Turn " + game.getTurn() + " Revival Phase**");
        game.setPhaseForWhispers("Turn " + game.getTurn() + " Revival Phase\n");
        List<Faction> factions = game.getFactionsInStormOrder();
        StringBuilder message = new StringBuilder();
        boolean nonBTRevival = false;
        int factionsWithRevivals = 0;
        for (Faction faction : factions) {
            int numFreeRevived = faction.performFreeRevivals();
            if (numFreeRevived > 0) {
                factionsWithRevivals++;
                if (!(faction instanceof BTFaction))
                    nonBTRevival = true;
            }
            faction.presentPaidRevivalChoices(numFreeRevived);
        }

        if (btWasHighThreshold && factionsWithRevivals > 0) {
            Faction btFaction = game.getFaction("BT");
            message.append(btFaction.getEmoji())
                    .append(" gain ")
                    .append(factionsWithRevivals)
                    .append(" ")
                    .append(Emojis.SPICE)
                    .append(" from free revivals.\n");
            btFaction.addSpice(factionsWithRevivals, "for free revivals");
        }

        if (!message.isEmpty()) {
            turnSummary.publish(message.toString());
        }
        if (nonBTRevival && game.hasGameOption(GameOption.TECH_TOKENS))
            TechToken.addSpice(game, TechToken.AXLOTL_TANKS);
        for (Faction faction : factions) {
            if (faction.getPaidRevivalMessage() != null)
                turnSummary.publish(faction.getPaidRevivalMessage());
        }

        game.setUpdated(UpdateType.MAP);
    }

    public boolean ecazAmbassadorPlacement(Game game) {
        if (game.hasFaction("Ecaz")) {
            EcazFaction ecaz = (EcazFaction) game.getFaction("Ecaz");
            ecaz.sendAmbassadorLocationMessage(1);
            ecazAskedAboutAmbassadors = true;
            ecazAmbassadorsToBePlaced = true;
        }
        return ecazAmbassadorsToBePlaced;
    }

    public void ecazAmbassadorsComplete() {
        ecazAmbassadorsToBePlaced = false;
    }

    public boolean isEcazAskedAboutAmbassadors() {
        return ecazAskedAboutAmbassadors;
    }

    public boolean isEcazAmbassadorsToBePlaced() {
        return ecazAmbassadorsToBePlaced;
    }

    public boolean isCyborgRevivalComplete() {
        return cyborgRevivalComplete;
    }

    public void setCyborgRevivalComplete(boolean cyborgRevivalComplete) {
        this.cyborgRevivalComplete = cyborgRevivalComplete;
    }
}
