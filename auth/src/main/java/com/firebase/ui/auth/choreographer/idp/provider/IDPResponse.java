package com.firebase.ui.auth.choreographer.idp.provider;

import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;

public class IDPResponse implements Parcelable {

    private final String mProviderId;
    private final Bundle mResponseBundle;

    public IDPResponse(String providerId, Bundle response) {
        mProviderId = providerId;
        mResponseBundle = response;
    }

    public static final Creator<IDPResponse> CREATOR = new Creator<IDPResponse>() {
        @Override
        public IDPResponse createFromParcel(Parcel in) {
            return new IDPResponse(
                    in.readString(),
                    in.readBundle()
            );
        }

        @Override
        public IDPResponse[] newArray(int size) {
            return new IDPResponse[size];
        }
    };

    public String getProviderType() {
        return mProviderId;
    }

    public Bundle getResponse() {
        return mResponseBundle;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(mProviderId);
        dest.writeBundle(mResponseBundle);
    }
}
