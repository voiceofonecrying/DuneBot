package constants;

import model.TechToken;

public final class Emojis {
    public static final String MOD_EMPEROR = ":god_emperor:";
    public static final String ATREIDES = ":atreides:";
    public static final String ATREIDES_TROOP = ":atreides_troop:";
    public static final String BG = ":bg:";
    public static final String BG_FIGHTER = ":bg_fighter:";
    public static final String BG_ADVISOR = ":bg_advisor:";
    public static final String BT = ":bt:";
    public static final String BT_TROOP = ":bt_troop:";
    public static final String CHOAM = ":choam:";
    public static final String CHOAM_TROOP = ":choam_troop:";
    public static final String ECAZ = ":ecaz:";
    public static final String ECAZ_TROOP = ":ecaz_troop:";
    public static final String EMPEROR = ":emperor:";
    public static final String EMPEROR_TROOP = ":emperor_troop:";
    public static final String EMPEROR_SARDAUKAR = ":emperor_sardaukar:";
    public static final String FREMEN = ":fremen:";
    public static final String FREMEN_TROOP = ":fremen_troop:";
    public static final String FREMEN_FEDAYKIN = ":fremen_fedaykin:";
    public static final String GUILD = ":guild:";
    public static final String GUILD_TROOP = ":guild_troop:";
    public static final String HARKONNEN = ":harkonnen:";
    public static final String HARKONNEN_TROOP = ":harkonnen_troop:";
    public static final String IX = ":ix:";
    public static final String IX_SUBOID = ":ixian_suboid:";
    public static final String IX_CYBORG = ":ixian_cyborg:";
    public static final String MORITANI = ":moritani:";
    public static final String MORITANI_TROOP = ":moritani_troop:";
    public static final String RICHESE = ":rich:";
    public static final String RICHESE_TROOP = ":richese_troop:";
    public static final String SPICE = ":spice4:";
    public static final String TREACHERY = ":treachery:";
    public static final String DUNE_RULEBOOK = ":DuneRulebook01:";
    public static final String WEIRDING = ":weirding:";
    public static final String WORM = ":worm:";
    public static final String AXLOTL_TANKS = ":axlotltanks:";
    public static final String HEIGHLINERS = ":heighliners:";
    public static final String SPICE_PRODUCTION = ":spiceproduction:";
    public static final String TRANSPARENT_WORM = ":transparent_worm:";
    public static final String NO_FIELD = ":no_field:";

    private Emojis() {
    }

    public static String getFactionEmoji(String factionName) {
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
        return "faction";
    }

    public static String getForceEmoji(String forceName) {
        switch (forceName) {
            case "Fremen" -> {
                return FREMEN_TROOP;
            }
            case "Fremen*" -> {
                return FREMEN_FEDAYKIN;
            }
            case "Atreides" -> {
                return ATREIDES_TROOP;
            }
            case "Harkonnen" -> {
                return HARKONNEN_TROOP;
            }
            case "BG" -> {
                return BG_FIGHTER;
            }
            case "Advisor" -> {
                return BG_ADVISOR;
            }
            case "Guild" -> {
                return GUILD_TROOP;
            }
            case "Emperor" -> {
                return EMPEROR_TROOP;
            }
            case "Emperor*" -> {
                return EMPEROR_SARDAUKAR;
            }
            case "CHOAM" -> {
                return CHOAM_TROOP;
            }
            case "BT" -> {
                return BT_TROOP;
            }
            case "Richese" -> {
                return RICHESE_TROOP;
            }
            case "NoField" -> {
                return NO_FIELD;
            }
            case "Ix" -> {
                return IX_SUBOID;
            }
            case "Ix*" -> {
                return IX_CYBORG;
            }
            case "Ecaz" -> {
                return ECAZ_TROOP;
            }
            case "Moritani" -> {
                return MORITANI_TROOP;
            }
        }
        return "force";
    }

    public static String getTechTokenEmoji(String techToken) {
        switch (techToken) {
            case TechToken.AXLOTL_TANKS -> {
                return AXLOTL_TANKS;
            }
            case TechToken.HEIGHLINERS -> {
                return HEIGHLINERS;
            }
            case TechToken.SPICE_PRODUCTION -> {
                return SPICE_PRODUCTION;
            }
        }
        return " tech token ";
    }
}
