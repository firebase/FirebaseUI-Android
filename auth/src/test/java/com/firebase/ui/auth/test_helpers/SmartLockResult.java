package com.firebase.ui.auth.test_helpers;

import android.support.annotation.Nullable;
import android.support.v4.app.FragmentActivity;

import com.firebase.ui.auth.IdpResponse;
import com.firebase.ui.auth.ui.ActivityHelper;
import com.firebase.ui.auth.ui.AppCompatBase;
import com.firebase.ui.auth.util.SmartLock;
import com.google.firebase.auth.FirebaseUser;

import java.util.concurrent.CountDownLatch;

import static org.junit.Assert.assertEquals;

public class SmartLockResult extends SmartLock {
    private CountDownLatch mCountDownLatch;
    private String mPassword;
    private String mProvider;

    public void await() throws InterruptedException {
        mCountDownLatch.await();
    }

    @Override
    public void saveCredentialsOrFinish(AppCompatBase activity,
                                        ActivityHelper helper,
                                        FirebaseUser firebaseUser,
                                        @Nullable String password,
                                        @Nullable IdpResponse idpResponse) {
        assertEquals(TestConstants.EMAIL, firebaseUser.getEmail());
        assertEquals(TestConstants.NAME, firebaseUser.getDisplayName());
        assertEquals(TestConstants.PHOTO_URL, firebaseUser.getPhotoUrl() != null
                             ? firebaseUser.getPhotoUrl().toString() : null);
        assertEquals(mPassword, password);
        String provider = null;
        if (idpResponse != null) {
            provider = idpResponse.getProviderType();
        }
        assertEquals(mProvider, provider);
        mCountDownLatch.countDown();
    }

    public static SmartLockResult newInstance(FragmentActivity activity,
                                              String tag,
                                              String password,
                                              String provider) {
        SmartLockResult result = new SmartLockResult();

        result.mCountDownLatch = new CountDownLatch(1);
        result.mPassword = password;
        result.mProvider = provider;

        activity.getSupportFragmentManager()
                .beginTransaction()
                .add(result, tag)
                .commit();

        return result;
    }
}
