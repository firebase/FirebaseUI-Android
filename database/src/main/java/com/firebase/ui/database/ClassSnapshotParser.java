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

package com.firebase.ui.database;

import com.firebase.ui.common.Preconditions;
import com.google.firebase.database.DataSnapshot;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * A convenience implementation of {@link SnapshotParser} that converts a {@link DataSnapshot} to
 * the parametrized class via {@link DataSnapshot#getValue(Class)}.
 *
 * @param <T> the POJO class to create from snapshots.
 */
public class ClassSnapshotParser<T> implements SnapshotParser<T> {
    private Class<T> mClass;

    public ClassSnapshotParser(@NonNull Class<T> clazz) {
        mClass = Preconditions.checkNotNull(clazz);
    }

    @Nullable
    @Override
    public T parseSnapshot(@NonNull DataSnapshot snapshot) {
        // In FirebaseUI controlled usages, we can guarantee that our getValue calls will be nonnull
        // because we check for nullity with ValueEventListeners and use ChildEventListeners.
        // However, since this API is public, devs could use it for any snapshot including null
        // ones. Hence the nullability discrepancy.
        return snapshot.getValue(mClass);
    }
}
