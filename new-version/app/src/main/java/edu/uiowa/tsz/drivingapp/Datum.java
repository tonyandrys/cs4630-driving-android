package edu.uiowa.tsz.drivingapp;

import java.util.ArrayList;
import java.util.Locale;

/**
 * Datum
 * One class for ALL sensor readings, regardless of # of params.
 */
public class Datum {

    public static final String TYPE_ACCELEROMETER = "A";
    public static final String TYPE_GYROSCOPE = "G";
    public static final String TYPE_LOCATION = "L";
    public static final String TYPE_ROAD_COMPOSITION = "C";
    public static final String TYPE_ROAD_HAZARD = "H";

    private long timestamp;
    private String type;
    private ArrayList<Float> data;

    public Datum(long ts, String type, float... floats) {
        this.timestamp = ts;
        this.type = type;
        this.data = new ArrayList<Float>(floats.length);
        for (float f: floats) {
            data.add(f);
        }
    }

    public float getValueByIndex(int i) {
        return data.get(i);
    }

    public int getNumberOfValues() {
        return this.data.size();
    }

    public String toString() {
        // CSV is hardcoded right now, deal with it
        String dataAsACSVString = "";
        for (int i=0; i<data.size(); i++) {
            dataAsACSVString += data.get(i).toString();
            if (i!=data.size()-1) {
                dataAsACSVString += ",";
            }
        }
        return String.format(Locale.US, "%d,%s,%s", this.timestamp, this.type, dataAsACSVString);
    }


}
