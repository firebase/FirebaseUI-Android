package com.firebase.ui.auth.util.signincontainer;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Pair;
import android.widget.Toast;

import com.firebase.ui.auth.ui.FragmentBase;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Result;
import com.google.android.gms.common.api.ResultCallback;

public abstract class SmartLockBase<R extends Result> extends FragmentBase implements
        GoogleApiClient.ConnectionCallbacks,
        ResultCallback<R>,
        GoogleApiClient.OnConnectionFailedListener {
    protected GoogleApiClient mGoogleApiClient;

    private boolean mWasProgressDialogShowing;
    private Pair<Integer, Intent> mActivityResultPair;

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
