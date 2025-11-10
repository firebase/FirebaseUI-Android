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

/**
 * Parser for email link sign-in URLs.
 * Extracts session information and parameters from Firebase email authentication links.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class EmailLinkParser(link: String) {

    private val params: Map<String, String>

    init {
        require(link.isNotBlank()) { "Link cannot be empty" }
        params = parseUri(link.toUri())
        require(params.isNotEmpty()) { "Invalid link: no parameters found" }
    }

    /**
     * The out-of-band code (OOB code) from the email link.
     * This is a required field for email link authentication.
     * @throws IllegalArgumentException if the OOB code is missing from the link
     */
    val oobCode: String
        get() = requireNotNull(params[OOB_CODE]) {
            "Invalid email link: missing required OOB code"
        }

    val sessionId: String?
        get() = params[LinkParameters.SESSION_IDENTIFIER]

    val anonymousUserId: String?
        get() = params[LinkParameters.ANONYMOUS_USER_ID_IDENTIFIER]

    val forceSameDeviceBit: Boolean
        get() {
            val bit = params[LinkParameters.FORCE_SAME_DEVICE_IDENTIFIER]
            // Default value is false when no bit is set
            return bit == "1"
        }

    val providerId: String?
        get() = params[LinkParameters.PROVIDER_ID_IDENTIFIER]

    private fun parseUri(uri: Uri): Map<String, String> {
        val map = mutableMapOf<String, String>()
        try {
            val queryParameters = uri.queryParameterNames
            for (param in queryParameters) {
                if (param.equals(LINK, ignoreCase = true) ||
                    param.equals(CONTINUE_URL, ignoreCase = true)) {
                    val innerUriString = uri.getQueryParameter(param)
                    if (innerUriString != null) {
                        val innerUri = innerUriString.toUri()
                        val innerValues = parseUri(innerUri)
                        map.putAll(innerValues)
                    }
                } else {
                    val value = uri.getQueryParameter(param)
                    if (value != null) {
                        map[param] = value
                    }
                }
            }
        } catch (e: Exception) {
            // Do nothing - return what we have
        }
        return map
    }

    object LinkParameters {
        const val SESSION_IDENTIFIER = "ui_sid"
        const val ANONYMOUS_USER_ID_IDENTIFIER = "ui_auid"
        const val FORCE_SAME_DEVICE_IDENTIFIER = "ui_sd"
        const val PROVIDER_ID_IDENTIFIER = "ui_pid"
    }

    companion object {
        private const val LINK = "link"
        private const val OOB_CODE = "oobCode"
        private const val CONTINUE_URL = "continueUrl"
    }
}