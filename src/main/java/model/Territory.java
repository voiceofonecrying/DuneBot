package model;

import constants.Emojis;
import enums.GameOption;
import enums.UpdateType;
import exceptions.InvalidGameStateException;
import model.factions.*;

import java.text.MessageFormat;
import java.util.*;
import java.util.stream.Collectors;

public class Territory {
    protected final String territoryName;
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

    public void removeForces(Game game, String forceName, int amount) {
        if (amount < 0) throw new IllegalArgumentException("You cannot remove a negative strength value from a force.");
        int forceStrength = getForceStrength(forceName);
        if (forceStrength < amount) throw new IllegalArgumentException("Not enough " + forceName + " forces in " + territoryName + ".");
        setForceStrength(forceName, forceStrength - amount);
        if (!game.hasGameOption(GameOption.BG_COEXIST_WITH_ALLY) || game.getPhaseForTracker() < 7)
            game.getTerritories().flipAdvisorsIfAlone(game, this);
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
        return richeseNoField != null;
    }

    public String getEcazAmbassador() {
        return ecazAmbassador;
    }

    public boolean hasEcazAmbassador() {
        return ecazAmbassador != null;
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

    public boolean hasTerrorToken() {
        return !getTerrorTokens().isEmpty();
    }

    public boolean hasTerrorToken(String name) {
        return getTerrorTokens().stream()
                .filter(t -> t.equals(name))
                .map(t -> true)
                .findFirst().orElse(false);
    }

    public void addTerrorToken(Game game, String terrorToken) throws InvalidGameStateException {
        if (!terrorTokens.isEmpty() && !(game.hasGameOption(GameOption.HOMEWORLDS) && game.getMoritaniFaction().isHighThreshold()))
            throw new InvalidGameStateException(territoryName + " already has a Terror Token.");
        terrorTokens.add(terrorToken);
        game.setUpdated(UpdateType.MAP);
    }

    public void removeTerrorToken(Game game, String terrorTokenName, boolean returnToMoritani) {
        if (!terrorTokens.remove(terrorTokenName))
            throw new IllegalArgumentException(territoryName + " does not have the " + terrorTokenName + " Terror Token.");
        if (returnToMoritani)
            game.getMoritaniFaction().addTerrorToken(terrorTokenName);
        game.setUpdated(UpdateType.MAP);
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

    public String shieldWallRemoveTroops(Game game) {
        StringBuilder message = new StringBuilder();

        for (Force force : new ArrayList<>(forces)) {
            String name = force.getFactionName();
            int strength = force.getStrength();
            removeForces(game, name, strength);
            game.getTleilaxuTanks().addForces(name, strength);

            message.append(MessageFormat.format(
                    "{0} lose {1} {2} to the atomic explosion.\n",
                    Emojis.getFactionEmoji(force.getFactionName()),
                    strength, Emojis.getForceEmoji(name)
            ));
        }

        return message.toString();
    }
    
    public void stormTroopsFremen(Game game) {
        List<Force> fremenForces = forces.stream()
                .filter(f -> f.getFactionName().equalsIgnoreCase("Fremen"))
                .toList();

        int totalTroops = fremenForces.stream().mapToInt(Force::getStrength).sum();
        int totalLostTroops = Math.ceilDiv(totalTroops, 2);

        Force regularForce = getForce("Fremen");
        Force fedaykin = getForce("Fremen*");

        int lostRegularForces = Math.min(regularForce.getStrength(), totalLostTroops);
        totalLostTroops -= lostRegularForces;
        int lostFedaykin = Math.min(fedaykin.getStrength(), totalLostTroops);

        if (lostRegularForces > 0)
            stormRemoveTroops("Fremen", "Fremen", lostRegularForces, game);
        if (lostFedaykin > 0)
            stormRemoveTroops("Fremen*", "Fremen", lostFedaykin, game);
    }

    public void stormRemoveTroops(String forceName, String factionName, int strength, Game game) {
        removeForces(game, forceName, strength);
        game.getTleilaxuTanks().addForces(forceName, strength);
        game.getTurnSummary().publish(MessageFormat.format(
                "{0} lose {1} {2} to the storm in {3}.",
                Emojis.getFactionEmoji(factionName),
                strength, Emojis.getForceEmoji(forceName),
                territoryName
        ));
    }

    public void stormTroops(Game game) {
        if (richeseNoField != null)
            game.getRicheseFaction().revealNoField();
        int advisorStrength = getForceStrength("Advisor");
        if (advisorStrength > 0)
            stormRemoveTroops("Advisor", "BG", getForceStrength("Advisor"), game);
        List<Force> nonFremenForces = forces.stream()
                .filter(f -> !f.getFactionName().equalsIgnoreCase("Fremen"))
                .filter(force -> !(force.getName().equalsIgnoreCase("Hidden Mobile Stronghold")))
                .toList();

        stormTroopsFremen(game);
        for (Force force : nonFremenForces) {
            stormRemoveTroops(force.getName(), force.getFactionName(), force.getStrength(), game);
        }
    }

    public void stormRemoveSpice(Game game) {
        if (spice != 0)
            game.getTurnSummary().publish(spice + " " + Emojis.SPICE + " in " + territoryName + " was blown away by the storm.");
        spice = 0;
    }

    public void stormRemoveAmbassador(Game game) {
        if (ecazAmbassador != null) {
            game.getTurnSummary().publish(Emojis.ECAZ + " " + ecazAmbassador + " Ambassador was removed from " + territoryName + " and returned to supply.");
            game.getEcazFaction().addAmbassadorToSupply(ecazAmbassador);
            ecazAmbassador = null;
        }
    }

    public String lasgunShieldDestroysSpice() {
        String message = MessageFormat.format(
                "{0} {1} in {2} was destroyed by Lasgun-Shield.",
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

        FremenFaction fremen = game.getFremenFactionOrNull();
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
            for (Force force : forcesToRemove) {
                if (force.getName().equals("Hidden Mobile Stronghold"))
                    continue;
                // Move this to Territory::removeForces - but Game::removeForces is checking for high/low threshold, so that must move over too
                game.removeForces(territoryName, game.getFaction(force.getFactionName()), force.getStrength(), force.getName().contains("*"), true);
            }
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

    public boolean isValidStrongholdForShipmentFremenRideAndBTHT(Faction faction, boolean nonIxMayEnter) {
        if (!isStronghold)
            return false;
        else if (territoryName.equals("Hidden Mobile Stronghold"))
            return faction instanceof IxFaction || nonIxMayEnter;
        else
            return true;
    }

    public boolean factionMayNotEnter(Game game, Faction faction, boolean isShipment, boolean isInitialPlacement) {
        if (isInitialPlacement) {
            return faction instanceof MoritaniFaction && !forces.isEmpty();
        } else if (isShipment) {
            if (territoryName.equals("Hidden Mobile Stronghold") && !(faction instanceof IxFaction))
                return true;
            if (aftermathToken)
                return true;
            if (onlyEcazAndAllyPresent(game))
                return false;
            return isStronghold && getActiveFactions(game).size() >= 2 && !hasActiveFaction(faction) && notEcazAllyException(game, faction);
        }
        return !territoryName.equals("Polar Sink") && notEcazAllyException(game, faction) && !onlyEcazAndAllyPresent(game)
                && (faction.hasAlly() && hasAllyForces(game, faction)
                || isStronghold && getActiveFactions(game).size() >= 2 && !hasActiveFaction(faction));
    }

    public boolean factionMustMoveOut(Game game, Faction faction) {
        return !territoryName.equals("Polar Sink") && faction.hasAlly() && hasAllyForces(game, faction) && notEcazAllyException(game, faction);
    }

    private boolean hasAllyForces(Game game, Faction faction) {
        Faction ally = null;
        if (faction.hasAlly())
            ally = game.getFaction(faction.getAlly());
        if (ally == null)
            return false;
        if (!game.hasGameOption(GameOption.BG_COEXIST_WITH_ALLY) && ally instanceof BGFaction && hasForce("Advisor"))
            return true;
        return hasActiveFaction(ally);
    }

    private boolean notEcazAllyException(Game game, Faction faction) {
        return (!(faction instanceof EcazFaction) || getActiveFactions(game).stream().noneMatch(f -> f.getName().equals(faction.getAlly())))
                && (!faction.getAlly().equals("Ecaz") || getActiveFactions(game).stream().noneMatch(f -> f instanceof EcazFaction));
    }

    protected boolean onlyEcazAndAllyPresent(Game game) {
        if (getActiveFactions(game).size() == 2) {
            EcazFaction ecaz = game.getEcazFactionOrNull();
            return ecaz != null && ecaz.hasAlly() && hasActiveFaction(ecaz) && hasActiveFaction(game.getFaction(ecaz.getAlly()));
        }
        return false;
    }

    public String getAggregateTerritoryName() {
        int endLocation = territoryName.indexOf(" (");
        if (endLocation == -1) return territoryName;
        return territoryName.substring(0, endLocation);
    }
}
