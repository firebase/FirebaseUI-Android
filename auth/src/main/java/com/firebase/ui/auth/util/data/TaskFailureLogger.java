package com.firebase.ui.auth.util.data;

import android.util.Log;

import com.google.android.gms.tasks.OnFailureListener;

import androidx.annotation.NonNull;

public class TaskFailureLogger implements OnFailureListener {
    private String mTag;
    private String mMessage;

    public TaskFailureLogger(@NonNull String tag, @NonNull String message) {
        mTag = tag;
        mMessage = message;
    }

    @Override
    public void onFailure(@NonNull Exception e) {
        Log.w(mTag, mMessage, e);
    }
}
