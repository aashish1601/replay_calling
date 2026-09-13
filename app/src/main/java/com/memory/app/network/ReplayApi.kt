package com.memory.app.network

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface ReplayApi {

    @POST("calls/start")
    suspend fun startRecording(@Body request: StartRecordingRequest): Response<StartRecordingResponse>

    @POST("calls/stop")
    suspend fun stopRecording(@Body request: StopRecordingRequest): Response<StopRecordingResponse>
}

@JsonClass(generateAdapter = true)
data class StartRecordingRequest(
    @Json(name = "userPhone") val userPhone: String,
    @Json(name = "contactPhone") val contactPhone: String,
    @Json(name = "userId") val userId: String
)

@JsonClass(generateAdapter = true)
data class StartRecordingResponse(
    @Json(name = "success") val success: Boolean,
    @Json(name = "callSid") val callSid: String?,
    @Json(name = "message") val message: String?
)

@JsonClass(generateAdapter = true)
data class StopRecordingRequest(
    @Json(name = "callSid") val callSid: String,
    @Json(name = "userId") val userId: String
)

@JsonClass(generateAdapter = true)
data class StopRecordingResponse(
    @Json(name = "success") val success: Boolean,
    @Json(name = "message") val message: String?
)
