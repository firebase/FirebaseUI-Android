package com.firebase.ui.auth.viewmodel;

import android.app.Application;
import android.net.Uri;

import androidx.lifecycle.Observer;
import androidx.test.core.app.ApplicationProvider;

import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.IdpResponse;
import com.firebase.ui.auth.data.model.FlowParameters;
import com.firebase.ui.auth.data.model.Resource;
import com.firebase.ui.auth.data.remote.GenericIdpAnonymousUpgradeLinkingHandler;
import com.firebase.ui.auth.testhelpers.AutoCompleteTask;
import com.firebase.ui.auth.testhelpers.ResourceMatchers;
import com.firebase.ui.auth.testhelpers.TestHelper;
import com.firebase.ui.auth.ui.HelperActivityBase;
import com.firebase.ui.auth.ui.idp.WelcomeBackIdpPrompt;
import com.firebase.ui.auth.util.data.AuthOperationManager;
import com.firebase.ui.auth.viewmodel.email.EmailLinkSignInHandler;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.auth.OAuthProvider;

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
public class GenericIdpAnonymousUpgradeLinkingHandlerTest {

    private static final String MICROSOFT_PROVIDER = "microsoft.com";
    private static final String ID_TOKEN = "idToken";
    private static final String ACCESS_TOKEN = "accessToken";
    private static final String DISPLAY_NAME = "displayName";
    private static final String SCOPE = "scope";
    private static final String CUSTOM_PARAMETER_KEY = "customParameterKey";
    private static final String CUSTOM_PARAMETER_VALUE = "customParameterValue";

    private GenericIdpAnonymousUpgradeLinkingHandler mHandler;

    @Mock
    private FirebaseAuth mMockAuth;
    @Mock
    private FirebaseUser mMockUser;

    @Mock
    private FirebaseAuth mScratchMockAuth;

    @Mock
    private HelperActivityBase mMockActivity;

    @Mock
    private WelcomeBackIdpPrompt mMockWelcomeBackIdpPrompt;

    @Mock
    private Observer<Resource<IdpResponse>> mResponseObserver;

    @Mock
    private AuthResult mMockAuthResult;

    @Before
    public void setUp() {
        TestHelper.initialize();
        MockitoAnnotations.initMocks(this);

        FlowParameters testParams
                = TestHelper.getFlowParameters(
                Arrays.asList(MICROSOFT_PROVIDER, GoogleAuthProvider.PROVIDER_ID),
                /* enableAnonymousUpgrade= */ true);
        when(mMockActivity.getFlowParams()).thenReturn(testParams);
        when(mMockWelcomeBackIdpPrompt.getFlowParams()).thenReturn(testParams);

        mHandler = new GenericIdpAnonymousUpgradeLinkingHandler(
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
    public void testStartSignIn_anonymousUpgradeLinkingFlow_expectIdpResponseWithCredential() {
        setupAnonymousUpgrade();
        AuthOperationManager authOperationManager = AuthOperationManager.getInstance();
        authOperationManager.mScratchAuth = mScratchMockAuth;

        when(mScratchMockAuth.startActivityForSignInWithProvider(
                any(HelperActivityBase.class), any(OAuthProvider.class)))
                .thenReturn(AutoCompleteTask.forSuccess(mMockAuthResult));

        AuthCredential credential
                = OAuthProvider.getCredential(MICROSOFT_PROVIDER, ID_TOKEN, ACCESS_TOKEN);
        when(mMockAuthResult.getCredential()).thenReturn(credential);
        when(mMockAuthResult.getUser()).thenReturn(mMockUser);
        when(mMockUser.getDisplayName()).thenReturn(DISPLAY_NAME);
        when(mMockUser.getPhotoUrl()).thenReturn(new Uri.Builder().build());

        mHandler.startSignIn(mMockAuth, mMockWelcomeBackIdpPrompt, MICROSOFT_PROVIDER);

        ArgumentCaptor<OAuthProvider> providerCaptor = ArgumentCaptor.forClass(OAuthProvider.class);
        verify(mScratchMockAuth).startActivityForSignInWithProvider(eq(mMockWelcomeBackIdpPrompt),
                providerCaptor.capture());
        assertThat(providerCaptor.getValue().getProviderId()).isEqualTo(MICROSOFT_PROVIDER);

        InOrder inOrder = inOrder(mResponseObserver);
        inOrder.verify(mResponseObserver)
                .onChanged(argThat(ResourceMatchers.<IdpResponse>isLoading()));

        ArgumentCaptor<Resource<IdpResponse>> resolveCaptor =
                ArgumentCaptor.forClass(Resource.class);
        inOrder.verify(mResponseObserver).onChanged(resolveCaptor.capture());

        IdpResponse idpResponse = resolveCaptor.getValue().getValue();
        assertThat(idpResponse.getCredentialForLinking()).isNotNull();
    }

    private void setupAnonymousUpgrade() {
        // Mock isAnonymous() to return true so canUpgradeAnonymous will return true
        when(mMockUser.isAnonymous()).thenReturn(true);
        when(mMockAuth.getCurrentUser()).thenReturn(mMockUser);
    }
}
