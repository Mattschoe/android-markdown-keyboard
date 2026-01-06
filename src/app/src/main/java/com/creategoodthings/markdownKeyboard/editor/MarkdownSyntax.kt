package com.creategoodthings.markdownKeyboard.editor

/**
 * The one place that decides which markdown this keyboard *writes*.
 *
 * Parsing stays deliberately more tolerant than this: text arriving from Obsidian, a git diff
 * or another keyboard may use two spaces or tabs, and we still have to understand it.
 */
object MarkdownSyntax {
    /** One nesting level, as emitted. */
    const val INDENT_UNIT = "    "

    /** How many columns an existing tab is worth when measuring someone else's indentation. */
    const val TAB_WIDTH = 4

    const val DEFAULT_BULLET = '-'
    const val DEFAULT_ORDERED_DELIMITER = '.'

    /** Heading key cycles none -> H1 -> ... -> this level -> none. */
    const val MAX_HEADING_LEVEL = 3

    /** Opens and closes a code block. */
    const val FENCE = "```"

    /**
     * The skeletons the insertion keys write, kept here so the rules that *recognise* an
     * abandoned one — see [com.creategoodthings.markdownKeyboard.editor.document.Skeletons] —
     * cannot drift away from what was written.
     */
    const val LINK_SKELETON = "[]()"
    const val IMAGE_SKELETON = "![]()"

    val TABLE_SKELETON = listOf(
        "|  |  |",
        "| --- | --- |",
        "|  |  |",
    ).joinToString("\n")

    /** Inside the first header cell. */
    const val TABLE_CARET = 2
}

enum class InlineStyle(val delimiter: String) {
    Bold("**"),
    Italic("*"),
    Code("`"),
    Strikethrough("~~"),
}

enum class ListKind { Bullet, Ordered, Task }
