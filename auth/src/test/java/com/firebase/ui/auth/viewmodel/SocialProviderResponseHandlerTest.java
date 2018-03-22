package com.firebase.ui.auth.viewmodel;

import android.app.Activity;
import android.arch.lifecycle.Observer;

import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.IdpResponse;
import com.firebase.ui.auth.data.model.FlowParameters;
import com.firebase.ui.auth.data.model.IntentRequiredException;
import com.firebase.ui.auth.data.model.Resource;
import com.firebase.ui.auth.data.model.User;
import com.firebase.ui.auth.testhelpers.AutoCompleteTask;
import com.firebase.ui.auth.testhelpers.FakeAuthResult;
import com.firebase.ui.auth.testhelpers.FakeProviderQueryResult;
import com.firebase.ui.auth.testhelpers.ResourceMatchers;
import com.firebase.ui.auth.testhelpers.TestConstants;
import com.firebase.ui.auth.testhelpers.TestHelper;
import com.firebase.ui.auth.viewmodel.idp.SocialProviderResponseHandler;
import com.firebase.ui.auth.viewmodel.smartlock.SmartLockHandler;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FacebookAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.auth.ProviderQueryResult;
import com.google.firebase.auth.UserProfileChangeRequest;

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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link SmartLockHandler}.
 */
@RunWith(RobolectricTestRunner.class)
public class SocialProviderResponseHandlerTest {
    @Mock FirebaseAuth mMockAuth;
    @Mock Observer<Resource<IdpResponse>> mResultObserver;

    private SocialProviderResponseHandler mHandler;

    @Before
    public void setUp() {
        TestHelper.initialize();
        MockitoAnnotations.initMocks(this);

        mHandler = new SocialProviderResponseHandler(RuntimeEnvironment.application);

        FlowParameters testParams = TestHelper.getFlowParameters(AuthUI.SUPPORTED_PROVIDERS);

        mHandler.initializeForTesting(testParams, mMockAuth, null, null);
    }

    @Test
    public void testSignInIdp_success() {
        mHandler.getOperation().observeForever(mResultObserver);

        when(mMockAuth.signInWithCredential(any(AuthCredential.class)))
                .thenReturn(AutoCompleteTask.forSuccess(FakeAuthResult.INSTANCE));
        when(FakeAuthResult.INSTANCE.getUser().updateProfile(any(UserProfileChangeRequest.class)))
                .thenReturn(AutoCompleteTask.<Void>forSuccess(null));

        IdpResponse response = new IdpResponse.Builder(new User.Builder(
                GoogleAuthProvider.PROVIDER_ID, TestConstants.EMAIL).build())
                .setToken(TestConstants.TOKEN)
                .build();

        mHandler.startSignIn(response);

        InOrder inOrder = inOrder(mResultObserver);

        inOrder.verify(mResultObserver)
                .onChanged(argThat(ResourceMatchers.<IdpResponse>isLoading()));
        inOrder.verify(mResultObserver)
                .onChanged(argThat(ResourceMatchers.<IdpResponse>isSuccess()));
    }

    @Test
    public void testSignInNonIdp_success() {
        mHandler.getOperation().observeForever(mResultObserver);

        IdpResponse response = new IdpResponse.Builder(new User.Builder(
                EmailAuthProvider.PROVIDER_ID, TestConstants.EMAIL).build())
                .build();

        mHandler.startSignIn(response);

        verify(mResultObserver).onChanged(argThat(ResourceMatchers.<IdpResponse>isSuccess()));
    }

    @Test
    public void testSignInResponse_failure() {
        mHandler.getOperation().observeForever(mResultObserver);

        IdpResponse response = IdpResponse.fromError(new Exception("Failure"));

        mHandler.startSignIn(response);

        verify(mResultObserver).onChanged(argThat(ResourceMatchers.<IdpResponse>isFailure()));
    }

    @Test
    public void testSignInIdp_resolution() {
        mHandler.getOperation().observeForever(mResultObserver);

        when(mMockAuth.signInWithCredential(any(AuthCredential.class)))
                .thenReturn(AutoCompleteTask.<AuthResult>forFailure(
                        new FirebaseAuthUserCollisionException("foo", "bar")));
        when(mMockAuth.fetchProvidersForEmail(any(String.class)))
                .thenReturn(AutoCompleteTask.<ProviderQueryResult>forSuccess(
                        new FakeProviderQueryResult(Collections.singletonList(
                                FacebookAuthProvider.PROVIDER_ID))));

        IdpResponse response = new IdpResponse.Builder(new User.Builder(
                GoogleAuthProvider.PROVIDER_ID, TestConstants.EMAIL).build())
                .setToken(TestConstants.TOKEN)
                .build();

        mHandler.startSignIn(response);

        InOrder inOrder = inOrder(mResultObserver);

        inOrder.verify(mResultObserver)
                .onChanged(argThat(ResourceMatchers.<IdpResponse>isLoading()));

        ArgumentCaptor<Resource<IdpResponse>> resolveCaptor =
                ArgumentCaptor.forClass(Resource.class);
        inOrder.verify(mResultObserver).onChanged(resolveCaptor.capture());

        // Call activity result
        IntentRequiredException e =
                ((IntentRequiredException) resolveCaptor.getValue().getException());
        mHandler.onActivityResult(e.getRequestCode(), Activity.RESULT_OK, response.toIntent());

        // Make sure we get success
        inOrder.verify(mResultObserver)
                .onChanged(argThat(ResourceMatchers.<IdpResponse>isSuccess()));
    }
}
