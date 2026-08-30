package com.creategoodthings.markdownKeyboard.editor.document

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

/** Finding paired inline delimiters on a line. */
class InlineSpansTest {

    @Test fun aClosedPairIsOneSpan() {
        assertEquals(
            listOf(InlineSpans.Span(0, 2, 3, 5)),
            InlineSpans.find("**a**", "**"),
        )
    }

    @Test fun noDelimitersMeansNoSpans() {
        assertEquals(emptyList<InlineSpans.Span>(), InlineSpans.find("a", "**"))
    }

    @Test fun twoPairsAreTwoSpans() {
        assertEquals(2, InlineSpans.find("*a* *b*", "*").size)
    }

    @Test fun anUnclosedTrailingDelimiterIsIgnored() {
        assertEquals(emptyList<InlineSpans.Span>(), InlineSpans.find("*a", "*"))
        assertEquals(1, InlineSpans.find("*a* *b", "*").size)
    }

    /** Bold must not register as two italics, or the two keys would fight over the same text. */
    @Test fun aDoubleDelimiterIsNotTwoSingles() {
        assertEquals(emptyList<InlineSpans.Span>(), InlineSpans.find("**a**", "*"))
    }

    @Test fun aRunOfThreeIsNotAnExactPair() {
        assertEquals(emptyList<InlineSpans.Span>(), InlineSpans.find("***a***", "**"))
        assertEquals(emptyList<InlineSpans.Span>(), InlineSpans.find("***a***", "*"))
    }

    @Test fun anEscapedDelimiterDoesNotOpenASpan() {
        assertEquals(emptyList<InlineSpans.Span>(), InlineSpans.find("\\*a\\*", "*"))
    }

    /** An escaped backslash is not an escape, so the delimiter after it counts. */
    @Test fun anEscapedBackslashDoesNotEscapeTheDelimiter() {
        assertEquals(1, InlineSpans.find("\\\\*a*", "*").size)
    }

    @Test fun allFourDelimitersAreFound() {
        assertEquals(1, InlineSpans.find("**a**", "**").size)
        assertEquals(1, InlineSpans.find("*a*", "*").size)
        assertEquals(1, InlineSpans.find("`a`", "`").size)
        assertEquals(1, InlineSpans.find("~~a~~", "~~").size)
    }

    @Test fun spanAtCountsTheDelimitersAsInside() {
        val line = "x **ab** y"
        val span = InlineSpans.find(line, "**").single()
        assertEquals(2, span.openStart)
        assertEquals(8, span.closeEnd)

        for (column in span.openStart..span.closeEnd) {
            assertNotNull("column $column", InlineSpans.spanAt(line, "**", column))
        }
    }

    @Test fun spanAtStopsOnePastTheClosingDelimiter() {
        val line = "x **ab** y"
        assertNull(InlineSpans.spanAt(line, "**", 9))
        assertNull(InlineSpans.spanAt(line, "**", 1))
    }

    @Test fun spanAtPicksTheEnclosingPairWhenThereAreSeveral() {
        val line = "*a* *b*"
        assertEquals(InlineSpans.find(line, "*")[1], InlineSpans.spanAt(line, "*", 5))
    }
}
