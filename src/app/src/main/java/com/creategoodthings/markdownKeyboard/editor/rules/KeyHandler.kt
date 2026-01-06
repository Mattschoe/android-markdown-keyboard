package com.creategoodthings.markdownKeyboard.editor.rules

import com.creategoodthings.markdownKeyboard.editor.ContextNeed
import com.creategoodthings.markdownKeyboard.editor.KeyAction
import com.creategoodthings.markdownKeyboard.editor.Snapshot
import com.creategoodthings.markdownKeyboard.editor.TextEdit

/**
 * One family of keys, as a pure function of the text around the cursor.
 *
 * No Android types cross this boundary in either direction, which is what makes every rule
 * testable on the JVM from a before/after pair of strings.
 */
interface KeyHandler {
    /** How much text this handler needs for [action], or null if it does not handle it. */
    fun contextNeed(action: KeyAction): ContextNeed?

    /** Null means "nothing to do"; the caller may then fall back to default editor behaviour. */
    fun handle(snapshot: Snapshot, action: KeyAction): TextEdit?
}
