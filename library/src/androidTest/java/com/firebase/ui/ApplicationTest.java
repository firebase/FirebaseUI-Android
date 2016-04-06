package com.firebase.ui;

import android.app.Application;
import android.content.Context;
import android.test.ApplicationTestCase;

import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;

/**
 * <a href="http://d.android.com/tools/testing/testing_android.html">Testing Fundamentals</a>
 */
public class ApplicationTest extends ApplicationTestCase<Application> {

    private static final String APP_NAME = "firebaseui-tests";

    public ApplicationTest() {
        super(Application.class);
    }

    public static FirebaseApp getAppInstance(Context context) {
        try {
            return FirebaseApp.getInstance(APP_NAME);
        } catch (IllegalStateException e) {
            return initializeApp(context);
        }
    }

    public static FirebaseApp initializeApp(Context context) {
        return FirebaseApp.initializeApp(context, new FirebaseOptions.Builder()
                .setApplicationId("foo-bar")
                .setDatabaseUrl("https://firebaseui-tests.firebaseio-demo.com/")
                .build(), APP_NAME);
    }
}
