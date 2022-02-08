package shapes.lines;

import model.Range;
import model.Vector;
import model.er.Attribute;
import model.others.Pair;
import model.others.Tuple;
import model.rs.Column;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

public class SchemaLine extends Path2D.Double {
    public static ArrayList<Tuple<Integer, Integer, Integer>> xTaken = new ArrayList<>();
    public static ArrayList<Tuple<Integer, Integer, Integer>> yTaken = new ArrayList<>();

    public static ArrayList<Line2D.Double> toDodge = new ArrayList<>();

    public static int getTarget(ArrayList<Tuple<Integer, Integer, Integer>> axis, int srcI, int from, int to, double diff) {
        final AtomicInteger src = new AtomicInteger(srcI);
        while (axis.parallelStream().anyMatch(e -> e.getA() == src.get() && Range.intersects(e.getB(), e.getC(), from, to)))
            src.set(srcI += diff);
        axis.add(new Tuple<>(src.get(), from, to));
        return src.get();
    }

    public static void resetLines() {
        xTaken.clear();
        yTaken.clear();
        toDodge.clear();
    }

    public SchemaLine(Vector a, Vector b, Line.LineStyle style) {
        if (style == Line.LineStyle.AXIS_ALIGNED) axisLine(a, b);
        else if (style == Line.LineStyle.STRAIGHT) straightLine(a, b);
        else axisCurvyLine(a, b);
    }

    public void straightLine(@NotNull Vector a, @NotNull Vector b) {
        boolean bUp = getBUp(a, b);
        moveTo(a.getX(), a.getY());
        lineTo(a.getX(), a.getY() + (getAUp(a, b) ? -30 : 30));
        dodgingTo(b.getX(), b.getY() + (bUp ? -30 : 30));
        lineTo(b.getX(), b.getY());
        arrow(bUp, b.getX(), b.getY() + (bUp ? -Column.HEIGHT / 2 : Column.HEIGHT / 2));
    }

    public void axisLine(@NotNull Vector a, @NotNull Vector b) {
        boolean aUp = getAUp(a, b);
        boolean bUp = getBUp(a, b);
        int aY = (int) (a.getY() + (aUp ? -30 : 30));
        int bY = (int) (b.getY() + (bUp ? -30 : 30));
        double targetX = getTarget(xTaken, (int) b.getX(), (int) a.getY(), (int) b.getY(), 10);
        double targetY = getTarget(yTaken,
                aUp && bUp ? Math.min(aY, bY) : aUp ? aY : bUp ? bY : Math.max(aY, bY),
                (int) b.getX(), (int) a.getX(),
                aUp || bUp ? -7 : 7);

        moveTo(a.getX(), a.getY());
        dodgingTo(a.getX(), targetY);
        dodgingTo(targetX, targetY);
        dodgingTo(targetX, b.getY());
        arrow(bUp, targetX, b.getY() + (bUp ? -Column.HEIGHT / 2 : Column.HEIGHT / 2));
    }

    private static final double RADIUS = 5;

    public void axisCurvyLine(@NotNull Vector a, @NotNull Vector b) {
        if (Math.abs(a.getX() - b.getX()) < RADIUS * 2) {
            axisLine(a, b);
            return;
        }

        boolean aUp = getAUp(a, b);
        boolean bUp = getBUp(a, b);
        int aY = (int) (a.getY() + (aUp ? -30 : 30));
        int bY = (int) (b.getY() + (bUp ? -30 : 30));
        double targetX = getTarget(xTaken, (int) b.getX(), (int) a.getY(), (int) b.getY(), 10);
        double targetY = getTarget(yTaken,
                aUp && bUp ? Math.min(aY, bY) : aUp ? aY : bUp ? bY : Math.max(aY, bY),
                (int) b.getX(), (int) a.getX(),
                aUp || bUp ? -7 : 7);

        moveTo(a.getX(), a.getY());

        dodgingTo(a.getX(), targetY + (aUp ? RADIUS : -RADIUS));
        quadTo(a.getX(), targetY, a.getX() + (b.getX() > a.getX() ? RADIUS : -RADIUS), targetY);

        dodgingTo(targetX + (b.getX() > a.getX() ? -RADIUS : RADIUS), targetY);
        quadTo(targetX, targetY, targetX, targetY + (bUp ? RADIUS : -RADIUS));

        dodgingTo(targetX, b.getY());
        arrow(bUp, targetX, b.getY() + (bUp ? -Column.HEIGHT / 2 : Column.HEIGHT / 2));
    }

    private boolean getAUp(Vector a, Vector b) {
        if (Math.abs(b.getY() - a.getY()) < (Attribute.HEIGHT + 20)) return true;
        else return b.getY() < a.getY();
    }

    private boolean getBUp(Vector a, Vector b) {
        if (Math.abs(b.getY() - a.getY()) < (Attribute.HEIGHT + 20)) return true;
        return b.getY() > a.getY();
    }

    private static final double JUMP_LINE_RADIUS = 3;

    public void dodgingTo(@NotNull Vector vector) {
        Vector currentPoint = new Vector(getCurrentPoint());
        Vector diff = vector.minus(currentPoint);
        double len = diff.len();
        Vector dir = diff.norm().multi(JUMP_LINE_RADIUS);
        Vector r90 = dir.rotate90();
        Line2D.Double line = new Line2D.Double(currentPoint, vector);
        toDodge.stream()
                .filter(l -> noEndsMeet(l, line) && l.intersectsLine(line))
                .map(l -> intersection(l, line))
                .filter(Objects::nonNull)
                .distinct()
                .map(p -> new Pair<>(p, currentPoint.minus(p).len()))
                .sorted(Comparator.comparingDouble(Pair::getB))
                .forEach(pair -> {
                    double dist = pair.getB();
                    if (dist < JUMP_LINE_RADIUS || len - dist < JUMP_LINE_RADIUS) return;

                    Vector p = pair.getA();
                    lineTo(p.minus(dir));
                    curveTo(p.minus(dir).add(r90),
                            p.add(dir).add(r90),
                            p.add(dir));
//                    lineTo(p.minus(dir));
//                    lineTo(p.add(dir).add(r90));
//                    moveTo(p.minus(dir).minus(r90));
//                    lineTo(p.add(dir));
                });
        lineTo(vector.getX(), vector.getY());
        toDodge.add(line);
    }

    private boolean noEndsMeet(Line2D.@NotNull Double l, Line2D.@NotNull Double line) {
        return !(
                Objects.equals(l.getP1(), line.getP1()) ||
                        Objects.equals(l.getP2(), line.getP1()) ||
                        Objects.equals(l.getP1(), line.getP2()) ||
                        Objects.equals(l.getP2(), line.getP2())
        );
    }

    public void dodgingTo(double x, double y) {
        dodgingTo(new Vector(x, y));
    }


    public void arrow(boolean up, double x, double y) {
        moveTo(x, y);
        lineTo(x - 5, y + (up ? -5 : 5));
        moveTo(x, y);
        lineTo(x + 5, y + (up ? -5 : 5));
        moveTo(x, y);
    }

    public void lineTo(@NotNull Point2D point) {
        lineTo(point.getX(), point.getY());
    }

    public void moveTo(@NotNull Point2D point) {
        moveTo(point.getX(), point.getY());
    }

    public void curveTo(@NotNull Point2D a, @NotNull Point2D b, @NotNull Point2D c) {
        curveTo(a.getX(), a.getY(), b.getX(), b.getY(), c.getX(), c.getY());
    }

    public void arrow(boolean up, @NotNull Vector pos) {
        arrow(up, pos.getX(), pos.getY());
    }

    public static @Nullable Vector intersection(@NotNull Line2D a, @NotNull Line2D b) {
        double x0 = a.getX1(), y0 = a.getY1(),
                x1 = a.getX2(), y1 = a.getY2(),
                x2 = b.getX1(), y2 = b.getY1(),
                x3 = b.getX2(), y3 = b.getY2();
        double sx1 = x1 - x0,
                sy1 = y1 - y0,
                sx2 = x3 - x2,
                sy2 = y3 - y2;
        double v = -sx2 * sy1 + sx1 * sy2;
        if (v == 0) return null;
        double s = (-sy1 * (x0 - x2) + sx1 * (y0 - y2)) / v;
        double t = (sx2 * (y0 - y2) - sy2 * (x0 - x2)) / v;
        if (s > 0 && s < 1 && t > 0 && t < 1) return new Vector(x0 + (t * sx1), y0 + (t * sy1));
        return null;
    }
}
