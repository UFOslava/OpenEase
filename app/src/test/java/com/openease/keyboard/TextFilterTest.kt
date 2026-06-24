package com.openease.keyboard

import org.junit.Assert.assertEquals
import org.junit.Test

class TextFilterTest {

    @Test
    fun filterPrintableAscii_keepsPrintableAscii() {
        val input = "Hello, World! 123 ~"
        val expected = "Hello, World! 123 ~"
        assertEquals(expected, filterPrintableAscii(input))
    }

    @Test
    fun filterPrintableAscii_removesNonPrintableChars() {
        val input = "Hello\nWorld\t😊"
        val expected = "HelloWorld"
        assertEquals(expected, filterPrintableAscii(input))
    }

    @Test
    fun filterPrintableAscii_truncatesTo255Chars() {
        val input = "a".repeat(300)
        val expected = "a".repeat(255)
        assertEquals(255, filterPrintableAscii(input).length)
        assertEquals(expected, filterPrintableAscii(input))
    }
}
