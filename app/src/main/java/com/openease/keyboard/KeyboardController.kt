package com.openease.keyboard

import android.view.inputmethod.InputConnection

class KeyboardController {
    fun handleKeyPress(key: String, inputConnection: InputConnection?) {
        if (key.length == 1) {
            inputConnection?.commitText(key, 1)
        }
    }

    fun handleDelete(inputConnection: InputConnection?) {
        inputConnection?.deleteSurroundingText(1, 0)
    }
}
