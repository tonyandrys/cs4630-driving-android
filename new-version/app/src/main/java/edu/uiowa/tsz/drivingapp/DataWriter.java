package edu.uiowa.tsz.drivingapp;

import android.util.Log;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * DataWriter
 * Writes a set (array) of SensorReadings (DATUMS) to a File (f) asynchronously.
 */
public class DataWriter implements Runnable {
    public final static String TAG = "DataWriter";
    private File fh;
    private Datum[] data;

    public DataWriter(Datum[] d, File f) {
        this.fh = f;
        this.data = d;
    }

    @Override
    public void run() {
        try {
            PrintWriter pw = new PrintWriter(new FileWriter(fh, true));

            int lineCount = 0;
            // stringify data in array and write line by line.
            for (int i=0; i<data.length; i++) {
                Datum val = data[i];
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
