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

package com.firebase.ui.auth.util;

import android.support.annotation.RestrictTo;

/**
 * Constants used for passing Intent extra params between authentication flow activities.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public final class ExtraConstants {
    public static final String EXTRA_FLOW_PARAMS = "extra_flow_params";
    public static final String EXTRA_IDP_RESPONSE = "extra_idp_response";
    public static final String EXTRA_USER = "extra_user";
    public static final String EXTRA_CREDENTIAL = "extra_credential";

    public static final String EXTRA_EMAIL = "extra_email";
    public static final String EXTRA_ALLOW_NEW_EMAILS = "extra_allow_new_emails";
    public static final String EXTRA_REQUIRE_NAME = "extra_require_name";
    public static final String EXTRA_GOOGLE_SIGN_IN_OPTIONS = "extra_google_sign_in_options";
    public static final String EXTRA_FACEBOOK_PERMISSIONS = "extra_facebook_permissions";

    public static final String EXTRA_PARAMS = "extra_params";
    public static final String EXTRA_PHONE = "extra_phone_number";
    public static final String EXTRA_COUNTRY_ISO = "extra_country_iso";
    public static final String EXTRA_NATIONAL_NUMBER = "extra_national_number";

    public static final String HAS_EXISTING_INSTANCE = "has_existing_instance";

    private ExtraConstants() {
        throw new AssertionError("No instance for you!");
    }
}
