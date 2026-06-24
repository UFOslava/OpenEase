package com.openease.keyboard

/**
 * Filters the input string to only contain printable ASCII characters
 * (char codes 32 to 126 inclusive) and truncates the result to a maximum of 255 characters.
 */
fun filterPrintableAscii(input: String): String {
    return input.filter { it.code in 32..126 }.take(255)
}
