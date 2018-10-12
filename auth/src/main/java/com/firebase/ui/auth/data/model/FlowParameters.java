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
package com.firebase.ui.auth.data.model;

import android.content.Intent;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RestrictTo;
import android.support.annotation.StyleRes;
import android.text.TextUtils;

import com.firebase.ui.auth.AuthUI.IdpConfig;
import com.firebase.ui.auth.util.ExtraConstants;
import com.firebase.ui.auth.util.Preconditions;

import java.util.Collections;
import java.util.List;

/**
 * Encapsulates the core parameters and data captured during the authentication flow, in a
 * serializable manner, in order to pass data between activities.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class FlowParameters implements Parcelable {
    @NonNull
    public final String appName;

    @NonNull
    public final List<IdpConfig> providers;

    @StyleRes
    public final int themeId;

    @DrawableRes
    public final int logoId;

    @Nullable
    public final String termsOfServiceUrl;

    @Nullable
    public final String privacyPolicyUrl;

    public final boolean enableCredentials;
    public final boolean enableHints;
    public final boolean enableAnonymousUpgrade;
    public final boolean alwaysShowProviderChoice;

    public FlowParameters(
            @NonNull String appName,
            @NonNull List<IdpConfig> providers,
            @StyleRes int themeId,
            @DrawableRes int logoId,
            @Nullable String termsOfServiceUrl,
            @Nullable String privacyPolicyUrl,
            boolean enableCredentials,
            boolean enableHints,
            boolean enableAnonymousUpgrade,
            boolean alwaysShowProviderChoice) {
        this.appName = Preconditions.checkNotNull(appName, "appName cannot be null");
        this.providers = Collections.unmodifiableList(
                Preconditions.checkNotNull(providers, "providers cannot be null"));
        this.themeId = themeId;
        this.logoId = logoId;
        this.termsOfServiceUrl = termsOfServiceUrl;
        this.privacyPolicyUrl = privacyPolicyUrl;
        this.enableCredentials = enableCredentials;
        this.enableHints = enableHints;
        this.enableAnonymousUpgrade = enableAnonymousUpgrade;
        this.alwaysShowProviderChoice = alwaysShowProviderChoice;
    }

    /**
     * Extract FlowParameters from an Intent.
     */
    public static FlowParameters fromIntent(Intent intent) {
        return intent.getParcelableExtra(ExtraConstants.FLOW_PARAMS);
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(appName);
        dest.writeTypedList(providers);
        dest.writeInt(themeId);
        dest.writeInt(logoId);
        dest.writeString(termsOfServiceUrl);
        dest.writeString(privacyPolicyUrl);
        dest.writeInt(enableCredentials ? 1 : 0);
        dest.writeInt(enableHints ? 1 : 0);
        dest.writeInt(enableAnonymousUpgrade ? 1 : 0);
        dest.writeInt(alwaysShowProviderChoice ? 1 : 0);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<FlowParameters> CREATOR = new Creator<FlowParameters>() {
        @Override
        public FlowParameters createFromParcel(Parcel in) {
            String appName = in.readString();
            List<IdpConfig> providerInfo = in.createTypedArrayList(IdpConfig.CREATOR);
            int themeId = in.readInt();
            int logoId = in.readInt();
            String termsOfServiceUrl = in.readString();
            String privacyPolicyUrl = in.readString();
            boolean enableCredentials = in.readInt() != 0;
            boolean enableHints = in.readInt() != 0;
            boolean enableAnonymousUpgrade = in.readInt() != 0;
            boolean alwaysShowProviderChoice = in.readInt() != 0;

            return new FlowParameters(
                    appName,
                    providerInfo,
                    themeId,
                    logoId,
                    termsOfServiceUrl,
                    privacyPolicyUrl,
                    enableCredentials,
                    enableHints,
                    enableAnonymousUpgrade,
                    alwaysShowProviderChoice);
        }

        @Override
        public FlowParameters[] newArray(int size) {
            return new FlowParameters[size];
        }
    };

    public boolean isSingleProviderFlow() {
        return providers.size() == 1;
    }

    public boolean isTermsOfServiceUrlProvided() {
        return !TextUtils.isEmpty(termsOfServiceUrl);
    }

    public boolean isPrivacyPolicyUrlProvided() {
        return !TextUtils.isEmpty(privacyPolicyUrl);
    }

    public boolean isAnonymousUpgradeEnabled() {
        return enableAnonymousUpgrade;
    }

    public boolean shouldShowProviderChoice() {
        return !isSingleProviderFlow() || alwaysShowProviderChoice;
    }
}
