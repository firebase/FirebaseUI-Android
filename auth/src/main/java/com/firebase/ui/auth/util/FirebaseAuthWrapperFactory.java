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

import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;

/**
 * Utility class for producing instances of {@link FirebaseAuthWrapper}.
 */
public class FirebaseAuthWrapperFactory {

    /**
     * Produces a {@link FirebaseAuthWrapper} given the Firebase application name.
     */
    public static FirebaseAuthWrapper getFirebaseAuthWrapper(String appName) {
        return getFirebaseAuthWrapper(FirebaseApp.getInstance(appName));
    }

    /**
     * Produces a {@link FirebaseAuthWrapper} given a {@link FirebaseApp} instance.
     */
    public static FirebaseAuthWrapper getFirebaseAuthWrapper(FirebaseApp firebaseApp) {
        return new FirebaseAuthWrapperImpl(FirebaseAuth.getInstance(firebaseApp));
    }
}
