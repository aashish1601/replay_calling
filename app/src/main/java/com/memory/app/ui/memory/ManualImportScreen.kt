package com.memory.app.ui.memory

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.FileOpen
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManualImportScreen(
    viewModel: RecordingsViewModel,
    onBackClick: () -> Unit
) {
    val matchSuggestion by viewModel.matchSuggestion.collectAsState()
    val scanResult by viewModel.scanResult.collectAsState()

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.importFromUri(uri)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Import Recording") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (matchSuggestion != null) {
                val suggestion = matchSuggestion!!
                Text(
                    text = "Possible Call Match",
                    style = MaterialTheme.typography.titleLarge
                )
                Spacer(modifier = Modifier.height(16.dp))
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Call ID: ${suggestion.callId}")
                        Text("Confidence: ${(suggestion.confidence * 100).toInt()}%")
                        Spacer(modifier = Modifier.height(8.dp))
                        suggestion.reasons.forEach { reason ->
                            Text("- $reason", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Button(onClick = {
                        viewModel.associateRecordingToCall(suggestion.recordingId, suggestion.callId, suggestion.confidence)
                        viewModel.clearMatchSuggestion()
                        onBackClick()
                    }) {
                        Text("Associate")
                    }
                    OutlinedButton(onClick = {
                        viewModel.clearMatchSuggestion()
                        onBackClick()
                    }) {
                        Text("Keep unassociated")
                    }
                }
            } else {
                Icon(
                    imageVector = Icons.Rounded.FileOpen,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "Import a Call Recording",
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Select an audio file (.m4a, .mp3, .wav, etc.) from your device storage to import it into Memory.",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(32.dp))
                Button(
                    onClick = {
                        viewModel.clearMatchSuggestion()
                        launcher.launch(arrayOf("audio/*"))
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp)
                ) {
                    Text("Select Audio File")
                }

                scanResult?.let {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}
