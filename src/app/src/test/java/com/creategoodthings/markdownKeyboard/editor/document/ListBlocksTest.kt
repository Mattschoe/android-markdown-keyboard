package com.creategoodthings.markdownKeyboard.editor.document

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

/** Lifting the list around the caret out of the text. */
class ListBlocksTest {

    private fun blockAt(text: String, offset: Int) = ListBlocks.at(LineParser.parse(text), offset)

    private fun levelsOf(vararg indents: String): List<Int> {
        val text = indents.joinToString("\n") { "$it- x" }
        return blockAt(text, 2)!!.block.entries.map { it.level }
    }

    // Levels come from the sequence of widths, not from dividing by a fixed unit, which is what
    // lets a two-space list written in Obsidian nest correctly against our four-space output.

    @Test fun aFlatListIsAllLevelZero() {
        assertEquals(listOf(0, 0, 0), levelsOf("", "", ""))
    }

    @Test fun fourSpacesIsOneLevel() {
        assertEquals(listOf(0, 1, 1, 0), levelsOf("", "    ", "    ", ""))
    }

    @Test fun twoSpaceInputStillNests() {
        assertEquals(listOf(0, 1, 2), levelsOf("", "  ", "    "))
    }

    @Test fun aShallowerSiblingRejoinsTheLevelAbove() {
        assertEquals(listOf(0, 1, 1), levelsOf("", "    ", "  "))
    }

    @Test fun levelsUnwindBackToTheTop() {
        assertEquals(listOf(0, 1, 2, 0, 1), levelsOf("", "    ", "        ", "", "    "))
    }

    @Test fun aBlockThatStartsIndentedStartsAtLevelZero() {
        assertEquals(listOf(0, 1), levelsOf("    ", "        "))
    }

    @Test fun tabsAndSpacesMeasureAlike() {
        assertEquals(listOf(0, 1), levelsOf("", "\t"))
    }

    @Test fun thereIsNoBlockOffAListLine() {
        assertNull(blockAt("plain text", 2))
        assertNull(blockAt("", 0))
        assertNull(blockAt("# heading", 3))
    }

    @Test fun aBlankLineEndsTheBlock() {
        val located = blockAt("- a\n- b\n\n- c", 2)!!
        assertEquals(listOf("a", "b"), located.block.entries.map { it.content })
    }

    @Test fun aParagraphEndsTheBlock() {
        val located = blockAt("- a\nplain\n- c", 2)!!
        assertEquals(listOf("a"), located.block.entries.map { it.content })
    }

    @Test fun aHeadingEndsTheBlock() {
        val located = blockAt("- a\n# h\n- c", 2)!!
        assertEquals(listOf("a"), located.block.entries.map { it.content })
    }

    @Test fun boundsCoverTheMemberLinesAndNotTheSurroundingNewlines() {
        val text = "intro\n- a\n- b\nafter"
        val located = blockAt(text, 8)!!
        assertEquals(6, located.start)
        assertEquals(13, located.end)
        assertEquals("- a\n- b", text.substring(located.start, located.end))
    }

    @Test fun theColumnIsRelativeToTheItemsContent() {
        val located = blockAt("- hello", 4)!!
        assertEquals(ListCursor(0, 2), located.block.cursor)
    }

    /** Inside the marker there is no content column to speak of, so it clamps to the start. */
    @Test fun aCaretInsideTheMarkerClampsToColumnZero() {
        assertEquals(ListCursor(0, 0), blockAt("- hello", 1)!!.block.cursor)
        assertEquals(ListCursor(0, 0), blockAt("- hello", 0)!!.block.cursor)
    }

    @Test fun aCaretPastTheContentClampsToItsEnd() {
        assertEquals(ListCursor(1, 1), blockAt("- a\n- b", 7)!!.block.cursor)
    }

    @Test fun theCursorEntryIsTheCaretsOwnLine() {
        val located = blockAt("- a\n- b\n- c", 9)!!
        assertEquals(2, located.block.cursor.entry)
        assertNotNull(located.block.currentEntry)
    }

    /**
     * A continuation line carries no marker, and `at()` only ever admits parsed list lines, so
     * an unmarked entry can never come out of here. `ListBlock.normalized()` handles them all
     * the same, but only `clearEntry` can actually produce one.
     */
    @Test fun everyEntryFromTheTextCarriesAMarker() {
        val located = blockAt("- a\n    continuation\n- b", 2)!!
        assertEquals(listOf("a"), located.block.entries.map { it.content })
        assertEquals(1, located.block.entries.count { it.marker != null })
    }
}
