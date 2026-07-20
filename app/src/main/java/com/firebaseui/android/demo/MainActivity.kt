package com.firebaseui.android.demo

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.firebase.ui.auth.FirebaseAuthUI
import com.firebase.ui.auth.util.EmailLinkConstants
import com.firebaseui.android.demo.auth.AuthChooserActivity
import com.firebaseui.android.demo.auth.HighLevelApiDemoActivity
import com.firebaseui.android.demo.database.DatabaseDemoActivity
import com.firebaseui.android.demo.firestore.FirestoreDemoActivity
import com.firebaseui.android.demo.storage.StorageDemoActivity
import com.google.firebase.FirebaseApp

class MainActivity : ComponentActivity() {
    companion object {
        private const val USE_AUTH_EMULATOR = false
        private const val AUTH_EMULATOR_HOST = "10.0.2.2"
        private const val AUTH_EMULATOR_PORT = 9099
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        FirebaseApp.initializeApp(applicationContext)
        val authUI = FirebaseAuthUI.getInstance()

        if (USE_AUTH_EMULATOR) {
            authUI.auth.useEmulator(AUTH_EMULATOR_HOST, AUTH_EMULATOR_PORT)
        }

        var pendingEmailLink = intent.getStringExtra(EmailLinkConstants.EXTRA_EMAIL_LINK)
        if (pendingEmailLink.isNullOrEmpty() && authUI.canHandleIntent(intent)) {
            pendingEmailLink = intent.data?.toString()
        }

        Log.d("MainActivity", "Pending email link: $pendingEmailLink")

        fun launchHighLevelDemo() {
            val demoIntent = Intent(this, HighLevelApiDemoActivity::class.java).apply {
                pendingEmailLink?.let { link ->
                    putExtra(EmailLinkConstants.EXTRA_EMAIL_LINK, link)
                    pendingEmailLink = null
                }
            }
            startActivity(demoIntent)
        }

        if (savedInstanceState == null && !pendingEmailLink.isNullOrEmpty()) {
            launchHighLevelDemo()
            finish()
            return
        }

        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    ChooserScreen(
                        onAuthClick = {
                            startActivity(Intent(this, AuthChooserActivity::class.java))
                        },
                        onDatabaseClick = {
                            startActivity(Intent(this, DatabaseDemoActivity::class.java))
                        },
                        onFirestoreClick = {
                            startActivity(Intent(this, FirestoreDemoActivity::class.java))
                        },
                        onStorageClick = {
                            startActivity(Intent(this, StorageDemoActivity::class.java))
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun ChooserScreen(
    onAuthClick: () -> Unit,
    onDatabaseClick: () -> Unit,
    onFirestoreClick: () -> Unit,
    onStorageClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .systemBarsPadding()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        Text("FirebaseUI Android", style = MaterialTheme.typography.headlineLarge)
        Text(
            "Choose a module to explore its demos",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Card(modifier = Modifier.fillMaxWidth(), onClick = onAuthClick) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Auth",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "High-Level API, Low-Level API, Custom Slots & Theming, Credential Linking",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Card(modifier = Modifier.fillMaxWidth(), onClick = onDatabaseClick) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Database",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Paginated list with FirebaseRecyclerPagingAdapter and orderByChild",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Card(modifier = Modifier.fillMaxWidth(), onClick = onFirestoreClick) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Firestore",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Paginated list with FirestorePagingAdapter and orderBy",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Card(modifier = Modifier.fillMaxWidth(), onClick = onStorageClick) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Storage",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Loading images from Firebase Storage with Glide",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
    }
}
