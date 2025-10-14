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

package com.firebase.ui.auth.compose.testutil

import android.os.Looper
import com.google.android.gms.tasks.Task
import org.robolectric.Shadows.shadowOf

/**
 * Awaits the completion of a Firebase [Task] in a Robolectric test environment.
 *
 * This extension function is specifically designed for Robolectric tests where the main looper
 * must be manually pumped to allow Firebase task callbacks to execute. Unlike the standard
 * `await()` from kotlinx-coroutines-play-services, this function uses a blocking approach
 * that works correctly with Robolectric's shadow looper.
 *
 * **Why this is needed:**
 * - Firebase's standard `await()` from kotlinx-coroutines-play-services can hang in Robolectric
 *   because the looper doesn't auto-advance
 * - This function manually pumps the looper in a blocking manner
 * - It properly handles task cancellation and exceptions
 *
 * **Usage:**
 * ```kotlin
 * @Test
 * fun myTest() = runBlocking {
 *     val result = auth.signInWithEmailAndPassword(email, password).awaitWithLooper()
 *     // result is now available
 * }
 * ```
 *
 * @return The result of the task if successful
 * @throws Exception if the task fails or is cancelled
 */
fun <T> Task<T>.awaitWithLooper(timeoutMs: Long = 10000): T {
    val startTime = System.currentTimeMillis()

    // Pump the looper until the task completes
    while (!isComplete) {
        shadowOf(Looper.getMainLooper()).idle()
        Thread.sleep(5)

        // Check for timeout
        if (System.currentTimeMillis() - startTime > timeoutMs) {
            throw Exception("Task timed out after ${timeoutMs}ms")
        }
    }

    // Task is complete, return result or throw exception
    return when {
        isCanceled -> throw Exception("Task was cancelled")
        isSuccessful -> result
        else -> throw (exception ?: Exception("Unknown Firebase Task failure"))
    }
}
