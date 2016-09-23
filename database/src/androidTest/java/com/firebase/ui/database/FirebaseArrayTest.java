/*
 * Copyright 2016 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.firebase.ui.database;

import android.support.test.runner.AndroidJUnit4;
import android.test.InstrumentationTestCase;
import android.test.suitebuilder.annotation.SmallTest;

import com.google.firebase.FirebaseApp;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;

import junit.framework.AssertionFailedError;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class FirebaseArrayTest extends InstrumentationTestCase {

    private static final int TIMEOUT = 5000;

    private DatabaseReference mRef;
    private FirebaseArray mArray;

    @Before
    public void setUp() throws Exception {
        FirebaseApp app = ApplicationTest.getAppInstance(getInstrumentation().getContext());
        mRef = FirebaseDatabase.getInstance(app).getReference().child("firebasearray");
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
        if (mRef != null) {
            mRef.getRoot().removeValue();
        }

        if (mArray != null) {
            mArray.cleanup();
        }
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

    public static void runAndWaitUntil(final FirebaseArray array, Query ref, Runnable task, Callable<Boolean> done) throws InterruptedException {
        final java.util.concurrent.Semaphore semaphore = new java.util.concurrent.Semaphore(0);
        array.setOnChangedListener(new FirebaseArray.OnChangedListener() {
            public void onChanged(EventType type, int index, int oldIndex) {
                semaphore.release();
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                throw new IllegalStateException(databaseError.toException());
            }
        });
        task.run();
        boolean isDone = false;
        long startedAt = System.currentTimeMillis();
        while (!isDone && System.currentTimeMillis() - startedAt < TIMEOUT) {
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
