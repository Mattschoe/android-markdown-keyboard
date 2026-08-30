package com.creategoodthings.markdownKeyboard.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

private const val TIMEOUT = 300L

/** Taps the key [times] times, [gapMs] apart, starting from [this]. */
private fun ShiftState.tap(vararg gapsMs: Long): ShiftState =
    gapsMs.fold(this) { state, gap -> state.onTap(gap, TIMEOUT) }

class ShiftStateTest {
    @Test
    fun `one tap arms a single shift`() {
        assertEquals(ShiftState.Shifted, ShiftState.Off.tap(TIMEOUT * 10))
    }

    @Test
    fun `double tap locks`() {
        assertEquals(ShiftState.Locked, ShiftState.Off.tap(TIMEOUT * 10, TIMEOUT - 1))
    }

    @Test
    fun `a tap exactly on the double-tap boundary still locks`() {
        assertEquals(ShiftState.Locked, ShiftState.Shifted.onTap(TIMEOUT, TIMEOUT))
    }

    @Test
    fun `a slow second tap cancels instead of locking`() {
        assertEquals(ShiftState.Off, ShiftState.Off.tap(TIMEOUT * 10, TIMEOUT + 1))
    }

    @Test
    fun `tapping a lock releases it, however fast`() {
        assertEquals(ShiftState.Off, ShiftState.Locked.onTap(1, TIMEOUT))
        assertEquals(ShiftState.Off, ShiftState.Locked.onTap(TIMEOUT * 10, TIMEOUT))
    }

    @Test
    fun `a third quick tap unlocks rather than cycling back to shifted`() {
        assertEquals(ShiftState.Off, ShiftState.Off.tap(TIMEOUT * 10, 1, 1))
    }

    @Test
    fun `a quick tap after the shift was spent arms, it does not lock`() {
        // Tap, type a letter, then tap again straight away: the first shift is gone, so this is
        // a fresh single shift and not a double tap.
        val spent = ShiftState.Off.tap(TIMEOUT * 10).afterCharacter()
        assertEquals(ShiftState.Shifted, spent.onTap(1, TIMEOUT))
    }

    @Test
    fun `a single shift is spent by one character`() {
        assertEquals(ShiftState.Off, ShiftState.Shifted.afterCharacter())
    }

    @Test
    fun `a lock survives typing`() {
        assertEquals(ShiftState.Locked, ShiftState.Locked.afterCharacter())
    }

    @Test
    fun `typing with shift off changes nothing`() {
        assertEquals(ShiftState.Off, ShiftState.Off.afterCharacter())
    }

    @Test
    fun `only the off state types lower case`() {
        assertFalse(ShiftState.Off.isUpperCase)
        assertTrue(ShiftState.Shifted.isUpperCase)
        assertTrue(ShiftState.Locked.isUpperCase)
    }
}
