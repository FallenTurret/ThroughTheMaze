package ru.hse.throughthemaze.gameplay;

import android.os.Parcel;
import android.os.Parcelable;

public class Balls implements Parcelable {

    public Ball[] balls;

    public Balls(Ball[] balls) {
        this.balls = balls;
    }

    protected Balls(Parcel in) {
        balls = in.createTypedArray(Ball.CREATOR);
    }

    public static final Creator<Balls> CREATOR = new Creator<Balls>() {
        @Override
        public Balls createFromParcel(Parcel in) {
            return new Balls(in);
        }

        @Override
        public Balls[] newArray(int size) {
            return new Balls[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeTypedArray(balls, 0);
    }
}