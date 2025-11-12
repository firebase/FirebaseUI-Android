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

package com.firebase.ui.auth

import android.content.Context
import android.content.pm.PackageManager
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Unit tests for [FirebaseUIComposeRegistrar] covering component registration
 * and AndroidManifest.xml configuration validation.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class FirebaseUIComposeRegistrarTest {

    private lateinit var context: Context
    private lateinit var registrar: FirebaseUIComposeRegistrar

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        registrar = FirebaseUIComposeRegistrar()
    }

    // =============================================================================================
    // Component Registration Tests
    // =============================================================================================

    @Test
    fun `getComponents() returns exactly one component`() {
        val components = registrar.getComponents()

        assertThat(components).hasSize(1)
    }

    // =============================================================================================
    // AndroidManifest.xml Configuration Tests
    // =============================================================================================

    @Test
    fun `ComponentDiscoveryService has correct meta-data for FirebaseUIComposeRegistrar`() {
        val packageInfo = context.packageManager.getPackageInfo(
            context.packageName,
            PackageManager.GET_SERVICES or PackageManager.GET_META_DATA
        )

        val service = packageInfo.services?.find { serviceInfo ->
            serviceInfo.name == "com.google.firebase.components.ComponentDiscoveryService"
        }

        assertThat(service).isNotNull()

        val metaData = service?.metaData
        assertThat(metaData).isNotNull()

        // Check the meta-data key contains the fully qualified class name
        val expectedMetaDataKey = "com.google.firebase.components:com.firebase.ui.auth.FirebaseUIComposeRegistrar"
        val metaDataValue = metaData?.getString(expectedMetaDataKey)

        assertThat(metaDataValue).isEqualTo("com.google.firebase.components.ComponentRegistrar")
    }

    // =============================================================================================
    // Class Metadata Tests
    // =============================================================================================

    @Test
    fun `meta-data key matches actual class fully qualified name`() {
        // The expected meta-data key format used in AndroidManifest.xml
        val expectedMetaDataKey = "com.google.firebase.components:com.firebase.ui.auth.FirebaseUIComposeRegistrar"
        val actualClassName = FirebaseUIComposeRegistrar::class.java.name

        // Extract the class name from the meta-data key (after the colon)
        val classNameInMetaData = expectedMetaDataKey.substringAfter(":")

        assertThat(classNameInMetaData).isEqualTo(actualClassName)
    }

    @Test
    fun `registrar implements ComponentRegistrar interface`() {
        val interfaces = FirebaseUIComposeRegistrar::class.java.interfaces

        assertThat(interfaces.map { it.name }).contains("com.google.firebase.components.ComponentRegistrar")
    }
}
