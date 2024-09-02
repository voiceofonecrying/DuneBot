package model.factions;

import constants.Emojis;
import enums.GameOption;
import enums.UpdateType;
import model.*;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class EcazFaction extends Faction {
    private final List<String> ambassadorPool;
    private final List<String> ambassadorSupply;

    private final List<String> triggeredAmbassadors;

    private Leader loyalLeader;

    public EcazFaction(String player, String userName, Game game) throws IOException {
        super("Ecaz", player, userName, game);

        setSpice(12);
        this.freeRevival = 2;
        this.emoji = Emojis.ECAZ;
        this.highThreshold = 7;
        this.lowThreshold = 6;
        this.homeworld = "Ecaz";
        Territory ecaz = game.getTerritories().addHomeworld(game, homeworld, name);
        ecaz.addForces(name, 14);
        game.getHomeworlds().put(name, homeworld);
        this.occupiedIncome = 2;
        game.getTerritories().get("Imperial Basin (Center Sector)").addForces("Ecaz", 6);
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

    public void triggerAmbassador(Faction triggeringFaction, String ambassador) {
        game.getTurnSummary().publish("The " + ambassador + " ambassador has been triggered!");
        switch (ambassador) {
            case "Ecaz" -> {
                DuneChoice getVidal = new DuneChoice("ecaz-get-vidal", "Get Duke Vidal");
                DuneChoice offerAlliance = new DuneChoice("ecaz-offer-alliance-" + triggeringFaction.getName(), "Offer Alliance");
                if (game.getLeaderTanks().stream().anyMatch(leader -> leader.getName().equals("Duke Vidal"))
                        || (game.hasFaction("Harkonnen") && game.getFaction("Harkonnen").getLeaders().stream().anyMatch(leader -> leader.getName().equals("Duke Vidal")))
                        || (game.hasFaction("BT") && game.getFaction("BT").getLeaders().stream().anyMatch(leader -> leader.getName().equals("Duke Vidal"))))
                    getVidal.setDisabled(true);
                if (game.getFaction("Ecaz").hasAlly() || triggeringFaction.hasAlly())
                    offerAlliance.setDisabled(true);
                List<DuneChoice> choices = new LinkedList<>();
                choices.add(getVidal);
                choices.add(offerAlliance);
                chat.publish("Your Ecaz Ambassador has been triggered by " + triggeringFaction.getEmoji() + "! Which would you like to do?", choices);
                ambassadorSupply.add("Ecaz");
            }
            case "Atreides" ->
                    chat.publish(triggeringFaction.getEmoji() + " hand is:\n\t" + String.join("\n\t", triggeringFaction.getTreacheryHand().stream().map(TreacheryCard::prettyNameAndDescription).toList()));
            case "BG" ->
                    chat.publish("Which Ambassador effect would you like to trigger?",
                            ambassadorPool.stream().map(option -> new DuneChoice("ecaz-bg-trigger-" + option + "-" + triggeringFaction.getName(), option)).collect(Collectors.toCollection(LinkedList::new)));
            case "CHOAM" -> presentCHOAMAmbassadorDiscardChoices();
            case "Emperor" -> addSpice(5, Emojis.EMPEROR + " ambassador");
            case "Fremen" -> presentFremenAmbassadorRideFromChoices();
            case "Guild" -> presentGuildAmbassadorDestinationChoices();
            case "Harkonnen" ->
                    game.getModInfo().publish("Harkonnen ambassador token was triggered by " + triggeringFaction.getEmoji() + ", please show Ecaz player a random traitor card that " + triggeringFaction.getEmoji() + " holds.");
            case "Ix" ->
                    game.getModInfo().publish("Ixian ambassador token was triggered, Ecaz may discard a treachery card and draw a new one.");
            case "Richese" ->
                    game.getModInfo().publish("Richese ambassador token was triggered, Ecaz may draw a treachery card for 3 spice.");
            case "BT" ->
                    game.getModInfo().publish("BT ambassador token was triggered, Ecaz may revive a leader or up to 4 forces for free.");
        }

        for (Territory territory : game.getTerritories().values()) {
            if (territory.getEcazAmbassador() == null) continue;
            if (territory.getEcazAmbassador().equals(ambassador)) territory.setEcazAmbassador(null);
        }
        if (!ambassador.equals("BG")) {
            triggeredAmbassadors.add(ambassador);
        }

        long nonEcazAmbassadorsCount = game.getTerritories().values().stream()
                .map(Territory::getEcazAmbassador)
                .filter(Objects::nonNull)
                .filter(a -> !a.equals("Ecaz"))
                .count();
        nonEcazAmbassadorsCount += ambassadorSupply.stream().filter(a -> !a.equals("Ecaz")).count();

        if (nonEcazAmbassadorsCount == 0) drawNewSupply();
        setUpdated(UpdateType.MISC_BACK_OF_SHIELD);
        game.setUpdated(UpdateType.MAP);
    }

    public void presentCHOAMAmbassadorDiscardChoices() {
        List<DuneChoice> choices = new ArrayList<>();
        int i = 0;
        for (TreacheryCard c : treacheryHand)
            choices.add(new DuneChoice("ecaz-choam-discard-" + c.name() + "-" + i++, c.name()));
        choices.add(new DuneChoice("secondary", "ecaz-choam-discard-finished", "Done discarding"));
        chat.publish("Select " + Emojis.TREACHERY + " to discard for 3 " + Emojis.SPICE + " each (one at a time).", choices);
    }

    public void presentFremenAmbassadorRideFromChoices() {
        List<DuneChoice> choices = game.getTerritories().values().stream().filter(t -> !(t instanceof HomeworldTerritory)).filter(t -> t.hasForce("Ecaz")).map(Territory::getTerritoryName).map(t -> new DuneChoice("ecaz-fremen-move-from-" + t, t)).collect(Collectors.toList());
        choices.add(new DuneChoice("danger", "pass-shipment-fremen-ride", "No move"));
        chat.publish("Where would you like to ride from with your Fremen ambassador?", choices);
    }

    public void presentGuildAmbassadorDestinationChoices() {
        if (getReservesStrength() == 0)
            chat.publish("You have no " + Emojis.ECAZ_TROOP + " in reserves to place with the Guild ambassador.");
        else {
            String buttonSuffix = "-guild-ambassador";
            List<DuneChoice> choices = new LinkedList<>();
            choices.add(new DuneChoice("stronghold" + buttonSuffix, "Stronghold"));
            choices.add(new DuneChoice("spice-blow" + buttonSuffix, "Spice Blow Territories"));
            choices.add(new DuneChoice("rock" + buttonSuffix, "Rock Territories"));
            boolean revealedDiscoveryTokenOnMap = game.getTerritories().values().stream().anyMatch(Territory::isDiscovered);
            if (game.hasGameOption(GameOption.DISCOVERY_TOKENS) && revealedDiscoveryTokenOnMap)
                choices.add(new DuneChoice("discovery-tokens" + buttonSuffix, "Discovery Tokens"));
            choices.add(new DuneChoice("other" + buttonSuffix, "Somewhere else"));
            choices.add(new DuneChoice("danger", "pass-shipment" + buttonSuffix, "Pass shipment"));
            chat.publish("Where would you like to place up to 4 " + Emojis.ECAZ_TROOP + " from reserves?", choices);
        }
    }

    public void sendAmbassadorLocationMessage(int cost) {
        List<DuneChoice> choices = new LinkedList<>();
        for (Territory territory : game.getTerritories().values()) {
            if (!territory.isStronghold()) continue;
            if (territory.getTerritoryName().equals("Hidden Mobile Stronghold") && !game.hasFaction("Ix")) continue;
            DuneChoice stronghold = new DuneChoice("ecaz-place-ambassador-" + territory.getTerritoryName() + "-" + cost, "Place Ambassador in " + territory.getTerritoryName());
            if (territory.getEcazAmbassador() != null || game.getStorm() == territory.getSector())
                stronghold.setDisabled(true);
            choices.add(stronghold);
        }
        choices.add(new DuneChoice("secondary", "ecaz-no-more-ambassadors", "No more ambassadors."));
        chat.publish("Use these buttons to place Ambassador tokens from your supply for " + cost + " " + Emojis.SPICE + "." + getPlayer(), choices);
    }

    public void sendAmbassadorMessage(String territory, int cost) {
        List<DuneChoice> choices = new LinkedList<>();
        for (String ambassador : ambassadorSupply)
            choices.add(new DuneChoice("ecaz-ambassador-selected-" + ambassador + "-" + territory + "-" + cost, ambassador));
        chat.publish("Which ambassador would you like to send?", choices);
    }

    public void placeAmbassador(Territory territory, String ambassador) {
        ambassadorSupply.removeIf(a -> a.equals(ambassador));
        territory.setEcazAmbassador(ambassador);
        setUpdated(UpdateType.MISC_BACK_OF_SHIELD);
        game.setUpdated(UpdateType.MAP);
    }

    public void checkForAmbassadorTrigger(Territory targetTerritory, Faction targetFaction) {
        String ambassador = targetTerritory.getEcazAmbassador();
        if (ambassador != null && !(targetFaction instanceof EcazFaction)
                && !targetFaction.getName().equals(targetTerritory.getEcazAmbassador())
                && !getAlly().equals(targetFaction.getName())) {
            List<DuneChoice> choices = new ArrayList<>();
            choices.add(new DuneChoice("ecaz-trigger-ambassador-" + ambassador + "-" + targetFaction.getName(), "Trigger"));
            choices.add(new DuneChoice("danger", "ecaz-don't-trigger-ambassador", "Don't Trigger"));
            game.getTurnSummary().publish(Emojis.ECAZ + " has an opportunity to trigger their " + ambassador + " ambassador.");
            chat.publish("Will you trigger your " + ambassador + " ambassador against " + targetFaction.getEmoji() + " in " + targetTerritory.getTerritoryName() + "? " + player, choices);
        }
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
        getLedger().publish(ambassador + " ambassador token was added to the pool.");
    }

    public void addAmbassadorToSupply(String ambassador) {
        ambassadorSupply.add(ambassador);
        getLedger().publish(ambassador + " ambassador token was added to the supply.");
        setUpdated(UpdateType.MISC_BACK_OF_SHIELD);
    }
}
