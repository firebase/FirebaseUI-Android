package com.firebase.ui.database;

import android.content.Context;

import com.firebase.ui.database.utils.Bean;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.database.DatabaseError;

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

    public static ChangeEventListener runAndWaitUntil(FirebaseArray array,
                                                      Runnable task,
                                                      Callable<Boolean> done) throws InterruptedException {
        final Semaphore semaphore = new Semaphore(0);
        ChangeEventListener listener = array.addChangeEventListener(new ChangeEventListener() {
            @Override
            public void onChildChanged(ChangeEventListener.EventType type, int index, int oldIndex) {
                semaphore.release();
            }

            @Override
            public void onDataChanged() {}

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

        return listener;
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
