package com.firebaseui.lint

import com.android.tools.lint.client.api.IssueRegistry
import com.android.tools.lint.client.api.Vendor

/**
 * Registry for custom FirebaseUI lint checks.
 */
class LintIssueRegistry : IssueRegistry() {
    override val api: Int
        get() = com.android.tools.lint.detector.api.CURRENT_API

    override val issues = listOf(
            FirestoreRecyclerAdapterLifecycleDetector.ISSUE_MISSING_LISTENING_START_METHOD,
            FirestoreRecyclerAdapterLifecycleDetector.ISSUE_MISSING_LISTENING_STOP_METHOD
    )

    override val vendor = Vendor(
        vendorName = "FirebaseUI Android",
        identifier = "com.firebaseui.lint",
        feedbackUrl = "https://github.com/firebase/FirebaseUI-Android",
        contact = "https://github.com/firebase/FirebaseUI-Android"
    )
}
