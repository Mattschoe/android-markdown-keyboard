package com.creategoodthings.markdownKeyboard.editor.document

/**
 * One line of the snapshot, already classified.
 *
 * Markdown's block structure is line-oriented, so this is the unit every block rule works in.
 */
data class Line(
    val index: Int,
    /** Offset of the line's first character in the snapshot text. */
    val start: Int,
    /** Offset one past its last character, not counting the newline. */
    val end: Int,
    val text: String,
    val type: LineType,
)

sealed interface LineType {
    data object Blank : LineType

    data object Paragraph : LineType

    data class Heading(val level: Int, val contentColumn: Int) : LineType

    data class Quote(val depth: Int, val contentColumn: Int) : LineType

    data class CodeFence(val fence: String) : LineType

    data class ListItem(
        val indent: String,
        val marker: ListMarker,
        /** Column within the line where the item's own content starts. */
        val contentColumn: Int,
    ) : LineType
}

/** The line [offset] sits on. Line starts and line ends never collide, so this is unambiguous. */
fun List<Line>.lineAt(offset: Int): Line? = firstOrNull { offset in it.start..it.end }
