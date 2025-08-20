package model.factions;

import constants.Emojis;
import enums.GameOption;
import enums.UpdateType;
import exceptions.InvalidGameStateException;
import model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

abstract class FactionTestTemplate {
    Game game;
    TestTopic turnSummary;
    TestTopic gameActions;
    Faction faction;
    TestTopic chat;
    TestTopic ledger;

    abstract Faction getFaction();

    @BeforeEach
    void baseSetUp() throws IOException {
        game = new Game();
        turnSummary = new TestTopic();
        game.setTurnSummary(turnSummary);
        gameActions = new TestTopic();
        game.setGameActions(gameActions);
        chat = new TestTopic();
        ledger = new TestTopic();
    }

    void commonPostInstantiationSetUp() {
        faction = getFaction();
        faction.setChat(chat);
        faction.setLedger(ledger);
        game.addFaction(faction);
    }

    void throwTestTopicMessages(TestTopic topic) {
        throw new RuntimeException("\n- " + String.join("\n- ", topic.getMessages()));
    }

    @Nested
    @DisplayName("#getInfoChannelPrefix")
    class GetInfoChannelPrefix {
        Faction faction;

        @BeforeEach
        public void setUp() {
            faction = getFaction();
        }

        @Test
        public void testNullPrefix() {
            faction.setInfoChannelPrefix(null);
            assertEquals(faction.name.toLowerCase(), faction.getInfoChannelPrefix());
        }

        @Test
        public void testPrefix() {
            faction.setInfoChannelPrefix("some-prefix");
            assertEquals("some-prefix", faction.getInfoChannelPrefix());
        }
    }

    @Nested
    @DisplayName("#assignSkillToLeader")
    class AssignSkillToLeader {
        Leader leader;
        LeaderSkillCard mentat;
        LeaderSkillCard swordmaster;

        @BeforeEach
        public void setUp() throws InvalidGameStateException {
            mentat = game.getLeaderSkillDeck().stream().filter(ls -> ls.name().equals("Mentat")).findFirst().orElseThrow();
            swordmaster = game.getLeaderSkillDeck().stream().filter(ls -> ls.name().equals("Swordmaster of Ginaz")).findFirst().orElseThrow();
            game.getLeaderSkillDeck().remove(mentat);
            faction.getLeaderSkillsHand().add(mentat);
            game.getLeaderSkillDeck().remove(swordmaster);
            faction.getLeaderSkillsHand().add(swordmaster);
            leader = faction.getLeaders().getFirst();
            int numLeaders = faction.getLeaders().size();
            faction.assignSkillToLeader(leader.getName(), "Mentat");
            assertEquals(numLeaders, faction.getLeaders().size());
        }

        @Test
        public void testLeaderHasSkill() {
            Leader newLeader = faction.getLeader(leader.getName()).orElseThrow();
            assertEquals(mentat, newLeader.getSkillCard());
            assertEquals(mentat, leader.getSkillCard());
        }

        @Test
        public void testLeaderSkillsHandEmpty() {
            assertTrue(faction.getLeaderSkillsHand().isEmpty());
        }

        @Test
        public void testSwordmasterBackInSkillsDeck() {
            assertTrue(game.getLeaderSkillDeck().contains(swordmaster));
        }

        @Test
        public void testPublishedToFactionChat() {
            assertEquals("After years of training, " + leader.getName() + " has become a Mentat!", chat.getMessages().getLast());
        }
    }

    @Nested
    @DisplayName("#revival")
    class Revival {
        Faction faction;
        int freeRevivals;

        @BeforeEach
        public void setUp() throws InvalidGameStateException {
            faction = getFaction();
            freeRevivals =  faction.getFreeRevival();
            faction.setChat(chat);
            faction.setLedger(ledger);
            game.startRevival();
        }

        @Test
        public void testInitialMaxRevival() {
            assertEquals(3, faction.getMaxRevival());
        }

        @Test
        public void testMaxRevivalSetTo5() {
            faction.setMaxRevival(5);
            assertEquals(5, faction.getMaxRevival());
        }

        @Test
        public void testMaxRevivalWithRecruits() throws InvalidGameStateException {
            game.getRevival().setRecruitsInPlay(true);
            assertEquals(7, faction.getMaxRevival());
        }

        @Test
        public void testRevivalCost() {
            assertEquals(4, faction.revivalCost(1, 1));
        }

        @Test
        public void testRevivalCostAlliedWithBT() throws IOException {
            game.addFaction(new BTFaction("bt", "bt"));
            faction.setAlly("BT");
            assertEquals(2, faction.revivalCost(1, 1));
        }

        @Test
        public void testFreeReviveStars() {
            assertThrows(IllegalArgumentException.class, () -> faction.removeForces(faction.getHomeworld(), 1, true, true));
        }

        @Test
        public void testPaidRevivalChoices() throws InvalidGameStateException {
            faction.removeForces(faction.getHomeworld(), 5, false, true);
            faction.performFreeRevivals();
            faction.presentPaidRevivalChoices(freeRevivals);
            assertEquals(1, chat.getMessages().size());
            assertEquals(1, chat.getChoices().size());
            assertEquals(3 - freeRevivals + 1, chat.getChoices().getFirst().size());
        }

        @Test
        public void testPaidRevivalChoicesNoForcesInTanks() throws InvalidGameStateException {
            faction.presentPaidRevivalChoices(0);
            assertEquals(0, chat.getMessages().size());
            assertEquals(0, chat.getChoices().size());
            assertEquals(faction.getEmoji() + " has no forces in the tanks", faction.getPaidRevivalMessage());
        }

        @Test
        public void testPaidRevivalChoicesInsufficientSpice() throws InvalidGameStateException {
            faction.subtractSpice(faction.getSpice(), "Test");
            faction.setMaxRevival(5);
            faction.removeForces(faction.getHomeworld(), 5, false, true);
            faction.performFreeRevivals();
            faction.presentPaidRevivalChoices(freeRevivals);
            assertEquals(1, chat.getMessages().size());
//            assertEquals("You do not have enough " + Emojis.SPICE + " to purchase additional revivals.", chat.getMessages().getFirst());
//            assertEquals(0, chat.getChoices().size());
            assertEquals(1, chat.getChoices().size());
            assertFalse(chat.getChoices().getFirst().getFirst().isDisabled());
            assertTrue(chat.getChoices().getFirst().get(1).isDisabled());
        }

        @Test
        public void testPaidRevival() {
            faction.removeForces(faction.getHomeworld(), 5, false, true);
            game.reviveForces(faction, false, 3, 0);
            assertEquals(faction.getEmoji() + " revives 3 " + Emojis.getForceEmoji(faction.getName()) + " for free.", turnSummary.getMessages().getLast());
        }

        @Test
        public void testPaidRevivalMessageAfter3Free() throws InvalidGameStateException {
            faction.removeForces(faction.getHomeworld(), 5, false, true);
//            faction.performFreeRevivals();
            // The following should be replaced with performFreeRevivals after setting Fremen as ally, override for Fremen
            game.reviveForces(faction, false, 3, 0);
            faction.presentPaidRevivalChoices(3);
            assertEquals(0, chat.getMessages().size());
            assertEquals(0, chat.getChoices().size());
            assertEquals(faction.getEmoji() + " has revived their maximum", faction.getPaidRevivalMessage());
        }

        @Test
        public void testPaidRevivalMessageAfter1StarFree() throws InvalidGameStateException {
            assertThrows(IllegalArgumentException.class, () -> faction.removeForces(faction.getHomeworld(), 2, true, true));
//            faction.removeForces(faction.getHomeworld(), 2, true, true);
//            faction.performFreeRevivals();
//            faction.presentPaidRevivalChoices(1);
//            assertEquals(0, chat.getMessages().size());
//            assertEquals(0, chat.getChoices().size());
//            assertEquals(faction.getEmoji() + " has revived their maximum", turnSummary.getMessages().get(1));
        }

        @Test
        public void testPaidRevivalAfter3FreeButBTRaisedLimit() throws InvalidGameStateException {
            faction.setMaxRevival(5);
            faction.removeForces(faction.getHomeworld(), 5, false, true);
//            faction.performFreeRevivals();
            // The following should be replaced with performFreeRevivals after setting Fremen as ally, override for Fremen
            game.reviveForces(faction, false, 3, 0);
            int numRevived = 3;
            faction.presentPaidRevivalChoices(numRevived);
            assertEquals(1, chat.getMessages().size());
            assertEquals(1, chat.getMessages().size());
            assertEquals(1, chat.getChoices().size());
            assertEquals(5 - numRevived + 1, chat.getChoices().getFirst().size());
        }
    }

    @Nested
    @DisplayName("#removeLeader")
    class RemoveLeader {
        Leader leader;

        @BeforeEach
        public void setUp() {
            leader = faction.getLeaders().getFirst();
        }

        @Test
        public void testRemoveLeader() {
            faction.removeLeader(leader);
            assertFalse(faction.getLeaders().contains(leader));
            assertTrue(faction.getUpdateTypes().contains(UpdateType.MISC_BACK_OF_SHIELD));
            assertFalse(faction.getUpdateTypes().contains(UpdateType.MISC_FRONT_OF_SHIELD));
        }

        @Test
        public void testRemoveSkilledLeader() throws InvalidGameStateException {
            leader.setSkillCard(new LeaderSkillCard("Mentat"));
            faction.removeLeader(leader);
            assertFalse(faction.getLeaders().contains(leader));
            assertTrue(faction.getUpdateTypes().contains(UpdateType.MISC_BACK_OF_SHIELD));
            assertTrue(faction.getUpdateTypes().contains(UpdateType.MISC_FRONT_OF_SHIELD));
        }
    }

    @Nested
    @DisplayName("#removeLeaderByName")
    class RemoveLeaderByName {
        String leaderName;

        @BeforeEach
        public void setUp() {
            leaderName = faction.getLeaders().getFirst().getName();
        }

        @Test
        public void testRemoveLeader() {
            faction.removeLeader(leaderName);
            assertFalse(faction.getLeaders().stream().anyMatch(l -> l.getName().equals(leaderName)));
            assertTrue(faction.getUpdateTypes().contains(UpdateType.MISC_BACK_OF_SHIELD));
            assertFalse(faction.getUpdateTypes().contains(UpdateType.MISC_FRONT_OF_SHIELD));
        }

        @Test
        public void testRemoveSkilledLeader() throws InvalidGameStateException {
            faction.getLeader(leaderName).orElseThrow().setSkillCard(new LeaderSkillCard("Mentat"));
            faction.removeLeader(leaderName);
            assertFalse(faction.getLeaders().stream().anyMatch(l -> l.getName().equals(leaderName)));
            assertTrue(faction.getUpdateTypes().contains(UpdateType.MISC_BACK_OF_SHIELD));
            assertTrue(faction.getUpdateTypes().contains(UpdateType.MISC_FRONT_OF_SHIELD));
        }
    }

    @Nested
    @DisplayName("#reviveLeader")
    class ReviveLeader {
        Faction faction;
        Leader leader;
        int leaderValue;
        TestTopic ledger;

        @BeforeEach
        public void setUp() {
            faction = getFaction();
            ledger = new TestTopic();
            faction.setLedger(ledger);
            leader = faction.getLeaders().getFirst();
            leaderValue = leader.getAssassinationValue();
            game.killLeader(faction, leader.getName());
            assertFalse(faction.getLeaders().contains(leader));
            assertTrue(game.getLeaderTanks().contains(leader));

            ledger = new TestTopic();
            faction.setLedger(ledger);
            turnSummary = new TestTopic();
            game.setTurnSummary(turnSummary);
        }

        @Test
        public void testLeaderIsNotInTheTanks() {
            Leader leader2 = faction.getLeaders().getFirst();
            assertNotEquals(leader, leader2);
            assertThrows(IllegalArgumentException.class, () -> faction.reviveLeader(leader2.getName(), null));
        }

        @Test
        public void testZeroAlternativeCostAllowedForAll() {
            assertDoesNotThrow(() -> faction.reviveLeader(leader.getName(), 0));
        }

        @Test
        public void testNoAlternativeCostBTNotInGameOrFactionIsBT() {
            assertThrows(IllegalArgumentException.class, () -> faction.reviveLeader(leader.getName(), 1));
        }

        @Nested
        @DisplayName("#payStandardCost")
        class PayStandardCost {
            @BeforeEach
            public void setUp() throws InvalidGameStateException {
                faction.subtractSpice(faction.getSpice(), "Test");
                faction.addSpice(10, "Test");
                ledger.clear();
                faction.reviveLeader(leader.getName(), null);
                assertTrue(faction.getLeaders().contains(leader));
                assertFalse(game.getLeaderTanks().contains(leader));
            }

            @Test
            public void testSpiceReducedByCost() {
                assertEquals(10 - leaderValue, faction.getSpice());
            }

            @Test
            public void testLedgerMessage() {
                assertEquals("-" + leaderValue + " " + Emojis.SPICE + " revive " + leader.getName() + " = " + faction.getSpice() + " " + Emojis.SPICE, ledger.getMessages().getFirst());
            }

            @Test
            public void testTurnSummaryMessage() {
                assertEquals(faction.getEmoji() + " " + leader.getName() + " was revived from the tanks for " + leaderValue + " " + Emojis.SPICE, turnSummary.getMessages().getFirst());
            }
        }

        @Nested
        @DisplayName("#payStandardCostToBT")
        class PayStandardCostToBT {
            BTFaction bt;
            TestTopic btLedger;

            @BeforeEach
            public void setUp() throws InvalidGameStateException, IOException {
                bt = new BTFaction("p", "u");
                game.addFaction(bt);
                btLedger = new TestTopic();
                bt.setLedger(btLedger);
                bt.subtractSpice(bt.getSpice(), "Test");
                btLedger.clear();
                game.addFaction(bt);
                faction.subtractSpice(faction.getSpice(), "Test");
                faction.addSpice(10, "Test");
                ledger.clear();
                faction.reviveLeader(leader.getName(), null);
                assertTrue(faction.getLeaders().contains(leader));
                assertFalse(game.getLeaderTanks().contains(leader));
            }

            @Test
            public void testSpiceReducedByCost() {
                assertEquals(10 - leaderValue, faction.getSpice());
            }

            @Test
            public void testBTGetPaid() {
                assertEquals(leaderValue, bt.getSpice());
            }

            @Test
            public void testBTLedgerMessage() {
                assertEquals("+" + leaderValue + " " + Emojis.SPICE + " " + faction.getEmoji() + " revived " + leader.getName() + " = " + bt.getSpice() + " " + Emojis.SPICE, btLedger.getMessages().getFirst());
            }

            @Test
            public void testLedgerMessage() {
                assertEquals("-" + leaderValue + " " + Emojis.SPICE + " revive " + leader.getName() + " = " + faction.getSpice() + " " + Emojis.SPICE, ledger.getMessages().getFirst());
            }

            @Test
            public void testTurnSummaryMessage() {
                assertEquals(faction.getEmoji() + " " + leader.getName() + " was revived from the tanks for " + leaderValue + " " + Emojis.SPICE + " paid to " + Emojis.BT, turnSummary.getMessages().getFirst());
            }
        }

        @Nested
        @DisplayName("#payStandardCostToBT")
        class AlliedWithBT {
            BTFaction bt;
            TestTopic btLedger;

            @BeforeEach
            public void setUp() throws InvalidGameStateException, IOException {
                bt = new BTFaction("p", "u");
                game.addFaction(bt);
                btLedger = new TestTopic();
                bt.setLedger(btLedger);
                bt.subtractSpice(bt.getSpice(), "Test");
                game.addFaction(bt);
                game.createAlliance(faction, bt);
                ledger = new TestTopic();
                faction.setLedger(ledger);
                btLedger = new TestTopic();
                bt.setLedger(btLedger);
                turnSummary = new TestTopic();
                game.setTurnSummary(turnSummary);
                faction.subtractSpice(faction.getSpice(), "Test");
                faction.addSpice(10, "Test");
                ledger.clear();
                faction.reviveLeader(leader.getName(), null);
                assertTrue(faction.getLeaders().contains(leader));
                assertFalse(game.getLeaderTanks().contains(leader));
            }

            @Test
            public void testSpiceReducedByCost() {
                assertEquals(10 - Math.ceilDiv(leaderValue, 2), faction.getSpice());
            }

            @Test
            public void testBTGetPaid() {
                assertEquals(Math.ceilDiv(leaderValue, 2), bt.getSpice());
            }

            @Test
            public void testBTLedgerMessage() {
                assertEquals("+" + Math.ceilDiv(leaderValue, 2) + " " + Emojis.SPICE + " " + faction.getEmoji() + " revived " + leader.getName() + " = " + bt.getSpice() + " " + Emojis.SPICE, btLedger.getMessages().getFirst());
            }

            @Test
            public void testLedgerMessage() {
                assertEquals("-" + Math.ceilDiv(leaderValue, 2) + " " + Emojis.SPICE + " revive " + leader.getName() + " = " + faction.getSpice() + " " + Emojis.SPICE, ledger.getMessages().getFirst());
            }

            @Test
            public void testTurnSummaryMessage() {
                assertEquals(faction.getEmoji() + " " + leader.getName() + " was revived from the tanks for " + Math.ceilDiv(leaderValue, 2) + " " + Emojis.SPICE + " paid to " + Emojis.BT, turnSummary.getMessages().getFirst());
            }
        }

        @Nested
        @DisplayName("#payBTAlternativeCost")
        class PayBTAlternativeCost {
            BTFaction bt;
            TestTopic btLedger;

            @BeforeEach
            public void setUp() throws InvalidGameStateException, IOException {
                bt = new BTFaction("p", "u");
                game.addFaction(bt);
                btLedger = new TestTopic();
                bt.setLedger(btLedger);
                bt.subtractSpice(bt.getSpice(), "Test");
                btLedger.clear();
                game.addFaction(bt);
                faction.subtractSpice(faction.getSpice(), "Test");
                faction.addSpice(10, "Test");
                ledger.clear();
                faction.reviveLeader(leader.getName(), 7);
                assertTrue(faction.getLeaders().contains(leader));
                assertFalse(game.getLeaderTanks().contains(leader));
            }

            @Test
            public void testSpiceReducedByCost() {
                assertEquals(3, faction.getSpice());
            }

            @Test
            public void testBTGetPaid() {
                assertEquals(7, bt.getSpice());
            }

            @Test
            public void testBTLedgerMessage() {
                assertEquals("+7 " + Emojis.SPICE + " " + faction.getEmoji() + " revived " + leader.getName() + " = " + bt.getSpice() + " " + Emojis.SPICE, btLedger.getMessages().getFirst());
            }

            @Test
            public void testLedgerMessage() {
                assertEquals("-7 " + Emojis.SPICE + " revive " + leader.getName() + " = " + faction.getSpice() + " " + Emojis.SPICE, ledger.getMessages().getFirst());
            }

            @Test
            public void testTurnSummaryMessage() {
                assertEquals(faction.getEmoji() + " " + leader.getName() + " was revived from the tanks for 7 " + Emojis.SPICE + " paid to " + Emojis.BT, turnSummary.getMessages().getFirst());
            }
        }

        @Test
        public void testFactionDoesNotHaveEnoughSpice() {
            faction.subtractSpice(faction.getSpice(), "Test");
            assertThrows(InvalidGameStateException.class, () -> faction.reviveLeader(leader.getName(), null));
            assertFalse(faction.getLeaders().contains(leader));
            assertTrue(game.getLeaderTanks().contains(leader));
        }
    }

    @Nested
    @DisplayName("#homeworld")
    class Homeworld {
        String homeworldName;
        HomeworldTerritory territory;

        @BeforeEach
        public void setUp() throws IOException {
            game.addFaction(new HarkonnenFaction("p", "u"));
            game.addFaction(new EmperorFaction("p", "u"));
            homeworldName = getFaction().getHomeworld();
            territory = (HomeworldTerritory) game.getTerritories().get(homeworldName);
        }

        @Test
        public void testHomeworld() {
            assertNotNull(territory);
            assertEquals(homeworldName, territory.getTerritoryName());
            assertEquals(-1, territory.getSector());
            assertFalse(territory.isStronghold());
            assertInstanceOf(HomeworldTerritory.class, territory);
            assertFalse(territory.isDiscoveryToken());
            assertFalse(territory.isNearShieldWall());
            assertFalse(territory.isRock());
        }

        @Test
        public void testHomweworldDialAdvantageHighThreshold() {
            assertTrue(getFaction().isHighThreshold());
            assertEquals(0, getFaction().homeworldDialAdvantage(game, territory));
            game.addGameOption(GameOption.HOMEWORLDS);
            assertEquals(2, getFaction().homeworldDialAdvantage(game, territory));
        }

        @Test
        public void testHomweworldDialAdvantageLowThreshold() {
            int numForces = territory.getForceStrength(getFaction().getName());
            int numSpecials = territory.getForceStrength(getFaction().getName() + "*");
            game.removeForces(territory.getTerritoryName(), getFaction(), numForces, numSpecials, true);
            assertEquals(0, getFaction().homeworldDialAdvantage(game, territory));
            game.addGameOption(GameOption.HOMEWORLDS);
            getFaction().checkForLowThreshold();
            assertFalse(getFaction().isHighThreshold());
            assertEquals(2, getFaction().homeworldDialAdvantage(game, territory));
        }

        @Test
        public void testNativeStartsAtHighThreshold() {
            assertTrue(getFaction().isHighThreshold());
        }

        @Test
        public void testNoReturnToHighThresholdWhileOccupied() {
            game.addGameOption(GameOption.HOMEWORLDS);
            int strength = territory.getForceStrength(getFaction().getName());
            territory.removeForces(game, getFaction().getName(), territory.getForceStrength(getFaction().getName()));
            territory.removeForces(game, getFaction().getName() + "*", territory.getForceStrength(getFaction().getName() + "*"));
            getFaction().checkForLowThreshold();
            assertFalse(getFaction().isHighThreshold());
            String occupierName = getFaction().getName().equals("Harkonnen") ? "Emperor" : "Harkonnen";
            territory.addForces(occupierName, 1);
            territory.addForces(getFaction().getName(), strength);
            assertTrue(getFaction().isHomeworldOccupied());
            assertFalse(getFaction().isHighThreshold());
        }
    }

    @Nested
    @DisplayName("#addSpice")
    class AddSpice {
        Faction faction;

        @BeforeEach
        void setUpAddSpice() {
            faction = getFaction();
            faction.subtractSpice(faction.getSpice(), "Test");
            faction.addSpice(10, "Test");
            faction.setLedger(ledger);
        }

        @Test
        void validPositive() {
            faction.addSpice(2, "test");
            assertEquals(12, faction.getSpice());
        }

        @Test
        void zero() {
            faction.addSpice(0, "test");
            assertEquals(10, faction.getSpice());
        }

        @Test
        void negative() {
            assertThrows(IllegalArgumentException.class, () -> faction.addSpice(-1, "test"));
        }
    }

    @Nested
    @DisplayName("#subtractSpice")
    class SubtractSpice {
        Faction faction;

        @BeforeEach
        void setUpRemoveSpice() {
            faction = getFaction();
            faction.subtractSpice(faction.getSpice(), "Test");
            faction.addSpice(10, "Test");
            faction.setLedger(ledger);
        }

        @Test
        void validPositive() {
            faction.subtractSpice(2, "test");
            assertEquals(8, faction.getSpice());
        }

        @Test
        void zero() {
            faction.subtractSpice(0, "test");
            assertEquals(10, faction.getSpice());
        }

        @Test
        void negative() {
            assertThrows(IllegalArgumentException.class, () -> faction.subtractSpice(-2, "test"));
        }

        @Test
        void tooMuch() {
            assertThrows(IllegalStateException.class, () -> faction.subtractSpice(11, "test"));
        }
    }

    @Nested
    @DisplayName("#addFrontOfShieldSpice")
    class AddFrontOfShieldSpice {
        Faction faction;

        @BeforeEach
        void setUpAddSpice() {
            faction = getFaction();
            faction.addFrontOfShieldSpice(10);
        }

        @Test
        void validPositive() {
            faction.addFrontOfShieldSpice(2);
            assertEquals(12, faction.getFrontOfShieldSpice());
        }

        @Test
        void zero() {
            faction.addFrontOfShieldSpice(0);
            assertEquals(10, faction.getFrontOfShieldSpice());
        }

        @Test
        void negative() {
            assertThrows(IllegalArgumentException.class, () -> faction.addFrontOfShieldSpice(-1));
        }
    }

    @Nested
    @DisplayName("#subtractFrontOfShieldSpice")
    class SubtractFrontOfShieldSpice {
        Faction faction;

        @BeforeEach
        void setUpRemoveSpice() {
            faction = getFaction();
            faction.addFrontOfShieldSpice(10);
        }

        @Test
        void validPositive() {
            faction.subtractFrontOfShieldSpice(2);
            assertEquals(8, faction.getFrontOfShieldSpice());
        }

        @Test
        void zero() {
            faction.subtractFrontOfShieldSpice(0);
            assertEquals(10, faction.getFrontOfShieldSpice());
        }

        @Test
        void negative() {
            assertThrows(IllegalArgumentException.class, () -> faction.subtractFrontOfShieldSpice(-2));
        }

        @Test
        void tooMuch() {
            assertThrows(IllegalStateException.class, () -> faction.subtractFrontOfShieldSpice(11));
        }
    }

    @Test
    void testGetSpiceSupportPhasesString() {
        assertEquals(" for bidding and shipping!", getFaction().getSpiceSupportPhasesString());
    }

    @Test
    void testDrawTwoTraitorsAndMustDiscardTwo() {
        faction.addTraitorCard(game.getTraitorDeck().pop());
        faction.drawTwoTraitorsWithRihaniDecipherer("testing purposes");
        assertEquals("You must discard two Traitors. player", chat.getMessages().getFirst());
        assertEquals("Reveal and discard an unused traitor:", chat.getMessages().get(1));
        assertEquals("Discard a traitor just drawn:", chat.getMessages().getLast());
        assertEquals(3, chat.getChoices().getFirst().size());
        assertTrue(chat.getChoices().getFirst().getFirst().getId().startsWith("traitor-reveal-and-discard-"));
        assertEquals(2, chat.getChoices().getLast().size());
        assertTrue(chat.getChoices().getLast().getFirst().getId().startsWith("traitor-discard-"));
        assertEquals(faction.getEmoji() + " has drawn 2 Traitor cards for testing purposes.", turnSummary.getMessages().getLast());
    }

    @Nested
    @DisplayName("#discard")
    class Discard {
        Faction faction;

        @BeforeEach
        void setUp() {
            faction = getFaction();
            faction.addTreacheryCard(new TreacheryCard("Recruits"));
        }

        @Test
        void testFactionDoesNotHaveCard() {
            assertThrows(IllegalArgumentException.class, () -> faction.discard("Kulon"));
        }

        @Test
        void testNoReasonGiven() {
            faction.discard("Recruits");
            assertEquals(faction.getEmoji() + " discards Recruits.", turnSummary.getMessages().getLast());
        }

        @Test
        void testDiscardWithReason() {
            faction.discard("Recruits", "to raise all revival limits to 7");
            assertEquals(faction.getEmoji() + " discards Recruits to raise all revival limits to 7.", turnSummary.getMessages().getLast());
        }
    }

    @Nested
    @DisplayName("#placeForces")
    class PlaceForces {
        Faction faction;
        Territory territory;
        BGFaction bg;
        TestTopic bgChat;

        @BeforeEach
        void setUp() throws IOException {
            faction = getFaction();
            territory = game.getTerritories().get("The Great Flat");
            bg = new BGFaction("p", "u");
            bgChat = new TestTopic();
            bg.setChat(bgChat);
            bg.setLedger(new TestTopic());
        }

        @Test
        void testForcesStringInTurnSummaryMessage() throws InvalidGameStateException {
            faction.placeForces(territory, 3, 0, false, false, false, false, false);
            assertEquals(faction.getEmoji() + ": 3 " + Emojis.getForceEmoji(faction.getName()) + " placed on The Great Flat", turnSummary.getMessages().getFirst());
        }

        @Test
        void testSpiceCostInTurnSummaryMessage() throws InvalidGameStateException {
            faction.placeForces(territory, 1, 0, true, true, true, false, false);
            assertEquals(faction.getEmoji() + ": 1 " + Emojis.getForceEmoji(faction.getName()) + " placed on The Great Flat for 2 " + Emojis.SPICE, turnSummary.getMessages().getFirst());
        }

        @Test
        void testHeighlinersMessageAfterFirstShipmentMessage() throws InvalidGameStateException {
            game.addGameOption(GameOption.TECH_TOKENS);
            faction.addTechToken("Heighliners");
            faction.addTechToken("Axlotl Tanks");
            faction.placeForces(territory, 1, 0, true, true, true, false, false);
            assertEquals(faction.getEmoji() + ": 1 " + Emojis.getForceEmoji(faction.getName()) + " placed on The Great Flat for 2 " + Emojis.SPICE, turnSummary.getMessages().getFirst());
            assertEquals("2 " + Emojis.SPICE + " is placed on " + Emojis.HEIGHLINERS, turnSummary.getMessages().getLast());
        }

        @Test
        void testBGGetFlipMessage() throws InvalidGameStateException {
            game.addFaction(bg);
            bg.placeForcesFromReserves(territory, 1, false);
            faction.placeForces(territory, 1, 0, true, true, true, false, false);
            assertEquals("Will you flip to " + Emojis.BG_ADVISOR + " in The Great Flat? p", bgChat.getMessages().getFirst());
        }

        @Test
        void testBGGetAdviseMessage() throws InvalidGameStateException {
            game.addFaction(bg);
            faction.placeForces(territory, 1, 0, true, true, true, false, false);
            assertEquals("Would you like to advise the shipment to The Great Flat? p", bgChat.getMessages().getLast());
        }
    }

    @Nested
    @DisplayName("#placeForcesFromReserves")
    class PlaceForcesFromReserves {
        Faction faction;
        HomeworldTerritory homeworld;
        Territory sietchTabr;

        @BeforeEach
        void setUp() {
            sietchTabr = game.getTerritories().get("Sietch Tabr");
            faction = getFaction();
            homeworld = faction.getHomeworldTerritory();
            faction.placeForcesFromReserves(sietchTabr, 1, false);
        }

        @Test
        void testMapUpdated() {
            assertTrue(game.getUpdateTypes().contains(UpdateType.MAP));
        }

        @Test
        void testFlipToLowThreshold() {
            game.addGameOption(GameOption.HOMEWORLDS);
            assertTrue(faction.isHighThreshold());
            int numForces = homeworld.getForceStrength(getFaction().getName());
            faction.placeForcesFromReserves(sietchTabr, numForces, false);
            assertFalse(faction.isHighThreshold());
        }
    }

    @Nested
    @DisplayName("#payForShipment")
    class PayForShipment {
        int spiceBeforeShipment;
        Territory habbanyaSietch;
        GuildFaction guild;
        EmperorFaction emperor;

        @BeforeEach
        void setUp() throws IOException {
            spiceBeforeShipment = faction.getSpice();
            habbanyaSietch = game.getTerritory("Habbanya Sietch");
            guild = new GuildFaction("gu", "gu");
            guild.setLedger(new TestTopic());
            emperor = new EmperorFaction("em", "em");
            emperor.setLedger(new TestTopic());
        }

        @Test
        void testFactionDoesNotHaveEnoughSpiceToShip() {
            faction.subtractSpice(spiceBeforeShipment, "Test");
            assertThrows(InvalidGameStateException.class, () -> faction.payForShipment(1, habbanyaSietch, false, false));
        }

        @Test
        void testFactionDoesNotHaveEnoughSpiceEvenWithAllySupport() {
            game.addFaction(emperor);
            faction.subtractSpice(spiceBeforeShipment, "Test");
            game.createAlliance(faction, emperor);
            emperor.setSpiceForAlly(1);
//            guild.setAllySpiceForShipping(true);
            assertThrows(InvalidGameStateException.class, () -> faction.payForShipment(2, habbanyaSietch, false, false));
        }

        @Test
        void testFactionCanPayWithAllySupport() throws InvalidGameStateException {
            game.addFaction(guild);
            game.addFaction(emperor);
            faction.subtractSpice(spiceBeforeShipment - 1, "Test");
            game.createAlliance(faction, emperor);
            emperor.setSpiceForAlly(1);
            assertEquals(" for 2 " + Emojis.SPICE + " (1 from " + Emojis.EMPEROR + ") paid to " + Emojis.GUILD, faction.payForShipment(2, habbanyaSietch, false, false));
            assertEquals(0, faction.getSpice());
            assertEquals(9, emperor.getSpice());
            assertEquals(7, guild.getSpice());
        }

        @Test
        void testNormalPaymentWithGuildInGame() throws InvalidGameStateException {
            game.addFaction(guild);
            assertEquals(" for 1 " + Emojis.SPICE + " paid to " + Emojis.GUILD, faction.payForShipment(1, habbanyaSietch, false, false));
            assertEquals(spiceBeforeShipment - 1, faction.getSpice());
            assertEquals(6, guild.getSpice());
        }

        @Test
        void testKaramaShipmentDoesNotPayGuild() throws InvalidGameStateException {
            game.addFaction(guild);
            assertEquals(" for 1 " + Emojis.SPICE, faction.payForShipment(1, habbanyaSietch, true, false));
            assertEquals(spiceBeforeShipment - 1, faction.getSpice());
            assertEquals(5, guild.getSpice());
        }

        @Test
        void testGuildAtLowThresdhold() throws InvalidGameStateException {
            game.addGameOption(GameOption.HOMEWORLDS);
            game.addFaction(guild);
            guild.placeForces(habbanyaSietch, 15, 0, false, false, false, false, false);
            assertFalse(guild.isHighThreshold());
            faction.addSpice(1, "Test");
            spiceBeforeShipment = faction.getSpice();
            assertEquals(" for 3 " + Emojis.SPICE + ", 2 " + Emojis.SPICE + " paid to " + Emojis.GUILD, faction.payForShipment(3, habbanyaSietch, false, false));
            assertEquals(spiceBeforeShipment - 3, faction.getSpice());
            assertEquals(7, guild.getSpice());
        }

        @Test
        void testGuildOccupied() throws InvalidGameStateException {
            game.addGameOption(GameOption.HOMEWORLDS);
            game.addFaction(guild);
            guild.placeForces(habbanyaSietch, 15, 0, false, false, false, false, false);
            assertFalse(guild.isHighThreshold());
            faction.placeForces(guild.getHomeworldTerritory(), 1, 0, false, false, false, false, false);
            assertTrue(guild.isHomeworldOccupied());
            faction.addSpice(1, "Test");
            spiceBeforeShipment = faction.getSpice();
            assertEquals(" for 3 " + Emojis.SPICE + ", 2 " + Emojis.SPICE + " paid to " + Emojis.GUILD + ", 1 " + Emojis.SPICE + " paid to " + faction.getEmoji(), faction.payForShipment(3, habbanyaSietch, false, false));
            assertEquals(spiceBeforeShipment - 3 + 1, faction.getSpice());
            assertEquals(7, guild.getSpice());
        }

        @Test
        void testNormalPaymentWithGuildNotInGame() throws InvalidGameStateException {
            assertEquals(" for 1 " + Emojis.SPICE, faction.payForShipment(1, habbanyaSietch, false, false));
            assertEquals(spiceBeforeShipment - 1, faction.getSpice());
        }
    }

    @Nested
    @DisplayName("#executeShipment")
    class ExecuteShipment {
        Faction faction;
        Shipment shipment;

        @BeforeEach
        void setUp() {
            faction = getFaction();
            shipment = faction.getShipment();
            shipment.clear();
            shipment.setForce(1);
            shipment.setTerritoryName("Sietch Tabr");
        }

        @Test
        void testPaidShipment() throws InvalidGameStateException {
            int spice = faction.getSpice();
            faction.executeShipment(game, false, false);
            assertEquals(spice - 1, faction.getSpice());
        }

        @Test
        void testGuildAmbassadorAndBTHighThreshold() {
            int spice = faction.getSpice();
            faction.subtractSpice(spice, "Test");
            assertDoesNotThrow(() -> faction.executeShipment(game, false, true));
        }
    }

    @Nested
    @DisplayName("#executeMovement")
    class ExecuteMovement {
        Faction faction;
        Territory theGreatFlat;
        Territory funeralPlain;
        Movement movement;
        TestTopic bgChat;
        TestTopic ecazChat;
        TestTopic moritaniChat;

        @BeforeEach
        void setUp() {
            faction = getFaction();
            theGreatFlat = game.getTerritories().get("The Great Flat");
            funeralPlain = game.getTerritories().get("Funeral Plain");
            faction.placeForcesFromReserves(theGreatFlat, 1, false);
            movement = faction.getMovement();
            movement.clear();
            movement.setForce(1);
            movement.setMovingFrom("The Great Flat");
            movement.setMovingTo("Funeral Plain");
        }

        @Test
        void testBGGetFlipMessage() throws IOException, InvalidGameStateException {
            BGFaction bg = new BGFaction("p", "u");
            bgChat = new TestTopic();
            bg.setChat(bgChat);
            bg.setLedger(new TestTopic());
            game.addFaction(bg);
            bg.placeForcesFromReserves(funeralPlain, 1, false);
            faction.executeMovement();
            assertEquals("Will you flip to " + Emojis.BG_ADVISOR + " in Funeral Plain? p", bgChat.getMessages().getFirst());
        }

        @Test
        void testEcazGetAmbassadorMessage() throws IOException, InvalidGameStateException {
            EcazFaction ecaz = new EcazFaction("ec", "ec");
            ecazChat = new TestTopic();
            ecaz.setChat(ecazChat);
            game.addFaction(ecaz);
            funeralPlain.setEcazAmbassador("Ecaz");
            faction.executeMovement();
            assertEquals("Will you trigger your Ecaz Ambassador in Funeral Plain against " + faction.getEmoji() + "? ec", ecazChat.getMessages().getFirst());
        }

        @Test
        void testMoritaniGetTerrorTokenMessage() throws IOException, InvalidGameStateException {
            MoritaniFaction moritani = new MoritaniFaction("mo", "mo");
            moritaniChat = new TestTopic();
            moritani.setChat(moritaniChat);
            game.addFaction(moritani);
            funeralPlain.addTerrorToken(game, "Robbery");
            faction.executeMovement();
            assertEquals("Will you trigger your Robbery Terror Token in Funeral Plain against " + faction.getEmoji() + "? mo", moritaniChat.getMessages().getFirst());
        }
    }

    @Nested
    @DisplayName("#removeForces")
    class RemoveForces {
        Faction faction;
        Territory territory;
        String forceName;

        @BeforeEach
        void setUp() {
            faction = getFaction();

            territory = game.getTerritories().get("Sihaya Ridge");
            forceName = faction.getName();

            territory.addForces(forceName, 5);
            faction.removeReserves(5);
        }

        @Test
        void validToReserves() {
            int reservesStrength = faction.getReservesStrength();
            faction.removeForces(territory.getTerritoryName(), forceName, 2, false, false);
            assertEquals(3, territory.getForceStrength(forceName));
            assertEquals(reservesStrength + 2, faction.getReservesStrength());
            assertEquals(0, game.getTleilaxuTanks().getForces().stream().filter(f -> f.getName().equals(forceName)).count());
        }

        @Test
        void validToTanks() {
            int reservesStrength = faction.getReservesStrength();
            TleilaxuTanks tanks = game.getTleilaxuTanks();
            int tanksStrength = tanks.getForceStrength(forceName);
            faction.removeForces(territory.getTerritoryName(), forceName, 2, true, true);
            assertEquals(3, territory.getForceStrength(forceName));
            assertEquals(tanksStrength + 2, tanks.getForceStrength(forceName));
            assertEquals(reservesStrength, faction.getReservesStrength());
        }

        @Test
        void invalidToReservesTooMany() {
            assertThrows(IllegalArgumentException.class,
                    () -> faction.removeForces(territory.getTerritoryName(), forceName, 6, false, false));
        }

        @Test
        void invalidToReservesNegatives() {
            assertThrows(IllegalArgumentException.class,
                    () -> faction.removeForces(territory.getTerritoryName(), forceName, -1, false, false));
        }

        @Test
        void invalidToTanksTooMany() {
            assertThrows(IllegalArgumentException.class,
                    () -> faction.removeForces(territory.getTerritoryName(), forceName, 6, true, false));
        }

        @Test
        void invalidToTanksNegatives() {
            assertThrows(IllegalArgumentException.class,
                    () -> faction.removeForces(territory.getTerritoryName(), forceName, -1, true, false));
        }
    }

    @Nested
    @DisplayName("#withdrawForces")
    class WithdrawForces {
        Territory falseWallEast_farNorthSector;
        Territory falseWallEast_middleSector;
        Territory falseWallEast_southSector;
        int homeworldForcesBefore;

        @BeforeEach
        void setUp() {
            game.setStorm(7);
            falseWallEast_farNorthSector = game.getTerritory("False Wall East (Far North Sector)");
            falseWallEast_middleSector = game.getTerritory("False Wall East (Middle Sector)");
            falseWallEast_southSector = game.getTerritory("False Wall East (South Sector)");

            falseWallEast_southSector.addForces(faction.getName(), 1);
            falseWallEast_middleSector.addForces(faction.getName(), 1);
            falseWallEast_farNorthSector.addForces(faction.getName(), 1);
            homeworldForcesBefore = faction.getHomeworldTerritory().getForceStrength(faction.getName());

            faction.withdrawForces(1, 0, List.of(falseWallEast_southSector, falseWallEast_middleSector), "Harass and Withdraw");
        }

        @Test
        void testOneForceReturnedToReserves() {
            assertFalse(falseWallEast_southSector.hasActiveFaction(faction) && falseWallEast_middleSector.hasActiveFaction(faction));
            assertTrue(turnSummary.getMessages().stream().anyMatch(m -> m.equals("1 " + Emojis.getForceEmoji(faction.getName()) + " returned to reserves with Harass and Withdraw.")));
        }

        @Test
        void testForcesWereAddedToReserves() {
            assertEquals(homeworldForcesBefore + 1, faction.getHomeworldTerritory().getForceStrength(faction.getName()));
        }

        @Test
        void testForcesInFarNorthNotReturnedToReserves() {
            assertEquals(1, falseWallEast_farNorthSector.getForceStrength(faction.getName()));
        }
    }

    @Nested
    @DisplayName("#isNearShieldWall")
    class IsNearShieldWall {
        Faction faction;
        Territory territory;
        String forceName;

        @BeforeEach
        void setUp() {
            faction = getFaction();
            forceName = faction.getName();
        }

        @Test
        void trueOnShieldWall() {
            territory = game.getTerritories().get("Shield Wall (North Sector)");
            territory.addForces(forceName, 1);
            assertTrue(faction.isNearShieldWall());
        }

        @Test
        void trueInImperialBasin() {
            territory = game.getTerritories().get("Imperial Basin (Center Sector)");
            territory.addForces(forceName, 1);
            assertTrue(faction.isNearShieldWall());
        }

        @Test
        void trueInFalseWallEast() {
            territory = game.getTerritories().get("False Wall East (Far North Sector)");
            territory.addForces(forceName, 1);
            assertTrue(faction.isNearShieldWall());
        }

        @Test
        void trueInGaraKulon() {
            territory = game.getTerritories().get("Gara Kulon");
            territory.addForces(forceName, 1);
            assertTrue(faction.isNearShieldWall());
        }
    }

    @Nested
    @DisplayName("#hasTreacheryCard")
    class HasTreacheryCard {
        Faction faction;
        TreacheryCard familyAtomics;

        @BeforeEach
        void setUp() {
            faction = getFaction();

            familyAtomics = game.getTreacheryDeck().stream()
                    .filter(t -> t.name().equals("Family Atomics"))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Family Atomics not found"));
        }

        @Test
        void hasFamilyAtomics() {
            faction.addTreacheryCard(familyAtomics);
            assertTrue(faction.hasTreacheryCard("Family Atomics"));
        }

        @Test
        void doesNotHaveFamilyAtomics() {
            assertFalse(faction.hasTreacheryCard("Family Atomics"));
        }
    }

    @Nested
    @DisplayName("hmsStrongholdProxy")
    class HMSStrongholdProxy {
        Faction faction;
        StrongholdCard arrakeenCard;
        StrongholdCard hmsCard;

        @BeforeEach
        void setUp() {
            faction = getFaction();
            arrakeenCard = new StrongholdCard("Arrakeen");
            hmsCard = new StrongholdCard("Hidden Mobile Stronghold");
        }

        @Test
        void testHasArrakeenButNotHMS() {
            faction.addStrongholdCard(arrakeenCard);
            assertThrows(InvalidGameStateException.class, () -> faction.setHmsStrongholdProxy(arrakeenCard));
        }

        @Test
        void testHasHMSButNotArrakeen() {
            faction.addStrongholdCard(hmsCard);
            assertThrows(InvalidGameStateException.class, () -> faction.setHmsStrongholdProxy(arrakeenCard));
        }

        @Test
        void testHasHMSAndArrakeen() {
            faction.addStrongholdCard(arrakeenCard);
            faction.addStrongholdCard(hmsCard);
            assertDoesNotThrow(() -> faction.setHmsStrongholdProxy(arrakeenCard));
        }
    }

    @Nested
    @DisplayName("#moritaniTerrorAlliance")
    class MoritaniTerrorAlliance {
        Faction faction;
        MoritaniFaction moritani;
        TestTopic moritaniChat;
        Territory arrakeen;

        @BeforeEach
        public void setUp() throws InvalidGameStateException, IOException {
            faction = getFaction();
            moritani = new MoritaniFaction("mo", "mo");
            game.addFaction(moritani);
            moritaniChat = new TestTopic();
            moritani.setChat(moritaniChat);
            moritani.setLedger(new TestTopic());
            arrakeen = game.getTerritory("Arrakeen");
            game.getMoritaniFaction().placeTerrorToken(arrakeen, "Robbery");
        }

        @Test
        public void testAcceptTerrorAlliance() throws InvalidGameStateException {
            faction.acceptTerrorAlliance(moritani, "Arrakeen", "Robbery");
            assertFalse(arrakeen.hasTerrorToken());
            assertTrue(moritani.getTerrorTokens().contains("Robbery"));
            assertEquals("You have sent the emissary away with news of their new alliance!", chat.getMessages().getLast());
        }

        @Test
        public void testDenyTerrorAlliance() throws InvalidGameStateException {
            faction.denyTerrorAlliance("Arrakeen", "Robbery");
            assertFalse(arrakeen.hasTerrorToken());
            assertFalse(moritani.getTerrorTokens().contains("Robbery"));
            assertEquals("You have sent the emissary away empty-handed. Time to prepare for the worst.", chat.getMessages().getLast());
            assertEquals("Your terrorist in Arrakeen can rob the " + faction.getEmoji() + "! What would you like to do?", moritaniChat.getMessages().getLast());
            assertTrue(moritani.getAlly().isEmpty());
            assertTrue(faction.getAlly().isEmpty());
        }
    }

    @Nested
    @DisplayName("#performMentatPauseActions")
    class PerformMentatPauseActions {
        Faction faction;

        @BeforeEach
        void setUp() {
            faction = getFaction();
            faction.setChat(chat);
        }

        @Test
        void testCanPayToRemoveExtortion() {
            faction.subtractSpice(faction.getSpice(), "Test");
            faction.addSpice(3, "Test");
            faction.performMentatPauseActions(true);
            assertEquals("Will you pay " + Emojis.MORITANI + " 3 " + Emojis.SPICE + " to remove the Extortion token from the game? " + faction.getPlayer(), chat.getMessages().getFirst());
            assertEquals(1, chat.getChoices().size());
        }

        @Test
        void testCannotPayToRemoveExtortion() {
            faction.subtractSpice(faction.getSpice(), "Test");
            faction.performMentatPauseActions(true);
            assertEquals("You do not have enough spice to pay Extortion.", chat.getMessages().getFirst());
            assertEquals(0, chat.getChoices().size());
        }
    }
}