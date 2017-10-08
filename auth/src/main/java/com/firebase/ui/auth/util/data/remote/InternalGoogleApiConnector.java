package com.firebase.ui.auth.util.data.remote;

import android.app.Activity;
import android.arch.lifecycle.Observer;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Pair;

import com.firebase.ui.auth.util.ui.ActivityResult;
import com.firebase.ui.auth.util.ui.FlowHolder;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;

public abstract class InternalGoogleApiConnector extends GoogleApiConnector
        implements Observer<ActivityResult>, GoogleApiClient.OnConnectionFailedListener {
    private static final int RC_CONNECTION = 28;

    private final FlowHolder mFlowHolder;

    protected InternalGoogleApiConnector(GoogleApiClient.Builder builder, FlowHolder holder) {
        super(builder);
        mFlowHolder = holder;

        mFlowHolder.getOnActivityResult().observeForever(this);
    }

    @Override
    protected void connect() {
        super.connect();
        mFlowHolder.getOnActivityResult().observeForever(this);
    }

    @Override
    protected void disconnect() {
        super.disconnect();
        mFlowHolder.getOnActivityResult().removeObserver(this);
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult result) {
        if (result.hasResolution()) {
            mFlowHolder.getPendingIntentStarter()
                    .setValue(Pair.create(result.getResolution(), RC_CONNECTION));
        }
    }

    @Override
    public void onChanged(@Nullable ActivityResult result) {
        if (result.getRequestCode() == RC_CONNECTION) {
            if (result.getResultCode() == Activity.RESULT_OK) {
                mClient.connect();
            } else {
                disconnect();
                onConnectionFailedIrreparably();
            }
        }
    }

    protected abstract void onConnectionFailedIrreparably();
}