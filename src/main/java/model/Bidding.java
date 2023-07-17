package model;

import java.util.*;

public class Bidding {
    private int bidCardNumber;
    private int numCardsForBid;

    private List<String> bidOrder;
    private TreacheryCard bidCard;
    private boolean richeseCacheCard;

    private final LinkedList<TreacheryCard> market;

    private String currentBidder;
    private int currentBid;
    private String bidLeader;

    public Bidding() {
        super();

        this.bidCardNumber = 0;
        this.numCardsForBid = 0;
        this.bidOrder = new ArrayList<>();
        this.bidCard = null;
        this.market = new LinkedList<>();
        this.currentBidder = "";
        this.currentBid = 0;
        this.bidLeader = "";
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
        richeseCacheCard = false;
        bidLeader = "";
        currentBid = 0;
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
