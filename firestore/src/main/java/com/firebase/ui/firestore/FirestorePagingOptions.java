package com.firebase.ui.firestore;

/**
 * Created by samstern on 3/1/18.
 */
public class FirestorePagingOptions {

    private final int mPageSize;
    private final int mLoadTriggerDistance ;
    private final int mMaxPages;

    FirestorePagingOptions(int pageSize, int loadTriggerDistance, int maxPages) {
        mPageSize = pageSize;
        mLoadTriggerDistance = loadTriggerDistance;
        mMaxPages = maxPages;
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

    public static class Builder {

        private int mPageSize = 10;
        private int mLoadTriggerDistance = 5;
        private int mMaxPages = 3;

        public Builder() {}

        public Builder setPageSize(int pageSize) {
            mPageSize = pageSize;
            return this;
        }

        public Builder setLoadTriggerDistance(int distance) {
            mLoadTriggerDistance = distance;
            return this;
        }

        public Builder setMaxPages(int maxPages) {
            mMaxPages = maxPages;
            return this;
        }

        public FirestorePagingOptions build() {
            return new FirestorePagingOptions(mPageSize, mLoadTriggerDistance, mMaxPages);
        }
    }

}
