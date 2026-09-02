package com.firebase.ui.auth.ui.screens

import android.content.Context
import androidx.navigation3.runtime.NavKey
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
import kotlin.reflect.full.createInstance
import kotlinx.serialization.json.Json

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

    // =============================================================================================
    // AuthRoute shape — this is public API, and PR-shaped changes to it break every caller.
    // =============================================================================================

    /** Naming a flow means "enter this flow", so it must resolve to that flow's start step. */
    @Test
    fun `every flow entry point resolves to its start step`() {
        assertThat(AuthRoute.Email.startKey()).isEqualTo(AuthRoute.Email.SignIn())
        assertThat(AuthRoute.Phone.startKey()).isEqualTo(AuthRoute.Phone.EnterPhoneNumber)
        assertThat(AuthRoute.MfaEnrollment.startKey())
            .isEqualTo(AuthRoute.MfaEnrollment.SelectFactor)
    }

    /**
     * A flow entry point names a flow, not a destination: it is not a `NavKey`, so it can never be
     * pushed onto the back stack in place of the step it resolves to.
     */
    @Test
    fun `a flow entry point is not itself a destination`() {
        assertThat(AuthRoute.Email).isNotInstanceOf(NavKey::class.java)
        assertThat(AuthRoute.Phone).isNotInstanceOf(NavKey::class.java)
        assertThat(AuthRoute.MfaEnrollment).isNotInstanceOf(NavKey::class.java)
        assertThat(AuthRoute.Email.SignIn()).isInstanceOf(NavKey::class.java)
    }

    @Test
    fun `the email step list is fully populated and distinct`() {
        val steps = AuthRoute.Email.steps

        assertThat(steps).hasSize(EmailAuthMode.entries.size)
        assertThat(steps.map { it::class }).containsNoDuplicates()
        assertThat(steps.map { it.mode }).containsExactlyElementsIn(EmailAuthMode.entries)
    }

    @Test
    fun `every email mode maps to exactly one step`() {
        EmailAuthMode.entries.forEach { mode ->
            assertThat(AuthRoute.Email.stepFor(mode).mode).isEqualTo(mode)
        }
    }

    /** The address is a field on the key, so `withEmail` is a `copy`. */
    @Test
    fun `withEmail puts the address on the key`() {
        val step = AuthRoute.Email.SignUp()

        assertThat(step.email).isNull()
        assertThat(step.withEmail(null)).isEqualTo(AuthRoute.Email.SignUp(null))
        assertThat(step.withEmail("user+tag@example.com"))
            .isEqualTo(AuthRoute.Email.SignUp("user+tag@example.com"))
        // Same step, different address: two DIFFERENT keys. This is the whole reason
        // navigateToEmailStep has to compare step *types* rather than keys.
        assertThat(AuthRoute.Email.SignUp("a@b.com"))
            .isNotEqualTo(AuthRoute.Email.SignUp("c@d.com"))
    }

    /**
     * `SignUp("")` and `SignUp(null)` do not collapse into one destination: they are different
     * keys, so an empty address is a state the flow can genuinely be in. That is what this asserts
     * — the keys really are distinct, which is the fact `navigateToEmailStep`'s type comparison
     * exists for.
     *
     * The *consequence* — that an empty address on the key still falls back to the prefill, rather
     * than blanking a field the host had populated — belongs to `EmailAuthDestinations`, which
     * reads `step.email?.ifEmpty { null } ?: prefillEmail()`. Asserting that expression here would
     * only test the Kotlin stdlib: it would keep passing with `?.ifEmpty { null }` deleted from the
     * product. It is pinned against the destination instead, by
     * `EmailAuthRouteNavigationTest`'s `a step entered with an empty address falls back to the
     * prefill`.
     */
    @Test
    fun `an empty address is a key distinct from a null one`() {
        val step = AuthRoute.Email.SignUp()

        assertThat(step.withEmail("")).isEqualTo(AuthRoute.Email.SignUp(""))
        assertThat(step.withEmail("")).isNotEqualTo(step.withEmail(null))
        assertThat(AuthRoute.Email.stepFor(EmailAuthMode.SignUp, "")).isEqualTo(
            AuthRoute.Email.SignUp("")
        )
        assertThat(AuthRoute.Email.SignUp(TYPED_ADDRESS))
            .isNotEqualTo(AuthRoute.Email.SignUp(""))
    }

    /**
     * The address reaches saved state through kotlinx.serialization, so that round-trip is the one
     * place an awkward local part could be lost.
     */
    @Test
    fun `awkward addresses round-trip through the key's serializer`() {
        listOf(
            "user+tag@example.com",
            "user name@example.com",
            "user#hash@example.com",
            "user?query=1@example.com",
            "user&more@example.com",
            "ada@königsberg.example",
            "用户@例え.jp",
        ).forEach { address ->
            AuthRoute.Email.steps.forEach { prototype ->
                val step = prototype.withEmail(address)
                val encoded = Json.encodeToString(
                    AuthRoute.Email.Step.serializer(),
                    step,
                )
                val decoded = Json.decodeFromString(AuthRoute.Email.Step.serializer(), encoded)

                assertThat(decoded).isEqualTo(step)
                assertThat(decoded.email).isEqualTo(address)
            }
        }
    }

    @Test
    fun `isStep recognises the email steps and nothing else`() {
        AuthRoute.Email.steps.forEach { step ->
            assertThat(AuthRoute.Email.isStep(step)).isTrue()
        }

        assertThat(AuthRoute.Email.isStep(AuthRoute.MethodPicker)).isFalse()
        assertThat(AuthRoute.Email.isStep(AuthRoute.Phone.EnterPhoneNumber)).isFalse()
        assertThat(AuthRoute.Email.isStep(null)).isFalse()
    }

    /**
     * `allAuthRoutes` is what the reachability test asserts against, built from the same per-flow
     * `steps` lists the hosts register from.
     *
     * Email steps are `data class`es (they carry the address), so `objectInstance` is null for
     * them and [declaredSteps] falls back to constructing one with default arguments. That is not
     * a weakening of the check: `createInstance()` throws `IllegalArgumentException("Class should
     * have a single no-arg constructor: …")` when no fully-defaulted constructor exists, so a step
     * that grew a required field errors this test loudly rather than dropping out of it.
     */
    @Test
    fun `the full route list names every step the hierarchy declares`() {
        val all = allAuthRoutes
        val standalone = listOf(
            AuthRoute.MethodPicker,
            AuthRoute.Success,
            AuthRoute.MfaChallenge,
        )
        val flows = listOf(AuthRoute.Email, AuthRoute.Phone, AuthRoute.MfaEnrollment)
        val declared = declaredSteps(AuthRoute.Email.Step::class) +
                declaredSteps(AuthRoute.Phone.Step::class) +
                declaredSteps(AuthRoute.MfaEnrollment.Step::class)

        assertThat(all).containsExactlyElementsIn(standalone + flows + declared)
        // A flow entry point and its start step are separate values, so nothing in the list
        // aliases anything else in it.
        assertThat(all).containsNoDuplicates()
    }

    @Test
    fun `each flow's step list holds exactly the steps it declares`() {
        assertThat(AuthRoute.Email.steps)
            .containsExactlyElementsIn(declaredSteps(AuthRoute.Email.Step::class))
        assertThat(AuthRoute.Phone.steps)
            .containsExactlyElementsIn(declaredSteps(AuthRoute.Phone.Step::class))
        assertThat(AuthRoute.MfaEnrollment.steps)
            .containsExactlyElementsIn(declaredSteps(AuthRoute.MfaEnrollment.Step::class))
    }

    /**
     * Every step the sealed [stepType] declares, in its default (address-free) form.
     *
     * `objectInstance` alone does not suffice — see the KDoc above. `createInstance()` returns a
     * non-null `T` or throws, so there is nothing to null-check here.
     */
    private fun declaredSteps(stepType: KClass<out AuthRoute>): List<AuthRoute> {
        val subclasses = stepType.sealedSubclasses
        assertThat(subclasses).isNotEmpty()
        return subclasses.map { subclass -> subclass.objectInstance ?: subclass.createInstance() }
    }

    private companion object {
        const val TYPED_ADDRESS = "user+tag@example.com"
    }
}
