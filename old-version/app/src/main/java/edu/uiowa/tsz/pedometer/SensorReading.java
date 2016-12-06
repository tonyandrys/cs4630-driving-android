package edu.uiowa.tsz.pedometer;

import android.annotation.SuppressLint;

import java.util.Locale;

/**
 * Object for holding sensor readings because passing around float[][]s is making me lose my mind
 **/

public class SensorReading extends Object {

    private long timestamp;
    private String type;
    private float x;
    private float y;
    private float z;

    // A sensor reading contains a long timestamp (ts), String type ('A' or 'G'), and floats x,y,z which are the measurements.
    @SuppressLint("Assert")
    SensorReading(long ts, String type, float x, float y, float z) {
        this.timestamp = ts;
        this.type = type;
        this.x = x;
        this.y = y;
        this.z = z;
        assert(this.type.equalsIgnoreCase("G"))||( this.type.equalsIgnoreCase("A"));
    }

    // Returns contents of SensorReading in CSV compliant string format.
    public String toString() {
        return String.format(Locale.US, "%d,%s,%f,%f,%f", this.timestamp, this.type, this.x, this.y, this.z);
    }

}