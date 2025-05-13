/*
 * Copyright 2016 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.firebase.uidemo

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.util.ExtraConstants
import com.firebase.uidemo.auth.AnonymousUpgradeActivity
import com.firebase.uidemo.auth.AuthUiActivity
import com.firebase.uidemo.auth.compose.AuthComposeActivity
import com.firebase.uidemo.auth.compose.ChooserScreen
import com.firebase.uidemo.database.firestore.FirestoreChatActivity
import com.firebase.uidemo.database.firestore.FirestorePagingActivity
import com.firebase.uidemo.database.realtime.FirebaseDbPagingActivity
import com.firebase.uidemo.database.realtime.RealtimeDbChatActivity
import com.firebase.uidemo.storage.ImageActivity

class ChooserActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Deep-link handling identical to the original Java implementation
        if (AuthUI.canHandleIntent(intent)) {
            val authIntent = Intent(this, AuthUiActivity::class.java).apply {
                putExtra(ExtraConstants.EMAIL_LINK_SIGN_IN, intent.data.toString())
            }
            startActivity(authIntent)
            finish()
            return
        }

        setContent { ChooserScreen() }
    }


    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun ChooserScreen() {
        val items = remember { activityItems }

        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text(stringResource(R.string.app_name)) }
                )
            }
        ) { padding ->
            LazyColumn(
                contentPadding = padding,
                modifier = Modifier.fillMaxSize()
            ) {
                items(items) { entry -> ActivityRow(entry) }
            }
        }
    }

    @Composable
    private fun ActivityRow(entry: ActivityEntry) {
        val ctx = LocalContext.current
        Card(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .clickable { ctx.startActivity(Intent(ctx, entry.clazz)) }
        ) {
            Column(Modifier.padding(16.dp)) {
                Text(
                    text = stringResource(entry.titleRes),
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = stringResource(entry.descRes),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }


    private data class ActivityEntry(
        val clazz: Class<*>,
        @StringRes val titleRes: Int,
        @StringRes val descRes: Int
    )

    private val activityItems = listOf(
        ActivityEntry(AuthUiActivity::class.java,
            R.string.title_auth_activity, R.string.desc_auth),
        ActivityEntry(AuthComposeActivity::class.java,
            R.string.auth_compose_title, R.string.desc_auth),
        ActivityEntry(AnonymousUpgradeActivity::class.java,
            R.string.title_anonymous_upgrade, R.string.desc_anonymous_upgrade),
        ActivityEntry(FirestoreChatActivity::class.java,
            R.string.title_firestore_activity, R.string.desc_firestore),
        ActivityEntry(FirestorePagingActivity::class.java,
            R.string.title_firestore_paging_activity, R.string.desc_firestore_paging),
        ActivityEntry(RealtimeDbChatActivity::class.java,
            R.string.title_realtime_database_activity, R.string.desc_realtime_database),
        ActivityEntry(FirebaseDbPagingActivity::class.java,
            R.string.title_realtime_database_paging_activity, R.string.desc_realtime_database_paging),
        ActivityEntry(ImageActivity::class.java,
            R.string.title_storage_activity, R.string.desc_storage)
    )
}


@Preview(showBackground = true, widthDp = 360, heightDp = 640)
@Composable
private fun ChooserScreenPreview() {
    MaterialTheme { ChooserActivity().run { ChooserScreen() } }
}