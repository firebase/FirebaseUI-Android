package com.firebaseui.android.demo.firestore

import android.os.Bundle
import android.view.ViewGroup
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.paging.LoadState
import androidx.paging.PagingConfig
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.firebase.ui.firestore.paging.FirestorePagingAdapter
import com.firebase.ui.firestore.paging.FirestorePagingOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class FirestoreDemoActivity : ComponentActivity() {

    private lateinit var adapter: ScoreAdapter
    private var currentPage by mutableIntStateOf(1)
    private var prevAppendWasLoading = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val collection = FirebaseFirestore.getInstance().collection("firestore_demo")

        // Query must only contain where()/orderBy() — the paging library adds limit().
        val query: Query = collection.orderBy("score")

        val options = FirestorePagingOptions.Builder<ScoreItem>()
            .setLifecycleOwner(this)
            .setQuery(query, PagingConfig(pageSize = 10), ScoreItem::class.java)
            .build()

        adapter = ScoreAdapter(options)

        adapter.addLoadStateListener { states ->
            if (states.refresh is LoadState.Loading) {
                currentPage = 1
                prevAppendWasLoading = false
                return@addLoadStateListener
            }
            val appendLoading = states.append is LoadState.Loading
            if (prevAppendWasLoading && !appendLoading) {
                currentPage++
            }
            prevAppendWasLoading = appendLoading
        }

        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    FirestoreDemoScreen(
                        adapter = adapter,
                        currentPage = currentPage,
                        onSeedData = { signInThenSeed(collection) },
                        onRefresh = { adapter.refresh() }
                    )
                }
            }
        }
    }

    private fun signInThenSeed(collection: CollectionReference) {
        val auth = FirebaseAuth.getInstance()
        val signIn = if (auth.currentUser != null) {
            com.google.android.gms.tasks.Tasks.forResult(null)
        } else {
            auth.signInAnonymously()
        }
        signIn.addOnSuccessListener { seedData(collection) }
    }

    private fun seedData(collection: CollectionReference) {
        for (i in 1..50) {
            collection.add(ScoreItem("Item $i", (1..100).random()))
        }
    }
}

data class ScoreItem(var name: String = "", var score: Int = 0)

class ScoreViewHolder(parent: ViewGroup) : RecyclerView.ViewHolder(
    TextView(parent.context).apply {
        layoutParams = RecyclerView.LayoutParams(
            RecyclerView.LayoutParams.MATCH_PARENT,
            RecyclerView.LayoutParams.WRAP_CONTENT
        )
        setPadding(48, 24, 48, 24)
        textSize = 16f
    }
)

class ScoreAdapter(options: FirestorePagingOptions<ScoreItem>) :
    FirestorePagingAdapter<ScoreItem, ScoreViewHolder>(options) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = ScoreViewHolder(parent)

    override fun onBindViewHolder(holder: ScoreViewHolder, position: Int, model: ScoreItem) {
        (holder.itemView as TextView).text = "${model.name}  —  score: ${model.score}"
    }
}

@Composable
fun FirestoreDemoScreen(
    adapter: ScoreAdapter,
    currentPage: Int,
    onSeedData: () -> Unit,
    onRefresh: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Firebase Firestore Paging", style = MaterialTheme.typography.headlineSmall)
        Text(
            "Paginated list using FirestorePagingAdapter with orderBy(\"score\").",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onSeedData) { Text("Authenticate & Seed Data") }
            OutlinedButton(onClick = onRefresh) { Text("Refresh") }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Page: $currentPage",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        AndroidView(
            factory = { context ->
                RecyclerView(context).apply {
                    layoutManager = LinearLayoutManager(context)
                    addItemDecoration(
                        DividerItemDecoration(context, DividerItemDecoration.VERTICAL)
                    )
                    setAdapter(adapter)
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        )
    }
}
