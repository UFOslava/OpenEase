package com.gridtype.keyboard.ui.main

import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.view.inputmethod.InputMethodManager
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation3.runtime.NavKey
import com.gridtype.keyboard.CompassDirection
import com.gridtype.keyboard.EditLayout
import com.gridtype.keyboard.CloudSync
import com.gridtype.keyboard.InteractionLookupEngine
import com.gridtype.keyboard.InteractionType
import com.gridtype.keyboard.Preferences
import com.gridtype.keyboard.Setup
import com.gridtype.keyboard.getLayoutsList
import com.gridtype.keyboard.saveLayoutsOrder
import com.gridtype.keyboard.createNewLayout
import com.gridtype.keyboard.deleteLayout
import java.io.File
import org.json.JSONObject
import kotlin.math.atan2
import kotlin.math.roundToInt
import kotlin.math.sqrt

@Composable
fun MainScreen(
    onItemClick: (NavKey) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var isEnabled by remember { mutableStateOf(false) }
    var isSelected by remember { mutableStateOf(false) }

    fun updateKeyboardState() {
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        val enabledIMEs = imm.enabledInputMethodList
        isEnabled = enabledIMEs.any { it.packageName == context.packageName }

        val currentIME = Settings.Secure.getString(context.contentResolver, Settings.Secure.DEFAULT_INPUT_METHOD)
        isSelected = currentIME?.startsWith(context.packageName) == true
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                updateKeyboardState()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
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
                .padding(horizontal = 8.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            // App Title with Gradient (Remains as nice custom text)
            Text(
                text = "GridType",
                style = androidx.compose.ui.text.TextStyle(
                    fontSize = 40.sp,
                    fontWeight = FontWeight.ExtraBold,
                    brush = Brush.horizontalGradient(
                        colors = listOf(Color(0xFF8A2387), Color(0xFFE94057), Color(0xFFF27121))
                    )
                )
            )

            Text(
                text = "Keyboard Settings",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(top = 8.dp)
            )

            Spacer(modifier = Modifier.height(40.dp))

            // Traditional flat list settings
            SettingsCategoryHeader(title = "Keyboard Activation")

            SettingsListItem(
                title = "Turn on GridType keyboard",
                subtitle = if (isEnabled && isSelected) "Keyboard is fully active" else "Setup required",
                trailingContent = {
                    val statusText = if (isEnabled && isSelected) "Active" else "Setup"
                    val statusColor = if (isEnabled && isSelected) Color(0xFF4BB543) else MaterialTheme.colorScheme.error
                    Text(
                        text = statusText,
                        color = statusColor,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                },
                onClick = { onItemClick(Setup) }
            )

            SettingsCategoryHeader(title = "General Settings")

            SettingsListItem(
                title = "Keyboard Theme",
                subtitle = "Dark Slate (Default)",
                onClick = { /* Demo click */ }
            )

            SettingsListItem(
                title = "Preferences",
                subtitle = "Vibration, sound, and debug settings",
                onClick = { onItemClick(Preferences) }
            )

            SettingsListItem(
                title = "Cloud Sync",
                subtitle = "Backup & restore layouts via Google Drive",
                onClick = { onItemClick(CloudSync) }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Layouts List Section in Main Menu
            LayoutsListSection(
                onEditClick = { layoutName ->
                    onItemClick(EditLayout(layoutName))
                }
            )

            // Debug Category for Base Languages
            val isDebug = remember {
                (context.applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0
            }
            if (isDebug) {
                Spacer(modifier = Modifier.height(24.dp))
                SettingsCategoryHeader(title = "Base Languages (Debug)")
                
                val baseLanguages = remember {
                    context.assets.list("layouts")?.filter { it.endsWith(".lang") } ?: emptyList()
                }
                
                baseLanguages.forEach { langName ->
                    SettingsListItem(
                        title = langName,
                        subtitle = "Base Language Template (Read-Only)",
                        onClick = {
                            onItemClick(EditLayout(langName))
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun PreferencesScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val sharedPrefs = remember { context.getSharedPreferences("gridtype_settings", Context.MODE_PRIVATE) }

    var showDebugInfo by remember {
        mutableStateOf(sharedPrefs.getBoolean("show_interaction_debug_info", true))
    }
    var longPressDelay by remember {
        val saved = sharedPrefs.getInt("long_press_delay", 670)
        val rounded = ((saved / 10f).roundToInt() * 10).coerceIn(10, 2000)
        mutableStateOf(rounded)
    }

    var keyboardAspectRatio by remember {
        val saved = sharedPrefs.getInt("keyboard_aspect_ratio", 110)
        mutableStateOf(saved.coerceIn(70, 130))
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
            // Header Row with Back Button
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
                    text = "Preferences",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold
                )
            }

            SettingsCategoryHeader(title = "Debug & Gestures")

            // Toggle Item: Show interaction debug info
            SettingsToggleItem(
                title = "Show interaction debug info",
                subtitle = "Draw colorful overlays over squares on successful resolves",
                checked = showDebugInfo,
                onCheckedChange = { newValue ->
                    showDebugInfo = newValue
                    sharedPrefs.edit().putBoolean("show_interaction_debug_info", newValue).apply()
                }
            )

            // Slider Item: Long press delay
            SettingsSliderItem(
                title = "Long press delay",
                subtitle = "Duration before touch registers as long press",
                value = longPressDelay,
                valueRange = 10f..2000f,
                onValueChange = { newValue ->
                    longPressDelay = newValue
                    sharedPrefs.edit().putInt("long_press_delay", newValue).apply()
                }
            )

            // Slider Item: Keyboard aspect ratio
            SettingsPercentSliderItem(
                title = "Keyboard aspect ratio",
                subtitle = "Adjust the width percentage of the keys",
                value = keyboardAspectRatio,
                valueRange = 70f..130f,
                onValueChange = { newValue ->
                    keyboardAspectRatio = newValue
                    sharedPrefs.edit().putInt("keyboard_aspect_ratio", newValue).apply()
                }
            )
        }
    }
}

@Composable
fun SetupScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var isEnabled by remember { mutableStateOf(false) }
    var isSelected by remember { mutableStateOf(false) }

    fun updateKeyboardState() {
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        val enabledIMEs = imm.enabledInputMethodList
        isEnabled = enabledIMEs.any { it.packageName == context.packageName }

        val currentIME = Settings.Secure.getString(context.contentResolver, Settings.Secure.DEFAULT_INPUT_METHOD)
        isSelected = currentIME?.startsWith(context.packageName) == true
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                updateKeyboardState()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
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
            // Header Row with Back Button
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
                    text = "Turn on Keyboard",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Step 1: Enable Keyboard
            SetupStepItem(
                stepNumber = "1",
                title = "Enable Keyboard",
                description = "Turn on GridType Keyboard in System settings.",
                isCompleted = isEnabled,
                actionLabel = "Enable",
                onActionClick = {
                    val intent = Intent(Settings.ACTION_INPUT_METHOD_SETTINGS).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                }
            )

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant,
                thickness = 0.5.dp,
                modifier = Modifier.padding(vertical = 16.dp)
            )

            // Step 2: Choose/Select Keyboard
            SetupStepItem(
                stepNumber = "2",
                title = "Select Keyboard",
                description = "Choose GridType Keyboard as your active input method.",
                isCompleted = isSelected,
                actionLabel = "Choose",
                enabled = isEnabled,
                onActionClick = {
                    val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                    imm.showInputMethodPicker()
                }
            )

            Spacer(modifier = Modifier.height(40.dp))

            if (isEnabled && isSelected) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "🎉 GridType Keyboard is active!",
                        color = Color(0xFF4BB543),
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
        }
    }
}

@Composable
fun EditLayoutScreen(
    layoutName: String,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedSquare by remember { mutableStateOf<String?>(null) }
    var selectedGesture by remember { mutableStateOf<InteractionType?>(null) }
    var selectedDirection by remember { mutableStateOf<CompassDirection?>(null) }
    var editingValue by remember { mutableStateOf("") }
    
    var currentMappings by remember(layoutName, selectedSquare) {
        mutableStateOf(
            if (selectedSquare != null) getMappingsForSquare(context, layoutName, selectedSquare!!)
            else emptyMap()
        )
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
            // Header Row with Back Button
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
                    text = "Edit Layout: $layoutName",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold
                )
            }

            SettingsCategoryHeader(title = "Keyboard Switcher")

            SettingsListItem(
                title = "Switch active keyboard",
                subtitle = "Select a different keyboard. When editing the symbols of GridType, using another keyboard prevents deadlock (e.g., you cannot type 'p' to add it if it was previously removed from your layout).",
                onClick = {
                    val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                    imm.showInputMethodPicker()
                }
            )

            Spacer(modifier = Modifier.height(24.dp))

            SettingsCategoryHeader(title = "Layout Editor")
            
            // 1. Grid Selector
            LayoutGridSelector(
                selectedSquare = selectedSquare,
                onSquareSelected = { sq ->
                    selectedSquare = sq
                    selectedGesture = null
                    selectedDirection = null
                    editingValue = ""
                }
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // 2. Gesture Chooser (only enabled if a square is selected)
            if (selectedSquare != null) {
                Text(
                    text = "Select Gesture for Square $selectedSquare",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                
                GestureChooser(
                    square = selectedSquare!!,
                    layoutName = layoutName,
                    selectedGesture = selectedGesture,
                    selectedDirection = selectedDirection,
                    onGestureSelected = { gesture, direction ->
                        selectedGesture = gesture
                        selectedDirection = direction
                        val key = if (direction == null) gesture.name.lowercase() else "${gesture.name.lowercase()}_${direction.name}"
                        editingValue = currentMappings[key] ?: ""
                    },
                    currentMappings = currentMappings
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // 3. Value Editor (only enabled if a gesture is selected)
                if (selectedGesture != null) {
                    val defaultValue = remember(selectedSquare, selectedGesture, selectedDirection) {
                        getDefaultValueForGesture(context, layoutName, selectedSquare!!, selectedGesture!!, selectedDirection)
                    }
                    
                    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                        Text(
                            text = "Gesture: ${selectedGesture!!.name}${if (selectedDirection != null) " ($selectedDirection)" else ""}",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        TextField(
                            value = editingValue,
                            onValueChange = {
                                if (it.length <= 255) {
                                    editingValue = it
                                }
                            },
                            label = { Text("Output String (max 255 chars)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            // Reset Button
                            Button(
                                onClick = {
                                    if (layoutName.endsWith(".lang")) {
                                        // Reset is disabled for lang editor
                                    } else {
                                        resetGestureValue(context, layoutName, selectedSquare!!, selectedGesture!!, selectedDirection)
                                        // Reload mappings
                                        currentMappings = getMappingsForSquare(context, layoutName, selectedSquare!!)
                                        editingValue = defaultValue ?: ""
                                    }
                                },
                                enabled = !layoutName.endsWith(".lang"),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.errorContainer,
                                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                                )
                            ) {
                                Text("Reset to Default")
                            }
                            
                            // Save Button
                            Button(
                                onClick = {
                                    saveGestureValue(context, layoutName, selectedSquare!!, selectedGesture!!, selectedDirection, editingValue)
                                    // Reload mappings
                                    currentMappings = getMappingsForSquare(context, layoutName, selectedSquare!!)
                                }
                            ) {
                                Text("Save")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LayoutGridSelector(
    selectedSquare: String?,
    onSquareSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Select a square to edit:",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            for (row in 0..3) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    for (col in 0..3) {
                        val squareName = "$col$row"
                        val isSelected = squareName == selectedSquare
                        
                        Box(
                            modifier = Modifier
                                .size(60.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.surfaceVariant
                                )
                                .clickable { onSquareSelected(squareName) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = squareName,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                                        else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun GestureChooser(
    square: String,
    layoutName: String,
    selectedGesture: InteractionType?,
    selectedDirection: CompassDirection?,
    onGestureSelected: (InteractionType, CompassDirection?) -> Unit,
    currentMappings: Map<String, String>,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val textMeasurer = rememberTextMeasurer()
    
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(360.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier
                .size(340.dp)
                .pointerInput(square) {
                    awaitPointerEventScope {
                        while (true) {
                            val down = awaitFirstDown()
                            val startPos = down.position
                            val startTime = System.currentTimeMillis()
                            var lastPos = startPos
                            var hasMoved = false
                            var maxDist = 0f
                            
                            val dragResult = drag(down.id) { change ->
                                change.consume()
                                lastPos = change.position
                                val dist = (lastPos - startPos).getDistance()
                                if (dist > 15.dp.toPx()) {
                                    hasMoved = true
                                }
                                if (dist > maxDist) maxDist = dist
                            }
                            
                            val endTime = System.currentTimeMillis()
                            val duration = endTime - startTime
                            
                            if (!hasMoved) {
                                // Tap or Long Press
                                val dx = startPos.x - size.width / 2f
                                val dy = startPos.y - size.height / 2f
                                val r = sqrt(dx*dx + dy*dy)
                                
                                val (gesture, direction) = resolveTapOnChooser(dx, dy, r, duration, density)
                                if (gesture != null) {
                                    if (!InteractionLookupEngine.isHardcoded(square, gesture, direction)) {
                                        onGestureSelected(gesture, direction)
                                    }
                                }
                            } else {
                                // Drag, Drag-Return, or Bolt
                                val dx = lastPos.x - startPos.x
                                val dy = lastPos.y - startPos.y
                                val dist = sqrt(dx*dx + dy*dy)
                                
                                val isReturn = maxDist > 40.dp.toPx() && dist < 25.dp.toPx()
                                
                                val angle = atan2(dy, dx) * 180 / Math.PI
                                val normAngle = if (angle < 0) angle + 360 else angle
                                val direction = getDirectionFromAngle(normAngle)
                                
                                val speed = maxDist / duration // px/ms
                                
                                val gesture = when {
                                    isReturn -> InteractionType.DRAG_RETURN
                                    speed > 1.5f -> InteractionType.BOLT
                                    else -> InteractionType.DRAG
                                }
                                
                                if (!InteractionLookupEngine.isHardcoded(square, gesture, direction)) {
                                    onGestureSelected(gesture, direction)
                                }
                            }
                        }
                    }
                }
        ) {
            val cx = size.width / 2f
            val cy = size.height / 2f
            
            val rCenter = 42.dp.toPx()
            val rDrag = 80.dp.toPx()
            val rDragReturn = 115.dp.toPx()
            val rBolt = 150.dp.toPx()
            val rLoop = 175.dp.toPx()
            
            // 1. Draw Loop Background Ring
            val isLoopSelected = selectedGesture == InteractionType.LOOP
            val isLoopHardcoded = InteractionLookupEngine.isHardcoded(square, InteractionType.LOOP, null)
            val loopColor = when {
                isLoopHardcoded -> Color.Gray.copy(alpha = 0.3f)
                isLoopSelected -> Color(0xFF8B5CF6) // Purple
                else -> Color(0xFF374151) // Neutral Gray
            }
            drawCircle(
                color = loopColor,
                radius = rLoop,
                style = Stroke(width = 6.dp.toPx())
            )
            
            // Draw Loop Value Text if any
            val loopValue = currentMappings["loop"]
            if (!loopValue.isNullOrEmpty()) {
                val displayText = if (loopValue.length > 1) "${loopValue.first()}..." else loopValue
                val textLayoutResult = textMeasurer.measure(
                    text = displayText,
                    style = TextStyle(color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                )
                val tx = cx + rLoop * kotlin.math.cos(-Math.PI.toFloat() / 4f)
                val ty = cy + rLoop * kotlin.math.sin(-Math.PI.toFloat() / 4f)
                drawText(
                    textLayoutResult = textLayoutResult,
                    topLeft = Offset(tx - textLayoutResult.size.width / 2f, ty - textLayoutResult.size.height / 2f)
                )
            }
            
            // 2. Draw 8 directions with nested arcs
            val directions = listOf(
                CompassDirection.E, CompassDirection.SE, CompassDirection.S, CompassDirection.SW,
                CompassDirection.W, CompassDirection.NW, CompassDirection.N, CompassDirection.NE
            )
            
            directions.forEachIndexed { i, dir ->
                val angleDeg = i * 45f
                val startAngle = angleDeg - 20f
                val sweepAngle = 40f
                
                // Draw Drag (Inner)
                val isDragSelected = selectedGesture == InteractionType.DRAG && selectedDirection == dir
                val isDragHardcoded = InteractionLookupEngine.isHardcoded(square, InteractionType.DRAG, dir)
                val dragColor = when {
                    isDragHardcoded -> Color.Gray.copy(alpha = 0.2f)
                    isDragSelected -> Color(0xFF22C55E) // Green
                    else -> Color(0xFF374151)
                }
                val dragStrokeWidth = (80 - 42).dp.toPx()
                val dragRadius = (42 + 80).dp.toPx() / 2f
                drawArc(
                    color = dragColor,
                    startAngle = startAngle,
                    sweepAngle = sweepAngle,
                    useCenter = false,
                    topLeft = Offset(cx - dragRadius, cy - dragRadius),
                    size = androidx.compose.ui.geometry.Size(dragRadius * 2, dragRadius * 2),
                    style = Stroke(width = dragStrokeWidth)
                )
                
                // Draw Drag-Return (Middle)
                val isDrSelected = selectedGesture == InteractionType.DRAG_RETURN && selectedDirection == dir
                val isDrHardcoded = InteractionLookupEngine.isHardcoded(square, InteractionType.DRAG_RETURN, dir)
                val drColor = when {
                    isDrHardcoded -> Color.Gray.copy(alpha = 0.2f)
                    isDrSelected -> Color(0xFF06B6D4) // Blue-Green (Cyan)
                    else -> Color(0xFF1F2937)
                }
                val drStrokeWidth = (115 - 80).dp.toPx()
                val drRadius = (80 + 115).dp.toPx() / 2f
                drawArc(
                    color = drColor,
                    startAngle = startAngle,
                    sweepAngle = sweepAngle,
                    useCenter = false,
                    topLeft = Offset(cx - drRadius, cy - drRadius),
                    size = androidx.compose.ui.geometry.Size(drRadius * 2, drRadius * 2),
                    style = Stroke(width = drStrokeWidth)
                )
                
                // Draw Bolt (Outer)
                val isBoltSelected = selectedGesture == InteractionType.BOLT && selectedDirection == dir
                val isBoltHardcoded = InteractionLookupEngine.isHardcoded(square, InteractionType.BOLT, dir)
                val boltColor = when {
                    isBoltHardcoded -> Color.Gray.copy(alpha = 0.2f)
                    isBoltSelected -> Color(0xFFEAB308) // Yellow
                    else -> Color(0xFF111827)
                }
                val boltStrokeWidth = (150 - 115).dp.toPx()
                val boltRadius = (115 + 150).dp.toPx() / 2f
                drawArc(
                    color = boltColor,
                    startAngle = startAngle,
                    sweepAngle = sweepAngle,
                    useCenter = false,
                    topLeft = Offset(cx - boltRadius, cy - boltRadius),
                    size = androidx.compose.ui.geometry.Size(boltRadius * 2, boltRadius * 2),
                    style = Stroke(width = boltStrokeWidth)
                )
                
                // Draw Values text on the arcs
                val angleRad = angleDeg * Math.PI / 180.0
                
                // Inner (Drag) Text
                val dragVal = currentMappings["drag_${dir.name}"]
                if (!dragVal.isNullOrEmpty()) {
                    val displayText = if (dragVal.length > 1) "${dragVal.first()}..." else dragVal
                    val textLayoutResult = textMeasurer.measure(
                        text = displayText,
                        style = TextStyle(color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    )
                    val tx = cx + dragRadius * kotlin.math.cos(angleRad).toFloat()
                    val ty = cy + dragRadius * kotlin.math.sin(angleRad).toFloat()
                    drawText(
                        textLayoutResult = textLayoutResult,
                        topLeft = Offset(tx - textLayoutResult.size.width / 2f, ty - textLayoutResult.size.height / 2f)
                    )
                }
                
                // Middle (Drag-Return) Text
                val drVal = currentMappings["drag_return_${dir.name}"]
                if (!drVal.isNullOrEmpty()) {
                    val displayText = if (drVal.length > 1) "${drVal.first()}..." else drVal
                    val textLayoutResult = textMeasurer.measure(
                        text = displayText,
                        style = TextStyle(color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    )
                    val tx = cx + drRadius * kotlin.math.cos(angleRad).toFloat()
                    val ty = cy + drRadius * kotlin.math.sin(angleRad).toFloat()
                    drawText(
                        textLayoutResult = textLayoutResult,
                        topLeft = Offset(tx - textLayoutResult.size.width / 2f, ty - textLayoutResult.size.height / 2f)
                    )
                }
                
                // Outer (Bolt) Text
                val boltVal = currentMappings["bolt_${dir.name}"]
                if (!boltVal.isNullOrEmpty()) {
                    val displayText = if (boltVal.length > 1) "${boltVal.first()}..." else boltVal
                    val textLayoutResult = textMeasurer.measure(
                        text = displayText,
                        style = TextStyle(color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    )
                    val tx = cx + boltRadius * kotlin.math.cos(angleRad).toFloat()
                    val ty = cy + boltRadius * kotlin.math.sin(angleRad).toFloat()
                    drawText(
                        textLayoutResult = textLayoutResult,
                        topLeft = Offset(tx - textLayoutResult.size.width / 2f, ty - textLayoutResult.size.height / 2f)
                    )
                }
            }
            
            // 3. Draw Center Square divided diagonally
            val halfW = 35.dp.toPx()
            
            // Tap (Top-Right triangle)
            val isTapSelected = selectedGesture == InteractionType.TAP
            val isTapHardcoded = InteractionLookupEngine.isHardcoded(square, InteractionType.TAP, null)
            val tapColor = when {
                isTapHardcoded -> Color.Gray.copy(alpha = 0.3f)
                isTapSelected -> Color(0xFFF97316) // Orange
                else -> Color(0xFF374151)
            }
            val tapPath = Path().apply {
                moveTo(cx - halfW, cy - halfW)
                lineTo(cx + halfW, cy - halfW)
                lineTo(cx + halfW, cy + halfW)
                close()
            }
            drawPath(path = tapPath, color = tapColor)
            
            // Long Press (Bottom-Left triangle)
            val isLpSelected = selectedGesture == InteractionType.LONG_PRESS
            val isLpHardcoded = InteractionLookupEngine.isHardcoded(square, InteractionType.LONG_PRESS, null)
            val lpColor = when {
                isLpHardcoded -> Color.Gray.copy(alpha = 0.3f)
                isLpSelected -> Color(0xFFEC4899) // Pink
                else -> Color(0xFF1F2937)
            }
            val lpPath = Path().apply {
                moveTo(cx - halfW, cy - halfW)
                lineTo(cx - halfW, cy + halfW)
                lineTo(cx + halfW, cy + halfW)
                close()
            }
            drawPath(path = lpPath, color = lpColor)
            
            // Draw diagonal dividing line
            drawLine(
                color = Color.DarkGray,
                start = Offset(cx - halfW, cy - halfW),
                end = Offset(cx + halfW, cy + halfW),
                strokeWidth = 2.dp.toPx()
            )
            
            // Draw half a dot for Tap (Top-Right)
            drawArc(
                color = Color.White.copy(alpha = 0.8f),
                startAngle = -135f,
                sweepAngle = 180f,
                useCenter = true,
                topLeft = Offset(cx - 12.dp.toPx(), cy - 12.dp.toPx()),
                size = androidx.compose.ui.geometry.Size(24.dp.toPx(), 24.dp.toPx())
            )
            
            // Draw half a circuit for Long Press (Bottom-Left)
            drawArc(
                color = Color.White.copy(alpha = 0.8f),
                startAngle = 45f,
                sweepAngle = 180f,
                useCenter = false,
                topLeft = Offset(cx - 12.dp.toPx(), cy - 12.dp.toPx()),
                size = androidx.compose.ui.geometry.Size(24.dp.toPx(), 24.dp.toPx()),
                style = Stroke(width = 2.dp.toPx())
            )
        }
    }
}

fun resolveTapOnChooser(
    dx: Float,
    dy: Float,
    r: Float,
    duration: Long,
    density: Density
): Pair<InteractionType?, CompassDirection?> {
    val rCenter = with(density) { 42.dp.toPx() }
    val rDrag = with(density) { 80.dp.toPx() }
    val rDragReturn = with(density) { 115.dp.toPx() }
    val rBolt = with(density) { 150.dp.toPx() }
    val rLoop = with(density) { 185.dp.toPx() }
    
    if (r < rCenter) {
        val gesture = if (duration >= 500) InteractionType.LONG_PRESS else InteractionType.TAP
        return Pair(gesture, null)
    }
    
    val angle = atan2(dy, dx) * 180 / Math.PI
    val normAngle = if (angle < 0) angle + 360 else angle
    val direction = getDirectionFromAngle(normAngle)
    
    return when {
        r < rDrag -> Pair(InteractionType.DRAG, direction)
        r < rDragReturn -> Pair(InteractionType.DRAG_RETURN, direction)
        r < rBolt -> Pair(InteractionType.BOLT, direction)
        r < rLoop -> Pair(InteractionType.LOOP, null)
        else -> Pair(null, null)
    }
}

fun getDirectionFromAngle(angle: Double): CompassDirection {
    return when {
        angle >= 337.5 || angle < 22.5 -> CompassDirection.E
        angle >= 22.5 && angle < 67.5 -> CompassDirection.SE
        angle >= 67.5 && angle < 112.5 -> CompassDirection.S
        angle >= 112.5 && angle < 157.5 -> CompassDirection.SW
        angle >= 157.5 && angle < 202.5 -> CompassDirection.W
        angle >= 202.5 && angle < 247.5 -> CompassDirection.NW
        angle >= 247.5 && angle < 292.5 -> CompassDirection.N
        else -> CompassDirection.NE
    }
}

@Composable
fun LayoutsListSection(
    onEditClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val sharedPrefs = remember { context.getSharedPreferences("gridtype_settings", Context.MODE_PRIVATE) }

    val layouts = remember {
        mutableStateListOf<String>().apply {
            addAll(getLayoutsList(context))
        }
    }

    var showAddDialog by remember { mutableStateOf(false) }
    var newLayoutName by remember { mutableStateOf("") }

    val defaultLanguages = remember {
        context.assets.list("layouts")?.filter { it.endsWith(".lang") } ?: listOf("english.lang")
    }
    var baseLanguageExpanded by remember { mutableStateOf(false) }
    var selectedBaseLanguage by remember { mutableStateOf(defaultLanguages.firstOrNull() ?: "english.lang") }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "LAYOUTS",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            IconButton(onClick = { showAddDialog = true }) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add Layout",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }

        // Reordering dragging state
        var draggingIndex by remember { mutableStateOf<Int?>(null) }
        var dragOffset by remember { mutableStateOf(0f) }

        layouts.forEachIndexed { index, layoutName ->
            key(layoutName) {
                LayoutListItem(
                    name = layoutName,
                    isDragging = draggingIndex == index,
                    dragOffset = if (draggingIndex == index) dragOffset else 0f,
                    showDeleteButton = layouts.size > 1,
                    onClick = { onEditClick(layoutName) },
                    onRemoveClick = {
                        deleteLayout(context, layoutName)
                        layouts.remove(layoutName)
                        saveLayoutsOrder(context, layouts)
                    },
                    onDragStart = {
                        draggingIndex = index
                        dragOffset = 0f
                    },
                    onDrag = { delta ->
                        dragOffset += delta
                        // Check if we should swap up
                        if (dragOffset < -80f && index > 0) {
                            val temp = layouts[index]
                            layouts[index] = layouts[index - 1]
                            layouts[index - 1] = temp
                            draggingIndex = index - 1
                            dragOffset = 0f
                            saveLayoutsOrder(context, layouts)
                        }
                        // Check if we should swap down
                        else if (dragOffset > 80f && index < layouts.size - 1) {
                            val temp = layouts[index]
                            layouts[index] = layouts[index + 1]
                            layouts[index + 1] = temp
                            draggingIndex = index + 1
                            dragOffset = 0f
                            saveLayoutsOrder(context, layouts)
                        }
                    },
                    onDragEnd = {
                        draggingIndex = null
                        dragOffset = 0f
                    }
                )
            }
        }
    }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Create New Layout") },
            text = {
                Column {
                    Text("Enter a name for the new layout:", style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    TextField(
                        value = newLayoutName,
                        onValueChange = { newLayoutName = it },
                        placeholder = { Text("e.g. My Custom ENG") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text("Base Language Template:", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { baseLanguageExpanded = true }
                            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(4.dp))
                            .padding(12.dp)
                    ) {
                        Text(
                            text = selectedBaseLanguage,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        DropdownMenu(
                            expanded = baseLanguageExpanded,
                            onDismissRequest = { baseLanguageExpanded = false }
                        ) {
                            defaultLanguages.forEach { lang ->
                                DropdownMenuItem(
                                    text = { Text(lang) },
                                    onClick = {
                                        selectedBaseLanguage = lang
                                        baseLanguageExpanded = false
                                    }
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))

                    Text("Theme:", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                            .padding(12.dp)
                    ) {
                        Text(
                            text = "Default Theme (Disabled)",
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newLayoutName.isNotBlank()) {
                            val success = createNewLayout(context, newLayoutName, selectedBaseLanguage)
                            if (success) {
                                val name = newLayoutName.trim()
                                layouts.add(name)
                                saveLayoutsOrder(context, layouts)
                            }
                            newLayoutName = ""
                            showAddDialog = false
                        }
                    }
                ) {
                    Text("Create")
                }
            },
            dismissButton = {
                Button(onClick = { showAddDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun LayoutListItem(
    name: String,
    isDragging: Boolean,
    dragOffset: Float,
    showDeleteButton: Boolean,
    onClick: () -> Unit,
    onRemoveClick: () -> Unit,
    onDragStart: () -> Unit,
    onDrag: (Float) -> Unit,
    onDragEnd: () -> Unit
) {
    var swipeOffset by remember { mutableStateOf(0f) }
    val density = LocalDensity.current
    val limitPx = if (showDeleteButton) with(density) { -80.dp.toPx() } else 0f

    val animatedSwipeOffset by animateFloatAsState(
        targetValue = swipeOffset,
        animationSpec = tween(durationMillis = 200)
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp)
            .offset { IntOffset(0, dragOffset.roundToInt()) }
            .background(Color.Transparent),
        contentAlignment = Alignment.CenterStart
    ) {
        // Behind layer: Delete button
        if (showDeleteButton) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .width(80.dp)
                        .fillMaxHeight()
                        .background(Color(0xFFEF4444))
                        .clickable {
                            swipeOffset = 0f
                            onRemoveClick()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", tint = Color.White)
                        Text("Delete", color = Color.White, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }

        // Front layer: Layout Card
        Row(
            modifier = Modifier
                .fillMaxSize()
                .offset { IntOffset(animatedSwipeOffset.roundToInt(), 0) }
                .background(MaterialTheme.colorScheme.surface)
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            swipeOffset = if (swipeOffset < limitPx / 2f) {
                                limitPx
                            } else {
                                0f
                            }
                        },
                        onDragCancel = {
                            swipeOffset = 0f
                        },
                        onHorizontalDrag = { change, dragAmount ->
                            change.consume()
                            swipeOffset = (swipeOffset + dragAmount).coerceIn(limitPx, 0f)
                        }
                    )
                }
                .clickable { onClick() }
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Drag handle on the left
            Icon(
                imageVector = Icons.Default.Menu,
                contentDescription = "Drag to reorder",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .size(28.dp)
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = { onDragStart() },
                            onDragEnd = { onDragEnd() },
                            onDragCancel = { onDragEnd() },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                onDrag(dragAmount.y)
                            }
                        )
                    }
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = if (name == "English") "System Default" else "User Custom Layout",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// Helper traditional settings composables

@Composable
fun SettingsCategoryHeader(
    title: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = title.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Bold,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 16.dp)
    )
}

@Composable
fun SettingsListItem(
    title: String,
    subtitle: String? = null,
    leadingIcon: (@Composable () -> Unit)? = null,
    trailingContent: (@Composable () -> Unit)? = null,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (leadingIcon != null) {
                    leadingIcon()
                    Spacer(modifier = Modifier.width(16.dp))
                }
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Medium
                    )
                    if (subtitle != null) {
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            if (trailingContent != null) {
                trailingContent()
            } else {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
        HorizontalDivider(
            color = MaterialTheme.colorScheme.outlineVariant,
            thickness = 0.5.dp,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
    }
}

@Composable
fun SettingsToggleItem(
    title: String,
    subtitle: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Medium
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                    checkedTrackColor = MaterialTheme.colorScheme.primary,
                    uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                    uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            )
        }
        HorizontalDivider(
            color = MaterialTheme.colorScheme.outlineVariant,
            thickness = 0.5.dp,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
    }
}

@Composable
fun SettingsSliderItem(
    title: String,
    subtitle: String? = null,
    value: Int,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Medium
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = "${value}ms",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.width(80.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.End
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        Slider(
            value = value.toFloat(),
            onValueChange = { newValue ->
                val rounded = (newValue / 10f).roundToInt() * 10
                onValueChange(rounded.coerceIn(10, 2000))
            },
            valueRange = valueRange,
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary,
                inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        )
        Spacer(modifier = Modifier.height(8.dp))
        HorizontalDivider(
            color = MaterialTheme.colorScheme.outlineVariant,
            thickness = 0.5.dp,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}

@Composable
fun SettingsPercentSliderItem(
    title: String,
    subtitle: String? = null,
    value: Int,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Medium
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = "$value%",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.width(80.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.End
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        Slider(
            value = value.toFloat(),
            onValueChange = { newValue ->
                onValueChange(newValue.roundToInt().coerceIn(70, 130))
            },
            valueRange = valueRange,
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary,
                inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        )
        Spacer(modifier = Modifier.height(8.dp))
        HorizontalDivider(
            color = MaterialTheme.colorScheme.outlineVariant,
            thickness = 0.5.dp,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}

@Composable
fun SetupStepItem(
    stepNumber: String,
    title: String,
    description: String,
    isCompleted: Boolean,
    actionLabel: String,
    onActionClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(if (isCompleted) Color(0xFF4BB543) else MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            if (isCompleted) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Completed",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            } else {
                Text(
                    text = stepNumber,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        if (!isCompleted) {
            Button(
                onClick = onActionClick,
                enabled = enabled,
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Text(
                    text = actionLabel,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

// Layout editor helper functions

fun getMappingsForSquare(
    context: Context,
    layoutName: String,
    square: String
): Map<String, String> {
    val map = mutableMapOf<String, String>()
    
    // 1. Load base layout first
    val baseLayoutName = if (layoutName.endsWith(".lang")) {
        layoutName
    } else {
        val file = File(File(context.filesDir, "layouts"), "$layoutName.json")
        if (file.exists()) {
            try {
                val json = JSONObject(file.readText().replace(Regex("(?s)/\\*.*?\\*/|//.*?\r?\n"), ""))
                json.optString("baseLayout", "english.lang")
            } catch (e: Exception) {
                "english.lang"
            }
        } else {
            "english.lang"
        }
    }
    
    try {
        val localBaseFile = File(File(context.filesDir, "layouts"), baseLayoutName)
        val baseJsonString = if (localBaseFile.exists()) {
            localBaseFile.readText()
        } else {
            context.assets.open("layouts/$baseLayoutName").bufferedReader().use { it.readText() }
        }.replace(Regex("(?s)/\\*.*?\\*/|//.*?\r?\n"), "")
        
        val baseObj = JSONObject(baseJsonString)
        val mappings = baseObj.getJSONArray("mappings")
        for (i in 0 until mappings.length()) {
            val m = mappings.getJSONObject(i)
            if (m.getString("square") == square) {
                val gst = m.getString("gesture").lowercase()
                val dir = if (m.isNull("direction")) "" else m.getString("direction").uppercase()
                val key = if (dir.isEmpty()) gst else "${gst}_$dir"
                map[key] = m.getString("value")
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    
    // 2. Load user overrides if not editing a .lang file directly
    if (!layoutName.endsWith(".lang")) {
        try {
            val file = File(File(context.filesDir, "layouts"), "$layoutName.json")
            if (file.exists()) {
                val json = JSONObject(file.readText().replace(Regex("(?s)/\\*.*?\\*/|//.*?\r?\n"), ""))
                val overrides = json.getJSONArray("overrides")
                for (i in 0 until overrides.length()) {
                    val m = overrides.getJSONObject(i)
                    if (m.getString("square") == square) {
                        val gst = m.getString("gesture").lowercase()
                        val dir = if (m.isNull("direction")) "" else m.getString("direction").uppercase()
                        val key = if (dir.isEmpty()) gst else "${gst}_$dir"
                        map[key] = m.getString("value")
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    return map
}

fun getDefaultValueForGesture(
    context: Context,
    layoutName: String,
    square: String,
    gesture: InteractionType,
    direction: CompassDirection?
): String? {
    if (layoutName.endsWith(".lang")) {
        return null
    }
    
    val layoutsDir = File(context.filesDir, "layouts")
    val userLayoutFile = File(layoutsDir, "$layoutName.json")
    if (!userLayoutFile.exists()) return null
    
    try {
        val jsonString = userLayoutFile.readText().replace(Regex("(?s)/\\*.*?\\*/|//.*?\r?\n"), "")
        val jsonObject = JSONObject(jsonString)
        val baseLayout = jsonObject.optString("baseLayout", "english.lang")
        
        val assetPath = "layouts/$baseLayout"
        val localBaseFile = File(layoutsDir, baseLayout)
        val baseJsonString = if (localBaseFile.exists()) {
            localBaseFile.readText()
        } else {
            context.assets.open(assetPath).bufferedReader().use { it.readText() }
        }.replace(Regex("(?s)/\\*.*?\\*/|//.*?\r?\n"), "")
        
        val baseObj = JSONObject(baseJsonString)
        val mappings = baseObj.getJSONArray("mappings")
        for (i in 0 until mappings.length()) {
            val mapObj = mappings.getJSONObject(i)
            val sq = mapObj.getString("square")
            val gst = mapObj.getString("gesture")
            val dir = if (mapObj.isNull("direction")) null else mapObj.getString("direction")
            val valStr = mapObj.getString("value")
            
            if (sq == square && gst.equals(gesture.name, ignoreCase = true) && dir == direction?.name) {
                return valStr
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return ""
}

fun saveGestureValue(
    context: Context,
    layoutName: String,
    square: String,
    gesture: InteractionType,
    direction: CompassDirection?,
    newValue: String
) {
    val layoutsDir = File(context.filesDir, "layouts")
    if (!layoutsDir.exists()) {
        layoutsDir.mkdirs()
    }
    
    if (layoutName.endsWith(".lang")) {
        val file = File(layoutsDir, layoutName)
        val jsonObject = if (file.exists()) {
            try {
                val jsonString = file.readText().replace(Regex("(?s)/\\*.*?\\*/|//.*?\r?\n"), "")
                JSONObject(jsonString)
            } catch (e: Exception) {
                JSONObject().apply {
                    put("name", layoutName.removeSuffix(".lang").replaceFirstChar { it.uppercase() })
                    put("mappings", org.json.JSONArray())
                }
            }
        } else {
            try {
                val assetJson = context.assets.open("layouts/$layoutName").bufferedReader().use { it.readText() }
                val cleanJson = assetJson.replace(Regex("(?s)/\\*.*?\\*/|//.*?\r?\n"), "")
                JSONObject(cleanJson)
            } catch (e: Exception) {
                JSONObject().apply {
                    put("name", layoutName.removeSuffix(".lang").replaceFirstChar { it.uppercase() })
                    put("mappings", org.json.JSONArray())
                }
            }
        }
        
        val mappings = jsonObject.getJSONArray("mappings")
        var found = false
        for (i in 0 until mappings.length()) {
            val mapObj = mappings.getJSONObject(i)
            val sq = mapObj.getString("square")
            val gst = mapObj.getString("gesture")
            val dir = if (mapObj.isNull("direction")) null else mapObj.getString("direction")
            
            if (sq == square && gst.equals(gesture.name, ignoreCase = true) && dir == direction?.name) {
                mapObj.put("value", newValue)
                found = true
                break
            }
        }
        
        if (!found) {
            val newMapping = JSONObject().apply {
                put("square", square)
                put("gesture", gesture.name.lowercase())
                if (direction != null) put("direction", direction.name) else put("direction", JSONObject.NULL)
                put("value", newValue)
            }
            mappings.put(newMapping)
        }
        
        file.writeText(jsonObject.toString(2))
    } else {
        val file = File(layoutsDir, "$layoutName.json")
        if (!file.exists()) return
        
        val jsonString = file.readText().replace(Regex("(?s)/\\*.*?\\*/|//.*?\r?\n"), "")
        val jsonObject = JSONObject(jsonString)
        val overrides = jsonObject.getJSONArray("overrides")
        
        var found = false
        for (i in 0 until overrides.length()) {
            val mapObj = overrides.getJSONObject(i)
            val sq = mapObj.getString("square")
            val gst = mapObj.getString("gesture")
            val dir = if (mapObj.isNull("direction")) null else mapObj.getString("direction")
            
            if (sq == square && gst.equals(gesture.name, ignoreCase = true) && dir == direction?.name) {
                mapObj.put("value", newValue)
                found = true
                break
            }
        }
        
        if (!found) {
            val newOverride = JSONObject().apply {
                put("square", square)
                put("gesture", gesture.name.lowercase())
                if (direction != null) put("direction", direction.name) else put("direction", JSONObject.NULL)
                put("value", newValue)
            }
            overrides.put(newOverride)
        }
        
        file.writeText(jsonObject.toString(2))
    }
}

fun resetGestureValue(
    context: Context,
    layoutName: String,
    square: String,
    gesture: InteractionType,
    direction: CompassDirection?
) {
    if (layoutName.endsWith(".lang")) {
        return
    }
    
    val layoutsDir = File(context.filesDir, "layouts")
    val file = File(layoutsDir, "$layoutName.json")
    if (!file.exists()) return
    
    try {
        val jsonString = file.readText().replace(Regex("(?s)/\\*.*?\\*/|//.*?\r?\n"), "")
        val jsonObject = JSONObject(jsonString)
        val overrides = jsonObject.getJSONArray("overrides")
        
        val newOverrides = org.json.JSONArray()
        for (i in 0 until overrides.length()) {
            val mapObj = overrides.getJSONObject(i)
            val sq = mapObj.getString("square")
            val gst = mapObj.getString("gesture")
            val dir = if (mapObj.isNull("direction")) null else mapObj.getString("direction")
            
            if (!(sq == square && gst.equals(gesture.name, ignoreCase = true) && dir == direction?.name)) {
                newOverrides.put(mapObj)
            }
        }
        
        jsonObject.put("overrides", newOverrides)
        file.writeText(jsonObject.toString(2))
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

// End of MainScreen.kt
