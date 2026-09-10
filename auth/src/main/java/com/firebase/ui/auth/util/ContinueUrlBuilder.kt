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

import android.net.Uri
import androidx.annotation.RestrictTo
import androidx.core.net.toUri
import com.firebase.ui.auth.util.EmailLinkParser.LinkParameters.ANONYMOUS_USER_ID_IDENTIFIER
import com.firebase.ui.auth.util.EmailLinkParser.LinkParameters.FORCE_SAME_DEVICE_IDENTIFIER
import com.firebase.ui.auth.util.EmailLinkParser.LinkParameters.PROVIDER_ID_IDENTIFIER
import com.firebase.ui.auth.util.EmailLinkParser.LinkParameters.SESSION_IDENTIFIER

/**
 * Builder for constructing continue URLs with embedded session and authentication parameters.
 * Used in email link sign-in flows to pass state between devices.
 *
 * The incoming URL comes from the consumer's [com.google.firebase.auth.ActionCodeSettings], so it
 * may already carry a query string and/or a fragment. Parameters are appended through [Uri], the
 * same parser [EmailLinkParser] reads them back with, which places them in the query whatever the
 * URL's shape and percent-encodes their values.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class ContinueUrlBuilder(url: String) {

    private var continueUrl: Uri

    init {
        require(url.isNotBlank()) { "URL cannot be empty" }
        continueUrl = url.toUri()
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

        continueUrl = continueUrl.buildUpon().appendQueryParameter(key, value).build()
    }

    // Untouched when nothing was appended: Uri hands back the string it was parsed from.
    fun build(): String = continueUrl.toString()
}
