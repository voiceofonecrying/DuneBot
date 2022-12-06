package model;

import java.util.ArrayList;
import java.util.List;

public class GameFactionBase {
    private final List<Resource> resources;
    private final List<Deck> decks;

    public GameFactionBase() {
        this.resources = new ArrayList<>();
        this.decks = new ArrayList<>();
    }

    public List<Resource> getResources() {
        return resources;
    }

    public void addResource(Resource resource) {
        resources.add(resource);
    }

    public void removeResource(String resourceName) {
        this.resources.remove(getResource(resourceName));
    }


    public Resource getResource(String name) {
        return resources.stream()
                .filter(r -> r.getName().equals(name))
                .findFirst()
                .get();
    }

    public List<Deck> getDecks() {
        return decks;
    }

    public void addDeck(Deck deck) {
        decks.add(deck);
    }

    public Deck getDeck(String name) {
        return decks.stream()
                .filter(d -> d.getName().equals(name))
                .findFirst()
                .get();
    }
}
