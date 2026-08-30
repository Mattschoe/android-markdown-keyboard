package com.creategoodthings.markdownKeyboard.editor.rules

import com.creategoodthings.markdownKeyboard.Marked.NO_EDIT
import com.creategoodthings.markdownKeyboard.applyPurely
import com.creategoodthings.markdownKeyboard.assertKey
import com.creategoodthings.markdownKeyboard.editor.KeyAction
import com.creategoodthings.markdownKeyboard.editor.MarkdownEngine
import com.creategoodthings.markdownKeyboard.snapshotOf
import org.junit.Assert.assertEquals
import org.junit.Test

/** Backspace: it takes structure apart before it takes characters. */
class BackspaceTest {

    private fun backspace(before: String, after: String) =
        assertKey(KeyAction.Backspace, before, after)

    // ---- verified against the old implementation during the refactor ----

    @Test fun justAfterAMarkerTheMarkerGoes() = backspace("- [ ] ▮task", "▮task")

    @Test fun aNestedItemOutdentsBeforeItUnmarks() =
        backspace("1. a\n    1. ▮b", "1. a\n2. ▮b")

    @Test fun unmarkingRenumbersWhatFollows() =
        backspace("1. a\n2. ▮b\n3. c", "1. a\n▮b\n2. c")

    @Test fun anEmptyStylePairDisappearsWhole() = backspace("x**▮**", "x▮")

    @Test fun anOrdinaryCharacterIsDeleted() = backspace("abc▮", "ab▮")

    @Test fun indentationGoesOneLevelAtATime() = backspace("        ▮x", "    ▮x")

    @Test fun thereIsNothingToDeleteAtTheStart() = backspace("▮abc", NO_EDIT)

    @Test fun aSelectionIsDeleted() = backspace("a«bcd»e", "a▮e")

    // ---- the rest of the behaviour, written down for the first time ----

    @Test fun oneCharacterIntoTheContentDeletesThatCharacter() = backspace("- a▮", "- ▮")

    /** Inside the marker there is no structure left to unwind, so it is a plain deletion. */
    @Test fun insideTheMarkerItIsAPlainDeletion() = backspace("-▮ a", "▮ a")

    /** Joining lines is the editor's own behaviour; nothing here tries to be clever about it. */
    @Test fun atColumnZeroItJoinsTheLines() = backspace("- a\n▮- b", "- a▮- b")

    @Test fun everyEmptyStylePairDisappearsWhole() {
        backspace("x~~▮~~", "x▮")
        backspace("x`▮`", "x▮")
        backspace("x*▮*", "x▮")
    }

    /** An emoji is one character to the user, so its surrogate pair goes together. */
    @Test fun anEmojiDeletesAsOneCharacter() = backspace("a😀▮", "a▮")

    @Test fun theLastItemInAListCanBeUnmarked() = backspace("- a\n- ▮b", "- a\n▮b")

    @Test fun theOnlyItemInAListCanBeUnmarked() = backspace("- ▮a", "▮a")

    @Test fun aPartialIndentIsTakenAsFarAsItGoes() = backspace("  ▮x", " ▮x")

    @Test fun theLongestMatchingDelimiterWins() = backspace("a**▮**b", "a▮b")

    // ---- an abandoned skeleton goes back whole, like an empty style pair ----

    @Test fun anEmptyLinkDisappearsWhole() = backspace("text [▮]()", "text ▮")

    @Test fun anEmptyImageDisappearsWhole() = backspace("text ![▮]()", "text ▮")

    /** With the caret in the empty URL rather than the empty label. */
    @Test fun anEmptyLinkGoesFromTheTargetSideToo() = backspace("text [](▮)", "text ▮")

    /** Once there is anything to keep, backspace goes back to deleting characters. */
    @Test fun aLinkWithSomethingInItIsDeletedByHand() {
        backspace("[a](▮)", "[a]▮)")
        backspace("[](url▮)", "[](ur▮)")
    }

    /** The caret has to be inside the skeleton; in front of it there is ordinary text to delete. */
    @Test fun theCharacterBeforeAnEmptyLinkIsStillJustACharacter() = backspace("ab▮[]()", "a▮[]()")

    @Test fun anEmptyTableDisappearsWhole() =
        backspace("| ▮ |  |\n| --- | --- |\n|  |  |", "▮")

    @Test fun anEmptyTableGoesFromAnyOfItsCells() =
        backspace("|  |  |\n| --- | --- |\n| ▮ |  |", "▮")

    /** It takes the line break that gave it its own line with it. */
    @Test fun aDeletedTableTakesItsLeadingBreak() =
        backspace("text\n| ▮ |  |\n| --- | --- |\n|  |  |", "text▮")

    /** A table with anything typed into it is the user's, not a skeleton. */
    @Test fun aTableWithContentIsDeletedByHand() =
        backspace("| a |  |\n| --- | --- |\n| b▮ |  |", "| a |  |\n| --- | --- |\n| ▮ |  |")

    /** Rows without a delimiter row are just lines that happen to contain pipes. */
    @Test fun pipesWithoutADelimiterRowAreNotATable() = backspace("|  |  |\n| ▮ |  |", "|  |  |\n|▮ |  |")

    @Test fun anEmptyCodeBlockDisappearsWhole() = backspace("```\n▮\n```", "▮")

    @Test fun aDeletedCodeBlockTakesItsLeadingBreakToo() =
        backspace("text\n```\n▮\n```", "text▮")

    /** Code in the block makes it the user's; from there backspace deletes characters again. */
    @Test fun aCodeBlockWithCodeInItIsDeletedByHand() =
        backspace("```\nlet x = 1▮\n```", "```\nlet x = ▮\n```")

    /** An unclosed fence is not a block yet, whatever the line below it looks like. */
    @Test fun anUnclosedFenceIsNotABlock() = backspace("```\n▮", "```▮")

    /** Half a table may be half of a longer one, so a truncated window deletes nothing. */
    @Test fun aTableRunningOffTheWindowIsLeftAlone() {
        val text = "| --- | --- |\n|  |  |"
        val snapshot = snapshotOf(text, caret = 16, reachedStart = false, reachedEnd = true)
        val edit = MarkdownEngine.edit(snapshot, KeyAction.Backspace)!!
        assertEquals("| --- | --- |\n|▮ |  |", applyPurely(snapshot, edit))
    }
}
