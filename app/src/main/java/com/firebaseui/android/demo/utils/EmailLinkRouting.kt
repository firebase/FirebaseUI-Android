package com.firebaseui.android.demo.utils

import android.net.Uri
import androidx.core.net.toUri

/**
 * Works out which auth demo a returning email link came from.
 *
 * Demo plumbing, not an example of library usage: this app has several auth demos behind one deep
 * link, so it has to tag its own links and route the return trip. An app with a single sign-in
 * screen needs none of this — it passes the link straight to `FirebaseAuthScreen(emailLink = …)`.
 */

/** The query parameter each demo tags its own continue URL with. */
const val EMAIL_LINK_ORIGIN_PARAM = "demo"

/** The continue URL, which sits either directly on [uri] or nested inside its `link`. */
private fun continueUrlOf(uri: Uri): Uri? {
    uri.getQueryParameter("continueUrl")?.let { return it.toUri() }
    uri.getQueryParameter("link")?.let { return continueUrlOf(it.toUri()) }
    return null
}

/**
 * Which demo sent [link], or null when it says nothing about where it came from.
 *
 * Read off the continue URL rather than the outer link, so the library's own `ui_` parameters and
 * any the action handler adds cannot be mistaken for the tag.
 */
internal fun emailLinkOrigin(link: String?): String? =
    link?.takeIf { it.isNotEmpty() }
        ?.let { runCatching { continueUrlOf(it.toUri()) }.getOrNull() }
        ?.getQueryParameter(EMAIL_LINK_ORIGIN_PARAM)
