package com.firebase.ui.firestore;

import android.os.Handler;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;

public class FirestoreInfiniteScrollListener extends RecyclerView.OnScrollListener {

    private LinearLayoutManager mManager;
    private FirestoreInfiniteArray mArray;

    private Handler mHandler = new Handler();

    // TODO: This could take the same options as the Array
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

        // TODO: configurable "closeness" and number of pages to keep
        boolean movingDown = dy > 0;
        boolean movingUp = !movingDown;
        boolean closeToTop = (firstVisible <= 5);
        boolean closeToBottom = (totalSize - lastVisible) <= 5;

        if (closeToBottom && movingDown) {

            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    // Load one page down
                    mArray.loadNextPage();

                    // Unload top page.
                    if (mArray.getPagesLoaded() >= 3 ) {
                        mArray.unloadTopPage();
                    }
                }
            });

        } else if (closeToTop && movingUp) {

            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    // Load one page up
                    mArray.loadPrevPage();

                    // Unload bottom page
                    if (mArray.getPagesLoaded() >= 3) {
                        mArray.unloadBottomPage();
                    }
                }
            });

        }
    }

}
