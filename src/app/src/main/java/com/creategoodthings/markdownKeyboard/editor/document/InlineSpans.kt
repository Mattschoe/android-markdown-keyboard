package com.creategoodthings.markdownKeyboard.editor.document

/** Locates paired inline delimiters (`**`, `*`, `` ` ``, `~~`) within a single line. */
object InlineSpans {
    data class Span(
        val openStart: Int,
        val contentStart: Int,
        val contentEnd: Int,
        val closeEnd: Int,
    )

    /** Delimiter pairs on [line], in the order they open. Unclosed trailing openers are ignored. */
    fun find(line: String, delimiter: String): List<Span> {
        val marks = openings(line, delimiter)
        val spans = ArrayList<Span>()
        var i = 0
        while (i + 1 < marks.size) {
            val open = marks[i]
            val close = marks[i + 1]
            spans += Span(open, open + delimiter.length, close, close + delimiter.length)
            i += 2
        }
        return spans
    }

    /** The pair enclosing [column], counting the delimiters themselves as inside. */
    fun spanAt(line: String, delimiter: String, column: Int): Span? =
        find(line, delimiter).firstOrNull { column in it.openStart..it.closeEnd }

    private fun openings(line: String, delimiter: String): List<Int> {
        val marks = ArrayList<Int>()
        val char = delimiter[0]
        var i = 0
        while (i <= line.length - delimiter.length) {
            val runIsExact = (i == 0 || line[i - 1] != char) &&
                (i + delimiter.length >= line.length || line[i + delimiter.length] != char)
            if (line.startsWith(delimiter, i) && runIsExact && !isEscaped(line, i)) {
                marks += i
                i += delimiter.length
            } else {
                i++
            }
        }
        return marks
    }

    private fun isEscaped(line: String, index: Int): Boolean {
        var backslashes = 0
        var i = index - 1
        while (i >= 0 && line[i] == '\\') {
            backslashes++
            i--
        }
        return backslashes % 2 == 1
    }
}
