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
import androidx.paging.LivePagedListBuilder;
import androidx.paging.PagedList;
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
 *
 * Use {@link Builder} to create a new instance.
 */
public final class FirestorePagingOptions<T> {

    private static final String ERR_DATA_SET = "Data already set. " +
            "Call only one of setPagingData() or setQuery()";

    private final LiveData<PagedList<DocumentSnapshot>> mData;
    private final LiveData<PagingData<DocumentSnapshot>> mPagingData;
    private final SnapshotParser<T> mParser;
    private final DiffUtil.ItemCallback<DocumentSnapshot> mDiffCallback;
    private final LifecycleOwner mOwner;

    private FirestorePagingOptions(@NonNull LiveData<PagedList<DocumentSnapshot>> data,
                                   @NonNull LiveData<PagingData<DocumentSnapshot>> pagingData,
                                   @NonNull SnapshotParser<T> parser,
                                   @NonNull DiffUtil.ItemCallback<DocumentSnapshot> diffCallback,
                                   @Nullable LifecycleOwner owner) {
        mData = data;
        mPagingData = pagingData;
        mParser = parser;
        mDiffCallback = diffCallback;
        mOwner = owner;
    }

    @NonNull
    public LiveData<PagedList<DocumentSnapshot>> getData() {
        return mData;
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

        private LiveData<PagedList<DocumentSnapshot>> mData;
        private LiveData<PagingData<DocumentSnapshot>> mPagingData;
        private SnapshotParser<T> mParser;
        private LifecycleOwner mOwner;
        private DiffUtil.ItemCallback<DocumentSnapshot> mDiffCallback;

        /**
         *
         * This method has been deprecated. Use {@link #setPagingData(LiveData, Class)}
         * instead.
         *
         * Directly set data using and parse with a {@link ClassSnapshotParser} based on
         * the given class.
         * <p>
         * Do not call this method after calling {@code setQuery}.
         */
        @Deprecated
        @NonNull
        public Builder<T> setData(@NonNull LiveData<PagedList<DocumentSnapshot>> data,
                                              @NonNull Class<T> modelClass) {

         return setData(data, new ClassSnapshotParser<>(modelClass));
        }

        /**
         * Directly set data using and parse with a {@link ClassSnapshotParser} based on
         * the given class.
         * <p>
         * Do not call this method after calling {@code setQuery}.
         */
        @NonNull
        public Builder<T> setPagingData(@NonNull LiveData<PagingData<DocumentSnapshot>> data,
                                  @NonNull Class<T> modelClass) {
            return setPagingData(data, new ClassSnapshotParser<>(modelClass));
        }

        /**
         *
         * This method has been deprecated. Use {@link #setPagingData(LiveData, SnapshotParser)}
         * instead.
         *
         * Directly set data and parse with a custom {@link SnapshotParser}.
         * <p>
         * Do not call this method after calling {@code setQuery}.
         */
        @Deprecated
        @NonNull
        public Builder<T> setData(@NonNull LiveData<PagedList<DocumentSnapshot>> data,
                                  @NonNull SnapshotParser<T> parser) {
            assertNull(mData, ERR_DATA_SET);

            mData = data;
            mParser = parser;
            return this;
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
         * This method has been deprecated. Use {@link #setQuery(Query, PagingConfig, Class)}
         * instead.
         *
         * Sets the query using {@link Source#DEFAULT} and a {@link ClassSnapshotParser} based
         * on the given Class.
         *
         * See {@link #setQuery(Query, Source, PagedList.Config, SnapshotParser)}.
         */
        @Deprecated
        @NonNull
        public Builder<T> setQuery(@NonNull Query query,
                                   @NonNull PagedList.Config config,
                                   @NonNull Class<T> modelClass) {
            return setQuery(query, Source.DEFAULT, config, modelClass);
        }

        /**
         *
         * Sets the query using {@link Source#DEFAULT} and a {@link ClassSnapshotParser} based
         * on the given Class.
         *
         * See {@link #setQuery(Query, Source, PagingConfig, SnapshotParser)}.
         */
        @NonNull
        public Builder<T> setQuery(@NonNull Query query,
                                   @NonNull PagingConfig config,
                                   @NonNull Class<T> modelClass) {
            return setQuery(query, Source.DEFAULT, config, modelClass);
        }

        /**
         * This method has been deprecated.
         * Use {@link #setQuery(Query, PagingConfig, SnapshotParser)} instead.
         *
         * Sets the query using {@link Source#DEFAULT} and a custom {@link SnapshotParser}.
         *
         * See {@link #setQuery(Query, Source, PagedList.Config, SnapshotParser)}.
         */
        @Deprecated
        @NonNull
        public Builder<T> setQuery(@NonNull Query query,
                                   @NonNull PagedList.Config config,
                                   @NonNull SnapshotParser<T> parser) {
            return setQuery(query, Source.DEFAULT, config, parser);
        }

        /**
         *
         * Sets the query using {@link Source#DEFAULT} and a custom {@link SnapshotParser}.
         *
         * See {@link #setQuery(Query, Source, PagingConfig, SnapshotParser)}.
         */
        @NonNull
        public Builder<T> setQuery(@NonNull Query query,
                                   @NonNull PagingConfig config,
                                   @NonNull SnapshotParser<T> parser) {
            return setQuery(query, Source.DEFAULT, config, parser);
        }

        /**
         * This method has been deprecated.
         * Use {@link #setQuery(Query, Source, PagingConfig, Class)} instead.
         *
         * Sets the query using a custom {@link Source} and a {@link ClassSnapshotParser} based
         * on the given class.
         *
         * See {@link #setQuery(Query, Source, PagedList.Config, SnapshotParser)}.
         */
        @Deprecated
        @NonNull
        public Builder<T> setQuery(@NonNull Query query,
                                   @NonNull Source source,
                                   @NonNull PagedList.Config config,
                                   @NonNull Class<T> modelClass) {
            return setQuery(query, source, config, new ClassSnapshotParser<>(modelClass));
        }

        /**
         *
         * Sets the query using a custom {@link Source} and a {@link ClassSnapshotParser} based
         * on the given class.
         *
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
         * This method has been deprecated.
         * Use {@link #setQuery(Query, Source, PagingConfig, SnapshotParser)} instead.
         *
         * Sets the Firestore query to paginate.
         *
         * @param query the Firestore query. This query should only contain where() and
         *              orderBy() clauses. Any limit() or pagination clauses will cause errors.
         * @param source the data source to use for query data.
         * @param config paging configuration, passed directly to the support paging library.
         * @param parser the {@link SnapshotParser} to parse {@link DocumentSnapshot} into model
         *               objects.
         * @return this, for chaining.
         */
        @Deprecated
        @NonNull
        public Builder<T> setQuery(@NonNull Query query,
                                   @NonNull Source source,
                                   @NonNull PagedList.Config config,
                                   @NonNull SnapshotParser<T> parser) {
            assertNull(mData, ERR_DATA_SET);

            // Build paged list
            FirestoreDataSource.Factory factory = new FirestoreDataSource.Factory(query, source);
            mData = new LivePagedListBuilder<>(factory, config).build();

            mParser = parser;
            return this;
        }

        /**
         *
         * Sets the Firestore query to paginate.
         *
         * @param query the Firestore query. This query should only contain where() and
         *              orderBy() clauses. Any limit() or pagination clauses will cause errors.
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
            assertNull(mData, ERR_DATA_SET);
            assertNull(mPagingData, ERR_DATA_SET);

            mParser = parser;

            // Paging 2 support
            PagedList.Config oldConfig = toOldConfig(config);
            final FirestoreDataSource.Factory factory = new FirestoreDataSource.Factory(query, source);
            mData = new LivePagedListBuilder<PageKey, DocumentSnapshot>(factory, oldConfig).build();


            // For Paging 3 support
            final Pager<PageKey, DocumentSnapshot> pager = new Pager<>(config,
                    new Function0<PagingSource<PageKey, DocumentSnapshot>>() {
                        @Override
                        public PagingSource<PageKey, DocumentSnapshot> invoke() {
                            return factory.asPagingSourceFactory().invoke();
                        }
                    });


            mPagingData = PagingLiveData.cachedIn(PagingLiveData.getLiveData(pager),
                    mOwner.getLifecycle());
            return this;
        }



        /**
         * Sets an optional custom {@link DiffUtil.ItemCallback} to compare
         * {@link DocumentSnapshot} objects.
         *
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
         * Sets an optional {@link LifecycleOwner} to control the lifecycle of the adapter. Otherwise,
         * you must manually call {@link FirestorePagingAdapter#startListening()}
         * and {@link FirestorePagingAdapter#stopListening()}.
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
            if ((mData == null && mPagingData == null) || mParser == null) {
                throw new IllegalStateException("Must call setQuery() or setPagingData()" +
                        " before calling build().");
            }

            if (mDiffCallback == null) {
                mDiffCallback = new DefaultSnapshotDiffCallback<T>(mParser);
            }

            return new FirestorePagingOptions<>(mData, mPagingData, mParser, mDiffCallback, mOwner);
        }

        /**
         * Workaround to support the new PagingConfig class
         * This should be removed once we fully migrate to Paging 3
         * @param config the new PagingConfig
         * @return the old PagedList.Config
         */
        private PagedList.Config toOldConfig(@NonNull PagingConfig config) {
            return new PagedList.Config.Builder()
                    .setEnablePlaceholders(config.enablePlaceholders)
                    .setInitialLoadSizeHint(config.initialLoadSize)
                    .setMaxSize(config.maxSize)
                    .setPrefetchDistance(config.prefetchDistance)
                    .setPageSize(config.pageSize)
                    .build();
        }

    }

}
