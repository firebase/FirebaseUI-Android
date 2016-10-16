package com.firebase.ui.auth.util.smartlock;

import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.widget.Toast;

import com.firebase.ui.auth.BuildConfig;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Result;
import com.google.android.gms.common.api.ResultCallback;

public abstract class SmartLock<R extends Result> extends Fragment implements
        GoogleApiClient.ConnectionCallbacks,
        ResultCallback<R>,
        GoogleApiClient.OnConnectionFailedListener {
    private static final String TAG = "SmartLock";

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
}
