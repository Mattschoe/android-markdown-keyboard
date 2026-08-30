package com.creategoodthings.markdownKeyboard.editor.rules

import com.creategoodthings.markdownKeyboard.Marked.NO_EDIT
import com.creategoodthings.markdownKeyboard.applyPurely
import com.creategoodthings.markdownKeyboard.assertKey
import com.creategoodthings.markdownKeyboard.editor.InlineStyle
import com.creategoodthings.markdownKeyboard.editor.KeyAction
import com.creategoodthings.markdownKeyboard.editor.MarkdownEngine
import com.creategoodthings.markdownKeyboard.snapshotOf
import org.junit.Assert.assertEquals
import org.junit.Test

/** Bold, italic, code and strikethrough, as toggles. */
class InlineStyleTest {

    private fun toggle(style: InlineStyle, before: String, after: String) =
        assertKey(KeyAction.ToggleInlineStyle(style), before, after)

    private fun bold(before: String, after: String) = toggle(InlineStyle.Bold, before, after)

    // ---- verified against the old implementation during the refactor ----

    @Test fun anEmptyCaretOpensAPairAroundIt() = bold("a▮", "a**▮**")

    @Test fun aCaretInsideASpanUnwrapsIt() = bold("**ab▮cd**", "ab▮cd")

    @Test fun italicUnwrapsToo() = toggle(InlineStyle.Italic, "*ab▮cd*", "ab▮cd")

    @Test fun aSelectionIsWrappedAndStaysSelected() = bold("x «ab» y", "x **«ab»** y")

    @Test fun aWrappedSelectionIsUnwrappedAndStaysSelected() = bold("x «**ab**» y", "x «ab» y")

    // ---- every delimiter ----

    @Test fun eachStyleOpensItsOwnPair() {
        toggle(InlineStyle.Bold, "▮", "**▮**")
        toggle(InlineStyle.Italic, "▮", "*▮*")
        toggle(InlineStyle.Code, "▮", "`▮`")
        toggle(InlineStyle.Strikethrough, "▮", "~~▮~~")
    }

    @Test fun eachStyleWrapsASelection() {
        toggle(InlineStyle.Bold, "«ab»", "**«ab»**")
        toggle(InlineStyle.Italic, "«ab»", "*«ab»*")
        toggle(InlineStyle.Code, "«ab»", "`«ab»`")
        toggle(InlineStyle.Strikethrough, "«ab»", "~~«ab»~~")
    }

    // ---- the rest of the behaviour, written down for the first time ----

    /** `spanAt` counts the closing delimiter as inside, so a caret right after it unwraps. */
    @Test fun aCaretJustPastAClosingDelimiterUnwrapsTheSpan() = bold("**ab**▮", "ab▮")

    @Test fun aCaretOnTheOpeningDelimiterUnwrapsTheSpan() = bold("▮**ab**", "▮ab")

    @Test fun theInnerSpanOfANestedPairIsTheOneThatToggles() =
        toggle(InlineStyle.Italic, "**bold *ita▮lic* bold**", "**bold ita▮lic bold**")

    @Test fun codeInsideACodeSpanUnwrapsIt() = toggle(InlineStyle.Code, "`co▮de`", "co▮de")

    @Test fun unwrappingFromInsideKeepsTheCaretOnTheSameCharacter() =
        bold("**a▮bc**", "a▮bc")

    @Test fun unwrappingClampsACaretSittingOnTheOpeningDelimiter() = bold("x**▮abc**", "x▮abc")

    @Test fun aSelectionThatIsExactlyTheDelimitersCollapses() = bold("«****»", "▮")

    // ---- an abandoned empty pair, rather than a second one inside it ----

    /** `InlineSpans` will not read a run of four as an exact pair, so the handler matches runs. */
    @Test fun pressingAnInlineStyleTwiceCancelsThePair() {
        bold("a**▮**", "a▮")
        toggle(InlineStyle.Italic, "a*▮*", "a▮")
        toggle(InlineStyle.Strikethrough, "a~~▮~~", "a▮")
    }

    /** Only the pressed style's own pair: a run of two asterisks is bold's, not italic's. */
    @Test fun anotherStyleInsideAnEmptyPairStillOpensItsOwn() =
        toggle(InlineStyle.Italic, "a**▮**", "a***▮***")

    /**
     * Bold, italic, italic: the second italic press takes back the italic the first one added,
     * rather than stacking a third pair on top of it.
     */
    @Test fun aStyleTakesBackItsOwnDelimiterAndLeavesTheOneUnderneath() {
        bold("▮", "**▮**")
        toggle(InlineStyle.Italic, "**▮**", "***▮***")
        toggle(InlineStyle.Italic, "***▮***", "**▮**")
        bold("**▮**", "▮")
    }

    /** Bold, italic, bold: the bold pair comes off from under the italic, leaving the italic. */
    @Test fun aStyleComesOffFromUnderTheOneAboveIt() {
        bold("▮", "**▮**")
        toggle(InlineStyle.Italic, "**▮**", "***▮***")
        bold("***▮***", "*▮*")
        toggle(InlineStyle.Italic, "*▮*", "▮")
    }

    /** The run is read outside in, longest first, so four asterisks are two bold pairs. */
    @Test fun aRunIsReadAsTheDelimitersItWasWrittenFrom() {
        bold("a****▮****", "a**▮**")
        toggle(InlineStyle.Italic, "a****▮****", "a*****▮*****")
    }

    /** A style that is not in the stack opens, whichever way round the two were pressed. */
    @Test fun theOrderOfTheStackDoesNotMatter() {
        toggle(InlineStyle.Italic, "▮", "*▮*")
        bold("*▮*", "***▮***")
        toggle(InlineStyle.Italic, "***▮***", "**▮**")
    }

    /** Code escalates instead, because an empty code span is a block the user has not opened yet. */
    @Test fun theCodeKeyInsideAnEmptyCodeSpanOpensAFencedBlock() =
        toggle(InlineStyle.Code, "`▮`", "```\n▮\n```")

    /** Including the doubled pair an earlier version of the keyboard left behind. */
    @Test fun aDoubledCodePairEscalatesToo() =
        toggle(InlineStyle.Code, "``▮``", "```\n▮\n```")

    /** A block gets its own line, the same way the code block key gives itself one. */
    @Test fun escalatingMidLineBreaksTheLineFirst() =
        toggle(InlineStyle.Code, "text `▮`", "text \n```\n▮\n```")

    /** And the empty block folds back down, so the key walks the ladder in both directions. */
    @Test fun theCodeKeyInsideAnEmptyBlockGivesBackACodeSpan() =
        toggle(InlineStyle.Code, "```\n▮\n```", "`▮`")

    @Test fun theLadderRunsBothWays() {
        toggle(InlineStyle.Code, "▮", "`▮`")
        toggle(InlineStyle.Code, "`▮`", "```\n▮\n```")
        toggle(InlineStyle.Code, "```\n▮\n```", "`▮`")
    }

    /** A block with code in it is already literal, so the key has nothing left to say. */
    @Test fun theCodeKeyInsideAFencedBlockDoesNothing() {
        toggle(InlineStyle.Code, "```\nlet x = 1▮\n```", NO_EDIT)
        toggle(InlineStyle.Code, "```\nlet x = 1\n▮\n```", NO_EDIT)
    }

    /**
     * The body of a long code block, with the opening fence off the top of the window: the count
     * of fences above says nothing there, so the key falls back to opening a pair.
     */
    @Test fun anUnseeableFenceIsNotGuessedAt() {
        val snapshot = snapshotOf("let x = 1", caret = 9, reachedStart = false, reachedEnd = true)
        val edit = MarkdownEngine.edit(snapshot, KeyAction.ToggleInlineStyle(InlineStyle.Code))!!
        assertEquals("let x = 1`▮`", applyPurely(snapshot, edit))
    }
}
