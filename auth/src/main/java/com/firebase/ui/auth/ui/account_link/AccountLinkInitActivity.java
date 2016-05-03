package com.firebase.ui.auth.ui.account_link;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.firebase.ui.auth.choreographer.Controller;
import com.firebase.ui.auth.choreographer.ControllerConstants;
import com.firebase.ui.auth.choreographer.account_link.AccountLinkController;
import com.firebase.ui.auth.ui.BaseActivity;

public class AccountLinkInitActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mId = AccountLinkController.ID_INIT;
        finish(RESULT_OK, getIntent());
    }

    @Override
    protected Controller setUpController() {
        return new AccountLinkController(getApplicationContext(), mAppName);
    }

    public static Intent createStartIntent(Context context, String appName, String id, String
            provider) {
        return new Intent(context, AccountLinkInitActivity.class).putExtra(ControllerConstants
                .EXTRA_APP_NAME, appName).putExtra(ControllerConstants.EXTRA_EMAIL, id).putExtra
                (ControllerConstants.EXTRA_PROVIDER, provider);
    }
}
