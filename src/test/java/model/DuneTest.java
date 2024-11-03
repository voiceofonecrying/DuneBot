package model;

import exceptions.InvalidGameStateException;
import model.factions.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

public class DuneTest {
    Game game;
    TleilaxuTanks tanks;
    TestTopic turnSummary;
    TestTopic modInfo;
    TestTopic gameActions;

    AtreidesFaction atreides;
    TestTopic atreidesChat;
    TestTopic atreidesLedger;

    BGFaction bg;
    TestTopic bgChat;
    TestTopic bgLedger;

    BTFaction bt;
    TestTopic btChat;
    TestTopic btLedger;

    ChoamFaction choam;
    TestTopic choamChat;
    TestTopic choamLedger;

    EcazFaction ecaz;
    TestTopic ecazChat;
    TestTopic ecazLedger;

    EmperorFaction emperor;
    TestTopic emperorChat;
    TestTopic emperorLedger;

    FremenFaction fremen;
    TestTopic fremenChat;
    TestTopic fremenLedger;

    GuildFaction guild;
    TestTopic guildChat;
    TestTopic guildLedger;

    HarkonnenFaction harkonnen;
    TestTopic harkonnenChat;
    TestTopic harkonnenLedger;

    IxFaction ix;
    TestTopic ixChat;
    TestTopic ixLedger;

    MoritaniFaction moritani;
    TestTopic moritaniChat;
    TestTopic moritaniLedger;

    RicheseFaction richese;
    TestTopic richeseChat;
    TestTopic richeseLedger;

    Leader duncanIdaho;
    Leader ladyJessica;
    Leader alia;
    Leader zoal;
    Leader wykk;
    Leader auditor;
    Leader chani;
    Leader burseg;
    Leader bashar;
    Leader feydRautha;
    Leader ummanKudu;
    Leader cammarPilru;
    Leader bindikkNarvi;
    Leader ladyHelena;
    Leader dukeVidal;

    Territory arrakeen;
    Territory carthag;
    Territory sietchTabr;
    Territory habbanyaSietch;
    Territory tueksSietch;
    Territory hms;
    Territory polarSink;
    Territory funeralPlain;
    Territory garaKulon;
    Territory sihayaRidge;
    Territory redChasm;
    Territory cielagoNorth_westSector;
    Territory cielagoNorth_middleSector;
    Territory cielagoNorth_eastSector;
    Territory falseWallEast_farNorthSector;
    Territory falseWallEast_middleSector;
    Territory falseWallEast_southSector;
    Territory falseWallEast_farSouthSector;
    Territory meridian_westSector;
    Territory meridian_eastSector;
    Territory windPassNorth_northSector;
    Territory windPassNorth_southSector;
    Territory windPass_northSector;

    TreacheryCard crysknife;
    TreacheryCard chaumas;
    TreacheryCard lasgun;
    TreacheryCard shield;
    TreacheryCard snooper;
    TreacheryCard cheapHero;
    TreacheryCard familyAtomics;
    TreacheryCard weatherControl;
    TreacheryCard baliset;
    TreacheryCard kulon;
    TreacheryCard harassAndWithdraw;
    TreacheryCard reinforcements;

    @BeforeEach
    void setUp() throws IOException, InvalidGameStateException {
        game = new Game();
        tanks = game.getTleilaxuTanks();
        turnSummary = new TestTopic();
        game.setTurnSummary(turnSummary);
        modInfo = new TestTopic();
        game.setModInfo(modInfo);
        gameActions = new TestTopic();
        game.setGameActions(gameActions);

        atreides = new AtreidesFaction("at", "at");
        atreidesChat = new TestTopic();
        atreides.setChat(atreidesChat);
        atreidesLedger = new TestTopic();
        atreides.setLedger(atreidesLedger);

        bg = new BGFaction("bg", "bg");
        bgChat = new TestTopic();
        bg.setChat(bgChat);
        bgLedger = new TestTopic();
        bg.setLedger(bgLedger);

        bt = new BTFaction("bt", "bt");
        btChat = new TestTopic();
        bt.setChat(btChat);
        btLedger = new TestTopic();
        bt.setLedger(btLedger);

        choam = new ChoamFaction("ch", "ch");
        choamChat = new TestTopic();
        choam.setChat(choamChat);
        choamLedger = new TestTopic();
        choam.setLedger(choamLedger);

        ecaz = new EcazFaction("ec", "ec");
        ecazChat = new TestTopic();
        ecaz.setChat(ecazChat);
        ecazLedger = new TestTopic();
        ecaz.setLedger(ecazLedger);

        emperor = new EmperorFaction("em", "em");
        emperorChat = new TestTopic();
        emperor.setChat(emperorChat);
        emperorLedger = new TestTopic();
        emperor.setLedger(emperorLedger);

        fremen = new FremenFaction("fr", "fr");
        fremenChat = new TestTopic();
        fremen.setChat(fremenChat);
        fremenLedger = new TestTopic();
        fremen.setLedger(fremenLedger);

        guild = new GuildFaction("gu", "gu");
        guildChat = new TestTopic();
        guild.setChat(guildChat);
        guildLedger = new TestTopic();
        guild.setLedger(guildLedger);

        harkonnen = new HarkonnenFaction("ha", "ha");
        harkonnenChat = new TestTopic();
        harkonnen.setChat(harkonnenChat);
        harkonnenLedger = new TestTopic();
        harkonnen.setLedger(harkonnenLedger);

        ix = new IxFaction("ix", "ix");
        ixChat = new TestTopic();
        ix.setChat(ixChat);
        ixLedger = new TestTopic();
        ix.setLedger(ixLedger);

        moritani = new MoritaniFaction("mo", "mo");
        moritaniChat = new TestTopic();
        moritani.setChat(moritaniChat);
        moritaniLedger = new TestTopic();
        moritani.setLedger(moritaniLedger);

        richese = new RicheseFaction("ri", "ri");
        richeseChat = new TestTopic();
        richese.setChat(richeseChat);
        richeseLedger = new TestTopic();
        richese.setLedger(richeseLedger);

        duncanIdaho = atreides.getLeader("Duncan Idaho").orElseThrow();
        ladyJessica = atreides.getLeader("Lady Jessica").orElseThrow();
        alia = bg.getLeader("Alia").orElseThrow();
        zoal = bt.getLeader("Zoal").orElseThrow();
        wykk = bt.getLeader("Wykk").orElseThrow();
        auditor = choam.getLeader("Auditor").orElseThrow();
        chani = fremen.getLeader("Chani").orElseThrow();
        burseg = emperor.getLeader("Burseg").orElseThrow();
        bashar = emperor.getLeader("Bashar").orElseThrow();
        feydRautha = harkonnen.getLeader("Feyd Rautha").orElseThrow();
        ummanKudu = harkonnen.getLeader("Umman Kudu").orElseThrow();
        cammarPilru = ix.getLeader("Cammar Pilru").orElseThrow();
        bindikkNarvi = ecaz.getLeader("Bindikk Narvi").orElseThrow();
        ladyHelena = richese.getLeader("Lady Helena").orElseThrow();
        dukeVidal = new Leader("Duke Vidal", 6, "None", null, false);

        arrakeen = game.getTerritory("Arrakeen");
        carthag = game.getTerritory("Carthag");
        sietchTabr = game.getTerritory("Sietch Tabr");
        habbanyaSietch = game.getTerritory("Habbanya Sietch");
        tueksSietch = game.getTerritory("Tuek's Sietch");
        hms = game.getTerritory("Hidden Mobile Stronghold");
        polarSink = game.getTerritory("Polar Sink");
        funeralPlain = game.getTerritory("Funeral Plain");
        garaKulon = game.getTerritory("Gara Kulon");
        sihayaRidge = game.getTerritory("Sihaya Ridge");
        redChasm = game.getTerritory("Red Chasm");
        cielagoNorth_westSector = game.getTerritory("Cielago North (West Sector)");
        cielagoNorth_middleSector = game.getTerritory("Cielago North (Center Sector)");
        cielagoNorth_eastSector = game.getTerritory("Cielago North (East Sector)");
        falseWallEast_farNorthSector = game.getTerritory("False Wall East (Far North Sector)");
        falseWallEast_middleSector = game.getTerritory("False Wall East (Middle Sector)");
        falseWallEast_southSector = game.getTerritory("False Wall East (South Sector)");
        falseWallEast_farSouthSector = game.getTerritory("False Wall East (Far South Sector)");
        meridian_westSector = game.getTerritory("Meridian (West Sector)");
        meridian_eastSector = game.getTerritory("Meridian (East Sector)");
        windPassNorth_northSector = game.getTerritory("Wind Pass North (North Sector)");
        windPassNorth_southSector = game.getTerritory("Wind Pass North (South Sector)");
        windPass_northSector = game.getTerritory("Wind Pass (North Sector)");

        crysknife = game.getTreacheryDeck().stream().filter(c -> c.name().equals("Crysknife")).findFirst().orElseThrow();
        chaumas = game.getTreacheryDeck().stream().filter(c -> c.name().equals("Chaumas")).findFirst().orElseThrow();
        lasgun = game.getTreacheryDeck().stream().filter(c -> c.name().equals("Lasgun")). findFirst().orElseThrow();
        shield = getTreacheryCard("Shield");
        snooper = new TreacheryCard("Snooper");
        cheapHero = game.getTreacheryDeck().stream().filter(c -> c.name().equals("Cheap Heroine")).findFirst().orElseThrow();
        familyAtomics = getTreacheryCard("Family Atomics");
        weatherControl = getTreacheryCard("Weather Control");
        baliset = new TreacheryCard("Baliset");
        kulon = getTreacheryCard("Kulon");
        harassAndWithdraw = new TreacheryCard("Harass and Withdraw");
        reinforcements = new TreacheryCard("Reinforcements");
    }

    TreacheryCard getTreacheryCard(String cardName) {
        return game.getTreacheryDeck().stream()
                .filter(t -> t.name().equals(cardName))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(cardName + " not found"));
    }

    void throwTestTopicMessages(TestTopic topic) {
        throw new RuntimeException("\n- " + String.join("\n- ", topic.getMessages()));
    }

    @Test
    void testGameAndFactionsCreatedAndTopicsAdded() {
        assertEquals(turnSummary, game.getTurnSummary());
        assertEquals(modInfo, game.getModInfo());
        assertEquals(gameActions, game.getGameActions());

        assertEquals(atreidesChat, atreides.getChat());
        assertEquals(atreidesLedger, atreides.getLedger());
        assertEquals(bgChat, bg.getChat());
        assertEquals(bgLedger, bg.getLedger());
        assertEquals(btChat, bt.getChat());
        assertEquals(btLedger, bt.getLedger());
        assertEquals(choamChat, choam.getChat());
        assertEquals(choamLedger, choam.getLedger());
        assertEquals(ecazChat, ecaz.getChat());
        assertEquals(ecazLedger, ecaz.getLedger());
        assertEquals(emperorChat, emperor.getChat());
        assertEquals(emperorLedger, emperor.getLedger());
        assertEquals(fremenChat, fremen.getChat());
        assertEquals(fremenLedger, fremen.getLedger());
        assertEquals(guildChat, guild.getChat());
        assertEquals(guildLedger, guild.getLedger());
        assertEquals(harkonnenChat, harkonnen.getChat());
        assertEquals(harkonnenLedger, harkonnen.getLedger());
        assertEquals(ixChat, ix.getChat());
        assertEquals(ixLedger, ix.getLedger());
        assertEquals(moritaniChat, moritani.getChat());
        assertEquals(moritaniLedger, moritani.getLedger());
        assertEquals(richeseChat, richese.getChat());
        assertEquals(richeseLedger, richese.getLedger());
    }
}
