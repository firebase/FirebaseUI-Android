package com.firebase.ui.auth.util.data;

import android.support.annotation.NonNull;
import android.util.Log;

import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.BuildConfig;
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
            Class<?> buildConfigClass = Class.forName(
                    AuthUI.getApplicationContext().getPackageName() + "."
                            + BuildConfig.class.getSimpleName());
            if (buildConfigClass.getDeclaredField("DEBUG").getBoolean(null)) {
                Log.w(mTag, mMessage, e);
            }
        } catch (Exception ignored) {}
    }
}
