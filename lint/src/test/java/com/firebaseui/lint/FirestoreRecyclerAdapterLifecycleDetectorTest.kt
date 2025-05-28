/*
 * Copyright 2025 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.firebaseui.lint

import com.android.tools.lint.checks.infrastructure.TestFiles.java
import com.android.tools.lint.checks.infrastructure.TestLintTask
import com.firebaseui.lint.FirestoreRecyclerAdapterLifecycleDetector.Companion.ISSUE_MISSING_LISTENING_START_METHOD
import com.firebaseui.lint.FirestoreRecyclerAdapterLifecycleDetector.Companion.ISSUE_MISSING_LISTENING_STOP_METHOD
import org.junit.Test
import java.io.File

class FirestoreRecyclerAdapterLifecycleDetectorTest {

  // Nasty hack to make lint tests pass on Windows. For some reason, lint doesn't
  // automatically find the Android SDK in its standard path on Windows. This hack looks
  // through the system properties to find the path defined in `local.properties` and then
  // sets lint's SDK home to that path if it's found.
  private val sdkPath = System.getProperty("java.library.path").split(';').find {
    it.contains("SDK", true)
  }

  fun configuredLint(): TestLintTask = TestLintTask.lint().apply {
     sdkHome(File(sdkPath ?: return@apply))
  }

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
