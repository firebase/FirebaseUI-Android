package com.firebase.ui.firestore;

import android.util.Log;

import com.firebase.ui.firestore.paging.FirestorePagingSource;
import com.firebase.ui.firestore.paging.LoadingState;
import com.firebase.ui.firestore.paging.PageKey;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.Source;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import androidx.annotation.Nullable;
import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import androidx.lifecycle.Observer;
import androidx.paging.PagingSource;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(AndroidJUnit4.class)
public class FirestorePagingSourceTest {

    private FirestorePagingSource mPagingSource;

    /**
     * Needed to run tasks on the main thread so observeForever() doesn't throw.
     */
    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    @Mock Query mMockQuery;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        initMockQuery();

        // Create a testing paging source
        mPagingSource = new FirestorePagingSource(mMockQuery, Source.DEFAULT);
    }

    @Test
    public void testLoadInitial_success() throws Exception {
        mockQuerySuccess(new ArrayList<DocumentSnapshot>());

        TestObserver<LoadingState> observer = new TestObserver<>(2);
        mPagingSource.getLoadingState().observeForever(observer);

        // Kick off an initial load of 20 items
        PagingSource.LoadParams.Refresh<PageKey> params = new PagingSource.LoadParams.Refresh<>(null, 20, false);
        mPagingSource.loadSingle(params).blockingSubscribe();

        // Should go from LOADING_INITIAL --> LOADED --> LOADING_FINISHED
        observer.await();
        observer.assertResults(Arrays.asList(LoadingState.LOADING_INITIAL, LoadingState.LOADED,
                LoadingState.FINISHED));
    }

    @Test
    public void testLoadInitial_failure() throws Exception {
        mockQueryFailure("Could not get initial documents.");

        TestObserver<LoadingState> observer = new TestObserver<>(2);
        mPagingSource.getLoadingState().observeForever(observer);

        // Kick off an initial load of 20 items
        PagingSource.LoadParams.Refresh<PageKey> params = new PagingSource.LoadParams.Refresh<>(null, 20, false);
        mPagingSource.loadSingle(params).blockingSubscribe();

        // Should go from LOADING_INITIAL --> ERROR
        observer.await();
        observer.assertResults(Arrays.asList(LoadingState.LOADING_INITIAL, LoadingState.ERROR));
    }

    @Test
    public void testLoadAfter_success() throws Exception {
        mockQuerySuccess(new ArrayList<DocumentSnapshot>());

        TestObserver<LoadingState> observer = new TestObserver<>(2);
        mPagingSource.getLoadingState().observeForever(observer);

        // Kick off an initial load of 20 items
        PageKey pageKey = new PageKey(null, null);
        PagingSource.LoadParams.Append<PageKey> params = new PagingSource.LoadParams.Append<>(pageKey, 20, false);
        mPagingSource.loadSingle(params).blockingSubscribe();

        // Should go from LOADING_MORE --> LOADED --> LOADING_FINISHED
        observer.await();
        observer.assertResults(Arrays.asList(LoadingState.LOADING_MORE, LoadingState.LOADED,
                LoadingState.FINISHED));
    }

    @Test
    public void testLoadAfter_failure() throws Exception {
        mockQueryFailure("Could not load more documents.");

        TestObserver<LoadingState> observer = new TestObserver<>(2);
        mPagingSource.getLoadingState().observeForever(observer);

        // Kick off an initial load of 20 items
        PageKey pageKey = new PageKey(null, null);
        PagingSource.LoadParams.Append<PageKey> params = new PagingSource.LoadParams.Append<>(pageKey, 20, false);
        mPagingSource.loadSingle(params).blockingSubscribe();

        // Should go from LOADING_MORE --> ERROR
        observer.await();
        observer.assertResults(Arrays.asList(LoadingState.LOADING_MORE, LoadingState.ERROR));
    }

//    @Test
//    public void testLoadAfter_retry() throws Exception {
//        mockQueryFailure("Could not load more documents.");
//
//        TestObserver<LoadingState> observer1 = new TestObserver<>(2);
//        mPagingSource.getLoadingState().observeForever(observer1);
//
//        // Kick off an initial load of 20 items
//        PageKey pageKey = new PageKey(null, null);
//        PagingSource.LoadParams.Append<PageKey> params = new PagingSource.LoadParams.Append<>
//        (pageKey, 20, false);
//        mPagingSource.loadSingle(params).blockingSubscribe();
//
//        // Should go from LOADING_MORE --> ERROR
//        observer1.await();
//        observer1.assertResults(Arrays.asList(LoadingState.LOADING_MORE, LoadingState.ERROR));
//
//        // Create a new observer
//        TestObserver<LoadingState> observer2 = new TestObserver<>(3);
//        mPagingSource.getLoadingState().observeForever(observer2);
//
//        // Retry the load
//        mockQuerySuccess(new ArrayList<DocumentSnapshot>());
////        mPagingSource.retry();
//
//        // Should go from ERROR --> LOADING_MORE --> SUCCESS
//        observer2.await();
//        observer2.assertResults(
//                Arrays.asList(LoadingState.ERROR, LoadingState.LOADING_MORE, LoadingState.LOADED));
//    }

    private void initMockQuery() {
        when(mMockQuery.startAfter(any(DocumentSnapshot.class))).thenReturn(mMockQuery);
        when(mMockQuery.endBefore(any(DocumentSnapshot.class))).thenReturn(mMockQuery);
        when(mMockQuery.limit(anyLong())).thenReturn(mMockQuery);
    }

    private void mockQuerySuccess(List<DocumentSnapshot> snapshots) {
        QuerySnapshot mockSnapshot = mock(QuerySnapshot.class);
        when(mockSnapshot.getDocuments()).thenReturn(snapshots);

        when(mMockQuery.get(Source.DEFAULT)).thenReturn(Tasks.forResult(mockSnapshot));
    }

    private void mockQueryFailure(String message) {
        when(mMockQuery.get(Source.DEFAULT))
                .thenReturn(Tasks.<QuerySnapshot>forException(new Exception(message)));
    }

    private static class TestObserver<T> implements Observer<T> {

        private final List<T> mResults = new ArrayList<>();
        private final CountDownLatch mLatch;

        public TestObserver(int expectedCount) {
            mLatch = new CountDownLatch(expectedCount);
        }

        @Override
        public void onChanged(@Nullable T t) {
            if (t != null) {
                mResults.add(t);
                mLatch.countDown();
            }
        }

        public List<T> getResults() {
            return mResults;
        }

        public void await() throws InterruptedException {
            mLatch.await();
        }

        public void assertResults(List<T> expected) {
            assertEquals(expected.size(), mResults.size());

            for (int i = 0; i < mResults.size(); i++) {
//                Log.e("Test", mResults.get(i) + "");
                assertEquals(mResults.get(i), expected.get(i));
            }
        }

    }
}
