package com.firebase.ui.auth.viewmodel;

import android.app.Application;

import androidx.lifecycle.Observer;
import androidx.test.core.app.ApplicationProvider;

import com.firebase.ui.auth.IdpResponse;
import com.firebase.ui.auth.data.model.FlowParameters;
import com.firebase.ui.auth.data.model.Resource;
import com.firebase.ui.auth.data.model.User;
import com.firebase.ui.auth.testhelpers.ResourceMatchers;
import com.firebase.ui.auth.testhelpers.TestConstants;
import com.firebase.ui.auth.testhelpers.TestHelper;
import com.firebase.ui.auth.viewmodel.credentialmanager.CredentialManagerHandler;
import com.google.firebase.auth.EmailAuthProvider;
import androidx.activity.ComponentActivity;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.android.controller.ActivityController;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;

/**
 * Unit tests for {@link CredentialManagerHandler}.
 *
 * NOTE: This example shows a minimal approach in Java. If your actual code uses coroutines,
 * you may need a custom setup or workaround to test asynchronous behavior in Java.
 */
@RunWith(RobolectricTestRunner.class)
public class CredentialManagerHandlerTest {

    @Mock
    Observer<Resource<IdpResponse>> mResultObserver;

    // Handler under test
    private CredentialManagerHandler mHandler;

    @Before
    public void setUp() {
        TestHelper.initialize();
        MockitoAnnotations.initMocks(this);

        // Create our handler
        mHandler = new CredentialManagerHandler(
                (Application) ApplicationProvider.getApplicationContext()
        );

        // Provide FlowParameters (e.g., for Email provider)
        FlowParameters testParams = TestHelper.getFlowParameters(
                Collections.singletonList(EmailAuthProvider.PROVIDER_ID)
        );
        mHandler.init(testParams);

        // Supply a default IdpResponse
        User testUser = new User.Builder(EmailAuthProvider.PROVIDER_ID, TestConstants.EMAIL).build();
        IdpResponse testResponse = new IdpResponse.Builder(testUser).build();
        mHandler.setResponse(testResponse);

        // Observe the LiveData so we can verify calls
        mHandler.getOperation().observeForever(mResultObserver);
    }

    @Test
    @Ignore("This test is not possible with CredentialManager")
    public void testSaveCredentials_success() {
        ActivityController<TestActivity> controller = Robolectric.buildActivity(TestActivity.class).setup();
        TestActivity testActivity = controller.get();

        // In a real test, you might inject a mock or fake CredentialManager
        // that returns success. For simplicity, we rely on valid user/password
        // so the Handler will produce a SUCCESS Resource if no exceptions occur.

        // Mock a valid FirebaseUser
        // E.g., if your code checks user.email != null, ensure it's non-null
        // (We won't rely on mMockAuth in this example, but you could if needed.)
        com.google.firebase.auth.FirebaseUser mockUser = TestHelper.getMockFirebaseUser();
        // Make sure .getEmail() is non-null
        org.mockito.Mockito.when(mockUser.getEmail()).thenReturn(TestConstants.EMAIL);

        // Save with a non-null user and password => should produce LOADING -> SUCCESS
        mHandler.saveCredentials(
                testActivity,  // A mocked Activity or real, as needed
                mockUser,
                "test-password"
        );

        verify(mResultObserver).onChanged(argThat(ResourceMatchers.isLoading()));
        verify(mResultObserver).onChanged(argThat(ResourceMatchers.isSuccess()));
    }

    @Test
    public void testSaveCredentials_failure() {
        ActivityController<TestActivity> controller = Robolectric.buildActivity(TestActivity.class).setup();
        TestActivity testActivity = controller.get();

        // If we pass a null user or missing password,
        // the Handler should produce LOADING -> FAILURE
        mHandler.saveCredentials(
                testActivity,
                null, // invalid user
                "some-password"
        );

        verify(mResultObserver).onChanged(argThat(ResourceMatchers.isLoading()));
        verify(mResultObserver).onChanged(argThat(ResourceMatchers.isFailure()));
    }
}



class TestActivity extends ComponentActivity {
    // Empty activity is enough for Robolectric
}
