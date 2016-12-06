package edu.uiowa.tsz.drivingapp;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.util.Locale;

/**
 * Convenience wrapper around the SQLite database.
 * Construct an instance of this class and call .getWritableDatabase() to get a reference to the DB.
 * Schema upgrades are also performed automatically, which is cool too.
 */
public class RecordDBHelper extends SQLiteOpenHelper {
    public static final String TAG = "RecordDBHelper";
    public static final int DATABASE_VERSION = 5;
    public static final String DATABASE_NAME = "Records.db";

    /**
     * Get a reference to the DB with this constructor
     * ex: RecordDBHelper dbHelper = new RecordDBHelper(getContext());
     * See https://developer.android.com/reference/android/database/sqlite/SQLiteOpenHelper.html
     *
     * @param context Reference to android component calling the database.
     */
    public RecordDBHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(RecordContract.SQL_CREATE_ENTRIES);
        Log.v(TAG, String.format(Locale.US, "Created database %s (v%d)", DATABASE_NAME, DATABASE_VERSION));
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL(RecordContract.SQL_DELETE_ENTRIES);
        onCreate(db);
        Log.v(TAG, String.format(Locale.US, "Upgraded database %s (v%d -> v%d )", DATABASE_NAME, oldVersion, newVersion));
    }

    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        onUpgrade(db, oldVersion, newVersion);
        Log.v(TAG, String.format(Locale.US, "*Downgraded* database %s (v%d -> v%d )", DATABASE_NAME, oldVersion, newVersion));
    }
}
