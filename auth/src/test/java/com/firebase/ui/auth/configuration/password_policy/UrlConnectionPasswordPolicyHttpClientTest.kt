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

import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Assert.assertThrows
import org.junit.Test
import java.io.IOException
import java.io.OutputStream
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketTimeoutException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

/**
 * Tests [UrlConnectionPasswordPolicyHttpClient] against a loopback socket server.
 *
 * @suppress Internal test class
 */
class UrlConnectionPasswordPolicyHttpClientTest {

    private val server = ServerSocket(0, 1, InetAddress.getLoopbackAddress())
    private val receivedHeaders = ConcurrentHashMap<String, String>()
    private val released = CountDownLatch(1)
    private val url = "http://127.0.0.1:${server.localPort}/v2/passwordPolicy?key=k"

    @After
    fun tearDown() {
        released.countDown()
        server.close()
    }

    /** Serves one request, recording its headers, then lets [respond] write the response. */
    private fun serve(respond: (OutputStream) -> Unit) {
        thread(isDaemon = true) {
            runCatching {
                server.accept().use { socket: Socket ->
                    val reader = socket.getInputStream().bufferedReader()
                    reader.readLine()
                    while (true) {
                        val line = reader.readLine()
                        if (line.isNullOrEmpty()) break
                        val (name, value) = line.split(":", limit = 2)
                        receivedHeaders[name.trim().lowercase()] = value.trim()
                    }
                    respond(socket.getOutputStream())
                    socket.getOutputStream().flush()
                }
            }
        }
    }

    private fun respondWith(code: Int, body: String) = serve { out ->
        val bytes = body.toByteArray()
        out.write(
            "HTTP/1.1 $code X\r\nContent-Length: ${bytes.size}\r\nConnection: close\r\n\r\n"
                .toByteArray()
        )
        out.write(bytes)
    }

    @Test
    fun `2xx returns the body`() {
        respondWith(200, """{"schemaVersion":1}""")

        val response = UrlConnectionPasswordPolicyHttpClient().get(url, emptyMap())

        assertThat(response.code).isEqualTo(200)
        assertThat(response.body).isEqualTo("""{"schemaVersion":1}""")
    }

    @Test
    fun `non-2xx returns the code with a null body and does not throw`() {
        respondWith(403, """{"error":{"code":403}}""")

        val response = UrlConnectionPasswordPolicyHttpClient().get(url, emptyMap())

        assertThat(response.code).isEqualTo(403)
        assertThat(response.body).isNull()
    }

    @Test
    fun `request headers are sent`() {
        respondWith(200, "{}")

        UrlConnectionPasswordPolicyHttpClient().get(
            url,
            mapOf("X-Android-Package" to "com.example", "X-Android-Cert" to "ABCDEF"),
        )

        assertThat(receivedHeaders).containsEntry("x-android-package", "com.example")
        assertThat(receivedHeaders).containsEntry("x-android-cert", "ABCDEF")
    }

    @Test
    fun `read timeout fails fast when the server never responds`() {
        serve { released.await(30, TimeUnit.SECONDS) }
        val client = UrlConnectionPasswordPolicyHttpClient(readTimeoutMs = 200)

        val start = System.nanoTime()
        assertThrows(SocketTimeoutException::class.java) { client.get(url, emptyMap()) }
        val elapsedMs = (System.nanoTime() - start) / 1_000_000

        assertThat(elapsedMs).isLessThan(5_000L)
    }

    @Test
    fun `body at the size cap is returned`() {
        val body = "a".repeat(UrlConnectionPasswordPolicyHttpClient.DEFAULT_MAX_BODY_BYTES)
        respondWith(200, body)

        assertThat(UrlConnectionPasswordPolicyHttpClient().get(url, emptyMap()).body)
            .hasLength(body.length)
    }

    @Test
    fun `body over the size cap throws IOException`() {
        respondWith(200, "a".repeat(UrlConnectionPasswordPolicyHttpClient.DEFAULT_MAX_BODY_BYTES + 1))

        assertThrows(IOException::class.java) {
            UrlConnectionPasswordPolicyHttpClient().get(url, emptyMap())
        }
    }
}
