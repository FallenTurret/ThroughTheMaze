package com.example.danil.throughthemaze;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.sqlite.SQLiteDatabase;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import com.example.danil.throughthemaze.database.MapDBHandler;
import com.example.danil.throughthemaze.database.MapDBManager;
import com.example.danil.throughthemaze.gameplay.Ball;
import com.example.danil.throughthemaze.map.Map;
import com.example.danil.throughthemaze.view.Draw2D;

import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;

public class GameCycleActivity extends AppCompatActivity {

    private static final long UPDATE_FREQUENCY = 20;
    private volatile Ball ball;
    private Draw2D draw;
    private Intent accelerometer;
    private GameCycleThread cycle;

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

    private BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            ball = intent.getParcelableExtra(Ball.class.getName());
        }
    };

    class GameCycleThread implements Runnable {

        private Thread worker;
        private AtomicBoolean running = new AtomicBoolean(false);

        public void start() {
            worker = new Thread(this);
            worker.start();
        }

        public void stop() {
            running.set(false);
        }

        @Override
        public void run() {
            running.set(true);
            while (running.get()) {
                long time = System.currentTimeMillis();

                draw.x = ball.x;
                draw.y = ball.y;
                draw.invalidate();

                long cycleTime = System.currentTimeMillis() - time;
                if (cycleTime < UPDATE_FREQUENCY) {
                    try {
                        Thread.sleep(UPDATE_FREQUENCY - cycleTime);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    };

    @Override
    protected void onResume() {
        super.onResume();

        registerReceiver(receiver, new IntentFilter(Service.INPUT_SERVICE));
        cycle = new GameCycleThread();
        cycle.start();
    }

    @Override
    protected void onPause() {
        super.onPause();

        unregisterReceiver(receiver);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        cycle.stop();
        stopService(accelerometer);
    }

}
