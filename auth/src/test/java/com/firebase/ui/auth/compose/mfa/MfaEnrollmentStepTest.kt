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

import org.junit.Assert.assertEquals
import org.junit.Test

class MfaEnrollmentStepTest {

    @Test
    fun `enum has all expected values`() {
        val values = MfaEnrollmentStep.entries.toTypedArray()

        assertEquals(5, values.size)
        assertEquals(MfaEnrollmentStep.SelectFactor, values[0])
        assertEquals(MfaEnrollmentStep.ConfigureSms, values[1])
        assertEquals(MfaEnrollmentStep.ConfigureTotp, values[2])
        assertEquals(MfaEnrollmentStep.VerifyFactor, values[3])
        assertEquals(MfaEnrollmentStep.ShowRecoveryCodes, values[4])
    }

    @Test
    fun `valueOf works correctly`() {
        assertEquals(MfaEnrollmentStep.SelectFactor, MfaEnrollmentStep.valueOf("SelectFactor"))
        assertEquals(MfaEnrollmentStep.ConfigureSms, MfaEnrollmentStep.valueOf("ConfigureSms"))
        assertEquals(MfaEnrollmentStep.ConfigureTotp, MfaEnrollmentStep.valueOf("ConfigureTotp"))
        assertEquals(MfaEnrollmentStep.VerifyFactor, MfaEnrollmentStep.valueOf("VerifyFactor"))
        assertEquals(MfaEnrollmentStep.ShowRecoveryCodes, MfaEnrollmentStep.valueOf("ShowRecoveryCodes"))
    }

    @Test
    fun `enum ordinals are in expected order`() {
        assertEquals(0, MfaEnrollmentStep.SelectFactor.ordinal)
        assertEquals(1, MfaEnrollmentStep.ConfigureSms.ordinal)
        assertEquals(2, MfaEnrollmentStep.ConfigureTotp.ordinal)
        assertEquals(3, MfaEnrollmentStep.VerifyFactor.ordinal)
        assertEquals(4, MfaEnrollmentStep.ShowRecoveryCodes.ordinal)
    }
}
