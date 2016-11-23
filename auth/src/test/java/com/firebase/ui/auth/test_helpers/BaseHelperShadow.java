package com.firebase.ui.auth.test_helpers;

import com.firebase.ui.auth.ui.BaseHelper;
import com.google.android.gms.auth.api.credentials.CredentialsApi;
import com.google.firebase.auth.FirebaseAuth;

import org.mockito.Mockito;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;

@Implements(BaseHelper.class)
public class BaseHelperShadow {
    public static FirebaseAuth sFirebaseAuth;
    public static CredentialsApi sCredentialsApi;

    public BaseHelperShadow() {
        if (sFirebaseAuth == null) {
            sFirebaseAuth = Mockito.mock(FirebaseAuth.class);
        }
        if (sCredentialsApi == null) {
            sCredentialsApi = Mockito.mock(CredentialsApi.class);
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
}
