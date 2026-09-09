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

/** The continue URL, which sits either directly on [uri] or nested inside its `link`. */
private fun continueUrlOf(uri: Uri): Uri? {
    uri.getQueryParameter("continueUrl")?.let { return it.toUri() }
    uri.getQueryParameter("link")?.let { return continueUrlOf(it.toUri()) }
    return null
}

/**
 * Which demo sent [link], or null when it says nothing about where it came from.
 *
 * Each demo tags the last path segment of its continue URL — a path rather than a query because
 * `ContinueUrlBuilder` appends "?" and would corrupt a query string that was already there.
 */
internal fun emailLinkOrigin(link: String?): String? =
    link?.takeIf { it.isNotEmpty() }
        ?.let { runCatching { continueUrlOf(it.toUri()) }.getOrNull() }
        ?.pathSegments
        ?.lastOrNull()
