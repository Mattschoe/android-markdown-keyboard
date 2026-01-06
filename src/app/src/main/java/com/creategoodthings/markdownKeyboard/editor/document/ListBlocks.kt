package com.creategoodthings.markdownKeyboard.editor.document

/** Finds the [ListBlock] around a caret and remembers where it came from in the text. */
object ListBlocks {
    data class Located(val block: ListBlock, val start: Int, val end: Int)

    fun at(lines: List<Line>, offset: Int): Located? {
        val cursorLine = lines.lineAt(offset) ?: return null
        val cursorType = cursorLine.type as? LineType.ListItem ?: return null

        var first = cursorLine.index
        while (first > 0 && lines[first - 1].type is LineType.ListItem) first--
        var last = cursorLine.index
        while (last < lines.lastIndex && lines[last + 1].type is LineType.ListItem) last++

        val members = lines.subList(first, last + 1)
        val levels = levelsOf(
            members.map { LineParser.indentWidth((it.type as LineType.ListItem).indent) }
        )
        val entries = members.mapIndexed { i, line ->
            val type = line.type as LineType.ListItem
            ListEntry(levels[i], type.marker, line.text.substring(type.contentColumn))
        }

        val cursorIndex = cursorLine.index - first
        val column = (offset - cursorLine.start - cursorType.contentColumn)
            .coerceIn(0, entries[cursorIndex].content.length)

        return Located(
            block = ListBlock(entries, ListCursor(cursorIndex, column)),
            start = members.first().start,
            end = members.last().end,
        )
    }

    /**
     * Turns raw indent widths into nesting levels.
     *
     * Levels come from the *sequence* of widths rather than from dividing by a fixed unit, so a
     * list written elsewhere with two-space indents nests correctly even though we emit four.
     */
    private fun levelsOf(widths: List<Int>): List<Int> {
        val stack = ArrayList<Int>()
        return widths.map { width ->
            while (stack.isNotEmpty() && width < stack.last()) stack.removeAt(stack.lastIndex)
            if (stack.isEmpty() || width > stack.last()) stack.add(width)
            stack.lastIndex
        }
    }
}
