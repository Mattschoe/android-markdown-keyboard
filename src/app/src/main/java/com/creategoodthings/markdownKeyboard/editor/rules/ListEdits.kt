package com.creategoodthings.markdownKeyboard.editor.rules

import com.creategoodthings.markdownKeyboard.editor.ListKind
import com.creategoodthings.markdownKeyboard.editor.MarkdownSyntax
import com.creategoodthings.markdownKeyboard.editor.Snapshot
import com.creategoodthings.markdownKeyboard.editor.TextEdit
import com.creategoodthings.markdownKeyboard.editor.document.ListBlock
import com.creategoodthings.markdownKeyboard.editor.document.ListBlocks
import com.creategoodthings.markdownKeyboard.editor.document.ListMarker

/** The tail end of the list pipeline, shared by every key that touches a list. */
internal object ListEdits {

    /** Normalises [block], renders it and diffs it back against the text it came from. */
    fun commit(snapshot: Snapshot, located: ListBlocks.Located, block: ListBlock): TextEdit? {
        val normalized = block.normalized()
        return TextEdit.replacingRegion(
            snapshot = snapshot,
            regionStart = located.start,
            regionEnd = located.end,
            newRegion = normalized.render(),
            newSelectionStart = located.start + normalized.cursorOffset(),
        )
    }

    fun markerFor(kind: ListKind): ListMarker = when (kind) {
        ListKind.Bullet -> ListMarker.Bullet(MarkdownSyntax.DEFAULT_BULLET)
        ListKind.Ordered -> ListMarker.Ordered(1, MarkdownSyntax.DEFAULT_ORDERED_DELIMITER)
        ListKind.Task -> ListMarker.Task(MarkdownSyntax.DEFAULT_BULLET, checked = false)
    }
}
