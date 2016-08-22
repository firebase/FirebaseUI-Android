package com.firebase.ui.auth.util;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;

/**
 * Helper class wrapping {@link GoogleApiAvailability}. Used internally but can also be used
 * by client application
 */
public class PlayServicesHelper {

    private static final String TAG = "PlayServicesHelper";

    @VisibleForTesting
    public static GoogleApiAvailability sApiAvailability = GoogleApiAvailability.getInstance();

    private final Context mContext;

    public static PlayServicesHelper getInstance(Context context) {
        return new PlayServicesHelper(context);
    }

    private PlayServicesHelper(Context context) {
        this.mContext = context;
    }

    /**
     * Returns {@code true} if Google Play Services is available and at the correct version,
     * false otherwise.
     */
    public boolean isPlayServicesAvailable() {
        int playServicesAvailable = sApiAvailability.isGooglePlayServicesAvailable(mContext);
        return playServicesAvailable == ConnectionResult.SUCCESS;
    }

    /**
     * Returns {@code true} if Google Play Services is either already available or can be made
     * available with user action, {@code false} otherwise.
     */
    public boolean canMakePlayServicesAvailable() {
        // Check if already available
        if (isPlayServicesAvailable()) {
            return true;
        }

        // Check if error is resolvable
        int availabilityCode = sApiAvailability.isGooglePlayServicesAvailable(mContext);
        boolean isUserResolvable = sApiAvailability.isUserResolvableError(availabilityCode);

        // Although the API considers SERVICE_INVALID to be resolvable, it can cause crashes
        // on devices with no GmsCore installed at all (like emulators) and therefore we will
        // avoid trying to resolve it.
        return (isUserResolvable && !(availabilityCode == ConnectionResult.SERVICE_INVALID));
    }

    /**
     * Kick off the process to make Play Services available if necessary and possible.
     * @param activity the Activity that will host necessary dialogs.
     * @param requestCode a request code to be used to return results to the Activity.
     * @param cancelListener (optional) a Dialog listener if the user cancels the recommended action.
     * @return {@code true} if a resolution is launched or if a resolution was not necessary,
     *         {@code false otherwise}.
     */
    public boolean makePlayServicesAvailable(
            @NonNull Activity activity,
            int requestCode,
            @Nullable Dialog.OnCancelListener cancelListener) {

        // Check if already available
        if (isPlayServicesAvailable()) {
            return true;
        }

        // Check if error is resolvable
        if (!canMakePlayServicesAvailable()) {
            return false;
        }

        // Get an error dialog for the error code
        int errorCode = sApiAvailability.isGooglePlayServicesAvailable(activity);
        Dialog dialog = sApiAvailability.getErrorDialog(activity, errorCode, requestCode,
                cancelListener);

        // Display the dialog, if possible
        if (dialog != null) {
            dialog.show();
            return true;
        } else {
            // The documentation says the dialog should never be null for a non-SUCCESS
            // connection result, but add these warnings just in case.
            Log.w(TAG, "apiAvailability.getErrorDialog() was null!");
            Log.w(TAG, "errorCode=" + errorCode);
            return false;
        }
    }

}
