package com.firebase.ui.firestore.paging;

import android.arch.lifecycle.LifecycleOwner;
import android.arch.lifecycle.LiveData;
import android.arch.paging.LivePagedListBuilder;
import android.arch.paging.PagedList;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.firebase.ui.firestore.ClassSnapshotParser;
import com.firebase.ui.firestore.SnapshotParser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.Query;

/**
 * TODO: Document
 */
public class FirestorePagingOptions<T> {

    // TODO: Default config

    private final LiveData<PagedList<DocumentSnapshot>> mData;
    private final SnapshotParser<T> mParser;
    private final LifecycleOwner mOwner;

    private FirestorePagingOptions(@NonNull LiveData<PagedList<DocumentSnapshot>> data,
                                  @NonNull SnapshotParser<T> parser,
                                  @Nullable LifecycleOwner owner) {
        mData = data;
        mParser = parser;
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

    @Nullable
    public LifecycleOwner getOwner() {
        return mOwner;
    }

    public static class Builder<T> {

        private LiveData<PagedList<DocumentSnapshot>> mData;
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
            mData = new LivePagedListBuilder<>(
                    FirestoreDataSource.newFactory(query),
                    config).build();

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
