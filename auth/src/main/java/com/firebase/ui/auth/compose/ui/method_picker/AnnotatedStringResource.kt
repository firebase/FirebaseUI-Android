package com.firebase.ui.auth.compose.ui.method_picker

import android.content.Context
import android.content.Intent
import androidx.annotation.StringRes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.core.net.toUri

@Composable
internal fun AnnotatedStringResource(
    context: Context,
    modifier: Modifier = Modifier,
    @StringRes id: Int,
    vararg links: Pair<String, String>,
    inPreview: Boolean = false,
    previewText: String? = null,
) {
    val labels = links.map { it.first }.toTypedArray()

    val template = if (inPreview && previewText != null) {
        previewText
    } else {
        stringResource(id = id, *labels)
    }

    val annotated = buildAnnotatedString {
        var currentIndex = 0

        links.forEach { (label, url) ->
            val start = template.indexOf(label, currentIndex).takeIf { it >= 0 } ?: return@forEach

            append(template.substring(currentIndex, start))

            withLink(
                LinkAnnotation.Url(
                    url,
                    styles = TextLinkStyles(
                        style = SpanStyle(
                            color = MaterialTheme.colorScheme.primary,
                            textDecoration = TextDecoration.Underline,
                        )
                    )
                ) {
                    val intent = Intent(Intent.ACTION_VIEW, url.toUri())
                    context.startActivity(intent)
                }
            ) {
                append(label)
            }

            currentIndex = start + label.length
        }

        if (currentIndex < template.length) {
            append(template.substring(currentIndex))
        }
    }

    Text(
        modifier = modifier,
        text = annotated,
        style = MaterialTheme.typography.bodyMedium,
        textAlign = TextAlign.Center
    )
}
