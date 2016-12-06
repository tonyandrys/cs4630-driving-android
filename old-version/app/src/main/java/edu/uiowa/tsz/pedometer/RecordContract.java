package edu.uiowa.tsz.pedometer;

import android.provider.BaseColumns;

/**
 * SQL Contract class for Driving Records
 */
public final class RecordContract {

    // Class should not be instantiated.
    private RecordContract() {};

    // Record table structure
    public static class RecordEntry implements BaseColumns {
        public static final String TABLE_NAME = "records";
        public static final String COLUMN_NAME_LOGNAME = "name";
        public static final String COLUMN_NAME_COMMENT = "comment";
        public static final String COLUMN_NAME_START_TIME = "startTime";
        public static final String COLUMN_NAME_END_TIME = "endTime";
        public static final String COLUMN_NAME_ACCEL_PATH = "accelPath";
        public static final String COLUMN_NAME_GYRO_PATH = "gyroPath";
    }

    // SQL Creation strings
    public static final String TEXT_TYPE = "TEXT";
    public static final String SQL_CREATE_ENTRIES =
            "CREATE TABLE " + RecordEntry.TABLE_NAME + " (" +
                    RecordEntry._ID + " INTEGER PRIMARY KEY," +
                    RecordEntry.COLUMN_NAME_LOGNAME + TEXT_TYPE + "," +
                    RecordEntry.COLUMN_NAME_COMMENT + TEXT_TYPE + "," +
                    RecordEntry.COLUMN_NAME_START_TIME + TEXT_TYPE + "," +
                    RecordEntry.COLUMN_NAME_END_TIME + TEXT_TYPE + "," +
                    RecordEntry.COLUMN_NAME_ACCEL_PATH + TEXT_TYPE + "," +
                    RecordEntry.COLUMN_NAME_GYRO_PATH + TEXT_TYPE + "," +
                    ")";

    public static final String SQL_DELETE_ENTRIES =
            "DROP TABLE IF EXISTS " + RecordEntry.TABLE_NAME;

}
