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

package com.firebase.ui.auth.ui.email;

import android.content.Intent;
import android.support.design.widget.TextInputLayout;
import android.widget.Button;

import com.firebase.ui.auth.R;
import com.firebase.ui.auth.data.model.User;
import com.firebase.ui.auth.testhelpers.TestConstants;
import com.firebase.ui.auth.testhelpers.TestHelper;
import com.google.firebase.auth.EmailAuthProvider;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.util.Collections;

import static org.junit.Assert.assertEquals;

@RunWith(RobolectricTestRunner.class)
public class EmailActivityTest {
    private EmailActivity createActivity() {
        Intent startIntent = EmailActivity.createIntent(
                RuntimeEnvironment.application,
                TestHelper.getFlowParameters(Collections.singletonList(EmailAuthProvider.PROVIDER_ID)));

        return Robolectric.buildActivity(EmailActivity.class, startIntent)
                .create()
                .start()
                .visible()
                .get();
    }

    @Before
    public void setUp() {
        TestHelper.initialize();
    }

    @Test
    public void testSignUpButton_validatesFields() {
        EmailActivity emailActivity = createActivity();

        // Trigger RegisterEmailFragment (bypass check email)
        emailActivity.onNewUser(
                new User.Builder(EmailAuthProvider.PROVIDER_ID, TestConstants.EMAIL).build());

        Button button = emailActivity.findViewById(R.id.button_create);
        button.performClick();

        TextInputLayout nameLayout = emailActivity.findViewById(R.id.name_layout);
        TextInputLayout passwordLayout = emailActivity.findViewById(R.id.password_layout);

        assertEquals(
                emailActivity.getString(R.string.fui_required_field),
                nameLayout.getError().toString());
        assertEquals(
                String.format(
                        emailActivity.getResources().getQuantityString(
                                R.plurals.fui_error_weak_password,
                                R.integer.fui_min_password_length),
                        emailActivity.getResources()
                                .getInteger(R.integer.fui_min_password_length)
                ),
                passwordLayout.getError().toString());
    }
}
