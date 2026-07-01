package com.gridtype.keyboard.ui.main

import android.app.Activity
import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.gms.auth.GoogleAuthUtil
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Scope
import com.gridtype.keyboard.DriveNode
import com.gridtype.keyboard.FileNode
import com.gridtype.keyboard.FolderNode
import com.gridtype.keyboard.GoogleDriveClient
import com.gridtype.keyboard.getLayoutsList
import com.gridtype.keyboard.deleteLayout
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
    val coroutineScope = rememberCoroutineScope()
    
    var emailInput by remember { mutableStateOf("developer@gridtype.com") }
    var useSimulatorToggle by remember { mutableStateOf(GoogleDriveClient.useSimulator) }
    
    var syncTrigger by remember { mutableStateOf(0) }
    var isLoading by remember { mutableStateOf(false) }
    
    var filesList by remember { mutableStateOf<List<DriveNode>>(emptyList()) }
    var syncItems by remember { mutableStateOf<List<LayoutSyncItem>>(emptyList()) }
    
    val localLayouts = remember(syncTrigger) { getLayoutsList(context) }
    
    // Asynchronous loader to populate Explorer and Layout comparison board
    LaunchedEffect(
        GoogleDriveClient.isConnected,
        GoogleDriveClient.useSimulator,
        GoogleDriveClient.simulatedPath.size,
        GoogleDriveClient.realPathStack.size,
        syncTrigger
    ) {
        if (GoogleDriveClient.isConnected) {
            isLoading = true
            
            // List files in current directory
            filesList = GoogleDriveClient.listCurrentFiles()
            
            // Compute layout sync status list
            val items = mutableListOf<LayoutSyncItem>()
            val processedNames = mutableSetOf<String>()
            
            for (localName in localLayouts) {
                val file = File(File(context.filesDir, "layouts"), "$localName.json")
                val localJson = if (file.exists()) file.readText().replace("\r", "") else null
                
                val remoteFileName = "${localName.lowercase().replace(" ", "_")}_backup.json"
                val remoteJson = GoogleDriveClient.getFileContent(remoteFileName)?.replace("\r", "")
                
                if (remoteJson != null) {
                    val localNormalized = localJson?.replace(Regex("\\s+"), "")
                    val remoteNormalized = remoteJson.replace(Regex("\\s+"), "")
                    val status = if (localNormalized == remoteNormalized) SyncStatus.SYNCED else SyncStatus.MODIFIED
                    items.add(LayoutSyncItem(localName, status, localJson, remoteJson))
                } else {
                    items.add(LayoutSyncItem(localName, SyncStatus.LOCAL_ONLY, localJson, null))
                }
                processedNames.add(localName.lowercase().replace(" ", "_"))
            }
            
            for (node in filesList) {
                if (node is FileNode && node.name.endsWith("_backup.json")) {
                    val cleanRemoteName = node.name.removeSuffix("_backup.json")
                    if (!processedNames.contains(cleanRemoteName)) {
                        val remoteJson = GoogleDriveClient.getFileContent(node.name)?.replace("\r", "")
                        if (remoteJson != null) {
                            val displayName = try {
                                JSONObject(remoteJson).optString("name", cleanRemoteName.replace("_", " ").replaceFirstChar { it.uppercase() })
                            } catch (e: Exception) {
                                cleanRemoteName.replace("_", " ").replaceFirstChar { it.uppercase() }
                            }
                            items.add(LayoutSyncItem(displayName, SyncStatus.CLOUD_ONLY, null, remoteJson))
                        }
                    }
                }
            }
            syncItems = items
            isLoading = false
        } else {
            filesList = emptyList()
            syncItems = emptyList()
        }
    }

    // Google Sign-In Intent Result Launcher
    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            isLoading = true
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)
                if (account != null) {
                    coroutineScope.launch {
                        try {
                            val token = withContext(Dispatchers.IO) {
                                GoogleAuthUtil.getToken(
                                    context,
                                    account.account ?: android.accounts.Account(account.email!!, "com.google"),
                                    "oauth2:https://www.googleapis.com/auth/drive.file"
                                )
                            }
                            GoogleDriveClient.connectReal(account.email!!, token)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        } finally {
                            isLoading = false
                        }
                    }
                }
            } catch (e: ApiException) {
                e.printStackTrace()
                isLoading = false
            }
        }
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
            // Header
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

            // Connection card
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
                        // Simulator toggle
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                        ) {
                            Text(
                                text = "Use Sandbox Simulator",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Switch(
                                checked = useSimulatorToggle,
                                onCheckedChange = {
                                    useSimulatorToggle = it
                                    GoogleDriveClient.useSimulator = it
                                }
                            )
                        }
                        
                        if (useSimulatorToggle) {
                            Text(
                                text = "Simulate Google Drive authentication and file backup operations (in-memory). No network access required.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            TextField(
                                value = emailInput,
                                onValueChange = { emailInput = it },
                                label = { Text("Simulated Email") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = {
                                    if (emailInput.isNotBlank()) {
                                        GoogleDriveClient.connectSimulated(emailInput.trim())
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Connect Simulator")
                            }
                        } else {
                            Text(
                                text = "Connect to your live Google Drive. Layout files will be stored directly on your personal Drive account inside the directory of your choice.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = {
                                    val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                                        .requestEmail()
                                        .requestScopes(Scope("https://www.googleapis.com/auth/drive.file"))
                                        .build()
                                    val client = GoogleSignIn.getClient(context, gso)
                                    googleSignInLauncher.launch(client.signInIntent)
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Sign In with Google")
                            }
                        }
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column {
                                Text(
                                    text = if (GoogleDriveClient.useSimulator) "Connected (Simulator)" else "Connected (Live Drive)",
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

            // Spinner during network actions
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxWidth().height(100.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            // Folder Explorer and Synchronization Boards
            if (GoogleDriveClient.isConnected && !isLoading) {
                var showNewFolderDialog by remember { mutableStateOf(false) }
                var newFolderNameInput by remember { mutableStateOf("") }
                
                // Disk Explorer Section
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
                        
                        // Breadcrumbs path Text
                        Text(
                            text = GoogleDriveClient.getBreadcrumbText(),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                                .padding(8.dp)
                                .fillMaxWidth()
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // Navigation Up row
                        if (GoogleDriveClient.canNavigateUp()) {
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
                        
                        // Files/Folders List
                        if (filesList.isEmpty()) {
                            Text(
                                text = "Folder is empty.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(vertical = 12.dp)
                            )
                        } else {
                            filesList.forEach { node ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            if (node is FolderNode) {
                                                GoogleDriveClient.navigateToFolder(node.name, node.id)
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
                                    
                                    IconButton(
                                        onClick = {
                                            coroutineScope.launch {
                                                GoogleDriveClient.deleteNode(node.id)
                                                syncTrigger++
                                            }
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
                
                // Sync comparison board
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
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = item.name,
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        
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
                                    
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        // Upload Backup button
                                        if (item.status == SyncStatus.LOCAL_ONLY || item.status == SyncStatus.MODIFIED) {
                                            IconButton(
                                                onClick = {
                                                    coroutineScope.launch {
                                                        val remoteFileName = "${item.name.lowercase().replace(" ", "_")}_backup.json"
                                                        if (item.localContent != null) {
                                                            GoogleDriveClient.uploadFile(remoteFileName, item.localContent)
                                                            syncTrigger++
                                                        }
                                                    }
                                                }
                                            ) {
                                                Text(
                                                    text = "📤",
                                                    fontSize = 20.sp
                                                )
                                            }
                                        }
                                        
                                        // Download Restore button
                                        if (item.status == SyncStatus.CLOUD_ONLY || item.status == SyncStatus.MODIFIED) {
                                            IconButton(
                                                onClick = {
                                                    coroutineScope.launch {
                                                        if (item.remoteContent != null) {
                                                            val layoutsDir = File(context.filesDir, "layouts")
                                                            if (!layoutsDir.exists()) layoutsDir.mkdirs()
                                                            
                                                            val localFile = File(layoutsDir, "${item.name}.json")
                                                            localFile.writeText(item.remoteContent)
                                                            
                                                            val list = getLayoutsList(context).toMutableList()
                                                            if (!list.contains(item.name)) {
                                                                list.add(item.name)
                                                                com.gridtype.keyboard.saveLayoutsOrder(context, list)
                                                            }
                                                            syncTrigger++
                                                        }
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
                                                    coroutineScope.launch {
                                                        // Resolve backup name or ID
                                                        val remoteFileName = "${item.name.lowercase().replace(" ", "_")}_backup.json"
                                                        // Find ID first in real API mode, or delete by simulated name
                                                        if (GoogleDriveClient.useSimulator) {
                                                            GoogleDriveClient.deleteNode("sim_${remoteFileName.lowercase()}")
                                                        } else {
                                                            // For real API, search if file exists to get ID, then delete
                                                            val fileNode = filesList.firstOrNull { it.name == remoteFileName }
                                                            if (fileNode != null) {
                                                                GoogleDriveClient.deleteNode(fileNode.id)
                                                            }
                                                        }
                                                        syncTrigger++
                                                    }
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
                
                // Create Folder Dialog
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
                                    placeholder = { Text("e.g. Layout Backups") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        },
                        confirmButton = {
                            Button(
                                onClick = {
                                    if (newFolderNameInput.isNotBlank()) {
                                        coroutineScope.launch {
                                            GoogleDriveClient.createFolder(newFolderNameInput.trim())
                                            newFolderNameInput = ""
                                            showNewFolderDialog = false
                                            syncTrigger++
                                        }
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
