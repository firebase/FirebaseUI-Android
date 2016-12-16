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

import android.support.annotation.Nullable;

import com.google.firebase.auth.ProviderQueryResult;

import java.util.List;

public class FakeProviderQueryResult implements ProviderQueryResult {
    private List<String> mProviders;

    public FakeProviderQueryResult(List<String> providers) {
        mProviders = providers;
    }

    @Nullable
    @Override
    public List<String> getProviders() {
        return mProviders;
    }
}
