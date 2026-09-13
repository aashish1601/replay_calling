package com.memory.app.ui.incall

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.memory.app.model.CallState

@Composable
fun InCallOverlay(
    modifier: Modifier = Modifier,
    viewModel: InCallViewModel = hiltViewModel()
) {
    val currentCall by viewModel.currentCall.collectAsState()

    val call = currentCall
    if (call != null &&
        call.state != CallState.DISCONNECTED &&
        call.state != CallState.UNKNOWN
    ) {
        InCallScreen(
            viewModel = viewModel,
            modifier = modifier
        )
    }
}
