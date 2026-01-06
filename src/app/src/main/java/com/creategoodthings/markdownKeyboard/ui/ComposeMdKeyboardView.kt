package com.creategoodthings.markdownKeyboard.ui

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.AbstractComposeView
import com.creategoodthings.markdownKeyboard.editor.KeyAction
import com.creategoodthings.markdownKeyboard.ui.theme.MarkdownKeyboardTheme

/**
 * Actions are passed in rather than pulled out of the context, so the keyboard can be composed
 * anywhere, including a preview, without an input method service behind it.
 */
class ComposeMdKeyboardView(
    context: Context,
    private val onAction: (KeyAction) -> Unit,
) : AbstractComposeView(context) {
    @Composable
    override fun Content() {
        MarkdownKeyboardTheme {
            MarkdownKeyboard(onAction = onAction)
        }
    }
}
