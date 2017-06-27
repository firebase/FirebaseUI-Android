package com.firebase.ui.auth.testhelpers;

import android.support.v4.app.FragmentActivity;

import com.firebase.ui.auth.ui.FlowParameters;
import com.firebase.ui.auth.util.AuthInstances;
import com.firebase.ui.auth.util.signincontainer.SaveSmartLock;
import com.google.android.gms.auth.api.credentials.CredentialsApi;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.PhoneAuthProvider;

import org.mockito.Mockito;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;

import static org.mockito.Mockito.when;

@Implements(AuthInstances.class)
public class AuthInstancesShadow {

    public static FirebaseAuth sFirebaseAuth;
    public static FirebaseUser sFirebaseUser;
    public static CredentialsApi sCredentialsApi;
    public static SaveSmartLock sSaveSmartLock;
    public static PhoneAuthProvider sPhoneAuthProvider;

    private AuthInstancesShadow() {}

    @Implementation
    public static FirebaseAuth getFirebaseAuth(FlowParameters parameters) {
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
    public static FirebaseUser getCurrentUser(FlowParameters parameters) {
        if (sFirebaseUser == null) {
            sFirebaseUser = Mockito.mock(FirebaseUser.class);
            when(sFirebaseUser.getEmail()).thenReturn(TestConstants.EMAIL);
            when(sFirebaseUser.getDisplayName()).thenReturn(TestConstants.NAME);
            when(sFirebaseUser.getPhotoUrl()).thenReturn(TestConstants.PHOTO_URI);
        }

        return sFirebaseUser;
    }

    @Implementation
    public static SaveSmartLock getSaveSmartLockInstance(FragmentActivity activity,
                                                         FlowParameters parameters) {
        if (sSaveSmartLock == null) {
            sSaveSmartLock = Mockito.mock(SaveSmartLock.class);
        }

        return sSaveSmartLock;
    }

    @Implementation
    public static PhoneAuthProvider getPhoneAuthProviderInstance() {
        if (sPhoneAuthProvider == null) {
            sPhoneAuthProvider = Mockito.mock(PhoneAuthProvider.class);
        }

        return sPhoneAuthProvider;
    }

}
