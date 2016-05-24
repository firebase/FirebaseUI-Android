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

import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;
import static org.mockito.Mockito.when;

import com.firebase.ui.auth.BuildConfig;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.firebase.auth.FirebaseAuth;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(RobolectricGradleTestRunner.class)
@Config(constants = BuildConfig.class)
public class HeadlessAPIWrapperImplTest {

    private FirebaseAuthWrapperImpl mHeadlessAPIWrapperImpl;

    @Mock
    private FirebaseAuth mMockFirebaseAuth;
    @Mock
    private GoogleApiAvailability mMockGoogleApiAvailability;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mHeadlessAPIWrapperImpl = new FirebaseAuthWrapperImpl(mMockFirebaseAuth);
    }

    @Test
    public void testIsPlayServicesAvailable() {
        when(mMockGoogleApiAvailability.isGooglePlayServicesAvailable(
                RuntimeEnvironment.application))
                .thenReturn(ConnectionResult.SERVICE_UPDATING);
        assertFalse(mHeadlessAPIWrapperImpl.isPlayServicesAvailable(
                RuntimeEnvironment.application, mMockGoogleApiAvailability));
        when(mMockGoogleApiAvailability.isGooglePlayServicesAvailable(
                RuntimeEnvironment.application))
                .thenReturn(ConnectionResult.SERVICE_MISSING);
        assertFalse(mHeadlessAPIWrapperImpl.isPlayServicesAvailable(
                RuntimeEnvironment.application, mMockGoogleApiAvailability));
        when(mMockGoogleApiAvailability.isGooglePlayServicesAvailable(
                RuntimeEnvironment.application))
                .thenReturn(ConnectionResult.SERVICE_DISABLED);
        assertFalse(mHeadlessAPIWrapperImpl.isPlayServicesAvailable(
                RuntimeEnvironment.application, mMockGoogleApiAvailability));
        when(mMockGoogleApiAvailability.isGooglePlayServicesAvailable(
                RuntimeEnvironment.application))
                .thenReturn(ConnectionResult.SERVICE_INVALID);
        assertFalse(mHeadlessAPIWrapperImpl.isPlayServicesAvailable(
                RuntimeEnvironment.application, mMockGoogleApiAvailability));
        when(mMockGoogleApiAvailability.isGooglePlayServicesAvailable(
                RuntimeEnvironment.application))
                .thenReturn(ConnectionResult.SERVICE_VERSION_UPDATE_REQUIRED);
        assertTrue(mHeadlessAPIWrapperImpl.isPlayServicesAvailable(
                RuntimeEnvironment.application, mMockGoogleApiAvailability));
        when(mMockGoogleApiAvailability.isGooglePlayServicesAvailable(
                RuntimeEnvironment.application))
                .thenReturn(ConnectionResult.SUCCESS);
        assertTrue(mHeadlessAPIWrapperImpl.isPlayServicesAvailable(
                RuntimeEnvironment.application, mMockGoogleApiAvailability));
    }
}
