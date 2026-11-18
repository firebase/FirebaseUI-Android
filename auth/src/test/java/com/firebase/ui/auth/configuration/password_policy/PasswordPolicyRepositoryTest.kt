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
import androidx.test.core.app.ApplicationProvider
import com.firebase.ui.auth.configuration.password_policy.PasswordPolicyHttpClient.HttpResponse
import com.firebase.ui.auth.configuration.password_policy.PasswordPolicyRepository.Companion.HEADER_CERT
import com.firebase.ui.auth.configuration.password_policy.PasswordPolicyRepository.Companion.HEADER_PACKAGE
import com.firebase.ui.auth.configuration.password_policy.PasswordPolicyTest.Companion.SAMPLE_RESPONSE
import com.google.common.truth.Truth.assertThat
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.IOException
import java.net.SocketTimeoutException
import java.util.concurrent.Executors

/**
 * Unit tests for [PasswordPolicyRepository] request shape, failure handling and caching.
 *
 * @suppress Internal test class
 */
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class PasswordPolicyRepositoryTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    private class Request(val url: String, val headers: Map<String, String>)

    private class FakeHttpClient(
        var respond: (String) -> HttpResponse = { HttpResponse(200, SAMPLE_RESPONSE) },
    ) : PasswordPolicyHttpClient {
        val requests = mutableListOf<Request>()
        val threadNames = mutableListOf<String>()
        override fun get(url: String, headers: Map<String, String>): HttpResponse {
            synchronized(requests) {
                requests += Request(url, headers)
                threadNames += Thread.currentThread().name
            }
            return respond(url)
        }
    }

    private val http = FakeHttpClient()
    private val logs = mutableListOf<String>()
    private var now = 0L

    private fun repository(
        certificateHash: CertificateHashProvider = CertificateHashProvider { _, _ -> CERT },
        ioDispatcher: CoroutineDispatcher = Dispatchers.Unconfined,
    ) = PasswordPolicyRepository(
        httpClient = http,
        certificateHash = certificateHash,
        ioDispatcher = ioDispatcher,
        clockMs = { now },
        log = { synchronized(logs) { logs += it } },
    )

    private fun auth(appName: String = "[DEFAULT]", tenantId: String? = null): FirebaseAuth {
        val options = FirebaseOptions.Builder()
            .setApiKey(API_KEY)
            .setApplicationId("1:123:android:abc")
            .setProjectId("test-project")
            .build()
        val app = mock<FirebaseApp> {
            on { name } doReturn appName
            on { this.options } doReturn options
        }
        return mock {
            on { this.app } doReturn app
            on { this.tenantId } doReturn tenantId
        }
    }

    // =============================================================================================
    // Request shape
    // =============================================================================================

    @Test
    fun `success returns the parsed policy`() = runTest {
        val policy = repository().getPolicy(context, auth())

        assertThat(policy).isEqualTo(PasswordPolicy.parse(SAMPLE_RESPONSE))
        assertThat(http.requests.single().url).isEqualTo(
            "https://identitytoolkit.googleapis.com/v2/passwordPolicy?key=$API_KEY"
        )
    }

    @Test
    fun `tenantId is sent as a query param when set`() = runTest {
        repository().getPolicy(context, auth(tenantId = "tenant-1"))

        assertThat(http.requests.single().url).endsWith("?key=$API_KEY&tenantId=tenant-1")
    }

    @Test
    fun `tenantId query param is absent when tenantId is null`() = runTest {
        repository().getPolicy(context, auth())

        assertThat(http.requests.single().url).doesNotContain("tenantId")
    }

    @Test
    fun `package and certificate headers are sent`() = runTest {
        repository().getPolicy(context, auth())

        assertThat(http.requests.single().headers).containsExactly(
            HEADER_PACKAGE, context.packageName,
            HEADER_CERT, CERT,
        )
    }

    @Test
    fun `certificate header is omitted when the hash is null`() = runTest {
        repository(certificateHash = { _, _ -> null }).getPolicy(context, auth())

        assertThat(http.requests.single().headers)
            .containsExactly(HEADER_PACKAGE, context.packageName)
    }

    @Test
    fun `certificate header is omitted when hashing throws`() = runTest {
        val repo = repository(certificateHash = { _, _ -> throw IllegalStateException("boom") })

        val policy = repo.getPolicy(context, auth())

        assertThat(policy).isNotNull()
        assertThat(http.requests.single().headers)
            .containsExactly(HEADER_PACKAGE, context.packageName)
    }

    // =============================================================================================
    // Failures resolve to no policy
    // =============================================================================================

    @Test
    fun `offline IOException returns null`() = runTest {
        http.respond = { url -> throw IOException("Unable to resolve host for $url") }

        assertThat(repository().getPolicy(context, auth())).isNull()
    }

    @Test
    fun `timeout returns null`() = runTest {
        http.respond = { throw SocketTimeoutException("timeout") }

        assertThat(repository().getPolicy(context, auth())).isNull()
    }

    @Test
    fun `403 returns null`() = runTest {
        http.respond = { HttpResponse(403, null) }

        assertThat(repository().getPolicy(context, auth())).isNull()
    }

    @Test
    fun `emulator 501 NOT_IMPLEMENTED returns null`() = runTest {
        http.respond = { HttpResponse(501, null) }

        assertThat(repository().getPolicy(context, auth())).isNull()
    }

    @Test
    fun `malformed JSON returns null`() = runTest {
        http.respond = { HttpResponse(200, "<html>") }

        assertThat(repository().getPolicy(context, auth())).isNull()
    }

    @Test
    fun `unsupported schemaVersion returns null`() = runTest {
        http.respond = { HttpResponse(200, """{"schemaVersion":2}""") }

        assertThat(repository().getPolicy(context, auth())).isNull()
    }

    @Test
    fun `cancellation is rethrown`() = runTest {
        http.respond = { throw CancellationException("cancelled") }

        val result = runCatching { repository().getPolicy(context, auth()) }

        assertThat(result.exceptionOrNull()).isInstanceOf(CancellationException::class.java)
    }

    @Test
    fun `logs never contain the API key or URL`() = runTest {
        val repo = repository(certificateHash = { _, _ -> throw IllegalStateException(API_KEY) })
        http.respond = { url -> throw IOException("failed to connect to $url") }

        repo.getPolicy(context, auth())

        assertThat(logs).isNotEmpty()
        logs.forEach { line ->
            assertThat(line).doesNotContain("key=")
            assertThat(line).doesNotContain(API_KEY)
            assertThat(line).doesNotContain("googleapis")
        }
        assertThat(logs).contains("Password policy fetch failed: java.io.IOException")
    }

    // =============================================================================================
    // Cache
    // =============================================================================================

    @Test
    fun `cache hit does not send a second request`() = runTest {
        val repo = repository()

        val first = repo.getPolicy(context, auth())
        val second = repo.getPolicy(context, auth())

        assertThat(second).isEqualTo(first)
        assertThat(http.requests).hasSize(1)
    }

    @Test
    fun `concurrent callers share one request`() = runTest {
        // A separate StandardTestDispatcher makes withContext suspend, so every caller reaches
        // the cache check before the first fetch completes. Only the lock serialises them.
        val repo = repository(ioDispatcher = StandardTestDispatcher(testScheduler))

        val results = List(5) { async { repo.getPolicy(context, auth()) } }.awaitAll()

        assertThat(results).doesNotContain(null)
        assertThat(results.toSet()).containsExactly(PasswordPolicy.parse(SAMPLE_RESPONSE))
        assertThat(http.requests).hasSize(1)
    }

    @Test
    fun `fetch runs on the injected io dispatcher`() = runTest {
        val executor = Executors.newSingleThreadExecutor { Thread(it, IO_THREAD) }
        executor.asCoroutineDispatcher().use { dispatcher ->
            repository(ioDispatcher = dispatcher).getPolicy(context, auth())
        }

        assertThat(http.threadNames).containsExactly(IO_THREAD)
    }

    @Test
    fun `cache is keyed by app name and tenantId`() = runTest {
        val repo = repository()

        repo.getPolicy(context, auth())
        repo.getPolicy(context, auth(appName = "secondary"))
        repo.getPolicy(context, auth(tenantId = "tenant-1"))
        repo.getPolicy(context, auth(tenantId = "tenant-2"))
        repo.getPolicy(context, auth(tenantId = "tenant-1"))

        assertThat(http.requests).hasSize(4)
    }

    @Test
    fun `failed fetch is retried after the failure TTL, not before`() = runTest {
        val repo = repository()
        http.respond = { HttpResponse(503, null) }

        assertThat(repo.getPolicy(context, auth())).isNull()
        now += PasswordPolicyRepository.DEFAULT_FAILURE_TTL_MS - 1
        assertThat(repo.getPolicy(context, auth())).isNull()
        assertThat(http.requests).hasSize(1)

        http.respond = { HttpResponse(200, SAMPLE_RESPONSE) }
        now += 1
        assertThat(repo.getPolicy(context, auth())).isNotNull()
        assertThat(http.requests).hasSize(2)
    }

    @Test
    fun `refetchIfCached sends no request when nothing is cached`() = runTest {
        assertThat(repository().refetchIfCached(context, auth())).isNull()
        assertThat(http.requests).isEmpty()
    }

    @Test
    fun `refetchIfCached sends no request when only a failure is cached`() = runTest {
        val repo = repository()
        http.respond = { HttpResponse(403, null) }
        repo.getPolicy(context, auth())

        assertThat(repo.refetchIfCached(context, auth())).isNull()
        assertThat(http.requests).hasSize(1)
    }

    @Test
    fun `refetchIfCached replaces a cached policy`() = runTest {
        val repo = repository()
        repo.getPolicy(context, auth())
        val updated = SAMPLE_RESPONSE.replace("\"minPasswordLength\":10", "\"minPasswordLength\":12")
        http.respond = { HttpResponse(200, updated) }

        assertThat(repo.refetchIfCached(context, auth())!!.minPasswordLength).isEqualTo(12)
        assertThat(repo.getPolicy(context, auth())!!.minPasswordLength).isEqualTo(12)
        assertThat(http.requests).hasSize(2)
    }

    @Test
    fun `failed refetch keeps the previous policy`() = runTest {
        val repo = repository()
        val original = repo.getPolicy(context, auth())
        http.respond = { throw IOException() }

        assertThat(repo.refetchIfCached(context, auth())).isEqualTo(original)
        assertThat(repo.getPolicy(context, auth())).isEqualTo(original)
        assertThat(http.requests).hasSize(2)
    }

    @Test
    fun `refetchIfCached only touches its own key`() = runTest {
        val repo = repository()
        repo.getPolicy(context, auth(tenantId = "tenant-1"))

        assertThat(repo.refetchIfCached(context, auth(tenantId = "tenant-2"))).isNull()
        assertThat(http.requests).hasSize(1)
    }

    private companion object {
        const val API_KEY = "AIzaFakeKeyForTests123"
        const val CERT = "0123456789ABCDEF0123456789ABCDEF01234567"
        const val IO_THREAD = "password-policy-test-io"
    }
}
