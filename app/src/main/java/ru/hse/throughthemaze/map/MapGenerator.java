package ru.hse.throughthemaze.map;

import ru.hse.throughthemaze.database.MapDBManager;

import java.sql.SQLException;

public class MapGenerator {
    public static void main(String[] args) throws SQLException {
        MapDBManager.createOrClear();
        int numberOfMaps = Integer.parseInt(args[0]);
        int size = Integer.parseInt(args[1]);
        for (int i = 0; i < numberOfMaps; i++) {
            Map map = new Map(size);
            map.generate();
            MapDBManager.addMap(map, i);
        }
    }
}