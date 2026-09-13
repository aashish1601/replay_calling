package com.memory.app.ui.memory

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.memory.app.db.CallDao
import com.memory.app.db.CallEntity
import com.memory.app.db.RecordingEntity
import com.memory.app.recording.RecordingCapability
import com.memory.app.recording.RecordingCapabilityManager
import com.memory.app.recording.RecordingDiscoveryWorker
import com.memory.app.recording.RecordingImporter
import com.memory.app.recording.RecordingMatch
import com.memory.app.recording.RecordingMatcher
import com.memory.app.recording.RecordingRepository
import com.memory.app.recording.RecordingScanner
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RecordingsViewModel @Inject constructor(
    private val recordingRepository: RecordingRepository,
    private val recordingScanner: RecordingScanner,
    private val recordingMatcher: RecordingMatcher,
    private val recordingImporter: RecordingImporter,
    private val capabilityManager: RecordingCapabilityManager,
    private val callDao: CallDao,
    @ApplicationContext private val context: Context
) : ViewModel() {

    val recordings: StateFlow<List<RecordingEntity>> =
        recordingRepository.observeAll().stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _scanResult = MutableStateFlow<String?>(null)
    val scanResult: StateFlow<String?> = _scanResult.asStateFlow()

    private val _capability = MutableStateFlow<RecordingCapability?>(null)
    val capability: StateFlow<RecordingCapability?> = _capability.asStateFlow()

    private val _matchSuggestion = MutableStateFlow<RecordingMatch?>(null)
    val matchSuggestion: StateFlow<RecordingMatch?> = _matchSuggestion.asStateFlow()

    init {
        refreshCapabilities()
    }

    fun refreshCapabilities() {
        viewModelScope.launch {
            _capability.value = capabilityManager.detectCapabilities()
        }
    }

    fun triggerScan() {
        viewModelScope.launch {
            _isScanning.value = true
            _scanResult.value = null
            try {
                val discovered = recordingScanner.scanAll()
                var newCount = 0
                var matchedCount = 0

                for (recording in discovered) {
                    val existing = recordingRepository.getByUri(recording.uri.toString())
                    if (existing != null) continue

                    val recordingId = recordingRepository.importDiscovered(recording)
                    newCount++

                    val recentCalls = callDao.getRecentCalls(
                        since = System.currentTimeMillis() - 7 * 24 * 60 * 60 * 1000
                    )
                    val entity = recordingRepository.getById(recordingId) ?: continue
                    val bestMatch = recordingMatcher.findBestMatch(entity, recentCalls)

                    if (bestMatch != null && bestMatch.confidence >= RecordingMatcher.AUTO_MATCH_THRESHOLD) {
                        recordingRepository.update(
                            entity.copy(
                                associatedCallId = bestMatch.callId,
                                matchConfidence = bestMatch.confidence,
                                processingStatus = "PROCESSED"
                            )
                        )
                        matchedCount++
                    }
                }

                _scanResult.value = "Found $newCount new recording(s), " +
                        "$matchedCount auto-matched to calls."
            } catch (e: Exception) {
                _scanResult.value = "Scan failed: ${e.message}"
            } finally {
                _isScanning.value = false
            }
        }
    }

    fun scheduleBackgroundScan() {
        RecordingDiscoveryWorker.schedule(context, delaySeconds = 5)
    }

    suspend fun getRecordingById(id: Long): RecordingEntity? {
        return recordingRepository.getById(id)
    }

    suspend fun getCallById(id: Long): CallEntity? {
        return callDao.getCallById(id)
    }

    suspend fun getRecordingsForCall(callId: Long): List<RecordingEntity> {
        return recordingRepository.getForCall(callId)
    }

    suspend fun getRecentCalls(): List<CallEntity> {
        return callDao.getRecentCalls(
            since = System.currentTimeMillis() - 30L * 24 * 60 * 60 * 1000
        )
    }

    fun importFromUri(uri: Uri) {
        viewModelScope.launch {
            try {
                val id = recordingImporter.importFromUri(uri)
                val entity = recordingRepository.getById(id) ?: return@launch

                // Attempt matching
                val recentCalls = callDao.getRecentCalls(
                    since = System.currentTimeMillis() - 7 * 24 * 60 * 60 * 1000
                )
                val bestMatch = recordingMatcher.findBestMatch(entity, recentCalls)
                if (bestMatch != null) {
                    _matchSuggestion.value = bestMatch
                    if (bestMatch.confidence >= RecordingMatcher.AUTO_MATCH_THRESHOLD) {
                        associateRecordingToCall(entity.id, bestMatch.callId, bestMatch.confidence)
                    }
                }
            } catch (e: Exception) {
                _scanResult.value = "Import failed: ${e.message}"
            }
        }
    }

    fun associateRecordingToCall(recordingId: Long, callId: Long, confidence: Float? = null) {
        viewModelScope.launch {
            val entity = recordingRepository.getById(recordingId) ?: return@launch
            recordingRepository.update(
                entity.copy(
                    associatedCallId = callId,
                    matchConfidence = confidence ?: 1.0f,
                    processingStatus = "PROCESSED"
                )
            )
        }
    }

    fun removeRecording(recordingId: Long) {
        viewModelScope.launch {
            recordingRepository.delete(recordingId)
        }
    }

    fun clearMatchSuggestion() {
        _matchSuggestion.value = null
    }
}
