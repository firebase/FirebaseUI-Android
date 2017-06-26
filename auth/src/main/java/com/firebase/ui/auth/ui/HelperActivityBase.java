package com.firebase.ui.auth.ui;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RestrictTo;
import android.support.v7.app.AppCompatActivity;

import com.firebase.ui.auth.IdpResponse;
import com.firebase.ui.auth.util.signincontainer.SaveSmartLock;
import com.google.firebase.auth.FirebaseUser;

@SuppressWarnings("Registered")
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class HelperActivityBase extends AppCompatActivity {

    protected BaseHelper mActivityHelper;

    @Override
    protected void onCreate(Bundle savedInstance) {
        super.onCreate(savedInstance);
        mActivityHelper = new BaseHelper(this, getIntent());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mActivityHelper.dismissDialog();
    }

    public void finish(int resultCode, Intent intent) {
        BaseHelper.finishActivity(this, resultCode, intent);
    }

    public void saveCredentialsOrFinish(
            @Nullable SaveSmartLock saveSmartLock,
            FirebaseUser firebaseUser,
            @NonNull String password,
            IdpResponse response) {
        mActivityHelper.saveCredentialsOrFinish(saveSmartLock, this, firebaseUser, password, response);
    }

    public void saveCredentialsOrFinish(
            @Nullable SaveSmartLock saveSmartLock,
            FirebaseUser firebaseUser,
            IdpResponse response) {
        mActivityHelper.saveCredentialsOrFinish(saveSmartLock, this, firebaseUser, null, response);
    }
}

