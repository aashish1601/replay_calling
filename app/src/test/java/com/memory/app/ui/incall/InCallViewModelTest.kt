package com.memory.app.ui.incall

import android.content.ContextWrapper
import com.memory.app.model.CallDirection
import com.memory.app.model.CallSession
import com.memory.app.model.CallState
import com.memory.app.model.RecordingMode
import com.memory.app.network.ReplayApi
import com.memory.app.network.StartRecordingRequest
import com.memory.app.network.StartRecordingResponse
import com.memory.app.network.StopRecordingRequest
import com.memory.app.network.StopRecordingResponse
import com.memory.app.repository.Contact
import com.memory.app.repository.ContactsRepository
import com.memory.app.repository.SettingsRepository
import com.memory.app.telecom.CallStateManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Response

@OptIn(ExperimentalCoroutinesApi::class)
class InCallViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var callStateManager: CallStateManager
    private lateinit var fakeContactsRepository: FakeContactsRepository
    private lateinit var fakeSettingsRepository: FakeSettingsRepository
    private lateinit var fakeReplayApi: FakeReplayApi
    private lateinit var viewModel: InCallViewModel

    private class FakeContactsRepository : ContactsRepository(
        context = ContextWrapper(null)
    ) {
        var contactsToReturn: List<Contact> = emptyList()
        override suspend fun getContacts(): List<Contact> = contactsToReturn
    }

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
        Dispatchers.setMain(testDispatcher)
        callStateManager = CallStateManager()
        fakeContactsRepository = FakeContactsRepository()
        fakeSettingsRepository = FakeSettingsRepository()
        fakeReplayApi = FakeReplayApi()
        viewModel = InCallViewModel(
            callStateManager,
            fakeContactsRepository,
            fakeSettingsRepository,
            fakeReplayApi
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun formattedDuration_showsDialingText_whenDialing() {
        val session = CallSession(
            id = "call_1",
            phoneNumber = "123",
            contactName = "Bob",
            direction = CallDirection.OUTGOING,
            state = CallState.DIALING,
            startedAt = System.currentTimeMillis()
        )
        callStateManager.addOrUpdateCall(session)

        assertEquals("Dialing...", viewModel.formattedDuration.value)
    }

    @Test
    fun formattedDuration_showsRingingText_whenRinging() {
        val session = CallSession(
            id = "call_1",
            phoneNumber = "123",
            contactName = "Bob",
            direction = CallDirection.INCOMING,
            state = CallState.RINGING,
            startedAt = System.currentTimeMillis()
        )
        callStateManager.addOrUpdateCall(session)

        assertEquals("Incoming Call...", viewModel.formattedDuration.value)
    }

    @Test
    fun displayContactName_usesSessionContactName_ifPresent() {
        val session = CallSession(
            id = "call_1",
            phoneNumber = "+15551234",
            contactName = "Charlie",
            direction = CallDirection.INCOMING,
            state = CallState.RINGING,
            startedAt = System.currentTimeMillis()
        )
        callStateManager.addOrUpdateCall(session)

        assertEquals("Charlie", viewModel.displayContactName.value)
    }

    @Test
    fun userActions_toggleMuteSpeakerKeypad() {
        assertFalse(viewModel.isMuted.value)
        viewModel.toggleMute()
        assertTrue(viewModel.isMuted.value)

        assertFalse(viewModel.isSpeakerOn.value)
        viewModel.toggleSpeaker()
        assertTrue(viewModel.isSpeakerOn.value)

        assertFalse(viewModel.isKeypadOpen.value)
        viewModel.toggleKeypad()
        assertTrue(viewModel.isKeypadOpen.value)
        viewModel.closeKeypad()
        assertFalse(viewModel.isKeypadOpen.value)
    }

    @Test
    fun answerAndReject_delegateToCallStateManager() {
        val session = CallSession(
            id = "call_1",
            phoneNumber = "123",
            contactName = "Dave",
            direction = CallDirection.INCOMING,
            state = CallState.RINGING,
            startedAt = System.currentTimeMillis()
        )
        callStateManager.addOrUpdateCall(session)

        viewModel.answerCall(record = false)
        assertEquals(CallState.ACTIVE, callStateManager.currentCall.value?.state)

        viewModel.hangUpCall()
        assertEquals(null, callStateManager.currentCall.value)
    }
}
