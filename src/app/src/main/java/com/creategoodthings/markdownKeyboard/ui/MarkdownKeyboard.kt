package com.creategoodthings.markdownKeyboard.ui

import android.os.SystemClock
import android.view.ViewConfiguration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeGestures
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.creategoodthings.markdownKeyboard.editor.KeyAction
import com.creategoodthings.markdownKeyboard.ui.theme.LocalKeyboardColors

/**
 * The keyboard: two markdown rows that never change, then whichever character page is showing.
 *
 * Page and shift are UI state and stay here, above the `ComposeMdKeyboardView` seam, so neither
 * leaks into `editor/` and the keyboard still composes in a preview with no service behind it.
 * Both page-switch keys follow the shift key's precedent: they carry [KeyAction.Noop] and do
 * their real work in `onClick`.
 */
@Composable
fun MarkdownKeyboard(onAction: (KeyAction) -> Unit) {
    var shift by remember { mutableStateOf(ShiftState.Off) }
    var page by remember { mutableStateOf(KeyboardPage.Letters) }
    var lastShiftTapMs by remember { mutableLongStateOf(0L) }
    val doubleTapTimeoutMs = remember { ViewConfiguration.getDoubleTapTimeout().toLong() }

    val density = LocalDensity.current
    val alternates = remember(density) { AlternatesHostState(stripMetrics(density)) }

    fun onShiftTap() {
        val now = SystemClock.uptimeMillis()
        shift = shift.onTap(now - lastShiftTapMs, doubleTapTimeoutMs)
        lastShiftTapMs = now
    }

    CompositionLocalProvider(LocalAlternatesHost provides alternates) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(LocalKeyboardColors.current.background)
                .onGloballyPositioned(alternates::attach)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = KEYBOARD_EDGE_PADDING.dp,
                        end = KEYBOARD_EDGE_PADDING.dp,
                        top = KEYBOARD_TOP_PADDING.dp,
                    )
            ) {
                MarkdownRows(onAction)

                when (page) {
                    KeyboardPage.Letters -> LetterRows(
                        shift = shift,
                        onAction = onAction,
                        onShiftTap = ::onShiftTap,
                        onTyped = { shift = shift.afterCharacter() },
                        onPage = { page = it },
                    )

                    KeyboardPage.Symbols -> SymbolRows(onAction) { page = it }
                    KeyboardPage.SymbolsExtra -> SymbolsExtraRows(onAction) { page = it }
                    KeyboardPage.Numeric -> NumericRows(onAction) { page = it }
                }

                Spacer(
                    modifier = Modifier
                        .fillMaxWidth()
                        .windowInsetsBottomHeight(systemAffordanceInsets())
                )
            }

            // Last child, so it floats over the keys. It takes no pointer input of its own: the
            // key that opened it owns the whole gesture, right through to the release.
            alternates.strip?.let { AlternatesStripView(it) }
        }
    }
}

/**
 * The strip along the bottom that the OS keeps for itself.
 *
 * `navigationBars` alone is not enough: the IME window gets a *reduced* navigation bar inset
 * (24dp on gesture navigation) while the system still paints and taps its own affordances —
 * the IME switcher, the OEM's voice button — in a taller band (48dp) that hangs over the
 * bottom key row. `safeGestures` reports that taller band (`systemGestures` +
 * `mandatorySystemGestures` + `tappableElement`), so the union of the two is the first row
 * of pixels a key may safely occupy under either navigation mode.
 */
@Composable
private fun systemAffordanceInsets(): WindowInsets =
    WindowInsets.safeGestures.union(WindowInsets.navigationBars)
