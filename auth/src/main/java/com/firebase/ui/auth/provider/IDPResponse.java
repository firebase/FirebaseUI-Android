/*
 * Copyright 2016 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.firebase.ui.auth.provider;

import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;

public class IDPResponse implements Parcelable {

    private final String mProviderId;
    private final String mEmail;
    private final Bundle mResponseBundle;

    public IDPResponse(String providerId, String email, Bundle response) {
        mProviderId = providerId;
        mEmail = email;
        mResponseBundle = response;
    }

    public static final Creator<IDPResponse> CREATOR = new Creator<IDPResponse>() {
        @Override
        public IDPResponse createFromParcel(Parcel in) {
            return new IDPResponse(
                    in.readString(),
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

    public String getEmail() {
        return mEmail;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(mProviderId);
        dest.writeString(mEmail);
        dest.writeBundle(mResponseBundle);
    }
}
