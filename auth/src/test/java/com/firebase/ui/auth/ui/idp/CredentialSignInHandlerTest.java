package com.firebase.ui.auth.ui.idp;

import static junit.framework.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;

import com.firebase.ui.auth.BuildConfig;
import com.firebase.ui.auth.provider.IDPResponse;
import com.firebase.ui.auth.test_helpers.ActivityHelperShadow;
import com.firebase.ui.auth.test_helpers.AutoCompleteTask;
import com.firebase.ui.auth.test_helpers.CustomRobolectricGradleTestRunner;
import com.firebase.ui.auth.test_helpers.FakeAuthResult;
import com.firebase.ui.auth.test_helpers.FakeProviderQueryResult;
import com.firebase.ui.auth.test_helpers.TestConstants;
import com.firebase.ui.auth.test_helpers.TestHelper;
import com.firebase.ui.auth.ui.ActivityHelper;
import com.firebase.ui.auth.ui.ExtraConstants;
import com.firebase.ui.auth.ui.FlowParameters;
import com.firebase.ui.auth.ui.account_link.SaveCredentialsActivity;
import com.firebase.ui.auth.ui.account_link.WelcomeBackIDPPrompt;
import com.firebase.ui.auth.ui.account_link.WelcomeBackPasswordPrompt;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FacebookAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.ProviderQueryResult;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.robolectric.annotation.Config;

import java.util.Arrays;


@RunWith(CustomRobolectricGradleTestRunner.class)
@Config(constants = BuildConfig.class, sdk = 21, shadows = {ActivityHelperShadow.class})
public class CredentialSignInHandlerTest {
    private static final int RC_ACCOUNT_LINK = 3;
    private static final int RC_SAVE_CREDENTIALS = 4;
    private static final String LINKING_ERROR = "ERROR_TEST_LINKING";
    private static final String LINKING_EXPLANATION = "Test explanation";

    @Test
    public void testSignInSucceeded() {
        Activity mockActivty = mock(Activity.class);
        ActivityHelper mockActivityHelper = mock(ActivityHelper.class);
        FirebaseUser mockFirebaseUser = TestHelper.makeMockFirebaseUser();
        IDPResponse mockIdpResponse = mock(IDPResponse.class);

        CredentialSignInHandler credentialSignInHandler = new CredentialSignInHandler(
                mockActivty,
                mockActivityHelper,
                RC_ACCOUNT_LINK,
                RC_SAVE_CREDENTIALS,
                mockIdpResponse);

        Context mockContext = mock(Context.class);
        FlowParameters mockFlowParams = mock(FlowParameters.class);
        Task mockTask = mock(Task.class);

        when(mockTask.isSuccessful()).thenReturn(true);
        when(mockTask.getResult()).thenReturn(new FakeAuthResult(mockFirebaseUser));
        when(mockActivityHelper.getApplicationContext()).thenReturn(mockContext);
        when(mockActivityHelper.getFlowParams()).thenReturn(mockFlowParams);
        credentialSignInHandler.onComplete(mockTask);

        ArgumentCaptor<Intent> intentCaptor = ArgumentCaptor.forClass(Intent.class);
        ArgumentCaptor<Integer> intCaptor = ArgumentCaptor.forClass(Integer.class);
        verify(mockActivty).startActivityForResult(intentCaptor.capture(), intCaptor.capture());
        Intent capturedIntent = intentCaptor.getValue();
        assertEquals(RC_SAVE_CREDENTIALS, (int) intCaptor.getValue());
        assertEquals(
                SaveCredentialsActivity.class.getName(),
                capturedIntent.getComponent().getClassName());
        assertEquals(
                TestConstants.EMAIL,
                capturedIntent.getExtras().getString(ExtraConstants.EXTRA_EMAIL));
        assertEquals(
                TestConstants.NAME,
                capturedIntent.getExtras().getString(ExtraConstants.EXTRA_NAME));
        assertEquals(
                TestConstants.PHOTO_URL,
                capturedIntent.getExtras().getString(ExtraConstants.EXTRA_PROFILE_PICTURE_URI));
    }

    @Test
    public void testSignInFailed_withFacebookAlreadyLinked() {
        Activity mockActivty = mock(Activity.class);
        ActivityHelper mockActivityHelper = mock(ActivityHelper.class);
        FirebaseAuth mockFirebaseAuth = mock(FirebaseAuth.class);
        IDPResponse mockIdpResponse = mock(IDPResponse.class);
        CredentialSignInHandler credentialSignInHandler = new CredentialSignInHandler(
                mockActivty,
                mockActivityHelper,
                RC_ACCOUNT_LINK,
                RC_SAVE_CREDENTIALS,
                mockIdpResponse);

        Context mockContext = mock(Context.class);
        Task mockTask = mock(Task.class);
        FlowParameters mockFlowParams = mock(FlowParameters.class);
        // pretend there was already an account with this email
        when(mockTask.getException()).thenReturn(
                new FirebaseAuthUserCollisionException(LINKING_ERROR, LINKING_EXPLANATION));
        when(mockIdpResponse.getEmail()).thenReturn(TestConstants.EMAIL);
        when(mockActivityHelper.getFirebaseAuth()).thenReturn(mockFirebaseAuth);
        when(mockActivityHelper.getApplicationContext()).thenReturn(mockContext);
        when(mockActivityHelper.getFlowParams()).thenReturn(mockFlowParams);

        // pretend the account has Facebook linked already
        when(mockFirebaseAuth.fetchProvidersForEmail(TestConstants.EMAIL)).thenReturn(
                new AutoCompleteTask<ProviderQueryResult>(
                        new FakeProviderQueryResult(
                                Arrays.asList(FacebookAuthProvider.PROVIDER_ID)), true, null));


        credentialSignInHandler.onComplete(mockTask);
        ArgumentCaptor<Intent> intentCaptor = ArgumentCaptor.forClass(Intent.class);
        ArgumentCaptor<Integer> intCaptor = ArgumentCaptor.forClass(Integer.class);
        verify(mockActivty).startActivityForResult(intentCaptor.capture(), intCaptor.capture());
        Intent capturedIntent = intentCaptor.getValue();
        assertEquals(RC_ACCOUNT_LINK, (int) intCaptor.getValue());
        assertEquals(
                WelcomeBackIDPPrompt.class.getName(),
                capturedIntent.getComponent().getClassName());
        assertEquals(
                TestConstants.EMAIL,
                capturedIntent.getExtras().getString(ExtraConstants.EXTRA_EMAIL));
        assertEquals(
                FacebookAuthProvider.PROVIDER_ID,
                capturedIntent.getExtras().getString(ExtraConstants.EXTRA_PROVIDER));

    }


    @Test
    public void testSignInFailed_withPasswordAccountAlreadyLinked() {
        Activity mockActivty = mock(Activity.class);
        ActivityHelper mockActivityHelper = mock(ActivityHelper.class);
        FirebaseAuth mockFirebaseAuth = mock(FirebaseAuth.class);
        IDPResponse mockIdpResponse = mock(IDPResponse.class);
        when(mockIdpResponse.getEmail()).thenReturn(TestConstants.EMAIL);
        CredentialSignInHandler credentialSignInHandler = new CredentialSignInHandler(
                mockActivty,
                mockActivityHelper,
                RC_ACCOUNT_LINK,
                RC_SAVE_CREDENTIALS,
                mockIdpResponse);

        Context mockContext = mock(Context.class);
        Task mockTask = mock(Task.class);
        FlowParameters mockFlowParams = mock(FlowParameters.class);

        // pretend there was already an account with this email
        when(mockTask.getException()).thenReturn(
                new FirebaseAuthUserCollisionException(LINKING_ERROR, LINKING_EXPLANATION));
        when(mockIdpResponse.getEmail()).thenReturn(TestConstants.EMAIL);
        when(mockActivityHelper.getFirebaseAuth()).thenReturn(mockFirebaseAuth);
        when(mockActivityHelper.getApplicationContext()).thenReturn(mockContext);
        when(mockActivityHelper.getFlowParams()).thenReturn(mockFlowParams);

        // pretend the account has a Password account linked already
        when(mockFirebaseAuth.fetchProvidersForEmail(TestConstants.EMAIL)).thenReturn(
                new AutoCompleteTask<ProviderQueryResult>(
                        new FakeProviderQueryResult(
                                Arrays.asList(EmailAuthProvider.PROVIDER_ID)), true, null));


        credentialSignInHandler.onComplete(mockTask);
        ArgumentCaptor<Intent> intentCaptor = ArgumentCaptor.forClass(Intent.class);
        ArgumentCaptor<Integer> intCaptor = ArgumentCaptor.forClass(Integer.class);
        verify(mockActivty).startActivityForResult(intentCaptor.capture(), intCaptor.capture());
        Intent capturedIntent = intentCaptor.getValue();
        assertEquals(RC_ACCOUNT_LINK, (int) intCaptor.getValue());
        assertEquals(
                WelcomeBackPasswordPrompt.class.getName(),
                capturedIntent.getComponent().getClassName());
        assertEquals(
                TestConstants.EMAIL,
                ((IDPResponse) capturedIntent.getExtras().getParcelable(ExtraConstants
                        .EXTRA_IDP_RESPONSE)).getEmail());
    }
}
