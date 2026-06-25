package com.gridtype.keyboard

import android.view.inputmethod.InputConnection

class KeyboardController {
    fun handleKeyPress(key: String, inputConnection: InputConnection?) {
        if (key.isNotEmpty()) {
            inputConnection?.commitText(key, 1)
        }
    }

    fun handleDelete(inputConnection: InputConnection?) {
        if (inputConnection == null) return
        val text = inputConnection.getTextBeforeCursor(20, 0)
        if (text.isNullOrEmpty()) {
            inputConnection.deleteSurroundingText(1, 0)
            return
        }
        val deleteLength = getDeleteLength(text.toString())
        inputConnection.deleteSurroundingText(deleteLength, 0)
    }

    private fun getDeleteLength(text: String): Int {
        if (text.isEmpty()) return 1
        val boundary = java.text.BreakIterator.getCharacterInstance()
        boundary.setText(text)
        val last = boundary.last()
        val previous = boundary.previous()
        if (previous != java.text.BreakIterator.DONE) {
            return last - previous
        }
        return 1
    }
}
