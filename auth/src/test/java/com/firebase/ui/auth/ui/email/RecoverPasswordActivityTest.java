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

import static junit.framework.Assert.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Intent;
import android.widget.Button;

import com.firebase.ui.auth.test_helpers.ActivityHelperShadow;
import com.firebase.ui.auth.test_helpers.AutoCompleteTask;
import com.firebase.ui.auth.BuildConfig;
import com.firebase.ui.auth.test_helpers.CustomRobolectricGradleTestRunner;
import com.firebase.ui.auth.R;
import com.firebase.ui.auth.test_helpers.TestConstants;
import com.firebase.ui.auth.test_helpers.TestHelper;
import com.firebase.ui.auth.ui.ExtraConstants;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;

import java.util.Collections;

@RunWith(CustomRobolectricGradleTestRunner.class)
@Config(constants = BuildConfig.class, sdk = 21)
public class RecoverPasswordActivityTest {

    @Before
    public void setUp() {
        TestHelper.initializeApp(RuntimeEnvironment.application);
    }

    private RecoverPasswordActivity createActivity() {
        Intent startIntent = RecoverPasswordActivity.createIntent(
                RuntimeEnvironment.application,
                TestHelper.getFlowParameters(
                        RuntimeEnvironment.application,
                        Collections.<String>emptyList()),
                TestConstants.EMAIL);
        return Robolectric.buildActivity(RecoverPasswordActivity.class).withIntent(startIntent)
                .create().visible().get();
    }

    @Test
    @Config(shadows = {ActivityHelperShadow.class})
    public void testNextButton_sendsEmail() {
        RecoverPasswordActivity recoverPasswordActivity = createActivity();
        Button nextButton = (Button) recoverPasswordActivity.findViewById(R.id.button_done);
        when(ActivityHelperShadow.firebaseAuth.sendPasswordResetEmail(TestConstants.EMAIL))
                .thenReturn(new AutoCompleteTask<Void>(null, true, null));
        nextButton.performClick();

        Intent nextIntent = Shadows.shadowOf(recoverPasswordActivity)
                .getNextStartedActivityForResult()
                .intent;

        assertEquals(
                nextIntent.getComponent().getClassName(),
                ConfirmRecoverPasswordActivity.class.getName());

        verify(ActivityHelperShadow.firebaseAuth).sendPasswordResetEmail(TestConstants.EMAIL);
        assertEquals(
                TestConstants.EMAIL,
                nextIntent.getExtras().getString(ExtraConstants.EXTRA_EMAIL));
    }
}
