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

import androidx.annotation.RestrictTo;

/**
 * Constants used for passing Intent extra params between authentication flow activities.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public final class ExtraConstants {
    public static final String FLOW_PARAMS = "extra_flow_params";
    public static final String IDP_RESPONSE = "extra_idp_response";
    public static final String USER = "extra_user";
    public static final String CREDENTIAL = "extra_credential";

    public static final String EMAIL = "extra_email";
    public static final String DEFAULT_EMAIL = "extra_default_email";
    public static final String ALLOW_NEW_EMAILS = "extra_allow_new_emails";
    public static final String REQUIRE_NAME = "extra_require_name";
    public static final String GOOGLE_SIGN_IN_OPTIONS = "extra_google_sign_in_options";
    public static final String FACEBOOK_PERMISSIONS = "extra_facebook_permissions";
    public static final String GITHUB_PERMISSIONS = "extra_github_permissions";
    public static final String GITHUB_URL = "github_url";

    public static final String PARAMS = "extra_params";
    public static final String PHONE = "extra_phone_number";
    public static final String COUNTRY_ISO = "extra_country_iso";
    public static final String NATIONAL_NUMBER = "extra_national_number";

    public static final String WHITELISTED_COUNTRIES = "whitelisted_countries";
    public static final String BLACKLISTED_COUNTRIES = "blacklisted_countries";

    public static final String EMAIL_LINK_SIGN_IN = "email_link_sign_in";
    public static final String ACTION_CODE_SETTINGS = "action_code_settings";
    public static final String FORCE_SAME_DEVICE = "force_same_device";
    public static final String PROVIDER_ID = "provider_id";

    public static final String GENERIC_OAUTH_PROVIDER_ID = "generic_oauth_provider_id";
    public static final String GENERIC_OAUTH_PROVIDER_NAME = "generic_oauth_provider_name";
    public static final String GENERIC_OAUTH_BUTTON_ID = "generic_oauth_button_id";
    public static final String GENERIC_OAUTH_SCOPES = "generic_oauth_scopes";
    public static final String GENERIC_OAUTH_CUSTOM_PARAMETERS = "generic_oauth_custom_parameters";

    private ExtraConstants() {
        throw new AssertionError("No instance for you!");
    }
}
