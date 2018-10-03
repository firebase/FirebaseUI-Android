package com.firebaseui.lint

import com.android.tools.lint.client.api.IssueRegistry

/**
 * Registry for custom FirebaseUI lint checks.
 */
class LintIssueRegistry : IssueRegistry() {
    override val issues = listOf(
            FirestoreRecyclerAdapterLifecycleDetector.ISSUE_MISSING_LISTENING_START_METHOD,
            FirestoreRecyclerAdapterLifecycleDetector.ISSUE_MISSING_LISTENING_STOP_METHOD
    )
}
