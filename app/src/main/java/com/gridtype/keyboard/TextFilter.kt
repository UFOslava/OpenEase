package com.gridtype.keyboard

/**
 * Sanitizes input text to block unsafe or undesirable Unicode characters:
 * 1. Control characters (U+0000..U+001F, U+007F..U+009F)
 * 2. Bidirectional (BiDi) text controls
 * 3. Stacking limits on combining marks (max 3 consecutive combining marks per base character)
 * 4. Unpaired surrogates (stripped during decoding)
 * 5. Private Use Areas (PUA)
 * 6. Invisible layout controls (stripping standalone ZWJ, keeping ZWJ only in valid emoji sequences)
 * 7. Deprecated and annotation characters
 *
 * The result is truncated to a maximum of 255 code points.
 */
fun sanitizeInputText(input: String): String {
    if (input.isEmpty()) return ""

    // 1. Decode to code points while stripping unpaired surrogates
    val validCodePoints = mutableListOf<Int>()
    var i = 0
    while (i < input.length) {
        val char = input[i]
        if (char.isHighSurrogate()) {
            if (i + 1 < input.length && input[i + 1].isLowSurrogate()) {
                val cp = Character.toCodePoint(char, input[i + 1])
                validCodePoints.add(cp)
                i += 2
            } else {
                // Unpaired high surrogate, discard
                i += 1
            }
        } else if (char.isLowSurrogate()) {
            // Unpaired low surrogate, discard
            i += 1
        } else {
            validCodePoints.add(char.code)
            i += 1
        }
    }

    // 2. Filter code points
    val filteredCodePoints = mutableListOf<Int>()
    for (idx in validCodePoints.indices) {
        val cp = validCodePoints[idx]

        // Rule 1: Control characters
        if (cp in 0x0000..0x001F || cp in 0x007F..0x009F) {
            continue
        }

        // Rule 2: BiDi control characters
        if (cp == 0x200E || cp == 0x200F || cp in 0x202A..0x202E || cp in 0x2066..0x2069) {
            continue
        }

        // Rule 5: Private Use Areas (PUA)
        if (cp in 0xE000..0xF8FF || cp in 0xF0000..0xFFFFD || cp in 0x100000..0x10FFFD) {
            continue
        }

        // Rule 6: Invisible layout controls (except emoji ZWJ joiners)
        if (cp == 0x200B || cp in 0x2060..0x2064 || cp == 0xFEFF) {
            continue
        }
        if (cp == 0x200D) { // ZWJ
            val precededByEmoji = idx > 0 && isEmoji(validCodePoints[idx - 1])
            val followedByEmoji = idx < validCodePoints.size - 1 && isEmoji(validCodePoints[idx + 1])
            if (!(precededByEmoji && followedByEmoji)) {
                // Standalone ZWJ outside emoji sequence, discard
                continue
            }
        }

        // Rule 7: Deprecated and annotation characters
        if (cp in 0xFFF9..0xFFFB || cp == 0xFFFC || cp == 0xFFFD) {
            continue
        }

        filteredCodePoints.add(cp)
    }

    // 3. Rule 3: Stacking limits on combining marks (Zalgo protection)
    val finalCodePoints = mutableListOf<Int>()
    var consecutiveCombiningMarks = 0
    for (cp in filteredCodePoints) {
        if (isCombiningMark(cp)) {
            if (consecutiveCombiningMarks < 3) {
                finalCodePoints.add(cp)
                consecutiveCombiningMarks++
            }
        } else {
            finalCodePoints.add(cp)
            consecutiveCombiningMarks = 0
        }
    }

    // 4. Limit to 255 code points and reconstruct string
    val limitedCodePoints = finalCodePoints.take(255)
    val sb = java.lang.StringBuilder()
    for (cp in limitedCodePoints) {
        sb.appendCodePoint(cp)
    }
    return sb.toString()
}

private fun isCombiningMark(cp: Int): Boolean {
    val type = Character.getType(cp)
    return type == Character.NON_SPACING_MARK.toInt() ||
           type == Character.COMBINING_SPACING_MARK.toInt() ||
           type == Character.ENCLOSING_MARK.toInt()
}

private fun isEmoji(cp: Int): Boolean {
    return cp in 0x1F300..0x1F5FF || // Miscellaneous Symbols and Pictographs
           cp in 0x1F600..0x1F64F || // Emoticons
           cp in 0x1F680..0x1F6FF || // Transport and Map Symbols
           cp in 0x1F900..0x1F9FF || // Supplemental Symbols and Pictographs
           cp in 0x1FA70..0x1FAFF || // Symbols and Pictographs Extended-A
           cp in 0x2600..0x26FF ||   // Miscellaneous Symbols
           cp in 0x2700..0x27BF ||   // Dingbats
           cp in 0x1F1E6..0x1F1FF || // Regional Indicator Symbols (Flags)
           cp in 0x1F004..0x1F0CF || // Mahjong / Domino tiles / Playing cards
           cp in 0x1F170..0x1F251 || // Enclosed Alphanumeric Supplement / Enclosed CJK Letters
           cp in 0x1F000..0x1F0FF || // Miscellaneous Cryptographic symbols
           cp in 0xFE00..0xFE0F ||   // Variation Selectors
           cp in 0x1F3FB..0x1F3FF    // Emoji Modifier Fitzpatrick (skin tones)
}
