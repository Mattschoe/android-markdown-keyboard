package com.creategoodthings.markdownKeyboard.ui

/**
 * The shift key's three states and the tap machine that moves between them, matching the
 * behaviour every other Android keyboard has trained users to expect.
 *
 * One tap arms a *one-shot* shift that falls away again after the next character; a second tap
 * inside the platform double-tap window turns that into a caps lock that stays until tapped off.
 * A slow second tap is read as "cancel", not as a lock, so a mistaken tap costs one tap to undo.
 *
 * Kept free of Android types so the whole machine is testable on the host JVM; the composable
 * supplies the clock and the timeout.
 */
enum class ShiftState {
    Off,
    Shifted,
    Locked;

    /** Whether letter keys should currently emit upper case. */
    val isUpperCase: Boolean get() = this != Off

    /**
     * @param sinceLastTapMs time since the previous shift tap, whatever its outcome.
     * @param doubleTapTimeoutMs the platform's double-tap window.
     */
    fun onTap(sinceLastTapMs: Long, doubleTapTimeoutMs: Long): ShiftState = when {
        this == Shifted && sinceLastTapMs <= doubleTapTimeoutMs -> Locked
        this == Off -> Shifted
        else -> Off
    }

    /** A one-shot shift is spent by the character it capitalised; a lock is not. */
    fun afterCharacter(): ShiftState = if (this == Shifted) Off else this
}
