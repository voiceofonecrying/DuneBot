package model;

import constants.Emojis;
import enums.MoveType;
import helpers.Exclude;
import model.Force;
import model.Game;
import model.Territory;
import model.factions.Faction;
import model.factions.FremenFaction;
import model.factions.IdualiFaction;

import java.awt.Point;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

public class IdualiTerritory extends Territory {
    private static final String STRONGHOLD_TERRITORY = "Sietch Jacurutu";
    public static final List<String> FREMEN_MAY_SHIP = List.of("The Great Flat", "The Greater Flat", "Funeral Plain");
    private final String adjacentTerritoryName;
    private final String publicName;
    private final int rotation;
    private final Point drawPoint;
    private final List<Point> forcePlacementPoints;
    @Exclude
    private Game game;

    public IdualiTerritory(Game game, String territoryName,  int sector, String adjacentTerritoryName) {
		boolean fremenCanShip = FREMEN_MAY_SHIP.contains(adjacentTerritoryName);
        super(territoryName, sector, true, false, false, false, fremenCanShip);
        this.game = game;
		this.publicName = "Sector " + sector + " Location";
		this.adjacentTerritoryName = adjacentTerritoryName;
		this.inIdualiRange = true;

        int x;
        int y;

        switch(sector) {
            case 1 -> {
                if (adjacentTerritoryName.equals("Meridian")) {
                    x = 450;
                    y = 1030;
                    rotation = 50;
                } else {
                    x = 590;
                    y = 1030;
                    rotation = 340;
                }
            }
            case 2 -> {
                if (adjacentTerritoryName.equals("Cielago South (East Sector)")) {
                    x = 590;
                    y = 950;
                    rotation = 320;
                } else {
                    x = 710;
                    y = 990;
                    rotation = 330;
                }
            }
            case 3 -> {
                if (adjacentTerritoryName.equals("South Mesa (South Sector)")) {
                    x = 880;
                    y = 920;
                    rotation = 40;
                } else {
                    x = 790;
                    y = 990;
                    rotation = 330;
                }
            }
            case 4 -> {
                rotation = 280;
                x = 885;
                y = 700;
            }
            case 5 -> {
                rotation = 290;
                x = 840;
                y = 680;
            }
            case 6 -> {
                rotation = 250;
                x = 840;
                y = 640;
            }
            case 7 -> {
                rotation = 240;
                if (adjacentTerritoryName.equals("Gara Kulon")) {
                    x = 420;
                    y = 970;
                } else {
                    x = 420;
                    y = 970;
                }
            }
            case 8 -> {
                rotation = 220;
                if (adjacentTerritoryName.equals("Old Gap (East Sector)")) {
                    x = 840;
                    y = 120;
                } else if (adjacentTerritoryName.equals("Basin")) {
                    x = 857;
                    y = 135;
                } else {
                    x = 890;
                    y = 150;
                }
            }
            case 9 -> {
                rotation = 200;
                x = 647;
                y = 115;
            }
            case 10 -> {
                rotation = 180;
                if (adjacentTerritoryName.equals("Old Gap (West Sector)")) {
                    x = 420;
                    y = 970;
                } else {
                    x = 420;
                    y = 970;
                }
            }
            case 11 -> {
                rotation = 160;
                x = 355;
                y = 50;
            }
            case 12 -> {
                rotation = 140;
                x = 202;
                y = 139;
            }
            case 13 -> {
                rotation = 120;
                if (adjacentTerritoryName.equals("Bight Of The Cliff (North Sector)" )) {
                    x = 88;
                    y = 275;
                } else {
                    x = 88;
                    y = 275;
                }
            }
            case 14 -> {
                if (adjacentTerritoryName.equals("Funeral Plain")) {
                    rotation = 90;
                    x = 37;
                    y = 441;
                } else if (adjacentTerritoryName.equals("The Great Flat")) {
                    rotation = 100;
                    x = 34;
                    y = 493;
                } else {
                    rotation = 80;
                    x = 50;
                    y = 410;
                }
            }
            case 15 -> {
                rotation = 80;
                if (adjacentTerritoryName.equals("The Greater Flat")) {
                    x = 34;
                    y = 590;
                } else {
                    x = 34;
                    y = 678;
                }
            }
            case 16 -> {
                rotation = 60;
                x = 88;
                y = 785;
            }
            case 17 -> {
                rotation = 40;
                x = 215;
                y = 880;
            }
            default -> {
                rotation = 20;
                x = 325;
                y = 945;
            }
        }

        drawPoint = new Point(x, y);
        forcePlacementPoints = new ArrayList<>();
        forcePlacementPoints.add(new Point(x, y - 10));
        forcePlacementPoints.add(new Point(x, y + 10));
    }

    @Override
    public String getPublicName() {
        return publicName;
    }

    public void setGame(Game game) {
        this.game = game;
    }

	public void establishAdjacency() {
		Territory adjacent = game.getTerritory(adjacentTerritoryName);
        String aggregateName = adjacent.getAggregateTerritoryName();

        for (Territory territory : game.getTerritories().values()) {
            if (territory.getAggregateTerritoryName().equals(aggregateName))
                territory.setAdjacentToIdualiTerritory();
        }

        game.putTerritoryInAnotherTerritory(this, adjacent);
	}

    @Override
    public int costToShipInto() {
        return 2;
    }

    public void callParentAddForces(String forceName, int amount) {
        super.addForces(forceName, amount);
    }

	@Override
	public boolean factionMayNotEnter(Game game, Faction faction, boolean isShipment, boolean isInitialPlacement) {
        if (isInitialPlacement) {
            return !(faction instanceof IdualiFaction);
        } else if (isShipment) {
            if (faction instanceof FremenFaction && !inFremenRange && !faction.getAlly().equals("Guild") && game.getIdualiFaction().getOccupier() != faction)
                return true;
            if (onlyEcazAndAllyPresent(game))
                return false;
            return getActiveFactions(game).size() >= 2 && !hasActiveFaction(faction) && notEcazAllyException(game, faction);
        }
        return (super.notEcazAllyException(game, faction) && !onlyEcazAndAllyPresent(game)
                && (faction.hasAlly() && hasAllyForces(game, faction))
                || (getActiveFactions(game).size() >= 2 && !hasActiveFaction(faction)));
    }

    @Override
    public void addForces(String forceName, int amount) {
        if (amount == 0)
            return;
        super.addForces(forceName, amount);

        if (!game.isSetupFinished())
            return;
        String factionName = Force.getFactionNameFromForceName(forceName);
        Faction faction = game.getFaction(factionName);
        Random r = new Random();
        int m = r.nextInt(3) + 1;
        String message = "";

        switch(territoryName) {
            case "Sietch Jacurutu" -> {
                    switch(m) {
                    case 1 -> message = "";
                    case 2 -> message = "";
                    case 3 -> message = "";
                }
            }
            case "Shuloch" -> {
                    switch(m) {
                    case 1 -> message = "";
                    case 2 -> message = "";
                    case 3 -> message = "";
                }
            }
            case "Kharuz Hollow" -> {
                    switch(m) {
                    case 1 -> message = "";
                    case 2 -> message = "";
                    case 3 -> message = "";
                }
            }
        }

        faction.getChat().publish(message);
    }

    public Point getForcePlacementPoint(int key) {
        return forcePlacementPoints.get(key);
    }

    public Point getDrawPoint() {
        return drawPoint;
    }

    public int getRotation() {
        return rotation;
    }
}
