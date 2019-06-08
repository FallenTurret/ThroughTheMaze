package ru.hse.throughthemaze.map;

import java.util.*;

public class Map {
    public static final double VERTEX_RADIUS = 1;
    public static final double CORRIDOR_WIDTH = VERTEX_RADIUS / 2;
    public static final double BORDER = 200;
    public int size;
    public Vertex[] vertexes;
    public ArrayList<TreeSet<Integer>> edges;
    public int[] start;
    public int end;

    public Map(int size) {
        this.size = size;
        vertexes = new Vertex[size];
        edges = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            edges.add(new TreeSet<Integer>());
        }
    }

    public void generate() {
        generateVertexes();
        addSomeEdges();
    }

    private void generateVertexes() {
        while (true) {
            for (int i = 0; i < size; i++) {
                vertexes[i] = new Vertex(Math.random() * BORDER, Math.random() * BORDER);
            }
            boolean correct = true;
            for (int i = 0; i < size; i++) {
                for (int j = i + 1; j < size; j++) {
                    if (vertexes[i].dist(vertexes[j]) < 2 * VERTEX_RADIUS) {
                        correct = false;
                    }
                }
            }
            if (correct) {
                break;
            }
        }
    }

    private boolean intersects(Vertex a, Vertex b, Vertex c) {
        Segment p = new Segment(a, b);
        if (p.dist(c) < VERTEX_RADIUS + CORRIDOR_WIDTH) {
            return true;
        }
        return false;
    }

    private boolean intersects(Vertex a, Vertex b, Vertex c, Vertex d) {
        Segment p = new Segment(a, b);
        Segment q = new Segment(c, d);
        int sum = 0;
        sum += p.side(c);
        sum += p.side(d);
        if (sum != 1) {
            return false;
        }
        sum += q.side(a);
        sum += q.side(b);
        return (sum == 2);
    }

    private void addSomeEdges() {
        ArrayList<Integer> edgesToAdd = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            for (int j = i + 1; j < size; j++) {
                if (!edges.get(i).contains(j)) {
                    edgesToAdd.add(i * size + j);
                }
            }
        }
        Collections.shuffle(edgesToAdd);
        for (int e: edgesToAdd) {
            int i = e / size;
            int j = e % size;
            boolean notIntersects = true;
            for (int k = 0; k < size; k++) {
                if (k == i || k == j) {
                    continue;
                }
                if (intersects(vertexes[i], vertexes[j], vertexes[k])) {
                    notIntersects = false;
                    break;
                }
                for (int l: edges.get(k)) {
                    if (l == i || l == j) {
                        continue;
                    }
                    if (intersects(vertexes[i], vertexes[j], vertexes[k], vertexes[l])) {
                        notIntersects = false;
                        break;
                    }
                }
            }
            if (notIntersects) {
                edges.get(i).add(j);
                edges.get(j).add(i);
            }
        }
    }

    public void pickStartAndEnd(int n) {
        ArrayList<Integer> starts = new ArrayList<>();
        for (int i = 0; i <= n; i++) {
            starts.add(i);
        }
        Collections.shuffle(starts);
        for (int i = 0; i < n; i++) {
            start[i] = starts.get(i);
        }
        end = starts.get(n);
    }
}