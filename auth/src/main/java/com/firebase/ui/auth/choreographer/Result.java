package com.firebase.ui.auth.choreographer;

import android.content.Intent;

public class Result {

    private final int mId;
    private final int mResultCode;
    private final Intent mData;

    public Result(int id, int resultCode, Intent data) {
        mId = id;
        mResultCode = resultCode;
        mData = data;
    }

    public int getId() {
        return mId;
    }

    public int getResultCode() {
        return mResultCode;
    }

    public Intent getData() {
        return mData;
    }

}
