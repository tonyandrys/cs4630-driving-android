package edu.uiowa.tsz.drivingapp;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import java.text.SimpleDateFormat;

import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.PowerManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import org.honorato.multistatetogglebutton.MultiStateToggleButton;
import org.honorato.multistatetogglebutton.ToggleButton;
import org.w3c.dom.Text;

import java.util.Date;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {
    public static final String TAG = "MainActivity";

    // Intent keys
    public static String INTENT_EXTRA_FILENAME = "filename";
    public static String INTENT_USE_LOCATION = "useLocation";

    // OS/Power Management
    PowerManager pm;
    PowerManager.WakeLock wl;
    private boolean isLoggingEnabled = false;

    // layout references
    private Toolbar toolbar;
    private RelativeLayout loggingActiveContainer;
    private RelativeLayout loggingInactiveContainer;
    private Button toggleLoggingButton;
    private TextView dataVelocityTextView;
    private ProgressBar dataVelocityProgressBar;
    private TextView cardinalDirectionTextView;
    private ProgressBar cardinalDirectionProgressBar;
    private MultiStateToggleButton roadCompositionToggleButton;

    // action bar (!= Toolbar!) & menu references
    private ActionBar actionBar;
    private MenuItem stopLoggingMenuItem;

    // driving data caches
    private boolean lastBearingWasZero;

    // Broadcast receiver
    private BroadcastReceiver velocityReceiver;

    public MainActivity() {
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // instantiate OS service references
        pm = (PowerManager)getSystemService(Context.POWER_SERVICE);

        // store references to necessary views
        this.loggingActiveContainer = (RelativeLayout)findViewById(R.id.logging_active_rl);
        this.loggingInactiveContainer = (RelativeLayout)findViewById(R.id.logging_inactive_rl);
        this.toggleLoggingButton = (Button)findViewById(R.id.logging_toggle_button);
        this.dataVelocityTextView = (TextView)findViewById(R.id.data_velocity_tv);
        this.cardinalDirectionTextView = (TextView)findViewById(R.id.cardinal_direction_label);
        this.dataVelocityProgressBar = (ProgressBar)findViewById(R.id.data_velocity_progressBar);
        this.cardinalDirectionProgressBar = (ProgressBar)findViewById(R.id.data_cardinal_direction_progressBar);

        // ActionBar setup
        this.toolbar = (Toolbar)findViewById(R.id.action_bar);
        setSupportActionBar(toolbar);
        this.actionBar = getSupportActionBar();

        // show progress bars & initialize driving data caches
        this.dataVelocityProgressBar.setVisibility(ProgressBar.VISIBLE);
        this.cardinalDirectionProgressBar.setVisibility(ProgressBar.VISIBLE);
        this.lastBearingWasZero = false;

        // Create and register a velocity receiver with the velocity intent from the service
        this.velocityReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                // Update velocity UI
                if (intent.hasExtra(LoggingService.INTENT_EXTRA_VELOCITY)) {
                    if (isLoggingEnabled) {
                        float velocity = intent.getFloatExtra(LoggingService.INTENT_EXTRA_VELOCITY, 0);
                        int mph = (int)(Math.round(velocity)*2.2369);
                        String v = String.format(Locale.US, "%02d", mph);
                        dataVelocityTextView.setText(v);
                        // show the mph data in driving pane if it is invisible
                        if (dataVelocityTextView.getVisibility() == TextView.INVISIBLE) {
                            dataVelocityProgressBar.setVisibility(ProgressBar.INVISIBLE);
                            dataVelocityTextView.setVisibility(TextView.VISIBLE);
                        }
                        Log.v("VelocityReceiver", "New velocity detected (" + v + ").");
                    } else {
                        Log.w("VelocityReceiver", "Discarding velocity reading, logging is not enabled! This is probably a bug.");
                    }
                }

                // Update direction UI
                if (intent.hasExtra(LoggingService.INTENT_EXTRA_BEARING)) {
                    double bearing = intent.getDoubleExtra(LoggingService.INTENT_EXTRA_BEARING, -1.0);

                    // TODO: UI fix outlined here
                    // If this bearing is zero, cache it but don't touch the UI until we receive it a second time.
                    // Noisy location data can yield a bearing of zero regardless of the *real* direction of travel.
                    // So, we will only write a bearing of zero if we get it twice in a row.
                    //if ((bearing == 0) && (!lastBearingWasZero)) {
                    //    lastBearingWasZero = true;
                    //}

                    // if we got a bearing from the intent, map the appropriate cardinal direction to the measurement and write it to the UI.
                    if (bearing != 1.0) {
                        String directions[] = {"N", "NE", "E", "SE", "S", "SW", "W", "NW", "N"};
                        String direction =  directions[ (int)Math.round((  ((double)bearing % 360) / 45)) ];
                        cardinalDirectionTextView.setText(direction);
                    }

                    // show the cardinal direction data if it is invisible
                    if (cardinalDirectionTextView.getVisibility() == TextView.INVISIBLE) {
                        cardinalDirectionProgressBar.setVisibility(ProgressBar.INVISIBLE);
                        cardinalDirectionTextView.setVisibility(TextView.VISIBLE);
                    }
                    Log.v("VelocityReceiver", "New direction detected (" + bearing + ").");
                }

            }
        };
        LocalBroadcastManager.getInstance(this).registerReceiver(this.velocityReceiver, new IntentFilter(LoggingService.ACTION_UI_UPDATE));

        // MultiStateToggleButton listener
        // TODO: Wire this up to a BroadcastReceiver in LoggingService that writes these changes to a file. Record class will have to be updated to support this new file association, database as well.
        this.roadCompositionToggleButton = (MultiStateToggleButton)findViewById(R.id.road_composition_toggle_button);
        this.roadCompositionToggleButton.setOnValueChangedListener(new ToggleButton.OnValueChangedListener() {
            @Override
            public void onValueChanged(int value) {
                Log.v(TAG, String.format(Locale.US, "Road composition toggle group touched -> new value is '%d'.", value));
            }
        });

        // Do internal filesystem checks, create initial directories if this is a first launch.
        if (!FileSystemManager.isInternalStorageOK(this)) {
            FileSystemManager.prepareInternalStorage(this);
        }

        // TODO: before refreshLoggingView() is called here, need to recover value of isLoggingEnabled from a savedInstanceState bundle. Currently this is not captured by an onPause/Destroy method, but it should be to avoid bugs.
        refreshLoggingView();
    }

    @Override
    protected void onResume() {
        super.onResume();
        int fineLocationPermissionCheck = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION);
        if (fineLocationPermissionCheck == PackageManager.PERMISSION_GRANTED) {
            this.toggleLoggingButton.setEnabled(true);
        } else {
            this.toggleLoggingButton.setEnabled(false);
        }
        refreshLoggingView();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        if (this.isLoggingEnabled) {
            inflater.inflate(R.menu.logging_active_menu, menu);
            Log.v(TAG, "inflating R.menu.logging_active_menu");
        } else {
            inflater.inflate(R.menu.logging_inactive_menu, menu);
            Log.v(TAG, "inflating R.menu.logging_inactive_menu");
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.stop_logging_menuitem:
                // HACK: need to move logic out of toggle...buttonTouched into a more abstract method (toggleLogging) and point the toggle button in the UI at the more general method to remove the ugly null parameter.
                toggleLoggingButtonTouched(null);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onDestroy() {
        // Unregister velocity listener
        LocalBroadcastManager.getInstance(this).unregisterReceiver(this.velocityReceiver);
        super.onDestroy();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case 1: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Button toggleButton = (Button)findViewById(R.id.logging_toggle_button);
                    toggleButton.setEnabled(true);

                } else {
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                }
                return;
            }
            // other 'case' lines to check for other
            // permissions this app might request
        }
    }

    public void toggleLoggingButtonTouched(View v) {

        // Logging was NOT enabled when button was touched, so it is now enabled. Do everything to setup tracking, show relevant UI components, and start logging.
        if (!isLoggingEnabled) {
            isLoggingEnabled = true;
            startLoggingService();
        }

        // Logging IS enabled, so it should now be disabled.
        else {
            isLoggingEnabled = false;
            stopLoggingService();
        }

        // update UI
        Log.v(TAG, "isLoggingEnabled set to " + isLoggingEnabled + ". Updating UI...");
        refreshLoggingView();
    }

    public void locationPermissionButtonTouched(View v) {
        ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
    }

    public void showRecordsListActivity(View v) {
        Log.v(TAG, "Trying to transition to Record List Activity");
        Intent i = new Intent(getApplicationContext(), RecordListActivity.class);
        startActivity(i);
    }

    public void startLoggingService() {
        Log.v(TAG, "Starting the Logging Service.");
        // Hold wakelock throughout the logging process.
        wl = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, TAG);
        wl.acquire();

        // start logging service. filenames will be tagged with date:
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd-HH:mm:ss", Locale.US);
        Date now = new Date();
        Intent i = new Intent(getApplicationContext(), LoggingService.class);
        i.putExtra(INTENT_EXTRA_FILENAME, dateFormat.format(now));
        startService(i);
    }

    public void stopLoggingService() {
        Log.v(TAG, "Halting the Logging Service.");
        Intent i = new Intent(getApplicationContext(), LoggingService.class);
        stopService(i);
        wl.release();
    }

    /**
     * refreshLoggingView()
     * Walks through the component tree and modifies visibility of RelativeLayout logging containers to display the appropriate sets of UI controls with respect to the logging state (this.isLoggingEnabled).
     * Call this whenever this.isLoggingEnabled is changed to update UI.
     */
    public void refreshLoggingView() {
        if (this.isLoggingEnabled) {
            loggingActiveContainer.setVisibility(RelativeLayout.VISIBLE);
            loggingInactiveContainer.setVisibility(RelativeLayout.GONE);
        } else {
            loggingActiveContainer.setVisibility(RelativeLayout.GONE);
            loggingInactiveContainer.setVisibility(RelativeLayout.VISIBLE);
        }

        this.actionBar.invalidateOptionsMenu();

    }

}
