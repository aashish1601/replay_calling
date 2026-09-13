package com.memory.app.ui.recents

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.memory.app.db.CallEntity
import com.memory.app.repository.CallRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class RecentsViewModel @Inject constructor(
    private val callRepository: CallRepository
) : ViewModel() {

    val calls: StateFlow<List<CallEntity>> = callRepository.getAllCalls()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    suspend fun getCallDetail(callId: Long): CallEntity? {
        return callRepository.getCallById(callId)
    }
}
