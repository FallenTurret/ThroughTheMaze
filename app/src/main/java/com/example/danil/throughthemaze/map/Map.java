package com.example.danil.throughthemaze.map;

import java.util.*;

public class Map {
    private static final double VERTEX_RADIUS = 1;
    private static final double CORRIDOR_WIDTH = VERTEX_RADIUS / 2;
    private static final double BORDER = 1000;
    public int size;
    public Vertex[] vertexes;
    public ArrayList<LinkedList<Integer>> edges;

    public Map(int size) {
        this.size = size;
        vertexes = new Vertex[size];
        edges = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            edges.add(new LinkedList<Integer>());
        }
    }

    public Map(int size, Vertex[] vertexes, ArrayList<LinkedList<Integer>> edges) {
        this.size = size;
        this.vertexes = vertexes;
        this.edges = edges;
    }

    public void generate() {
        while (true) {
            generateVertexes();
            ArrayList<Integer> indexes = new ArrayList<>();
            for (int i = 0; i < size; i++) {
                indexes.add(i);
                edges.get(i).clear();
            }
            if (triangulate(indexes) && check(indexes)) {
                break;
            }
        }
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

    private boolean check(ArrayList<Integer> indexes) {
        for (int i: indexes) {
            for (int j: edges.get(i)) {
                for (int k: indexes) {
                    if (i != k && j != k && intersects(vertexes[i], vertexes[j], vertexes[k])) {
                        return false;
                    }
                    for (int l: edges.get(k)) {
                        if (i != k && i != l && j != k && j != l
                                && intersects(vertexes[i], vertexes[j], vertexes[k], vertexes[l])) {
                            return false;
                        }
                    }
                }
            }
        }
        return true;
    }

    private boolean oneSide(Vertex a, Vertex b, ArrayList <Integer> indexes) {
        Segment x = new Segment(a, b);
        int side = 0;
        for (int i: indexes) {
            side += x.side(vertexes[i]);
        }
        return (side == 0 || side == indexes.size() - 2);
    }

    private void removeIntersections(Vertex a, Vertex b, ArrayList<Integer> indexes) {
        for (int i: indexes) {
            LinkedList<Integer> newEdges = new LinkedList<>();
            for (int j: edges.get(i)) {
                if (!intersects(a, b, vertexes[i], vertexes[j])) {
                    newEdges.add(j);
                }
            }
            edges.set(i, newEdges);
        }
    }

    private boolean triangulate(ArrayList<Integer> indexes) {
        if (indexes.size() <= 4) {
            for (int i = 0; i < indexes.size(); i++) {
                for (int j = i + 1; j < indexes.size(); j++) {
                    int a = indexes.get(i);
                    int b = indexes.get(j);
                    edges.get(a).add(b);
                    edges.get(b).add(a);
                }
            }
            if (check(indexes)) {
                return true;
            }
            if (indexes.size() == 3) {
                return false;
            }
            for (int i = 1; i < 4; i++) {
                int j = 2, k = 3;
                if (i == 2) {
                    j = 1;
                }
                if (i == 3) {
                    k = 1;
                }
                Vertex a = vertexes[indexes.get(0)];
                Vertex b = vertexes[indexes.get(i)];
                Vertex c = vertexes[indexes.get(j)];
                Vertex d = vertexes[indexes.get(k)];
                if (!intersects(a, b, c, d)) {
                    continue;
                }
                edges.get(0).remove((Integer) i);
                edges.get(i).remove((Integer) 0);
                if (check(indexes)) {
                    return true;
                }
                edges.get(0).add(i);
                edges.get(i).add(0);
                edges.get(j).remove((Integer) k);
                edges.get(k).remove((Integer) j);
                return check(indexes);
            }
            return check(indexes);
        }
        ArrayList<Vertex> list = new ArrayList<>();
        for (int i: indexes) {
            list.add(vertexes[i]);
        }
        Collections.sort(list, Vertex.compareX);
        int mid = list.size() / 2;
        if (list.size() < 12 && list.size() != 8) {
            mid = 3;
        }
        double division = (list.get(mid).x + list.get(mid - 1).x) / 2;
        ArrayList<Integer> left = new ArrayList<>();
        ArrayList<Integer> right = new ArrayList<>();
        for (int i: indexes) {
            if (vertexes[i].x < division) {
                left.add(i);
            } else {
                right.add(i);
            }
        }
        if (!triangulate(left) || !triangulate(right)) {
            return false;
        }
        int startLeft = -1;
        int endLeft = -1;
        int startRight = -1;
        int endRight = -1;
        for (int i: left) {
            for (int j: right) {
                if (oneSide(vertexes[i], vertexes[j], indexes)) {
                    if (startLeft == -1) {
                        startLeft = i;
                        startRight = j;
                    } else {
                        endLeft = i;
                        endRight = j;
                    }
                }
            }
        }
        Set<Integer> unmarked = new HashSet<>();
        for (int i: indexes) {
            if (i != startLeft && i != startRight) {
                unmarked.add(i);
            }
        }
        while (startLeft != endLeft || startRight != endRight) {
            Vertex center = vertexes[startLeft].midTo(vertexes[startRight]);
            int closest = -1;
            for (int i: unmarked) {
                if (closest == -1 || center.dist(vertexes[i]) < center.dist(vertexes[closest])) {
                    closest = i;
                }
            }
            if (closest == -1) {
                return false;
            }
            unmarked.remove(closest);
            if (vertexes[closest].x < division) {
                removeIntersections(vertexes[startRight], vertexes[closest], left);
                edges.get(startRight).add(closest);
                edges.get(closest).add(startRight);
                if (!edges.get(startLeft).contains(closest)) {
                    edges.get(startLeft).add(closest);
                    edges.get(closest).add(startLeft);
                }
                startLeft = closest;
            } else {
                removeIntersections(vertexes[startLeft], vertexes[closest], right);
                edges.get(startLeft).add(closest);
                edges.get(closest).add(startLeft);
                if (!edges.get(startRight).contains(closest)) {
                    edges.get(startRight).add(closest);
                    edges.get(closest).add(startRight);
                }
                startRight = closest;
            }
        }
        return true;
    }
}