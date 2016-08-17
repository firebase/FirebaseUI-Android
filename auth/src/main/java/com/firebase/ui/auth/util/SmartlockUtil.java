package com.firebase.ui.auth.util;

import android.app.Activity;
import android.content.Intent;
import android.support.annotation.Nullable;

import com.firebase.ui.auth.ui.FlowParameters;
import com.firebase.ui.auth.ui.account_link.SaveCredentialsActivity;
import com.google.firebase.auth.FirebaseUser;

/**
 * Helper class to deal with Smartlock Flows.
 */
public class SmartlockUtil {

    /**
     * If SmartLock is enabled and Google Play Services is available, start the save credential
     * Activity. Otherwise, finish the calling Activity with RESULT_OK.
     * @param activity the calling Activity.
     * @param requestCode request code to use when starting the save operation.
     * @param parameters calling Activity flow parameters.
     * @param firebaseUser Firebase user to save in Credential.
     * @param password (optional) password for email credential.
     * @param provider (optional) provider string for provider credential.
     */
    public static void saveCredentialOrFinish(Activity activity,
                                              int requestCode,
                                              FlowParameters parameters,
                                              FirebaseUser firebaseUser,
                                              @Nullable String password,
                                              @Nullable String provider) {

        // If SmartLock is disabled, finish the Activity
        if (!parameters.smartLockEnabled) {
            finishActivity(activity);
            return;
        }

        // If Play Services is not available, finish the Activity
        if(!PlayServicesHelper.getInstance(activity).isPlayServicesAvailable()) {
            finishActivity(activity);
            return;
        }

        // Launch save activity
        Intent saveCredentialIntent = SaveCredentialsActivity.createIntent(activity, parameters,
                firebaseUser, password, provider);
        activity.startActivityForResult(saveCredentialIntent, requestCode);
    }

    private static void finishActivity(Activity activity) {
        activity.setResult(Activity.RESULT_OK, new Intent());
        activity.finish();
    }

}
