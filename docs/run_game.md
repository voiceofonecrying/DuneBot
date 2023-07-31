# Run the Game
The game is run using `/run advance` to run through the phases of the game.

Each round, `/run advance` goes through the following phases/sub-phases.

1. Start Storm Phase (Storm moves)
2. End Storm Phase (Show next storm movement to the Fremen)
3. Spice Blow
4. CHOAM Charity
5. Start Bidding Phase (For Richese Black Market)
6. Show Card Counts and Calculate Total Cards for Bid
7. Finish Bidding Phase
8. Revival
9. Shipping
10. Battle
11. Spice Harvest
12. Mentat Pause

After the Mentat Pause, play continues to #1 in the next round.

## Storm Phase
"Start Storm Phase" and "End Storm Phase" are separate actions to give the mod the ability to do actions
before sending the Fremen information about the next storm card (Weather Control, Family Atomics, etc...).
Right now there is only support for using storm cards, even if the Fremen are not in the game.

Running "Start Storm Phase" will automatically remove troops and spice blow in the storm's path. These may
need to be brought back if someone uses Weather Control.

Here are some commands that could be helpful during this phase:
* `/removeforces`
* `/placeforces` - remember to set isShippment to false to stop them from getting charged.
* `/set-spice-in-territory`
* `/destroy-shield-wall`
* `/discard`
* `/reviveforces` - remember to set paid to false to stop them from getting charged.


## Spice Blow
After spice blow starts, the mod runs `/draw-spice-blow`, passing the discard pile (A or B).  Splitting this action
up allows for more flexibility during the spice blow phase (Harvester).  The bot should know which spice blow
gets blown away by the storm.

Here are some commands that could be helpful during this phase:
* `/set-spice-in-territory` - This is if Harvester is used
* `/create-alliance` - Any other alliance that the faction was in previous to running this command is removed.  This command only shows the alliance on the map, and does nothing else.
* `/remove-alliance` - Run if a faction is on their own (you don't need ot run it when switching to a different alliance).
* `/discard`

## CHOAM Charity
This phase should run without much mod intervention (CHOAM isn't supported yet). BG automatically gets CHOAM Charity.

Here are some commands that could be helpful during this phase:
* `/resourceaddorsubtract` - For the resource, type in "spice". If the amount is negative then the spice is removed. Don't forget to type in a message.
* `/discard`

## Bidding Phase
The first part of the bidding phase ("Start Bidding Phase") is used in cases when actions should be done before the number
of cards up for bid are calculated (Richese Black Market).  Use the `/richese black-market-bid` command to put a Richese
Black Market card up for bid.

In the second part of the bidding phase (Show Card Counts and Calculate Total Cards for Bid) the number of cards up for bid
is calculated and bidding can start. Use `/run advance` to proceed to this stage. Cards for bid will be pulled from the treachery deck into the bidding market of cards. One extra card will be included if Ix is in the game.

For bidding on Richese cache cards use `/richese card-bid`.
If no factions bid on the card and the Richese does not take it for free, use `/richese remove-card` to remove the card from the game.
Use `/richese karama-buy` for Richese ability to play Karama and buy any Richese cache card for 3 spice.

To start bidding on a normal (non-Richese) card, use the `/run bidding` command, and then use the `/awardbid` command to award the bid.
When using `/awardbid`, make sure to include the faction that gets paid for the card (if a faction gets paid). The way to handle another faction paying for a card is to use `/remove-spice` for the paying faction and `/add-spice` for the faction getting their cost covered.

If Ix is in the game, the first `/run bidding` each turn will present the bidding market to the Ix player instead of putting a card up for auction.
Use `/ix send-card-to-deck` to put the Ix's rejected card to the top or bottom of the treachery deck.
Use `/ix technology` for Ix to swap a card in hand for the next card up for bid. Call this before `/run bidding`
Use `/ix ally-card-swap` for Ix ally ability to swap just won card for the card at the top of the treachery deck.

If all factions pass on a non-Richese card, use `/run advance`. This will usually end the bidding phase.
If the Richese cache card has not been auctioned yet, use `/richese card-bid` and then `/run advance` again to end the bidding phase

Here are some commands that could be helpful during this phase:
* `/richese black-market-bid`
* `/richese card-bid`
* `/run bidding`
* `/ix send-card-to-deck`
* `/ix technology`
* `/awardbid`
* `/add-spice`
* `/remove-spice`
* `/discard`
* `/richese remove-card`
* `/richese karama-buy`