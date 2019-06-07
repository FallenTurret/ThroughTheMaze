package ru.hse.throughthemaze.map;

import java.util.Comparator;

public class Vertex {
    public static Comparator<Vertex> compareX = new Comparator<Vertex>() {
        @Override
        public int compare(Vertex o1, Vertex o2) {
            return Double.compare(o2.x, o1.x);
        }
    };

    public double x;
    public double y;

    public Vertex(double x, double y) {
        this.x = x;
        this.y = y;
    }

    public double dist(Vertex other) {
        return Math.sqrt((x - other.x) * (x - other.x) + (y - other.y) * (y - other.y));
    }

    public Vertex midTo(Vertex other) {
        return new Vertex((x + other.x) / 2, (y + other.y) / 2);
    }

}