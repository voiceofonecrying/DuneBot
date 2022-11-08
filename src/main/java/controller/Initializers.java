package controller;

import model.Faction;
import model.Game;
import model.Territory;
import net.dv8tion.jda.internal.utils.tuple.Pair;
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
        gameState.getResources().getJSONObject("tanks_forces").put(faction.getName(), 0);
        switch (faction.getName()) {
            case "Atreides" -> {
                faction.put("free_revival", 2);
                faction.setEmoji("<:atreides:991763327996923997>");
                resources.put("spice", 10);
                resources.put("reserves", 10);
                resources.getJSONArray("leaders").put("Lady Jessica - 5" + faction.getEmoji());
                resources.getJSONArray("leaders").put("Thufir Hawat - 5" + faction.getEmoji());
                resources.getJSONArray("leaders").put("Gurney Halleck - 4" + faction.getEmoji());
                resources.getJSONArray("leaders").put("Duncan Idaho - 2" + faction.getEmoji());
                resources.getJSONArray("leaders").put("Dr Wellington Yueh - 1" + faction.getEmoji());
                traitorDeck.put("Lady Jessica - 5" + faction.getEmoji());
                traitorDeck.put("Thufir Hawat - 5" + faction.getEmoji());
                traitorDeck.put("Gurney Halleck - 4" + faction.getEmoji());
                traitorDeck.put("Duncan Idaho - 2" + faction.getEmoji());
                traitorDeck.put("Dr Wellington Yueh - 1" + faction.getEmoji());
                resources.put("forces lost", 0);
                gameState.getJSONObject("game_state").getJSONObject("game_board").getJSONObject("Arrakeen").getJSONObject("forces").put("Atreides", 10);
            }
            case "Harkonnen" -> {
                faction.put("free_revival", 2);
                faction.setEmoji("<:harkonnen:991763320333926551>");
                resources.put("spice", 10);
                resources.put("reserves", 10);
                resources.getJSONArray("leaders").put("Feyd Rautha - 6" + faction.getEmoji());
                resources.getJSONArray("leaders").put("Beast Rabban - 4" + faction.getEmoji());
                resources.getJSONArray("leaders").put("Piter de Vries - 3" + faction.getEmoji());
                resources.getJSONArray("leaders").put("Captian Iakin Nefud - 2" + faction.getEmoji());
                resources.getJSONArray("leaders").put("Umman Kudu - 1" + faction.getEmoji());
                traitorDeck.put("Feyd Rautha - 6" + faction.getEmoji());
                traitorDeck.put("Beast Rabban - 4" + faction.getEmoji());
                traitorDeck.put("Piter de Vries - 3" + faction.getEmoji());
                traitorDeck.put("Captian Iakin Nefud - 2" + faction.getEmoji());
                traitorDeck.put("Umman Kudu - 1" + faction.getEmoji());
                gameState.getJSONObject("game_state").getJSONObject("game_board").getJSONObject("Carthag").getJSONObject("forces").put("Harkonnen", 10);
            }
            case "Emperor" -> {
                faction.put("free_revival", 1);
                faction.setEmoji("<:emperor:991763323454500914>");
                gameState.getResources().getJSONObject("tanks_forces").put(faction.getName() + "*", 0);
                resources.put("spice", 10);
                resources.put("reserves", 15);
                resources.put("reserves*", 5);
                resources.getJSONArray("leaders").put("Hasimir Fenring - 6" + faction.getEmoji());
                resources.getJSONArray("leaders").put("Captain Aramsham - 5" + faction.getEmoji());
                resources.getJSONArray("leaders").put("Caid - 3" + faction.getEmoji());
                resources.getJSONArray("leaders").put("Burseg - 3" + faction.getEmoji());
                resources.getJSONArray("leaders").put("Bashar - 2" + faction.getEmoji());
                traitorDeck.put("Hasimir Fenring - 6" + faction.getEmoji());
                traitorDeck.put("Captain Aramsham - 5" + faction.getEmoji());
                traitorDeck.put("Caid - 3" + faction.getEmoji());
                traitorDeck.put("Burseg - 3" + faction.getEmoji());
                traitorDeck.put("Bashar - 2" + faction.getEmoji());
            }
            case "Fremen" -> {
                faction.put("free_revival", 3);
                faction.setEmoji("<:fremen:991763322225577984>");
                gameState.getResources().getJSONObject("tanks_forces").put(faction.getName() + "*", 0);
                resources.put("spice", 3);
                resources.put("reserves", 17);
                resources.put("reserves*", 3);
                resources.getJSONArray("leaders").put("Stilgar - 7" + faction.getEmoji());
                resources.getJSONArray("leaders").put("Chani - 6" + faction.getEmoji());
                resources.getJSONArray("leaders").put("Otheym - 5" + faction.getEmoji());
                resources.getJSONArray("leaders").put("Shadout Mapes - 3" + faction.getEmoji());
                resources.getJSONArray("leaders").put("Jamis - 2" + faction.getEmoji());
                traitorDeck.put("Stilgar - 7" + faction.getEmoji());
                traitorDeck.put("Chani - 6" + faction.getEmoji());
                traitorDeck.put("Otheym - 5" + faction.getEmoji());
                traitorDeck.put("Shadout Mapes - 3" + faction.getEmoji());
                traitorDeck.put("Jamis - 2" + faction.getEmoji());
            }
            case "BG" -> {
                faction.put("free_revival", 1);
                faction.setEmoji("<:bg:991763326830911519>");
                resources.put("spice", 5);
                resources.put("reserves", 20);
                resources.getJSONArray("leaders").put("Alia - 5" + faction.getEmoji());
                resources.getJSONArray("leaders").put("Margot Lady Fenring - 5" + faction.getEmoji());
                resources.getJSONArray("leaders").put("Princess Irulan - 5" + faction.getEmoji());
                resources.getJSONArray("leaders").put("Mother Ramallo - 5" + faction.getEmoji());
                resources.getJSONArray("leaders").put("Wanna Yueh - 5" + faction.getEmoji());
                traitorDeck.put("Alia - 5" + faction.getEmoji());
                traitorDeck.put("Margot Lady Fenring - 5" + faction.getEmoji());
                traitorDeck.put("Princess Irulan - 5" + faction.getEmoji());
                traitorDeck.put("Mother Ramallo - 5" + faction.getEmoji());
                traitorDeck.put("Wanna Yueh - 5" + faction.getEmoji());
            }
            case "Guild" -> {
                faction.put("free_revival", 1);
                faction.setEmoji("<:guild:991763321290244096>");
                resources.put("spice", 5);
                resources.put("reserves", 15);
                resources.getJSONArray("leaders").put("Staban Tuek - 5" + faction.getEmoji());
                resources.getJSONArray("leaders").put("Master Bewt - 3" + faction.getEmoji());
                resources.getJSONArray("leaders").put("Esmar Tuek - 3" + faction.getEmoji());
                resources.getJSONArray("leaders").put("Soo Soo Sook - 2" + faction.getEmoji());
                resources.getJSONArray("leaders").put("Guild Rep - 1" + faction.getEmoji());
                gameState.getJSONObject("game_state").getJSONObject("game_board").getJSONObject("Tuek's Sietch").getJSONObject("forces").put("Guild", 5);
                traitorDeck.put("Staban Tuek - 5" + faction.getEmoji());
                traitorDeck.put("Master Bewt - 3" + faction.getEmoji());
                traitorDeck.put("Esmar Tuek - 3" + faction.getEmoji());
                traitorDeck.put("Soo Soo Sook - 2" + faction.getEmoji());
                traitorDeck.put("Guild Rep - 1" + faction.getEmoji());
            }
            case "Ix" -> {
                faction.put("free_revival", 1);
                faction.setEmoji("<:ix:991763319406997514>");
                gameState.getResources().getJSONObject("tanks_forces").put(faction.getName() + "*", 0);
                resources.put("spice", 10);
                resources.put("reserves", 10);
                resources.put("reserves*", 4);
                resources.getJSONArray("leaders").put("Ctair Pilru - 5" + faction.getEmoji());
                resources.getJSONArray("leaders").put("Tessia Vernius - 5" + faction.getEmoji());
                resources.getJSONArray("leaders").put("Dominic Vernius - 4" + faction.getEmoji());
                resources.getJSONArray("leaders").put("Kailea Vernius - 2" + faction.getEmoji());
                resources.getJSONArray("leaders").put("Cammar Pilru - 1" + faction.getEmoji());
                gameState.getJSONObject("game_state").getJSONObject("game_board").put("Hidden Mobile Stronghold", new Territory("Hidden Mobile Stronghold", -1, false, true));
                gameState.getJSONObject("game_state").getJSONObject("game_board").getJSONObject("Hidden Mobile Stronghold").getJSONObject("forces").put("Ix suboid", 3);
                gameState.getJSONObject("game_state").getJSONObject("game_board").getJSONObject("Hidden Mobile Stronghold").getJSONObject("forces").put("Ix cyborg", 3);
                traitorDeck.put("Ctair Pilru - 5" + faction.getEmoji());
                traitorDeck.put("Tessia Vernius - 5" + faction.getEmoji());
                traitorDeck.put("Dominic Vernius - 4" + faction.getEmoji());
                traitorDeck.put("Kailea Vernius - 2" + faction.getEmoji());
                traitorDeck.put("Cammar Pilru - 1" + faction.getEmoji());
            }
            case "BT" -> {
                faction.put("free_revival", 2);
                faction.setEmoji("<:bt:991763325576810546>");
                resources.put("spice", 5);
                resources.put("reserves", 20);
                resources.getJSONArray("leaders").put("Zoal - X" + faction.getEmoji());
                resources.getJSONArray("leaders").put("Hidar Fen Ajidica - 4" + faction.getEmoji());
                resources.getJSONArray("leaders").put("Master Zaaf - 3" + faction.getEmoji());
                resources.getJSONArray("leaders").put("Wykk - 2" + faction.getEmoji());
                resources.getJSONArray("leaders").put("Blin - 1" + faction.getEmoji());
                traitorDeck.put("Zoal - X" + faction.getEmoji());
                traitorDeck.put("Hidar Fen Ajidica - 4" + faction.getEmoji());
                traitorDeck.put("Master Zaaf - 3" + faction.getEmoji());
                traitorDeck.put("Wykk - 2" + faction.getEmoji());
                traitorDeck.put("Blin - 1" + faction.getEmoji());
            }
            case "CHOAM" -> {
                faction.put("free_revival", 0);
                faction.setEmoji("<:choam:991763324624703538>");
                resources.put("spice", 2);
                resources.put("reserves", 20);
                resources.getJSONArray("leaders").put("Frankos Aru - 4" + faction.getEmoji());
                resources.getJSONArray("leaders").put("Lady Jalma - 4" + faction.getEmoji());
                resources.getJSONArray("leaders").put("Rajiv Londine - 3" + faction.getEmoji());
                resources.getJSONArray("leaders").put("Duke Verdun - 3" + faction.getEmoji());
                resources.getJSONArray("leaders").put("Viscount Tull - 2" + faction.getEmoji());
                resources.getJSONArray("leaders").put("Auditor - 2" + faction.getEmoji());
                resources.put("inflation token", "double");
                traitorDeck.put("Frankos Aru - 4" + faction.getEmoji());
                traitorDeck.put("Lady Jalma - 4" + faction.getEmoji());
                traitorDeck.put("Rajiv Londine - 3" + faction.getEmoji());
                traitorDeck.put("Duke Verdun - 3" + faction.getEmoji());
                traitorDeck.put("Viscount Tull - 2" + faction.getEmoji());
                traitorDeck.put("Auditor - 2" + faction.getEmoji());
            }
            case "Rich" -> {
                faction.put("free_revival", 2);
                faction.setEmoji("<:rich:991763318467465337>");
                resources.put("spice", 5);
                resources.put("reserves", 20);
                resources.getJSONArray("leaders").put("Ein Calimar - 5" + faction.getEmoji());
                resources.getJSONArray("leaders").put("Lady Helena - 4" + faction.getEmoji());
                resources.getJSONArray("leaders").put("Flinto Kinnis - 3" + faction.getEmoji());
                resources.getJSONArray("leaders").put("Haloa Rund - 2" + faction.getEmoji());
                resources.getJSONArray("leaders").put("Talis Balt - 2" + faction.getEmoji());
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
                traitorDeck.put("Ein Calimar - 5" + faction.getEmoji());
                traitorDeck.put("Lady Helena - 4" + faction.getEmoji());
                traitorDeck.put("Flinto Kinnis - 3" + faction.getEmoji());
                traitorDeck.put("Haloa Rund - 2" + faction.getEmoji());
                traitorDeck.put("Talis Balt - 2" + faction.getEmoji());
            }
        }
    }



    public static JSONObject buildBoard() {

        JSONObject territories = new JSONObject();

        territories.put("Cielago North(1)", new Territory("Cielago North", 1, false, false));
        territories.put("Cielago Depression(1)", new Territory("Cielago Depression", 1, false, false));
        territories.put("Meridian(1)", new Territory("Meridian", 1, false, false));
        territories.put("Cielago South(1)", new Territory("Cielago South", 1, false, false));
        territories.put("Cielago North(2)", new Territory("Cielago North", 2, false, false));
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
        territories.put("Red Chasm", new Territory("Red Chasm", 6, false, false));
        territories.put("False Wall East(7)", new Territory("False Wall East", 7, true, false));
        territories.put("The Minor Erg(7)", new Territory("The Minor Erg", 7, false, false));
        territories.put("Shield Wall(7)", new Territory("Shield Wall", 7, true, false));
        territories.put("Pasty Mesa(7)", new Territory("Pasty Mesa", 7, true, false));
        territories.put("Gara Kulon", new Territory("Gara Kulon", 7, false, false));
        territories.put("False Wall East(8)", new Territory("False Wall East", 8, true, false));
        territories.put("Imperial Basin(8)", new Territory("Imperial Basin", 8, true, false));
        territories.put("Shield Wall(8)", new Territory("Shield Wall", 8, true, false));
        territories.put("Hole in the Rock", new Territory("Hole in the Rock", 8, false, false));
        territories.put("Rim Wall West", new Territory("Rim Wall West", 8, true, false));
        territories.put("Basin", new Territory("Basin", 8, false, false));
        territories.put("Old Gap(8)", new Territory("Old Gap", 8, false, false));
        territories.put("Sihaya Ridge", new Territory("Sihaya Ridge", 8, false, false));
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
        territories.put("The Great Flat", new Territory("The Great Flat", 14, false, false));
        territories.put("Funeral Plain", new Territory("Funeral Plain", 14, false, false));
        territories.put("Bight of the Cliff(14)", new Territory("Bight of the Cliff", 14, false, false));
        territories.put("Wind Pass(15)", new Territory("Wind Pass", 15, false, false));
        territories.put("The Greater Flat", new Territory("The Greater Flat", 15, false, false));
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
        spiceDeck.put("Sihaya Ridge - 6");
        spiceDeck.put("Shai-Hulud");
        spiceDeck.put("Shai-Hulud");
        spiceDeck.put("Shai-Hulud");
        spiceDeck.put("Shai-Hulud");
        spiceDeck.put("Shai-Hulud");
        spiceDeck.put("Shai-Hulud");
        spiceDeck.put("Hagga Basin(12) - 6");
        spiceDeck.put("Red Chasm - 8");
        spiceDeck.put("The Minor Erg(7) - 8");
        spiceDeck.put("Cielago North(2) - 8");
        spiceDeck.put("Funeral Plain - 6");
        spiceDeck.put("The Great Flat - 10");
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

    public static JSONArray buildStormDeck() {
        JSONArray stormDeck = new JSONArray();
        stormDeck.put(1);
        stormDeck.put(2);
        stormDeck.put(3);
        stormDeck.put(4);
        stormDeck.put(5);
        stormDeck.put(6);
        return stormDeck;
    }

    public record Coordinates(int x, int y) {}

    public static Coordinates getDrawCoordinates(String location) {
        switch (location) {
            case "sigil 1" -> {
                return new Coordinates(475, 978);
            }
            case "sigil 2" -> {
                return new Coordinates(865, 753);
            }
            case "sigil 3" -> {
                return new Coordinates(865, 301);
            }
            case "sigil 4" -> {
                return new Coordinates(475, 75);
            }
            case "sigil 5" -> {
                return new Coordinates(85, 301);
            }
            case "sigil 6" -> {
                return new Coordinates(85, 753);
            }
            case "turn 0", "turn 1" -> {
                return new Coordinates(124, 60);
            }
            case "turn 2" -> {
                return new Coordinates(148, 75);
            }
            case "turn 3" -> {
                return new Coordinates(160, 105);
            }
            case "turn 4" -> {
                return new Coordinates(148,135);
            }
            case "turn 5" -> {
                return new Coordinates(124,155);
            }
            case "turn 6" -> {
                return new Coordinates(95,150);
            }
            case "turn 7" -> {
                return new Coordinates(67,135);
            }
            case "turn 8" -> {
                return new Coordinates(60,108);
            }
            case "turn 9" -> {
                return new Coordinates(65,80);
            }
            case "turn 10" -> {
                return new Coordinates(93,60);
            }
        }

        return new Coordinates(0, 0);
    }
}
