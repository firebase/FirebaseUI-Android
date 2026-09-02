package com.firebase.ui.auth.ui.screens

import android.content.Context
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import com.firebase.ui.auth.configuration.authUIConfiguration
import com.firebase.ui.auth.configuration.auth_provider.AuthProvider
import com.firebase.ui.auth.ui.screens.email.EmailAuthMode
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.reflect.KClass

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class FirebaseAuthScreenRouteTest {

    private lateinit var applicationContext: Context

    @Before
    fun setUp() {
        applicationContext = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun `single email provider starts at email route`() {
        val configuration = authUIConfiguration {
            context = applicationContext
            providers {
                provider(
                    AuthProvider.Email(
                        emailLinkActionCodeSettings = null,
                        passwordValidationRules = emptyList()
                    )
                )
            }
        }

        assertThat(getStartRoute(configuration)).isEqualTo(AuthRoute.Email)
    }

    @Test
    fun `single phone provider starts at phone route`() {
        val configuration = authUIConfiguration {
            context = applicationContext
            providers {
                provider(
                    AuthProvider.Phone(
                        defaultNumber = null,
                        defaultCountryCode = null,
                        allowedCountries = null
                    )
                )
            }
        }

        assertThat(getStartRoute(configuration)).isEqualTo(AuthRoute.Phone)
    }

    @Test
    fun `single google provider starts at method picker`() {
        val configuration = authUIConfiguration {
            context = applicationContext
            providers {
                provider(
                    AuthProvider.Google(
                        scopes = emptyList(),
                        serverClientId = "test-client-id"
                    )
                )
            }
        }

        assertThat(getStartRoute(configuration)).isEqualTo(AuthRoute.MethodPicker)
    }

    @Test
    fun `single email provider shows picker when always shown is enabled`() {
        val configuration = authUIConfiguration {
            context = applicationContext
            providers {
                provider(
                    AuthProvider.Email(
                        emailLinkActionCodeSettings = null,
                        passwordValidationRules = emptyList()
                    )
                )
            }
            isProviderChoiceAlwaysShown = true
        }

        assertThat(getStartRoute(configuration)).isEqualTo(AuthRoute.MethodPicker)
    }

    @Test
    fun `multiple providers start at method picker`() {
        val configuration = authUIConfiguration {
            context = applicationContext
            providers {
                provider(
                    AuthProvider.Email(
                        emailLinkActionCodeSettings = null,
                        passwordValidationRules = emptyList()
                    )
                )
                provider(
                    AuthProvider.Phone(
                        defaultNumber = null,
                        defaultCountryCode = null,
                        allowedCountries = null
                    )
                )
            }
        }

        assertThat(getStartRoute(configuration)).isEqualTo(AuthRoute.MethodPicker)
    }

    // AuthRoute shape — public API.

    /**
     * Naming a flow means "enter this flow", so its route has to be its start step's — both
     * `route`, which callers navigate with, and `routePattern`, which the graph registers.
     */
    @Test
    fun `every flow entry point resolves to its start step`() {
        assertThat(AuthRoute.Email.route).isEqualTo(AuthRoute.Email.SignIn.route)
        assertThat(AuthRoute.Email.routePattern).isEqualTo(AuthRoute.Email.SignIn.routePattern)

        assertThat(AuthRoute.Phone.route).isEqualTo(AuthRoute.Phone.EnterPhoneNumber.route)
        assertThat(AuthRoute.Phone.routePattern)
            .isEqualTo(AuthRoute.Phone.EnterPhoneNumber.routePattern)

        assertThat(AuthRoute.MfaEnrollment.route)
            .isEqualTo(AuthRoute.MfaEnrollment.SelectFactor.route)
        assertThat(AuthRoute.MfaEnrollment.routePattern)
            .isEqualTo(AuthRoute.MfaEnrollment.SelectFactor.routePattern)
    }

    /** Only the email steps take an argument, so only they may differ from their pattern. */
    @Test
    fun `routePattern equals route for every destination that takes no argument`() {
        val argumentless = listOf(
            AuthRoute.MethodPicker,
            AuthRoute.Success,
            AuthRoute.MfaChallenge,
            AuthRoute.Phone,
            AuthRoute.Phone.EnterPhoneNumber,
            AuthRoute.Phone.EnterVerificationCode,
            AuthRoute.MfaEnrollment.SelectFactor,
            AuthRoute.MfaEnrollment.ConfigureSms,
            AuthRoute.MfaEnrollment.ConfigureTotp,
            AuthRoute.MfaEnrollment.VerifyFactor,
        )

        argumentless.forEach { route ->
            assertThat(route.routePattern).isEqualTo(route.route)
        }
    }

    @Test
    fun `every email step registers the optional email argument`() {
        AuthRoute.Email.steps.forEach { step ->
            assertThat(step.routePattern).isEqualTo("${step.route}?$EMAIL_ARG={$EMAIL_ARG}")
        }
    }

    /**
     * The step lists must be computed getters: stored, they would be built before the nested
     * objects were assigned and capture them as null. Reading a route off each entry catches that.
     */
    @Test
    fun `the email step list is fully populated and distinct`() {
        val routes = AuthRoute.Email.steps.map { it.route }

        assertThat(routes).hasSize(EmailAuthMode.entries.size)
        assertThat(routes).containsNoDuplicates()
        routes.forEach { route -> assertThat(route).isNotEmpty() }
    }

    @Test
    fun `every email mode maps to exactly one step`() {
        EmailAuthMode.entries.forEach { mode ->
            assertThat(AuthRoute.Email.stepFor(mode).mode).isEqualTo(mode)
        }
    }

    @Test
    fun `withEmail appends an encoded address and omits an empty one`() {
        val step = AuthRoute.Email.SignUp

        assertThat(step.withEmail(null)).isEqualTo(step.route)
        assertThat(step.withEmail("")).isEqualTo(step.route)
        assertThat(step.withEmail("user+tag@example.com"))
            .isEqualTo("${step.route}?$EMAIL_ARG=user%2Btag%40example.com")
    }

    /**
     * The address goes into a URI query string, so anything a local part or domain may legally
     * hold has to come back out unchanged — a raw `#` truncates the URI, a raw space invalidates it.
     */
    @Test
    fun `withEmail encodes every address so the argument round-trips`() {
        val step = AuthRoute.Email.SignUp

        listOf(
            "user+tag@example.com",
            "user name@example.com",
            "user#hash@example.com",
            "user?query=1@example.com",
            "user&more@example.com",
            "ada@königsberg.example",
            "用户@例え.jp",
        ).forEach { address ->
            val encoded = step.withEmail(address)

            assertThat(encoded).startsWith("${step.route}?$EMAIL_ARG=")
            // The encoded form is what travels; nothing that would break the URI survives in it.
            assertThat(encoded.substringAfter('=')).doesNotContain("#")
            assertThat(encoded).doesNotContain(" ")
            assertThat(Uri.parse(encoded).getQueryParameters(EMAIL_ARG))
                .containsExactly(address)
        }
    }

    @Test
    fun `isStep recognises the email steps and nothing else`() {
        AuthRoute.Email.steps.forEach { step ->
            assertThat(AuthRoute.Email.isStep(step.routePattern)).isTrue()
        }

        assertThat(AuthRoute.Email.isStep(AuthRoute.MethodPicker.routePattern)).isFalse()
        assertThat(AuthRoute.Email.isStep(AuthRoute.Phone.routePattern)).isFalse()
        // A live destination.route is always the pattern, never the bare navigation route.
        assertThat(AuthRoute.Email.isStep(AuthRoute.Email.SignIn.route)).isFalse()
        assertThat(AuthRoute.Email.isStep(null)).isFalse()
    }

    /**
     * `AuthRoute.all` has to name every step the sealed hierarchy declares: one left out of its
     * flow's `steps` list is registered nowhere and throws at whoever navigates to it. Enumerated
     * from `sealedSubclasses`, since a hand-written list would agree with itself either way.
     */
    @Test
    fun `the full route list names every step the hierarchy declares`() {
        val all = AuthRoute.all
        val standalone = listOf(
            AuthRoute.MethodPicker,
            AuthRoute.Success,
            AuthRoute.MfaChallenge,
        )
        val flows = listOf(AuthRoute.Email, AuthRoute.Phone, AuthRoute.MfaEnrollment)
        val declared = declaredSteps(AuthRoute.Email.Step::class) +
                declaredSteps(AuthRoute.Phone.Step::class) +
                declaredSteps(AuthRoute.MfaEnrollment.Step::class)

        // Exactly, not at least: the list must not grow a value no host registers either.
        assertThat(all).containsExactlyElementsIn(standalone + flows + declared)
        // Each flow entry point shares its start step's route, which is the only duplication the
        // list is allowed to hold — one collision per flow.
        assertThat(all.map { it.route }.distinct()).hasSize(all.size - flows.size)
    }

    /** The same check from the other side: hosts register a flow from its own `steps` list. */
    @Test
    fun `each flow's step list holds exactly the steps it declares`() {
        assertThat(AuthRoute.Email.steps)
            .containsExactlyElementsIn(declaredSteps(AuthRoute.Email.Step::class))
        assertThat(AuthRoute.Phone.steps)
            .containsExactlyElementsIn(declaredSteps(AuthRoute.Phone.Step::class))
        assertThat(AuthRoute.MfaEnrollment.steps)
            .containsExactlyElementsIn(declaredSteps(AuthRoute.MfaEnrollment.Step::class))
    }

    /** Every step object the sealed [stepType] declares, by reflection rather than a literal list. */
    private fun declaredSteps(stepType: KClass<out AuthRoute>): List<AuthRoute> {
        val subclasses = stepType.sealedSubclasses
        assertThat(subclasses).isNotEmpty()
        return subclasses.map { subclass ->
            requireNotNull(subclass.objectInstance) {
                "${subclass.simpleName} is a step but not an object. Steps are named by identity " +
                        "(the graph registers them, callers pass them), so each has to be a " +
                        "singleton."
            }
        }
    }
}
