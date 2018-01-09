package com.firebase.ui.auth.viewmodel;

import android.content.Intent;
import android.support.annotation.RestrictTo;

/**
 * TODO(samstern): Document
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
