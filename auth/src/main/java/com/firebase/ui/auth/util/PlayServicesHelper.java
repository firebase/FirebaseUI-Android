package com.firebase.ui.auth.util;

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;

import com.google.android.gms.common.GoogleApiAvailability;

public class PlayServicesHelper {
    private static GoogleApiAvailability mApiAvailability;

    private static GoogleApiAvailability getGoogleApiAvailability() {
        if (mApiAvailability == null) {
            mApiAvailability = GoogleApiAvailability.getInstance();
        }
        return mApiAvailability;
    }

    /**
     * @return true if play services is available, false otherwise.
     */
    public static boolean makePlayServicesAvailable(Activity activity,
                                                   int requestCode,
                                                   DialogInterface.OnCancelListener cancelListener) {
        Dialog errorDialog = getGoogleApiAvailability().getErrorDialog(
                activity,
                getGoogleApiAvailability().isGooglePlayServicesAvailable(activity),
                requestCode,
                cancelListener);

        // The error dialog will be null if isGooglePlayServicesAvailable returned SUCCESS
        if (errorDialog == null) {
            return true;
        } else {
            errorDialog.show();
            return false;
        }
    }
}
