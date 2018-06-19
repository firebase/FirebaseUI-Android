package com.firebase.ui.auth.data.model;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RestrictTo;

import com.google.gson.annotations.SerializedName;

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public final class GitHubTokenResponse {
    @SerializedName("access_token") private String mToken;
    @SerializedName("token_type") private String mType;
    @SerializedName("scope") private String mScope;

    @NonNull
    public String getToken() {
        return mToken;
    }

    public void setToken(@NonNull String token) {
        mToken = token;
    }

    @NonNull
    public String getType() {
        return mType;
    }

    public void setType(@NonNull String type) {
        mType = type;
    }

    @Nullable
    public String getScope() {
        return mScope;
    }

    public void setScope(@Nullable String scope) {
        mScope = scope;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        GitHubTokenResponse response = (GitHubTokenResponse) o;

        return mToken.equals(response.mToken)
                && (mType == null ? response.mType == null : mType.equals(response.mType))
                && (mScope == null ? response.mScope == null : mScope.equals(response.mScope));
    }

    @Override
    public int hashCode() {
        int result = mToken.hashCode();
        result = 31 * result + (mType == null ? 0 : mType.hashCode());
        result = 31 * result + (mScope == null ? 0 : mScope.hashCode());
        return result;
    }

    @Override
    public String toString() {
        return "GitHubTokenResponse{" +
                "mToken='" + mToken + '\'' +
                ", mType='" + mType + '\'' +
                ", mScope='" + mScope + '\'' +
                '}';
    }
}
