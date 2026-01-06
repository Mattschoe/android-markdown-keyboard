package com.creategoodthings.markdownKeyboard.editor

import com.creategoodthings.markdownKeyboard.editor.rules.BackspaceHandler
import com.creategoodthings.markdownKeyboard.editor.rules.BlockHandler
import com.creategoodthings.markdownKeyboard.editor.rules.EnterHandler
import com.creategoodthings.markdownKeyboard.editor.rules.IndentHandler
import com.creategoodthings.markdownKeyboard.editor.rules.InlineStyleHandler
import com.creategoodthings.markdownKeyboard.editor.rules.InsertionHandler
import com.creategoodthings.markdownKeyboard.editor.rules.KeyHandler
import com.creategoodthings.markdownKeyboard.editor.rules.ListHandler

/**
 * The whole of the keyboard's markdown behaviour, as `(text, cursor, key) -> edit`.
 *
 * Pure and free of Android types, so the entire feature set is exercisable from before/after
 * strings on the JVM.
 */
object MarkdownEngine {
    private val handlers: List<KeyHandler> = listOf(
        EnterHandler,
        BackspaceHandler,
        IndentHandler,
        ListHandler,
        InlineStyleHandler,
        BlockHandler,
        InsertionHandler,
    )

    /** Ask before reading, so the reader fetches the right amount of text exactly once. */
    fun contextNeed(action: KeyAction): ContextNeed =
        handlers.firstNotNullOfOrNull { it.contextNeed(action) } ?: ContextNeed.None

    fun edit(snapshot: Snapshot, action: KeyAction): TextEdit? =
        handlers.firstOrNull { it.contextNeed(action) != null }?.handle(snapshot, action)
}
