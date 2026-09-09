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
 *
 * The incoming URL comes from the consumer's [com.google.firebase.auth.ActionCodeSettings], so it
 * may already carry a query string and/or a fragment. Appended parameters join an existing
 * query rather than starting a second one, and are always placed before the fragment.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class ContinueUrlBuilder(url: String) {

    /** The incoming URL up to, but not including, the fragment. */
    private val baseUrl: String

    /** The fragment including its leading `#`, or empty when the URL has none. */
    private val fragment: String

    private val params = StringBuilder()

    init {
        require(url.isNotBlank()) { "URL cannot be empty" }

        val fragmentStart = url.indexOf('#')
        baseUrl = if (fragmentStart == -1) url else url.substring(0, fragmentStart)
        fragment = if (fragmentStart == -1) "" else url.substring(fragmentStart)
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

        if (params.isNotEmpty()) {
            params.append("&")
        }
        params.append("$key=$value")
    }

    fun build(): String {
        // No params added, so the URL is handed back untouched.
        if (params.isEmpty()) return baseUrl + fragment

        val queryStart = baseUrl.indexOf('?')
        val separator = when {
            queryStart == -1 -> "?"
            // The query is open, so no separator is needed. Only the first `?` marks the
            // query; a later one is a literal inside a value and leaves it open to append.
            queryStart == baseUrl.length - 1 || baseUrl.endsWith('&') -> ""
            else -> "&"
        }
        return baseUrl + separator + params + fragment
    }
}
