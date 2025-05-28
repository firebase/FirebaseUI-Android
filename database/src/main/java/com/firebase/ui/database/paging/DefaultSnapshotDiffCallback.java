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

package com.firebase.ui.database.paging;

import android.annotation.SuppressLint;

import com.firebase.ui.database.SnapshotParser;
import com.google.firebase.database.DataSnapshot;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.recyclerview.widget.DiffUtil;

/**
 * Default diff callback implementation for Firebase Data snapshots.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class DefaultSnapshotDiffCallback<T> extends DiffUtil.ItemCallback<DataSnapshot> {

    private final SnapshotParser<T> mParser;

    public DefaultSnapshotDiffCallback(@NonNull SnapshotParser<T> parser) {
        mParser = parser;
    }

    @Override
    public boolean areItemsTheSame(@NonNull DataSnapshot oldItem,
                                   @NonNull DataSnapshot newItem) {
        return oldItem.getKey().equals(newItem.getKey());
    }

    @SuppressLint("DiffUtilEquals")
    @Override
    public boolean areContentsTheSame(@NonNull DataSnapshot oldItem,
                                      @NonNull DataSnapshot newItem) {
        T oldModel = mParser.parseSnapshot(oldItem);
        T newModel = mParser.parseSnapshot(newItem);

        return oldModel.equals(newModel);
    }
}
