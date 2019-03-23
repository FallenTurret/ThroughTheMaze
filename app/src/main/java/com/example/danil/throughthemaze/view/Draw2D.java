package com.example.danil.throughthemaze.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.View;
import com.example.danil.throughthemaze.map.Map;
import com.example.danil.throughthemaze.map.Vertex;

public class Draw2D extends View {

    private static final double SCALE = 20;

    private Map map;
    private Paint paint = new Paint();
    public double x;
    public double y;

    public Draw2D(Context context, Map map) {
        super(context);
        this.map = map;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.GREEN);
        canvas.drawPaint(paint);
        paint.setColor(Color.WHITE);
        double width = canvas.getWidth();
        double height = canvas.getHeight();
        width /= 2;
        height /= 2;
        canvas.save();
        canvas.translate((float)width, (float)height);
        canvas.scale((float)SCALE, (float)SCALE);
        for (Vertex v: map.vertexes) {
            double dx = v.x - x;
            double dy = v.y - y;
            canvas.drawCircle((float)dx, (float)dy, (float)Map.VERTEX_RADIUS, paint);
        }
        for (int i = 0; i < map.size; i++) {
            for (int j: map.edges.get(i)) {
                if (i > j) {
                    continue;
                }
                Vertex a = map.vertexes[i];
                Vertex b = map.vertexes[j];
                double dist = a.dist(b);
                double ax = a.x - x;
                double ay = a.y - y;
                double bx = b.x - x;
                double by = b.y - y;
                double angle = -Math.atan2(by - ay, bx - ax);
                canvas.save();
                canvas.rotate((float)Math.toDegrees(angle));
                double newx = ax * Math.cos(angle) - ay * Math.sin(angle);
                double newy = ax * Math.sin(angle) + ay * Math.cos(angle) - Map.CORRIDOR_WIDTH;
                canvas.drawRect((float)newx, (float)newy,
                        (float)(newx + dist), (float)(newy + 2 * Map.CORRIDOR_WIDTH), paint);
                canvas.restore();
            }
        }
        paint.setColor(Color.RED);
        canvas.drawCircle(0, 0, (float)(Map.CORRIDOR_WIDTH / 2), paint);
        canvas.restore();
    }
}