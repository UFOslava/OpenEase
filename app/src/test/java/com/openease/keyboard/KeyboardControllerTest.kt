package com.openease.keyboard

import android.view.inputmethod.InputConnection
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test

class KeyboardControllerTest {

    private val controller = KeyboardController()
    private val inputConnection = mockk<InputConnection>(relaxed = true)

    @Test
    fun handleKeyPress_commitsText() {
        controller.handleKeyPress("k", inputConnection)
        verify(exactly = 1) { inputConnection.commitText("k", 1) }
    }

    @Test
    fun handleDelete_deletesSurroundingText() {
        controller.handleDelete(inputConnection)
        verify(exactly = 1) { inputConnection.deleteSurroundingText(1, 0) }
    }
}
