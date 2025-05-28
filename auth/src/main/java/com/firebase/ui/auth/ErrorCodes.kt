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

package com.firebase.ui.auth

import androidx.annotation.RestrictTo
import androidx.annotation.IntDef
import kotlin.jvm.JvmStatic

/**
 * Error codes for failed sign-in attempts.
 */
object ErrorCodes {
    /**
     * Valid codes that can be returned from FirebaseUiException.getErrorCode().
     */
    @Retention(AnnotationRetention.SOURCE)
    @IntDef(
        UNKNOWN_ERROR,
        NO_NETWORK,
        PLAY_SERVICES_UPDATE_CANCELLED,
        DEVELOPER_ERROR,
        PROVIDER_ERROR,
        ANONYMOUS_UPGRADE_MERGE_CONFLICT,
        EMAIL_MISMATCH_ERROR,
        INVALID_EMAIL_LINK_ERROR,
        EMAIL_LINK_WRONG_DEVICE_ERROR,
        EMAIL_LINK_PROMPT_FOR_EMAIL_ERROR,
        EMAIL_LINK_CROSS_DEVICE_LINKING_ERROR,
        EMAIL_LINK_DIFFERENT_ANONYMOUS_USER_ERROR,
        ERROR_USER_DISABLED,
        ERROR_GENERIC_IDP_RECOVERABLE_ERROR
    )
    annotation class Code

    /**
     * An unknown error has occurred.
     */
    const val UNKNOWN_ERROR = 0

    /**
     * Sign in failed due to lack of network connection.
     */
    const val NO_NETWORK = 1

    /**
     * A required update to Play Services was cancelled by the user.
     */
    const val PLAY_SERVICES_UPDATE_CANCELLED = 2

    /**
     * A sign-in operation couldn't be completed due to a developer error.
     */
    const val DEVELOPER_ERROR = 3

    /**
     * An external sign-in provider error occurred.
     */
    const val PROVIDER_ERROR = 4

    /**
     * Anonymous account linking failed.
     */
    const val ANONYMOUS_UPGRADE_MERGE_CONFLICT = 5

    /**
     * Signing in with a different email in the WelcomeBackIdp flow or email link flow.
     */
    const val EMAIL_MISMATCH_ERROR = 6

    /**
     * Attempting to sign in with an invalid email link.
     */
    const val INVALID_EMAIL_LINK_ERROR = 7

    /**
     * Attempting to open an email link from a different device.
     */
    const val EMAIL_LINK_WRONG_DEVICE_ERROR = 8

    /**
     * We need to prompt the user for their email.
     */
    const val EMAIL_LINK_PROMPT_FOR_EMAIL_ERROR = 9

    /**
     * Cross device linking flow - we need to ask the user if they want to continue linking or
     * just sign in.
     */
    const val EMAIL_LINK_CROSS_DEVICE_LINKING_ERROR = 10

    /**
     * Attempting to open an email link from the same device, with anonymous upgrade enabled,
     * but the underlying anonymous user has been changed.
     */
    const val EMAIL_LINK_DIFFERENT_ANONYMOUS_USER_ERROR = 11

    /**
     * Attempting to auth with account that is currently disabled in the Firebase console.
     */
    const val ERROR_USER_DISABLED = 12

    /**
     * Recoverable error occurred during the Generic IDP flow.
     */
    const val ERROR_GENERIC_IDP_RECOVERABLE_ERROR = 13

    @JvmStatic
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    fun toFriendlyMessage(@Code code: Int): String = when (code) {
        UNKNOWN_ERROR -> "Unknown error"
        NO_NETWORK -> "No internet connection"
        PLAY_SERVICES_UPDATE_CANCELLED -> "Play Services update cancelled"
        DEVELOPER_ERROR -> "Developer error"
        PROVIDER_ERROR -> "Provider error"
        ANONYMOUS_UPGRADE_MERGE_CONFLICT -> "User account merge conflict"
        EMAIL_MISMATCH_ERROR -> "You are are attempting to sign in a different email than previously provided"
        INVALID_EMAIL_LINK_ERROR -> "You are are attempting to sign in with an invalid email link"
        EMAIL_LINK_PROMPT_FOR_EMAIL_ERROR -> "Please enter your email to continue signing in"
        EMAIL_LINK_WRONG_DEVICE_ERROR -> "You must open the email link on the same device."
        EMAIL_LINK_CROSS_DEVICE_LINKING_ERROR -> "You must determine if you want to continue linking or complete the sign in"
        EMAIL_LINK_DIFFERENT_ANONYMOUS_USER_ERROR -> "The session associated with this sign-in request has either expired or was cleared"
        ERROR_USER_DISABLED -> "The user account has been disabled by an administrator."
        ERROR_GENERIC_IDP_RECOVERABLE_ERROR -> "Generic IDP recoverable error."
        else -> throw IllegalArgumentException("Unknown code: $code")
    }
} 