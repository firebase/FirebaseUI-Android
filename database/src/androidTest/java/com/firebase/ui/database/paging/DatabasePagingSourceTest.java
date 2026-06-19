package com.firebase.ui.database.paging;

import androidx.paging.PagingSource;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.firebase.ui.database.Bean;
import com.firebase.ui.database.TestUtils;
import com.google.firebase.FirebaseApp;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import androidx.annotation.NonNull;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
public class DatabasePagingSourceTest {
    private static final int PAGE_SIZE = 3;
    private static final int TOTAL_ITEMS = 6;

    private DatabaseReference mRef;

    @Before
    public void setUp() throws InterruptedException {
        FirebaseApp app = TestUtils.getAppInstance(ApplicationProvider.getApplicationContext());
        mRef = FirebaseDatabase.getInstance(app).getReference().child("paging_test");

        mRef.removeValue();
        for (int i = 1; i <= TOTAL_ITEMS; i++) {
            mRef.push().setValue(new Bean(i));
        }

        CountDownLatch latch = new CountDownLatch(1);
        mRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.getChildrenCount() >= TOTAL_ITEMS) {
                    mRef.removeEventListener(this);
                    latch.countDown();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
        assertTrue("Timed out seeding test data", latch.await(30, TimeUnit.SECONDS));
    }

    @After
    public void tearDown() {
        mRef.removeValue();
    }

    @Test
    public void testOrderByChild_noDuplicatesAcrossPages() {
        DatabasePagingSource source = new DatabasePagingSource(mRef.orderByChild("number"));

        PagingSource.LoadResult<DatabasePagingKey, DataSnapshot> result1 =
                source.loadSingle(new PagingSource.LoadParams.Refresh<>(null, PAGE_SIZE, false))
                        .timeout(30, TimeUnit.SECONDS)
                        .blockingGet();

        assertTrue(result1 instanceof PagingSource.LoadResult.Page);
        PagingSource.LoadResult.Page<DatabasePagingKey, DataSnapshot> page1 =
                (PagingSource.LoadResult.Page<DatabasePagingKey, DataSnapshot>) result1;
        assertEquals(PAGE_SIZE, page1.getData().size());

        DatabasePagingKey nextKey = page1.getNextKey();

        PagingSource.LoadResult<DatabasePagingKey, DataSnapshot> result2 =
                source.loadSingle(new PagingSource.LoadParams.Append<>(nextKey, PAGE_SIZE, false))
                        .timeout(30, TimeUnit.SECONDS)
                        .blockingGet();

        assertTrue(result2 instanceof PagingSource.LoadResult.Page);
        PagingSource.LoadResult.Page<DatabasePagingKey, DataSnapshot> page2 =
                (PagingSource.LoadResult.Page<DatabasePagingKey, DataSnapshot>) result2;

        Set<String> allKeys = new HashSet<>();
        for (DataSnapshot snapshot : page1.getData()) {
            allKeys.add(snapshot.getKey());
        }
        for (DataSnapshot snapshot : page2.getData()) {
            assertTrue("Duplicate key across pages: " + snapshot.getKey(),
                    allKeys.add(snapshot.getKey()));
        }
        assertEquals(TOTAL_ITEMS, allKeys.size());
    }
}
