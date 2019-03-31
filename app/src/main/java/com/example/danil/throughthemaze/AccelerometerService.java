package com.example.danil.throughthemaze;

import android.app.Service;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.IBinder;
import android.support.annotation.Nullable;
import com.example.danil.throughthemaze.gameplay.Ball;

import java.util.Timer;
import java.util.TimerTask;

public class AccelerometerService extends Service {

    private SensorManager sensorManager;
    private Sensor sensorAcceleration;
    private SensorEventListener listener;
    private Timer timer;
    private Ball ball;

    private double ax;
    private double ay;

    @Override
    public int onStartCommand(final Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        ball = intent.getParcelableExtra(Ball.class.getName());
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        sensorAcceleration = sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY);
        listener = new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent event) {
                ax = event.values[0];
                ay = event.values[1];
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) {}
        };
        sensorManager.registerListener(listener, sensorAcceleration, SensorManager.SENSOR_DELAY_GAME);

        timer = new Timer();
        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                ball.ax = ax;
                ball.ay = ay;
                Intent intent = new Intent(Service.INPUT_SERVICE);
                intent.putExtra(Ball.class.getName(), ball);
                sendBroadcast(intent);
            }
        };
        timer.schedule(task, 0, SensorManager.SENSOR_DELAY_GAME);
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        sensorManager.unregisterListener(listener);
        timer.cancel();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

}
