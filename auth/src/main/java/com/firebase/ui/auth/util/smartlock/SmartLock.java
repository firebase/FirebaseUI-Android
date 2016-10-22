package com.firebase.ui.auth.util.smartlock;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.firebase.ui.auth.BuildConfig;
import com.google.android.gms.auth.api.credentials.Credential;
import com.google.android.gms.auth.api.credentials.IdentityProviders;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Result;
import com.google.android.gms.common.api.ResultCallback;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FacebookAuthProvider;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.auth.TwitterAuthProvider;
import com.google.firebase.auth.UserInfo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract class SmartLock<R extends Result> extends Fragment implements
        GoogleApiClient.ConnectionCallbacks,
        ResultCallback<R>,
        GoogleApiClient.OnConnectionFailedListener {
    private static final String TAG = "SmartLockBase";

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true); // TODO: 10/16/2016 test low mem devices
    }

    @Override
    public void onConnectionSuspended(int i) {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "Connection suspended with code " + i);
        }
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "connection failed with " + connectionResult.getErrorMessage()
                    + " and code: " + connectionResult.getErrorCode());
        }
        Toast.makeText(getContext(), "An error has occurred.", Toast.LENGTH_SHORT).show();
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
}
