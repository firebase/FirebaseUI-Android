package com.firebase.ui.auth.testhelpers;

import android.support.v4.app.FragmentActivity;

import com.firebase.ui.auth.ui.BaseHelper;
import com.firebase.ui.auth.util.signincontainer.SaveSmartLock;
import com.google.android.gms.auth.api.credentials.CredentialsApi;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.PhoneAuthProvider;
import com.google.firebase.auth.FirebaseUser;

import org.mockito.Mockito;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;

import static org.mockito.Mockito.when;

@Implements(BaseHelper.class)
public class BaseHelperShadow {
    public static FirebaseAuth sFirebaseAuth;
    public static FirebaseUser sFirebaseUser;
    public static CredentialsApi sCredentialsApi;
    public static SaveSmartLock sSaveSmartLock;
    public static PhoneAuthProvider sPhoneAuthProvider;

    public BaseHelperShadow() {
        if (sFirebaseUser == null) {
            sFirebaseUser = Mockito.mock(FirebaseUser.class);
            when(sFirebaseUser.getEmail()).thenReturn(TestConstants.EMAIL);
            when(sFirebaseUser.getDisplayName()).thenReturn(TestConstants.NAME);
            when(sFirebaseUser.getPhotoUrl()).thenReturn(TestConstants.PHOTO_URI);
        }
        if (sFirebaseAuth == null) {
            sFirebaseAuth = Mockito.mock(FirebaseAuth.class);
            when(sFirebaseAuth.getCurrentUser()).thenReturn(sFirebaseUser);
        }
        if (sCredentialsApi == null) {
            sCredentialsApi = Mockito.mock(CredentialsApi.class);
        }
        if (sSaveSmartLock == null) {
            sSaveSmartLock = Mockito.mock(SaveSmartLock.class);
        }
        if (sPhoneAuthProvider == null) {
            sPhoneAuthProvider = Mockito.mock(PhoneAuthProvider.class);
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

    @Implementation
    public PhoneAuthProvider getPhoneAuthProviderInstance() {
        return sPhoneAuthProvider;
    }
}
