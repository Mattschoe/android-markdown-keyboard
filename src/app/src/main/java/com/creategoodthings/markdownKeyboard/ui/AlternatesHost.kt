package com.creategoodthings.markdownKeyboard.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.creategoodthings.markdownKeyboard.ui.theme.LocalKeyboardColors
import kotlin.math.roundToInt

/** Slide targets are deliberately wider than a letter key: the finger is already moving. */
private const val STRIP_ITEM_WIDTH = 44f
private const val STRIP_ITEM_HEIGHT = 46f
private const val STRIP_PADDING = 4f
private const val STRIP_GAP = 6f
private const val STRIP_SLOP = 24f
private const val STRIP_CORNER_RADIUS = 12f
private const val STRIP_ITEM_CORNER_RADIUS = 8f
private const val STRIP_ELEVATION = 6f

/**
 * The open strip, if any, and the keyboard box it is drawn in.
 *
 * Reached through [LocalAlternatesHost] rather than passed down, so that adding alternates to a
 * key costs one field on its [KeyItem] and nothing at the ~40 call sites that have none. The
 * keyboard's *actions* are still passed in as a parameter, which is what keeps the keyboard
 * composable in a preview with no service behind it; this local defaults to null, and a [Key]
 * with no host simply falls back to firing its own action on hold.
 */
@Stable
internal class AlternatesHostState(private val metrics: StripMetrics) {

    var strip by mutableStateOf<StripState?>(null)
        private set

    private var hostCoordinates: LayoutCoordinates? = null

    /** The originating key's top-left in overlay space, so [move] can take key-local positions. */
    private var keyOrigin = Offset.Zero

    fun attach(coordinates: LayoutCoordinates) {
        hostCoordinates = coordinates
    }

    /** @return false when there is no overlay or no room, and the caller should fall back. */
    fun open(keyCoordinates: LayoutCoordinates?, values: List<String>, baseIndex: Int): Boolean {
        val overlay = hostCoordinates?.takeIf { it.isAttached } ?: return false
        val key = keyCoordinates?.takeIf { it.isAttached } ?: return false
        if (values.isEmpty()) return false

        val host = Bounds(0f, 0f, overlay.size.width.toFloat(), overlay.size.height.toFloat())
        if (values.size > AlternatesGeometry.capacity(host.width, metrics)) return false

        // The one coordinate conversion in the whole feature.
        val rect = overlay.localBoundingBoxOf(key, clipBounds = false)
        keyOrigin = Offset(rect.left, rect.top)
        val keyBounds = Bounds(rect.left, rect.top, rect.width, rect.height)

        val placement = AlternatesGeometry.place(
            key = keyBounds,
            count = values.size,
            anchorIndex = baseIndex.coerceIn(0, values.lastIndex),
            host = host,
            metrics = metrics,
        )
        strip = StripState(values, placement, highlighted = placement.anchorIndex)
        return true
    }

    /**
     * @param position the finger in the originating key's local space.
     * @return true when the highlight moved, so the caller can tick the haptic.
     */
    fun move(position: Offset): Boolean {
        val open = strip ?: return false
        val index = AlternatesGeometry.highlight(
            open.placement,
            keyOrigin.x + position.x,
            keyOrigin.y + position.y,
        )
        if (index == open.highlighted) return false
        strip = open.copy(highlighted = index)
        return true
    }

    /** @return the character to commit, or null for "commit nothing". */
    fun close(): String? {
        val open = strip ?: return null
        strip = null
        return open.values.getOrNull(open.highlighted)
    }
}

internal data class StripState(
    val values: List<String>,
    val placement: StripPlacement,
    val highlighted: Int,
)

internal val LocalAlternatesHost = staticCompositionLocalOf<AlternatesHostState?> { null }

internal fun stripMetrics(density: Density): StripMetrics = with(density) {
    StripMetrics(
        itemWidth = STRIP_ITEM_WIDTH.dp.toPx(),
        itemHeight = STRIP_ITEM_HEIGHT.dp.toPx(),
        padding = STRIP_PADDING.dp.toPx(),
        gap = STRIP_GAP.dp.toPx(),
        slop = STRIP_SLOP.dp.toPx(),
    )
}

/**
 * The strip itself. Drawn as the last child of the keyboard's box rather than in a `Popup`: a
 * popup is a separate window, and Android delivers a whole gesture to the window that saw the
 * first touch, so the strip could never see the sliding finger anyway. Being in-tree also means
 * no window token to go stale while the input view is being recreated.
 */
@Composable
internal fun AlternatesStripView(state: StripState) {
    val colors = LocalKeyboardColors.current
    val shape = RoundedCornerShape(STRIP_CORNER_RADIUS.dp)

    Row(
        modifier = Modifier
            .offset {
                IntOffset(
                    state.placement.bounds.left.roundToInt(),
                    state.placement.bounds.top.roundToInt(),
                )
            }
            .shadow(STRIP_ELEVATION.dp, shape)
            .background(colors.strip, shape)
            .padding(STRIP_PADDING.dp)
            // Decorative: the key underneath carries the accessibility actions, and leaving this
            // in the tree would put phantom nodes over the keys for TalkBack to find.
            .clearAndSetSemantics {},
    ) {
        state.values.forEachIndexed { index, value ->
            val on = index == state.highlighted
            Box(
                modifier = Modifier
                    .size(STRIP_ITEM_WIDTH.dp, STRIP_ITEM_HEIGHT.dp)
                    .clip(RoundedCornerShape(STRIP_ITEM_CORNER_RADIUS.dp))
                    .background(if (on) colors.stripHighlight else Color.Transparent),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = value,
                    fontSize = KEY_FONT_SIZE.sp,
                    color = if (on) colors.onStripHighlight else colors.onStrip,
                )
            }
        }
    }
}
