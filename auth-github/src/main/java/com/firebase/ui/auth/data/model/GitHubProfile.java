package com.firebase.ui.auth.data.model;

import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RestrictTo;

import com.google.gson.annotations.SerializedName;

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public final class GitHubProfile {
    @SerializedName("email") private String mEmail;
    @SerializedName("name") private String mName;
    @SerializedName("avatar_url") private String mAvatarUrl;

    @NonNull
    public String getEmail() {
        return mEmail;
    }

    public void setEmail(@NonNull String email) {
        mEmail = email;
    }

    @Nullable
    public String getName() {
        return mName;
    }

    public void setName(@Nullable String name) {
        mName = name;
    }

    @Nullable
    public String getAvatarUrl() {
        return mAvatarUrl;
    }

    @Nullable
    public Uri getAvatarUri() {
        return mAvatarUrl == null ? null : Uri.parse(mAvatarUrl);
    }

    public void setAvatarUrl(@Nullable String avatarUrl) {
        mAvatarUrl = avatarUrl;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        GitHubProfile profile = (GitHubProfile) o;

        return mEmail.equals(profile.mEmail)
                && (mName == null ? profile.mName == null : mName.equals(profile.mName))
                && (mAvatarUrl == null ? profile.mAvatarUrl == null : mAvatarUrl.equals(profile.mAvatarUrl));
    }

    @Override
    public int hashCode() {
        int result = mEmail.hashCode();
        result = 31 * result + (mName == null ? 0 : mName.hashCode());
        result = 31 * result + (mAvatarUrl == null ? 0 : mAvatarUrl.hashCode());
        return result;
    }

    @Override
    public String toString() {
        return "GitHubProfile{" +
                "mEmail='" + mEmail + '\'' +
                ", mName='" + mName + '\'' +
                ", mAvatarUrl='" + mAvatarUrl + '\'' +
                '}';
    }
}
