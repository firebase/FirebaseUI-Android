/*
 * Copyright 2025 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.firebase.ui.auth.ui.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.firebase.ui.auth.R
import com.firebase.ui.auth.ui.FirebaseAuthTestTags
import com.firebase.ui.auth.ui.exposeTestTagsAsResourceIds

@Composable
fun TermsAndPrivacyForm(
    modifier: Modifier = Modifier,
    tosUrl: String?,
    ppUrl: String?
) {
    val uriHandler = LocalUriHandler.current
    Row(
        // This component is never composed as its own semantics owner — every call site sits
        // inside a screen Scaffold that already flags itself — but the flag is applied here as
        // well so the tags above remain exposed even for a future caller that hosts this
        // component with no flagged ancestor of its own. Setting the property twice is a no-op.
        modifier = modifier.exposeTestTagsAsResourceIds(),
    ) {
        TextButton(
            modifier = Modifier.testTag(FirebaseAuthTestTags.TermsAndPrivacy.TOS_LINK),
            onClick = {
                tosUrl?.let {
                    uriHandler.openUri(it)
                }
            },
            contentPadding = PaddingValues.Zero,
        ) {
            Text(
                text = stringResource(R.string.fui_terms_of_service),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                textDecoration = TextDecoration.Underline
            )
        }
        Spacer(modifier = Modifier.width(24.dp))
        TextButton(
            modifier = Modifier.testTag(FirebaseAuthTestTags.TermsAndPrivacy.PRIVACY_LINK),
            onClick = {
                ppUrl?.let {
                    uriHandler.openUri(it)
                }
            },
            contentPadding = PaddingValues.Zero,
        ) {
            Text(
                text = stringResource(R.string.fui_privacy_policy),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                textDecoration = TextDecoration.Underline
            )
        }
    }
}