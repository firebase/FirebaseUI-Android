package com.firebase.ui.firestore;

import android.os.Handler;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;

/**
 * RecyclerView scroll listener that triggers loading/unloading in a
 * {@link FirestorePagingAdapter} so that the adapter can appear to be infinite sroll.
 */
public class FirestoreInfiniteScrollListener extends RecyclerView.OnScrollListener {

    private final LinearLayoutManager mManager;
    private final FirestorePagingAdapter mAdapter;

    private Handler mHandler = new Handler();

    public FirestoreInfiniteScrollListener(LinearLayoutManager manager,
                                           FirestorePagingAdapter adapter) {
        mManager = manager;
        mAdapter = adapter;
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

        int totalSize = mAdapter.getItemCount();

        boolean movingDown = dy > 0;
        boolean movingUp = !movingDown;
        boolean closeToTop = (firstVisible <= mAdapter.getOptions().getLoadTriggerDistance());
        boolean closeToBottom = (totalSize - lastVisible) <= mAdapter.getOptions().getLoadTriggerDistance();

        if (closeToBottom && movingDown) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    // Load one page down
                    mAdapter.loadNextPage();
                }
            });

        } else if (closeToTop && movingUp) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    // Load one page up
                    mAdapter.loadPageUp();
                }
            });
        }
    }

}
