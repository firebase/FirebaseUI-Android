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
package com.firebase.ui.auth.util

import androidx.annotation.RestrictTo
import com.firebase.ui.auth.util.EmailLinkParser.LinkParameters.ANONYMOUS_USER_ID_IDENTIFIER
import com.firebase.ui.auth.util.EmailLinkParser.LinkParameters.FORCE_SAME_DEVICE_IDENTIFIER
import com.firebase.ui.auth.util.EmailLinkParser.LinkParameters.PROVIDER_ID_IDENTIFIER
import com.firebase.ui.auth.util.EmailLinkParser.LinkParameters.SESSION_IDENTIFIER

/**
 * Builder for constructing continue URLs with embedded session and authentication parameters.
 * Used in email link sign-in flows to pass state between devices.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class ContinueUrlBuilder(url: String) {

    private val continueUrl: StringBuilder

    init {
        require(url.isNotBlank()) { "URL cannot be empty" }
        continueUrl = StringBuilder(url).append("?")
    }

    fun appendSessionId(sessionId: String): ContinueUrlBuilder {
        addQueryParam(SESSION_IDENTIFIER, sessionId)
        return this
    }

    fun appendAnonymousUserId(anonymousUserId: String): ContinueUrlBuilder {
        addQueryParam(ANONYMOUS_USER_ID_IDENTIFIER, anonymousUserId)
        return this
    }

    fun appendProviderId(providerId: String): ContinueUrlBuilder {
        addQueryParam(PROVIDER_ID_IDENTIFIER, providerId)
        return this
    }

    fun appendForceSameDeviceBit(forceSameDevice: Boolean): ContinueUrlBuilder {
        val bit = if (forceSameDevice) "1" else "0"
        addQueryParam(FORCE_SAME_DEVICE_IDENTIFIER, bit)
        return this
    }

    private fun addQueryParam(key: String, value: String) {
        if (value.isBlank()) return

        val isFirstParam = continueUrl.last() == '?'
        val mark = if (isFirstParam) "" else "&"
        continueUrl.append("$mark$key=$value")
    }

    fun build(): String {
        if (continueUrl.last() == '?') {
            // No params added so we remove the '?'
            continueUrl.setLength(continueUrl.length - 1)
        }
        return continueUrl.toString()
    }
}