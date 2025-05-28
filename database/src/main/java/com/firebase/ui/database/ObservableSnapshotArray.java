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

import com.firebase.ui.common.BaseObservableSnapshotArray;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;

import java.util.List;

import androidx.annotation.NonNull;

/**
 * Exposes a collection of items in Firebase as a {@link List} of {@link DataSnapshot}. To observe
 * the list attach a {@link com.google.firebase.database.ChildEventListener}.
 *
 * @param <T> a POJO class to which the DataSnapshots can be converted.
 */
public abstract class ObservableSnapshotArray<T>
        extends BaseObservableSnapshotArray<DataSnapshot, DatabaseError, ChangeEventListener, T> {
    /**
     * Create an ObservableSnapshotArray with a custom {@link SnapshotParser}.
     *
     * @param parser the {@link SnapshotParser} to use
     */
    public ObservableSnapshotArray(@NonNull SnapshotParser<T> parser) {
        super(new CachingSnapshotParser<>(parser));
    }
}
