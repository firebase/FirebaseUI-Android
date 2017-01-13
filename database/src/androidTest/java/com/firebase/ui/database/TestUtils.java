package com.firebase.ui.database;

import android.content.Context;

import com.firebase.ui.database.utils.Bean;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.Query;

import junit.framework.AssertionFailedError;

import java.util.concurrent.Callable;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

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

    public static void setJoinResolver(FirebaseIndexArray array, final DatabaseReference ref) {
        array.setJoinResolver(new JoinResolver() {
            @Override
            public Query onJoin(DataSnapshot keySnapshot, String previousChildKey) {
                return ref.child(keySnapshot.getKey());
            }

            @Override
            public Query onDisjoin(DataSnapshot keySnapshot) {
                return ref.child(keySnapshot.getKey());
            }

            @Override
            public void onJoinFailed(int index, DataSnapshot snapshot) {
                throw new IllegalStateException(index + ": " + snapshot);
            }
        });
    }

    public static void runAndWaitUntil(FirebaseArray array,
                                       Runnable task,
                                       Callable<Boolean> done) throws InterruptedException {
        final Semaphore semaphore = new Semaphore(0);
        array.setChangeEventListener(new FirebaseArray.ChangeEventListener() {
            @Override
            public void onChildChanged(EventType type, int index, int oldIndex) {
                semaphore.release();
            }

            @Override
            public void onDataChanged() {}

            @Override
            public void onDataChanged() {
            }

            @Override
            public void onCancelled(DatabaseError error) {
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
        if (!isDone) {
            throw new AssertionFailedError();
        }
    }

    public static boolean isValuesEqual(FirebaseArray array, int[] expected) {
        if (array.size() != expected.length) return false;
        for (int i = 0; i < array.size(); i++) {
            if (!array.get(i).getValue(Integer.class).equals(expected[i])) {
                return false;
            }
        }
        return true;
    }

    public static Bean getBean(FirebaseArray array, int index) {
        return array.get(index).getValue(Bean.class);
    }
}
