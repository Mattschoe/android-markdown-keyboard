package com.creategoodthings.markdownKeyboard.editor.rules

import com.creategoodthings.markdownKeyboard.assertKey
import com.creategoodthings.markdownKeyboard.editor.KeyAction
import org.junit.Test

/** The skeletons the user then fills in. */
class InsertionTest {

    private val table = "|  |  |\n| --- | --- |\n|  |  |"

    // ---- verified against the old implementation during the refactor ----

    @Test fun anEmptyLinkPutsTheCaretInTheLabel() = assertKey(KeyAction.InsertLink, "▮", "[▮]()")

    @Test fun aSelectedLabelPutsTheCaretInTheUrl() =
        assertKey(KeyAction.InsertLink, "«docs»", "[docs](▮)")

    @Test fun anImageIsALinkWithABang() = assertKey(KeyAction.InsertImage, "▮", "![▮]()")

    @Test fun aTableLandsInItsFirstHeaderCell() =
        assertKey(KeyAction.InsertTable, "▮", "| ▮ |  |\n| --- | --- |\n|  |  |")

    @Test fun aBlockSkeletonOpensItsOwnLine() =
        assertKey(KeyAction.InsertTable, "text▮", "text\n| ▮ |  |\n| --- | --- |\n|  |  |")

    @Test fun anEmptyCodeBlockPutsTheCaretInItsBody() =
        assertKey(KeyAction.InsertCodeBlock, "▮", "```\n▮\n```")

    @Test fun aSelectionBecomesTheBodyOfACodeBlock() =
        assertKey(KeyAction.InsertCodeBlock, "«x = 1»", "```\nx = 1▮\n```")

    @Test fun aHorizontalRuleIsThreeDashes() =
        assertKey(KeyAction.InsertHorizontalRule, "▮", "---▮")

    // ---- further cases ----

    @Test fun aSelectedImageLabelAlsoLandsInTheUrl() =
        assertKey(KeyAction.InsertImage, "«cat»", "![cat](▮)")

    @Test fun leadingWhitespaceStillCountsAsItsOwnLine() =
        assertKey(KeyAction.InsertHorizontalRule, "    ▮", "    ---▮")

    @Test fun aRuleAfterTextOpensItsOwnLine() =
        assertKey(KeyAction.InsertHorizontalRule, "text▮", "text\n---▮")

    @Test fun aCodeBlockAfterTextOpensItsOwnLine() =
        assertKey(KeyAction.InsertCodeBlock, "text▮", "text\n```\n▮\n```")

    // ---- the rest of the behaviour, written down for the first time ----

    /** Nothing moves the trailing text out of the way, so it lands on the table's last row. */
    @Test fun textAfterTheCaretIsLeftGluedToTheEndOfATable() =
        assertKey(KeyAction.InsertTable, "ab▮cd", "ab\n| ▮ |  |\n| --- | --- |\n|  |  |cd")

    @Test fun aMultiLineSelectionBecomesAMultiLineCodeBody() =
        assertKey(KeyAction.InsertCodeBlock, "«a\nb»", "```\na\nb▮\n```")

    /** The label is taken verbatim, brackets and all; nothing is escaped. */
    @Test fun aLabelContainingBracketsIsNotEscaped() =
        assertKey(KeyAction.InsertLink, "«a]b»", "[a]b](▮)")

    @Test fun aLabelContainingParensIsNotEscaped() =
        assertKey(KeyAction.InsertLink, "«a)b»", "[a)b](▮)")
}
