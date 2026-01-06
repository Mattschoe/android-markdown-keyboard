package com.creategoodthings.markdownKeyboard.editor.document

import com.creategoodthings.markdownKeyboard.editor.MarkdownSyntax

data class ListEntry(
    val level: Int,
    /** null when the line carries no marker: a continuation line, or an item that lost its own. */
    val marker: ListMarker?,
    val content: String,
) {
    val prefix: String
        get() = MarkdownSyntax.INDENT_UNIT.repeat(level) + (marker?.render() ?: "")

    fun render(): String = prefix + content
}

/** Where the caret sits, expressed against the structure rather than against the text. */
data class ListCursor(val entry: Int, val column: Int)

/**
 * The contiguous run of list lines the caret stands in, as structure rather than as text.
 *
 * Every list key follows the same pipeline: mutate, [normalized], [render], diff. Renumbering
 * is therefore not something Enter does and Backspace forgets to do; it is [normalized], run
 * unconditionally, in one place, for all of them.
 */
data class ListBlock(
    val entries: List<ListEntry>,
    val cursor: ListCursor,
) {
    val currentEntry: ListEntry get() = entries[cursor.entry]

    /** Enter mid-item: the text after the caret moves down into a fresh item. */
    fun splitAtCursor(): ListBlock {
        val entry = currentEntry
        val column = cursor.column.coerceIn(0, entry.content.length)
        val carried = when (val marker = entry.marker) {
            // A new task always starts unticked, whatever the item above it looks like.
            is ListMarker.Task -> marker.copy(checked = false)
            // The number is a placeholder; normalized() decides what it really is.
            else -> marker
        }
        val updated = entries.toMutableList()
        updated[cursor.entry] = entry.copy(content = entry.content.take(column))
        updated.add(cursor.entry + 1, ListEntry(entry.level, carried, entry.content.drop(column)))
        return copy(entries = updated, cursor = ListCursor(cursor.entry + 1, 0))
    }

    fun setMarker(index: Int, marker: ListMarker?): ListBlock {
        val updated = entries.toMutableList()
        updated[index] = updated[index].copy(marker = marker)
        return copy(entries = updated)
    }

    /** Strips an entry back to a plain, unindented line, keeping whatever it said. */
    fun clearEntry(index: Int): ListBlock {
        val updated = entries.toMutableList()
        updated[index] = updated[index].copy(level = 0, marker = null)
        val moved = if (cursor.entry == index) ListCursor(index, 0) else cursor
        return copy(entries = updated, cursor = moved)
    }

    /**
     * Moves an entry one nesting level, carrying its children with it.
     *
     * An item can never sit more than one level deeper than the item above it, which is what
     * keeps repeated indent presses from producing a list no parser will accept.
     */
    fun changeIndent(index: Int, delta: Int): ListBlock {
        val base = entries[index].level
        val ceiling = if (index == 0) 0 else entries[index - 1].level + 1
        val shift = (base + delta).coerceIn(0, ceiling) - base
        if (shift == 0) return this

        val updated = entries.toMutableList()
        var i = index
        while (i < updated.size && (i == index || updated[i].level > base)) {
            updated[i] = updated[i].copy(level = (updated[i].level + shift).coerceAtLeast(0))
            i++
        }
        return copy(entries = updated)
    }

    /**
     * Renumbers every ordered run, per nesting level.
     *
     * The first ordered item in the block keeps its own number, so a list that deliberately
     * starts at 5 stays there. Every later run restarts at 1. A bullet or task interrupting a
     * run ends it; an unmarked continuation line does not, because markdown renders it as part
     * of the item above. A blank unmarked line does end the list, and what follows starts over.
     */
    fun normalized(): ListBlock {
        val counters = HashMap<Int, Int>()
        var seenOrdered = false

        val updated = entries.map { entry ->
            counters.keys.filter { it > entry.level }.toList().forEach(counters::remove)
            when (val marker = entry.marker) {
                is ListMarker.Ordered -> {
                    val number = counters[entry.level]?.plus(1)
                        ?: if (seenOrdered) 1 else marker.number
                    seenOrdered = true
                    counters[entry.level] = number
                    entry.copy(marker = marker.copy(number = number))
                }

                null -> {
                    if (entry.content.isBlank()) counters.clear()
                    entry
                }

                else -> {
                    counters.remove(entry.level)
                    entry
                }
            }
        }
        return copy(entries = updated)
    }

    fun render(): String = entries.joinToString("\n") { it.render() }

    /** Where the caret ends up within [render]'s output. */
    fun cursorOffset(): Int {
        var offset = 0
        for (i in 0 until cursor.entry) offset += entries[i].render().length + 1
        val entry = entries[cursor.entry]
        return offset + entry.prefix.length + cursor.column.coerceIn(0, entry.content.length)
    }
}
