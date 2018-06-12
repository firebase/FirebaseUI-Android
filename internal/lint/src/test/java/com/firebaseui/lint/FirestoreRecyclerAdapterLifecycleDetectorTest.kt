package com.firebaseui.lint

import com.android.tools.lint.checks.infrastructure.TestFiles.java
import com.firebaseui.lint.FirestoreRecyclerAdapterLifecycleDetector.Companion.ISSUE_MISSING_LISTENING_START_METHOD
import com.firebaseui.lint.FirestoreRecyclerAdapterLifecycleDetector.Companion.ISSUE_MISSING_LISTENING_STOP_METHOD
import com.firebaseui.lint.LintTestHelper.configuredLint
import org.junit.Test

class FirestoreRecyclerAdapterLifecycleDetectorTest {

  @Test
  fun `Checks missing startListening() method call`() {
    configuredLint()
        .files(java("""
          |public class MissingStartListeningMethodCall {
          | private FirestoreRecyclerAdapter adapter;
          |}
          """.trimMargin()))
        .issues(ISSUE_MISSING_LISTENING_START_METHOD)
        .run()
        .expect("""
          |src/MissingStartListeningMethodCall.java:2: Warning: Have not called .startListening(). [FirestoreAdapterMissingStartListeningMethod]
          | private FirestoreRecyclerAdapter adapter;
          | ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
          |0 errors, 1 warnings
          """.trimMargin())
  }

  @Test
  fun `Checks missing stopListening() method call`() {
    configuredLint()
        .files(java("""
          |public class MissingStopListeningMethodCall {
          | private FirestoreRecyclerAdapter adapter;
          |
          | public void onStart() {
          |   adapter.startListening();
          | }
          |}
          """.trimMargin()))
        .issues(ISSUE_MISSING_LISTENING_STOP_METHOD)
        .run()
        .expect("""
          |src/MissingStopListeningMethodCall.java:2: Warning: Have called .startListening() without .stopListening(). [FirestoreAdapterMissingStopListeningMethod]
          | private FirestoreRecyclerAdapter adapter;
          | ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
          |0 errors, 1 warnings
          """.trimMargin())
  }

  @Test
  fun `Checks no warnings when startListening & stopListening methods called`() {
    configuredLint()
        .files(java("""
          |public class HasCalledStartStopListeningMethods {
          | private FirestoreRecyclerAdapter adapter;
          |
          | public void onStart() {
          |   adapter.startListening();
          | }
          |
          | public void onStop() {
          |   adapter.stopListening();
          | }
          |}
          """.trimMargin()))
        .issues(ISSUE_MISSING_LISTENING_START_METHOD, ISSUE_MISSING_LISTENING_STOP_METHOD)
        .run()
        .expectClean()
  }
}
