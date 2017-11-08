package com.firebase.ui.database;

import android.content.Context;
import android.support.annotation.NonNull;

import com.firebase.ui.common.ChangeEventType;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;

import java.util.concurrent.Callable;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertTrue;

public class TestUtils {
    private static final String APP_NAME = "firebaseui-tests";
    private static final int TIMEOUT = 10000;

    public static FirebaseApp getAppInstance(Context context) {
        try {
            return FirebaseApp.getInstance(APP_NAME);
        } catch (IllegalStateException e) {
            return initializeApp(context);
        }
    }

    private static FirebaseApp initializeApp(Context context) {
        return FirebaseApp.initializeApp(context, new FirebaseOptions.Builder()
                .setApplicationId("fir-ui-tests")
                .setDatabaseUrl("https://fir-ui-tests.firebaseio.com/")
                .build(), APP_NAME);
    }

    public static ChangeEventListener runAndWaitUntil(
            ObservableSnapshotArray<?> array,
            Runnable task,
            Callable<Boolean> done) throws InterruptedException {

        final Semaphore semaphore = new Semaphore(0);
        ChangeEventListener listener = array.addChangeEventListener(new ChangeEventListener() {
            @Override
            public void onChildChanged(@NonNull ChangeEventType type,
                                       @NonNull DataSnapshot snapshot,
                                       int newIndex,
                                       int oldIndex) {
                semaphore.release();
            }

            @Override
            public void onDataChanged() {
            }

            @Override
            public void onError(@NonNull DatabaseError error) {
                throw new IllegalStateException(error.toException());
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
        assertTrue("Timed out waiting for expected results on FirebaseArray", isDone);

        return listener;
    }

    public static boolean isValuesEqual(ObservableSnapshotArray<Integer> array, int[] expected) {
        if (array.size() != expected.length) return false;
        for (Integer i : array) {
            if (!i.equals(expected[i])) {
                return false;
            }
        }
        return true;
    }

    public static void pushValue(DatabaseReference keyRef,
                                 DatabaseReference ref,
                                 Object value,
                                 Object priority) {
        String key = keyRef.push().getKey();

        if (priority != null) {
            keyRef.child(key).setValue(true, priority);
            ref.child(key).setValue(value, priority);
        } else {
            keyRef.child(key).setValue(true);
            ref.child(key).setValue(value);
        }
    }
}
