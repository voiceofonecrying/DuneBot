package model;

import constants.Emojis;
import enums.UpdateType;
import model.factions.*;

import java.text.MessageFormat;
import java.util.*;
import java.util.stream.Collectors;

public class Territory {
    private final String territoryName;
    private final int sector;
    private boolean isStronghold;
    private final boolean isDiscoveryToken;
    private boolean justDiscovered;
    private final boolean isNearShieldWall;
    protected final List<Force> forces;
    private boolean isRock;
    private int spice;
    private Integer richeseNoField;
    private String ecazAmbassador;
    private final List<String> terrorTokens;
    private boolean aftermathToken;
    private String discoveryToken;
    private boolean discovered;

    public Territory(String territoryName, int sector, boolean isRock, boolean isStronghold, boolean isDiscoveryToken, boolean isNearShieldWall) {
        this.territoryName = territoryName;
        this.sector = sector;
        this.isRock = isRock;
        this.isStronghold = isStronghold;
        this.isDiscoveryToken = isDiscoveryToken;
        this.justDiscovered = false;
        this.isNearShieldWall = isNearShieldWall;
        this.spice = 0;
        this.forces = new ArrayList<>();
        this.richeseNoField = null;
        this.ecazAmbassador = null;
        this.aftermathToken = false;
        this.terrorTokens = new LinkedList<>();
    }

    public Territory(String territoryName, int sector, boolean isRock, boolean isStronghold, boolean isNearShieldWall) {
        this(territoryName, sector, isRock, isStronghold, false, isNearShieldWall);
    }

    public String getTerritoryName() {
        return territoryName;
    }

    public int getSector() {
        return sector;
    }

    public boolean isRock() {
        return isRock;
    }

    public void setRock(boolean rock) {
        isRock = rock;
    }

    public boolean isStronghold() {
        return isStronghold;
    }

    public void setStronghold(boolean isStronghold) {
        this.isStronghold = isStronghold;
    }

    public boolean isDiscoveryToken() {
        return isDiscoveryToken;
    }

    public boolean isJustDiscovered() {
        return justDiscovered;
    }

    public void setJustDiscovered(boolean justDiscovered) {
        this.justDiscovered = justDiscovered;
    }

    public int costToShipInto() {
        return isStronghold || isDiscoveryToken ? 1 : 2;
    }

    public boolean isNearShieldWall() {
        return isNearShieldWall;
    }

    public int getSpice() {
        return spice;
    }

    public void setSpice(int spice) {
        this.spice = spice;
    }

    public List<Force> getForces() {
        return forces;
    }

    public Force getForce(String name) {
        List<Force> forceObjects = forces.stream().filter(force -> force.getName().equals(name)).toList();
        if (forceObjects.size() > 1) {
            // Combine same name Forces if they got split
            int totalStrength = forceObjects.stream().mapToInt(Force::getStrength).sum();
            forces.removeAll(forceObjects);
            if (totalStrength > 0)
                forces.add(new Force(name, totalStrength));
        } else if (forceObjects.size() == 1 && forceObjects.getFirst().getStrength() == 0) {
            // Remove 0 strength Force
            forces.remove(forceObjects.getFirst());
        }
        return forces.stream().filter(force -> force.getName().equals(name)).findFirst().orElse(new Force(name, 0));
    }

    public boolean hasForce(String name) {
        return forces.stream().anyMatch(force -> force.getName().equals(name));
    }

    /**
     * Gives the total count of the faction's forces in the territory.
     * Counts a Richese No-Field token as 1 force.
     *
     * @param faction The faction to filter by.
     * @return the total of regular, starred, and No-Field the faction has in the territory.
     */
    public int getTotalForceCount(Faction faction) {
        int amount = forces.stream()
                .filter(force -> force.getFactionName().equals(faction.getName()))
                .map(Force::getStrength)
                .reduce(0, Integer::sum);
        if (faction instanceof RicheseFaction && hasRicheseNoField()) amount++;
        return amount;
    }

    /**
     * Gives the total count of factions with forces in the territory.
     *
     * @return the number of factions in the territory.
     */
    public int countFactions() {
        Set<String> factionNames = new HashSet<>();
        forces.stream().filter(force -> !force.getName().equals("Hidden Mobile Stronghold"))
                .forEach(force -> factionNames.add(force.getFactionName()));
        if (hasRicheseNoField()) factionNames.add("Richese");
        return factionNames.size();
    }

    public void removeForce(String name) {
        int forceStrength = getForceStrength(name);
        removeForces(name, forceStrength);
    }

    public List<String> getActiveFactionNames() {
        Set<String> factions = forces.stream()
                .filter(force -> !(force.getName().equals("Advisor")))
                .filter(force -> !(force.getName().equals("Hidden Mobile Stronghold")))
                .map(Force::getFactionName)
                .collect(Collectors.toSet());

        if (hasRicheseNoField()) factions.add("Richese");

        return new ArrayList<>(factions);
    }

    // Returns list of factions that have active forces in the territories (not Advisors)
    public List<Faction> getActiveFactions(Game game) {
        return getActiveFactionNames().stream()
                .map(game::getFaction)
                .toList();
    }

    public boolean hasActiveFaction(Faction faction) {
        return hasActiveFaction(faction.getName());
    }

    public boolean hasActiveFaction(String factionName) {
        return getActiveFactionNames().contains(factionName);
    }

    public int countActiveFactions() {
        return getActiveFactionNames().size();
    }

    public int getForceStrength(String forceName) {
        return getForce(forceName).getStrength();
    }

    private void setForceStrength(String name, int strength) {
        if (strength < 0) throw new IllegalArgumentException("You cannot set a negative strength value for a force.");
        forces.remove(getForce(name));
        if (strength > 0)
            forces.add(new Force(name, strength));
    }

    public void addForces(String forceName, int amount) {
        int forceStrength = getForceStrength(forceName);
        setForceStrength(forceName, forceStrength + amount);
    }

    public void removeForces(String forceName, int amount) {
        if (amount < 0) throw new IllegalArgumentException("You cannot remove a negative strength value from a force.");
        int forceStrength = getForceStrength(forceName);
        if (forceStrength < amount) throw new IllegalArgumentException("Not enough forces in " + territoryName + ".");
        setForceStrength(forceName, forceStrength - amount);
    }

    /**
     * Places a force from reserves into this territory.
     * Reports removal from reserves to ledger.
     * Switches homeworld to low threshold if applicable.
     *
     * @param game      The Game instance.
     * @param faction   The faction that owns the force.
     * @param amount    The number of forces to place.
     * @param special   Whether the force is a special reserve.
     */
    public void placeForceFromReserves(Game game, Faction faction, int amount, boolean special) {
        String forceName;
        if (special) {
            faction.removeSpecialReserves(amount);
            forceName = faction.getName() + "*";
        } else {
            faction.removeReserves(amount);
            forceName = faction.getName();
            if (faction instanceof BGFaction && hasForce("Advisor")) {
                int advisors = getForceStrength("Advisor");
                addForces("BG", advisors);
                removeForce("Advisor");
            }
        }
        faction.getLedger().publish(
                MessageFormat.format("{0} {1} removed from reserves.", amount, Emojis.getForceEmoji(faction.getName() + (special ? "*" : ""))));
        addForces(forceName, amount);
        faction.checkForLowThreshold();
        game.setUpdated(UpdateType.MAP);
    }

    public void addSpice(Game game, Integer spice) {
        this.spice += spice;
        game.setUpdated(UpdateType.MAP);
    }

    public Integer getRicheseNoField() {
        return richeseNoField;
    }

    public void setRicheseNoField(Integer richeseNoField) {
        this.richeseNoField = richeseNoField;
    }

    public boolean hasRicheseNoField() {
        return getRicheseNoField() != null;
    }

    public String getEcazAmbassador() {
        return ecazAmbassador;
    }

    public void setEcazAmbassador(String ecazAmbassador) {
        this.ecazAmbassador = ecazAmbassador;
    }

    public void removeEcazAmbassador() {
        this.ecazAmbassador = null;
    }

    public List<String> getTerrorTokens() {
        return terrorTokens;
    }

    public void addTerrorToken(String terrorToken) {
        this.terrorTokens.add(terrorToken);
    }

    public boolean hasTerrorToken() {
        return !getTerrorTokens().isEmpty();
    }

    public boolean hasTerrorToken(String name) {
        return getTerrorTokens().stream()
                .filter(t -> t.equals(name))
                .map(t -> true)
                .findFirst().orElse(false);
    }

    public void removeTerrorToken(String name) {
        getTerrorTokens().remove(name);
    }

    public boolean isAftermathToken() {
        return aftermathToken;
    }

    public void setAftermathToken(boolean aftermathToken) {
        this.aftermathToken = aftermathToken;
    }

    public String getDiscoveryToken() {
        return discoveryToken;
    }

    public void setDiscoveryToken(String discoveryToken) {
        this.discoveryToken = discoveryToken;
    }

    public boolean isDiscovered() {
        return discovered;
    }

    public void setDiscovered(boolean discovered) {
        this.discovered = discovered;
    }

    public String stormTroopsFremen(Game game) {
        List<Force> fremenForces = forces.stream()
                .filter(f -> f.getFactionName().equalsIgnoreCase("Fremen"))
                .toList();
        StringBuilder message = new StringBuilder();

        int totalTroops = fremenForces.stream().mapToInt(Force::getStrength).sum();
        int totalLostTroops = Math.ceilDiv(totalTroops, 2);

        Force regularForce = getForce("Fremen");
        Force fedaykin = getForce("Fremen*");

        int lostRegularForces = Math.min(regularForce.getStrength(), totalLostTroops);
        totalLostTroops -= lostRegularForces;
        int lostFedaykin = Math.min(fedaykin.getStrength(), totalLostTroops);

        if (lostRegularForces > 0)
            message.append(stormRemoveTroops("Fremen", "Fremen", lostRegularForces, game));

        if (lostFedaykin > 0)
            message.append(stormRemoveTroops("Fremen*", "Fremen", lostFedaykin, game));

        return message.toString();
    }

    public String stormRemoveTroops(String forceName, String factionName, int strength, Game game) {
        removeForces(forceName, strength);
        game.getTleilaxuTanks().addForces(forceName, strength);

        return MessageFormat.format(
                "{0} lose {1} {2} to the storm in {3}.\n",
                Emojis.getFactionEmoji(factionName),
                strength, Emojis.getForceEmoji(forceName),
                territoryName
        );
    }

    public String stormTroops(Game game) {
        StringBuilder message = new StringBuilder();
        List<Force> nonFremenForces = forces.stream()
                .filter(f -> !f.getFactionName().equalsIgnoreCase("Fremen"))
                .filter(force -> !(force.getName().equalsIgnoreCase("Hidden Mobile Stronghold")))
                .toList();

        message.append(stormTroopsFremen(game));
        for (Force force : nonFremenForces) {
            message.append(stormRemoveTroops(force.getName(), force.getFactionName(), force.getStrength(), game));
        }
        return message.toString();
    }

    public String stormRemoveSpice() {
        String message = MessageFormat.format(
                "{0} {1} in {2} was blown away by the storm.\n",
                spice, Emojis.SPICE, territoryName
        );
        spice = 0;
        return message;
    }

    public String shaiHuludAppears(Game game, String wormName, boolean firstWorm) {
        String response = "";
        if (firstWorm)
            response = Emojis.WORM + " " + wormName + " has been spotted in " + territoryName + "!\n";
        if (spice > 0)
            response += spice + " " + Emojis.SPICE + " is eaten by the worm!\n";
        spice = 0;

        FremenFaction fremen = null;
        if (game.hasFaction("Fremen"))
            fremen = (FremenFaction) game.getFaction("Fremen");
        if (countFactions() > 0) {
            List<Force> forcesToRemove = new ArrayList<>();
            StringBuilder message = new StringBuilder();
            for (Force force : forces) {
                if (force.getName().contains("Fremen") || fremen != null && fremen.hasAlly() && force.getName().contains(fremen.getAlly()))
                    continue;
                message.append(MessageFormat.format("{0} {1} devoured by {2}\n",
                        force.getStrength(), Emojis.getForceEmoji(force.getName()), wormName
                ));
                forcesToRemove.add(force);
            }
            response += message.toString();
            for (Force force : forcesToRemove)
                // Move this to Territory::removeForces - but Game::removeForces is checking for high/low threshold, so that must move over too
                game.removeForces(territoryName, game.getFaction(force.getFactionName()), force.getStrength(), force.getName().contains("*"), true);
            if (wormName.equals("Shai-Hulud")) {
                String fremenForces = "";
                int strength = getForceStrength("Fremen");
                if (strength > 0)
                    fremenForces += strength + " " + Emojis.FREMEN_TROOP + " ";
                strength = getForceStrength("Fremen*");
                if (strength > 0)
                    fremenForces += strength + " " + Emojis.FREMEN_FEDAYKIN + " ";
                if (!fremenForces.isEmpty()) {
                    response += "After the Nexus, " + fremenForces + "may ride " + wormName + "!\n";
                    Objects.requireNonNull(fremen).addWormRide(territoryName);
                }
            }
        }
        if (fremen != null && wormName.equals("Great Maker")) {
            String fremenForces = "";
            int strength = fremen.getReservesStrength();
            if (strength > 0)
                fremenForces += strength + " " + Emojis.FREMEN_TROOP + " ";
            strength = fremen.getSpecialReservesStrength();
            if (strength > 0)
                fremenForces += strength + " " + Emojis.FREMEN_FEDAYKIN + " ";
            if (!fremenForces.isEmpty()) {
                response += "After the Nexus, " + fremenForces + "in reserves may ride " + wormName + "!\n";
                fremen.addWormRide(fremen.getHomeworld());
            } else {
                response += Emojis.FREMEN + " have no forces in reserves to ride Great Maker.\n";
            }
        }
        return response;
    }

    public boolean factionMayMoveIn(Game game, Faction faction) {
        return territoryName.equals("Polar Sink") || !faction.hasAlly() || !hasActiveFaction(game.getFaction(faction.getAlly())) ||
                (faction instanceof EcazFaction && getActiveFactions(game).stream().anyMatch(f -> f.getName().equals(faction.getAlly()))
                        || faction.getAlly().equals("Ecaz") && getActiveFactions(game).stream().anyMatch(f -> f instanceof EcazFaction));
    }

    public String getAggregateTerritoryName() {
        int endLocation = territoryName.indexOf(" (");
        if (endLocation == -1) return territoryName;
        return territoryName.substring(0, endLocation);
    }
}
