package model.factions;

import constants.Emojis;
import enums.GameOption;
import exceptions.InvalidGameStateException;
import model.HomeworldTerritory;
import model.Territory;
import model.TestTopic;
import model.TreacheryCard;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class EmperorFactionTest extends FactionTestTemplate {
    private EmperorFaction faction;

    @Override
    Faction getFaction() {
        return faction;
    }

    @BeforeEach
    void setUp() throws IOException {
        faction = new EmperorFaction("player", "player");
        commonPostInstantiationSetUp();
    }

    @Test
    public void testInitialSpice() {
        assertEquals(10, faction.getSpice());
    }

    @Nested
    @DisplayName("#presentKaitainHighThresholdChoices")
    class PresentKaitainHighThresholdChoices {
        @BeforeEach
        public void setUp() {
            game.addGameOption(GameOption.HOMEWORLDS);
        }

        @Test
        public void testLowThreshold() {
            faction.removeForces("Kaitain", 15, false, true);
            faction.checkForHighThreshold();
            faction.checkForLowThreshold();
            faction.presentKaitainHighThresholdChoices();
            assertEquals("You are at Low Threshold and cannot use Kaitain High Threshold ability.", chat.getMessages().getFirst());
            assertTrue(chat.getChoices().isEmpty());
        }

        @Test
        public void testNoCardsHighThreshold() {
            faction.presentKaitainHighThresholdChoices();
            assertEquals("You have no " + Emojis.TREACHERY + " cards for Kaitain High Threshold.", chat.getMessages().getFirst());
            assertTrue(chat.getChoices().isEmpty());
        }

        @Test
        public void testHasCardsHighThreshold() {
            faction.addTreacheryCard(new TreacheryCard("Baliset"));
            faction.addTreacheryCard(new TreacheryCard("Karama"));
            faction.presentKaitainHighThresholdChoices();
            assertEquals("Would you like to pay 2 " + Emojis.SPICE + " to discard a " + Emojis.TREACHERY + " card with Kaitain High Threshold? player", chat.getMessages().getFirst());
            assertEquals(3, chat.getChoices().getFirst().size());
        }
    }

    @Nested
    @DisplayName("#revival")
    class Revival extends FactionTestTemplate.Revival {
        @Test
        @Override
        public void testPaidRevival() {
            faction.removeForces(faction.getHomeworld(), 5, false, true);
            EmperorFaction emperor = (EmperorFaction) faction;
            faction.removeForces(emperor.getSecondHomeworld(), 2, true, true);
            game.reviveForces(faction, false, 2, 1);
            assertEquals(faction.getEmoji() + " revives 2 " + Emojis.getForceEmoji(faction.getName()) + " 1 " + Emojis.getForceEmoji("Emperor*") + " for free.", turnSummary.getMessages().getLast());
        }

        @Test
        @Override
        public void testFreeReviveStars() {
            EmperorFaction emperor = (EmperorFaction) faction;
            faction.removeForces(emperor.getSecondHomeworld(), 3, true, true);
            assertEquals(1, faction.countFreeStarredRevival());
        }

        @Test
        @Override
        public void testPaidRevivalMessageAfter1StarFree() throws InvalidGameStateException {
            EmperorFaction emperor = (EmperorFaction) faction;
            assertDoesNotThrow(() -> faction.removeForces(emperor.getSecondHomeworld(), 2, true, true));
            faction.performFreeRevivals();
            faction.presentPaidRevivalChoices(1);
            assertEquals(0, chat.getMessages().size());
            assertEquals(0, chat.getChoices().size());
            assertEquals(faction.getEmoji() + " has no revivable forces in the tanks", faction.getPaidRevivalMessage());
        }

//        @Test
//        public void testNoForcesInTanksTriggersReviveExtraAllyForces() throws IOException, InvalidGameStateException {
//            Faction bg = new BGFaction("bg", "bg");
//            bg.setLedger(new TestTopic());
//            game.addFaction(bg);
//            game.createAlliance(faction, bg);
//            bg.removeForces("Wallach IX", 2, false, true);
//            faction.presentPaidRevivalChoices(0);
//            assertEquals("Would you like to purchase additional revivals for " + Emojis.BG + "? " + faction.getPlayer(), chat.getMessages().getLast());
//            assertEquals(4, chat.getChoices().getLast().size());
//        }
    }

    @Nested
    @DisplayName("#presentAllyRevivalChoices")
    class PresentAllyRevivalChoices {
        @Nested
        @DisplayName("#fremenAsAlly")
        class BTAsAlly {
            Faction bt;

            @BeforeEach
            public void setUp() throws IOException {
                bt = new BTFaction("bt", "bt");
                bt.setLedger(new TestTopic());
                game.addFaction(bt);
                game.createAlliance(faction, bt);
                turnSummary.clear();
            }

            @Test
            public void testThreeBTForceInTanksCostOneSpiceEachToRevive() {
                faction.subtractSpice(7, "test setup");
                assertEquals(3, faction.getSpice());
                bt.removeForces("Tleilax", 5, false, true);
                bt.reviveForces(false, 2);
                faction.presentAllyRevivalChoices();
                assertEquals("Would you like to purchase additional revivals for " + Emojis.BT + "? " + faction.getPlayer(), chat.getMessages().getLast());
                assertEquals(4, chat.getChoices().getLast().size());
                assertFalse(chat.getChoices().getLast().getLast().isDisabled());
            }
        }

        @Nested
        @DisplayName("#fremenAsAlly")
        class FremenAsAlly {
            Faction fremen;

            @BeforeEach
            public void setUp() throws IOException {
                fremen = new FremenFaction("fr", "fr");
                fremen.setLedger(new TestTopic());
                game.addFaction(fremen);
                game.createAlliance(faction, fremen);
                turnSummary.clear();
            }

            @Test
            public void testNoAllyForcesInTanks() {
                faction.presentAllyRevivalChoices();
                assertEquals("Your ally " + Emojis.FREMEN + " has no revivable forces for you to pay for.", chat.getMessages().getLast());
                assertEquals(0, turnSummary.getChoices().size());
            }

            @Test
            public void testOnlyFedaykinInTanks() {
                fremen.removeForces("Southern Hemisphere", 2, true, true);
                faction.presentAllyRevivalChoices();
                assertEquals("Your ally " + Emojis.FREMEN + " has no revivable forces for you to pay for.", chat.getMessages().getLast());
            }

            @Test
            public void testOneFremenForceTwoFedaykinInTanks() {
                fremen.removeForces("Southern Hemisphere", 2, true, true);
                fremen.removeForces("Southern Hemisphere", 1, false, true);
                faction.presentAllyRevivalChoices();
                assertEquals("Would you like to purchase additional revivals for " + Emojis.FREMEN + "? " + faction.getPlayer(), chat.getMessages().getLast());
                assertEquals(4, chat.getChoices().getLast().size());
                assertTrue(chat.getChoices().getLast().get(2).isDisabled());
            }

            @Test
            public void testTwoFremenForceTwoFedaykinInTanks() {
                fremen.removeForces("Southern Hemisphere", 2, true, true);
                fremen.removeForces("Southern Hemisphere", 2, false, true);
                faction.presentAllyRevivalChoices();
                assertEquals("Would you like to purchase additional revivals for " + Emojis.FREMEN + "? " + faction.getPlayer(), chat.getMessages().getLast());
                assertEquals(4, chat.getChoices().getLast().size());
                assertFalse(chat.getChoices().getLast().get(2).isDisabled());
                assertTrue(chat.getChoices().getLast().getLast().isDisabled());
            }

            @Test
            public void testThreeFremenForceTwoFedaykinInTanks() {
                fremen.removeForces("Southern Hemisphere", 2, true, true);
                fremen.removeForces("Southern Hemisphere", 3, false, true);
                faction.presentAllyRevivalChoices();
                assertEquals("Would you like to purchase additional revivals for " + Emojis.FREMEN + "? " + faction.getPlayer(), chat.getMessages().getLast());
                assertEquals(4, chat.getChoices().getLast().size());
                assertFalse(chat.getChoices().getLast().getLast().isDisabled());
            }

            @Test
            public void testEmperorDoesNotHaveEnoughSpiceToReviveAll() {
                faction.subtractSpice(8, "test");
                fremen.removeForces("Southern Hemisphere", 3, false, true);
                faction.presentAllyRevivalChoices();
                assertFalse(chat.getChoices().getLast().get(1).isDisabled());
                assertTrue(chat.getChoices().getLast().get(2).isDisabled());
            }
        }
    }

    @Test
    public void testFreeReviveStarsNoneInTanks() {
        assertEquals(0, faction.countFreeStarredRevival());
    }

    @Test
    public void testFreeRevival() {
        assertEquals(1, faction.getFreeRevival());
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
        assertEquals(2, faction.getFreeRevival());
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
        assertEquals(2, faction.getFreeRevival());
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
        assertEquals(4, faction.getFreeRevival());
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
    public void testEmperorCanPayForOneSardaukar() throws InvalidGameStateException {
        faction.removeForces(faction.getHomeworld(), 1, false, true);
        faction.removeForces(faction.getSecondHomeworld(), 1, true, true);
        faction.setStarRevived(false);
        faction.presentPaidRevivalChoices(1);
        assertEquals("Would you like to purchase additional revivals including 1 " + Emojis.EMPEROR_SARDAUKAR + "? " + faction.getPlayer(), chat.getMessages().getFirst());
        assertEquals(2, faction.getRevivableForces());
        assertEquals(3, chat.getChoices().getFirst().size());
    }

    @Nested
    @DisplayName("#reviveForces")
    class ReviveForces {
        @BeforeEach
        public void setUp() {
        }

        @Test
        public void testNoSardaukarInTanks() {
            faction.removeForces("Kaitain", 1, false, true);
            faction.reviveForces(true, 1);
            assertEquals(Emojis.EMPEROR + " revives 1 " + Emojis.EMPEROR_TROOP + " for 2 " + Emojis.SPICE, turnSummary.getMessages().getLast());
        }

        @Test
        public void testSardaukarInTanksCannotBeRevived() {
            faction.removeForces("Salusa Secundus", 1, true, true);
            faction.setStarRevived(true);
            assertThrows(IllegalArgumentException.class, () -> faction.reviveForces(true, 1));
        }

        @Test
        public void testSardaukarInTanksCanBeRevived() {
            faction.removeForces("Salusa Secundus", 1, true, true);
            faction.setStarRevived(false);
            faction.reviveForces(true, 1);
            assertEquals(Emojis.EMPEROR + " revives 1 " + Emojis.EMPEROR_SARDAUKAR + " for 2 " + Emojis.SPICE, turnSummary.getMessages().getLast());
        }

//        @Test
//        public void testCanReviveExtraAllyForces() throws IOException, InvalidGameStateException {
//            Faction bg = new BGFaction("bg", "bg");
//            bg.setLedger(new TestTopic());
//            game.addFaction(bg);
//            game.createAlliance(faction, bg);
//            faction.removeForces("Kaitain", 2, false, true);
//            faction.presentPaidRevivalChoices(1);
//            bg.removeForces("Wallach IX", 2, false, true);
//            faction.reviveForces(true, 0);
//            assertEquals(Emojis.EMPEROR + " does not purchase additional revivals.", turnSummary.getMessages().getLast());
//            assertEquals("Would you like to purchase additional revivals for " + Emojis.BG + "? " + faction.getPlayer(), chat.getMessages().getLast());
//            assertEquals(4, chat.getChoices().getLast().size());
//            assertTrue(faction.isPaidRevivalTBD());
//        }

        @Test
        public void testCanReviveExtraFremenAllyForces() throws IOException, InvalidGameStateException {
            Faction fremen = new FremenFaction("fr", "fr");
            fremen.setLedger(new TestTopic());
            game.addFaction(fremen);
            game.createAlliance(faction, fremen);
            faction.removeForces("Kaitain", 5, false, true);
            faction.removeForces("Salusa Secundus", 3, true, true);
            fremen.removeForces("Southern Hemisphere", 5, false, true);
            fremen.removeForces("Southern Hemisphere", 2, false, true);
            game.startRevival();
            game.getRevival().performPreSteps(game);
            game.getRevival().startRevivingForces(game);
            assertEquals("Would you like to purchase additional revivals for " + Emojis.FREMEN + "? " + faction.getPlayer(), chat.getMessages().getLast());
            assertEquals(4, chat.getChoices().getLast().size());
            assertTrue(faction.isPaidRevivalTBD());
        }

//        @Test
//        public void testAllyHasNoRevivableForces() throws IOException, InvalidGameStateException {
//            Faction bg = new BGFaction("bg", "bg");
//            bg.setLedger(new TestTopic());
//            game.addFaction(bg);
//            game.createAlliance(faction, bg);
//            faction.removeForces("Kaitain", 2, false, true);
//            faction.presentPaidRevivalChoices(1);
//            faction.reviveForces(true, 0);
//            assertEquals(Emojis.EMPEROR + " does not purchase additional revivals.", turnSummary.getMessages().getLast());
//            assertEquals("Your ally " + Emojis.BG + " has no revivable forces for you to pay for.", chat.getMessages().getLast());
//            assertFalse(faction.isPaidRevivalTBD());
//        }
    }

    @Nested
    @DisplayName("#reviveAllyForces")
    class ReviveAllyForces {
        BTFaction bt;

        @BeforeEach
        void setUp() throws IOException {
            bt = new BTFaction("bt", "bt");
            bt.setLedger(new TestTopic());
        }

        @Nested
        @DisplayName("atreidesAsAlly")
        class AtreidesAsAlly {
            AtreidesFaction atreides;

            @BeforeEach
            void setUp() throws IOException {
                atreides = new AtreidesFaction("at", "at");
                atreides.setLedger(new TestTopic());
                game.addFaction(atreides);
                game.createAlliance(faction, atreides);
            }

            @Test
            void testAllyHasNoForcesInTanks() {
                assertThrows(IllegalArgumentException.class, () -> faction.reviveAllyForces(3));
            }

            @Test
            void testEmperorDoesNotReviveAllyForces() throws InvalidGameStateException {
                game.removeForces("Caladan", atreides, 3, false, true);
                faction.reviveAllyForces(0);
                assertEquals("You will not revive any extra forces for your ally.", chat.getMessages().getLast());
                assertEquals(Emojis.EMPEROR + " does not purchase extra revivals for " + Emojis.ATREIDES, turnSummary.getMessages().getLast());
                assertEquals(10, faction.getSpice());
            }

            @Test
            void testEmperorRevivesTwoAllyForces() throws InvalidGameStateException {
                game.removeForces("Caladan", atreides, 3, false, true);
                faction.reviveAllyForces(2);
                assertEquals("Your revival request for your ally has been submitted to the " + Emojis.BT, chat.getMessages().getLast());
                assertEquals(Emojis.EMPEROR + " pays 4 " + Emojis.SPICE + " to revive 2 " + Emojis.ATREIDES_TROOP, turnSummary.getMessages().getLast());
                assertEquals(6, faction.getSpice());
            }

            @Test
            void testEmperorRevivesTwoAllyForcesWithBTInTheGame() throws InvalidGameStateException {
                game.addFaction(bt);
                game.removeForces("Caladan", atreides, 3, false, true);
                faction.reviveAllyForces(2);
                assertEquals(Emojis.EMPEROR + " pays 4 " + Emojis.SPICE + " to " + Emojis.BT + " to revive 2 " + Emojis.ATREIDES_TROOP, turnSummary.getMessages().getLast());
                assertEquals(6, faction.getSpice());
                assertEquals(9, bt.getSpice());
            }

            @Test
            void testEmperorDoesNotHaveEnoughSpice() {
                faction.subtractSpice(7, "Test");
                game.removeForces("Caladan", atreides, 3, false, true);
                assertThrows(IllegalStateException.class, () -> faction.reviveAllyForces(2));
            }
        }

        @Nested
        @DisplayName("ixAsAlly")
        class IxAsAlly {
            IxFaction ix;

            @BeforeEach
            void setUp() throws IOException {
                ix = new IxFaction("ix", "ix");
                ix.setLedger(new TestTopic());
                game.addFaction(ix);
                game.createAlliance(faction, ix);
                game.removeForces("Ix", ix, 1, 2, true);
            }

            @Test
            void testCyborgRevivalPreferred() throws InvalidGameStateException {
                faction.reviveAllyForces(2);
                assertEquals(Emojis.EMPEROR + " pays 6 " + Emojis.SPICE + " to revive 2 " + Emojis.IX_CYBORG, turnSummary.getMessages().getLast());
                assertEquals(4, faction.getSpice());
                assertEquals(1, game.getTleilaxuTanks().getForceStrength("Ix"));
                assertEquals(0, game.getTleilaxuTanks().getForceStrength("Ix*"));
            }

            @Test
            void testReviveCyborgsAndSuboid() throws InvalidGameStateException {
                faction.reviveAllyForces(3);
                assertEquals(Emojis.EMPEROR + " pays 8 " + Emojis.SPICE + " to revive 1 " + Emojis.IX_SUBOID + " 2 " + Emojis.IX_CYBORG, turnSummary.getMessages().getLast());
                assertEquals(2, faction.getSpice());
                assertEquals(0, game.getTleilaxuTanks().getForceStrength("Ix"));
                assertEquals(0, game.getTleilaxuTanks().getForceStrength("Ix*"));
            }

            @Test
            void testNotEnoughSpiceForTwoCyborgs() throws InvalidGameStateException {
                faction.subtractSpice(5, "Test");
                faction.reviveAllyForces(2);
                assertEquals(Emojis.EMPEROR + " pays 5 " + Emojis.SPICE + " to revive 1 " + Emojis.IX_SUBOID + " 1 " + Emojis.IX_CYBORG, turnSummary.getMessages().getLast());
                assertEquals(0, faction.getSpice());
                assertEquals(0, game.getTleilaxuTanks().getForceStrength("Ix"));
                assertEquals(1, game.getTleilaxuTanks().getForceStrength("Ix*"));
            }
        }
    }

    @Nested
    @DisplayName("#placeForces")
    class PlaceForces extends FactionTestTemplate.PlaceForces {
        @Test
        @Override
        void testForcesStringInTurnSummaryMessage() throws InvalidGameStateException {
            faction.placeForces(territory, 3, 2, false, false, false, false, false);
            assertEquals(faction.getEmoji() + ": 3 " + Emojis.EMPEROR_TROOP + " 2 " + Emojis.EMPEROR_SARDAUKAR + " placed on The Great Flat", turnSummary.getMessages().getFirst());
        }
    }
    @Nested
    @DisplayName("#placeForcesFromReserves")
    class PlaceForcesFromReserves extends FactionTestTemplate.PlaceForcesFromReserves {
        @Test
        void testFlipSalusaSecundusToLowThreshold() {
            game.addGameOption(GameOption.HOMEWORLDS);
            faction.placeForcesFromReserves(sietchTabr, 5, true);
            assertFalse(((EmperorFaction) faction).isSecundusHighThreshold());
        }
    }

    @Nested
    @DisplayName("#payForShipment")
    class PayForShipment extends FactionTestTemplate.PayForShipment {
        BGFaction bg;

        @BeforeEach
        @Override
        void setUp() throws IOException {
            bg = new BGFaction("bg", "bg");
            bg.setLedger(new TestTopic());
            super.setUp();
        }

        @Test
        void testFactionCanPayWithAllySupport() throws InvalidGameStateException {
            game.addFaction(guild);
            game.addFaction(bg);
            faction.subtractSpice(spiceBeforeShipment - 1, "Test");
            game.createAlliance(faction, bg);
            bg.setSpiceForAlly(1);
            assertEquals(" for 2 " + Emojis.SPICE + " (1 from " + Emojis.BG + ") paid to " + Emojis.GUILD, faction.payForShipment(2, habbanyaSietch, false, false));
            assertEquals(0, faction.getSpice());
            assertEquals(4, bg.getSpice());
            assertEquals(7, guild.getSpice());
        }

        @Test
        @Override
        void testNormalPaymentWithGuildInGame() throws InvalidGameStateException {
            assertEquals(" for 1 " + Emojis.SPICE, faction.payForShipment(1, habbanyaSietch, false, false));
            assertEquals(spiceBeforeShipment - 1, faction.getSpice());
        }
    }

    @Nested
    @DisplayName("#withdrawForces")
    class WithdrawForces extends FactionTestTemplate.WithdrawForces {
        int secondHomeworldForcesBefore;
        Territory salusaSecundus;

        @Override
        @BeforeEach
        void setUp() {
            falseWallEast_southSector = game.getTerritory("False Wall East (South Sector)");
            falseWallEast_southSector.addForces("Emperor*", 1);
            falseWallEast_middleSector = game.getTerritory("False Wall East (Middle Sector)");
            falseWallEast_middleSector.addForces("Emperor*", 1);
            salusaSecundus = game.getTerritory(faction.getSecondHomeworld());
            secondHomeworldForcesBefore = salusaSecundus.getForceStrength("Emperor*");

            faction.withdrawForces(0, 1, List.of(falseWallEast_southSector, falseWallEast_middleSector), "Harass and Withdraw");
            super.setUp();
        }

        @Override
        @Test
        void testOneForceReturnedToReserves() {
            super.testOneForceReturnedToReserves();
            assertTrue(turnSummary.getMessages().stream().anyMatch(m -> m.equals("1 " + Emojis.EMPEROR_SARDAUKAR + " returned to reserves with Harass and Withdraw.")));
        }

        @Override
        @Test
        void testForcesWereAddedToReserves() {
            super.testForcesWereAddedToReserves();
            assertEquals(secondHomeworldForcesBefore + 1, salusaSecundus.getForceStrength("Emperor*"));
        }
    }

    @Test
    public void testInitialHasMiningEquipment() {
        assertFalse(faction.hasMiningEquipment());
    }

    @Test
    public void testInitialReserves() {
        assertEquals(15, faction.getReservesStrength());
        assertEquals(5, faction.getSpecialReservesStrength());
        assertEquals(20, faction.getTotalReservesStrength());
        assertEquals(0, game.getTerritory(faction.getHomeworld()).getForceStrength("Emperor*"));
        assertEquals(0, game.getTerritory(faction.getSecondHomeworld()).getForceStrength("Emperor"));
    }

    @Test
    public void testInitialForcePlacement() {
        for (Territory territory : game.getTerritories().values()) {
            if (territory instanceof HomeworldTerritory) continue;
            assertEquals(0, territory.countFactions());
        }
    }

    @Test
    public void testEmoji() {
        assertEquals(Emojis.EMPEROR, faction.getEmoji());
    }

    @Test
    public void testForceEmoji() {
        assertEquals(Emojis.EMPEROR_TROOP, faction.getForceEmoji());
    }

    @Test
    @Override
    public void testSpecialForceEmoji() {
        assertEquals(Emojis.EMPEROR_SARDAUKAR, faction.getSpecialForceEmoji());
    }

    @Test
    public void testHandLimit() {
        assertEquals(4, faction.getHandLimit());
    }

    @Test
    public void testKaitainHighDiscard() {
        TreacheryCard kulon = game.getTreacheryDeck().stream()
                .filter(t -> t.name().equals("Kulon"))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Kulon not found"));
        faction.addTreacheryCard(kulon);
        assertEquals(10, faction.getSpice());
        assertTrue(faction.hasTreacheryCard("Kulon"));
        assertTrue(turnSummary.getMessages().isEmpty());

        faction.kaitainHighDiscard("Kulon");

        assertEquals(8, faction.getSpice());
        assertFalse(faction.hasTreacheryCard("Kulon"));
        assertEquals("You discarded Kulon.", chat.getMessages().getLast());
        assertEquals(Emojis.EMPEROR + " discards Kulon and pays 2 " + Emojis.SPICE + " with Kaitain High Threshold ability.", turnSummary.getMessages().getFirst());
    }

    @Test
    public void sardaukarGetRemovedToSalusaSecundus() {
        Territory territory = game.getTerritory("Habbanya Sietch");

        int regularAmount = 2;
        faction.removeReserves(regularAmount);
        territory.addForces("Emperor", regularAmount);

        int specialAmount = 1;
        faction.removeSpecialReserves(specialAmount);
        territory.addForces("Emperor*", specialAmount);

        assertEquals(13, game.getTerritory("Kaitain").getForceStrength("Emperor"));
        assertEquals(4, game.getTerritory("Salusa Secundus").getForceStrength("Emperor*"));

        faction.removeForces("Habbanya Sietch", 2, false, false);
        faction.removeForces("Habbanya Sietch", 1, true, false);

        assertEquals(15, game.getTerritory("Kaitain").getForceStrength("Emperor"));
        assertEquals(5, game.getTerritory("Salusa Secundus").getForceStrength("Emperor*"));
    }

    @Test
    public void testRemovePullsFromSecundusIfNecessary() {
        Territory kaitain = game.getTerritory("Kaitain");
        Territory salusaSecundus = game.getTerritory("Salusa Secundus");
        int forcesToRemove = kaitain.getForceStrength(faction.getName()) - 1;
        kaitain.removeForces(game, faction.getName(), forcesToRemove);
        salusaSecundus.addForces(faction.getName(), 1);
        assertDoesNotThrow(() -> faction.removeReserves(2));
    }

    @Test
    public void testRemoveSpecialPullsFromKaitainIfNecessary() {
        Territory salusaSecundus = game.getTerritory("Salusa Secundus");
        Territory kaitain = game.getTerritory("Kaitain");
        int forcesToRemove = salusaSecundus.getForceStrength(faction.getName() + "*") - 1;
        salusaSecundus.removeForces(game, faction.getName() + "*", forcesToRemove);
        kaitain.addForces(faction.getName() + "*", 1);
        assertDoesNotThrow(() -> faction.removeSpecialReserves(2));
    }

    @Test
    public void testSecondHomeworld() {
        String homeworldName = faction.getSecondHomeworld();
        Territory territory = game.getTerritories().get(homeworldName);
        assertNotNull(territory);
        assertEquals(homeworldName, territory.getTerritoryName());
        assertEquals(-1, territory.getSector());
        assertFalse(territory.isStronghold());
        assertInstanceOf(HomeworldTerritory.class, territory);
        assertFalse(territory.isDiscoveryToken());
        assertFalse(territory.isNearShieldWall());
        assertFalse(territory.isRock());
    }

    @Nested
    @DisplayName("#homeworld")
    class Homeworld extends FactionTestTemplate.Homeworld {
        @Test
        public void testSecondHomweworldDialAdvantageHighThreshold() {
            HomeworldTerritory secondTerritory = (HomeworldTerritory) game.getTerritories().get(faction.getSecondHomeworld());
            assertEquals(0, faction.homeworldDialAdvantage(game, secondTerritory));
            game.addGameOption(GameOption.HOMEWORLDS);
            assertEquals(3, faction.homeworldDialAdvantage(game, secondTerritory));
        }

        @Test
        @Override
        public void testHomweworldDialAdvantageLowThreshold() {
            int numForces = territory.getForceStrength(faction.getName());
            int numSpecials = territory.getForceStrength(faction.getName() + "*");
            game.removeForces(territory.getTerritoryName(), faction, numForces, numSpecials, true);
            assertEquals(0, getFaction().homeworldDialAdvantage(game, territory));
            game.addGameOption(GameOption.HOMEWORLDS);
            faction.checkForLowThreshold();
            assertFalse(faction.isHighThreshold());
            assertEquals(3, faction.homeworldDialAdvantage(game, territory));
        }

        @Test
        public void testSecondHomweworldDialAdvantageLowThreshold() {
            HomeworldTerritory secondTerritory = (HomeworldTerritory) game.getTerritories().get(faction.getSecondHomeworld());
            int numSpecials = secondTerritory.getForceStrength(faction.getName() + "*");
            game.removeForces(secondTerritory.getTerritoryName(), faction, 0, numSpecials, true);
            assertEquals(0, faction.homeworldDialAdvantage(game, secondTerritory));
            game.addGameOption(GameOption.HOMEWORLDS);
            faction.checkForLowThreshold();
            assertEquals(2, faction.homeworldDialAdvantage(game, secondTerritory));
        }
    }

    @Nested
    @DisplayName("#secondHomeworldOccupation")
    class SecondHomeworldOccupation {
        @BeforeEach
        public void setUp() throws IOException {
            game.addFaction(new HarkonnenFaction("p", "u"));
        }

        @Test
        public void testNoReturnToHighThresholdWhileOccupied() {
            String homeworldName = faction.getSecondHomeworld();
            HomeworldTerritory territory = (HomeworldTerritory) game.getTerritories().get(homeworldName);
            game.addGameOption(GameOption.HOMEWORLDS);
            territory.removeForces(game, faction.getName() + "*", 5);
            assertFalse(faction.isSecundusHighThreshold());
            territory.addForces("Harkonnen", 1);
            territory.addForces(faction.getName() + "*", 3);
            assertTrue(faction.isSecundusOccupied());
            assertFalse(faction.isSecundusHighThreshold());
        }

        @Test
        public void testNoReturnToHighThresholdWith2Sardaukar() {
            String homeworldName = faction.getSecondHomeworld();
            HomeworldTerritory territory = (HomeworldTerritory) game.getTerritories().get(homeworldName);
            game.addGameOption(GameOption.HOMEWORLDS);
            territory.removeForces(game, faction.getName() + "*", 5);
            assertFalse(faction.isSecundusHighThreshold());
            territory.addForces("Harkonnen", 1);
            territory.addForces(faction.getName() + "*", 3);
            assertFalse(faction.isSecundusHighThreshold());
            assertTrue(faction.isSecundusOccupied());
            territory.removeForces(game, faction.getName() + "*", 1);
            territory.removeForces(game, "Harkonnen", 1);
            assertFalse(faction.isSecundusHighThreshold());
            assertFalse(faction.isSecundusOccupied());
        }

        @Test
        public void testReturnToHighThresholdWith3Sardaukar() {
            String homeworldName = faction.getSecondHomeworld();
            HomeworldTerritory territory = (HomeworldTerritory) game.getTerritories().get(homeworldName);
            game.addGameOption(GameOption.HOMEWORLDS);
            territory.removeForces(game, faction.getName() + "*", 5);
            assertFalse(faction.isSecundusHighThreshold());
            territory.addForces("Harkonnen", 1);
            territory.addForces(faction.getName() + "*", 3);
            assertFalse(faction.isSecundusHighThreshold());
            assertTrue(faction.isSecundusOccupied());
            territory.removeForces(game, "Harkonnen", 1);
            assertTrue(faction.isSecundusHighThreshold());
            assertFalse(faction.isSecundusOccupied());
        }
    }

    @Test
    void testGetSpiceSupportPhasesString() {
        assertEquals(" for bidding, shipping, and battles!", getFaction().getSpiceSupportPhasesString());
    }
}