package com.creategoodthings.markdownKeyboard.editor

import kotlin.math.min

/**
 * The result of a keypress: one replacement over [Snapshot.text], plus where the cursor lands.
 *
 * A single range rather than a script of editor calls, so that rules stay comparable in tests
 * and the code that talks to Android stays one small function.
 *
 * All offsets are into [Snapshot.text]. [newSelectionStart] and [newSelectionEnd] are read
 * against the text *after* the replacement; everything before [replaceStart] is unchanged, so
 * the two coordinate systems agree up to that point.
 */
data class TextEdit(
    val replaceStart: Int,
    val replaceEnd: Int,
    val replacement: String,
    val newSelectionStart: Int,
    val newSelectionEnd: Int = newSelectionStart,
) {
    companion object {
        /** Replaces the current selection (or inserts at the caret) with [text]. */
        fun insert(snapshot: Snapshot, text: String, cursorOffset: Int = text.length): TextEdit =
            TextEdit(
                replaceStart = snapshot.selectionStart,
                replaceEnd = snapshot.selectionEnd,
                replacement = text,
                newSelectionStart = snapshot.selectionStart + cursorOffset,
            )

        /**
         * Turns `snapshot.text[regionStart, regionEnd)` into [newRegion] using the smallest
         * replacement that does the job, by trimming the head and tail that did not change.
         *
         * Rendering a whole list block and replacing it wholesale would work, but it flickers
         * and flattens the host app's undo history; trimming usually gets a five-line renumber
         * down to the two lines that actually moved.
         *
         * The trim is then widened back out so the edit always spans the current selection.
         * That is what lets [com.creategoodthings.markdownKeyboard.ime.EditApplier] work purely
         * in cursor-relative terms, with no dependence on absolute document offsets.
         *
         * Returns null when neither the text nor the cursor would move.
         */
        fun replacingRegion(
            snapshot: Snapshot,
            regionStart: Int,
            regionEnd: Int,
            newRegion: String,
            newSelectionStart: Int,
            newSelectionEnd: Int = newSelectionStart,
        ): TextEdit? {
            val oldRegion = snapshot.text.substring(regionStart, regionEnd)
            val unchanged = oldRegion == newRegion &&
                newSelectionStart == snapshot.selectionStart &&
                newSelectionEnd == snapshot.selectionEnd
            if (unchanged) return null

            val maxAffix = min(oldRegion.length, newRegion.length)
            var prefix = 0
            while (prefix < maxAffix && oldRegion[prefix] == newRegion[prefix]) prefix++
            var suffix = 0
            while (
                suffix < maxAffix - prefix &&
                oldRegion[oldRegion.length - 1 - suffix] == newRegion[newRegion.length - 1 - suffix]
            ) suffix++

            prefix = min(prefix, (snapshot.selectionStart - regionStart).coerceAtLeast(0))
            suffix = min(suffix, (regionEnd - snapshot.selectionEnd).coerceAtLeast(0))

            return TextEdit(
                replaceStart = regionStart + prefix,
                replaceEnd = regionEnd - suffix,
                replacement = newRegion.substring(prefix, newRegion.length - suffix),
                newSelectionStart = newSelectionStart,
                newSelectionEnd = newSelectionEnd,
            )
        }
    }
}
