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

package com.firebase.ui.auth.ui.screens.email

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.unit.dp
import com.firebase.ui.auth.configuration.password_policy.PasswordPolicy
import com.firebase.ui.auth.configuration.password_policy.PasswordPolicyRepository
import com.firebase.ui.auth.configuration.string_provider.AuthUIStringProvider
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/** Supplies the project password policy to [SignUpUI]. Must return `null` rather than throw. */
internal fun interface PasswordPolicySource {
    suspend fun getPolicy(context: Context): PasswordPolicy?

    /** Emits on collection and again whenever [getPolicy] may return a newer policy. */
    val revision: Flow<Int> get() = flowOf(0)
}

/**
 * Activation switch for the sign-up password requirements checklist. `null`, the default, keeps
 * it off: nothing is fetched and [SignUpUI] renders as before.
 */
internal val LocalPasswordPolicySource = staticCompositionLocalOf<PasswordPolicySource?> { null }

/** A [PasswordPolicySource] for [auth]'s app and tenant, cached by this repository. */
internal fun PasswordPolicyRepository.sourceFor(auth: FirebaseAuth): PasswordPolicySource {
    val repository = this
    return object : PasswordPolicySource {
        override suspend fun getPolicy(context: Context) = repository.getPolicy(context, auth)
        override val revision: Flow<Int> = repository.revision(auth)
    }
}

/** One checklist line. [symbols] is set only on the special character line. */
internal data class PasswordRequirement(
    val label: String,
    val isMet: Boolean,
    val symbols: String? = null,
)

/**
 * The requirements [policy] configures, in the JS SDK's order, evaluated against [password]. Max
 * length is not shown: its only string reads as an error even when met.
 */
internal fun passwordRequirements(
    policy: PasswordPolicy,
    password: String,
    stringProvider: AuthUIStringProvider,
): List<PasswordRequirement> {
    val status = policy.validate(password)
    return buildList {
        add(
            PasswordRequirement(
                stringProvider.passwordTooShort(policy.minPasswordLength),
                status.meetsMinPasswordLength,
            )
        )
        status.containsLowercaseLetter?.let {
            add(PasswordRequirement(stringProvider.passwordMissingLowercase, it))
        }
        status.containsUppercaseLetter?.let {
            add(PasswordRequirement(stringProvider.passwordMissingUppercase, it))
        }
        status.containsNumericCharacter?.let {
            add(PasswordRequirement(stringProvider.passwordMissingDigit, it))
        }
        status.containsNonAlphanumericCharacter?.let {
            val symbols = policy.allowedNonAlphanumericCharacters
                .flatMap { it.codePointSymbols() }
                .distinct()
                .joinToString(" ")
            add(
                PasswordRequirement(
                    stringProvider.passwordMissingSpecialCharacter,
                    it,
                    symbols.ifEmpty { null },
                )
            )
        }
    }
}

/** Each code point of this string as its own string, so non-BMP symbols stay intact. */
private fun String.codePointSymbols(): List<String> {
    val symbols = mutableListOf<String>()
    var i = 0
    while (i < length) {
        val end = offsetByCodePoints(i, 1)
        symbols += substring(i, end)
        i = end
    }
    return symbols
}

/**
 * Live checklist of [requirements], from [passwordRequirements]. Hints only: it never affects
 * whether sign-up can be submitted. Each line exposes met/unmet as a toggleable state, so it is
 * not conveyed by colour.
 */
@Composable
internal fun PasswordRequirementsChecklist(
    requirements: List<PasswordRequirement>,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        requirements.forEach { requirement ->
            val tint = if (requirement.isMet) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            }
            Row(
                modifier = Modifier.semantics(mergeDescendants = true) {
                    toggleableState = ToggleableState(requirement.isMet)
                },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = if (requirement.isMet) {
                        Icons.Filled.CheckCircle
                    } else {
                        Icons.Outlined.RadioButtonUnchecked
                    },
                    contentDescription = null,
                    tint = tint,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = requirement.label,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    requirement.symbols?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}
