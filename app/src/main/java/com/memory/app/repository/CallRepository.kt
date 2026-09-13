package com.memory.app.repository

import com.memory.app.db.CallDao
import com.memory.app.db.CallEntity
import com.memory.app.model.CallSession
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class CallRepository @Inject constructor(
    private val callDao: CallDao
) {
    suspend fun saveCall(session: CallSession) {
        val endedAt = System.currentTimeMillis()
        val duration = if (session.startedAt > 0) endedAt - session.startedAt else 0
        
        val entity = CallEntity(
            phoneNumber = session.phoneNumber,
            contactName = session.contactName,
            direction = session.direction.name,
            startedAt = session.startedAt,
            endedAt = endedAt,
            duration = duration,
            state = session.state.name
        )
        callDao.insert(entity)
    }
    
    fun getAllCalls(): Flow<List<CallEntity>> = callDao.getAllCalls()

    suspend fun getCallById(id: Long): CallEntity? = callDao.getCallById(id)
}
