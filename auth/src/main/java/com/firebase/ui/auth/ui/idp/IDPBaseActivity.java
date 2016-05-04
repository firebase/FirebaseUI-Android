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

package com.firebase.ui.auth.ui.idp;

import android.os.Bundle;

import com.firebase.ui.auth.choreographer.Controller;
import com.firebase.ui.auth.choreographer.idp.IDPController;
import com.firebase.ui.auth.ui.BaseActivity;

public class IDPBaseActivity extends BaseActivity {

    public static final int EMAIL_LOGIN_NEEDED = RESULT_FIRST_USER + 2;
    public static final int LOGIN_CANCELLED = RESULT_FIRST_USER + 3;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    protected Controller setUpController() {
        return new IDPController(this, mAppName);
    }
}
