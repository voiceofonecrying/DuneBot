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
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class MikarrolFactionTest extends FactionTestTemplate {
    private MikarrolFaction faction;

    @Override
    Faction getFaction() {
        return faction;
    }

    @BeforeEach
    void setUp() throws IOException {
        faction = new MikarrolFaction("player", "player");
        commonPostInstantiationSetUp();
        // Add other factions only in tests that need them. A global Fremen fixture
        // places forces on Arrakis and invalidates initial-placement assertions.
    }

    private <T extends Faction> T addOpponent(T opponent) {
        opponent.setChat(new TestTopic());
        opponent.setLedger(new TestTopic());
        game.addFaction(opponent);
        return opponent;
    }

    private void setRegularReserves(Faction target, int amount) {
        int current = target.getReservesStrength();
        if (current > amount)
            target.removeReserves(current - amount);
        else if (current < amount)
            target.addReserves(amount - current);
    }

    private void setSpecialReserves(Faction target, int amount) {
        int current = target.getSpecialReservesStrength();
        if (current > amount)
            target.removeSpecialReserves(current - amount);
        else if (current < amount)
            target.addSpecialReserves(amount - current);
    }

    private void addCards(int count) {
        // Duplicate names are intentional: discard choices are unique by name.
        for (int i = 0; i < count; i++)
            faction.addTreacheryCard(new TreacheryCard("Karama"));
    }

    private List<String> latestChoiceIds() {
        return chat.getChoices().getLast().stream().map(DuneChoice::getId).toList();
    }

    private void assertRaidEnded() {
        InvalidGameStateException error = assertThrows(InvalidGameStateException.class, () -> faction.raid());
        assertEquals("No faction is selected to raid currently.", error.getMessage());
    }

    @Test
    void testInitialState() throws Exception {
        assertEquals(12, faction.getSpice());
        assertEquals(20, faction.getReservesStrength());
        assertEquals(0, faction.getSpecialReservesStrength());
        assertEquals(5, faction.getHandLimit());
        assertEquals(":mikarrol:", faction.getEmoji());
        assertEquals(":mikarrol_troop:", faction.getForceEmoji());
        assertFalse(faction.hasMiningEquipment());
        assertFalse(faction.isRaidersBlocked());
        assertEquals(0, faction.getSlaversAmount());
        assertEquals(0, faction.getAllySlaversAmount());
    }

    @Test
    void testHomeworldRegistration() throws Exception {
        assertEquals("Kublai XII", faction.getHomeworld());
        assertEquals("Kublai XII", game.getHomeworlds().get("Mikarrol"));
        assertInstanceOf(HomeworldTerritory.class, game.getTerritory("Kublai XII"));
        assertEquals(20, game.getTerritory("Kublai XII").getForceStrength("Mikarrol"));
    }

    @Test
    void testInitialForcePlacement() throws Exception {
        for (Territory territory : game.getTerritories().values()) {
            if (!(territory instanceof HomeworldTerritory))
                assertEquals(0, territory.countFactions(), territory.getTerritoryName());
        }
    }

    @Test
    void testFreeRevivalHighThreshold() throws Exception {
        assertEquals(2, faction.getFreeRevival());
    }

    @Test
    void testFreeRevivalHighThresholdWithRecruits() throws Exception {
        game.startRevival();
        game.getRevival().setRecruitsInPlay(true);
        assertEquals(4, faction.getFreeRevival());
    }

    @Test
    void testFreeRevivalHighThresholdWithFremen() throws Exception {
        faction.setAlly("Fremen");
        assertEquals(3, faction.getFreeRevival());
    }

    @Test
    void testFreeRevivalHighThresholdWithFremenWithRecruits() throws Exception {
        faction.setAlly("Fremen");
        game.startRevival();
        game.getRevival().setRecruitsInPlay(true);
        assertEquals(6, faction.getFreeRevival());
    }

    @Test
    void testFreeRevivalLowThreshold() throws Exception {
        game.addGameOption(GameOption.HOMEWORLDS);
        setRegularReserves(faction, 5);
        faction.checkForLowThreshold();
        assertFalse(faction.isHighThreshold());
        assertEquals(3, faction.getFreeRevival());
    }

    @Test
    void testFreeRevivalLowThresholdWithRecruits() throws Exception {
        game.addGameOption(GameOption.HOMEWORLDS);
        setRegularReserves(faction, 5);
        faction.checkForLowThreshold();
        assertFalse(faction.isHighThreshold());
        game.startRevival();
        game.getRevival().setRecruitsInPlay(true);
        assertEquals(6, faction.getFreeRevival());
    }

    @Test
    void testFreeRevivalLowThresholdWithFremen() throws Exception {
        faction.setAlly("Fremen");
        game.addGameOption(GameOption.HOMEWORLDS);
        setRegularReserves(faction, 5);
        faction.checkForLowThreshold();
        assertFalse(faction.isHighThreshold());
        assertEquals(4, faction.getFreeRevival());
    }

    @Test
    void testFreeRevivalLowThresholdWithFremenWithRecruits() throws Exception {
        faction.setAlly("Fremen");
        game.addGameOption(GameOption.HOMEWORLDS);
        setRegularReserves(faction, 5);
        faction.checkForLowThreshold();
        assertFalse(faction.isHighThreshold());
        game.startRevival();
        game.getRevival().setRecruitsInPlay(true);
        assertEquals(7, faction.getFreeRevival());
    }

    @Nested
    @DisplayName("#canUseOrnithopters")
    class CanUseOrnithopters extends FactionTestTemplate.CanUseOrnithopters {
        @Test
        @Override
        void testNoOrnithopters() {
            assertFalse(faction.canUseOrnithopters(game, territory, false));
        }
    }

    @Nested
    @DisplayName("#offerRaidChoice / raid / keep / discard / decline")
    class Raid {
        private AtreidesFaction victim;
        private TestTopic victimChat;
        private TestTopic victimLedger;
        private TreacheryCard karama;

        @BeforeEach
        void setUpRaid() throws IOException {
            victim = addOpponent(new AtreidesFaction("at", "at"));
            victimChat = (TestTopic) victim.getChat();
            victimLedger = (TestTopic) victim.getLedger();
            karama = new TreacheryCard("Karama");
            victim.addTreacheryCard(karama);
        }

        @Test
        void testOfferChoices() throws Exception {
            faction.offerRaidChoice(victim);
            assertEquals(List.of("mikarrol-raid", "mikarrol-decline-raid"), latestChoiceIds());
            assertEquals("Will you raid " + victim.getEmoji() + "? player", chat.getMessages().getLast());
            assertTrue(victim.getTreacheryHand().contains(karama));
            assertTrue(faction.getTreacheryHand().isEmpty());
        }

        @Test
        void testOfferAfterBlock() throws Exception {
            faction.setRaidersBlocked(true);
            int choices = chat.getChoices().size();
            faction.offerRaidChoice(victim);
            assertFalse(faction.isRaidersBlocked());
            assertEquals(choices, chat.getChoices().size());
            assertRaidEnded();
            faction.offerRaidChoice(victim);
            faction.raid();
            assertTrue(faction.getTreacheryHand().contains(karama));
        }

        @Test
        void testEmptyOffer() throws Exception {
            victim.discard("Karama");
            int choices = chat.getChoices().size();
            faction.offerRaidChoice(victim);
            assertEquals(choices, chat.getChoices().size());
            assertEquals(victim.getEmoji() + " has no " + Emojis.TREACHERY + " cards to raid.", turnSummary.getMessages().getLast());
            assertRaidEnded();
        }

        @Test
        void testTransferDoesNotDiscard() throws Exception {
            int discarded = game.getTreacheryDiscard().size();
            faction.raid("Atreides");
            assertSame(karama, faction.getTreacheryHand().getFirst());
            assertTrue(victim.getTreacheryHand().isEmpty());
            assertEquals(discarded, game.getTreacheryDiscard().size());
        }

        @Test
        void testRaidReportsToBothPlayers() throws Exception {
            faction.raid("Atreides");
            assertEquals("Karama has been stolen by " + faction.getEmoji() + " raiders!", victimChat.getMessages().getLast());
            assertEquals("Karama stolen by " + faction.getEmoji(), victimLedger.getMessages().getLast());
            assertEquals("Your raiders have returned, stealing Karama from " + victim.getEmoji() + "!", ledger.getMessages().getLast());
            assertEquals(faction.getEmoji() + " has raided the treachery hand of " + victim.getEmoji() + ".", turnSummary.getMessages().getLast());
        }

        @Test
        void testKeepCard() throws Exception {
            int spice = faction.getSpice();
            faction.raid("Atreides");
            assertEquals(List.of("mikarrol-keep-raid-card", "mikarrol-discard-raid-card"), latestChoiceIds());
            faction.keepRaidedCard();
            assertTrue(faction.getTreacheryHand().contains(karama));
            assertFalse(game.getTreacheryDiscard().contains(karama));
            assertEquals(spice, faction.getSpice());
            assertEquals("You will keep Karama from the raid", chat.getMessages().getLast());
            assertRaidEnded();
        }

        @Test
        void testDiscardCardGainsTwoSpice() throws Exception {
            int spice = faction.getSpice();
            int discarded = game.getTreacheryDiscard().size();
            faction.raid("Atreides");
            faction.discardRaidedCard();
            assertFalse(faction.getTreacheryHand().contains(karama));
            assertFalse(victim.getTreacheryHand().contains(karama));
            assertSame(karama, game.getTreacheryDiscard().getLast());
            assertEquals(discarded + 1, game.getTreacheryDiscard().size());
            assertEquals(spice + 2, faction.getSpice());
            assertEquals("You chose not to keep the raided Karama", chat.getMessages().getLast());
            assertRaidEnded();
        }

        @Test
        void testDecline() throws Exception {
            int spice = faction.getSpice();
            faction.offerRaidChoice(victim);
            faction.declineRaid();
            assertTrue(victim.getTreacheryHand().contains(karama));
            assertTrue(faction.getTreacheryHand().isEmpty());
            assertEquals(spice, faction.getSpice());
            assertEquals(faction.getEmoji() + " will not raid " + victim.getEmoji() + ".", turnSummary.getMessages().getLast());
            assertEquals("You will not raid", chat.getMessages().getLast());
            assertRaidEnded();
        }

        @Test
        void testVictimEmptiesHandAfterOffer() throws Exception {
            faction.offerRaidChoice(victim);
            victim.discard("Karama");
            faction.raid();
            assertEquals("Your raiders have returned, revealing the enemy had nothing to steal.", chat.getMessages().getLast());
            assertTrue(faction.getTreacheryHand().isEmpty());
            assertRaidEnded();
        }

        @Test
        void testRandomRaidTransfersExactlyOneEligibleCard() throws Exception {
            victim.addTreacheryCard(new TreacheryCard("Recruits"));
            victim.addTreacheryCard(new TreacheryCard("Baliset"));
            List<TreacheryCard> original = new ArrayList<>(victim.getTreacheryHand());
            faction.raid("Atreides");
            assertEquals(1, faction.getTreacheryHand().size());
            assertEquals(2, victim.getTreacheryHand().size());
            TreacheryCard stolen = faction.getTreacheryHand().getFirst();
            assertTrue(original.contains(stolen));
            assertFalse(victim.getTreacheryHand().contains(stolen));
            original.remove(stolen);
            assertTrue(victim.getTreacheryHand().containsAll(original));
        }

        @Test
        void testRaidAboveNormalLimitDuringInfiniteHand() throws Exception {
            addCards(5);
            faction.setInfiniteHand();
            faction.raid("Atreides");
            assertEquals(6, faction.getTreacheryHand().size());
            assertTrue(victim.getTreacheryHand().isEmpty());
        }

        @Test
        void testFailedRaidAtHandLimitDoesNotLoseVictimCard() throws Exception {
            addCards(5);
            assertThrows(IllegalStateException.class, () -> faction.raid("Atreides"));
            assertTrue(victim.getTreacheryHand().contains(karama));
            assertEquals(5, faction.getTreacheryHand().size());
            assertFalse(game.getTreacheryDiscard().contains(karama));
        }
    }

    @Nested
    @DisplayName("#setInfiniteHand / returnToNormalHandSize")
    class HandLimit {
        @Test
        void testNormalHandIsUnchanged() throws Exception {
            int messages = chat.getMessages().size();
            faction.returnToNormalHandSize();
            assertEquals(5, faction.getHandLimit());
            assertEquals(messages, chat.getMessages().size());
        }

        @Test
        void testUnrelatedHandLimitIsPreserved() throws Exception {
            faction.setHandLimit(6);
            faction.returnToNormalHandSize();
            assertEquals(6, faction.getHandLimit());
        }

        @Test
        void testDiscardChoicesAreUniqueByName() throws Exception {
            faction.setInfiniteHand();
            addCards(5);
            faction.addTreacheryCard(new TreacheryCard("Recruits"));
            int discarded = game.getTreacheryDiscard().size();
            faction.returnToNormalHandSize();
            assertEquals(100, faction.getHandLimit());
            assertEquals(6, faction.getTreacheryHand().size());
            assertEquals(discarded, game.getTreacheryDiscard().size());
            List<String> ids = latestChoiceIds();
            assertEquals(3, ids.size());
            assertEquals(Set.of("mikarrol-discard-Karama", "mikarrol-discard-Recruits", "mikarrol-finished-discarding"), Set.copyOf(ids));
            assertEquals("mikarrol-finished-discarding", ids.getLast());
        }

        @Test
        void testRestoreAfterDiscarding() throws Exception {
            faction.setInfiniteHand();
            addCards(6);
            faction.returnToNormalHandSize();
            faction.discard("Karama");
            faction.returnToNormalHandSize();
            assertEquals(5, faction.getHandLimit());
            assertEquals(5, faction.getTreacheryHand().size());
        }
    }

    @Nested
    @DisplayName("#setSlaversAmount / slavers")
    class Slavers {
        @Test
        void testOversizedSelectionPreservesPreviousValue() throws Exception {
            faction.setSlaversAmount(3);
            assertThrows(InvalidGameStateException.class, () -> faction.setSlaversAmount(21));
            assertEquals(3, faction.getSlaversAmount());
        }

        @Test
        void testSaleTransfersForcesAndPays() throws Exception {
            faction.setSlaversAmount(4);
            faction.slavers();
            assertEquals(16, faction.getReservesStrength());
            assertEquals(4, game.getTleilaxuTanks().getForceStrength("Mikarrol"));
            assertEquals(16, faction.getSpice());
            assertEquals(0, faction.getSlaversAmount());
            assertEquals("You sold 4 forces.", chat.getMessages().getLast());
            assertEquals(faction.getEmoji() + " sell 4 forces to slavers.", turnSummary.getMessages().getLast());
        }

        @Test
        void testSaleClampedAfterReservesDecrease() throws Exception {
            faction.setSlaversAmount(10);
            setRegularReserves(faction, 3);
            faction.slavers();
            assertEquals(0, faction.getReservesStrength());
            assertEquals(3, game.getTleilaxuTanks().getForceStrength("Mikarrol"));
            assertEquals(15, faction.getSpice());
            assertEquals(0, faction.getSlaversAmount());
        }

        @Test
        void testReservesExhaustedBeforeSale() throws Exception {
            faction.setSlaversAmount(4);
            setRegularReserves(faction, 0);
            faction.slavers();
            assertEquals(12, faction.getSpice());
            assertEquals(0, game.getTleilaxuTanks().getForceStrength("Mikarrol"));
            assertEquals(0, faction.getSlaversAmount());
        }

        @Test
        void testSaleCrossesLowThreshold() throws Exception {
            game.addGameOption(GameOption.HOMEWORLDS);
            faction.setSlaversAmount(15);
            faction.slavers();
            assertEquals(5, faction.getReservesStrength());
            assertFalse(faction.isHighThreshold());
        }

        @Test
        void testSaleAtSixKeepsHighThreshold() throws Exception {
            game.addGameOption(GameOption.HOMEWORLDS);
            faction.setSlaversAmount(14);
            faction.slavers();
            assertEquals(6, faction.getReservesStrength());
            assertTrue(faction.isHighThreshold());
        }

        @Test
        void testRejectNegativeSelection() throws Exception {
            faction.setSlaversAmount(3);
            assertThrows(InvalidGameStateException.class, () -> faction.setSlaversAmount(-1));
            assertEquals(3, faction.getSlaversAmount());
        }
    }

    @Nested
    @DisplayName("#setAllySlaversAmount / allySlavers / setAlly")
    class AllySlavers {
        private AtreidesFaction ally;

        @BeforeEach
        void setUpAlly() throws IOException {
            ally = addOpponent(new AtreidesFaction("at", "at"));
            faction.setAlly("Atreides");
            setRegularReserves(ally, 10);
        }

        @Test
        void testRejectOversizedSelection() throws Exception {
            faction.setAllySlaversAmount(3);
            assertThrows(InvalidGameStateException.class, () -> faction.setAllySlaversAmount(11));
            assertEquals(3, faction.getAllySlaversAmount());
        }

        @Test
        void testRegularSalePaysOnlyAlly() throws Exception {
            int spice = ally.getSpice();
            faction.setAllySlaversAmount(4);
            faction.allySlavers();
            assertEquals(6, ally.getReservesStrength());
            assertEquals(4, game.getTleilaxuTanks().getForceStrength("Atreides"));
            assertEquals(spice + 4, ally.getSpice());
            assertEquals(12, faction.getSpice());
            assertEquals(20, faction.getReservesStrength());
            assertEquals(0, faction.getAllySlaversAmount());
            assertEquals(ally.getEmoji() + " sell 4 forces to slavers, through " + faction.getEmoji() + ".", turnSummary.getMessages().getLast());
        }

        @Test
        void testUsesAllySelectionNotOwnSelection() throws Exception {
            faction.setSlaversAmount(2);
            faction.setAllySlaversAmount(5);
            faction.allySlavers();
            assertEquals(5, ally.getReservesStrength());
            assertEquals(5, game.getTleilaxuTanks().getForceStrength("Atreides"));
            assertEquals(2, faction.getSlaversAmount());
        }

        @Test
        void testClampAfterReservesDecrease() throws Exception {
            int spice = ally.getSpice();
            faction.setAllySlaversAmount(8);
            setRegularReserves(ally, 2);
            faction.allySlavers();
            assertEquals(0, ally.getReservesStrength());
            assertEquals(2, game.getTleilaxuTanks().getForceStrength("Atreides"));
            assertEquals(spice + 2, ally.getSpice());
            assertEquals(0, faction.getAllySlaversAmount());
        }

        @Test
        void testChangingAllyClearsSelection() throws Exception {
            addOpponent(new FremenFaction("fr", "fr"));
            faction.setAllySlaversAmount(4);
            faction.setSlaversAmount(2);
            faction.setAlly("Fremen");
            assertEquals(0, faction.getAllySlaversAmount());
            assertEquals(2, faction.getSlaversAmount());
            faction.allySlavers();
            assertEquals(10, ally.getReservesStrength());
        }

        @Test
        void testPendingSaleWithoutAllyDoesNotExecute() throws Exception {
            int spice = ally.getSpice();
            faction.setAllySlaversAmount(4);
            faction.ally = null;
            faction.allySlavers();
            assertEquals(10, ally.getReservesStrength());
            assertEquals(spice, ally.getSpice());
        }

        @Test
        void testRejectNegativeSelection() throws Exception {
            faction.setAllySlaversAmount(3);
            assertThrows(InvalidGameStateException.class, () -> faction.setAllySlaversAmount(-1));
            assertEquals(3, faction.getAllySlaversAmount());
        }

        @Test
        void testFremenExactRegularBoundary() throws Exception {
            FremenFaction specialAlly = addOpponent(new FremenFaction("special", "special"));
            faction.setAlly("Fremen");
            setRegularReserves(specialAlly, 4);
            setSpecialReserves(specialAlly, 3);
            int spice = specialAlly.getSpice();
            int regularTanks = game.getTleilaxuTanks().getForceStrength("Fremen");
            int specialTanks = game.getTleilaxuTanks().getForceStrength("Fremen*");
            faction.setAllySlaversAmount(4);
            faction.allySlavers();
            assertAll(
                    () -> assertEquals(0, specialAlly.getReservesStrength()),
                    () -> assertEquals(3, specialAlly.getSpecialReservesStrength()),
                    () -> assertEquals(regularTanks + 4, game.getTleilaxuTanks().getForceStrength("Fremen")),
                    () -> assertEquals(specialTanks + 0, game.getTleilaxuTanks().getForceStrength("Fremen*")),
                    () -> assertEquals(spice + 4, specialAlly.getSpice()),
                    () -> assertEquals(0, faction.getAllySlaversAmount()));
        }

        @Test
        void testFremenPartialSpecial() throws Exception {
            FremenFaction specialAlly = addOpponent(new FremenFaction("special", "special"));
            faction.setAlly("Fremen");
            setRegularReserves(specialAlly, 2);
            setSpecialReserves(specialAlly, 3);
            int spice = specialAlly.getSpice();
            int regularTanks = game.getTleilaxuTanks().getForceStrength("Fremen");
            int specialTanks = game.getTleilaxuTanks().getForceStrength("Fremen*");
            faction.setAllySlaversAmount(3);
            faction.allySlavers();
            assertAll(
                    () -> assertEquals(0, specialAlly.getReservesStrength()),
                    () -> assertEquals(2, specialAlly.getSpecialReservesStrength()),
                    () -> assertEquals(regularTanks + 2, game.getTleilaxuTanks().getForceStrength("Fremen")),
                    () -> assertEquals(specialTanks + 1, game.getTleilaxuTanks().getForceStrength("Fremen*")),
                    () -> assertEquals(spice + 3, specialAlly.getSpice()),
                    () -> assertEquals(0, faction.getAllySlaversAmount()));
        }

        @Test
        void testFremenSpecialOnlyPartial() throws Exception {
            FremenFaction specialAlly = addOpponent(new FremenFaction("special", "special"));
            faction.setAlly("Fremen");
            setRegularReserves(specialAlly, 0);
            setSpecialReserves(specialAlly, 3);
            int spice = specialAlly.getSpice();
            int regularTanks = game.getTleilaxuTanks().getForceStrength("Fremen");
            int specialTanks = game.getTleilaxuTanks().getForceStrength("Fremen*");
            faction.setAllySlaversAmount(1);
            faction.allySlavers();
            assertAll(
                    () -> assertEquals(0, specialAlly.getReservesStrength()),
                    () -> assertEquals(2, specialAlly.getSpecialReservesStrength()),
                    () -> assertEquals(regularTanks + 0, game.getTleilaxuTanks().getForceStrength("Fremen")),
                    () -> assertEquals(specialTanks + 1, game.getTleilaxuTanks().getForceStrength("Fremen*")),
                    () -> assertEquals(spice + 1, specialAlly.getSpice()),
                    () -> assertEquals(0, faction.getAllySlaversAmount()));
        }

        @Test
        void testFremenAllReserves() throws Exception {
            FremenFaction specialAlly = addOpponent(new FremenFaction("special", "special"));
            faction.setAlly("Fremen");
            setRegularReserves(specialAlly, 2);
            setSpecialReserves(specialAlly, 3);
            int spice = specialAlly.getSpice();
            int regularTanks = game.getTleilaxuTanks().getForceStrength("Fremen");
            int specialTanks = game.getTleilaxuTanks().getForceStrength("Fremen*");
            faction.setAllySlaversAmount(5);
            faction.allySlavers();
            assertAll(
                    () -> assertEquals(0, specialAlly.getReservesStrength()),
                    () -> assertEquals(0, specialAlly.getSpecialReservesStrength()),
                    () -> assertEquals(regularTanks + 2, game.getTleilaxuTanks().getForceStrength("Fremen")),
                    () -> assertEquals(specialTanks + 3, game.getTleilaxuTanks().getForceStrength("Fremen*")),
                    () -> assertEquals(spice + 5, specialAlly.getSpice()),
                    () -> assertEquals(0, faction.getAllySlaversAmount()));
        }

        @Test
        void testEmperorPartialSpecial() throws Exception {
            EmperorFaction specialAlly = addOpponent(new EmperorFaction("special", "special"));
            faction.setAlly("Emperor");
            setRegularReserves(specialAlly, 2);
            setSpecialReserves(specialAlly, 3);
            int spice = specialAlly.getSpice();
            int regularTanks = game.getTleilaxuTanks().getForceStrength("Emperor");
            int specialTanks = game.getTleilaxuTanks().getForceStrength("Emperor*");
            faction.setAllySlaversAmount(3);
            faction.allySlavers();
            assertAll(
                    () -> assertEquals(0, specialAlly.getReservesStrength()),
                    () -> assertEquals(2, specialAlly.getSpecialReservesStrength()),
                    () -> assertEquals(regularTanks + 2, game.getTleilaxuTanks().getForceStrength("Emperor")),
                    () -> assertEquals(specialTanks + 1, game.getTleilaxuTanks().getForceStrength("Emperor*")),
                    () -> assertEquals(spice + 3, specialAlly.getSpice()),
                    () -> assertEquals(0, faction.getAllySlaversAmount()));
        }

        @Test
        void testEmperorSpecialOnlyPartial() throws Exception {
            EmperorFaction specialAlly = addOpponent(new EmperorFaction("special", "special"));
            faction.setAlly("Emperor");
            setRegularReserves(specialAlly, 0);
            setSpecialReserves(specialAlly, 3);
            int spice = specialAlly.getSpice();
            int regularTanks = game.getTleilaxuTanks().getForceStrength("Emperor");
            int specialTanks = game.getTleilaxuTanks().getForceStrength("Emperor*");
            faction.setAllySlaversAmount(1);
            faction.allySlavers();
            assertAll(
                    () -> assertEquals(0, specialAlly.getReservesStrength()),
                    () -> assertEquals(2, specialAlly.getSpecialReservesStrength()),
                    () -> assertEquals(regularTanks + 0, game.getTleilaxuTanks().getForceStrength("Emperor")),
                    () -> assertEquals(specialTanks + 1, game.getTleilaxuTanks().getForceStrength("Emperor*")),
                    () -> assertEquals(spice + 1, specialAlly.getSpice()),
                    () -> assertEquals(0, faction.getAllySlaversAmount()));
        }
    }

    @Nested
    @DisplayName("#replenishment")
    class Replenishment {
        @Test
        void testLowDefeatWith8InTanks() throws Exception {
            game.addGameOption(GameOption.HOMEWORLDS);
            setRegularReserves(faction, 5);
            faction.checkForLowThreshold();
            assertEquals(false, faction.isHighThreshold());
            game.getTleilaxuTanks().addForces("Mikarrol", 8);
            int reserves = faction.getReservesStrength();
            int spice = faction.getSpice();
            int messages = turnSummary.getMessages().size();
            faction.replenishment(false);
            assertEquals(reserves + 2, faction.getReservesStrength());
            assertEquals(6, game.getTleilaxuTanks().getForceStrength("Mikarrol"));
            assertEquals(spice, faction.getSpice());
            assertEquals(faction.getEmoji() + " acquire 2 troops to replenish their forces after battle.", turnSummary.getMessages().getLast());
        }

        @Test
        void testLowVictoryWith8InTanks() throws Exception {
            game.addGameOption(GameOption.HOMEWORLDS);
            setRegularReserves(faction, 5);
            faction.checkForLowThreshold();
            assertEquals(false, faction.isHighThreshold());
            game.getTleilaxuTanks().addForces("Mikarrol", 8);
            int reserves = faction.getReservesStrength();
            int spice = faction.getSpice();
            int messages = turnSummary.getMessages().size();
            faction.replenishment(true);
            assertEquals(reserves + 4, faction.getReservesStrength());
            assertEquals(4, game.getTleilaxuTanks().getForceStrength("Mikarrol"));
            assertEquals(spice, faction.getSpice());
            assertEquals(faction.getEmoji() + " acquire 4 troops to replenish their forces after battle.", turnSummary.getMessages().getLast());
        }

        @Test
        void testHighDefeatWith8InTanks() throws Exception {
            game.addGameOption(GameOption.HOMEWORLDS);
            setRegularReserves(faction, 10);
            faction.checkForLowThreshold();
            assertEquals(true, faction.isHighThreshold());
            game.getTleilaxuTanks().addForces("Mikarrol", 8);
            int reserves = faction.getReservesStrength();
            int spice = faction.getSpice();
            int messages = turnSummary.getMessages().size();
            faction.replenishment(false);
            assertEquals(reserves + 3, faction.getReservesStrength());
            assertEquals(5, game.getTleilaxuTanks().getForceStrength("Mikarrol"));
            assertEquals(spice, faction.getSpice());
            assertEquals(faction.getEmoji() + " acquire 3 troops to replenish their forces after battle.", turnSummary.getMessages().getLast());
        }

        @Test
        void testHighVictoryWith0InTanks() throws Exception {
            game.addGameOption(GameOption.HOMEWORLDS);
            setRegularReserves(faction, 10);
            faction.checkForLowThreshold();
            assertEquals(true, faction.isHighThreshold());
            game.getTleilaxuTanks().addForces("Mikarrol", 0);
            int reserves = faction.getReservesStrength();
            int spice = faction.getSpice();
            int messages = turnSummary.getMessages().size();
            faction.replenishment(true);
            assertEquals(reserves + 0, faction.getReservesStrength());
            assertEquals(0, game.getTleilaxuTanks().getForceStrength("Mikarrol"));
            assertEquals(spice, faction.getSpice());
            assertEquals(messages, turnSummary.getMessages().size());
        }

        @Test
        void testHighVictoryWith1InTanks() throws Exception {
            game.addGameOption(GameOption.HOMEWORLDS);
            setRegularReserves(faction, 10);
            faction.checkForLowThreshold();
            assertEquals(true, faction.isHighThreshold());
            game.getTleilaxuTanks().addForces("Mikarrol", 1);
            int reserves = faction.getReservesStrength();
            int spice = faction.getSpice();
            int messages = turnSummary.getMessages().size();
            faction.replenishment(true);
            assertEquals(reserves + 1, faction.getReservesStrength());
            assertEquals(0, game.getTleilaxuTanks().getForceStrength("Mikarrol"));
            assertEquals(spice, faction.getSpice());
            assertEquals(faction.getEmoji() + " acquire 1 troops to replenish their forces after battle.", turnSummary.getMessages().getLast());
        }

        @Test
        void testHighVictoryWith8InTanks() throws Exception {
            game.addGameOption(GameOption.HOMEWORLDS);
            setRegularReserves(faction, 10);
            faction.checkForLowThreshold();
            assertEquals(true, faction.isHighThreshold());
            game.getTleilaxuTanks().addForces("Mikarrol", 8);
            int reserves = faction.getReservesStrength();
            int spice = faction.getSpice();
            int messages = turnSummary.getMessages().size();
            faction.replenishment(true);
            assertEquals(reserves + 5, faction.getReservesStrength());
            assertEquals(3, game.getTleilaxuTanks().getForceStrength("Mikarrol"));
            assertEquals(spice, faction.getSpice());
            assertEquals(faction.getEmoji() + " acquire 5 troops to replenish their forces after battle.", turnSummary.getMessages().getLast());
        }
    }

    @Nested
    @DisplayName("#performEndOfPhaseActions")
    class EndOfPhase {
        @Test
        void testExecutesBothSalesAndRestoresHandLimit() throws Exception {
            AtreidesFaction ally = addOpponent(new AtreidesFaction("at", "at"));
            faction.setAlly("Atreides");
            int allyReserves = ally.getReservesStrength();
            int allySpice = ally.getSpice();
            faction.setSlaversAmount(2);
            faction.setAllySlaversAmount(3);
            faction.setInfiniteHand();
            faction.performEndOfPhaseActions();
            assertEquals(18, faction.getReservesStrength());
            assertEquals(14, faction.getSpice());
            assertEquals(allyReserves - 3, ally.getReservesStrength());
            assertEquals(allySpice + 3, ally.getSpice());
            assertEquals(2, game.getTleilaxuTanks().getForceStrength("Mikarrol"));
            assertEquals(3, game.getTleilaxuTanks().getForceStrength("Atreides"));
            assertEquals(0, faction.getSlaversAmount());
            assertEquals(0, faction.getAllySlaversAmount());
            assertEquals(5, faction.getHandLimit());
            faction.performEndOfPhaseActions();
            assertEquals(14, faction.getSpice());
            assertEquals(allySpice + 3, ally.getSpice());
        }

    }
}
