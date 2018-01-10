package com.firebase.ui.auth.viewmodel;

import android.arch.lifecycle.Observer;

import com.firebase.ui.auth.IdpResponse;
import com.firebase.ui.auth.data.model.FlowParameters;
import com.firebase.ui.auth.data.model.Resource;
import com.firebase.ui.auth.data.model.User;
import com.firebase.ui.auth.testhelpers.AutoCompleteTask;
import com.firebase.ui.auth.testhelpers.AutoContinueTask;
import com.firebase.ui.auth.testhelpers.FakeAuthResult;
import com.firebase.ui.auth.testhelpers.ResourceMatchers;
import com.firebase.ui.auth.testhelpers.TestConstants;
import com.firebase.ui.auth.testhelpers.TestHelper;
import com.firebase.ui.auth.viewmodel.email.WelcomeBackPasswordHandler;
import com.google.android.gms.auth.api.credentials.Credential;
import com.google.android.gms.auth.api.credentials.CredentialsClient;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FacebookAuthProvider;
import com.google.firebase.auth.FirebaseAuth;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.util.Collections;

import static junit.framework.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link WelcomeBackPasswordHandler}.
 */
@RunWith(RobolectricTestRunner.class)
public class WelcomeBackPasswordHandlerTest {

    @Mock FirebaseAuth mMockAuth;
    @Mock CredentialsClient mMockCredentials;

    @Mock Observer<Resource<IdpResponse>> mResponseObserver;

    private WelcomeBackPasswordHandler mHandler;

    @Before
    public void setUp() {
        TestHelper.initialize();
        MockitoAnnotations.initMocks(this);

        mHandler = new WelcomeBackPasswordHandler(RuntimeEnvironment.application);

        FlowParameters testParams = TestHelper.getFlowParameters(Collections.singletonList(
                EmailAuthProvider.PROVIDER_ID));
        mHandler.initializeForTesting(testParams, mMockAuth, mMockCredentials, null);
    }

    @Test
    public void testSignIn_signsInAndSavesCredentials() {
        mHandler.getSignInResult().observeForever(mResponseObserver);

        // Mock sign in to always succeed
        when(mMockAuth.signInWithEmailAndPassword(TestConstants.EMAIL, TestConstants.PASSWORD))
                .thenReturn(new AutoCompleteTask<>(FakeAuthResult.INSTANCE, true, null));

        // Mock smartlock save to always succeed
        when(mMockCredentials.save(any(Credential.class)))
                .thenReturn(new AutoCompleteTask<Void>(null, true, null));

        // Kick off the sign in flow
        mHandler.startSignIn(TestConstants.EMAIL, TestConstants.PASSWORD, null, null);

        // Verify that we get a loading event
        verify(mResponseObserver).onChanged(argThat(ResourceMatchers.<IdpResponse>isLoading()));

        // Verify that sign in is called with the right arguments
        verify(mMockAuth).signInWithEmailAndPassword(
                TestConstants.EMAIL, TestConstants.PASSWORD);

        // Verify that a matching credential is saved in SmartLock
        ArgumentCaptor<Credential> credentialCaptor = ArgumentCaptor.forClass(Credential.class);
        verify(mMockCredentials).save(credentialCaptor.capture());

        Credential captured = credentialCaptor.getValue();
        assertEquals(captured.getId(), TestConstants.EMAIL);
        assertEquals(captured.getPassword(), TestConstants.PASSWORD);

        // Verify that we get a success event
        verify(mResponseObserver).onChanged(argThat(ResourceMatchers.<IdpResponse>isSuccess()));
    }

    @Test
    public void testSignIn_linksIdpCredential() {
        mHandler.getSignInResult().observeForever(mResponseObserver);

        // Fake social response from Facebook
        User user = new User.Builder(FacebookAuthProvider.PROVIDER_ID, TestConstants.EMAIL)
                .build();

        IdpResponse response = new IdpResponse.Builder(user)
                .setToken(TestConstants.TOKEN)
                .setSecret(TestConstants.SECRET)
                .build();

        AuthCredential credential = FacebookAuthProvider.getCredential(TestConstants.TOKEN);

        // Mock sign in to always succeed
        when(mMockAuth.signInWithEmailAndPassword(TestConstants.EMAIL, TestConstants.PASSWORD))
                .thenReturn(new AutoCompleteTask<>(FakeAuthResult.INSTANCE, true, null));

        // Mock linking to always succeed
        when(FakeAuthResult.INSTANCE.getUser().linkWithCredential(credential))
                .thenReturn(new AutoContinueTask<>(FakeAuthResult.INSTANCE,
                        FakeAuthResult.INSTANCE,
                        true,
                        null));

        // Mock smartlock save to always succeed
        when(mMockCredentials.save(any(Credential.class)))
                .thenReturn(new AutoCompleteTask<Void>(null, true, null));

        // Kick off the sign in flow
        mHandler.startSignIn(TestConstants.EMAIL, TestConstants.PASSWORD, response, credential);

        // Verify that we get a loading event
        verify(mResponseObserver).onChanged(argThat(ResourceMatchers.<IdpResponse>isLoading()));

        // Verify that sign in is called with the right arguments
        verify(mMockAuth).signInWithEmailAndPassword(
                TestConstants.EMAIL, TestConstants.PASSWORD);

        // Verify that account linking is attempted
        verify(FakeAuthResult.INSTANCE.getUser()).linkWithCredential(credential);

        // Verify that we get a success event
        verify(mResponseObserver).onChanged(argThat(ResourceMatchers.<IdpResponse>isSuccess()));
    }
}
