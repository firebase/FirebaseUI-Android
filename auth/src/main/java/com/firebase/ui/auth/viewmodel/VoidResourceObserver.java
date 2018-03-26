package com.firebase.ui.auth.viewmodel;

import android.support.annotation.NonNull;
import android.support.annotation.RestrictTo;
import android.support.annotation.StringRes;

import com.firebase.ui.auth.ui.HelperActivityBase;

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public abstract class VoidResourceObserver extends ResourceObserver<Void> {
    protected VoidResourceObserver(@NonNull HelperActivityBase activity, @StringRes int message) {
        super(activity, message);
    }

    @Override
    protected final void onSuccess(@NonNull Void aVoid) {
        onSuccess();
    }

    protected abstract void onSuccess();
}
