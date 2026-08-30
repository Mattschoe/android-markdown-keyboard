package com.creategoodthings.markdownKeyboard.editor.document

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Classifying one line at a time. */
class LineParserTest {

    @Test fun blankLines() {
        assertEquals(LineType.Blank, LineParser.classify(""))
        assertEquals(LineType.Blank, LineParser.classify("   "))
        assertEquals(LineType.Blank, LineParser.classify("\t"))
    }

    @Test fun plainText() {
        assertEquals(LineType.Paragraph, LineParser.classify("plain text"))
    }

    @Test fun everyBulletCharacterOpensAList() {
        for (bullet in listOf('-', '*', '+')) {
            val type = LineParser.classify("$bullet a") as LineType.ListItem
            assertEquals(ListMarker.Bullet(bullet), type.marker)
            assertEquals(2, type.contentColumn)
        }
    }

    @Test fun orderedMarkersTakeEitherDelimiter() {
        val dot = LineParser.classify("1. a") as LineType.ListItem
        assertEquals(ListMarker.Ordered(1, '.'), dot.marker)
        assertEquals(3, dot.contentColumn)

        val paren = LineParser.classify("1) a") as LineType.ListItem
        assertEquals(ListMarker.Ordered(1, ')'), paren.marker)
        assertEquals(3, paren.contentColumn)

        val wide = LineParser.classify("12. a") as LineType.ListItem
        assertEquals(ListMarker.Ordered(12, '.'), wide.marker)
        assertEquals(4, wide.contentColumn)
    }

    @Test fun tasks() {
        val unchecked = LineParser.classify("- [ ] a") as LineType.ListItem
        assertEquals(ListMarker.Task('-', checked = false), unchecked.marker)
        assertEquals(6, unchecked.contentColumn)

        for (box in listOf("x", "X")) {
            val checked = LineParser.classify("- [$box] a") as LineType.ListItem
            assertEquals(ListMarker.Task('-', checked = true), checked.marker)
        }
    }

    @Test fun aMarkerWithNoContentIsStillAListItem() {
        val type = LineParser.classify("- ") as LineType.ListItem
        assertEquals(2, type.contentColumn)
        assertEquals("", "- ".substring(type.contentColumn))
    }

    /** The marker may be written without its trailing space, so the column has to be clamped. */
    @Test fun taskMarkerWithoutTrailingSpaceDoesNotOverrunTheLine() {
        val type = LineParser.classify("- [ ]") as LineType.ListItem
        assertEquals(5, type.contentColumn)
        assertEquals("", "- [ ]".substring(type.contentColumn))
    }

    @Test fun aMarkerNeedsItsSpace() {
        assertEquals(LineType.Paragraph, LineParser.classify("-"))
        assertEquals(LineType.Paragraph, LineParser.classify("1."))
        assertEquals(LineType.Paragraph, LineParser.classify("#h"))
    }

    @Test fun headings() {
        for (level in 1..6) {
            assertEquals(LineType.Heading(level, level + 1), LineParser.classify("#".repeat(level) + " h"))
        }
        assertEquals(LineType.Paragraph, LineParser.classify("####### h"))
    }

    @Test fun quotes() {
        assertEquals(LineType.Quote(1, 2), LineParser.classify("> q"))
        assertEquals(LineType.Quote(2, 4), LineParser.classify("> > q"))
    }

    @Test fun codeFences() {
        assertEquals(LineType.CodeFence("```"), LineParser.classify("```"))
        assertEquals(LineType.CodeFence("```"), LineParser.classify("```kotlin"))
        assertEquals(LineType.CodeFence("~~~"), LineParser.classify("~~~"))
    }

    @Test fun indentationIsKeptVerbatim() {
        assertEquals("    ", (LineParser.classify("    - a") as LineType.ListItem).indent)
        assertEquals("\t", (LineParser.classify("\t- a") as LineType.ListItem).indent)
    }

    @Test fun indentWidthCountsATabAsFour() {
        assertEquals(0, LineParser.indentWidth(""))
        assertEquals(2, LineParser.indentWidth("  "))
        assertEquals(4, LineParser.indentWidth("    "))
        assertEquals(4, LineParser.indentWidth("\t"))
        assertEquals(6, LineParser.indentWidth("  \t"))
    }

    @Test fun lineOffsetsAreContiguous() {
        val lines = LineParser.parse("one\ntwo\nthree")
        assertEquals(listOf(0, 4, 8), lines.map { it.start })
        assertEquals(listOf(3, 7, 13), lines.map { it.end })
        assertEquals(listOf("one", "two", "three"), lines.map { it.text })
    }

    @Test fun aTrailingNewlineProducesAFinalEmptyLine() {
        val lines = LineParser.parse("a\n")
        assertEquals(2, lines.size)
        assertEquals("", lines[1].text)
        assertEquals(2, lines[1].start)
        assertEquals(2, lines[1].end)
    }

    @Test fun oneLineWithNoNewlineIsOneLine() {
        assertEquals(1, LineParser.parse("solo").size)
    }

    @Test fun emptyTextIsOneEmptyLine() {
        val lines = LineParser.parse("")
        assertEquals(1, lines.size)
        assertEquals(0, lines.lineAt(0)?.index)
    }

    @Test fun lineAtResolvesBothEdgesOfALine() {
        val lines = LineParser.parse("ab\ncd")
        assertEquals(0, lines.lineAt(0)?.index)  // line start
        assertEquals(0, lines.lineAt(2)?.index)  // line end, before the newline
        assertEquals(1, lines.lineAt(3)?.index)  // start of the next line
        assertEquals(1, lines.lineAt(5)?.index)  // end of the document
        assertNull(lines.lineAt(6))
    }
}
