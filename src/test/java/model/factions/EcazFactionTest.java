package model.factions;

import constants.Emojis;
import enums.GameOption;
import exceptions.InvalidGameStateException;
import model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

public class EcazFactionTest extends FactionTestTemplate {
    private EcazFaction faction;

    @Override
    Faction getFaction() {
        return faction;
    }

    @BeforeEach
    void setUp() throws IOException {
        faction = new EcazFaction("player", "player");
        commonPostInstantiationSetUp();
    }

    @Test
    public void testInitialSpice() {
        assertEquals(12, faction.getSpice());
    }

    @Test
    public void testFreeRevival() {
        assertEquals(2, faction.getFreeRevival());
    }

    @Test
    public void testFreeRevivalAlliedToFremen() {
        faction.setAlly("Fremen");
        assertEquals(3, faction.getFreeRevival());
    }

    @Test
    public void testFreeRevivalLowThreshold() {
        game.addGameOption(GameOption.HOMEWORLDS);
        Territory homeworld = game.getTerritory(faction.getHomeworld());
        int forcesToRemove = homeworld.getForceStrength(faction.getName()) - (faction.highThreshold - 1);
        homeworld.removeForces(game, faction.getName(), forcesToRemove);
        faction.checkForLowThreshold();
        assertFalse(faction.isHighThreshold());
        assertEquals(3, faction.getFreeRevival());
    }

    @Test
    public void testFreeRevivalLowThresholdAlliedToFremen() {
        faction.setAlly("Fremen");
        game.addGameOption(GameOption.HOMEWORLDS);
        Territory homeworld = game.getTerritory(faction.getHomeworld());
        int forcesToRemove = homeworld.getForceStrength(faction.getName()) - (faction.highThreshold - 1);
        homeworld.removeForces(game, faction.getName(), forcesToRemove);
        faction.checkForLowThreshold();
        assertFalse(faction.isHighThreshold());
        assertEquals(4, faction.getFreeRevival());
    }

    @Test
    public void testFreeRevivalWithRecruits() throws InvalidGameStateException {
        game.startRevival();
        game.getRevival().setRecruitsInPlay(true);
        assertEquals(4, faction.getFreeRevival());
    }

    @Test
    public void testFreeRevivalWithRecruitsAlliedWithFremen() throws InvalidGameStateException {
        game.startRevival();
        game.getRevival().setRecruitsInPlay(true);
        faction.setAlly("Fremen");
        assertEquals(6, faction.getFreeRevival());
    }

    @Test
    public void testFreeRevivalWithRecruitsLowThreshold() throws InvalidGameStateException {
        game.startRevival();
        game.getRevival().setRecruitsInPlay(true);
        game.addGameOption(GameOption.HOMEWORLDS);
        Territory homeworld = game.getTerritory(faction.getHomeworld());
        int forcesToRemove = homeworld.getForceStrength(faction.getName()) - (faction.highThreshold - 1);
        homeworld.removeForces(game, faction.getName(), forcesToRemove);
        faction.checkForLowThreshold();
        assertFalse(faction.isHighThreshold());
        assertEquals(6, faction.getFreeRevival());
    }

    @Test
    public void testFreeRevivalWithRecruitsLowThresholdAlliedToFremen() throws InvalidGameStateException {
        game.startRevival();
        game.getRevival().setRecruitsInPlay(true);
        faction.setAlly("Fremen");
        game.addGameOption(GameOption.HOMEWORLDS);
        Territory homeworld = game.getTerritory(faction.getHomeworld());
        int forcesToRemove = homeworld.getForceStrength(faction.getName()) - (faction.highThreshold - 1);
        homeworld.removeForces(game, faction.getName(), forcesToRemove);
        faction.checkForLowThreshold();
        assertFalse(faction.isHighThreshold());
        assertEquals(7, faction.getFreeRevival());
    }

    @Test
    public void testReviveDukeVidal() throws InvalidGameStateException {
        faction.addLeader(game.getDukeVidal());
        game.killLeader(faction, "Duke Vidal");
        faction.reviveLeader("Duke Vidal", null);
        assertEquals("Duke Vidal is no longer in service to " + Emojis.ECAZ + " - what a rotten scoundrel!", turnSummary.getMessages().getLast());
    }

    @Test
    public void testInitialHasMiningEquipment() {
        assertFalse(faction.hasMiningEquipment());
    }

    @Test
    public void testInitialReserves() {
        assertEquals(14, faction.getReservesStrength());
    }

    @Test
    public void testInitialForcePlacement() {
        for (Territory territory : game.getTerritories().values()) {
            if (territory instanceof HomeworldTerritory) continue;
            if (territory.getTerritoryName().equals("Imperial Basin (Center Sector)")) {
                assertEquals(6, territory.getForceStrength("Ecaz"));
                assertEquals(1, territory.countFactions());
            } else {
                assertEquals(0, territory.countFactions());
            }
        }
    }

    @Test
    public void testEmoji() {
        assertEquals(Emojis.ECAZ, faction.getEmoji());
    }

    @Test
    public void testHandLimit() {
        assertEquals(4, faction.getHandLimit());
    }

    @Nested
    @DisplayName("sendAmbassadorLocationMessage")
    class SendAmbassadorLocationMessage {
        @Test
        void testNoAmbassadorsInSupply() {
            TestTopic modInfo = new TestTopic();
            game.setModInfo(modInfo);
            faction.getAmbassadorSupply().clear();
            faction.sendAmbassadorLocationMessage(1);
            assertEquals("You have no Ambassadors in supply to place.", chat.getMessages().getFirst());
            assertEquals(Emojis.ECAZ + " has no Ambassadors to place. Please advance the game. " + game.getModOrRoleMention(), modInfo.getMessages().getFirst());
        }

        @Test
        void testInsufficientSpice() {
            TestTopic modInfo = new TestTopic();
            game.setModInfo(modInfo);
            faction.subtractSpice(11, "test");
            faction.sendAmbassadorLocationMessage(2);
            assertEquals("You do not have 2 " + Emojis.SPICE + " to place an Ambassador.", chat.getMessages().getFirst());
            assertEquals(Emojis.ECAZ + " does not have 2 " + Emojis.SPICE + " to place an Ambassador. Please advance the game. " + game.getModOrRoleMention(), modInfo.getMessages().getFirst());
        }

        @Test
        void testNoHMS() {
            faction.sendAmbassadorLocationMessage(1);
            assertEquals(6, chat.getChoices().getFirst().size());
            assertTrue(chat.getChoices().getFirst().stream().noneMatch(c -> c.getLabel().contains("Hidden Mobile Stronghold")));
        }

        @Test
        void testHMSIxInGame() throws IOException {
            IxFaction ix = new IxFaction("ix", "ix");
            game.addFaction(ix);
            faction.sendAmbassadorLocationMessage(1);
            assertEquals(7, chat.getChoices().getFirst().size());
            assertTrue(chat.getChoices().getFirst().stream().anyMatch(c -> c.getLabel().contains("Hidden Mobile Stronghold")));
        }
    }

    @Nested
    @DisplayName("placeAmbassador")
    class PlaceAmbassador {
        @Test
        void testNoAmbassadorsInSupply() {
            faction.getAmbassadorSupply().clear();
            assertThrows(InvalidGameStateException.class, () -> faction.placeAmbassador("Arrakeen", "Fremen", 1));
        }

        @Test
        void testInsufficientSpice() throws InvalidGameStateException {
            TestTopic modInfo = new TestTopic();
            game.setModInfo(modInfo);
            faction.subtractSpice(11, "test");
            faction.placeAmbassador("Arrakeen", "Fremen", 2);
            assertEquals("You do not have 2 " + Emojis.SPICE + " to place your Ambassador.", chat.getMessages().getFirst());
            assertEquals(Emojis.ECAZ + " does not have 2 " + Emojis.SPICE + " to place an Ambassador. Please advance the game. " + game.getModOrRoleMention(), modInfo.getMessages().getFirst());
        }

        @Test
        void testAmbassadorPlaced() throws InvalidGameStateException {
            String ambassador = faction.getAmbassadorSupply().getFirst();
            String territoryName = "Arrakeen";
            Territory arrakeen = game.getTerritory(territoryName);
            faction.placeAmbassador(territoryName, ambassador, 1);
            assertEquals(11, faction.getSpice());
            assertFalse(faction.getAmbassadorSupply().contains(ambassador));
            assertEquals(ambassador, arrakeen.getEcazAmbassador());
            assertEquals("The " + ambassador + " Ambassador has been sent to Arrakeen.", chat.getMessages().getFirst());
            assertEquals("-1 " + Emojis.SPICE + " " + ambassador + " Ambassador to Arrakeen = 11 " + Emojis.SPICE, ledger.getMessages().getFirst());
            assertEquals(Emojis.ECAZ + " has sent the " + ambassador + " Ambassador to Arrakeen.", turnSummary.getMessages().getFirst());
        }
    }

    @Nested
    @DisplayName("#checkForAmbassadorTrigger")
    class CheckForAmbassadorTrigger {
        AtreidesFaction atreides;
        BGFaction bg;
        Territory carthag;
        TestTopic turnSummary;

        @BeforeEach
        void setUp() throws IOException {
            turnSummary = new TestTopic();
            game.setTurnSummary(turnSummary);
            atreides = new AtreidesFaction("p", "u");
            bg = new BGFaction("p", "u");
            carthag = game.getTerritory("Carthag");
            carthag.setEcazAmbassador("BG");
        }

        @Test
        void testEcazDoesNotTrigger() {
            faction.checkForAmbassadorTrigger(carthag, faction);
            assertTrue(turnSummary.getMessages().isEmpty());
            assertTrue(chat.getMessages().isEmpty());
        }

        @Test
        void testOtherFactionTriggers() {
            faction.checkForAmbassadorTrigger(carthag, atreides);
            assertEquals(Emojis.ECAZ + " has an opportunity to trigger their BG Ambassador.", turnSummary.getMessages().getFirst());
            assertTrue(chat.getMessages().getFirst().contains("Will you trigger your BG Ambassador against " + Emojis.ATREIDES + " in Carthag?"));
            assertEquals(2, chat.getChoices().getFirst().size());
        }

        @Test
        void testAllyDoesNotTrigger() {
            faction.setAlly("Atreides");
            atreides.setAlly("Ecaz");
            faction.checkForAmbassadorTrigger(carthag, atreides);
            assertTrue(turnSummary.getMessages().isEmpty());
            assertTrue(chat.getMessages().isEmpty());
        }

        @Test
        void testAmbassadorFactionDoesNotTrigger() {
            faction.checkForAmbassadorTrigger(carthag, bg);
            assertTrue(turnSummary.getMessages().isEmpty());
            assertTrue(chat.getMessages().isEmpty());
        }
    }

    @Nested
    @DisplayName("#triggerAmbassador")
    class TriggerAmbassador {
        HarkonnenFaction harkonnen;
        TestTopic modInfo;

        @BeforeEach
        public void setUp() throws IOException {
            harkonnen = new HarkonnenFaction("p", "u");
            game.addFaction(harkonnen);
            modInfo = new TestTopic();
            game.setModInfo(modInfo);
            faction.addTreacheryCard(new TreacheryCard("Karama"));
            faction.addTreacheryCard(new TreacheryCard("Kulon"));
            faction.addTreacheryCard(new TreacheryCard("Cheap Hero"));
            faction.addTreacheryCard(new TreacheryCard("Family Atomics"));
        }

        @Test
        public void testTriggerAtreidesRevealsTreacheryHand() {
            harkonnen.addTreacheryCard(new TreacheryCard("Karama"));
            harkonnen.addTreacheryCard(new TreacheryCard("Shield"));
            faction.triggerAmbassador(harkonnen, "Atreides");
            assertEquals(Emojis.HARKONNEN + " hand is:\n\t" + Emojis.TREACHERY + " **Karama** _Special_\n\t" + Emojis.TREACHERY + " **Shield** _Defense - Projectile_", chat.getMessages().getFirst());
        }

        @Test
        public void testTriggerBGGivesChoicesOfNonSupplyAmbassadors() {
            faction.triggerAmbassador(harkonnen, "BG");
            assertEquals("Which Ambassador effect would you like to trigger?", chat.getMessages().getFirst());
            assertEquals(5, chat.getChoices().getFirst().size());
//            assertEquals("", String.join(", ",chat.getChoices().getFirst().stream().map(DuneChoice::getLabel).toList()));
        }

        @Test
        public void testTriggerCHOAMGivesDiscardButtons() {
            faction.triggerAmbassador(harkonnen, "CHOAM");
            assertEquals("Select " + Emojis.TREACHERY + " to discard for 3 " + Emojis.SPICE + " each (one at a time).", chat.getMessages().getFirst());
            assertEquals(5, chat.getChoices().getFirst().size());
            assertEquals("Done discarding", chat.getChoices().getFirst().getLast().getLabel());
        }

        @Test
        public void testTriggerEmperorAdds5Spice() {
            faction.triggerAmbassador(harkonnen, "Emperor");
            assertEquals("+5 " + Emojis.SPICE + " " + Emojis.EMPEROR + " Ambassador = 17 " + Emojis.SPICE, ledger.getMessages().getFirst());
            assertEquals(17, faction.getSpice());
        }

        @Test
        public void testTriggerFremenPresentsTerritoriesToMoveFrom() {
            faction.triggerAmbassador(harkonnen, "Fremen");
            assertEquals("Where would you like to ride from with your Fremen Ambassador?", chat.getMessages().getFirst());
            assertEquals(2, chat.getChoices().getFirst().size());
            assertEquals("Imperial Basin (Center Sector)", chat.getChoices().getFirst().getFirst().getLabel());
            assertEquals("No move", chat.getChoices().getFirst().getLast().getLabel());
        }

        @Test
        public void testTriggerGuildPresentsDestinationChoices() {
            faction.triggerAmbassador(harkonnen, "Guild");
            assertTrue(modInfo.getMessages().isEmpty());
            assertEquals("Where would you like to place up to 4 " + Emojis.ECAZ_TROOP + " from reserves?", chat.getMessages().getFirst());
            assertEquals(5, chat.getChoices().getFirst().size());
            assertEquals("Stronghold", chat.getChoices().getFirst().getFirst().getLabel());
            assertEquals("stronghold-guild-ambassador", chat.getChoices().getFirst().getFirst().getId());
            assertEquals("Pass shipment", chat.getChoices().getFirst().getLast().getLabel());
        }

        @Test
        public void testTriggerGuildNoForcesInReserves() {
            game.removeForces(faction.getHomeworld(), faction, 14, 0, true);
            faction.triggerAmbassador(harkonnen, "Guild");
            assertTrue(modInfo.getMessages().isEmpty());
            assertEquals("You have no " + Emojis.ECAZ_TROOP + " in reserves to place with the Guild Ambassador.", chat.getMessages().getFirst());
        }

        @Test
        public void testTriggerHarkonnenShowTraitor() throws IOException {
            AtreidesFaction atreides;
            atreides = new AtreidesFaction("p", "u");
            game.addFaction(atreides);
            atreides.addTraitorCard(new TraitorCard("Feyd Rautha", "Harkonnen", 6));

            faction.triggerAmbassador(atreides, "Harkonnen");
            assertTrue(chat.getMessages().getFirst().contains(Emojis.ATREIDES + " has "));
            assertTrue(chat.getMessages().getFirst().contains(" as a Traitor!"));
        }

        @Test
        public void testTriggerHarkonnenShowFaceDancer() throws IOException {
            BTFaction bt;
            bt = new BTFaction("p", "u");
            game.addFaction(bt);
            bt.addTraitorCard(new TraitorCard("Duncan Idaho", "Atreides", 2));
            bt.addTraitorCard(new TraitorCard("Feyd Rautha", "Harkonnen", 6));
            bt.addTraitorCard(new TraitorCard("Burseg", "Emperor", 3));

            faction.triggerAmbassador(bt, "Harkonnen");
            assertTrue(chat.getMessages().getFirst().contains(Emojis.BT + " has "));
            assertTrue(chat.getMessages().getFirst().contains(" as a Face Dancer!"));
        }

        @Test
        public void testTriggerIxGivesDiscardButtons() {
            faction.triggerAmbassador(harkonnen, "Ix");
            assertEquals("You can discard a " + Emojis.TREACHERY + " from your hand and draw a new one.", chat.getMessages().getFirst());
            assertEquals(5, chat.getChoices().getFirst().size());
            assertEquals("Don't discard", chat.getChoices().getFirst().getLast().getLabel());
        }

        @Test
        public void testTriggerRicheseGivesButtonsToBuyACard() {
            faction.discard("Cheap Hero");
            faction.triggerAmbassador(harkonnen, "Richese");
            assertEquals("Would you like to buy a " + Emojis.TREACHERY + " card for 3 " + Emojis.SPICE + "?", chat.getMessages().getFirst());
            assertEquals(2, chat.getChoices().getFirst().size());
            assertEquals("Yes", chat.getChoices().getFirst().getFirst().getLabel());
            assertEquals("No", chat.getChoices().getFirst().getLast().getLabel());
        }

        @Test
        public void testTriggerRicheseNotEnoughSpiceToBuyACard() {
            faction.subtractSpice(faction.getSpice(), "Test");
            faction.addSpice(2, "Test");
            faction.discard("Cheap Hero");
            faction.triggerAmbassador(harkonnen, "Richese");
            assertEquals("You do not have enough " + Emojis.SPICE + " to buy a " + Emojis.TREACHERY + " card with your Richese Ambassador.", chat.getMessages().getFirst());
            assertEquals(0, chat.getChoices().size());
        }

        @Test
        public void testRicheseAmbassadorWhenHandIsFull() {
            faction.triggerAmbassador(harkonnen, "Richese");
            assertEquals("Your hand is full, so you cannot buy a " + Emojis.TREACHERY + " card with your Richese Ambassador.", chat.getMessages().getFirst());
            assertEquals(0, chat.getChoices().size());
        }

        @Test
        public void testBTAmbassadorNoLeadersOrForcesInTanks() {
            faction.triggerAmbassador(harkonnen, "BT");
            assertEquals("You have no leaders or " + Emojis.ECAZ_TROOP + " in the tanks to revive with your BT Ambassador.", chat.getMessages().getFirst());
            assertEquals(0, chat.getChoices().size());
            assertEquals(Emojis.ECAZ + " has no leaders or " + Emojis.ECAZ_TROOP + " in the tanks to revive.", turnSummary.getMessages().get(1));
        }

        @Test
        public void testBTAmbassadorOnly2ForcesInTanks() {
            faction.removeForces(faction.getHomeworld(), 2, false, true);
            faction.triggerAmbassador(harkonnen, "BT");
            assertEquals("You revived 2 " + Emojis.ECAZ_TROOP + " with your BT Ambassador.", chat.getMessages().getFirst());
            assertEquals(0, chat.getChoices().size());
        }

        @Test
        public void testBTAmbassadorOnly5ForcesInTanks() {
            faction.removeForces(faction.getHomeworld(), 5, false, true);
            faction.triggerAmbassador(harkonnen, "BT");
            assertEquals("You revived 4 " + Emojis.ECAZ_TROOP + " with your BT Ambassador.", chat.getMessages().getFirst());
            assertEquals(0, chat.getChoices().size());
        }

        @Test
        public void testBTAmbassadorOnly1LeaderInTanks() {
            game.killLeader(faction, "Whitmore Bludd");
            faction.triggerAmbassador(harkonnen, "BT");
            assertEquals("Whitmore Bludd was revived with your BT Ambassador.", chat.getMessages().getFirst());
            assertEquals(0, chat.getChoices().size());
        }

        @Test
        public void testBTAmbassadorOnly2LeadersInTanks() {
            game.killLeader(faction, "Whitmore Bludd");
            game.killLeader(faction, "Sanya Ecaz");
            faction.triggerAmbassador(harkonnen, "BT");
            assertEquals("Which leader would you like to revive?", chat.getMessages().getFirst());
            assertEquals(2, chat.getChoices().getFirst().size());
        }

        @Test
        public void testBTAmbassador5ForcesAnd2LeadersInTanks() {
            faction.removeForces(faction.getHomeworld(), 5, false, true);
            game.killLeader(faction, "Whitmore Bludd");
            game.killLeader(faction, "Sanya Ecaz");
            faction.triggerAmbassador(harkonnen, "BT");
            assertEquals("Would you like to revive a leader or 4 " + Emojis.ECAZ_TROOP + "?", chat.getMessages().getFirst());
            assertEquals(2, chat.getChoices().getFirst().size());
        }
    }

    @Nested
    @DisplayName("#gainDukeVidalWithEcazAmbassador")
    class GainDukeVidalWithEcazAmbassador {
        @Test
        void testDukeVidalIsWithNoFaction() throws InvalidGameStateException {
            assertFalse(faction.getLeader("Duke Vidal").isPresent());
            faction.gainDukeVidalWithEcazAmbassador();
            assertTrue(faction.getLeader("Duke Vidal").isPresent());
            assertEquals("Duke Vidal now works for " + Emojis.ECAZ, turnSummary.getMessages().getFirst());
        }

        @Test
        void testDukeVidalIsWithMoritani() throws InvalidGameStateException, IOException {
            MoritaniFaction moritani = new MoritaniFaction("mo", "mo");
            moritani.setChat(new TestTopic());
            game.addFaction(moritani);
            game.assignDukeVidalToAFaction(moritani.getName());
            assertTrue(moritani.getLeader("Duke Vidal").isPresent());
            assertFalse(faction.getLeader("Duke Vidal").isPresent());
            faction.gainDukeVidalWithEcazAmbassador();
            assertTrue(faction.getLeader("Duke Vidal").isPresent());
            assertFalse(moritani.getLeader("Duke Vidal").isPresent());
            assertEquals("Duke Vidal now works for " + Emojis.ECAZ, turnSummary.getMessages().getLast());
        }

        @Test
        void testDukeVidalIsWithHarkonnen() throws IOException {
            HarkonnenFaction harkonnen = new HarkonnenFaction("ha", "ha");
            harkonnen.setChat(new TestTopic());
            game.addFaction(harkonnen);
            game.assignDukeVidalToAFaction(harkonnen.getName());
            assertTrue(harkonnen.getLeader("Duke Vidal").isPresent());
            assertFalse(faction.getLeader("Duke Vidal").isPresent());
            assertThrows(InvalidGameStateException.class, () -> faction.gainDukeVidalWithEcazAmbassador());
        }

        @Test
        void testDukeVidalIsWithBT() throws IOException {
            BTFaction bt = new BTFaction("bt", "bt");
            bt.setChat(new TestTopic());
            game.addFaction(bt);
            game.assignDukeVidalToAFaction(bt.getName());
            assertTrue(bt.getLeader("Duke Vidal").isPresent());
            assertFalse(faction.getLeader("Duke Vidal").isPresent());
            assertThrows(InvalidGameStateException.class, () -> faction.gainDukeVidalWithEcazAmbassador());
        }

        @Test
        void testDukeVidalIsWithEcazOccupier() throws IOException, InvalidGameStateException {
            game.addGameOption(GameOption.HOMEWORLDS);
            AtreidesFaction atreides = new AtreidesFaction("at", "at");
            atreides.setChat(new TestTopic());
            atreides.setLedger(new TestTopic());
            game.addFaction(atreides);
            Territory ecazHomeworld = game.getTerritory("Ecaz");
            ecazHomeworld.removeForces(game, "Ecaz", 14);
            atreides.placeForceFromReserves(game, ecazHomeworld, 2, false);
            assertTrue(faction.isHomeworldOccupied());
            assertTrue(atreides.getLeader("Duke Vidal").isPresent());
            assertFalse(faction.getLeader("Duke Vidal").isPresent());
            faction.gainDukeVidalWithEcazAmbassador();
            assertTrue(faction.getLeader("Duke Vidal").isPresent());
            assertFalse(atreides.getLeader("Duke Vidal").isPresent());
            assertEquals("Duke Vidal now works for " + Emojis.ECAZ, turnSummary.getMessages().getLast());
        }

        @Test
        void testDukeVidalIsInTheTanks() throws IOException {
            MoritaniFaction moritani = new MoritaniFaction("mo", "mo");
            moritani.setChat(new TestTopic());
            moritani.setLedger(new TestTopic());
            game.addFaction(moritani);
            game.assignDukeVidalToAFaction(moritani.getName());
            assertTrue(moritani.getLeader("Duke Vidal").isPresent());
            assertFalse(faction.getLeader("Duke Vidal").isPresent());
            game.killLeader(moritani, "Duke Vidal");
            assertThrows(InvalidGameStateException.class, () -> faction.gainDukeVidalWithEcazAmbassador());
        }
    }

    @Nested
    @DisplayName("discardWithCHOAMAmbassador")
    class DiscardWithCHOAMAmbassador {
        @Test
        void testDiscardOneAndGetAskedAgain() {
            faction.addTreacheryCard(new TreacheryCard("Kulon"));
            faction.addTreacheryCard(new TreacheryCard("Baliset"));
            faction.discardWithCHOAMAmbassador("Kulon");
            assertEquals("You discarded Kulon for 3 " + Emojis.SPICE, chat.getMessages().getFirst());
            assertEquals(15, faction.getSpice());
            assertEquals("Select " + Emojis.TREACHERY + " to discard for 3 " + Emojis.SPICE + " each (one at a time).", chat.getMessages().getLast());
        }

        @Test
        void testDiscardingLastCard() {
            faction.addTreacheryCard(new TreacheryCard("Kulon"));
            faction.discardWithCHOAMAmbassador("Kulon");
            assertEquals("You discarded Kulon for 3 " + Emojis.SPICE, chat.getMessages().getFirst());
            assertEquals(15, faction.getSpice());
            assertEquals("You have no " + Emojis.TREACHERY + " to discard with your " + Emojis.CHOAM + " Ambassador. Your Ambassador has been used.", chat.getMessages().getLast());
            assertTrue(chat.getChoices().isEmpty());
        }

        @Test
        void testDiscardNone() {
            faction.discardWithCHOAMAmbassador("None");
            assertEquals("You are finished discarding with your " + Emojis.CHOAM + " Ambassador.", chat.getMessages().getFirst());
            assertTrue(chat.getChoices().isEmpty());
        }
    }

    @Nested
    @DisplayName("#discardAndDrawWithIxAmbassador")
    class DiscardAndDrawWithIxAmbassador {
        @Test
        void testEcazHasNoCards() {
            assertThrows(IllegalArgumentException.class, () -> faction.discardAndDrawWithIxAmbassador("Kulon"));
        }

        @Test
        void testSuccessfulDiscard() {
            faction.addTreacheryCard(new TreacheryCard("Kulon"));
            faction.discardAndDrawWithIxAmbassador("Kulon");
            assertTrue(chat.getMessages().getFirst().startsWith("You discarded Kulon and drew "));
            assertEquals(1, faction.getTreacheryHand().size());
            assertNotEquals("Kulon", faction.getTreacheryHand().getFirst().name());
        }

        @Test
        void testNoDiscard() {
            faction.addTreacheryCard(new TreacheryCard("Kulon"));
            faction.discardAndDrawWithIxAmbassador("None");
            assertEquals("You will not discard and draw a new card with your Ix Ambassador.", chat.getMessages().getFirst());
            assertEquals(1, faction.getTreacheryHand().size());
            assertEquals("Kulon", faction.getTreacheryHand().getFirst().name());
        }
    }

    @Nested
    @DisplayName("#buyCardWithRicheseAmbassador")
    class BuyCardWithRicheseAmbassador {
        @Test
        void testBuyCard() {
            faction.buyCardWithRicheseAmbassador(true);
            assertTrue(chat.getMessages().getFirst().contains("with your Richese Ambassador."));
            assertEquals(Emojis.ECAZ + " buys a " + Emojis.TREACHERY + " card for 3 " + Emojis.SPICE + " with their Richese Ambassador." , turnSummary.getMessages().getLast());
            assertEquals(1, faction.getTreacheryHand().size());
        }

        @Test
        void testDontBuyCard() {
            faction.buyCardWithRicheseAmbassador(false);
            assertEquals("You will not buy a " + Emojis. TREACHERY + " with your Richese Ambassador.", chat.getMessages().getFirst());
            assertEquals(Emojis.ECAZ + " does not buy a " + Emojis.TREACHERY + " with their Richese Ambassador." , turnSummary.getMessages().getLast());
            assertEquals(0, faction.getTreacheryHand().size());
        }

        @Test
        void testNotEnoughSpice() {
            faction.subtractSpice(10, "test");
            assertEquals(2, faction.getSpice());
            assertThrows(IllegalStateException.class, () -> faction.buyCardWithRicheseAmbassador(true));
        }

        @Test
        void testHandIsFull() {
            faction.addTreacheryCard(new TreacheryCard("Kulon"));
            faction.addTreacheryCard(new TreacheryCard("Baliset"));
            faction.addTreacheryCard(new TreacheryCard("Shield"));
            faction.addTreacheryCard(new TreacheryCard("Snooper"));
            assertThrows(IllegalStateException.class, () -> faction.buyCardWithRicheseAmbassador(true));
        }
    }

    @Nested
    @DisplayName("removeAmbassadorFromMap")
    class RemoveAmbassadorFromMap {
        String ambassador;

        @BeforeEach
        void setUp() throws InvalidGameStateException {
            assertEquals(6, faction.getAmbassadorSupply().size());
            ambassador = faction.getAmbassadorSupply().getFirst();
            faction.placeAmbassador("Sietch Tabr", ambassador, 1);
            assertEquals(5, faction.getAmbassadorSupply().size());
            assertEquals(5, faction.ambassadorPool.size());
        }

        @Test
        void testReturnToSupply() {
            faction.removeAmbassadorFromMap(ambassador, true);
            assertEquals(6, faction.getAmbassadorSupply().size());
            assertTrue(faction.getAmbassadorSupply().contains(ambassador));
            assertEquals(5, faction.ambassadorPool.size());
            assertFalse(faction.ambassadorPool.contains(ambassador));
        }

        @Test
        void testReturnToPool() {
            faction.removeAmbassadorFromMap(ambassador, false);
            assertEquals(5, faction.getAmbassadorSupply().size());
            assertFalse(faction.getAmbassadorSupply().contains(ambassador));
            assertEquals(6, faction.ambassadorPool.size());
            assertTrue(faction.ambassadorPool.contains(ambassador));
        }
    }

    @Nested
    @DisplayName("#occupierTakesDukeVidal")
    class OccupierTakesDukeVidal {
        HomeworldTerritory ecazHomeworld;
        BTFaction bt;

        @BeforeEach
        public void setUp() throws IOException {
            ecazHomeworld = faction.getHomeworldTerritory();
            bt = new BTFaction("bt", "bt");
            TestTopic btChat = new TestTopic();
            bt.setChat(btChat);
            game.addFaction(bt);
            game.addGameOption(GameOption.HOMEWORLDS);
        }

        @Test
        public void testRemoveEcazForces() {
            ecazHomeworld.addForces("BT", 1);
            assertFalse(bt.getLeaders().stream().anyMatch(l -> l.getName().equals("Duke Vidal")));
            ecazHomeworld.removeForces(game, "Ecaz", 14);
            assertTrue(faction.isHomeworldOccupied());
            assertEquals("Ecaz has flipped to Low Threshold.", turnSummary.getMessages().getFirst());
            assertEquals("Ecaz is now occupied by " + Emojis.BT, turnSummary.getMessages().get(1));
            assertEquals("Duke Vidal has left to work for " + Emojis.BT + " (Ecaz homeworld occupied)", turnSummary.getMessages().getLast());
            assertTrue(bt.getLeaders().stream().anyMatch(l -> l.getName().equals("Duke Vidal")));
        }

        @Test
        public void testAddEnemyForcesToEmptyEcazHomeworld() {
            ecazHomeworld.removeForces(game, "Ecaz", 14);
            assertFalse(faction.isHomeworldOccupied());
            ecazHomeworld.addForces("BT", 1);
            assertTrue(faction.isHomeworldOccupied());
            assertEquals("Ecaz has flipped to Low Threshold.", turnSummary.getMessages().getFirst());
            assertEquals("Ecaz is now occupied by " + Emojis.BT, turnSummary.getMessages().get(1));
            assertEquals("Duke Vidal has left to work for " + Emojis.BT + " (Ecaz homeworld occupied)", turnSummary.getMessages().getLast());
            assertTrue(bt.getLeaders().stream().anyMatch(l -> l.getName().equals("Duke Vidal")));
        }

        @Test
        public void testRemoveEcazForcesVidalInTanks() {
            assertFalse(game.getLeaderTanks().stream().anyMatch(l -> l.getName().equals("Duke Vidal")));
            faction.addLeader(game.getDukeVidal());
            game.killLeader(faction, "Duke Vidal");
            assertTrue(game.getLeaderTanks().stream().anyMatch(l -> l.getName().equals("Duke Vidal")));

            ecazHomeworld.addForces("BT", 1);
            assertFalse(bt.getLeaders().stream().anyMatch(l -> l.getName().equals("Duke Vidal")));
            turnSummary.clear();
            ecazHomeworld.removeForces(game, "Ecaz", 14);
            assertTrue(faction.isHomeworldOccupied());

            assertFalse(bt.getLeaders().stream().anyMatch(l -> l.getName().equals("Duke Vidal")));
            assertTrue(game.getLeaderTanks().stream().anyMatch(l -> l.getName().equals("Duke Vidal")));
            assertEquals("Ecaz has flipped to Low Threshold.", turnSummary.getMessages().getFirst());
            assertEquals("Ecaz is now occupied by " + Emojis.BT, turnSummary.getMessages().get(1));
            assertEquals(Emojis.BT + " may revive Duke Vidal from the tanks.", turnSummary.getMessages().getLast());
        }

        @Test
        public void testAddEnemyForcesToEmptyEcazHomeworldVidalInTanks() {
            assertFalse(game.getLeaderTanks().stream().anyMatch(l -> l.getName().equals("Duke Vidal")));
            faction.addLeader(game.getDukeVidal());
            game.killLeader(faction, "Duke Vidal");
            assertTrue(game.getLeaderTanks().stream().anyMatch(l -> l.getName().equals("Duke Vidal")));
            turnSummary.clear();

            ecazHomeworld.removeForces(game, "Ecaz", 14);
            assertFalse(faction.isHomeworldOccupied());
            ecazHomeworld.addForces("BT", 1);
            assertTrue(faction.isHomeworldOccupied());

            assertFalse(bt.getLeaders().stream().anyMatch(l -> l.getName().equals("Duke Vidal")));
            assertTrue(game.getLeaderTanks().stream().anyMatch(l -> l.getName().equals("Duke Vidal")));
            assertEquals("Ecaz has flipped to Low Threshold.", turnSummary.getMessages().getFirst());
            assertEquals("Ecaz is now occupied by " + Emojis.BT, turnSummary.getMessages().get(1));
            assertEquals(Emojis.BT + " may revive Duke Vidal from the tanks.", turnSummary.getMessages().getLast());
        }

        @Test
        public void testOccupierNotGivenVidalTwice() {
            ecazHomeworld.removeForces(game, "Ecaz", 14);
            assertFalse(faction.isHomeworldOccupied());
            ecazHomeworld.addForces("BT", 1);
            assertTrue(faction.isHomeworldOccupied());
            assertEquals(6, bt.getLeaders().size());

            turnSummary.clear();
            ecazHomeworld.addForces("Ecaz", 1);
            ecazHomeworld.removeForces(game, "Ecaz", 1);
            assertEquals(6, bt.getLeaders().size());
            assertTrue(turnSummary.getMessages().isEmpty());
        }

        @Test
        public void testNewOccupierTakesVidal() throws IOException {
            BGFaction bg = new BGFaction("bg", "bg");
            TestTopic bgChat = new TestTopic();
            bg.setChat(bgChat);
            game.addFaction(bg);

            ecazHomeworld.removeForces(game, "Ecaz", 14);
            assertFalse(faction.isHomeworldOccupied());
            ecazHomeworld.addForces("BT", 1);
            assertTrue(faction.isHomeworldOccupied());
            assertEquals("Ecaz has flipped to Low Threshold.", turnSummary.getMessages().getFirst());
            assertEquals("Ecaz is now occupied by " + Emojis.BT, turnSummary.getMessages().get(1));
            assertEquals("Duke Vidal has left to work for " + Emojis.BT + " (Ecaz homeworld occupied)", turnSummary.getMessages().getLast());
            assertTrue(bt.getLeaders().stream().anyMatch(l -> l.getName().equals("Duke Vidal")));

            turnSummary.clear();
            ecazHomeworld.addForces("BG", 1);
            ecazHomeworld.removeForces(game, "BT", 1);
            assertEquals("Duke Vidal has left to work for " + Emojis.BG + " (Ecaz homeworld occupied)", turnSummary.getMessages().getLast());
            assertTrue(bg.getLeaders().stream().anyMatch(l -> l.getName().equals("Duke Vidal")));
            assertFalse(bt.getLeaders().stream().anyMatch(l -> l.getName().equals("Duke Vidal")));
        }

        @Test
        public void testNewOccupierToEmptyEcazTakesVidal() throws IOException {
            BGFaction bg = new BGFaction("bg", "bg");
            TestTopic bgChat = new TestTopic();
            bg.setChat(bgChat);
            game.addFaction(bg);

            ecazHomeworld.removeForces(game, "Ecaz", 14);
            assertFalse(faction.isHomeworldOccupied());
            ecazHomeworld.addForces("BT", 1);
            assertTrue(faction.isHomeworldOccupied());
            assertEquals("Ecaz has flipped to Low Threshold.", turnSummary.getMessages().getFirst());
            assertEquals("Ecaz is now occupied by " + Emojis.BT, turnSummary.getMessages().get(1));
            assertEquals("Duke Vidal has left to work for " + Emojis.BT + " (Ecaz homeworld occupied)", turnSummary.getMessages().getLast());
            assertTrue(bt.getLeaders().stream().anyMatch(l -> l.getName().equals("Duke Vidal")));

            turnSummary.clear();
            ecazHomeworld.removeForces(game, "BT", 1);
            ecazHomeworld.addForces("BG", 1);
            assertEquals("Duke Vidal has left to work for " + Emojis.BG + " (Ecaz homeworld occupied)", turnSummary.getMessages().getLast());
            assertTrue(bg.getLeaders().stream().anyMatch(l -> l.getName().equals("Duke Vidal")));
            assertFalse(bt.getLeaders().stream().anyMatch(l -> l.getName().equals("Duke Vidal")));
        }
    }
}
