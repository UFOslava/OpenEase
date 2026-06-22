package com.openease.keyboard.ui.main

import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.view.inputmethod.InputMethodManager
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.navigation3.runtime.NavKey
import com.openease.keyboard.theme.OpenEaseTheme

@Composable
fun MainScreen(
    onItemClick: (NavKey) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var isEnabled by remember { mutableStateOf(false) }
    var isSelected by remember { mutableStateOf(false) }

    // Helper function to update state
    fun updateKeyboardState() {
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        val enabledIMEs = imm.enabledInputMethodList
        isEnabled = enabledIMEs.any { it.packageName == context.packageName }

        val currentIME = Settings.Secure.getString(context.contentResolver, Settings.Secure.DEFAULT_INPUT_METHOD)
        isSelected = currentIME?.startsWith(context.packageName) == true
    }

    // Refresh state when activity resumes
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
        color = Color(0xFF0F0F13) // Dark slate background
    ) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            Spacer(modifier = Modifier.height(40.dp))

            // App Title with Gradient
            Text(
                text = "OpenEase",
                style = androidx.compose.ui.text.TextStyle(
                    fontSize = 40.sp,
                    fontWeight = FontWeight.ExtraBold,
                    brush = Brush.horizontalGradient(
                        colors = listOf(Color(0xFF8A2387), Color(0xFFE94057), Color(0xFFF27121))
                    )
                )
            )

            Text(
                text = "Keyboard Setup",
                fontSize = 16.sp,
                color = Color(0xFF9E9EAF),
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(top = 8.dp)
            )

            Spacer(modifier = Modifier.height(48.dp))

            // Step 1: Enable Keyboard
            SetupStepCard(
                stepNumber = "1",
                title = "Enable Keyboard",
                description = "Turn on OpenEase Keyboard in System settings.",
                isCompleted = isEnabled,
                actionLabel = "Enable",
                onActionClick = {
                    val intent = Intent(Settings.ACTION_INPUT_METHOD_SETTINGS).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Step 2: Choose/Select Keyboard
            SetupStepCard(
                stepNumber = "2",
                title = "Select Keyboard",
                description = "Choose OpenEase Keyboard as your active input method.",
                isCompleted = isSelected,
                actionLabel = "Choose",
                enabled = isEnabled,
                onActionClick = {
                    val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                    imm.showInputMethodPicker()
                }
            )

            Spacer(modifier = Modifier.weight(1f))

            if (isEnabled && isSelected) {
                Text(
                    text = "🎉 OpenEase Keyboard is active!",
                    color = Color(0xFF4BB543),
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    modifier = Modifier.padding(bottom = 24.dp)
                )
            }
        }
    }
}

@Composable
fun SetupStepCard(
    stepNumber: String,
    title: String,
    description: String,
    isCompleted: Boolean,
    actionLabel: String,
    onActionClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isCompleted) Color(0xFF1E2922) else Color(0xFF1E1E24)
        )
    ) {
        Row(
            modifier = Modifier
                .padding(20.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Step Number or Check Icon
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isCompleted) Color(0xFF4BB543) else Color(0xFF2E2F3E)),
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
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Title & Description
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Text(
                    text = description,
                    color = Color(0xFF9E9EAF),
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 4.dp),
                    lineHeight = 16.sp
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Action Button
            if (!isCompleted) {
                Button(
                    onClick = onActionClick,
                    enabled = enabled,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF6C5DD3),
                        contentColor = Color.White,
                        disabledContainerColor = Color(0xFF2E2F3E),
                        disabledContentColor = Color(0xFF9E9EAF)
                    )
                ) {
                    Text(
                        text = actionLabel,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}
