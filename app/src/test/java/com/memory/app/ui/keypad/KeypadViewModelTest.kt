package com.memory.app.ui.keypad

import android.content.ContextWrapper
import com.memory.app.model.RecordingMode
import com.memory.app.network.ReplayApi
import com.memory.app.network.StartRecordingRequest
import com.memory.app.network.StartRecordingResponse
import com.memory.app.network.StopRecordingRequest
import com.memory.app.network.StopRecordingResponse
import com.memory.app.repository.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import retrofit2.Response

class KeypadViewModelTest {

    private lateinit var viewModel: KeypadViewModel
    private lateinit var fakeReplayApi: FakeReplayApi
    private lateinit var fakeSettingsRepository: FakeSettingsRepository

    private class FakeSettingsRepository : SettingsRepository(
        context = ContextWrapper(null)
    ) {
        val modeFlow = MutableStateFlow(RecordingMode.NATIVE)
        val phoneFlow = MutableStateFlow("+15550000")
        override val recordingMode: StateFlow<RecordingMode> = modeFlow
        override val userPhone: StateFlow<String> = phoneFlow
    }

    private class FakeReplayApi : ReplayApi {
        override suspend fun startRecording(request: StartRecordingRequest): Response<StartRecordingResponse> {
            return Response.success(StartRecordingResponse(true, "sid_1", "Ok"))
        }

        override suspend fun stopRecording(request: StopRecordingRequest): Response<StopRecordingResponse> {
            return Response.success(StopRecordingResponse(true, "Ok"))
        }
    }

    @Before
    fun setUp() {
        fakeReplayApi = FakeReplayApi()
        fakeSettingsRepository = FakeSettingsRepository()
        viewModel = KeypadViewModel(fakeReplayApi, fakeSettingsRepository)
    }

    @Test
    fun appendDigit_updatesPhoneNumber() {
        viewModel.appendDigit("1")
        viewModel.appendDigit("2")
        viewModel.appendDigit("3")

        assertEquals("123", viewModel.phoneNumber.value)
    }

    @Test
    fun deleteLastDigit_removesLastCharacter() {
        viewModel.appendDigit("5")
        viewModel.appendDigit("5")
        viewModel.appendDigit("5")
        viewModel.deleteLastDigit()

        assertEquals("55", viewModel.phoneNumber.value)
    }

    @Test
    fun clear_resetsPhoneNumber() {
        viewModel.appendDigit("9")
        viewModel.appendDigit("1")
        viewModel.appendDigit("1")
        viewModel.clear()

        assertEquals("", viewModel.phoneNumber.value)
    }
}
