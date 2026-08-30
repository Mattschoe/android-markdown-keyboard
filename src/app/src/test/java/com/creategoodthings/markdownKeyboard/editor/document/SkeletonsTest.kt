package com.creategoodthings.markdownKeyboard.editor.document

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Recognising a skeleton the user opened and abandoned. */
class SkeletonsTest {

    // ---- links and images ----

    @Test fun theCaretInTheLabelIsInsideTheSkeleton() =
        assertEquals(5 until 9, Skeletons.emptyInlineTargetAt("text []()", 6))

    @Test fun theCaretInTheTargetIsInsideItToo() =
        assertEquals(5 until 9, Skeletons.emptyInlineTargetAt("text []()", 8))

    @Test fun theCaretJustPastTheSkeletonStillCounts() =
        assertEquals(5 until 9, Skeletons.emptyInlineTargetAt("text []()", 9))

    @Test fun theCaretInFrontOfTheSkeletonDoesNot() =
        assertNull(Skeletons.emptyInlineTargetAt("text []()", 5))

    /** The `!` belongs to the skeleton, so an image is matched before the link inside it. */
    @Test fun anImageIsMatchedWholeRatherThanAsALinkWithAStrayBang() =
        assertEquals(5 until 10, Skeletons.emptyInlineTargetAt("text ![]()", 7))

    @Test fun aSkeletonWithAnythingInItIsOrdinaryText() {
        assertNull(Skeletons.emptyInlineTargetAt("[a]()", 2))
        assertNull(Skeletons.emptyInlineTargetAt("[](url)", 5))
    }

    /** With more than one on the line, the caret picks out which. */
    @Test fun theSkeletonTheCaretStandsInIsTheOneFound() =
        assertEquals(5 until 9, Skeletons.emptyInlineTargetAt("[]() []()", 7))

    // ---- tables ----

    private val table = "|  |  |\n| --- | --- |\n|  |  |"

    private fun tableAround(text: String, caret: Int): IntRange? {
        val lines = LineParser.parse(text)
        return Skeletons.emptyTableAround(lines, lines.lineAt(caret)!!)
    }

    @Test fun anEmptyTableIsFoundFromAnyOfItsRows() {
        assertEquals(0 until table.length, tableAround(table, 2))
        assertEquals(0 until table.length, tableAround(table, 24))
    }

    @Test fun theRunStopsAtTheTextAroundIt() =
        assertEquals(5 until 5 + table.length, tableAround("text\n$table\nmore", 7))

    @Test fun aFilledCellMakesItTheUsersTable() =
        assertNull(tableAround(table.replace("|  |  |\n| ---", "| a |  |\n| ---"), 2))

    @Test fun aRunWithoutADelimiterRowIsNotATable() =
        assertNull(tableAround("|  |  |\n|  |  |", 2))

    @Test fun alignedDelimitersAreStillDelimiters() =
        assertEquals(0 until 29, tableAround("|  |  |\n| :-- | --: |\n|  |  |", 2))

    // ---- fenced blocks ----

    private fun blockAround(text: String, caret: Int): IntRange? {
        val lines = LineParser.parse(text)
        return Skeletons.emptyFencedBlockAround(lines, lines.lineAt(caret)!!)
    }

    @Test fun anEmptyBlockIsFoundFromItsBody() = assertEquals(0 until 8, blockAround("```\n\n```", 4))

    @Test fun anEmptyBlockIsFoundFromItsOpeningFence() =
        assertEquals(0 until 8, blockAround("```\n\n```", 3))

    @Test fun severalBlankLinesAreStillAnEmptyBlock() =
        assertEquals(0 until 9, blockAround("```\n\n\n```", 4))

    @Test fun aBlockWithCodeInItIsTheUsers() = assertNull(blockAround("```\nlet x = 1\n```", 5))

    @Test fun anUnclosedFenceIsNotABlock() = assertNull(blockAround("```\n", 4))

    /** Tildes close tildes; a `~~~` under a ``` opens a second block rather than closing one. */
    @Test fun theClosingFenceHasToMatchTheOpeningOne() = assertNull(blockAround("```\n\n~~~", 4))

    /** From the closing fence the block above is finished, so there is nothing to take back. */
    @Test fun theClosingFenceIsNotInsideTheBlock() = assertNull(blockAround("```\n\n```", 8))

    // ---- what the window let us see ----

    @Test fun aRunTouchingASeenEdgeIsFullyVisible() {
        val lines = LineParser.parse(table)
        val run = 0 until table.length
        assertEquals(true, Skeletons.runIsFullyVisible(lines, run, true, true))
    }

    @Test fun aRunTouchingAnUnseenEdgeIsNot() {
        val lines = LineParser.parse(table)
        val run = 0 until table.length
        assertEquals(false, Skeletons.runIsFullyVisible(lines, run, false, true))
        assertEquals(false, Skeletons.runIsFullyVisible(lines, run, true, false))
    }

    /** Text on both sides of the run means the window edges never touched it. */
    @Test fun aRunWithTextAroundItIsVisibleWhateverTheFlagsSay() {
        val text = "text\n$table\nmore"
        val lines = LineParser.parse(text)
        val run = 5 until 5 + table.length
        assertEquals(true, Skeletons.runIsFullyVisible(lines, run, false, false))
    }
}
