package com.firebase.ui.firestore.paging;

import com.firebase.ui.firestore.ClassSnapshotParser;
import com.firebase.ui.firestore.SnapshotParser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.Source;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LiveData;
import androidx.paging.Pager;
import androidx.paging.PagingConfig;
import androidx.paging.PagingData;
import androidx.paging.PagingLiveData;
import androidx.paging.PagingSource;
import androidx.recyclerview.widget.DiffUtil;
import kotlin.jvm.functions.Function0;

import static com.firebase.ui.common.Preconditions.assertNull;

/**
 * Options to configure an {@link FirestorePagingAdapter}.
 * <p>
 * Use {@link Builder} to create a new instance.
 */
public final class FirestorePagingOptions<T> {

    private static final String ERR_DATA_SET = "Data already set. " +
            "Call only one of setPagingData() or setQuery()";

    private final LiveData<PagingData<DocumentSnapshot>> mPagingData;
    private final SnapshotParser<T> mParser;
    private final DiffUtil.ItemCallback<DocumentSnapshot> mDiffCallback;
    private final LifecycleOwner mOwner;

    private FirestorePagingOptions(@NonNull LiveData<PagingData<DocumentSnapshot>> pagingData,
                                   @NonNull SnapshotParser<T> parser,
                                   @NonNull DiffUtil.ItemCallback<DocumentSnapshot> diffCallback,
                                   @Nullable LifecycleOwner owner) {
        mPagingData = pagingData;
        mParser = parser;
        mDiffCallback = diffCallback;
        mOwner = owner;
    }

    @NonNull
    public LiveData<PagingData<DocumentSnapshot>> getPagingData() {
        return mPagingData;
    }

    @NonNull
    public SnapshotParser<T> getParser() {
        return mParser;
    }

    @NonNull
    public DiffUtil.ItemCallback<DocumentSnapshot> getDiffCallback() {
        return mDiffCallback;
    }

    @Nullable
    public LifecycleOwner getOwner() {
        return mOwner;
    }

    /**
     * Builder for {@link FirestorePagingOptions}.
     */
    public static final class Builder<T> {

        private LiveData<PagingData<DocumentSnapshot>> mPagingData;
        private SnapshotParser<T> mParser;
        private LifecycleOwner mOwner;
        private DiffUtil.ItemCallback<DocumentSnapshot> mDiffCallback;

        /**
         * Directly set data using and parse with a {@link ClassSnapshotParser} based on the given
         * class.
         * <p>
         * Do not call this method after calling {@code setQuery}.
         */
        @NonNull
        public Builder<T> setPagingData(@NonNull LiveData<PagingData<DocumentSnapshot>> data,
                                        @NonNull Class<T> modelClass) {
            return setPagingData(data, new ClassSnapshotParser<>(modelClass));
        }

        /**
         * Directly set data and parse with a custom {@link SnapshotParser}.
         * <p>
         * Do not call this method after calling {@code setQuery}.
         */
        @NonNull
        public Builder<T> setPagingData(@NonNull LiveData<PagingData<DocumentSnapshot>> pagingData,
                                        @NonNull SnapshotParser<T> parser) {
            assertNull(mPagingData, ERR_DATA_SET);

            mPagingData = pagingData;
            mParser = parser;
            return this;
        }

        /**
         * Sets the query using {@link Source#DEFAULT} and a {@link ClassSnapshotParser} based on
         * the given Class.
         * <p>
         * See {@link #setQuery(Query, Source, PagingConfig, SnapshotParser)}.
         */
        @NonNull
        public Builder<T> setQuery(@NonNull Query query,
                                   @NonNull PagingConfig config,
                                   @NonNull Class<T> modelClass) {
            return setQuery(query, Source.DEFAULT, config, modelClass);
        }

        /**
         * Sets the query using {@link Source#DEFAULT} and a custom {@link SnapshotParser}.
         * <p>
         * See {@link #setQuery(Query, Source, PagingConfig, SnapshotParser)}.
         */
        @NonNull
        public Builder<T> setQuery(@NonNull Query query,
                                   @NonNull PagingConfig config,
                                   @NonNull SnapshotParser<T> parser) {
            return setQuery(query, Source.DEFAULT, config, parser);
        }

        /**
         * Sets the query using a custom {@link Source} and a {@link ClassSnapshotParser} based on
         * the given class.
         * <p>
         * See {@link #setQuery(Query, Source, PagingConfig, SnapshotParser)}.
         */
        @NonNull
        public Builder<T> setQuery(@NonNull Query query,
                                   @NonNull Source source,
                                   @NonNull PagingConfig config,
                                   @NonNull Class<T> modelClass) {
            return setQuery(query, source, config, new ClassSnapshotParser<>(modelClass));
        }

        /**
         * Sets the Firestore query to paginate.
         *
         * @param query  the Firestore query. This query should only contain where() and orderBy()
         *               clauses. Any limit() or pagination clauses will cause errors.
         * @param source the data source to use for query data.
         * @param config paging configuration, passed directly to the support paging library.
         * @param parser the {@link SnapshotParser} to parse {@link DocumentSnapshot} into model
         *               objects.
         * @return this, for chaining.
         */
        @NonNull
        public Builder<T> setQuery(@NonNull final Query query,
                                   @NonNull final Source source,
                                   @NonNull PagingConfig config,
                                   @NonNull SnapshotParser<T> parser) {
            assertNull(mPagingData, ERR_DATA_SET);

            mParser = parser;

            final Pager<PageKey, DocumentSnapshot> pager = new Pager<>(config,
                    () -> new FirestorePagingSource(query, source));

            mPagingData = PagingLiveData.cachedIn(PagingLiveData.getLiveData(pager),
                    mOwner.getLifecycle());
            return this;
        }


        /**
         * Sets an optional custom {@link DiffUtil.ItemCallback} to compare {@link DocumentSnapshot}
         * objects.
         * <p>
         * The default implementation is {@link DefaultSnapshotDiffCallback}.
         *
         * @return this, for chaining.
         */
        @NonNull
        public Builder<T> setDiffCallback(@NonNull DiffUtil.ItemCallback<DocumentSnapshot> diffCallback) {
            mDiffCallback = diffCallback;
            return this;
        }

        /**
         * Sets an optional {@link LifecycleOwner} to control the lifecycle of the adapter.
         * Otherwise, you must manually call {@link FirestorePagingAdapter#startListening()} and
         * {@link FirestorePagingAdapter#stopListening()}.
         *
         * @return this, for chaining.
         */
        @NonNull
        public Builder<T> setLifecycleOwner(@NonNull LifecycleOwner owner) {
            mOwner = owner;
            return this;
        }

        /**
         * Build the {@link FirestorePagingOptions} object.
         */
        @NonNull
        public FirestorePagingOptions<T> build() {
            if (mPagingData == null || mParser == null) {
                throw new IllegalStateException("Must call setQuery() or setPagingData()" +
                        " before calling build().");
            }

            if (mDiffCallback == null) {
                mDiffCallback = new DefaultSnapshotDiffCallback<>(mParser);
            }

            return new FirestorePagingOptions<>(mPagingData, mParser, mDiffCallback, mOwner);
        }
    }

}
