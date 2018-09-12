package com.firebase.ui.auth.viewmodel;

import android.app.Activity;
import android.arch.lifecycle.Observer;

import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.FirebaseAuthAnonymousUpgradeException;
import com.firebase.ui.auth.IdpResponse;
import com.firebase.ui.auth.data.model.FlowParameters;
import com.firebase.ui.auth.data.model.IntentRequiredException;
import com.firebase.ui.auth.data.model.Resource;
import com.firebase.ui.auth.data.model.User;
import com.firebase.ui.auth.testhelpers.AutoCompleteTask;
import com.firebase.ui.auth.testhelpers.FakeAuthResult;
import com.firebase.ui.auth.testhelpers.FakeSignInMethodQueryResult;
import com.firebase.ui.auth.testhelpers.ResourceMatchers;
import com.firebase.ui.auth.testhelpers.TestConstants;
import com.firebase.ui.auth.testhelpers.TestHelper;
import com.firebase.ui.auth.ui.email.WelcomeBackPasswordPrompt;
import com.firebase.ui.auth.ui.idp.WelcomeBackIdpPrompt;
import com.firebase.ui.auth.viewmodel.idp.SocialProviderResponseHandler;
import com.firebase.ui.auth.viewmodel.smartlock.SmartLockHandler;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FacebookAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GithubAuthProvider;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.auth.SignInMethodQueryResult;

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
import java.util.Arrays;
import java.util.Collections;

import static com.google.common.truth.Truth.assertThat;
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
    @Mock FirebaseUser mUser;
    @Mock Observer<Resource<IdpResponse>> mResultObserver;

    private SocialProviderResponseHandler mHandler;

    private static final ArrayList<String> NON_GITHUB_PROVIDERS = new ArrayList<>();
    static {
        NON_GITHUB_PROVIDERS.addAll(AuthUI.SUPPORTED_PROVIDERS);
        NON_GITHUB_PROVIDERS.remove(GithubAuthProvider.PROVIDER_ID);
    }

    @Before
    public void setUp() {
        TestHelper.initialize();
        MockitoAnnotations.initMocks(this);

        mHandler = new SocialProviderResponseHandler(RuntimeEnvironment.application);
        FlowParameters testParams = TestHelper.getFlowParameters(NON_GITHUB_PROVIDERS);

        mHandler.initializeForTesting(testParams, mMockAuth, null, null);
    }

    @Test
    public void testSignInIdp_success() {
        mHandler.getOperation().observeForever(mResultObserver);

        when(mMockAuth.signInWithCredential(any(AuthCredential.class)))
                .thenReturn(AutoCompleteTask.forSuccess(FakeAuthResult.INSTANCE));

        IdpResponse response = new IdpResponse.Builder(new User.Builder(
                GoogleAuthProvider.PROVIDER_ID, TestConstants.EMAIL).build())
                .setToken(TestConstants.TOKEN)
                .build();

        mHandler.startSignIn(response);

        verify(mMockAuth).signInWithCredential(any(AuthCredential.class));

        InOrder inOrder = inOrder(mResultObserver);
        inOrder.verify(mResultObserver)
                .onChanged(argThat(ResourceMatchers.<IdpResponse>isLoading()));
        inOrder.verify(mResultObserver)
                .onChanged(argThat(ResourceMatchers.<IdpResponse>isSuccess()));
    }

    @Test(expected = IllegalStateException.class)
    public void testSignInNonIdp_failure() {
        mHandler.getOperation().observeForever(mResultObserver);

        IdpResponse response = new IdpResponse.Builder(new User.Builder(
                EmailAuthProvider.PROVIDER_ID, TestConstants.EMAIL).build())
                .build();

        mHandler.startSignIn(response);
    }

    @Test
    public void testSignInResponse_failure() {
        mHandler.getOperation().observeForever(mResultObserver);

        IdpResponse response = IdpResponse.from(new Exception("Failure"));

        mHandler.startSignIn(response);

        verify(mResultObserver).onChanged(argThat(ResourceMatchers.<IdpResponse>isFailure()));
    }

    @Test
    public void testSignInIdp_resolution() {
        mHandler.getOperation().observeForever(mResultObserver);

        when(mMockAuth.signInWithCredential(any(AuthCredential.class)))
                .thenReturn(AutoCompleteTask.<AuthResult>forFailure(
                        new FirebaseAuthUserCollisionException("foo", "bar")));
        when(mMockAuth.fetchSignInMethodsForEmail(any(String.class)))
                .thenReturn(AutoCompleteTask.<SignInMethodQueryResult>forSuccess(
                        new FakeSignInMethodQueryResult(Collections.singletonList(
                                FacebookAuthProvider.FACEBOOK_SIGN_IN_METHOD))));

        IdpResponse response = new IdpResponse.Builder(new User.Builder(
                GoogleAuthProvider.PROVIDER_ID, TestConstants.EMAIL).build())
                .setToken(TestConstants.TOKEN)
                .build();

        mHandler.startSignIn(response);

        verify(mMockAuth).signInWithCredential(any(AuthCredential.class));
        verify(mMockAuth).fetchSignInMethodsForEmail(any(String.class));

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


    @Test
    public void testSignInIdp_anonymousUserUpgradeEnabledAndNewUser_expectSuccess() {
        mHandler.getOperation().observeForever(mResultObserver);
        setupAnonymousUpgrade();

        when(mMockAuth.getCurrentUser().linkWithCredential(any(AuthCredential.class)))
                .thenReturn(AutoCompleteTask.forSuccess(FakeAuthResult.INSTANCE));

        IdpResponse response = new IdpResponse.Builder(new User.Builder(
                GoogleAuthProvider.PROVIDER_ID, TestConstants.EMAIL).build())
                .setToken(TestConstants.TOKEN)
                .build();

        mHandler.startSignIn(response);

        verify(mMockAuth.getCurrentUser()).linkWithCredential(any(AuthCredential.class));

        InOrder inOrder = inOrder(mResultObserver);
        inOrder.verify(mResultObserver)
                .onChanged(argThat(ResourceMatchers.<IdpResponse>isLoading()));
        inOrder.verify(mResultObserver)
                .onChanged(argThat(ResourceMatchers.<IdpResponse>isSuccess()));
    }

    @Test
    public void testSignInIdp_anonymousUserUpgradeEnabledAndExistingUserWithSameIdp_expectMergeFailure() {
        mHandler.getOperation().observeForever(mResultObserver);
        setupAnonymousUpgrade();

        when(mMockAuth.getCurrentUser().linkWithCredential(any(AuthCredential.class)))
                .thenReturn(AutoCompleteTask.<AuthResult>forFailure(
                        new FirebaseAuthUserCollisionException("foo", "bar")));

        // Case 1: Anon user signing in with a Google credential that belongs to an existing user.
        when(mMockAuth.fetchSignInMethodsForEmail(any(String.class)))
                .thenReturn(AutoCompleteTask.<SignInMethodQueryResult>forSuccess(
                        new FakeSignInMethodQueryResult(Arrays.asList(
                                GoogleAuthProvider.GOOGLE_SIGN_IN_METHOD,
                                FacebookAuthProvider.FACEBOOK_SIGN_IN_METHOD))));


        IdpResponse response = new IdpResponse.Builder(new User.Builder(
                GoogleAuthProvider.PROVIDER_ID, TestConstants.EMAIL).build())
                .setToken(TestConstants.TOKEN)
                .build();

        mHandler.startSignIn(response);

        verify(mMockAuth.getCurrentUser()).linkWithCredential(any(AuthCredential.class));

        InOrder inOrder = inOrder(mResultObserver);
        inOrder.verify(mResultObserver)
                .onChanged(argThat(ResourceMatchers.<IdpResponse>isLoading()));

        ArgumentCaptor<Resource<IdpResponse>> resolveCaptor =
                ArgumentCaptor.forClass(Resource.class);
        inOrder.verify(mResultObserver).onChanged(resolveCaptor.capture());

        FirebaseAuthAnonymousUpgradeException e =
                (FirebaseAuthAnonymousUpgradeException) resolveCaptor.getValue().getException();

        assertThat(e.getResponse().getCredentialForLinking()).isNotNull();
    }

    @Test
    public void testSignInIdp_anonymousUserUpgradeEnabledAndExistingIdpUserWithDifferentIdp_expectMergeFailure() {
        mHandler.getOperation().observeForever(mResultObserver);
        setupAnonymousUpgrade();

        when(mMockAuth.getCurrentUser().linkWithCredential(any(AuthCredential.class)))
                .thenReturn(AutoCompleteTask.<AuthResult>forFailure(
                        new FirebaseAuthUserCollisionException("foo", "bar")));

        // Case 2 & 3: trying to link with an account that has 1 idp, which is different from the
        // one that we're trying to log in with
        when(mMockAuth.fetchSignInMethodsForEmail(any(String.class)))
                .thenReturn(AutoCompleteTask.<SignInMethodQueryResult>forSuccess(
                        new FakeSignInMethodQueryResult(Collections.singletonList(
                                FacebookAuthProvider.FACEBOOK_SIGN_IN_METHOD))));

        IdpResponse response = new IdpResponse.Builder(new User.Builder(
                GoogleAuthProvider.PROVIDER_ID, TestConstants.EMAIL).build())
                .setToken(TestConstants.TOKEN)
                .build();

        mHandler.startSignIn(response);

        verify(mMockAuth.getCurrentUser()).linkWithCredential(any(AuthCredential.class));

        InOrder inOrder = inOrder(mResultObserver);
        inOrder.verify(mResultObserver)
                .onChanged(argThat(ResourceMatchers.<IdpResponse>isLoading()));

        ArgumentCaptor<Resource<IdpResponse>> resolveCaptor =
                ArgumentCaptor.forClass(Resource.class);
        inOrder.verify(mResultObserver).onChanged(resolveCaptor.capture());

        // Make sure that we are trying to start the WelcomeBackIdpPrompt activity
        IntentRequiredException e =
                ((IntentRequiredException) resolveCaptor.getValue().getException());
        assertThat(e.getIntent().getComponent().getClassName())
                .isEqualTo(WelcomeBackIdpPrompt.class.toString().split(" ")[1]);

        assertThat(IdpResponse.fromResultIntent(e.getIntent())).isEqualTo(response);

    }

    @Test
    public void testSignInIdp_anonymousUserUpgradeEnabledAndExistingPasswordUserWithDifferentIdp_expectMergeFailure() {
        mHandler.getOperation().observeForever(mResultObserver);
        setupAnonymousUpgrade();

        when(mMockAuth.getCurrentUser().linkWithCredential(any(AuthCredential.class)))
                .thenReturn(AutoCompleteTask.<AuthResult>forFailure(
                        new FirebaseAuthUserCollisionException("foo", "bar")));

        // Case 2 & 3: trying to link with an account that has 1 password provider and logging in
        // with an idp that has the same email
        when(mMockAuth.fetchSignInMethodsForEmail(any(String.class)))
                .thenReturn(AutoCompleteTask.<SignInMethodQueryResult>forSuccess(
                        new FakeSignInMethodQueryResult(Collections.singletonList(
                                EmailAuthProvider.EMAIL_PASSWORD_SIGN_IN_METHOD))));

        IdpResponse response = new IdpResponse.Builder(new User.Builder(
                FacebookAuthProvider.PROVIDER_ID, TestConstants.EMAIL).build())
                .setToken(TestConstants.TOKEN)
                .setSecret(TestConstants.SECRET)
                .build();

        mHandler.startSignIn(response);

        verify(mMockAuth.getCurrentUser()).linkWithCredential(any(AuthCredential.class));

        InOrder inOrder = inOrder(mResultObserver);
        inOrder.verify(mResultObserver)
                .onChanged(argThat(ResourceMatchers.<IdpResponse>isLoading()));

        ArgumentCaptor<Resource<IdpResponse>> resolveCaptor =
                ArgumentCaptor.forClass(Resource.class);
        inOrder.verify(mResultObserver).onChanged(resolveCaptor.capture());

        // Make sure that we are trying to start the WelcomeBackIdpPrompt activity
        IntentRequiredException e =
                ((IntentRequiredException) resolveCaptor.getValue().getException());
        assertThat(e.getIntent().getComponent().getClassName())
                .isEqualTo(WelcomeBackPasswordPrompt.class.toString().split(" ")[1]);

        assertThat(IdpResponse.fromResultIntent(e.getIntent())).isEqualTo(response);
    }

    private void setupAnonymousUpgrade() {
        // enableAnonymousUpgrade must be set to true
        FlowParameters testParams = TestHelper.getFlowParameters(NON_GITHUB_PROVIDERS,
                /* enableAnonymousUpgrade */ true);
        mHandler.initializeForTesting(testParams, mMockAuth, null, null);

        when(mUser.isAnonymous()).thenReturn(true);
        when(mMockAuth.getCurrentUser()).thenReturn(mUser);
    }

}
