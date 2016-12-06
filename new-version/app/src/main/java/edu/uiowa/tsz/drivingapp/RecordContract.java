package edu.uiowa.tsz.drivingapp;

import android.provider.BaseColumns;

/**
 * SQL Contract Class for Driving Records
 * The SQLite schema is defined here; basic utility commands are also stored as constants here to prevent duplication.
 */

public final class RecordContract {

    // Class should not be instantiated.
    private RecordContract() {};

        // Record table structure
        public static class RecordEntry implements BaseColumns {
            public static final String TABLE_NAME = "records";
            public static final String COLUMN_NAME_LOG_NAME = "name";
            public static final String COLUMN_NAME_DISTANCE = "distance";
            public static final String COLUMN_NAME_START_TIME = "startTime";
            public static final String COLUMN_NAME_END_TIME = "endTime";
            public static final String COLUMN_NAME_FILE_COUNT = "fileCount";
            public static final String COLUMN_NAME_ACCEL_PATH = "accelPath";
            public static final String COLUMN_NAME_GYRO_PATH = "gyroPath";
            public static final String COLUMN_NAME_LOCATION_PATH = "locationPath";
            public static final String COLUMN_NAME_ROAD_HAZARD_PATH = "roadHazardPath";
            public static final String COLUMN_NAME_ROAD_COMPOSITION_PATH = "roadCompositionPath";
        }

    // SQL Creation strings
    public static final String TEXT_TYPE = " TEXT";
    public static final String SEP = ",";
    public static final String SQL_CREATE_ENTRIES =
            "CREATE TABLE " + RecordEntry.TABLE_NAME + " (" +
                    RecordEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                    RecordEntry.COLUMN_NAME_LOG_NAME + TEXT_TYPE + SEP +
                    RecordEntry.COLUMN_NAME_DISTANCE + TEXT_TYPE + SEP +
                    RecordEntry.COLUMN_NAME_START_TIME + TEXT_TYPE + SEP +
                    RecordEntry.COLUMN_NAME_END_TIME + TEXT_TYPE + SEP +
                    RecordEntry.COLUMN_NAME_FILE_COUNT + TEXT_TYPE + SEP +
                    RecordEntry.COLUMN_NAME_ACCEL_PATH + TEXT_TYPE + SEP +
                    RecordEntry.COLUMN_NAME_GYRO_PATH + TEXT_TYPE + SEP +
                    RecordEntry.COLUMN_NAME_LOCATION_PATH + TEXT_TYPE + SEP +
                    RecordEntry.COLUMN_NAME_ROAD_HAZARD_PATH + TEXT_TYPE + SEP +
                    RecordEntry.COLUMN_NAME_ROAD_COMPOSITION_PATH + TEXT_TYPE +
                    ")";

    // Delete everything.
    public static final String SQL_DELETE_ENTRIES =
            "DROP TABLE IF EXISTS " + RecordEntry.TABLE_NAME;

}
