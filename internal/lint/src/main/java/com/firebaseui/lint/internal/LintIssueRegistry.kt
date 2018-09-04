package com.firebaseui.lint.internal

import com.android.tools.lint.client.api.IssueRegistry

/**
 * Registry for custom FirebaseUI lint checks.
 */
class LintIssueRegistry : IssueRegistry() {
    override val issues = listOf(
        NonGlobalIdDetector.NON_GLOBAL_ID
    )
}
