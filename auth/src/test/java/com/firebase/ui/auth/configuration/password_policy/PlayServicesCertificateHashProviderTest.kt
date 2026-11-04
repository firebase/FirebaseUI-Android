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

package com.firebase.ui.auth.configuration.password_policy

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.Signature
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import java.security.MessageDigest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/**
 * Tests that [PlayServicesCertificateHashProvider] returns uppercase SHA-1 hex or `null`, never throws.
 *
 * @suppress Internal test class
 */
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class PlayServicesCertificateHashProviderTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun `package with one signature returns its uppercase SHA-1 hex`() {
        val signatureBytes = byteArrayOf(0x30, 0x0A, 0x1B, 0x7F, 0x00, 0xFF.toByte(), 0x42)
        shadowOf(context.packageManager).installPackage(
            PackageInfo().apply {
                packageName = SIGNED_PACKAGE
                signatures = arrayOf(Signature(signatureBytes))
            }
        )
        val expected = MessageDigest.getInstance("SHA-1").digest(signatureBytes)
            .joinToString("") { "%02X".format(it) }

        assertThat(PlayServicesCertificateHashProvider.sha1Hex(context, SIGNED_PACKAGE))
            .isEqualTo(expected)
    }

    @Test
    fun `unknown package returns null`() {
        assertThat(PlayServicesCertificateHashProvider.sha1Hex(context, "com.example.missing"))
            .isNull()
    }

    private companion object {
        const val SIGNED_PACKAGE = "com.example.signed"
    }
}
