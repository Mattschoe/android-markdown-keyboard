package com.creategoodthings.markdownKeyboard.ime

import android.inputmethodservice.InputMethodService
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.creategoodthings.markdownKeyboard.editor.KeyAction
import com.creategoodthings.markdownKeyboard.editor.MarkdownEngine
import com.creategoodthings.markdownKeyboard.ui.ComposeMdKeyboardView

class MdIMEService : InputMethodService(), LifecycleOwner, SavedStateRegistryOwner {
    private var lifecycleRegistry: LifecycleRegistry = LifecycleRegistry(this)
    override val lifecycle: Lifecycle
        get() = lifecycleRegistry

    private val savedStateRegistryController = SavedStateRegistryController.create(this)
    override val savedStateRegistry: SavedStateRegistry
        get() = savedStateRegistryController.savedStateRegistry

    override fun onCreateInputView(): View {
        window?.window?.decorView?.let { decorView ->
            decorView.setViewTreeLifecycleOwner(this)
            decorView.setViewTreeSavedStateRegistryOwner(this)
        }
        return ComposeMdKeyboardView(this, ::onKeyAction)
    }

    /**
     * The single door between the keyboard's UI and its logic.
     *
     * Plain text commits blind so that ordinary typing costs no round trip to the host app;
     * everything else reads the document first and applies a computed edit.
     */
    fun onKeyAction(action: KeyAction) {
        val conn = currentInputConnection ?: return
        when (action) {
            is KeyAction.CommitText -> conn.commitText(action.text, 1)
            KeyAction.Done -> requestHideSelf(0)
            KeyAction.Noop -> Unit
            else -> applyMarkdownAction(conn, action)
        }
    }

    private fun applyMarkdownAction(conn: InputConnection, action: KeyAction) {
        val snapshot = SnapshotReader.read(conn, MarkdownEngine.contextNeed(action))
        val edit = snapshot?.let { MarkdownEngine.edit(it, action) }
        when {
            edit != null && snapshot != null -> EditApplier.apply(conn, snapshot, edit)
            // Falling back matters most for backspace: an unreadable field still has to delete.
            action == KeyAction.Backspace -> sendDeleteKey(conn)
            action == KeyAction.Enter -> conn.commitText("\n", 1)
        }
    }

    private fun sendDeleteKey(conn: InputConnection) {
        conn.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DEL))
        conn.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DEL))
    }

    override fun onCreate() {
        super.onCreate()
        savedStateRegistryController.performRestore(null)
        handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
    }

    override fun onStartInputView(editorInfo: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(editorInfo, restarting)
        handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
    }

    override fun onFinishInputView(finishingInput: Boolean) {
        super.onFinishInputView(finishingInput)
        handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
    }

    override fun onDestroy() {
        super.onDestroy()
        handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    }

    private fun handleLifecycleEvent(event: Lifecycle.Event) =
        lifecycleRegistry.handleLifecycleEvent(event)
}
