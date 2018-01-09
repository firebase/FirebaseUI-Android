package com.firebase.ui.auth.viewmodel;

import android.content.Intent;
import android.support.annotation.RestrictTo;

/**
 * POJO holding information for the Activity to finish with.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class PendingFinish {

    private int mCode;
    private Intent mData;

    public PendingFinish(int code, Intent data) {
        this.mCode = code;
        this.mData = data;
    }

    public int getCode() {
        return mCode;
    }

    public Intent getData() {
        return mData;
    }

}
