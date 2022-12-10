package controller;

import model.*;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.json.JSONArray;
import org.json.JSONObject;

import java.awt.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.IntStream;

public class Initializers {

    public static void newFaction(Faction faction, Game gameState) throws IOException {
        Resource<Integer> spiceResource = new Resource<>("spice", 0);
        Resource<Integer> freeRevival = new Resource<>("free_revival", 0);
        Force reserves = new Force(faction.getName(), 0);
        Resource<Integer> frontOfShieldSpice = new Resource<>("front_of_shield_spice", 0);

        faction.addResource(spiceResource);
        faction.addResource(freeRevival);
        faction.addResource(reserves);
        faction.addResource(frontOfShieldSpice);

        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(
                Objects.requireNonNull(Initializers.class.getResourceAsStream("Leaders.csv"))
        ));
        CSVParser csvParser = CSVParser.parse(bufferedReader, CSVFormat.EXCEL);

        Deck traitorDeck = gameState.getDeck("traitor");

        for (CSVRecord csvRecord : csvParser) {
            if (csvRecord.get("faction").equals(faction.getName())) {
                TraitorCard traitorCard = new TraitorCard(
                        csvRecord.get("name"),
                        csvRecord.get("faction"),
                        Integer.parseInt(csvRecord.get("strength"))
                );
                faction.addResource(new Leader(csvRecord.get("name"), Integer.parseInt(csvRecord.get("strength"))));
                traitorDeck.addCard(traitorCard);
            }
        }

        gameState.addFaction(faction);
        switch (faction.getName()) {
            case "Atreides" -> {
                spiceResource.setValue(10);
                freeRevival.setValue(2);
                reserves.setValue(10);
                faction.setEmoji("<:atreides:991763327996923997>");
                faction.addResource(new IntegerResource("forces_lost", 0, 0, 7));
                gameState.getTerritories().get("Arrakeen").getForces().add(new Force("Atreides", 10));
            }
            case "Harkonnen" -> {
                spiceResource.setValue(10);
                freeRevival.setValue(2);
                reserves.setValue(10);
                faction.setEmoji("<:harkonnen:991763320333926551>");
                gameState.getTerritories().get("Carthag").getForces().add(new Force("Carthag", 10));
            }
            case "Emperor" -> {
                spiceResource.setValue(10);
                freeRevival.setValue(1);
                reserves.setValue(15);
                faction.addResource(new Force("Sardaukar", 5));
                faction.setEmoji("<:emperor:991763323454500914>");
            }
            case "Fremen" -> {
                spiceResource.setValue(3);
                freeRevival.setValue(3);
                reserves.setValue(17);
                faction.addResource(new Force("Fedaykin", 3));
                faction.setEmoji("<:fremen:991763322225577984>");
            }
            case "BG" -> {
                spiceResource.setValue(5);
                freeRevival.setValue(1);
                reserves.setValue(20);
                faction.setEmoji("<:bg:991763326830911519>");
            }
            case "Guild" -> {
                spiceResource.setValue(5);
                freeRevival.setValue(1);
                reserves.setValue(15);
                faction.setEmoji("<:guild:991763321290244096>");
                gameState.getTerritories().get("Tuek's Sietch").getForces().add(new Force("Guild", 5));
            }
            case "Ix" -> {
                spiceResource.setValue(10);
                freeRevival.setValue(1);
                reserves.setValue(10);
                faction.addResource(new Force("Cyborg", 4));
                faction.setEmoji("<:ix:991763319406997514>");
                gameState.getTerritories().get("Hidden Mobile Stronghold").getForces().add(new Force("Ix", 3));
                gameState.getTerritories().get("Hidden Mobile Stronghold").getForces().add(new Force("Cyborg", 3));
            }
            case "BT" -> {
                spiceResource.setValue(5);
                freeRevival.setValue(2);
                reserves.setValue(20);
                faction.setEmoji("<:bt:991763325576810546>");
            }
            case "CHOAM" -> {
                spiceResource.setValue(2);
                freeRevival.setValue(0);
                reserves.setValue(20);
                faction.setEmoji("<:choam:991763324624703538>");
                faction.addResource(new Resource("inflation", "behind shield"));
            }
            case "Rich" -> {
                spiceResource.setValue(5);
                freeRevival.setValue(2);
                reserves.setValue(20);
                faction.setEmoji("<:rich:991763318467465337>");
                faction.addResource( new IntegerResource("no field", 0, 0, 0));
                faction.addResource(new IntegerResource("no field", 3, 3, 3));
                faction.addResource(new IntegerResource("no field", 5, 5, 5));
                faction.addResource(new Resource<List<TreacheryCard>>("cache", new ArrayList<>()));
                List<TreacheryCard> cache = (List<TreacheryCard>) faction.getResource("cache");
                cache.add(new TreacheryCard("Ornithoper", "Special - Movement"));
                cache.add(new TreacheryCard("Residual Poison", "Special"));
                cache.add(new TreacheryCard("Semuta Drug", "Special"));
                cache.add(new TreacheryCard("Stone Burner", "Weapon - Special"));
                cache.add(new TreacheryCard("Mirror Weapon", "Weapon - Special"));
                cache.add(new TreacheryCard("Portable Snooper", "Defense - Poison"));
                cache.add(new TreacheryCard("Distrans", "Special"));
                cache.add(new TreacheryCard("Juice of Sapho", "Special"));
                cache.add(new TreacheryCard("Karama", "Special"));
                cache.add(new TreacheryCard("Nullentropy", "Special"));
            }
        }
    }

    public static Deck buildSpiceDeck() throws IOException {
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(
                Objects.requireNonNull(Initializers.class.getClassLoader().getResourceAsStream("StormCards.csv"))
        ));
        CSVParser csvParser = CSVParser.parse(bufferedReader, CSVFormat.RFC4180.builder().setHeader().setSkipHeaderRecord(true).build());

        Deck spiceDeck = new Deck("spice");

        for (CSVRecord csvRecord : csvParser) {
            SpiceCard spiceCard = new SpiceCard(
                    csvRecord.get(0),
                    csvRecord.get(1),
                    Integer.parseInt(csvRecord.get(2)),
                    Integer.parseInt(csvRecord.get(3)),
                    false
            );
            spiceDeck.addCard(spiceCard);
        }

        IntStream.range(0,6).forEach(i -> spiceDeck.addCard(new SpiceCard("Shai-Hulud")));

        return spiceDeck;
    }

    public static Deck buildTreacheryDeck() throws IOException {
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(
                Objects.requireNonNull(Initializers.class.getClassLoader().getResourceAsStream("TreacheryCards.csv"))
        ));
        CSVParser csvParser = CSVParser.parse(bufferedReader, CSVFormat.RFC4180.builder().setHeader().setSkipHeaderRecord(true).build());

        Deck treacheryDeck = new Deck("treachery");

        for (CSVRecord csvRecord : csvParser) {
            TreacheryCard treacheryCard = new TreacheryCard(
                    csvRecord.get(0),
                    csvRecord.get(1)
            );
            treacheryDeck.addCard(treacheryCard);
        }

        return treacheryDeck;
    }

    public static Deck buildStormDeck() {
        Deck stormDeck = new Deck("storm");

        IntStream.range(1,7).forEach(i -> stormDeck.addCard(new StormCard(i)));

        return stormDeck;
    }

    public static List<Point> getPoints(String territory) {
        List<Point> points = new ArrayList<>();

        switch (territory) {

            case "Carthag" -> {
                points.add(new Point(464,287));
                points.add(new Point(464,245));
                points.add(new Point(442,264));
                points.add(new Point(485,264));
            }
            case "Tuek's Sietch" -> {
                points.add(new Point(769, 676));
                points.add(new Point(761, 730));
                points.add(new Point(795, 698));
                points.add(new Point(748, 695));
            }
            case "Arrakeen" -> {
                points.add(new Point(601, 255));
                points.add(new Point(605, 214));
                points.add(new Point(577, 229));
                points.add(new Point(626, 229));
            }
            case "Sietch Tabr" -> {
                points.add(new Point(177,339));
                points.add(new Point(150,363));
                points.add(new Point(198,372));
            }
            case "Habbanya Sietch" -> {
                points.add(new Point(188, 702));
                points.add(new Point(188, 738));
                points.add(new Point(165, 710));
            }
            case "Cielago North(1)" -> {
                points.add(new Point(474, 721));
            }
            case "Cielago Depression(1)" -> {
                points.add(new Point(443, 805));
                points.add(new Point(492,803));

            }
            case "Meridian(1)" -> {
                points.add(new Point(420,913));
            }
            case "Cielago South(1)" -> {
                points.add(new Point(468,898));
                points.add(new Point(497,859));
                points.add(new Point(508,898));
            }
            case "Cielago North(2)" -> {
                points.add(new Point(521,719));
                points.add(new Point(509,639));
                points.add(new Point(543,700));
            }
            case "Cielago Depression(2)" -> {
                points.add(new Point(544,797));
            }
            case "Cielago South(2)" -> {
                points.add(new Point(551,858));

            }
            case "Cielago East(2)" -> {
                points.add(new Point(601,892));
                points.add(new Point(607,806));
            }
            case "Harg Pass(3)" -> {
                points.add(new Point(535,616));
                points.add(new Point(517,585));
            }
            case "False Wall South(3)" -> {
                points.add(new Point(624,710));
                points.add(new Point(662,731));
                points.add(new Point(697,765));
                points.add(new Point(583,669));
            }
            case "Cielago East(3)" -> {
                points.add(new Point(581,859));
                points.add(new Point(645,803));
            }
            case "South Mesa(3)" -> {
                points.add(new Point(552,800));
                points.add(new Point(731,832));

            }
            case "Harg Pass(4)" -> {
                points.add(new Point(570, 600));
                points.add(new Point(561,583));
            }
            case "False Wall East(4)" -> {
                points.add(new Point(530,562));
                points.add(new Point(545,565));
            }
            case "The Minor Erg(4)" -> {
                points.add(new Point(621,607));
                points.add(new Point(626,626));
            }
            case "False Wall South(4)" -> {
                points.add(new Point(650,655));
                points.add(new Point(681,669));
                points.add(new Point(710,710));
            }
            case "Pasty Mesa(4)" -> {
                points.add(new Point(663,611));
                points.add(new Point(686,619));
                points.add(new Point(722,634));
            }
            case "South Mesa(4)" -> {
                points.add(new Point(785,766));
                points.add(new Point(826,683));
                points.add(new Point(805,748));
                points.add(new Point(827,718));
            }
            case "False Wall East(5)" -> {
                points.add(new Point(538,543));
                points.add(new Point(561,539));
            }
            case "The Minor Erg(5)" -> {
                points.add(new Point(610,550));
                points.add(new Point(632,643));
                points.add(new Point(580,560));
            }
            case "Pasty Mesa(5)" -> {
                points.add(new Point(530,570));
                points.add(new Point(675,570));
                points.add(new Point(800,575));
                points.add(new Point(750,610));
            }
            case "South Mesa(5)" -> {
                points.add(new Point(845,645));
                points.add(new Point(860,560));
            }
            case "False Wall East(6)" -> {
                points.add(new Point(548,521));
                points.add(new Point(565,505));
            }
            case "The Minor Erg(6)" -> {
                points.add(new Point(615,500));
                points.add(new Point(590,510));
                points.add(new Point(645,495));
            }
            case "Pasty Mesa(6)" -> {
                points.add(new Point(730,480));
                points.add(new Point(760,470));
                points.add(new Point(795,445));
                points.add(new Point(680,510));
            }
            case "Red Chasm" -> {
                points.add(new Point(840,495));
                points.add(new Point(845,465));
                points.add(new Point(865,515));
            }
            case "False Wall East(7)" -> {
                points.add(new Point(535,485));
                points.add(new Point(560,485));
            }
            case "The Minor Erg(7)" -> {
                points.add(new Point(578,475));
                points.add(new Point(595,472));
                points.add(new Point(624,458));
                points.add(new Point(626,443));
            }
            case "Shield Wall(7)" -> {
                points.add(new Point(636,400));
                points.add(new Point(620,415));
                points.add(new Point(672,370));
                points.add(new Point(590,440));
            }
            case "Pasty Mesa(7)" -> {
                points.add(new Point(720,400));
                points.add(new Point(780,390));
                points.add(new Point(675,435));
                points.add(new Point(825,385));
            }
            case "Gara Kulon" -> {
                points.add(new Point(780,300));
                points.add(new Point(755,342));
            }
            case "False Wall East(8)" -> {
                points.add(new Point(521,476));
                points.add(new Point(534,463));
            }
            case "Imperial Basin(8)" -> {
                points.add(new Point(548,413));
                points.add(new Point(716,366));
            }
            case "Shield Wall(8)" -> {
                points.add(new Point(580,410));
                points.add(new Point(607,392));
                points.add(new Point(665,355));
            }
            case "Hole in the Rock" -> {
                points.add(new Point(660,320));
            }
            case "Rim Wall West" -> {
                points.add(new Point(630,290));
            }
            case "Basin" -> {
                points.add(new Point(690,245));
            }
            case "Old Gap(8)" -> {
                points.add(new Point(666,213));
                points.add(new Point(685,190));
            }
            case "Sihaya Ridge" -> {
                points.add(new Point(740,245));
                points.add(new Point(727,273));
                points.add(new Point(764,266));
            }
            case "Imperial Basin(9)" -> {
                points.add(new Point(527,367));
                points.add(new Point(513,403));
                points.add(new Point(553,296));
            }
            case "Old Gap(9)" -> {
                points.add(new Point(600,150));
                points.add(new Point(560,150));
                points.add(new Point(638,182));
            }
            case "Imperial Basin(10)" -> {
                points.add(new Point(495,370));
            }
            case "Arsunt(10)" -> {
                points.add(new Point(460,380));
                points.add(new Point(455,340));
            }
            case "Tsimpo(10)" -> {
                points.add(new Point(440,190));
                points.add(new Point(480,185));
            }
            case "Broken Land(10)" -> {
                points.add(new Point(455,135));
                points.add(new Point(480,135));
            }
            case "Old Gap(10)" -> {
                points.add(new Point(525,140));
            }
            case "Arsunt(11)" -> {
                points.add(new Point(447,457));
            }
            case "Hagga Basin(11)" -> {
                points.add(new Point(380,340));
                points.add(new Point(410,325));
            }
            case "Tsimpo(11)" -> {
                points.add(new Point(390,245));
            }
            case "Plastic Basin(11)" -> {
                points.add(new Point(315,228));
                points.add(new Point(343,213));
            }
            case "Broken Land(11)" -> {
                points.add(new Point(295,177));
                points.add(new Point(370,158));
                points.add(new Point(330,176));
            }
            case "Hagga Basin(12)" -> {
                points.add(new Point(387,436));
                points.add(new Point(330,379));
                points.add(new Point(339,345));
            }
            case "Plastic Basin(12)" -> {
                points.add(new Point(245,290));
                points.add(new Point(270,255));
            }
            case "Tsimpo(12)" -> {
                points.add(new Point(320,305));
            }
            case "Rock Outcroppings(12)" -> {
                points.add(new Point(195,274));
                points.add(new Point(232,222));
            }
            case "Wind Pass(13)" -> {
                points.add(new Point(390,490));
            }
            case "Plastic Basin(13)" -> {
                points.add(new Point(250,410));
                points.add(new Point(300,440));
            }
            case "Bight of the Cliff(13)" -> {
                points.add(new Point(105,375));
                points.add(new Point(170,405));
            }
            case "Rock Outcroppings(13)" -> {
                points.add(new Point(145,315));
                points.add(new Point(155,295));
                points.add(new Point(160,275));
            }
            case "Wind Pass(14)" -> {
                points.add(new Point(375,520));
            }
            case "The Great Flat" -> {
                points.add(new Point(140,515));
                points.add(new Point(290,515));
                points.add(new Point(215,515));
            }
            case "Funeral Plain" -> {
                points.add(new Point(160,470));
                points.add(new Point(90,455));
                points.add(new Point(225,460));
            }
            case "Bight of the Cliff(14)" -> {
                points.add(new Point(105,420));
            }
            case "Wind Pass(15)" -> {
                points.add(new Point(360,555));
            }
            case "The Greater Flat" -> {
                points.add(new Point(115,575));
                points.add(new Point(250,560));
            }
            case "Habbanya Erg(15)" -> {
                points.add(new Point(140,630));
                points.add(new Point(215,600));
                points.add(new Point(90,625));
            }
            case "False Wall West(15)" -> {
                points.add(new Point(300,580));
                points.add(new Point(320,575));
            }
            case "Wind Pass North(16)" -> {
                points.add(new Point(380,590));
                points.add(new Point(373,601));
                points.add(new Point(406,573));
            }
            case "Wind Pass(16)" -> {
                points.add(new Point(350,615));
            }
            case "False Wall West(16)" -> {
                points.add(new Point(305,615));
                points.add(new Point(287,631));
            }
            case "Habbanya Erg(16)" -> {
                points.add(new Point(230,640));
            }
            case "Habbanya Ridge Flat(16)" -> {
                points.add(new Point(125,710));
            }
            case "Wind Pass North(17)" -> {
                points.add(new Point(377,655));
            }
            case "Cielago West(17)" -> {
                points.add(new Point(335,710));
            }
            case "False Wall West(17)" -> {
                points.add(new Point(285,705));
                points.add(new Point(274,733));
            }
            case "Habbanya Ridge Flat(17)" -> {
                points.add(new Point(245,850));
                points.add(new Point(265,795));
                points.add(new Point(230,765));
            }
            case "Cielago North(18)" -> {
                points.add(new Point(410,720));
            }
            case "Cielago Depression(18)" -> {
                points.add(new Point(385,785));
            }
            case "Cielago West(18)" -> {
                points.add(new Point(330,805));
            }
            case "Meridian(18)" -> {
                points.add(new Point(360,890));
            }
            case "Polar Sink" -> {
                points.add(new Point(460,545));
                points.add(new Point(465,515));
                points.add(new Point(415,550));
                points.add(new Point(500,520));
            }
            case "Forces Tanks" -> {
                points.add(new Point(45,990));
                points.add(new Point(100,990));
            }
            case "Leaders Tanks" -> {
                points.add(new Point(900,985));
                points.add(new Point(860,985));
                points.add(new Point(820,985));
                points.add(new Point(780,985));
                points.add(new Point(740,985));
                points.add(new Point(700,985));
                points.add(new Point(660,1010));
            }
        }
        return points;
    }

    public static Point getDrawCoordinates(String location) {
        switch (location) {
            case "sigil 1" -> {
                return new Point(475, 978);
            }
            case "sigil 2" -> {
                return new Point(865, 753);
            }
            case "sigil 3" -> {
                return new Point(865, 301);
            }
            case "sigil 4" -> {
                return new Point(475, 80);
            }
            case "sigil 5" -> {
                return new Point(85, 301);
            }
            case "sigil 6" -> {
                return new Point(85, 753);
            }
            case "turn 0", "turn 1" -> {
                return new Point(124, 60);
            }
            case "turn 2" -> {
                return new Point(148, 75);
            }
            case "turn 3" -> {
                return new Point(160, 105);
            }
            case "turn 4" -> {
                return new Point(148,135);
            }
            case "turn 5" -> {
                return new Point(124,155);
            }
            case "turn 6" -> {
                return new Point(95,150);
            }
            case "turn 7" -> {
                return new Point(67,135);
            }
            case "turn 8" -> {
                return new Point(60,108);
            }
            case "turn 9" -> {
                return new Point(65,80);
            }
            case "turn 10" -> {
                return new Point(93,60);
            }
            case "phase 1" -> {
                return new Point(237, 45);
            }
            case "phase 2" -> {
                return new Point(297,45);
            }
            case "phase 3" -> {
                return new Point(357,45);
            }
            case "phase 4" -> {
                return new Point(417,45);
            }
            case "phase 5" -> {
                return new Point(533,45);
            }
            case "phase 6" -> {
                return new Point(593,45);
            }
            case "phase 7" -> {
                return new Point(653,45);
            }
            case "phase 8" -> {
                return new Point(713,45);
            }
            case "storm 1" -> {
                return new Point(487,958);
            }
            case "storm 2" -> {
                return new Point(635,927);
            }
            case "storm 3" -> {
                return new Point(760,850);
            }
            case "storm 4" -> {
                return new Point(858,732);
            }
            case "storm 5" -> {
                return new Point(899,589);
            }
            case "storm 6" -> {
                return new Point(898,441);
            }
            case "storm 7" -> {
                return new Point(843,300);
            }
            case "storm 8" -> {
                return new Point(740,190);
            }
            case "storm 9" -> {
                return new Point(610,120);
            }
            case "storm 10" -> {
                return new Point(463,98);
            }
            case "storm 11" -> {
                return new Point(318,129);
            }
            case "storm 12" -> {
                return new Point(189,207);
            }
            case "storm 13" -> {
                return new Point(96,323);
            }
            case "storm 14" -> {
                return new Point(49,464);
            }
            case "storm 15" -> {
                return new Point(53,614);
            }
            case "storm 16" -> {
                return new Point(107,755);
            }
            case "storm 17" -> {
                return new Point(210,865);
            }
            case "storm 18" -> {
                return new Point(340,940);
            }
        }

        return new Point(0, 0);
    }
}
