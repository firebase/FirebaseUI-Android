package com.firebase.ui.auth.util.data;

import android.support.annotation.NonNull;
import android.util.Log;

import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.BuildConfig;
import com.google.android.gms.tasks.OnFailureListener;

public class TaskFailureLogger implements OnFailureListener {
    private static final boolean LOG;

    private String mTag;
    private String mMessage;

    static {
        boolean log;
        try {
            Class<?> buildConfigClass = Class.forName(
                    AuthUI.getApplicationContext().getPackageName() + "."
                            + BuildConfig.class.getSimpleName());
            log = buildConfigClass.getDeclaredField("DEBUG").getBoolean(null);
        } catch (Exception ignored) {
            log = false;
        }
        LOG = log;
    }

    public TaskFailureLogger(@NonNull String tag, @NonNull String message) {
        mTag = tag;
        mMessage = message;
    }

    @Override
    public void onFailure(@NonNull Exception e) {
        if (LOG) { Log.w(mTag, mMessage, e); }
    }
}
