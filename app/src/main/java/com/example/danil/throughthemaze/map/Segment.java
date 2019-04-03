package com.example.danil.throughthemaze.map;

public class Segment {
    private static final double EPS = 1e-8;
    public Vertex a;
    public Vertex b;

    public Segment(Vertex a, Vertex b) {
        this.a = a;
        this.b = b;
    }

    public double dist(Vertex c) {
        double scalar1 = (c.x - a.x) * (b.x - a.x) + (c.y - a.y) * (b.y - a.y);
        double scalar2 = (b.x - a.x) * (b.x - a.x) + (b.y - a.y) * (b.y - a.y);
        if (scalar1 < EPS) {
            return a.dist(c);
        }
        if (scalar2 < scalar1 + EPS) {
            return b.dist(c);
        }
        double coefficient = scalar1 / scalar2;
        return c.dist(new Vertex(a.x + coefficient * (b.x - a.x), a.y + coefficient * (b.y - a.y)));
    }

    public int side(Vertex c) {
        double vector = (c.x - a.x) * (b.y - a.y) - (c.y - a.y) * (b.x - a.x);
        if (vector > EPS) {
            return 1;
        }
        return 0;
    }

    public Segment move(double delta) {
        Vertex v = new Vertex(a.y - b.y, b.x - a.x);
        v.x *= delta / a.dist(b);
        v.y *= delta / a.dist(b);
        return new Segment(new Vertex(a.x + v.x, a.y + v.y), new Vertex(b.x + v.x, b.y + v.y));
    }
}