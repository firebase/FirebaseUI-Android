package com.firebase.ui.auth.util.data.remote;

import com.google.android.gms.common.api.GoogleApiClient;

public abstract class GoogleApiConnector
        implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {
    protected final GoogleApiClient mClient;

    protected GoogleApiConnector(GoogleApiClient.Builder builder) {
        mClient = builder.addConnectionCallbacks(this).addOnConnectionFailedListener(this).build();
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
