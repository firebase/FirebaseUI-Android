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

package com.firebase.ui.auth.compose.mfa

import com.google.firebase.auth.TotpSecret as FirebaseTotpSecret
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class TotpSecretTest {

    @Mock
    private lateinit var mockFirebaseTotpSecret: FirebaseTotpSecret
    private lateinit var totpSecret: TotpSecret

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        totpSecret = TotpSecret.from(mockFirebaseTotpSecret)
    }

    @Test
    fun `sharedSecretKey returns value from Firebase TOTP secret`() {
        // Given
        val expectedSecret = "JBSWY3DPEHPK3PXP"
        `when`(mockFirebaseTotpSecret.sharedSecretKey).thenReturn(expectedSecret)

        // When
        val result = totpSecret.sharedSecretKey

        // Then
        assertEquals(expectedSecret, result)
    }

    @Test
    fun `generateQrCodeUrl generates correct URL format`() {
        // Given
        val accountName = "user@example.com"
        val issuer = "MyApp"
        val expectedUrl = "otpauth://totp/MyApp:user@example.com?secret=JBSWY3DPEHPK3PXP&issuer=MyApp&algorithm=SHA1&digits=6&period=30"
        `when`(mockFirebaseTotpSecret.generateQrCodeUrl(accountName, issuer)).thenReturn(expectedUrl)

        // When
        val result = totpSecret.generateQrCodeUrl(accountName, issuer)

        // Then
        assertEquals(expectedUrl, result)
        verify(mockFirebaseTotpSecret).generateQrCodeUrl(accountName, issuer)
    }

    @Test
    fun `openInOtpApp calls Firebase TOTP secret method`() {
        // Given
        val qrCodeUrl = "otpauth://totp/MyApp:user@example.com?secret=JBSWY3DPEHPK3PXP"

        // When
        totpSecret.openInOtpApp(qrCodeUrl)

        // Then
        verify(mockFirebaseTotpSecret).openInOtpApp(qrCodeUrl)
    }

    @Test
    fun `getFirebaseTotpSecret returns the underlying Firebase TOTP secret`() {
        // When
        val result = totpSecret.getFirebaseTotpSecret()

        // Then
        assertEquals(mockFirebaseTotpSecret, result)
    }

    @Test
    fun `from creates TotpSecret instance from Firebase TOTP secret`() {
        // When
        val result = TotpSecret.from(mockFirebaseTotpSecret)

        // Then
        assertNotNull(result)
        assertEquals(mockFirebaseTotpSecret, result.getFirebaseTotpSecret())
    }
}
