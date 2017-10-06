/*
 * Copyright 2016 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.firebase.ui.auth.ui.provider;

import android.arch.lifecycle.ViewModelProviders;
import android.support.annotation.LayoutRes;

import com.facebook.WebDialog;
import com.facebook.login.LoginManager;
import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.R;
import com.firebase.ui.auth.data.remote.FacebookSignInHandler;
import com.firebase.ui.auth.ui.HelperActivityBase;

public class FacebookProvider implements Provider {
    private final FacebookSignInHandler mHandler;

    public FacebookProvider(HelperActivityBase activity, AuthUI.IdpConfig idpConfig) {
        WebDialog.setWebDialogTheme(activity.getFlowHolder().getParams().themeId);
        mHandler = ViewModelProviders.of(activity).get(FacebookSignInHandler.class);
        mHandler.init(new FacebookSignInHandler.Params(
                idpConfig, activity.getSignInHandler(), activity.getFlowHolder()));
    }

    @Override
    @LayoutRes
    public int getButtonLayout() {
        return R.layout.fui_idp_button_facebook;
    }

    @Override
    public void startLogin(HelperActivityBase activity) {
        LoginManager.getInstance().logInWithReadPermissions(activity, mHandler.getPermissions());
    }
}
