package com.example.danil.throughthemaze.map;

import java.util.ArrayList;
import java.util.Comparator;

public class Vertex {
    public static Comparator<Vertex> compareX = new Comparator<Vertex>() {
        @Override
        public int compare(Vertex o1, Vertex o2) {
            if (o1.x < o2.x) {
                return 1;
            }
            if (o1.x > o2.x) {
                return -1;
            }
            return 0;
        }
    };

    public double x;
    public double y;
    public ArrayList<Vertex> adjacentVertexes = new ArrayList<>();

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