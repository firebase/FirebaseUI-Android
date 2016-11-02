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
@SmallTest
public class FirebaseArrayTest extends InstrumentationTestCase {
    private DatabaseReference mRef;
    private FirebaseArray mArray;

    @Before
    public void setUp() throws Exception {
        FirebaseApp app = getAppInstance(getInstrumentation().getContext());
        mRef = FirebaseDatabase.getInstance(app).getReference().child("firebasearray");
        mArray = new FirebaseArray(mRef);
        mRef.removeValue();
        runAndWaitUntil(mArray, new Runnable() {
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
        runAndWaitUntil(mArray, new Runnable() {
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
        runAndWaitUntil(mArray, new Runnable() {
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
        runAndWaitUntil(mArray, new Runnable() {
            public void run() {
                mRef.push().setValue(4, 0.5);
            }
        }, new Callable<Boolean>() {
            public Boolean call() throws Exception {
                return mArray.getItem(3).getValue(Integer.class).equals(3)
                        && mArray.getItem(0).getValue(Integer.class).equals(4);
            }
        });
    }

    @Test
    public void testChangePriorities() throws Exception {
        runAndWaitUntil(mArray, new Runnable() {
            public void run() {
                mArray.getItem(2).getRef().setPriority(0.5);
            }
        }, new Callable<Boolean>() {
            public Boolean call() throws Exception {
                return isValuesEqual(mArray, new int[]{3, 1, 2});
            }
        });
    }
}
