package constants;

import java.awt.*;

public class Colors {
    public static final Color ATREIDES = Color.decode("#59602E");
    public static final Color BG = Color.decode("#4B5576");
    public static final Color BT = Color.decode("#79009A");
    public static final Color CHOAM = Color.decode("#CF1E24");
    public static final Color ECAZ = Color.decode("#965695");
    public static final Color EMPEROR = Color.decode("#A03929");
    public static final Color FREMEN = Color.decode("#D69733");
    public static final Color GUILD = Color.decode("#C8622F");
    public static final Color IX = Color.decode("#B9B786");
    public static final Color HARKONNEN = Color.decode("#3B3530");
    public static final Color MORITANI = Color.decode("#419BA8");
    public static final Color RICHESE = Color.decode("#B1AFA3");

    public static Color getFactionColor(String factionName) {
        switch (factionName) {
            case "Fremen" -> {
                return FREMEN;
            }
            case "Atreides" -> {
                return ATREIDES;
            }
            case "Harkonnen" -> {
                return HARKONNEN;
            }
            case "BG" -> {
                return BG;
            }
            case "Guild" -> {
                return GUILD;
            }
            case "Emperor" -> {
                return EMPEROR;
            }
            case "CHOAM" -> {
                return CHOAM;
            }
            case "BT" -> {
                return BT;
            }
            case "Richese" -> {
                return RICHESE;
            }
            case "Ix" -> {
                return IX;
            }
            case "Ecaz" -> {
                return ECAZ;
            }
            case "Moritani" -> {
                return MORITANI;
            }
        }
        throw new IllegalArgumentException("Invalid faction name: " + factionName);
    }
}
