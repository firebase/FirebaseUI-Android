/*
 * Copyright 2025 Google Inc. All Rights Reserved.
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
