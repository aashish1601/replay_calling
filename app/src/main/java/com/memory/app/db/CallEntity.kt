package com.memory.app.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "calls")
data class CallEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val phoneNumber: String,
    val contactName: String? = null,
    val direction: String, // INCOMING, OUTGOING, MISSED
    val startedAt: Long,
    val endedAt: Long? = null,
    val duration: Long? = null,
    val state: String = "DISCONNECTED", // RINGING, ACTIVE, DISCONNECTED
    val createdAt: Long = System.currentTimeMillis()
)
