package com.firebase.ui.firestore;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RestrictTo;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.ViewGroup;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;

/**
 * Adapter to paginate a Cloud Firestore query and bind it to a RecyclerView.
 *
 * See also {@link FirestorePagingOptions}.
 * See also {@link FirestoreInfiniteScrollListener}.
 */
public abstract class FirestorePagingAdapter<T, VH extends RecyclerView.ViewHolder>
        extends RecyclerView.Adapter<VH> {

    private static final String TAG = "FirestorePagingAdapter";

    private final SnapshotParser<T> mParser;
    private final Query mForwardQuery;
    private final FirestorePagingOptions mOptions;

    private List<Page> mPages = new ArrayList<>();

    /**
     * Create a new paging adapter from the given options.
     */
    public FirestorePagingAdapter(FirestorePagingOptions<T> options) {
        mOptions = options;

        mParser = options.getParser();
        mForwardQuery = options.getQuery();

        Page page = new Page(0);
        mPages.add(page);

        onLoadingStateChanged(true);
        page.load(queryAfter(null));
    }

    @Override
    public abstract VH onCreateViewHolder(ViewGroup parent, int viewType);

    protected abstract void onBindViewHolder(VH holder, int position, T model);

    /**
     * Get the options used to configure the adapter.
     */
    public FirestorePagingOptions getOptions() {
        return mOptions;
    }

    @Override
    public void onBindViewHolder(VH holder, int position) {
        onBindViewHolder(holder, position, get(position));
    }

    /**
     * Get the number of pages currently loaded in memory. This is <b>not</b> the same
     * as the number of pages ever loaded by the adapter as the adapter dynamically unloads
     * pages that are not used.
     */
    public int getPagesLoaded() {
        int count = 0;
        for (Page page : mPages) {
            if (page.getState() == PageState.LOADED) {
                count++;
            }
        }

        return count;
    }

    /**
     * When scrolling up through the list, load the next not-yet-loaded page.
     *
     * If this page load results in the number of loaded pages exceeding the maximum
     * specified in the options, unload the bottom-most page.
     */
    public void loadPageUp() {
        if (countState(PageState.LOADING) > 0) {
            return;
        }

        // Load the last UNLOADED page before the middle "core" of LOADED pages
        int firstLoaded = findFirstOfState(PageState.LOADED);
        int lastUnloadedBefore = findLastOfState(PageState.UNLOADED, firstLoaded);

        if (lastUnloadedBefore != -1) {
            logd("RELOADING " + lastUnloadedBefore);

            Page page = mPages.get(lastUnloadedBefore);
            DocumentSnapshot endBefore = getFirstOfPage(lastUnloadedBefore + 1);

            Query query = queryBetween(page.getFirst(), endBefore);
            page.load(query);
        }

        // Unload the bottom page if too many are loaded
        if (getPagesLoaded() > mOptions.getMaxPages()) {
            unloadBottomPage();
        }
    }

    /**
     * When scrolling down through the list, load the next not-yet-loaded page.
     *
     * If this page load results in the number of loaded pages exceeding the maximum
     * specified in the options, unload the top-most page.
     */
    public void loadNextPage() {
        if (countState(PageState.LOADING) > 0) {
            return;
        }

        // There are two cases here
        //  1. Need to load a whole new page
        //  2. Need to load an UNLOADED bottom page

        int lastLoaded = findLastOfState(PageState.LOADED);

        if (lastLoaded == mPages.size() - 1) {
            // Case 1: Load a new page at the bottom
            Page lastPage = mPages.get(mPages.size() - 1);
            DocumentSnapshot lastSnapshot = lastPage.getLast();

            // Reached the end, no more items to show
            // Note: if items are added to the end this is not detected properly.
            if (lastSnapshot == null) {
                return;
            }

            // Add and start loading
            int nextPageIndex = mPages.size();
            Page nextPage = new Page(nextPageIndex);
            mPages.add(nextPage);

            logd("LOADING " + nextPageIndex);
            Query nextQuery = queryAfter(lastSnapshot);
            nextPage.load(nextQuery);
        } else {
            // Case 2: Need to load a previously unloaded page
            // Find the first UNLOADED page after the middle "core" of loaded pages
            int firstUnloadedAfter = findFirstOfState(PageState.UNLOADED, lastLoaded);

            logd("RELOADING " + firstUnloadedAfter);
            Page page = mPages.get(firstUnloadedAfter);
            DocumentSnapshot endBefore = getFirstOfPage(firstUnloadedAfter + 1);
            Query query = queryBetween(page.getFirst(), endBefore);

            page.load(query);
        }

        // Unload the top-most loaded page if we have too many pages loaded.
        if (getPagesLoaded() > mOptions.getMaxPages()) {
            unloadTopPage();
        }
    }

    /**
     * Get the total number of loaded items among all loaded pages.
     *
     * Note that this operation is O(n) in the number of pages.
     */
    @Override
    public int getItemCount() {
        int size = 0;

        for (Page page : mPages) {
            size += page.size();
        }

        return size;
    }

    /**
     * Get the snapshot at the specified index, where 0 is the first loaded snapshot. These
     * indexes are relative to the snapshots loaded at any given time and are not absolute.
     *
     * This operation is O(n) in the number of pages.
     */
    @NonNull
    public DocumentSnapshot getSnapshot(int index) {
        int remaining = index;
        for (Page page : mPages) {
            if (remaining < page.size()) {
                return page.get(remaining);
            }

            remaining -= page.size();
        }

        throw new IllegalArgumentException(
                "Requested non-existent index: " + index + ", size=" + getItemCount());
    }

    /**
     * Get the model at the specified index by converting a snapfrot from
     * {@link #getSnapshot(int)}.
     */
    @NonNull
    public T get(int index) {
        return mParser.parseSnapshot(getSnapshot(index));
    }

    /**
     * Called when a page begins or finishes loading, to indicate if there are any current loading
     * operations going on.
     *
     * Useful to override and control UI elements such as a progress bar or loading spinner.
     * @param isLoading
     */
    // TODO: Better interface
    protected void onLoadingStateChanged(boolean isLoading) {
        // No-op, this is for overriding.
    }

    private void unloadTopPage() {
        // Find first loaded page
        int firstLoaded = findFirstOfState(PageState.LOADED);
        if (firstLoaded != -1) {
            mPages.get(firstLoaded).unload();
            logd("UNLOADING " + firstLoaded);
        }
    }

    private void unloadBottomPage() {
        int lastLoaded = findLastOfState(PageState.LOADED);
        if (lastLoaded != -1) {
            mPages.get(lastLoaded).unload();
            logd("UNLOADING " + lastLoaded);
        }
    }

    private void onPageLoaded(int index, int size) {
        int itemsBefore = 0;
        for (int i = 0; i < index; i++) {
            itemsBefore += mPages.get(i).size();
        }

        notifyItemRangeInserted(itemsBefore, size);

        if (countState(PageState.LOADING) > 0) {
            onLoadingStateChanged(true);
        } else {
            onLoadingStateChanged(false);
        }
    }

    private void onPageUnloaded(int index, int size) {
        int itemsBefore = 0;
        for (int i = 0; i < index; i++) {
            itemsBefore += mPages.get(i).size();
        }

        notifyItemRangeRemoved(itemsBefore, size);
    }

    private int findFirstOfState(PageState state) {
        return findFirstOfState(state, 0);
    }

    private int findFirstOfState(PageState state, int startingAt) {
        for (int i = startingAt; i < mPages.size(); i++) {
            Page page = mPages.get(i);
            if (page.getState() == state) {
                return i;
            }
        }

        return -1;
    }

    private int findLastOfState(PageState state) {
        return findLastOfState(state, mPages.size() - 1);
    }

    private int findLastOfState(PageState state, int endingAt) {
        for (int i = endingAt; i >= 0; i--) {
            Page page = mPages.get(i);
            if (page.getState() == state) {
                return i;
            }
        }

        return -1;
    }

    private int countState(PageState state) {
        int count = 0;
        for (Page page : mPages) {
            if (page.getState() == state) {
                count++;
            }
        }

        return count;
    }

    @Nullable
    private DocumentSnapshot getFirstOfPage(int i) {
        if (i < 0 || i >= mPages.size()) {
            return null;
        }

        Page page = mPages.get(i);
        if (page == null) {
            return null;
        }

        return page.getFirst();
    }

    private Query queryBetween(@Nullable DocumentSnapshot startAt,
                               @Nullable DocumentSnapshot endBefore) {

        Query query = mForwardQuery;
        if (startAt != null) {
            query = query.startAt(startAt);
        }

        if (endBefore != null) {
            query = query.endBefore(endBefore);
        } else {
            query = query.limit(mOptions.getPageSize());
        }

        return query;
    }

    private Query queryAfter(@Nullable DocumentSnapshot startAfter) {
        Query query = mForwardQuery;
        if (startAfter != null) {
            query = query.startAfter(startAfter);
        }

        query = query.limit(mOptions.getPageSize());

        return query;
    }

    private void logd(String message) {
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, message);
        }
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    private enum PageState {
        LOADING,
        LOADED,
        UNLOADED
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    private class Page implements OnCompleteListener<QuerySnapshot> {

        private final int mIndex;
        private PageState mState;
        private DocumentSnapshot mFirstSnapshot;

        private List<DocumentSnapshot> mSnapshots = new ArrayList<>();

        public Page(int index) {
            mIndex = index;
            mState = PageState.UNLOADED;
        }

        public void load(Query query) {
            if (mState == PageState.LOADING) {
                return;
            }

            mState = PageState.LOADING;
            onLoadingStateChanged(true);
            query.get().addOnCompleteListener(this);
        }

        public void unload() {
            int size = mSnapshots.size();
            mSnapshots.clear();

            onPageUnloaded(mIndex, size);
            mState = PageState.UNLOADED;
        }

        @Override
        public void onComplete(@NonNull Task<QuerySnapshot> task) {
            // TODO: Better error handling
            if (!task.isSuccessful()) {
                Log.w(TAG, "Failed to get page", task.getException());
            }

            // Add all snapshots
            mSnapshots.addAll(task.getResult().getDocuments());

            // Set first in page
            if (mSnapshots.isEmpty()) {
                mFirstSnapshot = null;
            } else {
                mFirstSnapshot = mSnapshots.get(0);
            }

            // Mark page as loaded
            logd("LOADED " + mIndex);
            mState = PageState.LOADED;
            onPageLoaded(mIndex, mSnapshots.size());
        }

        public PageState getState() {
            return mState;
        }

        public DocumentSnapshot get(int index) {
            return mSnapshots.get(index);
        }

        public int size() {
            return mSnapshots.size();
        }

        @Nullable
        public DocumentSnapshot getFirst() {
            return mFirstSnapshot;
        }

        @Nullable
        public DocumentSnapshot getLast() {
            if (mSnapshots == null || mSnapshots.isEmpty()) {
                return null;
            }
            return mSnapshots.get(mSnapshots.size() - 1);
        }
    }
}
