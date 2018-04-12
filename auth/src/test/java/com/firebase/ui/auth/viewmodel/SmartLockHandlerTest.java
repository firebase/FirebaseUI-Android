package com.firebase.ui.auth.viewmodel;

import android.app.Activity;
import android.arch.lifecycle.Observer;

import com.firebase.ui.auth.IdpResponse;
import com.firebase.ui.auth.data.model.FlowParameters;
import com.firebase.ui.auth.data.model.PendingIntentRequiredException;
import com.firebase.ui.auth.data.model.Resource;
import com.firebase.ui.auth.data.model.User;
import com.firebase.ui.auth.testhelpers.AutoCompleteTask;
import com.firebase.ui.auth.testhelpers.ResourceMatchers;
import com.firebase.ui.auth.testhelpers.TestConstants;
import com.firebase.ui.auth.testhelpers.TestHelper;
import com.firebase.ui.auth.viewmodel.smartlock.SmartLockHandler;
import com.google.android.gms.auth.api.credentials.Credential;
import com.google.android.gms.auth.api.credentials.CredentialsClient;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;

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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link SmartLockHandler}.
 */
@RunWith(RobolectricTestRunner.class)
public class SmartLockHandlerTest {

    @Mock FirebaseAuth mMockAuth;
    @Mock CredentialsClient mMockCredentials;
    @Mock Observer<Resource<IdpResponse>> mResultObserver;

    private SmartLockHandler mHandler;

    @Before
    public void setUp() {
        TestHelper.initialize();
        MockitoAnnotations.initMocks(this);

        mHandler = new SmartLockHandler(RuntimeEnvironment.application);

        FlowParameters testParams = TestHelper.getFlowParameters(Collections.singletonList(
                EmailAuthProvider.PROVIDER_ID));

        mHandler.initializeForTesting(testParams, mMockAuth, mMockCredentials, null);
        mHandler.setResponse(new IdpResponse.Builder(
                new User.Builder(EmailAuthProvider.PROVIDER_ID, TestConstants.EMAIL).build()
        ).build());
    }

    @Test
    public void testSaveCredentials_success() {
        mHandler.getOperation().observeForever(mResultObserver);

        when(mMockCredentials.save(any(Credential.class)))
                .thenReturn(AutoCompleteTask.<Void>forSuccess(null));

        mHandler.saveCredentials(TestHelper.getMockFirebaseUser(), TestConstants.PASSWORD, null);

        verify(mResultObserver).onChanged(argThat(ResourceMatchers.<IdpResponse>isLoading()));
        verify(mResultObserver).onChanged(argThat(ResourceMatchers.<IdpResponse>isSuccess()));
    }

    @Test
    public void testSaveCredentials_resolution() {
        mHandler.getOperation().observeForever(mResultObserver);

        // Mock credentials to throw an RAE
        ResolvableApiException mockRae = mock(ResolvableApiException.class);
        when(mMockCredentials.save(any(Credential.class)))
                .thenReturn(AutoCompleteTask.<Void>forFailure(mockRae));

        // Kick off save
        mHandler.saveCredentials(TestHelper.getMockFirebaseUser(), TestConstants.PASSWORD, null);

        InOrder inOrder = inOrder(mResultObserver);

        inOrder.verify(mResultObserver)
                .onChanged(argThat(ResourceMatchers.<IdpResponse>isLoading()));

        // Make sure we get a resolution
        ArgumentCaptor<Resource<IdpResponse>> resolveCaptor =
                ArgumentCaptor.forClass(Resource.class);
        inOrder.verify(mResultObserver).onChanged(resolveCaptor.capture());

        // Call activity result
        PendingIntentRequiredException e =
                ((PendingIntentRequiredException) resolveCaptor.getValue().getException());
        mHandler.onActivityResult(e.getRequestCode(), Activity.RESULT_OK);

        // Make sure we get success
        inOrder.verify(mResultObserver)
                .onChanged(argThat(ResourceMatchers.<IdpResponse>isSuccess()));
    }

    @Test
    public void testSaveCredentials_failure() {
        mHandler.getOperation().observeForever(mResultObserver);

        when(mMockCredentials.save(any(Credential.class)))
                .thenReturn(AutoCompleteTask.<Void>forFailure(new Exception("FAILED")));

        mHandler.saveCredentials(TestHelper.getMockFirebaseUser(), TestConstants.PASSWORD, null);

        verify(mResultObserver).onChanged(argThat(ResourceMatchers.<IdpResponse>isLoading()));
        verify(mResultObserver).onChanged(argThat(ResourceMatchers.<IdpResponse>isFailure()));
    }

}
