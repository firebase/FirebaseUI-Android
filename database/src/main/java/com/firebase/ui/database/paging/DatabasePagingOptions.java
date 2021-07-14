package com.firebase.ui.database.paging;

import com.firebase.ui.database.ClassSnapshotParser;
import com.firebase.ui.database.SnapshotParser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.Query;
import com.google.firebase.database.annotations.NotNull;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LiveData;
import androidx.paging.Pager;
import androidx.paging.PagingConfig;
import androidx.paging.PagingData;
import androidx.paging.PagingLiveData;
import androidx.recyclerview.widget.DiffUtil;

/**
 * Options to configure an {@link FirebaseRecyclerPagingAdapter}.
 *
 * Use {@link Builder} to create a new instance.
 */
public final class DatabasePagingOptions<T> {

    private final SnapshotParser<T> mParser;
    private final LiveData<PagingData<DataSnapshot>> mData;
    private final DiffUtil.ItemCallback<DataSnapshot> mDiffCallback;
    private final LifecycleOwner mOwner;

    private DatabasePagingOptions(@NonNull LiveData<PagingData<DataSnapshot>> data,
                                  @NonNull SnapshotParser<T> parser,
                                  @NonNull DiffUtil.ItemCallback<DataSnapshot> diffCallback,
                                  @Nullable LifecycleOwner owner) {
        mParser = parser;
        mData = data;
        mDiffCallback = diffCallback;
        mOwner = owner;
    }

    @NonNull
    public LiveData<PagingData<DataSnapshot>> getData() {
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

        private LiveData<PagingData<DataSnapshot>> mData;
        private SnapshotParser<T> mParser;
        private LifecycleOwner mOwner;
        private DiffUtil.ItemCallback<DataSnapshot> mDiffCallback;

        /**
         * Sets the query using a {@link ClassSnapshotParser} based
         * on the given class.
         *
         * See {@link #setQuery(Query, PagingConfig, SnapshotParser)}.
         */
        @NonNull
        public Builder<T> setQuery(@NonNull Query query,
                                   @NonNull PagingConfig config,
                                   @NonNull Class<T> modelClass) {
            return setQuery(query, config, new ClassSnapshotParser<>(modelClass));
        }
        /**
         * Sets the Database query to paginate.
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
                                   @NonNull PagingConfig config,
                                   @NotNull SnapshotParser<T> parser) {
            final Pager<String, DataSnapshot> pager = new Pager<>(config,
                    () -> new DatabasePagingSource(query));
            mData = PagingLiveData.cachedIn(PagingLiveData.getLiveData(pager),
                    mOwner.getLifecycle());

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
                mDiffCallback = new DefaultSnapshotDiffCallback<>(mParser);
            }

            return new DatabasePagingOptions<>(mData, mParser, mDiffCallback, mOwner);
        }

    }
}
