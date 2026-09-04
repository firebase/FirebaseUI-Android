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

import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.NavMetadataKey
import androidx.navigation3.runtime.get
import androidx.navigation3.runtime.metadata
import androidx.navigation3.scene.Scene
import com.firebase.ui.auth.mfa.MfaEnrollmentStep
import com.firebase.ui.auth.ui.screens.email.EmailAuthMode
import com.firebase.ui.auth.ui.screens.phone.PhoneAuthStep
import kotlinx.serialization.Serializable

/**
 * A destination the library's navigation back stack can be told to go to.
 *
 * * **[Destination]** — a real back-stack entry, and therefore a Navigation 3 [NavKey]. Every one
 *   is registered on both hosts' entry providers, so navigating to it always resolves. All are
 *   `@Serializable`, which is what lets [androidx.navigation3.runtime.rememberNavBackStack]
 *   persist the stack — and any field a step carries, today only [Email.Step.email] — across
 *   configuration change *and* process death.
 * * **[FlowEntry]** — [Email], [Phone] and [MfaEnrollment]. Naming one means "enter this flow";
 *   [FlowEntry.startKey] resolves it to the step the flow opens on, so callers never have to name
 *   a step. Deliberately **not** a [NavKey]: it can never be put on a back stack, and [toKey] is
 *   the one way to turn it into something that can be.
 *
 * @since 10.0.0
 */
@Serializable
sealed interface AuthRoute {

    /** An [AuthRoute] that is a real destination, and therefore a Navigation 3 back-stack key. */
    @Serializable
    sealed interface Destination : AuthRoute, NavKey

    /**
     * An [AuthRoute] that names a *flow* rather than a destination. [startKey] converts it to the
     * destination entering the flow lands on; [toKey] applies that to any [AuthRoute].
     */
    sealed interface FlowEntry : AuthRoute {
        /** The destination entering this flow lands on. */
        fun startKey(): Destination
    }

    @Serializable
    data object MethodPicker : Destination

    @Serializable
    data object Success : Destination

    @Serializable
    data object MfaChallenge : Destination

    /**
     * Reauthentication, as an entry on the host's own back stack rather than a separate surface.
     *
     * Wraps the [step] it presents instead of duplicating the destination hierarchy: the same key
     * type in the same stack cannot mean two configurations, and a wrapper is the cheapest way to
     * say "this step, but in reauthentication mode".
     *
     * [requestId] and [userUid] make the entry the presentation marker itself, which is why nothing else
     * has to be saved alongside the stack.
     */
    @Serializable
    data class Reauth(
        val requestId: String,
        val userUid: String,
        val step: Destination,
    ) : Destination

    /** Email and password, password recovery, and email-link sign-in. Starts at [SignIn]. */
    object Email : FlowEntry {
        override fun startKey(): Destination = SignIn()

        /**
         * One step per [EmailAuthMode], carrying the address typed so far as a field on the key,
         * which is what preserves it across a switch: the step being left is disposed along with
         * everything it held in composition state.
         *
         * Two instances of the same step with different addresses are **different keys**, so
         * [com.firebase.ui.auth.ui.screens.email.navigateToEmailStep] asks "already on the stack?"
         * of the step's *type*, not of the key.
         */
        @Serializable
        sealed interface Step : Destination {
            /** The address this step was entered with, or null. */
            val email: String?

            /** This step carrying [email] instead. */
            fun withEmail(email: String?): Step
        }

        @Serializable
        data class SignIn(override val email: String? = null) : Step {
            override fun withEmail(email: String?): Step = copy(email = email)
        }

        @Serializable
        data class SignUp(override val email: String? = null) : Step {
            override fun withEmail(email: String?): Step = copy(email = email)
        }

        @Serializable
        data class ResetPassword(override val email: String? = null) : Step {
            override fun withEmail(email: String?): Step = copy(email = email)
        }

        @Serializable
        data class EmailLinkSignIn(override val email: String? = null) : Step {
            override fun withEmail(email: String?): Step = copy(email = email)
        }

        /** The flow's start step carrying [email]. */
        fun startKey(email: String?): Step = SignIn(email)

        internal val steps: List<Step>
            get() = listOf(SignIn(), SignUp(), ResetPassword(), EmailLinkSignIn())

        internal fun stepFor(mode: EmailAuthMode, email: String? = null): Step = when (mode) {
            EmailAuthMode.SignIn -> SignIn(email)
            EmailAuthMode.SignUp -> SignUp(email)
            EmailAuthMode.ResetPassword -> ResetPassword(email)
            EmailAuthMode.EmailLinkSignIn -> EmailLinkSignIn(email)
        }

        /** Whether [key] — a live back-stack key — belongs to this flow. */
        internal fun isStep(key: NavKey?): Boolean = key is Step
    }

    /** Phone number verification. Starts at [EnterPhoneNumber]. */
    object Phone : FlowEntry {
        override fun startKey(): Destination = EnterPhoneNumber

        /**
         * One step per screen the phone flow walks through. The internal
         * `AuthRoute.Phone.Step.phoneStep` extension maps a live key to the [PhoneAuthStep]
         * [com.firebase.ui.auth.ui.screens.phone.PhoneAuthScreen] renders.
         */
        @Serializable
        sealed interface Step : Destination

        @Serializable
        data object EnterPhoneNumber : Step

        @Serializable
        data object EnterVerificationCode : Step

        internal val steps: List<Step>
            get() = listOf(EnterPhoneNumber, EnterVerificationCode)

        internal fun stepFor(phoneStep: PhoneAuthStep): Step =
            steps.first { it.phoneStep == phoneStep }
    }

    /**
     * Second-factor enrolment. Starts at [SelectFactor], or straight at the only allowed factor's
     * step — resolved at flow entry by
     * [com.firebase.ui.auth.ui.screens.mfa.mfaEnrollmentStartStep].
     */
    object MfaEnrollment : FlowEntry {
        override fun startKey(): Destination = SelectFactor

        /**
         * One step per screen the enrolment flow walks through. The internal
         * `AuthRoute.MfaEnrollment.Step.enrollmentStep` extension maps a live key to the
         * [MfaEnrollmentStep] [com.firebase.ui.auth.ui.screens.mfa.MfaEnrollmentScreen] renders.
         */
        @Serializable
        sealed interface Step : Destination

        @Serializable
        data object SelectFactor : Step

        @Serializable
        data object ConfigureSms : Step

        @Serializable
        data object ConfigureTotp : Step

        @Serializable
        data object VerifyFactor : Step

        internal val steps: List<Step>
            get() = listOf(SelectFactor, ConfigureSms, ConfigureTotp, VerifyFactor)

        internal fun stepFor(enrollmentStep: MfaEnrollmentStep): Step =
            steps.first { it.enrollmentStep == enrollmentStep }
    }
}

/** Which [EmailAuthMode] the hosted screen renders for this step. */
internal val AuthRoute.Email.Step.mode: EmailAuthMode
    get() = when (this) {
        is AuthRoute.Email.SignIn -> EmailAuthMode.SignIn
        is AuthRoute.Email.SignUp -> EmailAuthMode.SignUp
        is AuthRoute.Email.ResetPassword -> EmailAuthMode.ResetPassword
        is AuthRoute.Email.EmailLinkSignIn -> EmailAuthMode.EmailLinkSignIn
    }

/** Which [PhoneAuthStep] the hosted screen renders for this step. */
internal val AuthRoute.Phone.Step.phoneStep: PhoneAuthStep
    get() = when (this) {
        AuthRoute.Phone.EnterPhoneNumber -> PhoneAuthStep.EnterPhoneNumber
        AuthRoute.Phone.EnterVerificationCode -> PhoneAuthStep.EnterVerificationCode
    }

/** Which [MfaEnrollmentStep] the hosted screen renders for this step. */
internal val AuthRoute.MfaEnrollment.Step.enrollmentStep: MfaEnrollmentStep
    get() = when (this) {
        AuthRoute.MfaEnrollment.SelectFactor -> MfaEnrollmentStep.SelectFactor
        AuthRoute.MfaEnrollment.ConfigureSms -> MfaEnrollmentStep.ConfigureSms
        AuthRoute.MfaEnrollment.ConfigureTotp -> MfaEnrollmentStep.ConfigureTotp
        AuthRoute.MfaEnrollment.VerifyFactor -> MfaEnrollmentStep.VerifyFactor
    }

/**
 * Every value a caller can hand the back stack, flow entry points included. Built from the same
 * per-flow `steps` lists the hosts register from.
 */
internal val allAuthRoutes: List<AuthRoute>
    get() = listOf(AuthRoute.MethodPicker, AuthRoute.Success, AuthRoute.MfaChallenge) +
            listOf(AuthRoute.Email) + AuthRoute.Email.steps +
            listOf(AuthRoute.Phone) + AuthRoute.Phone.steps +
            listOf(AuthRoute.MfaEnrollment) + AuthRoute.MfaEnrollment.steps

/**
 * Resolves [this] to the key to actually push: a [AuthRoute.FlowEntry]'s start step, or the
 * destination itself. Every push goes through this, or it would push a key nothing registered.
 */
internal fun AuthRoute.toKey(): AuthRoute.Destination = when (this) {
    is AuthRoute.FlowEntry -> startKey()
    is AuthRoute.Destination -> this
}

/**
 * Whether [this] — a live back-stack key, or null for an empty stack — is *at* [route], ignoring
 * any argument the key carries.
 *
 * Compares runtime classes, not keys: `SignIn("bob@x.com") != SignIn(null)` as keys, but both are
 * the same destination, which is what makes "is the flow already on its start step?" independent
 * of what has been typed into it.
 */
internal fun NavKey?.isAt(route: AuthRoute): Boolean =
    this != null && this::class == route.toKey()::class

/**
 * Sends the flow back to [route] as the only thing on the back stack: exactly one entry, and it is
 * [route]'s resolved key, whatever the stack held before.
 *
 * Adds before trimming, so no single write leaves the stack empty. The pushed key is newly
 * constructed, but a key `==` to one already on the stack keeps its saved composition state — do
 * not rely on a reset to blank a form.
 */
internal fun NavBackStack<NavKey>.resetBackStackTo(route: AuthRoute) {
    add(route.toKey())
    while (size > 1) removeAt(0)
}

/**
 * Pushes [route]'s key, guaranteeing it ends up on top **exactly once** — two keys that are `==`
 * are one entry to Navigation 3, and a duplicate either crashes inside `runtime-saveable` or
 * silently shares one instance of the screen.
 *
 * A push whose key is already on the stack therefore **moves** it, dropping everything that was
 * above it: `[A,B]` push `B` → `[A,B]`, and `[A,B,C]` push `B` → `[A,B]`. Every call site relies on
 * the target never being buried, so that trim never happens today; a new one must uphold that or
 * accept the trim.
 */
internal fun NavBackStack<NavKey>.pushUnique(route: AuthRoute) {
    val existing = indexOf(route.toKey())
    add(route.toKey())
    if (existing >= 0) {
        while (size > existing + 1) removeAt(existing)
    }
}

/**
 * Pops one entry, unless that would empty the stack. Returns whether anything was popped. The
 * guard is needed because `NavDisplay` throws on an empty back stack, and throws from
 * recomposition rather than from the call that emptied it, so it cannot be caught at the call site.
 */
internal fun NavBackStack<NavKey>.popOrNull(): Boolean =
    if (size > 1) {
        removeAt(size - 1)
        true
    } else {
        false
    }

/**
 * Metadata slot the library stamps its own key into on every entry it registers, so that a
 * [Scene] can be asked which [AuthRoute] it is showing — see [authRoute].
 *
 * @since 10.0.0
 */
object AuthRouteMetadataKey : NavMetadataKey<AuthRoute>

/** Per-key metadata stamping [route] into [AuthRouteMetadataKey]. */
internal fun authRouteMetadata(route: AuthRoute): Map<String, Any> =
    metadata { put(AuthRouteMetadataKey, route) }

/**
 * The [AuthRoute] this [Scene] is showing, or `null` for a scene the library did not register.
 *
 * This is what a [com.firebase.ui.auth.configuration.AuthUITransitions] lambda reads off
 * `initialState` / `targetState` to vary the animation per destination:
 *
 * ```kotlin
 * AuthUITransitions(
 *     transitionSpec = {
 *         if (targetState.authRoute() is AuthRoute.Success) {
 *             fadeIn() togetherWith fadeOut()
 *         } else {
 *             slideInHorizontally { it } togetherWith slideOutHorizontally { -it }
 *         }
 *     },
 * )
 * ```
 *
 * Read from the entry metadata the library stamps itself (see [AuthRouteMetadataKey]); prefer it
 * over [Scene.key], which mid-transition still reports the outgoing destination. A scene showing
 * several entries reports the topmost.
 *
 * @since 10.0.0
 */
fun Scene<NavKey>.authRoute(): AuthRoute? =
    entries.lastOrNull()?.metadata?.get(AuthRouteMetadataKey)
