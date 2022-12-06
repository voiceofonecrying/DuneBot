package model;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class Deck {
    private final String name;
    private final LinkedList<Card> cards;

    public Deck(String name) {
        this.name = name;
        cards = new LinkedList<>();
    }

    public String getName() {
        return name;
    }

    public void addCard(Card card) {
        cards.add(card);
    }

    public Card pop() { return cards.pop(); }

    public void push(Card card) { cards.push(card); }

    public List<Card> getTopCards(int number) {
        return cards.subList(0, number - 1);
    }

    public List<Card> getAllCards() {
        return cards;
    }

    public void shuffle() {
        Collections.shuffle(cards);
    }
}
