package edu.uiowa.tsz.drivingapp;

import android.content.Context;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Provides static methods to do things in and around the app's filesystem sandboxes.
 */
public class FileSystemManager {

    public static final String TAG = "FileSystemManager";
    public static final String FILE_NAME_LOG_DIRECTORY = "logs";
    public static final String FILE_NAME_ARCHIVE_DIRECTORY = "archives";

    /**
     * If the app's internal storage space is empty or is structured in an unexpected way, this will
     * return true. If so, call formatInternalStorage().
     * // TODO: this could benefit from some free disk space checks
     * @return
     */
     public static boolean isInternalStorageOK(final Context context) {
         File intStoragePath = context.getFilesDir();
         File[] contents = intStoragePath.listFiles();

         // These directories should exist and their contents be writable in the app's internal storage sandbox.
         boolean logDirectoryExists = false;
         boolean archiveDirectoryExists = false;

         // Linear search through everything the root of internal storage.
         for (int i=0; i<contents.length; i++) {
             File f = contents[i];
             if (f.getName().equals(FILE_NAME_LOG_DIRECTORY)) {
                 logDirectoryExists = f.isDirectory();
             }
             if (f.getName().equals(FILE_NAME_ARCHIVE_DIRECTORY)) {
                 archiveDirectoryExists = f.isDirectory();
             }
         }

         // Log and return status
         if ((logDirectoryExists) && (archiveDirectoryExists)) {
             Log.v(TAG, String.format(Locale.US, "Internal filesystem is OK."));

             // Clean the archive folder out while we're here. Do it off the main thread so it's not a big deal.
             Thread t = new Thread(new Runnable() {
                 @Override
                 public void run() {
                     cleanArchiveFolder(context);
                 }
             });
             t.start();
             return true;

         } else {
             if (!logDirectoryExists) {
                 Log.w(TAG, String.format(Locale.US, "logfile directory '%s' does not exist in internal storage! Call prepareInternalStorage().", FILE_NAME_LOG_DIRECTORY));
             }
             if (!archiveDirectoryExists) {
                 Log.w(TAG, String.format(Locale.US, "archive directory '%s' does not exist in internal storage! Call prepareInternalStorage().", FILE_NAME_ARCHIVE_DIRECTORY));
             }
             return false;
         }
    }

    /**
     * Archive folder gets filled with junk quickly, so run this to wipe it.
     */
    public static void cleanArchiveFolder(Context context) {
        File intStoragePath = context.getFilesDir();
        File archiveDirectory = new File(intStoragePath, FILE_NAME_ARCHIVE_DIRECTORY);
        File[] archiveFiles = archiveDirectory.listFiles();
        int deletedCount = 0;
        int notDeletedCount = 0;
        for (int i=0; i<archiveFiles.length; i++) {
            File f = archiveFiles[i];
            boolean success = f.delete();
            if (success) {
                deletedCount++;
            } else {
                notDeletedCount++;
            }
        }
        Log.v(TAG, String.format(Locale.US, "Cleaned up %d archive files from internal storage.", deletedCount));
        if (notDeletedCount > 0) {
            Log.w(TAG, String.format(Locale.US, "Failed to delete %d archive files from internal storage!", notDeletedCount));
        }
    }

    /**
     * Creates the basic internal storage structure for the app to function.
     * @param context
     */
    public static void prepareInternalStorage(Context context) {
        File intStoragePath = context.getFilesDir();
        File logDirectory = new File(intStoragePath, FILE_NAME_LOG_DIRECTORY);
        File archiveDirectory = new File(intStoragePath, FILE_NAME_ARCHIVE_DIRECTORY);
        File[] directoriesToCreate = {logDirectory, archiveDirectory};
        for (int i=0; i<directoriesToCreate.length; i++) {
            File d = directoriesToCreate[i];
            if (!d.exists()) {
                if (d.mkdir()) {
                    Log.v(TAG, String.format(Locale.US, "Successfully created directory '%s' in internal storage.", d));
                } else {
                    Log.e(TAG, String.format(Locale.US, "Directory '%s' does not exist in internal storage; attempt to create directory failed!", d));
                }
            }
        }
    }

    /**
     * Convenience function to get every logfile stored on the device and dump it to a File[].
     * @param context
     * @return
     */
    public static File[] getAllLogFiles(Context context) {
        File logDir = new File(context.getFilesDir(), FILE_NAME_LOG_DIRECTORY);
        return logDir.listFiles();
    }

    /**
     * Creates a new file in the /archives/ directory of internal storage.
     * Use this to build file descriptors for zip files that you need to create using ZipUtility.
     * IMPORTANT: ".zip" will be concatenated at the end of 'filename' if it is not detected!
     * (Some phones, especially mine, blow the fuck up when trying to send archive files with no extension)
     * @param context
     * @param filename
     * @return File
     */
    public static File createNewArchiveFile(Context context, String filename) {
        String name = filename;
        if (!filename.endsWith(".zip")) {
            name = filename + ".zip";
            Log.w(TAG, String.format(Locale.US, "Filename passed to createNewArchiveFile() '%s' did not end with .zip! Modifying filename to '%s' automagically.", filename, name));
        }
        return new File(context.getFilesDir(), FILE_NAME_ARCHIVE_DIRECTORY + File.separator + name);
    }

    /**
     * Creates and returns an empty File object with name 'filename' in the correct directory for logfiles.
     * @param context
     * @return
     */
    public static File createNewLogFile(Context context, String filename) {
        return new File(context.getFilesDir(), FILE_NAME_LOG_DIRECTORY + File.separator + filename);
    }

    /**
     * Don't use this, its unreliable. Stick with internal storage, even though it's such a pain.
     * @param context
     * @param filename
     * @return
     */
    public static File createNewFileInExternalStorage(Context context, String filename) {
        return new File(context.getExternalCacheDir(), filename);
    }

    /**
     * Appends the contents of a string 'stringToWrite' to the File 'targetFile' without blocking the main thread.
     * Useful for files such as ROAD_HAZARD/ROAD_COMPOSITION which are only written to after a specific user UI interaction.
     * @param context
     * @param stringToWrite
     * @param targetFile
     * @return
     */
 //   public static String appendStringToLogFile(Context context, String stringToWrite, File targetFile)

    /**
     * Retrieves the File object for a file in the internal logs directory with name 'filename'.
     * @return File if 'filename' exists, null if it can't be found (or read).
     */
    public static File retrieveLogFile(Context context, String filename) {
        File intStoragePath = context.getFilesDir();
        File target = new File(intStoragePath, FILE_NAME_LOG_DIRECTORY + File.separator + filename);
        if ((!target.isFile()) || (!target.exists())) {
            // there was a problem. complain.
            Log.e(TAG, String.format("%s could not be read! Does it exist?", target.getPath()));
            return null;
        } else {
            return target;
        }
    }

    /**
     * Compresses (zips) a set of files off the main thread, final archive file is written to 'outputZipPath'.
     * The calling activity/android component *must* implement the ZipUtility.FileZipListener interface to get progress & completion
     * updates and to avoid a ClassCastException.
     *
     * @param context
     * @param filesToZip
     * @param outputZipFile
     */
    public static void zipFilesToFile(final Context context, final List<String> filesToZip, final File outputZipFile) {
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    ZipUtility.zip(context, (ZipUtility.FileZipListener)context, filesToZip.toArray(new String[0]), outputZipFile.getAbsolutePath(), true);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        t.start();

        //return outputZipPath;
    }

}
