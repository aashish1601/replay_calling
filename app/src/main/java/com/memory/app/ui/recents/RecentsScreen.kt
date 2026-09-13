package com.memory.app.ui.recents

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.CallMade
import androidx.compose.material.icons.automirrored.rounded.CallMissed
import androidx.compose.material.icons.automirrored.rounded.CallReceived
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.History
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.memory.app.db.CallEntity
import com.memory.app.util.CallUtils

@Composable
fun RecentsScreen(
    viewModel: RecentsViewModel,
    onCallDetailClick: (Long) -> Unit
) {
    val calls by viewModel.calls.collectAsStateWithLifecycle()
    val context = LocalContext.current

    if (calls.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(24.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                    modifier = Modifier.size(80.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Rounded.History,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(40.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "No Recent Calls",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Calls you make or receive will appear here.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            items(calls, key = { it.id }) { call ->
                RecentCallItem(
                    call = call,
                    onItemClick = { onCallDetailClick(call.id) },
                    onCallClick = { CallUtils.launchCall(context, call.phoneNumber) }
                )
            }
        }
    }
}

@Composable
fun RecentCallItem(
    call: CallEntity,
    onItemClick: () -> Unit,
    onCallClick: () -> Unit
) {
    val isMissed = call.direction == "MISSED" || call.state == "MISSED"
    val isIncoming = call.direction == "INCOMING"
    
    val directionIcon = when {
        isMissed -> Icons.AutoMirrored.Rounded.CallMissed
        isIncoming -> Icons.AutoMirrored.Rounded.CallReceived
        else -> Icons.AutoMirrored.Rounded.CallMade
    }
    
    val directionColor = when {
        isMissed -> MaterialTheme.colorScheme.error
        isIncoming -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.secondary
    }

    val displayName = if (!call.contactName.isNullOrBlank()) call.contactName else call.phoneNumber

    ListItem(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onItemClick),
        leadingContent = {
            Surface(
                shape = CircleShape,
                color = directionColor.copy(alpha = 0.12f),
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = directionIcon,
                        contentDescription = null,
                        tint = directionColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        },
        headlineContent = {
            Text(
                text = displayName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = if (isMissed) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
            )
        },
        supportingContent = {
            Column {
                if (!call.contactName.isNullOrBlank()) {
                    Text(
                        text = call.phoneNumber,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val directionText = when {
                        isMissed -> "Missed call"
                        isIncoming -> "Incoming"
                        else -> "Outgoing"
                    }
                    val durationText = CallUtils.formatDuration(call.duration)
                    val fullSub = if (isMissed) directionText else "$directionText • $durationText"
                    
                    Text(
                        text = fullSub,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = CallUtils.formatTimestamp(call.startedAt),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        },
        trailingContent = {
            IconButton(onClick = onCallClick) {
                Icon(
                    imageVector = Icons.Rounded.Call,
                    contentDescription = "Call ${call.phoneNumber}",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    )
}
