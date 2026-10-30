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
                    x = 402;
                    y = 963;
                    rotation = 25;
                } else {
                    x = 543;
                    y = 954;
                    rotation = 320;
                }
            }
            case 2 -> {
                if (adjacentTerritoryName.equals("Cielago South (East Sector)")) {
                    x = 580;
                    y = 960;
                    rotation = 340;
                } else {
                    x = 645;
                    y = 935;
                    rotation = 330;
                }
            }
            case 3 -> {
                if (adjacentTerritoryName.equals("South Mesa (South Sector)")) {
                    x = 770;
                    y = 861;
                    rotation = 330;
                } else {
                    x = 725;
                    y = 887;
                    rotation = 310;
                }
            }
            case 4 -> {
                rotation = 330;
                x = 825;
                y = 807;
            }
            case 5 -> {
                rotation = 290;
                x = 910;
                y = 590;
            }
            case 6 -> {
                rotation = 230;
                x = 910;
                y = 450;
            }
            case 7 -> {
                rotation = 230;
                x = 824;
                y = 265;
            }
            case 8 -> {
                rotation = 220;
                if (adjacentTerritoryName.equals("Old Gap (East Sector)")) {
                    x = 718;
                    y = 152;
                } else if (adjacentTerritoryName.equals("Basin")) {
                    x = 753;
                    y = 190;
                } else {
                    x = 807;
                    y = 225;
                }
            }
            case 9 -> {
                rotation = 200;
                x = 647;
                y = 115;
            }
            case 10 -> {
                if (adjacentTerritoryName.equals("Old Gap (West Sector)")) {
                    rotation = 200;
                    x = 540;
                    y = 94;
                } else {
                    rotation = 140;
                    x = 394;
                    y = 100;
                }
            }
            case 11 -> {
                rotation = 150;
                x = 305;
                y = 115;
            }
            case 12 -> {
                rotation = 170;
                x = 235;
                y = 158;
            }
            case 13 -> {
                if (adjacentTerritoryName.equals("Bight Of The Cliff (North Sector)" )) {
                    rotation = 90;
                    x = 68;
                    y = 370;
                } else {
                    rotation = 160;
                    x = 133;
                    y = 250;
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
                rotation = 70;
                if (adjacentTerritoryName.equals("The Greater Flat")) {
                    x = 37;
                    y = 565;
                } else {
                    x = 48;
                    y = 655;
                }
            }
            case 16 -> {
                rotation = 30;
                x = 130;
                y = 805;
            }
            case 17 -> {
                rotation = 40;
                x = 215;
                y = 880;
            }
            default -> {
                rotation = 20;
                x = 315;
                y = 937;
            }
        }

        drawPoint = new Point(x, y);
        forcePlacementPoints = new ArrayList<>();
        forcePlacementPoints.add(new Point(x, y - 14));
        forcePlacementPoints.add(new Point(x, y + 14));
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
