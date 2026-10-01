package com.bitchat.android.ui

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.bitchat.android.geohash.ChannelID
import com.bitchat.android.geohash.GeohashChannel
import com.bitchat.android.geohash.GeohashChannelLevel
import com.bitchat.android.mesh.MeshService
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner

/**
 * Behaviour for the commands added on top of the inherited BitChat set: `/sos`, `/transfer`,
 * `/who` and `/help`, plus the unknown-command path. Existing commands keep their own tests.
 */
@RunWith(RobolectricTestRunner::class)
class CommandFeatureTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @OptIn(ExperimentalCoroutinesApi::class)
    private val testDispatcher = UnconfinedTestDispatcher()
    private val testScope = TestScope(testDispatcher)
    private val chatState = ChatState(scope = testScope)
    private lateinit var commandProcessor: CommandProcessor

    private val messageManager = MessageManager(state = chatState)
    private val channelManager = ChannelManager(
        state = chatState,
        messageManager = messageManager,
        dataManager = DataManager(context = context),
        coroutineScope = testScope
    )
    private val meshService: MeshService = mock()
    private val sentMessages = mutableListOf<Triple<String, List<String>, String?>>()

    private val onSendMessage: (String, List<String>, String?) -> Unit = { content, mentions, channel ->
        sentMessages += Triple(content, mentions, channel)
    }

    private fun run(command: String, myPeerID: String = "me") {
        commandProcessor.processCommand(
            command = command,
            meshService = meshService,
            myPeerID = myPeerID,
            onSendMessage = onSendMessage,
            viewModel = null
        )
    }

    private fun mainTimelineTexts(): List<String> =
        chatState.getMessagesValue().map { it.content }

    /**
     * Command replies follow the active conversation: a command typed in a private chat or a
     * channel answers there, not on the main timeline.
     */
    private fun outputTexts(): List<String> = buildList {
        addAll(chatState.getMessagesValue().map { it.content })
        chatState.getChannelMessagesValue().forEach { (_, list) -> addAll(list.map { it.content }) }
        chatState.getPrivateChatsValue().forEach { (_, list) -> addAll(list.map { it.content }) }
    }

    @Before
    fun setup() {
        commandProcessor = CommandProcessor(
            state = chatState,
            messageManager = messageManager,
            channelManager = channelManager,
            privateChatManager = PrivateChatManager(
                state = chatState,
                messageManager = messageManager,
                dataManager = DataManager(context = context),
                noiseSessionDelegate = mock<NoiseSessionDelegate>()
            )
        )
        whenever(meshService.getPeerNicknames()).thenReturn(mapOf("peer-alice" to "alice"))
        sentMessages.clear()
    }

    // MARK: - /sos

    @Test
    fun `sos broadcasts on the mesh timeline`() {
        run("/sos")

        val sent = sentMessages.single()
        assertEquals(SOS_PREFIX, sent.first)
        assertEquals(emptyList<String>(), sent.second)
        assertEquals(null, sent.third)
        assertTrue(mainTimelineTexts().any { it.startsWith(SOS_PREFIX) })
        assertTrue(mainTimelineTexts().any { it.contains("sent SOS") })
    }

    @Test
    fun `sos appends the note`() {
        run("/sos need help at the north entrance")

        assertEquals("$SOS_PREFIX need help at the north entrance", sentMessages.single().first)
    }

    @Test
    fun `sos is rejected in a private chat`() {
        chatState.setSelectedPrivateChatPeer("peer-alice")

        run("/sos")

        assertTrue("nothing may be sent from a private chat", sentMessages.isEmpty())
        assertTrue(outputTexts().any { it.contains("only works in the mesh") })
    }

    @Test
    fun `sos is rejected in a location channel`() {
        chatState.setSelectedLocationChannel(
            ChannelID.Location(GeohashChannel(GeohashChannelLevel.CITY, "u0nd"))
        )

        run("/sos")

        assertTrue("nothing may be sent from a location channel", sentMessages.isEmpty())
        assertTrue(outputTexts().any { it.contains("only works in the mesh") })
    }

    @Test
    fun `sos is allowed inside a mesh channel`() {
        chatState.setCurrentChannel("#relief")

        run("/sos")

        assertEquals("#relief", sentMessages.single().third)
        assertEquals(SOS_PREFIX, sentMessages.single().first)
    }

    // MARK: - /transfer

    @Test
    fun `transfer requires an active channel`() {
        run("/transfer alice", myPeerID = "me")

        assertTrue(mainTimelineTexts().any { it.contains("must be in a channel") })
    }

    @Test
    fun `transfer refuses a non creator`() {
        channelManager.joinChannel("#owned", password = null, myPeerID = "creator")
        chatState.setCurrentChannel("#owned")

        run("/transfer alice", myPeerID = "someone-else")

        assertTrue(outputTexts().any { it.contains("must be the channel creator") })
        assertFalse(channelManager.isChannelCreator("#owned", "peer-alice"))
    }

    @Test
    fun `transfer hands ownership to the named peer`() {
        channelManager.joinChannel("#owned", password = null, myPeerID = "creator")
        chatState.setCurrentChannel("#owned")

        run("/transfer alice", myPeerID = "creator")

        assertTrue(channelManager.isChannelCreator("#owned", "peer-alice"))
        assertTrue(outputTexts().any { it.contains("transferred ownership") })
    }

    @Test
    fun `transfer reports an unknown nickname`() {
        channelManager.joinChannel("#owned", password = null, myPeerID = "creator")
        chatState.setCurrentChannel("#owned")

        run("/transfer nobody", myPeerID = "creator")

        assertTrue(outputTexts().any { it.contains("not found") })
    }

    @Test
    fun `transfer refuses to transfer to yourself`() {
        channelManager.joinChannel("#owned", password = null, myPeerID = "creator")
        chatState.setCurrentChannel("#owned")
        whenever(meshService.getPeerNicknames()).thenReturn(mapOf("creator" to "creator"))

        run("/transfer creator", myPeerID = "creator")

        assertTrue(outputTexts().any { it.contains("already own") })
    }

    @Test
    fun `transfer is not broadcast to the channel`() {
        channelManager.joinChannel("#owned", password = null, myPeerID = "creator")
        chatState.setCurrentChannel("#owned")

        run("/transfer alice", myPeerID = "creator")

        val channelTraffic = chatState.getChannelMessagesValue()["#owned"].orEmpty()
            .filterNot { it.sender == "system" }
        assertTrue("ownership change must stay local", channelTraffic.isEmpty())
    }

    // MARK: - /who, /help and unknown commands

    @Test
    fun `who is an alias of w`() {
        run("/who")
        val viaAlias = mainTimelineTexts()

        chatState.setMessages(emptyList())
        run("/w")
        val viaPrimary = mainTimelineTexts()

        assertEquals(viaPrimary, viaAlias)
        assertTrue("both spellings must answer", viaAlias.isNotEmpty())
        assertTrue(viaAlias.any { it.contains("no one else is around right now.") })
    }

    @Test
    fun `help is generated from the registry`() {
        run("/help")

        val help = mainTimelineTexts().single()
        assertEquals(CommandRegistry.helpText(
            CommandContext(inPrivateChat = false, inChannel = false, inPublicGeohash = false)
        ), help)
        assertTrue(help.contains("/sos"))
        assertFalse("help must not advertise channel commands here", help.contains("/transfer"))
    }

    @Test
    fun `help inside a channel lists the channel commands`() {
        chatState.setCurrentChannel("#relief")

        run("/help")

        val help = outputTexts().single()
        assertTrue(help.contains("/transfer <nickname>"))
        assertTrue(help.contains("/pass <password>"))
    }

    @Test
    fun `unknown command points at help`() {
        run("/wtf")

        assertTrue(mainTimelineTexts().any { it.contains("unknown command: /wtf") })
        assertTrue(mainTimelineTexts().any { it.contains("/help") })
    }

    @Test
    fun `unknown command is not advertised`() {
        run("/")

        assertTrue(
            CommandRegistry.available(
                CommandContext(inPrivateChat = false, inChannel = false, inPublicGeohash = false)
            ).none { it.command == "/wtf" }
        )
    }

    // MARK: - Suggestions

    @Test
    fun `suggestions come from the registry and include the new commands`() {
        commandProcessor.updateCommandSuggestions("/")

        val offered = chatState.getCommandSuggestionsValue().map { it.command }
        assertTrue(offered.contains("/sos"))
        assertTrue(offered.contains("/help"))
        assertTrue(offered.contains("/who") == false)
        assertEquals(
            CommandRegistry.available(
                CommandContext(inPrivateChat = false, inChannel = false, inPublicGeohash = false)
            ).map { it.command }.sorted(),
            offered.sorted()
        )
    }

    @Test
    fun `suggestions omit removed and unavailable commands`() {
        commandProcessor.updateCommandSuggestions("/")

        val offered = chatState.getCommandSuggestionsValue().map { it.command }
        assertFalse(offered.contains("/save"))
        assertFalse(offered.contains("/transfer"))
        assertFalse(offered.contains("/pass"))
    }

    @Test
    fun `suggestions inside a channel add the channel commands`() {
        chatState.setCurrentChannel("#relief")
        commandProcessor.updateCommandSuggestions("/")

        val offered = chatState.getCommandSuggestionsValue().map { it.command }
        assertTrue(offered.contains("/transfer"))
        assertTrue(offered.contains("/pass"))
    }

    @Test
    fun `who alias is suggested when typing it`() {
        commandProcessor.updateCommandSuggestions("/who")

        assertEquals(listOf("/w"), chatState.getCommandSuggestionsValue().map { it.command })
    }
}
