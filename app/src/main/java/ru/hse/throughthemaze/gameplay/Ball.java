package ru.hse.throughthemaze.gameplay;

import android.graphics.Color;
import android.os.Parcel;
import android.os.Parcelable;
import ru.hse.throughthemaze.map.Map;

import java.nio.ByteBuffer;

public class Ball implements Parcelable {
    public static final int SIZE = 52;
    public static final double RADIUS = Map.CORRIDOR_WIDTH / 2;
    public double x;
    public double y;
    public double vx;
    public double vy;
    public double ax;
    public double ay;
    public int color;

    public Ball() {}

    public Ball(double x, double y) {
        this.x = x;
        this.y = y;
        vx = 0;
        vy = 0;
        ax = 0;
        ay = 0;
        color = Color.RED;
    }

    public Ball(Ball other) {
        x = other.x;
        y = other.y;
        vx = other.vx;
        vy = other.vy;
        ax = other.ax;
        ay = other.ay;
        color = other.color;
    }

    public Ball(Parcel in) {
        byte[] data = new byte[SIZE];
        in.readByteArray(data);
        read(data);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeByteArray(write());
    }

    public static final Parcelable.Creator<Ball> CREATOR = new Parcelable.Creator<Ball>() {

        @Override
        public Ball createFromParcel(Parcel source) {
            return new Ball(source);
        }

        @Override
        public Ball[] newArray(int size) {
            return new Ball[size];
        }
    };

    public byte[] write() {
        ByteBuffer out = ByteBuffer.allocate(SIZE);
        out.putDouble(x);
        out.putDouble(y);
        out.putDouble(vx);
        out.putDouble(vy);
        out.putDouble(ax);
        out.putDouble(ay);
        out.putInt(color);
        out.flip();
        byte[] bytes = new byte[out.remaining()];
        out.get(bytes);
        return bytes;
    }

    public void read(byte[] data) {
        ByteBuffer buffer = ByteBuffer.allocate(data.length).put(data);
        buffer.flip();
        x = buffer.getDouble();
        y = buffer.getDouble();
        vx = buffer.getDouble();
        vy = buffer.getDouble();
        ax = buffer.getDouble();
        ay = buffer.getDouble();
        color = buffer.getInt();
    }

    public static byte[] toByteArray(Ball[] balls) {
        ByteBuffer buffer = ByteBuffer.allocate(SIZE * balls.length);
        for (Ball ball: balls) {
            buffer.put(ball.write());
        }
        buffer.flip();
        byte[] bytes = new byte[buffer.remaining()];
        buffer.get(bytes);
        return bytes;
    }

    public static Ball[] fromByteArray(byte[] array) {
        ByteBuffer buffer = ByteBuffer.allocate(array.length).put(array);
        buffer.flip();
        Ball[] res = new Ball[buffer.remaining() / SIZE];
        for (int i = 0; i < res.length; i++) {
            res[i] = new Ball();
            byte[] bytes = new byte[SIZE];
            buffer.get(bytes);
            res[i].read(bytes);
        }
        return res;
    }
}