package model;

import constants.Emojis;
import enums.GameOption;
import exceptions.ChannelNotFoundException;
import exceptions.InvalidGameStateException;
import model.factions.*;
import model.topics.DuneTopic;

import java.text.MessageFormat;
import java.util.*;
import java.util.stream.Collectors;

public class Bidding {
    private final LinkedList<TreacheryCard> market;
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
    private int ixTechnologyCardNumber;
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
    private boolean topBidderDeclared;
    private boolean allPlayersPassed;
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
        this.ixTechnologyCardNumber = 0;
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
        this.topBidderDeclared = false;
        this.allPlayersPassed = false;
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

        for (Faction faction : game.getFactions()) {
            faction.setMaxBid(0);
            faction.setBid("");
            if (bidCardNumber == 0)
                faction.setAutoBidTurn(false);
            faction.setAutoBid(faction.isAutoBidTurn());
        }

        bidCard = market.removeFirst();
        bidCardNumber++;
        cardFromMarket = true;
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
        LinkedList<TreacheryCard> treacheryDeck = game.getTreacheryDeck();
        for (int i = 0; i < numCardsInMarket; i++) {
            if (treacheryDeck.isEmpty()) {
                treacheryDeckReshuffled = true;
                numCardsFromOldDeck = i;
                List<TreacheryCard> treacheryDiscard = game.getTreacheryDiscard();
                treacheryDeck.addAll(treacheryDiscard);
                Collections.shuffle(treacheryDeck);
                treacheryDiscard.clear();
            }
            market.add(treacheryDeck.pollLast());
        }
        marketPopulated = true;
        return numCardsInMarket;
    }

    public int moveMarketToDeck(Game game) {
        int numCardsReturned = market.size() + 1;
        Iterator<TreacheryCard> marketIterator = market.descendingIterator();
        while (marketIterator.hasNext()) game.getTreacheryDeck().add(marketIterator.next());
        game.getTreacheryDeck().add(bidCard);
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
        TreacheryCard newCard = market.removeFirst();
        faction.addTreacheryCard(newCard);
        market.addFirst(cardFromIx);
        ixTechnologyUsed = true;
        ixTechnologyCardNumber = bidCardNumber + 1;
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

    public boolean isCardFromIxHand() {
        return bidCardNumber == ixTechnologyCardNumber;
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
        nextBidder = getEligibleBidOrder(game).getFirst();
    }

    public List<String> getEligibleBidOrder(Game game) {
        return getBidOrder()
                .stream()
                .filter(f -> game.getFaction(f).getHandLimit() > game.getFaction(f).getTreacheryHand().size())
                .collect(Collectors.toList());
    }

    public void updateBidOrder(Game game) {
        if (bidOrder.isEmpty()) {
            List<Faction> bidOrderFactions = game.getFactionsInStormOrder();
            bidOrder = bidOrderFactions.stream().map(Faction::getName).collect(Collectors.toList());
        } else {
            bidOrder.add(bidOrder.removeFirst());
        }

        String firstFaction = bidOrder.getFirst();
        String faction = firstFaction;
        while (game.getFaction(faction).getHandLimit() <= game.getFaction(faction).getTreacheryHand().size()) {
            bidOrder.add(bidOrder.removeFirst());
            faction = bidOrder.getFirst();
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
        topBidderDeclared = false;
        allPlayersPassed = false;
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

    public void blockIxBiddingAdvantage(Game game) throws InvalidGameStateException {
        this.marketShownToIx = true;
        TreacheryCard card = market.pollLast();
        if (card == null)
            throw new InvalidGameStateException("There are no cards in the bidding market.");
        game.getTreacheryDeck().add(card);
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

    public TreacheryCard getPreviousCard() {
        return previousCard;
    }

    public boolean createBidMessage(Game game, boolean tag) {
        DuneTopic biddingPhase = game.getBiddingPhase();
        String nextBidderName = getNextBidder(game);
        List<String> bidOrder = getEligibleBidOrder(game);
        StringBuilder message = new StringBuilder();
        message.append(
                MessageFormat.format(
                        "R{0}:C{1}",
                        game.getTurn(), bidCardNumber
                )
        );
        if (silentAuction) {
            message.append(" (Silent Auction)");
        } else if (isRicheseBidding()) {
            message.append(" (Once Around)");
        }
        message.append("\n");

        for (String factionName : bidOrder) {
            Faction f = game.getFaction(factionName);
            if (factionName.equals(nextBidderName) && tag) {
                if (f.getName().equals(bidLeader)) {
                    biddingPhase.publish(message.toString());
                    biddingPhase.publish(f.getEmoji() + " has the top bid.");
                    return true;
                }
                message.append(f.getEmoji()).append(" - ").append(f.getPlayer()).append("\n");
            } else {
                message.append(f.getEmoji()).append(" - ").append(f.getBid()).append("\n");
            }
        }

        biddingPhase.publish(message.toString());
        return false;
    }

    public void presentCacheCardChoices(Game game) {
        RicheseFaction richeseFaction = (RicheseFaction) game.getFaction("Richese");
        List<DuneChoice> choices = new ArrayList<>();
        String message;
        for (TreacheryCard card : richeseFaction.getTreacheryCardCache()) {
            choices.add(new DuneChoice("richesecachecard-" + card.name(), card.name()));
        }
        if (bidCardNumber < numCardsForBid - 1) {
            message = "Please select your cache card to sell or choose to sell last. " + richeseFaction.getPlayer();
            choices.add(new DuneChoice("danger", "richesecachetime-last", "Sell cache card last"));
        } else {
            message = "Please select your cache card to sell. You must sell now. " + richeseFaction.getPlayer();
        }
        if (!richeseFaction.isHomeworldOccupied()) richeseFaction.getChat().publish(message, choices);
        else {
            richeseFaction.getOccupier().getChat().publish(message, choices);
            richeseFaction.getOccupier().getChat().publish("(You are getting these buttons because you occupy " + Emojis.RICHESE + " homeworld)");
        }
    }

    public void assignAndPayForCard(Game game, String winnerName, String paidToFactionName, int spentValue) throws InvalidGameStateException {
        if (bidCard == null) {
            throw new InvalidGameStateException("There is no card up for bid.");
        }
        Faction winner = game.getFaction(winnerName);
        List<TreacheryCard> winnerHand = winner.getTreacheryHand();
        if ((!winner.hasAlly() && winner.getSpice() < spentValue) || (winner.hasAlly() && winner.getSpice() + winner.getAllySpiceBidding() < spentValue)) {
            throw new InvalidGameStateException(winner.getEmoji() + " does not have enough spice to buy the card.");
        } else if (winnerHand.size() >= winner.getHandLimit()) {
            throw new InvalidGameStateException(winner.getEmoji() + " already has a full hand.");
        }

        String currentCard = MessageFormat.format(
                "R{0}:C{1}",
                game.getTurn(),
                bidCardNumber
        );
        int allySupport = Math.min(winner.getAllySpiceBidding(), spentValue);

        String allyString = winner.hasAlly() && winner.getAllySpiceBidding() > 0 ? "(" + allySupport + " from " + game.getFaction(winner.getAlly()).getEmoji() + ")" : "";

        DuneTopic turnSummary = game.getTurnSummary();
        turnSummary.publish(
                MessageFormat.format(
                        "{0} wins {1} for {2} {3} {4}",
                        winner.getEmoji(),
                        currentCard,
                        spentValue,
                        Emojis.SPICE,
                        allyString
                )
        );

        // Winner pays for the card
        winner.setAllySpiceBidding(Math.max(winner.getAllySpiceBidding() - spentValue, 0));
        winner.subtractSpice(spentValue - allySupport, currentCard);
        if (winner.hasAlly()) {
            game.getFaction(winner.getAlly()).subtractSpice(allySupport, currentCard + " (ally support)");
        }

        if (game.hasFaction(paidToFactionName)) {
            int spicePaid = spentValue;
            Faction paidToFaction = game.getFaction(paidToFactionName);

            if (paidToFaction instanceof EmperorFaction && game.hasGameOption(GameOption.HOMEWORLDS)
                    && !paidToFaction.isHighThreshold()) {
                spicePaid = Math.ceilDiv(spentValue, 2);
                if (paidToFaction.isHomeworldOccupied()) {
                    Faction occupier = paidToFaction.getOccupier();
                    occupier.addSpice(Math.floorDiv(spentValue, 2), "Tribute from " + Emojis.EMPEROR + " for " + currentCard);
                    turnSummary.publish(
                            MessageFormat.format(
                                    "{0} is paid {1} {2} for {3} (homeworld occupied)",
                                    occupier.getEmoji(),
                                    Math.floorDiv(spentValue, 2),
                                    Emojis.SPICE,
                                    currentCard
                            )
                    );
                }
            }

            if (paidToFaction instanceof RicheseFaction && paidToFaction.isHomeworldOccupied()) {
                spicePaid = Math.ceilDiv(spentValue, 2);
                Faction occupier = paidToFaction.getOccupier();
                occupier.addSpice(Math.floorDiv(spentValue, 2), "Tribute from " + Emojis.EMPEROR + " for " + currentCard);
                turnSummary.publish(
                        MessageFormat.format(
                                "{0} is paid {1} {2} for {3} (homeworld occupied)",
                                occupier.getEmoji(),
                                Math.floorDiv(spentValue, 2),
                                Emojis.SPICE,
                                currentCard
                        )
                );
            }

            paidToFaction.addSpice(spicePaid, currentCard);

            turnSummary.publish(
                    MessageFormat.format(
                            "{0} is paid {1} {2} for {3}",
                            paidToFaction.getEmoji(),
                            spicePaid,
                            Emojis.SPICE,
                            currentCard
                    )
            );
        }

        winner.addTreacheryCard(bidCard);
        winner.getLedger().publish(
                "Received " + bidCard.name() +
                        " from bidding. (R" + game.getTurn() + ":C" + bidCardNumber + ")");
        clearBidCardInfo(winnerName);

        // Harkonnen draw an additional card
        if (winner instanceof HarkonnenFaction && winnerHand.size() < winner.getHandLimit() && !winner.isHomeworldOccupied()) {
            if (game.drawTreacheryCard("Harkonnen")) {
                turnSummary.publish(MessageFormat.format(
                        "The {0} deck was empty and has been replenished from the discard pile.",
                        Emojis.TREACHERY
                ));
            }

            turnSummary.publish(MessageFormat.format(
                    "{0} draws another card from the {1} deck.",
                    winner.getEmoji(), Emojis.TREACHERY
            ));

            TreacheryCard addedCard = winner.getLastTreacheryCard();
            winner.getLedger().publish(
                    "Received " + addedCard.name() + " as an extra card. (" + currentCard + ")"
            );

        } else if (winner instanceof HarkonnenFaction && winner.isHomeworldOccupied() && winner.getOccupier().hasAlly()) {
            game.getModInfo().publish("Harkonnen occupier or ally may draw one from the deck (you must do this for them).");
            game.getTurnSummary().publish("Giedi Prime is occupied by " + winner.getOccupier().getName() + ", they or their ally may draw an additional card from the deck.");
        } else if (winner instanceof HarkonnenFaction && winner.isHomeworldOccupied() && winner.getOccupier().getTreacheryHand().size() < winner.getOccupier().getHandLimit()) {
            game.drawCard("treachery deck", winner.getOccupier().getName());
            turnSummary.publish(MessageFormat.format(
                    "Giedi Prime is occupied, {0} draws another card from the {1} deck instead of {2}.",
                    winner.getEmoji(), Emojis.TREACHERY, Emojis.HARKONNEN
            ));
        }

        if (market.isEmpty() && bidCardNumber == numCardsForBid - 1 && richeseCacheCardOutstanding) {
            presentCacheCardChoices(game);
            game.getModInfo().publish(Emojis.RICHESE + " has been asked to select the last card of the turn.");
        }
    }

    public void awardTopBidder(Game game) throws InvalidGameStateException {
        String winnerName = bidLeader;
        if (winnerName.isEmpty()) {
            if (richeseCacheCard || blackMarketCard)
                assignAndPayForCard(game, "Richese", "", 0);
            else
                throw new InvalidGameStateException("There is no top bidder for this card.");
        } else {
            String paidToFactionName = "Bank";
            if ((richeseCacheCard || blackMarketCard) && !winnerName.equals("Richese"))
                paidToFactionName = "Richese";
            else if (!winnerName.equals("Emperor"))
                paidToFactionName = "Emperor";
            int spentValue = currentBid;
            assignAndPayForCard(game, winnerName, paidToFactionName, spentValue);
        }
    }

    public boolean richeseWinner(Game game, boolean allPlayersPassed) throws InvalidGameStateException {
        DuneTopic modInfo = game.getModInfo();
        DuneTopic biddingPhase = game.getBiddingPhase();
        if (allPlayersPassed) {
            biddingPhase.publish("All players passed.\n");
            if (richeseCacheCard) {
                topBidderDeclared = true;
                biddingPhase.publish(Emojis.RICHESE + " may take cache card for free or remove it from the game.");
                modInfo.publish("Use /award-top-bidder to assign card back to " + Emojis.RICHESE + ". Use /richese remove-card to remove it from the game. " + game.getModOrRoleMention());
            } else {
                decrementBidCardNumber();
                biddingPhase.publish("The black market card has been returned to " + Emojis.RICHESE);
                modInfo.publish("The black market card has been returned to " + Emojis.RICHESE);
                modInfo.publish("Use /run advance to continue the bidding phase. " + game.getModOrRoleMention());
                awardTopBidder(game);
                return true;
            }
        } else {
            String winnerEmoji = game.getFaction(bidLeader).getEmoji();
            biddingPhase.publish(winnerEmoji + " has the top bid.");
            String modMessage;
            if (richeseCacheCard) {
                if (bidCardNumber == numCardsForBid) {
                    modMessage = "Use /run advance to end the bidding phase. ";
                } else {
                    modMessage = "Use /run bidding to put the next card up for bid. ";
                }
            } else {
                modMessage = "Use /run advance to continue the bidding phase. ";
            }
            awardTopBidder(game);
            modInfo.publish("The card has been awarded to " + winnerEmoji);
            modInfo.publish(modMessage + game.getModOrRoleMention());
            return true;
        }
        return false;
    }

    private boolean factionDoesNotHaveKarama(Faction faction) {
        List<TreacheryCard> hand = faction.getTreacheryHand();
        if (faction instanceof BGFaction && hand.stream().anyMatch(c -> c.type().equals("Worthless Card"))) {
            return false;
        }
        return hand.stream().noneMatch(c -> c.name().equals("Karama"));
    }

    public String pass(Game game, Faction faction) throws ChannelNotFoundException, InvalidGameStateException {
        if (topBidderDeclared || allPlayersPassed)
            throw new InvalidGameStateException("Bidding has ended on the current card.\nset-auto-pass-entire-turn is the only valid bidding command until the next card is auctions.");
        faction.setMaxBid(-1);
        game.getModInfo().publish(faction.getEmoji() + " passed their bid.");
        tryBid(game, faction);
        if (faction.isAutoBid() && !game.getBidding().isSilentAuction())
            return "You will auto-pass until the next card or until you set auto-pass to false.";
        return "You will pass one time.";
    }

    public String setAutoPass(Game game, Faction faction, boolean enabled) throws InvalidGameStateException {
        if (topBidderDeclared || allPlayersPassed)
            throw new InvalidGameStateException("Bidding has ended on the current card.\nset-auto-pass-entire-turn is the only valid bidding command until the next card is auctions.");
        faction.setAutoBid(enabled);
        game.getModInfo().publish(faction.getEmoji() + " set auto-pass to " + enabled);
        tryBid(game, faction);
        String responseMessage = "You set auto-pass to " + enabled + ".";
        if (enabled) {
            responseMessage += "\nYou will auto-pass if the top bid is " + faction.getMaxBid() + " or higher.";
        }
        return responseMessage;
    }

    public String setAutoPassEntireTurn(Game game, Faction faction, boolean enabled) throws InvalidGameStateException {
        faction.setAutoBidTurn(enabled);
        game.getModInfo().publish(faction.getEmoji() + " set auto-pass-entire-turn to " + enabled);
        if (!topBidderDeclared &&!allPlayersPassed && bidCard != null) {
            faction.setAutoBid(enabled);
            tryBid(game, faction);
        }
        String responseMessage = "You set auto-pass-entire-turn to " + enabled + ".";
        if (enabled) {
            responseMessage += "\nYou will auto-pass if the top bid is " + faction.getMaxBid() + " or higher on this card then auto-pass on remaining cards this turn.";
        } else {
            responseMessage += "\nYou are back to normal bidding, and auto-pass is diabled for this card.";
        }
        return responseMessage;
    }

    public String bid(Game game, Faction faction, boolean useExact, int bidAmount, Boolean newOutbidAllySetting, Boolean enableAutoPass) throws ChannelNotFoundException, InvalidGameStateException {
        if (topBidderDeclared || allPlayersPassed)
            throw new InvalidGameStateException("Bidding has ended on the current card.\nset-auto-pass-entire-turn is the only valid bidding command until the next card is auctions.");
        if (bidAmount > faction.getSpice() + faction.getAllySpiceBidding()
                && factionDoesNotHaveKarama(faction))
            throw new InvalidGameStateException("You have insufficient " + Emojis.SPICE + " for this bid and no Karama to avoid paying.");

        faction.setUseExact(useExact);
        faction.setMaxBid(bidAmount);
        String modMessage = faction.getEmoji() + " set their bid to " + (useExact ? "exactly " : "increment up to ") + bidAmount + ".";
        String responseMessage = "You will bid ";
        boolean silentAuction = game.getBidding().isSilentAuction();
        if (silentAuction) {
            responseMessage += "exactly " + bidAmount + " in the silent auction.";
        } else if (useExact) {
            responseMessage += "exactly " + bidAmount + " if possible.";
        } else {
            responseMessage += "+1 up to " + bidAmount + ".";
        }
        int spiceAvaiable = faction.getSpice() + faction.getAllySpiceBidding();
        if (bidAmount > faction.getSpice() + faction.getAllySpiceBidding())
            responseMessage += "\nIf you win for more than " + spiceAvaiable + ", you will have to use your Karama.";
        if (enableAutoPass != null) {
            faction.setAutoBid(enableAutoPass);
            modMessage += enableAutoPass ? " Auto-pass enabled." : " No auto-pass.";
        }
        game.getModInfo().publish(modMessage);
        String responseMessage2 = "";
        if (!silentAuction) {
            if (faction.isAutoBid()) {
                responseMessage += "\nYou will then auto-pass.";
            } else {
                responseMessage += "\nYou will not auto-pass.\nA new bid or pass will be needed if you are outbid.";
            }
            boolean outbidAllyValue = faction.isOutbidAlly();
            if (newOutbidAllySetting != null) {
                outbidAllyValue = newOutbidAllySetting;
                faction.setOutbidAlly(outbidAllyValue);
                responseMessage2 = faction.getEmoji() + " set their outbid ally policy to " + outbidAllyValue;
                game.getModInfo().publish(responseMessage2);
                faction.getChat().publish(responseMessage2);
            }
            if (faction.hasAlly()) {
                responseMessage2 = "\nYou will" + (outbidAllyValue ? "" : " not") + " outbid your ally";
            }
        }
        game.getBidding().tryBid(game, faction);
        return responseMessage + responseMessage2;
    }

    public void tryBid(Game game, Faction faction) throws InvalidGameStateException {
        if (bidCard == null)
            throw new InvalidGameStateException("There is no card currently up for bid.");
        List<String> eligibleBidOrder = getEligibleBidOrder(game);
        if (eligibleBidOrder.isEmpty() && !silentAuction) {
            throw new InvalidGameStateException("All hands are full.");
        }
        if (silentAuction) {
            if (faction.getMaxBid() == -1) {
                faction.setBid("pass");
                faction.setMaxBid(0);
            } else {
                faction.setBid(String.valueOf(faction.getMaxBid()));
            }
            boolean allHaveBid = true;
            for (String factionName : getEligibleBidOrder(game)) {
                Faction f = game.getFaction(factionName);
                if (f.getBid().isEmpty()) {
                    allHaveBid = false;
                    currentBid = 0;
                    bidLeader = "";
                    break;
                }
                if (f.getMaxBid() > currentBid) {
                    currentBid = Integer.parseInt(f.getBid());
                    bidLeader = factionName;
                }
            }
            if (allHaveBid) {
                createBidMessage(game, false);
                richeseWinner(game, currentBid == 0);
            }
            return;
        }
        if (!currentBidder.equals(faction.getName())) return;
        boolean onceAroundFinished = false;
        do {
            if (!faction.isOutbidAlly() && faction.hasAlly() && faction.getAlly().equals(bidLeader)) {
                faction.setBid("pass (ally had top bid)");
            } else if (faction.getMaxBid() == -1) {
                faction.setBid("pass");
                faction.setMaxBid(0);
            } else if (faction.getMaxBid() <= currentBid) {
                if (!faction.isAutoBid()) return;
                faction.setBid("pass");
            } else {
                if (faction.isUseExactBid()) faction.setBid(String.valueOf(faction.getMaxBid()));
                else faction.setBid(String.valueOf(currentBid + 1));
                currentBid = Integer.parseInt(faction.getBid());
                bidLeader = faction.getName();
            }

            boolean tag = true;
            if (currentBidder.equals(eligibleBidOrder.getLast())) {
                if (isRicheseBidding()) onceAroundFinished = true;
                if (bidLeader.isEmpty()) allPlayersPassed = true;
                if (onceAroundFinished || allPlayersPassed) tag = false;
            }
            if (!silentAuction)
                topBidderDeclared = createBidMessage(game, tag);

            if (onceAroundFinished) {
                if (richeseWinner(game, allPlayersPassed)) {
                    return;
                }
            } else if (allPlayersPassed) {
                game.getBiddingPhase().publish("All players passed. " + Emojis.TREACHERY + " cards will be returned to the deck.");
                String modMessage = "Use /run advance to return the " + Emojis.TREACHERY + " cards to the deck";
                if (richeseCacheCardOutstanding)
                    modMessage += ". Then use /richese card-bid to auction the " + Emojis.RICHESE + " cache card.";
                else
                    modMessage += " and end the bidding phase.";
                game.getModInfo().publish(modMessage);
            } else if (topBidderDeclared) {
                game.getModInfo().publish("Use /award-top-bidder to assign card to the winner and pay appropriate recipient.\nUse /award-bid if a Karama affected winner or payment. " + game.getModOrRoleMention());
            }

            faction = game.getFaction(advanceBidder(game));
        } while (!topBidderDeclared && !allPlayersPassed && !onceAroundFinished);
    }
}
