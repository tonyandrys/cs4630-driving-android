package edu.uiowa.tsz.drivingapp;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.ListView;
import android.widget.ProgressBar;

import com.dropbox.client2.ProgressListener;
import com.dropbox.client2.ProgressListener.Adjusted;

import java.io.File;
import java.util.ArrayList;
import java.util.Locale;

/**
 * RecordListActivity
 * Functionally, all this activity does is provide the root container for the ActionBar and the
 * fragment (RecordListFragment) that handles most of the logic.
 *
 * Actually, now it handles logfile compression and shoots big zipfiles to Dropbox. So now it does quite a bit.
 *
 */
public class RecordListActivity extends AppCompatActivity implements RecordListFragment.OnRecordSelectedListener, ZipUtility.FileZipListener, DropboxManager.DropboxUploadListener {

    public static final String TAG = "RecordListActivity";

    private Toolbar toolbar;
    private ActionBar actionBar;
    private ProgressBar progressBar;
    private ProgressDialog progressDialog;
    private ProgressDialog progressDialog2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_record_list);

        // ActionBar setup
        this.toolbar = (Toolbar)findViewById(R.id.action_bar);
        setSupportActionBar(toolbar);
        this.actionBar = getSupportActionBar();
        //this.progressBar = (ProgressBar)findViewById(R.id.record_list_progressbar);
        this.progressDialog = new ProgressDialog(this, ProgressDialog.STYLE_HORIZONTAL);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.record_list_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.export_all_menu_button:
                startArchivingProcess();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }


    private void startArchivingProcess() {

        // compile all of the log files to be compressed
        File[] files = FileSystemManager.getAllLogFiles(this);

        // construct and show progressDialog
        this.progressDialog = new ProgressDialog(this);
        this.progressDialog.setMax(files.length);
        this.progressDialog.setProgress(0);
        this.progressDialog.setCancelable(false);
        this.progressDialog.setIndeterminate(false);
        this.progressDialog.setTitle("Compressing Logs");
        this.progressDialog.setMessage("Please hang the fuck on.");
        this.progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        this.progressDialog.show();

        ArrayList<String> pathList = new ArrayList<String>(files.length);
        for (int i = 0; i < files.length; i++) {
            pathList.add(files[i].getAbsolutePath());
        }

        // start zipping
        String zipName = "archive" + Integer.toString((int) Math.floor(Math.random() * 999999)) + ".zip";       // give it a random name. This is kind of unsafe in the sense that 0.00000001% of the time there could be a name collision.
        File archive = FileSystemManager.createNewArchiveFile(this, zipName);
        FileSystemManager.zipFilesToFile(this, pathList, archive);
    }

    /**
     * When a list item is touched by the user, we transition to the RecordDetailActivity and pass
     * it the Record model associated with the row.
     * @param record
     */
    @Override
    public void onRecordSelected(Record record) {
            Intent i = new Intent(this, RecordDetailActivity.class);
            i.putExtra(Record.INTENT_EXTRA_RECORD, record);
            Log.v(TAG, String.format(Locale.US, "Displaying detail activity for record %s", record.getRecordName()));
            startActivity(i);
    }

    /**
     * Fired after a new file has been written to the archive.
     * @param fileNumber The number of the current file
     * @param totalNumber Total number of files to be compressed
     */
    @Override
    public void onFileZipped(int fileNumber, int totalNumber) {
        String update = String.format(Locale.US, "Compressed file %d/%d.", fileNumber, totalNumber);
        this.progressDialog.setProgress(fileNumber);
        this.progressDialog.setMessage(update);
        Log.v(TAG, update);
    }

    /**
     * Fired when compression has finished.
     * @param zipArchive File pointer to finalized archive file
     * @param totalNumber Total number of files added the archive.
     */
    @Override
    public void onArchiveFinished(File zipArchive, int totalNumber) {
        Log.v(TAG, "onArchiveFinishedListener() called!");

        // EXPERIMENT! This will probably fail.
        this.progressDialog.dismiss();

        // Start uploading zipArchive to Dropbox!
        this.progressDialog2 = new ProgressDialog(this);
        this.progressDialog2.setMax(100);
        this.progressDialog2.setProgress(0);
        this.progressDialog2.setCancelable(false);
        this.progressDialog2.setIndeterminate(false);
        this.progressDialog2.setTitle("Uploading Archive");
        this.progressDialog2.setMessage(String.format(Locale.US, "Sending %s to Dropbox...", zipArchive.getName()));
        this.progressDialog2.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        this.progressDialog2.show();
        DropboxManager dm = new DropboxManager(this);
        dm.uploadArchiveFile(this, zipArchive);
    }

    /**
     * Fired when archive (zipfile) upload to Dropbox sends progress updates for the UI.
     * @param bytes bytes uploaded so far
     * @param total total bytes to upload
     */
    @Override
    public void onProgressChanged(long bytes, long total) {
        // Calculate percent uploaded and write it to the progress dialog.
        int percentComplete = (int) (100.0 * bytes/total);
        Log.v(TAG, String.format(Locale.US, "onProgressChanged() -> %d/%d = %d%% percent.", bytes, total, percentComplete));
        this.progressDialog2.setProgress(percentComplete);
    }

    @Override
    public void onUploadFinished(String linkToFile) {
        this.progressDialog2.dismiss();

        // a stupid popup to show that everything is OK
        AlertDialog alertDialog = new AlertDialog.Builder(RecordListActivity.this).create();
        alertDialog.setTitle("Upload Finished!");
        alertDialog.setMessage("Archive is at URL: " + linkToFile);
        alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
        alertDialog.show();
    }
}

