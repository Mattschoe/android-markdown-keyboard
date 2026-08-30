package com.creategoodthings.markdownKeyboard.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * What a key is *for*, which is the only thing that decides how it is painted.
 *
 * Keys that put something in the document — letters, space, and every markdown key — are
 * [Primary] and sit brightest. Keys that operate the keyboard rather than the text are
 * [Utility] and recede. Exactly one key is [Accent].
 */
enum class KeyTone { Primary, Utility, Accent }

/** The fill and label colour for one key in one state. Resolved by [KeyboardColors.forTone]. */
@Immutable
data class KeyColors(val background: Color, val foreground: Color)

/**
 * The keyboard's own palette, separate from [androidx.compose.material3.ColorScheme].
 *
 * Material's surface roles cannot express "the brightest key" across both themes: in light
 * `surfaceContainerLowest` is the brightest of the family and in dark it is the darkest, so a
 * single role name would invert between themes. The keyboard's layering is stated directly here
 * instead, and stays legible when a proper theme system replaces the two constants below.
 */
@Immutable
data class KeyboardColors(
    val background: Color,
    val key: Color,
    val keyPressed: Color,
    val onKey: Color,
    val utilityKey: Color,
    val utilityKeyPressed: Color,
    val onUtilityKey: Color,
    val accentKey: Color,
    val accentKeyPressed: Color,
    val onAccentKey: Color,
    /** The alternates strip, which floats over the keys and so cannot reuse a key colour. */
    val strip: Color,
    val onStrip: Color,
    val stripHighlight: Color,
    val onStripHighlight: Color,
) {
    fun forTone(tone: KeyTone, pressed: Boolean): KeyColors = when (tone) {
        KeyTone.Primary -> KeyColors(if (pressed) keyPressed else key, onKey)
        KeyTone.Utility -> KeyColors(if (pressed) utilityKeyPressed else utilityKey, onUtilityKey)
        KeyTone.Accent -> KeyColors(if (pressed) accentKeyPressed else accentKey, onAccentKey)
    }
}

/**
 * Light: keys are lighter than the board behind them, and a press darkens toward it. Utility keys
 * sit below the board so they read as recessed rather than as dimmer letters.
 */
val LightKeyboardColors = KeyboardColors(
    background = NeutralLight88,
    key = KeyWhite,
    keyPressed = NeutralLight92,
    onKey = NeutralLight10,
    utilityKey = NeutralLight80,
    utilityKeyPressed = NeutralLight70,
    onUtilityKey = NeutralLight20,
    accentKey = AccentLight,
    accentKeyPressed = AccentLightPressed,
    onAccentKey = OnAccentLight,
    strip = KeyWhite,
    onStrip = NeutralLight10,
    stripHighlight = AccentLight,
    onStripHighlight = OnAccentLight,
)

/** Dark: the same layering inverted — a press lifts a key toward the light instead of away. */
val DarkKeyboardColors = KeyboardColors(
    background = NeutralDark08,
    key = NeutralDark22,
    keyPressed = NeutralDark30,
    onKey = NeutralDark90,
    utilityKey = NeutralDark14,
    utilityKeyPressed = NeutralDark22,
    onUtilityKey = NeutralDark80,
    accentKey = AccentDark,
    accentKeyPressed = AccentDarkPressed,
    onAccentKey = OnAccentDark,
    strip = NeutralDark30,
    onStrip = NeutralDark90,
    stripHighlight = AccentDark,
    onStripHighlight = OnAccentDark,
)

val LocalKeyboardColors = staticCompositionLocalOf { LightKeyboardColors }
