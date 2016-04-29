package com.firebase.ui.auth.choreographer.idp.provider;

import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;

public class IDPProviderParcel implements Parcelable {

    private final String mProviderId;
    private final Bundle mProviderExtra;

    public IDPProviderParcel(String providerId, Bundle providerExtra) {
        mProviderId = providerId;
        mProviderExtra = providerExtra;
    }

    public static final Creator<IDPProviderParcel> CREATOR = new Creator<IDPProviderParcel>() {
        @Override
        public IDPProviderParcel createFromParcel(Parcel in) {
            return new IDPProviderParcel(
                    in.readString(),
                    in.readBundle()
            );
        }

        @Override
        public IDPProviderParcel[] newArray(int size) {
            return new IDPProviderParcel[size];
        }
    };

    public String getProviderType() {
        return mProviderId;
    }

    public Bundle getProviderExtra() {
        return mProviderExtra;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(mProviderId);
        dest.writeBundle(mProviderExtra);
    }
}
