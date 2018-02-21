package com.firebase.ui.firestore;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.firebase.ui.common.ChangeEventType;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class FirestoreInfiniteArray<T> extends ObservableSnapshotArray<T> {

    private static final String TAG = "FirestoreInfiniteArray";

    private SnapshotParser<T> mParser;

    private Query mForwardQuery;
    private Query mReverseQuery;

    private int mPageSize = 10;

    private List<Page> mPages = new ArrayList<>();

    public FirestoreInfiniteArray(Query forwardQuery, Query reverseQuery, SnapshotParser<T> parser) {
        super(parser);

        mParser = parser;
        mForwardQuery = forwardQuery;
        mReverseQuery = reverseQuery;

        Page page = new Page(0, queryAfter(null));
        mPages.add(page);
    }

    public void loadNextPage() {
        Page lastPage = getLastPage();

        if (lastPage.getState() == PageState.LOADING) {
            Log.d(TAG, "Skipping double-load.");
            return;
        }

        int size = size();
        DocumentSnapshot lastSnapshot = lastPage.getLast();

        if (lastSnapshot == null) {
            Log.w(TAG, "Skipping load because last snapshot is null!");
            return;
        }

        Query nextQuery = queryAfter(lastSnapshot);
        Log.d(TAG, "loadNextPage: sizeBefore=" + size + ", lastId=" + lastSnapshot.getId());
        Page nextPage = new Page(size, nextQuery);
        mPages.add(nextPage);
    }


    @NonNull
    @Override
    protected List<DocumentSnapshot> getSnapshots() {
        // TODO: How to allow outside clearing?
        Log.w(TAG, "Called getSnapshots() on an infinite array.");
        return Collections.emptyList();
    }

    @NonNull
    @Override
    public DocumentSnapshot getSnapshot(int index) {
        int remaining = index;
        for (Page page : mPages) {
            if (remaining < page.size()) {
                return page.get(remaining);
            }

            remaining -= page.size();
        }

        // TODO
        Log.e(TAG, "Requested bad index: " + index);
        return null;
    }

    @NonNull
    @Override
    public T get(int index) {
        return mParser.parseSnapshot(getSnapshot(index));
    }

    @Override
    public int size() {
        int size = 0;

        for (Page page : mPages) {
            size += page.size();
        }

        return size;
    }

    private Page getLastPage() {
        return mPages.get(mPages.size() - 1);
    }

    private Query queryAfter(@Nullable DocumentSnapshot snapshot) {
        if (snapshot == null) {
            return mForwardQuery.limit(mPageSize);
        }

        return mForwardQuery
                .startAfter(snapshot)
                .limit(mPageSize);
    }

    private enum PageState {
        LOADING,
        LOADED
    }

    private class Page implements ChangeEventListener {

        // TODO: state

        private PageState mState = PageState.LOADING;

        public final int mStartingPosition;
        public final Query mQuery;
        public final FirestoreArray<T> mItems;

        private List<DocumentSnapshot> mSnapshots = new ArrayList<>();

        public Page(int startingPosition, Query query) {
            mStartingPosition = startingPosition;
            mQuery = query;

            mItems = new FirestoreArray<>(query, mParser);
            mItems.addChangeEventListener(this);
        }

        @Override
        public void onChildChanged(@NonNull ChangeEventType type,
                                   @NonNull DocumentSnapshot snapshot,
                                   int newIndex,
                                   int oldIndex) {
            switch (type) {
                // TODO: Implement all types
                case ADDED:
                    int newIndexAdjusted = newIndex + mStartingPosition;
                    int oldIndexAdjusted = oldIndex == -1
                            ? -1
                            : oldIndex + mStartingPosition;

                    Log.d(TAG, "onChildAdded, old=" + oldIndexAdjusted + ", new=" + newIndexAdjusted);
                    notifyOnChildChanged(type, snapshot, newIndexAdjusted, oldIndexAdjusted);
                    mSnapshots.add(snapshot);
                    break;
                case MOVED:
                    break;
                case CHANGED:
                    break;
                case REMOVED:
                    break;
            }
        }

        @Override
        public void onDataChanged() {
            // TODO
            Log.d(TAG, "onDataChanged");
            mState = PageState.LOADED;
        }

        @Override
        public void onError(@NonNull FirebaseFirestoreException e) {
            // TODO
            mState = PageState.LOADED;
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
        public DocumentSnapshot getLast() {
            if (mSnapshots == null || mSnapshots.isEmpty()) {
                return null;
            }
            return mSnapshots.get(mSnapshots.size() - 1);
        }
    }
}
