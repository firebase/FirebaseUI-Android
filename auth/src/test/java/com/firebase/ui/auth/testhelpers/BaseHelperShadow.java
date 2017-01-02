package com.firebase.ui.auth.testhelpers;

import android.support.v4.app.FragmentActivity;

import com.firebase.ui.auth.ui.BaseHelper;
import com.firebase.ui.auth.util.signincontainer.SaveSmartLock;
import com.google.android.gms.auth.api.credentials.CredentialsApi;
import com.google.firebase.auth.FirebaseAuth;

import org.mockito.Mockito;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;

@Implements(BaseHelper.class)
public class BaseHelperShadow {
    public static FirebaseAuth sFirebaseAuth;
    public static CredentialsApi sCredentialsApi;
    public static SaveSmartLock sSaveSmartLock;

    public BaseHelperShadow() {
        if (sFirebaseAuth == null) {
            sFirebaseAuth = Mockito.mock(FirebaseAuth.class);
        }
        if (sCredentialsApi == null) {
            sCredentialsApi = Mockito.mock(CredentialsApi.class);
        }
        if (sSaveSmartLock == null) {
            sSaveSmartLock = Mockito.mock(SaveSmartLock.class);
        }
    }

    @Implementation
    public FirebaseAuth getFirebaseAuth() {
        return sFirebaseAuth;
    }

    @Implementation
    public CredentialsApi getCredentialsApi() {
        return sCredentialsApi;
    }

    @Implementation
    public SaveSmartLock getSaveSmartLockInstance(FragmentActivity activity) {
        return sSaveSmartLock;
    }
}
