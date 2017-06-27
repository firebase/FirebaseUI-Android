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

    static {
        // FirebaseAuth
        sFirebaseAuth = Mockito.mock(FirebaseAuth.class);
        when(sFirebaseAuth.getCurrentUser()).thenReturn(sFirebaseUser);

        // CredentialsApi
        sCredentialsApi = Mockito.mock(CredentialsApi.class);

        // FirebaseUser
        sFirebaseUser = Mockito.mock(FirebaseUser.class);
        when(sFirebaseUser.getEmail()).thenReturn(TestConstants.EMAIL);
        when(sFirebaseUser.getDisplayName()).thenReturn(TestConstants.NAME);
        when(sFirebaseUser.getPhotoUrl()).thenReturn(TestConstants.PHOTO_URI);

        // SaveSmartLock
        sSaveSmartLock = Mockito.mock(SaveSmartLock.class);

        // PhoneAuthProvider
        sPhoneAuthProvider = Mockito.mock(PhoneAuthProvider.class);
    }

    private AuthInstancesShadow() {}

    @Implementation
    public static FirebaseAuth getFirebaseAuth(FlowParameters parameters) {
        return sFirebaseAuth;
    }

    @Implementation
    public static CredentialsApi getCredentialsApi() {
        return sCredentialsApi;
    }

    @Implementation
    public static FirebaseUser getCurrentUser(FlowParameters parameters) {
        return sFirebaseUser;
    }

    @Implementation
    public static SaveSmartLock getSaveSmartLockInstance(FragmentActivity activity,
                                                         FlowParameters parameters) {
        return sSaveSmartLock;
    }

    @Implementation
    public static PhoneAuthProvider getPhoneAuthProviderInstance() {
        return sPhoneAuthProvider;
    }

}
