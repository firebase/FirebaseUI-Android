package com.firebase.ui.auth.api;

import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;
import static org.mockito.Mockito.when;


import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.firebase.ui.auth.BuildConfig;
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

    private HeadlessAPIWrapperImpl mHeadlessAPIWrapperImpl;

    @Mock
    private FirebaseAuth mMockFirebaseAuth;
    @Mock
    private GoogleApiAvailability mMockGoogleApiAvailability;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mHeadlessAPIWrapperImpl = new HeadlessAPIWrapperImpl(mMockFirebaseAuth);
    }

    @Test
    public void testIsGMSCorePresent() {
    when(mMockGoogleApiAvailability.isGooglePlayServicesAvailable(
            RuntimeEnvironment.application))
            .thenReturn(ConnectionResult.SERVICE_UPDATING);
    assertFalse(mHeadlessAPIWrapperImpl.isGMSCorePresent(
            RuntimeEnvironment.application,mMockGoogleApiAvailability));
    when(mMockGoogleApiAvailability.isGooglePlayServicesAvailable(
            RuntimeEnvironment.application))
            .thenReturn(ConnectionResult.SERVICE_MISSING);
    assertFalse(mHeadlessAPIWrapperImpl.isGMSCorePresent(
            RuntimeEnvironment.application,mMockGoogleApiAvailability));
    when(mMockGoogleApiAvailability.isGooglePlayServicesAvailable(
            RuntimeEnvironment.application))
            .thenReturn(ConnectionResult.SERVICE_DISABLED);
    assertFalse(mHeadlessAPIWrapperImpl.isGMSCorePresent(
            RuntimeEnvironment.application,mMockGoogleApiAvailability));
    when(mMockGoogleApiAvailability.isGooglePlayServicesAvailable(
            RuntimeEnvironment.application))
            .thenReturn(ConnectionResult.SERVICE_INVALID);
    assertFalse(mHeadlessAPIWrapperImpl.isGMSCorePresent(
            RuntimeEnvironment.application,mMockGoogleApiAvailability));
    when(mMockGoogleApiAvailability.isGooglePlayServicesAvailable(
            RuntimeEnvironment.application))
            .thenReturn(ConnectionResult.SERVICE_VERSION_UPDATE_REQUIRED);
    assertTrue(mHeadlessAPIWrapperImpl.isGMSCorePresent(
            RuntimeEnvironment.application,mMockGoogleApiAvailability));
    when(mMockGoogleApiAvailability.isGooglePlayServicesAvailable(
            RuntimeEnvironment.application))
            .thenReturn(ConnectionResult.SUCCESS);
    assertTrue(mHeadlessAPIWrapperImpl.isGMSCorePresent(
            RuntimeEnvironment.application,mMockGoogleApiAvailability));
    }
}
