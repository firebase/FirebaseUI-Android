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

import android.app.Dialog;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.VisibleForTesting;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.firebase.ui.auth.R;

public final class CompletableProgressDialog extends DialogFragment {
    private static final String TAG = "ComProgressDialog";

    private ProgressBar mProgress;
    @VisibleForTesting TextView mMessageView;
    private CharSequence mMessage;
    private ImageView mSuccessImage;

    public static CompletableProgressDialog show(FragmentManager manager) {
        CompletableProgressDialog dialog = new CompletableProgressDialog();
        dialog.showAllowingStateLoss(manager, TAG);
        return dialog;
    }

    /**
     * This method is adapted from {@link #show(FragmentManager, String)}
     */
    public void showAllowingStateLoss(FragmentManager manager, String tag) {
        // This prevents us from hitting FragmentManager.checkStateLoss() which
        // throws a runtime exception if state has already been saved.
        if (manager.isStateSaved()) {
            return;
        }

        show(manager, tag);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        View rootView = View.inflate(getContext(), R.layout.fui_phone_progress_dialog, null);

        mProgress = rootView.findViewById(R.id.progress_bar);
        mMessageView = rootView.findViewById(R.id.progress_msg);
        mSuccessImage = rootView.findViewById(R.id.progress_success_imaage);

        if (mMessage != null) {
            setMessage(mMessage);
        }

        return new AlertDialog.Builder(getContext()).setView(rootView).create();
    }

    public void onComplete(String msg) {
        setMessage(msg);

        if (mProgress != null) {
            mProgress.setVisibility(View.GONE);
        }

        if (mSuccessImage != null) {
            mSuccessImage.setVisibility(View.VISIBLE);
        }
    }

    public void setMessage(CharSequence message) {
        if (mProgress != null && mMessageView != null) {
            mMessageView.setText(message);
        } else {
            mMessage = message;
        }
    }
}
