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

import com.google.firebase.firestore.MetadataChanges;
import com.google.firebase.firestore.Query;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LifecycleOwner;

import static com.firebase.ui.common.Preconditions.assertNonNull;
import static com.firebase.ui.common.Preconditions.assertNull;

/**
 * Options to configure an {@link FirestoreRecyclerAdapter}.
 *
 * @see Builder
 */
public final class FirestoreRecyclerOptions<T> {

    private static final String ERR_SNAPSHOTS_SET = "Snapshot array already set. " +
            "Call only one of setSnapshotArray or setQuery";
    private static final String ERR_SNAPSHOTS_NULL = "Snapshot array cannot be null. " +
            "Call one of setSnapshotArray or setQuery";

    private ObservableSnapshotArray<T> mSnapshots;
    private LifecycleOwner mOwner;

    private FirestoreRecyclerOptions(ObservableSnapshotArray<T> snapshots,
                                     @Nullable LifecycleOwner owner) {
        mSnapshots = snapshots;
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
     * Get the (optional) {@link LifecycleOwner}.
     */
    @Nullable
    public LifecycleOwner getOwner() {
        return mOwner;
    }

    /**
     * Builder for {@link FirestoreRecyclerOptions}.
     *
     * @param <T> the model class for the {@link FirestoreRecyclerAdapter}.
     */
    public static final class Builder<T> {

        private ObservableSnapshotArray<T> mSnapshots;
        private LifecycleOwner mOwner;

        /**
         * Directly set the {@link ObservableSnapshotArray}.
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
         * Calls {@link #setQuery(Query, MetadataChanges, Class)} with metadata changes excluded.
         */
        @NonNull
        public Builder<T> setQuery(@NonNull Query query, @NonNull SnapshotParser<T> parser) {
            return setQuery(query, MetadataChanges.EXCLUDE, parser);
        }

        /**
         * Calls {@link #setQuery(Query, MetadataChanges, Class)} with metadata changes excluded.
         */
        @NonNull
        public Builder<T> setQuery(@NonNull Query query, @NonNull Class<T> modelClass) {
            return setQuery(query, MetadataChanges.EXCLUDE, modelClass);
        }

        /**
         * Set the query to use (with options) and provide a model class to which each snapshot will
         * be converted.
         * <p>
         * Do not call this method after calling {@link #setSnapshotArray(ObservableSnapshotArray)}.
         */
        @NonNull
        public Builder<T> setQuery(@NonNull Query query,
                                   @NonNull MetadataChanges changes,
                                   @NonNull Class<T> modelClass) {
            return setQuery(query, changes, new ClassSnapshotParser<>(modelClass));
        }

        /**
         * Set the query to use (with options) and provide a custom {@link SnapshotParser}.
         * <p>
         * Do not call this method after calling {@link #setSnapshotArray(ObservableSnapshotArray)}.
         */
        @NonNull
        public Builder<T> setQuery(@NonNull Query query,
                                   @NonNull MetadataChanges changes,
                                   @NonNull SnapshotParser<T> parser) {
            assertNull(mSnapshots, ERR_SNAPSHOTS_SET);

            mSnapshots = new FirestoreArray<>(query, changes, parser);
            return this;
        }

        /**
         * Set a {@link LifecycleOwner} for the adapter. Listening will stop/start after the
         * appropriate lifecycle events.
         */
        @NonNull
        public Builder<T> setLifecycleOwner(@Nullable LifecycleOwner owner) {
            mOwner = owner;
            return this;
        }

        /**
         * Build a {@link FirestoreRecyclerOptions} from the provided arguments.
         */
        @NonNull
        public FirestoreRecyclerOptions<T> build() {
            assertNonNull(mSnapshots, ERR_SNAPSHOTS_NULL);

            return new FirestoreRecyclerOptions<>(mSnapshots, mOwner);
        }

    }

}
