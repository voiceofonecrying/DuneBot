package model;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;

import java.awt.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Initializers {

    public static CSVParser getCSVFile(String name) throws IOException {
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(Objects.requireNonNull(Initializers.class
                .getClassLoader().getResourceAsStream(name))));

        return CSVParser.parse(bufferedReader, CSVFormat.RFC4180.builder().setHeader().setSkipHeaderRecord(true).build());
    }

    public static String getJSONString(String name) throws IOException {
        InputStream inputStream = Game.class.getClassLoader().getResourceAsStream(name);
        int size = inputStream.available();
        byte[] buffer = new byte[size];
        inputStream.read(buffer);
        inputStream.close();
        return new String(buffer, StandardCharsets.UTF_8);
    }

    public static List<Point> getPoints(String territory) {
        List<Point> points = new ArrayList<>();

        switch (territory) {

            case "Carthag" -> {
                points.add(new Point(464, 287));
                points.add(new Point(464, 245));
                points.add(new Point(442, 264));
                points.add(new Point(485, 264));
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
                points.add(new Point(177, 339));
                points.add(new Point(150, 363));
                points.add(new Point(198, 372));
            }
            case "Habbanya Sietch" -> {
                points.add(new Point(188, 702));
                points.add(new Point(188, 738));
                points.add(new Point(165, 710));
            }
            case "Cielago North (Center Sector)" -> points.add(new Point(474, 721));
            case "Cielago Depression (Center Sector)" -> {
                points.add(new Point(443, 805));
                points.add(new Point(492, 803));

            }
            case "Meridian (East Sector)" -> points.add(new Point(420, 913));
            case "Cielago South (West Sector)" -> {
                points.add(new Point(468, 898));
                points.add(new Point(497, 859));
                points.add(new Point(508, 898));
            }
            case "Cielago North (East Sector)" -> {
                points.add(new Point(521, 719));
                points.add(new Point(509, 639));
                points.add(new Point(543, 700));
            }
            case "Cielago Depression (East Sector)" -> points.add(new Point(544, 797));
            case "Cielago South (East Sector)" -> points.add(new Point(551, 858));
            case "Cielago East (West Sector)" -> {
                points.add(new Point(601, 892));
                points.add(new Point(607, 806));
            }
            case "Harg Pass (West Sector)" -> {
                points.add(new Point(535, 616));
                points.add(new Point(517, 585));
            }
            case "False Wall South (West Sector)" -> {
                points.add(new Point(624, 710));
                points.add(new Point(662, 731));
                points.add(new Point(697, 765));
                points.add(new Point(583, 669));
            }
            case "Cielago East (East Sector)" -> {
                points.add(new Point(581, 859));
                points.add(new Point(645, 803));
            }
            case "South Mesa (South Sector)" -> {
                points.add(new Point(552, 800));
                points.add(new Point(731, 832));

            }
            case "Harg Pass (East Sector)" -> {
                points.add(new Point(570, 600));
                points.add(new Point(561, 583));
            }
            case "False Wall East (Far South Sector)" -> {
                points.add(new Point(530, 562));
                points.add(new Point(545, 565));
            }
            case "The Minor Erg (Far South Sector)" -> {
                points.add(new Point(621, 607));
                points.add(new Point(626, 626));
            }
            case "False Wall South (East Sector)" -> {
                points.add(new Point(650, 655));
                points.add(new Point(681, 669));
                points.add(new Point(710, 710));
            }
            case "Pasty Mesa (Far South Sector)" -> {
                points.add(new Point(663, 611));
                points.add(new Point(686, 619));
                points.add(new Point(722, 634));
            }
            case "South Mesa (Middle Sector)" -> {
                points.add(new Point(785, 766));
                points.add(new Point(826, 683));
                points.add(new Point(805, 748));
                points.add(new Point(827, 718));
            }
            case "False Wall East (South Sector)" -> {
                points.add(new Point(538, 543));
                points.add(new Point(561, 539));
            }
            case "The Minor Erg (South Sector)" -> {
                points.add(new Point(610, 550));
                points.add(new Point(632, 643));
                points.add(new Point(580, 560));
            }
            case "Pasty Mesa (South Sector)" -> {
                points.add(new Point(690, 570));
                points.add(new Point(750, 574));
                points.add(new Point(800, 575));
            }
            case "South Mesa (North Sector)" -> {
                points.add(new Point(845, 645));
                points.add(new Point(860, 560));
            }
            case "False Wall East (Middle Sector)" -> {
                points.add(new Point(548, 521));
                points.add(new Point(565, 505));
            }
            case "The Minor Erg (North Sector)" -> {
                points.add(new Point(615, 500));
                points.add(new Point(590, 510));
                points.add(new Point(645, 495));
            }
            case "Pasty Mesa (North Sector)" -> {
                points.add(new Point(730, 480));
                points.add(new Point(760, 470));
                points.add(new Point(795, 445));
                points.add(new Point(680, 510));
            }
            case "Red Chasm" -> {
                points.add(new Point(840, 495));
                points.add(new Point(845, 465));
                points.add(new Point(865, 515));
            }
            case "False Wall East (North Sector)" -> {
                points.add(new Point(535, 485));
                points.add(new Point(560, 485));
            }
            case "The Minor Erg (Far North Sector)" -> {
                points.add(new Point(578, 475));
                points.add(new Point(610, 472));
                points.add(new Point(624, 458));
                points.add(new Point(626, 443));
            }
            case "Shield Wall (South Sector)" -> {
                points.add(new Point(636, 400));
                points.add(new Point(620, 415));
                points.add(new Point(672, 370));
                points.add(new Point(590, 440));
            }
            case "Pasty Mesa (Far North Sector)" -> {
                points.add(new Point(720, 400));
                points.add(new Point(780, 390));
                points.add(new Point(675, 435));
                points.add(new Point(825, 385));
            }
            case "Gara Kulon" -> {
                points.add(new Point(780, 300));
                points.add(new Point(760, 340));
                points.add(new Point(810, 340));
            }
            case "False Wall East (Far North Sector)" -> {
                points.add(new Point(521, 476));
                points.add(new Point(534, 463));
            }
            case "Imperial Basin (East Sector)" -> {
                points.add(new Point(548, 413));
                points.add(new Point(716, 366));
            }
            case "Shield Wall (North Sector)" -> {
                points.add(new Point(580, 410));
                points.add(new Point(607, 392));
                points.add(new Point(665, 355));
            }
            case "Hole In The Rock" -> points.add(new Point(660, 320));
            case "Rim Wall West" -> points.add(new Point(630, 290));
            case "Basin" -> points.add(new Point(690, 245));
            case "Old Gap (East Sector)" -> {
                points.add(new Point(666, 213));
                points.add(new Point(685, 190));
            }
            case "Sihaya Ridge" -> {
                points.add(new Point(740, 245));
                points.add(new Point(727, 273));
                points.add(new Point(764, 266));
            }
            case "Imperial Basin (Center Sector)" -> {
                points.add(new Point(527, 367));
                points.add(new Point(513, 403));
                points.add(new Point(553, 296));
            }
            case "Old Gap (Middle Sector)" -> {
                points.add(new Point(600, 150));
                points.add(new Point(560, 150));
                points.add(new Point(638, 182));
            }
            case "Imperial Basin (West Sector)" -> points.add(new Point(495, 370));
            case "Arsunt (East Sector)" -> {
                points.add(new Point(460, 380));
                points.add(new Point(455, 340));
            }
            case "Tsimpo (East Sector)" -> {
                points.add(new Point(440, 190));
                points.add(new Point(480, 185));
            }
            case "Broken Land (East Sector)" -> {
                points.add(new Point(455, 135));
                points.add(new Point(480, 135));
            }
            case "Old Gap (West Sector)" -> points.add(new Point(525, 140));
            case "Arsunt (West Sector)" -> points.add(new Point(447, 457));
            case "Hagga Basin (East Sector)" -> {
                points.add(new Point(380, 340));
                points.add(new Point(410, 325));
            }
            case "Tsimpo (Middle Sector)" -> points.add(new Point(390, 245));
            case "Plastic Basin (North Sector)" -> {
                points.add(new Point(315, 228));
                points.add(new Point(343, 213));
            }
            case "Broken Land (West Sector)" -> {
                points.add(new Point(295, 177));
                points.add(new Point(370, 158));
                points.add(new Point(330, 176));
            }
            case "Hagga Basin (West Sector)" -> {
                points.add(new Point(387, 436));
                points.add(new Point(330, 379));
                points.add(new Point(339, 345));
            }
            case "Plastic Basin (Middle Sector)" -> {
                points.add(new Point(245, 290));
                points.add(new Point(270, 255));
            }
            case "Tsimpo (West Sector)" -> points.add(new Point(320, 305));
            case "Rock Outcroppings (North Sector)" -> {
                points.add(new Point(195, 274));
                points.add(new Point(232, 222));
            }
            case "Wind Pass (Far North Sector)" -> points.add(new Point(390, 490));
            case "Plastic Basin (South Sector)" -> {
                points.add(new Point(250, 410));
                points.add(new Point(300, 440));
            }
            case "Bight Of The Cliff (North Sector)" -> {
                points.add(new Point(105, 375));
                points.add(new Point(170, 405));
            }
            case "Rock Outcroppings (South Sector)" -> {
                points.add(new Point(145, 310));
                points.add(new Point(155, 295));
                points.add(new Point(160, 275));
            }
            case "Wind Pass (North Sector)" -> points.add(new Point(375, 520));
            case "The Great Flat" -> {
                points.add(new Point(140, 515));
                points.add(new Point(290, 515));
                points.add(new Point(215, 515));
            }
            case "Funeral Plain" -> {
                points.add(new Point(160, 470));
                points.add(new Point(90, 455));
                points.add(new Point(225, 460));
            }
            case "Bight Of The Cliff (South Sector)" -> points.add(new Point(105, 420));
            case "Wind Pass (South Sector)" -> points.add(new Point(360, 555));
            case "The Greater Flat" -> {
                points.add(new Point(115, 575));
                points.add(new Point(250, 560));
            }
            case "Habbanya Erg (West Sector)" -> {
                points.add(new Point(140, 630));
                points.add(new Point(215, 600));
                points.add(new Point(90, 625));
            }
            case "False Wall West (North Sector)" -> {
                points.add(new Point(300, 580));
                points.add(new Point(320, 575));
            }
            case "Wind Pass North (North Sector)" -> {
                points.add(new Point(380, 560));
                points.add(new Point(373, 601));
                points.add(new Point(406, 573));
            }
            case "Wind Pass (Far South Sector)" -> points.add(new Point(350, 615));
            case "False Wall West (Middle Sector)" -> {
                points.add(new Point(305, 615));
                points.add(new Point(287, 631));
            }
            case "Habbanya Erg (East Sector)" -> points.add(new Point(230, 640));
            case "Habbanya Ridge Flat (West Sector)" -> points.add(new Point(125, 710));
            case "Wind Pass North (South Sector)" -> points.add(new Point(377, 655));
            case "Cielago West (North Sector)" -> points.add(new Point(335, 710));
            case "False Wall West (South Sector)" -> {
                points.add(new Point(285, 705));
                points.add(new Point(274, 733));
            }
            case "Habbanya Ridge Flat (East Sector)" -> {
                points.add(new Point(245, 850));
                points.add(new Point(265, 795));
                points.add(new Point(230, 765));
            }
            case "Cielago North (West Sector)" -> points.add(new Point(410, 720));
            case "Cielago Depression (West Sector)" -> points.add(new Point(385, 785));
            case "Cielago West (South Sector)" -> points.add(new Point(330, 805));
            case "Meridian (West Sector)" -> {
                points.add(new Point(325, 860));
                points.add(new Point(370, 870));
            }
            case "Polar Sink" -> {
                points.add(new Point(460, 545));
                points.add(new Point(465, 515));
                points.add(new Point(415, 550));
                points.add(new Point(500, 520));
            }
            case "Forces Tanks" -> {
                points.add(new Point(45, 990));
                points.add(new Point(100, 990));
            }
            case "Leaders Tanks" -> {
                points.add(new Point(900, 985));
                points.add(new Point(860, 985));
                points.add(new Point(820, 985));
                points.add(new Point(780, 985));
                points.add(new Point(740, 985));
                points.add(new Point(700, 985));
                points.add(new Point(660, 1010));
            }
        }
        return points;
    }

    public static Point getDrawCoordinates(String location) {
        switch (location) {
            case "sigil 1" -> {
                return new Point(475, 978);
            }
            case "ally 1" -> {
                return new Point(475, 1040);
            }
            case "sigil 2" -> {
                return new Point(865, 753);
            }
            case "ally 2" -> {
                return new Point(915, 783);
            }
            case "sigil 3" -> {
                return new Point(865, 301);
            }
            case "ally 3" -> {
                return new Point(915, 271);
            }
            case "sigil 4" -> {
                return new Point(475, 80);
            }
            case "ally 4" -> {
                return new Point(475, 20);
            }
            case "sigil 5" -> {
                return new Point(85, 301);
            }
            case "ally 5" -> {
                return new Point(35, 271);
            }
            case "sigil 6" -> {
                return new Point(85, 753);
            }
            case "ally 6" -> {
                return new Point(35, 783);
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
                return new Point(148, 135);
            }
            case "turn 5" -> {
                return new Point(124, 155);
            }
            case "turn 6" -> {
                return new Point(95, 150);
            }
            case "turn 7" -> {
                return new Point(67, 135);
            }
            case "turn 8" -> {
                return new Point(60, 108);
            }
            case "turn 9" -> {
                return new Point(65, 80);
            }
            case "turn 10" -> {
                return new Point(93, 60);
            }
            case "phase 1" -> {
                return new Point(237, 45);
            }
            case "phase 2" -> {
                return new Point(297, 45);
            }
            case "phase 3" -> {
                return new Point(357, 45);
            }
            case "phase 4" -> {
                return new Point(417, 45);
            }
            case "phase 5" -> {
                return new Point(533, 45);
            }
            case "phase 6" -> {
                return new Point(593, 45);
            }
            case "phase 7" -> {
                return new Point(653, 45);
            }
            case "phase 8" -> {
                return new Point(713, 45);
            }
            case "storm 1" -> {
                return new Point(487, 958);
            }
            case "storm 2" -> {
                return new Point(635, 927);
            }
            case "storm 3" -> {
                return new Point(760, 850);
            }
            case "storm 4" -> {
                return new Point(858, 732);
            }
            case "storm 5" -> {
                return new Point(899, 589);
            }
            case "storm 6" -> {
                return new Point(898, 441);
            }
            case "storm 7" -> {
                return new Point(843, 300);
            }
            case "storm 8" -> {
                return new Point(740, 190);
            }
            case "storm 9" -> {
                return new Point(610, 120);
            }
            case "storm 10" -> {
                return new Point(463, 98);
            }
            case "storm 11" -> {
                return new Point(318, 129);
            }
            case "storm 12" -> {
                return new Point(189, 207);
            }
            case "storm 13" -> {
                return new Point(96, 323);
            }
            case "storm 14" -> {
                return new Point(49, 464);
            }
            case "storm 15" -> {
                return new Point(53, 614);
            }
            case "storm 16" -> {
                return new Point(107, 755);
            }
            case "storm 17" -> {
                return new Point(210, 865);
            }
            case "storm 18" -> {
                return new Point(340, 940);
            }
            case "tech token 0" -> {
                return new Point(390, 1020);
            }
            case "tech token 1" -> {
                return new Point(815, 820);
            }
            case "tech token 2" -> {
                return new Point(805, 205);
            }
            case "tech token 3" -> {
                return new Point(535, 80);
            }
            case "tech token 4" -> {
                return new Point(20, 240);
            }
            case "tech token 5" -> {
                return new Point(20, 810);
            }
            case "shield wall" -> {
                return new Point(652, 370);
            }
        }

        return new Point(0, 0);
    }

    public static Point calculateStormCoordinates(int sector) {
        double r = ((3*Math.PI)/2) + ((sector - 1) * (Math.PI/9));
        double x = 475 + (262*Math.cos(r));
        double y = 528 - (262*Math.sin(r));
        return new Point((int)Math.round(x), (int)Math.round(y));
    }
}
