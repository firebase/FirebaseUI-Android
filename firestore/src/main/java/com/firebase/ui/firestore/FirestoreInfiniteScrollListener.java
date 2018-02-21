package com.firebase.ui.firestore;

import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;

public class FirestoreInfiniteScrollListener extends RecyclerView.OnScrollListener {

    private LinearLayoutManager mManager;
    private FirestoreInfiniteArray mArray;

    public FirestoreInfiniteScrollListener(LinearLayoutManager manager,
                                           FirestoreInfiniteArray array) {
        mManager = manager;
        mArray = array;
    }

    @Override
    public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
        super.onScrollStateChanged(recyclerView, newState);
    }

    @Override
    public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
        super.onScrolled(recyclerView, dx, dy);

        int firstVisible = mManager.findFirstVisibleItemPosition();
        int lastVisible = mManager.findLastVisibleItemPosition();

        int totalSize = mArray.size();

        // TODO: configurable "closeness"
        boolean movingDown = dy > 0;
        boolean closeToBottom = (totalSize - lastVisible) <= 5;

        if (closeToBottom && movingDown) {
            mArray.loadNextPage();
        }
    }

}
