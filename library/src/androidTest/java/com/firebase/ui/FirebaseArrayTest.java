package com.firebase.ui;

import android.test.AndroidTestCase;

import com.firebase.client.Firebase;
import com.firebase.ui.database.FirebaseArray;

import junit.framework.AssertionFailedError;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

public class FirebaseArrayTest extends AndroidTestCase {
    private Firebase mRef;
    private FirebaseArray mArray;

    @Before
    public void setUp() throws Exception {
        Firebase.setAndroidContext(this.getContext());
        mRef = new Firebase("https://firebaseui-tests.firebaseio-demo.com/firebasearray");
        mArray = new FirebaseArray(mRef);
        mRef.removeValue();
        runAndWaitUntil(mArray, mRef, new Runnable() {
            public void run() {
                for (int i = 1; i <= 3; i++) {
                    mRef.push().setValue(i, i);
                }
            }
        }, new Callable<Boolean>() {
            public Boolean call() throws Exception {
                return mArray.getCount() == 3;
            }
        });
    }

    @After
    public void tearDown() throws Exception {
        mArray.cleanup();
        mRef.removeValue();
    }

    @Test
    public void testSize() throws Exception {
        assertEquals(3, mArray.getCount());
    }

    @Test
    public void testPushIncreasesSize() throws Exception {
        assertEquals(3, mArray.getCount());
        runAndWaitUntil(mArray, mRef, new Runnable() {
            public void run() {
                mRef.push().setValue(4);
            }
        }, new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                return mArray.getCount() == 4;
            }
        });
    }
    @Test
    public void testPushAppends() throws Exception {
        runAndWaitUntil(mArray, mRef, new Runnable() {
            public void run() {
                mRef.push().setValue(4, 4);
            }
        }, new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                return mArray.getItem(3).getValue(Integer.class).equals(4);
            }
        });
    }

    @Test
    public void testAddValueWithPriority() throws Exception {
        runAndWaitUntil(mArray, mRef, new Runnable() {
            public void run() {
                mRef.push().setValue(4, 0.5);
            }
        }, new Callable<Boolean>() {
            public Boolean call() throws Exception {
                return mArray.getItem(3).getValue(Integer.class).equals(3) && mArray.getItem(0).getValue(Integer.class).equals(4);
            }
        });
    }

    @Test
    public void testChangePriorities() throws Exception {
        runAndWaitUntil(mArray, mRef, new Runnable() {
            public void run() {
                mArray.getItem(2).getRef().setPriority(0.5);
            }
        }, new Callable<Boolean>() {
            public Boolean call() throws Exception {
                return isValuesEqual(mArray, new int[]{3, 1, 2});
            }
        });
    }

    private static boolean isValuesEqual(FirebaseArray array, int[] expected) {
        if (array.getCount() != expected.length) return false;
        for (int i=0; i < array.getCount(); i++) {
            if (!array.getItem(i).getValue(Integer.class).equals(expected[i])) {
                return false;
            }
        }
        return true;
    }

    private Integer getIntValue(FirebaseArray array, int index) {
        return array.getItem(index).getValue(Integer.class);
    }

    private static void print(FirebaseArray array) {
        for (int i=0; i < array.getCount(); i++) {
            System.out.println(i+": key="+array.getItem(i).getKey()+" value="+array.getItem(i).getValue());

        }
    }

    public static void runAndWaitUntil(final FirebaseArray array, Firebase ref, Runnable task, Callable<Boolean> done) throws InterruptedException {
        final java.util.concurrent.Semaphore semaphore = new java.util.concurrent.Semaphore(0);
        array.setOnChangedListener(new FirebaseArray.OnChangedListener() {
            public void onChanged(FirebaseArray.OnChangedListener.EventType type, int index, int oldIndex) {
                semaphore.release();
            }
        });
        task.run();
        boolean isDone = false;
        long startedAt = System.currentTimeMillis();
        while (!isDone && System.currentTimeMillis() - startedAt < 5000) {
            semaphore.tryAcquire(1, TimeUnit.SECONDS);
            try {
                isDone = done.call();
            } catch (Exception e) {
                e.printStackTrace();
                // and we're not done
            }
        }
        if (!isDone) {
            throw new AssertionFailedError();
        }
        array.setOnChangedListener(null);
    }
}