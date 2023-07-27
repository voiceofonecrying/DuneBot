package model;

import exceptions.InvalidGameStateException;
import model.factions.Faction;
import java.util.*;

public class Bidding {
    private int bidCardNumber;
    private int numCardsForBid;

    private List<String> bidOrder;
    private TreacheryCard bidCard;
    private boolean richeseCacheCard;
    private boolean richeseCacheCardOutstanding;

    private final LinkedList<TreacheryCard> market;
    private boolean marketPopulated;
    private boolean treacheryDeckReshuffled;
    private boolean cardFromMarket;

    private String currentBidder;
    private int currentBid;
    private String bidLeader;

    public Bidding() {
        super();

        this.bidCardNumber = 0;
        this.numCardsForBid = 0;
        this.bidOrder = new ArrayList<>();
        this.bidCard = null;
        this.richeseCacheCard = false;
        this.richeseCacheCardOutstanding = false;
        this.market = new LinkedList<>();
        this.marketPopulated = false;
        this.treacheryDeckReshuffled = false;
        this.cardFromMarket = false;
        this.currentBidder = "";
        this.currentBid = 0;
        this.bidLeader = "";
    }

    public TreacheryCard nextBidCard(Game game) throws InvalidGameStateException {
        treacheryDeckReshuffled = false;
        if (bidCardNumber != 0 && bidCardNumber == numCardsForBid) {
            throw new InvalidGameStateException("All cards for this round have already been bid on.");
        }
        int numCardsInMarket = numCardsForBid - bidCardNumber;
        if (richeseCacheCardOutstanding) numCardsInMarket--;

        if (!marketPopulated) {
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

    public int moveMarketToDeck(Game game) {
        int numCardsReturned = market.size() + 1;
        Iterator<TreacheryCard> marketIterator = market.descendingIterator();
        while (marketIterator.hasNext()) game.getTreacheryDeck().addFirst(marketIterator.next());
        game.getTreacheryDeck().addFirst(bidCard);
        clearBidCardInfo();
        return numCardsReturned;
    }

    public boolean isTreacheryDeckReshuffled() {
        return treacheryDeckReshuffled;
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
        return bidOrder;
    }

    public void setBidOrder(List<String> bidOrder) {
        this.bidOrder = bidOrder;
    }

    public void clearBidCardInfo() {
        bidCard = null;
        if (richeseCacheCard) {
            richeseCacheCardOutstanding = false;
        }
        richeseCacheCard = false;
        bidLeader = "";
        currentBid = 0;
        cardFromMarket = false;
    }

    public LinkedList<TreacheryCard> getMarket() {
        return market;
    }

    public String getCurrentBidder() {
        return currentBidder;
    }

    public void setCurrentBidder(String currentBidder) {
        this.currentBidder = currentBidder;
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
