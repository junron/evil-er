package shapes;

import model.Vector;
import model.lines.Line;

import java.awt.geom.Path2D;

public class SubsetSymbol extends Path2D.Double {
    public SubsetSymbol(Vector pos, Vector direction, double size, Line.LineStyle style) {
        if (style == Line.LineStyle.AXIS_ALIGNED) {
            if (Math.abs(direction.getX()) > Math.abs(direction.getY()))
                direction.set(0, direction.getY() > 0 ? 1 : -1);
            else direction.set(direction.getX() > 0 ? 1 : -1, 0);
        }
        direction.norm().scale(size);
        Vector a = pos.add(direction).add(direction.rot90()),
                b = pos.add(direction.rot90()).add(direction.negate()),
                c = pos.add(direction.negate()).add(direction.negate().rot90()),
                d = pos.add(direction.negate().rot90()).add(direction);
        moveTo(b.getX(), b.getY());
        if (style == Line.LineStyle.CURVE)
            curveTo(a.getX(), a.getY(), d.getX(), d.getY(), c.getX(), c.getY());
        else {
            lineTo(a.getX(), a.getY());
            lineTo(d.getX(), d.getY());
            lineTo(c.getX(), c.getY());
        }
    }
}