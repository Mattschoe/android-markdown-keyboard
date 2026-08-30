package com.creategoodthings.markdownKeyboard.ui

import com.creategoodthings.markdownKeyboard.ui.AlternatesGeometry.NONE
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The strip's whole arithmetic. Pure floats, so this runs on the host JVM with nothing on the
 * classpath — which is the point of keeping [AlternatesGeometry] free of Compose types.
 *
 * The fixture is a 400x300 keyboard with 40x40 keys, giving 40-wide strip entries: a strip of
 * four is 168 wide and 54 tall.
 */
class AlternatesGeometryTest {

    private val metrics = StripMetrics(
        itemWidth = 40f,
        itemHeight = 46f,
        padding = 4f,
        gap = 6f,
        slop = 24f,
    )

    private val host = Bounds(0f, 0f, 400f, 300f)

    /** A key in the middle of the second row, with room above it. */
    private val middleKey = Bounds(180f, 100f, 40f, 40f)

    private fun place(key: Bounds, count: Int = 4, anchorIndex: Int = 0) =
        AlternatesGeometry.place(key, count, anchorIndex, host, metrics)

    // ---- placement ----

    @Test fun theAnchorEntrySitsOverTheKeyItTypes() {
        val placement = place(middleKey)
        assertEquals(middleKey.centerX, placement.itemBounds(0).centerX, 0.01f)
        assertTrue(placement.above)
    }

    @Test fun aLaterAnchorSlidesTheStripLeftSoThatEntryStillSitsOverTheKey() {
        val placement = place(middleKey, count = 5, anchorIndex = 2)
        assertEquals(middleKey.centerX, placement.itemBounds(2).centerX, 0.01f)
    }

    @Test fun aStripOverTheLeftmostKeyStaysInsideTheKeyboard() {
        val placement = place(Bounds(0f, 100f, 40f, 40f))
        assertEquals(host.left, placement.bounds.left, 0.01f)
        assertTrue(placement.itemBounds(3).right <= host.right)
    }

    @Test fun aStripOverTheRightmostKeyStaysInsideTheKeyboard() {
        val placement = place(Bounds(360f, 100f, 40f, 40f))
        assertEquals(host.right, placement.bounds.right, 0.01f)
        assertTrue(placement.bounds.left >= host.left)
    }

    @Test fun aKeyOnTheTopRowGetsItsStripFlippedUnderneath() {
        val placement = place(Bounds(180f, 0f, 40f, 40f))
        assertFalse(placement.above)
        assertTrue(placement.bounds.top >= placement.key.bottom)
    }

    @Test fun capacityIsWhatFitsAcrossTheKeyboard() {
        // (400 - 2*4) / 40 = 9.8
        assertEquals(9, AlternatesGeometry.capacity(host.width, metrics))
        // Never zero: a caller with one alternate should still get a chance to show it.
        assertEquals(1, AlternatesGeometry.capacity(10f, metrics))
    }

    // ---- hit testing ----

    private fun StripPlacement.highlightAt(x: Float, y: Float) =
        AlternatesGeometry.highlight(this, x, y)

    private val StripPlacement.stripCenterY: Float get() = bounds.top + bounds.height / 2f

    @Test fun eachEntryIsPickedByItsOwnCentre() {
        val placement = place(middleKey)
        for (index in 0 until placement.count) {
            assertEquals(
                index,
                placement.highlightAt(placement.itemBounds(index).centerX, placement.stripCenterY),
            )
        }
    }

    @Test fun anEntryBoundaryBelongsToTheEntryOnItsRight() {
        val placement = place(middleKey)
        assertEquals(1, placement.highlightAt(placement.itemBounds(1).left, placement.stripCenterY))
    }

    @Test fun theEndEntriesAreGreedyOutToTheSlop() {
        val placement = place(middleKey)
        val justOutside = placement.bounds.left - metrics.slop / 2f
        assertEquals(0, placement.highlightAt(justOutside, placement.stripCenterY))

        val wellOutside = placement.bounds.left - metrics.slop * 2f
        assertEquals(NONE, placement.highlightAt(wellOutside, placement.stripCenterY))
    }

    @Test fun aFingerStillOnItsOwnKeyPicksTheAnchor() {
        val placement = place(middleKey, count = 5, anchorIndex = 2)
        assertEquals(2, placement.highlightAt(middleKey.centerX, middleKey.top + 5f))
    }

    @Test fun aFingerDraggedSidewaysOutOfTheKeyPicksNothing() {
        val placement = place(middleKey)
        assertEquals(NONE, placement.highlightAt(host.right, middleKey.top + 5f))
    }

    @Test fun aFingerDraggedBelowTheKeyPicksNothing() {
        val placement = place(middleKey)
        assertEquals(NONE, placement.highlightAt(middleKey.centerX, middleKey.bottom + 100f))
    }

    @Test fun aClampedStripStillPicksTheAnchorFromTheKeyItself() {
        val leftmost = Bounds(0f, 100f, 40f, 40f)
        val placement = place(leftmost)
        assertEquals(0, placement.highlightAt(leftmost.centerX, leftmost.top + 5f))
    }

    @Test fun aSingleEntryIsPickedAnywhereAcrossTheStrip() {
        val placement = place(middleKey, count = 1)
        assertEquals(0, placement.highlightAt(placement.bounds.left + 1f, placement.stripCenterY))
        assertEquals(0, placement.highlightAt(placement.bounds.right - 1f, placement.stripCenterY))
    }

    @Test fun aFlippedStripPicksItsEntriesTheSameWay() {
        val placement = place(Bounds(180f, 0f, 40f, 40f))
        assertEquals(2, placement.highlightAt(placement.itemBounds(2).centerX, placement.stripCenterY))
        // The corridor is now above the strip rather than below it.
        assertEquals(0, placement.highlightAt(placement.key.centerX, placement.key.top + 5f))
    }
}
