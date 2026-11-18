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
import android.util.Log
import com.google.android.gms.common.util.AndroidUtilsLight
import com.google.android.gms.common.util.Hex
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.concurrent.ConcurrentHashMap

/** Blocking HTTP GET seam. Throws [java.io.IOException] on transport failure. */
internal fun interface PasswordPolicyHttpClient {
    fun get(url: String, headers: Map<String, String>): HttpResponse

    /** [body] is read only for 2xx responses. */
    data class HttpResponse(val code: Int, val body: String?)
}

/** Returns the uppercase hex SHA-1 of the app's signing certificate, or `null`. */
internal fun interface CertificateHashProvider {
    fun sha1Hex(context: Context, packageName: String): String?
}

/**
 * [HttpURLConnection] client. A 2xx body larger than [maxBodyBytes] throws [IOException] instead
 * of being buffered.
 */
internal class UrlConnectionPasswordPolicyHttpClient(
    private val connectTimeoutMs: Int = DEFAULT_TIMEOUT_MS,
    private val readTimeoutMs: Int = DEFAULT_TIMEOUT_MS,
    private val maxBodyBytes: Int = DEFAULT_MAX_BODY_BYTES,
) : PasswordPolicyHttpClient {
    override fun get(
        url: String,
        headers: Map<String, String>,
    ): PasswordPolicyHttpClient.HttpResponse {
        val connection = URL(url).openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "GET"
            connection.connectTimeout = connectTimeoutMs
            connection.readTimeout = readTimeoutMs
            connection.useCaches = false
            headers.forEach { (name, value) -> connection.setRequestProperty(name, value) }
            val code = connection.responseCode
            val body = if (code in 200..299) {
                connection.inputStream.use { readCapped(it) }
            } else {
                null
            }
            return PasswordPolicyHttpClient.HttpResponse(code, body)
        } finally {
            connection.disconnect()
        }
    }

    private fun readCapped(input: InputStream): String {
        val out = ByteArrayOutputStream()
        val buffer = ByteArray(BUFFER_BYTES)
        while (true) {
            val read = input.read(buffer)
            if (read == -1) break
            if (out.size() + read > maxBodyBytes) {
                throw IOException("Response body exceeds $maxBodyBytes bytes")
            }
            out.write(buffer, 0, read)
        }
        return out.toString(Charsets.UTF_8.name())
    }

    internal companion object {
        const val DEFAULT_TIMEOUT_MS = 10_000
        const val DEFAULT_MAX_BODY_BYTES = 64 * 1024
        private const val BUFFER_BYTES = 8 * 1024
    }
}

/** Returns `null` instead of throwing, e.g. for an unknown package. */
internal object PlayServicesCertificateHashProvider : CertificateHashProvider {
    override fun sha1Hex(context: Context, packageName: String): String? = try {
        AndroidUtilsLight.getPackageCertificateHashBytes(context, packageName)
            ?.let { Hex.bytesToStringUppercase(it, false) }
    } catch (_: Exception) {
        null
    }
}

/**
 * Fetches and caches the project password policy per FirebaseApp name and tenant ID.
 *
 * A fetched policy is cached until [refetchIfCached] replaces it. A failed fetch is cached as
 * "no policy" for [failureTtlMs] so an offline or emulator setup does not refetch on every call.
 * Every failure resolves to `null`; only exception class names are logged, never URLs or keys.
 */
internal class PasswordPolicyRepository(
    private val httpClient: PasswordPolicyHttpClient = UrlConnectionPasswordPolicyHttpClient(),
    private val certificateHash: CertificateHashProvider = PlayServicesCertificateHashProvider,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val baseUrl: String = DEFAULT_BASE_URL,
    private val failureTtlMs: Long = DEFAULT_FAILURE_TTL_MS,
    private val clockMs: () -> Long = { System.nanoTime() / 1_000_000 },
    private val log: (String) -> Unit = { Log.w(TAG, it) },
) {
    private data class Key(val appName: String, val tenantId: String?)

    private sealed interface Entry {
        data class Fetched(val policy: PasswordPolicy) : Entry
        data class Failed(val atMs: Long) : Entry
    }

    private val cache = ConcurrentHashMap<Key, Entry>()
    private val locks = ConcurrentHashMap<Key, Mutex>()

    /** Returns the cached policy for [auth], fetching it if absent or a failure has expired. */
    suspend fun getPolicy(context: Context, auth: FirebaseAuth): PasswordPolicy? {
        val key = Key(auth.app.name, auth.tenantId)
        return lockFor(key).withLock {
            val entry = cache[key]
            when {
                entry is Entry.Fetched -> entry.policy
                entry is Entry.Failed && clockMs() - entry.atMs < failureTtlMs -> null
                else -> fetchAndStore(context, auth, key)
            }
        }
    }

    /**
     * Refetches only when a policy is already cached for [auth], matching the JS SDK's recache
     * after `PASSWORD_DOES_NOT_MEET_REQUIREMENTS`. A failed refetch keeps the previous policy.
     */
    suspend fun refetchIfCached(context: Context, auth: FirebaseAuth): PasswordPolicy? {
        val key = Key(auth.app.name, auth.tenantId)
        return lockFor(key).withLock {
            val previous = (cache[key] as? Entry.Fetched)?.policy ?: return@withLock null
            fetch(context, auth)?.also { cache[key] = Entry.Fetched(it) } ?: previous
        }
    }

    private fun lockFor(key: Key): Mutex = locks.getOrPut(key) { Mutex() }

    private suspend fun fetchAndStore(
        context: Context,
        auth: FirebaseAuth,
        key: Key,
    ): PasswordPolicy? {
        val policy = fetch(context, auth)
        cache[key] = if (policy != null) Entry.Fetched(policy) else Entry.Failed(clockMs())
        return policy
    }

    private suspend fun fetch(context: Context, auth: FirebaseAuth): PasswordPolicy? = try {
        withContext(ioDispatcher) {
            val response = httpClient.get(buildUrl(auth), buildHeaders(context))
            if (response.code !in 200..299 || response.body == null) {
                log("Password policy fetch failed: HTTP ${response.code}")
                null
            } else {
                PasswordPolicy.parse(response.body)
                    ?: run {
                        log("Password policy response rejected")
                        null
                    }
            }
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        log("Password policy fetch failed: ${e.javaClass.name}")
        null
    }

    private fun buildUrl(auth: FirebaseAuth): String {
        val apiKey = auth.app.options.apiKey
        val tenantId = auth.tenantId
        return buildString {
            append(baseUrl).append("?key=").append(URLEncoder.encode(apiKey, UTF_8))
            if (tenantId != null) {
                append("&tenantId=").append(URLEncoder.encode(tenantId, UTF_8))
            }
        }
    }

    private fun buildHeaders(context: Context): Map<String, String> {
        val packageName = context.packageName
        val cert = try {
            certificateHash.sha1Hex(context, packageName)
        } catch (e: Exception) {
            log("Certificate hash unavailable: ${e.javaClass.name}")
            null
        }
        return buildMap {
            put(HEADER_PACKAGE, packageName)
            if (cert != null) put(HEADER_CERT, cert)
        }
    }

    companion object {
        internal const val HEADER_PACKAGE = "X-Android-Package"
        internal const val HEADER_CERT = "X-Android-Cert"
        internal const val DEFAULT_BASE_URL =
            "https://identitytoolkit.googleapis.com/v2/passwordPolicy"
        internal const val DEFAULT_FAILURE_TTL_MS = 60_000L
        private const val TAG = "PasswordPolicy"
        private const val UTF_8 = "UTF-8"
    }
}
