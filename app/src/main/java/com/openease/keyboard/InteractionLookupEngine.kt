package com.openease.keyboard

sealed interface KeyboardCommand {
    data class KeyStroke(val text: String) : KeyboardCommand
    object OpenEmojiDrawer : KeyboardCommand
    object OpenSettings : KeyboardCommand
    object SmartBackspace : KeyboardCommand
    object HideKeyboard : KeyboardCommand
    object MoveCursorLeft : KeyboardCommand
    object MoveCursorRight : KeyboardCommand
    object IncrementHeight : KeyboardCommand
    object DecrementHeight : KeyboardCommand
    object IncrementHorizontalOffset : KeyboardCommand
    object DecrementHorizontalOffset : KeyboardCommand
}

data class LookupKey(
    val squareName: String,
    val type: InteractionType,
    val direction: CompassDirection?
)

object InteractionLookupEngine {
    private val lookupTable: Map<LookupKey, KeyboardCommand>

    init {
        val map = mutableMapOf<LookupKey, KeyboardCommand>()

        // 1. "00" tap hides the keyboard
        map[LookupKey("00", InteractionType.TAP, null)] = KeyboardCommand.HideKeyboard

        // 2. "00" long-press opens the settings
        map[LookupKey("00", InteractionType.LONG_PRESS, null)] = KeyboardCommand.OpenSettings

        // 3. "02" tap is smart backspace
        map[LookupKey("02", InteractionType.TAP, null)] = KeyboardCommand.SmartBackspace

        // 4. "03" tap is carriage return
        map[LookupKey("03", InteractionType.TAP, null)] = KeyboardCommand.KeyStroke("\n")

        // 5. "13", "23", and "33" taps are space
        map[LookupKey("13", InteractionType.TAP, null)] = KeyboardCommand.KeyStroke(" ")
        map[LookupKey("23", InteractionType.TAP, null)] = KeyboardCommand.KeyStroke(" ")
        map[LookupKey("33", InteractionType.TAP, null)] = KeyboardCommand.KeyStroke(" ")

        // 6. "W" drag is cursor left, "E" drag is cursor right (only for spacebar squares 13, 23, and 33)
        val spacebarSquares = listOf("13", "23", "33")
        for (sq in spacebarSquares) {
            map[LookupKey(sq, InteractionType.DRAG, CompassDirection.W)] = KeyboardCommand.MoveCursorLeft
            map[LookupKey(sq, InteractionType.DRAG, CompassDirection.E)] = KeyboardCommand.MoveCursorRight
        }

        // 7. Height adjustment dragging: decrement on "00" S drag, increment on "00" N drag
        map[LookupKey("00", InteractionType.DRAG, CompassDirection.S)] = KeyboardCommand.DecrementHeight
        map[LookupKey("00", InteractionType.DRAG, CompassDirection.N)] = KeyboardCommand.IncrementHeight

        // 8. Horizontal position adjustment dragging: decrement (left) on "00" W drag, increment (right) on "00" E drag
        map[LookupKey("00", InteractionType.DRAG, CompassDirection.W)] = KeyboardCommand.DecrementHorizontalOffset
        map[LookupKey("00", InteractionType.DRAG, CompassDirection.E)] = KeyboardCommand.IncrementHorizontalOffset

        // Hardcode "11" tap -> "k" stroke
        map[LookupKey("11", InteractionType.TAP, null)] = KeyboardCommand.KeyStroke("k")

        lookupTable = map
    }

    fun lookup(description: InteractionDescription): KeyboardCommand? {
        val type = description.interaction.type ?: return null
        val direction = description.interaction.direction
        val key = LookupKey(description.squareName, type, direction)
        return lookupTable[key]
    }
}
