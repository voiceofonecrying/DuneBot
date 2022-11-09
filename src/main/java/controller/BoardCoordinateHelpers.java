package controller;

import model.Line;

import java.awt.*;
import java.util.List;

public class BoardCoordinateHelpers {

    public static boolean onLine(Line line, Point coordinates) {
        return (coordinates.x <= Math.max(line.p1().x, line.p2().x)
                && coordinates.x <= Math.min(line.p1().x, line.p2().x)
                && coordinates.y <= Math.max(line.p1().y, line.p2().y)
                && coordinates.y <= Math.min(line.p1().y, line.p2().y));
    }

    public static int direction(Point a, Point b, Point c) {
        int val = (b.y - a.y) * (c.x - b.x)
                - (b.x - a.x) * (c.y - b.y);
        if (val == 0) return 0;
        if (val < 0) return 2;
        return 1;
    }

    public static boolean isIntersect(Line l1, Line l2) {
        int dir1 = direction(l1.p1(), l1.p2(), l2.p1());
        int dir2 = direction(l1.p1(), l1.p2(), l2.p2());
        int dir3 = direction(l2.p1(), l2.p2(), l1.p1());
        int dir4 = direction(l2.p1(), l2.p2(), l1.p2());

        return (dir1 != dir2 && dir3 != dir4)
                || (dir1 == 0 && onLine(l1, l2.p1()))
                || (dir2 == 0 && onLine(l1, l2.p2()))
                || (dir3 == 0 && onLine(l2, l1.p1()))
                || (dir4 == 0 && onLine(l2, l1.p2()));
    }

    public static boolean checkInside(List<Point> points, Point p) {

        if (points.size() < 3) return false;

        Line exline = new Line(p, new Point(9999, p.y));
        int count = 0;
        int i = 0;

        do {
            Line side = new Line(points.get(i), points.get(i + 1 % points.size()));
            if (isIntersect(side, exline)) {

                if (direction(side.p1(), p, side.p2()) == 0) return onLine(side, p);
                count++;
            }
            i = (i + 1) % points.size();
        } while (i != 0);
        return count % 2 == 1;
    }


}
