package com.memory.app.model

/**
 * Represents the user-selected recording mode.
 */
enum class RecordingMode(val displayName: String) {
    /** Native phone recording / MediaStore automated scanning */
    NATIVE("Native phone recording"),
    
    /** Best-effort fallback using the mic + forcing speakerphone */
    ACOUSTIC("Microphone + speakerphone"),
    
    /** Prompt the user after the call to manually import */
    ASK_ME("Ask me"),
    
    /** Automated recording logic is turned off */
    OFF("Off")
}
