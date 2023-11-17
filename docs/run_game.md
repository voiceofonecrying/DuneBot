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
* `/remove-forces`
* `/place-forces` - remember to set isShippment to false to stop them from getting charged.
* `/set-spice-in-territory`
* `/destroy-shield-wall`
* `/discard`
* `/revive-forces` - remember to set paid to false to stop them from getting charged.


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
* `/add-spice` - Don't forget to type in a message.
* `/remove-spice` - Don't forget to type in a message.
* `/discard`

## Bidding Phase
The first part of the bidding phase ("Start Bidding Phase") is used in cases when actions should be done before the number
of cards up for bid are calculated (Richese Black Market).  The Richese will be presented with buttons to select a card to auction or to decline black market for the turn.

In the second part of the bidding phase (Show Card Counts and Calculate Total Cards for Bid) the number of cards up for bid
is calculated and bidding can start. Use `/run advance` to proceed to this stage. Cards for bid will be pulled from the treachery deck into the bidding market of cards. One extra card will be included if Ix is in the game.

For bidding on Richese cache cards use `/richese card-bid`.
If no factions bid on the card and the Richese does not take it for free, use `/richese remove-card` to remove the card from the game.
Use `/richese karama-buy` for Richese ability to play Karama and buy any Richese cache card for 3 spice.

To start bidding on a normal (non-Richese) card, use the `/run bidding` command, and then use `/award-top-bidder` or `/award-bid` to award the bid.
`/award-top-bidder` will assign the card to the bidding leader from `/player` bidding commands and will pay the correct recipient for the spice.
When using `/award-bid`, make sure to include the faction that gets paid for the card (if a faction gets paid). The way to handle another faction paying for a card is to use `/remove-spice` for the paying faction and `/add-spice` for the faction getting their cost covered.

If Ix is in the game, the first `/run bidding` each turn will present the bidding market to the Ix player instead of putting a card up for auction.
Use `/ix send-card-to-deck` to put the Ix's rejected card to the top or bottom of the treachery deck.
Use `/ix technology` for Ix to swap a card in hand for the next card up for bid. Call this before `/run bidding`
Use `/ix ally-card-swap` for Ix ally ability to swap just won card for the card at the top of the treachery deck.

If all factions pass on a non-Richese card, use `/run advance`. This will usually end the bidding phase.
If the Richese cache card has not been auctioned yet, use `/richese card-bid` and then `/run advance` again to end the bidding phase

Here are some commands that could be helpful during this phase:
* `/richese card-bid`
* `/run bidding`
* `/ix technology`
* `/award-top-bidder`
* `/award-bid`
* `/add-spice`
* `/remove-spice`
* `/discard`
* `/richese remove-card`
* `/richese karama-buy`
These commands are available to the mod, but should not be needed. Player buttons will perform these actions.
* `/richese black-market-bid`
* `/ix send-card-to-deck`

## Revival
At the start of the revival phase, the bot will automatically do free revivals, taking Fremen alliance into account.
The bot will also take care of paying BT for the free revivals and paying the holder of the Axlotl Tanks token at the
end of the phase.

Use the following commands to do paid revivals:
* `/revive-forces` - Revive forces from the tanks to player reserves. Set Paid to true to charge for the revival and pay BT.
* `/revive-leader` - Revive a leader.  This can also be used for BT to revive a different faction's leader.
* `/add-spice`
* `/remove-spice`

The bot will automatically give revival money to BT.

## Shipping/Movement
The bot will show buttons to each faction during the shipping and movement phase.  The mod can always modify everything
using commands.

The following commands can be useful during the Shipping/Movement phase:
* `/place-forces` - Place forces on the board.  Set isShipment to true to charge for the shipment, and pay Guild.
* `/move-forces` - Move forces from one place to another on the board.
* `/remove-forces` - Remove forces to tanks or reserve.
* `/bg flip` - Flip a BG force.
* `/bg advise` - Place a BG advisor.
* `/richese no-fields-to-front-of-shield` - Remove a no-fields token from the board and place it in front of the Richese shield.  The mod has to manually place the troops.
* `/richese place-no-fields-token` - Place a no-fields token on the board.
* `/add-spice`
* `/remove-spice`
* `/discard` - Discard a card from a player's hand.

## Battle
Battle is the most manual of all the phases.  At the beginning of the phase the bot will show all the battles that
will take place, in storm order.  The mod asks players to create their battle plans, calculates the results, and then
sends commands to the bot to remove the appropriate forces, cards, spice, etc...

The following commands can be useful during the battle phase:
* `/remove-forces` - Remove forces from a territory and optionally send them to the tanks.
* `/kill-leader` - Send a leader to the tanks.
* `/richese no-fields-to-front-of-shield` - Remove a no-fields token from the board and place it in front of the Richese shield.  The mod has to manually place the troops.
* `/discard` - Discard a card from a player's hand.
* `/add-spice`
* `/remove-spice`
* `/hark capture-leader` - Harkonnen capture leader ability
* `/hark kill-leader` - Harkonnen kill leader ability
* `/hark return-leader` - Harkonnen return captured leader

## Spice Collection
There shouldn't typically be mod intervention during this phase.  Spice collection is calculated automatically and
spice is transferred.

## Mentat Pause
During the Mentat Pause spice is automatically transfered from front of shield to back of shield.

The following commands can be useful during the Mentat Pause:
* `/bt swap-face-dancer` - Swap one of the face dancers for a different one.  Currently, the mod has to keep track of which face dancers have been revealed.
* `/choam set-inflation` - Sets the CHOAM inflation marker to "Double" or "Cancel" CHOAM Charity in the next round.
* `/ix move-hms` - Move the HMS to a different location.
