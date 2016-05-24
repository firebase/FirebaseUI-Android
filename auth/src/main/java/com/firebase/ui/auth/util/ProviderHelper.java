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

package com.firebase.ui.auth.util;

import android.content.Context;
import android.os.Bundle;

import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.R;
import com.firebase.ui.auth.provider.FacebookProvider;
import com.firebase.ui.auth.provider.GoogleProvider;
import com.firebase.ui.auth.provider.IDPProviderParcel;
import com.google.firebase.auth.EmailAuthProvider;

import java.util.ArrayList;
import java.util.List;

public class ProviderHelper {
    public static List<IDPProviderParcel> getProviderParcels(
            Context context,
            List<String> providerIds) {
        ArrayList<IDPProviderParcel> providerInfo = new ArrayList<>();
        for (String provider : providerIds) {
            if (provider.equalsIgnoreCase(AuthUI.FACEBOOK_PROVIDER)) {
                providerInfo.add(FacebookProvider.createFacebookParcel(
                        context.getString(R.string.facebook_application_id)));
            } else if (provider.equalsIgnoreCase(AuthUI.GOOGLE_PROVIDER)) {
                providerInfo.add(GoogleProvider.createParcel(
                        context.getString(R.string.default_web_client_id)));
            } else if (provider.equalsIgnoreCase(AuthUI.EMAIL_PROVIDER)) {
                providerInfo.add(
                        new IDPProviderParcel(EmailAuthProvider.PROVIDER_ID, new Bundle())
                );
            }
        }
        return providerInfo;
    }
}
