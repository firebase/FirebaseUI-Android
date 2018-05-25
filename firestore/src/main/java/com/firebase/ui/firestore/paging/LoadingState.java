package com.firebase.ui.firestore.paging;

/**
 * Loading state exposed by {@link FirestorePagingAdapter}.
 */
public enum LoadingState {
    /**
     * Loading initial data.
     */
    LOADING_INITIAL,

    /**
     * Loading a page other than the first page.
     */
    LOADING_MORE,

    /**
     * Not currently loading any pages, at least one page loaded.
     */
    LOADED,

    /**
     * The last page loaded had zero documents, and therefore no further pages will be loaded.
     */
    FINISHED,

    /**
     * The most recent load encountered an error.
     */
    ERROR
}
