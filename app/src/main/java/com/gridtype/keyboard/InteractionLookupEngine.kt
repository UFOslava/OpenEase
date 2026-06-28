package com.gridtype.keyboard

import android.content.Context
import org.json.JSONObject
import java.io.File

sealed interface KeyboardCommand {
    data class TypeString(val text: String) : KeyboardCommand {
        val cleanText: String = sanitizeInputText(text)
    }
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
    object CarriageReturn : KeyboardCommand
    object CycleLayout : KeyboardCommand
}

data class LookupKey(
    val squareName: String,
    val type: InteractionType,
    val direction: CompassDirection?
)

object InteractionLookupEngine {
    private val hardcodedTable = mutableMapOf<LookupKey, KeyboardCommand>()
    private val activeLayoutTable = mutableMapOf<LookupKey, KeyboardCommand>()

    init {
        // 1. "00" tap hides the keyboard
        hardcodedTable[LookupKey("00", InteractionType.TAP, null)] = KeyboardCommand.HideKeyboard

        // 2. "00" long-press opens the settings
        hardcodedTable[LookupKey("00", InteractionType.LONG_PRESS, null)] = KeyboardCommand.OpenSettings

        // 3. "02" tap is smart backspace
        hardcodedTable[LookupKey("02", InteractionType.TAP, null)] = KeyboardCommand.SmartBackspace

        // 4. "03" tap is carriage return
        hardcodedTable[LookupKey("03", InteractionType.TAP, null)] = KeyboardCommand.CarriageReturn

        // 5. "13", "23", and "33" taps are space
        hardcodedTable[LookupKey("13", InteractionType.TAP, null)] = KeyboardCommand.TypeString(" ")
        hardcodedTable[LookupKey("23", InteractionType.TAP, null)] = KeyboardCommand.TypeString(" ")
        hardcodedTable[LookupKey("33", InteractionType.TAP, null)] = KeyboardCommand.TypeString(" ")

        // 6. "W" drag is cursor left, "E" drag is cursor right (only for spacebar squares 13, 23, and 33)
        val spacebarSquares = listOf("13", "23", "33")
        for (sq in spacebarSquares) {
            hardcodedTable[LookupKey(sq, InteractionType.DRAG, CompassDirection.W)] = KeyboardCommand.MoveCursorLeft
            hardcodedTable[LookupKey(sq, InteractionType.DRAG, CompassDirection.E)] = KeyboardCommand.MoveCursorRight
        }

        // 7. Height adjustment dragging: decrement on "00" S drag, increment on "00" N drag
        hardcodedTable[LookupKey("00", InteractionType.DRAG, CompassDirection.S)] = KeyboardCommand.DecrementHeight
        hardcodedTable[LookupKey("00", InteractionType.DRAG, CompassDirection.N)] = KeyboardCommand.IncrementHeight

        // 8. Horizontal position adjustment dragging: decrement (left) on "00" W drag, increment (right) on "00" E drag
        hardcodedTable[LookupKey("00", InteractionType.DRAG, CompassDirection.W)] = KeyboardCommand.DecrementHorizontalOffset
        hardcodedTable[LookupKey("00", InteractionType.DRAG, CompassDirection.E)] = KeyboardCommand.IncrementHorizontalOffset

        // 9. "01" square East drag-return cycles the layout
        hardcodedTable[LookupKey("01", InteractionType.DRAG_RETURN, CompassDirection.E)] = KeyboardCommand.CycleLayout
    }

    fun loadLayout(context: Context) {
        synchronized(this) {
            activeLayoutTable.clear()

            val sharedPrefs = context.getSharedPreferences("gridtype_settings", Context.MODE_PRIVATE)
            val activeLayoutName = sharedPrefs.getString("active_layout", "English") ?: "English"

            if (activeLayoutName.endsWith(".lang")) {
                val baseMap = loadLayoutFileFromAssets(context, "layouts/$activeLayoutName")
                activeLayoutTable.putAll(baseMap)
            } else {
                val layoutsDir = File(context.filesDir, "layouts")
                val userLayoutFile = File(layoutsDir, "$activeLayoutName.json")
                if (userLayoutFile.exists()) {
                    try {
                        var jsonString = userLayoutFile.readText()
                        jsonString = jsonString.replace(Regex("(?s)/\\*.*?\\*/|//.*?\r?\n"), "")
                        val jsonObject = JSONObject(jsonString)
                        val baseLayout = jsonObject.optString("baseLayout", "english.lang")
                        
                        // 1. Load the base layout first (lowest priority)
                        val baseMap = loadLayoutFileFromAssets(context, "layouts/$baseLayout")
                        activeLayoutTable.putAll(baseMap)
                        
                        // 2. Apply user overrides (medium priority)
                        val overridesMap = loadUserOverridesFromFile(userLayoutFile)
                        activeLayoutTable.putAll(overridesMap)
                    } catch (e: Exception) {
                        e.printStackTrace()
                        // Fallback to English template
                        val baseMap = loadLayoutFileFromAssets(context, "layouts/english.lang")
                        activeLayoutTable.putAll(baseMap)
                    }
                } else {
                    // Fallback to English template
                    val baseMap = loadLayoutFileFromAssets(context, "layouts/english.lang")
                    activeLayoutTable.putAll(baseMap)
                }
            }
        }
    }

    private fun loadLayoutFileFromAssets(context: Context, assetPath: String): Map<LookupKey, KeyboardCommand> {
        val map = mutableMapOf<LookupKey, KeyboardCommand>()
        try {
            var jsonString = context.assets.open(assetPath).bufferedReader().use { it.readText() }
            // Strip comments
            jsonString = jsonString.replace(Regex("(?s)/\\*.*?\\*/|//.*?\r?\n"), "")
            val jsonObject = JSONObject(jsonString)
            val mappingsArray = jsonObject.getJSONArray("mappings")
            for (i in 0 until mappingsArray.length()) {
                val mapObj = mappingsArray.getJSONObject(i)
                val square = mapObj.getString("square")
                val gestureStr = mapObj.getString("gesture")
                val directionStr = if (mapObj.isNull("direction")) null else mapObj.getString("direction")
                val value = mapObj.getString("value")

                val gesture = parseGestureType(gestureStr) ?: continue
                val direction = parseCompassDirection(directionStr)
                val filteredValue = sanitizeInputText(value).take(255)

                map[LookupKey(square, gesture, direction)] = KeyboardCommand.TypeString(filteredValue)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return map
    }

    private fun loadUserOverridesFromFile(file: File): Map<LookupKey, KeyboardCommand> {
        val map = mutableMapOf<LookupKey, KeyboardCommand>()
        try {
            var jsonString = file.readText()
            // Strip comments
            jsonString = jsonString.replace(Regex("(?s)/\\*.*?\\*/|//.*?\r?\n"), "")
            val jsonObject = JSONObject(jsonString)
            val overridesArray = jsonObject.getJSONArray("overrides")
            for (i in 0 until overridesArray.length()) {
                val mapObj = overridesArray.getJSONObject(i)
                val square = mapObj.getString("square")
                val gestureStr = mapObj.getString("gesture")
                val directionStr = if (mapObj.isNull("direction")) null else mapObj.getString("direction")
                val value = mapObj.getString("value")

                val gesture = parseGestureType(gestureStr) ?: continue
                val direction = parseCompassDirection(directionStr)
                val filteredValue = sanitizeInputText(value).take(255)

                map[LookupKey(square, gesture, direction)] = KeyboardCommand.TypeString(filteredValue)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return map
    }

    private fun parseGestureType(gesture: String): InteractionType? {
        return try {
            InteractionType.valueOf(gesture.uppercase().replace("-", "_"))
        } catch (e: Exception) {
            when (gesture.lowercase().replace("-", "_")) {
                "tap" -> InteractionType.TAP
                "long_press", "longpress" -> InteractionType.LONG_PRESS
                "drag" -> InteractionType.DRAG
                "drag_return", "dragreturn" -> InteractionType.DRAG_RETURN
                "bolt" -> InteractionType.BOLT
                "loop" -> InteractionType.LOOP
                else -> null
            }
        }
    }

    private fun parseCompassDirection(dir: String?): CompassDirection? {
        if (dir.isNullOrEmpty()) return null
        return try {
            CompassDirection.valueOf(dir.uppercase())
        } catch (e: Exception) {
            null
        }
    }

    fun lookup(description: InteractionDescription): KeyboardCommand? {
        val type = description.interaction.type ?: return null
        val direction = description.interaction.direction
        val key = LookupKey(description.squareName, type, direction)

        // 1. Hardcoded items (first priority, cannot be overridden)
        val hardcoded = hardcodedTable[key]
        if (hardcoded != null) return hardcoded

        // 2. Active layout (contains overrides + base layout)
        synchronized(this) {
            return activeLayoutTable[key]
        }
    }
}
