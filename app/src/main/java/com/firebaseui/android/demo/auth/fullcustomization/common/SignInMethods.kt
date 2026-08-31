package com.firebaseui.android.demo.auth.fullcustomization.common

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.tasks.await

/**
 * Whether [email] is already registered, via Firebase Auth's `fetchSignInMethodsForEmail` —
 * deprecated by Firebase ("legacy") and, depending on the project's Email Enumeration Protection
 * setting, may always return an empty list regardless of whether the email exists.
 *
 * Shared by the method picker and the email slot so the two routing decisions can't drift apart.
 */
suspend fun fetchLegacySignInMethods(auth: FirebaseAuth, email: String): List<String> {
    return try {
        @Suppress("DEPRECATION")
        auth.fetchSignInMethodsForEmail(email)
            .await()
            .signInMethods
            ?.filter { it.isNotBlank() }
            ?: emptyList()
    } catch (e: Exception) {
        Log.w("FullCustomizationDemo", "fetchSignInMethodsForEmail failed for $email", e)
        emptyList()
    }
}
