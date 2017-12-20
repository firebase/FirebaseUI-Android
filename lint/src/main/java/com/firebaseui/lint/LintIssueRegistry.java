package com.firebaseui.lint;

import com.android.tools.lint.client.api.IssueRegistry;
import com.android.tools.lint.detector.api.Issue;

import java.util.Collections;
import java.util.List;

/**
 * Registry for custom FirebaseUI lint checks.
 */
public class LintIssueRegistry extends IssueRegistry {

    @Override
    public List<Issue> getIssues() {
        return Collections.singletonList(NonGlobalIdDetector.NON_GLOBAL_ID);
    }

}
