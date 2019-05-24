package com.example.danil.throughthemaze.gameplay;

import android.graphics.Color;
import android.os.Parcel;
import android.os.Parcelable;
import com.example.danil.throughthemaze.map.Map;

import java.nio.ByteBuffer;

public class Ball implements Parcelable {
    private static final int SIZE = 52;
    public static final double RADIUS = Map.CORRIDOR_WIDTH / 2;
    public double x;
    public double y;
    public double vx;
    public double vy;
    public double ax;
    public double ay;
    public int color;

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
        ByteBuffer wrapped = ByteBuffer.wrap(data);
        x = wrapped.getDouble();
        y = wrapped.getDouble();
        vx = wrapped.getDouble();
        vy = wrapped.getDouble();
        ax = wrapped.getDouble();
        ay = wrapped.getDouble();
        color = wrapped.getInt();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        ByteBuffer out = ByteBuffer.allocate(SIZE);
        out.putDouble(x);
        out.putDouble(y);
        out.putDouble(vx);
        out.putDouble(vy);
        out.putDouble(ax);
        out.putDouble(ay);
        out.putInt(color);
        dest.writeByteArray(out.array());
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
}