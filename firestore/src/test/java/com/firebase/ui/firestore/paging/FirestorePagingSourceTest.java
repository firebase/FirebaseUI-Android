package com.firebase.ui.firestore.paging;

import com.google.android.gms.tasks.OnCanceledListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
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
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

import androidx.paging.PagingSource;
import androidx.paging.PagingSource.LoadParams.Append;
import androidx.paging.PagingSource.LoadParams.Refresh;
import androidx.paging.PagingSource.LoadResult.Page;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.functions.Consumer;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
public class FirestorePagingSourceTest {

    @Mock
    Query mMockQuery;

    ArrayList<DocumentSnapshot> mMockSnapshots = new ArrayList<>();
    Exception mMockException = new Exception("Could not load Data");

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        initMockQuery();

        for (int i = 0; i < 2; i++) {
            mMockSnapshots.add(mock(DocumentSnapshot.class));
        }
    }

    @Test
    public void testLoadInitial_success() {
        FirestorePagingSource pagingSource = new FirestorePagingSource(mMockQuery, Source.DEFAULT);
        mockQuerySuccess(mMockSnapshots);
        Page<PageKey, DocumentSnapshot> expected = new Page<>(mMockSnapshots, null, new PageKey(null, null));

        Refresh<PageKey> refreshRequest = new Refresh<>(null, 2, false);
        PagingSource.LoadResult<PageKey, DocumentSnapshot> actual =
                pagingSource.loadSingle(refreshRequest).blockingGet();

        assertTrue(actual instanceof Page);
        assertEquals(expected, actual);
    }

    @Test
    public void testLoadInitial_failure() {
        FirestorePagingSource pagingSource = new FirestorePagingSource(mMockQuery, Source.DEFAULT);
        mockQueryFailure(mMockException);
        PagingSource.LoadResult.Error<PageKey, DocumentSnapshot> expected =
                new PagingSource.LoadResult.Error<>(mMockException);

        Refresh<PageKey> refreshRequest = new Refresh<>(null, 2, false);
        PagingSource.LoadResult<PageKey, DocumentSnapshot> actual =
                pagingSource.loadSingle(refreshRequest).blockingGet();

        assertEquals(expected, actual);
    }

    @Test
    public void testLoadAfter_success() {
        FirestorePagingSource pagingSource = new FirestorePagingSource(mMockQuery, Source.DEFAULT);
        mockQuerySuccess(mMockSnapshots);
        PageKey pageKey = new PageKey(null, null);
        Page<PageKey, DocumentSnapshot> expected = new Page<>(mMockSnapshots, null, pageKey);

        Append<PageKey> appendRequest = new Append<>(pageKey, 2, false);
        PagingSource.LoadResult<PageKey, DocumentSnapshot> actual =
                pagingSource.loadSingle(appendRequest).blockingGet();

        assertTrue(actual instanceof Page);
        assertEquals(expected, actual);
    }

    @Test
    public void testLoadAfter_failure() {
        FirestorePagingSource pagingSource = new FirestorePagingSource(mMockQuery, Source.DEFAULT);
        mockQueryFailure(mMockException);
        PageKey pageKey = new PageKey(null, null);
        PagingSource.LoadResult.Error<PageKey, DocumentSnapshot> expected =
                new PagingSource.LoadResult.Error<>(mMockException);

        Append<PageKey> appendRequest = new Append<>(pageKey, 2, false);
        PagingSource.LoadResult<PageKey, DocumentSnapshot> actual =
                pagingSource.loadSingle(appendRequest).blockingGet();

        assertEquals(expected, actual);
    }

    /**
     * Cancelling a load interrupts the thread blocked in Tasks.await(). The resulting
     * InterruptedException must not escape the callable: by then the subscription is disposed, so
     * RxJava would report it as an UndeliverableException and crash the app.
     *
     * See https://github.com/firebase/FirebaseUI-Android/issues/2008
     */
    @Test
    public void testLoadSingle_interruptedWhileDisposed_doesNotReportUndeliverableException()
            throws Exception {
        FirestorePagingSource pagingSource = new FirestorePagingSource(mMockQuery, Source.DEFAULT);

        // Counted down once Tasks.await() starts registering listeners, which means the load is
        // genuinely running and about to block. Without waiting on this, the test could dispose
        // before the callable ever ran — and would then pass even against the unfixed source.
        CountDownLatch loadInFlight = new CountDownLatch(1);
        mockQueryNeverCompletes(loadInFlight);

        List<Throwable> undeliverableErrors = new CopyOnWriteArrayList<>();
        CountDownLatch undeliverableReported = new CountDownLatch(1);
        Consumer<? super Throwable> originalErrorHandler = RxJavaPlugins.getErrorHandler();
        RxJavaPlugins.setErrorHandler(throwable -> {
            undeliverableErrors.add(throwable);
            undeliverableReported.countDown();
        });

        try {
            Refresh<PageKey> refreshRequest = new Refresh<>(null, 2, false);
            Disposable disposable = pagingSource.loadSingle(refreshRequest)
                    .subscribe(result -> { }, error -> { });

            assertTrue("Load never reached Tasks.await(), so nothing was interrupted",
                    loadInFlight.await(5, TimeUnit.SECONDS));

            // Interrupts the worker thread blocked in Tasks.await().
            disposable.dispose();

            assertFalse("Interrupting an in-flight load must not surface as an undeliverable "
                            + "RxJava exception: " + undeliverableErrors,
                    undeliverableReported.await(2, TimeUnit.SECONDS));
        } finally {
            RxJavaPlugins.setErrorHandler(originalErrorHandler);
        }

        verify(mMockQuery).get(Source.DEFAULT);
    }

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

    private void mockQueryFailure(Exception exception) {
        when(mMockQuery.get(Source.DEFAULT)).thenReturn(Tasks.forException(exception));
    }

    /**
     * Stubs the query with a task that never completes, so Tasks.await() blocks until the thread
     * is interrupted. {@code inFlight} is counted down once await() has started.
     */
    @SuppressWarnings("unchecked")
    private void mockQueryNeverCompletes(CountDownLatch inFlight) {
        Task<QuerySnapshot> neverCompletes = mock(Task.class);
        when(neverCompletes.isComplete()).thenReturn(false);
        when(neverCompletes.addOnSuccessListener(any(Executor.class), any(OnSuccessListener.class)))
                .thenAnswer(invocation -> {
                    inFlight.countDown();
                    return neverCompletes;
                });
        when(neverCompletes.addOnFailureListener(any(Executor.class), any(OnFailureListener.class)))
                .thenReturn(neverCompletes);
        when(neverCompletes.addOnCanceledListener(any(Executor.class), any(OnCanceledListener.class)))
                .thenReturn(neverCompletes);

        when(mMockQuery.get(Source.DEFAULT)).thenReturn(neverCompletes);
    }
}
