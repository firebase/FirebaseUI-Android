package com.firebase.ui.auth.util.data;

import android.net.Uri;

import java.util.Set;

public class EmailLinkParser {

    private static String LINK = "link";
    private static final String OOB_CODE = "oobCode";

    private static EmailLinkParser instance = new EmailLinkParser();

    private EmailLinkParser() {}

    public static EmailLinkParser getInstance() {
        return instance;
    }

    public String getOobCodeFromLink(String signInLink) {
        Uri uri = Uri.parse(signInLink);
        try {
            Set<String> params = uri.getQueryParameterNames();
            if (params.contains(OOB_CODE)) {
                return uri.getQueryParameter(OOB_CODE);
            } else if (params.contains(LINK)) {
                Uri innerUri = Uri.parse(uri.getQueryParameter(LINK));
                return innerUri.getQueryParameter(OOB_CODE);
            }
        } catch (UnsupportedOperationException e) {

        }
        return null;
    }
}
