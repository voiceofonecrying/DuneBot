package model;

import exceptions.InvalidGameStateException;
import model.factions.Faction;
import java.util.*;
import java.util.stream.Collectors;

public class Bidding {
    private int bidCardNumber;
    private int numCardsForBid;

    private List<String> bidOrder;
    private TreacheryCard bidCard;
    private List<String> richeseBidOrder;
    private boolean richeseCacheCard;
    private boolean richeseCacheCardOutstanding;
    private boolean ixTechnologyUsed;

    private final LinkedList<TreacheryCard> market;
    private boolean marketPopulated;
    private boolean marketShownToIx;
    private boolean ixRejectOutstanding;
    private boolean treacheryDeckReshuffled;
    private boolean cardFromMarket;

    private String nextBidder;
    private String currentBidder;
    private int currentBid;
    private String bidLeader;
    private TreacheryCard previousCard;
    private String previousWinner;
    private boolean ixAllySwapped;

    public Bidding() {
        super();

        this.bidCardNumber = 0;
        this.numCardsForBid = 0;
        this.bidOrder = new ArrayList<>();
        this.bidCard = null;
        this.richeseBidOrder = null;
        this.richeseCacheCard = false;
        this.richeseCacheCardOutstanding = false;
        this.ixTechnologyUsed = false;
        this.market = new LinkedList<>();
        this.marketPopulated = false;
        this.marketShownToIx = false;
        this.ixRejectOutstanding = false;
        this.treacheryDeckReshuffled = false;
        this.cardFromMarket = false;
        this.nextBidder = null;
        this.currentBidder = "";
        this.currentBid = 0;
        this.bidLeader = "";
        this.previousCard = null;
        this.previousWinner = null;
        this.ixAllySwapped = false;
    }

    public TreacheryCard nextBidCard(Game game) throws InvalidGameStateException {
        treacheryDeckReshuffled = false;
        if (bidCardNumber != 0 && bidCardNumber == numCardsForBid) {
            throw new InvalidGameStateException("All cards for this round have already been bid on.");
        }
        populateMarket(game, false);
        if (market.isEmpty()) {
            throw new InvalidGameStateException("All cards from the bidding market have already been bid on.");
        }

        bidCard = market.remove(0);
        bidCardNumber++;
        cardFromMarket = true;

        for (Faction faction : game.getFactions()) {
            faction.setMaxBid(0);
            faction.setAutoBid(false);
            faction.setBid("");
        }
        return bidCard;
    }

    public int populateMarket(Game game, boolean ixInGame) {
        if (!marketPopulated) {
            int numCardsInMarket = numCardsForBid - bidCardNumber;
            if (richeseCacheCardOutstanding) numCardsInMarket--;
            if (ixInGame) numCardsInMarket++;
            List<TreacheryCard> treacheryDeck = game.getTreacheryDeck();
            for (int i = 0; i < numCardsInMarket; i++) {
                if (treacheryDeck.isEmpty()) {
                    treacheryDeckReshuffled = true;
                    List<TreacheryCard> treacheryDiscard = game.getTreacheryDiscard();
                    treacheryDeck.addAll(treacheryDiscard);
                    Collections.shuffle(treacheryDeck);
                    treacheryDiscard.clear();
                }
                market.add(treacheryDeck.remove(0));
            }
            marketPopulated = true;
            return numCardsInMarket;
        }
        return 0;
    }

    public int moveMarketToDeck(Game game) {
        int numCardsReturned = market.size() + 1;
        Iterator<TreacheryCard> marketIterator = market.descendingIterator();
        while (marketIterator.hasNext()) game.getTreacheryDeck().addFirst(marketIterator.next());
        game.getTreacheryDeck().addFirst(bidCard);
        clearBidCardInfo(null);
        return numCardsReturned;
    }

    public void putBackIxCard(Game game, String cardName, String location) {
        TreacheryCard card = market.stream()
                .filter(t -> t.name().equals(cardName))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Treachery card not found"));
        market.remove(card);
        if (location.equalsIgnoreCase("top")) {
            game.getTreacheryDeck().addFirst(card);
        } else {
            game.getTreacheryDeck().add(card);
        }
        Collections.shuffle(market);
        ixRejectOutstanding = false;
    }

    public void ixTechnology(Game game, String cardName) throws InvalidGameStateException {
        Faction faction = game.getFaction("Ix");
        if (ixTechnologyUsed) {
            throw new InvalidGameStateException("Ix has already used technology this turn.");
        } else if (bidCard != null) {
            throw new InvalidGameStateException("There is already a card up for bid.");
        } else if (numCardsForBid - bidCardNumber - (richeseCacheCardOutstanding ? 1 : 0) < market.size()) {
            throw new InvalidGameStateException("Ix must send a card back to the deck.");
        } else if (market.isEmpty()) {
            throw new InvalidGameStateException("There are no cards in the bidding market.");
        }
        TreacheryCard cardFromIx = faction.removeTreacheryCard(cardName);
        faction.addTreacheryCard(market.remove(0));
        market.addFirst(cardFromIx);
        ixTechnologyUsed = true;
    }

    public void ixAllyCardSwap(Game game) throws InvalidGameStateException {
        Faction faction = game.getFaction("Ix");
        String allyName = faction.getAlly();
        if (allyName == null) {
            throw new InvalidGameStateException(faction.getEmoji() + " does not have an ally");
        }
        Faction ally = game.getFaction(allyName);
        if (previousCard == null) {
            throw new InvalidGameStateException("No card has been won yet this turn.");
        } else if (ixAllySwapped) {
            throw new InvalidGameStateException(faction.getEmoji() + " ally " + ally.getEmoji() + " already swapped the previous card.");
        } else if (!previousWinner.equalsIgnoreCase(allyName)) {
            throw new InvalidGameStateException(faction.getEmoji() + " ally " + ally.getEmoji() + " did not win previous card.");
        }

        LinkedList<TreacheryCard> treacheryDeck = game.getTreacheryDeck();
        TreacheryCard cardToSwap = ally.removeTreacheryCard(previousCard);
        ally.addTreacheryCard(treacheryDeck.remove(0));
        treacheryDeck.addFirst(cardToSwap);
        ixAllySwapped = true;
    }

    public boolean isTreacheryDeckReshuffled() {
        return treacheryDeckReshuffled;
    }

    public boolean isCardFromMarket() {
        return cardFromMarket;
    }

    public TreacheryCard getBidCard() {
        return bidCard;
    }

    public void setBidCard(TreacheryCard bidCard) {
        this.bidCard = bidCard;
    }

    public boolean isRicheseCacheCard() {
        return richeseCacheCard;
    }

    public void setRicheseCacheCard(boolean richeseCacheCard) {
        this.richeseCacheCard = richeseCacheCard;
    }

    public boolean isRicheseCacheCardOutstanding() {
        return richeseCacheCardOutstanding;
    }

    public void setRicheseCacheCardOutstanding(boolean richeseCacheCardOutstanding) {
        this.richeseCacheCardOutstanding = richeseCacheCardOutstanding;
    }

    public boolean isRicheseBidding() {
        return richeseBidOrder != null;
    }

    public int getBidCardNumber() {
        return bidCardNumber;
    }

    public void setBidCardNumber(int bidCardNumber) {
        this.bidCardNumber = bidCardNumber;
    }

    public int getNumCardsForBid() {
        return numCardsForBid;
    }

    public void setNumCardsForBid(int numCardsForBid) {
        this.numCardsForBid = numCardsForBid;
    }

    public void incrementBidCardNumber() {
        bidCardNumber++;
    }

    public List<String> getBidOrder() {
        if (richeseBidOrder == null) return bidOrder;
        return richeseBidOrder;
    }

    public void setBidOrder(Game game, List<String> bidOrder) {
        this.bidOrder = bidOrder;
        nextBidder = getEligibleBidOrder(game).get(0);
    }

    public void setRicheseBidOrder(Game game, List<String> bidOrder) {
        this.richeseBidOrder = bidOrder;
        nextBidder = getEligibleBidOrder(game).get(0);
    }

    public List<String> getEligibleBidOrder(Game game) {
        return getBidOrder()
                .stream()
                .filter(f -> game.getFaction(f).getHandLimit() > game.getFaction(f).getTreacheryHand().size())
                .collect(Collectors.toList());
    }

    public void clearBidCardInfo(String winner) {
        bidCard = null;
        richeseBidOrder = null;
        if (richeseCacheCard) {
            richeseCacheCardOutstanding = false;
        }
        richeseCacheCard = false;
        bidLeader = "";
        currentBid = 0;
        cardFromMarket = false;
        previousCard = bidCard;
        previousWinner = winner;
        ixAllySwapped = false;
    }

    public LinkedList<TreacheryCard> getMarket() {
        return market;
    }

    public boolean isMarketPopulated() {
        return marketPopulated;
    }

    public boolean isMarketShownToIx() {
        return marketShownToIx;
    }

    public void setMarketShownToIx(boolean marketShownToIx) {
        this.marketShownToIx = marketShownToIx;
        if (marketShownToIx) ixRejectOutstanding = true;
    }

    public boolean isIxRejectOutstanding() {
        return ixRejectOutstanding;
    }

    public String getCurrentBidder() {
        return currentBidder;
    }

    public void setCurrentBidder(String currentBidder) {
        this.currentBidder = currentBidder;
    }

    public String getNextBidder(Game game) {
        Faction currentFaction = game.getFaction(currentBidder);
        int currentIndex = getEligibleBidOrder(game).indexOf(currentFaction.getName());
        int nextIndex = 0;
        if (currentIndex != getEligibleBidOrder(game).size() - 1) nextIndex = currentIndex + 1;
        nextBidder = getEligibleBidOrder(game).get(nextIndex);
        return nextBidder;
    }

    // Replace the function above after all games have nextBidder assigned by setBidOrder or getNextBidder
    // public String getNextBidder(Game game) {
    //     return nextBidder;
    // }

    public String advanceBidder(Game game) {
        currentBidder = (nextBidder != null ? nextBidder : getNextBidder(game));
        // Replace the line above after all games have nextBidder assigned by setBidOrder or getNextBidder
        // currentBidder = nextBidder;
        Faction currentFaction = game.getFaction(currentBidder);
        int currentIndex = getEligibleBidOrder(game).indexOf(currentFaction.getName());
        int nextIndex = 0;
        if (currentIndex != getEligibleBidOrder(game).size() - 1) nextIndex = currentIndex + 1;
        nextBidder = getEligibleBidOrder(game).get(nextIndex);
        return currentBidder;
    }

    public int getCurrentBid() {
        return currentBid;
    }

    public void setCurrentBid(int currentBid) {
        this.currentBid = currentBid;
    }

    public String getBidLeader() {
        return bidLeader;
    }

    public void setBidLeader(String bidLeader) {
        this.bidLeader = bidLeader;
    }
}
