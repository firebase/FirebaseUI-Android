package com.firebase.ui.firestore.paging;

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
import java.util.List;

import androidx.paging.PagingSource;
import androidx.paging.PagingSource.LoadParams.Append;
import androidx.paging.PagingSource.LoadParams.Refresh;
import androidx.paging.PagingSource.LoadResult.Page;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(AndroidJUnit4.class)
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
}
