package com.firebase.ui.auth.util;

import com.google.android.gms.auth.api.credentials.Credential;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.tasks.Task;

public interface CredentialTaskApi {
    Task<Status> disableAutoSignIn();

    Task<Status> delete(Credential credential);
}
