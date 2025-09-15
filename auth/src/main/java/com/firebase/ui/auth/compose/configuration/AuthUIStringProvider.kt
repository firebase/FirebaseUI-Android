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

package com.firebase.ui.auth.compose.configuration

import android.content.Context
import com.firebase.ui.auth.R

/**
 * An interface for providing localized string resources. This interface defines methods for all
 * user-facing strings, such as initializing(), signInWithGoogle(), invalidEmail(),
 * passwordsDoNotMatch(), etc., allowing for complete localization of the UI.
 */
interface AuthUIStringProvider {
    fun initializing(): String
    fun signInWithGoogle(): String
    fun invalidEmail(): String
    fun passwordsDoNotMatch(): String
}

class DefaultAuthUIStringProvider(private val context: Context) : AuthUIStringProvider {
    override fun initializing(): String = ""

    override fun signInWithGoogle(): String =
        context.getString(R.string.fui_sign_in_with_google)

    override fun invalidEmail(): String = ""

    override fun passwordsDoNotMatch(): String = ""
}
