package com.creategoodthings.markdownKeyboard.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * A fixed, wallpaper-independent palette.
 *
 * The keyboard is drawn over someone else's app, so it cannot borrow that app's colours, and a
 * dynamic (Material You) scheme would put an unknown accent under every key. Both themes are
 * therefore pinned here: one cool neutral ramp plus a single blue accent.
 *
 * Naming follows the tone the colour paints, not the Material role it happens to fill — see
 * [KeyboardColors] for how the keyboard uses them and [MarkdownKeyboardTheme] for the companion
 * app's Material scheme.
 */

// Neutral ramp — light.
val NeutralLight99 = Color(0xFFFAFAFD)
val NeutralLight95 = Color(0xFFEEEFF4)
val NeutralLight92 = Color(0xFFE6E8EE)
val NeutralLight88 = Color(0xFFDEE1E6)
val NeutralLight80 = Color(0xFFC9CDD6)
val NeutralLight70 = Color(0xFFB2B7C3)
val NeutralLight30 = Color(0xFF44464F)
val NeutralLight20 = Color(0xFF3B3F46)
val NeutralLight10 = Color(0xFF191C20)

// Neutral ramp — dark.
val NeutralDark06 = Color(0xFF111318)
val NeutralDark08 = Color(0xFF16171A)
val NeutralDark12 = Color(0xFF1D1F23)
val NeutralDark14 = Color(0xFF23252A)
val NeutralDark22 = Color(0xFF34363B)
val NeutralDark30 = Color(0xFF494C53)
val NeutralDark60 = Color(0xFF8F9099)
val NeutralDark80 = Color(0xFFC4C7CF)
val NeutralDark90 = Color(0xFFE2E2E9)

// Accent — the one saturated colour in the app, on the Enter key and the companion app's buttons.
val AccentLight = Color(0xFF3B5BDB)
val AccentLightPressed = Color(0xFF2A46B4)
val AccentLightContainer = Color(0xFFDCE1FF)
val OnAccentLight = Color(0xFFFFFFFF)
val OnAccentLightContainer = Color(0xFF001551)

val AccentDark = Color(0xFFA8C7FA)
val AccentDarkPressed = Color(0xFFC5DAFC)
val AccentDarkContainer = Color(0xFF2A4177)
val OnAccentDark = Color(0xFF06305B)
val OnAccentDarkContainer = Color(0xFFDCE1FF)

val KeyWhite = Color(0xFFFFFFFF)
