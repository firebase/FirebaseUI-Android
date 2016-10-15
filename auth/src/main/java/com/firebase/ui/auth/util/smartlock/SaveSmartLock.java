package com.firebase.ui.auth.util.smartlock;

import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentSender;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;

import com.firebase.ui.auth.BuildConfig;
import com.firebase.ui.auth.ui.ActivityHelper;
import com.firebase.ui.auth.ui.AppCompatBase;
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.credentials.Credential;
import com.google.android.gms.auth.api.credentials.IdentityProviders;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Status;
import com.google.firebase.auth.FacebookAuthProvider;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;

import static android.app.Activity.RESULT_OK;

public class SaveSmartLock extends SmartLock {
    @Override
    public void onConnected(Bundle bundle) {
        if (mEmail == null) {
            Log.e(TAG, "Unable to save null credential!");
            finishOk();
            return;
        }

        Credential.Builder builder = new Credential.Builder(mEmail);
        builder.setPassword(mPassword);
        if (mPassword == null) {
            // only password OR provider can be set, not both
            if (mProvider != null) {
                String translatedProvider = null;
                // translate the google.com/facebook.com provider strings into full URIs
                if (mProvider.equals(GoogleAuthProvider.PROVIDER_ID)) {
                    translatedProvider = IdentityProviders.GOOGLE;
                } else if (mProvider.equals(FacebookAuthProvider.PROVIDER_ID)) {
                    translatedProvider = IdentityProviders.FACEBOOK;
                }
                if (translatedProvider != null) {
                    builder.setAccountType(translatedProvider);
                }
            }
        }

        if (mName != null) {
            builder.setName(mName);
        }

        if (mProfilePictureUri != null) {
            builder.setProfilePictureUri(Uri.parse(mProfilePictureUri));
        }

        mActivityHelper.getCredentialsApi()
                .save(mCredentialsApiClient, builder.build())
                .setResultCallback(this);
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        super.onConnectionFailed(connectionResult);

        PendingIntent resolution =
                GoogleApiAvailability
                        .getInstance()
                        .getErrorResolutionPendingIntent(mActivity,
                                                         connectionResult.getErrorCode(),
                                                         RC_UPDATE_SERVICE);
        try {
            startIntentSenderForResult(resolution.getIntentSender(),
                                       RC_UPDATE_SERVICE,
                                       null,
                                       0,
                                       0,
                                       0,
                                       null);
        } catch (IntentSender.SendIntentException e) {
            e.printStackTrace();
            finishOk();
        }
    }

    @Override
    public void onResult(@NonNull Status status) {
        if (status.isSuccess()) {
            finishOk();
        } else {
            if (status.hasResolution()) {
                // Try to resolve the save request. This will prompt the user if
                // the credential is new.
                try {
                    status.startResolutionForResult(mActivity, RC_SAVE);
                } catch (IntentSender.SendIntentException e) {
                    // Could not resolve the request
                    Log.e(TAG, "STATUS: Failed to send resolution.", e);
                    finishOk();
                }
            } else {
                finishOk();
            }
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SAVE) {
            if (resultCode == RESULT_OK) {
                if (BuildConfig.DEBUG) {
                    Log.d(TAG, "SAVE: OK");
                }
                finishOk();
            } else {
                Log.e(TAG, "SAVE: Canceled by user");
                finishOk();
            }
        } else if (requestCode == RC_UPDATE_SERVICE) {
            if (resultCode == RESULT_OK) {
                Credential credential = new Credential.Builder(mEmail).setPassword(mPassword)
                        .build();
                mActivityHelper.getCredentialsApi()
                        .save(mCredentialsApiClient, credential)
                        .setResultCallback(this);
            } else {
                Log.e(TAG, "SAVE: Canceled by user");
                finishOk();
            }
        }
    }

    /**
     * If SmartLock is enabled and Google Play Services is available, save the credentials.
     * Otherwise, finish the calling Activity with RESULT_OK.
     *
     * @param activity     the calling Activity.
     * @param helper       activity helper.
     * @param firebaseUser Firebase user to save in Credential.
     * @param password     (optional) password for email credential.
     * @param provider     (optional) provider string for provider credential.
     */
    public void saveCredentialsOrFinish(AppCompatBase activity,
                                        ActivityHelper helper,
                                        FirebaseUser firebaseUser,
                                        @Nullable String password,
                                        @Nullable String provider) {
        if (!initializeAndContinue(activity,
                                   helper,
                                   firebaseUser,
                                   password,
                                   provider)) {
            return;
        }

        mCredentialsApiClient = new GoogleApiClient.Builder(activity)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(Auth.CREDENTIALS_API)
                .enableAutoManage(activity, this)
                .build();
    }

    public static SaveSmartLock getInstance(AppCompatBase activity) {
        SaveSmartLock result;

        FragmentManager fm = activity.getSupportFragmentManager();
        FragmentTransaction ft = fm.beginTransaction();

        Fragment fragment = fm.findFragmentByTag(TAG);
        if (fragment == null || !(fragment instanceof SaveSmartLock)) {
            result = new SaveSmartLock();
            ft.add(result, TAG).disallowAddToBackStack().commit();
        } else {
            result = (SaveSmartLock) fragment;
        }

        return result;
    }
}
