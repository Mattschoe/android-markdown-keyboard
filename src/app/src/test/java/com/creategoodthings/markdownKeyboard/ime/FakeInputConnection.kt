package com.creategoodthings.markdownKeyboard.ime

import android.os.Bundle
import android.os.Handler
import android.view.KeyEvent
import android.view.inputmethod.CompletionInfo
import android.view.inputmethod.CorrectionInfo
import android.view.inputmethod.ExtractedText
import android.view.inputmethod.ExtractedTextRequest
import android.view.inputmethod.InputConnection
import android.view.inputmethod.SurroundingText
import com.creategoodthings.markdownKeyboard.Marked
import org.mockito.Mockito

/**
 * A text field, as [InputConnection] sees it: a buffer plus a selection.
 *
 * Only the handful of calls `EditApplier` and `SnapshotReader` actually make are implemented;
 * everything else is inert. [log] records the calls in order so a test can assert what was *not*
 * done as easily as what was.
 */
class FakeInputConnection(
    initial: String,
    selectionStart: Int = initial.length,
    selectionEnd: Int = selectionStart,
    /** Offset of [initial] within a larger imaginary document, for windowed reads. */
    private val documentOffset: Int = 0,
    /** Set to have every [commitText] blow up, for the batch-edit unwinding test. */
    private val failOnCommit: Boolean = false,
) : InputConnection {

    private val buffer = StringBuilder(initial)
    var selStart = selectionStart
        private set
    var selEnd = selectionEnd
        private set

    val text: String get() = buffer.toString()

    val log = mutableListOf<String>()

    var batchDepth = 0
        private set

    /** Highest [batchDepth] reached, so a test can tell "never opened" from "opened and closed". */
    var maxBatchDepth = 0
        private set

    /** Windows asked for by [SnapshotReader], as `before to after`. */
    val surroundingRequests = mutableListOf<Pair<Int, Int>>()

    /** When false, [getSurroundingText] returns null, as an unco-operative editor would. */
    var surroundingTextAvailable = true

    /** Set to report the selection the wrong way round, which the reader must normalise. */
    var reportReversedSelection = false

    /** Set to -1 to model an editor that will not say where the window sits. */
    var reportedOffset: Int? = null

    /** The marked-up state, in the same notation the rule tests use. */
    fun marked(): String = Marked.render(text, minOf(selStart, selEnd), maxOf(selStart, selEnd))

    override fun beginBatchEdit(): Boolean {
        log += "beginBatchEdit"
        batchDepth++
        maxBatchDepth = maxOf(maxBatchDepth, batchDepth)
        return true
    }

    override fun endBatchEdit(): Boolean {
        log += "endBatchEdit"
        batchDepth--
        return batchDepth > 0
    }

    override fun finishComposingText(): Boolean {
        log += "finishComposingText"
        return true
    }

    override fun commitText(text: CharSequence?, newCursorPosition: Int): Boolean {
        log += "commitText(${text.toString().escaped()}, $newCursorPosition)"
        if (failOnCommit) throw IllegalStateException("editor went away")

        val inserted = text?.toString() ?: ""
        val start = minOf(selStart, selEnd)
        val end = maxOf(selStart, selEnd)
        buffer.replace(start, end, inserted)

        // Positive positions count from the end of the inserted run, non-positive from its start.
        val caret = if (newCursorPosition > 0) start + inserted.length + newCursorPosition - 1
        else start + newCursorPosition
        place(caret.coerceIn(0, buffer.length), caret.coerceIn(0, buffer.length))
        return true
    }

    override fun deleteSurroundingText(beforeLength: Int, afterLength: Int): Boolean {
        log += "deleteSurroundingText($beforeLength, $afterLength)"
        val start = minOf(selStart, selEnd)
        val end = maxOf(selStart, selEnd)
        val before = beforeLength.coerceIn(0, start)
        val after = afterLength.coerceIn(0, buffer.length - end)
        buffer.delete(end, end + after)
        buffer.delete(start - before, start)
        place(start - before, end - before)
        return true
    }

    /** Logged, so a test can tell an explicit selection restore from a caret move by commit. */
    override fun setSelection(start: Int, end: Int): Boolean {
        log += "setSelection($start, $end)"
        place(start, end)
        return true
    }

    private fun place(start: Int, end: Int) {
        selStart = start
        selEnd = end
    }

    override fun getSurroundingText(
        beforeLength: Int,
        afterLength: Int,
        flags: Int,
    ): SurroundingText? {
        surroundingRequests += beforeLength to afterLength
        if (!surroundingTextAvailable) return null

        val start = minOf(selStart, selEnd)
        val end = maxOf(selStart, selEnd)
        val from = (start - beforeLength).coerceAtLeast(0)
        val to = (end + afterLength).coerceAtMost(buffer.length)
        val window = buffer.substring(from, to)

        val inWindowStart = start - from
        val inWindowEnd = end - from
        return surroundingText(
            text = window,
            selectionStart = if (reportReversedSelection) inWindowEnd else inWindowStart,
            selectionEnd = if (reportReversedSelection) inWindowStart else inWindowEnd,
            offset = reportedOffset ?: (documentOffset + from),
        )
    }

    override fun sendKeyEvent(event: KeyEvent?): Boolean {
        log += "sendKeyEvent(${event?.keyCode})"
        return true
    }

    // Everything below is inert: nothing under test calls it.
    override fun getTextBeforeCursor(n: Int, flags: Int): CharSequence =
        buffer.substring((minOf(selStart, selEnd) - n).coerceAtLeast(0), minOf(selStart, selEnd))

    override fun getTextAfterCursor(n: Int, flags: Int): CharSequence =
        buffer.substring(maxOf(selStart, selEnd), (maxOf(selStart, selEnd) + n).coerceAtMost(buffer.length))

    override fun getSelectedText(flags: Int): CharSequence? =
        if (selStart == selEnd) null else buffer.substring(minOf(selStart, selEnd), maxOf(selStart, selEnd))

    override fun getCursorCapsMode(reqModes: Int): Int = 0

    override fun getExtractedText(request: ExtractedTextRequest?, flags: Int): ExtractedText? = null

    override fun deleteSurroundingTextInCodePoints(before: Int, after: Int): Boolean = false

    override fun setComposingText(text: CharSequence?, newCursorPosition: Int): Boolean = false

    override fun setComposingRegion(start: Int, end: Int): Boolean = false

    override fun commitCompletion(text: CompletionInfo?): Boolean = false

    override fun commitCorrection(correctionInfo: CorrectionInfo?): Boolean = false

    override fun performEditorAction(editorAction: Int): Boolean = false

    override fun performContextMenuAction(id: Int): Boolean = false

    override fun clearMetaKeyStates(states: Int): Boolean = false

    override fun reportFullscreenMode(enabled: Boolean): Boolean = false

    override fun performPrivateCommand(action: String?, data: Bundle?): Boolean = false

    override fun requestCursorUpdates(cursorUpdateMode: Int): Boolean = false

    override fun getHandler(): Handler? = null

    override fun closeConnection() = Unit

    override fun commitContent(
        inputContentInfo: android.view.inputmethod.InputContentInfo,
        flags: Int,
        opts: Bundle?,
    ): Boolean = false

    private fun String.escaped(): String = "\"" + replace("\n", "\\n") + "\""

    companion object {
        /**
         * `SurroundingText` is a platform value class, and the stub `android.jar` used by JVM
         * unit tests throws from every getter, so it has to be mocked rather than constructed.
         */
        fun surroundingText(
            text: String,
            selectionStart: Int,
            selectionEnd: Int,
            offset: Int,
        ): SurroundingText = Mockito.mock(SurroundingText::class.java).also {
            Mockito.`when`(it.text).thenReturn(text)
            Mockito.`when`(it.selectionStart).thenReturn(selectionStart)
            Mockito.`when`(it.selectionEnd).thenReturn(selectionEnd)
            Mockito.`when`(it.offset).thenReturn(offset)
        }
    }
}
