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
    fun handleKeyPress_commitsEmoji() {
        controller.handleKeyPress("😊", inputConnection)
        verify(exactly = 1) { inputConnection.commitText("😊", 1) }
    }

    @Test
    fun handleDelete_deletesSurroundingText() {
        io.mockk.every { inputConnection.getTextBeforeCursor(20, 0) } returns null
        controller.handleDelete(inputConnection)
        verify(exactly = 1) { inputConnection.deleteSurroundingText(1, 0) }
    }

    @Test
    fun handleDelete_withNormalText_deletesOneChar() {
        io.mockk.every { inputConnection.getTextBeforeCursor(20, 0) } returns "abc"
        controller.handleDelete(inputConnection)
        verify(exactly = 1) { inputConnection.deleteSurroundingText(1, 0) }
    }

    @Test
    fun handleDelete_withEmoji_deletesTwoChars() {
        io.mockk.every { inputConnection.getTextBeforeCursor(20, 0) } returns "ab😊"
        controller.handleDelete(inputConnection)
        verify(exactly = 1) { inputConnection.deleteSurroundingText(2, 0) }
    }
}
