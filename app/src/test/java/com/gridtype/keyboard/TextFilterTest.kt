package com.gridtype.keyboard

import org.junit.Assert.assertEquals
import org.junit.Test

class TextFilterTest {

    @Test
    fun sanitizeInputText_keepsStandardText() {
        val input = "Hello, World! 123 - Russian: Привет - Hebrew: שלום"
        val expected = "Hello, World! 123 - Russian: Привет - Hebrew: שלום"
        assertEquals(expected, sanitizeInputText(input))
    }

    @Test
    fun sanitizeInputText_blocksControlCharacters() {
        // U+0000 to U+001F, and U+007F to U+009F
        val input = "Hello\u0000World\u0008Test\u007FEnd"
        val expected = "HelloWorldTestEnd"
        assertEquals(expected, sanitizeInputText(input))
    }

    @Test
    fun sanitizeInputText_blocksBiDiControls() {
        // U+200E, U+200F, U+202A..U+202E, U+2066..U+2069
        val input = "Text\u200Ewith\u202EBiDi\u2067Controls"
        val expected = "TextwithBiDiControls"
        assertEquals(expected, sanitizeInputText(input))
    }

    @Test
    fun sanitizeInputText_limitsCombiningMarks() {
        // Base character 'a' followed by 5 combining nonspacing marks (e.g., U+0300 grave accent)
        val grave = "\u0300"
        val input = "a" + grave.repeat(5)
        // Should limit to 3 combining marks
        val expected = "a" + grave.repeat(3)
        assertEquals(expected, sanitizeInputText(input))
    }

    @Test
    fun sanitizeInputText_stripsUnpairedSurrogates() {
        // Valid surrogate pair (emoji): \uD83D\uDCBB -> U+1F4BB (💻)
        // Unpaired high surrogate: \uD83D by itself
        // Unpaired low surrogate: \uDCBB by itself
        val input = "Valid \uD83D\uDCBB and unpaired \uD83D and \uDCBB"
        val expected = "Valid \uD83D\uDCBB and unpaired  and "
        assertEquals(expected, sanitizeInputText(input))
    }

    @Test
    fun sanitizeInputText_blocksPUA() {
        // PUA BMP: U+E000
        // PUA Supp-A: U+F0000 (represented as surrogate pair \uDB80\uDC00)
        val puaBmp = "\uE000"
        val puaSuppA = String(Character.toChars(0xF0000))
        val input = "Text${puaBmp}with${puaSuppA}PUA"
        val expected = "TextwithPUA"
        assertEquals(expected, sanitizeInputText(input))
    }

    @Test
    fun sanitizeInputText_handlesZWJAndInvisibleControls() {
        // Zero-width space U+200B, BOM U+FEFF, invisible operators U+2060
        // Standalone ZWJ U+200D
        val zwsp = "\u200B"
        val bom = "\uFEFF"
        val invOp = "\u2060"
        val standaloneZwj = "\u200D"
        val input1 = "Text${zwsp}with${bom}invisible${invOp}controls${standaloneZwj}"
        val expected1 = "Textwithinvisiblecontrols"
        assertEquals(expected1, sanitizeInputText(input1))

        // ZWJ in a valid emoji sequence: 👨 (U+1F468) + ZWJ (U+200D) + 💻 (U+1F4BB)
        val emojiMan = String(Character.toChars(0x1F468))
        val emojiComputer = String(Character.toChars(0x1F4BB))
        val validSequence = emojiMan + "\u200D" + emojiComputer
        val input2 = "Emoji sequence: $validSequence"
        val expected2 = "Emoji sequence: $validSequence"
        assertEquals(expected2, sanitizeInputText(input2))
    }

    @Test
    fun sanitizeInputText_blocksDeprecatedAndAnnotation() {
        // U+FFF9, U+FFFC, U+FFFD
        val input = "Obsolete\uFFF9annotation\uFFFCreplacement\uFFFDEnd"
        val expected = "ObsoleteannotationreplacementEnd"
        assertEquals(expected, sanitizeInputText(input))
    }

    @Test
    fun sanitizeInputText_truncatesTo255CodePoints() {
        // Generate a string with 300 supplementary code points (emojis, each requiring 2 chars in UTF-16)
        val emoji = String(Character.toChars(0x1F44D)) // Thumbs up emoji
        val input = emoji.repeat(300)
        val sanitized = sanitizeInputText(input)
        
        // Output should be exactly 255 code points, which is 510 UTF-16 chars
        assertEquals(510, sanitized.length)
        val codePointCount = sanitized.codePointCount(0, sanitized.length)
        assertEquals(255, codePointCount)
    }
}
