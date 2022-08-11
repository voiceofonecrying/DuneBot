package Model;

public record Leader (String name, int strength, Faction faction, Faction traitorFor, boolean isAlive) {}
