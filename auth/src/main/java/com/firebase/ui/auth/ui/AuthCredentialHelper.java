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

package com.firebase.ui.auth.ui;

import com.firebase.ui.auth.provider.FacebookProvider;
import com.firebase.ui.auth.provider.GoogleProvider;
import com.firebase.ui.auth.provider.IDPResponse;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FacebookAuthProvider;
import com.google.firebase.auth.GoogleAuthProvider;

public class AuthCredentialHelper {
    public static AuthCredential getAuthCredential(IDPResponse idpResponse) {
        switch (idpResponse.getProviderType()) {
            case GoogleAuthProvider.PROVIDER_ID:
                return GoogleProvider.createAuthCredential(idpResponse);
            case FacebookAuthProvider.PROVIDER_ID:
                return FacebookProvider.createAuthCredential(idpResponse);
            default:
                return null;
        }
    }
}
