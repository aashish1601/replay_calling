package com.memory.app.util

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.telecom.TelecomManager
import androidx.core.app.ActivityCompat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object CallUtils {

    fun launchCall(context: Context, phoneNumber: String) {
        val trimmed = phoneNumber.trim()
        if (trimmed.isEmpty()) return
        
        val uri = Uri.parse("tel:${Uri.encode(trimmed)}")
        val telecomManager = context.getSystemService(Context.TELECOM_SERVICE) as? TelecomManager
        
        try {
            if (ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.CALL_PHONE
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                if (telecomManager != null) {
                    val extras = Bundle()
                    telecomManager.placeCall(uri, extras)
                    return
                }
            }
            
            val callIntent = Intent(Intent.ACTION_CALL, uri).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(callIntent)
        } catch (e: Exception) {
            val dialIntent = Intent(Intent.ACTION_DIAL, uri).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(dialIntent)
        }
    }

    fun formatTimestamp(timestampMillis: Long): String {
        if (timestampMillis <= 0) return ""
        
        val now = Calendar.getInstance()
        val callTime = Calendar.getInstance().apply { timeInMillis = timestampMillis }
        
        val isSameDay = now.get(Calendar.YEAR) == callTime.get(Calendar.YEAR) &&
                now.get(Calendar.DAY_OF_YEAR) == callTime.get(Calendar.DAY_OF_YEAR)
        
        val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
        return if (isSameDay) {
            "Today, " + timeFormat.format(Date(timestampMillis))
        } else {
            val dateFormat = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault())
            dateFormat.format(Date(timestampMillis))
        }
    }

    fun formatDuration(durationMillis: Long?): String {
        if (durationMillis == null || durationMillis <= 0) return "0s"
        val seconds = durationMillis / 1000
        val mins = seconds / 60
        val remSeconds = seconds % 60
        return if (mins > 0) {
            "${mins}m ${remSeconds}s"
        } else {
            "${seconds}s"
        }
    }
}
