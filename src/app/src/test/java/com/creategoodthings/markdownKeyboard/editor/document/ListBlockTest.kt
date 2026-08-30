package com.creategoodthings.markdownKeyboard.editor.document

import org.junit.Assert.assertEquals
import org.junit.Test

/** The structure itself, with no handler in the way. */
class ListBlockTest {

    private fun ordered(level: Int, number: Int, content: String = "x") =
        ListEntry(level, ListMarker.Ordered(number, '.'), content)

    private fun bullet(level: Int, content: String = "x") =
        ListEntry(level, ListMarker.Bullet('-'), content)

    private fun task(level: Int, checked: Boolean, content: String = "x") =
        ListEntry(level, ListMarker.Task('-', checked), content)

    private fun unmarked(content: String) = ListEntry(0, null, content)

    private fun block(vararg entries: ListEntry, cursor: ListCursor = ListCursor(0, 0)) =
        ListBlock(entries.toList(), cursor)

    private fun numbersOf(block: ListBlock) =
        block.entries.map { (it.marker as? ListMarker.Ordered)?.number }

    // ---- normalized() ----

    @Test fun aRunOfOnesBecomesOneTwoThree() {
        val result = block(ordered(0, 1), ordered(0, 1), ordered(0, 1)).normalized()
        assertEquals(listOf(1, 2, 3), numbersOf(result))
    }

    @Test fun theFirstOrderedItemKeepsItsOwnNumber() {
        val result = block(ordered(0, 5), ordered(0, 9), ordered(0, 2)).normalized()
        assertEquals(listOf(5, 6, 7), numbersOf(result))
    }

    @Test fun eachLevelCountsIndependently() {
        val result = block(ordered(0, 1), ordered(1, 1), ordered(1, 1), ordered(0, 1)).normalized()
        assertEquals(listOf(1, 1, 2, 2), numbersOf(result))
    }

    @Test fun anIndentedRunRestartsAtOneRatherThanInheritingItsParent() {
        val result = block(ordered(0, 7), ordered(1, 4)).normalized()
        assertEquals(listOf(7, 1), numbersOf(result))
    }

    @Test fun aBulletEndsAnOrderedRun() {
        val result = block(ordered(0, 1), ordered(0, 1), bullet(0), ordered(0, 9)).normalized()
        assertEquals(listOf(1, 2, null, 1), numbersOf(result))
    }

    /** Markdown renders an unmarked non-blank line as part of the item above it, so we do too. */
    @Test fun anUnmarkedNonBlankEntryIsTransparent() {
        val result = block(ordered(0, 1), unmarked("b"), ordered(0, 9)).normalized()
        assertEquals(listOf(1, null, 2), numbersOf(result))
    }

    @Test fun anUnmarkedBlankEntryEndsTheList() {
        val result = block(ordered(0, 1), unmarked(""), ordered(0, 9)).normalized()
        assertEquals(listOf(1, null, 1), numbersOf(result))
    }

    @Test fun normalisingIsIdempotent() {
        val once = block(ordered(0, 3), ordered(1, 8), ordered(1, 8), ordered(0, 1)).normalized()
        assertEquals(once, once.normalized())
    }

    @Test fun aBulletOrTaskListIsUntouched() {
        val bullets = block(bullet(0), bullet(0), bullet(1))
        assertEquals(bullets, bullets.normalized())

        val tasks = block(task(0, checked = true), task(0, checked = false))
        assertEquals(tasks, tasks.normalized())
    }

    // ---- changeIndent() ----

    @Test fun anItemCannotLandMoreThanOneLevelBelowTheItemAboveIt() {
        val result = block(bullet(0), bullet(0)).changeIndent(1, 5)
        assertEquals(listOf(0, 1), result.entries.map { it.level })
    }

    @Test fun indentingTheFirstItemIsANoOp() {
        val start = block(bullet(0), bullet(0))
        assertEquals(start, start.changeIndent(0, 1))
    }

    @Test fun outdentingAtLevelZeroIsANoOp() {
        val start = block(bullet(0), bullet(0))
        assertEquals(start, start.changeIndent(1, -1))
    }

    @Test fun childrenComeAlongWhenAnItemOutdents() {
        val result = block(bullet(0), bullet(1), bullet(2)).changeIndent(1, -1)
        assertEquals(listOf(0, 0, 1), result.entries.map { it.level })
    }

    @Test fun aFollowingSiblingDoesNotMove() {
        val result = block(bullet(0), bullet(0), bullet(0)).changeIndent(1, 1)
        assertEquals(listOf(0, 1, 0), result.entries.map { it.level })
    }

    // ---- splitAtCursor() ----

    @Test fun splittingAtTheEndOfContentOpensAnEmptyItemBelow() {
        val result = block(bullet(0, "abc"), cursor = ListCursor(0, 3)).splitAtCursor()
        assertEquals(listOf("abc", ""), result.entries.map { it.content })
        assertEquals(ListCursor(1, 0), result.cursor)
    }

    @Test fun splittingMidContentCarriesTheTailDown() {
        val result = block(bullet(0, "abcd"), cursor = ListCursor(0, 2)).splitAtCursor()
        assertEquals(listOf("ab", "cd"), result.entries.map { it.content })
    }

    @Test fun splittingAtColumnZeroEmptiesTheItemAndMovesItsTextDown() {
        val result = block(bullet(0, "abc"), cursor = ListCursor(0, 0)).splitAtCursor()
        assertEquals(listOf("", "abc"), result.entries.map { it.content })
    }

    @Test fun aCheckedTaskSplitsIntoAnUncheckedOne() {
        val result = block(task(0, checked = true, content = "done"), cursor = ListCursor(0, 4))
            .splitAtCursor()
        assertEquals(ListMarker.Task('-', checked = true), result.entries[0].marker)
        assertEquals(ListMarker.Task('-', checked = false), result.entries[1].marker)
    }

    /** The number carried down is a placeholder; `normalized()` is what decides it. */
    @Test fun theNewEntryInheritsTheLevelAndLeavesTheNumberToNormalized() {
        val result = block(ordered(2, 7, "abc"), cursor = ListCursor(0, 3)).splitAtCursor()
        assertEquals(2, result.entries[1].level)
        assertEquals(listOf(7, 8), numbersOf(result.normalized()))
    }

    @Test fun splittingClampsAnOutOfRangeColumn() {
        val result = block(bullet(0, "ab"), cursor = ListCursor(0, 99)).splitAtCursor()
        assertEquals(listOf("ab", ""), result.entries.map { it.content })
    }

    // ---- render() / cursorOffset() ----

    @Test fun renderingRoundTripsExceptForIndentation() {
        val text = "- a\n  - b\n    - c"
        val located = ListBlocks.at(LineParser.parse(text), 2)!!
        assertEquals("- a\n    - b\n        - c", located.block.render())
    }

    @Test fun renderingIsUnchangedWhenTheIndentAlreadyMatches() {
        val text = "1. a\n    1. b\n2. c"
        val located = ListBlocks.at(LineParser.parse(text), 3)!!
        assertEquals(text, located.block.render())
    }

    @Test fun theCursorLandsInsideTheRenderedStringForEveryEntryAndColumn() {
        val entries = listOf(bullet(0, "abc"), bullet(1, "de"), ordered(1, 4, ""))
        for (entry in entries.indices) {
            for (column in -2..6) {
                val rendered = ListBlock(entries, ListCursor(entry, column))
                val offset = rendered.cursorOffset()
                assertEquals(
                    "entry $entry column $column",
                    true,
                    offset in 0..rendered.render().length,
                )
            }
        }
    }

    @Test fun theCursorOffsetPointsAtTheRightCharacter() {
        val block = block(bullet(0, "abc"), bullet(1, "de"), cursor = ListCursor(1, 1))
        assertEquals("- abc\n    - de", block.render())
        assertEquals(13, block.cursorOffset())
        assertEquals('e', block.render()[13])
    }

    @Test fun aClearedEntryRendersWithNoIndentAndNoMarker() {
        val cleared = block(bullet(0, "a"), bullet(2, "b"), cursor = ListCursor(1, 1)).clearEntry(1)
        assertEquals("- a\nb", cleared.render())
        assertEquals(ListCursor(1, 0), cleared.cursor)
        assertEquals(4, cleared.cursorOffset())
    }
}
