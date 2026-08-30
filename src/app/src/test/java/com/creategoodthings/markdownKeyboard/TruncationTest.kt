package com.creategoodthings.markdownKeyboard

import com.creategoodthings.markdownKeyboard.editor.KeyAction
import com.creategoodthings.markdownKeyboard.editor.MarkdownEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

/**
 * Truncated windows, as characterisation.
 *
 * `Snapshot` carries `reachedStart` / `reachedEnd` so a rule can tell "there is no list below
 * this" from "I could not see far enough". **No rule consults either flag today**, so a list
 * longer than the widest window gets renumbered from whatever happened to be visible.
 *
 * These tests pin that down rather than assert the intended behaviour, so the day a rule starts
 * degrading properly, they fail and get rewritten instead of the change slipping past. The
 * intended behaviour is spelled out in the comment above each one.
 */
class TruncationTest {

    private fun edit(snapshot: com.creategoodthings.markdownKeyboard.editor.Snapshot, action: KeyAction) =
        MarkdownEngine.edit(snapshot, action)

    private fun applied(marked: String, action: KeyAction, reachedStart: Boolean, reachedEnd: Boolean): String {
        val (text, start, _) = Marked.parse(marked)
        val snapshot = snapshotOf(text, start, reachedStart, reachedEnd)
        val result = edit(snapshot, action)
        assertNotNull(result)
        return applyPurely(snapshot, result!!)
    }

    /**
     * Intended: with the bottom of the list out of sight, Enter should insert the new marker and
     * leave the numbering below alone. Today it renumbers to the window edge regardless.
     */
    @Test fun aTruncatedTailIsRenumberedAnyway() {
        assertEquals(
            "1. a\n2. b\n3. ▮\n4. c\n5. d",
            applied("1. a\n2. b▮\n3. c\n4. d", KeyAction.Enter, reachedStart = true, reachedEnd = false),
        )
    }

    /**
     * Intended: with the top of the list out of sight, the first *visible* item's number is not
     * the list's starting number, so nothing should be re-seeded from it. Today it is.
     */
    @Test fun numberingIsReseededFromTheFirstVisibleItem() {
        assertEquals(
            "7. a\n8. ▮\n9. b",
            applied("7. a▮\n9. b", KeyAction.Enter, reachedStart = false, reachedEnd = true),
        )
    }

    @Test fun bothEdgesOutOfSightStillRenumbersTheWholeWindow() {
        assertEquals(
            "3. a\n4. ▮\n5. b\n6. c",
            applied("3. a▮\n9. b\n9. c", KeyAction.Enter, reachedStart = false, reachedEnd = false),
        )
    }

    /** With both edges seen, the window *is* the document, and the ordinary rules apply. */
    @Test fun withBothEdgesSeenTheListIsRenumberedInFull() {
        assertEquals(
            "1. a\n2. b\n3. ▮\n4. c\n5. d",
            applied("1. a\n2. b▮\n3. c\n4. d", KeyAction.Enter, reachedStart = true, reachedEnd = true),
        )
    }

    /** The same blind spot on every other list key, not only Enter. */
    @Test fun indentAndNormalizeAreEquallyBlindToTruncation() {
        assertEquals(
            "1. a\n    1. ▮b\n2. c",
            applied("1. a\n2. ▮b\n3. c", KeyAction.IndentForward, reachedStart = false, reachedEnd = false),
        )
        assertEquals(
            "4. a\n5. ▮b",
            applied("4. a\n9. ▮b", KeyAction.NormalizeList, reachedStart = false, reachedEnd = false),
        )
    }

    /** The flags reach the rules intact; nothing strips them on the way in. */
    @Test fun theFlagsSurviveOnTheSnapshot() {
        val snapshot = snapshotOf("1. a", 4, reachedStart = false, reachedEnd = false)
        assertEquals(false, snapshot.reachedStart)
        assertEquals(false, snapshot.reachedEnd)
    }
}
