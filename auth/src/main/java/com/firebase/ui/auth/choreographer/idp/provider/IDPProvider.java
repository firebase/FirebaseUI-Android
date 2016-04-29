package com.firebase.ui.auth.choreographer.idp.provider;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

public interface IDPProvider {
    View getLoginButton(Context context);

    void setAuthenticationCallback(IDPCallback callback);

    void onActivityResult(int requestCode, int resultCode, Intent data);

    void startLogin(Activity activity, String mEmail);

    public interface IDPCallback {
        public void onSuccess(IDPResponse idpResponse);
        public void onFailure(Bundle extra);
    }
}
