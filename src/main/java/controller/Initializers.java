package controller;

import model.Faction;
import model.Game;
import model.Territory;
import org.json.JSONArray;
import org.json.JSONObject;

public class Initializers {

    public static void newFaction(Faction faction, Game gameState) {

        gameState.getJSONObject("game_state").getJSONObject("factions").put(faction.getName(), faction);
        JSONObject resources = gameState.getFaction(faction.getName()).getJSONObject("resources");
        JSONArray traitorDeck = gameState.getDeck("traitor_deck");
        resources.put("leaders", new JSONArray());
        resources.put("traitors", new JSONArray());
        resources.put("treachery_hand", new JSONArray());
        switch (faction.getName()) {
            case "Atreides" -> {
                faction.setEmoji("<:atreides:991763327996923997>");
                resources.put("spice", 10);
                resources.put("reserves", 10);
                resources.getJSONArray("leaders").put("lady jessica - 5");
                resources.getJSONArray("leaders").put("thufir hawat - 5");
                resources.getJSONArray("leaders").put("gurney halleck - 4");
                resources.getJSONArray("leaders").put("duncan idaho - 2");
                resources.getJSONArray("leaders").put("dr wellington yueh - 1");
                traitorDeck.put("lady jessica - 5");
                traitorDeck.put("thufir hawat - 5");
                traitorDeck.put("gurney halleck - 4");
                traitorDeck.put("duncan idaho - 2");
                traitorDeck.put("dr wellington yueh - 1");
                resources.put("forces lost", 0);
                gameState.getJSONObject("game_state").getJSONObject("game_board").getJSONObject("Arrakeen").getJSONObject("forces").put("Atreides", 10);
            }
            case "Harkonnen" -> {
                faction.setEmoji("<:harkonnen:991763320333926551>");
                resources.put("spice", 10);
                resources.put("reserves", 10);
                resources.getJSONArray("leaders").put("feyd rautha - 6");
                resources.getJSONArray("leaders").put("beast rabban - 4");
                resources.getJSONArray("leaders").put("piter de vries - 3");
                resources.getJSONArray("leaders").put("captian iakin nefud - 2");
                resources.getJSONArray("leaders").put("umman kudu - 1");
                traitorDeck.put("feyd rautha - 6");
                traitorDeck.put("beast rabban - 4");
                traitorDeck.put("piter de vries - 3");
                traitorDeck.put("captian iakin nefud - 2");
                traitorDeck.put("umman kudu - 1");
                gameState.getJSONObject("game_state").getJSONObject("game_board").getJSONObject("Carthag").getJSONObject("forces").put("Harkonnen", 10);
            }
            case "Emperor" -> {
                faction.setEmoji("<:emperor:991763323454500914>");
                resources.put("spice", 10);
                resources.put("reserves", 15);
                resources.put("sardaukar reserves", 5);
                resources.getJSONArray("leaders").put("hasimir fenring - 6");
                resources.getJSONArray("leaders").put("captain aramsham - 5");
                resources.getJSONArray("leaders").put("caid - 3");
                resources.getJSONArray("leaders").put("burseg - 3");
                resources.getJSONArray("leaders").put("bashar - 2");
                traitorDeck.put("hasimir fenring - 6");
                traitorDeck.put("captain aramsham - 5");
                traitorDeck.put("caid - 3");
                traitorDeck.put("burseg - 3");
                traitorDeck.put("bashar - 2");
            }
            case "Fremen" -> {
                faction.setEmoji("<:fremen:991763322225577984>");
                resources.put("spice", 3);
                resources.put("reserves", 17);
                resources.put("fedaykin reserves", 3);
                resources.getJSONArray("leaders").put("stilgar - 7");
                resources.getJSONArray("leaders").put("chani - 6");
                resources.getJSONArray("leaders").put("otheym - 5");
                resources.getJSONArray("leaders").put("shadout mapes - 3");
                resources.getJSONArray("leaders").put("jamis - 2");
                traitorDeck.put("stilgar - 7");
                traitorDeck.put("chani - 6");
                traitorDeck.put("otheym - 5");
                traitorDeck.put("shadout mapes - 3");
                traitorDeck.put("jamis - 2");
            }
            case "BG" -> {
                faction.setEmoji("<:bg:991763326830911519>");
                resources.put("spice", 5);
                resources.put("reserves", 20);
                resources.getJSONArray("leaders").put("alia - 5");
                resources.getJSONArray("leaders").put("margot lady fenring - 5");
                resources.getJSONArray("leaders").put("princess irulan - 5");
                resources.getJSONArray("leaders").put("mother ramallo - 5");
                resources.getJSONArray("leaders").put("wanna yueh - 5");
                traitorDeck.put("alia - 5");
                traitorDeck.put("margot lady fenring - 5");
                traitorDeck.put("princess irulan - 5");
                traitorDeck.put("mother ramallo - 5");
                traitorDeck.put("wanna yueh - 5");
            }
            case "Guild" -> {
                faction.setEmoji("<:guild:991763321290244096>");
                resources.put("spice", 5);
                resources.put("reserves", 15);
                resources.getJSONArray("leaders").put("staban tuek - 5");
                resources.getJSONArray("leaders").put("master bewt - 3");
                resources.getJSONArray("leaders").put("esmar tuek - 3");
                resources.getJSONArray("leaders").put("soo soo sook - 2");
                resources.getJSONArray("leaders").put("guild rep - 1");
                gameState.getJSONObject("game_state").getJSONObject("game_board").getJSONObject("Tuek's Sietch").getJSONObject("forces").put("Guild", 10);
                traitorDeck.put("staban tuek - 5");
                traitorDeck.put("master bewt - 3");
                traitorDeck.put("esmar tuek - 3");
                traitorDeck.put("soo soo sook - 2");
                traitorDeck.put("guild rep - 1");
            }
            case "Ix" -> {
                faction.setEmoji("<:ix:991763319406997514>");
                resources.put("spice", 10);
                resources.put("suboid reserves", 10);
                resources.put("cyborg reserves", 4);
                resources.getJSONArray("leaders").put("ctair pilru - 5");
                resources.getJSONArray("leaders").put("tessia vernius - 5");
                resources.getJSONArray("leaders").put("dominic vernius - 4");
                resources.getJSONArray("leaders").put("kailea vernius - 2");
                resources.getJSONArray("leaders").put("cammar pilru - 1");
                gameState.getJSONObject("game_state").getJSONObject("game_board").put("Hidden Mobile Stronghold", new Territory("Hidden Mobile Stronghold", -1, false, true));
                gameState.getJSONObject("game_state").getJSONObject("game_board").getJSONObject("Hidden Mobile Stronghold").getJSONObject("forces").put("Ix suboid", 3);
                gameState.getJSONObject("game_state").getJSONObject("game_board").getJSONObject("Hidden Mobile Stronghold").getJSONObject("forces").put("Ix cyborg", 3);
                traitorDeck.put("ctair pilru - 5");
                traitorDeck.put("tessia vernius - 5");
                traitorDeck.put("dominic vernius - 4");
                traitorDeck.put("kailea vernius - 2");
                traitorDeck.put("cammar pilru - 1");
            }
            case "BT" -> {
                faction.setEmoji("<:bt:991763325576810546>");
                resources.put("spice", 5);
                resources.put("reserves", 20);
                resources.getJSONArray("leaders").put("zoal - -1");
                resources.getJSONArray("leaders").put("hidar fen ajidica - 4");
                resources.getJSONArray("leaders").put("master zaaf - 3");
                resources.getJSONArray("leaders").put("wykk - 2");
                resources.getJSONArray("leaders").put("blin - 1");
                traitorDeck.put("zoal - -1");
                traitorDeck.put("hidar fen ajidica - 4");
                traitorDeck.put("master zaaf - 3");
                traitorDeck.put("wykk - 2");
                traitorDeck.put("blin - 1");
            }
            case "CHOAM" -> {
                faction.setEmoji("<:choam:991763324624703538>");
                resources.put("spice", 2);
                resources.put("reserves", 20);
                resources.getJSONArray("leaders").put("frankos aru - 4");
                resources.getJSONArray("leaders").put("lady jalma - 4");
                resources.getJSONArray("leaders").put("rajiv londine - 3");
                resources.getJSONArray("leaders").put("duke verdun - 3");
                resources.getJSONArray("leaders").put("viscount tull - 2");
                resources.getJSONArray("leaders").put("auditor - 2");
                resources.put("inflation token", "double");
                traitorDeck.put("frankos aru - 4");
                traitorDeck.put("lady jalma - 4");
                traitorDeck.put("rajiv londine - 3");
                traitorDeck.put("duke verdun - 3");
                traitorDeck.put("viscount tull - 2");
                traitorDeck.put("auditor - 2");
            }
            case "Rich" -> {
                faction.setEmoji("<:rich:991763318467465337>");
                resources.put("spice", 5);
                resources.put("reserves", 20);
                resources.getJSONArray("leaders").put("ein calimar - 5");
                resources.getJSONArray("leaders").put("lady helena - 4");
                resources.getJSONArray("leaders").put("flinto kinnis - 3");
                resources.getJSONArray("leaders").put("haloa rund - 2");
                resources.getJSONArray("leaders").put("talis balt - 2");
                resources.put("no field 0", 0);
                resources.put("no field 3", 3);
                resources.put("no field 5", 5);
                resources.put("cache", new JSONObject());
                resources.getJSONObject("cache").put("Ornithoper", "Special - Movement");
                resources.getJSONObject("cache").put("Residual Poison", "Special");
                resources.getJSONObject("cache").put("Semuta Drug", "Special");
                resources.getJSONObject("cache").put("Stone Burner", "Weapon - Special");
                resources.getJSONObject("cache").put("Mirror Weapon", "Weapon - Special");
                resources.getJSONObject("cache").put("Portable Snooper", "Defense - Poison");
                resources.getJSONObject("cache").put("Distrans", "Special");
                resources.getJSONObject("cache").put("Juice of Sapho", "Special");
                resources.getJSONObject("cache").put("Karama", "Special");
                resources.getJSONObject("cache").put("Nullentropy", "Special");
                traitorDeck.put("ein calimar - 5");
                traitorDeck.put("lady helena - 4");
                traitorDeck.put("flinto kinnis - 3");
                traitorDeck.put("haloa rund - 2");
                traitorDeck.put("talis balt - 2");
            }
        }
    }



    public static JSONObject buildBoard() {

        JSONObject territories = new JSONObject();

        territories.put("Cielago North(1)", new Territory("Cielago North", 1, false, false));
        territories.put("Cielago Depression(1)", new Territory("Cielago Depression", 1, false, false));
        territories.put("Meridian(1)", new Territory("Meridian", 1, false, false));
        territories.put("Cielago South(1)", new Territory("Cielago South", 1, false, false));
        territories.put("Cielago North(1)", new Territory("Cielago North", 2, false, false));
        territories.put("Cielago Depression(2)", new Territory("Cielago Depression", 2, false, false));
        territories.put("Cielago South(2)", new Territory("Cielago South", 2, false, false));
        territories.put("Cielago East(2)", new Territory("Cielago East", 2, false, false));
        territories.put("Harg Pass(3)", new Territory("Harg Pass", 3, false, false));
        territories.put("False Wall South(3)", new Territory("False Wall South", 3, true, false));
        territories.put("Cielago East(3)", new Territory("Cielago East", 3, false, false));
        territories.put("South Mesa(3)", new Territory("South Mesa", 3, false, false));
        territories.put("Harg Pass(4)", new Territory("Harg Pass", 4, false, false));
        territories.put("False Wall East(4)", new Territory("False Wall East", 4, true, false));
        territories.put("The Minor Erg(4)", new Territory("The Minor Erg", 4, false, false));
        territories.put("False Wall South(4)", new Territory("False Wall South", 4, true, false));
        territories.put("Pasty Mesa(4)", new Territory("Pasty Mesa", 4, true, false));
        territories.put("South Mesa(4)", new Territory("South Mesa", 4, false, false));
        territories.put("Tuek's Sietch", new Territory("Tuek's Sietch", 4, true, true));
        territories.put("False Wall East(5)", new Territory("False Wall East", 5, true, false));
        territories.put("The Minor Erg(5)", new Territory("The Minor Erg", 5, false, false));
        territories.put("Pasty Mesa(5)", new Territory("Pasty Mesa", 5, true, false));
        territories.put("South Mesa(5)", new Territory("South Mesa", 5, false, false));
        territories.put("False Wall East(6)", new Territory("False Wall East", 6, true, false));
        territories.put("The Minor Erg(6)", new Territory("The Minor Erg", 6, false, false));
        territories.put("Pasty Mesa(6)", new Territory("Pasty Mesa", 6, true, false));
        territories.put("Red Chasm(6)", new Territory("Red Chasm", 6, false, false));
        territories.put("False Wall East(7)", new Territory("False Wall East", 7, true, false));
        territories.put("The Minor Erg(7)", new Territory("The Minor Erg", 7, false, false));
        territories.put("Shield Wall(7)", new Territory("Shield Wall", 7, true, false));
        territories.put("Pasty Mesa(7)", new Territory("Pasty Mesa", 7, true, false));
        territories.put("Gara Kulon(7)", new Territory("Gara Kulon", 7, false, false));
        territories.put("False Wall East(8)", new Territory("False Wall East", 8, true, false));
        territories.put("Imperial Basin(8)", new Territory("Imperial Basin", 8, true, false));
        territories.put("Shield Wall(8)", new Territory("Shield Wall", 8, true, false));
        territories.put("Hole in the Rock(8)", new Territory("Hole in the Rock", 8, false, false));
        territories.put("Rim Wall West(8)", new Territory("Rim Wall West", 8, true, false));
        territories.put("Basin(8)", new Territory("Basin", 8, false, false));
        territories.put("Old Gap(8)", new Territory("Old Gap", 8, false, false));
        territories.put("Sihaya Ridge(8)", new Territory("Sihaya Ridge", 8, false, false));
        territories.put("Imperial Basin(9)", new Territory("Imperial Basin", 9, true, false));
        territories.put("Arrakeen", new Territory("Arrakeen", 9, true, true));
        territories.put("Old Gap(9)", new Territory("Old Gap", 9, false, false));
        territories.put("Imperial Basin(10)", new Territory("Imperial Basin", 10, true, false));
        territories.put("Arsunt(10)", new Territory("Arsunt", 10, false, false));
        territories.put("Carthag", new Territory("Carthag", 10, true, true));
        territories.put("Tsimpo(10)", new Territory("Tsimpo", 10, false, false));
        territories.put("Broken Land(10)", new Territory("Broken Land", 10, false, false));
        territories.put("Old Gap(10)", new Territory("Old Gap", 10, false, false));
        territories.put("Arsunt(11)", new Territory("Arsunt", 11, false, false));
        territories.put("Hagga Basin(11)",new Territory("Hagga Basin", 11, false, false));
        territories.put("Tsimpo(11)", new Territory("Tsimpo", 11, false, false));
        territories.put("Plastic Basin(11)", new Territory("Plastic Basin", 11, true, false));
        territories.put("Broken Land(11)", new Territory("Broken Land", 11, false, false));
        territories.put("Hagga Basin(12)", new Territory("Hagga Basin", 12, false, false));
        territories.put("Plastic Basin(12)", new Territory("Plastic Basin", 12, true, false));
        territories.put("Tsimpo(12)", new Territory("Tsimpo", 12, false, false));
        territories.put("Rock Outcroppings(12)", new Territory("Rock Outcroppings", 12, false, false));
        territories.put("Wind Pass(13)", new Territory("Wind Pass", 13, false, false));
        territories.put("Plastic Basin(13)", new Territory("Plastic Basin", 13, true, false));
        territories.put("Bight of the Cliff(13)", new Territory("Bight of the Cliff", 13, false, false));
        territories.put("Sietch Tabr", new Territory("Sietch Tabr", 13, true, true));
        territories.put("Rock Outcroppings(13)", new Territory("Rock Outcroppings", 13, false, false));
        territories.put("Wind Pass(14)", new Territory("Wind Pass", 14, false, false));
        territories.put("The Great Flat(14)", new Territory("The Great Flat", 14, false, false));
        territories.put("Funeral Plain(14)", new Territory("Funeral Plain", 14, false, false));
        territories.put("Bight of the Cliff(14)", new Territory("Bight of the Cliff", 14, false, false));
        territories.put("Wind Pass(15)", new Territory("Wind Pass", 15, false, false));
        territories.put("The Greater Flat(15)", new Territory("The Greater Flat", 15, false, false));
        territories.put("Habbanya Erg(15)", new Territory("Habbanya Erg", 15, false, false));
        territories.put("False Wall West(15)", new Territory("False Wall West", 15, true, false));
        territories.put("Wind Pass North(16)", new Territory("Wind Pass North", 16, false, false));
        territories.put("Wind Pass(16)", new Territory("Wind Pass", 16, false, false));
        territories.put("False Wall West(16)", new Territory("False Wall West", 16, true, false));
        territories.put("Habbanya Erg(16)", new Territory("Habbanya Erg", 16, false, false));
        territories.put("Habbanya Ridge Flat(16)", new Territory("Habbanya Ridge Flat", 16, false, false));
        territories.put("Habbanya Sietch", new Territory("Habbanya Sietch", 16, true, true));
        territories.put("Wind Pass North(17)", new Territory("Wind Pass North", 17, false, false));
        territories.put("Cielago West(17)", new Territory("Cielago West", 17, false, false));
        territories.put("False Wall West(17)", new Territory("False Wall West", 17, true, false));
        territories.put("Habbanya Ridge Flat(17)", new Territory("Habbanya Ridge Flat", 17, false, false));
        territories.put("Cielago North(18)", new Territory("Cielago North", 18, false, false));
        territories.put("Cielago Depression(18)", new Territory("Cielago Depression", 18, false, false));
        territories.put("Cielago West(18)", new Territory("Cielago West", 18, false, false));
        territories.put("Meridian(18)", new Territory("Meridian", 18, false, false));
        territories.put("Polar Sink", new Territory("Polar Sink", -1, false, false));
        return territories;
    }

    public static JSONArray buildSpiceDeck() {
        JSONArray spiceDeck = new JSONArray();

        spiceDeck.put("Habbanya Ridge Flat(17) - 10");
        spiceDeck.put("Cielago South(1) - 12");
        spiceDeck.put("Broken Land(11) - 8");
        spiceDeck.put("South Mesa(4) - 10");
        spiceDeck.put("Sihaya Ridge(8) - 6");
        spiceDeck.put("Shai-Hulud");
        spiceDeck.put("Shai-Hulud");
        spiceDeck.put("Shai-Hulud");
        spiceDeck.put("Shai-Hulud");
        spiceDeck.put("Shai-Hulud");
        spiceDeck.put("Shai-Hulud");
        spiceDeck.put("Hagga Basin(12) - 6");
        spiceDeck.put("Red Chasm(6) - 8");
        spiceDeck.put("The Minor Erg(7) - 8");
        spiceDeck.put("Cielago North(2) - 8");
        spiceDeck.put("Funeral Plain(14) - 6");
        spiceDeck.put("The Great Flat(14) - 10");
        spiceDeck.put("Habbanya Erg(15) - 8");
        spiceDeck.put("Old Gap(9) - 6");
        spiceDeck.put("Rock Outcroppings(13) - 6");
        spiceDeck.put("Wind Pass North(16) - 6");

        return spiceDeck;
    }

    public static JSONArray buildTreacheryDeck() {
        JSONArray treacheryDeck = new JSONArray();

        treacheryDeck.put("Lasgun | Weapon - Special");
        treacheryDeck.put("Chaumas | Weapon - Poison");
        treacheryDeck.put("Chaumurky | Weapon - Poison");
        treacheryDeck.put("Ellaca Drug | Weapon - Poison");
        treacheryDeck.put("Gom Jabbar | Weapon - Poison");
        treacheryDeck.put("Stunner | Weapon - Projectile");
        treacheryDeck.put("Slip Tip | Weapon - Projectile");
        treacheryDeck.put("Maula Pistol | Weapon - Projectile");
        treacheryDeck.put("Crysknife | Weapon - Projectile");
        treacheryDeck.put("Shield | Defense - Projectile");
        treacheryDeck.put("Shield | Defense - Projectile");
        treacheryDeck.put("Shield | Defense - Projectile");
        treacheryDeck.put("Shield | Defense - Projectile");
        treacheryDeck.put("Snooper | Defense - Poison");
        treacheryDeck.put("Snooper | Defense - Poison");
        treacheryDeck.put("Snooper | Defense - Poison");
        treacheryDeck.put("Snooper | Defense - Poison");
        treacheryDeck.put("La, La, La | Worthless Card");
        treacheryDeck.put("Baliset | Worthless Card");
        treacheryDeck.put("Kulon | Worthless Card");
        treacheryDeck.put("Trip to Gamont | Worthless Card");
        treacheryDeck.put("Jubba Cloak | Worthless Card");
        treacheryDeck.put("Tleilaxu Ghola | Special");
        treacheryDeck.put("Truthtrance | Special");
        treacheryDeck.put("Truthtrance | Special");
        treacheryDeck.put("Weather Control | Special");
        treacheryDeck.put("Karama | Special");
        treacheryDeck.put("Karama | Special");
        treacheryDeck.put("Family Atomics | Special");
        treacheryDeck.put("Hajr | Special");
        treacheryDeck.put("Cheap Hero | Special");
        treacheryDeck.put("Cheap Hero | Special");
        treacheryDeck.put("Cheap Hero | Special");

        return treacheryDeck;
    }
}
