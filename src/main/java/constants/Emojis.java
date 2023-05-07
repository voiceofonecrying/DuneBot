package constants;

public final class Emojis {
    public static String ATREIDES = "<:atreides:991763327996923997>";
    public static String ATREIDES_TROOP = "<:atreides_troop:998567375131652196>";
    public static String BG = "<:bg:991763326830911519>";
    public static String BG_FIGHTER = "<:bg_fighter:998567372644438036>";
    public static String BG_ADVISOR = "<:bg_advisor:998567373680427018>";
    public static String BT = "<:bt:991763325576810546>";
    public static String BT_TROOP = "<:bt_troop:998567371453255700>";
    public static String CHOAM = "<:choam:991763324624703538>";
    public static String CHOAM_TROOP = "<:choam_troop:998567370350141492>";
    public static String EMPEROR = "<:emperor:991763323454500914>";
    public static String EMPEROR_TROOP = "<:emperor_troop:998567367565115504>";
    public static String EMPEROR_SARDAUKAR = "<:emperor_sardaukar:998567368739541113>";
    public static String FREMEN = "<:fremen:991763322225577984>";
    public static String FREMEN_TROOP = "<:fremen_troop:998567365547671713>";
    public static String FREMEN_FEDAYKIN = "<:fremen_fedaykin:998567366432669806>";
    public static String GUILD = "<:guild:991763321290244096>";
    public static String GUILD_TROOP = "<:guild_troop:998567364461338634>";
    public static String HARKONNEN = "<:harkonnen:991763320333926551>";
    public static String HARKONNEN_TROOP = "<:harkonnen_troop:998567363412770856>";
    public static String IX = "<:ix:991763319406997514>";
    public static String IX_SUBOID = "<:ixian_suboid:998567360858427512>";
    public static String IX_CYBORG = "<:ixian_cyborg:998567362024456254>";
    public static String RICHESE = "<:rich:991763318467465337>";
    public static String RICHESE_TROOP = "<:richese_troop:998567359579160606>";
    public static String SPICE = "<:spice4:991763531798167573>";
    public static String TREACHERY = "<:treachery:991763073281040518>";
    public static String DUNE_RULEBOOK = "<:DuneRulebook01:991763013814198292>";
    public static String WEIRDING = "<:weirding:991763071775297681>";

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

    private Emojis() {}
}
