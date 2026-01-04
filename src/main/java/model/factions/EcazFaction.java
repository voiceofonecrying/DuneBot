package model.factions;

import constants.Emojis;
import enums.MoveType;
import enums.UpdateType;
import exceptions.InvalidGameStateException;
import model.*;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class EcazFaction extends Faction {
    protected final List<String> ambassadorPool;
    protected final List<String> ambassadorSupply;

    private final List<String> triggeredAmbassadors;

    private Leader loyalLeader;

    public EcazFaction(String player, String userName) throws IOException {
        super("Ecaz", player, userName);

        this.spice = 12;
        this.freeRevival = 2;
        this.emoji = Emojis.ECAZ;
        this.forceEmoji = Emojis.ECAZ_TROOP;
        this.highThreshold = 7;
        this.lowThreshold = 6;
        this.homeworld = "Ecaz";
        this.occupiedIncome = 2;
        this.ambassadorPool = new LinkedList<>();
        this.ambassadorSupply = new LinkedList<>();
        this.triggeredAmbassadors = new LinkedList<>();
        ambassadorPool.add("Atreides");
        ambassadorPool.add("BG");
        ambassadorPool.add("CHOAM");
        ambassadorPool.add("Emperor");
        ambassadorPool.add("Fremen");
        ambassadorPool.add("Harkonnen");
        ambassadorPool.add("Ix");
        ambassadorPool.add("Richese");
        ambassadorPool.add("Guild");
        ambassadorPool.add("BT");
        drawNewSupply();
    }

    @Override
    public void joinGame(@NotNull Game game) {
        super.joinGame(game);
        Territory ecaz = game.getTerritories().addHomeworld(game, homeworld, name);
        ecaz.addForces(name, 14);
        game.getHomeworlds().put(name, homeworld);
        game.getTerritories().get("Imperial Basin (Center Sector)").addForces("Ecaz", 6);
        game.createDukeVidal();
    }

    public void drawNewSupply() {
        this.ambassadorSupply.clear();
        ambassadorPool.addAll(triggeredAmbassadors);
        triggeredAmbassadors.clear();
        Collections.shuffle(ambassadorPool);

        for (int i = 0; i < 5; i++) {
            ambassadorSupply.add(ambassadorPool.removeFirst());
        }
        ambassadorSupply.add("Ecaz");
    }

    public void triggerAmbassador(Faction triggeringFaction, String ambassador, boolean forAlly) {
        chat.reply("You have triggered your " + ambassador + " Ambassador!");
        String triggerMessage = Emojis.ECAZ + " triggers their " + ambassador + " Ambassador against " + triggeringFaction.getEmoji();
        if (forAlly) {
            game.getFaction(ally).getChat().publish(Emojis.ECAZ + " has triggered their " + ambassador + " Ambassador for you!");
            game.getTurnSummary().publish(triggerMessage + " for their ally!");
        } else
            game.getTurnSummary().publish(triggerMessage + " !");
        List<String> supportedAmbassadorsForAlly = List.of("Emperor", "Fremen", "Guild");
        if (forAlly && !supportedAmbassadorsForAlly.contains(ambassador))
            game.getTurnSummary().publish(game.getModOrRoleMention() + " please execute the Ambassador for " + game.getFaction(ally).getEmoji());
        else {
            switch (ambassador) {
                case "Ecaz" -> {
                    DuneChoice getVidal = new DuneChoice("ecaz-get-vidal", "Get Duke Vidal");
                    DuneChoice offerAlliance = new DuneChoice("ecaz-offer-alliance-" + triggeringFaction.getName(), "Offer Alliance");
                    if (game.getLeaderTanks().stream().anyMatch(leader -> leader.getName().equals("Duke Vidal"))
                            || (game.hasHarkonnenFaction() && game.getHarkonnenFaction().isDukeVidalCaptured())
                            || (game.hasBTFaction() && game.getBTFaction().isDukeVidalGhola())
                            || isHomeworldOccupied())
                        getVidal.setDisabled(true);
                    if (hasAlly() || triggeringFaction.hasAlly())
                        offerAlliance.setDisabled(true);
                    List<DuneChoice> choices = new LinkedList<>();
                    choices.add(getVidal);
                    choices.add(offerAlliance);
                    chat.publish("Your Ecaz Ambassador has been triggered by " + triggeringFaction.getEmoji() + "! Which would you like to do?", choices);
                    ambassadorSupply.add("Ecaz");
                }
                case "Atreides" ->
                        chat.publish(triggeringFaction.getEmoji() + " hand is:\n\t" + String.join("\n\t", triggeringFaction.getTreacheryHand().stream().map(TreacheryCard::prettyNameAndDescription).toList()));
                case "BG" -> chat.publish("Which Ambassador effect would you like to trigger?",
                        ambassadorPool.stream().map(option -> new DuneChoice("ecaz-bg-trigger-" + option + "-" + triggeringFaction.getName(), option)).collect(Collectors.toCollection(LinkedList::new)));
                case "CHOAM" -> presentCHOAMAmbassadorDiscardChoices();
                case "Emperor" -> triggerEmperorAmbassador(forAlly);
                case "Fremen" -> presentFremenAmbassadorRideFromChoices(forAlly);
                case "Guild" -> presentGuildAmbassadorDestinationChoices(forAlly);
                case "Harkonnen" ->
                        chat.publish(triggeringFaction.getEmoji() + " has " + triggeringFaction.getTraitorHand().stream().findAny().orElseThrow().getEmojiNameAndStrengthString() + " as a " + (triggeringFaction instanceof BTFaction ? "Face Dancer!" : "Traitor!"));
                case "Ix" -> presentIxAmbassadorDiscardChoices();
                case "Richese" -> presentRicheseAmbassadorChoices();
                case "BT" -> presentBTAmbassadorChoices();
            }
        }

        for (Territory territory : game.getTerritories().values()) {
            if (territory.getEcazAmbassador() == null) continue;
            if (territory.getEcazAmbassador().equals(ambassador)) territory.setEcazAmbassador(null);
        }
        if (!ambassador.equals("BG") && !ambassador.equals("Ecaz")) {
            triggeredAmbassadors.add(ambassador);
        }

        long nonEcazAmbassadorsCount = game.getTerritories().values().stream()
                .map(Territory::getEcazAmbassador)
                .filter(Objects::nonNull)
                .filter(a -> !a.equals("Ecaz"))
                .count();
        nonEcazAmbassadorsCount += ambassadorSupply.stream().filter(a -> !a.equals("Ecaz")).count();

        if (nonEcazAmbassadorsCount == 0)
            drawNewSupply();
        setUpdated(UpdateType.MISC_BACK_OF_SHIELD);
        game.setUpdated(UpdateType.MAP);
    }

    public void triggerEmperorAmbassador(boolean forAlly) {
        Faction beneficiary = forAlly ? game.getFaction(ally) : this;
        beneficiary.addSpice(5, Emojis.EMPEROR + " Ambassador");
    }

    public void gainDukeVidalWithEcazAmbassador() throws InvalidGameStateException {
        if (leaders.stream().anyMatch(l -> l.getName().equals("Duke Vidal")))
            throw new InvalidGameStateException("Ecaz already has Duke Vidal.");
        else if (game.getLeaderTanks().stream().anyMatch(leader -> leader.getName().equals("Duke Vidal")))
            throw new InvalidGameStateException("Duke Vidal is in the tanks.");
        else if (isHomeworldOccupied())
            throw new InvalidGameStateException("Ecaz Homeworld is occupied. Ecaz may not take Duke Vidal.");
        else if (game.hasHarkonnenFaction() && game.getHarkonnenFaction().isDukeVidalCaptured())
            throw new InvalidGameStateException("Duke Vidal has been captured by Harkonnen.");
        else if (game.hasBTFaction() && game.getBTFaction().isDukeVidalGhola())
            throw new InvalidGameStateException("Duke Vidal is a ghola for BT");
        game.releaseDukeVidal(false);
        addLeader(game.getDukeVidal());
        chat.reply("Duke Vidal has come to fight for you!");
        game.getTurnSummary().publish("Duke Vidal now works for " + emoji);
    }

    public void presentCHOAMAmbassadorDiscardChoices() {
        if (treacheryHand.isEmpty()) {
            chat.publish("You have no " + Emojis.TREACHERY + " to discard with your " + Emojis.CHOAM + " Ambassador. Your Ambassador has been used.");
        } else {
            List<DuneChoice> choices = new ArrayList<>();
            int i = 0;
            for (TreacheryCard c : treacheryHand)
                choices.add(new DuneChoice("ecaz-choam-discard-" + c.name() + "-" + i++, c.name()));
            choices.add(new DuneChoice("secondary", "ecaz-choam-discard-None", "Done discarding"));
            chat.publish("Select " + Emojis.TREACHERY + " to discard for 3 " + Emojis.SPICE + " each (one at a time).", choices);
        }
    }

    public void discardWithCHOAMAmbassador(String cardName) {
        if (cardName.equals("None")) {
            chat.reply("You are finished discarding with your " + Emojis.CHOAM + " Ambassador.");
        } else {
            discard(cardName);
            addSpice(3, "discard " + cardName + " with CHOAM Ambassador.");
            chat.reply("You discarded " + cardName + " for 3 " + Emojis.SPICE);
            presentCHOAMAmbassadorDiscardChoices();
        }
    }

    public void presentFremenAmbassadorRideFromChoices(boolean forAlly) {
        Faction faction = this;
        if (forAlly)
            faction = game.getFaction(ally);
        faction.getMovement().setMoveType(MoveType.FREMEN_AMBASSADOR);
        faction.getMovement().presentMoveFromChoices();
    }

    public void presentGuildAmbassadorDestinationChoices(boolean forAlly) {
        Faction faction = forAlly ? game.getFaction(ally) : this;
        if (faction.getTotalReservesStrength() == 0) {
            String forceEmojis = faction.getForceEmoji();
            if (faction.hasSpecialForces())
                forceEmojis += " " + faction.getSpecialForceEmoji();
            faction.getChat().reply("You have no " + forceEmojis + " in reserves to place with the Guild Ambassador.");
        } else {
            faction.getMovement().setMoveType(MoveType.GUILD_AMBASSADOR);
            faction.getMovement().presentTerritoryTypeChoices();
        }
    }

    public void presentIxAmbassadorDiscardChoices() {
        if (treacheryHand.isEmpty()) {
            chat.publish("You have no " + Emojis.TREACHERY + " to discard with your " + Emojis.IX + " Ambassador. Your Ambassador has been used.");
        } else {
            List<DuneChoice> choices = new ArrayList<>();
            int i = 0;
            for (TreacheryCard c : treacheryHand)
                choices.add(new DuneChoice("ecaz-ix-discard-" + c.name() + "-" + i++, c.name()));
            choices.add(new DuneChoice("secondary", "ecaz-ix-discard-None", "Don't discard"));
            chat.publish("You can discard a " + Emojis.TREACHERY + " from your hand and draw a new one.", choices);
        }
    }

    public void discardAndDrawWithIxAmbassador(String cardName) {
        if (cardName.equals("None")) {
            chat.publish("You will not discard and draw a new card with your Ix Ambassador.");
        } else {
            discard(cardName);
            game.drawTreacheryCard("Ecaz", true, true);
            chat.publish("You discarded " + cardName + " and drew " + treacheryHand.getLast().name());
        }
    }

    public void presentRicheseAmbassadorChoices() {
        if (treacheryHand.size() == handLimit) {
            chat.publish("Your hand is full, so you cannot buy a " + Emojis.TREACHERY + " card with your Richese Ambassador.");
            game.getTurnSummary().publish(Emojis.ECAZ + " does not buy a card with their Richese Ambassador.");
        } else if (spice < 3) {
            chat.publish("You do not have enough " + Emojis.SPICE + " to buy a " + Emojis.TREACHERY + " card with your Richese Ambassador.");
            game.getTurnSummary().publish(Emojis.ECAZ + " does not buy a card with their Richese Ambassador.");
        } else {
            List<DuneChoice> choices = new ArrayList<>();
            choices.add(new DuneChoice("ecaz-richese-buy-yes", "Yes"));
            choices.add(new DuneChoice("secondary", "ecaz-richese-buy-no", "No"));
            chat.publish("Would you like to buy a " + Emojis.TREACHERY + " card for 3 " + Emojis.SPICE + "?", choices);
        }
    }

    public void presentBTAmbassadorChoices() {
        List<Leader> ecazLeadersInTanks = game.getLeaderTanks().stream().filter(l -> l.getName().equals("Sanya Ecaz") || l.getName().equals("Whitmore Bludd") || l.getName().equals("Ilesa Ecaz") || l.getName().equals("Rivvy Dinari") || l.getName().equals("Bindikk Narvi") || l.getName().equals("Duke Vidal")).toList();
        if (getRevivableForces() == 0) {
            if (ecazLeadersInTanks.isEmpty()) {
                chat.publish("You have no leaders or " + Emojis.ECAZ_TROOP + " in the tanks to revive with your BT Ambassador.");
                game.getTurnSummary().publish(Emojis.ECAZ + " has no leaders or " + Emojis.ECAZ_TROOP + " in the tanks to revive.");
            } else {
                presentLeaderChoicesWithBTAmbassador();
            }
        } else {
            int numForces = Math.min(getRevivableForces(), 4);
            if (ecazLeadersInTanks.isEmpty()) {
                reviveForcesWithBTAmbassador();
            } else {
                List<DuneChoice> choices = new ArrayList<>();
                choices.add(new DuneChoice("ecaz-bt-which-revival-leader", "Leader"));
                choices.add(new DuneChoice("ecaz-bt-which-revival-forces-" + numForces, numForces + " Forces"));
                chat.publish("Would you like to revive a leader or " + numForces + " " + Emojis.ECAZ_TROOP + "?", choices);
            }
        }
    }

    public void reviveForcesWithBTAmbassador() {
        int numForces = Math.min(getRevivableForces(), 4);
        game.reviveForces(this, false, numForces, 0);
        chat.reply("You revived " + numForces + " " + Emojis.ECAZ_TROOP + " with your BT Ambassador.");
    }

    public void presentLeaderChoicesWithBTAmbassador() {
        List<Leader> ecazLeadersInTanks = game.getLeaderTanks().stream().filter(l -> l.getName().equals("Sanya Ecaz") || l.getName().equals("Whitmore Bludd") || l.getName().equals("Ilesa Ecaz") || l.getName().equals("Rivvy Dinari") || l.getName().equals("Bindikk Narvi") || l.getName().equals("Duke Vidal")).toList();
        if (ecazLeadersInTanks.size() == 1) {
            reviveLeaderWithBTAmbassador(ecazLeadersInTanks.getFirst().getName());
        } else {
            List<DuneChoice> choices = new ArrayList<>();
            ecazLeadersInTanks.forEach(l -> choices.add(new DuneChoice("ecaz-bt-leader-" + l.getName(), l.getName())));
            chat.reply("Which leader would you like to revive?", choices);
        }
    }

    public void reviveLeaderWithBTAmbassador(String leaderName) {
        reviveLeader(leaderName);
        chat.reply(leaderName + " was revived with your BT Ambassador.");
        game.getTurnSummary().publish(Emojis.ECAZ + " revived " + leaderName + " with their BT Ambassador.");
    }

    public void buyCardWithRicheseAmbassador(boolean buy) {
        if (buy) {
            subtractSpice(3, "buy " + Emojis.TREACHERY + " with Richese Ambassador.");
            game.drawTreacheryCard("Ecaz", true, false);
            game.getTurnSummary().publish(Emojis.ECAZ + " buys a " + Emojis.TREACHERY + " card for 3 " + Emojis.SPICE + " with their Richese Ambassador.");
            chat.reply("You bought " + getTreacheryHand().getLast().name() + " with your Richese Ambassador.");
        } else {
            game.getTurnSummary().publish(Emojis.ECAZ + " does not buy a " + Emojis.TREACHERY + " with their Richese Ambassador.");
            chat.reply("You will not buy a " + Emojis.TREACHERY + " with your Richese Ambassador.");
        }
    }

    public void sendAmbassadorLocationMessage(int cost) throws InvalidGameStateException {
        if (ambassadorSupply.isEmpty()) {
            chat.publish("You have no Ambassadors in supply to place.");
            game.getModInfo().publish(Emojis.ECAZ + " has no Ambassadors to place. Please advance the game. " + game.getModOrRoleMention());
            game.getRevival().ecazAmbassadorsComplete();
        } else if (cost > spice) {
            chat.publish("You do not have " + cost + " " + Emojis.SPICE + " to place an Ambassador.");
            game.getModInfo().publish(Emojis.ECAZ + " does not have " + cost + " " + Emojis.SPICE + " to place an Ambassador. Please advance the game. " + game.getModOrRoleMention());
            game.getRevival().ecazAmbassadorsComplete();
        } else {
            List<DuneChoice> choices = new LinkedList<>();
            for (Territory territory : game.getTerritories().values()) {
                if (!territory.isStronghold()) continue;
                DuneChoice stronghold = new DuneChoice("ecaz-place-ambassador-" + territory.getTerritoryName() + "-" + cost, "Place Ambassador in " + territory.getTerritoryName());
                if (territory.getEcazAmbassador() != null || game.getStorm() == territory.getSector())
                    stronghold.setDisabled(true);
                choices.add(stronghold);
            }
            choices.add(new DuneChoice("secondary", "ecaz-no-more-ambassadors", "No more Ambassadors."));
            chat.publish("Would you like to place an Ambassador for " + cost + " " + Emojis.SPICE + "? " + getPlayer(), choices);
        }
    }

    public void sendAmbassadorMessage(String territory, int cost) {
        List<DuneChoice> choices = new LinkedList<>();
        for (String ambassador : ambassadorSupply)
            choices.add(new DuneChoice("ecaz-ambassador-selected-" + ambassador + "-" + territory + "-" + cost, ambassador));
        chat.reply("Which Ambassador would you like to send to " + territory + "?", choices);
    }

    public void placeAmbassador(String strongholdName, String ambassador, int cost) throws InvalidGameStateException {
        if (ambassadorSupply.isEmpty())
            throw new InvalidGameStateException("Ecaz has no Ambassadors in supply.");
        if (cost > spice) {
            chat.reply("You do not have " + cost + " " + Emojis.SPICE + " to place your Ambassador.");
            game.getModInfo().publish(Emojis.ECAZ + " does not have " + cost + " " + Emojis.SPICE + " to place an Ambassador. Please advance the game. " + game.getModOrRoleMention());
        } else {
            subtractSpice(cost, ambassador + " Ambassador to " + strongholdName);
            ambassadorSupply.removeIf(a -> a.equals(ambassador));
            game.getTerritory(strongholdName).setEcazAmbassador(ambassador);
            chat.reply("The " + ambassador + " Ambassador has been sent to " + strongholdName + ".");
            game.getTurnSummary().publish(Emojis.ECAZ + " has sent the " + ambassador + " Ambassador to " + strongholdName + ".");
            setUpdated(UpdateType.MISC_BACK_OF_SHIELD);
            game.setUpdated(UpdateType.MAP);
            sendAmbassadorLocationMessage(cost + 1);
        }
    }

    public void checkForAmbassadorTrigger(Territory targetTerritory, Faction targetFaction) {
        String ambassador = targetTerritory.getEcazAmbassador();
        if (ambassador != null && !(targetFaction instanceof EcazFaction)
                && !targetFaction.getName().equals(targetTerritory.getEcazAmbassador())
                && !getAlly().equals(targetFaction.getName())) {
            List<DuneChoice> choices = new ArrayList<>();
            choices.add(new DuneChoice("ambassador-trigger-" + ambassador + "-" + targetFaction.getName(), "Trigger"));
            if (hasAlly())
                choices.add(new DuneChoice("ambassador-trigger-ally-" + ambassador + "-" + targetFaction.getName(), "Trigger for ally"));
            choices.add(new DuneChoice("danger", "ambassador-dont-trigger-" + ambassador + "-" + targetFaction.getName(), "Don't Trigger"));
            game.getTurnSummary().publish(Emojis.ECAZ + " has an opportunity to trigger their " + ambassador + " Ambassador.");
            chat.publish("Will you trigger your " + ambassador + " Ambassador in " + targetTerritory.getTerritoryName() + " against " + targetFaction.getEmoji() + "? " + player, choices);
        }
    }

    public void returnAmbassadorToSuppy(Territory location, String ambassador) {
        location.removeEcazAmbassador();
        ambassadorSupply.add(ambassador);
        game.getTurnSummary().publish(Emojis.ECAZ + " " + ambassador + " ambassador returned to supply.");
        setUpdated(UpdateType.MISC_BACK_OF_SHIELD);
        setUpdated(UpdateType.MISC_FRONT_OF_SHIELD);
    }

    public Leader getLoyalLeader() {
        return loyalLeader;
    }

    public void setLoyalLeader(Leader loyalLeader) {
        this.loyalLeader = loyalLeader;
    }

    public String getAmbassadorSupplyInfoMessage() {
        StringBuilder supply = new StringBuilder();
        supply.append("\n__Ambassador Supply:__ ");

        String emojis = getAmbassadorSupplyEmojis();
        supply.append(emojis.isEmpty() ? "Empty" : emojis);
        return supply.toString();
    }

    public List<String> getAmbassadorSupply() {
        return this.ambassadorSupply;
    }

    public String getAmbassadorSupplyEmojis() {
        return ambassadorSupply.stream().map(a -> Emojis.getFactionEmoji(a) + " ").collect(Collectors.joining());
    }

    public void addToAmbassadorPool(String ambassador) {
        ambassadorPool.add(ambassador);
        getLedger().publish(ambassador + " Ambassador token was added to the pool.");
    }

    public void addAmbassadorToSupply(String ambassador) {
        ambassadorSupply.add(ambassador);
        getLedger().publish(ambassador + " Ambassador token was added to the supply.");
        setUpdated(UpdateType.MISC_BACK_OF_SHIELD);
    }

    public void removeAmbassadorFromMap(String ambassadorName, boolean toHand) {
        Territory territory = game.getTerritories().values().stream()
                .filter(t -> t.getEcazAmbassador() != null && t.getEcazAmbassador().equals(ambassadorName))
                .findFirst().orElseThrow(() -> new IllegalArgumentException("Ambassador not found on map"));
        territory.removeEcazAmbassador();
        if (toHand)
            addAmbassadorToSupply(ambassadorName);
        else
            addToAmbassadorPool(ambassadorName);
        game.setUpdated(UpdateType.MAP);
    }
}
