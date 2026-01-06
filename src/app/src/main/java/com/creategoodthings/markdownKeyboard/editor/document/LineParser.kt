package com.creategoodthings.markdownKeyboard.editor.document

import com.creategoodthings.markdownKeyboard.editor.MarkdownSyntax

/**
 * Splits snapshot text into classified [Line]s.
 *
 * Recognition is more permissive than what the keyboard emits: any of `-*+` opens a bullet,
 * `.` or `)` closes an ordered marker, and indentation may be spaces or tabs of any width.
 * Text written elsewhere still has to parse.
 */
object LineParser {
    private val TASK = Regex("""^([ \t]*)([-*+]) \[([ xX])] ?(.*)$""")
    private val ORDERED = Regex("""^([ \t]*)(\d{1,9})([.)]) (.*)$""")
    private val BULLET = Regex("""^([ \t]*)([-*+]) (.*)$""")
    private val HEADING = Regex("""^(#{1,6}) (.*)$""")
    private val QUOTE = Regex("""^((?:> ?)+)(.*)$""")
    private val FENCE = Regex("""^[ \t]*(```|~~~).*$""")

    fun parse(text: String): List<Line> {
        val lines = ArrayList<Line>()
        var start = 0
        var index = 0
        while (true) {
            val newline = text.indexOf('\n', start)
            val end = if (newline < 0) text.length else newline
            val body = text.substring(start, end)
            lines += Line(index, start, end, body, classify(body))
            if (newline < 0) break
            start = newline + 1
            index++
        }
        return lines
    }

    fun classify(line: String): LineType {
        if (line.isBlank()) return LineType.Blank

        FENCE.matchEntire(line)?.let { return LineType.CodeFence(it.groupValues[1]) }

        TASK.matchEntire(line)?.let {
            val (indent, bullet, box) = it.destructured
            return listItem(line, indent, ListMarker.Task(bullet[0], checked = box[0] != ' '))
        }

        ORDERED.matchEntire(line)?.let {
            val (indent, number, delimiter) = it.destructured
            return listItem(line, indent, ListMarker.Ordered(number.toInt(), delimiter[0]))
        }

        BULLET.matchEntire(line)?.let {
            val (indent, bullet) = it.destructured
            return listItem(line, indent, ListMarker.Bullet(bullet[0]))
        }

        HEADING.matchEntire(line)?.let {
            val level = it.groupValues[1].length
            return LineType.Heading(level, level + 1)
        }

        QUOTE.matchEntire(line)?.let {
            val prefix = it.groupValues[1]
            return LineType.Quote(prefix.count { c -> c == '>' }, prefix.length)
        }

        return LineType.Paragraph
    }

    /**
     * A marker may be written without its trailing space (`- [ ]` at the end of a line), so the
     * content column is clamped to the line rather than trusting the marker's rendered length.
     */
    private fun listItem(line: String, indent: String, marker: ListMarker): LineType.ListItem =
        LineType.ListItem(
            indent = indent,
            marker = marker,
            contentColumn = (indent.length + marker.render().length).coerceAtMost(line.length),
        )

    /** Visual width of an indent string, counting a tab as [MarkdownSyntax.TAB_WIDTH]. */
    fun indentWidth(indent: String): Int =
        indent.sumOf { if (it == '\t') MarkdownSyntax.TAB_WIDTH else 1 }
}
