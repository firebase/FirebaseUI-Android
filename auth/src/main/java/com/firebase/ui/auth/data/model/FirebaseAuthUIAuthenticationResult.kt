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

package com.firebase.ui.auth.data.model

import com.firebase.ui.auth.IdpResponse

/**
 * Result of launching a [FirebaseAuthUIActivityResultContract]
 */
data class FirebaseAuthUIAuthenticationResult(
    /**
     * The result code of the received activity result
     *
     * @see android.app.Activity.RESULT_CANCELED
     * @see android.app.Activity.RESULT_OK
     */
    val resultCode: Int,
    /**
     * The contained [IdpResponse] returned from the Firebase library
     */
    val idpResponse: IdpResponse?
)