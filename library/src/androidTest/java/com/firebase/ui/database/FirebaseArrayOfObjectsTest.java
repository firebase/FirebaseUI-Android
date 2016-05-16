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

import android.test.AndroidTestCase;

import com.firebase.ui.ApplicationTest;
import com.firebase.ui.database.FirebaseArray;
import com.google.firebase.FirebaseApp;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;

import junit.framework.AssertionFailedError;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

public class FirebaseArrayOfObjectsTest extends AndroidTestCase {
    public static class Bean {
        int number;
        String text;
        boolean bool;

        public Bean() {
            // necessary for Jackson
        }

        public Bean(int number, String text, boolean bool) {
            this.number = number;
            this.text = text;
            this.bool = bool;
        }
        public Bean(int index) {
            this(index, "Text "+index, index % 2 == 0);
        }

        public int getNumber() {
            return number;
        }

        public String getText() {
            return text;
        }

        public boolean isBool() {
            return bool;
        }
    }

    private DatabaseReference mRef;
    private FirebaseArray mArray;

    @Before
    public void setUp() throws Exception {
        FirebaseApp app = ApplicationTest.getAppInstance(getContext());
        mRef = FirebaseDatabase.getInstance(app).getReference()
                .child("firebasearray").child("objects");
        mArray = new FirebaseArray(mRef);
        mRef.removeValue();
        runAndWaitUntil(mArray, mRef, new Runnable() {
                    @Override
                    public void run() {
                        for (int i = 1; i <= 3; i++) {
                            mRef.push().setValue(new Bean(i, "Text " + i, i % 2 == 0 ? true : false), i);
                        }
                    }
                }, new Callable<Boolean>() {
                    @Override
                    public Boolean call() throws Exception {
                        return mArray.getCount() == 3;
                    }
                }
        );
    }

    @After
    public void tearDown() throws Exception {
        mRef.removeValue();
        mArray.cleanup();
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
                mRef.push().setValue(new Bean(4));
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
                mRef.push().setValue(new Bean(4), 4);
            }
        }, new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                return mArray.getItem(3).getValue(Bean.class).getNumber() == 4;
            }
        });
    }

    @Test
    public void testAddValueWithPriority() throws Exception {
        runAndWaitUntil(mArray, mRef, new Runnable() {
            public void run() {
                mRef.push().setValue(new Bean(4), 0.5);
            }
        }, new Callable<Boolean>() {
            public Boolean call() throws Exception {
                return mArray.getItem(3).getValue(Bean.class).getNumber() == 3 && mArray.getItem(0).getValue(Bean.class).getNumber() == 4;
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
                return getBean(mArray, 0).getNumber() == 3 && getBean(mArray, 1).getNumber() == 1 && getBean(mArray, 2).getNumber() == 2;
                //return isValuesEqual(mArray, new int[]{3, 1, 2});
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

    private Bean getBean(FirebaseArray array, int index) {
        return array.getItem(index).getValue(Bean.class);
    }

    public static void runAndWaitUntil(final FirebaseArray array, Query ref, Runnable task, Callable<Boolean> done) throws InterruptedException {
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
    }}
