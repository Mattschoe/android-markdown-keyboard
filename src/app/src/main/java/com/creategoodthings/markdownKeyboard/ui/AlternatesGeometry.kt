package com.creategoodthings.markdownKeyboard.ui

import kotlin.math.floor

/**
 * Where the alternates strip goes and which of its entries the finger is over.
 *
 * All of the strip's arithmetic lives here, and — like [ShiftState] — the file is free of Android
 * and Compose types so it can be tested on the host JVM. That is deliberate down to not reusing
 * `androidx.compose.ui.geometry.Rect`: compose-ui is not on the unit test classpath.
 *
 * Coordinates are pixels in the overlay's own space, which is the keyboard's outermost box.
 */
data class Bounds(val left: Float, val top: Float, val width: Float, val height: Float) {
    val right: Float get() = left + width
    val bottom: Float get() = top + height
    val centerX: Float get() = left + width / 2f
}

/** The strip's dimensions, converted from dp once at the current density. */
data class StripMetrics(
    val itemWidth: Float,
    val itemHeight: Float,
    /** Inset between the strip's edge and its items, on all four sides. */
    val padding: Float,
    /** Vertical clearance between the strip and the key it belongs to. */
    val gap: Float,
    /** How far past an edge the finger may stray and still count as inside. */
    val slop: Float,
)

data class StripPlacement(
    val bounds: Bounds,
    val count: Int,
    /** The entry parked over the key: the default pick, and what the corridor resolves to. */
    val anchorIndex: Int,
    /** False when the strip had to be flipped under the key for want of room above. */
    val above: Boolean,
    val key: Bounds,
    val metrics: StripMetrics,
) {
    fun itemBounds(index: Int): Bounds = Bounds(
        left = bounds.left + metrics.padding + index * metrics.itemWidth,
        top = bounds.top + metrics.padding,
        width = metrics.itemWidth,
        height = metrics.itemHeight,
    )
}

object AlternatesGeometry {
    /** [highlight] returns this when releasing should commit nothing. */
    const val NONE = -1

    /** How many entries fit across the keyboard; a wider strip could not be clamped inside it. */
    fun capacity(hostWidth: Float, metrics: StripMetrics): Int =
        floor((hostWidth - 2f * metrics.padding) / metrics.itemWidth).toInt().coerceAtLeast(1)

    /**
     * Parks entry [anchorIndex] over [key]'s centre, then slides the whole strip back inside
     * [host]. At the far left the strip drifts right and at the far right it drifts left, which
     * is why the anchor is a starting point rather than a guarantee.
     */
    fun place(
        key: Bounds,
        count: Int,
        anchorIndex: Int,
        host: Bounds,
        metrics: StripMetrics,
    ): StripPlacement {
        val width = count * metrics.itemWidth + 2f * metrics.padding
        val height = metrics.itemHeight + 2f * metrics.padding

        val wanted =
            key.centerX - metrics.itemWidth / 2f - metrics.padding - anchorIndex * metrics.itemWidth
        val left = if (width >= host.width) host.left
        else wanted.coerceIn(host.left, host.right - width)

        // Above the key by preference. A key on the top row has nowhere to pop to, so the strip
        // flips underneath rather than being drawn off the top of the keyboard.
        val preferredTop = key.top - metrics.gap - height
        val above = preferredTop >= host.top
        val top = if (above) preferredTop
        else (key.bottom + metrics.gap).coerceAtMost((host.bottom - height).coerceAtLeast(host.top))

        return StripPlacement(
            bounds = Bounds(left, top, width, height),
            count = count,
            anchorIndex = anchorIndex,
            above = above,
            key = key,
            metrics = metrics,
        )
    }

    /**
     * @return the entry under ([x], [y]); [StripPlacement.anchorIndex] while the finger is still
     *   in the corridor between the strip and its own key, so a hold-and-release without sliding
     *   commits the character the key already had; [NONE] once the finger has left both, which is
     *   the only way to close the strip without typing anything.
     */
    fun highlight(placement: StripPlacement, x: Float, y: Float): Int {
        val strip = placement.bounds
        val key = placement.key
        val metrics = placement.metrics

        val inCorridor = if (placement.above) y > strip.bottom && y <= key.bottom
        else y >= key.top && y < strip.top
        if (inCorridor) {
            val overKey = x >= key.left - metrics.slop && x <= key.right + metrics.slop
            return if (overKey) placement.anchorIndex else NONE
        }

        if (y < strip.top - metrics.slop || y > strip.bottom + metrics.slop) return NONE
        if (x < strip.left - metrics.slop || x > strip.right + metrics.slop) return NONE

        // The end entries are greedy over the strip's own padding and its slop, so a slide that
        // overshoots the last character still picks it rather than cancelling.
        val offset = x - (strip.left + metrics.padding)
        return floor(offset / metrics.itemWidth).toInt().coerceIn(0, placement.count - 1)
    }
}
