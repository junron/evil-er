package model.lines;

import model.Drawable;
import model.Node;

public abstract class Line<A extends Node, B extends Node> implements Drawable {
    public A a;
    public B b;

    public Line(A a, B b) {
        this.a = a;
        this.b = b;
    }
}
