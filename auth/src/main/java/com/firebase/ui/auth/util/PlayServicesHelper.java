package com.firebase.ui.auth.util;

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;

import com.google.android.gms.common.GoogleApiAvailability;

/**
 * Helper class wrapping {@link GoogleApiAvailability}. Used internally but can also be used by
 * client application
 */
public class PlayServicesHelper {
    @Deprecated
    public static GoogleApiAvailability getGoogleApiAvailability() {
        return GoogleApiAvailability.getInstance();
    }

    /**
     * @param activity       The Activity that will host necessary dialogs.
     * @param requestCode    A request code to be used to return results to the Activity.
     * @param cancelListener A Dialog listener if the user cancels the recommended action.
     * @return true if play services is available, false otherwise.
     */
    public static boolean makePlayServicesAvailable(Activity activity,
                                                    int requestCode,
                                                    DialogInterface.OnCancelListener cancelListener) {
        Dialog errorDialog = GoogleApiAvailability.getInstance().getErrorDialog(
                activity,
                GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(activity),
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
