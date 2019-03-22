package com.example.danil.throughthemaze.database;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.io.*;

public class MapDBHandler extends SQLiteOpenHelper {

    private static final String ASSETS_PATH = "databases";
    private static final int DATABASE_VERSION = 1;
    private static final String DATABASE_NAME = "maps";
    private Context context;
    private SharedPreferences preferences;

    private boolean installedDatabaseIsOutdated() {
        return preferences.getInt(DATABASE_NAME, 0) < DATABASE_VERSION;
    }

    private void writeDatabaseVersionInPreferences() {
        preferences.edit().putInt(DATABASE_NAME, DATABASE_VERSION).apply();
    }

    private synchronized void installOrUpdateIfNecessary() {
        if (installedDatabaseIsOutdated()) {
            context.deleteDatabase(DATABASE_NAME);
            installDatabaseFromAssets();
            writeDatabaseVersionInPreferences();
        }
    }

    private void installDatabaseFromAssets() {
        try (InputStream in = context.getAssets().open("$ASSETS_PATH/$DATABASE_NAME.sqlite3")) {
            File output = new File(context.getDatabasePath(DATABASE_NAME).getPath());
            try (OutputStream out = new FileOutputStream(output)) {
                byte[] buf = new byte[1024];
                int len;
                while ((len = in.read(buf)) > 0) {
                    out.write(buf, 0, len);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("The $DATABASE_NAME couldn't be installed", e);
        }
    }

    public MapDBHandler(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        this.context = context;
        preferences = context.getSharedPreferences(
                context.getPackageName() + ".database_versions", Context.MODE_PRIVATE);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {}

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {}

    @Override
    public SQLiteDatabase getWritableDatabase() {
        throw new RuntimeException("The $DATABASE_NAME database is not writable");
    }

    @Override
    public SQLiteDatabase getReadableDatabase() {
        installOrUpdateIfNecessary();
        return super.getReadableDatabase();
    }
}