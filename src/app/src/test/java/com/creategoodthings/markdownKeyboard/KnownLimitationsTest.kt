package com.creategoodthings.markdownKeyboard

import com.creategoodthings.markdownKeyboard.editor.InlineStyle
import com.creategoodthings.markdownKeyboard.editor.KeyAction
import com.creategoodthings.markdownKeyboard.editor.MarkdownEngine
import com.creategoodthings.markdownKeyboard.editor.Snapshot
import com.creategoodthings.markdownKeyboard.editor.document.LineParser
import com.creategoodthings.markdownKeyboard.editor.document.LineType
import com.creategoodthings.markdownKeyboard.ime.EditApplier
import com.creategoodthings.markdownKeyboard.ime.FakeInputConnection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

/**
 * Behaviours that are known to be wrong or arbitrary, recorded so that a
 * change to any of them shows up as a failing test rather than silently.
 *
 * Nothing here is an endorsement. Each test says what the behaviour should probably be.
 */
class KnownLimitationsTest {

    // ---- code fences are not tracked across lines ----

    /** `LineParser` classifies each line alone, so a fence gives no protection to its contents. */
    @Test fun aListMarkerInsideAFenceStillParsesAsAList() {
        val inside = LineParser.parse("```\n- not a list\n```")[1]
        assertEquals(LineType.ListItem::class, inside.type::class)
    }

    /** Which means Enter continues a list that only exists because it is inside a code block. */
    @Test fun enterContinuesAListInsideAFence() =
        assertKey(KeyAction.Enter, "```\n- not a list▮\n```", "```\n- not a list\n- ▮\n```")

    @Test fun backspaceUnmarksAListItemInsideAFence() =
        assertKey(KeyAction.Backspace, "```\n- ▮x\n```", "```\n▮x\n```")

    // ---- indent and outdent act on the caret's line only ----

    /**
     * Worse than "one line moves": the applier deletes the selection first and then commits an
     * edit computed for a single line, so **the rest of the selection is lost**. The edit itself
     * is fine — it is the pair that is wrong, because a single-line region cannot cover a
     * selection that runs past it.
     */
    @Test fun indentingAMultiLineSelectionDropsEverythingBelowTheFirstLine() {
        assertPureKey(KeyAction.IndentForward, "«he\nllo»", "    ▮he\nllo")

        val snapshot = Snapshot("he\nllo", 0, 6, windowStart = 0)
        val edit = MarkdownEngine.edit(snapshot, KeyAction.IndentForward)!!
        val conn = FakeInputConnection(snapshot.text, 0, 6)
        EditApplier.apply(conn, snapshot, edit)

        assertEquals("    he", conn.text)
        assertNotEquals("the applied text still matches the computed edit", "    he\nllo", conn.text)
    }

    /** On a list the block covers the selection, so the same press merely ignores the extra lines. */
    @Test fun indentingAMultiLineSelectionInAListMovesOnlyTheFirstLine() =
        assertKey(KeyAction.IndentForward, "- a\n«- b\n- c»", "- a\n    - ▮b\n- c")

    // ---- ToggleQuote does not understand nesting ----

    @Test fun quotingTwiceDoesNotNest() {
        assertKey(KeyAction.ToggleQuote, "a▮", "> a▮")
        assertKey(KeyAction.ToggleQuote, "> a▮", "a▮")
    }

    @Test fun quotingStripsOneLevelFromANestedQuote() =
        assertKey(KeyAction.ToggleQuote, "> > a▮", "> a▮")

    // ---- inline styles run straight across a line break ----

    /** A style key on a selection that spans lines produces markdown no renderer will honour. */
    @Test fun anInlineStyleWrapsStraightAcrossANewline() =
        assertKey(KeyAction.ToggleInlineStyle(InlineStyle.Bold), "«a\nb»", "**«a\nb»**")

    // ---- normalized() reads an unmarked line as a continuation ----

    /**
     * Real markdown agrees that an unmarked line continues the item above it, but it is a
     * judgement call: unmarking item 2 leaves item 3 numbered 2 rather than restarting.
     */
    @Test fun unmarkingAnItemLeavesTheRestOfTheListCounting() =
        assertKey(KeyAction.Backspace, "1. a\n2. ▮b\n3. c", "1. a\n▮b\n2. c")

    // ---- the keyboard's own dead keys ----

    /** The emoji key is `Noop`, and so are the page-switch keys: a page is UI state only. */
    @Test fun aNoopReadsNothingAndEditsNothing() {
        assertEquals(
            com.creategoodthings.markdownKeyboard.editor.ContextNeed.None,
            MarkdownEngine.contextNeed(KeyAction.Noop),
        )
        assertEquals(null, MarkdownEngine.edit(Snapshot("abc", 1, 1), KeyAction.Noop))
    }

    @Test fun plainTypingNeedsNoContextAtAll() {
        assertEquals(
            com.creategoodthings.markdownKeyboard.editor.ContextNeed.None,
            MarkdownEngine.contextNeed(KeyAction.CommitText("q")),
        )
    }
}
