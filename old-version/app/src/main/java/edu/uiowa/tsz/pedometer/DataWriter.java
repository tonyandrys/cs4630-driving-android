package edu.uiowa.tsz.pedometer;

import android.util.Log;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Locale;

/**
 * DataWriter
 * Writes a block of sensor data from buffer queue to the file handler `fh`.
 * When fh is not a new file, new data is appended below old data.
 **/

public class DataWriter implements Runnable {
    public final static String TAG = "DataWriter";

    private File fh;
    private SensorReading[] data;

    public DataWriter(SensorReading[] d, File f) {
        this.fh = f;
        this.data = d;
    }

    @Override
    public void run() {
        try {
            PrintWriter pw = new PrintWriter(new FileWriter(fh, true));
            Log.i(TAG, "Trying to write " + Integer.toString(this.data.length) + "sensor readings to " + this.fh.getName() + "...");

            int lineCount = 0;
            // stringify data in array and write line by line.
            for (int i=0; i<data.length; i++) {
                SensorReading val = data[i];
                // map entry to string
                // ts, A, x, y, z
                // String s = String.format(Locale.US, "%s", val.toString());
                pw.println(val.toString());
                lineCount++;
            }
            pw.close();
            Log.i(TAG, "Closed output stream to " + this.fh.getName() + ", wrote " + Integer.toString(lineCount) + " lines.");

        } catch (FileNotFoundException e) {
            Log.e(TAG, "Could not find file (" + fh.getName() + ")!");
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

}
