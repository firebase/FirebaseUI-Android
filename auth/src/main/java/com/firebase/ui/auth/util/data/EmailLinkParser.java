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

    private static String LINK = "link";

    private static final String OOB_CODE = "oobCode";

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

    private Map<String, String> parseUri(Uri uri) {
        Map<String, String> map = new HashMap<>();
        try {
            Set<String> queryParameters = uri.getQueryParameterNames();
            for (String param : queryParameters) {
                if (param.equalsIgnoreCase(LINK)) {
                    Uri innerUri = Uri.parse(uri.getQueryParameter(LINK));
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
