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
is calculated and bidding can start.

To start bidding on a normal (non-Richese) card, use the `/run bidding` command, and then use the `/awardbid` command to award the bid.
When using `/awardbid`, make sure to include the faction that gets paid for the card (if a faction gets paid). Right now
the only way to handle another faction paying for a card is to use `/resourceaddorsubtract`.

For Richese bidding use `/richese card-bid`.

The bot does not currently stop bidding when bidding is finished on the last card, so it's up to the mod to keep track of
how many cards are up for bid that round and stop placing cards up for bid when the last card is awarded.

Here are some commands that could be helpful during this phase:
* `/richese black-market-bid`
* `/run bidding`
* `/richese card-bid`
* `/awardbid`
* `/resourceaddorsubtract` - For the resource, type in "spice". If the amount is negative then the spice is removed. Don't forget to type in a message.
* `/discard`