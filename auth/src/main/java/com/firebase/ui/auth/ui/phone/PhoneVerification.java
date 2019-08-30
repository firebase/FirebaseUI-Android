package com.firebase.ui.auth.ui.phone;

import com.google.firebase.auth.PhoneAuthCredential;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public final class PhoneVerification {
    private final String mNumber;
    private final PhoneAuthCredential mCredential;
    private final boolean mIsAutoVerified;

    public PhoneVerification(@NonNull String number,
                             @NonNull PhoneAuthCredential credential,
                             boolean verified) {
        mNumber = number;
        mCredential = credential;
        mIsAutoVerified = verified;
    }

    @NonNull
    public String getNumber() {
        return mNumber;
    }

    @NonNull
    public PhoneAuthCredential getCredential() {
        return mCredential;
    }

    public boolean isAutoVerified() {
        return mIsAutoVerified;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        PhoneVerification that = (PhoneVerification) o;

        return mIsAutoVerified == that.mIsAutoVerified
                && mNumber.equals(that.mNumber)
                && mCredential.equals(that.mCredential);
    }

    @Override
    public int hashCode() {
        int result = mNumber.hashCode();
        result = 31 * result + mCredential.hashCode();
        result = 31 * result + (mIsAutoVerified ? 1 : 0);
        return result;
    }

    public String toString() {
        return "PhoneVerification{" +
                "mNumber='" + mNumber + '\'' +
                ", mCredential=" + mCredential +
                ", mIsAutoVerified=" + mIsAutoVerified +
                '}';
    }
}
