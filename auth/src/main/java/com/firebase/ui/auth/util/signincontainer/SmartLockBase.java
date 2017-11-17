package com.firebase.ui.auth.util.signincontainer;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;
import android.util.Pair;
import android.widget.Toast;

import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.ui.FragmentBase;
import com.firebase.ui.auth.util.data.ProviderUtils;
import com.google.android.gms.auth.api.credentials.Credential;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Result;
import com.google.android.gms.common.api.ResultCallback;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserInfo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract class SmartLockBase<R extends Result> extends FragmentBase implements
        GoogleApiClient.ConnectionCallbacks,
        ResultCallback<R>,
        GoogleApiClient.OnConnectionFailedListener {
    private static final String TAG = "SmartLockBase";

    protected GoogleApiClient mGoogleApiClient;

    private boolean mWasProgressDialogShowing;
    private Pair<Integer, Intent> mActivityResultPair;

    /**
     * Make a list of {@link Credential} from a FirebaseUser. Useful for deleting Credentials, not
     * for saving since we don't have access to the password.
     */
    public static List<Credential> credentialsFromFirebaseUser(@NonNull FirebaseUser user) {
        if (TextUtils.isEmpty(user.getEmail())) {
            Log.w(TAG, "Can't get credentials from user with no email: " + user);
            return Collections.emptyList();
        }

        List<Credential> credentials = new ArrayList<>();
        for (UserInfo userInfo : user.getProviderData()) {
            // Get provider ID from Firebase Auth
            @AuthUI.SupportedProvider String providerId = userInfo.getProviderId();

            // Convert to Credentials API account type
            String accountType = ProviderUtils.providerIdToAccountType(providerId);

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

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    @Override
    public void onStart() {
        super.onStart();
        if (mActivityResultPair != null) {
            finish(mActivityResultPair.first, mActivityResultPair.second);
        } else if (mWasProgressDialogShowing) {
            getDialogHolder().showLoadingDialog(com.firebase.ui.auth.R.string.fui_progress_dialog_loading);
            mWasProgressDialogShowing = false;
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        mWasProgressDialogShowing = getDialogHolder().isProgressDialogShowing();
        getDialogHolder().dismissDialog();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mGoogleApiClient != null) {
            mGoogleApiClient.disconnect();
        }
    }

    @Override
    public void finish(int resultCode, Intent resultIntent) {
        if (getActivity() == null) {
            // Because this fragment lives beyond the activity lifecycle, Fragment#getActivity()
            // might return null and we'll throw a NPE. To get around this, we wait until the
            // activity comes back to life in onStart and we finish it there.
            mActivityResultPair = new Pair<>(resultCode, resultIntent);
        } else {
            super.finish(resultCode, resultIntent);
        }
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Toast.makeText(getContext(),
                com.firebase.ui.auth.R.string.fui_general_error,
                Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onConnectionSuspended(int i) {
        // Just wait
    }
}
