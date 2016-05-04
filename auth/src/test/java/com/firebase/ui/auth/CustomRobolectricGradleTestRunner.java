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

package com.firebase.ui.auth;

import com.firebase.ui.auth.api.FirebaseAuthWrapperFactory;

import org.junit.runners.model.InitializationError;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.internal.bytecode.InstrumentationConfiguration;

public class CustomRobolectricGradleTestRunner extends RobolectricGradleTestRunner {
    public CustomRobolectricGradleTestRunner(Class<?> klass) throws InitializationError {
        super(klass);
    }

    @Override
    public InstrumentationConfiguration createClassLoaderConfig(Config config) {
        InstrumentationConfiguration.Builder builder = InstrumentationConfiguration.newBuilder();
        builder.addInstrumentedClass(FirebaseAuthWrapperFactory.class.getName());
        return builder.build();
    }
}
