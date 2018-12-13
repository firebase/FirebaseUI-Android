package com.firebase.ui.auth.viewmodel;

import android.arch.lifecycle.Observer;
import android.support.annotation.Nullable;

import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.ErrorCodes;
import com.firebase.ui.auth.FirebaseAuthAnonymousUpgradeException;
import com.firebase.ui.auth.FirebaseUiException;
import com.firebase.ui.auth.IdpResponse;
import com.firebase.ui.auth.data.model.FlowParameters;
import com.firebase.ui.auth.data.model.Resource;
import com.firebase.ui.auth.data.model.State;
import com.firebase.ui.auth.data.model.User;
import com.firebase.ui.auth.testhelpers.AutoCompleteTask;
import com.firebase.ui.auth.testhelpers.AutoContinueTask;
import com.firebase.ui.auth.testhelpers.ResourceMatchers;
import com.firebase.ui.auth.testhelpers.TestConstants;
import com.firebase.ui.auth.testhelpers.TestHelper;
import com.firebase.ui.auth.util.data.AuthOperationManager;
import com.firebase.ui.auth.util.data.ContinueUrlBuilder;
import com.firebase.ui.auth.util.data.EmailLinkPersistenceManager;
import com.firebase.ui.auth.util.data.SessionUtils;
import com.firebase.ui.auth.viewmodel.email.EmailLinkSignInHandler;
import com.google.firebase.auth.ActionCodeResult;
import com.google.firebase.auth.AdditionalUserInfo;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.EmailAuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FacebookAuthCredential;
import com.google.firebase.auth.FacebookAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseUser;

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
import java.util.Collections;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link EmailLinkSignInHandler}.
 */
@RunWith(RobolectricTestRunner.class)
public class EmailLinkSignInHandlerTest {

    private static final String EMAIL_LINK =
            "https://fake.com/__/auth/action?apiKey=apiKey&mode=signIn"
                    + "&oobCode=oobCode"
                    + "&continueUrl=";

    private static final String CONTINUE_URL = "https://google.com";

    private EmailLinkSignInHandler mHandler;
    private EmailLinkPersistenceManager mPersistenceManager;

    @Mock private FirebaseAuth mMockAuth;
    @Mock private FirebaseAuth mScratchMockAuth;
    @Mock private FirebaseUser mMockAnonUser;
    @Mock private Observer<Resource<IdpResponse>> mResponseObserver;
    @Mock private AuthResult mMockAuthResult;
    @Mock private AdditionalUserInfo mockAdditionalUserInfo;

    @Mock private ActionCodeResult mMockActionCodeResult;

    @Before
    public void setUp() {
        TestHelper.initialize();
        MockitoAnnotations.initMocks(this);

        mHandler = new EmailLinkSignInHandler(RuntimeEnvironment.application);


        initializeHandlerWithSessionInfo(TestConstants.SESSION_ID, null, null, false);


        mPersistenceManager = EmailLinkPersistenceManager.getInstance();

        FirebaseUser user = TestHelper.getMockFirebaseUser();
        when(mMockAuthResult.getUser()).thenReturn(user);
        when(mMockAuthResult.getAdditionalUserInfo()).thenReturn(mockAdditionalUserInfo);
        when(mockAdditionalUserInfo.isNewUser()).thenReturn(false);
    }

    @Test
    @SuppressWarnings("all")
    public void
    testStartSignIn_differentDeviceLinkWithForceSameDeviceTrue_expectWrongDeviceError() {
        String differentSessionId = SessionUtils.generateRandomAlphaNumericString(10);
        initializeHandlerWithSessionInfo(differentSessionId, null, null, true);

        mHandler.getOperation().observeForever(mResponseObserver);
        when(mMockAuth.isSignInWithEmailLink(any(String.class))).thenReturn(true);


        mHandler.startSignIn();

        verify(mMockAuth).isSignInWithEmailLink(any(String.class));
        ArgumentCaptor<Resource<IdpResponse>> captor =
                ArgumentCaptor.forClass(Resource.class);

        InOrder inOrder = inOrder(mResponseObserver);
        inOrder.verify(mResponseObserver)
                .onChanged(argThat(ResourceMatchers.<IdpResponse>isLoading()));
        inOrder.verify(mResponseObserver).onChanged(captor.capture());

        FirebaseUiException exception = (FirebaseUiException) captor.getValue().getException();
        assertThat(exception).isNotNull();
        assertThat(exception.getErrorCode()).isEqualTo(ErrorCodes.EMAIL_LINK_WRONG_DEVICE_ERROR);
    }

    @Test
    @SuppressWarnings("all")
    public void
    testStartSignIn_differentDeviceLinkWithNoSessionId_expectInvalidLinkError() {
        initializeHandlerWithSessionInfo(null, null, null, true);

        mHandler.getOperation().observeForever(mResponseObserver);
        when(mMockAuth.isSignInWithEmailLink(any(String.class))).thenReturn(true);

        mHandler.startSignIn();

        verify(mMockAuth).isSignInWithEmailLink(any(String.class));
        ArgumentCaptor<Resource<IdpResponse>> captor =
                ArgumentCaptor.forClass(Resource.class);

        InOrder inOrder = inOrder(mResponseObserver);
        inOrder.verify(mResponseObserver)
                .onChanged(argThat(ResourceMatchers.<IdpResponse>isLoading()));
        inOrder.verify(mResponseObserver).onChanged(captor.capture());

        FirebaseUiException exception = (FirebaseUiException) captor.getValue().getException();
        assertThat(exception).isNotNull();
        assertThat(exception.getErrorCode()).isEqualTo(ErrorCodes.INVALID_EMAIL_LINK_ERROR);
    }

    @Test
    @SuppressWarnings("all")
    public void
    testStartSignIn_differentDeviceLinkWithValidSessionInfo_expectPromptForEmailError() {
        String differentSessionId = SessionUtils.generateRandomAlphaNumericString(10);
        initializeHandlerWithSessionInfo(differentSessionId, null, null, false);

        mHandler.getOperation().observeForever(mResponseObserver);
        when(mMockAuth.isSignInWithEmailLink(any(String.class))).thenReturn(true);
        when(mMockAuth.checkActionCode(any(String.class))).thenReturn(AutoCompleteTask.forSuccess
                (mMockActionCodeResult));

        mHandler.startSignIn();

        verify(mMockAuth).isSignInWithEmailLink(any(String.class));
        verify(mMockAuth).checkActionCode(any(String.class));

        ArgumentCaptor<Resource<IdpResponse>> captor =
                ArgumentCaptor.forClass(Resource.class);

        InOrder inOrder = inOrder(mResponseObserver);
        inOrder.verify(mResponseObserver)
                .onChanged(argThat(ResourceMatchers.<IdpResponse>isLoading()));
        inOrder.verify(mResponseObserver).onChanged(captor.capture());

        FirebaseUiException exception = (FirebaseUiException) captor.getValue().getException();
        assertThat(exception).isNotNull();
        assertThat(exception.getErrorCode())
                .isEqualTo(ErrorCodes.EMAIL_LINK_PROMPT_FOR_EMAIL_ERROR);
    }

    @Test
    @SuppressWarnings("all")
    public void testStartSignIn_differentDeviceLinkWithValidSessionInfo_expectInvalidLinkError() {
        String differentSessionId = SessionUtils.generateRandomAlphaNumericString(10);
        initializeHandlerWithSessionInfo(differentSessionId, null, null, false);

        mHandler.getOperation().observeForever(mResponseObserver);
        when(mMockAuth.isSignInWithEmailLink(any(String.class))).thenReturn(true);
        when(mMockAuth.checkActionCode(any(String.class)))
                .thenReturn(AutoCompleteTask.<ActionCodeResult>forFailure(new Exception("foo")));

        mHandler.startSignIn();

        verify(mMockAuth).isSignInWithEmailLink(any(String.class));
        verify(mMockAuth).checkActionCode(any(String.class));

        ArgumentCaptor<Resource<IdpResponse>> captor =
                ArgumentCaptor.forClass(Resource.class);

        InOrder inOrder = inOrder(mResponseObserver);
        inOrder.verify(mResponseObserver)
                .onChanged(argThat(ResourceMatchers.<IdpResponse>isLoading()));
        inOrder.verify(mResponseObserver).onChanged(captor.capture());

        FirebaseUiException exception = (FirebaseUiException) captor.getValue().getException();
        assertThat(exception).isNotNull();
        assertThat(exception.getErrorCode())
                .isEqualTo(ErrorCodes.INVALID_EMAIL_LINK_ERROR);
    }

    @Test
    @SuppressWarnings("all")
    public void
    testStartSignIn_differentDeviceLinkWithAnonymousUpgradeEnabled_expectInvalidLinkError() {
        String differentSessionId = SessionUtils.generateRandomAlphaNumericString(10);
        String anonUserId = SessionUtils.generateRandomAlphaNumericString(10);
        initializeHandlerWithSessionInfo(differentSessionId, anonUserId, null, false);

        mHandler.getOperation().observeForever(mResponseObserver);
        when(mMockAuth.isSignInWithEmailLink(any(String.class))).thenReturn(true);

        mHandler.startSignIn();

        verify(mMockAuth).isSignInWithEmailLink(any(String.class));

        ArgumentCaptor<Resource<IdpResponse>> captor =
                ArgumentCaptor.forClass(Resource.class);

        InOrder inOrder = inOrder(mResponseObserver);
        inOrder.verify(mResponseObserver)
                .onChanged(argThat(ResourceMatchers.<IdpResponse>isLoading()));
        inOrder.verify(mResponseObserver).onChanged(captor.capture());

        FirebaseUiException exception = (FirebaseUiException) captor.getValue().getException();
        assertThat(exception).isNotNull();
        assertThat(exception.getErrorCode())
                .isEqualTo(ErrorCodes.EMAIL_LINK_WRONG_DEVICE_ERROR);
    }


    @Test
    @SuppressWarnings("all")
    public void testStartSignIn_invalidLink_expectInvalidLinkError() {
        mHandler.getOperation().observeForever(mResponseObserver);

        when(mMockAuth.isSignInWithEmailLink(any(String.class))).thenReturn(false);

        mHandler.startSignIn();

        verify(mMockAuth).isSignInWithEmailLink(any(String.class));

        ArgumentCaptor<Resource<IdpResponse>> captor =
                ArgumentCaptor.forClass(Resource.class);
        InOrder inOrder = inOrder(mResponseObserver);
        inOrder.verify(mResponseObserver)
                .onChanged(argThat(ResourceMatchers.<IdpResponse>isLoading()));
        inOrder.verify(mResponseObserver).onChanged(captor.capture());

        FirebaseUiException exception = (FirebaseUiException) captor.getValue().getException();
        assertThat(exception).isNotNull();
        assertThat(exception.getErrorCode()).isEqualTo(ErrorCodes
                .INVALID_EMAIL_LINK_ERROR);
    }

    @Test
    @SuppressWarnings("all")
    public void testStartSignIn_normalFlow() {
        mHandler.getOperation().observeForever(mResponseObserver);
        when(mMockAuth.isSignInWithEmailLink(any(String.class))).thenReturn(true);

        mPersistenceManager.saveEmail(RuntimeEnvironment.application, TestConstants.EMAIL,
                TestConstants.SESSION_ID, TestConstants.UID);

        when(mMockAuth.signInWithCredential(any(AuthCredential.class))).thenReturn
                (AutoCompleteTask.forSuccess(mMockAuthResult));

        mHandler.startSignIn();

        ArgumentCaptor<Resource<IdpResponse>> captor =
                ArgumentCaptor.forClass(Resource.class);

        InOrder inOrder = inOrder(mResponseObserver);
        inOrder.verify(mResponseObserver)
                .onChanged(argThat(ResourceMatchers.<IdpResponse>isLoading()));
        inOrder.verify(mResponseObserver).onChanged(captor.capture());

        IdpResponse response = captor.getValue().getValue();

        assertThat(response.getUser().getProviderId()).isEqualTo(AuthUI.EMAIL_LINK_PROVIDER);
        assertThat(response.getUser().getEmail()).isEqualTo(mMockAuthResult.getUser()
                .getEmail());
        assertThat(response.getUser().getName()).isEqualTo(mMockAuthResult.getUser()
                .getDisplayName());
        assertThat(response.getUser().getPhotoUri()).isEqualTo(mMockAuthResult.getUser()
                .getPhotoUrl());

        assertThat(mPersistenceManager.retrieveSessionRecord(RuntimeEnvironment.application))
                .isNull();
    }

    @Test
    @SuppressWarnings("all")
    public void testStartSignIn_normalFlowWithAnonymousUpgrade_expectSuccessfulMerge() {
        mHandler.getOperation().observeForever(mResponseObserver);
        setupAnonymousUpgrade();
        when(mMockAuth.isSignInWithEmailLink(any(String.class))).thenReturn(true);

        mPersistenceManager.saveEmail(RuntimeEnvironment.application, TestConstants.EMAIL,
                TestConstants.SESSION_ID, TestConstants.UID);

        when(mMockAuth.getCurrentUser().linkWithCredential(any(AuthCredential.class)))
                .thenReturn(new AutoContinueTask<>(mMockAuthResult,
                        mMockAuthResult,
                        true,
                        null));

        mHandler.startSignIn();

        ArgumentCaptor<Resource<IdpResponse>> captor =
                ArgumentCaptor.forClass(Resource.class);

        InOrder inOrder = inOrder(mResponseObserver);

        inOrder.verify(mResponseObserver)
                .onChanged(argThat(ResourceMatchers.<IdpResponse>isLoading()));
        inOrder.verify(mResponseObserver).onChanged(captor.capture());

        IdpResponse response = captor.getValue().getValue();

        assertThat(response.getUser().getProviderId()).isEqualTo(AuthUI.EMAIL_LINK_PROVIDER);
        assertThat(response.getUser().getEmail()).isEqualTo(mMockAuthResult.getUser()
                .getEmail());
        assertThat(response.getUser().getName()).isEqualTo(mMockAuthResult.getUser()
                .getDisplayName());
        assertThat(response.getUser().getPhotoUri()).isEqualTo(mMockAuthResult.getUser()
                .getPhotoUrl());

        assertThat(mPersistenceManager.retrieveSessionRecord(RuntimeEnvironment.application))
                .isNull();
    }

    @Test
    @SuppressWarnings("all")
    public void testStartSignIn_normalFlowWithAnonymousUpgrade_expectMergeFailure() {
        mHandler.getOperation().observeForever(mResponseObserver);
        setupAnonymousUpgrade();
        when(mMockAuth.isSignInWithEmailLink(any(String.class))).thenReturn(true);

        mPersistenceManager.saveEmail(RuntimeEnvironment.application, TestConstants.EMAIL,
                TestConstants.SESSION_ID, TestConstants.UID);
        when(mMockAuth.getCurrentUser().linkWithCredential(any(AuthCredential.class)))
                .thenReturn(AutoCompleteTask.<AuthResult>forFailure(
                        new FirebaseAuthUserCollisionException("foo", "bar")));

        mHandler.startSignIn();

        ArgumentCaptor<Resource<IdpResponse>> captor =
                ArgumentCaptor.forClass(Resource.class);

        InOrder inOrder = inOrder(mResponseObserver);

        inOrder.verify(mResponseObserver)
                .onChanged(argThat(ResourceMatchers.<IdpResponse>isLoading()));
        inOrder.verify(mResponseObserver).onChanged(captor.capture());


        assertThat(captor.getValue().getException()).isNotNull();
        FirebaseAuthAnonymousUpgradeException mergeException =
                (FirebaseAuthAnonymousUpgradeException) captor.getValue().getException();
        assertThat(mergeException.getResponse().getCredentialForLinking()).isNotNull();
        assertThat(mPersistenceManager.retrieveSessionRecord(RuntimeEnvironment.application))
                .isNull();
    }

    @Test
    @SuppressWarnings("all")
    public void testStartSignIn_linkingFlow_expectSuccessfulLink() {
        mHandler.getOperation().observeForever(mResponseObserver);
        when(mMockAuth.isSignInWithEmailLink(any(String.class))).thenReturn(true);

        mPersistenceManager.saveEmail(RuntimeEnvironment.application, TestConstants.EMAIL,
                TestConstants.SESSION_ID, TestConstants.UID);
        mPersistenceManager.saveIdpResponseForLinking(RuntimeEnvironment.application,
                buildFacebookIdpResponse());


        when(mMockAuth.signInWithCredential(any(AuthCredential.class))).thenReturn
                (AutoCompleteTask.forSuccess(mMockAuthResult));

        // Mock linking with Facebook to always work
        when(mMockAuthResult.getUser().linkWithCredential(any(FacebookAuthCredential
                .class)))
                .thenReturn(new AutoContinueTask<>(mMockAuthResult,
                        mMockAuthResult,
                        true,
                        null));

        mHandler.startSignIn();

        // Validate regular sign in
        ArgumentCaptor<EmailAuthCredential> credentialCaptor
                = ArgumentCaptor.forClass(EmailAuthCredential.class);
        verify(mMockAuth).signInWithCredential(credentialCaptor.capture());

        assertThat(credentialCaptor.getValue().getEmail()).isEqualTo(TestConstants.EMAIL);
        assertThat(credentialCaptor.getValue().getSignInMethod()).isEqualTo(EmailAuthProvider
                .EMAIL_LINK_SIGN_IN_METHOD);

        // Validate linking was called
        verify(mMockAuthResult.getUser()).linkWithCredential(any(FacebookAuthCredential
                .class));

        // Validate that the data was cleared
        assertThat(mPersistenceManager.retrieveSessionRecord(RuntimeEnvironment.application))
                .isNull();

        // Validate IdpResponse
        ArgumentCaptor<Resource<IdpResponse>> captor =
                ArgumentCaptor.forClass(Resource.class);

        InOrder inOrder = inOrder(mResponseObserver);

        inOrder.verify(mResponseObserver)
                .onChanged(argThat(ResourceMatchers.<IdpResponse>isLoading()));
        inOrder.verify(mResponseObserver).onChanged(captor.capture());


        IdpResponse response = captor.getValue().getValue();

        assertThat(captor.getValue().getState()).isEqualTo(State.SUCCESS);

        assertThat(response.getUser().getProviderId()).isEqualTo(AuthUI.EMAIL_LINK_PROVIDER);
        assertThat(response.getUser().getEmail()).isEqualTo(mMockAuthResult.getUser()
                .getEmail());
        assertThat(response.getUser().getName()).isEqualTo(mMockAuthResult.getUser()
                .getDisplayName());
        assertThat(response.getUser().getPhotoUri()).isEqualTo(mMockAuthResult.getUser()
                .getPhotoUrl());
    }

    @Test
    @SuppressWarnings("all")
    public void testStartSignIn_linkingFlow_expectUserCollisionException() {
        mHandler.getOperation().observeForever(mResponseObserver);
        when(mMockAuth.isSignInWithEmailLink(any(String.class))).thenReturn(true);

        mPersistenceManager.saveEmail(RuntimeEnvironment.application, TestConstants.EMAIL,
                TestConstants.SESSION_ID, TestConstants.UID);
        mPersistenceManager.saveIdpResponseForLinking(RuntimeEnvironment.application,
                buildFacebookIdpResponse());

        when(mMockAuth.signInWithCredential(any(AuthCredential.class))).thenReturn
                (AutoCompleteTask.forSuccess(mMockAuthResult));

        // Mock linking with Facebook to always work
        when(mMockAuthResult.getUser().linkWithCredential(any(FacebookAuthCredential
                .class)))
                .thenReturn(AutoCompleteTask.<AuthResult>forFailure(
                        new FirebaseAuthUserCollisionException("foo", "bar")));

        mHandler.startSignIn();

        ArgumentCaptor<Resource<IdpResponse>> captor =
                ArgumentCaptor.forClass(Resource.class);

        InOrder inOrder = inOrder(mResponseObserver);

        inOrder.verify(mResponseObserver)
                .onChanged(argThat(ResourceMatchers.<IdpResponse>isLoading()));
        inOrder.verify(mResponseObserver).onChanged(captor.capture());


        assertThat(captor.getValue().getException()).isNotNull();
        FirebaseAuthUserCollisionException collisionException =
                (FirebaseAuthUserCollisionException) captor.getValue().getException();
        assertThat(mPersistenceManager.retrieveSessionRecord(RuntimeEnvironment.application))
                .isNull();
    }


    @Test
    @SuppressWarnings("all")
    public void testStartSignIn_linkingFlowWithAnonymousUpgradeEnabled_expectMergeFailure() {
        mHandler.getOperation().observeForever(mResponseObserver);
        setupAnonymousUpgrade();
        when(mMockAuth.isSignInWithEmailLink(any(String.class))).thenReturn(true);

        mPersistenceManager.saveEmail(RuntimeEnvironment.application, TestConstants.EMAIL,
                TestConstants.SESSION_ID, TestConstants.UID);
        mPersistenceManager.saveIdpResponseForLinking(RuntimeEnvironment.application,
                buildFacebookIdpResponse());

        // Need to control FirebaseAuth's return values
        AuthOperationManager authOperationManager = AuthOperationManager.getInstance();
        authOperationManager.mScratchAuth = mScratchMockAuth;

        when(mScratchMockAuth.signInWithCredential(any(AuthCredential.class)))
                .thenReturn(AutoCompleteTask.forSuccess(mMockAuthResult));

        // Mock linking with Facebook to always work
        when(mMockAuthResult.getUser().linkWithCredential(any(AuthCredential
                .class)))
                .thenReturn(new AutoContinueTask<>(mMockAuthResult,
                        mMockAuthResult,
                        true,
                        null));

        mHandler.startSignIn();

        // Validate regular sign in
        ArgumentCaptor<EmailAuthCredential> credentialCaptor
                = ArgumentCaptor.forClass(EmailAuthCredential.class);
        verify(mScratchMockAuth).signInWithCredential(credentialCaptor.capture());

        assertThat(credentialCaptor.getValue().getEmail()).isEqualTo(TestConstants.EMAIL);
        assertThat(credentialCaptor.getValue().getSignInMethod()).isEqualTo(EmailAuthProvider
                .EMAIL_LINK_SIGN_IN_METHOD);

        // Validate linking was called
        verify(mMockAuthResult.getUser()).linkWithCredential(any(FacebookAuthCredential
                .class));

        // Validate that the data was cleared
        assertThat(mPersistenceManager.retrieveSessionRecord(RuntimeEnvironment.application))
                .isNull();

        // Validate IdpResponse
        ArgumentCaptor<Resource<IdpResponse>> captor =
                ArgumentCaptor.forClass(Resource.class);

        InOrder inOrder = inOrder(mResponseObserver);

        inOrder.verify(mResponseObserver)
                .onChanged(argThat(ResourceMatchers.<IdpResponse>isLoading()));
        inOrder.verify(mResponseObserver).onChanged(captor.capture());


        FirebaseAuthAnonymousUpgradeException mergeException =
                ((FirebaseAuthAnonymousUpgradeException) captor.getValue().getException());

        IdpResponse response = mergeException.getResponse();
        assertThat(response.getCredentialForLinking()).isNotNull();
        assertThat(response.getCredentialForLinking().getProvider()).isEqualTo
                (FacebookAuthProvider.PROVIDER_ID);
    }

    @Test
    @SuppressWarnings("all")
    public void testStartSignIn_linkingFlowWithAnonymousUpgradeEnabled_failedSignInPropagated() {
        mHandler.getOperation().observeForever(mResponseObserver);
        setupAnonymousUpgrade();
        when(mMockAuth.isSignInWithEmailLink(any(String.class))).thenReturn(true);

        mPersistenceManager.saveEmail(RuntimeEnvironment.application, TestConstants.EMAIL,
                TestConstants.SESSION_ID, TestConstants.UID);
        mPersistenceManager.saveIdpResponseForLinking(RuntimeEnvironment.application,
                buildFacebookIdpResponse());

        // Need to control FirebaseAuth's return values
        AuthOperationManager authOperationManager = AuthOperationManager.getInstance();
        authOperationManager.mScratchAuth = mScratchMockAuth;

        when(mScratchMockAuth.signInWithCredential(any(AuthCredential.class)))
                .thenReturn(AutoContinueTask.<AuthResult>forFailure(new Exception("FAILED")));

        mHandler.startSignIn();

        // Verify sign in was called
        verify(mScratchMockAuth).signInWithCredential(any(AuthCredential.class));

        // Validate that the data was cleared
        assertThat(mPersistenceManager.retrieveSessionRecord(RuntimeEnvironment.application))
                .isNull();

        // Validate failure
        ArgumentCaptor<Resource<IdpResponse>> captor =
                ArgumentCaptor.forClass(Resource.class);

        InOrder inOrder = inOrder(mResponseObserver);

        inOrder.verify(mResponseObserver)
                .onChanged(argThat(ResourceMatchers.<IdpResponse>isLoading()));
        inOrder.verify(mResponseObserver).onChanged(captor.capture());

        assertThat(captor.getValue().getException()).isNotNull();
    }

    @Test
    @SuppressWarnings("all")
    public void testStartSignIn_linkingFlowWithAnonymousUpgradeEnabled_failedLinkPropagated() {
        mHandler.getOperation().observeForever(mResponseObserver);
        setupAnonymousUpgrade();
        when(mMockAuth.isSignInWithEmailLink(any(String.class))).thenReturn(true);

        mPersistenceManager.saveEmail(RuntimeEnvironment.application, TestConstants.EMAIL,
                TestConstants.SESSION_ID, TestConstants.UID);
        mPersistenceManager.saveIdpResponseForLinking(RuntimeEnvironment.application,
                buildFacebookIdpResponse());

        // Need to control FirebaseAuth's return values
        AuthOperationManager authOperationManager = AuthOperationManager.getInstance();
        authOperationManager.mScratchAuth = mScratchMockAuth;

        when(mScratchMockAuth.signInWithCredential(any(AuthCredential.class)))
                .thenReturn(AutoCompleteTask.forSuccess(mMockAuthResult));

        // Mock linking with Facebook to always work
        when(mMockAuthResult.getUser().linkWithCredential(any(AuthCredential
                .class)))
                .thenReturn(AutoContinueTask.<AuthResult>forFailure(new Exception("FAILED")));

        mHandler.startSignIn();

        // Verify sign in was called
        verify(mScratchMockAuth).signInWithCredential(any(AuthCredential.class));

        // Validate linking was called
        verify(mMockAuthResult.getUser()).linkWithCredential(any(FacebookAuthCredential
                .class));

        // Validate that the data was cleared
        assertThat(mPersistenceManager.retrieveSessionRecord(RuntimeEnvironment.application))
                .isNull();

        // Validate failure
        ArgumentCaptor<Resource<IdpResponse>> captor =
                ArgumentCaptor.forClass(Resource.class);

        InOrder inOrder = inOrder(mResponseObserver);

        inOrder.verify(mResponseObserver)
                .onChanged(argThat(ResourceMatchers.<IdpResponse>isLoading()));
        inOrder.verify(mResponseObserver).onChanged(captor.capture());

        assertThat(captor.getValue().getException()).isNotNull();
    }

    private IdpResponse buildFacebookIdpResponse() {
        User user = new User.Builder(FacebookAuthProvider.PROVIDER_ID, TestConstants.EMAIL)
                .build();

        return new IdpResponse.Builder(user)
                .setToken(TestConstants.TOKEN)
                .setSecret(TestConstants.SECRET)
                .build();
    }

    private void setupAnonymousUpgrade() {
        // enableAnonymousUpgrade must be set to true
        FlowParameters testParams = TestHelper.getFlowParameters(Collections.singletonList(
                EmailAuthProvider.PROVIDER_ID), /* enableAnonymousUpgrade */ true);

        testParams.emailLink = mHandler.getArguments().emailLink;
        mHandler.initializeForTesting(testParams, mMockAuth, null, null);

        // Mock isAnonymous() to return true so canUpgradeAnonymous will return true
        when(mMockAnonUser.isAnonymous()).thenReturn(true);
        when(mMockAuth.getCurrentUser()).thenReturn(mMockAnonUser);
    }

    private void initializeHandlerWithSessionInfo(@Nullable String sessionId,
                                                  @Nullable String anonymousUserId,
                                                  @Nullable String providerId,
                                                  boolean forceSameDevice) {
        FlowParameters testParams = TestHelper.getFlowParameters(new ArrayList<String>());
        ContinueUrlBuilder continueUrlBuilder = new ContinueUrlBuilder(CONTINUE_URL);
        continueUrlBuilder.appendSessionId(sessionId);
        continueUrlBuilder.appendAnonymousUserId(anonymousUserId);
        continueUrlBuilder.appendProviderId(providerId);
        continueUrlBuilder.appendForceSameDeviceBit(forceSameDevice);

        testParams.emailLink = EMAIL_LINK + continueUrlBuilder.build();

        mHandler.initializeForTesting(testParams, mMockAuth, null, null);
    }
}
