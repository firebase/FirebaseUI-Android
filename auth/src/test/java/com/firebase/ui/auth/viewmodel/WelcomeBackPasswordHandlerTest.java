package com.firebase.ui.auth.viewmodel;

import android.app.Application;
import android.os.Looper;

import com.firebase.ui.auth.FirebaseAuthAnonymousUpgradeException;
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
import com.firebase.ui.auth.util.data.AuthOperationManager;
import com.firebase.ui.auth.viewmodel.email.WelcomeBackPasswordHandler;
import com.google.android.gms.auth.api.identity.SignInClient;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FacebookAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.LooperMode;

import java.util.Collections;

import androidx.lifecycle.Observer;
import androidx.test.core.app.ApplicationProvider;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.robolectric.Shadows.shadowOf;

/**
 * Unit tests for {@link WelcomeBackPasswordHandler}.
 */
@RunWith(RobolectricTestRunner.class)
@LooperMode(LooperMode.Mode.PAUSED)
public class WelcomeBackPasswordHandlerTest {

    @Mock FirebaseAuth mMockAuth;
    @Mock FirebaseAuth mScratchMockAuth;
    @Mock FirebaseUser mUser;
    @Mock SignInClient mMockSignInClient;
    @Mock Observer<Resource<IdpResponse>> mResponseObserver;

    private WelcomeBackPasswordHandler mHandler;

    @Before
    public void setUp() {
        TestHelper.initialize();
        MockitoAnnotations.initMocks(this);

        mHandler = new WelcomeBackPasswordHandler(
                (Application) ApplicationProvider.getApplicationContext());

        FlowParameters testParams = TestHelper.getFlowParameters(
                Collections.singletonList(EmailAuthProvider.PROVIDER_ID));
        mHandler.initializeForTesting(testParams, mMockAuth, mMockSignInClient);
    }

    @Test
    public void testSignIn_linksIdpCredential() {
        mHandler.getOperation().observeForever(mResponseObserver);

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
                .thenReturn(AutoCompleteTask.forSuccess(FakeAuthResult.INSTANCE));

        // Mock linking to always succeed
        when(FakeAuthResult.INSTANCE.getUser().linkWithCredential(credential))
                .thenReturn(new AutoContinueTask<>(FakeAuthResult.INSTANCE,
                        FakeAuthResult.INSTANCE,
                        true,
                        null));

        // NOTE: The previous smart lock save stub has been removed because the new API
        // no longer supports saving a Credential via SignInClient.

        // Kick off the sign in flow
        mHandler.startSignIn(TestConstants.EMAIL, TestConstants.PASSWORD, response, credential);
        shadowOf(Looper.getMainLooper()).idle();

        // Verify that we get a loading event
        verify(mResponseObserver).onChanged(argThat(ResourceMatchers.isLoading()));

        // Verify that sign in is called with the right arguments
        verify(mMockAuth).signInWithEmailAndPassword(TestConstants.EMAIL, TestConstants.PASSWORD);

        // Verify that account linking is attempted
        verify(FakeAuthResult.INSTANCE.getUser()).linkWithCredential(credential);

        // Verify that we get a success event
        verify(mResponseObserver).onChanged(argThat(ResourceMatchers.isSuccess()));
    }

    @Test
    public void testSignIn_propagatesFailure() {
        mHandler.getOperation().observeForever(mResponseObserver);

        // Mock sign in to always fail
        when(mMockAuth.signInWithEmailAndPassword(any(String.class), any(String.class)))
                .thenReturn(AutoContinueTask.forFailure(new Exception("FAILED")));

        // Kick off the sign in flow
        mHandler.startSignIn(TestConstants.EMAIL, TestConstants.PASSWORD, null, null);
        shadowOf(Looper.getMainLooper()).idle();

        // Verify that we get a loading event
        verify(mResponseObserver).onChanged(argThat(ResourceMatchers.isLoading()));

        // Verify that sign in is called with the right arguments
        verify(mMockAuth).signInWithEmailAndPassword(TestConstants.EMAIL, TestConstants.PASSWORD);

        // Verify that we get a failure event
        verify(mResponseObserver).onChanged(argThat(ResourceMatchers.isFailure()));
    }

    @Test
    public void testSignIn_anonymousUserUpgradeEnabledWithEmailFlow_expectValidateCredentialAndMergeFailure() {
        setupAnonymousUpgrade();
        mHandler.getOperation().observeForever(mResponseObserver);

        // Fake user with password provider
        User user = new User.Builder(EmailAuthProvider.PROVIDER_ID, TestConstants.EMAIL)
                .build();
        IdpResponse response = new IdpResponse.Builder(user).build();

        // Control FirebaseAuth's return values
        AuthOperationManager authOperationManager = AuthOperationManager.getInstance();
        authOperationManager.mScratchAuth = mScratchMockAuth;

        when(mScratchMockAuth.signInWithCredential(any(AuthCredential.class)))
                .thenReturn(AutoCompleteTask.forSuccess(FakeAuthResult.INSTANCE));

        // Kick off the sign in flow
        mHandler.startSignIn(TestConstants.EMAIL, TestConstants.PASSWORD, response, null);

        // Verify that signInWithCredential was called
        ArgumentCaptor<EmailAuthCredential> credentialCaptor =
                ArgumentCaptor.forClass(EmailAuthCredential.class);
        verify(mScratchMockAuth).signInWithCredential(credentialCaptor.capture());

        EmailAuthCredential capturedCredential = credentialCaptor.getValue();

        // TODO: EmailAuthCredential no longer exposes .getEmail() or .getPassword()
        // assertThat(capturedCredential.getEmail()).isEqualTo(TestConstants.EMAIL);
        // assertThat(capturedCredential.getPassword()).isEqualTo(TestConstants.PASSWORD);

        verifyMergeFailure();
    }

    @Test
    public void testSignIn_anonymousUserUpgradeEnabledWithSocialFlow_expectSafeLinksIdpCredentialAndMergeFailure() {
        setupAnonymousUpgrade();
        mHandler.getOperation().observeForever(mResponseObserver);

        // Fake social response from Facebook
        User user = new User.Builder(FacebookAuthProvider.PROVIDER_ID, TestConstants.EMAIL)
                .build();

        IdpResponse response = new IdpResponse.Builder(user)
                .setToken(TestConstants.TOKEN)
                .setSecret(TestConstants.SECRET)
                .build();

        AuthCredential credential = FacebookAuthProvider.getCredential(TestConstants.TOKEN);

        // Control FirebaseAuth's return values
        AuthOperationManager authOperationManager = AuthOperationManager.getInstance();
        authOperationManager.mScratchAuth = mScratchMockAuth;

        when(mScratchMockAuth.signInWithCredential(any(AuthCredential.class)))
                .thenReturn(AutoCompleteTask.forSuccess(FakeAuthResult.INSTANCE));

        // Mock linking with Facebook to always work
        when(FakeAuthResult.INSTANCE.getUser().linkWithCredential(credential))
                .thenReturn(new AutoContinueTask<>(FakeAuthResult.INSTANCE,
                        FakeAuthResult.INSTANCE,
                        true,
                        null));

        // Kick off the sign in flow
        mHandler.startSignIn(TestConstants.EMAIL, TestConstants.PASSWORD, response, credential);

        // Verify that signInWithCredential was called
        ArgumentCaptor<EmailAuthCredential> credentialCaptor =
                ArgumentCaptor.forClass(EmailAuthCredential.class);
        verify(mScratchMockAuth).signInWithCredential(credentialCaptor.capture());

        EmailAuthCredential capturedCredential = credentialCaptor.getValue();

        // TODO: EmailAuthCredential no longer exposes .getEmail() or .getPassword()
        // assertThat(capturedCredential.getEmail()).isEqualTo(TestConstants.EMAIL);
        // assertThat(capturedCredential.getPassword()).isEqualTo(TestConstants.PASSWORD);

        // Verify that account linking is attempted
        verify(FakeAuthResult.INSTANCE.getUser()).linkWithCredential(credential);

        verifyMergeFailure();
    }

    private void setupAnonymousUpgrade() {
        // enableAnonymousUpgrade must be set to true
        FlowParameters testParams = TestHelper.getFlowParameters(
                Collections.singletonList(EmailAuthProvider.PROVIDER_ID),
                /* enableAnonymousUpgrade */ true);
        mHandler.initializeForTesting(testParams, mMockAuth, mMockSignInClient);

        // Mock isAnonymous() to return true so that anonymous upgrade is enabled
        when(mUser.isAnonymous()).thenReturn(true);
        when(mMockAuth.getCurrentUser()).thenReturn(mUser);
    }

    private void verifyMergeFailure() {
        InOrder inOrder = inOrder(mResponseObserver);

        inOrder.verify(mResponseObserver)
                .onChanged(argThat(ResourceMatchers.isLoading()));

        ArgumentCaptor<Resource<IdpResponse>> resolveCaptor =
                ArgumentCaptor.forClass(Resource.class);
        inOrder.verify(mResponseObserver).onChanged(resolveCaptor.capture());

        FirebaseAuthAnonymousUpgradeException e =
                ((FirebaseAuthAnonymousUpgradeException) resolveCaptor.getValue().getException());

        EmailAuthCredential responseCredential =
                (EmailAuthCredential) e.getResponse().getCredentialForLinking();

        // TODO: EmailAuthCredential no longer exposes .getEmail() or .getPassword()
        // assertThat(responseCredential.getEmail()).isEqualTo(TestConstants.EMAIL);
        // assertThat(responseCredential.getPassword()).isEqualTo(TestConstants.PASSWORD);
    }
}
