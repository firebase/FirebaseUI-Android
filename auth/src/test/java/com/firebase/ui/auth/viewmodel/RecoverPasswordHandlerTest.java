package com.firebase.ui.auth.viewmodel;

import android.arch.lifecycle.Observer;

import com.firebase.ui.auth.data.model.FlowParameters;
import com.firebase.ui.auth.data.model.Resource;
import com.firebase.ui.auth.testhelpers.AutoCompleteTask;
import com.firebase.ui.auth.testhelpers.ResourceMatchers;
import com.firebase.ui.auth.testhelpers.TestConstants;
import com.firebase.ui.auth.testhelpers.TestHelper;
import com.firebase.ui.auth.viewmodel.email.RecoverPasswordHandler;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link RecoverPasswordHandler}.
 */
@RunWith(RobolectricTestRunner.class)
public class RecoverPasswordHandlerTest {

    @Mock FirebaseAuth mMockAuth;
    @Mock Observer<Resource<String>> mObserver;

    private RecoverPasswordHandler mHandler;

    @Before
    public void setUp() {
        TestHelper.initialize();
        MockitoAnnotations.initMocks(this);

        mHandler = new RecoverPasswordHandler(RuntimeEnvironment.application);

        FlowParameters testParams = TestHelper.getFlowParameters(Collections.singletonList(
                EmailAuthProvider.PROVIDER_ID));
        mHandler.initializeForTesting(testParams, mMockAuth, null, null);
    }

    @Test
    public void testReset_sendsRecoverEmail() {
        // Send password email succeeds
        when(mMockAuth.sendPasswordResetEmail(TestConstants.EMAIL))
                .thenReturn(AutoCompleteTask.<Void>forSuccess(null));

        // Begin observation, then send the email
        mHandler.getOperation().observeForever(mObserver);
        mHandler.startReset(TestConstants.EMAIL);

        // Should get in-progress resource
        verify(mObserver).onChanged(argThat(ResourceMatchers.<String>isLoading()));

        // Firebase auth should be called
        verify(mMockAuth).sendPasswordResetEmail(TestConstants.EMAIL);

        // Should get the success resource
        verify(mObserver).onChanged(argThat(ResourceMatchers.<String>isSuccess()));
    }

    @Test
    public void testReset_propagatesFailure() {
        // Send password email fails
        when(mMockAuth.sendPasswordResetEmail(TestConstants.EMAIL))
                .thenReturn(AutoCompleteTask.<Void>forFailure(new Exception("FAILED")));

        // Begin observation, then send the email
        mHandler.getOperation().observeForever(mObserver);
        mHandler.startReset(TestConstants.EMAIL);

        // Should get in-progress resource
        verify(mObserver).onChanged(argThat(ResourceMatchers.<String>isLoading()));

        // Firebase auth should be called
        verify(mMockAuth).sendPasswordResetEmail(TestConstants.EMAIL);

        // Should get the success resource
        verify(mObserver).onChanged(argThat(ResourceMatchers.<String>isFailure()));
    }

}
