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

package com.firebase.ui.auth.ui;

import android.support.annotation.NonNull;
import android.util.Log;

import com.google.android.gms.tasks.OnFailureListener;

public class TaskFailureLogger implements OnFailureListener {
    private String mTag;
    private String mMessage;

    public TaskFailureLogger(String tag, String message) {
        mTag = tag;
        mMessage = message;
    }

    @Override
    public void onFailure(@NonNull Exception e) {
        Log.w(mTag, mMessage, e);
    }
}
