package edu.uiowa.tsz.drivingapp;

import android.content.Intent;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import java.io.File;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Locale;

/**
 * Container activity for the fragment that renders Record details (RecordDetailFragment).
 */
public class RecordDetailActivity extends AppCompatActivity {

    public static final String TAG = "RecordDetailActivity";
    private RecordDetailFragment recordDetailFragment;
    private Toolbar toolbar;
    private ActionBar actionBar;

    // Record being viewed in this activity
    private Record record;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_record_detail);

        // ActionBar setup
        this.toolbar = (Toolbar)findViewById(R.id.action_bar);
        setSupportActionBar(toolbar);
        this.actionBar = getSupportActionBar();

        // Fetch item to display from the bundle
        record = (Record)getIntent().getSerializableExtra(Record.INTENT_EXTRA_RECORD);
        if (savedInstanceState == null) {
            recordDetailFragment = RecordDetailFragment.newInstance(record);
            FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
            ft.replace(R.id.activity_record_detail_container, recordDetailFragment);
            ft.commit();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.record_detail_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.email_menu_button:
                // get paths to all files associated with this record and load them into the email intent
                String[] filenames = record.getFileMap().values().toArray(new String[0]);
                ArrayList<String> pathList = new ArrayList<String>(filenames.length);
                for (int i=0;i<filenames.length;i++) {
                    File f = FileSystemManager.retrieveLogFile(this, filenames[i]);
                    try {
                        pathList.add(f.getAbsolutePath());
                    } catch (NullPointerException e) {
                        e.printStackTrace();
                    }
                }
                Intent i = IntentGenerator.buildEmailIntentWithMultipleAttachments(this, IntentGenerator.LOG_EMAIL_DESTINATION, record.getRecordName(), record.toString(), pathList);
                this.startActivity(Intent.createChooser(i, "Email log files..."));

                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

}
