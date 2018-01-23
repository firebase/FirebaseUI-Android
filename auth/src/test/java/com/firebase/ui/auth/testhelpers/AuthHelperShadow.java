package com.firebase.ui.auth.testhelpers;

import com.firebase.ui.auth.util.AuthHelper;
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

    private static FirebaseAuth sFirebaseAuth;
    private static FirebaseUser sFirebaseUser;
    private static CredentialsApi sCredentialsApi;
    private static PhoneAuthProvider sPhoneAuthProvider;

    public AuthHelperShadow() {}

    @Implementation
    public static FirebaseAuth getFirebaseAuth() {
        if (sFirebaseAuth == null) {
            sFirebaseAuth = Mockito.mock(FirebaseAuth.class);
            when(sFirebaseAuth.getCurrentUser()).thenReturn(sFirebaseUser);
        }

        return sFirebaseAuth;
    }

    @Implementation
    public static CredentialsApi getCredentialsApi() {
        if (sCredentialsApi == null) {
            sCredentialsApi = Mockito.mock(CredentialsApi.class);
        }

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
    public static PhoneAuthProvider getPhoneAuthProvider() {
        if (sPhoneAuthProvider == null) {
            sPhoneAuthProvider = Mockito.mock(PhoneAuthProvider.class);
        }

        return sPhoneAuthProvider;
    }

}
