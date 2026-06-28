package com.gridtype.keyboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import kotlin.math.roundToInt
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.geometry.Offset

@Composable
fun KeyboardView(
    onKeyClick: (String) -> Unit,
    onSettingsClick: () -> Unit,
    onBackspaceClick: () -> Unit,
    onHideKeyboardClick: () -> Unit,
    onMoveCursorLeft: () -> Unit,
    onMoveCursorRight: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val sharedPrefs = remember { context.getSharedPreferences("gridtype_settings", android.content.Context.MODE_PRIVATE) }

    val vibrator = remember { context.getSystemService(android.os.Vibrator::class.java) }
    fun vibrateDot() {
        try {
            vibrator?.vibrate(android.os.VibrationEffect.createOneShot(15, android.os.VibrationEffect.DEFAULT_AMPLITUDE))
        } catch (e: Exception) {
            // Ignore
        }
    }

    val configuration = LocalConfiguration.current
    val screenWidthDp = configuration.screenWidthDp.toFloat()
    val screenHeightDp = configuration.screenHeightDp.toFloat()
    val orientationStr = if (configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE) "LANDSCAPE" else "PORTRAIT"

    val displayId = try {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            context.display.displayId
        } else {
            @Suppress("DEPRECATION")
            (context.getSystemService(android.content.Context.WINDOW_SERVICE) as? android.view.WindowManager)
                ?.defaultDisplay?.displayId ?: 0
        }
    } catch (e: Throwable) {
        0
    }

    val prefKey = "grid_horizontal_offset_ID${displayId}_$orientationStr"
    val heightPrefKey = "grid_height_ID${displayId}_$orientationStr"

    var isEmojiDrawerOpen by remember { mutableStateOf(false) }
    var keyboardHeight by remember(heightPrefKey) {
        val saved = sharedPrefs.getFloat(heightPrefKey, 240f)
        mutableStateOf(saved.coerceIn(180f, screenWidthDp).dp)
    }
    var keyboardAspectRatio by remember {
        val saved = sharedPrefs.getInt("keyboard_aspect_ratio", 110)
        mutableStateOf(saved.coerceIn(70, 130))
    }
    val density = LocalDensity.current
    val currentHeight = keyboardHeight

    // Bounds for the horizontal offset
    val gridWidthDp = keyboardHeight.value * (keyboardAspectRatio / 100f)
    val maxOffset = (screenWidthDp - gridWidthDp) / 2f
    val minOffset = -maxOffset
    val defaultOffset = ((screenWidthDp * 0.25f) / 20f).roundToInt() * 20f

    var gridHorizontalOffset by remember(prefKey) {
        val saved = sharedPrefs.getFloat(prefKey, defaultOffset)
        mutableStateOf(saved.coerceIn(minOffset, maxOffset))
    }
    val currentOffset = gridHorizontalOffset.coerceIn(minOffset, maxOffset)

    var lastInteraction by remember { mutableStateOf<InteractionDescription?>(null) }
    var activeSquare by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()

    var showDebugInfo by remember {
        mutableStateOf(sharedPrefs.getBoolean("show_interaction_debug_info", true))
    }
    var longPressDelay by remember {
        val saved = sharedPrefs.getInt("long_press_delay", 670)
        val rounded = ((saved / 10f).roundToInt() * 10).coerceIn(10, 2000)
        mutableStateOf(rounded)
    }

    DisposableEffect(sharedPrefs) {
        InteractionLookupEngine.loadLayout(context)

        val listener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == "show_interaction_debug_info") {
                showDebugInfo = sharedPrefs.getBoolean("show_interaction_debug_info", true)
            } else if (key == "long_press_delay") {
                val saved = sharedPrefs.getInt("long_press_delay", 670)
                longPressDelay = ((saved / 10f).roundToInt() * 10).coerceIn(10, 2000)
            } else if (key == "keyboard_aspect_ratio") {
                keyboardAspectRatio = sharedPrefs.getInt("keyboard_aspect_ratio", 110).coerceIn(70, 130)
            } else if (key == "active_layout") {
                InteractionLookupEngine.loadLayout(context)
            } else if (key == heightPrefKey) {
                keyboardHeight = sharedPrefs.getFloat(heightPrefKey, 240f).coerceIn(180f, screenWidthDp).dp
            } else if (key == prefKey) {
                val gridWidthVal = keyboardHeight.value * (keyboardAspectRatio / 100f)
                val maxOffsetVal = (screenWidthDp - gridWidthVal) / 2f
                val minOffsetVal = -maxOffsetVal
                gridHorizontalOffset = sharedPrefs.getFloat(prefKey, defaultOffset).coerceIn(minOffsetVal, maxOffsetVal)
            }
        }
        sharedPrefs.registerOnSharedPreferenceChangeListener(listener)
        onDispose {
            sharedPrefs.unregisterOnSharedPreferenceChangeListener(listener)
        }
    }

    fun executeKeyboardCommand(command: KeyboardCommand) {
        when (command) {
            is KeyboardCommand.TypeString -> {
                if (command.cleanText.isNotEmpty()) {
                    onKeyClick(command.cleanText)
                }
            }
            KeyboardCommand.CarriageReturn -> onKeyClick("\n")
            KeyboardCommand.OpenEmojiDrawer -> isEmojiDrawerOpen = true
            KeyboardCommand.OpenSettings -> onSettingsClick()
            KeyboardCommand.SmartBackspace -> onBackspaceClick()
            KeyboardCommand.HideKeyboard -> onHideKeyboardClick()
            KeyboardCommand.MoveCursorLeft -> onMoveCursorLeft()
            KeyboardCommand.MoveCursorRight -> onMoveCursorRight()
            KeyboardCommand.CycleLayout -> {
                val layoutsList = getLayoutsList(context)
                val activeLayoutName = sharedPrefs.getString("active_layout", "English") ?: "English"
                val currentIndex = layoutsList.indexOf(activeLayoutName)
                if (currentIndex != -1 && layoutsList.isNotEmpty()) {
                    val nextIndex = (currentIndex + 1) % layoutsList.size
                    val nextLayout = layoutsList[nextIndex]
                    sharedPrefs.edit().putString("active_layout", nextLayout).apply()
                }
            }
            KeyboardCommand.IncrementHeight -> {
                val newHeight = (keyboardHeight.value + 20f).coerceIn(180f, screenWidthDp)
                keyboardHeight = newHeight.dp
                sharedPrefs.edit().putFloat(heightPrefKey, newHeight).apply()
            }
            KeyboardCommand.DecrementHeight -> {
                val newHeight = (keyboardHeight.value - 20f).coerceIn(180f, screenWidthDp)
                keyboardHeight = newHeight.dp
                sharedPrefs.edit().putFloat(heightPrefKey, newHeight).apply()
            }
            KeyboardCommand.IncrementHorizontalOffset -> {
                val gridWidthVal = keyboardHeight.value * (keyboardAspectRatio / 100f)
                val maxOffsetVal = (screenWidthDp - gridWidthVal) / 2f
                val minOffsetVal = -maxOffsetVal
                val newOffset = (gridHorizontalOffset + 20f).coerceIn(minOffsetVal, maxOffsetVal)
                gridHorizontalOffset = newOffset
                sharedPrefs.edit().putFloat(prefKey, newOffset).apply()
            }
            KeyboardCommand.DecrementHorizontalOffset -> {
                val gridWidthVal = keyboardHeight.value * (keyboardAspectRatio / 100f)
                val maxOffsetVal = (screenWidthDp - gridWidthVal) / 2f
                val minOffsetVal = -maxOffsetVal
                val newOffset = (gridHorizontalOffset - 20f).coerceIn(minOffsetVal, maxOffsetVal)
                gridHorizontalOffset = newOffset
                sharedPrefs.edit().putFloat(prefKey, newOffset).apply()
            }
        }
    }

    val overlayAlpha = remember { androidx.compose.animation.core.Animatable(0f) }

    LaunchedEffect(lastInteraction, activeSquare) {
        if (activeSquare != null) {
            overlayAlpha.snapTo(0f)
        } else if (lastInteraction != null) {
            overlayAlpha.snapTo(1f)
            delay(5000)
            overlayAlpha.animateTo(
                targetValue = 0f,
                animationSpec = androidx.compose.animation.core.tween(durationMillis = 5000)
            )
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .height(currentHeight)
            .background(Color(0xFF1E1E24)) // Dark premium slate background
    ) {
        if (isEmojiDrawerOpen) {
            var emojiViewRef by remember { mutableStateOf<com.vanniktech.emoji.EmojiView?>(null) }

            DisposableEffect(Unit) {
                onDispose {
                    emojiViewRef?.tearDown()
                }
            }

            Column(modifier = Modifier.fillMaxSize()) {
                // Drag Handle at the top of the emoji drawer
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(16.dp)
                        .background(Color(0xFF1E1E24))
                        .pointerInput(Unit) {
                            detectDragGestures { change, dragAmount ->
                                change.consume()
                                val dragDp = with(density) { -dragAmount.y.toDp() }
                                val newH = (keyboardHeight.value + dragDp.value).coerceIn(180f, screenWidthDp).dp
                                keyboardHeight = newH
                                sharedPrefs.edit().putFloat(heightPrefKey, newH.value).apply()
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .width(36.dp)
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(Color(0xFF4E4F62))
                    )
                }

                // Vanniktech EmojiView wrapped in AndroidView
                AndroidView(
                    factory = { context ->
                        val themedContext = android.view.ContextThemeWrapper(context, com.gridtype.keyboard.R.style.Theme_GridType)
                        com.vanniktech.emoji.EmojiView(themedContext).apply {
                            emojiViewRef = this
                            
                            val noSearchEmojiInstance = try {
                                val clazz = Class.forName("com.vanniktech.emoji.search.NoSearchEmoji")
                                val constructor = clazz.getDeclaredConstructor()
                                constructor.isAccessible = true
                                constructor.newInstance() as com.vanniktech.emoji.search.SearchEmoji
                            } catch (e: Exception) {
                                object : com.vanniktech.emoji.search.SearchEmoji {
                                    override fun search(query: String): List<com.vanniktech.emoji.search.SearchEmojiResult> = emptyList()
                                }
                            }

                            setUp(
                                rootView = this,
                                onEmojiClickListener = { emoji ->
                                    onKeyClick(emoji.unicode)
                                },
                                onEmojiBackspaceClickListener = null, // Hide top backspace button
                                editText = null,
                                searchEmoji = noSearchEmojiInstance // Prevent search crashes by hiding search icon
                            )
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                )

                // Emoji Drawer Bottom Controller Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .background(Color(0xFF16161D)) // Slightly darker bottom bar
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Switch back to ABC button
                    Box(
                        modifier = Modifier
                            .height(36.dp)
                            .width(64.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF2E2F3E))
                            .clickable { isEmojiDrawerOpen = false },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "ABC",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    // Backspace button
                    Box(
                        modifier = Modifier
                            .height(36.dp)
                            .width(64.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF2E2F3E))
                            .clickable { onBackspaceClick() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Backspace",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        } else {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                // Centered Grid Container with Aspect Ratio Width
                val gridWidth = currentHeight * (keyboardAspectRatio / 100f)
                Box(
                    modifier = Modifier
                        .width(gridWidth)
                        .height(currentHeight)
                        .offset(x = currentOffset.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(currentHeight, keyboardAspectRatio) {
                                awaitPointerEventScope {
                                    while (true) {
                                        val down = awaitFirstDown()
                                        val W = size.width
                                        val H = size.height
                                        val colWidth = W / 4f
                                        val rowHeight = H / 4f

                                        val startX = kotlin.math.floor(down.position.x / colWidth).toInt().coerceIn(0, 3)
                                        val startY = kotlin.math.floor(down.position.y / rowHeight).toInt().coerceIn(0, 3)
                                        val squareName = "$startX$startY"

                                        activeSquare = squareName
                                        var isSwipe = false
                                        var longPressTriggered = false
                                        val dwells = mutableListOf<DwellTrack>()
                                        dwells.add(DwellTrack("C", down.uptimeMillis))

                                        // Pre-populate with empty to show active start
                                        lastInteraction = InteractionDescription(
                                            squareName = squareName,
                                            rawInteraction = emptyList(),
                                            interaction = Interaction(null, null),
                                            timings = dwells.toList()
                                        )

                                        val longPressJob = coroutineScope.launch {
                                            delay(longPressDelay.toLong())
                                            if (!isSwipe) {
                                                longPressTriggered = true
                                                vibrateDot()
                                                val (raw, inter) = resolveInteraction(
                                                    dwells = dwells,
                                                    longPressTriggered = true,
                                                    isSwipe = false,
                                                    touchReleased = false,
                                                    currentTimeMs = android.os.SystemClock.uptimeMillis()
                                                )
                                                val desc = InteractionDescription(
                                                    squareName = squareName,
                                                    rawInteraction = raw,
                                                    interaction = inter,
                                                    timings = dwells.toList()
                                                )
                                                lastInteraction = desc
                                                val command = InteractionLookupEngine.lookup(desc)
                                                if (command != null) {
                                                    executeKeyboardCommand(command)
                                                }
                                            }
                                        }

                                        var pointer = down
                                        while (pointer.pressed) {
                                            val event = awaitPointerEvent()
                                            val drag = event.changes.firstOrNull { it.id == pointer.id }
                                            if (drag == null || !drag.pressed) break
                                            pointer = drag

                                            val curX = kotlin.math.floor(drag.position.x / colWidth).toInt()
                                            val curY = kotlin.math.floor(drag.position.y / rowHeight).toInt()
                                            val hoverSquare = "$curX$curY"
                                            activeSquare = hoverSquare

                                            val sector = when {
                                                curX == startX && curY < startY -> "N"
                                                curX > startX && curY < startY -> "NE"
                                                curX > startX && curY == startY -> "E"
                                                curX > startX && curY > startY -> "SE"
                                                curX == startX && curY > startY -> "S"
                                                curX < startX && curY > startY -> "SW"
                                                curX < startX && curY == startY -> "W"
                                                curX < startX && curY < startY -> "NW"
                                                else -> "C"
                                            }

                                            if (sector != "C") {
                                                longPressJob.cancel()
                                                isSwipe = true
                                            }

                                            val lastDwell = dwells.lastOrNull()
                                            if (lastDwell != null && lastDwell.sector != sector) {
                                                lastDwell.exitTime = drag.uptimeMillis
                                                dwells.add(DwellTrack(sector, drag.uptimeMillis))
                                                vibrateDot()
                                            }

                                            if (isSwipe) {
                                                val (raw, inter) = resolveInteraction(
                                                    dwells = dwells,
                                                    longPressTriggered = false,
                                                    isSwipe = true,
                                                    touchReleased = false,
                                                    currentTimeMs = drag.uptimeMillis
                                                )
                                                lastInteraction = InteractionDescription(
                                                    squareName = squareName,
                                                    rawInteraction = raw,
                                                    interaction = inter,
                                                    timings = dwells.toList()
                                                )
                                            }
                                        }

                                        // Touch up
                                        longPressJob.cancel()
                                        activeSquare = null

                                        val lastDwell = dwells.lastOrNull()
                                        if (lastDwell != null) {
                                            lastDwell.exitTime = pointer.uptimeMillis
                                        }

                                        val (raw, inter) = resolveInteraction(
                                            dwells = dwells,
                                            longPressTriggered = longPressTriggered,
                                            isSwipe = isSwipe,
                                            touchReleased = true,
                                            currentTimeMs = pointer.uptimeMillis
                                        )
                                        val desc = InteractionDescription(
                                            squareName = squareName,
                                            rawInteraction = raw,
                                            interaction = inter,
                                            timings = dwells.toList()
                                        )
                                        lastInteraction = desc
                                        if (!longPressTriggered) {
                                            if (inter.type == InteractionType.TAP) {
                                                vibrateDot()
                                            }
                                            val command = InteractionLookupEngine.lookup(desc)
                                            if (command != null) {
                                                executeKeyboardCommand(command)
                                            }
                                        }
                                    }
                                }
                            }
                    ) {
                        for (y in 0..3) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(currentHeight / 4)
                            ) {
                                for (x in 0..3) {
                                    val name = "$x$y"
                                    val isPressed = activeSquare == name
                                    Box(
                                        modifier = Modifier
                                            .width(gridWidth / 4)
                                            .height(currentHeight / 4)
                                            .background(if (isPressed) Color(0xFF6C5DD3) else Color(0xFF2E2F3E))
                                            .border(width = 0.5.dp, color = Color(0xFF4E4F62)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = name,
                                            color = Color.White,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Overlays Layer (always drawn on top of the entire grid)
                    val localInteraction = lastInteraction
                    val currentAlpha = overlayAlpha.value
                    if (showDebugInfo && localInteraction != null && currentAlpha > 0f) {
                        val name = localInteraction.squareName
                        if (name.length == 2) {
                            val startX = name[0].toString().toIntOrNull() ?: 0
                            val startY = name[1].toString().toIntOrNull() ?: 0
                            val interaction = localInteraction.interaction
                            val isDiscarded = localInteraction.rawInteraction == listOf("DISCARDED")

                            androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                                val squareWidth = size.width / 4f
                                val squareHeight = size.height / 4f
                                val squareSize = (squareWidth + squareHeight) / 2f
                                val targetCenter = Offset((startX + 0.5f) * squareWidth, (startY + 0.5f) * squareHeight)

                                // 1. Paint background overlay if unresolved or discarded
                                if (isDiscarded) {
                                    drawRect(
                                        color = Color(0xFF8B5A2B).copy(alpha = 0.5f * currentAlpha),
                                        topLeft = Offset(startX * squareWidth, startY * squareHeight),
                                        size = androidx.compose.ui.geometry.Size(squareWidth, squareHeight)
                                    )
                                } else if (interaction.type == null) {
                                    drawRect(
                                        color = Color(0xFFEF4444).copy(alpha = 0.4f * currentAlpha),
                                        topLeft = Offset(startX * squareWidth, startY * squareHeight),
                                        size = androidx.compose.ui.geometry.Size(squareWidth, squareHeight)
                                    )
                                }

                                // 2. Draw overlay shapes
                                if (!isDiscarded && interaction.type != null) {
                                    when (interaction.type) {
                                        InteractionType.TAP -> {
                                            drawCircle(Color.Yellow.copy(alpha = currentAlpha), radius = 9.dp.toPx(), center = targetCenter)
                                        }
                                        InteractionType.LONG_PRESS -> {
                                            drawCircle(
                                                color = Color.Yellow.copy(alpha = currentAlpha),
                                                radius = 24.dp.toPx(),
                                                center = targetCenter,
                                                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3.dp.toPx())
                                            )
                                        }
                                        InteractionType.DRAG -> {
                                            val dir = interaction.direction
                                            if (dir != null) {
                                                val isDiagonal = dir == CompassDirection.NE || dir == CompassDirection.SE || dir == CompassDirection.SW || dir == CompassDirection.NW
                                                val angle = getDirectionAngle(dir)
                                                val length = if (isDiagonal) squareSize * 1.4142f else squareSize
                                                rotate(angle, pivot = targetCenter) {
                                                    val start = targetCenter
                                                    val end = Offset(targetCenter.x, targetCenter.y - length)
                                                    drawLine(color = Color.Green.copy(alpha = currentAlpha), start = start, end = end, strokeWidth = 6.dp.toPx())
                                                    val arrowLength = 16.dp.toPx()
                                                    val arrowWidth = 12.dp.toPx()
                                                    drawLine(color = Color.Green.copy(alpha = currentAlpha), start = end, end = Offset(end.x - arrowWidth, end.y + arrowLength), strokeWidth = 6.dp.toPx())
                                                    drawLine(color = Color.Green.copy(alpha = currentAlpha), start = end, end = Offset(end.x + arrowWidth, end.y + arrowLength), strokeWidth = 6.dp.toPx())
                                                }
                                            }
                                        }
                                        InteractionType.DRAG_RETURN -> {
                                            val dir = interaction.direction
                                            if (dir != null) {
                                                val isDiagonal = dir == CompassDirection.NE || dir == CompassDirection.SE || dir == CompassDirection.SW || dir == CompassDirection.NW
                                                val angle = getDirectionAngle(dir)
                                                val length = if (isDiagonal) squareSize * 1.4142f else squareSize
                                                rotate(angle, pivot = targetCenter) {
                                                    val start = targetCenter
                                                    val end = Offset(targetCenter.x, targetCenter.y - length)
                                                    drawLine(color = Color.Green.copy(alpha = currentAlpha), start = start, end = end, strokeWidth = 6.dp.toPx())
                                                    val arrowLength = 16.dp.toPx()
                                                    val arrowWidth = 12.dp.toPx()
                                                    drawLine(color = Color.Green.copy(alpha = currentAlpha), start = end, end = Offset(end.x - arrowWidth, end.y + arrowLength), strokeWidth = 6.dp.toPx())
                                                    drawLine(color = Color.Green.copy(alpha = currentAlpha), start = end, end = Offset(end.x + arrowWidth, end.y + arrowLength), strokeWidth = 6.dp.toPx())
                                                    drawLine(color = Color.Green.copy(alpha = currentAlpha), start = start, end = Offset(start.x - arrowWidth, start.y - arrowLength), strokeWidth = 6.dp.toPx())
                                                    drawLine(color = Color.Green.copy(alpha = currentAlpha), start = start, end = Offset(start.x + arrowWidth, start.y - arrowLength), strokeWidth = 6.dp.toPx())
                                                }
                                            }
                                        }
                                        InteractionType.BOLT -> {
                                            val dir = interaction.direction
                                            if (dir != null) {
                                                val isDiagonal = dir == CompassDirection.NE || dir == CompassDirection.SE || dir == CompassDirection.SW || dir == CompassDirection.NW
                                                val angle = getDirectionAngle(dir)
                                                val length = if (isDiagonal) squareSize * 1.4142f else squareSize
                                                rotate(angle, pivot = targetCenter) {
                                                    val start = targetCenter
                                                    val end = Offset(targetCenter.x, targetCenter.y - length)
                                                    drawLine(color = Color.Yellow.copy(alpha = currentAlpha), start = start, end = end, strokeWidth = 6.dp.toPx())
                                                    val arrowLength = 16.dp.toPx()
                                                    val arrowWidth = 12.dp.toPx()
                                                    drawLine(color = Color.Yellow.copy(alpha = currentAlpha), start = end, end = Offset(end.x - arrowWidth, end.y + arrowLength), strokeWidth = 6.dp.toPx())
                                                    drawLine(color = Color.Yellow.copy(alpha = currentAlpha), start = end, end = Offset(end.x + arrowWidth, end.y + arrowLength), strokeWidth = 6.dp.toPx())
                                                }
                                            }
                                        }
                                        InteractionType.LOOP -> {
                                            drawCircle(
                                                color = Color(0xFF3B82F6).copy(alpha = currentAlpha),
                                                radius = 24.dp.toPx(),
                                                center = targetCenter,
                                                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3.dp.toPx())
                                            )
                                        }
                                    }
                                }

                                // 3. Draw red X for any discarded squares
                                val discardedSquaresList = getDiscardedSquares(localInteraction)
                                for (discardedName in discardedSquaresList) {
                                    if (discardedName.length == 2) {
                                        val dx = discardedName[0].toString().toIntOrNull() ?: 0
                                        val dy = discardedName[1].toString().toIntOrNull() ?: 0
                                        val cx = (dx + 0.5f) * squareWidth
                                        val cy = (dy + 0.5f) * squareHeight
                                        val offset = 10.dp.toPx()
                                        drawLine(
                                            color = Color.Red.copy(alpha = currentAlpha),
                                            start = Offset(cx - offset, cy - offset),
                                            end = Offset(cx + offset, cy + offset),
                                            strokeWidth = 3.dp.toPx()
                                        )
                                        drawLine(
                                            color = Color.Red.copy(alpha = currentAlpha),
                                            start = Offset(cx - offset, cy + offset),
                                            end = Offset(cx + offset, cy - offset),
                                            strokeWidth = 3.dp.toPx()
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Debug preview area floating at the top-left (so it does not shrink or push the grid)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp),
                    contentAlignment = Alignment.TopStart
                ) {
                    Text(
                        text = if (lastInteraction != null) {
                            "Square: ${lastInteraction!!.squareName} | Type: ${lastInteraction!!.interaction.type} | Dir: ${lastInteraction!!.interaction.direction} | Timings: ${lastInteraction!!.timings.map { "${it.sector}:${if (it.exitTime != null) "${it.exitTime!! - it.entryTime}ms" else "active"}" }}"
                        } else {
                            "Interactions list is empty"
                        },
                        fontSize = 10.sp,
                        color = Color(0xFF9E9EAF),
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .background(Color(0xCC1E1E24), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}

data class InteractionDescription(
    val squareName: String,
    val rawInteraction: List<String>,
    val interaction: Interaction,
    val timings: List<DwellTrack>
)

data class DwellTrack(
    val sector: String,
    val entryTime: Long,
    var exitTime: Long? = null
)

fun areAdjacent(dir1: String, dir2: String): Boolean {
    val c1 = when (dir1) {
        "NW" -> Pair(0, 0)
        "N"  -> Pair(0, 1)
        "NE" -> Pair(0, 2)
        "W"  -> Pair(1, 0)
        "E"  -> Pair(1, 2)
        "SW" -> Pair(2, 0)
        "S"  -> Pair(2, 1)
        "SE" -> Pair(2, 2)
        else -> return false
    }
    val c2 = when (dir2) {
        "NW" -> Pair(0, 0)
        "N"  -> Pair(0, 1)
        "NE" -> Pair(0, 2)
        "W"  -> Pair(1, 0)
        "E"  -> Pair(1, 2)
        "SW" -> Pair(2, 0)
        "S"  -> Pair(2, 1)
        "SE" -> Pair(2, 2)
        else -> return false
    }
    return Math.abs(c1.first - c2.first) <= 1 && Math.abs(c1.second - c2.second) <= 1
}

enum class InteractionType {
    TAP, LONG_PRESS, DRAG, DRAG_RETURN, BOLT, LOOP;

    override fun toString(): String {
        return when (this) {
            TAP -> "tap"
            LONG_PRESS -> "long_press"
            DRAG -> "drag"
            DRAG_RETURN -> "drag-return"
            BOLT -> "bolt"
            LOOP -> "loop"
        }
    }
}

enum class CompassDirection {
    N, NE, E, SE, S, SW, W, NW
}

data class Interaction(
    val type: InteractionType?,
    val direction: CompassDirection?
)

fun resolveInteraction(
    dwells: List<DwellTrack>,
    longPressTriggered: Boolean,
    isSwipe: Boolean,
    touchReleased: Boolean,
    currentTimeMs: Long
): Pair<List<String>, Interaction> {
    if (!isSwipe) {
        if (longPressTriggered) {
            return Pair(listOf("L"), Interaction(InteractionType.LONG_PRESS, null))
        }
        if (touchReleased) {
            return Pair(listOf("C"), Interaction(InteractionType.TAP, null))
        }
        return Pair(emptyList(), Interaction(null, null))
    }

    val rawDirections = mutableListOf<String>()
    // Skip the starting "C" if it exists as the first dwell
    val swipeDwells = if (dwells.firstOrNull()?.sector == "C") dwells.drop(1) else dwells

    for (i in swipeDwells.indices) {
        val dwell = swipeDwells[i]
        if (dwell.sector == "C") {
            rawDirections.add("C")
        } else {
            val duration = (dwell.exitTime ?: currentTimeMs) - dwell.entryTime
            val isLast = (i == swipeDwells.lastIndex)
            if (isLast || duration >= 30) {
                rawDirections.add(dwell.sector)
            }
        }
    }

    if (rawDirections.isEmpty()) {
        return Pair(emptyList(), Interaction(null, null))
    }

    // Check adjacency on compass directions only
    val compassOnly = rawDirections.filter { it != "C" }
    for (i in 0 until compassOnly.size - 1) {
        if (!areAdjacent(compassOnly[i], compassOnly[i + 1])) {
            return Pair(listOf("DISCARDED"), Interaction(null, null))
        }
    }

    val interaction = resolveInteractionFromRaw(rawDirections)
    return Pair(rawDirections, interaction)
}

private fun resolveInteractionFromRaw(raw: List<String>): Interaction {
    // 1. Tap: raw is ["C"]
    if (raw == listOf("C")) {
        return Interaction(InteractionType.TAP, null)
    }

    // 2. Long Press: raw is ["L"]
    if (raw == listOf("L")) {
        return Interaction(InteractionType.LONG_PRESS, null)
    }

    // 3. Drag: exactly 1 compass direction
    if (raw.size == 1) {
        val dir = raw[0].toCompassDirection()
        if (dir != null) {
            return Interaction(InteractionType.DRAG, dir)
        }
    }

    // 4. Drag-return: exactly [D, "C"]
    if (raw.size == 2 && raw[1] == "C") {
        val dir = raw[0].toCompassDirection()
        if (dir != null) {
            return Interaction(InteractionType.DRAG_RETURN, dir)
        }
    }

    // 5. Bolt: exactly [D1, "C", D2] where D1 == D2
    if (raw.size == 3 && raw[1] == "C") {
        val dir1 = raw[0].toCompassDirection()
        val dir2 = raw[2].toCompassDirection()
        if (dir1 != null && dir1 == dir2) {
            return Interaction(InteractionType.BOLT, dir1)
        }
    }

    // 6. Loop: between 3 and 8 adjacent compass directions, ending with "C"
    if (raw.size in 4..9 && raw.last() == "C") {
        val compassDirs = raw.subList(0, raw.size - 1)
        val allAreCompass = compassDirs.all { it.toCompassDirection() != null }
        if (allAreCompass) {
            var adjacent = true
            for (i in 0 until compassDirs.size - 1) {
                if (!areAdjacent(compassDirs[i], compassDirs[i + 1])) {
                    adjacent = false
                    break
                }
            }
            if (adjacent) {
                return Interaction(InteractionType.LOOP, null)
            }
        }
    }

    return Interaction(null, null)
}

fun String.toCompassDirection(): CompassDirection? {
    return try {
        CompassDirection.valueOf(this)
    } catch (e: IllegalArgumentException) {
        null
    }
}

fun getDirectionAngle(direction: CompassDirection): Float {
    return when (direction) {
        CompassDirection.N -> 0f
        CompassDirection.NE -> 45f
        CompassDirection.E -> 90f
        CompassDirection.SE -> 135f
        CompassDirection.S -> 180f
        CompassDirection.SW -> 225f
        CompassDirection.W -> 270f
        CompassDirection.NW -> 315f
    }
}

fun hasLowDwellDiscard(timings: List<DwellTrack>): Boolean {
    val swipeDwells = if (timings.firstOrNull()?.sector == "C") timings.drop(1) else timings
    if (swipeDwells.size <= 1) return false

    for (i in 0 until swipeDwells.lastIndex) {
        val dwell = swipeDwells[i]
        if (dwell.sector != "C") {
            val exitTime = dwell.exitTime
            if (exitTime != null) {
                val duration = exitTime - dwell.entryTime
                if (duration < 30) {
                    return true
                }
            }
        }
    }
    return false
}

fun getDiscardedSquares(lastInteraction: InteractionDescription): List<String> {
    val squareName = lastInteraction.squareName
    if (squareName.length != 2) return emptyList()
    val startX = squareName[0].toString().toIntOrNull() ?: return emptyList()
    val startY = squareName[1].toString().toIntOrNull() ?: return emptyList()

    val swipeDwells = if (lastInteraction.timings.firstOrNull()?.sector == "C") lastInteraction.timings.drop(1) else lastInteraction.timings
    if (swipeDwells.size <= 1) return emptyList()

    val discardedSquares = mutableListOf<String>()
    for (i in 0 until swipeDwells.lastIndex) {
        val dwell = swipeDwells[i]
        if (dwell.sector != "C") {
            val exitTime = dwell.exitTime
            if (exitTime != null) {
                val duration = exitTime - dwell.entryTime
                if (duration < 30) {
                    val target = getTargetSquareForSector(startX, startY, dwell.sector)
                    if (target != null) {
                        discardedSquares.add(target)
                    }
                }
            }
        }
    }
    return discardedSquares
}

fun getTargetSquareForSector(startX: Int, startY: Int, sector: String): String? {
    val dx = when (sector) {
        "NW", "W", "SW" -> -1
        "N", "S" -> 0
        "NE", "E", "SE" -> 1
        else -> 0
    }
    val dy = when (sector) {
        "NW", "N", "NE" -> -1
        "W", "E" -> 0
        "SW", "S", "SE" -> 1
        else -> 0
    }
    val tx = (startX + dx).coerceIn(0, 3)
    val ty = (startY + dy).coerceIn(0, 3)
    return "$tx$ty"
}
