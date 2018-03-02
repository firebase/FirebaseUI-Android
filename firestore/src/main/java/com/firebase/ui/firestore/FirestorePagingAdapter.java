package com.firebase.ui.firestore;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.ViewGroup;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;

/**
 * Adapter to paginate a Cloud Firestore query and bind it to a RecyclerView.
 *
 * See also {@link FirestorePagingOptions}.
 * See also {@link FirestoreInfiniteScrollListener}.
 */
public abstract class FirestorePagingAdapter<T, VH extends RecyclerView.ViewHolder>
        extends RecyclerView.Adapter<VH>
        implements Page.Listener {

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

        Page page = new Page(0, this);
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
            if (page.getState() == Page.State.LOADED) {
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
        if (hasAny(Page.State.LOADING)) {
            return;
        }

        // Load the last UNLOADED page before the middle "core" of LOADED pages
        int firstLoaded = findFirstOfState(Page.State.LOADED);
        int lastUnloadedBefore = findLastOfState(Page.State.UNLOADED, firstLoaded);

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
        if (hasAny(Page.State.LOADING)) {
            return;
        }

        // There are two cases here
        //  1. Need to load a whole new page
        //  2. Need to load an UNLOADED bottom page

        int lastLoaded = findLastOfState(Page.State.LOADED);

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
            Page nextPage = new Page(nextPageIndex, this);
            mPages.add(nextPage);

            logd("LOADING " + nextPageIndex);
            Query nextQuery = queryAfter(lastSnapshot);
            nextPage.load(nextQuery);
        } else {
            // Case 2: Need to load a previously unloaded page
            // Find the first UNLOADED page after the middle "core" of loaded pages
            int firstUnloadedAfter = findFirstOfState(Page.State.UNLOADED, lastLoaded);

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

    @Override
    public void onPageStateChanged(Page page, Page.State state) {
        if (state == Page.State.LOADING) {
            onLoadingStateChanged(true);
        } else {
            onLoadingStateChanged(hasAny(Page.State.LOADING));
        }

        if (state == Page.State.LOADED) {
            onPageLoaded(page.getIndex(), page.size());
        }

        if (state == Page.State.UNLOADED) {
            onPageUnloaded(page.getIndex(), page.size());
        }
    }

    @Override
    public void onPageError(Page page, Exception ex) {
        Log.w(TAG, "Failed to get page", ex);
        // TODO: Remove page?
    }

    /**
     * Called when a page begins or finishes loading, to indicate if there are any current loading
     * operations going on.
     *
     * Useful to override and control UI elements such as a progress bar or loading spinner.
     */
    protected void onLoadingStateChanged(boolean isLoading) {
        // No-op, this is for overriding.
    }

    private void unloadTopPage() {
        // Find first loaded page
        int firstLoaded = findFirstOfState(Page.State.LOADED);
        if (firstLoaded != -1) {
            mPages.get(firstLoaded).unload();
            logd("UNLOADING " + firstLoaded);
        }
    }

    private void unloadBottomPage() {
        int lastLoaded = findLastOfState(Page.State.LOADED);
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

        if (hasAny(Page.State.LOADING)) {
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

    private int findFirstOfState(Page.State state) {
        return findFirstOfState(state, 0);
    }

    private int findFirstOfState(Page.State state, int startingAt) {
        for (int i = startingAt; i < mPages.size(); i++) {
            Page page = mPages.get(i);
            if (page.getState() == state) {
                return i;
            }
        }

        return -1;
    }

    private int findLastOfState(Page.State state) {
        return findLastOfState(state, mPages.size() - 1);
    }

    private int findLastOfState(Page.State state, int endingAt) {
        for (int i = endingAt; i >= 0; i--) {
            Page page = mPages.get(i);
            if (page.getState() == state) {
                return i;
            }
        }

        return -1;
    }

    private boolean hasAny(Page.State state) {
        for (Page page : mPages) {
            if (page.getState() == state) {
                return true;
            }
        }

        return false;
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

    private static void logd(String message) {
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, message);
        }
    }
}
