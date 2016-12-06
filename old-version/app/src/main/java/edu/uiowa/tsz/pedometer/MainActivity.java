package edu.uiowa.tsz.pedometer;

import android.app.DialogFragment;

import android.support.v4.app.FragmentManager;

import android.content.Context;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.PowerManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;sa
import java.util.List;
import java.util.Locale;

// implementing dialog interface allows this activity to catch modal button actions
public class MainActivity extends AppCompatActivity implements StartLoggingDialogFragment.StartLoggingDialogListener {
    PowerManager pm;
    PowerManager.WakeLock wl;

    // intent keys
    public static String INTENT_EXTRA_FILENAME = "filename";

    // UI references
    Button startButton;
    Button stopButton;

    // dialog which collects filename and comments
    StartLoggingDialogFragment dialog;

    int counter;                                // used for filename prefix tagging

    // comment and log name of the *last* fired instance of the service.
    String logName = "";
    String logComment = "";

    // email intent parameters
    String EMAIL_TO = "uicssensorlogstorage@gmail.com";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        pm = (PowerManager)getSystemService(Context.POWER_SERVICE);
        startButton = (Button)this.findViewById(R.id.startButton);
        stopButton = (Button)this.findViewById(R.id.stopButton);
        counter = 0;
    }

    public void onStartButtonClicked(View v) {

        // display name & comment modal as a fragment, identified by the class tag.
        FragmentManager fm = getSupportFragmentManager();
        this.dialog = new StartLoggingDialogFragment();
        this.dialog.show(fm, StartLoggingDialogFragment.TAG);

        // start the service
        /*Log.i("HW1", "start the service");

        // get and hold wakelock until the stop button is pressed.
        wl = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, "MainActivity");
        wl.acquire();

        // generate a filename and send it to the service. Will be modified with sensor-specific suffixes.
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd-HH:mm:ss", Locale.US);
        // String filename = Integer.toString(counter) + "@" + dateFormat.format(new Date());

        // Simplifying filename generation because its such a pain to get files off.
        // New format counter-[ACCEL/GYRO]
        String filename = Integer.toString(counter);

        Intent i = new Intent(getApplicationContext(), AccelService.class);
        i.putExtra(INTENT_EXTRA_FILENAME, filename);
        startService(i);

        Button startButton = (Button)this.findViewById(R.id.startButton);
        startButton.setEnabled(false);
        stopButton.setEnabled(true);*/
    }



    public void onStopButtonClicked(View v) {
        // stop the service
        Log.i("HW1", "stop the service");
        Intent i = new Intent(getApplicationContext(), AccelService.class);
        stopService(i);
        wl.release();
        stopButton.setEnabled(false);
        startButton.setEnabled(true);

        // accel & gyro file paths
        SharedPreferences prefs = getSharedPreferences(AccelService.TAG, 0);
        String accelPath = prefs.getString(AccelService.SHAREDPREFS_ACCEL_SAVED_FILE_PATH, "");
        String gyroPath = prefs.getString(AccelService.SHAREDPREFS_GYRO_SAVED_FILE_PATH, "");
        ArrayList<String> paths = new ArrayList<String>(2);
        paths.add(accelPath);
        paths.add(gyroPath);

        // send email intent
        sendEmailIntent(this, paths);
    }

    // Launches email intent to store logs
    protected void sendEmailIntent(Context context, List<String> filePaths) {

            // need to "send multiple" to get more than one attachment
            final Intent emailIntent = new Intent(Intent.ACTION_SEND_MULTIPLE);
            emailIntent.setType("text/plain");
            emailIntent.putExtra(android.content.Intent.EXTRA_EMAIL,
                    new String[]{EMAIL_TO});
            emailIntent.putExtra(Intent.EXTRA_SUBJECT, this.logComment);
            emailIntent.putExtra(Intent.EXTRA_TEXT, this.logComment);

            // has to be an ArrayList
            ArrayList<Uri> uris = new ArrayList<Uri>();

            // convert from paths to Android friendly Parcelable Uri's
            for (String file : filePaths) {
                File fileIn = new File(file);
                Uri u = Uri.fromFile(fileIn);
                uris.add(u);
            }
            emailIntent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris);
            context.startActivity(Intent.createChooser(emailIntent, "Send mail..."));

    }

    @Override
    public void onStartLoggingDialogPositiveClick(android.support.v4.app.DialogFragment dialog) {
        Log.v("HW1", "MainActivity caught the dialog response! Whoopie![1]");
        Log.i("HW1", "start the service");

        // query Dialog instance to extract comment and filename
        StartLoggingDialogFragment df = (StartLoggingDialogFragment)dialog;
        logComment = df.getComment();
        logName = df.getName();

        // get and hold wakelock until the stop button is pressed.
        wl = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, "MainActivity");
        wl.acquire();

        // generate a filename and send it to the service. Will be modified with sensor-specific suffixes.
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd-HH:mm:ss", Locale.US);
        Date now = new Date();

        // send filename to service and fire it
        Intent i = new Intent(getApplicationContext(), AccelService.class);
        i.putExtra(INTENT_EXTRA_FILENAME, logName + dateFormat.format(now));
        startService(i);

        // Disable the start button and explicitly re-enable the stop button to prevent any UI mistakes
        Button startButton = (Button)this.findViewById(R.id.startButton);
        startButton.setEnabled(false);
        stopButton.setEnabled(true);
    }

    @Override
    public void onStartLoggingDialogNegativeClick(android.support.v4.app.DialogFragment dialog) {
        Log.v("HW1", "Nothing happens.");
    }
}
