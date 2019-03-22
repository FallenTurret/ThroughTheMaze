package com.example.danil.throughthemaze.database;

import android.database.sqlite.SQLiteDatabase;
import com.example.danil.throughthemaze.map.Map;
import com.example.danil.throughthemaze.map.Vertex;
import org.sqlite.JDBC;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class MapDBManager {

    private SQLiteDatabase database;
    private static Connection connection;
    private static final String CON_STR = "jdbc:sqlite:app/src/main/assets/databases/maps.sqlite3";

    public MapDBManager(SQLiteDatabase database) {
        this.database = database;
    }

    public static void createOrClear() throws SQLException {
        DriverManager.registerDriver(new JDBC());
        connection = DriverManager.getConnection(CON_STR);
        ResultSet res = connection.getMetaData().getTables(
                null, null, "%", null);
        List<String> names = new ArrayList<>();
        while (res.next()) {
            names.add(res.getString(3));
        }
        res.close();
        for (String name: names) {
            try (Statement statement = connection.createStatement()) {
                statement.execute("DROP TABLE " + name);
            }
        }
    }

    public static void addMap(Map map, int id) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute("CREATE TABLE Vertexes" + id + "(ID INTEGER, x FLOAT, y FLOAT)");
        }
        for (int i = 0; i < map.size; i++) {
            try (PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO Vertexes" + id + " VALUES (?, ?, ?)")) {
                statement.setInt(1, i);
                statement.setDouble(2, map.vertexes[i].x);
                statement.setDouble(3, map.vertexes[i].y);
                statement.execute();
            }
        }
        try (Statement statement = connection.createStatement()) {
            statement.execute("CREATE TABLE Edges" + id + "(v INTEGER, u INTEGER)");
        }
        for (int i = 0; i < map.size; i++) {
            for (int j: map.edges.get(i)) {
                try (PreparedStatement statement = connection.prepareStatement(
                        "INSERT INTO Edges" + id + " VALUES (?, ?)")) {
                    statement.setInt(1, i);
                    statement.setInt(2, j);
                    statement.execute();
                }
            }
        }
    }
}