package com.firebase.ui.auth.util.data;

import android.support.annotation.NonNull;
import android.support.annotation.RestrictTo;
import static com.firebase.ui.auth.util.data.EmailLinkParser.LinkParameters.*;
import android.text.TextUtils;

import com.google.android.gms.common.internal.Preconditions;

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class ContinueUrlBuilder {

    private StringBuilder mContinueUrl;

    public ContinueUrlBuilder(@NonNull String url) {
        Preconditions.checkNotEmpty(url);
        mContinueUrl = new StringBuilder(url + "?");
    }

    public ContinueUrlBuilder appendSessionId(@NonNull String sessionId) {
        addQueryParam(SESSION_IDENTIFIER, sessionId);
        return this;
    }

    public ContinueUrlBuilder appendAnonymousUserId(@NonNull String anonymousUserId) {
        addQueryParam(ANONYMOUS_USER_ID_IDENTIFIER, anonymousUserId);
        return this;
    }

    public ContinueUrlBuilder appendProviderId(@NonNull String providerId) {
        addQueryParam(PROVIDER_ID_IDENTIFIER, providerId);
        return this;
    }

    public ContinueUrlBuilder appendForceSameDeviceBit(@NonNull boolean forceSameDevice) {
        String bit = forceSameDevice ? "1" : "0";
        addQueryParam(FORCE_SAME_DEVICE_IDENTIFIER, bit);
        return this;
    }

    private void addQueryParam(String key, String value) {
        if (TextUtils.isEmpty(value)) {
            return;
        }
        boolean isFirstParam = mContinueUrl.charAt(mContinueUrl.length() - 1) == '?';
        String mark = isFirstParam ? "" : "&";
        mContinueUrl.append(String.format("%s%s=%s", mark, key, value));
    }

    public String build() {
        if (mContinueUrl.charAt(mContinueUrl.length() - 1) == '?') {
            // No params added so we remove the '?'
            mContinueUrl.setLength(mContinueUrl.length() - 1);
        }
        return mContinueUrl.toString();
    }
}
