package com.gridtype.keyboard

import android.content.Context
import java.io.File
import org.json.JSONArray
import org.json.JSONObject

fun getLayoutsList(context: Context): List<String> {
    val sharedPrefs = context.getSharedPreferences("gridtype_settings", Context.MODE_PRIVATE)
    val layoutsDir = File(context.filesDir, "layouts")
    if (!layoutsDir.exists()) {
        layoutsDir.mkdirs()
    }

    var filesList = layoutsDir.listFiles()?.map { it.nameWithoutExtension } ?: emptyList()

    // If empty (first run, or if all layouts were deleted), initialize with English
    if (filesList.isEmpty()) {
        createNewLayout(context, "English", "english.lang")
        filesList = listOf("English")
    }

    val savedOrder = sharedPrefs.getString("layouts_order", null)
    if (savedOrder != null) {
        try {
            val orderArray = JSONArray(savedOrder)
            val orderedList = mutableListOf<String>()
            for (i in 0 until orderArray.length()) {
                val name = orderArray.getString(i)
                if (filesList.contains(name)) {
                    orderedList.add(name)
                }
            }
            val remaining = filesList.filter { !orderedList.contains(it) }
            return orderedList + remaining
        } catch (e: Exception) {
            // fallback
        }
    }
    return filesList
}

fun saveLayoutsOrder(context: Context, order: List<String>) {
    val sharedPrefs = context.getSharedPreferences("gridtype_settings", Context.MODE_PRIVATE)
    val orderArray = JSONArray()
    for (name in order) {
        orderArray.put(name)
    }
    sharedPrefs.edit().putString("layouts_order", orderArray.toString()).apply()
}

fun createNewLayout(context: Context, name: String, baseLanguage: String): Boolean {
    val sanitizedName = name.replace(Regex("[\\\\/:*?\"<>|]"), "").trim()
    if (sanitizedName.isEmpty()) return false

    val targetDir = File(context.filesDir, "layouts")
    if (!targetDir.exists()) {
        targetDir.mkdirs()
    }

    val file = File(targetDir, "$sanitizedName.json")
    if (file.exists()) return false

    val json = JSONObject().apply {
        put("layoutName", sanitizedName)
        put("baseLayout", baseLanguage)
        put("theme", "Default")
        put("overrides", JSONArray())
    }
    file.writeText(json.toString(2))
    return true
}

fun deleteLayout(context: Context, name: String) {
    val file = File(context.filesDir, "layouts/$name.json")
    if (file.exists()) {
        file.delete()
    }
    val sharedPrefs = context.getSharedPreferences("gridtype_settings", Context.MODE_PRIVATE)
    if (sharedPrefs.getString("active_layout", "English") == name) {
        val remaining = getLayoutsList(context)
        val nextActive = remaining.firstOrNull() ?: "English"
        sharedPrefs.edit().putString("active_layout", nextActive).apply()
    }
}
