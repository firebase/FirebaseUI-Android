package com.firebase.ui.auth.util.data;

import android.net.Uri;
import android.text.TextUtils;

import com.google.android.gms.common.internal.Preconditions;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class EmailLinkParser {

    private static String LINK = "link";

    private static final String OOB_CODE = "oobCode";
    private static final String CONTINUE_URL = "continueUrl";

    private Map<String, String> mParams;

    public EmailLinkParser(@NonNull String link) {
        Preconditions.checkNotEmpty(link);
        mParams = parseUri(Uri.parse(link));
        if (mParams.isEmpty()) {
            throw new IllegalArgumentException("Invalid link: no parameters found");
        }
    }

    public String getOobCode() {
        return mParams.get(OOB_CODE);
    }

    public String getSessionId() {
        return mParams.get(LinkParameters.SESSION_IDENTIFIER);
    }

    public String getAnonymousUserId() {
        return mParams.get(LinkParameters.ANONYMOUS_USER_ID_IDENTIFIER);
    }

    public boolean getForceSameDeviceBit() {
        String forceSameDeviceBit = mParams.get(LinkParameters.FORCE_SAME_DEVICE_IDENTIFIER);
        if (TextUtils.isEmpty(forceSameDeviceBit)) {
            // Default value is false when no bit is set
            return false;
        }
        return forceSameDeviceBit.equals("1");
    }

    public String getProviderId() {
        return mParams.get(LinkParameters.PROVIDER_ID_IDENTIFIER);
    }

    private Map<String, String> parseUri(Uri uri) {
        Map<String, String> map = new HashMap<>();
        try {
            Set<String> queryParameters = uri.getQueryParameterNames();
            for (String param : queryParameters) {
                if (param.equalsIgnoreCase(LINK) || param.equalsIgnoreCase(CONTINUE_URL)) {
                    Uri innerUri = Uri.parse(uri.getQueryParameter(param));
                    Map<String, String> innerValues = parseUri(innerUri);
                    if (innerValues != null) {
                        map.putAll(innerValues);
                    }
                } else {
                    String value = uri.getQueryParameter(param);
                    if (value != null) {
                        map.put(param, value);
                    }
                }
            }
        } catch (Exception e) {
            // Do nothing.
        }
        return map;
    }

    public static class LinkParameters {
        public static final String SESSION_IDENTIFIER = "ui_sid";
        public static final String ANONYMOUS_USER_ID_IDENTIFIER = "ui_auid";
        public static final String FORCE_SAME_DEVICE_IDENTIFIER = "ui_sd";
        public static final String PROVIDER_ID_IDENTIFIER = "ui_pid";
    }
}
