package com.bitchat.android.ui.debug

import android.util.Log
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * TEMPORARY DIAGNOSTIC (image-send debugging) — remove this file and every
 * `ImageSendDiagnostics.log(...)` call site once the regression is identified.
 *
 * Records a timestamped line for each stage of the outgoing image transfer
 * path (sender) and the FILE_TRANSFER handling path (receiver). Entries are
 * viewable in-app from the Debug sheet's "Image send diagnostics" section,
 * with copy-to-clipboard, so no ADB/Logcat is required.
 */
object ImageSendDiagnostics {
    private const val TAG = "ImageSendDiag"
    private const val MAX_ENTRIES = 400
    private val lock = Any()
    private val entries = ArrayDeque<String>()
    private val timeFormat = SimpleDateFormat("HH:mm:ss.SSS", Locale.US)

    fun log(message: String) {
        val line = "${timeFormat.format(Date())}  $message"
        Log.d(TAG, line)
        synchronized(lock) {
            entries.addLast(line)
            while (entries.size > MAX_ENTRIES) entries.removeFirst()
        }
    }

    fun snapshot(): List<String> = synchronized(lock) { entries.toList() }

    fun clear() = synchronized(lock) { entries.clear() }
}
