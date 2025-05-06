package com.firebase.uidemo.auth.compose

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.firebase.uidemo.R
import com.firebase.uidemo.auth.AnonymousUpgradeActivity
import com.firebase.uidemo.auth.AuthUiActivity
import com.firebase.uidemo.database.firestore.FirestoreChatActivity
import com.firebase.uidemo.database.firestore.FirestorePagingActivity
import com.firebase.uidemo.database.realtime.FirebaseDbPagingActivity
import com.firebase.uidemo.database.realtime.RealtimeDbChatActivity
import com.firebase.uidemo.storage.ImageActivity
import com.firebase.uidemo.ui.theme.FirebaseUIDemoTheme

data class DemoActivityItem(val titleRes: Int, val descRes: Int, val activityClass: Class<out Activity>)

val demoActivities = listOf(
    DemoActivityItem(R.string.title_auth_activity, R.string.desc_auth, AuthUiActivity::class.java),
    DemoActivityItem(R.string.auth_compose_title, R.string.desc_auth, AuthComposeActivity::class.java),
    DemoActivityItem(R.string.title_anonymous_upgrade, R.string.desc_anonymous_upgrade, AnonymousUpgradeActivity::class.java),
    DemoActivityItem(R.string.title_firestore_activity, R.string.desc_firestore, FirestoreChatActivity::class.java),
    DemoActivityItem(R.string.title_firestore_paging_activity, R.string.desc_firestore_paging, FirestorePagingActivity::class.java),
    DemoActivityItem(R.string.title_realtime_database_activity, R.string.desc_realtime_database, RealtimeDbChatActivity::class.java),
    DemoActivityItem(R.string.title_realtime_database_paging_activity, R.string.desc_realtime_database_paging, FirebaseDbPagingActivity::class.java),
    DemoActivityItem(R.string.title_storage_activity, R.string.desc_storage, ImageActivity::class.java),
)

@Composable
fun ChooserScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    Surface(
        modifier = modifier
            .fillMaxSize()
            .padding(top = 40.dp, start = 16.dp, end = 16.dp, bottom = 16.dp)
    ) {
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(demoActivities) { item ->
                DemoActivityCard(item) {
                    context.startActivity(Intent(context, item.activityClass))
                }
            }
        }
    }
}

@Composable
fun DemoActivityCard(item: DemoActivityItem, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Text(
                text = stringResource(id = item.titleRes),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(id = item.descRes),
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ChooserScreenPreview() {
    FirebaseUIDemoTheme {
        ChooserScreen()
    }
} 