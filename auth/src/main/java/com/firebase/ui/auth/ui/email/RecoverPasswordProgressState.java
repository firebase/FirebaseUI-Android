package com.firebase.ui.auth.ui.email;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.firebase.ui.auth.data.model.ProgressState;

public final class RecoverPasswordProgressState extends ProgressState {
    private final String mEmail;

    public RecoverPasswordProgressState(boolean done) {
        this(done, null, null);
    }

    public RecoverPasswordProgressState(boolean done,
                                        @Nullable String email,
                                        @Nullable Exception e) {
        super(done, e);
        mEmail = email;

        if (isSuccessful() && TextUtils.isEmpty(email)) {
            throw new IllegalStateException("Email cannot be null in the success state");
        }
    }

    @NonNull
    public String getEmail() {
        if (!isSuccessful()) {
            throw new IllegalStateException("Password reset is not yet complete or unsuccessful.");
        }
        return mEmail;
    }

    @Override
    public String toString() {
        return "RecoverPasswordProgressState{" +
                "mEmail='" + mEmail + '\'' +
                ", done=" + isDone() +
                ", successful=" + isSuccessful() +
                ", exception=" + getException() +
                '}';
    }
}
