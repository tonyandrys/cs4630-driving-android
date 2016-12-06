package edu.uiowa.tsz.drivingapp;


import android.location.Location;

/**
 * Processes incoming location fixes by throwing out the horrible ones and keeping the best ones.
 */
public class LocationProcessor {
    public static final String TAG = "LocationProcessor";

    private Location currentBestLocation;

    public LocationProcessor() {
        this.currentBestLocation = null;
    }

    public Location getBestLocation() {
        return this.currentBestLocation;
    }

    // process a new location, return it if it's the best OR return the cached location if it's worse.
    public Location feed(Location l) {
        if (isNewLocationBetterThanCurrentLocation(l)) {
            return l;
        } else {
            return this.currentBestLocation;
        }
    }

    /** Checks whether two providers are the same */
    private boolean isSameProvider(String provider1, String provider2) {
        if (provider1 == null) {
            return provider2 == null;
        }
        return provider1.equals(provider2);
    }

    /**
     * Checks if a new location sucks less than the best location we have so far.
     * @param newLocation
     * @return true if it is better, false if it is worse.
     */
    private boolean isNewLocationBetterThanCurrentLocation(Location newLocation) {
        // *some* location is better than no location at all.
        if (this.currentBestLocation == null) {
            return true;
        }

        // check time of both locations
        long timeDelta = newLocation.getTime() - currentBestLocation.getTime();
        boolean isNewer = timeDelta > 0;

        // check accuracy of both locations
        int accuracyDelta = (int)(newLocation.getAccuracy() - currentBestLocation.getAccuracy());
        boolean isLessAccurate = accuracyDelta > 0;
        boolean isMoreAccurate = accuracyDelta < 0;
        boolean isSignificantlyLessAccurate = accuracyDelta > 200;

        // are the providers the same?
        boolean isFromSameProvider = isSameProvider(newLocation.getProvider(), currentBestLocation.getProvider());

        // Determine location quality using a combination of timeliness and accuracy
        if (isMoreAccurate) {
            return true;
        } else if (isNewer && !isLessAccurate) {
            return true;
        } else if (isNewer && !isSignificantlyLessAccurate && isFromSameProvider) {
            return true;
        }
        return false;
    }

}
