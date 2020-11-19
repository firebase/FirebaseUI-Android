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
import androidx.recyclerview.widget.DiffUtil;

import static com.firebase.ui.common.Preconditions.assertNull;

/**
 * Options to configure an {@link FirestorePagingAdapter}.
 *
 * Use {@link Builder} to create a new instance.
 */
public final class FirestorePagingOptions<T> {

    private static final String ERR_DATA_SET = "Data  already set. " +
            "Call only one of setData() or setQuery()";

    private final LiveData<PagedList<DocumentSnapshot>> mData;
    private final SnapshotParser<T> mParser;
    private final DiffUtil.ItemCallback<DocumentSnapshot> mDiffCallback;
    private final LifecycleOwner mOwner;

    private FirestorePagingOptions(@NonNull LiveData<PagedList<DocumentSnapshot>> data,
                                   @NonNull SnapshotParser<T> parser,
                                   @NonNull DiffUtil.ItemCallback<DocumentSnapshot> diffCallback,
                                   @Nullable LifecycleOwner owner) {
        mData = data;
        mParser = parser;
        mDiffCallback = diffCallback;
        mOwner = owner;
    }

    @NonNull
    public LiveData<PagedList<DocumentSnapshot>> getData() {
        return mData;
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
        private SnapshotParser<T> mParser;
        private LifecycleOwner mOwner;
        private DiffUtil.ItemCallback<DocumentSnapshot> mDiffCallback;

        /**
         * Directly set data using and parse with a {@link ClassSnapshotParser} based on
         * the given class.
         * <p>
         * Do not call this method after calling {@code setQuery}.
         */
        @NonNull
        public Builder<T> setData(@NonNull LiveData<PagedList<DocumentSnapshot>> data,
                                              @NonNull Class<T> modelClass) {

         return setData(data, new ClassSnapshotParser<>(modelClass));
        }

        /**
         * Directly set data and parse with a custom {@link SnapshotParser}.
         * <p>
         * Do not call this method after calling {@code setQuery}.
         */
        @NonNull
        public Builder<T> setData(@NonNull LiveData<PagedList<DocumentSnapshot>> data,
                                  @NonNull SnapshotParser<T> parser) {
            assertNull(mData, ERR_DATA_SET);

            mData = data;
            mParser = parser;
            return this;
        }

        /**
         * Sets the query using {@link Source#DEFAULT} and a {@link ClassSnapshotParser} based
         * on the given Class.
         *
         * See {@link #setQuery(Query, Source, PagedList.Config, SnapshotParser)}.
         */
        @NonNull
        public Builder<T> setQuery(@NonNull Query query,
                                   @NonNull PagedList.Config config,
                                   @NonNull Class<T> modelClass) {
            return setQuery(query, Source.DEFAULT, config, modelClass);
        }

        /**
         * Sets the query using {@link Source#DEFAULT} and a custom {@link SnapshotParser}.
         *
         * See {@link #setQuery(Query, Source, PagedList.Config, SnapshotParser)}.
         */
        @NonNull
        public Builder<T> setQuery(@NonNull Query query,
                                   @NonNull PagedList.Config config,
                                   @NonNull SnapshotParser<T> parser) {
            return setQuery(query, Source.DEFAULT, config, parser);
        }

        /**
         * Sets the query using a custom {@link Source} and a {@link ClassSnapshotParser} based
         * on the given class.
         *
         * See {@link #setQuery(Query, Source, PagedList.Config, SnapshotParser)}.
         */
        @NonNull
        public Builder<T> setQuery(@NonNull Query query,
                                   @NonNull Source source,
                                   @NonNull PagedList.Config config,
                                   @NonNull Class<T> modelClass) {
            return setQuery(query, source, config, new ClassSnapshotParser<>(modelClass));
        }

        /**
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
            if (mData == null || mParser == null) {
                throw new IllegalStateException("Must call setQuery() or setDocumentSnapshot()" +
                        " before calling build().");
            }

            if (mDiffCallback == null) {
                mDiffCallback = new DefaultSnapshotDiffCallback<T>(mParser);
            }

            return new FirestorePagingOptions<>(mData, mParser, mDiffCallback, mOwner);
        }

    }

}
