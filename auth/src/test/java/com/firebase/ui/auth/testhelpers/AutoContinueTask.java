/*
 * Copyright 2025 Google Inc. All Rights Reserved.
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

package com.firebase.ui.auth.testhelpers;

import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Subclass of {@link AutoCompleteTask} that also supports continuations.
 */
@SuppressWarnings("unchecked")
public class AutoContinueTask<TResult> extends AutoCompleteTask<TResult> {

    private Object mContinuationResult;

    public AutoContinueTask(TResult result,
                            Object continuationResult,
                            boolean complete,
                            @Nullable Exception exception) {
        super(result, complete, exception);
        mContinuationResult = continuationResult;
    }

    @NonNull
    @Override
    public <TContinuationResult> Task<TContinuationResult> continueWith(
            @NonNull Continuation<TResult, TContinuationResult> continuation) {
        return (Task<TContinuationResult>) Tasks.forResult(mContinuationResult);
    }

    @NonNull
    @Override
    public <TContinuationResult> Task<TContinuationResult> continueWithTask(
            @NonNull Continuation<TResult, Task<TContinuationResult>> continuation) {
        return (Task<TContinuationResult>) Tasks.forResult(mContinuationResult);
    }
}
