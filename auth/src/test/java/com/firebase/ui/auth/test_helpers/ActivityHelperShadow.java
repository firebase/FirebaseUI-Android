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

package com.firebase.ui.auth.test_helpers;

import com.firebase.ui.auth.ui.ActivityHelper;
import com.google.android.gms.auth.api.credentials.CredentialsApi;
import com.google.firebase.auth.FirebaseAuth;

import org.mockito.Mockito;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;

@Implements(ActivityHelper.class)
public class ActivityHelperShadow {
    public static FirebaseAuth firebaseAuth;
    public static CredentialsApi credentialsApi;

    public ActivityHelperShadow() {
        if (firebaseAuth == null) {
            firebaseAuth = Mockito.mock(FirebaseAuth.class);
        }
        if (credentialsApi == null) {
            credentialsApi = Mockito.mock(CredentialsApi.class);
        }
    }

    @Implementation
    public FirebaseAuth getFirebaseAuth() {
        return firebaseAuth;
    }

    @Implementation
    public CredentialsApi getCredentialsApi() {
        return credentialsApi;
    }
}
