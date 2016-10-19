package com.firebase.ui.auth.ui.provider;

import android.os.Bundle;

import com.firebase.ui.auth.BuildConfig;
import com.firebase.ui.auth.provider.IdpProvider.IdpCallback;
import com.firebase.ui.auth.IdpResponse;
import com.firebase.ui.auth.provider.TwitterProvider;
import com.firebase.ui.auth.test_helpers.CustomRobolectricGradleTestRunner;
import com.firebase.ui.auth.test_helpers.FacebookProviderShadow;
import com.firebase.ui.auth.test_helpers.FirebaseAuthWrapperImplShadow;
import com.firebase.ui.auth.test_helpers.GoogleProviderShadow;
import com.twitter.sdk.android.core.Result;
import com.twitter.sdk.android.core.TwitterAuthToken;
import com.twitter.sdk.android.core.TwitterException;
import com.twitter.sdk.android.core.TwitterSession;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.util.concurrent.CountDownLatch;

import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;

@RunWith(CustomRobolectricGradleTestRunner.class)
@Config(constants = BuildConfig.class,
        shadows = {
                FirebaseAuthWrapperImplShadow.class,
                GoogleProviderShadow.class,
                FacebookProviderShadow.class
        }, sdk = 21)
public class TwitterProviderTest {
    private static final String FAKE_AUTH_TOKEN = "fakeAuthToken";
    private static final String FAKE_AUTH_SECRET = "fakeAuthSecret";
    private static final long FAKE_USER_ID = 555;
    private static final String FAKE_USER_NAME = "testAccountName";

    private static class AssertResultCallback implements IdpCallback {
        private CountDownLatch mCountDownLatch;
        private boolean mAssertSuccess;

        public AssertResultCallback(boolean assertSuccess) {
            mCountDownLatch = new CountDownLatch(1);
            mAssertSuccess = assertSuccess;
        }

        private void await() throws InterruptedException {
            mCountDownLatch.await();
        }

        @Override
        public void onSuccess(IdpResponse idpResponse) {
            assertTrue(mAssertSuccess);
            mCountDownLatch.countDown();
        }

        @Override
        public void onFailure(Bundle extra) {
            assertFalse(mAssertSuccess);
            mCountDownLatch.countDown();
        }
    }

    @Test
    public void testSuccessCallsCallback() {
        TwitterProvider twitterProvider = new TwitterProvider(RuntimeEnvironment.application);

        AssertResultCallback assertResultCallback = new AssertResultCallback(true);
        twitterProvider.setAuthenticationCallback(assertResultCallback);

        TwitterAuthToken twitterAuthToken = new TwitterAuthToken(FAKE_AUTH_TOKEN, FAKE_AUTH_SECRET);
        TwitterSession twitterSession = new TwitterSession(
                twitterAuthToken,
                FAKE_USER_ID,
                FAKE_USER_NAME);

        Result<TwitterSession> result = new Result<>(twitterSession, null);
        twitterProvider.success(result);

        try {
            assertResultCallback.await();
        } catch (InterruptedException e) {
            assertTrue("Interrupted waiting for result", false);
        }
    }

    @Test
    public void testFailureCallsCallback() {
        TwitterProvider twitterProvider = new TwitterProvider(RuntimeEnvironment.application);

        AssertResultCallback assertResultCallback = new AssertResultCallback(false);
        twitterProvider.setAuthenticationCallback(assertResultCallback);

        TwitterException twitterException = new TwitterException("Fake exception");
        twitterProvider.failure(twitterException);

        try {
            assertResultCallback.await();
        } catch (InterruptedException e) {
            assertTrue("Interrupted waiting for result", false);
        }
    }
}
