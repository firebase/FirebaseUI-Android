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

package com.firebase.ui.firestore;

import com.firebase.ui.common.Preconditions;
import com.google.firebase.firestore.DocumentSnapshot;

import androidx.annotation.NonNull;

/**
 * An implementation of {@link SnapshotParser} that converts {@link DocumentSnapshot} to
 * a class using {@link DocumentSnapshot#toObject(Class)}.
 */
public class ClassSnapshotParser<T> implements SnapshotParser<T> {

    private final Class<T> mModelClass;

    public ClassSnapshotParser(@NonNull Class<T> modelClass) {
        mModelClass = Preconditions.checkNotNull(modelClass);
    }

    @NonNull
    @Override
    public T parseSnapshot(@NonNull DocumentSnapshot snapshot) {
        return snapshot.toObject(mModelClass);
    }

}
