package com.firebaseui.android.demo.auth.fullcustomization.common

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.firebaseui.android.demo.R

/**
 * The page frame shared by the MFA and reauthentication screens: mascot, headline, a single
 * elevated card, and bottom-anchored actions.
 *
 * The email and phone steps predate this and inline the same structure themselves.
 *
 * verticalScroll measures content with infinite max height, and Column distributes weights
 * against the MIN height when max is infinite (RowColumnMeasurePolicy.kt) — so
 * heightIn(min = viewport) makes the weighted spacers expand (centering content, anchoring the
 * actions to the bottom) when everything fits, and collapse to zero (plain scrolling) when it
 * doesn't.
 */
@Composable
fun AuthPage(
    @DrawableRes mascot: Int,
    mascotDescription: String,
    title: String,
    cardContentDescription: String,
    actions: @Composable ColumnScope.() -> Unit,
    card: @Composable ColumnScope.() -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        // Full-bleed, and deliberately outside the safeDrawingPadding below so it runs edge to
        // edge under the system bars — same as MainUI and PhoneSignInUI do for their slots.
        Image(
            painter = painterResource(id = R.drawable.custom_background),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding(),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .heightIn(min = maxHeight)
                    .padding(horizontal = 40.dp, vertical = 24.dp),
            ) {
                Spacer(modifier = Modifier.weight(1f))

                Column(modifier = Modifier.fillMaxWidth()) {
                    Image(
                        painter = painterResource(id = mascot),
                        contentDescription = mascotDescription,
                        modifier = Modifier.size(72.dp),
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = title,
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(modifier = Modifier.height(24.dp))

                    HardOffsetShadow(shape = AuthFieldShape, modifier = Modifier.fillMaxWidth()) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .semantics { contentDescription = cardContentDescription },
                            color = MaterialTheme.colorScheme.surface,
                            shape = AuthFieldShape,
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp),
                                content = card,
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.weight(1f))
                Spacer(modifier = Modifier.height(24.dp))

                Column(modifier = Modifier.fillMaxWidth(), content = actions)
            }
        }
    }
}
