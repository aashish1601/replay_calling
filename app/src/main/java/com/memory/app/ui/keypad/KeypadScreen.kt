package com.memory.app.ui.keypad

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Backspace
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.material.icons.rounded.Mic
import android.widget.Toast
import com.memory.app.util.CallUtils

data class KeypadButtonData(
    val digit: String,
    val letters: String
)

val keypadButtons = listOf(
    KeypadButtonData("1", ""),
    KeypadButtonData("2", "ABC"),
    KeypadButtonData("3", "DEF"),
    KeypadButtonData("4", "GHI"),
    KeypadButtonData("5", "JKL"),
    KeypadButtonData("6", "MNO"),
    KeypadButtonData("7", "PQRS"),
    KeypadButtonData("8", "TUV"),
    KeypadButtonData("9", "WXYZ"),
    KeypadButtonData("*", ""),
    KeypadButtonData("0", "+"),
    KeypadButtonData("#", "")
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun KeypadButton(
    digit: String,
    subtext: String,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null
) {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier
            .size(72.dp)
            .clip(CircleShape)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            Text(
                text = digit,
                style = MaterialTheme.typography.titleLarge,
                fontSize = 26.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (subtext.isNotEmpty()) {
                Text(
                    text = subtext,
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun KeypadScreen(
    viewModel: KeypadViewModel
) {
    val phoneNumber by viewModel.phoneNumber.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.uiEvent.collect { event ->
            when (event) {
                is KeypadUiEvent.ShowToast -> {
                    Toast.makeText(context, event.message, Toast.LENGTH_LONG).show()
                }

                is KeypadUiEvent.LaunchNativeCall -> {
                    CallUtils.launchCall(context, event.phoneNumber)
                }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Display area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = phoneNumber,
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Keypad grid
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                for (row in 0..3) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        for (col in 0..2) {
                            val index = row * 3 + col
                            val item = keypadButtons[index]
                            val onLongClickAction: (() -> Unit)? = if (item.digit == "0") {
                                { viewModel.appendDigit("+") }
                            } else null
                            KeypadButton(
                                digit = item.digit,
                                subtext = item.letters,
                                onClick = { viewModel.appendDigit(item.digit) },
                                onLongClick = onLongClickAction
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Action row: Call, Delete
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Spacer(modifier = Modifier.weight(0.2f))

                    // Call Button
                    Button(
                        onClick = {
                            if (phoneNumber.isNotBlank()) {
                                viewModel.startCall(phoneNumber)
                            }
                        },
                        modifier = Modifier.weight(1f).height(64.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(Icons.Rounded.Call, contentDescription = "Place call")
                        Spacer(Modifier.width(8.dp))
                        Text("Call", style = MaterialTheme.typography.titleMedium)
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    // Delete Button
                    if (phoneNumber.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .weight(0.2f)
                                .size(56.dp)
                                .clip(CircleShape)
                                .combinedClickable(
                                    onClick = { viewModel.deleteLastDigit() },
                                    onLongClick = { viewModel.clear() }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.Backspace,
                                contentDescription = "Delete digit",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        Spacer(modifier = Modifier.weight(0.2f))
                    }
                }
            }
        }
    }
}
