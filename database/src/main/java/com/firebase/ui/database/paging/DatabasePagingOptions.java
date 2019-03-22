package com.firebase.ui.database.paging;

import android.arch.lifecycle.LifecycleOwner;
import android.arch.lifecycle.LiveData;
import android.arch.paging.LivePagedListBuilder;
import android.arch.paging.PagedList;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.util.DiffUtil;

import com.firebase.ui.database.ClassSnapshotParser;
import com.firebase.ui.database.SnapshotParser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.Query;
import com.google.firebase.database.annotations.NotNull;

/**
 * Options to configure an {@link FirebaseRecyclerPagingAdapter}.
 *
 * Use {@link Builder} to create a new instance.
 */
public final class DatabasePagingOptions<T> {

    private final SnapshotParser<T> mParser;
    private final LiveData<PagedList<DataSnapshot>> mData;
    private final DiffUtil.ItemCallback<DataSnapshot> mDiffCallback;
    private final LifecycleOwner mOwner;

    private DatabasePagingOptions(@NonNull LiveData<PagedList<DataSnapshot>> data,
                                  @NonNull SnapshotParser<T> parser,
                                  @NonNull DiffUtil.ItemCallback<DataSnapshot> diffCallback,
                                  @Nullable LifecycleOwner owner) {
        mParser = parser;
        mData = data;
        mDiffCallback = diffCallback;
        mOwner = owner;
    }

    @NonNull
    public LiveData<PagedList<DataSnapshot>> getData() {
        return mData;
    }

    @NonNull
    public SnapshotParser<T> getParser() {
        return mParser;
    }

    @NonNull
    public DiffUtil.ItemCallback<DataSnapshot> getDiffCallback() {
        return mDiffCallback;
    }

    @Nullable
    public LifecycleOwner getOwner() {
        return mOwner;
    }

    /**
     * Builder for {@link DatabasePagingOptions}.
     */
    public static final class Builder<T> {

        private LiveData<PagedList<DataSnapshot>> mData;
        private SnapshotParser<T> mParser;
        private LifecycleOwner mOwner;
        private DiffUtil.ItemCallback<DataSnapshot> mDiffCallback;

        /**
         * Sets the query using a {@link ClassSnapshotParser} based
         * on the given class.
         *
         * See {@link #setQuery(Query, PagedList.Config, SnapshotParser)}.
         */
        @NonNull
        public Builder<T> setQuery(@NonNull Query query,
                                   @NonNull PagedList.Config config,
                                   @NonNull Class<T> modelClass) {
            return setQuery(query, config, new ClassSnapshotParser<>(modelClass));
        }
        /**
         * Sets the Firestore query to paginate.
         *
         * @param query the FirebaseDatabase query. This query should only contain orderByKey(), orderByChild() and
         *              orderByValue() clauses. Any limit will cause an error such as limitToLast() or limitToFirst().
         * @param config paging configuration, passed directly to the support paging library.
         * @param parser the {@link SnapshotParser} to parse {@link DataSnapshot} into model
         *               objects.
         * @return this, for chaining.
         */
        @NonNull
        public Builder<T> setQuery(@NonNull Query query,
                                   @NonNull PagedList.Config config,
                                   @NotNull SnapshotParser<T> parser) {
            FirebaseDataSource.Factory factory = new FirebaseDataSource.Factory(query);
            mData = new LivePagedListBuilder<>(factory, config).build();

            mParser = parser;
            return this;
        }

        /**
         * Sets an optional custom {@link DiffUtil.ItemCallback} to compare
         * {@link T} objects.
         *
         * @return this, for chaining.
         */
        @NonNull
        public Builder<T> setDiffCallback(@NonNull DiffUtil.ItemCallback<DataSnapshot> diffCallback) {
            mDiffCallback = diffCallback;
            return this;
        }


        /**
         * Sets an optional {@link LifecycleOwner} to control the lifecycle of the adapter. Otherwise,
         * you must manually call {@link FirebaseRecyclerPagingAdapter#startListening()}
         * and {@link FirebaseRecyclerPagingAdapter#stopListening()}.
         *
         * @return this, for chaining.
         */
        @NonNull
        public Builder<T> setLifecycleOwner(@NonNull LifecycleOwner owner) {
            mOwner = owner;
            return this;
        }

        /**
         * Build the {@link DatabasePagingOptions} object.
         */
        @NonNull
        public DatabasePagingOptions<T> build() {
            if (mData == null) {
                throw new IllegalStateException("Must call setQuery() before calling build().");
            }

            if (mDiffCallback == null) {
                mDiffCallback = new DefaultSnapshotDiffCallback<T>(mParser);
            }

            return new DatabasePagingOptions<>(mData, mParser, mDiffCallback, mOwner);
        }

    }
}
