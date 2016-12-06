package edu.uiowa.tsz.pedometer;

import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.IBinder;
import android.util.Log;
import android.os.PowerManager.WakeLock;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class AccelService extends Service implements SensorEventListener {

    // Class logging prefix
    static final String TAG = "AccelService";

    // Header for output files
    static final String OUTPUT_FILE_HEADER = "timestamp,type,x,y,z";

    // File descriptor for output target.
    private File accelfh;
    private File gyrofh;

    // HACK: Access paths of accelfh and gyrofh in SharedPreferences using these keys
    public static final String SHAREDPREFS_ACCEL_SAVED_FILE_PATH = "ACCEL_SAVED_FILE_PATH";
    public static final String SHAREDPREFS_GYRO_SAVED_FILE_PATH = "GYRO_SAVED_FILE_PATH";

    // Concurrency - parameters and members for the shared queues.
    final protected int BLOCKING_QUEUE_CAPCITY = 100;
    final protected BlockingQueue<SensorReading> accelQueue = new ArrayBlockingQueue<>(BLOCKING_QUEUE_CAPCITY);
    final protected BlockingQueue<SensorReading> gyroQueue = new ArrayBlockingQueue<>(BLOCKING_QUEUE_CAPCITY);

    // Sensor/OS related instances
    protected NotificationManager notificationManager;
    protected SensorManager sensorManager;
    protected Sensor accelerometer;
    protected Sensor gyroscope;

    // Threads
    private SensorConsumer sc1;
    private SensorConsumer sc2;


    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void onCreate() {
        // Get instances of all necessary sensors from the OS.
        notificationManager = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        Log.i(TAG, "onCreate()");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startID) {
        // Service will receive all sensor messages
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_FASTEST);
        sensorManager.registerListener(this, gyroscope, SensorManager.SENSOR_DELAY_FASTEST);
        Log.i(TAG, "onStartCommand");

        // Construct filehandlers for both accel and gyro files
        String accelFilename = intent.getStringExtra(MainActivity.INTENT_EXTRA_FILENAME) + "-ACCEL";
        String gyroFilename = intent.getStringExtra(MainActivity.INTENT_EXTRA_FILENAME) + "-GYRO";
        this.accelfh = new File(this.getExternalCacheDir(), accelFilename);
        this.gyrofh = new File(this.getExternalCacheDir(), gyroFilename);

        // Store paths of accel and gyro file in sharedprefs
        SharedPreferences prefs = getSharedPreferences(TAG, 0);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(SHAREDPREFS_ACCEL_SAVED_FILE_PATH, accelfh.getAbsolutePath());
        editor.putString(SHAREDPREFS_GYRO_SAVED_FILE_PATH, gyrofh.getAbsolutePath());
        editor.apply();

        Log.i(TAG, "ACCEL FILE IS AT " + this.accelfh.getAbsolutePath());
        Log.i(TAG, "GYRO FILE IS AT " + this.gyrofh.getAbsolutePath());

        // Write header lines to both files
        try {
            PrintWriter pw = new PrintWriter(new FileWriter(accelfh, true));
            pw.println(OUTPUT_FILE_HEADER);
            pw.close();
            Log.i(TAG, "Created accel data file (" + accelfh.getName() + ") and wrote header (" + OUTPUT_FILE_HEADER + ").");
        } catch (FileNotFoundException e) {
            Log.e(TAG, "File (" + accelfh.getName() + ") not found!");
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            PrintWriter pw = new PrintWriter(new FileWriter(gyrofh, true));
            pw.println(OUTPUT_FILE_HEADER);
            pw.close();
            Log.i(TAG, "Created accel data file (" + gyrofh.getName() + ") and wrote header (" + OUTPUT_FILE_HEADER + ").");
        } catch (FileNotFoundException e) {
            Log.e(TAG, "File (" + gyrofh.getName() + ") not found!");
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Start one consumer for the accel queue, one for the gyro queue
        this.sc1 = new SensorConsumer(this.accelQueue, this.accelfh);
        this.sc2 = new SensorConsumer(this.gyroQueue, this.gyrofh );
        sc1.start();
        sc2.start();
        return START_STICKY;
    }


    @Override
    public void onDestroy() {
        sensorManager.unregisterListener(this, accelerometer);
        sensorManager.unregisterListener(this, gyroscope);
        Log.i("AccelService", "Destroyed sensor listeners.");
    }


    @Override
    public void onSensorChanged(SensorEvent e) {

    SensorReading newEvent;
        // Produce: put new value onto queue if possible, log if we can't (and will therefore lose data)

        if (e.sensor == accelerometer) {
            newEvent = new SensorReading(e.timestamp, "A", e.values[0], e.values[1], e.values[2]);
            try {
                accelQueue.put(newEvent);
            } catch (InterruptedException e1) {
                Log.w(TAG, "** lost data -> (time: " + newEvent.toString() + ")");
                e1.printStackTrace();
            }
        } else if (e.sensor == gyroscope) {
            newEvent = new SensorReading(e.timestamp, "G", e.values[0], e.values[1], e.values[2]);
            try {
                gyroQueue.put(newEvent);
            } catch (InterruptedException e1) {
                Log.w(TAG, "** lost data -> (time: " + newEvent.toString() + ")");
                e1.printStackTrace();
            }
        }



    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }
}
