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

package com.firebase.ui.auth.ui.screens

import androidx.compose.runtime.snapshots.Snapshot
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The contract [resetBackStackTo] has to satisfy, asserted against the helper directly rather than
 * through a host, so each clause fails on its own.
 *
 * Every reset in [FirebaseAuthScreen] and the reauthentication sheet goes through this one
 * function — success, cancellation, a genuine idle, and the sheet's return to provider selection.
 * A reset that quietly did nothing would leave a just-failed form underneath the destination it
 * navigated to, where system back returns a signed-in user to it.
 *
 * The two helpers that go with it are asserted here too, for the same reason — [pushUnique], which
 * is what keeps two `==` keys off one stack, and [isAt], which is the argument-blind destination
 * test the reset guards ask.
 *
 * @suppress Internal test class
 */
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [34])
class ResetBackStackToTest {

    /**
     * Clause 1: an absolute postcondition. Not "pop until you find it" — whatever the stack held,
     * afterwards it holds exactly the one entry.
     */
    @Test
    fun `a reset always leaves exactly the target entry`() {
        listOf(
            listOf(AuthRoute.MethodPicker),
            listOf(AuthRoute.Email.SignIn()),
            listOf(AuthRoute.MethodPicker, AuthRoute.Email.SignIn("a@b.com")),
            listOf(
                AuthRoute.MethodPicker,
                AuthRoute.Email.SignIn("a@b.com"),
                AuthRoute.Email.SignUp("a@b.com"),
                AuthRoute.Email.ResetPassword("a@b.com"),
            ),
            listOf(AuthRoute.Success, AuthRoute.MfaEnrollment.VerifyFactor),
        ).forEach { initial ->
            val stack = stackOf(initial)

            stack.resetBackStackTo(AuthRoute.Success)

            assertThat(stack.toList()).containsExactly(AuthRoute.Success)
        }
    }

    /**
     * Clause 2: it can never be a no-op. The hazard is `indexOf(key)` returning `-1`, so a stack
     * holding nothing resembling the target must still end up holding exactly the target — and a
     * stack that *already* holds it, buried under other entries, must lose those.
     */
    @Test
    fun `a reset is never a no-op, whether or not the target is already on the stack`() {
        val targetAbsent = stackOf(
            listOf(AuthRoute.Email.SignIn(), AuthRoute.Email.SignUp())
        )
        targetAbsent.resetBackStackTo(AuthRoute.MethodPicker)
        assertThat(targetAbsent.toList()).containsExactly(AuthRoute.MethodPicker)

        val targetBuried = stackOf(
            listOf(AuthRoute.MethodPicker, AuthRoute.Email.SignIn(), AuthRoute.Success)
        )
        targetBuried.resetBackStackTo(AuthRoute.MethodPicker)
        assertThat(targetBuried.toList()).containsExactly(AuthRoute.MethodPicker)

        // Already exactly the target: still one entry afterwards, not two.
        val targetOnly = stackOf(listOf(AuthRoute.MethodPicker))
        targetOnly.resetBackStackTo(AuthRoute.MethodPicker)
        assertThat(targetOnly.toList()).containsExactly(AuthRoute.MethodPicker)
    }

    /**
     * Clause 3: the stack is never empty at any single snapshot write.
     *
     * `clear()` then `add()` is the evil twin — the same end state, but with a point in the middle
     * at which stopping leaves the stack empty, and `NavDisplay` throws `IllegalArgumentException:
     * NavDisplay backstack cannot be empty` from recomposition, far from this call.
     *
     * Deliberately *not* claimed: that a recomposition would catch the intermediate. It cannot —
     * nothing yields between two back-to-back snapshot writes in one synchronous helper, which is
     * the same fact [pushUnique] relies on to hold two `==` keys between its `add` and its trim.
     * What is asserted is the code's invariant, by observing every snapshot write the reset
     * performs and recording the stack's size at each one, so the ordering is pinned rather than
     * inferred from the end state.
     */
    @Test
    fun `a reset never leaves the stack empty, even momentarily`() {
        val stack = stackOf(
            listOf(
                AuthRoute.MethodPicker,
                AuthRoute.Email.SignIn("a@b.com"),
                AuthRoute.Email.SignUp("a@b.com"),
            )
        )
        val sizes = mutableListOf<Int>()

        Snapshot.observe(writeObserver = { sizes += stack.size }) {
            stack.resetBackStackTo(AuthRoute.Success)
        }

        assertThat(sizes).isNotEmpty()
        assertThat(sizes.min()).isAtLeast(1)
        assertThat(stack.toList()).containsExactly(AuthRoute.Success)
    }

    /**
     * Clause 4: a flow entry point resolves to its start step. Two callers pass `startRoute`, which
     * [getStartRoute] can return as [AuthRoute.Email] or [AuthRoute.Phone] — flow entry points,
     * which are not registered destinations.
     */
    @Test
    fun `a reset to a flow entry point pushes that flow's start step`() {
        val toEmail = stackOf(listOf(AuthRoute.Success))
        toEmail.resetBackStackTo(AuthRoute.Email)
        assertThat(toEmail.toList()).containsExactly(AuthRoute.Email.SignIn())

        val toPhone = stackOf(listOf(AuthRoute.Success))
        toPhone.resetBackStackTo(AuthRoute.Phone)
        assertThat(toPhone.toList()).containsExactly(AuthRoute.Phone.EnterPhoneNumber)

        val toMfa = stackOf(listOf(AuthRoute.Success))
        toMfa.resetBackStackTo(AuthRoute.MfaEnrollment)
        assertThat(toMfa.toList()).containsExactly(AuthRoute.MfaEnrollment.SelectFactor)
    }

    /**
     * Clause 5, the half this file can actually assert: the *key* left on the stack is the newly
     * constructed one, not the one that was already there. The `Cancelled` and `Idle` branches
     * clear the typed address and then reset, expecting a blank start step — a surviving
     * `SignIn("stale@example.com")` would hand the next session the previous user's address.
     *
     * The other half of clause 5 — whether the *composition state* behind that key is fresh — is
     * conditional (it is only fresh when the pushed key differs from every key already on the
     * stack) and is not observable here: this test drives the bare list, with no `NavDisplay` and
     * so no `SaveableStateHolder` to restore anything. `FirebaseAuthScreenEmailRecoveryTest`'s
     * `a cancellation on the start step keeps the address the recovery carried` covers it
     * end-to-end, in the direction the product actually depends on.
     */
    @Test
    fun `a reset to the email flow drops the address the old entry carried`() {
        val stack = stackOf(
            listOf(
                AuthRoute.Email.SignIn("stale@example.com"),
                AuthRoute.Email.SignUp("stale@example.com"),
            )
        )

        stack.resetBackStackTo(AuthRoute.Email)

        assertThat(stack.toList()).containsExactly(AuthRoute.Email.SignIn(null))
    }

    // =============================================================================================
    // pushUnique — two keys that are == are one entry, so a duplicate must never reach the stack
    // =============================================================================================

    /**
     * A push that finds its key already on top must leave the stack unchanged in shape. Two taps
     * in one frame on any host callback that pushes is the reachable case, and the redundant entry
     * it would add is a duplicate content key.
     */
    @Test
    fun `pushing the key already on top does not duplicate it`() {
        val stack = stackOf(listOf(AuthRoute.MethodPicker, AuthRoute.MfaChallenge))

        repeat(3) { stack.pushUnique(AuthRoute.MfaChallenge) }

        assertThat(stack.toList())
            .containsExactly(AuthRoute.MethodPicker, AuthRoute.MfaChallenge)
            .inOrder()
    }

    /**
     * The buried case: the requested destination ends up on top as the *same* entry rather than a
     * second copy, so no two keys on the stack are ever `==`, and the entries that sat above its
     * earlier position go with it. A `NavBackStack` is a plain list and could hold the second copy,
     * but not safely, so the trim is deliberate. See [pushUnique]'s KDoc for why, and for the
     * caller-side invariants that keep this case unreachable at every current call site.
     */
    @Test
    fun `pushing a key buried on the stack moves it to the top rather than copying it`() {
        val stack = stackOf(
            listOf(
                AuthRoute.Email.SignIn(),
                AuthRoute.MfaChallenge,
                AuthRoute.Email.SignUp("a@b.com"),
            )
        )

        stack.pushUnique(AuthRoute.MfaChallenge)

        assertThat(stack.toList()).containsExactly(AuthRoute.Email.SignIn(), AuthRoute.MfaChallenge)
            .inOrder()
        assertThat(stack.toList()).containsNoDuplicates()
    }

    /** A key that is genuinely new is simply pushed, leaving what was there underneath. */
    @Test
    fun `pushing an absent key leaves everything underneath it`() {
        val stack = stackOf(listOf(AuthRoute.MethodPicker))

        stack.pushUnique(AuthRoute.Phone)

        assertThat(stack.toList())
            .containsExactly(AuthRoute.MethodPicker, AuthRoute.Phone.EnterPhoneNumber)
            .inOrder()
    }

    /** Like [resetBackStackTo], a push resolves a flow entry point rather than pushing it. */
    @Test
    fun `pushing a flow entry point pushes that flow's start step`() {
        val stack = stackOf(listOf(AuthRoute.MethodPicker))

        stack.pushUnique(AuthRoute.MfaEnrollment)

        assertThat(stack.last()).isEqualTo(AuthRoute.MfaEnrollment.SelectFactor)
    }

    /**
     * The whole point, stated as the invariant rather than as a case list: whatever sequence of
     * pushes the flow performs, no two keys on the stack are ever equal — because equal keys
     * produce equal `NavEntry.contentKey`s, and `SaveableStateHolder.SaveableStateProvider`
     * throws `IllegalArgumentException: Key … was used multiple times` on the second one.
     */
    @Test
    fun `no sequence of pushes can put two equal keys on the stack`() {
        val stack = stackOf(listOf(AuthRoute.MethodPicker))
        val pushes = listOf(
            AuthRoute.Email,
            AuthRoute.MfaChallenge,
            AuthRoute.Email,
            AuthRoute.MfaEnrollment,
            AuthRoute.MfaEnrollment.VerifyFactor,
            AuthRoute.MfaEnrollment.VerifyFactor,
            AuthRoute.MfaChallenge,
            AuthRoute.Success,
            AuthRoute.Success,
            AuthRoute.Phone,
        )

        pushes.forEach { route ->
            stack.pushUnique(route)

            assertThat(stack.toList()).containsNoDuplicates()
            assertThat(stack.last()).isEqualTo(route.toKey())
            assertThat(stack.size).isAtLeast(1)
        }
    }

    // =============================================================================================
    // isAt — the argument-blind destination test the reset guards ask
    // =============================================================================================

    /**
     * `isAt` is what the reset guards ask, and it has to ignore whatever a key carries: a `SignIn`
     * entry holding an address is still "at" the email flow's start step. A bare `==` on keys is
     * finer than that and would fire the guard.
     */
    @Test
    fun `isAt ignores the address a step carries`() {
        assertThat(AuthRoute.Email.SignIn("bob@example.com").isAt(AuthRoute.Email)).isTrue()
        assertThat(AuthRoute.Email.SignIn().isAt(AuthRoute.Email)).isTrue()
        assertThat(AuthRoute.Email.SignIn("bob@example.com").isAt(AuthRoute.Email.SignIn()))
            .isTrue()
    }

    @Test
    fun `isAt still separates different destinations, and null is at nothing`() {
        assertThat(AuthRoute.Email.SignUp("bob@example.com").isAt(AuthRoute.Email)).isFalse()
        assertThat(AuthRoute.MethodPicker.isAt(AuthRoute.Email)).isFalse()
        assertThat(AuthRoute.Success.isAt(AuthRoute.MethodPicker)).isFalse()
        assertThat(AuthRoute.MethodPicker.isAt(AuthRoute.MethodPicker)).isTrue()
        assertThat(null.isAt(AuthRoute.Email)).isFalse()
        assertThat(null.isAt(AuthRoute.MethodPicker)).isFalse()
    }

    private fun stackOf(keys: List<NavKey>): NavBackStack<NavKey> =
        NavBackStack<NavKey>().apply { addAll(keys) }
}
