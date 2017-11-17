package com.firebase.ui.auth.ui;

import android.support.annotation.RestrictTo;

/**
 * Constants used for passing Intent extra params between authentication flow activities.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class ExtraConstants {
    public static final String EXTRA_FLOW_PARAMS = "extra_flow_params";
    public static final String EXTRA_IDP_RESPONSE = "extra_idp_response";
    public static final String EXTRA_USER = "extra_user";
    public static final String EXTRA_EMAIL = "extra_email";
    public static final String EXTRA_PHONE = "extra_phone_number";
    public static final String EXTRA_COUNTRY_CODE = "extra_country_code";
    public static final String EXTRA_NATIONAL_NUMBER = "extra_national_number";
    public static final String HAS_EXISTING_INSTANCE = "has_existing_instance";
    public static final String EXTRA_PARAMS = "extra_params";
}
