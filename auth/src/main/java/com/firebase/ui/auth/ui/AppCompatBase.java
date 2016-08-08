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

import android.content.Intent;
import android.os.Bundle;

public class AppCompatBase extends android.support.v7.app.AppCompatActivity {
    protected ActivityHelper mActivityHelper;

    @Override
    protected void onCreate(Bundle savedInstance) {
        super.onCreate(savedInstance);
        mActivityHelper = new ActivityHelper(this, getIntent());
        mActivityHelper.configureTheme();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mActivityHelper.dismissDialog();
    }

    public void finish(int resultCode, Intent intent) {
        mActivityHelper.finish(resultCode, intent);
    }

}
