package com.memory.app.ui.incall

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeOff
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.CallEnd
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Dialpad
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.MicOff
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.VolumeOff
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.memory.app.model.CallDirection
import com.memory.app.model.CallSession
import com.memory.app.model.CallState

@Composable
fun InCallScreen(
    viewModel: InCallViewModel,
    modifier: Modifier = Modifier
) {
    val currentCall by viewModel.currentCall.collectAsState()
    val formattedDuration by viewModel.formattedDuration.collectAsState()
    val contactName by viewModel.displayContactName.collectAsState()
    val phoneNumber by viewModel.displayPhoneNumber.collectAsState()
    val isMuted by viewModel.isMuted.collectAsState()
    val isSpeakerOn by viewModel.isSpeakerOn.collectAsState()
    val isKeypadOpen by viewModel.isKeypadOpen.collectAsState()

    val session = currentCall ?: return

    InCallScreenContent(
        session = session,
        contactName = contactName,
        phoneNumber = phoneNumber,
        formattedDuration = formattedDuration,
        isMuted = isMuted,
        isSpeakerOn = isSpeakerOn,
        isKeypadOpen = isKeypadOpen,
        onAnswer = { record -> viewModel.answerCall(record) },
        onReject = viewModel::rejectCall,
        onHangUp = viewModel::hangUpCall,
        onToggleMute = viewModel::toggleMute,
        onToggleSpeaker = viewModel::toggleSpeaker,
        onToggleKeypad = viewModel::toggleKeypad,
        onCloseKeypad = viewModel::closeKeypad,
        onDtmfKeyPress = viewModel::onDtmfKeyPress,
        onDtmfKeyRelease = viewModel::onDtmfKeyRelease,
        modifier = modifier
    )

    val context = LocalContext.current
    LaunchedEffect(Unit) {
        viewModel.uiEvent.collect { event ->
            android.widget.Toast.makeText(context, event, android.widget.Toast.LENGTH_LONG).show()
        }
    }
}

@Composable
fun InCallScreenContent(
    session: CallSession,
    contactName: String,
    phoneNumber: String,
    formattedDuration: String,
    isMuted: Boolean,
    isSpeakerOn: Boolean,
    isKeypadOpen: Boolean,
    onAnswer: (Boolean) -> Unit,
    onReject: () -> Unit,
    onHangUp: () -> Unit,
    onToggleMute: () -> Unit,
    onToggleSpeaker: () -> Unit,
    onToggleKeypad: () -> Unit,
    onCloseKeypad: () -> Unit,
    onDtmfKeyPress: (Char) -> Unit,
    onDtmfKeyRelease: () -> Unit,
    modifier: Modifier = Modifier
) {
    val topPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val bottomPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    val isIncomingRinging = session.direction == CallDirection.INCOMING && session.state == CallState.RINGING

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = topPadding, bottom = bottomPadding)
        ) {
            if (isIncomingRinging) {
                IncomingCallContent(
                    contactName = contactName,
                    phoneNumber = phoneNumber,
                    onAnswer = onAnswer,
                    onReject = onReject,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                OngoingCallContent(
                    contactName = contactName,
                    phoneNumber = phoneNumber,
                    formattedDuration = formattedDuration,
                    isMuted = isMuted,
                    isSpeakerOn = isSpeakerOn,
                    isKeypadOpen = isKeypadOpen,
                    onHangUp = onHangUp,
                    onToggleMute = onToggleMute,
                    onToggleSpeaker = onToggleSpeaker,
                    onToggleKeypad = onToggleKeypad,
                    modifier = Modifier.fillMaxSize()
                )
            }

            AnimatedVisibility(
                visible = isKeypadOpen && !isIncomingRinging,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                modifier = Modifier.fillMaxSize()
            ) {
                InCallKeypadOverlay(
                    onClose = onCloseKeypad,
                    onKeyPress = onDtmfKeyPress,
                    onKeyRelease = onDtmfKeyRelease,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

@Composable
fun IncomingCallContent(
    contactName: String,
    phoneNumber: String,
    onAnswer: (Boolean) -> Unit,
    onReject: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Spacer(modifier = Modifier.height(32.dp))

        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Large Avatar
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Person,
                    contentDescription = "Contact Avatar",
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = contactName,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onBackground
            )

            if (phoneNumber.isNotEmpty() && phoneNumber != contactName) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = phoneNumber,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Incoming Call...",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Medium
            )
        }

        // Action Buttons Row (Decline, Answer, Answer + Record)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 24.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Decline (Reject)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                FloatingActionButton(
                    onClick = onReject,
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError,
                    shape = CircleShape,
                    modifier = Modifier
                        .size(64.dp)
                        .semantics { contentDescription = "Decline call" },
                    elevation = FloatingActionButtonDefaults.elevation(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.CallEnd,
                        contentDescription = null,
                        modifier = Modifier.size(32.dp)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Decline",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            // Answer Normally
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                FloatingActionButton(
                    onClick = { onAnswer(false) },
                    containerColor = Color(0xFF1976D2), // Blue for normal answer
                    contentColor = Color.White,
                    shape = CircleShape,
                    modifier = Modifier
                        .size(64.dp)
                        .semantics { contentDescription = "Answer call" },
                    elevation = FloatingActionButtonDefaults.elevation(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Call,
                        contentDescription = null,
                        modifier = Modifier.size(32.dp)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Answer",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            // Answer + Record
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                FloatingActionButton(
                    onClick = { onAnswer(true) },
                    containerColor = Color(0xFF2E7D32), // Green for Answer + Record
                    contentColor = Color.White,
                    shape = CircleShape,
                    modifier = Modifier
                        .size(64.dp)
                        .semantics { contentDescription = "Answer and Record call" },
                    elevation = FloatingActionButtonDefaults.elevation(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Mic,
                        contentDescription = null,
                        modifier = Modifier.size(32.dp)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Record",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
        }
    }
}

@Composable
fun OngoingCallContent(
    contactName: String,
    phoneNumber: String,
    formattedDuration: String,
    isMuted: Boolean,
    isSpeakerOn: Boolean,
    isKeypadOpen: Boolean,
    onHangUp: () -> Unit,
    onToggleMute: () -> Unit,
    onToggleSpeaker: () -> Unit,
    onToggleKeypad: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Spacer(modifier = Modifier.height(24.dp))

        // Contact details and status
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.secondaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Person,
                    contentDescription = "Contact Avatar",
                    modifier = Modifier.size(52.dp),
                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = contactName,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onBackground
            )

            if (phoneNumber.isNotEmpty() && phoneNumber != contactName) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = phoneNumber,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Timer / Status Text
            Text(
                text = formattedDuration,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
        }

        // Controls Grid & Hang Up
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Control buttons row: Mute, Speaker, Keypad
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                CallControlButton(
                    icon = if (isMuted) Icons.Rounded.MicOff else Icons.Rounded.Mic,
                    label = if (isMuted) "Muted" else "Mute",
                    isActive = isMuted,
                    onClick = onToggleMute,
                    contentDescription = if (isMuted) "Unmute call" else "Mute call"
                )

                CallControlButton(
                    icon = if (isSpeakerOn) Icons.AutoMirrored.Rounded.VolumeUp else Icons.AutoMirrored.Rounded.VolumeOff,
                    label = "Speaker",
                    isActive = isSpeakerOn,
                    onClick = onToggleSpeaker,
                    contentDescription = if (isSpeakerOn) "Switch to earpiece" else "Switch to speaker"
                )

                CallControlButton(
                    icon = Icons.Rounded.Dialpad,
                    label = "Keypad",
                    isActive = isKeypadOpen,
                    onClick = onToggleKeypad,
                    contentDescription = "Show keypad overlay"
                )
            }

            Spacer(modifier = Modifier.height(40.dp))

            // Hang Up button
            FloatingActionButton(
                onClick = onHangUp,
                containerColor = MaterialTheme.colorScheme.error,
                contentColor = MaterialTheme.colorScheme.onError,
                shape = CircleShape,
                modifier = Modifier
                    .size(72.dp)
                    .semantics { contentDescription = "Hang up call" },
                elevation = FloatingActionButtonDefaults.elevation(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.CallEnd,
                    contentDescription = null,
                    modifier = Modifier.size(36.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun CallControlButton(
    icon: ImageVector,
    label: String,
    isActive: Boolean,
    onClick: () -> Unit,
    contentDescription: String,
    modifier: Modifier = Modifier
) {
    val containerColor = if (isActive) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }

    val contentColor = if (isActive) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        IconButton(
            onClick = onClick,
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(containerColor)
                .semantics { this.contentDescription = contentDescription }
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(30.dp)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}

@Composable
fun InCallKeypadOverlay(
    onClose: () -> Unit,
    onKeyPress: (Char) -> Unit,
    onKeyRelease: () -> Unit,
    modifier: Modifier = Modifier
) {
    var dtmfSequence by remember { mutableStateOf("") }

    Box(
        modifier = modifier
            .background(MaterialTheme.colorScheme.background.copy(alpha = 0.96f))
            .padding(24.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header with Close button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Keypad",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                IconButton(
                    onClick = onClose,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Close keypad",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Typed DTMF Sequence Display
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = dtmfSequence,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center
                )
            }

            // Keypad Grid (1-9, *, 0, #)
            val keypadGrid = listOf(
                listOf("1" to "", "2" to "ABC", "3" to "DEF"),
                listOf("4" to "GHI", "5" to "JKL", "6" to "MNO"),
                listOf("7" to "PQRS", "8" to "TUV", "9" to "WXYZ"),
                listOf("*" to "", "0" to "+", "#" to "")
            )

            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                keypadGrid.forEach { row ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(24.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        row.forEach { (number, letters) ->
                            KeypadButton(
                                number = number,
                                letters = letters,
                                onPress = {
                                    dtmfSequence += number
                                    onKeyPress(number[0])
                                },
                                onRelease = {
                                    onKeyRelease()
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun KeypadButton(
    number: String,
    letters: String,
    onPress: () -> Unit,
    onRelease: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }

    LaunchedEffect(interactionSource) {
        interactionSource.interactions.collect { interaction ->
            when (interaction) {
                is PressInteraction.Press -> onPress()
                is PressInteraction.Release, is PressInteraction.Cancel -> onRelease()
            }
        }
    }

    Card(
        modifier = Modifier
            .size(72.dp)
            .clip(CircleShape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = {}
            ),
        shape = CircleShape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = number,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (letters.isNotEmpty()) {
                Text(
                    text = letters,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    fontSize = 9.sp
                )
            }
        }
    }
}

@Preview(showBackground = true, name = "Incoming Call Light")
@Composable
fun IncomingCallPreview() {
    MaterialTheme {
        IncomingCallContent(
            contactName = "John Doe",
            phoneNumber = "+1 555-0199",
            onAnswer = {},
            onReject = {}
        )
    }
}

@Preview(showBackground = true, name = "Ongoing Call Light")
@Composable
fun OngoingCallPreview() {
    MaterialTheme {
        OngoingCallContent(
            contactName = "Jane Smith",
            phoneNumber = "+1 555-0123",
            formattedDuration = "00:04:32",
            isMuted = false,
            isSpeakerOn = true,
            isKeypadOpen = false,
            onHangUp = {},
            onToggleMute = {},
            onToggleSpeaker = {},
            onToggleKeypad = {}
        )
    }
}

@Preview(showBackground = true, name = "Keypad Overlay Light")
@Composable
fun KeypadOverlayPreview() {
    MaterialTheme {
        InCallKeypadOverlay(
            onClose = {},
            onKeyPress = {},
            onKeyRelease = {}
        )
    }
}
