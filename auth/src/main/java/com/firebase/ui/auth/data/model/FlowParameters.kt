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
package com.firebase.ui.auth.data.model

import android.content.Intent
import android.os.Parcel
import android.os.Parcelable
import android.text.TextUtils
import com.firebase.ui.auth.AuthMethodPickerLayout
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.AuthUI.IdpConfig
import com.firebase.ui.auth.util.ExtraConstants
import com.firebase.ui.auth.util.Preconditions
import com.google.firebase.auth.ActionCodeSettings
import com.google.firebase.auth.GoogleAuthProvider
import java.util.Collections
import androidx.annotation.DrawableRes
import androidx.annotation.NonNull
import androidx.annotation.Nullable
import androidx.annotation.RestrictTo
import androidx.annotation.StyleRes

/**
 * Encapsulates the core parameters and data captured during the authentication flow, in a
 * serializable manner, in order to pass data between activities.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class FlowParameters(
    @JvmField val appName: String,
    providers: List<IdpConfig>,
    @JvmField val defaultProvider: IdpConfig?,
    @StyleRes @JvmField val themeId: Int,
    @DrawableRes @JvmField val logoId: Int,
    @JvmField val termsOfServiceUrl: String?,
    @JvmField val privacyPolicyUrl: String?,
    @JvmField val enableCredentials: Boolean,
    @JvmField val enableAnonymousUpgrade: Boolean,
    @JvmField val alwaysShowProviderChoice: Boolean,
    @JvmField val lockOrientation: Boolean,
    @JvmField var emailLink: String?,
    @JvmField val passwordResetSettings: ActionCodeSettings?,
    @JvmField val authMethodPickerLayout: AuthMethodPickerLayout?
) : Parcelable {

    // Wrap the providers list in an unmodifiable list to mimic the original behavior.
    @JvmField
    val providers: List<IdpConfig> =
        Collections.unmodifiableList(Preconditions.checkNotNull(providers, "providers cannot be null"))

    init {
        Preconditions.checkNotNull(appName, "appName cannot be null")
    }

    /**
     * Constructor used for parcelable.
     */
    private constructor(parcel: Parcel) : this(
        appName = Preconditions.checkNotNull(parcel.readString(), "appName cannot be null"),
        providers = parcel.createTypedArrayList(IdpConfig.CREATOR)
            ?: emptyList(),
        defaultProvider = parcel.readParcelable(IdpConfig::class.java.classLoader),
        themeId = parcel.readInt(),
        logoId = parcel.readInt(),
        termsOfServiceUrl = parcel.readString(),
        privacyPolicyUrl = parcel.readString(),
        enableCredentials = parcel.readInt() != 0,
        enableAnonymousUpgrade = parcel.readInt() != 0,
        alwaysShowProviderChoice = parcel.readInt() != 0,
        lockOrientation = parcel.readInt() != 0,
        emailLink = parcel.readString(),
        passwordResetSettings = parcel.readParcelable(ActionCodeSettings::class.java.classLoader),
        authMethodPickerLayout = parcel.readParcelable(AuthMethodPickerLayout::class.java.classLoader)
    )

    /**
     * Extract FlowParameters from an Intent.
     */
    companion object CREATOR : Parcelable.Creator<FlowParameters> {
        override fun createFromParcel(parcel: Parcel): FlowParameters {
            return FlowParameters(parcel)
        }

        override fun newArray(size: Int): Array<FlowParameters?> {
            return arrayOfNulls(size)
        }

        @JvmStatic
        fun fromIntent(intent: Intent): FlowParameters =
            // getParcelableExtra returns a nullable type so we use !! to mirror the Java behavior.
            intent.getParcelableExtra<FlowParameters>(ExtraConstants.FLOW_PARAMS)!!
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeString(appName)
        dest.writeTypedList(providers)
        dest.writeParcelable(defaultProvider, flags)
        dest.writeInt(themeId)
        dest.writeInt(logoId)
        dest.writeString(termsOfServiceUrl)
        dest.writeString(privacyPolicyUrl)
        dest.writeInt(if (enableCredentials) 1 else 0)
        dest.writeInt(if (enableAnonymousUpgrade) 1 else 0)
        dest.writeInt(if (alwaysShowProviderChoice) 1 else 0)
        dest.writeInt(if (lockOrientation) 1 else 0)
        dest.writeString(emailLink)
        dest.writeParcelable(passwordResetSettings, flags)
        dest.writeParcelable(authMethodPickerLayout, flags)
    }

    override fun describeContents(): Int = 0

    fun isSingleProviderFlow(): Boolean = providers.size == 1

    fun isTermsOfServiceUrlProvided(): Boolean = !TextUtils.isEmpty(termsOfServiceUrl)

    fun isPrivacyPolicyUrlProvided(): Boolean = !TextUtils.isEmpty(privacyPolicyUrl)

    fun isAnonymousUpgradeEnabled(): Boolean = enableAnonymousUpgrade

    fun isPlayServicesRequired(): Boolean {
        // Play services only required for Google Sign In and the Credentials API
        return isProviderEnabled(GoogleAuthProvider.PROVIDER_ID) || enableCredentials
    }

    fun isProviderEnabled(@AuthUI.SupportedProvider provider: String): Boolean {
        for (idp in providers) {
            if (idp.providerId == provider) {
                return true
            }
        }
        return false
    }

    fun shouldShowProviderChoice(): Boolean {
        return defaultProvider == null && (!isSingleProviderFlow() || alwaysShowProviderChoice)
    }

    fun getDefaultOrFirstProvider(): IdpConfig {
        return defaultProvider ?: providers[0]
    }
}