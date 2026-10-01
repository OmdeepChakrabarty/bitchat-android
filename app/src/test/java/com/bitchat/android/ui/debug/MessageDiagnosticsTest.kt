package com.bitchat.android.ui.debug

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * The diagnostics buffer is what makes a send/receive failure inspectable on the device, so
 * its recording, direction labelling and truncation behavior are pinned here.
 */
@RunWith(RobolectricTestRunner::class)
class MessageDiagnosticsTest {

    @Before
    fun setUp() {
        MessageDiagnostics.clear()
    }

    @After
    fun tearDown() {
        MessageDiagnostics.clear()
    }

    @Test
    fun `records each direction with its label`() {
        MessageDiagnostics.tx("text.send", "route=mesh-timeline chars=5")
        MessageDiagnostics.rx("text.received", "route=broadcast peer=01020304 chars=5")
        MessageDiagnostics.sys("command", "name=/help")

        val lines = MessageDiagnostics.snapshot()
        assertEquals(3, lines.size)
        assertTrue(lines[0].contains("TX"))
        assertTrue(lines[0].contains("text.send"))
        assertTrue(lines[0].contains("route=mesh-timeline"))
        assertTrue(lines[1].contains("RX"))
        assertTrue(lines[1].contains("text.received"))
        assertTrue(lines[2].contains("SYS"))
        assertTrue(lines[2].contains("command"))
    }

    @Test
    fun `lines are timestamped so stages can be ordered`() {
        MessageDiagnostics.tx("media.pickup", "bytes=10")

        val line = MessageDiagnostics.snapshot().single()
        assertTrue("expected a leading HH:mm:ss.SSS stamp in: $line", line.matches(Regex("^\\d{2}:\\d{2}:\\d{2}\\.\\d{3}\\s+.*")))
    }

    @Test
    fun `clear empties the buffer`() {
        MessageDiagnostics.tx("media.route", "route=public")
        assertFalse(MessageDiagnostics.snapshot().isEmpty())

        MessageDiagnostics.clear()
        assertTrue(MessageDiagnostics.snapshot().isEmpty())
    }

    @Test
    fun `buffer is bounded so a long session cannot grow without limit`() {
        repeat(600) { MessageDiagnostics.tx("media.progress", "state=sending sent=$it") }

        assertEquals(500, MessageDiagnostics.snapshot().size)
    }

    @Test
    fun `peer identifiers are truncated to a prefix`() {
        assertEquals("01020304", MessageDiagnostics.peer("0102030405060708"))
        assertEquals("none", MessageDiagnostics.peer(null))
    }

    @Test
    fun `detail is optional so bare events still record`() {
        MessageDiagnostics.tx("media.prepare")

        assertTrue(MessageDiagnostics.snapshot().single().contains("media.prepare"))
    }
}
