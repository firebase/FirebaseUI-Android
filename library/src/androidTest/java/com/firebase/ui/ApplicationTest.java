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

package com.firebase.ui;

import android.app.Application;
import android.content.Context;
import android.test.ApplicationTestCase;

import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;

/**
 * <a href="http://d.android.com/tools/testing/testing_android.html">Testing Fundamentals</a>
 */
public class ApplicationTest extends ApplicationTestCase<Application> {

    private static final String APP_NAME = "firebaseui-tests";

    public ApplicationTest() {
        super(Application.class);
    }

    public static FirebaseApp getAppInstance(Context context) {
        try {
            return FirebaseApp.getInstance(APP_NAME);
        } catch (IllegalStateException e) {
            return initializeApp(context);
        }
    }

    public static FirebaseApp initializeApp(Context context) {
        return FirebaseApp.initializeApp(context, new FirebaseOptions.Builder()
                .setApplicationId("foo-bar")
                .setDatabaseUrl("https://firebaseui-tests.firebaseio-demo.com/")
                .build(), APP_NAME);
    }
}
