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
import com.google.firebase.database.Query;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LifecycleOwner;

import static com.firebase.ui.common.Preconditions.assertNonNull;
import static com.firebase.ui.common.Preconditions.assertNull;

/**
 * Options to configure a {@link FirebaseListAdapter}.
 *
 * @see Builder
 */
public final class FirebaseListOptions<T> {

    private static final String ERR_SNAPSHOTS_SET = "Snapshot array already set. " +
            "Call only one of setSnapshotArray, setQuery, or setIndexedQuery.";

    private final ObservableSnapshotArray<T> mSnapshots;
    private final @LayoutRes int mLayout;
    private final LifecycleOwner mOwner;

    private FirebaseListOptions(ObservableSnapshotArray<T> snapshots,
                                @LayoutRes int layout,
                                LifecycleOwner owner) {
        mSnapshots = snapshots;
        mLayout = layout;
        mOwner = owner;
    }

    /**
     * Get the {@link ObservableSnapshotArray} to observe.
     */
    @NonNull
    public ObservableSnapshotArray<T> getSnapshots() {
        return mSnapshots;
    }

    /**
     * Get the resource ID of the layout file for a list item.
     */
    @LayoutRes
    public int getLayout() {
        return mLayout;
    }

    /**
     * Get the (optional) {@link LifecycleOwner}.
     */
    @Nullable
    public LifecycleOwner getOwner() {
        return mOwner;
    }

    /**
     * Builder for {@link FirebaseListOptions}.
     *
     * @param <T> the model class for the {@link FirebaseListAdapter}.
     */
    public static final class Builder<T> {

        private ObservableSnapshotArray<T> mSnapshots;
        private @LayoutRes Integer mLayout;
        private LifecycleOwner mOwner;

        /**
         * Directly set the {@link ObservableSnapshotArray} to observe.
         * <p>
         * Do not call this method after calling {@code setQuery}.
         */
        @NonNull
        public Builder<T> setSnapshotArray(@NonNull ObservableSnapshotArray<T> snapshots) {
            assertNull(mSnapshots, ERR_SNAPSHOTS_SET);

            mSnapshots = snapshots;
            return this;
        }

        /**
         * Set the query to listen on and a {@link SnapshotParser} to parse data snapshots.
         * <p>
         * Do not call this method after calling {@link #setSnapshotArray(ObservableSnapshotArray)}.
         */
        @NonNull
        public Builder<T> setQuery(@NonNull Query query, @NonNull SnapshotParser<T> parser) {
            assertNull(mSnapshots, ERR_SNAPSHOTS_SET);

            mSnapshots = new FirebaseArray<>(query, parser);
            return this;
        }

        /**
         * Set the query to listen on and a {@link Class} to which data snapshots should be
         * converted. Do not call this method after calling {@link #setSnapshotArray(ObservableSnapshotArray)}.
         */
        @NonNull
        public Builder<T> setQuery(@NonNull Query query, @NonNull Class<T> modelClass) {
            return setQuery(query, new ClassSnapshotParser<>(modelClass));
        }

        /**
         * Set an indexed query to listen on and a {@link SnapshotParser} to parse data snapshots.
         * The keyQuery is used to find a list of IDs, which are then queried at the dataRef.
         * <p>
         * Do not call this method after calling {@link #setSnapshotArray(ObservableSnapshotArray)}.
         */
        @NonNull
        public Builder<T> setIndexedQuery(@NonNull Query keyQuery,
                                          @NonNull DatabaseReference dataRef,
                                          @NonNull SnapshotParser<T> parser) {
            assertNull(mSnapshots, ERR_SNAPSHOTS_SET);

            mSnapshots = new FirebaseIndexArray<>(keyQuery, dataRef, parser);
            return this;
        }

        /**
         * Set an indexed query to listen on and a {@link Class} to which data snapshots should be
         * converted. The keyQuery is used to find a list of keys, which are then queried at the
         * dataRef.
         * <p>
         * Do not call this method after calling {@link #setSnapshotArray(ObservableSnapshotArray)}.
         */
        @NonNull
        public Builder<T> setIndexedQuery(@NonNull Query keyQuery,
                                          @NonNull DatabaseReference dataRef,
                                          @NonNull Class<T> modelClass) {
            return setIndexedQuery(keyQuery, dataRef, new ClassSnapshotParser<>(modelClass));
        }

        /**
         * Set the resource ID for the item layout.
         */
        @NonNull
        public Builder<T> setLayout(@LayoutRes int layout) {
            mLayout = layout;
            return this;
        }

        /**
         * Set the optional {@link LifecycleOwner}. Listening will stop/start after the appropriate
         * lifecycle events.
         */
        @NonNull
        public Builder<T> setLifecycleOwner(@Nullable LifecycleOwner owner) {
            mOwner = owner;
            return this;
        }

        /**
         * Build a {@link FirebaseListOptions} from the provided arguments.
         */
        @NonNull
        public FirebaseListOptions<T> build() {
            assertNonNull(mSnapshots, "Snapshot array cannot be null. " +
                    "Call setQuery or setSnapshotArray.");
            assertNonNull(mLayout, "Layout cannot be null. " +
                    "Call setLayout.");

            return new FirebaseListOptions<>(mSnapshots, mLayout, mOwner);
        }

    }
}
