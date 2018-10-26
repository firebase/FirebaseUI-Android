package com.firebase.ui.auth.viewmodel;

import android.arch.lifecycle.Observer;

import com.firebase.ui.auth.data.model.FlowParameters;
import com.firebase.ui.auth.data.model.Resource;
import com.firebase.ui.auth.data.model.State;
import com.firebase.ui.auth.testhelpers.AutoCompleteTask;
import com.firebase.ui.auth.testhelpers.ResourceMatchers;
import com.firebase.ui.auth.testhelpers.TestConstants;
import com.firebase.ui.auth.testhelpers.TestHelper;
import com.firebase.ui.auth.util.data.EmailLinkPersistenceManager;
import com.firebase.ui.auth.viewmodel.email.EmailLinkSendEmailHandler;
import com.google.firebase.auth.ActionCodeSettings;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link EmailLinkSendEmailHandler}.
 */
@RunWith(RobolectricTestRunner.class)
public class EmailLinkSendEmailHandlerTest {

    private static String URL = "url";

    private EmailLinkPersistenceManager mPersistenceManager;
    private EmailLinkSendEmailHandler mHandler;

    @Mock private FirebaseAuth mMockAuth;
    @Mock private Observer<Resource<String>> mResponseObserver;

    @Before
    public void setUp() {
        TestHelper.initialize();
        MockitoAnnotations.initMocks(this);

        mHandler = new EmailLinkSendEmailHandler(RuntimeEnvironment.application);
        FlowParameters testParams = TestHelper.getFlowParameters(new ArrayList<String>());

        mHandler.initializeForTesting(testParams, mMockAuth, null, null);
        mPersistenceManager = EmailLinkPersistenceManager.getInstance();
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testSendSignInLinkToEmail_expectSuccess() {
        mHandler.getOperation().observeForever(mResponseObserver);

        ActionCodeSettings actionCodeSettings = ActionCodeSettings.newBuilder().setUrl(URL).build();
        when(mMockAuth.sendSignInLinkToEmail(any(String.class), any(ActionCodeSettings.class)))
                .thenReturn(AutoCompleteTask.<Void>forSuccess(null));

        mHandler.sendSignInLinkToEmail(TestConstants.EMAIL, actionCodeSettings);

        verify(mMockAuth).sendSignInLinkToEmail(eq(TestConstants.EMAIL), eq(actionCodeSettings));

        String email = mPersistenceManager.retrieveSessionRecord(RuntimeEnvironment.application)
                .getEmail();
        assertThat(email).isNotNull();
        assertThat(email).isEqualTo(TestConstants.EMAIL);

        ArgumentCaptor<Resource<String>> captor = ArgumentCaptor.forClass(Resource.class);
        InOrder inOrder = inOrder(mResponseObserver);
        inOrder.verify(mResponseObserver)
                .onChanged(argThat(ResourceMatchers.<String>isLoading()));

        inOrder.verify(mResponseObserver).onChanged(captor.capture());

        assertThat(captor.getValue().getState()).isEqualTo(State.SUCCESS);
        assertThat(captor.getValue().getValue()).isEqualTo(TestConstants.EMAIL);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testSendSignInLinkToEmail_expectFailure() {
        mHandler.getOperation().observeForever(mResponseObserver);

        ActionCodeSettings actionCodeSettings = ActionCodeSettings.newBuilder().setUrl(URL).build();
        when(mMockAuth.sendSignInLinkToEmail(any(String.class), any(ActionCodeSettings.class)))
                .thenReturn(AutoCompleteTask.<Void>forFailure(new Exception()));

        mHandler.sendSignInLinkToEmail(TestConstants.EMAIL, actionCodeSettings);

        verify(mMockAuth).sendSignInLinkToEmail(eq(TestConstants.EMAIL), eq(actionCodeSettings));


        ArgumentCaptor<Resource<String>> captor = ArgumentCaptor.forClass(Resource.class);
        InOrder inOrder = inOrder(mResponseObserver);
        inOrder.verify(mResponseObserver)
                .onChanged(argThat(ResourceMatchers.<String>isLoading()));
        inOrder.verify(mResponseObserver).onChanged(captor.capture());

        assertThat(captor.getValue().getState()).isEqualTo(State.FAILURE);
        assertThat(captor.getValue().getException()).isNotNull();
    }
}
