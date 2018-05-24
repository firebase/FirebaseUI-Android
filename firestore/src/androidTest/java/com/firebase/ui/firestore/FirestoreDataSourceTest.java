package com.firebase.ui.firestore;

import android.arch.lifecycle.Observer;
import android.arch.paging.PageKeyedDataSource;
import android.support.annotation.Nullable;
import android.support.test.runner.AndroidJUnit4;

import com.firebase.ui.firestore.paging.FirestoreDataSource;
import com.firebase.ui.firestore.paging.LoadingState;
import com.firebase.ui.firestore.paging.PageKey;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.Source;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(AndroidJUnit4.class)
public class FirestoreDataSourceTest {

    private FirestoreDataSource mDataSource;

    @Mock Query mMockQuery;
    @Mock PageKeyedDataSource.LoadInitialCallback<PageKey, DocumentSnapshot> mInitialCallback;
    @Mock PageKeyedDataSource.LoadCallback<PageKey, DocumentSnapshot> mAfterCallback;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        initMockQuery();

        // Create a testing data source
        mDataSource = new FirestoreDataSource(mMockQuery, Source.DEFAULT);
    }

    @Test
    public void testLoadInitial_success() throws Exception {
        mockQuerySuccess(new ArrayList<DocumentSnapshot>());

        TestObserver<LoadingState> observer = new TestObserver<>(2);
        mDataSource.getLoadingState().observeForever(observer);

        // Kick off an initial load of 20 items
        PageKeyedDataSource.LoadInitialParams<PageKey> params =
                new PageKeyedDataSource.LoadInitialParams<>(20, false);
        mDataSource.loadInitial(params, mInitialCallback);

        // Should go from LOADING_INITIAL --> LOADED
        observer.await();
        observer.assertResults(Arrays.asList(LoadingState.LOADING_INITIAL, LoadingState.LOADED));
    }

    @Test
    public void testLoadInitial_failure() throws Exception {
        mockQueryFailure("Could not get initial documents.");

        TestObserver<LoadingState> observer = new TestObserver<>(2);
        mDataSource.getLoadingState().observeForever(observer);

        // Kick off an initial load of 20 items
        PageKeyedDataSource.LoadInitialParams<PageKey> params =
                new PageKeyedDataSource.LoadInitialParams<>(20, false);
        mDataSource.loadInitial(params, mInitialCallback);

        // Should go from LOADING_INITIAL --> ERROR
        observer.await();
        observer.assertResults(Arrays.asList(LoadingState.LOADING_INITIAL, LoadingState.ERROR));
    }

    @Test
    public void testLoadAfter_success() throws Exception {
        mockQuerySuccess(new ArrayList<DocumentSnapshot>());

        TestObserver<LoadingState> observer = new TestObserver<>(2);
        mDataSource.getLoadingState().observeForever(observer);

        // Kick off an initial load of 20 items
        PageKey pageKey = new PageKey(null, null);
        PageKeyedDataSource.LoadParams<PageKey> params =
                new PageKeyedDataSource.LoadParams<>(pageKey, 20);
        mDataSource.loadAfter(params, mAfterCallback);

        // Should go from LOADING_MORE --> LOADED
        observer.await();
        observer.assertResults(Arrays.asList(LoadingState.LOADING_MORE, LoadingState.LOADED));
    }

    @Test
    public void testLoadAfter_failure() throws Exception {
        mockQueryFailure("Could not load more documents.");

        TestObserver<LoadingState> observer = new TestObserver<>(2);
        mDataSource.getLoadingState().observeForever(observer);

        // Kick off an initial load of 20 items
        PageKey pageKey = new PageKey(null, null);
        PageKeyedDataSource.LoadParams<PageKey> params =
                new PageKeyedDataSource.LoadParams<>(pageKey, 20);
        mDataSource.loadAfter(params, mAfterCallback);

        // Should go from LOADING_MORE --> ERROR
        observer.await();
        observer.assertResults(Arrays.asList(LoadingState.LOADING_MORE, LoadingState.ERROR));
    }

    @Test
    public void testLoadAfter_retry() throws Exception {
        mockQueryFailure("Could not load more documents.");

        TestObserver<LoadingState> observer1 = new TestObserver<>(2);
        mDataSource.getLoadingState().observeForever(observer1);

        // Kick off an initial load of 20 items
        PageKey pageKey = new PageKey(null, null);
        PageKeyedDataSource.LoadParams<PageKey> params =
                new PageKeyedDataSource.LoadParams<>(pageKey, 20);
        mDataSource.loadAfter(params, mAfterCallback);

        // Should go from LOADING_MORE --> ERROR
        observer1.await();
        observer1.assertResults(Arrays.asList(LoadingState.LOADING_MORE, LoadingState.ERROR));

        // Create a new observer
        TestObserver<LoadingState> observer2 = new TestObserver<>(3);
        mDataSource.getLoadingState().observeForever(observer2);

        // Retry the load
        mockQuerySuccess(new ArrayList<DocumentSnapshot>());
        mDataSource.retry();

        // Should go from ERROR --> LOADING_MORE --> SUCCESS
        observer2.await();
        observer2.assertResults(
                Arrays.asList(LoadingState.ERROR, LoadingState.LOADING_MORE, LoadingState.LOADED));
    }

    private void initMockQuery() {
        when(mMockQuery.startAfter(any())).thenReturn(mMockQuery);
        when(mMockQuery.endBefore(any())).thenReturn(mMockQuery);
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
                assertEquals(mResults.get(i), expected.get(i));
            }
        }

    }
}
