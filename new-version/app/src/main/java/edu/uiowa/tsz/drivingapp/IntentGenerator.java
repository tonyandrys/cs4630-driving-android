package edu.uiowa.tsz.drivingapp;

import android.content.ContentProvider;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static android.support.v4.content.FileProvider.getUriForFile;

/**
 * Generates Intents that are needed in multiple places & contexts.
 */
public class IntentGenerator {
    public static final String TAG = "IntentBuilder";
    public static final String LOG_EMAIL_DESTINATION = "uicssensorlogstorage@gmail.com";                // Group log dump address
    public static final String CONTENT_PROVIDER_AUTHORITY = "edu.uiowa.tsz.fileprovider";               // this is so fucking stupid

    public static Intent buildEmailIntentWithSingleAttachment(Context context, String destEmail, String subjectText, String bodyText, File attachFile) {
        final Intent emailIntent = new Intent(Intent.ACTION_SEND);
        emailIntent.setType("text/plain");
        emailIntent.putExtra(android.content.Intent.EXTRA_EMAIL,
                new String[]{destEmail});
        emailIntent.putExtra(Intent.EXTRA_SUBJECT, subjectText);
        emailIntent.putExtra(Intent.EXTRA_TEXT, bodyText);

        // FileProvider Whitelisting
        Uri u = getUriForFile(context, CONTENT_PROVIDER_AUTHORITY, attachFile);

        /** Blame Google. Not me. **/
        // Give read access to files to be attached to all apps that respond to email intents.
        List<ResolveInfo> resInfoList = context.getPackageManager().queryIntentActivities(emailIntent, PackageManager.MATCH_DEFAULT_ONLY);
        for (ResolveInfo resolveInfo : resInfoList) {
            String packageName = resolveInfo.activityInfo.packageName;
            context.grantUriPermission(packageName, u, Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
        }

        // attachFile is now a blessed uri, can be attached safely.
        emailIntent.putExtra(Intent.EXTRA_STREAM, u);
        return emailIntent;
    }

    /**
     * ContentProvider permission is enabled here. It will NOT work in the method above until I re-write it.
     * @param context
     * @param destEmail
     * @param subjectText
     * @param bodyText
     * @param attachPaths
     * @return
     */
    public static Intent buildEmailIntentWithMultipleAttachments(Context context, String destEmail, String subjectText, String bodyText, List<String> attachPaths) {
        // need to "send multiple" to get more than one attachment
        final Intent emailIntent = new Intent(Intent.ACTION_SEND_MULTIPLE);
        emailIntent.setType("text/plain");
        emailIntent.putExtra(android.content.Intent.EXTRA_EMAIL,
                new String[]{destEmail});
        emailIntent.putExtra(Intent.EXTRA_SUBJECT, subjectText);
        emailIntent.putExtra(Intent.EXTRA_TEXT, bodyText);

        // has to be an ArrayList
        ArrayList<Uri> uris = new ArrayList<Uri>();

        // convert from paths to Android friendly Parcelable Uris
        for (String file : attachPaths) {
            File fileIn = new File(file);
            Uri u = getUriForFile(context, CONTENT_PROVIDER_AUTHORITY, fileIn); // ContentProvider whitelisting

            /** Blame Google. Not me. **/
            // Give read access to files to be attached to all apps that respond to email intents.
            List<ResolveInfo> resInfoList = context.getPackageManager().queryIntentActivities(emailIntent, PackageManager.MATCH_DEFAULT_ONLY);
            for (ResolveInfo resolveInfo : resInfoList) {
                String packageName = resolveInfo.activityInfo.packageName;
                context.grantUriPermission(packageName, u, Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
            }
            uris.add(u);
        }

        // Finally we're done.
        emailIntent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris);
        return emailIntent;
    }

}
