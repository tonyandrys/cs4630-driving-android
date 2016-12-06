package edu.uiowa.tsz.drivingapp;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * Zips things up.
 */
public class ZipUtility {

    private static final int BUFFER_SIZE = 2048;
    public static final String TAG = "ZipUtility";

    /**
     * Caller should implement this to get progress & finished updates for updating the UI.
     */
    public interface FileZipListener {
        public void onFileZipped(int fileNumber, int totalNumber);
        public void onArchiveFinished(File zipArchive, int totalNumber);
    }

    /**
     * Compresses all file paths in 'files[]' into an archive and writes it to zipFileDest.
     * Don't run this on the main thread unless you're zipping less than two really small files.
     * @param
     * @param files
     * @param zipFileDest
     * @param shouldCallListenerOnMainThread - true to explicitly call FileZipListener methods on the main thread (via context.getMainLooper()).
     *                                       - false if this method is already running on the main thread OR FileZipListener's callback methods don't touch the main thread.
     * @throws IOException
     */
    public static void zip(final Context context, final FileZipListener zipListener, final String[] files, final String zipFileDest, boolean shouldCallListenerOnMainThread) throws IOException {
        Log.v(TAG, String.format(Locale.US, "Zipping %d files to archive '%s'", files.length, zipFileDest));
        BufferedInputStream origin = null;
        ZipOutputStream out = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(zipFileDest)));
        Handler mainHandler = new Handler(context.getMainLooper());

        try {
            byte data[] = new byte[BUFFER_SIZE];

            // Add paths in 'files' one by one
            for (int i = 0; i < files.length; i++) {
                FileInputStream fi = new FileInputStream(files[i]);
                origin = new BufferedInputStream(fi, BUFFER_SIZE);
                ZipEntry entry;
                try {
                    entry = new ZipEntry(files[i].substring(files[i].lastIndexOf("/") + 1));
                    out.putNextEntry(entry);
                    int count;
                    while ((count = origin.read(data, 0, BUFFER_SIZE)) != -1) {
                        out.write(data, 0, count);
                    }
                }
                finally {
                    // Next file completed, send response to listener.
                    final int c = i+1;
                    if (shouldCallListenerOnMainThread) {
                        mainHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                zipListener.onFileZipped(c, files.length); // file number 'i' of 'files.length' has been compressed successfully
                            }
                        });
                    } else {
                        zipListener.onFileZipped(i+1, files.length); // file number 'i' of 'files.length' has been compressed successfully
                    }
                    origin.close();
                }
            }
        }
        finally {
            // archiving is finished.
            out.close();
            if (shouldCallListenerOnMainThread) {
                mainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        zipListener.onArchiveFinished(new File(zipFileDest), files.length);
                    }
                });
            } else {
                zipListener.onArchiveFinished(new File(zipFileDest), files.length);
            }
        }
    }

}
