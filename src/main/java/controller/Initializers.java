package controller;

import model.Territory;
import model.TreacheryCard;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class Initializers {



    public static JSONObject buildBoard() {

        JSONObject territories = new JSONObject();

        territories.put("Cielago North", new Territory("Cielago North", 1, false, false));
        territories.put("Cielago Depression", new Territory("Cielago Depression", 1, false, false));
        territories.put("Meridian", new Territory("Meridian", 1, false, false));
        territories.put("Cielago South", new Territory("Cielago South", 1, false, false));
        territories.put("Cielago North", new Territory("Cielago North", 2, false, false));
        territories.put("Cielago Depression", new Territory("Cielago Depression", 2, false, false));
        territories.put("Cielago South", new Territory("Cielago South", 2, false, false));
        territories.put("Cielago East", new Territory("Cielago East", 2, false, false));
        territories.put("Harg Pass", new Territory("Harg Pass", 3, false, false));
        territories.put("False Wall South", new Territory("False Wall South", 3, true, false));
        territories.put("Cielago East", new Territory("Cielago East", 3, false, false));
        territories.put("South Mesa", new Territory("South Mesa", 3, false, false));
        territories.put("Harg Pass", new Territory("Harg Pass", 4, false, false));
        territories.put("False Wall East", new Territory("False Wall East", 4, true, false));
        territories.put("The Minor Erg", new Territory("The Minor Erg", 4, false, false));
        territories.put("False Wall South", new Territory("False Wall South", 4, true, false));
        territories.put("Pasty Mesa", new Territory("Pasty Mesa", 4, true, false));
        territories.put("South Mesa", new Territory("South Mesa", 4, false, false));
        territories.put("Tuek's Sietch", new Territory("Tuek's Sietch", 4, true, true));
        territories.put("False Wall East", new Territory("False Wall East", 5, true, false));
        territories.put("The Minor Erg", new Territory("The Minor Erg", 5, false, false));
        territories.put("Pasty Mesa", new Territory("Pasty Mesa", 5, true, false));
        territories.put("South Mesa", new Territory("South Mesa", 5, false, false));
        territories.put("False Wall East", new Territory("False Wall East", 6, true, false));
        territories.put("The Minor Erg", new Territory("The Minor Erg", 6, false, false));
        territories.put("Pasty Mesa", new Territory("Pasty Mesa", 6, true, false));
        territories.put("Red Chasm", new Territory("Red Chasm", 6, false, false));
        territories.put("False Wall East", new Territory("False Wall East", 7, true, false));
        territories.put("The Minor Erg", new Territory("The Minor Erg", 7, false, false));
        territories.put("Shield Wall", new Territory("Shield Wall", 7, true, false));
        territories.put("Pasty Mesa", new Territory("Pasty Mesa", 7, true, false));
        territories.put("Gara Kulon", new Territory("Gara Kulon", 7, false, false));
        territories.put("False Wall East", new Territory("False Wall East", 8, true, false));
        territories.put("Imperial Basin", new Territory("Imperial Basin", 8, true, false));
        territories.put("Shield Wall", new Territory("Shield Wall", 8, true, false));
        territories.put("Hole in the Rock", new Territory("Hole in the Rock", 8, false, false));
        territories.put("Rim Wall West", new Territory("Rim Wall West", 8, true, false));
        territories.put("Basin", new Territory("Basin", 8, false, false));
        territories.put("Old Gap", new Territory("Old Gap", 8, false, false));
        territories.put("Sihaya Ridge", new Territory("Sihaya Ridge", 8, false, false));
        territories.put("Imperial Basin", new Territory("Imperial Basin", 9, true, false));
        territories.put("Arrakeen", new Territory("Arrakeen", 9, true, true));
        territories.put("Old Gap", new Territory("Old Gap", 9, false, false));
        territories.put("Imperial Basin", new Territory("Imperial Basin", 10, true, false));
        territories.put("Arsunt", new Territory("Arsunt", 10, false, false));
        territories.put("Carthag", new Territory("Carthag", 10, true, true));
        territories.put("Tsimpo", new Territory("Tsimpo", 10, false, false));
        territories.put("Broken Land", new Territory("Broken Land", 10, false, false));
        territories.put("Old Gap", new Territory("Old Gap", 10, false, false));
        territories.put("Arsunt", new Territory("Arsunt", 11, false, false));
        territories.put("Hagga Basin",new Territory("Hagga Basin", 11, false, false));
        territories.put("Tsimpo", new Territory("Tsimpo", 11, false, false));
        territories.put("Plastic Basin", new Territory("Plastic Basin", 11, true, false));
        territories.put("Broken Land", new Territory("Broken Land", 11, false, false));
        territories.put("Hagga Basin", new Territory("Hagga Basin", 12, false, false));
        territories.put("Plastic Basin", new Territory("Plastic Basin", 12, true, false));
        territories.put("Tsimpo", new Territory("Tsimpo", 12, false, false));
        territories.put("Rock Outcroppings", new Territory("Rock Outcroppings", 12, false, false));
        territories.put("Wind Pass", new Territory("Wind Pass", 13, false, false));
        territories.put("Plastic Basin", new Territory("Plastic Basin", 13, true, false));
        territories.put("Bight of the Cliff", new Territory("Bight of the Cliff", 13, false, false));
        territories.put("Sietch Tabr", new Territory("Sietch Tabr", 13, true, true));
        territories.put("Rock Outcroppings", new Territory("Rock Outcroppings", 13, false, false));
        territories.put("Wind Pass", new Territory("Wind Pass", 14, false, false));
        territories.put("The Great Flat", new Territory("The Great Flat", 14, false, false));
        territories.put("Funeral Plain", new Territory("Funeral Plain", 14, false, false));
        territories.put("Bight of the Cliff", new Territory("Bight of the Cliff", 14, false, false));
        territories.put("Wind Pass", new Territory("Wind Pass", 15, false, false));
        territories.put("The Greater Flat", new Territory("The Greater Flat", 15, false, false));
        territories.put("Habbanya Erg", new Territory("Habbanya Erg", 15, false, false));
        territories.put("False Wall West", new Territory("False Wall West", 15, true, false));
        territories.put("Wind Pass North", new Territory("Wind Pass North", 16, false, false));
        territories.put("Wind Pass", new Territory("Wind Pass", 16, false, false));
        territories.put("False Wall West", new Territory("False Wall West", 16, true, false));
        territories.put("Habbanya Erg", new Territory("Habbanya Erg", 16, false, false));
        territories.put("Habbanya Ridge Flat", new Territory("Habbanya Ridge Flat", 16, false, false));
        territories.put("Habbanya Sietch", new Territory("Habbanya Sietch", 16, true, true));
        territories.put("Wind Pass North", new Territory("Wind Pass North", 17, false, false));
        territories.put("Cielago West", new Territory("Cielago West", 17, false, false));
        territories.put("False Wall West", new Territory("False Wall West", 17, true, false));
        territories.put("Habbanya Ridge Flat", new Territory("Habbanya Ridge Flat", 17, false, false));
        territories.put("Cielago North", new Territory("Cielago North", 18, false, false));
        territories.put("Cielago Depression", new Territory("Cielago Depression", 18, false, false));
        territories.put("Cielago West", new Territory("Cielago West", 18, false, false));
        territories.put("Meridian", new Territory("Meridian", 18, false, false));
        territories.put("Polar Sink", new Territory("Polar Sink", -1, false, false));
        return territories;
    }

    public static JSONObject buildSpiceDeck() {
        JSONObject spiceDeck = new JSONObject();

        spiceDeck.put("Habbanya Ridge Flat", new JSONArray(new int[]{17, 10}));
        spiceDeck.put("Cielago South", new JSONArray(new int[]{1, 12}));
        spiceDeck.put("Broken Land", new JSONArray(new int[]{11, 8}));
        spiceDeck.put("South Mesa", new JSONArray(new int[]{4, 10}));
        spiceDeck.put("Sihaya Ridge", new JSONArray(new int[]{8, 6}));
        spiceDeck.put("Shai-Hulud(1)",  new JSONArray(new int[]{-1, 0}));
        spiceDeck.put("Shai-Hulud(2)",  new JSONArray(new int[]{-1, 0}));
        spiceDeck.put("Shai-Hulud(3)",  new JSONArray(new int[]{-1, 0}));
        spiceDeck.put("Shai-Hulud(4)",  new JSONArray(new int[]{-1, 0}));
        spiceDeck.put("Shai-Hulud(5)",  new JSONArray(new int[]{-1, 0}));
        spiceDeck.put("Shai-Hulud(6)",  new JSONArray(new int[]{-1, 0}));
        spiceDeck.put("Hagga Basin",  new JSONArray(new int[]{12, 6}));
        spiceDeck.put("Red Chasm",  new JSONArray(new int[]{6, 8}));
        spiceDeck.put("The Minor Erg",  new JSONArray(new int[]{7, 8}));
        spiceDeck.put("Cielago North",  new JSONArray(new int[]{2, 8}));
        spiceDeck.put("Funeral Plain",  new JSONArray(new int[]{14, 6}));
        spiceDeck.put("The Great Flat",  new JSONArray(new int[]{14, 10}));
        spiceDeck.put("Habbanya Erg",  new JSONArray(new int[]{15, 8}));
        spiceDeck.put("Old Gap",  new JSONArray(new int[]{9, 6}));
        spiceDeck.put("Rock Outcroppings",  new JSONArray(new int[]{13, 6}));
        spiceDeck.put("Wind Pass North",  new JSONArray(new int[]{16, 6}));

        return spiceDeck;
    }

    public static JSONObject buildTreacheryDeck() {
        JSONObject treacheryDeck = new JSONObject();

        treacheryDeck.put("Lasgun", "Weapon - Special");
        treacheryDeck.put("Chaumas", "Weapon - Poison");
        treacheryDeck.put("Chaumurky", "Weapon - Poison");
        treacheryDeck.put("Ellaca Drug", "Weapon - Poison");
        treacheryDeck.put("Gom Jabbar", "Weapon - Poison");
        treacheryDeck.put("Stunner", "Weapon - Projectile");
        treacheryDeck.put("Slip Tip", "Weapon - Projectile");
        treacheryDeck.put("Maula Pistol", "Weapon - Projectile");
        treacheryDeck.put("Crysknife", "Weapon - Projectile");
        treacheryDeck.put("Shield(1)", "Defense - Projectile");
        treacheryDeck.put("Shield(2)", "Defense - Projectile");
        treacheryDeck.put("Shield(3)", "Defense - Projectile");
        treacheryDeck.put("Shield(4)", "Defense - Projectile");
        treacheryDeck.put("Snooper(1)", "Defense - Poison");
        treacheryDeck.put("Snooper(2)", "Defense - Poison");
        treacheryDeck.put("Snooper(3)", "Defense - Poison");
        treacheryDeck.put("Snooper(4)", "Defense - Poison");
        treacheryDeck.put("La, La, La", "Worthless Card");
        treacheryDeck.put("Baliset", "Worthless Card");
        treacheryDeck.put("Kulon", "Worthless Card");
        treacheryDeck.put("Trip to Gamont", "Worthless Card");
        treacheryDeck.put("Jubba Cloak", "Worthless Card");
        treacheryDeck.put("Tleilaxu Ghola", "Special");
        treacheryDeck.put("Truthtrance(1)", "Special");
        treacheryDeck.put("Truthtrance(2)", "Special");
        treacheryDeck.put("Weather Control", "Special");
        treacheryDeck.put("Karama(1)", "Special");
        treacheryDeck.put("Karama(2)", "Special");
        treacheryDeck.put("Family Atomics", "Special");
        treacheryDeck.put("Hajr", "Special");
        treacheryDeck.put("Cheap Hero(1)", "Special");
        treacheryDeck.put("Cheap Hero(2)", "Special");
        treacheryDeck.put("Cheap Hero(3)", "Special");

        return treacheryDeck;
    }
}
