package com.firebase.ui.auth.data.model;

import android.content.Intent;
import android.support.annotation.Nullable;
import android.support.annotation.RestrictTo;

/**
 * Immutable object representation of an {@link android.app.Activity#onActivityResult(int, int,
 * Intent) activity result}.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public final class ActivityResult {
    private final int mRequestCode;
    private final int mResultCode;
    @Nullable private final Intent mData;

    public ActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        mRequestCode = requestCode;
        mResultCode = resultCode;
        mData = data;
    }

    public int getRequestCode() {
        return mRequestCode;
    }

    public int getResultCode() {
        return mResultCode;
    }

    @Nullable
    public Intent getData() {
        return mData == null ? null : new Intent(mData);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ActivityResult result = (ActivityResult) o;

        return mRequestCode == result.mRequestCode
                && mResultCode == result.mResultCode
                && (mData == null ? result.mData == null : mData.equals(result.mData));
    }

    @Override
    public int hashCode() {
        int result = mRequestCode;
        result = 31 * result + mResultCode;
        result = 31 * result + (mData == null ? 0 : mData.hashCode());
        return result;
    }

    @Override
    public String toString() {
        return "ActivityResult{" +
                "mRequestCode=" + mRequestCode +
                ", mResultCode=" + mResultCode +
                ", mData=" + mData +
                '}';
    }
}
