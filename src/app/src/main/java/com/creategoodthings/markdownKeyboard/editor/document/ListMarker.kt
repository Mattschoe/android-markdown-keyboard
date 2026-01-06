package com.creategoodthings.markdownKeyboard.editor.document

import com.creategoodthings.markdownKeyboard.editor.ListKind

/** The `- `, `1. ` or `- [ ] ` that opens a list item, including its trailing space. */
sealed interface ListMarker {
    val kind: ListKind

    fun render(): String

    data class Bullet(val bullet: Char) : ListMarker {
        override val kind: ListKind get() = ListKind.Bullet
        override fun render(): String = "$bullet "
    }

    data class Ordered(val number: Int, val delimiter: Char) : ListMarker {
        override val kind: ListKind get() = ListKind.Ordered
        override fun render(): String = "$number$delimiter "
    }

    data class Task(val bullet: Char, val checked: Boolean) : ListMarker {
        override val kind: ListKind get() = ListKind.Task
        override fun render(): String = "$bullet [${if (checked) 'x' else ' '}] "
    }
}
