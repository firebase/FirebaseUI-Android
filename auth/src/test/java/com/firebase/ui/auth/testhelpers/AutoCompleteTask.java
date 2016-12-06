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

package com.firebase.ui.auth.testhelpers;

import android.app.Activity;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

import java.util.concurrent.Executor;

public class AutoCompleteTask<TResult> extends Task<TResult> {
    private TResult mResult;
    private boolean mComplete;
    private boolean mSuccess;
    private Exception mException;

    public AutoCompleteTask(TResult result, boolean complete, @Nullable Exception exception) {
        mResult = result;
        mComplete = complete;
        mSuccess = exception == null;
        mException = exception;
    }

    @Override
    public boolean isComplete() {
        return mComplete;
    }

    @Override
    public boolean isSuccessful() {
        return mSuccess;
    }

    @Override
    public TResult getResult() {
        return mResult;
    }

    @Nullable
    @Override
    public Exception getException() {
        return mException;
    }

    @NonNull
    @Override
    public Task<TResult> addOnCompleteListener(@NonNull OnCompleteListener<TResult> onCompleteListener) {
        if (mComplete) {
            onCompleteListener.onComplete(this);
        }
        return this;
    }

    @NonNull
    @Override
    public Task<TResult> addOnSuccessListener(@NonNull OnSuccessListener<? super TResult> onSuccessListener) {
        if (mSuccess) {
            onSuccessListener.onSuccess(mResult);
        }
        return this;
    }

    @NonNull
    @Override
    public Task<TResult> addOnSuccessListener(@NonNull Executor executor, @NonNull OnSuccessListener
            onSuccessListener) {
        throw new RuntimeException("Method not implemented");
    }

    @NonNull
    @Override
    public Task<TResult> addOnSuccessListener(@NonNull Activity activity,
                                              @NonNull OnSuccessListener onSuccessListener) {
        throw new RuntimeException("Method not implemented");
    }

    @NonNull
    @Override
    public Task<TResult> addOnFailureListener(@NonNull OnFailureListener onFailureListener) {
        if (!mSuccess) {
            onFailureListener.onFailure(mException);
        }
        return this;
    }

    @NonNull
    @Override
    public Task<TResult> addOnFailureListener(@NonNull Executor executor,
                                              @NonNull OnFailureListener onFailureListener) {
        throw new RuntimeException("Method not implemented");
    }

    @NonNull
    @Override
    public Task<TResult> addOnFailureListener(@NonNull Activity activity,
                                              @NonNull OnFailureListener onFailureListener) {
        return addOnFailureListener(onFailureListener);
    }

    @Override
    public TResult getResult(@NonNull Class aClass) throws Throwable {
        throw new RuntimeException("Method not implemented");
    }
}
