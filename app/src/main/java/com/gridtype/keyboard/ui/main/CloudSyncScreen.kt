package com.gridtype.keyboard.ui.main

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gridtype.keyboard.FileNode
import com.gridtype.keyboard.FolderNode
import com.gridtype.keyboard.GoogleDriveClient
import com.gridtype.keyboard.getLayoutsList
import com.gridtype.keyboard.createNewLayout
import com.gridtype.keyboard.deleteLayout
import java.io.File
import org.json.JSONObject

enum class SyncStatus {
    SYNCED,
    LOCAL_ONLY,
    CLOUD_ONLY,
    MODIFIED
}

data class LayoutSyncItem(
    val name: String,
    val status: SyncStatus,
    val localContent: String?,
    val remoteContent: String?
)

@Composable
fun CloudSyncScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var emailInput by remember { mutableStateOf("developer@gridtype.com") }
    
    // Trigger recomposition on file changes
    var syncTrigger by remember { mutableStateOf(0) }
    
    val localLayouts = remember(syncTrigger) { getLayoutsList(context) }
    val currentRemoteNodes = remember(GoogleDriveClient.isConnected, GoogleDriveClient.currentPath.size, syncTrigger) {
        GoogleDriveClient.listCurrentFiles()
    }
    
    val syncItems = remember(localLayouts, currentRemoteNodes, syncTrigger) {
        val items = mutableListOf<LayoutSyncItem>()
        val processedNames = mutableSetOf<String>()
        
        // 1. Process Local Layouts
        for (localName in localLayouts) {
            val file = File(File(context.filesDir, "layouts"), "$localName.json")
            val localJson = if (file.exists()) file.readText().replace("\r", "") else null
            
            // Search remote layouts in current folder
            val remoteFileName = "${localName.lowercase().replace(" ", "_")}_backup.json"
            val remoteJson = GoogleDriveClient.getFileContent(remoteFileName)?.replace("\r", "")
            
            if (remoteJson != null) {
                // Exists on both sides. Compare content.
                // Normalize JSON strings by removing whitespace/formatting
                val localNormalized = localJson?.replace(Regex("\\s+"), "")
                val remoteNormalized = remoteJson.replace(Regex("\\s+"), "")
                val status = if (localNormalized == remoteNormalized) SyncStatus.SYNCED else SyncStatus.MODIFIED
                items.add(LayoutSyncItem(localName, status, localJson, remoteJson))
            } else {
                items.add(LayoutSyncItem(localName, SyncStatus.LOCAL_ONLY, localJson, null))
            }
            processedNames.add(localName.lowercase().replace(" ", "_"))
        }
        
        // 2. Process Remote-Only Layouts (in the current directory)
        for (node in currentRemoteNodes) {
            if (node is FileNode && node.name.endsWith("_backup.json")) {
                val cleanRemoteName = node.name.removeSuffix("_backup.json")
                if (!processedNames.contains(cleanRemoteName)) {
                    // Find actual layout name inside remote json if possible
                    val remoteJson = node.content.replace("\r", "")
                    val displayName = try {
                        JSONObject(remoteJson).optString("name", cleanRemoteName.replace("_", " ").replaceFirstChar { it.uppercase() })
                    } catch (e: Exception) {
                        cleanRemoteName.replace("_", " ").replaceFirstChar { it.uppercase() }
                    }
                    items.add(LayoutSyncItem(displayName, SyncStatus.CLOUD_ONLY, null, remoteJson))
                }
            }
        }
        items
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 8.dp, vertical = 16.dp)
        ) {
            // Screen Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBackClick) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Google Drive Sync",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold
                )
            }

            // 1. Connection Panel
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "ACCOUNT CONNECTION",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    if (!GoogleDriveClient.isConnected) {
                        Text(
                            text = "Connect your Google Drive account to backup your layouts and sync them across all your devices.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        TextField(
                            value = emailInput,
                            onValueChange = { emailInput = it },
                            label = { Text("Google Email") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Button(
                            onClick = {
                                if (emailInput.isNotBlank()) {
                                    GoogleDriveClient.connect(emailInput.trim())
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Connect Simulated Drive")
                        }
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column {
                                Text(
                                    text = "Connected",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF4BB543)
                                )
                                Text(
                                    text = GoogleDriveClient.userEmail,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Button(
                                onClick = { GoogleDriveClient.disconnect() },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                            ) {
                                Text("Disconnect")
                            }
                        }
                    }
                }
            }

            // 2. Folder Explorer & Navigation (Only visible when connected)
            if (GoogleDriveClient.isConnected) {
                var showNewFolderDialog by remember { mutableStateOf(false) }
                var newFolderNameInput by remember { mutableStateOf("") }
                
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "DISK EXPLORER",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                            IconButton(onClick = { showNewFolderDialog = true }) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "New Folder",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // Breadcrumbs Path
                        val breadcrumbText = "My Drive" + if (GoogleDriveClient.currentPath.isEmpty()) "" else " > " + GoogleDriveClient.currentPath.joinToString(" > ")
                        Text(
                            text = breadcrumbText,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                                .padding(8.dp)
                                .fillMaxWidth()
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // Navigate Up Button
                        if (GoogleDriveClient.currentPath.isNotEmpty()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { GoogleDriveClient.navigateUp() }
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(imageVector = Icons.Default.KeyboardArrowUp, contentDescription = "Up")
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(".. (Navigate Up)", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                            }
                            HorizontalDivider()
                        }
                        
                        // Folders and Files list
                        if (currentRemoteNodes.isEmpty()) {
                            Text(
                                text = "Folder is empty.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(vertical = 12.dp)
                            )
                        } else {
                            currentRemoteNodes.forEach { node ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            if (node is FolderNode) {
                                                GoogleDriveClient.navigateTo(node.name)
                                            }
                                        }
                                        .padding(vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = if (node is FolderNode) "📁" else "📄",
                                            fontSize = 20.sp,
                                            modifier = Modifier.padding(end = 8.dp)
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text(
                                            text = node.name,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                    
                                    // Option to delete folders/files in the explorer
                                    IconButton(
                                        onClick = {
                                            GoogleDriveClient.deleteNode(node.name)
                                            syncTrigger++
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Delete",
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                                HorizontalDivider()
                            }
                        }
                    }
                }
                
                // 3. Layouts Sync Board
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "LAYOUT SYNCHRONIZATION",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                        
                        if (syncItems.isEmpty()) {
                            Text(
                                text = "No layouts found locally or remotely in this directory.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            syncItems.forEach { item ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    // Left: Layout Name and Status badge
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = item.name,
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        
                                        // Status badge
                                        val badgeColor = when (item.status) {
                                            SyncStatus.SYNCED -> Color(0xFF4BB543)
                                            SyncStatus.LOCAL_ONLY -> Color(0xFF3B82F6)
                                            SyncStatus.CLOUD_ONLY -> Color(0xFFF97316)
                                            SyncStatus.MODIFIED -> Color(0xFFEF4444)
                                        }
                                        val badgeText = when (item.status) {
                                            SyncStatus.SYNCED -> "Synced"
                                            SyncStatus.LOCAL_ONLY -> "Local Only"
                                            SyncStatus.CLOUD_ONLY -> "Cloud Only"
                                            SyncStatus.MODIFIED -> "Out of Sync"
                                        }
                                        
                                        Box(
                                            modifier = Modifier
                                                .background(badgeColor.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = badgeText,
                                                color = badgeColor,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                    
                                    // Right: Sync Actions
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        // Backup button (Upload local content to remote)
                                        if (item.status == SyncStatus.LOCAL_ONLY || item.status == SyncStatus.MODIFIED) {
                                            IconButton(
                                                onClick = {
                                                    val remoteFileName = "${item.name.lowercase().replace(" ", "_")}_backup.json"
                                                    if (item.localContent != null) {
                                                        GoogleDriveClient.uploadFile(remoteFileName, item.localContent)
                                                        syncTrigger++
                                                    }
                                                }
                                            ) {
                                                Text(
                                                    text = "📤",
                                                    fontSize = 20.sp
                                                )
                                            }
                                        }
                                        
                                        // Restore button (Download remote content to local)
                                        if (item.status == SyncStatus.CLOUD_ONLY || item.status == SyncStatus.MODIFIED) {
                                            IconButton(
                                                onClick = {
                                                    if (item.remoteContent != null) {
                                                        // 1. Create or overwrite local layout file
                                                        val layoutsDir = File(context.filesDir, "layouts")
                                                        if (!layoutsDir.exists()) layoutsDir.mkdirs()
                                                        
                                                        val localFile = File(layoutsDir, "${item.name}.json")
                                                        localFile.writeText(item.remoteContent)
                                                        
                                                        // 2. Add layout to order/list if not already present
                                                        val list = getLayoutsList(context).toMutableList()
                                                        if (!list.contains(item.name)) {
                                                            list.add(item.name)
                                                            com.gridtype.keyboard.saveLayoutsOrder(context, list)
                                                        }
                                                        syncTrigger++
                                                    }
                                                }
                                            ) {
                                                Text(
                                                    text = "📥",
                                                    fontSize = 20.sp
                                                )
                                            }
                                        }
                                        
                                        // Delete Local Layout Button (Cannot delete last layout)
                                        if (item.localContent != null) {
                                            val canDelete = getLayoutsList(context).size > 1
                                            IconButton(
                                                onClick = {
                                                    deleteLayout(context, item.name)
                                                    syncTrigger++
                                                },
                                                enabled = canDelete
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Delete,
                                                    contentDescription = "Delete Local",
                                                    tint = if (canDelete) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline
                                                )
                                            }
                                        }
                                        
                                        // Delete Cloud Backup Layout Button
                                        if (item.remoteContent != null) {
                                            IconButton(
                                                onClick = {
                                                    val remoteFileName = "${item.name.lowercase().replace(" ", "_")}_backup.json"
                                                    GoogleDriveClient.deleteNode(remoteFileName)
                                                    syncTrigger++
                                                }
                                            ) {
                                                Text(
                                                    text = "☁️🗑️",
                                                    fontSize = 20.sp
                                                )
                                            }
                                        }
                                    }
                                }
                                HorizontalDivider()
                            }
                        }
                    }
                }
                
                // Create Folder Dialog popup
                if (showNewFolderDialog) {
                    AlertDialog(
                        onDismissRequest = { showNewFolderDialog = false },
                        title = { Text("Create New Folder") },
                        text = {
                            Column {
                                Text("Enter folder name:", style = MaterialTheme.typography.bodyMedium)
                                Spacer(modifier = Modifier.height(8.dp))
                                TextField(
                                    value = newFolderNameInput,
                                    onValueChange = { newFolderNameInput = it },
                                    placeholder = { Text("e.g. My Backups") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        },
                        confirmButton = {
                            Button(
                                onClick = {
                                    if (newFolderNameInput.isNotBlank()) {
                                        GoogleDriveClient.createFolder(newFolderNameInput.trim())
                                        newFolderNameInput = ""
                                        showNewFolderDialog = false
                                        syncTrigger++
                                    }
                                }
                            ) {
                                Text("Create")
                            }
                        },
                        dismissButton = {
                            Button(onClick = { showNewFolderDialog = false }) {
                                Text("Cancel")
                            }
                        }
                    )
                }
            }
        }
    }
}
