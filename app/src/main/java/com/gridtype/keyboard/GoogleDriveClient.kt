package com.gridtype.keyboard

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

sealed class DriveNode {
    abstract val name: String
}

data class FolderNode(
    override val name: String,
    val children: MutableList<DriveNode> = mutableListOf()
) : DriveNode()

data class FileNode(
    override val name: String,
    var content: String
) : DriveNode()

object GoogleDriveClient {
    var isConnected by mutableStateOf(false)
    var userEmail by mutableStateOf("")
    
    // Breadcrumb path representing the current directory path (excluding root "My Drive")
    var currentPath = mutableListOf<String>()
    
    private val root = FolderNode("My Drive").apply {
        // Pre-populate with default layouts backup folders and files
        val gridTypeFolder = FolderNode("GridType")
        val layoutsFolder = FolderNode("Layouts").apply {
            children.add(
                FileNode(
                    "english_backup.json",
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

    fun connect(email: String) {
        isConnected = true
        userEmail = email
    }

    fun disconnect() {
        isConnected = false
        userEmail = ""
        currentPath.clear()
    }

    // Resolves and returns the folder at the current path
    fun getCurrentFolder(): FolderNode {
        var current = root
        for (folderName in currentPath) {
            val next = current.children.firstOrNull { it is FolderNode && it.name == folderName } as? FolderNode
            if (next != null) {
                current = next
            } else {
                break
            }
        }
        return current
    }

    fun listCurrentFiles(): List<DriveNode> {
        if (!isConnected) return emptyList()
        return getCurrentFolder().children.toList()
    }

    fun navigateTo(folderName: String) {
        val current = getCurrentFolder()
        val target = current.children.firstOrNull { it is FolderNode && it.name == folderName } as? FolderNode
        if (target != null) {
            currentPath.add(folderName)
        }
    }

    fun navigateUp() {
        if (currentPath.isNotEmpty()) {
            currentPath.removeAt(currentPath.size - 1)
        }
    }

    fun createFolder(name: String) {
        val current = getCurrentFolder()
        if (current.children.none { it.name == name }) {
            current.children.add(FolderNode(name))
        }
    }

    fun uploadFile(name: String, content: String) {
        val current = getCurrentFolder()
        val existing = current.children.firstOrNull { it is FileNode && it.name == name } as? FileNode
        if (existing != null) {
            existing.content = content
        } else {
            current.children.add(FileNode(name, content))
        }
    }

    fun deleteNode(name: String) {
        val current = getCurrentFolder()
        current.children.removeAll { it.name == name }
    }

    fun getFileContent(name: String): String? {
        val current = getCurrentFolder()
        val file = current.children.firstOrNull { it is FileNode && it.name == name } as? FileNode
        return file?.content
    }
}
