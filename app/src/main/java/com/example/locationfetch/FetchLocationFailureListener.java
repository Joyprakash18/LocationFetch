package com.example.locationfetch;

/**
 * Location fetch listener for getting location callbacks
 */
public interface FetchLocationFailureListener {
    /**
     * this method will be called after a failed fetch of location
     * @param errorMessage the reason of failure
     */
    void onLocationFetchFailed(String errorMessage);
}
