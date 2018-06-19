package com.firebase.ui.auth.testhelpers;

import android.os.Parcel;

import com.google.firebase.auth.AdditionalUserInfo;

import java.util.Map;

public final class FakeAdditionalUserInfo implements AdditionalUserInfo {
    public static final AdditionalUserInfo INSTANCE = new FakeAdditionalUserInfo();

    // Singleton
    private FakeAdditionalUserInfo() {}

    @Override
    public String getProviderId() {
        return null;
    }

    @Override
    public Map<String, Object> getProfile() {
        return null;
    }

    @Override
    public String getUsername() {
        return null;
    }

    @Override
    public boolean isNewUser() {
        return false;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        throw new IllegalStateException("Don't try to parcel FakeAuthResult!");
    }
}
