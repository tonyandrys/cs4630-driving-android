package edu.uiowa.tsz.drivingapp;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.util.Log;

import java.util.Locale;

/**
 * DatabaseManager
 * In which I do my best to abstract over the trash heap that is sqlite.
 */
public class DatabaseManager {
    public static final String TAG = "DatabaseManager";

    private DatabaseManager() {};

    /**
     * Adds a new Record to the database using the keys (column names) and values defined in 'cv'.
     * @param context
     * @param cv
     */
    public static void insertRecord(final Context context, final ContentValues cv) {
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                RecordDBHelper dbHelper = new RecordDBHelper(context);
                SQLiteDatabase db = dbHelper.getWritableDatabase();
                long id = db.insert(RecordContract.RecordEntry.TABLE_NAME, null, cv);
                if (id == -1) {
                    Log.e(TAG, String.format(Locale.US, "Failed to insert Record '%s' into db!", cv.get(RecordContract.RecordEntry.COLUMN_NAME_LOG_NAME)));
                } else {
                    Log.v(TAG, String.format(Locale.US, "Added Record '%s' to the database.", cv.get(RecordContract.RecordEntry.COLUMN_NAME_LOG_NAME)));
                }
            }
        });
        t.start();
    }

    /**
     * Modifies values of a Record at row 'rowID'.
     * For each element (k,v) in 'cv' -> the value in column 'k' will be changed to 'v'. All other columns in the row will not be changed.
     * @param rowID
     * @param context
     * @param cv
     */
    public static void updateRecordByID(final int rowID, final Context context, final ContentValues cv) {
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                RecordDBHelper dbHelper = new RecordDBHelper(context);
                SQLiteDatabase db = dbHelper.getWritableDatabase();
                int updatedRows = db.update(RecordContract.RecordEntry.TABLE_NAME, cv, "WHERE id = ?", new String[]{Integer.toString(rowID)});
                if (updatedRows == 1) {
                    Log.v(TAG, String.format(Locale.US, "Updated Record with id '%d'", rowID));
                } else {
                    Log.e(TAG, String.format(Locale.US, String.format(Locale.US, "Unexpected # of rows modified when updating %d! Should be 1, actual # was '%d'."), rowID, updatedRows));
                }
            }
        });
        t.start();
    }

    /**
     * Drops the Record at row 'rowID' from the Record table.
     * @param rowID
     * @param context
     */
    public static void deleteRecordByID(final int rowID, final Context context) {
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                RecordDBHelper dbHelper = new RecordDBHelper(context);
                SQLiteDatabase db = dbHelper.getWritableDatabase();
                int deletedRows = db.delete(RecordContract.RecordEntry.TABLE_NAME, "WHERE id = ?", new String[]{Integer.toString(rowID)});
                if (deletedRows == 1) {
                    Log.v(TAG, String.format(Locale.US, "Deleted Record with id '%d'", rowID));
                } else {
                    Log.e(TAG, String.format(Locale.US, String.format(Locale.US, "Unexpected # of rows deleted when updating %d! Should be 1, actual # was '%d'."), rowID, deletedRows));
                }

            }
        });
        t.start();
    }

    public static Cursor getAllRecords(final Context context) {
        String query = "SELECT * FROM " + RecordContract.RecordEntry.TABLE_NAME + " ORDER BY " + RecordContract.RecordEntry.COLUMN_NAME_START_TIME + " DESC";
        SQLiteDatabase db = new RecordDBHelper(context).getReadableDatabase();
        return db.rawQuery(query, null);
    }

}
