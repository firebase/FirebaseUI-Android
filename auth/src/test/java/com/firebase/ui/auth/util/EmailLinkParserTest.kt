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

package com.firebase.ui.auth.util

import androidx.core.net.toUri
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Unit tests for [EmailLinkParser], including round-trips of the continue URLs that
 * [ContinueUrlBuilder] produces.
 *
 * @suppress Internal test class
 */
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class EmailLinkParserTest {

    @Test
    fun `parses the parameters of a continue url built from a url without a query`() {
        val parser = EmailLinkParser(
            ContinueUrlBuilder("https://example.com/finish")
                .appendSessionId("sid123")
                .appendAnonymousUserId("auid456")
                .appendProviderId("google.com")
                .appendForceSameDeviceBit(true)
                .build()
        )

        assertThat(parser.sessionId).isEqualTo("sid123")
        assertThat(parser.anonymousUserId).isEqualTo("auid456")
        assertThat(parser.providerId).isEqualTo("google.com")
        assertThat(parser.forceSameDeviceBit).isTrue()
    }

    @Test
    fun `parses the parameters of a continue url built from a url that already had a query`() {
        val parser = EmailLinkParser(
            ContinueUrlBuilder("https://example.com/finish?demo=fullcustomization")
                .appendSessionId("sid123")
                .appendAnonymousUserId("auid456")
                .appendProviderId("google.com")
                .appendForceSameDeviceBit(true)
                .build()
        )

        assertThat(parser.sessionId).isEqualTo("sid123")
        assertThat(parser.anonymousUserId).isEqualTo("auid456")
        assertThat(parser.providerId).isEqualTo("google.com")
        assertThat(parser.forceSameDeviceBit).isTrue()
    }

    @Test
    fun `parses the parameters of a continue url that had a query and a fragment`() {
        val parser = EmailLinkParser(
            ContinueUrlBuilder("https://example.com/finish?demo=fullcustomization#section")
                .appendSessionId("sid123")
                .appendForceSameDeviceBit(false)
                .build()
        )

        assertThat(parser.sessionId).isEqualTo("sid123")
        assertThat(parser.forceSameDeviceBit).isFalse()
    }

    @Test
    fun `parses the parameters out of the continue url nested in an email link`() {
        val continueUrl = ContinueUrlBuilder("https://example.com/finish?demo=fullcustomization")
            .appendSessionId("sid123")
            .appendAnonymousUserId("auid456")
            .appendForceSameDeviceBit(true)
            .build()

        val emailLink = "https://example.firebaseapp.com/__/auth/action".toUri()
            .buildUpon()
            .appendQueryParameter("mode", "signIn")
            .appendQueryParameter("oobCode", "oob789")
            .appendQueryParameter("continueUrl", continueUrl)
            .build()
            .toString()

        val parser = EmailLinkParser(emailLink)

        assertThat(parser.oobCode).isEqualTo("oob789")
        assertThat(parser.sessionId).isEqualTo("sid123")
        assertThat(parser.anonymousUserId).isEqualTo("auid456")
        assertThat(parser.forceSameDeviceBit).isTrue()
    }

    @Test
    fun `missing optional parameters read back as null`() {
        val parser = EmailLinkParser(
            ContinueUrlBuilder("https://example.com/finish").appendSessionId("sid123").build()
        )

        assertThat(parser.sessionId).isEqualTo("sid123")
        assertThat(parser.anonymousUserId).isNull()
        assertThat(parser.providerId).isNull()
        // The bit defaults to false when the link does not carry one.
        assertThat(parser.forceSameDeviceBit).isFalse()
    }

    @Test
    fun `parses the parameters out of a continue url nested behind a dynamic link`() {
        val continueUrl = ContinueUrlBuilder("https://example.com/finish?demo=fullcustomization")
            .appendSessionId("sid123")
            .appendAnonymousUserId("auid456")
            .build()

        val actionLink = "https://example.firebaseapp.com/__/auth/action".toUri()
            .buildUpon()
            .appendQueryParameter("mode", "signIn")
            .appendQueryParameter("oobCode", "oob789")
            .appendQueryParameter("continueUrl", continueUrl)
            .build()
            .toString()

        // Exercises parseUri's `link=` branch, not just `continueUrl=`.
        val dynamicLink = "https://example.page.link/x".toUri()
            .buildUpon()
            .appendQueryParameter("link", actionLink)
            .build()
            .toString()

        val parser = EmailLinkParser(dynamicLink)

        assertThat(parser.oobCode).isEqualTo("oob789")
        assertThat(parser.sessionId).isEqualTo("sid123")
        assertThat(parser.anonymousUserId).isEqualTo("auid456")
    }

    @Test(expected = IllegalArgumentException::class)
    fun `blank link is rejected`() {
        EmailLinkParser("   ")
    }

    @Test(expected = IllegalArgumentException::class)
    fun `link without parameters is rejected`() {
        EmailLinkParser("https://example.com/finish")
    }

    @Test(expected = IllegalArgumentException::class)
    fun `missing oob code is rejected when read`() {
        EmailLinkParser("https://example.com/finish?ui_sid=sid123").oobCode
    }
}
