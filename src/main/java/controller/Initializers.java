package controller;

import model.SpiceCard;
import model.Territory;
import model.TreacheryCard;

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

    public static List<SpiceCard> buildSpiceDeck(int gameId) {
        List<SpiceCard> spiceDeck = new ArrayList<>();

        spiceDeck.add(new SpiceCard(gameId, "Habbanya Ridge Flat", 17, "Deck", 10));
        spiceDeck.add(new SpiceCard(gameId, "Broken Land", 11, "Deck", 8));
        spiceDeck.add(new SpiceCard(gameId, "Cielago South", 1, "Deck", 12));
        spiceDeck.add(new SpiceCard(gameId, "South Mesa", 4, "Deck", 10));
        spiceDeck.add(new SpiceCard(gameId, "Sihaya Ridge", 8, "Deck", 6));
        spiceDeck.add(new SpiceCard(gameId, "Shai-Hulud(1)", -1, "Deck", 0));
        spiceDeck.add(new SpiceCard(gameId, "Shai-Hulud(2)", -1, "Deck", 0));
        spiceDeck.add(new SpiceCard(gameId, "Shai-Hulud(3)", -1, "Deck", 0));
        spiceDeck.add(new SpiceCard(gameId, "Shai-Hulud(4)", -1, "Deck", 0));
        spiceDeck.add(new SpiceCard(gameId, "Shai-Hulud(5)", -1, "Deck", 0));
        spiceDeck.add(new SpiceCard(gameId, "Shai-Hulud(6)", -1, "Deck", 0));
        spiceDeck.add(new SpiceCard(gameId, "Hagga Basin", 12, "Deck", 6));
        spiceDeck.add(new SpiceCard(gameId, "Red Chasm", 6, "Deck", 8));
        spiceDeck.add(new SpiceCard(gameId, "The Minor Erg", 7, "Deck", 8));
        spiceDeck.add(new SpiceCard(gameId, "Cielago North", 2, "Deck", 8));
        spiceDeck.add(new SpiceCard(gameId, "Funeral Plain", 14, "Deck", 6));
        spiceDeck.add(new SpiceCard(gameId, "The Great Flat", 14, "Deck", 10));
        spiceDeck.add(new SpiceCard(gameId, "Habbanya Erg", 15, "Deck", 8));
        spiceDeck.add(new SpiceCard(gameId, "Old Gap", 9, "Deck", 6));
        spiceDeck.add(new SpiceCard(gameId, "Rock Outcroppings", 13, "Deck", 6));
        spiceDeck.add(new SpiceCard(gameId, "Wind Pass North", 16, "Deck", 6));

        return spiceDeck;
    }

    public static List<TreacheryCard> buildTreacheryDeck(int gameId) {
        List<TreacheryCard> treacheryDeck = new ArrayList<>();

        treacheryDeck.add(new TreacheryCard(gameId, "Lasgun", "Deck"));
        treacheryDeck.add(new TreacheryCard(gameId, "Chaumas", "Deck"));
        treacheryDeck.add(new TreacheryCard(gameId, "Chaumurky", "Deck"));
        treacheryDeck.add(new TreacheryCard(gameId, "Ellaca Drug", "Deck"));
        treacheryDeck.add(new TreacheryCard(gameId, "Gom Jabbar", "Deck"));
        treacheryDeck.add(new TreacheryCard(gameId, "Stunner", "Deck"));
        treacheryDeck.add(new TreacheryCard(gameId, "Slip Tip", "Deck"));
        treacheryDeck.add(new TreacheryCard(gameId, "Maula Pistol", "Deck"));
        treacheryDeck.add(new TreacheryCard(gameId, "Crysknife", "Deck"));
        treacheryDeck.add(new TreacheryCard(gameId, "Shield(1)", "Deck"));
        treacheryDeck.add(new TreacheryCard(gameId, "Shield(2)", "Deck"));
        treacheryDeck.add(new TreacheryCard(gameId, "Shield(3)", "Deck"));
        treacheryDeck.add(new TreacheryCard(gameId, "Shield(4)", "Deck"));
        treacheryDeck.add(new TreacheryCard(gameId, "Snooper(1)", "Deck"));
        treacheryDeck.add(new TreacheryCard(gameId, "Snooper(2)", "Deck"));
        treacheryDeck.add(new TreacheryCard(gameId, "Snooper(3)", "Deck"));
        treacheryDeck.add(new TreacheryCard(gameId, "Snooper(4)", "Deck"));
        treacheryDeck.add(new TreacheryCard(gameId, "La, La, La", "Deck"));
        treacheryDeck.add(new TreacheryCard(gameId, "Baliset", "Deck"));
        treacheryDeck.add(new TreacheryCard(gameId, "Kulon", "Deck"));
        treacheryDeck.add(new TreacheryCard(gameId, "Trip to Gamont", "Deck"));
        treacheryDeck.add(new TreacheryCard(gameId, "Jubba Cloak", "Deck"));
        treacheryDeck.add(new TreacheryCard(gameId, "Tleilaxu Ghola", "Deck"));
        treacheryDeck.add(new TreacheryCard(gameId, "Truthtrance(1)", "Deck"));
        treacheryDeck.add(new TreacheryCard(gameId, "Truthtrance(2)", "Deck"));
        treacheryDeck.add(new TreacheryCard(gameId, "Weather Control", "Deck"));
        treacheryDeck.add(new TreacheryCard(gameId, "Karama(1)", "Deck"));
        treacheryDeck.add(new TreacheryCard(gameId, "Karama(2)", "Deck"));
        treacheryDeck.add(new TreacheryCard(gameId, "Family Atomics", "Deck"));
        treacheryDeck.add(new TreacheryCard(gameId, "Hajr", "Deck"));
        treacheryDeck.add(new TreacheryCard(gameId, "Cheap Hero(1)", "Deck"));
        treacheryDeck.add(new TreacheryCard(gameId, "Cheap Hero(2)", "Deck"));
        treacheryDeck.add(new TreacheryCard(gameId, "Cheap Hero(3)", "Deck"));

        return treacheryDeck;
    }
}
