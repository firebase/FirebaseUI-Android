package com.firebase.ui.firestore;

import com.google.firebase.firestore.Query;

/**
 * Options to configure a {@link FirestorePagingAdapter}
 * and {@link FirestoreInfiniteScrollListener}.
 *
 * TODO(samstern): Document all methods.
 */
public class FirestorePagingOptions<T> {

    private final Query mQuery;
    private final SnapshotParser<T> mParser;

    private final int mPageSize;
    private final int mLoadTriggerDistance ;
    private final int mMaxPages;

    FirestorePagingOptions(Query query,
                           SnapshotParser<T> parser,
                           int pageSize,
                           int loadTriggerDistance,
                           int maxPages) {
        mQuery = query;
        mParser = parser;

        mPageSize = pageSize;
        mLoadTriggerDistance = loadTriggerDistance;
        mMaxPages = maxPages;
    }

    public Query getQuery() {
        return mQuery;
    }

    public SnapshotParser<T> getParser() {
        return mParser;
    }

    public int getLoadTriggerDistance() {
        return mLoadTriggerDistance;
    }

    public int getMaxPages() {
        return mMaxPages;
    }

    public int getPageSize() {
        return mPageSize;
    }

    public static class Builder<T> {

        private Query mQuery;
        private SnapshotParser<T> mParser;

        private int mPageSize = 10;
        private int mLoadTriggerDistance = 5;
        private int mMaxPages = 3;

        public Builder() {}

        public Builder<T> setQuery(Query query, SnapshotParser<T> parser) {
            mQuery = query;
            mParser = parser;
            return this;
        }

        public Builder<T> setQuery(Query query, Class<T> clazz) {
            mQuery = query;
            mParser = new ClassSnapshotParser<>(clazz);
            return this;
        }


        public Builder<T> setPageSize(int pageSize) {
            mPageSize = pageSize;
            return this;
        }

        public Builder<T> setLoadTriggerDistance(int distance) {
            mLoadTriggerDistance = distance;
            return this;
        }

        public Builder<T> setMaxPages(int maxPages) {
            mMaxPages = maxPages;
            return this;
        }

        public FirestorePagingOptions<T> build() {
            return new FirestorePagingOptions<>(mQuery, mParser,
                    mPageSize, mLoadTriggerDistance, mMaxPages);
        }
    }

}
