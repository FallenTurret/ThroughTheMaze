package ru.hse.throughthemaze.gameplay;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.support.annotation.Nullable;
import ru.hse.throughthemaze.MainActivity;
import ru.hse.throughthemaze.map.Map;
import ru.hse.throughthemaze.map.Segment;
import ru.hse.throughthemaze.map.Vertex;

import java.util.Timer;
import java.util.TimerTask;

public class PhysicsEngine extends Service {

    public class EngineBinder extends Binder {
        public PhysicsEngine getService() {
            return PhysicsEngine.this;
        }
    }

    private final IBinder binder = new EngineBinder();
    public static Map map;
    private Ball[] balls;
    private Timer timer;

    private synchronized void passTime(long time) {
        for (Ball ball: balls) {
            double t = (double)time / 1000;
            ball.x += ball.vx * t + ball.ax * t * t / 2;
            ball.y += ball.vy * t + ball.ay * t * t / 2;
            ball.vx *= 0.99;
            ball.vy *= 0.99;
            ball.vx += ball.ax * t;
            ball.vy += ball.ay * t;
            Vertex curPoint = new Vertex(ball.x, ball.y);
            Vertex end = new Vertex(map.vertexes[map.end].x, map.vertexes[map.end].y);
            if (end.dist(curPoint) + Ball.RADIUS < Map.VERTEX_RADIUS) {
                ball.color = -1;
            }
        }
        checkForClash();
    }

    private void checkForClash() {
        for (int k = 0; k < balls.length; k++) {
            Ball ball = balls[k];
            Vertex curPoint = new Vertex(ball.x, ball.y);
            for (int j = k + 1; j < balls.length; j++) {
                Vertex secondPoint = new Vertex(balls[j].x, balls[j].y);
                if (curPoint.dist(secondPoint) < 2 * Ball.RADIUS) {
                    processClash(ball, balls[j]);
                }
            }
            boolean next = false;
            for (int i = 0; i < map.size; i++) {
                Vertex v = map.vertexes[i];
                if (v.dist(curPoint) < Map.VERTEX_RADIUS) {
                    if (Map.VERTEX_RADIUS < Ball.RADIUS + v.dist(curPoint)) {
                        boolean inCorridor = false;
                        for (int j : map.edges.get(i)) {
                            Vertex a = map.vertexes[i];
                            Vertex b = map.vertexes[j];
                            Segment l = new Segment(a, b);
                            if (l.dist(curPoint) < Map.CORRIDOR_WIDTH) {
                                inCorridor = true;
                                break;
                            }
                        }
                        if (inCorridor) {
                            break;
                        }
                        double deltax = curPoint.x - v.x;
                        double deltay = curPoint.y - v.y;
                        double multiplier = Map.VERTEX_RADIUS / v.dist(curPoint);
                        deltax *= multiplier;
                        deltay *= multiplier;
                        Vertex onBorder = new Vertex(v.x + deltax, v.y + deltay);
                        processClash(new Segment(onBorder, new Vertex(onBorder.x - deltay, onBorder.y + deltax)), ball);
                    }
                    next = true;
                    break;
                }
            }
            if (next) {
                continue;
            }
            for (int i = 0; i < map.size; i++) {
                for (int j : map.edges.get(i)) {
                    Vertex a = map.vertexes[i];
                    Vertex b = map.vertexes[j];
                    Segment l = new Segment(a, b);
                    if (l.dist(curPoint) < Map.CORRIDOR_WIDTH) {
                        if (Map.CORRIDOR_WIDTH < l.dist(curPoint) + Map.CORRIDOR_WIDTH / 2) {
                            Segment border = l.move(Map.CORRIDOR_WIDTH);
                            if (border.dist(curPoint) < Map.CORRIDOR_WIDTH / 2) {
                                processClash(border, ball);
                            } else {
                                processClash(l.move(-Map.CORRIDOR_WIDTH), ball);
                            }
                        }
                        break;
                    }
                }
            }
        }
    }

    private void processClash(Segment l, Ball ball) {
        Vertex curPoint = new Vertex(ball.x, ball.y);
        Vertex futurePoint = new Vertex(ball.x + ball.vx, ball.y + ball.vy);
        if (l.side(curPoint) + l.side(futurePoint) != 1 && l.dist(curPoint) < l.dist(futurePoint)) {
            return;
        }
        double angle = Math.atan2(ball.vy, ball.vx) - Math.atan2(l.b.y - l.a.y, l.b.x - l.a.x);
        if (angle < 0) {
            angle += 2 * Math.PI;
        }
        if (angle > Math.PI) {
            angle -= Math.PI;
        }
        angle *= -2;
        double newvx = Math.cos(angle) * ball.vx - Math.sin(angle) * ball.vy;
        double newvy = Math.cos(angle) * ball.vy + Math.sin(angle) * ball.vx;
        ball.vx = newvx;
        ball.vy = newvy;
    }

    private void processClash(Ball a, Ball b) {
        double vx = a.vx;
        double vy = a.vy;
        a.vx = b.vx / 2;
        a.vy = b.vy / 2;
        b.vx = vx / 2;
        b.vy = vy / 2;
    }

    public synchronized void updateBall(int i, Ball newBall) {
        balls[i].ax = newBall.ax;
        balls[i].ay = newBall.ay;
    }

    public synchronized Ball getBall(int i) {
        return new Ball(balls[i]);
    }

    @org.jetbrains.annotations.Nullable
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        Balls balls = intent.getParcelableExtra(Balls.class.getName());
        this.balls = balls.balls;
        timer = new Timer();
        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                passTime(MainActivity.UPDATE_FREQUENCY / 10);
            }
        };
        timer.schedule(task, 0, MainActivity.UPDATE_FREQUENCY / 10);
        return binder;
    }

    @Override
    public void onDestroy() {
        timer.cancel();
        super.onDestroy();
    }
}