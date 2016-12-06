package edu.uiowa.tsz.drivingapp;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.util.Log;

import com.dropbox.client2.DropboxAPI;
import com.dropbox.client2.ProgressListener;
import com.dropbox.client2.android.AndroidAuthSession;
import com.dropbox.client2.exception.DropboxException;
import com.dropbox.client2.session.AppKeyPair;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.Locale;

/**
 * Access to basic Dropbox features wrt uploading files.
 */
public class DropboxManager {
    public static final String TAG = "DropboxManager";
    private DropboxAPI<AndroidAuthSession> mDBApi;

    // Hashmap keys for the values returned by loadAPIKeysFromFile().
    // They're also identical to the string names that are expected in res/values/api_keys.xml
    private static final String STRING_DROPBOX_APP_KEY = "DROPBOX_APP_KEY";
    private static final String STRING_DROPBOX_APP_SECRET = "DROPBOX_APP_SECRET";
    private static final String STRING_DROPBOX_OAUTH_TOKEN = "DROPBOX_OAUTH_TOKEN";

    // The caller should implement this to get upload progress and completion updates.
    public interface DropboxUploadListener {
        public void onProgressChanged(long bytes, long total);
        public void onUploadFinished(String linkToFile);
    }

    public DropboxManager(Context context) {
        HashMap<String, String> keyMap = loadAPIKeysFromFile(context);
        String oauthToken = keyMap.get(STRING_DROPBOX_OAUTH_TOKEN);
        AppKeyPair appKeys = new AppKeyPair(keyMap.get(STRING_DROPBOX_APP_KEY), keyMap.get(STRING_DROPBOX_APP_SECRET));
        AndroidAuthSession session = new AndroidAuthSession(appKeys);
        mDBApi = new DropboxAPI<AndroidAuthSession>(session);
        mDBApi.getSession().setOAuth2AccessToken(oauthToken);
        // mDBApi is now setup to work with uicssensorlogstorage@gmail.com Dropbox account.
    }

    /**
     * Uploads an archive file to the group Dropbox.
     * Method is threaded -- you can call this on the main thread without breaking anything.
     * Calling activity *must* implement DropboxManager.DropboxUploadListener or this will explode.
     * @param context
     * @param fileToUpload
     */
    public void uploadArchiveFile(Context context, final File fileToUpload) {
        final DropboxUploadListener caller = (DropboxUploadListener)context;
        final String pathToArchiveDirectory = "/archives/";
        final String serverUploadPath = pathToArchiveDirectory + fileToUpload.getName();
        final Handler mainHandler = new Handler(context.getMainLooper());

        // Dropbox directly hooks into this listener, it's just a limitation of this shit API that I need to pass the calls from this thing back to the calling activity.
        final ProgressListener progressListener = new ProgressListener() {
            @Override
            public void onProgress(long bytes, long total) {
                final long b = bytes;
                final long t = total;
                mainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        caller.onProgressChanged(b, t);
                    }
                });
            }
        };

        // Do the actual upload for the love of god
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    FileInputStream is = new FileInputStream(fileToUpload);
                    // do upload, returns file metadata
                    DropboxAPI.Entry e = mDBApi.putFile(serverUploadPath, new FileInputStream(fileToUpload), fileToUpload.length(), null, true, progressListener);
                    String pathToUploadedFile = e.path;
                    final DropboxAPI.DropboxLink link = mDBApi.share(pathToUploadedFile);
                    Log.v(TAG, String.format(Locale.US, "Here's the uploaded link: %s", link.url));
                    mainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            caller.onUploadFinished(link.url);
                        }
                    });

                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (DropboxException e) {
                    e.printStackTrace();
                }
            }
        });
        t.start();
    }

    /**
     * Loads Dropbox API keys from res/values/api_keys.xml and returns them as a map.
     * @param context
     * @return
     */
    private HashMap<String,String> loadAPIKeysFromFile(Context context) {
        // should exist in res/values/api_keys.xml
        String appKey = context.getString(R.string.DROPBOX_APP_KEY);
        String appSecret = context.getString(R.string.DROPBOX_APP_SECRET);
        String oauthToken = context.getString(R.string.DROPBOX_OAUTH_TOKEN);

        HashMap<String,String> map = new HashMap<String,String>(3);
        map.put(STRING_DROPBOX_APP_KEY, appKey);
        map.put(STRING_DROPBOX_APP_SECRET, appSecret);
        map.put(STRING_DROPBOX_OAUTH_TOKEN, oauthToken);
        return map;
    }


}
