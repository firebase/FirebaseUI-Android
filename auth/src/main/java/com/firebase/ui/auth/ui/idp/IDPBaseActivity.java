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

package com.firebase.ui.auth.ui.idp;

import com.firebase.ui.auth.provider.FacebookProvider;
import com.firebase.ui.auth.provider.GoogleProvider;
import com.firebase.ui.auth.IdpResponse;
import com.firebase.ui.auth.provider.TwitterProvider;
import com.firebase.ui.auth.ui.AppCompatBase;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FacebookAuthProvider;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.auth.TwitterAuthProvider;

public class IDPBaseActivity extends AppCompatBase {
    protected AuthCredential createCredential(IdpResponse idpSignInResponse) {
        if (idpSignInResponse.getProviderType().equalsIgnoreCase(FacebookAuthProvider.PROVIDER_ID)) {
            return FacebookProvider.createAuthCredential(idpSignInResponse);
        } else if (idpSignInResponse.getProviderType().equalsIgnoreCase(GoogleAuthProvider
                .PROVIDER_ID)) {
            return GoogleProvider.createAuthCredential(idpSignInResponse);
        } else if (idpSignInResponse
                .getProviderType()
                .equalsIgnoreCase(TwitterAuthProvider.PROVIDER_ID)) {
            return TwitterProvider.createAuthCredential(idpSignInResponse);
        }
        return null;
    }
}
