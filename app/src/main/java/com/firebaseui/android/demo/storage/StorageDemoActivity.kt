package com.firebaseui.android.demo.storage

import android.graphics.drawable.Drawable
import android.os.Bundle
import android.widget.ImageView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.google.firebase.storage.FirebaseStorage

class StorageDemoActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    StorageDemoScreen()
                }
            }
        }
    }
}

@Composable
fun StorageDemoScreen() {
    var gsUrl by remember { mutableStateOf("") }
    var stringStatus by remember { mutableStateOf("Not loaded") }
    var stringLoadKey by remember { mutableIntStateOf(0) }
    var refStatus by remember { mutableStateOf("Not loaded") }
    var refLoadKey by remember { mutableIntStateOf(0) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .systemBarsPadding()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Firebase Storage + Glide", style = MaterialTheme.typography.headlineSmall)
        Text(
            "Enter a gs:// URL and load it using either approach below.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        OutlinedTextField(
            value = gsUrl,
            onValueChange = { gsUrl = it },
            label = { Text("gs:// URL") },
            placeholder = { Text("gs://your-project.appspot.com/path/to/image.png") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        // Approach 1: gs:// string via StringLoader
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Via gs:// String", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Uses FirebaseImageLoader.StringLoader, registered in StorageGlideModule. " +
                        "Pass the gs:// URL string directly to Glide.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Button(onClick = {
                    stringStatus = "Loading..."
                    stringLoadKey++
                }) { Text("Load") }
                StatusText(stringStatus)
                AndroidView(
                    factory = { ImageView(it) },
                    update = { view ->
                        if (stringLoadKey > 0) {
                            GlideApp.with(view)
                                .load(gsUrl)
                                .listener(glideListener { success, error ->
                                    stringStatus = if (success) "Loaded" else "Error: $error"
                                })
                                .into(view)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                )
            }
        }

        // Approach 2: StorageReference
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Via StorageReference", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Converts the gs:// URL to a StorageReference first, then passes it to Glide. " +
                        "Handled by FirebaseImageLoader.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Button(onClick = {
                    refStatus = "Loading..."
                    refLoadKey++
                }) { Text("Load") }
                StatusText(refStatus)
                AndroidView(
                    factory = { ImageView(it) },
                    update = { view ->
                        if (refLoadKey > 0) {
                            runCatching {
                                FirebaseStorage.getInstance().getReferenceFromUrl(gsUrl)
                            }.onSuccess { ref ->
                                GlideApp.with(view)
                                    .load(ref)
                                    .listener(glideListener { success, error ->
                                        refStatus = if (success) "Loaded" else "Error: $error"
                                    })
                                    .into(view)
                            }.onFailure { e ->
                                refStatus = "Invalid URL: ${e.message}"
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                )
            }
        }
    }
}

@Composable
private fun StatusText(status: String) {
    Text(
        text = "Status: $status",
        style = MaterialTheme.typography.bodySmall,
        color = if (status.startsWith("Error") || status.startsWith("Invalid"))
            MaterialTheme.colorScheme.error
        else
            MaterialTheme.colorScheme.onSurface
    )
}

private fun glideListener(onResult: (success: Boolean, error: String?) -> Unit) =
    object : RequestListener<Drawable> {
        override fun onLoadFailed(
            e: GlideException?,
            model: Any?,
            target: Target<Drawable>,
            isFirstResource: Boolean
        ): Boolean {
            onResult(false, e?.message ?: "Unknown error")
            return false
        }

        override fun onResourceReady(
            resource: Drawable,
            model: Any,
            target: Target<Drawable>,
            dataSource: DataSource,
            isFirstResource: Boolean
        ): Boolean {
            onResult(true, null)
            return false
        }
    }
