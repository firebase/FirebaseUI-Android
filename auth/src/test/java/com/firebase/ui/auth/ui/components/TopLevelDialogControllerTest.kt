package com.firebase.ui.auth.ui.components

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
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

        // Simulate what the fixed screens now do immediately after showing the dialog: reset
        // the shared auth state back to Idle. If the controller were still keyed on authState,
        // this recomposition would tear it down (and the dialog with it).
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

        // Calling showErrorDialog again for the *same* Error instance (state hasn't changed)
        // must be a no-op — the de-dup set persists across calls since the controller is not
        // recreated between them.
        composeTestRule.runOnIdle {
            controller.showErrorDialog(exception = exception)
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText(stringProvider.errorDialogTitle).assertDoesNotExist()
    }
}
