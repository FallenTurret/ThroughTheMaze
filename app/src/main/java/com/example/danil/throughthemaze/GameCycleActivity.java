package com.example.danil.throughthemaze;

import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import com.example.danil.throughthemaze.database.MapDBHandler;
import com.example.danil.throughthemaze.database.MapDBManager;
import com.example.danil.throughthemaze.gameplay.Ball;
import com.example.danil.throughthemaze.map.Map;
import com.example.danil.throughthemaze.view.Draw2D;

import java.util.Random;

public class GameCycleActivity extends AppCompatActivity {

    private Ball ball;
    private Draw2D draw;
    private Intent accelerometer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        MapDBHandler dbLoader = new MapDBHandler(this);
        SQLiteDatabase db = dbLoader.getReadableDatabase();
        MapDBManager manager = new MapDBManager(db);
        int mapCount = manager.getMapCount();
        Random random = new Random();
        int mapId = random.nextInt(mapCount);
        Map map = manager.loadMap(mapId);
        db.close();
        int start = random.nextInt(map.size);
        int end = -1;
        while (end == -1 || end == start) {
            end = random.nextInt(map.size);
        }
        double x = map.vertexes[start].x;
        double y = map.vertexes[start].y;
        ball = new Ball(x, y);
        accelerometer = new Intent(this, AccelerometerService.class);
        accelerometer.putExtra(Ball.class.getName(), ball);
        startService(accelerometer);
        draw = new Draw2D(this, map, end);
        draw.x = x;
        draw.y = y;
        setContentView(draw);
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        stopService(accelerometer);
    }
}
