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
import android.widget.Button;

import com.firebase.ui.auth.BuildConfig;
import com.firebase.ui.auth.R;
import com.firebase.ui.auth.testhelpers.ActivityHelperShadow;
import com.firebase.ui.auth.testhelpers.AutoCompleteTask;
import com.firebase.ui.auth.testhelpers.BaseHelperShadow;
import com.firebase.ui.auth.testhelpers.CustomRobolectricGradleTestRunner;
import com.firebase.ui.auth.testhelpers.TestConstants;
import com.firebase.ui.auth.testhelpers.TestHelper;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.util.Collections;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(CustomRobolectricGradleTestRunner.class)
@Config(constants = BuildConfig.class, sdk = 25)
public class RecoverPasswordActivityTest {

    @Before
    public void setUp() {
        TestHelper.initializeApp(RuntimeEnvironment.application);
    }

    private RecoverPasswordActivity createActivity() {
        Intent startIntent = RecoverPasswordActivity.createIntent(
                RuntimeEnvironment.application,
                TestHelper.getFlowParameters(Collections.<String>emptyList()),
                TestConstants.EMAIL);
        return Robolectric.buildActivity(RecoverPasswordActivity.class).withIntent(startIntent)
                .create().visible().get();
    }

    @Test
    @Config(shadows = {BaseHelperShadow.class, ActivityHelperShadow.class})
    public void testNextButton_sendsEmail() {
        RecoverPasswordActivity recoverPasswordActivity = createActivity();
        Button nextButton = (Button) recoverPasswordActivity.findViewById(R.id.button_done);
        when(ActivityHelperShadow.sFirebaseAuth.sendPasswordResetEmail(TestConstants.EMAIL))
                .thenReturn(new AutoCompleteTask<Void>(null, true, null));
        nextButton.performClick();
        verify(ActivityHelperShadow.sFirebaseAuth).sendPasswordResetEmail(TestConstants.EMAIL);
    }
}
