package model.rs;

import main.renderer.DiagramGraphics;
import model.Drawable;
import model.Vector;
import model.others.Tuple;
import shapes.lines.RangeLine;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Table extends Vector implements Drawable {
    public String name;
    public Map<String, Column> attributeMap;

    private List<Column> sorted;
    private final List<Tuple<Boolean, String, Table>> foreign;
    private boolean highlighted;
    public long keyCount = 0;
    public long colCount = 0;

    private List<Column> keys;

    public Table(String name) {
        this.name = name;
        this.attributeMap = new HashMap<>();
        this.sorted = new ArrayList<>();
        this.foreign = new ArrayList<>();
    }

    public void add(Column column) {
        column.parent = this;
        this.attributeMap.put(column.name, column);
        revalidate();
    }

    public void add(Table other, String name, boolean required) {
        this.foreign.add(new Tuple<>(required, name, other));
        revalidate();
    }

    public void remove(Column column) {
        if (this.attributeMap.remove(column.name, column)) column.parent = null;
        revalidate();
    }

    public void revalidate() {
        this.sorted = attributeMap.values().stream().sorted(Comparator.naturalOrder()).collect(Collectors.toList());
        for (int i = 0; i < this.sorted.size(); i++) this.sorted.get(i).index = i;
        keyCount = getKeys().size();
        colCount = cols().count();
    }

    private static final Font title = new Font(null, Font.BOLD, 12);
    private static final Font normal = new Font(null, Font.PLAIN, 12);
    private static final Font small = new Font(null, Font.PLAIN, 10);

    @Override
    public void draw(DiagramGraphics g) {
        AffineTransform transform = g.getTransform();
        g.translate(getX(), getY());

        g.setFont(title);
        g.drawStringCenter(name, (float) (Column.WIDTH / 2), (float) (Column.HEIGHT / 2 + 10));
        g.setFont(normal);

        if (highlighted) g.draw(getShape());
        g.translate(0, Column.HEIGHT);
        for (Column e : sorted) {
            e.draw(g);
            g.translate(Column.WIDTH, 0);
        }
        g.setTransform(transform);

        Vector offset = this.add(sorted.size() * Column.WIDTH, Column.HEIGHT);
        for (Tuple<Boolean, String, Table> other : foreign) {
            other.getC().drawAsForeign(g, other.getA(), other.getB(), offset.clone());
            offset.incre(other.getC().keyCount * Column.WIDTH, 0);
        }
        if (highlighted) g.draw(getShapeWorld(), new Color(0, 0, 0, 30), Color.BLACK);
    }

    @Override
    public void predraw(DiagramGraphics g) {
        Vector offset = this.add(sorted.size() * Column.WIDTH, Column.HEIGHT);
        for (Tuple<Boolean, String, Table> other : foreign) {
            other.getC().predrawAsForeign(g, offset.clone());
            offset.incre(other.getC().keyCount * Column.WIDTH, 0);
        }
    }

    public void drawAsForeign(DiagramGraphics g, Boolean isKey, String b, Vector origin) {
        double diff = keyCount * .5 * Column.WIDTH;
        Vector center = origin.add(diff, 7);
        keys.forEach(e -> {
            e.drawAsForeign(g, isKey, origin);
            origin.incre(Column.WIDTH, 0);
        });
        g.draw(new RangeLine(center.add(-diff, 0), center.add(diff, 0)));
        Font font = g.getFont();
        g.setFont(small);
        g.drawStringCenter(b, center, Color.WHITE);
        g.setFont(font);
    }

    public void predrawAsForeign(DiagramGraphics g, Vector origin) {
        keys.forEach(e -> {
            e.drawLine(g, origin);
            origin.incre(Column.WIDTH, 0);
        });
    }

    /**
     * Combines keys and all required foreign keys
     *
     * @return minimal set of keys
     */
    public List<Column> getKeys() {
        return keys = Stream.concat(sorted.stream().filter(e -> e.key),
                foreign.stream().filter(Tuple::getA).flatMap(e -> e.getC().getKeys().stream())).collect(Collectors.toList());
    }

    /**
     * Combine all columns and all foreign keys
     *
     * @return all columns
     */
    public Stream<Column> cols() {
        return Stream.concat(sorted.stream(), foreign.stream().flatMap(e -> e.getC().getKeys().stream()));
    }

    public Rectangle2D getShapeWorld() {
        return new Rectangle2D.Double(getX(), getY(), colCount * Column.WIDTH, Column.HEIGHT * 2);
    }

    public Rectangle2D getShape() {
        return new Rectangle2D.Double(0, 0, colCount * Column.WIDTH, Column.HEIGHT * 2);
    }

    public void setHighlighted(boolean highlighted) {
        this.highlighted = highlighted;
    }

    public boolean isHighlighted() {
        return highlighted;
    }
}
