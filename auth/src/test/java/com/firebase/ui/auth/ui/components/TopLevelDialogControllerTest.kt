package com.firebase.ui.auth.ui.components

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.firebase.ui.auth.AuthException
import com.firebase.ui.auth.AuthState
import com.firebase.ui.auth.configuration.string_provider.DefaultAuthUIStringProvider
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Unit tests for [TopLevelDialogController] and [rememberTopLevelDialogController].
 *
 * These cover the fix for a bug where keying `remember` on the live `authState` value recreated
 * the controller (and wiped its `shownErrorStates` de-duplication set) on every state change —
 * which, combined with screens resetting `AuthState` back to `Idle` immediately after consuming
 * an `Error`, would tear down and discard the just-shown dialog on the very next recomposition.
 *
 * @suppress Internal test class
 */
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [34])
class TopLevelDialogControllerTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var stringProvider: DefaultAuthUIStringProvider

    @Test
    fun `controller survives an authState change instead of being recreated`() {
        stringProvider = DefaultAuthUIStringProvider(ApplicationProvider.getApplicationContext())
        var state: AuthState = AuthState.Idle
        lateinit var controller: TopLevelDialogController

        composeTestRule.setContent {
            controller = rememberTopLevelDialogController(stringProvider) { state }
            controller.CurrentDialog()
        }

        val error = AuthState.Error(Exception("boom"))
        composeTestRule.runOnIdle {
            state = error
            controller.showErrorDialog(
                exception = AuthException.from(error.exception, stringProvider)
            )
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText(stringProvider.errorDialogTitle).assertExists()

        // Mirrors the fixed screens resetting authState right after showing the dialog.
        composeTestRule.runOnIdle {
            state = AuthState.Idle
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText(stringProvider.errorDialogTitle).assertExists()
    }

    @Test
    fun `showErrorDialog does not re-show the same error state twice`() {
        stringProvider = DefaultAuthUIStringProvider(ApplicationProvider.getApplicationContext())
        var state: AuthState = AuthState.Idle
        lateinit var controller: TopLevelDialogController

        composeTestRule.setContent {
            controller = rememberTopLevelDialogController(stringProvider) { state }
            controller.CurrentDialog()
        }

        val error = AuthState.Error(Exception("boom"))
        val exception = AuthException.from(error.exception, stringProvider)

        composeTestRule.runOnIdle {
            state = error
            controller.showErrorDialog(exception = exception)
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText(stringProvider.errorDialogTitle).assertExists()

        composeTestRule.runOnIdle {
            controller.dismissDialog()
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText(stringProvider.errorDialogTitle).assertDoesNotExist()

        // Same Error instance again — must be a no-op, the de-dup set persists across calls.
        composeTestRule.runOnIdle {
            controller.showErrorDialog(exception = exception)
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText(stringProvider.errorDialogTitle).assertDoesNotExist()
    }

    @Test
    fun `second observer of the same error does not overwrite the first observer's dialog`() {
        stringProvider = DefaultAuthUIStringProvider(ApplicationProvider.getApplicationContext())
        var state: AuthState = AuthState.Idle
        lateinit var controller: TopLevelDialogController

        composeTestRule.setContent {
            controller = rememberTopLevelDialogController(stringProvider) { state }
            controller.CurrentDialog()
        }

        val error = AuthState.Error(Exception("boom"))
        val exception = AuthException.from(error.exception, stringProvider)

        var firstOnRetryCalled = false
        var secondOnRetryCalled = false

        // Observer #1 (e.g. FirebaseAuthScreen's top-level effect): sees the Error, shows the
        // dialog passing the specific errorState, then immediately resets the live state to
        // Idle -- mirroring the real screens' consume-then-reset pattern.
        composeTestRule.runOnIdle {
            state = error
            controller.showErrorDialog(
                exception = exception,
                errorState = error,
                onRetry = { firstOnRetryCalled = true }
            )
            state = AuthState.Idle
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText(stringProvider.errorDialogTitle).assertExists()

        // Observer #2 (e.g. EmailAuthScreen's own effect on the same authState emission): its
        // LaunchedEffect(authState) still holds the same `error` instance locally, so it passes
        // it as errorState even though the controller's live currentAuthState() now reads Idle.
        composeTestRule.runOnIdle {
            controller.showErrorDialog(
                exception = exception,
                errorState = error,
                onRetry = { secondOnRetryCalled = true }
            )
        }
        composeTestRule.waitForIdle()

        // Dedup must key off the passed-in errorState, not the (possibly already-reset) live
        // state, so observer #2's call is a no-op and observer #1's dialog/callback survives.
        composeTestRule.onNodeWithText(stringProvider.retryAction).performClick()
        assert(firstOnRetryCalled && !secondOnRetryCalled) {
            "Expected observer #1's dialog/callback to survive untouched, but observer #2's " +
                "call overwrote it (firstOnRetryCalled=$firstOnRetryCalled, " +
                "secondOnRetryCalled=$secondOnRetryCalled)"
        }
    }
}
