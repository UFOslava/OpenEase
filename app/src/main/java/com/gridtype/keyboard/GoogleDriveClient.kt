package com.gridtype.keyboard

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

sealed class DriveNode {
    abstract val name: String
    abstract val id: String
}

data class FolderNode(
    override val name: String,
    override val id: String,
    val children: MutableList<DriveNode> = mutableListOf()
) : DriveNode()

data class FileNode(
    override val name: String,
    override val id: String,
    var content: String
) : DriveNode()

object GoogleDriveClient {
    // Mode toggle
    var useSimulator by mutableStateOf(true)
    
    // Connection State
    var isConnected by mutableStateOf(false)
    var userEmail by mutableStateOf("")
    var accessToken by mutableStateOf("")
    
    // Simulated Folder Path Breadcrumbs
    var simulatedPath = mutableListOf<String>()
    
    // Real path stack tracking: Name to ID
    val realPathStack = mutableStateListOf<Pair<String, String>>()
    
    private val simulatedRoot = FolderNode("My Drive", "sim_root").apply {
        val gridTypeFolder = FolderNode("GridType", "sim_gridtype")
        val layoutsFolder = FolderNode("Layouts", "sim_layouts").apply {
            children.add(
                FileNode(
                    "english_backup.json",
                    "sim_english_backup",
                    """
                    {
                      "name": "English Backup",
                      "baseLayout": "english.lang",
                      "overrides": [
                        { "square": "12", "gesture": "tap", "direction": null, "value": "a_backup" }
                      ]
                    }
                    """.trimIndent()
                )
            )
            children.add(
                FileNode(
                    "colemak_backup.json",
                    "sim_colemak_backup",
                    """
                    {
                      "name": "Colemak Backup",
                      "baseLayout": "english.lang",
                      "overrides": [
                        { "square": "00", "gesture": "tap", "direction": null, "value": "q" }
                      ]
                    }
                    """.trimIndent()
                )
            )
        }
        gridTypeFolder.children.add(layoutsFolder)
        children.add(gridTypeFolder)
    }

    fun connectSimulated(email: String) {
        useSimulator = true
        isConnected = true
        userEmail = email
        accessToken = "mock_token"
        simulatedPath.clear()
    }

    fun connectReal(email: String, token: String) {
        useSimulator = false
        isConnected = true
        userEmail = email
        accessToken = token
        realPathStack.clear()
    }

    fun disconnect() {
        isConnected = false
        userEmail = ""
        accessToken = ""
        simulatedPath.clear()
        realPathStack.clear()
    }

    // --- Path Management ---
    fun getCurrentFolderId(): String {
        return if (useSimulator) {
            simulatedPath.lastOrNull() ?: "sim_root"
        } else {
            realPathStack.lastOrNull()?.second ?: "root"
        }
    }

    fun getBreadcrumbText(): String {
        return if (useSimulator) {
            "My Drive" + if (simulatedPath.isEmpty()) "" else " > " + simulatedPath.joinToString(" > ")
        } else {
            "My Drive" + if (realPathStack.isEmpty()) "" else " > " + realPathStack.map { it.first }.joinToString(" > ")
        }
    }

    fun canNavigateUp(): Boolean {
        return if (useSimulator) simulatedPath.isNotEmpty() else realPathStack.isNotEmpty()
    }

    fun navigateUp() {
        if (useSimulator) {
            if (simulatedPath.isNotEmpty()) {
                simulatedPath.removeAt(simulatedPath.size - 1)
            }
        } else {
            if (realPathStack.isNotEmpty()) {
                realPathStack.removeAt(realPathStack.size - 1)
            }
        }
    }

    fun navigateToFolder(name: String, id: String) {
        if (useSimulator) {
            simulatedPath.add(name)
        } else {
            realPathStack.add(Pair(name, id))
        }
    }

    // --- Simulated storage resolution helper ---
    private fun getSimulatedFolder(): FolderNode {
        var current = simulatedRoot
        for (folderName in simulatedPath) {
            val next = current.children.firstOrNull { it is FolderNode && it.name == folderName } as? FolderNode
            if (next != null) {
                current = next
            } else {
                break
            }
        }
        return current
    }

    // --- HTTP Helper for Real API ---
    private suspend fun makeHttpRequest(
        urlString: String,
        method: String,
        body: String? = null,
        contentType: String? = null
    ): String = withContext(Dispatchers.IO) {
        val url = URL(urlString)
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = method
        conn.setRequestProperty("Authorization", "Bearer $accessToken")
        conn.connectTimeout = 10000
        conn.readTimeout = 10000
        
        if (body != null) {
            conn.doOutput = true
            if (contentType != null) {
                conn.setRequestProperty("Content-Type", contentType)
            }
            conn.outputStream.use { os ->
                OutputStreamWriter(os, "UTF-8").use { writer ->
                    writer.write(body)
                }
            }
        }
        
        val responseCode = conn.responseCode
        if (responseCode in 200..299) {
            conn.inputStream.bufferedReader().use { it.readText() }
        } else {
            val errorText = conn.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
            throw Exception("HTTP $responseCode: $errorText")
        }
    }

    // --- Core Actions ---
    suspend fun listCurrentFiles(): List<DriveNode> {
        if (!isConnected) return emptyList()
        if (useSimulator) {
            return getSimulatedFolder().children.toList()
        }
        
        // Real API list
        val parentId = getCurrentFolderId()
        val query = "'$parentId' in parents and trashed = false"
        val url = "https://www.googleapis.com/drive/v3/files?q=" +
                URLEncoder.encode(query, "UTF-8") +
                "&fields=" + URLEncoder.encode("files(id,name,mimeType)", "UTF-8") +
                "&pageSize=100"
        
        return try {
            val response = makeHttpRequest(url, "GET")
            val json = JSONObject(response)
            val filesArray = json.getJSONArray("files")
            val list = mutableListOf<DriveNode>()
            for (i in 0 until filesArray.length()) {
                val fileObj = filesArray.getJSONObject(i)
                val id = fileObj.getString("id")
                val name = fileObj.getString("name")
                val mimeType = fileObj.getString("mimeType")
                
                if (mimeType == "application/vnd.google-apps.folder") {
                    list.add(FolderNode(name, id))
                } else {
                    list.add(FileNode(name, id, ""))
                }
            }
            list
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    suspend fun createFolder(name: String) {
        if (useSimulator) {
            val current = getSimulatedFolder()
            if (current.children.none { it.name == name }) {
                current.children.add(FolderNode(name, "sim_${name.lowercase()}"))
            }
            return
        }
        
        // Real API folder creation
        val parentId = getCurrentFolderId()
        val url = "https://www.googleapis.com/drive/v3/files"
        val body = JSONObject().apply {
            put("name", name)
            put("mimeType", "application/vnd.google-apps.folder")
            put("parents", org.json.JSONArray().put(parentId))
        }.toString()
        
        try {
            makeHttpRequest(url, "POST", body, "application/json")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun deleteNode(id: String) {
        if (useSimulator) {
            val current = getSimulatedFolder()
            current.children.removeAll { it.id == id }
            return
        }
        
        // Real API deletion
        val url = "https://www.googleapis.com/drive/v3/files/$id"
        try {
            makeHttpRequest(url, "DELETE")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun getFileContent(name: String): String? {
        if (useSimulator) {
            val current = getSimulatedFolder()
            val file = current.children.firstOrNull { it is FileNode && it.name == name } as? FileNode
            return file?.content
        }
        
        // Real API search and content download
        val parentId = getCurrentFolderId()
        val query = "name = '$name' and '$parentId' in parents and trashed = false"
        val searchUrl = "https://www.googleapis.com/drive/v3/files?q=" +
                URLEncoder.encode(query, "UTF-8") +
                "&fields=" + URLEncoder.encode("files(id)", "UTF-8")
        
        return try {
            val searchResponse = makeHttpRequest(searchUrl, "GET")
            val filesArray = JSONObject(searchResponse).getJSONArray("files")
            if (filesArray.length() > 0) {
                val fileId = filesArray.getJSONObject(0).getString("id")
                val downloadUrl = "https://www.googleapis.com/drive/v3/files/$fileId?alt=media"
                makeHttpRequest(downloadUrl, "GET")
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun uploadFile(name: String, content: String) {
        if (useSimulator) {
            val current = getSimulatedFolder()
            val existing = current.children.firstOrNull { it is FileNode && it.name == name } as? FileNode
            if (existing != null) {
                existing.content = content
            } else {
                current.children.add(FileNode(name, "sim_${name.lowercase()}", content))
            }
            return
        }
        
        // Real API search and write (Create + Content Update PATCH)
        val parentId = getCurrentFolderId()
        val query = "name = '$name' and '$parentId' in parents and trashed = false"
        val searchUrl = "https://www.googleapis.com/drive/v3/files?q=" +
                URLEncoder.encode(query, "UTF-8") +
                "&fields=" + URLEncoder.encode("files(id)", "UTF-8")
        
        try {
            val searchResponse = makeHttpRequest(searchUrl, "GET")
            val filesArray = JSONObject(searchResponse).getJSONArray("files")
            
            val fileId = if (filesArray.length() > 0) {
                filesArray.getJSONObject(0).getString("id")
            } else {
                val createUrl = "https://www.googleapis.com/drive/v3/files"
                val body = JSONObject().apply {
                    put("name", name)
                    put("parents", org.json.JSONArray().put(parentId))
                }.toString()
                val createResponse = makeHttpRequest(createUrl, "POST", body, "application/json")
                JSONObject(createResponse).getString("id")
            }
            
            val uploadUrl = "https://www.googleapis.com/upload/drive/v3/files/$fileId?uploadType=media"
            makeHttpRequest(uploadUrl, "PATCH", content, "text/plain")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
