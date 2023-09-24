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
    private boolean blackMarketCard;
    private boolean silentAuction;
    private boolean richeseCacheCardOutstanding;
    private boolean ixTechnologyUsed;

    private final LinkedList<TreacheryCard> market;
    private boolean marketPopulated;
    private boolean marketShownToIx;
    private boolean ixRejectOutstanding;
    private boolean treacheryDeckReshuffled;
    private int numCardsFromOldDeck;
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
        this.blackMarketCard = false;
        this.silentAuction = false;
        this.richeseCacheCardOutstanding = false;
        this.ixTechnologyUsed = false;
        this.market = new LinkedList<>();
        this.marketPopulated = false;
        this.marketShownToIx = false;
        this.ixRejectOutstanding = false;
        this.treacheryDeckReshuffled = false;
        this.numCardsFromOldDeck = 0;
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
        numCardsFromOldDeck = 0;
        if (bidCardNumber != 0 && bidCardNumber == numCardsForBid) {
            throw new InvalidGameStateException("All cards for this round have already been bid on.");
        }
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

    public int populateMarket(Game game) throws InvalidGameStateException {
        if (marketPopulated) {
            throw new InvalidGameStateException("Bidding market is already populated.");
        }
        List<Faction> factions = game.getFactions();
        numCardsForBid = factions.stream()
                .filter(f -> f.getHandLimit() > f.getTreacheryHand().size())
                .toList().size();
        int numCardsInMarket = numCardsForBid - bidCardNumber;
        if (richeseCacheCardOutstanding) numCardsInMarket--;
        if (game.hasFaction("Ix")) numCardsInMarket++;
        List<TreacheryCard> treacheryDeck = game.getTreacheryDeck();
        for (int i = 0; i < numCardsInMarket; i++) {
            if (treacheryDeck.isEmpty()) {
                treacheryDeckReshuffled = true;
                numCardsFromOldDeck = i;
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

    public int moveMarketToDeck(Game game) {
        int numCardsReturned = market.size() + 1;
        Iterator<TreacheryCard> marketIterator = market.descendingIterator();
        while (marketIterator.hasNext()) game.getTreacheryDeck().addFirst(marketIterator.next());
        game.getTreacheryDeck().addFirst(bidCard);
        clearBidCardInfo(null);
        numCardsForBid -= numCardsReturned;
        return numCardsReturned;
    }

    public void putBackIxCard(Game game, String cardName, String location) {
        TreacheryCard card = market.stream()
                .filter(t -> t.name().equals(cardName))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Treachery card not found"));
        market.remove(card);
        if (location.equalsIgnoreCase("top")) {
            game.getTreacheryDeck().add(card);
        } else {
            game.getTreacheryDeck().addFirst(card);
        }
        Collections.shuffle(market);
        ixRejectOutstanding = false;
    }

    public TreacheryCard ixTechnology(Game game, String cardName) throws InvalidGameStateException {
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
        TreacheryCard newCard = market.remove(0);
        faction.addTreacheryCard(newCard);
        market.addFirst(cardFromIx);
        ixTechnologyUsed = true;
        return newCard;
    }

    public TreacheryCard ixAllyCardSwap(Game game) throws InvalidGameStateException {
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
        treacheryDeckReshuffled = false;
        if (treacheryDeck.isEmpty()) {
            treacheryDeckReshuffled = true;
            List<TreacheryCard> treacheryDiscard = game.getTreacheryDiscard();
            treacheryDeck.addAll(treacheryDiscard);
            Collections.shuffle(treacheryDeck);
            treacheryDiscard.clear();
        }
        TreacheryCard cardToSwap = ally.removeTreacheryCard(previousCard);
        TreacheryCard newCard = treacheryDeck.pollLast();
        ally.addTreacheryCard(newCard);
        game.getTreacheryDiscard().add(cardToSwap);
        ixAllySwapped = true;
        return newCard;
    }

    public boolean isTreacheryDeckReshuffled() {
        return treacheryDeckReshuffled;
    }

    public int getNumCardsFromOldDeck() {
        return numCardsFromOldDeck;
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

    public boolean isBlackMarketCard() {
        return blackMarketCard;
    }

    public void setBlackMarketCard(boolean blackMarketCard) {
        this.blackMarketCard = blackMarketCard;
    }

    public boolean isSilentAuction() {
        return silentAuction;
    }

    public void setSilentAuction(boolean silentAuction) {
        this.silentAuction = silentAuction;
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

    public int getNumCardsForBid() {
        return numCardsForBid;
    }

    public void incrementBidCardNumber() {
        bidCardNumber++;
    }

    public void decrementBidCardNumber() {
        bidCardNumber--;
    }

    private List<String> getBidOrder() {
        if (richeseBidOrder == null) return bidOrder;
        return richeseBidOrder;
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

    public void updateBidOrder(Game game) {
        if (bidOrder.isEmpty()) {
            List<Faction> factions = game.getFactions();

            int firstBid = Math.ceilDiv(game.getStorm(), 3) % factions.size();

            List<Faction> bidOrderFactions = new ArrayList<>();

            bidOrderFactions.addAll(factions.subList(firstBid, factions.size()));
            bidOrderFactions.addAll(factions.subList(0, firstBid));
            bidOrder = bidOrderFactions.stream().map(Faction::getName).collect(Collectors.toList());
        } else {
            bidOrder.add(bidOrder.remove(0));
        }

        String firstFaction = bidOrder.get(0);
        String faction = firstFaction;
        while (game.getFaction(faction).getHandLimit() <= game.getFaction(faction).getTreacheryHand().size()) {
            faction = bidOrder.remove(0);
            bidOrder.add(faction);
            if (faction.equalsIgnoreCase(firstFaction)) {
                break;
            }
        }
    }

    public void clearBidCardInfo(String winner) {
        previousCard = bidCard;
        bidCard = null;
        richeseBidOrder = null;
        if (richeseCacheCard) {
            richeseCacheCardOutstanding = false;
        }
        richeseCacheCard = false;
        blackMarketCard = false;
        silentAuction = false;
        bidLeader = "";
        currentBid = 0;
        cardFromMarket = false;
        previousWinner = winner;
        ixAllySwapped = false;
    }

    public LinkedList<TreacheryCard> getMarket() {
        return market;
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

    public String advanceBidder(Game game) {
        currentBidder = getNextBidder(game);
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

    public TreacheryCard getPreviousCard() { return previousCard; }
}
