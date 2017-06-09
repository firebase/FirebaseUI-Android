/*
 * Copyright 2017 Google Inc. All Rights Reserved.
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

package com.firebase.ui.auth.ui.phone;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.firebase.ui.auth.R;

public final class CompletableProgressDialog extends ProgressDialog {
    private ProgressBar mProgress;
    private TextView mMessageView;
    private CharSequence mMessage;
    private ImageView mSuccessImage;

    public CompletableProgressDialog(Context context) {
        super(context);
    }

    public CompletableProgressDialog(Context context, int theme) {
        super(context, theme);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.phone_progress_dialog);

        mProgress = (ProgressBar) findViewById(R.id.progress_bar);
        mMessageView = (TextView) findViewById(R.id.progress_msg);
        mSuccessImage = (ImageView) findViewById(R.id.progress_success_imaage);

        if (mMessage != null) {
            setMessage(mMessage);
        }
    }

    @Override
    public void show() {
        super.show();
    }

    public void complete(String msg) {
        setMessage(msg);
        mProgress.setVisibility(View.GONE);
        mSuccessImage.setVisibility(View.VISIBLE);
    }

    @Override
    public void setMessage(CharSequence message) {
        if (mProgress != null) {
            mMessageView.setText(message);
        } else {
            mMessage = message;
        }
    }
}
