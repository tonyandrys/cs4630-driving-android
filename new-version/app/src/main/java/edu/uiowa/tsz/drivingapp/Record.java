package edu.uiowa.tsz.drivingapp;


import android.content.ContentValues;
import android.database.Cursor;
import android.util.ArraySet;
import android.util.Log;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Set;

/**
 * Record
 * The core data model for this app. Records encapsulate all data about a logging session.
 *      - Record objects can be constructed directly from a db projection (buildRecordFromCursor()).
 *      - A Record object may be modified and the changes can be written to its corresponding row in the database (toContentValuesRepresentation()).
 */
public class Record implements Serializable {

    private static final long serialVersionUID = 14929090909091492L;
    public static final String TAG = "Record";

    // When passing Records from component to component, use this intent key
    public static final String INTENT_EXTRA_RECORD = "record";

    private String recordName;
    private Date startTime;
    private Date endTime;
    private float distance;
    private HashMap<String,String> fileMap;

    /**
     * Given a cursor that is pointing to a row in the record database, this method constructs
     * and returns a Record object representing the data in the row.
     * @param c
     * @return Record
     */
    public static Record buildRecordFromCursor(Cursor c) {
        Record r = new Record();

        // log name
        int nameIndex = c.getColumnIndex(RecordContract.RecordEntry.COLUMN_NAME_LOG_NAME);
        if (!c.isNull(nameIndex)) { r.setRecordName(c.getString(nameIndex)); }

        // start & end time
        int startTimeIndex = c.getColumnIndex(RecordContract.RecordEntry.COLUMN_NAME_START_TIME);
        int endTimeIndex = c.getColumnIndex(RecordContract.RecordEntry.COLUMN_NAME_END_TIME);
        if (!c.isNull(startTimeIndex)) { r.setStartTime(new Date(c.getLong(startTimeIndex))); }
        if (!c.isNull(endTimeIndex)) { r.setEndTime(new Date(c.getLong(endTimeIndex))); }

        // distance
        int distanceIndex = c.getColumnIndex(RecordContract.RecordEntry.COLUMN_NAME_DISTANCE);
        if (!c.isNull(distanceIndex)) { r.setDistance(c.getFloat(distanceIndex)); }

        // files
        int accelPathIndex = c.getColumnIndex(RecordContract.RecordEntry.COLUMN_NAME_ACCEL_PATH);
        int gyroPathIndex = c.getColumnIndex(RecordContract.RecordEntry.COLUMN_NAME_GYRO_PATH);
        int locationPathIndex = c.getColumnIndex(RecordContract.RecordEntry.COLUMN_NAME_LOCATION_PATH);
        int roadHazardPathIndex = c.getColumnIndex(RecordContract.RecordEntry.COLUMN_NAME_ROAD_HAZARD_PATH);
        int roadCompositionPathIndex = c.getColumnIndex(RecordContract.RecordEntry.COLUMN_NAME_ROAD_COMPOSITION_PATH);

        if (!c.isNull(accelPathIndex)) {
            r.addFile(Datum.TYPE_ACCELEROMETER, c.getString(accelPathIndex));
        }
        if (!c.isNull(gyroPathIndex)) {
            r.addFile(Datum.TYPE_GYROSCOPE, c.getString(gyroPathIndex));
        }
        if (!c.isNull(locationPathIndex)) {
            r.addFile(Datum.TYPE_LOCATION, c.getString(locationPathIndex));
        }
        if (!c.isNull(roadHazardPathIndex)) {
            r.addFile(Datum.TYPE_ROAD_HAZARD, c.getString(roadHazardPathIndex));
        }
        if (!c.isNull(roadCompositionPathIndex)) {
            r.addFile(Datum.TYPE_ROAD_COMPOSITION, c.getString(roadCompositionPathIndex));
        }
        Log.v(TAG, String.format(Locale.US, "Built Record from cursor: %s", r.toString()));
        return r;
    }

    /**
     * Minimalist Record constructor, all primitive fields are initialized with "blank" values.
     * All data structures are created but contain nothing.
     */
    public Record() {
        this.recordName = "";
        this.startTime = new Date(0L);
        this.endTime = new Date(0L);
        this.distance = 0f;
        this.fileMap = new HashMap<String,String>();    // Potential memory problem? I don't like initializing these without a length.
    }

    /**
     * Creates a new record with name and a startTime.
     * @param name name of log
     * @param startTime Date object representing the *start* of logging.
     */
    public Record(String name, Date startTime) {
        this.recordName = name;
        this.startTime = startTime;
        this.endTime = startTime;                       // will be updated later
        this.distance = 0f;                             // so will this
        this.fileMap = new HashMap<String,String>();
    }

    public void setRecordName(String name) {
        this.recordName = name;
    }

    public String getRecordName() {
        if (this.recordName != null) {
            return this.recordName;
        } else {
            Log.w(TAG, "Record has no name -- returning the empty string! This is probably a bug.");
            return "";
        }
    }

    public Date getStartTime() {
        return this.startTime;
    }

    public void setStartTime(Date startTime) {
        this.startTime = startTime;
    }

    public Date getEndTime() {
        return this.endTime;
    }

    /**
     * Calculates the total time elapsed between startTime and endTime.
     * @return number of miliseconds in interval (long).
     */
    public long getElapsedTime() {
        long diff;
        if ((this.endTime == null) || (this.startTime == null)) {
            Log.w(TAG, String.format(Locale.US, "Record '%s' cannot calculate elapsed time, startTime or endTime is null! Returning 0ms.", this.recordName));
            diff = 0L;
        } else {
            // we have two valid Dates, calculate the difference
            diff = (this.endTime.getTime() - this.startTime.getTime());
            // if diff is negative, there's a bug somewhere and we should yell about it
            if (diff < 0) {
                Log.w(TAG, String.format(Locale.US, "Record '%s' has startTime > endTime! This is probably a bug.", this.recordName));
            }
        }
        return diff;
    }

    public void setEndTime(Date endTime) {
        this.endTime = endTime;
    }

    /**
     * Sets the total distance traveled within this record as a float (in miles).
     * @param miles
     */
    public void setDistance(float miles) {
        this.distance = miles;
    }

    public float getDistance() {
        return this.distance;
    }

    /**
     * Associates a new file with this driving record. Use type constants defined in Datum class.
     * @param type TYPE_ACCELEROMETER/TYPE_GYROSCOPE/TYPE_VELOCITY
     * @param path path to file
     * @return boolean true if file was added successfully, false on failure.
     */
    public boolean addFile(String type, String path) {
        // To avoid a huge mess, only allow one type of file per record. Hopefully this won't have to be altered.
        if (!this.fileMap.containsKey(type)) {
            this.fileMap.put(type, path);
            return true;
        } else {
            Log.e(TAG, String.format(Locale.US, "Record (name=%s) has existing file of type %s. Cannot add multiple files of the same type!", this.recordName, type));
            return false;
        }

    }

    /**
     * Removes a file from the driving record by type. Use type constants defined in Datum class.
     * @param type TYPE_ACCELEROMETER/TYPE_GYROSCOPE/TYPE_VELOCITY
     * @return boolean true if file was removed successfully, false if file to be removed is non-existent or another error occurs.
     */
    public boolean removeFile(String type) {
        if (this.fileMap.containsKey(type)) {
            this.fileMap.remove(type);
            return true;
        } else {
            Log.e(TAG, String.format(Locale.US, "Record (name=%s) no file of type %s. Cannot remove file!", this.recordName, type));
            return false;
        }
    }

    /**
     * Checks if the record has a file of type 'type' associated with it.
     * @param type TYPE_ACCELEROMETER/TYPE_GYROSCOPE/TYPE_VELOCITY
     * @return boolean true if file exists, false if not.
     */
    public boolean hasFileWithType(String type) {
        return this.fileMap.containsKey(type);
    }

    /**
     * Returns the file map {type_1 -> path_1, ... type_n -> path_n} for this record
     * @return HashMap<String,String>
     */
    public HashMap<String,String> getFileMap() {
        return this.fileMap;
    }

    public int getFileCount() {
        return this.fileMap.size();
    }

    /**
     * Converts this Record to a set of ContentValues for writing to the sqlite database.
     */
    public ContentValues toContentValueRepresentation() {
        ContentValues cv = new ContentValues();
        cv.put(RecordContract.RecordEntry.COLUMN_NAME_LOG_NAME, this.recordName);
        cv.put(RecordContract.RecordEntry.COLUMN_NAME_DISTANCE, Float.toString(this.distance));
        cv.put(RecordContract.RecordEntry.COLUMN_NAME_START_TIME, Long.toString(this.startTime.getTime()));
        cv.put(RecordContract.RecordEntry.COLUMN_NAME_END_TIME, Long.toString(this.endTime.getTime()));
        cv.put(RecordContract.RecordEntry.COLUMN_NAME_FILE_COUNT, Integer.toString(this.fileMap.size()));

        // one cv entry for each file
        String[] keys = fileMap.keySet().toArray(new String[0]);
        for (int i=0; i<keys.length; i++) {
            String k = keys[i];
            switch(k) {
                case Datum.TYPE_ACCELEROMETER:
                    cv.put(RecordContract.RecordEntry.COLUMN_NAME_ACCEL_PATH, fileMap.get(k));
                    break;
                case Datum.TYPE_GYROSCOPE:
                    cv.put(RecordContract.RecordEntry.COLUMN_NAME_GYRO_PATH, fileMap.get(k));
                    break;
                case Datum.TYPE_LOCATION:
                    cv.put(RecordContract.RecordEntry.COLUMN_NAME_LOCATION_PATH, fileMap.get(k));
                    break;
                case Datum.TYPE_ROAD_HAZARD:
                    cv.put(RecordContract.RecordEntry.COLUMN_NAME_ROAD_HAZARD_PATH, fileMap.get(k));
                    break;
                case Datum.TYPE_ROAD_COMPOSITION:
                    cv.put(RecordContract.RecordEntry.COLUMN_NAME_ROAD_COMPOSITION_PATH, fileMap.get(k));
                    break;
                default:
                    Log.w(TAG, String.format(Locale.US, "Record '%s' has unknown key '%s' in fileMap w/ no associated sqlite column! Discarding the file path.", this.recordName, k));
            }
        }
        return cv;
    }

    @Override
    public String toString() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd-HH:mm:ss", Locale.US);
        String fileTypesString = "";
        String[] keys = this.fileMap.keySet().toArray(new String[0]);
        for (int i=0;i<keys.length;i++) {
            fileTypesString += keys[i];
            if (i != keys.length-1) {
                fileTypesString += "/";
            }
        }
        return String.format(Locale.US, "['%s' | start '%s' | end '%s' | distance '%s' | files '%s']", this.recordName, dateFormat.format(this.startTime), dateFormat.format(this.endTime), this.distance, fileTypesString);
    }

}
