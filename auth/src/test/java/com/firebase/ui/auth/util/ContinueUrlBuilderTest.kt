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
 * Unit tests for [ContinueUrlBuilder]. Runs under Robolectric so the assertions can read the
 * built URLs back through the real [android.net.Uri] parser rather than by string matching.
 *
 * @suppress Internal test class
 */
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class ContinueUrlBuilderTest {

    @Test
    fun `url without a query gets a question mark separator`() {
        val url = ContinueUrlBuilder("https://example.com/finish")
            .appendSessionId("sid123")
            .appendAnonymousUserId("auid456")
            .appendProviderId("google.com")
            .appendForceSameDeviceBit(true)
            .build()

        assertThat(url).isEqualTo(
            "https://example.com/finish?ui_sid=sid123&ui_auid=auid456&ui_pid=google.com&ui_sd=1"
        )

        val uri = url.toUri()
        assertThat(uri.getQueryParameter("ui_sid")).isEqualTo("sid123")
        assertThat(uri.getQueryParameter("ui_auid")).isEqualTo("auid456")
        assertThat(uri.getQueryParameter("ui_pid")).isEqualTo("google.com")
        assertThat(uri.getQueryParameter("ui_sd")).isEqualTo("1")
    }

    @Test
    fun `url with an existing query gets an ampersand separator and keeps the consumer's params`() {
        val url = ContinueUrlBuilder("https://example.com/finish?demo=fullcustomization")
            .appendSessionId("sid123")
            .appendAnonymousUserId("auid456")
            .appendProviderId("google.com")
            .appendForceSameDeviceBit(true)
            .build()

        assertThat(url.count { it == '?' }).isEqualTo(1)

        val uri = url.toUri()
        assertThat(uri.getQueryParameter("demo")).isEqualTo("fullcustomization")
        assertThat(uri.getQueryParameter("ui_sid")).isEqualTo("sid123")
        assertThat(uri.getQueryParameter("ui_auid")).isEqualTo("auid456")
        assertThat(uri.getQueryParameter("ui_pid")).isEqualTo("google.com")
        assertThat(uri.getQueryParameter("ui_sd")).isEqualTo("1")
    }

    @Test
    fun `url with several existing params keeps every one of them`() {
        val url = ContinueUrlBuilder("https://example.com/finish?demo=full&lang=en")
            .appendSessionId("sid123")
            .build()

        val uri = url.toUri()
        assertThat(uri.getQueryParameter("demo")).isEqualTo("full")
        assertThat(uri.getQueryParameter("lang")).isEqualTo("en")
        assertThat(uri.getQueryParameter("ui_sid")).isEqualTo("sid123")
    }

    @Test
    fun `url ending in an open query marker is not given a second separator`() {
        assertThat(
            ContinueUrlBuilder("https://example.com/finish?").appendSessionId("sid123").build()
        ).isEqualTo("https://example.com/finish?ui_sid=sid123")

        assertThat(
            ContinueUrlBuilder("https://example.com/finish?demo=full&")
                .appendSessionId("sid123")
                .build()
        ).isEqualTo("https://example.com/finish?demo=full&ui_sid=sid123")
    }

    @Test
    fun `params are appended before the fragment`() {
        val url = ContinueUrlBuilder("https://example.com/finish?demo=full#section")
            .appendSessionId("sid123")
            .appendForceSameDeviceBit(false)
            .build()

        assertThat(url)
            .isEqualTo("https://example.com/finish?demo=full&ui_sid=sid123&ui_sd=0#section")

        val uri = url.toUri()
        assertThat(uri.getQueryParameter("ui_sid")).isEqualTo("sid123")
        assertThat(uri.getQueryParameter("ui_sd")).isEqualTo("0")
        assertThat(uri.fragment).isEqualTo("section")
    }

    @Test
    fun `fragment on a url without a query still gets a question mark separator`() {
        val url = ContinueUrlBuilder("https://example.com/finish#section")
            .appendSessionId("sid123")
            .build()

        assertThat(url).isEqualTo("https://example.com/finish?ui_sid=sid123#section")
        assertThat(url.toUri().getQueryParameter("ui_sid")).isEqualTo("sid123")
    }

    @Test
    fun `blank values are skipped`() {
        val url = ContinueUrlBuilder("https://example.com/finish")
            .appendSessionId("")
            .appendAnonymousUserId("   ")
            .appendProviderId("google.com")
            .build()

        assertThat(url).isEqualTo("https://example.com/finish?ui_pid=google.com")

        val uri = url.toUri()
        assertThat(uri.getQueryParameter("ui_sid")).isNull()
        assertThat(uri.getQueryParameter("ui_auid")).isNull()
    }

    @Test
    fun `blank first value does not steal the separator from the next param`() {
        val url = ContinueUrlBuilder("https://example.com/finish?demo=full")
            .appendSessionId("")
            .appendAnonymousUserId("auid456")
            .build()

        assertThat(url).isEqualTo("https://example.com/finish?demo=full&ui_auid=auid456")
    }

    @Test
    fun `no params leaves the url untouched`() {
        assertThat(ContinueUrlBuilder("https://example.com/finish").build())
            .isEqualTo("https://example.com/finish")

        assertThat(ContinueUrlBuilder("https://example.com/finish?demo=full").build())
            .isEqualTo("https://example.com/finish?demo=full")

        assertThat(ContinueUrlBuilder("https://example.com/finish?demo=full#section").build())
            .isEqualTo("https://example.com/finish?demo=full#section")
    }

    @Test
    fun `multi segment path is untouched`() {
        val url = ContinueUrlBuilder("https://example.com/finish/fullcustomization")
            .appendSessionId("sid123")
            .build()

        assertThat(url).isEqualTo("https://example.com/finish/fullcustomization?ui_sid=sid123")

        val uri = url.toUri()
        assertThat(uri.lastPathSegment).isEqualTo("fullcustomization")
        assertThat(uri.getQueryParameter("ui_sid")).isEqualTo("sid123")
    }

    @Test
    fun `force same device bit is written as one or zero`() {
        assertThat(
            ContinueUrlBuilder("https://example.com/finish").appendForceSameDeviceBit(true).build()
        ).isEqualTo("https://example.com/finish?ui_sd=1")

        assertThat(
            ContinueUrlBuilder("https://example.com/finish").appendForceSameDeviceBit(false).build()
        ).isEqualTo("https://example.com/finish?ui_sd=0")
    }

    @Test
    fun `query ending in an unencoded question mark still keeps every param`() {
        // An unencoded '?' is legal inside a query (RFC 3986), so a trailing one does
        // not mean the query is still open.
        val input = "https://example.com/finish?next=/search?"
        val url = ContinueUrlBuilder(input)
            .appendSessionId("sid123")
            .appendForceSameDeviceBit(true)
            .build()

        val uri = url.toUri()
        assertThat(uri.getQueryParameter("ui_sid")).isEqualTo("sid123")
        assertThat(uri.getQueryParameter("ui_sd")).isEqualTo("1")
        assertThat(uri.getQueryParameter("next")).isEqualTo("/search?")
    }

    @Test
    fun `url ending in a bare double question mark keeps every param`() {
        val url = ContinueUrlBuilder("https://example.com/finish??")
            .appendSessionId("sid123")
            .build()

        assertThat(url.toUri().getQueryParameter("ui_sid")).isEqualTo("sid123")
    }

    @Test
    fun `fragment before a query keeps the params in the query`() {
        // Everything after the first '#' is the fragment, query-looking or not.
        val url = ContinueUrlBuilder("https://example.com/finish#section?x=1")
            .appendSessionId("sid123")
            .build()

        val uri = url.toUri()
        assertThat(uri.getQueryParameter("ui_sid")).isEqualTo("sid123")
        assertThat(uri.fragment).isEqualTo("section?x=1")
    }

    @Test
    fun `percent encoded url in a query value survives untouched`() {
        val input = "https://example.com/finish?redirect=https%3A%2F%2Ffoo.com%2Fa%3Fb%3Dc"
        val url = ContinueUrlBuilder(input).appendSessionId("sid123").build()

        val uri = url.toUri()
        assertThat(uri.getQueryParameter("redirect")).isEqualTo("https://foo.com/a?b=c")
        assertThat(uri.getQueryParameter("ui_sid")).isEqualTo("sid123")
    }

    @Test
    fun `the consumer's url is preserved verbatim ahead of the appended params`() {
        val input = "https://example.com/finish?demo=full&lang=en"
        val url = ContinueUrlBuilder(input).appendSessionId("sid123").build()

        assertThat(url).startsWith(input)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `blank url is rejected`() {
        ContinueUrlBuilder("   ")
    }
}
