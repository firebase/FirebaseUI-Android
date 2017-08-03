package com.firebase.ui.auth.testhelpers;

import com.firebase.ui.auth.ui.HelperActivityBase;
import com.firebase.ui.auth.util.AuthHelper;
import com.firebase.ui.auth.util.signincontainer.SaveSmartLock;
import com.google.android.gms.auth.api.credentials.CredentialsApi;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.PhoneAuthProvider;

import org.mockito.Mockito;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;

import static org.mockito.Mockito.when;

@Implements(AuthHelper.class)
public class AuthHelperShadow {

    public static final FirebaseAuth sFirebaseAuth;
    public static final CredentialsApi sCredentialsApi;
    public static final SaveSmartLock sSaveSmartLock;
    public static final PhoneAuthProvider sPhoneAuthProvider;

    private static FirebaseUser sFirebaseUser;

    static {
        // CredentialsApi
        sCredentialsApi = Mockito.mock(CredentialsApi.class);

        // FirebaseAuth
        sFirebaseAuth = Mockito.mock(FirebaseAuth.class);
        when(sFirebaseAuth.getCurrentUser()).thenReturn(sFirebaseUser);

        // SaveSmartLock
        sSaveSmartLock = Mockito.mock(SaveSmartLock.class);

        // PhoneAuthProvider
        sPhoneAuthProvider = Mockito.mock(PhoneAuthProvider.class);
    }

    public AuthHelperShadow() {}

    @Implementation
    public static FirebaseAuth getFirebaseAuth() {
        return sFirebaseAuth;
    }

    @Implementation
    public static CredentialsApi getCredentialsApi() {
        return sCredentialsApi;
    }

    @Implementation
    public static FirebaseUser getCurrentUser() {
        if (sFirebaseUser == null) {
            sFirebaseUser = TestHelper.getMockFirebaseUser();
        }

        return sFirebaseUser;
    }

    @Implementation
    public static SaveSmartLock getSaveSmartLockInstance(HelperActivityBase activity) {
        return sSaveSmartLock;
    }

    @Implementation
    public static PhoneAuthProvider getPhoneAuthProvider() {
        return sPhoneAuthProvider;
    }

}
