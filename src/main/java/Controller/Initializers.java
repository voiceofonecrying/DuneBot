package Controller;

import Model.Territory;

import java.util.ArrayList;
import java.util.List;

public class Initializers {



    public static List<Territory> buildBoard(int gameId) {

        List<Territory> territories = new ArrayList<>();

        territories.add(new Territory(gameId, "Cielago North", 1, false, false, false, 0, null, 0, 0));
        territories.add(new Territory(gameId, "Cielago Depression", 1, false, false, false, 0, null, 0, 0));
        territories.add(new Territory(gameId, "Meridian", 1, false, false, false, 0, null, 0, 0));
        territories.add(new Territory(gameId, "Cielago South", 1, false, false,false, 0, null, 0, 0));
        territories.add(new Territory(gameId, "Cielago North", 2, false, false,false, 0, null, 0, 0));
        territories.add(new Territory(gameId, "Cielago Depression", 2, false, false,false, 0, null, 0, 0));
        territories.add(new Territory(gameId, "Cielago South", 2, false, false,false, 0, null, 0, 0));
        territories.add(new Territory(gameId, "Cielago East", 2, false, false,false, 0, null, 0, 0));
        territories.add(new Territory(gameId, "Harg Pass", 3, false, false,false, 0, null, 0, 0));
        territories.add(new Territory(gameId, "False Wall South", 3, true, false,false, 0, null, 0, 0));
        territories.add(new Territory(gameId, "Cielago East", 3, false, false,false, 0, null, 0, 0));
        territories.add(new Territory(gameId, "South Mesa", 3, false, false,false, 0, null, 0, 0));
        territories.add(new Territory(gameId, "Harg Pass", 4, false, false,false, 0, null, 0, 0));
        territories.add(new Territory(gameId, "False Wall East", 4, true, false,false, 0, null, 0, 0));
        territories.add(new Territory(gameId, "The Minor Erg", 4, false, false,false, 0, null, 0, 0));
        territories.add(new Territory(gameId, "False Wall South", 4, true, false,false, 0, null, 0, 0));
        territories.add(new Territory(gameId, "Pasty Mesa", 4, true, false,false, 0, null, 0, 0));
        territories.add(new Territory(gameId, "South Mesa", 4, false, false,false, 0, null, 0, 0));
        territories.add(new Territory(gameId, "Tuek's Sietch", 4, true, true,false, 0, null, 0, 0));
        territories.add(new Territory(gameId, "False Wall East", 5, true, false,false, 0, null, 0, 0));
        territories.add(new Territory(gameId, "The Minor Erg", 5, false, false,false, 0, null, 0, 0));
        territories.add(new Territory(gameId, "Pasty Mesa", 5, true, false,false, 0, null, 0, 0));
        territories.add(new Territory(gameId, "South Mesa", 5, false, false, false,0, null, 0, 0));
        territories.add(new Territory(gameId, "False Wall East", 6, true, false,false, 0, null, 0, 0));
        territories.add(new Territory(gameId, "The Minor Erg", 6, false, false,false, 0, null, 0, 0));
        territories.add(new Territory(gameId, "Pasty Mesa", 6, true, false, false,0, null, 0, 0));
        territories.add(new Territory(gameId, "Red Chasm", 6, false, false, false,0, null, 0, 0));
        territories.add(new Territory(gameId, "False Wall East", 7, true, false, false,0, null, 0, 0));
        territories.add(new Territory(gameId, "The Minor Erg", 7, false, false, false,0, null, 0, 0));
        territories.add(new Territory(gameId, "Shield Wall", 7, true, false, false,0, null, 0, 0));
        territories.add(new Territory(gameId, "Pasty Mesa", 7, true, false, false,0, null, 0, 0));
        territories.add(new Territory(gameId, "Gara Kulon", 7, false, false, false,0, null, 0, 0));
        territories.add(new Territory(gameId, "False Wall East", 8, true, false, false,0, null, 0, 0));
        territories.add(new Territory(gameId, "Imperial Basin", 8, true, false, false,0, null, 0, 0));
        territories.add(new Territory(gameId, "Shield Wall", 8, true, false, false,0, null, 0, 0));
        territories.add(new Territory(gameId, "Hole in the Rock", 8, false, false,false, 0, null, 0, 0));
        territories.add(new Territory(gameId, "Rim Wall West", 8, true, false,false, 0, null, 0, 0));
        territories.add(new Territory(gameId, "Basin", 8, false, false, false,0, null, 0, 0));
        territories.add(new Territory(gameId, "Old Gap", 8, false, false, false,0, null, 0, 0));
        territories.add(new Territory(gameId, "Sihaya Ridge", 8, false, false,false, 0, null, 0, 0));
        territories.add(new Territory(gameId, "Imperial Basin", 9, true, false, false,0, null, 0, 0));
        territories.add(new Territory(gameId, "Arrakeen", 9, true, true, false,0, null, 0, 0));
        territories.add(new Territory(gameId, "Old Gap", 9, false, false, false,0, null, 0, 0));
        territories.add(new Territory(gameId, "Imperial Basin", 10, true, false,false, 0, null, 0, 0));
        territories.add(new Territory(gameId, "Arsunt", 10, false, false,false, 0, null, 0, 0));
        territories.add(new Territory(gameId, "Carthag", 10, true, true, false,0, null, 0, 0));
        territories.add(new Territory(gameId, "Tsimpo", 10, false, false,true, 0, null, 0, 0));
        territories.add(new Territory(gameId, "Broken Land", 10, false, false, true, 0, null, 0, 0));
        territories.add(new Territory(gameId, "Old Gap", 10, false, false,false, 0, null, 0, 0));
        territories.add(new Territory(gameId, "Arsunt", 11, false, false,false, 0, null, 0, 0));
        territories.add(new Territory(gameId, "Hagga Basin", 11, false, false, true, 0, null, 0, 0));
        territories.add(new Territory(gameId, "Tsimpo", 11, false, false,true, 0, null, 0, 0));
        territories.add(new Territory(gameId, "Plastic Basin", 11, true, false,true, 0, null, 0, 0));
        territories.add(new Territory(gameId, "Broken Land", 11, false, false,true, 0, null, 0, 0));
        territories.add(new Territory(gameId, "Hagga Basin", 12, false, false, true,0, null, 0, 0));
        territories.add(new Territory(gameId, "Plastic Basin", 12, true, false,true, 0, null, 0, 0));
        territories.add(new Territory(gameId, "Tsimpo", 12, false, false, true,0, null, 0, 0));
        territories.add(new Territory(gameId, "Rock Outcroppings", 12, false, false,true, 0, null, 0, 0));
        territories.add(new Territory(gameId, "Wind Pass", 13, false, false,true, 0, null, 0, 0));
        territories.add(new Territory(gameId, "Plastic Basin", 13, true, false, true,0, null, 0, 0));
        territories.add(new Territory(gameId, "Bight of the Cliff", 13, false, false, true,0, null, 0, 0));
        territories.add(new Territory(gameId, "Sietch Tabr", 13, true, true, true,0, null, 0, 0));
        territories.add(new Territory(gameId, "Rock Outcroppings", 13, false, false,true, 0, null, 0, 0));
        territories.add(new Territory(gameId, "Wind Pass", 14, false, false,true, 0, null, 0, 0));
        territories.add(new Territory(gameId, "The Great Flat", 14, false, false,true, 0, null, 0, 0));
        territories.add(new Territory(gameId, "Funeral Plain", 14, false, false,true, 0, null, 0, 0));
        territories.add(new Territory(gameId, "Bight of the Cliff", 14, false, false,true, 0, null, 0, 0));
        territories.add(new Territory(gameId, "Wind Pass", 15, false, false, true,0, null, 0, 0));
        territories.add(new Territory(gameId, "The Greater Flat", 15, false, false,true, 0, null, 0, 0));
        territories.add(new Territory(gameId, "Habbanya Erg", 15, false, false, true,0, null, 0, 0));
        territories.add(new Territory(gameId, "False Wall West", 15, true, false,true, 0, null, 0, 0));
        territories.add(new Territory(gameId, "Wind Pass North", 16, false, false, true,0, null, 0, 0));
        territories.add(new Territory(gameId, "Wind Pass", 16, false, false,true, 0, null, 0, 0));
        territories.add(new Territory(gameId, "False Wall West", 16, true, false,true, 0, null, 0, 0));
        territories.add(new Territory(gameId, "Habbanya Erg", 16, false, false,true, 0, null, 0, 0));
        territories.add(new Territory(gameId, "Habbanya Ridge Flat", 16, false, false, false, 0, null, 0, 0));
        territories.add(new Territory(gameId, "Habbanya Sietch", 16, true, true, false, 0, null, 0, 0));
        territories.add(new Territory(gameId, "Wind Pass North", 17, false, false,true, 0, null, 0, 0));
        territories.add(new Territory(gameId, "Cielago West", 17, false, false,true, 0, null, 0, 0));
        territories.add(new Territory(gameId, "False Wall West", 17, true, false, true,0, null, 0, 0));
        territories.add(new Territory(gameId, "Habbanya Ridge Flat", 17, false, false, false,0, null, 0, 0));
        territories.add(new Territory(gameId, "Cielago North", 18, false, false, false,0, null, 0, 0));
        territories.add(new Territory(gameId, "Cielago Depression", 18, false, false, false, 0, null, 0, 0));
        territories.add(new Territory(gameId, "Cielago West", 18, false, false, true, 0, null, 0, 0));
        territories.add(new Territory(gameId, "Meridian", 18, false, false, false,0, null, 0, 0));
        territories.add(new Territory(gameId, "Polar Sink", -1, false, false, true,0, null, 0, 0));
        return territories;
    }
}
