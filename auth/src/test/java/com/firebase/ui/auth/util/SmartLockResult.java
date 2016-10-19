package com.firebase.ui.auth.util;

import android.support.annotation.Nullable;
import android.support.v4.app.FragmentActivity;

import com.firebase.ui.auth.test_helpers.TestConstants;
import com.firebase.ui.auth.ui.ActivityHelper;
import com.firebase.ui.auth.ui.AppCompatBase;
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
                                        @Nullable String provider) {
        assertEquals(firebaseUser.getEmail(), TestConstants.EMAIL);
        assertEquals(firebaseUser.getDisplayName(), TestConstants.NAME);
        assertEquals(firebaseUser.getPhotoUrl().toString(), TestConstants.PHOTO_URL);
        assertEquals(password, mPassword);
        assertEquals(provider, mProvider);
        mCountDownLatch.countDown();
    }

    public static SmartLockResult assertSmartLockResult(FragmentActivity activity,
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
