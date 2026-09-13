# Project Plan

Build the first working version of an Android application called Memory. It is a personal AI memory application, but in this phase, ONLY implement a fully working Android Phone/Dialer foundation where Memory becomes the default Phone app and makes/receives normal cellular calls using the device's SIM/carrier. Use Android Telecom APIs, InCallService, RoleManager. Features include Onboarding, Default Dialer Request, Recents, Contacts, Dial Pad, Outgoing/Incoming/Ongoing call UIs, Call history persistence via Room, Settings, and a Memory tab placeholder. Do NOT implement call recording, AI, transcription, or VoIP in this phase. Tech stack: Kotlin, Jetpack Compose, Material 3, MVVM, Clean Architecture, Hilt, Room. Package name: com.memory.app.

## Project Brief

# Project Brief: Memory (MVP Phase 1 - Default Dialer)

## Features
1. **Default Dialer Onboarding & Role Management:** Seamless user onboarding flow to request and assume the default Phone app role using Android's `RoleManager`.
2. **Cellular Call Management:** Robust handling of incoming, outgoing, and ongoing cellular calls (via SIM/carrier) utilizing Android Telecom APIs and `InCallService`.
3. **Dial Pad & Core Call UIs:** Functional dial pad and dedicated, responsive user interfaces for dialing, ringing, and active call states.
4. **Recents & Contacts Integration:** Access to the device's contacts list for easy dialing and a functional "Recents" tab to track call history.
5. **Foundation UI (Settings & Placeholder):** Basic settings screen and a placeholder "Memory" tab to set up the foundation for future AI integrations.

## High-Level Tech Stack
* **Language & UI Toolkit:** Kotlin, Jetpack Compose, Material 3
* **Navigation & Adaptive Strategy:** Jetpack Navigation 3 (state-driven) and Compose Material Adaptive library (used for all UI layouts)
* **Architecture & Concurrency:** MVVM (Model-View-ViewModel), Clean Architecture, Kotlin Coroutines
* **Dependency Injection:** Dagger Hilt
* **Telephony Core:** Android Telecom API, `InCallService`, `RoleManager`
* **Persistence:** Room Database (Explicitly required for persisting call history)

## Implementation Steps
**Total Duration:** 25h 10m 56s

### Task_1_FoundationAndTelecom: Set up Hilt, Room DB (Call History), and AndroidManifest (permissions, InCallService). Implement Onboarding UI and RoleManager logic to request Default Dialer role.
- **Status:** COMPLETED
- **Updates:** Completed Foundation and Telecom setup. Hilt and Room configured. Manifest updated with required permissions and InCallService. Onboarding UI with RoleManager logic for Default Dialer role implemented. CallEntity and RecordingProvider placeholder added.
- **Acceptance Criteria:**
  - Hilt and Room configured
  - Manifest includes Telecom and Contacts permissions
  - RoleManager successfully prompts for Default Dialer role
- **Duration:** 30h 39m 23s

### Task_2_CallServiceAndData: Implement InCallService to manage telecom call states (ringing, active, disconnected). Create repositories for Contacts, Room Call History, and Telecom API wrapping.
- **Status:** COMPLETED
- **Updates:** Completed InCallService implementation and data layer. Implemented CallStateManager to expose StateFlow of active calls. MemoryInCallService tracks telecom states and persists completed calls to Room via CallRepository. Implemented ContactsRepository to fetch device contacts.
- **Acceptance Criteria:**
  - InCallService tracks call states
  - Room Database persists call history
  - Contacts repository fetches device contacts

### Task_3_MainScreens: Build main Compose UI with Navigation: Bottom Nav, Dial Pad, Contacts list, Recents (Room integration), Settings, and Memory placeholder tab.
- **Status:** COMPLETED
- **Updates:** Completed Main Screens UI. Navigation graph implemented using Jetpack Navigation. Bottom navigation for Recents, Contacts, Keypad, and Memory tabs. Added Settings screen with Default Phone App check. Recents connects to CallRepository with detail view. Contacts screen fetches device contacts with dynamic permission check. Keypad screen allows entering phone numbers and placing calls via Telecom.
- **Acceptance Criteria:**
  - Navigation graph implemented
  - Dial Pad can initiate a call intent
  - Contacts and Recents load data correctly

### Task_4_InCallUI: Implement ViewModels and Compose UIs for Incoming, Outgoing, and Ongoing call states, wired to the InCallService.
- **Status:** COMPLETED
- **Updates:** Completed In-Call UI and ViewModels. Created InCallViewModel observing CallStateManager. Implemented IncomingCallContent with Accept/Decline, OngoingCallContent with call duration timer, Mute, Speaker, Keypad DTMF overlay, and Hang up controls. Integrated InCallOverlay in MainActivity to display when calls are active. Verified with unit tests.
- **Acceptance Criteria:**
  - Incoming call screen shows accept/reject
  - Ongoing call screen shows call controls
  - UIs react to InCallService state changes

### Task_5_RunAndVerify: Run and Verify the application. Instruct critic_agent to verify application stability (no crashes), confirm alignment with user requirements, and report critical UI issues.
- **Status:** COMPLETED
- **Updates:** Ran and verified application build and unit tests. Both assembleDebug and testDebugUnitTest completed cleanly with 0 failures. Critic agent confirmed codebase architecture, InCallService implementation, Manifest permissions, and Material 3 design alignment.
- **Acceptance Criteria:**
  - make sure all existing tests pass
  - build pass
  - app does not crash
- **Duration:** N/A

