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

package com.firebase.ui.firestore;

import android.annotation.SuppressLint;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;
import android.util.Log;

import com.firebase.ui.common.ChangeEventType;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.FirebaseFirestoreSettings;
import com.google.firebase.firestore.Query;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
@SuppressLint("LogConditional")
public class FirestoreArrayTest {

    /**
     * Simple document class containing only an integer field.
     */
    private static class IntegerDocument {

        public int field;

        public IntegerDocument() {}

        public IntegerDocument(int field) {
            this.field = field;
        }

    }

    /**
     * Simple listener that logs all events, to make test debugging easier.
     */
    private static class LoggingListener implements ChangeEventListener {

        private static final String TAG = "FirestoreTest_Listener";

        @Override
        public void onChildChanged(@NonNull ChangeEventType type,
                                   @NonNull DocumentSnapshot snapshot,
                                   int newIndex,
                                   int oldIndex) {
            Log.d(TAG, "onChildChanged: " + type + " at index " + newIndex);
        }

        @Override
        public void onDataChanged() {
            Log.d(TAG, "onDataChanged");
        }

        @Override
        public void onError(@NonNull FirebaseFirestoreException e) {
            Log.w(TAG, "onError", e);
        }
    }

    private static final String TAG = "FirestoreTest";
    private static final String FIREBASE_APP_NAME = "test-app";
    private static final int TIMEOUT = 30000;
    private static final int INITIAL_SIZE = 3;

    private CollectionReference mCollectionRef;
    private FirestoreArray<IntegerDocument> mArray;
    private ChangeEventListener mListener;

    @Before
    public void setUp() throws Exception {
        FirebaseApp app = getAppInstance(InstrumentationRegistry.getContext());

        // Configure Firestore and disable persistence
        FirebaseFirestore.getInstance(app)
                .setFirestoreSettings(new FirebaseFirestoreSettings.Builder()
                        .setPersistenceEnabled(false)
                        .build());

        // Get a fresh 'test' subcollection for each test
        mCollectionRef = FirebaseFirestore.getInstance(app).collection("firestorearray")
                .document().collection("test");

        Log.d(TAG, "Test Collection: " + getConsoleLink(mCollectionRef));

        // Query is the whole collection ordered by field
        mArray = new FirestoreArray<>(
                mCollectionRef.orderBy("field", Query.Direction.ASCENDING),
                new ClassSnapshotParser<>(IntegerDocument.class));

        // Add a listener to the array so that it's active
        mListener = mArray.addChangeEventListener(new LoggingListener());

        // Add some initial data
        runAndVerify(new Callable<Task<?>>() {
            @Override
            public Task<?> call() {
                List<Task> tasks = new ArrayList<>();
                for (int i = 0; i < INITIAL_SIZE; i++) {
                    tasks.add(mCollectionRef.document().set(new IntegerDocument(i)));
                }

                return Tasks.whenAll(tasks.toArray(new Task[tasks.size()]));
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
        if (mArray != null && mListener != null) {
            mArray.removeChangeEventListener(mListener);
        } else {
            Log.w(TAG, "mArray is null in tearDown");
        }
    }

    /**
     * Append a single document and confirm size increases by one.
     */
    @Test
    public void testPushIncreasesSize() throws Exception {
        runAndVerify(new Callable<Task<?>>() {
            @Override
            public Task<?> call() {
                return mCollectionRef.document().set(new IntegerDocument(4));
            }
        }, new Callable<Boolean>() {
            @Override
            public Boolean call() {
                return mArray.size() == (INITIAL_SIZE + 1);
            }
        });
    }

    /**
     * Append a single document to the query and confirm that size increases by one and that the
     * document is added to the end of the array.
     */
    @Test
    public void testAddToEnd() throws Exception {
        final int value = 4;

        runAndVerify(new Callable<Task<?>>() {
            @Override
            public Task<?> call() {
                return mCollectionRef.document().set(new IntegerDocument(value));
            }
        }, new Callable<Boolean>() {
            @Override
            public Boolean call() {
                if (mArray.size() == (INITIAL_SIZE + 1)) {
                    return mArray.get(mArray.size() - 1).field == value;
                }

                return false;
            }
        });
    }

    /**
     * Append a single document to the query and confirm that size increases by one and that the
     * document is added to the beginning of the array.
     */
    @Test
    public void testAddToBeginning() throws Exception {
        final int value = -1;

        runAndVerify(new Callable<Task<?>>() {
            @Override
            public Task<?> call() {
                return mCollectionRef.document().set(new IntegerDocument(value));
            }
        }, new Callable<Boolean>() {
            @Override
            public Boolean call() {
                if (mArray.size() == (INITIAL_SIZE + 1)) {
                    return mArray.get(0).field == value;
                }

                return false;
            }
        });
    }

    /**
     * Runs some setup action, waits until it is complete, and then waits for a verification
     * condition to be met. Times out after {@link #TIMEOUT}.
     */
    @SuppressWarnings("unchecked")
    private void runAndVerify(Callable<Task<?>> setup, Callable<Boolean> verify) throws Exception {
        final Semaphore semaphore = new Semaphore(0);
        long startTime = System.currentTimeMillis();

        // Run the setup action and release the semaphore when it is complete
        Task task = setup.call();
        task.addOnCompleteListener(new OnCompleteListener() {
            @Override
            public void onComplete(@NonNull Task task) {
                semaphore.release();
            }
        });

        // Wait for the verification condition to be met, or time out
        boolean isDone = false;
        while (!isDone && (System.currentTimeMillis() - startTime < TIMEOUT)) {
            boolean acquired = semaphore.tryAcquire(1, TimeUnit.SECONDS);
            if (acquired) {
                try {
                    isDone = verify.call();
                } catch (Exception e) {
                    Log.w(TAG, "error in verification callable", e);
                }
            }
        }

        assertTrue("Timed out waiting for setup callable.", task.isComplete());
        assertTrue("Timed out waiting for expected results.", isDone);
    }

    private FirebaseApp getAppInstance(Context context) {
        try {
            return FirebaseApp.getInstance(FIREBASE_APP_NAME);
        } catch (IllegalStateException e) {
            return initializeApp(context);
        }
    }

    private FirebaseApp initializeApp(Context context) {
        return FirebaseApp.initializeApp(context, new FirebaseOptions.Builder()
                .setApplicationId("firebaseuitests-app123")
                .setApiKey("AIzaSyCIA_uf-5Y4G83vlZmjMmCM_wkX62iWXf0")
                .setProjectId("firebaseuitests")
                .build(), FIREBASE_APP_NAME);
    }

    private String getConsoleLink(CollectionReference reference) {
        String base = "https://console.firebase.google.com/project/firebaseuitests/database/firestore/data/";
        return base + reference.getPath();
    }
}
