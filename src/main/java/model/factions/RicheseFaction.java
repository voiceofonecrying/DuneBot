package model.factions;

import constants.Emojis;
import enums.GameOption;
import enums.UpdateType;
import exceptions.InvalidGameStateException;
import model.*;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class RicheseFaction extends Faction {
    List<TreacheryCard> treacheryCardCache;
    Integer frontOfShieldNoField;
    List<Integer> behindShieldNoField;

    public RicheseFaction(String player, String userName) throws IOException {
        super("Richese", player, userName);

        this.spice = 5;
        this.freeRevival = 2;
        this.emoji = Emojis.RICHESE;
        this.highThreshold = 10;
        this.lowThreshold = 9;
        this.occupiedIncome = 1;
        this.homeworld = "Richese";
        this.behindShieldNoField = new ArrayList<>();
        this.behindShieldNoField.add(0);
        this.behindShieldNoField.add(3);
        this.behindShieldNoField.add(5);

        treacheryCardCache = new ArrayList<>();
        treacheryCardCache.add(new TreacheryCard("Ornithopter"));
        treacheryCardCache.add(new TreacheryCard("Residual Poison"));
        treacheryCardCache.add(new TreacheryCard("Semuta Drug"));
        treacheryCardCache.add(new TreacheryCard("Stone Burner"));
        treacheryCardCache.add(new TreacheryCard("Mirror Weapon"));
        treacheryCardCache.add(new TreacheryCard("Portable Snooper"));
        treacheryCardCache.add(new TreacheryCard("Distrans"));
        treacheryCardCache.add(new TreacheryCard("Juice of Sapho"));
        treacheryCardCache.add(new TreacheryCard("Karama"));
        treacheryCardCache.add(new TreacheryCard("Nullentropy Box"));
    }

    @Override
    public void joinGame(@NotNull Game game) {
        super.joinGame(game);
        Territory richese = game.getTerritories().addHomeworld(game, homeworld, name);
        richese.addForces(name, 20);
        game.getHomeworlds().put(name, homeworld);
    }

    public void presentBlackMarketMethodChoices(String cardName) {
        List<DuneChoice> choices = new ArrayList<>();
        choices.add(new DuneChoice("richese-black-market-method-" + cardName + "-" + "Normal", "Normal"));
        choices.add(new DuneChoice("richese-black-market-method-" + cardName + "-" + "OnceAroundCCW", "OnceAroundCCW"));
        choices.add(new DuneChoice("richese-black-market-method-" + cardName + "-" + "OnceAroundCW", "OnceAroundCW"));
        choices.add(new DuneChoice("richese-black-market-method-" + cardName + "-" + "Silent", "Silent"));
        choices.add(new DuneChoice("secondary", "richese-black-market-method-reselect", "Start over"));
        chat.reply("How would you like to sell " + cardName + "?", choices);
    }

    public void presentConfirmBlackMarketChoices(String cardName, String method) {
        List<DuneChoice> choices = new ArrayList<>();
        choices.add(new DuneChoice("success", "richese-black-market-" + cardName + "-" + method, "Confirm " + cardName + " by " + method + " auction."));
        choices.add(new DuneChoice("secondary", "richese-black-market-reselect", "Start over"));
        chat.reply("", choices);
    }

    public boolean hasFrontOfShieldNoField() {
        return frontOfShieldNoField != null;
    }

    public Integer getFrontOfShieldNoField() {
        return frontOfShieldNoField;
    }

    public void setFrontOfShieldNoField(Integer frontOfShieldNoField) {
        if (!List.of(0, 3, 5).contains(frontOfShieldNoField)) {
            throw new IllegalArgumentException("Front of shield no field must be 0, 3, or 5");
        }

        this.frontOfShieldNoField = frontOfShieldNoField;

        setUpdated(UpdateType.MISC_FRONT_OF_SHIELD);
    }

    public void shipNoField(Faction faction, Territory territory, int noField, boolean karama, boolean crossShip, int accompanyingForce) throws InvalidGameStateException {
        if (accompanyingForce > 0) {
            if (faction != this)
                throw new InvalidGameStateException("Only Richese can ship forces with a No-Field.");
            else if (!game.hasGameOption(GameOption.HOMEWORLDS))
                throw new InvalidGameStateException("Richese can only ship forces with No-Field in Homeworld games.");
            else if (!isHighThreshold)
                throw new InvalidGameStateException("Richese must be at High Threshold to ship forces with a No-Field.");
        }
        String territoryName = territory.getTerritoryName();
        String turnSummaryMessage;
        turnSummaryMessage = faction.getEmoji() + " ship a " + Emojis.NO_FIELD + " to " + territoryName;
        revealNoField();
        territory.setRicheseNoField(noField);
        faction.noFieldMessage(noField, territoryName);
        int spice = game.shipmentCost(faction, 1, territory, karama, false);
        turnSummaryMessage += faction.payForShipment(game, spice, territory, karama, true);
        if (game.hasFaction("BG") && accompanyingForce == 0) {
            if (territory.hasActiveFaction(game.getFaction("BG")) && !(faction instanceof BGFaction))
                ((BGFaction) game.getFaction("BG")).bgFlipMessageAndButtons(game, territory.getTerritoryName());
            if (!crossShip)
                ((BGFaction) game.getFaction("BG")).presentAdvisorChoices(game, faction, territory);
        }
        game.getTurnSummary().publish(turnSummaryMessage);
        if (!(faction instanceof RicheseFaction))
            revealNoField(game, faction);
        if (game.hasGameOption(GameOption.TECH_TOKENS))
            TechToken.addSpice(game, TechToken.HEIGHLINERS);

        if (accompanyingForce > 0)
            placeForces(territory, accompanyingForce, 0, true, true, false, game, karama, false);
        game.checkForTriggers(territory, faction, accompanyingForce + 1);
        game.setUpdated(UpdateType.MAP);
    }

    public Territory getTerritoryWithNoFieldOrNull() {
        return game.getTerritories().values().stream().filter(Territory::hasRicheseNoField).findFirst().orElse(null);
    }

    public void revealNoField(Game game, Faction factionShippedWithNoField) {
        Territory territory = getTerritoryWithNoFieldOrNull();
        if (territory != null) {
            int noField = territory.getRicheseNoField();
            int numReserves = factionShippedWithNoField.getReservesStrength();
            int specialStrength = factionShippedWithNoField.getSpecialReservesStrength();

            int numForces = Math.min(noField, numReserves);
            if (numForces > 0)
                factionShippedWithNoField.placeForceFromReserves(game, territory, numForces, false);

            int numSpecialForces = 0;
            int noFieldStrengthRemaining = noField - numForces;
            if (noFieldStrengthRemaining > 0 && specialStrength > 0) {
                numSpecialForces = Math.min(noFieldStrengthRemaining, specialStrength);
                factionShippedWithNoField.placeForceFromReserves(game, territory, numSpecialForces, true);
            }

            String forcesString = factionShippedWithNoField.forcesString(numForces, numSpecialForces);
            String message = "The " + noField + " " + Emojis.NO_FIELD + " in " + territory.getTerritoryName() + " reveals " + forcesString;
            int starReplacements = Math.min(numForces, factionShippedWithNoField.getSpecialReservesStrength());
            if (starReplacements > 0)
                message += "\n" + factionShippedWithNoField.getEmoji() + " may replace up to " + starReplacements + " " + Emojis.getForceEmoji(factionShippedWithNoField.getName()) + " with " + Emojis.getForceEmoji(factionShippedWithNoField.getName() + "*");

            game.getTurnSummary().publish(message);
            territory.setRicheseNoField(null);
            setFrontOfShieldNoField(noField);
        }
    }

    public void revealNoField() {
        revealNoField(game, this);
    }

    public TreacheryCard getTreacheryCardFromCache(String name) {
        return treacheryCardCache.stream()
                .filter(t -> t.name().equals(name))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Treachery card not found in cache"));
    }

    public TreacheryCard removeTreacheryCardFromCache(TreacheryCard card) {
        treacheryCardCache.remove(card);
        setUpdated(UpdateType.MISC_BACK_OF_SHIELD);
        return card;
    }

    public List<TreacheryCard> getTreacheryCardCache() {
        return treacheryCardCache;
    }
}
