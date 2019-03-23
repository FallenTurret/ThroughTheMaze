package com.example.danil.throughthemaze;

import android.annotation.SuppressLint;
import android.database.sqlite.SQLiteDatabase;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.os.Handler;
import android.view.MotionEvent;
import android.view.View;
import com.example.danil.throughthemaze.database.MapDBHandler;
import com.example.danil.throughthemaze.database.MapDBManager;
import com.example.danil.throughthemaze.map.Map;
import com.example.danil.throughthemaze.view.Draw2D;

import java.util.Random;

public class GameCycleActivity extends AppCompatActivity {

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
        double x = map.vertexes[start].x;
        double y = map.vertexes[start].y;
        Draw2D draw = new Draw2D(this, map);
        draw.x = x;
        draw.y = y;
        setContentView(draw);
    }

}
