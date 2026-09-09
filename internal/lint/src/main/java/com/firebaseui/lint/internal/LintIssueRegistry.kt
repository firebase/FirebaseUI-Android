package com.firebaseui.lint.internal

import com.android.tools.lint.client.api.IssueRegistry
import com.android.tools.lint.client.api.Vendor

/**
 * Registry for custom FirebaseUI lint checks.
 */
class LintIssueRegistry : IssueRegistry() {
    override val api: Int
        get() = com.android.tools.lint.detector.api.CURRENT_API

    override val issues = listOf(
        NonGlobalIdDetector.NON_GLOBAL_ID
    )

    override val vendor = Vendor(
        vendorName = "FirebaseUI Android",
        identifier = "com.firebaseui.lint.internal",
        feedbackUrl = "https://github.com/firebase/FirebaseUI-Android",
        contact = "https://github.com/firebase/FirebaseUI-Android"
    )
}
