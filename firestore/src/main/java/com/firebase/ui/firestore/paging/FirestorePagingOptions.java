package com.firebase.ui.firestore.paging;

import android.arch.lifecycle.LifecycleOwner;
import android.arch.paging.PagedList;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.firebase.ui.firestore.ClassSnapshotParser;
import com.firebase.ui.firestore.SnapshotParser;
import com.google.firebase.firestore.Query;

/**
 * Options to conifigure an {@link FirestorePagingAdapter}.
 */
public class FirestorePagingOptions<T> {

    private final PagingData mData;
    private final SnapshotParser<T> mParser;
    private final LifecycleOwner mOwner;

    private FirestorePagingOptions(@NonNull PagingData data,
                                   @NonNull SnapshotParser<T> parser,
                                   @Nullable LifecycleOwner owner) {
        mData = data;
        mParser = parser;
        mOwner = owner;
    }

    @NonNull
    public PagingData getData() {
        return mData;
    }

    @NonNull
    public SnapshotParser<T> getParser() {
        return mParser;
    }

    @Nullable
    public LifecycleOwner getOwner() {
        return mOwner;
    }

    public static class Builder<T> {

        private PagingData mData;
        private SnapshotParser<T> mParser;
        private LifecycleOwner mOwner;

        @NonNull
        public Builder<T> setQuery(@NonNull Query query,
                                   @NonNull PagedList.Config config,
                                   @NonNull Class<T> modelClass) {
            return setQuery(query, config, new ClassSnapshotParser<T>(modelClass));
        }

        @NonNull
        public Builder<T> setQuery(@NonNull Query query,
                                   @NonNull PagedList.Config config,
                                   @NonNull SnapshotParser<T> parser) {


            // Build paged list
            FirestoreDataSource.Factory factory = new FirestoreDataSource.Factory(query);
            mData = new PagingData(factory, config);

            mParser = parser;
            return this;
        }

        @NonNull
        public Builder<T> setOwner(@NonNull LifecycleOwner owner) {
            mOwner = owner;
            return this;
        }

        public FirestorePagingOptions<T> build() {
            return new FirestorePagingOptions<>(mData, mParser, mOwner);
        }

    }

}
