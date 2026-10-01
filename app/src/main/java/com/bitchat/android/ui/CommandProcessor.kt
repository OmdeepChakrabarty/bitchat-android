package com.bitchat.android.ui

import com.bitchat.android.mesh.MeshService
import com.bitchat.android.model.BitchatMessage
import com.bitchat.android.ui.debug.MessageDiagnostics
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Marks an `/sos` broadcast so it is distinguishable from ordinary chatter at a glance.
 * An optional note follows it on the same line.
 */
internal const val SOS_PREFIX = "🆘 SOS"

/**
 * Handles processing of IRC-style commands
 */
class CommandProcessor(
    private val state: ChatState,
    private val messageManager: MessageManager,
    private val channelManager: ChannelManager,
    private val privateChatManager: PrivateChatManager,
    private val coroutineScope: CoroutineScope? = null
) {
    
    // MARK: - Command Processing
    
    fun processCommand(command: String, meshService: MeshService, myPeerID: String, onSendMessage: (String, List<String>, String?) -> Unit, viewModel: ChatViewModel? = null): Boolean {
        if (!command.startsWith("/")) return false
        
        val parts = command.split(" ")
        val cmd = parts.first().lowercase()
        val definition = CommandRegistry.find(cmd)
        // Arguments are deliberately not logged: /pass takes a password and /pay a bearer token.
        MessageDiagnostics.sys("command", "name=$cmd resolved=${definition?.command ?: "none"}")
        if (definition == null) {
            handleUnknownCommand(cmd)
            return true
        }
        val context = currentContext()
        if (definition.meshOnly && !context.isMesh) {
            addSystemMessage("${definition.command} only works in the mesh timeline or a mesh channel.")
            return true
        }
        when (definition.handlerId) {
            CommandHandlerId.Join -> handleJoinCommand(parts, myPeerID)
            CommandHandlerId.Message -> handleMessageCommand(parts, meshService, viewModel)
            CommandHandlerId.Pay -> handlePayCommand(command, meshService, myPeerID, onSendMessage, viewModel)
            CommandHandlerId.Who -> handleWhoCommand(meshService, viewModel)
            CommandHandlerId.Clear -> handleClearCommand()
            CommandHandlerId.Pass -> handlePassCommand(parts, myPeerID)
            CommandHandlerId.Block -> handleBlockCommand(parts, meshService)
            CommandHandlerId.Unblock -> handleUnblockCommand(parts, meshService)
            CommandHandlerId.Hug -> handleActionCommand(parts, "gives", "a warm hug 🫂", meshService, myPeerID, onSendMessage, viewModel)
            CommandHandlerId.Slap -> handleActionCommand(parts, "slaps", "around a bit with a large trout 🐟", meshService, myPeerID, onSendMessage, viewModel)
            CommandHandlerId.Channels -> handleChannelsCommand()
            CommandHandlerId.Transfer -> handleTransferCommand(parts, meshService, myPeerID)
            CommandHandlerId.Sos -> handleSosCommand(parts.drop(1).joinToString(" ").trim(), onSendMessage, myPeerID)
            CommandHandlerId.Help -> addSystemMessage(CommandRegistry.helpText(context))
        }
        
        return true
    }
    
    /**
     * Where the command was typed. Drives the suggestions offered, the restriction `/sos`
     * enforces, and which conversation a command's reply is posted into.
     */
    private fun currentContext(): CommandContext = CommandContext(
        inPrivateChat = state.getSelectedPrivateChatPeerValue() != null,
        inChannel = state.getCurrentChannelValue() != null,
        inPublicGeohash = state.getSelectedPrivateChatPeerValue() == null &&
            state.selectedLocationChannel.value is com.bitchat.android.geohash.ChannelID.Location
    )
    
    private fun handleJoinCommand(parts: List<String>, myPeerID: String) {
        if (parts.size > 1) {
            val channelName = parts[1]
            val channel = if (channelName.startsWith("#")) channelName else "#$channelName"
            val password = if (parts.size > 2) parts[2] else null
            val success = channelManager.joinChannel(channel, password, myPeerID)
            if (success) {
                addSystemMessage("joined channel $channel")
            }
        } else {
            addSystemMessage("usage: /join <channel>")
        }
    }
    
    private fun handleMessageCommand(parts: List<String>, meshService: MeshService, viewModel: ChatViewModel?) {
        if (parts.size > 1) {
            val targetName = parts[1].removePrefix("@")
            val peerID = getPeerIDForNickname(targetName, meshService)
            
            if (peerID != null) {
                val success = privateChatManager.startPrivateChat(peerID, meshService)
                
                if (success) {
                    if (parts.size > 2) {
                        val messageContent = parts.drop(2).joinToString(" ")
                        val recipientNickname = getPeerNickname(peerID, meshService)
                        sendPrivateMessage(
                            messageContent, 
                            peerID, 
                            recipientNickname,
                            state.getNicknameValue(),
                            getMyPeerID(meshService),
                            meshService,
                            viewModel
                        )
                    } else {
                        addSystemMessage("started private chat with $targetName")
                    }
                }
            } else {
                addSystemMessage("user '$targetName' not found. they may be offline or using a different nickname.")
            }
        } else {
            addSystemMessage("usage: /msg <nickname> [message]")
        }
    }
    
    private fun handleWhoCommand(meshService: MeshService, viewModel: ChatViewModel? = null) {
        // Channel-aware who command (matches iOS behavior)
        val (peerList, contextDescription) = if (viewModel != null) {
            when (val selectedChannel = viewModel.selectedLocationChannel.value) {
                is com.bitchat.android.geohash.ChannelID.Mesh,
                null -> {
                    // Mesh channel: show Bluetooth-connected peers
                    val connectedPeers = state.getConnectedPeersValue()
                    val peerList = connectedPeers.joinToString(", ") { peerID ->
                        getPeerNickname(peerID, meshService)
                    }
                    Pair(peerList, "online users")
                }
                
                is com.bitchat.android.geohash.ChannelID.Location -> {
                    // Location channel: show geohash participants
                    val geohashPeople = viewModel.geohashPeople.value ?: emptyList()
                    val currentNickname = state.getNicknameValue()
                    
                    val participantList = geohashPeople.mapNotNull { person ->
                        val displayName = person.displayName
                        // Exclude self from list
                        if (displayName.startsWith("${currentNickname}#")) {
                            null
                        } else {
                            displayName
                        }
                    }.joinToString(", ")
                    
                    Pair(participantList, "participants in ${selectedChannel.channel.geohash}")
                }
            }
        } else {
            // Fallback to mesh behavior
            val connectedPeers = state.getConnectedPeersValue()
            val peerList = connectedPeers.joinToString(", ") { peerID ->
                getPeerNickname(peerID, meshService)
            }
            Pair(peerList, "online users")
        }
        
        addSystemMessage(
            if (peerList.isEmpty()) {
                "no one else is around right now."
            } else {
                "$contextDescription: $peerList"
            }
        )
    }
    
    private fun handleClearCommand() {
        when {
            state.getSelectedPrivateChatPeerValue() != null -> {
                // Clear private chat
                val peerID = state.getSelectedPrivateChatPeerValue()!!
                messageManager.clearPrivateMessages(peerID)
                // `/clear` removes history but should not navigate away from the chat the
                // command was issued in. A later message will repopulate this conversation.
                state.setSelectedPrivateChatPeer(peerID)
            }
            state.getCurrentChannelValue() != null -> {
                // Clear channel messages
                val channel = state.getCurrentChannelValue()!!
                messageManager.clearChannelMessages(channel)
            }
            else -> {
                // Clear main messages
                messageManager.clearMessages()
            }
        }
    }

    private fun handlePassCommand(parts: List<String>, peerID: String) {
        val currentChannel = state.getCurrentChannelValue()

        if (currentChannel == null) {
            addSystemMessage("you must be in a channel to set a password.")
            return
        }

        if (parts.size == 2){
            if(!channelManager.isChannelCreator(channel = currentChannel, peerID = peerID)){
                val systemMessage = BitchatMessage(
                    sender = "system",
                    content = "you must be the channel creator to set a password.",
                    timestamp = Date(),
                    isRelay = false
                )
                channelManager.addChannelMessage(currentChannel,systemMessage,null)
                return
            }
            val newPassword = parts[1]
            channelManager.setChannelPassword(currentChannel, newPassword)
            val systemMessage = BitchatMessage(
                sender = "system",
                content = "password changed for channel $currentChannel",
                timestamp = Date(),
                isRelay = false
            )
            channelManager.addChannelMessage(currentChannel,systemMessage,null)
        }
        else{
            val systemMessage = BitchatMessage(
                sender = "system",
                content = "usage: /pass <password>",
                timestamp = Date(),
                isRelay = false
            )
            channelManager.addChannelMessage(currentChannel,systemMessage,null)
        }
    }
    
    private fun handleBlockCommand(parts: List<String>, meshService: MeshService) {
        if (parts.size > 1) {
            val targetName = parts[1].removePrefix("@")
            privateChatManager.blockPeerByNickname(targetName, meshService)
        } else {
            // List blocked users
            addSystemMessage(privateChatManager.listBlockedUsers())
        }
    }
    
    private fun handleUnblockCommand(parts: List<String>, meshService: MeshService) {
        if (parts.size > 1) {
            val targetName = parts[1].removePrefix("@")
            privateChatManager.unblockPeerByNickname(targetName, meshService)
        } else {
            addSystemMessage("usage: /unblock <nickname>")
        }
    }
    
    private fun handleActionCommand(
        parts: List<String>, 
        verb: String, 
        object_: String, 
        meshService: MeshService,
        myPeerID: String,
        onSendMessage: (String, List<String>, String?) -> Unit,
        viewModel: ChatViewModel?
    ) {
        if (parts.size > 1) {
            val targetName = parts[1].removePrefix("@")
            val actionMessage = "* ${state.getNicknameValue() ?: "someone"} $verb $targetName $object_ *"

            // If we're in a geohash location channel, don't add a local echo here.
            // GeohashViewModel.sendGeohashMessage() will add the local echo with proper metadata.
            val isInLocationChannel = state.selectedLocationChannel.value is com.bitchat.android.geohash.ChannelID.Location

            // Send as regular message
            if (state.getSelectedPrivateChatPeerValue() != null) {
                val peerID = state.getSelectedPrivateChatPeerValue()!!
                sendPrivateMessage(
                    actionMessage,
                    peerID,
                    getPeerNickname(peerID, meshService),
                    state.getNicknameValue(),
                    myPeerID,
                    meshService,
                    viewModel
                )
            } else if (isInLocationChannel) {
                // Let the transport layer add the echo; just send it out
                onSendMessage(actionMessage, emptyList(), null)
            } else {
                val message = BitchatMessage(
                    sender = state.getNicknameValue() ?: myPeerID,
                    content = actionMessage,
                    timestamp = Date(),
                    isRelay = false,
                    senderPeerID = myPeerID,
                    channel = state.getCurrentChannelValue()
                )
                
                if (state.getCurrentChannelValue() != null) {
                    channelManager.addChannelMessage(state.getCurrentChannelValue()!!, message, myPeerID)
                    onSendMessage(actionMessage, emptyList(), state.getCurrentChannelValue())
                } else {
                    messageManager.addMessage(message)
                    onSendMessage(actionMessage, emptyList(), null)
                }
            }
        } else {
            addSystemMessage("usage: /${parts[0].removePrefix("/")} <nickname>")
        }
    }
    
    private fun handleChannelsCommand() {
        val allChannels = channelManager.getJoinedChannelsList()
        val channelList = if (allChannels.isEmpty()) {
            "no channels joined"
        } else {
            "joined channels: ${allChannels.joinToString(", ")}"
        }
        
        addSystemMessage(channelList)
    }

    /**
     * `/transfer <nickname>` — hand channel ownership to another peer.
     *
     * Creator-only and requires an active channel, mirroring `/pass`. The change is recorded
     * locally only: it is not announced to the channel, so it never adds traffic to the mesh
     * and cannot fail as a result of the network.
     */
    private fun handleTransferCommand(parts: List<String>, meshService: MeshService, peerID: String) {
        val channel = state.getCurrentChannelValue()
        if (channel == null) {
            addSystemMessage("usage: /transfer <nickname> — you must be in a channel to transfer it.")
            return
        }
        if (!channelManager.isChannelCreator(channel = channel, peerID = peerID)) {
            addSystemMessage("you must be the channel creator to transfer $channel.")
            return
        }
        if (parts.size < 2) {
            addSystemMessage("usage: /transfer <nickname>")
            return
        }
        val targetName = parts[1].removePrefix("@")
        val newOwner = getPeerIDForNickname(targetName, meshService)
        if (newOwner == null) {
            addSystemMessage("user '$targetName' not found. they may be offline or using a different nickname.")
            return
        }
        if (newOwner == peerID) {
            addSystemMessage("you already own $channel.")
            return
        }
        channelManager.transferChannelOwnership(channel, newOwner)
        addSystemMessage("transferred ownership of $channel to $targetName.")
    }

    /**
     * `/sos [note]` — broadcast a distress call on the mesh.
     *
     * Mesh only: the command is refused earlier in private chats and location channels, so
     * this handler always runs on the mesh timeline or a mesh channel. It reuses the ordinary
     * public message path — the same one `/hug`, `/slap` and `/pay public` use — rather than
     * introducing an SOS transport.
     */
    private fun handleSosCommand(
        note: String,
        onSendMessage: (String, List<String>, String?) -> Unit,
        myPeerID: String
    ) {
        val channel = state.getCurrentChannelValue()
        val where = if (channel != null) "in channel $channel" else "on the mesh timeline"
        val body = if (note.isBlank()) SOS_PREFIX else "$SOS_PREFIX $note"
        MessageDiagnostics.tx("sos", "channel=${channel ?: "timeline"} note=${note.isNotBlank()}")
        val message = BitchatMessage(
            sender = state.getNicknameValue() ?: myPeerID,
            content = body,
            timestamp = Date(),
            isRelay = false,
            senderPeerID = myPeerID,
            channel = channel
        )
        if (channel != null) {
            channelManager.addChannelMessage(channel, message, myPeerID)
        } else {
            messageManager.addMessage(message)
        }
        onSendMessage(body, emptyList(), channel)
        addSystemMessage("sent SOS $where.")
    }

    private fun handlePayCommand(
        command: String,
        meshService: MeshService,
        myPeerID: String,
        onSendMessage: (String, List<String>, String?) -> Unit,
        viewModel: ChatViewModel?
    ) {
        val args = command.trim().split(Regex("\\s+")).drop(1)
        if (args.isEmpty()) {
            addSystemMessage("usage: /pay <cashu token> [public] — Cashu tokens are bearer instruments")
            return
        }

        val publicConfirmed = args.lastOrNull()?.equals("public", ignoreCase = true) == true
        val rawToken = if (publicConfirmed) args.dropLast(1).joinToString(" ") else args.joinToString(" ")
        val token = CashuTokenDecoder.bareToken(rawToken)
        val info = token?.let { CashuTokenDecoder.decode(it, strict = true) }
        if (token == null || info == null) {
            addSystemMessage("invalid cashu token — not sending it")
            return
        }

        val selectedPeer = state.getSelectedPrivateChatPeerValue()
        if (selectedPeer != null) {
            privateChatManager.sendPrivateMessage(
                token,
                selectedPeer,
                getPeerNickname(selectedPeer, meshService),
                state.getNicknameValue(),
                myPeerID
            ) { content, peerID, recipientNickname, messageId ->
                sendPrivateMessageVia(meshService, content, peerID, recipientNickname, messageId, viewModel)
            }
        } else {
            if (!publicConfirmed) {
                addSystemMessage(
                    "Cashu tokens are bearer instruments. Anyone here can redeem this token. " +
                        "Confirm with: /pay <token> public"
                )
                return
            }
            val isLocationChannel =
                state.selectedLocationChannel.value is com.bitchat.android.geohash.ChannelID.Location
            if (!isLocationChannel) {
                val message = BitchatMessage(
                    sender = state.getNicknameValue() ?: myPeerID,
                    content = token,
                    timestamp = Date(),
                    isRelay = false,
                    senderPeerID = myPeerID,
                    channel = state.getCurrentChannelValue()
                )
                val channel = state.getCurrentChannelValue()
                if (channel != null) channelManager.addChannelMessage(channel, message, myPeerID)
                else messageManager.addMessage(message)
            }
            onSendMessage(token, emptyList(), state.getCurrentChannelValue())
        }

        addSystemMessage(
            "sent ${info.displayAmount ?: "Cashu token"} — bearer token; first redeemer wins"
        )
    }

    private fun addSystemMessage(content: String) {
        val message = BitchatMessage(
            sender = "system",
            content = content,
            timestamp = Date(),
            isRelay = false
        )
        val selectedPeer = state.getSelectedPrivateChatPeerValue()
        val selectedLocationChannel = state.selectedLocationChannel.value
        val channel = state.getCurrentChannelValue()
        when {
            selectedPeer != null -> {
                messageManager.addPrivateMessageNoUnread(selectedPeer, message.copy(isPrivate = true))
            }
            selectedLocationChannel is com.bitchat.android.geohash.ChannelID.Location -> {
                messageManager.addChannelMessage(
                    "geo:${selectedLocationChannel.channel.geohash}",
                    message
                )
            }
            channel != null -> channelManager.addChannelMessage(channel, message, null)
            else -> messageManager.addMessage(message)
        }
    }
    
    private fun handleUnknownCommand(cmd: String) {
        addSystemMessage("unknown command: $cmd. type /help to see available commands.")
    }
    
    /**
     * Dismiss the command and mention popups. The composer clears its field in code
     * after a send, which does not run onValueChange, so the popups need an explicit
     * clear.
     */
    fun clearSuggestions() {
        state.setShowCommandSuggestions(false)
        state.setCommandSuggestions(emptyList())
        state.setShowMentionSuggestions(false)
        state.setMentionSuggestions(emptyList())
    }

    // MARK: - Command Autocomplete

    fun updateCommandSuggestions(input: String) {
        if (!input.startsWith("/")) {
            state.setShowCommandSuggestions(false)
            state.setCommandSuggestions(emptyList())
            return
        }
        
        // Get all available commands based on context
        val allCommands = getAllAvailableCommands()
        
        // Filter commands based on input
        val filteredCommands = filterCommands(allCommands, input.lowercase())
        
        if (filteredCommands.isNotEmpty()) {
            state.setCommandSuggestions(filteredCommands)
            state.setShowCommandSuggestions(true)
        } else {
            state.setShowCommandSuggestions(false)
            state.setCommandSuggestions(emptyList())
        }
    }
    
    private fun getAllAvailableCommands(): List<CommandSuggestion> =
        CommandRegistry.available(currentContext()).map { it.suggestion() }
    
    private fun filterCommands(commands: List<CommandSuggestion>, input: String): List<CommandSuggestion> {
        return commands.filter { command ->
            // Check primary command
            command.command.startsWith(input) ||
            // Check aliases
            command.aliases.any { it.startsWith(input) }
        }.sortedBy { it.command }
    }
    
    fun selectCommandSuggestion(suggestion: CommandSuggestion): String {
        state.setShowCommandSuggestions(false)
        state.setCommandSuggestions(emptyList())
        return "${suggestion.command} "
    }
    
    // MARK: - Mention Autocomplete
    
    fun updateMentionSuggestions(input: String, meshService: MeshService, viewModel: ChatViewModel? = null) {
        // Check if input contains @ and we're at the end of a word or at the end of input
        val atIndex = input.lastIndexOf('@')
        if (atIndex == -1) {
            state.setShowMentionSuggestions(false)
            state.setMentionSuggestions(emptyList())
            return
        }
        
        // Get the text after the @ symbol
        val textAfterAt = input.substring(atIndex + 1)
        
        // If there's a space after @, don't show suggestions
        if (textAfterAt.contains(' ')) {
            state.setShowMentionSuggestions(false)
            state.setMentionSuggestions(emptyList())
            return
        }
        
        // Get peer candidates based on active channel (matches iOS logic exactly)
        val peerCandidates: List<String> = if (viewModel != null) {
            when (val selectedChannel = viewModel.selectedLocationChannel.value) {
                is com.bitchat.android.geohash.ChannelID.Mesh,
                null -> {
                    // Mesh channel: use Bluetooth mesh peer nicknames
                    val peerNicknames = meshService.getPeerNicknames()
                    peerNicknames.values.filter { it != peerNicknames[meshService.myPeerID] }
                }
                
                is com.bitchat.android.geohash.ChannelID.Location -> {
                    // Location channel: use geohash participants with collision-resistant suffixes
                    val geohashPeople = viewModel.geohashPeople.value
                    val currentNickname = state.getNicknameValue()
                    val duplicateNames = duplicateGeohashBaseNames(geohashPeople)
                    
                    geohashPeople.mapNotNull { person ->
                        val baseName = splitSuffix(person.displayName).first
                        val hasNicknameCollision =
                            baseName.lowercase(Locale.ROOT) in duplicateNames
                        val displayName = disambiguatedGeohashDisplayName(person, duplicateNames)
                        // A unique local nickname can be excluded directly. If it collides, the
                        // nickname alone cannot identify which row is self, so keep the suffixed
                        // rows rather than accidentally hiding the other user.
                        if (
                            !hasNicknameCollision &&
                            baseName.equals(currentNickname, ignoreCase = true)
                        ) {
                            null
                        } else {
                            displayName
                        }
                    }
                }
            }
        } else {
            // Fallback to mesh peers if no viewModel available
            val peerNicknames = meshService.getPeerNicknames()
            peerNicknames.values.filter { it != peerNicknames[meshService.myPeerID] }
        }
        
        val filteredNicknames = filterMentionCandidates(peerCandidates, textAfterAt)
        
        if (filteredNicknames.isNotEmpty()) {
            state.setMentionSuggestions(filteredNicknames)
            state.setShowMentionSuggestions(true)
        } else {
            state.setShowMentionSuggestions(false)
            state.setMentionSuggestions(emptyList())
        }
    }
    
    fun selectMentionSuggestion(nickname: String, currentText: String): String {
        state.setShowMentionSuggestions(false)
        state.setMentionSuggestions(emptyList())
        
        // Find the last @ symbol position
        val atIndex = currentText.lastIndexOf('@')
        if (atIndex == -1) {
            return "$currentText@$nickname "
        }
        
        // Replace the text from the @ symbol to the end with the mention
        val textBeforeAt = currentText.substring(0, atIndex)
        return "$textBeforeAt@$nickname "
    }
    
    // MARK: - Utility Functions
    
    private fun getPeerIDForNickname(nickname: String, meshService: MeshService): String? {
        return meshService.getPeerNicknames().entries.find { it.value == nickname }?.key
    }
    
    private fun getPeerNickname(peerID: String, meshService: MeshService): String {
        return meshService.getPeerNicknames()[peerID]
            ?: peerID
    }
    
    private fun getMyPeerID(meshService: MeshService): String {
        return meshService.myPeerID
    }

    private fun sendPrivateMessage(
        content: String,
        peerID: String,
        recipientNickname: String?,
        senderNickname: String?,
        myPeerID: String,
        meshService: MeshService,
        viewModel: ChatViewModel?
    ) {
        val send: (String, String, String, String) -> Unit =
            { messageContent, peerIdParam, recipientNicknameParam, messageId ->
                sendPrivateMessageVia(
                    meshService,
                    messageContent,
                    peerIdParam,
                    recipientNicknameParam,
                    messageId,
                    viewModel
                )
            }
        val scope = coroutineScope
        if (scope == null) {
            privateChatManager.sendPrivateMessage(
                content,
                peerID,
                recipientNickname,
                senderNickname,
                myPeerID,
                send
            )
        } else {
            scope.launch {
                privateChatManager.sendPrivateMessageDurably(
                    content,
                    peerID,
                    recipientNickname,
                    senderNickname,
                    myPeerID,
                    send
                )
            }
        }
    }

    private fun sendPrivateMessageVia(
        meshService: MeshService,
        content: String,
        peerID: String,
        recipientNickname: String,
        messageId: String,
        viewModel: ChatViewModel?
    ) {
        if (viewModel != null) {
            com.bitchat.android.services.MessageRouter
                .getInstance(viewModel.getApplication(), meshService)
                .sendPrivate(content, peerID, recipientNickname, messageId)
        } else {
            meshService.sendPrivateMessage(content, peerID, recipientNickname, messageId)
        }
    }
}

/**
 * Keep mention autocomplete useful in crowded channels: a bare `anon` identity has not announced
 * a username and is not actionable. Names such as `anon1234` are announced usernames and remain
 * valid mention targets.
 */
internal fun filterMentionCandidates(
    candidates: List<String>,
    query: String
): List<String> {
    return candidates.asSequence()
        .map(String::trim)
        .filter(String::isNotEmpty)
        .filterNot(::isUnannouncedNickname)
        .filter { nickname -> nickname.startsWith(query, ignoreCase = true) }
        .distinctBy { nickname -> nickname.lowercase(Locale.ROOT) }
        .sortedWith(String.CASE_INSENSITIVE_ORDER)
        .toList()
}
