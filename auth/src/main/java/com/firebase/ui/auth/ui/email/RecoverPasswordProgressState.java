package com.firebase.ui.auth.ui.email;

import android.support.annotation.NonNull;
import android.support.annotation.RestrictTo;

import com.firebase.ui.auth.data.model.ProgressState;

/**
 * Password recovery state.
 * <p>
 * When this state is successful, the email who's password was reset can be retrieved.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public final class RecoverPasswordProgressState extends ProgressState {
    private final String mEmail;

    public RecoverPasswordProgressState() {
        super();
        mEmail = null;
    }

    public RecoverPasswordProgressState(@NonNull String email) {
        super(null);
        mEmail = email;
    }

    public RecoverPasswordProgressState(@NonNull Exception e) {
        super(e);
        mEmail = null;
    }

    @NonNull
    public String getEmail() {
        if (!isSuccessful()) {
            throw new IllegalStateException("Password reset is not yet complete or unsuccessful.");
        }
        return mEmail;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        RecoverPasswordProgressState state = (RecoverPasswordProgressState) o;

        return mEmail == null ? state.mEmail == null : mEmail.equals(state.mEmail);
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (mEmail == null ? 0 : mEmail.hashCode());
        return result;
    }

    @Override
    public String toString() {
        return "RecoverPasswordProgressState{" +
                "mEmail='" + mEmail + '\'' +
                ", complete=" + isComplete() +
                ", successful=" + isSuccessful() +
                ", exception=" + getException() +
                '}';
    }
}
