package com.creategoodthings.markdownKeyboard.editor.rules

import com.creategoodthings.markdownKeyboard.Marked.NO_EDIT
import com.creategoodthings.markdownKeyboard.assertKey
import com.creategoodthings.markdownKeyboard.editor.KeyAction
import com.creategoodthings.markdownKeyboard.editor.ListKind
import org.junit.Test

/** The list keys: each one is its own undo. */
class ListKeyTest {

    private fun toggle(kind: ListKind, before: String, after: String) =
        assertKey(KeyAction.ToggleList(kind), before, after)

    // ---- verified against the old implementation during the refactor ----

    @Test fun aPlainLineBecomesABullet() = toggle(ListKind.Bullet, "ta▮sk", "- ta▮sk")

    @Test fun pressingBulletOnABulletRemovesIt() = toggle(ListKind.Bullet, "- ta▮sk", "ta▮sk")

    @Test fun aPlainLineBecomesATask() = toggle(ListKind.Task, "bu▮y milk", "- [ ] bu▮y milk")

    @Test fun swappingAMarkerRenumbersTheList() =
        toggle(ListKind.Ordered, "1. a\n- ▮b\n2. c", "1. a\n2. ▮b\n3. c")

    @Test fun normalizeRenumbersWithoutChangingAnythingElse() =
        assertKey(KeyAction.NormalizeList, "1. a\n1. ▮b\n1. c", "1. a\n2. ▮b\n3. c")

    // ---- further cases ----

    @Test fun everyKindMarksAPlainLine() {
        toggle(ListKind.Bullet, "▮x", "- ▮x")
        toggle(ListKind.Ordered, "▮x", "1. ▮x")
        toggle(ListKind.Task, "▮x", "- [ ] ▮x")
    }

    @Test fun everyKindUnmarksItsOwn() {
        toggle(ListKind.Bullet, "- ▮x", "▮x")
        toggle(ListKind.Ordered, "1. ▮x", "▮x")
        toggle(ListKind.Task, "- [ ] ▮x", "▮x")
    }

    @Test fun aCheckedTaskIsStillATaskForTheToggle() = toggle(ListKind.Task, "- [x] ▮x", "▮x")

    @Test fun markingAnIndentedLineKeepsItsIndent() =
        toggle(ListKind.Bullet, "    he▮llo", "    - he▮llo")

    @Test fun aCodeFenceIsNotTurnedIntoAListItem() = toggle(ListKind.Bullet, "``▮`", NO_EDIT)

    @Test fun normalizeOffAListDoesNothing() = assertKey(KeyAction.NormalizeList, "pl▮ain", NO_EDIT)

    @Test fun normalizeOnAnAlreadyCorrectListDoesNothing() =
        assertKey(KeyAction.NormalizeList, "1. a\n2. ▮b", NO_EDIT)

    @Test fun normalizeKeepsTheStartingNumber() =
        assertKey(KeyAction.NormalizeList, "5. a\n1. ▮b\n1. c", "5. a\n6. ▮b\n7. c")

    /** Only the block's *first* ordered item keeps its number; every later run restarts at 1. */
    @Test fun normalizeRenumbersNestedRunsIndependently() = assertKey(
        KeyAction.NormalizeList,
        "1. a\n    5. x\n    5. y\n1. ▮b",
        "1. a\n    1. x\n    2. y\n2. ▮b",
    )
}
