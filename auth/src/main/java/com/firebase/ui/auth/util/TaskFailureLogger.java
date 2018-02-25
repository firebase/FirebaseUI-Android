package com.firebase.ui.auth.util;

import android.support.annotation.NonNull;
import android.util.Log;

import com.firebase.ui.auth.AuthUI;
import com.google.android.gms.tasks.OnFailureListener;

public class TaskFailureLogger implements OnFailureListener {
    private String mTag;
    private String mMessage;

    public TaskFailureLogger(@NonNull String tag, @NonNull String message) {
        mTag = tag;
        mMessage = message;
    }

    @Override
    public void onFailure(@NonNull Exception e) {
        try {
            if (Class.forName(AuthUI.getApplicationContext().getPackageName() + ".BuildConfig")
                    .getDeclaredField("DEBUG").getBoolean(null)) {
                Log.w(mTag, mMessage, e);
            }
        } catch (Exception ignored) {}
    }
}
