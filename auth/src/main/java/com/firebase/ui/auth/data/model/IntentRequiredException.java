package com.firebase.ui.auth.data.model;

import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.annotation.RestrictTo;

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class IntentRequiredException extends StatefulException {
    private final Intent mIntent;
    private final int mRequestCode;

    public IntentRequiredException(@NonNull Intent intent, int requestCode) {
        mIntent = intent;
        mRequestCode = requestCode;
    }

    @NonNull
    public Intent getIntent() {
        return mIntent;
    }

    public int getRequestCode() {
        return mRequestCode;
    }
}
