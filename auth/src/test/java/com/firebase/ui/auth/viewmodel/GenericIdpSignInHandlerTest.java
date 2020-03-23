package com.firebase.ui.auth.viewmodel;

import android.app.Activity;
import android.app.Application;
import android.net.Uri;

import androidx.lifecycle.Observer;
import androidx.test.core.app.ApplicationProvider;

import com.firebase.ui.auth.AuthUI;

import com.firebase.ui.auth.FirebaseAuthAnonymousUpgradeException;
import com.firebase.ui.auth.FirebaseUiUserCollisionException;
import com.firebase.ui.auth.IdpResponse;
import com.firebase.ui.auth.data.model.FlowParameters;
import com.firebase.ui.auth.data.model.Resource;
import com.firebase.ui.auth.data.remote.GenericIdpSignInHandler;
import com.firebase.ui.auth.testhelpers.AutoCompleteTask;
import com.firebase.ui.auth.testhelpers.FakeAuthResult;
import com.firebase.ui.auth.testhelpers.FakeSignInMethodQueryResult;
import com.firebase.ui.auth.testhelpers.ResourceMatchers;
import com.firebase.ui.auth.testhelpers.TestHelper;
import com.firebase.ui.auth.ui.HelperActivityBase;
import com.firebase.ui.auth.ui.idp.WelcomeBackIdpPrompt;
import com.firebase.ui.auth.util.data.AuthOperationManager;
import com.firebase.ui.auth.viewmodel.email.EmailLinkSignInHandler;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.auth.OAuthProvider;
import com.google.firebase.auth.SignInMethodQueryResult;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;


import static com.google.common.truth.Truth.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link EmailLinkSignInHandler}.
 */
@RunWith(RobolectricTestRunner.class)
public class GenericIdpSignInHandlerTest {

    private static final String MICROSOFT_PROVIDER = "microsoft.com";
    private static final String ID_TOKEN = "idToken";
    private static final String ACCESS_TOKEN = "accessToken";
    private static final String PHOTO_URL = "photoUrl";
    private static final String DISPLAY_NAME = "displayName";
    private static final String EMAIL = "email";
    private static final String SCOPE = "scope";
    private static final String CUSTOM_PARAMETER_KEY = "customParameterKey";
    private static final String CUSTOM_PARAMETER_VALUE = "customParameterValue";

    private GenericIdpSignInHandler mHandler;

    @Mock
    private FirebaseAuth mMockAuth;
    @Mock
    private FirebaseUser mMockUser;

    @Mock
    private HelperActivityBase mMockActivity;

    @Mock
    private Observer<Resource<IdpResponse>> mResponseObserver;

    @Before
    public void setUp() {
        TestHelper.initialize();
        MockitoAnnotations.initMocks(this);

        FlowParameters testParams
                = TestHelper.getFlowParameters(
                Arrays.asList(MICROSOFT_PROVIDER, GoogleAuthProvider.PROVIDER_ID),
                /* enableAnonymousUpgrade= */ true);
        when(mMockActivity.getFlowParams()).thenReturn(testParams);

        mHandler = new GenericIdpSignInHandler(
                (Application) ApplicationProvider.getApplicationContext());

        Map<String, String> customParams = new HashMap<>();
        customParams.put(CUSTOM_PARAMETER_KEY, CUSTOM_PARAMETER_VALUE);

        AuthUI.IdpConfig config
                = new AuthUI.IdpConfig.MicrosoftBuilder()
                .setScopes(Arrays.asList(SCOPE))
                .setCustomParameters(customParams)
                .build();
        mHandler.initializeForTesting(config);
        mHandler.getOperation().observeForever(mResponseObserver);
    }

    @Test
    public void testStartSignIn_normalSignInFlow_expectSuccess() {
        mHandler.getOperation().observeForever(mResponseObserver);

        when(mMockAuth.startActivityForSignInWithProvider(any(Activity.class), any(OAuthProvider.class)))
                .thenReturn(AutoCompleteTask.forSuccess(FakeAuthResult.INSTANCE));

        mHandler.startSignIn(mMockAuth, mMockActivity, MICROSOFT_PROVIDER);

        ArgumentCaptor<OAuthProvider> providerCaptor = ArgumentCaptor.forClass(OAuthProvider.class);
        verify(mMockAuth).startActivityForSignInWithProvider(eq(mMockActivity), providerCaptor.capture());

        assertThat(providerCaptor.getValue().getProviderId()).isEqualTo(MICROSOFT_PROVIDER);

        InOrder inOrder = inOrder(mResponseObserver);
        inOrder.verify(mResponseObserver)
                .onChanged(argThat(ResourceMatchers.<IdpResponse>isLoading()));

        ArgumentCaptor<Resource<IdpResponse>> resourceCaptor =
                ArgumentCaptor.forClass(Resource.class);
        inOrder.verify(mResponseObserver)
                .onChanged(resourceCaptor.capture());

        IdpResponse returnedIdpResponse = resourceCaptor.getValue().getValue();

        assertThat(returnedIdpResponse.isSuccessful()).isTrue();
        assertThat(returnedIdpResponse.getUser()).isNotNull();
    }

    @Test
    public void testStartSignIn_normalSignInFlowWithRecoverableError_expectFailure() {
        AuthCredential credential
                = OAuthProvider.getCredential(MICROSOFT_PROVIDER, ID_TOKEN, ACCESS_TOKEN);
        FirebaseAuthUserCollisionException collisionException
                = new FirebaseAuthUserCollisionException("foo", "bar");
        collisionException.zza(EMAIL).zza(credential);

        when(mMockAuth.startActivityForSignInWithProvider(any(Activity.class), any(OAuthProvider.class)))
                .thenReturn(AutoCompleteTask.<AuthResult>forFailure(collisionException));

        mHandler.startSignIn(mMockAuth, mMockActivity, MICROSOFT_PROVIDER);

        ArgumentCaptor<OAuthProvider> providerCaptor = ArgumentCaptor.forClass(OAuthProvider.class);
        verify(mMockAuth).startActivityForSignInWithProvider(eq(mMockActivity), providerCaptor.capture());

        assertThat(providerCaptor.getValue().getProviderId()).isEqualTo(MICROSOFT_PROVIDER);

        InOrder inOrder = inOrder(mResponseObserver);
        inOrder.verify(mResponseObserver)
                .onChanged(argThat(ResourceMatchers.<IdpResponse>isLoading()));

        ArgumentCaptor<Resource<IdpResponse>> resourceCaptor =
                ArgumentCaptor.forClass(Resource.class);
        inOrder.verify(mResponseObserver)
                .onChanged(resourceCaptor.capture());

        FirebaseUiUserCollisionException e = (FirebaseUiUserCollisionException)
                resourceCaptor.getValue().getException();

        assertThat(e.getCredential()).isNotNull();
        assertThat(e.getEmail()).isEqualTo(EMAIL);
    }

    @Test
    public void testStartSignIn_normalSignInFlowWithError_expectFailure() {
        FirebaseAuthException firebaseAuthException
                = new FirebaseAuthException("foo", "bar");
        when(mMockAuth.startActivityForSignInWithProvider(any(Activity.class),
                any(OAuthProvider.class)))
                .thenReturn(AutoCompleteTask.<AuthResult>forFailure(firebaseAuthException));

        mHandler.startSignIn(mMockAuth, mMockActivity, MICROSOFT_PROVIDER);

        ArgumentCaptor<OAuthProvider> providerCaptor
                = ArgumentCaptor.forClass(OAuthProvider.class);
        verify(mMockAuth)
                .startActivityForSignInWithProvider(eq(mMockActivity), providerCaptor.capture());

        assertThat(providerCaptor.getValue().getProviderId()).isEqualTo(MICROSOFT_PROVIDER);

        InOrder inOrder = inOrder(mResponseObserver);
        inOrder.verify(mResponseObserver)
                .onChanged(argThat(ResourceMatchers.<IdpResponse>isLoading()));

        ArgumentCaptor<Resource<IdpResponse>> resourceCaptor =
                ArgumentCaptor.forClass(Resource.class);
        inOrder.verify(mResponseObserver)
                .onChanged(resourceCaptor.capture());

        assertThat(resourceCaptor.getValue().getException())
                .isInstanceOf(FirebaseAuthException.class);
    }

    @Test
    public void testStartSignIn_anonymousUpgradeFlow_expectSuccess() {
        setupAnonymousUpgrade();

        when(mMockAuth.getCurrentUser().startActivityForLinkWithProvider(
                any(Activity.class), any(OAuthProvider.class)))
                .thenReturn(AutoCompleteTask.forSuccess(FakeAuthResult.INSTANCE));


        mHandler.startSignIn(mMockAuth, mMockActivity, MICROSOFT_PROVIDER);

        ArgumentCaptor<OAuthProvider> providerCaptor
                = ArgumentCaptor.forClass(OAuthProvider.class);
        verify(mMockAuth.getCurrentUser())
                .startActivityForLinkWithProvider(eq(mMockActivity), providerCaptor.capture());

        assertThat(providerCaptor.getValue().getProviderId()).isEqualTo(MICROSOFT_PROVIDER);

        InOrder inOrder = inOrder(mResponseObserver);
        inOrder.verify(mResponseObserver)
                .onChanged(argThat(ResourceMatchers.<IdpResponse>isLoading()));

        ArgumentCaptor<Resource<IdpResponse>> resourceCaptor =
                ArgumentCaptor.forClass(Resource.class);
        inOrder.verify(mResponseObserver)
                .onChanged(resourceCaptor.capture());

        IdpResponse returnedIdpResponse = resourceCaptor.getValue().getValue();

        assertThat(returnedIdpResponse.isSuccessful()).isTrue();
        assertThat(returnedIdpResponse.getUser()).isNotNull();

    }

    @Test
    public void testStartSignIn_anonymousUpgradeFlowWithConflict_expectRecoverableError() {
        setupAnonymousUpgrade();

        AuthCredential credential
                = OAuthProvider.getCredential(MICROSOFT_PROVIDER, ID_TOKEN, ACCESS_TOKEN);
        FirebaseAuthUserCollisionException collisionException
                = new FirebaseAuthUserCollisionException("foo", "bar");
        collisionException.zza(EMAIL).zza(credential);
        when(mMockAuth.getCurrentUser().startActivityForLinkWithProvider(
                any(Activity.class), any(OAuthProvider.class)))
                .thenReturn(AutoCompleteTask.<AuthResult>forFailure(collisionException));

        // Case 1: Anon user signing in with an existing account
        when(mMockAuth.fetchSignInMethodsForEmail(any(String.class)))
                .thenReturn(AutoCompleteTask.<SignInMethodQueryResult>forSuccess(
                        new FakeSignInMethodQueryResult(Arrays.asList(
                                MICROSOFT_PROVIDER))));

        mHandler.startSignIn(mMockAuth, mMockActivity, MICROSOFT_PROVIDER);

        ArgumentCaptor<OAuthProvider> providerCaptor = ArgumentCaptor.forClass(OAuthProvider.class);
        verify(mMockAuth.getCurrentUser())
                .startActivityForLinkWithProvider(eq(mMockActivity), providerCaptor.capture());

        assertThat(providerCaptor.getValue().getProviderId()).isEqualTo(MICROSOFT_PROVIDER);

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

    @Test
    public void testStartSignIn_anonymousUpgradeFlowWithConflict_expectRecoverableLinkingError() {
        setupAnonymousUpgrade();

        AuthCredential credential
                = OAuthProvider.getCredential(MICROSOFT_PROVIDER, ID_TOKEN, ACCESS_TOKEN);
        FirebaseAuthUserCollisionException collisionException
                = new FirebaseAuthUserCollisionException("foo", "bar");
        collisionException.zza(EMAIL).zza(credential);

        when(mMockAuth.getCurrentUser().startActivityForLinkWithProvider(
                any(Activity.class), any(OAuthProvider.class)))
                .thenReturn(AutoCompleteTask.<AuthResult>forFailure(collisionException));

        // Case 2:  Anonymous user trying to link with a provider keyed by an email that already
        // belongs to an existing account
        when(mMockAuth.fetchSignInMethodsForEmail(any(String.class)))
                .thenReturn(AutoCompleteTask.<SignInMethodQueryResult>forSuccess(
                        new FakeSignInMethodQueryResult(Arrays.asList(
                                GoogleAuthProvider.PROVIDER_ID))));

        mHandler.startSignIn(mMockAuth, mMockActivity, MICROSOFT_PROVIDER);

        ArgumentCaptor<OAuthProvider> providerCaptor = ArgumentCaptor.forClass(OAuthProvider.class);
        verify(mMockAuth.getCurrentUser())
                .startActivityForLinkWithProvider(eq(mMockActivity), providerCaptor.capture());

        assertThat(providerCaptor.getValue().getProviderId()).isEqualTo(MICROSOFT_PROVIDER);

        InOrder inOrder = inOrder(mResponseObserver);
        inOrder.verify(mResponseObserver)
                .onChanged(argThat(ResourceMatchers.<IdpResponse>isLoading()));

        ArgumentCaptor<Resource<IdpResponse>> resolveCaptor =
                ArgumentCaptor.forClass(Resource.class);
        inOrder.verify(mResponseObserver).onChanged(resolveCaptor.capture());

        FirebaseUiUserCollisionException e =
                (FirebaseUiUserCollisionException) resolveCaptor.getValue().getException();
        assertThat(e.getCredential()).isNotNull();
        assertThat(e.getEmail()).isEqualTo(EMAIL);
    }

    private void setupAnonymousUpgrade() {
        // Mock isAnonymous() to return true so canUpgradeAnonymous will return true
        when(mMockUser.isAnonymous()).thenReturn(true);
        when(mMockAuth.getCurrentUser()).thenReturn(mMockUser);
    }
}
