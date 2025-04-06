package model;

import constants.Emojis;
import enums.GameOption;
import enums.UpdateType;
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
    private boolean cacheCardDecisionInProgress;
    private boolean richeseCacheCard;
    private boolean blackMarketDecisionInProgress;
    private boolean blackMarketCard;
    private boolean silentAuction;
    private boolean richeseCacheCardOutstanding;
    private boolean richeseCacheCardOccupierChoice;
    private final boolean gameHasIx;
    private boolean ixTechnologyUsed;
    private int ixTechnologyCardNumber;
    private boolean marketPopulated;
    private boolean marketShownToIx;
    private boolean ixRejectDecisionInProgress;
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

    public Bidding(Game game) {
        super();

        game.getTurnSummary().publish("**Turn " + game.getTurn() + " Bidding Phase**");
        game.setPhaseForWhispers("Turn " + game.getTurn() + " Bidding Phase\n");
        this.bidCardNumber = 0;
        this.numCardsForBid = 0;
        this.bidOrder = new ArrayList<>();
        this.bidCard = null;
        this.richeseBidOrder = null;
        this.cacheCardDecisionInProgress = false;
        this.richeseCacheCard = false;
        this.blackMarketDecisionInProgress = false;
        this.blackMarketCard = false;
        this.silentAuction = false;
        this.richeseCacheCardOccupierChoice = false;
        this.gameHasIx = game.hasIxFaction();
        this.ixTechnologyUsed = false;
        this.ixTechnologyCardNumber = 0;
        this.market = new LinkedList<>();
        this.marketPopulated = false;
        this.marketShownToIx = false;
        this.ixRejectDecisionInProgress = false;
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

        RicheseFaction richese = game.getRicheseFactionOrNull();
        if (richese != null) {
            this.richeseCacheCardOutstanding = !richese.getTreacheryCardCache().isEmpty();
            if (richese.getTreacheryHand().isEmpty()) {
                game.getModInfo().publish(Emojis.RICHESE + " has no cards for black market. Automatically advancing to regular bidding.");
            } else {
                askBlackMarket(game);
                game.getModInfo().publish(Emojis.RICHESE + " has been given buttons for black market.");
            }
        }
    }

    private void clearFactionBidInfo(Game game) {
        for (Faction faction : game.getFactions()) {
            faction.setMaxBid(0);
            faction.setBid("");
            if (bidCardNumber == 0)
                faction.setAutoBidTurn(false);
            faction.setAutoBid(faction.isAutoBidTurn());
        }
    }

    public void cardCountsInBiddingPhase(Game game) throws InvalidGameStateException {
        if (blackMarketDecisionInProgress)
            throw new InvalidGameStateException("Richese must decide on black market before advancing.");
        if (bidCard != null)
            throw new InvalidGameStateException("The black market card must be awarded before advancing.");

        StringBuilder message = new StringBuilder();
        message.append(MessageFormat.format(
                "{0} Number of Treachery Cards {0}\n",
                Emojis.TREACHERY
        ));
        message.append(
                game.getFactions().stream().map(
                        f -> MessageFormat.format(
                                "{0}: {1}\n", f.getEmoji(), f.getTreacheryHand().size()
                        )
                ).collect(Collectors.joining())
        );
        int numCardsForBid = populateMarket(game);
        message.append(
                MessageFormat.format(
                        "{0} cards will be pulled from the {1} deck for bidding.",
                        numCardsForBid, Emojis.TREACHERY
                )
        );
        if (gameHasIx) {
            message.append(
                    MessageFormat.format(
                            "\n{0} will send one of them back to the deck.",
                            Emojis.IX
                    )
            );
            game.getModInfo().publish("If anyone wants to Karama " + Emojis.IX + " bidding advantage, that must be done now.");
        }
        game.getTurnSummary().publish(message.toString());
        if (numCardsForBid == 0) {
            game.getModInfo().publish("All hands are full. If a player discards now, execute '/run bidding' again. Otherwise, '/run advance' to end bidding.");
        } else if (richeseCacheCardOutstanding) {
            presentCacheCardChoices(game);
            game.getModInfo().publish(Emojis.RICHESE + " has been given buttons for selling their cache card.");
        } else {
            game.getModInfo().publish("Start running commands to bid and then advance when all the bidding is done.");
        }
    }

    private void presentCardToRejectMessage(Game game) throws InvalidGameStateException {
        StringBuilder message = new StringBuilder();
        IxFaction ixFaction = game.getIxFaction();
        message.append(
                MessageFormat.format(
                        "Turn {0} - Select one of the following {1} cards to send back to the deck. {2}",
                        game.getTurn(), Emojis.TREACHERY, ixFaction.getPlayer()
                )
        );
        for (TreacheryCard card : market) {
            message.append(
                    MessageFormat.format("\n\t**{0}** _{1}_",
                            card.name(), card.type()
                    ));
        }
        ixFaction.getChat().publish(message.toString());
        presentCardToRejectChoices(game);
    }

    public void presentCardToRejectChoices(Game game) throws InvalidGameStateException {
        if (!ixRejectOutstanding)
            throw new InvalidGameStateException("You have already sent a card back.");
        List<DuneChoice> choices = new ArrayList<>();
        int i = 0;
        for (TreacheryCard card : market)
            choices.add(new DuneChoice("ix-card-to-reject-" + game.getTurn() + "-" + i++ + "-" + card.name(), card.name()));
        game.getIxFaction().getChat().reply("", choices);
        ixRejectDecisionInProgress = true;
    }

    public void askBlackMarket(Game game) {
        blackMarketDecisionInProgress = true;
        RicheseFaction richeseFaction = game.getRicheseFaction();
        String message2 = "Select a " + Emojis.TREACHERY + " card to sell on the black market. " + richeseFaction.getPlayer();
        List<DuneChoice> choices = new ArrayList<>();
        int i = 0;
        for (TreacheryCard card : richeseFaction.getTreacheryHand()) {
            i++;
            choices.add(new DuneChoice("richese-run-black-market-" + card.name() + "-" + i, card.name()));
        }
        choices.add(new DuneChoice("danger", "richese-run-black-market-skip", "No black market"));
        richeseFaction.getChat().publish(message2, choices);
    }

    private void runRicheseBid(Game game, String bidType, boolean blackMarket) throws InvalidGameStateException {
        clearFactionBidInfo(game);
        if (bidType.equalsIgnoreCase("Silent")) {
            silentAuction = true;
            if (blackMarket) {
                game.getBiddingPhase().publish(
                        MessageFormat.format(
                                "{0} We will now silently auction a card from {1} hand on the black market! Please use the bot to place your bid.",
                                game.getGameRoleMention(), Emojis.RICHESE
                        )
                );
            } else {
                game.getBiddingPhase().publish(
                        MessageFormat.format(
                                "{0} We will now silently auction a brand new Richese {1} {2} {1}!  Please use the bot to place your bid.",
                                game.getGameRoleMention(), Emojis.TREACHERY, bidCard.name()
                        )
                );
            }
            List<Faction> factions = game.getFactions();
            for (Faction faction : factions) {
                if (faction.getHandLimit() > faction.getTreacheryHand().size()) {
                    faction.getChat().publish(
                            MessageFormat.format(
                                    "{0} Use the bot to place your bid for the silent auction. Your bid will be the exact amount you set.",
                                    faction.getPlayer()
                            )
                    );
                }
            }
            int firstBid = Math.ceilDiv(game.getStorm(), 3) % factions.size();
            List<Faction> bidOrderFactions = new ArrayList<>();
            bidOrderFactions.addAll(factions.subList(firstBid, factions.size()));
            bidOrderFactions.addAll(factions.subList(0, firstBid));
            List<String> bidOrder = bidOrderFactions.stream().map(Faction::getName).collect(Collectors.toList());
            setRicheseBidOrder(game, bidOrder);
            List<String> filteredBidOrder = getEligibleBidOrder(game);
            Faction factionBeforeFirstToBid = game.getFaction(filteredBidOrder.getLast());
            currentBidder = factionBeforeFirstToBid.getName();
        } else {
            StringBuilder message = new StringBuilder();
            if (blackMarket) {
                message.append(
                        MessageFormat.format("{0} You may now place your bids for a black market card from {1} hand!\n",
                                game.getGameRoleMention(), Emojis.RICHESE
                        )
                );
            } else {
                message.append(
                        MessageFormat.format("{0} You may now place your bids for a shiny, brand new {1} {2}!\n",
                                game.getGameRoleMention(), Emojis.RICHESE, bidCard.name()
                        )
                );
            }

            List<Faction> factions = game.getFactions();
            List<Faction> bidOrderFactions = new ArrayList<>();
            List<Faction> factionsInBidDirection;
            if (bidType.equalsIgnoreCase("OnceAroundCW")) {
                factionsInBidDirection = new ArrayList<>(factions);
                Collections.reverse(factionsInBidDirection);
            } else {
                factionsInBidDirection = factions;
            }

            int richeseIndex = factionsInBidDirection.indexOf(game.getRicheseFaction());
            bidOrderFactions.addAll(factionsInBidDirection.subList(richeseIndex + 1, factions.size()));
            bidOrderFactions.addAll(factionsInBidDirection.subList(0, richeseIndex + 1));
            List<String> bidOrder = bidOrderFactions.stream().map(Faction::getName).collect(Collectors.toList());
            setRicheseBidOrder(game, bidOrder);
            List<String> filteredBidOrder = getEligibleBidOrder(game);
            Faction factionBeforeFirstToBid = game.getFaction(filteredBidOrder.getLast());
            currentBidder = factionBeforeFirstToBid.getName();
            game.getBiddingPhase().publish(message.toString());
            createBidMessage(game, true);
            advanceBidder(game);
            tryBid(game, game.getFaction(currentBidder));
        }
    }

    public void richeseCardAuction(Game game, String cardName, String bidType) throws InvalidGameStateException {
        if (bidCard != null) {
            throw new InvalidGameStateException("There is already a card up for bid.");
        } else if (!richeseCacheCardOutstanding) {
            if (bidCardNumber != 0 && bidCardNumber == numCardsForBid) {
                throw new InvalidGameStateException("All cards for this round have already been bid on.");
            } else {
                throw new InvalidGameStateException(Emojis.RICHESE + " card is not eligible to be sold.");
            }
        }

        RicheseFaction faction = game.getRicheseFaction();
        cacheCardDecisionInProgress = false;
        richeseCacheCard = true;
        faction.getChat().reply("Selling " + cardName + " by " + bidType + " auction.");
        setBidCard(game,
                faction.removeTreacheryCardFromCache(
                        faction.getTreacheryCardFromCache(cardName)
                )
        );
        bidCardNumber++;
        runRicheseBid(game, bidType, false);
        game.getFactions().forEach(f -> f.setUpdated(UpdateType.MISC_BACK_OF_SHIELD));
        if (bidType.equals("Silent"))
            game.getModInfo().publish("Players should use the bot to enter their bids for the silent auction.");
    }

    public void richeseCardFirstByOccupier(Game game) {
        game.getRicheseFaction().getChat().reply("You will sell first.");
        richeseCacheCardOccupierChoice = true;
        presentCacheCardChoices(game);
    }

    public void richeseCardLast(Game game) {
        game.getRicheseFaction().getChat().reply("You will sell last.");
        game.getModInfo().publish(Emojis.RICHESE + " will be given buttons when it is time for the last card.");
        cacheCardDecisionInProgress = false;
    }

    public void removeRicheseCacheCardFromGame(Game game) throws InvalidGameStateException {
        if (bidCard == null)
            throw new InvalidGameStateException("There is no card up for bid.");
        else if (!richeseCacheCard)
            throw new InvalidGameStateException("The card up for bid did not come from the Richese cache.");

        game.getTurnSummary().publish(MessageFormat.format(
                "{0} {1} has been removed from the game.",
                Emojis.RICHESE, bidCard.name()));
        clearBidCardInfo(null);
    }

    public void blackMarketAuction(Game game, String cardName, String bidType) throws InvalidGameStateException {
        if (bidCard != null) {
            throw new InvalidGameStateException("There is already a card up for bid.");
        } else if (bidCardNumber != 0) {
            throw new InvalidGameStateException("Black market card must be first in the bidding round.");
        }

        blackMarketDecisionInProgress = false;
        Faction faction = game.getRicheseFaction();
        List<TreacheryCard> cards = faction.getTreacheryHand();

        TreacheryCard card = cards.stream()
                .filter(c -> c.name().equalsIgnoreCase(cardName))
                .findFirst()
                .orElseThrow();

        cards.remove(card);
        blackMarketCard = true;
        setBidCard(game, card);
        bidCardNumber++;

        if (bidType.equalsIgnoreCase("Normal")) {
            updateBidOrder(game);
            List<String> bidOrder = getEligibleBidOrder(game);
            for (Faction f : game.getFactions()) {
                f.setMaxBid(0);
                f.setAutoBid(false);
                f.setBid("");
            }
            Faction factionBeforeFirstToBid = game.getFaction(bidOrder.getLast());
            currentBidder = factionBeforeFirstToBid.getName();
            createBidMessage(game, true);
            advanceBidder(game);
            tryBid(game, game.getFaction(currentBidder));
        } else {
            runRicheseBid(game, bidType, true);
        }
        sendAtreidesCardPrescience(game, card, false);
        game.getFactions().forEach(f -> f.setUpdated(UpdateType.MISC_BACK_OF_SHIELD));
    }

    public void auctionNextCard(Game game, boolean prescienceBlocked) throws InvalidGameStateException {
        if (bidCard != null) {
            throw new InvalidGameStateException("There is already a card up for bid.");
        } else if (cacheCardDecisionInProgress) {
            throw new InvalidGameStateException("Richese must decide on their cache card.");
        } else if (numCardsForBid == 0) {
            throw new InvalidGameStateException("Use /run advance.");
        } else if (bidCardNumber != 0 && bidCardNumber == numCardsForBid) {
            throw new InvalidGameStateException("All cards for this round have already been bid on.");
        } else if (ixRejectOutstanding) {
            throw new InvalidGameStateException(Emojis.IX + " must send a " + Emojis.TREACHERY + " card back to the deck.");
        }

        DuneTopic turnSummary = game.getTurnSummary();
        if (!marketShownToIx && gameHasIx) {
            if (treacheryDeckReshuffled) {
                turnSummary.publish(MessageFormat.format(
                        "There were only {0} left in the {1} deck. The {1} deck has been replenished from the discard pile.",
                        numCardsFromOldDeck, Emojis.TREACHERY
                ));
                treacheryDeckReshuffled = false;
            }
            String message = MessageFormat.format(
                    "{0} {1} cards have been shown to {2}",
                    market.size(), Emojis.TREACHERY, Emojis.IX
            );
            ixRejectOutstanding = true;
            presentCardToRejectMessage(game);
            marketShownToIx = true;
            turnSummary.publish(message);
        } else {
            updateBidOrder(game);
            List<String> bidOrder = getEligibleBidOrder(game);
            if (bidOrder.isEmpty())
                throw new InvalidGameStateException("All hands are full. If a player discards now, execute '/run bidding' again. Otherwise, '/run advance' to end bidding.");

            if (treacheryDeckReshuffled) {
                turnSummary.publish(MessageFormat.format(
                        "There were only {0} left in the {1} deck. The {1} deck has been replenished from the discard pile.",
                        numCardsFromOldDeck, Emojis.TREACHERY
                ));
                treacheryDeckReshuffled = false;
            }
            TreacheryCard bidCard = nextBidCard(game);
            sendAtreidesCardPrescience(game, bidCard, prescienceBlocked);
            Faction factionBeforeFirstToBid = game.getFaction(bidOrder.getLast());
            currentBidder = factionBeforeFirstToBid.getName();
            String newCardAnnouncement = MessageFormat.format("{0} You may now place your bids for R{1}:C{2}.",
                    game.getGameRoleMention(), game.getTurn(), bidCardNumber);
            if (isCardFromIxHand())
                newCardAnnouncement += "\nThis card is from " + Emojis.IX + " hand after they used technology.";
            game.getBiddingPhase().publish(newCardAnnouncement);
            createBidMessage(game, true);
            advanceBidder(game);
            tryBid(game, game.getFaction(currentBidder));
        }
    }

    private TreacheryCard nextBidCard(Game game) throws InvalidGameStateException {
        treacheryDeckReshuffled = false;
        numCardsFromOldDeck = 0;
        if (bidCardNumber != 0 && bidCardNumber == numCardsForBid) {
            throw new InvalidGameStateException("All cards for this round have already been bid on.");
        }
        if (market.isEmpty()) {
            throw new InvalidGameStateException("All cards from the bidding market have already been bid on.");
        }

        clearFactionBidInfo(game);
        bidCard = market.removeFirst();
        bidCardNumber++;
        cardFromMarket = true;
        game.getFactions().forEach(f -> f.setUpdated(UpdateType.MISC_BACK_OF_SHIELD));
        return bidCard;
    }

    protected int populateMarket(Game game) throws InvalidGameStateException {
        if (marketPopulated) {
            throw new InvalidGameStateException("Bidding market is already populated.");
        }
        List<Faction> factions = game.getFactions();
        numCardsForBid = factions.stream()
                .filter(f -> f.getHandLimit() > f.getTreacheryHand().size())
                .toList().size();
        int numCardsInMarket = numCardsForBid - bidCardNumber;
        if (richeseCacheCardOutstanding)
            numCardsInMarket--;
        if (gameHasIx)
            numCardsInMarket++;
        for (int i = 0; i < numCardsInMarket; i++)
            addCardToMarket(game, i);
        marketPopulated = true;
        return numCardsInMarket;
    }

    public void addCardToMarket(Game game, int i) {
        LinkedList<TreacheryCard> treacheryDeck = game.getTreacheryDeck();
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

    public void addMissedCardToMarket(Game game) {
        game.getTurnSummary().publish("A " + Emojis.TREACHERY + " card is being added to the bidding market.");
        addCardToMarket(game, 0);
        if (treacheryDeckReshuffled) {
            numCardsFromOldDeck = numCardsForBid;
            game.getTurnSummary().publish(MessageFormat.format(
                    "There were only {0} left in the {1} deck when bidding started. The {1} deck has been replenished from the discard pile.",
                    numCardsFromOldDeck, Emojis.TREACHERY
            ));
            treacheryDeckReshuffled = false;
        }
        numCardsForBid++;
        game.getTurnSummary().publish("The bidding market now has " + market.size() + " " + Emojis.TREACHERY + " cards remaining for auction. There are " + numCardsForBid + " total cards this turn.");
    }

    private void sendAtreidesCardPrescience(Game game, TreacheryCard card, boolean prescienceBlocked) {
        if (game.hasFaction("Atreides")) {
            String cardInfo = MessageFormat.format("{0} {1} {0} is up for bid (R{2}:C{3}).", Emojis.TREACHERY, card.name(), game.getTurn(), bidCardNumber);
            AtreidesFaction atreides = (AtreidesFaction) game.getFaction("Atreides");
            if (prescienceBlocked || atreides.isCardPrescienceBlocked()) {
                atreides.getChat().publish("Your " + Emojis.TREACHERY + " Prescience was blocked by Karama.");
                if (atreides.hasAlly() && atreides.isGrantingAllyTreacheryPrescience())
                    atreides.getAllianceThread().publish(atreides.getEmoji() + " " + Emojis.TREACHERY + " Prescience was blocked by Karama.");
                if (atreides.isHomeworldOccupied())
                    atreides.getOccupier().getChat().publish("Your " + Emojis.ATREIDES + " subjects in Caladan " + Emojis.TREACHERY + " Prescience was blocked by Karama.");
                atreides.setCardPrescienceBlocked(false);
            } else {
                atreides.getChat().publish("You predict " + cardInfo);
                if (atreides.hasAlly() && atreides.isGrantingAllyTreacheryPrescience())
                    atreides.getAllianceThread().publish(atreides.getEmoji() + " predicts " + cardInfo);
                if (atreides.isHomeworldOccupied())
                    atreides.getOccupier().getChat().publish("Your " + Emojis.ATREIDES + " subjects in Caladan predict " + cardInfo);
            }
        }
    }

    private int moveMarketToDeck(Game game) {
        int numCardsReturned = market.size();
        Iterator<TreacheryCard> marketIterator = market.descendingIterator();
        while (marketIterator.hasNext()) game.getTreacheryDeck().add(marketIterator.next());
        market.clear();
        if (bidCard != null) {
            game.getTreacheryDeck().add(bidCard);
            numCardsReturned++;
        }
        clearBidCardInfo(null);
        numCardsForBid -= numCardsReturned;
        return numCardsReturned;
    }

    public void putBackIxCard(Game game, String cardName, String location, boolean requestTechnology) throws InvalidGameStateException {
        ixRejectDecisionInProgress = false;
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
        game.getTurnSummary().publish(Emojis.IX + " sent a " + Emojis.TREACHERY + " to the " + location.toLowerCase() + " of the deck.");
        if (requestTechnology)
            game.getIxFaction().getChat().publish(Emojis.IX + " would like to use Technology on the first card. " + game.getModOrRoleMention());
        else
            auctionNextCard(game, false);
    }

    public void ixTechnology(Game game, String cardName) throws InvalidGameStateException {
        IxFaction faction = game.getIxFaction();
        if (ixTechnologyUsed) {
            throw new InvalidGameStateException("Ix has already used technology this turn.");
        } else if (bidCard != null) {
            throw new InvalidGameStateException("There is already a card up for bid.");
        } else if (numCardsForBid - bidCardNumber - (richeseCacheCardOutstanding ? 1 : 0) < market.size()) {
            throw new InvalidGameStateException("Ix must send a card back to the deck.");
        } else if (market.isEmpty()) {
            throw new InvalidGameStateException("There are no cards in the bidding market.");
        }
        TreacheryCard cardFromIx = faction.removeTreacheryCardWithoutDiscard(cardName);
        TreacheryCard newCard = market.removeFirst();
        faction.addTreacheryCard(newCard);
        market.addFirst(cardFromIx);
        ixTechnologyUsed = true;
        ixTechnologyCardNumber = bidCardNumber + 1;
        faction.getLedger().publish(
                MessageFormat.format("Received {0} and put {1} back as the next card for bid.",
                        newCard.name(), cardName)
        );
        game.getTurnSummary().publish(MessageFormat.format(
                "{0} used technology to swap a card from their hand for R{1}:C{2}.",
                Emojis.IX, game.getTurn(), game.getBidding().getBidCardNumber() + 1
        ));
        faction.getChat().publish("You took " + newCard.name() + " instead of " + cardName);
    }

    public void ixAllyCardSwap(Game game) throws InvalidGameStateException {
        TreacheryCard cardToDiscard = previousCard;
        IxFaction faction = game.getIxFaction();
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
            throw new InvalidGameStateException(faction.getEmoji() + " ally " + ally.getEmoji() + " did not win the previous card.");
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
        TreacheryCard cardToSwap = ally.removeTreacheryCardWithoutDiscard(previousCard);
        TreacheryCard newCard = treacheryDeck.pollLast();
        if (newCard == null)
            throw new InvalidGameStateException("There is no card in the treachery deck to swap for.");
        ally.addTreacheryCard(newCard);
        game.getTreacheryDiscard().add(cardToSwap);
        ixAllySwapped = true;
        ally.getLedger().publish(
                MessageFormat.format("Received {0} from the {1} deck and discarded {2} with {3} ally power.",
                        newCard.name(), Emojis.TREACHERY, cardToDiscard.name(), Emojis.IX)
        );
        DuneTopic turnSummary = game.getTurnSummary();
        turnSummary.publish(MessageFormat.format(
                "{0} as {1} ally discarded {2} and took the {3} deck top card.",
                ally.getEmoji(), Emojis.IX, cardToDiscard.name(), Emojis.TREACHERY
        ));
        if (treacheryDeckReshuffled) {
            turnSummary.publish(MessageFormat.format(
                    "There were no cards left in the {0} deck. The {0} deck has been replenished from the discard pile.",
                    Emojis.TREACHERY
            ));
            treacheryDeckReshuffled = false;
        }
    }

    public TreacheryCard getBidCard() {
        return bidCard;
    }

    private void setBidCard(Game game, TreacheryCard bidCard) {
        clearFactionBidInfo(game);
        this.bidCard = bidCard;
    }

    public boolean isCacheCardDecisionInProgress() {
        return cacheCardDecisionInProgress;
    }

    public void setCacheCardDecisionInProgress(boolean cacheCardDecisionInProgress) {
        this.cacheCardDecisionInProgress = cacheCardDecisionInProgress;
    }

    public boolean isBlackMarketDecisionInProgress() {
        return blackMarketDecisionInProgress;
    }

    public void setBlackMarketDecisionInProgress(boolean blackMarketDecisionInProgress) {
        this.blackMarketDecisionInProgress = blackMarketDecisionInProgress;
    }

    public boolean isBlackMarketCard() {
        return blackMarketCard;
    }

    public boolean isSilentAuction() {
        return silentAuction;
    }

    protected boolean isRicheseCacheCardOutstanding() {
        return richeseCacheCardOutstanding;
    }

    public void setRicheseCacheCardOutstanding(boolean richeseCacheCardOutstanding) {
        this.richeseCacheCardOutstanding = richeseCacheCardOutstanding;
    }

    private boolean isRicheseBidding() {
        return richeseBidOrder != null;
    }

    private boolean isCardFromIxHand() {
        return bidCardNumber == ixTechnologyCardNumber;
    }

    public int getBidCardNumber() {
        return bidCardNumber;
    }

    public int getCurrentBid() {
        return currentBid;
    }

    public int getNumCardsForBid() {
        return numCardsForBid;
    }

    public void decrementBidCardNumber() {
        bidCardNumber--;
    }

    private List<String> getBidOrder() {
        if (richeseBidOrder == null) return bidOrder;
        return richeseBidOrder;
    }

    private void setRicheseBidOrder(Game game, List<String> bidOrder) {
        this.richeseBidOrder = bidOrder;
        nextBidder = getEligibleBidOrder(game).getFirst();
    }

    protected List<String> getEligibleBidOrder(Game game) {
        return getBidOrder()
                .stream()
                .filter(f -> game.getFaction(f).getHandLimit() > game.getFaction(f).getTreacheryHand().size())
                .collect(Collectors.toList());
    }

    protected void updateBidOrder(Game game) {
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

    protected void clearBidCardInfo(String winner) {
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

    protected boolean isMarketShownToIx() {
        return marketShownToIx;
    }

    public void blockIxBiddingAdvantage(Game game) throws InvalidGameStateException {
        this.marketShownToIx = true;
        TreacheryCard card = market.pollLast();
        if (card == null)
            throw new InvalidGameStateException("There are no cards in the bidding market.");
        game.getTreacheryDeck().add(card);
        game.getTurnSummary().publish(
                MessageFormat.format(
                        "{0} have been blocked from using their bidding advantage.\n{1} cards will be pulled from the {2} deck for bidding.",
                        Emojis.IX, market.size(), Emojis.TREACHERY
                )
        );
    }

    public boolean isIxRejectOutstanding() {
        return ixRejectOutstanding;
    }

    protected String getCurrentBidder() {
        return currentBidder;
    }

    private String getNextBidder(Game game) {
        Faction currentFaction = game.getFaction(currentBidder);
        int currentIndex = getEligibleBidOrder(game).indexOf(currentFaction.getName());
        int nextIndex = 0;
        if (currentIndex != getEligibleBidOrder(game).size() - 1) nextIndex = currentIndex + 1;
        nextBidder = getEligibleBidOrder(game).get(nextIndex);
        return nextBidder;
    }

    private String advanceBidder(Game game) {
        currentBidder = getNextBidder(game);
        return currentBidder;
    }

    private boolean createBidMessage(Game game, boolean tag) {
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
        } else if (blackMarketCard)
            message.append(" (Black Market, Normal bidding)");
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

    public boolean presentCacheCardChoices(Game game) {
        if (game.getFactions().stream().noneMatch(f -> f.getTreacheryHand().size() < f.getHandLimit())) {
            cacheCardDecisionInProgress = false;
            richeseCacheCardOutstanding = false;
            game.getModInfo().publish("If anyone discards now, use /richese card-bid to auction the " + Emojis.RICHESE + " cache card. Otherwise, use /run advance to end the bidding phase.");
            return false;
        }
        cacheCardDecisionInProgress = true;
        RicheseFaction richeseFaction = game.getRicheseFaction();
        Faction factionToGetMessage = richeseFaction;
        List<DuneChoice> choices = richeseFaction.getTreacheryCardCache().stream().map(card -> new DuneChoice("richese-cache-card-" + card.name(), card.name())).collect(Collectors.toList());
        String message;
        if (richeseCacheCardOccupierChoice) {
            factionToGetMessage = richeseFaction.getOccupier();
            message = "Please select the " + Emojis.RICHESE + " cache card to be sold. " + factionToGetMessage.getPlayer();
        } else if (richeseFaction.isHomeworldOccupied()) {
            message = "Would you like to sell your cache card first or last? " + richeseFaction.getPlayer();
            message += "\n" + richeseFaction.getOccupier().getEmoji() + " occupies your homeworld and will choose the card.";
            choices.clear();
            choices.add(new DuneChoice("richese-cache-sell-first-occupier", "Sell cache card first"));
            choices.add(new DuneChoice("richese-cache-sell-last", "Sell cache card last"));
            richeseCacheCardOccupierChoice = true;
        } else if (bidCardNumber < numCardsForBid - 1) {
            message = "Please select your cache card to sell or choose to sell last. " + richeseFaction.getPlayer();
            choices.add(new DuneChoice("danger", "richese-cache-sell-last", "Sell cache card last"));
        } else {
            message = "Please select your cache card to be sold. You must sell now. " + richeseFaction.getPlayer();
        }
        factionToGetMessage.getChat().reply(message, choices);
        return true;
    }

    public void presentCacheCardMethodChoices(Game game, String cardName) {
        RicheseFaction richese = game.getRicheseFaction();
        if (richese.isHomeworldOccupied())
            richese.getOccupier().getChat().reply("You selected " + cardName + ". " + Emojis.RICHESE + " chooses the bidding method.");
        List<DuneChoice> choices = new ArrayList<>();
        choices.add(new DuneChoice("richese-cache-card-method-" + cardName + "-" + "OnceAroundCCW", "OnceAroundCCW"));
        choices.add(new DuneChoice("richese-cache-card-method-" + cardName + "-" + "OnceAroundCW", "OnceAroundCW"));
        choices.add(new DuneChoice("richese-cache-card-method-" + cardName + "-" + "Silent", "Silent"));
        if (!richese.isHomeworldOccupied())
            choices.add(new DuneChoice("secondary", "richese-cache-card-method-reselect", "Start over"));
        richese.getChat().reply("How would you like to sell " + cardName + "?", choices);
    }

    public void presentCacheCardConfirmChoices(Game game, String cardName, String method) {
        RicheseFaction richese = game.getRicheseFaction();
        List<DuneChoice> choices = new ArrayList<>();
        choices.add(new DuneChoice("success", "richese-cache-card-confirm-" + cardName + "-" + method, "Confirm " + cardName + " by " + method + " auction"));
        String startOverLabel = "Start over";
        String startOverID = "richese-cache-card-confirm-reselect";
        if (richese.isHomeworldOccupied()) {
            startOverLabel = "Reselect bid type";
            startOverID = "richese-cache-card-" + cardName;
        }
        choices.add(new DuneChoice("secondary", startOverID, startOverLabel));
        richese.getChat().reply("", choices);
    }

    public void assignAndPayForCard(Game game, String winnerName, String paidToFactionName, int spentValue, boolean harkBonusBlocked) throws InvalidGameStateException {
        if (bidCard == null) {
            throw new InvalidGameStateException("There is no card up for bid.");
        }
        Faction winner = game.getFaction(winnerName);
        List<TreacheryCard> winnerHand = winner.getTreacheryHand();
        String currentCard = MessageFormat.format(
                "R{0}:C{1}",
                game.getTurn(),
                bidCardNumber
        );
        winner.payForCard(currentCard, spentValue);

        DuneTopic turnSummary = game.getTurnSummary();
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
        if (winner instanceof HarkonnenFaction harkonnen) {
            if (harkBonusBlocked || harkonnen.isBonusCardBlocked()) {
                harkonnen.setBonusCardBlocked(false);
                turnSummary.publish(Emojis.HARKONNEN + " bonus card was blocked by Karama.");
            } else {
                if (winnerHand.size() < winner.getHandLimit() && !winner.isHomeworldOccupied()) {
                    if (game.drawTreacheryCard("Harkonnen", false, false))
                        turnSummary.publish(MessageFormat.format("The {0} deck was empty and has been replenished from the discard pile.", Emojis.TREACHERY));
                    turnSummary.publish(MessageFormat.format("{0} draws another card from the {1} deck.", winner.getEmoji(), Emojis.TREACHERY));
                    winner.getLedger().publish("Received " + winner.getLastTreacheryCard().name() + " as an extra card. (" + currentCard + ")");
                } else if (winner.isHomeworldOccupied() && winner.getOccupier().hasAlly()) {
                    game.getModInfo().publish("Harkonnen occupier or ally may draw one from the deck (you must do this for them).");
                    game.getTurnSummary().publish("Giedi Prime is occupied by " + winner.getOccupier().getName() + ", they or their ally may draw an additional card from the deck.");
                } else if (winner.isHomeworldOccupied() && winner.getOccupier().getTreacheryHand().size() < winner.getOccupier().getHandLimit()) {
                    game.drawTreacheryCard(winner.getOccupier().getName(), true, false);
                    turnSummary.publish(MessageFormat.format(
                            "Giedi Prime is occupied, {0} draws another card from the {1} deck instead of {2}.",
                            winner.getEmoji(), Emojis.TREACHERY, Emojis.HARKONNEN
                    ));
                }
            }
        }

        if (market.isEmpty() && bidCardNumber == numCardsForBid - 1 && richeseCacheCardOutstanding) {
            if (presentCacheCardChoices(game))
                game.getModInfo().publish(Emojis.RICHESE + " has been asked to select the last card of the turn.");
        }
    }

    public void awardTopBidder(Game game) throws InvalidGameStateException {
        awardTopBidder(game, false);
    }
    public void awardTopBidder(Game game, boolean harkBonusBlocked) throws InvalidGameStateException {
        String winnerName = bidLeader;
        if (winnerName.isEmpty()) {
            if (richeseCacheCard || blackMarketCard)
                assignAndPayForCard(game, "Richese", "", 0, harkBonusBlocked);
            else
                throw new InvalidGameStateException("There is no top bidder for this card.");
        } else {
            String paidToFactionName = "Bank";
            if ((richeseCacheCard || blackMarketCard) && !winnerName.equals("Richese"))
                paidToFactionName = "Richese";
            else if (!winnerName.equals("Emperor"))
                paidToFactionName = "Emperor";
            int spentValue = currentBid;
            assignAndPayForCard(game, winnerName, paidToFactionName, spentValue, harkBonusBlocked);
        }
    }

    private boolean richeseWinner(Game game, boolean allPlayersPassed) throws InvalidGameStateException {
        DuneTopic modInfo = game.getModInfo();
        DuneTopic biddingPhase = game.getBiddingPhase();
        if (allPlayersPassed) {
            if (richeseCacheCard) {
                topBidderDeclared = true;
                biddingPhase.publish("All players passed." + Emojis.RICHESE + " may take cache card for free or remove it from the game.");
                modInfo.publish("Use /award-top-bidder to assign card back to " + Emojis.RICHESE + ". Use /richese remove-card to remove it from the game. " + game.getModOrRoleMention());
            } else {
                decrementBidCardNumber();
                biddingPhase.publish("All players passed. The black market card has been returned to " + Emojis.RICHESE);
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

    public String pass(Game game, Faction faction) throws InvalidGameStateException {
        if (topBidderDeclared || allPlayersPassed)
            throw new InvalidGameStateException("Bidding has ended on the current card.\nset-auto-pass-entire-turn is the only valid bidding command until the next card is auctions.");
        faction.setMaxBid(-1);
        game.getModLedger().publish(faction.getEmoji() + " passed their bid.");
        tryBid(game, faction);
        if (faction.isAutoBid() && !game.getBidding().isSilentAuction())
            return "You will auto-pass until the next card or until you set auto-pass to false.";
        return "You will pass one time.";
    }

    public String setAutoPass(Game game, Faction faction, boolean enabled) throws InvalidGameStateException {
        if (topBidderDeclared || allPlayersPassed)
            throw new InvalidGameStateException("Bidding has ended on the current card.\nset-auto-pass-entire-turn is the only valid bidding command until the next card is auctions.");
        faction.setAutoBid(enabled);
        game.getModLedger().publish(faction.getEmoji() + " set auto-pass to " + enabled);
        tryBid(game, faction);
        String responseMessage = "You set auto-pass to " + enabled + ".";
        if (enabled) {
            responseMessage += "\nYou will auto-pass if the top bid is " + faction.getMaxBid() + " or higher.";
        }
        return responseMessage;
    }

    public String setAutoPassEntireTurn(Game game, Faction faction, boolean enabled) throws InvalidGameStateException {
        faction.setAutoBidTurn(enabled);
        game.getModLedger().publish(faction.getEmoji() + " set auto-pass-entire-turn to " + enabled);
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

    public String bid(Game game, Faction faction, boolean useExact, int bidAmount, Boolean newOutbidAllySetting, Boolean enableAutoPass) throws InvalidGameStateException {
        if (topBidderDeclared || allPlayersPassed)
            throw new InvalidGameStateException("Bidding has ended on the current card.\nset-auto-pass-entire-turn is the only valid bidding command until the next card is auctions.");
        String response = faction.bid(game, useExact, bidAmount, newOutbidAllySetting, enableAutoPass);
        game.getBidding().tryBid(game, faction);
        return response;
    }

    private void tryBid(Game game, Faction faction) throws InvalidGameStateException {
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
                if (blackMarketCard) {
                    richeseWinner(game, true);
                    return;
                }
                game.getBiddingPhase().publish("All players passed. " + Emojis.TREACHERY + " cards will be returned to the deck.");
                String modMessage = "Use /run advance to return the " + Emojis.TREACHERY + " cards to the deck";
                if (richeseCacheCardOutstanding)
                    modMessage += ". Then use /richese card-bid to auction the " + Emojis.RICHESE + " cache card.";
                else
                    modMessage += " and end the bidding phase.";
                modMessage += " " + game.getModOrRoleMention();
                game.getModInfo().publish(modMessage);
            } else if (topBidderDeclared) {
                game.getModInfo().publish("Use /award-top-bidder to assign card to the winner and pay appropriate recipient.\nUse /award-bid if a Karama affected winner or payment. " + game.getModOrRoleMention());
            }

            faction = game.getFaction(advanceBidder(game));
        } while (!topBidderDeclared && !allPlayersPassed && !onceAroundFinished);
    }

    public boolean finishBiddingPhase(Game game) throws InvalidGameStateException {
        if (bidCard == null && !market.isEmpty() && !getEligibleBidOrder(game).isEmpty()) {
            throw new InvalidGameStateException("Use /run bidding to auction the next card.");
        } else if (!marketShownToIx && gameHasIx) {
            throw new InvalidGameStateException("Use /run bidding to show bidding cards to Ix");
        } else if (cacheCardDecisionInProgress) {
            throw new InvalidGameStateException("Richese must decide on their cache card.");
        } else if (ixRejectDecisionInProgress) {
            throw new InvalidGameStateException("Ix must send a card back to the deck.");
        } else if (bidCard == null && richeseCacheCardOutstanding && !getEligibleBidOrder(game).isEmpty()) {
            throw new InvalidGameStateException(Emojis.RICHESE + " cache card must be completed before ending bidding.");
        } else if (bidCard != null && !cardFromMarket) {
            throw new InvalidGameStateException("Card up for bid is not from bidding market.");
        }

        if (bidCard != null) {
            int numCardsReturned = moveMarketToDeck(game);
            game.getTurnSummary().publish("All players passed. " + numCardsReturned + " cards were returned to top of the Treachery Deck.");
            if (richeseCacheCardOutstanding) {
                presentCacheCardChoices(game);
                game.getModInfo().publish(Emojis.RICHESE + " has been asked to select the last card of the turn.");
            }
        } else if (getEligibleBidOrder(game).isEmpty()) {
            if (richeseCacheCardOutstanding) {
                game.getTurnSummary().publish("All hands are full. " + Emojis.RICHESE + " may not auction a card from their cache.");
            } else {
                int numCardsReturned = moveMarketToDeck(game);
                game.getTurnSummary().publish("All hands are full. " + numCardsReturned + " cards were returned to top of the Treachery Deck.");
            }
        }

        if (richeseCacheCardOutstanding) {
            game.getModInfo().publish("Auction the " + Emojis.RICHESE + " cache card. Then /run advance again to end bidding.");
            return false;
        }

        if (game.hasFaction("Emperor"))
            ((EmperorFaction) game.getFaction("Emperor")).presentKaitainHighThresholdChoices();

        game.endBidding();
        game.getFactions().forEach(f -> f.setUpdated(UpdateType.MISC_BACK_OF_SHIELD));
        return true;
    }
}
