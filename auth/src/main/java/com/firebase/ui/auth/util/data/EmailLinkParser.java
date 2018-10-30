package com.firebase.ui.auth.util.data;

import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.RestrictTo;

import com.google.android.gms.common.internal.Preconditions;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class EmailLinkParser {

    // TODO(lsirac): centralize
    private static final String SESSION_IDENTIFIER = "ui_sid";
    private static final String ANONYMOUS_USER_ID_IDENTIFIER = "ui_auid";
    private static final String FORCE_SAME_DEVICE_IDENTIFIER = "ui_sd";
    private static final String PROVIDER_ID_IDENTIFIER = "ui_pid";


    private static String LINK = "link";

    private static final String OOB_CODE = "oobCode";
    private static final String CONTINUE_URL = "continueUrl";

    private Map<String, String> params;

    public EmailLinkParser(@NonNull String link) {
        Preconditions.checkNotEmpty(link);
        params = parseUri(Uri.parse(link));
        if (params.isEmpty()) {
            throw new IllegalArgumentException("Invalid link: no parameters found");
        }
    }

    public String getOobCode() {
        return params.get(OOB_CODE);
    }

    public String getSessionId() {
        return params.get(SESSION_IDENTIFIER);
    }

    public String getAnonymousUserId() {
        return params.get(ANONYMOUS_USER_ID_IDENTIFIER);
    }

    public boolean getForceSameDeviceBit() {
        return params.get(FORCE_SAME_DEVICE_IDENTIFIER).equals("1");
    }

    public String getProviderId() {
        return params.get(PROVIDER_ID_IDENTIFIER);
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
}
