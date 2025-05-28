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

import com.google.firebase.database.DatabaseReference;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.lifecycle.LifecycleObserver;

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
interface FirebaseAdapter<T> extends ChangeEventListener, LifecycleObserver {
    /**
     * Start listening for database changes and populate the adapter.
     */
    void startListening();

    /**
     * Stop listening for database changes and clear all items in the adapter.
     */
    void stopListening();

    /**
     * Returns the backing {@link ObservableSnapshotArray} used to populate this adapter.
     *
     * @return the backing snapshot array
     */
    @NonNull
    ObservableSnapshotArray<T> getSnapshots();

    /**
     * Gets the item at the specified position from the backing snapshot array.
     *
     * @see ObservableSnapshotArray#get(int)
     */
    @NonNull
    T getItem(int position);

    /**
     * Returns the reference at the specified position in this list.
     *
     * @param position index of the reference to return
     * @return the snapshot at the specified position in this list
     * @throws IndexOutOfBoundsException if the index is out of range (<tt>index &lt; 0 || index
     *                                   &gt;= size()</tt>)
     */
    @NonNull
    DatabaseReference getRef(int position);
}
