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

import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import com.google.firebase.FirebaseApp;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.Callable;

import static com.firebase.ui.database.TestUtils.getAppInstance;
import static com.firebase.ui.database.TestUtils.isValuesEqual;
import static com.firebase.ui.database.TestUtils.runAndWaitUntil;

@RunWith(AndroidJUnit4.class)
public class FirebaseArrayTest {
    private static final int INITIAL_SIZE = 3;
    private DatabaseReference mRef;
    private FirebaseArray<Integer> mArray;
    private ChangeEventListener mListener;

    @Before
    public void setUp() throws Exception {
        FirebaseApp app = getAppInstance(InstrumentationRegistry.getContext());
        mRef = FirebaseDatabase.getInstance(app).getReference().child("firebasearray");
        mArray = new FirebaseArray<>(mRef, new ClassSnapshotParser<>(Integer.class));
        mRef.removeValue();
        mListener = runAndWaitUntil(mArray, new Runnable() {
            @Override
            public void run() {
                for (int i = 1; i <= INITIAL_SIZE; i++) {
                    mRef.push().setValue(i, i);
                }
            }
        }, new Callable<Boolean>() {
            @Override
            public Boolean call() {
                return mArray.size() == INITIAL_SIZE;
            }
        });
    }

    @After
    public void tearDown() {
        mArray.removeChangeEventListener(mListener);
        mRef.getRoot().removeValue();
    }

    @Test
    public void testPushIncreasesSize() throws Exception {
        runAndWaitUntil(mArray, new Runnable() {
            @Override
            public void run() {
                mRef.push().setValue(4);
            }
        }, new Callable<Boolean>() {
            @Override
            public Boolean call() {
                return mArray.size() == 4;
            }
        });
    }

    @Test
    public void testPushAppends() throws Exception {
        runAndWaitUntil(mArray, new Runnable() {
            @Override
            public void run() {
                mRef.push().setValue(4, 4);
            }
        }, new Callable<Boolean>() {
            @Override
            public Boolean call() {
                return mArray.get(3).equals(4);
            }
        });
    }

    @Test
    public void testAddValueWithPriority() throws Exception {
        runAndWaitUntil(mArray, new Runnable() {
            @Override
            public void run() {
                mRef.push().setValue(4, 0.5);
            }
        }, new Callable<Boolean>() {
            @Override
            public Boolean call() {
                return mArray.get(3).equals(3) && mArray.get(0).equals(4);
            }
        });
    }

    @Test
    public void testChangePriorityBackToFront() throws Exception {
        runAndWaitUntil(mArray, new Runnable() {
            @Override
            public void run() {
                mArray.getSnapshot(2).getRef().setPriority(0.5);
            }
        }, new Callable<Boolean>() {
            @Override
            public Boolean call() {
                return isValuesEqual(mArray, new int[]{3, 1, 2});
            }
        });
    }

    @Test
    public void testChangePriorityFrontToBack() throws Exception {
        runAndWaitUntil(mArray, new Runnable() {
            @Override
            public void run() {
                mArray.getSnapshot(0).getRef().setPriority(4);
            }
        }, new Callable<Boolean>() {
            @Override
            public Boolean call() {
                return isValuesEqual(mArray, new int[]{2, 3, 1});
            }
        });
    }
}
