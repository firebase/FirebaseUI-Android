package com.firebase.ui.auth.viewmodel;

import android.arch.lifecycle.Observer;

import com.firebase.ui.auth.IdpResponse;
import com.firebase.ui.auth.data.model.FlowParameters;
import com.firebase.ui.auth.data.model.Resource;
import com.firebase.ui.auth.data.model.State;
import com.firebase.ui.auth.data.remote.AnonymousSignInHandler;
import com.firebase.ui.auth.testhelpers.AutoCompleteTask;
import com.firebase.ui.auth.testhelpers.FakeAuthResult;
import com.firebase.ui.auth.testhelpers.ResourceMatchers;
import com.firebase.ui.auth.testhelpers.TestHelper;
import com.google.firebase.auth.AuthResult;
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

import java.util.ArrayList;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link AnonymousSignInHandler}.
 */
@RunWith(RobolectricTestRunner.class)
public class AnonymousSignInHandlerTest {

    @Mock FirebaseAuth mMockAuth;
    AnonymousSignInHandler mHandler;

    @Mock Observer<Resource<IdpResponse>> mResponseObserver;

    @Before
    public void setUp() {
        TestHelper.initialize();
        MockitoAnnotations.initMocks(this);

        mHandler = new AnonymousSignInHandler(RuntimeEnvironment.application);
        FlowParameters testParams = TestHelper.getFlowParameters(new ArrayList<String>());
        mHandler.init(testParams);
        mHandler.mAuth = mMockAuth;
    }

    @Test
    public void testStartSignIn_expectSuccess() {
        mHandler.getOperation().observeForever(mResponseObserver);

        when(mMockAuth.signInAnonymously())
                .thenReturn(AutoCompleteTask.forSuccess(FakeAuthResult.INSTANCE));

        mHandler.startSignIn(null);

        verify(mMockAuth).signInAnonymously();

        InOrder inOrder = inOrder(mResponseObserver);
        inOrder.verify(mResponseObserver)
                .onChanged(argThat(ResourceMatchers.<IdpResponse>isLoading()));

        ArgumentCaptor<Resource<IdpResponse>> captor = ArgumentCaptor.forClass(Resource.class);
        inOrder.verify(mResponseObserver).onChanged(captor.capture());

        assertThat(captor.getValue().getState()).isEqualTo(State.SUCCESS);
        IdpResponse response = captor.getValue().getValue();
        assertThat(response.isNewUser()).isFalse();
    }

    @Test
    public void testStartSignIn_expectFailure() {
        mHandler.getOperation().observeForever(mResponseObserver);

        when(mMockAuth.signInAnonymously())
                .thenReturn(AutoCompleteTask.<AuthResult>forFailure(new Exception("FAILED")));

        mHandler.startSignIn(null);

        verify(mMockAuth).signInAnonymously();

        InOrder inOrder = inOrder(mResponseObserver);
        inOrder.verify(mResponseObserver)
                .onChanged(argThat(ResourceMatchers.<IdpResponse>isLoading()));
        inOrder.verify(mResponseObserver)
                .onChanged(argThat(ResourceMatchers.<IdpResponse>isFailure()));

    }
}
