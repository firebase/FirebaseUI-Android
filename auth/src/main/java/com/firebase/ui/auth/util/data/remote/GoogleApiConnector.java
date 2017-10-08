package com.firebase.ui.auth.util.data.remote;

import android.support.v4.app.FragmentActivity;

import com.google.android.gms.common.api.GoogleApiClient;

import java.util.concurrent.atomic.AtomicInteger;

public abstract class GoogleApiConnector
        implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {
    private static final AtomicInteger SAFE_ID = new AtomicInteger(10);

    protected final GoogleApiClient mClient;

    protected GoogleApiConnector(GoogleApiClient.Builder builder) {
        mClient = builder.addConnectionCallbacks(this).addOnConnectionFailedListener(this).build();
    }

    /**
     * @return a safe id for {@link GoogleApiClient.Builder#enableAutoManage(FragmentActivity, int,
     * GoogleApiClient.OnConnectionFailedListener)}
     */
    public static int getSafeAutoManageId() {
        return SAFE_ID.getAndIncrement();
    }

    protected void connect() {
        mClient.connect();
    }

    protected void disconnect() {
        mClient.disconnect();
    }

    @Override
    public void onConnectionSuspended(int cause) {
        // Just wait
    }
}
