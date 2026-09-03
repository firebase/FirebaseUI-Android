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

package com.firebase.ui.auth.ui.screens.reauth

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.NavMetadataKey
import androidx.navigation3.runtime.get
import androidx.navigation3.runtime.metadata
import androidx.navigation3.scene.OverlayScene
import androidx.navigation3.scene.Scene
import androidx.navigation3.scene.SceneStrategy
import androidx.navigation3.scene.SceneStrategyScope
import com.firebase.ui.auth.ui.exposeTestTagsAsResourceIds

/**
 * How a reauthentication entry is presented: the library's own modal sheet, or bare, over the
 * flow underneath, for a consumer slot that owns its own presentation.
 */
internal enum class ReauthPresentation { Sheet, Bare }

/**
 * Which reauthentication request an entry belongs to, and how it is presented.
 *
 * [requestId] identifies the *surface*: every entry of one request carries the same one, which is
 * what keeps the overlay — and therefore its sheet — a single one across the request's steps.
 */
internal data class ReauthOverlay(
    val requestId: String,
    val presentation: ReauthPresentation,
)

internal object ReauthOverlayKey : NavMetadataKey<ReauthOverlay>

internal fun reauthOverlayMetadata(
    requestId: String,
    presentation: ReauthPresentation,
): Map<String, Any> = metadata { put(ReauthOverlayKey, ReauthOverlay(requestId, presentation)) }

private fun NavEntry<NavKey>.reauthOverlay(): ReauthOverlay? = metadata[ReauthOverlayKey]

/**
 * Renders the *trailing run* of reauthentication entries as one overlay over everything below.
 *
 * A run, not a single entry, because reauthentication is several steps deep: the recipe's
 * one-entry strategy would push the previous step out from under the sheet and render it
 * full-screen behind the scrim. Only the topmost entry of the run is composed; the rest stay
 * owned by the scene so `NavDisplay` keeps their saveable state.
 *
 * @param surface The one condition for the surface: the sheet composes only while this resolves,
 * so a reauthentication entry with no request outstanding shows neither sheet nor scrim, and the entry it
 * would have composed is never reached.
 * @param transitionSpec Applied to step changes inside the overlay, so they animate the way the
 * flow underneath animates. There is no predictive-pop counterpart: that gesture drives
 * `NavDisplay`'s own transition, which an overlay is not part of.
 */
internal class ReauthSceneStrategy(
    private val surface: State<ReauthSurface?>,
    private val onDismissRequest: () -> Unit,
    private val transitionSpec:
    AnimatedContentTransitionScope<Scene<NavKey>>.() -> ContentTransform,
    private val popTransitionSpec:
    AnimatedContentTransitionScope<Scene<NavKey>>.() -> ContentTransform,
) : SceneStrategy<NavKey> {

    /**
     * The run to render, topmost last.
     *
     * `NavDisplay` keeps the first scene instance it sees for a given [Scene.key] and renders that
     * one until the key leaves the stack, so the scene reads the run from here rather than from
     * its own fields. Never written empty: this is what the sheet still shows while it hides.
     */
    private val run = mutableStateOf<List<NavEntry<NavKey>>>(emptyList())

    override fun SceneStrategyScope<NavKey>.calculateScene(
        entries: List<NavEntry<NavKey>>,
    ): Scene<NavKey>? {
        val reauthRun = entries.takeLastWhile { it.reauthOverlay() != null }
        val requestId = reauthRun.lastOrNull()?.reauthOverlay()?.requestId ?: return null
        run.value = reauthRun
        return ReauthScene(
            key = requestId,
            runState = run,
            surface = surface,
            ownedEntries = reauthRun,
            previousEntries = entries.dropLast(reauthRun.size),
            onDismissRequest = onDismissRequest,
            transitionSpec = transitionSpec,
            popTransitionSpec = popTransitionSpec,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
private data class ReauthScene(
    override val key: String,
    private val runState: State<List<NavEntry<NavKey>>>,
    private val surface: State<ReauthSurface?>,
    private val ownedEntries: List<NavEntry<NavKey>>,
    override val previousEntries: List<NavEntry<NavKey>>,
    private val onDismissRequest: () -> Unit,
    private val transitionSpec:
    AnimatedContentTransitionScope<Scene<NavKey>>.() -> ContentTransform,
    private val popTransitionSpec:
    AnimatedContentTransitionScope<Scene<NavKey>>.() -> ContentTransform,
) : OverlayScene<NavKey> {

    override val entries: List<NavEntry<NavKey>> = ownedEntries
    override val overlaidEntries: List<NavEntry<NavKey>> = previousEntries

    /** The sheet composed right now, if any. What [onRemove] has to hide before it goes. */
    private var sheetState: SheetState? = null

    override val content: @Composable (() -> Unit) = {
        // `NavDisplay` provides this overlay its own LocalLifecycleOwner, resumed while it is the
        // topmost one, so the recipe's `rememberLifecycleOwner` here would only nest a second cap
        // inside that.
        val run = runState.value.takeLastWhile { it.reauthOverlay()?.requestId == key }
        val top = run.lastOrNull()
        // Existence and content answer to one condition: no surface, no sheet and no scrim.
        // Latched for the same reason the run is kept — the surface is released as the entries
        // are popped, and dropping the sheet there would cut its hide short.
        val hasPresented = remember { mutableStateOf(false) }
        if (surface.value != null) hasPresented.value = true
        val presentation = top?.reauthOverlay()?.presentation?.takeIf { hasPresented.value }
        when (presentation) {
            ReauthPresentation.Sheet -> {
                val state = rememberModalBottomSheetState(skipPartiallyExpanded = true)
                sheetState = state
                ModalBottomSheet(
                    modifier = Modifier.exposeTestTagsAsResourceIds(),
                    onDismissRequest = onDismissRequest,
                    sheetState = state,
                ) {
                    Step(run, top)
                }
            }

            ReauthPresentation.Bare -> {
                sheetState = null
                Box(modifier = Modifier.fillMaxSize()) { Step(run, top) }
            }

            null -> Unit
        }
    }

    /** Hides the sheet before the entry leaves composition, which is what this hook is for. */
    override suspend fun onRemove() {
        sheetState?.hide()
    }

    /** [top], transitioned the way the flow underneath transitions between its destinations. */
    @Composable
    private fun Step(run: List<NavEntry<NavKey>>, top: NavEntry<NavKey>) {
        val target: Scene<NavKey> = ReauthStepScene(top)
        AnimatedContent(
            targetState = target,
            contentKey = { it.key },
            transitionSpec = {
                // A step stepped back out of is no longer in the run; one pushed under is.
                if (run.none { it.contentKey == initialState.key }) {
                    popTransitionSpec()
                } else {
                    transitionSpec()
                }
            },
            label = "ReauthStep",
        ) { step ->
            step.content()
        }
    }
}

/**
 * One reauthentication step as a [Scene] value, so a host's configured specs — written against
 * [Scene] — apply to step changes inside the overlay unchanged. Never handed to `NavDisplay`: the
 * overlay is the scene it renders.
 */
private data class ReauthStepScene(private val entry: NavEntry<NavKey>) : Scene<NavKey> {
    override val key: Any = entry.contentKey
    override val entries: List<NavEntry<NavKey>> = listOf(entry)
    override val previousEntries: List<NavEntry<NavKey>> = emptyList()
    override val content: @Composable () -> Unit = { entry.Content() }
}
