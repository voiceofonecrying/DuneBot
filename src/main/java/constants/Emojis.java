package constants;

public final class Emojis {
    public static final String MOD_EMPEROR = "<:god_emperor:1150080256917655652>";
    public static final String ATREIDES = "<:atreides:991763327996923997>";
    public static final String ATREIDES_TROOP = "<:atreides_troop:998567375131652196>";
    public static final String BG = "<:bg:991763326830911519>";
    public static final String BG_FIGHTER = "<:bg_fighter:998567372644438036>";
    public static final String BG_ADVISOR = "<:bg_advisor:998567373680427018>";
    public static final String BT = "<:bt:991763325576810546>";
    public static final String BT_TROOP = "<:bt_troop:998567371453255700>";
    public static final String CHOAM = "<:choam:991763324624703538>";
    public static final String CHOAM_TROOP = "<:choam_troop:998567370350141492>";
    public static final String ECAZ = "<:ecaz:1142126129105346590>";
    public static final String ECAZ_TROOP = "<:ecaz_troop:1153726788011298928>";
    public static final String EMPEROR = "<:emperor:991763323454500914>";
    public static final String EMPEROR_TROOP = "<:emperor_troop:998567367565115504>";
    public static final String EMPEROR_SARDAUKAR = "<:emperor_sardaukar:998567368739541113>";
    public static final String FREMEN = "<:fremen:991763322225577984>";
    public static final String FREMEN_TROOP = "<:fremen_troop:998567365547671713>";
    public static final String FREMEN_FEDAYKIN = "<:fremen_fedaykin:998567366432669806>";
    public static final String GUILD = "<:guild:991763321290244096>";
    public static final String GUILD_TROOP = "<:guild_troop:998567364461338634>";
    public static final String HARKONNEN = "<:harkonnen:991763320333926551>";
    public static final String HARKONNEN_TROOP = "<:harkonnen_troop:998567363412770856>";
    public static final String IX = "<:ix:991763319406997514>";
    public static final String IX_SUBOID = "<:ixian_suboid:998567360858427512>";
    public static final String IX_CYBORG = "<:ixian_cyborg:998567362024456254>";
    public static final String MORITANI = "<:moritani:1142126199775182879>";
    public static final String MORITANI_TROOP = "<:moritani_troop:1153670660552413235>";
    public static final String RICHESE = "<:rich:991763318467465337>";
    public static final String RICHESE_TROOP = "<:richese_troop:998567359579160606>";
    public static final String SPICE = "<:spice4:991763531798167573>";
    public static final String TREACHERY = "<:treachery:991763073281040518>";
    public static final String DUNE_RULEBOOK = "<:DuneRulebook01:991763013814198292>";
    public static final String WEIRDING = "<:weirding:991763071775297681>";
    public static final String WORM = ":worm:";
    public static final String AXLOTL_TANKS = "<:axlotltanks:991763497111269477>";
    public static final String HEIGHLINERS = "<:heighliners:991763495567769640>";
    public static final String SPICE_PRODUCTION = "<:spiceproduction:991763494267531304>";
    public static final String TRANSPARENT_WORM = "<:transparent_worm:991762942536187964>";
    public static final String NO_FIELD = "<:no_field:1121583397937106995>";

    private Emojis() {
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
            case "Ix" -> {
                return IX_SUBOID;
            }
            case "Ix*" -> {
                return IX_CYBORG;
            }
        }
        return " force ";
    }

    public static String getTechTokenEmoji(String techToken) {
        switch (techToken) {
            case "Axlotl Tanks" -> {
                return AXLOTL_TANKS;
            }
            case "Heighliners" -> {
                return HEIGHLINERS;
            }
            case "Spice Production" -> {
                return SPICE_PRODUCTION;
            }
        }
        return " tech token ";
    }
}
