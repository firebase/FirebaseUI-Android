package com.firebase.ui.auth.ui.email;

import com.firebase.ui.auth.choreographer.Controller;
import com.firebase.ui.auth.choreographer.email.EmailFlowController;
import com.firebase.ui.auth.ui.BaseActivity;

public class EmailFlowBaseActivity extends BaseActivity {
    @Override
    protected Controller setUpController() {
        return new EmailFlowController(getApplicationContext(), mAppName);
    }
}
