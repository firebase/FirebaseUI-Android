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

import androidx.annotation.RestrictTo;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public abstract class OperableViewModel<I, O> extends ViewModelBase<I> {
    private MutableLiveData<O> mOperation = new MutableLiveData<>();

    protected OperableViewModel(Application application) {
        super(application);
    }

    /**
     * Get the observable state of the operation.
     */
    public LiveData<O> getOperation() {
        return mOperation;
    }

    protected void setResult(O output) {
        mOperation.setValue(output);
    }
}
