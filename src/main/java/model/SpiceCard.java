package model;

public class SpiceCard extends Card{
    private final String territory;
    private final Integer sector;
    private final Integer spice;
    private final boolean special;
    public SpiceCard(String name, String territory, int sector, int spice, boolean special) {
        super(name);
        this.territory = territory;
        this.sector = sector;
        this.spice = spice;
        this.special = special;
    }

    public SpiceCard(String name) {
        super(name);
        this.territory = null;
        this.sector = null;
        this.spice = null;
        this.special = true;
    }

    public String getTerritory() {
        return territory;
    }

    public Integer getSector() {
        return sector;
    }

    public Integer getSpice() {
        return spice;
    }

    public boolean isSpecial() {
        return special;
    }
}
