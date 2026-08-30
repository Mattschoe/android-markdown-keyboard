package com.creategoodthings.markdownKeyboard.editor.rules

import com.creategoodthings.markdownKeyboard.assertKey
import com.creategoodthings.markdownKeyboard.editor.KeyAction
import org.junit.Test

/** The heading cycle and the quote toggle. */
class BlockTest {

    private fun heading(before: String, after: String) =
        assertKey(KeyAction.CycleHeading, before, after)

    private fun quote(before: String, after: String) =
        assertKey(KeyAction.ToggleQuote, before, after)

    // ---- verified against the old implementation during the refactor ----

    @Test fun aPlainLineBecomesAHeading() = heading("ti▮tle", "# ti▮tle")

    @Test fun aHeadingDeepens() = heading("# ti▮tle", "## ti▮tle")

    @Test fun theDeepestHeadingCyclesBackToNone() = heading("### ti▮tle", "ti▮tle")

    @Test fun quotingAndUnquoting() {
        quote("a▮b", "> a▮b")
        quote("> a▮b", "a▮b")
    }

    // ---- the whole cycle, and the edges ----

    @Test fun theHeadingCycleIsClosed() {
        heading("▮x", "# ▮x")
        heading("# ▮x", "## ▮x")
        heading("## ▮x", "### ▮x")
        heading("### ▮x", "▮x")
    }

    /** Deeper headings written elsewhere still parse, and the cycle drops them straight back. */
    @Test fun aHeadingDeeperThanTheKeyboardEmitsCyclesStraightToNone() =
        heading("#### ti▮tle", "ti▮tle")

    @Test fun aQuoteToggleOnlyTouchesTheCaretsLine() =
        quote("first\nsec▮ond\nthird", "first\n> sec▮ond\nthird")

    // ---- the rest of the behaviour, written down for the first time ----

    /** The quote goes outside the marker, which is what markdown wants but reads oddly. */
    @Test fun quotingAListItemPrefixesTheMarker() = quote("- ▮a", "> - ▮a")

    @Test fun cyclingAHeadingOnAListItemPrefixesTheMarker() = heading("- ▮a", "# - ▮a")

    /** A bare `#` is a paragraph, not a heading, so the cycle prefixes rather than deepens. */
    @Test fun aBareHashIsNotAHeading() = heading("#▮", "# #▮")

    @Test fun onlyASingleQuoteLevelIsUnderstood() = quote("> > a▮b", "> a▮b")
}
