package com.creategoodthings.markdownKeyboard.editor.rules

import com.creategoodthings.markdownKeyboard.assertKey
import com.creategoodthings.markdownKeyboard.editor.KeyAction
import org.junit.Test

/** Enter, in a list, in a quote, and in ordinary text. */
class EnterTest {

    private fun enter(before: String, after: String) = assertKey(KeyAction.Enter, before, after)

    // ---- verified against the old implementation during the refactor ----

    @Test fun appendsToAList() = enter("1. a\n2. b▮", "1. a\n2. b\n3. ▮")

    @Test fun renumbersEverythingBelowTheNewItem() = enter(
        "1. apples\n2. bread▮\n3. cheese\n4. dates",
        "1. apples\n2. bread\n3. ▮\n4. cheese\n5. dates",
    )

    @Test fun splittingAnItemCarriesTheTailDown() = enter("1. ap▮ples", "1. ap\n2. ▮ples")

    @Test fun anEmptyNestedItemOutdents() = enter("- a\n    - ▮", "- a\n- ▮")

    @Test fun anEmptyTopLevelItemLeavesTheList() = enter("- a\n- ▮", "- a\n▮")

    @Test fun aNewTaskStartsUnchecked() = enter("- [x] done▮", "- [x] done\n- [ ] ▮")

    @Test fun aPlainLineJustBreaks() = enter("hello▮", "hello\n▮")

    @Test fun aQuoteContinues() = enter("> quoted▮", "> quoted\n> ▮")

    @Test fun anEmptyQuoteLineLeavesTheQuote() = enter("> a\n> ▮", "> a\n▮")

    @Test fun aNestedRunCountsSeparatelyFromItsParent() = enter(
        "1. a\n    1. x\n    2. y▮\n2. b",
        "1. a\n    1. x\n    2. y\n    3. ▮\n2. b",
    )

    @Test fun aListThatStartsAtFiveStaysThere() = enter("5. a▮\n9. b", "5. a\n6. ▮\n7. b")

    @Test fun theOrderedDelimiterIsCarried() = enter("1) a▮", "1) a\n2) ▮")

    @Test fun twoSpaceIndentsAreRewrittenAsFour() = enter("- a\n  - b▮", "- a\n    - b\n    - ▮")

    @Test fun tabIndentsAreRewrittenAsFour() = enter("- a\n\t- b▮", "- a\n    - b\n    - ▮")

    @Test fun aSelectionIsReplacedByAPlainBreak() = enter("a«bcd»e", "a\n▮e")

    /** Fences are not tracked across lines, so this is a paragraph as far as the parser knows. */
    @Test fun insideACodeFenceItIsAPlainBreak() = enter("```\ncode▮\n```", "```\ncode\n▮\n```")

    // ---- the rest of the behaviour, written down for the first time ----

    /** The marker stays put and an empty item opens above it. */
    @Test fun atColumnZeroOfANonEmptyItemTheMarkerStaysAndAnEmptyItemOpensAbove() =
        enter("- ▮a", "- \n- ▮a")

    /** Content that is only whitespace counts as empty, so this leaves the list. */
    @Test fun anItemWhoseContentIsOnlyWhitespaceCountsAsEmpty() = enter("- a\n-  ▮ ", "- a\n▮  ")

    @Test fun aHeadingJustBreaks() = enter("# title▮", "# title\n▮")

    @Test fun splittingACheckedTaskMidContentLeavesTheNewItemUnchecked() =
        enter("- [x] do▮ne", "- [x] do\n- [ ] ▮ne")

    /** Inside the marker the column clamps to zero, so this behaves like column zero. */
    @Test fun aCaretInsideTheMarkerBehavesLikeColumnZero() = enter("-▮ a", "- \n- ▮a")

    @Test fun anEmptyDocumentJustBreaks() = enter("▮", "\n▮")

    @Test fun aDeeplyNestedEmptyItemStepsOutOneLevelPerPress() {
        enter("- a\n    - b\n        - ▮", "- a\n    - b\n    - ▮")
        enter("- a\n    - b\n    - ▮", "- a\n    - b\n- ▮")
    }
}
