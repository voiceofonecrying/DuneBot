package model;

public class Shipment {
    private int force;
    private int specialForce;
    private String territoryName;
    private boolean hasShipped;
    private int noField;
    private boolean toReserves;
    private String crossShipFrom;

    public Shipment() {
    }

    public void clear() {
        this.territoryName = "";
        this.force = 0;
        this.specialForce = 0;
        this.hasShipped = true;
        this.noField = -1;
        this.toReserves = false;
        this.crossShipFrom = "";
    }

    public int getForce() {
        return force;
    }

    public void setForce(int force) {
        this.force = force;
    }

    public int getSpecialForce() {
        return specialForce;
    }

    public void setSpecialForce(int specialForce) {
        this.specialForce = specialForce;
    }

    public String getTerritoryName() {
        return territoryName;
    }

    public void setTerritoryName(String territoryName) {
        this.territoryName = territoryName;
    }

    public boolean hasShipped() {
        return hasShipped;
    }

    public void setShipped(boolean hasShipped) {
        this.hasShipped = hasShipped;
    }

    public int getNoField() {
        return noField;
    }

    public void setNoField(int noField) {
        this.noField = noField;
    }

    public boolean isToReserves() {
        return toReserves;
    }

    public void setToReserves(boolean toReserves) {
        this.toReserves = toReserves;
    }

    public String getCrossShipFrom() {
        if (crossShipFrom == null) crossShipFrom = "";
        return crossShipFrom;
    }

    public void setCrossShipFrom(String crossShipFrom) {
        this.crossShipFrom = crossShipFrom;
    }
}