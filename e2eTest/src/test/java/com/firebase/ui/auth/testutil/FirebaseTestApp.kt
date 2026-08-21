package com.firebase.ui.auth.testutil

import android.content.Context
import com.firebase.ui.auth.FirebaseAuthUI
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions

/**
 * Returns the "[DEFAULT]" FirebaseApp used by e2e tests, pointed at the local Auth
 * emulator, initializing it once per JVM instead of per test.
 *
 * Every test class used to delete and re-initialize this app in its own `@Before`.
 * Robolectric shares statics across test classes within a run, so that churn raced
 * with other classes doing the same thing; newer firebase-auth releases surface the
 * loser of that race as "FirebaseApp was deleted" from useEmulator(). Per-test
 * isolation is already handled by [EmulatorAuthApi.clearEmulatorData], so the app
 * itself doesn't need to be recreated for every test.
 */
fun ensureTestFirebaseApp(context: Context): FirebaseApp {
    FirebaseApp.getApps(context).firstOrNull { it.name == FirebaseApp.DEFAULT_APP_NAME }?.let {
        return it
    }

    val app = FirebaseApp.initializeApp(
        context,
        FirebaseOptions.Builder()
            .setApiKey("fake-api-key")
            .setApplicationId("fake-app-id")
            .setProjectId("fake-project-id")
            .build()
    )
    FirebaseAuthUI.getInstance().auth.useEmulator("127.0.0.1", 9099)
    return app
}
