package com.firebase.ui.auth.testutil

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity

/**
 * Test activity for handling email link deep links in tests.
 *
 * This activity is used in tests to properly simulate the Android deep link flow
 * where the app is launched with an ACTION_VIEW intent containing the email link.
 *
 * The activity simply extracts the email link from the intent and makes it available
 * via the [emailLinkFromIntent] property for verification in tests.
 */
class EmailLinkTestActivity : ComponentActivity() {

    /**
     * The email link extracted from the deep link intent.
     * This will be populated when the activity is launched with an ACTION_VIEW intent.
     */
    var emailLinkFromIntent: String? = null
        private set

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent?.action == Intent.ACTION_VIEW) {
            emailLinkFromIntent = intent.data?.toString()
        }
    }
}
