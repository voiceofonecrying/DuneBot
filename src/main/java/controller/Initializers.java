package controller;

import model.Faction;
import model.Game;
import model.Territory;
import net.dv8tion.jda.internal.utils.tuple.Pair;
import org.json.JSONArray;
import org.json.JSONObject;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

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

    public static List<Point> getPoints(String territory) {
        List<Point> points = new ArrayList<>();

        switch (territory) {

            case "Carthag" -> {
                points.add(new Point(464,287));
                points.add(new Point(464,245));
                points.add(new Point(442,264));
                points.add(new Point(485,264));
            }
            case "Tuek's Sietch" -> {
                points.add(new Point(769, 676));
                points.add(new Point(761, 730));
                points.add(new Point(795, 698));
                points.add(new Point(748, 695));
            }
            case "Arrakeen" -> {
                points.add(new Point(601, 255));
                points.add(new Point(605, 214));
                points.add(new Point(577, 229));
                points.add(new Point(626, 229));
            }
            case "Sietch Tabr" -> {
                points.add(new Point(177,339));
                points.add(new Point(150,363));
                points.add(new Point(198,372));
            }
            case "Habbanya Sietch" -> {
                points.add(new Point(188, 702));
                points.add(new Point(188, 738));
                points.add(new Point(165, 710));
            }
            case "Cielago North(1)" -> {
                points.add(new Point(474, 721));
            }
            case "Cielago Depression(1)" -> {
                points.add(new Point(443, 805));
                points.add(new Point(492,803));

            }
            case "Meridian(1)" -> {
                points.add(new Point(420,913));
            }
            case "Cielago South(1)" -> {
                points.add(new Point(468,898));
                points.add(new Point(497,859));
                points.add(new Point(508,898));
            }
            case "Cielago North(2)" -> {
                points.add(new Point(521,719));
                points.add(new Point(509,639));
                points.add(new Point(543,700));
            }
            case "Cielago Depression(2)" -> {
                points.add(new Point(544,797));
            }
            case "Cielago South(2)" -> {
                points.add(new Point(551,858));

            }
            case "Cielago East(2)" -> {
                points.add(new Point(601,892));
                points.add(new Point(607,806));
            }
            case "Harg Pass(3)" -> {
                points.add(new Point(535,616));
                points.add(new Point(517,585));
            }
            case "False Wall South(3)" -> {
                points.add(new Point(624,710));
                points.add(new Point(662,731));
                points.add(new Point(697,765));
                points.add(new Point(583,669));
            }
            case "Cielago East(3)" -> {
                points.add(new Point(581,859));
                points.add(new Point(645,803));
            }
            case "South Mesa(3)" -> {
                points.add(new Point(552,800));
                points.add(new Point(731,832));

            }
            case "Harg Pass(4)" -> {
                points.add(new Point(570, 600));
                points.add(new Point(561,583));
            }
            case "False Wall East(4)" -> {
                points.add(new Point(530,562));
                points.add(new Point(545,565));
            }
            case "The Minor Erg(4)" -> {
                points.add(new Point(621,607));
                points.add(new Point(626,626));
            }
            case "False Wall South(4)" -> {
                points.add(new Point(650,655));
                points.add(new Point(681,669));
                points.add(new Point(710,710));
            }
            case "Pasty Mesa(4)" -> {
                points.add(new Point(663,611));
                points.add(new Point(686,619));
                points.add(new Point(722,634));
            }
            case "South Mesa(4)" -> {
                points.add(new Point(785,766));
                points.add(new Point(826,683));
                points.add(new Point(805,748));
                points.add(new Point(827,718));
            }
            case "False Wall East(5)" -> {
                points.add(new Point(538,543));
                points.add(new Point(561,539));
            }
            case "The Minor Erg(5)" -> {
                points.add(new Point(610,550));
                points.add(new Point(632,643));
                points.add(new Point(580,560));
            }
            case "Pasty Mesa(5)" -> {
                points.add(new Point(530,570));
                points.add(new Point(675,570));
                points.add(new Point(800,575));
                points.add(new Point(750,610));
            }
            case "South Mesa(5)" -> {
                points.add(new Point(845,645));
                points.add(new Point(860,560));
            }
            case "False Wall East(6)" -> {
                points.add(new Point(548,521));
                points.add(new Point(565,505));
            }
            case "The Minor Erg(6)" -> {
                points.add(new Point(615,500));
                points.add(new Point(590,510));
                points.add(new Point(645,495));
            }
            case "Pasty Mesa(6)" -> {
                points.add(new Point(730,480));
                points.add(new Point(760,470));
                points.add(new Point(795,445));
                points.add(new Point(680,510));
            }
            case "Red Chasm" -> {
                points.add(new Point(840,495));
                points.add(new Point(845,465));
                points.add(new Point(865,515));
            }
            case "False Wall East(7)" -> {
                points.add(new Point(535,485));
                points.add(new Point(560,485));
            }
            case "The Minor Erg(7)" -> {
                points.add(new Point(578,475));
                points.add(new Point(595,472));
                points.add(new Point(624,458));
                points.add(new Point(626,443));
            }
            case "Shield Wall(7)" -> {
                points.add(new Point(636,400));
                points.add(new Point(620,415));
                points.add(new Point(672,370));
                points.add(new Point(590,440));
            }
            case "Pasty Mesa(7)" -> {
                points.add(new Point(720,400));
                points.add(new Point(780,390));
                points.add(new Point(675,435));
                points.add(new Point(825,385));
            }
            case "Gara Kulon" -> {
                points.add(new Point(780,300));
                points.add(new Point(755,342));
            }
            case "False Wall East(8)" -> {
                points.add(new Point(521,476));
                points.add(new Point(534,463));
            }
            case "Imperial Basin(8)" -> {
                points.add(new Point(548,413));
                points.add(new Point(716,366));
            }
            case "Shield Wall(8)" -> {
                points.add(new Point(580,410));
                points.add(new Point(607,392));
                points.add(new Point(665,355));
            }
            case "Hole in the Rock" -> {
                points.add(new Point(660,320));
            }
            case "Rim Wall West" -> {
                points.add(new Point(630,290));
            }
            case "Basin" -> {
                points.add(new Point(690,245));
            }
            case "Old Gap(8)" -> {
                points.add(new Point(666,213));
                points.add(new Point(685,190));
            }
            case "Sihaya Ridge" -> {
                points.add(new Point(740,245));
                points.add(new Point(727,273));
                points.add(new Point(764,266));
            }
            case "Imperial Basin(9)" -> {
                points.add(new Point(527,367));
                points.add(new Point(513,403));
                points.add(new Point(553,296));
            }
            case "Old Gap(9)" -> {
                points.add(new Point(600,150));
                points.add(new Point(560,150));
                points.add(new Point(638,182));
            }
            case "Imperial Basin(10)" -> {
                points.add(new Point(495,370));
            }
            case "Arsunt(10)" -> {
                points.add(new Point(460,380));
                points.add(new Point(455,340));
            }
            case "Tsimpo(10)" -> {
                points.add(new Point(440,190));
                points.add(new Point(480,185));
            }
            case "Broken Land(10)" -> {
                points.add(new Point(455,135));
                points.add(new Point(480,135));
            }
            case "Old Gap(10)" -> {
                points.add(new Point(525,140));
            }
            case "Arsunt(11)" -> {
                points.add(new Point(447,457));
            }
            case "Hagga Basin(11)" -> {
                points.add(new Point(380,340));
                points.add(new Point(410,325));
            }
            case "Tsimpo(11)" -> {
                points.add(new Point(390,245));
            }
            case "Plastic Basin(11)" -> {
                points.add(new Point(315,228));
                points.add(new Point(343,213));
            }
            case "Broken Land(11)" -> {
                points.add(new Point(295,177));
                points.add(new Point(370,158));
                points.add(new Point(330,176));
            }
            case "Hagga Basin(12)" -> {
                points.add(new Point(387,436));
                points.add(new Point(330,379));
                points.add(new Point(339,345));
            }
            case "Plastic Basin(12)" -> {
                points.add(new Point(245,290));
                points.add(new Point(270,255));
            }
            case "Tsimpo(12)" -> {
                points.add(new Point(320,305));
            }
            case "Rock Outcroppings(12)" -> {
                points.add(new Point(195,274));
                points.add(new Point(232,222));
            }
            case "Wind Pass(13)" -> {
                points.add(new Point(390,490));
            }
            case "Plastic Basin(13)" -> {
                points.add(new Point(250,410));
                points.add(new Point(300,440));
            }
            case "Bight of the Cliff(13)" -> {
                points.add(new Point(105,375));
                points.add(new Point(170,405));
            }
            case "Rock Outcroppings(13)" -> {
                points.add(new Point(145,315));
                points.add(new Point(155,295));
                points.add(new Point(160,275));
            }
            case "Wind Pass(14)" -> {
                points.add(new Point(375,520));
            }
            case "The Great Flat" -> {
                points.add(new Point(140,515));
                points.add(new Point(290,515));
                points.add(new Point(215,515));
            }
            case "Funeral Plain" -> {
                points.add(new Point(160,470));
                points.add(new Point(90,455));
                points.add(new Point(225,460));
            }
            case "Bight of the Cliff(14)" -> {
                points.add(new Point(105,420));
            }
            case "Wind Pass(15)" -> {
                points.add(new Point(360,555));
            }
            case "The Greater Flat" -> {
                points.add(new Point(115,575));
                points.add(new Point(250,560));
            }
            case "Habbanya Erg(15)" -> {
                points.add(new Point(140,630));
                points.add(new Point(215,600));
                points.add(new Point(90,625));
            }
            case "False Wall West(15)" -> {
                points.add(new Point(300,580));
                points.add(new Point(320,575));
            }
            case "Wind Pass North(16)" -> {
                points.add(new Point(380,590));
                points.add(new Point(373,601));
                points.add(new Point(406,573));
            }
            case "Wind Pass(16)" -> {
                points.add(new Point(350,615));
            }
            case "False Wall West(16)" -> {
                points.add(new Point(305,615));
                points.add(new Point(287,631));
            }
            case "Habbanya Erg(16)" -> {
                points.add(new Point(230,640));
            }
            case "Habbanya Ridge Flat(16)" -> {
                points.add(new Point(125,710));
            }
            case "Wind Pass North(17)" -> {
                points.add(new Point(377,655));
            }
            case "Cielago West(17)" -> {
                points.add(new Point(335,710));
            }
            case "False Wall West(17)" -> {
                points.add(new Point(285,705));
                points.add(new Point(274,733));
            }
            case "Habbanya Ridge Flat(17)" -> {
                points.add(new Point(245,850));
                points.add(new Point(265,795));
                points.add(new Point(230,765));
            }
            case "Cielago North(18)" -> {
                points.add(new Point(410,720));
            }
            case "Cielago Depression(18)" -> {
                points.add(new Point(385,785));
            }
            case "Cielago West(18)" -> {
                points.add(new Point(330,805));
            }
            case "Meridian(18)" -> {
                points.add(new Point(360,890));
            }
            case "Polar Sink" -> {
                points.add(new Point(460,545));
                points.add(new Point(465,515));
                points.add(new Point(415,550));
                points.add(new Point(500,520));
            }
            case "Forces Tanks" -> {
                points.add(new Point(45,990));
                points.add(new Point(100,990));
            }
            case "Leaders Tanks" -> {
                points.add(new Point(900,985));
                points.add(new Point(860,985));
                points.add(new Point(820,985));
                points.add(new Point(780,985));
                points.add(new Point(740,985));
                points.add(new Point(700,985));
                points.add(new Point(660,1010));
            }
        }
        return points;
    }

    public static Point getDrawCoordinates(String location) {
        switch (location) {
            case "sigil 1" -> {
                return new Point(475, 978);
            }
            case "sigil 2" -> {
                return new Point(865, 753);
            }
            case "sigil 3" -> {
                return new Point(865, 301);
            }
            case "sigil 4" -> {
                return new Point(475, 80);
            }
            case "sigil 5" -> {
                return new Point(85, 301);
            }
            case "sigil 6" -> {
                return new Point(85, 753);
            }
            case "turn 0", "turn 1" -> {
                return new Point(124, 60);
            }
            case "turn 2" -> {
                return new Point(148, 75);
            }
            case "turn 3" -> {
                return new Point(160, 105);
            }
            case "turn 4" -> {
                return new Point(148,135);
            }
            case "turn 5" -> {
                return new Point(124,155);
            }
            case "turn 6" -> {
                return new Point(95,150);
            }
            case "turn 7" -> {
                return new Point(67,135);
            }
            case "turn 8" -> {
                return new Point(60,108);
            }
            case "turn 9" -> {
                return new Point(65,80);
            }
            case "turn 10" -> {
                return new Point(93,60);
            }
            case "phase 1" -> {
                return new Point(237, 45);
            }
            case "phase 2" -> {
                return new Point(297,45);
            }
            case "phase 3" -> {
                return new Point(357,45);
            }
            case "phase 4" -> {
                return new Point(417,45);
            }
            case "phase 5" -> {
                return new Point(533,45);
            }
            case "phase 6" -> {
                return new Point(593,45);
            }
            case "phase 7" -> {
                return new Point(653,45);
            }
            case "phase 8" -> {
                return new Point(713,45);
            }
            case "storm 1" -> {
                return new Point(487,958);
            }
            case "storm 2" -> {
                return new Point(635,927);
            }
            case "storm 3" -> {
                return new Point(760,850);
            }
            case "storm 4" -> {
                return new Point(858,732);
            }
            case "storm 5" -> {
                return new Point(899,589);
            }
            case "storm 6" -> {
                return new Point(898,441);
            }
            case "storm 7" -> {
                return new Point(843,300);
            }
            case "storm 8" -> {
                return new Point(740,190);
            }
            case "storm 9" -> {
                return new Point(610,120);
            }
            case "storm 10" -> {
                return new Point(463,98);
            }
            case "storm 11" -> {
                return new Point(318,129);
            }
            case "storm 12" -> {
                return new Point(189,207);
            }
            case "storm 13" -> {
                return new Point(96,323);
            }
            case "storm 14" -> {
                return new Point(49,464);
            }
            case "storm 15" -> {
                return new Point(53,614);
            }
            case "storm 16" -> {
                return new Point(107,755);
            }
            case "storm 17" -> {
                return new Point(210,865);
            }
            case "storm 18" -> {
                return new Point(340,940);
            }
        }

        return new Point(0, 0);
    }
}
