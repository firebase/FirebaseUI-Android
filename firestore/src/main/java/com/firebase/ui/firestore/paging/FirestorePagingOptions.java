package com.firebase.ui.firestore.paging;

import android.arch.lifecycle.LifecycleOwner;
import android.arch.lifecycle.LiveData;
import android.arch.paging.LivePagedListBuilder;
import android.arch.paging.PagedList;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.util.DiffUtil;

import com.firebase.ui.firestore.ClassSnapshotParser;
import com.firebase.ui.firestore.SnapshotParser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.Source;

/**
 * Options to configure an {@link FirestorePagingAdapter}.
 */
public final class FirestorePagingOptions<T> {

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

    public static final class Builder<T> {

        private LiveData<PagedList<DocumentSnapshot>> mData;
        private SnapshotParser<T> mParser;
        private LifecycleOwner mOwner;
        private DiffUtil.ItemCallback<DocumentSnapshot> mDiffCallback;

        @NonNull
        public Builder<T> setQuery(@NonNull Query query,
                                   @NonNull PagedList.Config config,
                                   @NonNull Class<T> modelClass) {
            return setQuery(query, config, new ClassSnapshotParser<>(modelClass));
        }

        @NonNull
        public Builder<T> setQuery(@NonNull Query query,
                                   @NonNull PagedList.Config config,
                                   @NonNull SnapshotParser<T> parser) {
            return setQuery(query, Source.DEFAULT, config, parser);
        }

        @NonNull
        public Builder<T> setQuery(@NonNull Query query,
                                   @NonNull Source source,
                                   @NonNull PagedList.Config config,
                                   @NonNull Class<T> modelClass) {
            return setQuery(query, source, config, new ClassSnapshotParser<>(modelClass));
        }

        @NonNull
        public Builder<T> setQuery(@NonNull Query query,
                                   @NonNull Source source,
                                   @NonNull PagedList.Config config,
                                   @NonNull SnapshotParser<T> parser) {
            // Build paged list
            FirestoreDataSource.Factory factory = new FirestoreDataSource.Factory(query, source);
            mData = new LivePagedListBuilder<>(factory, config).build();

            mParser = parser;
            return this;
        }

        @NonNull
        public Builder<T> setDiffCallback(@NonNull DiffUtil.ItemCallback<DocumentSnapshot> diffCallback) {
            mDiffCallback = diffCallback;
            return this;
        }

        @NonNull
        public Builder<T> setLifecycleOwner(@NonNull LifecycleOwner owner) {
            mOwner = owner;
            return this;
        }

        @NonNull
        public FirestorePagingOptions<T> build() {
            if (mData == null || mParser == null) {
                throw new IllegalStateException("Must call setQuery() before calling build().");
            }

            if (mDiffCallback == null) {
                mDiffCallback = new DefaultSnapshotDiffCallback<T>(mParser);
            }

            return new FirestorePagingOptions<>(mData, mParser, mDiffCallback, mOwner);
        }

    }

}
