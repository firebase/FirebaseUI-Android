package com.firebase.ui.auth.viewmodel;


import android.arch.lifecycle.Observer;

import com.firebase.ui.auth.FirebaseAuthAnonymousUpgradeException;
import com.firebase.ui.auth.IdpResponse;
import com.firebase.ui.auth.data.model.FlowParameters;
import com.firebase.ui.auth.data.model.Resource;
import com.firebase.ui.auth.data.model.User;
import com.firebase.ui.auth.testhelpers.AutoCompleteTask;
import com.firebase.ui.auth.testhelpers.FakeAuthResult;
import com.firebase.ui.auth.testhelpers.ResourceMatchers;
import com.firebase.ui.auth.testhelpers.TestConstants;
import com.firebase.ui.auth.testhelpers.TestHelper;
import com.firebase.ui.auth.viewmodel.phone.PhoneProviderResponseHandler;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthCredential;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthProvider;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.util.Collections;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link PhoneProviderResponseHandler}.
 */
@RunWith(RobolectricTestRunner.class)
public class PhoneProviderResponseHandlerTest {

    @Mock FirebaseAuth mMockAuth;
    @Mock FirebaseUser mMockUser;
    @Mock PhoneAuthCredential mCredential;
    @Mock Observer<Resource<IdpResponse>> mResponseObserver;

    private PhoneProviderResponseHandler mHandler;

    @Before
    public void setUp() {
        TestHelper.initialize();
        MockitoAnnotations.initMocks(this);

        mHandler = new PhoneProviderResponseHandler(RuntimeEnvironment.application);
        FlowParameters testParams = TestHelper.getFlowParameters(Collections.singletonList(
                PhoneAuthProvider.PROVIDER_ID));
        mHandler.initializeForTesting(testParams, mMockAuth, null, null);
    }

    @Test
    public void testSignIn_withValidCredentialAndNewUser_expectSuccess() {
        mHandler.getOperation().observeForever(mResponseObserver);

        when(mMockAuth.signInWithCredential(mCredential))
                .thenReturn(AutoCompleteTask.forSuccess(FakeAuthResult.INSTANCE));

        IdpResponse response = new IdpResponse.Builder(new User.Builder(
                PhoneAuthProvider.PROVIDER_ID, TestConstants.EMAIL).build())
                .build();

        mHandler.startSignIn(mCredential, response);
        verify(mMockAuth).signInWithCredential(mCredential);
        verify(mResponseObserver).onChanged(argThat(ResourceMatchers.<IdpResponse>isSuccess()));
    }

    @Test
    public void testSignIn_autoUpgradeAnonymousEnabledWithNewUser_expectSuccess() {
        mHandler.getOperation().observeForever(mResponseObserver);
        setupAnonymousUpgrade();

        when(mMockAuth.getCurrentUser().linkWithCredential(mCredential))
                .thenReturn(AutoCompleteTask.forSuccess(FakeAuthResult.INSTANCE));

        IdpResponse response = new IdpResponse.Builder(new User.Builder(
                PhoneAuthProvider.PROVIDER_ID, TestConstants.EMAIL).build())
                .build();

        mHandler.startSignIn(mCredential, response);

        verify(mMockAuth.getCurrentUser()).linkWithCredential(mCredential);
        verify(mResponseObserver).onChanged(argThat(ResourceMatchers.<IdpResponse>isSuccess()));
    }


    @Test
    public void testSignIn_autoUpgradeAnonymousEnabledWithExistingUser_expectMergeFailure() {
        mHandler.getOperation().observeForever(mResponseObserver);
        setupAnonymousUpgrade();

        FirebaseAuthUserCollisionException ex =
                new FirebaseAuthUserCollisionException("foo", "bar");
        TestHelper.setPrivateField(ex, FirebaseAuthUserCollisionException.class,
                AuthCredential.class, mCredential);

        when(mMockAuth.getCurrentUser().linkWithCredential(mCredential))
                .thenReturn(AutoCompleteTask.<AuthResult>forFailure(ex));

        IdpResponse response = new IdpResponse.Builder(new User.Builder(
                PhoneAuthProvider.PROVIDER_ID, TestConstants.EMAIL).build())
                .build();

        mHandler.startSignIn(mCredential, response);

        verify(mMockAuth.getCurrentUser()).linkWithCredential(mCredential);

        InOrder inOrder = inOrder(mResponseObserver);
        inOrder.verify(mResponseObserver)
                .onChanged(argThat(ResourceMatchers.<IdpResponse>isLoading()));

        ArgumentCaptor<Resource<IdpResponse>> resolveCaptor =
                ArgumentCaptor.forClass(Resource.class);
        inOrder.verify(mResponseObserver).onChanged(resolveCaptor.capture());

        FirebaseAuthAnonymousUpgradeException e =
                (FirebaseAuthAnonymousUpgradeException) resolveCaptor.getValue().getException();

        assertThat(e.getResponse().getCredentialForLinking()).isNotNull();
    }

    private void setupAnonymousUpgrade() {
        FlowParameters testParams = TestHelper.getFlowParameters(Collections.singletonList(
                PhoneAuthProvider.PROVIDER_ID), true);
        mHandler.initializeForTesting(testParams, mMockAuth, null, null);
        when(mMockAuth.getCurrentUser()).thenReturn(mMockUser);
        when(mMockUser.isAnonymous()).thenReturn(true);
    }
}
