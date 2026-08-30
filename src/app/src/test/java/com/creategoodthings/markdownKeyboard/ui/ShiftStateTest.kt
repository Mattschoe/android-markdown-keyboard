package com.creategoodthings.markdownKeyboard.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** The tap machine every other Android keyboard has trained users to expect. */
class ShiftStateTest {

    private val doubleTap = 300L

    @Test fun oneTapArmsAOneShotShift() {
        assertEquals(ShiftState.Shifted, ShiftState.Off.onTap(10_000L, doubleTap))
    }

    @Test fun aSecondTapInsideTheWindowLocks() {
        assertEquals(ShiftState.Locked, ShiftState.Shifted.onTap(doubleTap, doubleTap))
    }

    @Test fun aSlowSecondTapCancelsRatherThanLocking() {
        assertEquals(ShiftState.Off, ShiftState.Shifted.onTap(doubleTap + 1, doubleTap))
    }

    @Test fun tappingALockReleasesIt() {
        assertEquals(ShiftState.Off, ShiftState.Locked.onTap(10L, doubleTap))
        assertEquals(ShiftState.Off, ShiftState.Locked.onTap(10_000L, doubleTap))
    }

    @Test fun aOneShotShiftIsSpentByTheCharacterItCapitalised() {
        assertEquals(ShiftState.Off, ShiftState.Shifted.afterCharacter())
    }

    @Test fun aLockOutlivesTheCharactersItCapitalises() {
        assertEquals(ShiftState.Locked, ShiftState.Locked.afterCharacter())
    }

    @Test fun typingWithoutShiftChangesNothing() {
        assertEquals(ShiftState.Off, ShiftState.Off.afterCharacter())
    }

    @Test fun bothShiftedStatesTypeUpperCase() {
        assertFalse(ShiftState.Off.isUpperCase)
        assertTrue(ShiftState.Shifted.isUpperCase)
        assertTrue(ShiftState.Locked.isUpperCase)
    }
}
