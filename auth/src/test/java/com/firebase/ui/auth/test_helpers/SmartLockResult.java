package com.firebase.ui.auth.test_helpers;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentActivity;

import com.firebase.ui.auth.ui.ExtraConstants;
import com.firebase.ui.auth.ui.FlowParameters;
import com.firebase.ui.auth.util.smartlock.SaveSmartLock;
import com.google.firebase.auth.FirebaseUser;

import java.util.concurrent.CountDownLatch;

import static org.junit.Assert.assertEquals;

public class SmartLockResult extends SaveSmartLock {
    private CountDownLatch mCountDownLatch;
    private String mPassword;
    private String mProvider;

    public void await() throws InterruptedException {
        mCountDownLatch.await();
    }

    @Override
    public void saveCredentialsOrFinish(Context context,
                                        FlowParameters parameters,
                                        FirebaseUser firebaseUser,
                                        @Nullable String password,
                                        @Nullable String provider) {
        assertEquals(firebaseUser.getEmail(), TestConstants.EMAIL);
        assertEquals(firebaseUser.getDisplayName(), TestConstants.NAME);
        assertEquals(firebaseUser.getPhotoUrl() != null
                             ? firebaseUser.getPhotoUrl().toString() : null, TestConstants.PHOTO_URL);
        assertEquals(password, mPassword);
        assertEquals(provider, mProvider);
        mCountDownLatch.countDown();
    }

    public static SmartLockResult newInstance(FragmentActivity activity,
                                              FlowParameters parameters,
                                              String tag,
                                              String password,
                                              String provider) {
        SmartLockResult result = new SmartLockResult();

        Bundle bundle = new Bundle();
        bundle.putParcelable(ExtraConstants.EXTRA_FLOW_PARAMS, parameters);
        result.setArguments(bundle);

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
