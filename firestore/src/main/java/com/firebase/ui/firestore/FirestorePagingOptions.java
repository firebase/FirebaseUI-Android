package com.firebase.ui.firestore;

import com.google.firebase.firestore.Query;

/**
 * Options to configure a {@link FirestorePagingAdapter}
 * and {@link FirestoreInfiniteScrollListener}.
 *
 * See {@link Builder} to create a new instance.
 */
public class FirestorePagingOptions<T> {

    private final Query mQuery;
    private final SnapshotParser<T> mParser;

    private final int mPageSize;
    private final int mLoadTriggerDistance;
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

    /**
     * Get the base query to be used for paging.
     */
    public Query getQuery() {
        return mQuery;
    }

    /**
     * Get the {@link SnapshotParser} to be used to parse Firestore document snapshots.
     */
    public SnapshotParser<T> getParser() {
        return mParser;
    }

    /**
     * Get the distance from the top/bottom of the list that will trigger a new page load.
     */
    public int getLoadTriggerDistance() {
        return mLoadTriggerDistance;
    }

    /**
     * Get the max number of pages to keep in memory at any time.
     */
    public int getMaxPages() {
        return mMaxPages;
    }

    /**
     * Get the maximum number of items to load as a single page.
     */
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

        /**
         * Set the <b>base</b> query for pagination. The query may contain where() and orderBy()
         * clauses but may not contain any startAt/endAt or limit clauses as those will be
         * added automatically by the paging adapter.
         *
         * @param query the base query.
         * @param parser the SnapshotParser to be used to parse document snapshots.
         */
        public Builder<T> setQuery(Query query, SnapshotParser<T> parser) {
            mQuery = query;
            mParser = parser;
            return this;
        }

        /**
         * Sets the query using a standard {@link ClassSnapshotParser}.
         *
         * See {@link #setQuery(Query, Class)} for details.
         */
        public Builder<T> setQuery(Query query, Class<T> clazz) {
            mQuery = query;
            mParser = new ClassSnapshotParser<>(clazz);
            return this;
        }

        /**
         * Set the maximum number of items to load in a single page.
         */
        public Builder<T> setPageSize(int pageSize) {
            mPageSize = pageSize;
            return this;
        }

        /**
         * Set the distance (in items) from the bottom or top of the data set that will trigger
         * the adapter to load the next page.
         *
         * This option is ignored unless a {@link FirestoreInfiniteScrollListener} is
         * attached to the adapter.
         */
        public Builder<T> setLoadTriggerDistance(int distance) {
            mLoadTriggerDistance = distance;
            return this;
        }

        /**
         * Set the maximum number of pages to keep in memory at a time.
         */
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
