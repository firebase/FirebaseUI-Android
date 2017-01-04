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
    public static final String HAS_EXISTING_INSTANCE = "has_existing_instance";
}
