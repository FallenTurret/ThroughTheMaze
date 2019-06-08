package ru.hse.throughthemaze.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;
import ru.hse.throughthemaze.R;
import ru.hse.throughthemaze.gameplay.Ball;
import ru.hse.throughthemaze.map.Map;
import ru.hse.throughthemaze.map.Vertex;

public class Draw2D extends View {

    public Draw2D(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public Draw2D(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public Draw2D(Context context) {
        super(context);
    }

    private static final double SCALE = 80;

    private Paint paint = new Paint();
    public Map map;
    public int index;
    public Ball[] balls;


    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (balls == null) {
            return;
        }
        double x = balls[index].x;
        double y = balls[index].y;
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.GREEN);
        canvas.drawPaint(paint);
        double width = getWidth();
        double height = getHeight();
        width /= 2;
        height /= 2;
        canvas.save();
        canvas.translate((float)width, (float)height);
        canvas.scale((float)SCALE, (float)SCALE);
        paint.setColor(Color.WHITE);
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
                double angle = Math.atan2(by - ay, bx - ax);
                canvas.save();
                canvas.translate((float)ax, (float)ay);
                canvas.rotate((float)Math.toDegrees(angle));
                canvas.drawRect(0, (float)(-Map.CORRIDOR_WIDTH),
                        (float)(dist), (float)(Map.CORRIDOR_WIDTH), paint);
                canvas.restore();
            }
        }
        paint.setColor(Color.BLACK);
        canvas.drawCircle((float)(map.vertexes[map.end].x - x), (float)(map.vertexes[map.end].y - y),
                (float)Map.VERTEX_RADIUS, paint);
        for (Ball ball: balls) {
            paint.setColor(ball.color);
            canvas.drawCircle((float)(ball.x - x), (float)(ball.y - y), (float)Ball.RADIUS, paint);
        }
        canvas.restore();
    }

}