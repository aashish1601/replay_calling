package com.memory.app.model

import android.telecom.Call

data class CallSession(
    val id: String,
    val phoneNumber: String,
    val contactName: String?,
    val direction: CallDirection,
    val state: CallState,
    val startedAt: Long,
    val activeStartedAt: Long? = null,
    val telecomCall: Call? = null
)
