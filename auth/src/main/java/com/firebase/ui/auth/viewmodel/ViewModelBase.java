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

package com.firebase.ui.auth.viewmodel;

import android.app.Application;

import java.util.concurrent.atomic.AtomicBoolean;

import androidx.annotation.CallSuper;
import androidx.annotation.RestrictTo;
import androidx.lifecycle.AndroidViewModel;

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public abstract class ViewModelBase<T> extends AndroidViewModel {
    private final AtomicBoolean mIsInitialized = new AtomicBoolean();

    private T mArguments;

    protected ViewModelBase(Application application) {
        super(application);
    }

    public void init(T args) {
        if (mIsInitialized.compareAndSet(false, true)) {
            mArguments = args;
            onCreate();
        }
    }

    protected void onCreate() {}

    protected T getArguments() {
        return mArguments;
    }

    @RestrictTo(RestrictTo.Scope.TESTS)
    protected void setArguments(T arguments) {
        mArguments = arguments;
    }

    @CallSuper
    @Override
    protected void onCleared() {
        mIsInitialized.set(false);
    }
}
