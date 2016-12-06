package edu.uiowa.tsz.drivingapp;

import android.Manifest;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

/**
 * LoggingService
 * Handles and records sensor readings, coordinates writing data to memory/files.
 */
public class LoggingService extends Service implements SensorEventListener, LocationListener {

    public static final String TAG = "LoggingService";

    // Output file headers
    static final String ACCEL_GYRO_OUTPUT_FILE_HEADER = "timestamp,type,x,y,z";
    static final String LOCATION_OUTPUT_FILE_HEADER = "timestamp,type,latitude,longitude,velocity,bearing";
    static final String ROAD_COMPOSITION_OUTPUT_FILE_HEADER = "timestamp,type,compositionLabel,compositionValue";
    static final String ROAD_HAZARD_OUTPUT_FILE_HEADER = "timestamp,type,hazardLabel,hazardValue";

    // Broadcast references & message intent keys
    LocalBroadcastManager localBroadcastManager;
    public static final String ACTION_UI_UPDATE = "action-ui-update";
    public static final String INTENT_EXTRA_VELOCITY = "velocity";
    public static final String INTENT_EXTRA_BEARING = "bearing";


    // OS & Sensor references
    protected NotificationManager notificationManager;
    protected LocationManager locationManager;
    protected SensorManager sensorManager;
    protected Sensor accelerometer;
    protected Sensor gyroscope;

    // Location members
    private LocationProcessor locationProcessor;
    private boolean isLocationTrackingPermitted = false;

    // Concurrency - parameters and members for the shared queues.
    final protected int BLOCKING_QUEUE_CAPCITY = 100;
    final protected BlockingQueue<Datum> accelQueue = new ArrayBlockingQueue<>(BLOCKING_QUEUE_CAPCITY);
    final protected BlockingQueue<Datum> gyroQueue = new ArrayBlockingQueue<>(BLOCKING_QUEUE_CAPCITY);
    final protected BlockingQueue<Datum> locationQueue = new ArrayBlockingQueue<>(BLOCKING_QUEUE_CAPCITY);

    // Filename suffixes

    // File descriptors for writing output.
    private File accelfh;
    private File gyrofh;

    // Threads - one per BlockingQueue declared above.
    private SensorConsumer sc1;
    private SensorConsumer sc2;
    private SensorConsumer sc3;

    // Record for *this* logging instance.
    private Record activeRecord;

    public LoggingService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void onCreate() {
        // Get references to all necessary sensors from the OS.
        notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
        gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        locationProcessor = new LocationProcessor();
    }

    /**
     * Creates new logging files with the appropriate suffixes, writes the appropriate header, and returns file descriptor references to the open buffers in append mode.
     *
     * IMPORTANT: the three array params must be the same length, and associated values must be stored in the same index across all!
     * ie:
     *      - The first line in this new file will be the String in fh[i]
     *      - The filename of fd[i] is the result of concat(baseFileName, fs[i])
     *
     *      //TODO: The arguments can be refactored to just two (baseFileName and dataType array). But it still works fine as it is.
     *
     * @param baseFileName
     * @param fileHeaders
     * @param filenameSuffixes
     */
    public HashMap<String,File> initializeLoggingFiles(String baseFileName, String[] fileHeaders, String[] filenameSuffixes, String[] dataTypes) {

        // check right away that all array lengths are equal to maintain some level of sanity
        if (!(fileHeaders.length == filenameSuffixes.length)) {
            Log.e(TAG, String.format(Locale.US, "Cannot initialize logfiles! Length of file headers (l=%d), and filename suffixes (l=%d) arrays differ.", fileHeaders.length, filenameSuffixes.length));
            return null;
        }
        int fileCount = fileHeaders.length;
        HashMap<String,File> fmap = new HashMap<>(fileCount);

        // compile associated file meta data and write the first line of text.
        for (int i=0;i<fileCount;i++) {
            String filename = baseFileName + filenameSuffixes[i];       // construct filename
            String header = fileHeaders[i];
            String dataType = dataTypes[i];

            // Create new empty file in /intstorage/logs/
            File f = FileSystemManager.createNewLogFile(this, filename);

            // write the header string to the new file
            try {
                PrintWriter pw = new PrintWriter(new FileWriter(f, true));
                pw.println(header);
                pw.close();
                Log.i(TAG, "Created logfile '" + f.getName() + "' and wrote header '" + header + "'.");

                // everything is OK -- add the new fd to the file map.
                fmap.put(dataType, f);
            } catch (FileNotFoundException e) {
                Log.e(TAG, "File '" + f.getName() + "' not found!");
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return fmap;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startID) {
        // Let this service receive all sensor messages
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_FASTEST);
        sensorManager.registerListener(this, gyroscope, SensorManager.SENSOR_DELAY_FASTEST);

        // used to send UI update messages back to the calling activity.
        this.localBroadcastManager = LocalBroadcastManager.getInstance(this);

        // Request GPS & Network location updates
        try {
            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, this);
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);
            this.isLocationTrackingPermitted = true;
            Log.v(TAG, "Location tracking started.");
        } catch (SecurityException e) {
            Log.e(TAG, "Location permissions have not been granted to this application! Velocity & location information will not be recorded.");
            e.printStackTrace();
            this.isLocationTrackingPermitted = false;
        }

        // Initialize logfiles
        String baseFileName = intent.getStringExtra(MainActivity.INTENT_EXTRA_FILENAME);
        String[] fileHeaders = {ACCEL_GYRO_OUTPUT_FILE_HEADER, ACCEL_GYRO_OUTPUT_FILE_HEADER, LOCATION_OUTPUT_FILE_HEADER, ROAD_HAZARD_OUTPUT_FILE_HEADER, ROAD_COMPOSITION_OUTPUT_FILE_HEADER};
        String[] filenameSuffixes = {"-ACCEL.csv", "-GYRO.csv", "-LOC.csv", "-HAZ.csv", "-COMP.csv"};
        String[] fileDataTypes = {Datum.TYPE_ACCELEROMETER, Datum.TYPE_GYROSCOPE, Datum.TYPE_LOCATION, Datum.TYPE_ROAD_HAZARD, Datum.TYPE_ROAD_COMPOSITION};
        HashMap<String,File> fileMap = initializeLoggingFiles(baseFileName, fileHeaders, filenameSuffixes, fileDataTypes);

        // activeRecord encapsulates the information about this logging trip and is used as the database model. Add the new file paths to the record.
        this.activeRecord = new Record(intent.getStringExtra(MainActivity.INTENT_EXTRA_FILENAME), new Date());
        this.activeRecord.addFile(Datum.TYPE_ACCELEROMETER, fileMap.get(Datum.TYPE_ACCELEROMETER).getName());
        this.activeRecord.addFile(Datum.TYPE_GYROSCOPE, fileMap.get(Datum.TYPE_GYROSCOPE).getName());
        this.activeRecord.addFile(Datum.TYPE_LOCATION, fileMap.get(Datum.TYPE_LOCATION).getName());
        this.activeRecord.addFile(Datum.TYPE_ROAD_HAZARD, fileMap.get(Datum.TYPE_ROAD_HAZARD).getName());
        this.activeRecord.addFile(Datum.TYPE_ROAD_COMPOSITION, fileMap.get(Datum.TYPE_ROAD_COMPOSITION).getName());

        // Start a consumer for each bufferQueue
        this.sc1 = new SensorConsumer(this.accelQueue, fileMap.get(Datum.TYPE_ACCELEROMETER));
        this.sc2 = new SensorConsumer(this.gyroQueue, fileMap.get(Datum.TYPE_GYROSCOPE));
        this.sc3 = new SensorConsumer(this.locationQueue, fileMap.get(Datum.TYPE_LOCATION));
        sc1.start();
        sc2.start();
        sc3.start();

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        // Consumers should dump their remaining buffers to their files
        sc1.forceDumpToFile();
        sc2.forceDumpToFile();
        sc3.forceDumpToFile();

        // Write newly created record to DB.
        this.activeRecord.setEndTime(new Date());
        DatabaseManager.insertRecord(this, this.activeRecord.toContentValueRepresentation());

        // Unregister listeners and sensor hooks.
        sensorManager.unregisterListener(this, accelerometer);
        sensorManager.unregisterListener(this, gyroscope);
        Log.i(TAG, "Destroyed sensor listeners.");

        try {
            locationManager.removeUpdates(this);
            locationManager = null;
            Log.i(TAG, "Destroyed location listener.");
        } catch (SecurityException e) {
            e.printStackTrace();
        }
    }

    /**
     * Broadcasts the velocity associated with l (Location) to the MainActivity to update the UI.
     * @param l
     */
    public void broadcastVelocity(Location l) {
        Intent i = new Intent(ACTION_UI_UPDATE);
        i.putExtra(INTENT_EXTRA_VELOCITY, l.getSpeed());
        Log.v(TAG, "Best estimate for velocity is " + l.getSpeed());
        LocalBroadcastManager.getInstance(this).sendBroadcast(i);
    }

    /**
     * Broadcasts the cardinal direction associated with the bearing 'b' to MainActivity to update the UI.
     * @param b double in the interval [0,360]
     */
    public void broadcastBearing(double b) {

        Intent i = new Intent(ACTION_UI_UPDATE);
        i.putExtra(INTENT_EXTRA_BEARING, b);
        Log.v(TAG, String.format(Locale.US, "Best estimate for bearing is '%f", b));
        LocalBroadcastManager.getInstance(this).sendBroadcast(i);
    }

    /** Sensor Callbacks **/
    @Override
    public void onSensorChanged(SensorEvent e) {
        Datum newEvent;
        // Produce: put new value onto queue if possible, log if we can't (and will therefore lose data)

        if (e.sensor == accelerometer) {
            newEvent = new Datum(e.timestamp, Datum.TYPE_ACCELEROMETER, e.values[0], e.values[1], e.values[2]);
            try {
                accelQueue.put(newEvent);
            } catch (InterruptedException e1) {
                Log.w(TAG, "** lost accel data -> (time: " + newEvent.toString() + ")");
                e1.printStackTrace();
            }
        } else if (e.sensor == gyroscope) {
            newEvent = new Datum(e.timestamp, Datum.TYPE_GYROSCOPE, e.values[0], e.values[1], e.values[2]);
            try {
                gyroQueue.put(newEvent);
            } catch (InterruptedException e1) {
                Log.w(TAG, "** lost gyro data -> (time: " + newEvent.toString() + ")");
                e1.printStackTrace();
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

    /** Location Callbacks **/
    @Override
    public void onLocationChanged(Location location) {
        // called when a new location is found by the network location provider
        // ie, do something with 'loc'.
        Location l = locationProcessor.feed(location);
        Datum newEvent = new Datum(location.getTime(), Datum.TYPE_LOCATION, (float)location.getLatitude(), (float)location.getLongitude(), location.getSpeed(), location.getBearing());
        try {
            locationQueue.put(newEvent);
        } catch (InterruptedException e) {
            Log.w(TAG, "** lost location data -> (time: " + l.getTime() + ")");
            e.printStackTrace();
        }
        broadcastVelocity(l);
        broadcastBearing(l.getBearing());
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider) {

    }

    @Override
    public void onProviderDisabled(String provider) {

    }
}
