package com.creategoodthings.markdownKeyboard.ui

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.AbstractComposeView
import com.creategoodthings.markdownKeyboard.ui.theme.MarkdownKeyboardTheme


class ComposeMdKeyboardView(context: Context) : AbstractComposeView(context) {
    @Composable
    override fun Content() {
        MarkdownKeyboardTheme {
            MarkdownKeyboard()
        }
    }
}