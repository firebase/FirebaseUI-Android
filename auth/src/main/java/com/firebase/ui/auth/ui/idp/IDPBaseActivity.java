package com.firebase.ui.auth.ui.idp;

import android.app.Activity;
import android.os.Bundle;

import com.firebase.ui.auth.choreographer.Controller;
import com.firebase.ui.auth.choreographer.idp.IDPController;
import com.firebase.ui.auth.ui.BaseActivity;

public class IDPBaseActivity extends BaseActivity {

    public static final int EMAIL_LOGIN_NEEDED = Activity.RESULT_FIRST_USER + 2;
    public static final int LOGIN_CANCELLED = Activity.RESULT_FIRST_USER + 3;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    protected Controller setUpController() {
        return new IDPController(this, mAppName);
    }
}
