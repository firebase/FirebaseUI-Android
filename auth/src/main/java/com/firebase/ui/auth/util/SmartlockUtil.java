package com.firebase.ui.auth.util;

import android.app.Activity;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;

import com.firebase.ui.auth.provider.IdpResponse;
import com.firebase.ui.auth.ui.ExtraConstants;
import com.firebase.ui.auth.ui.FlowParameters;
import com.firebase.ui.auth.ui.account_link.SaveCredentialsActivity;
import com.google.android.gms.auth.api.credentials.Credential;
import com.google.android.gms.auth.api.credentials.IdentityProviders;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FacebookAuthProvider;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.auth.TwitterAuthProvider;
import com.google.firebase.auth.UserInfo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Helper class to deal with Smartlock Flows.
 */
public class SmartlockUtil {

    private static final String TAG = "SmartLockUtil";

    /**
     * If SmartLock is enabled and Google Play Services is available, start the save credential
     * Activity. Otherwise, finish the calling Activity with RESULT_OK.
     * @param activity the calling Activity.
     * @param requestCode request code to use when starting the save operation.
     * @param parameters calling Activity flow parameters.
     * @param firebaseUser Firebase user to save in Credential.
     * @param password (optional) password for email credential.
     * @param idpResponse (optional) response from signing in with a credential.
     */
    public static void saveCredentialOrFinish(Activity activity,
                                              int requestCode,
                                              FlowParameters parameters,
                                              FirebaseUser firebaseUser,
                                              @Nullable String password,
                                              @Nullable IdpResponse idpResponse) {

        // If SmartLock is disabled, finish the Activity
        if (!parameters.smartLockEnabled) {
            finishActivity(activity, idpResponse);
            return;
        }

        // If Play Services is not available, finish the Activity
        if(!PlayServicesHelper.getInstance(activity).isPlayServicesAvailable()) {
            finishActivity(activity, idpResponse);
            return;
        }

        // Launch save activity
        Intent saveCredentialIntent = SaveCredentialsActivity.createIntent(activity, parameters,
                firebaseUser, password, idpResponse);
        activity.startActivityForResult(saveCredentialIntent, requestCode);
    }

    /**
     * Translate a Firebase Auth provider ID (such as {@link GoogleAuthProvider#PROVIDER_ID}) to
     * a Credentials API account type (such as {@link IdentityProviders#GOOGLE}).
     */
    public static String providerIdToAccountType(@NonNull String providerId) {
        switch (providerId) {
            case GoogleAuthProvider.PROVIDER_ID:
                return IdentityProviders.GOOGLE;
            case FacebookAuthProvider.PROVIDER_ID:
                return IdentityProviders.FACEBOOK;
            case TwitterAuthProvider.PROVIDER_ID:
                return IdentityProviders.TWITTER;
            case EmailAuthProvider.PROVIDER_ID:
                // The account type for email/password creds is null
                return null;
        }

        return null;
    }

    /**
     * Make a list of {@link Credential} from a FirebaseUser. Useful for deleting Credentials,
     * not for saving since we don't have access to the password.
     */
    public static List<Credential> credentialsFromFirebaseUser(@NonNull FirebaseUser user) {
        if (TextUtils.isEmpty(user.getEmail())) {
            Log.w(TAG, "Can't get credentials from user with no email: " + user);
            return Collections.emptyList();
        }

        List<Credential> credentials = new ArrayList<>();
        for (UserInfo userInfo : user.getProviderData()) {
            // Get provider ID from Firebase Auth
            String providerId = userInfo.getProviderId();

            // Convert to Credentials API account type
            String accountType = providerIdToAccountType(providerId);

            // Build and add credential
            Credential.Builder builder = new Credential.Builder(user.getEmail())
                    .setAccountType(accountType);

            // Null account type means password, we need to add a random password
            // to make deletion succeed.
            if (accountType == null) {
                builder.setPassword("some_password");
            }

            credentials.add(builder.build());
        }

        return credentials;
    }

    private static void finishActivity(Activity activity, IdpResponse idpResponse) {
        activity.setResult(
                Activity.RESULT_OK,
                new Intent().putExtra(ExtraConstants.EXTRA_IDP_RESPONSE, idpResponse));
        activity.finish();
    }

}
