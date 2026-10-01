package com.bitchat.android.ui.debug

import android.util.Log
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * In-app diagnostics for the message and media paths.
 *
 * Records one line per stage of a send or receive so a failure can be located without
 * ADB or Logcat. Lines are kept in a small in-memory ring buffer, surfaced in the Debug
 * settings sheet ("Message diagnostics") and copyable to the clipboard.
 *
 * Privacy: this records routing metadata only. Message contents, command arguments,
 * passwords, tokens and keys are never recorded — a text send is logged by length and
 * destination, and a command by name only, so `/pass` and `/pay` arguments cannot leak.
 * Peer identifiers are truncated the same way the rest of the app logs them.
 */
object MessageDiagnostics {
    private const val TAG = "MsgDiag"
    private const val MAX_ENTRIES = 500

    /** Which side of the conversation a line describes. */
    enum class Direction(val label: String) {
        TX("TX"),
        RX("RX"),
        SYS("SYS")
    }

    private val lock = Any()
    private val entries = ArrayDeque<String>()
    private val timeFormat = SimpleDateFormat("HH:mm:ss.SSS", Locale.US)

    fun tx(event: String, detail: String = "") = record(Direction.TX, event, detail)

    fun rx(event: String, detail: String = "") = record(Direction.RX, event, detail)

    fun sys(event: String, detail: String = "") = record(Direction.SYS, event, detail)

    fun record(direction: Direction, event: String, detail: String = "") {
        val line = buildString {
            append(timeFormat.format(Date()))
            append("  ")
            append(direction.label)
            append("  ")
            append(event)
            if (detail.isNotEmpty()) {
                append(" | ")
                append(detail)
            }
        }
        Log.d(TAG, line)
        synchronized(lock) {
            entries.addLast(line)
            while (entries.size > MAX_ENTRIES) entries.removeFirst()
        }
    }

    fun snapshot(): List<String> = synchronized(lock) { entries.toList() }

    fun clear() = synchronized(lock) { entries.clear() }

    /**
     * Peer identifiers appear in routing decisions, but a prefix is enough to correlate
     * lines and matches how peers are logged elsewhere in the app.
     */
    fun peer(peerID: String?): String = peerID?.take(8) ?: "none"
}