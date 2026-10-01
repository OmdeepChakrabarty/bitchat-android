package com.bitchat.android.ui

/**
 * One declaration per slash command: its names, syntax, help text, where it is offered,
 * and which handler runs it.
 *
 * The registry is the single source of truth. The composer autocomplete and `/help` are both
 * generated from it, and [CommandHandlerId] guarantees a declared command always has code
 * behind it — adding an entry without a handler branch is a compile error, which is how
 * `/save` and `/transfer` previously ended up advertised but unrunnable.
 */
internal enum class CommandHandlerId {
    Join,
    Message,
    Pay,
    Who,
    Clear,
    Pass,
    Block,
    Unblock,
    Hug,
    Slap,
    Channels,
    Transfer,
    Sos,
    Help
}

/** Where a command was typed, so context restrictions and system output can follow it. */
internal data class CommandContext(
    val inPrivateChat: Boolean,
    val inChannel: Boolean,
    val inPublicGeohash: Boolean
) {
    /** True for the mesh timeline and mesh channels only: no private chat, no location channel. */
    val isMesh: Boolean get() = !inPrivateChat && !inPublicGeohash
}

internal data class CommandDefinition(
    val command: String,
    val aliases: List<String> = emptyList(),
    val syntax: String? = null,
    val description: String,
    val handlerId: CommandHandlerId,
    /** Offered only while a mesh channel is active. */
    val channelOnly: Boolean = false,
    /** Hidden in a public location channel, where `/pay` would be a public token send. */
    val hiddenInPublicGeohash: Boolean = false,
    /** Rejected outside the mesh; the description explains the restriction. */
    val meshOnly: Boolean = false
) {
    val names: List<String> get() = listOf(command) + aliases

    fun matches(input: String): Boolean = names.any { it == input }

    fun availableIn(context: CommandContext): Boolean {
        if (channelOnly && !context.inChannel) return false
        if (hiddenInPublicGeohash && context.inPublicGeohash) return false
        return true
    }

    fun suggestion(): CommandSuggestion =
        CommandSuggestion(command, aliases, syntax, description)

    fun helpLine(): String {
        val names = names.joinToString(" · ")
        val usage = if (syntax.isNullOrBlank()) names else "$names $syntax"
        return "$usage — $description"
    }
}

internal object CommandRegistry {

    val definitions: List<CommandDefinition> = listOf(
        CommandDefinition(
            command = "/block",
            syntax = "[nickname]",
            description = "block a peer, or list blocked peers",
            handlerId = CommandHandlerId.Block
        ),
        CommandDefinition(
            command = "/channels",
            description = "list the channels you have joined",
            handlerId = CommandHandlerId.Channels
        ),
        CommandDefinition(
            command = "/clear",
            description = "clear the messages in this conversation",
            handlerId = CommandHandlerId.Clear
        ),
        CommandDefinition(
            command = "/help",
            description = "list the available commands",
            handlerId = CommandHandlerId.Help
        ),
        CommandDefinition(
            command = "/hug",
            syntax = "<nickname>",
            description = "send someone a warm hug",
            handlerId = CommandHandlerId.Hug
        ),
        CommandDefinition(
            command = "/j",
            aliases = listOf("/join"),
            syntax = "<channel> [password]",
            description = "join or create a channel",
            handlerId = CommandHandlerId.Join
        ),
        CommandDefinition(
            command = "/m",
            aliases = listOf("/msg"),
            syntax = "<nickname> [message]",
            description = "start a private chat, optionally with a message",
            handlerId = CommandHandlerId.Message
        ),
        CommandDefinition(
            command = "/pass",
            syntax = "<password>",
            description = "change the channel password (channel creator)",
            handlerId = CommandHandlerId.Pass,
            channelOnly = true
        ),
        CommandDefinition(
            command = "/pay",
            syntax = "<token> [public]",
            description = "send a Cashu ecash token",
            handlerId = CommandHandlerId.Pay,
            hiddenInPublicGeohash = true
        ),
        CommandDefinition(
            command = "/slap",
            syntax = "<nickname>",
            description = "slap someone with a large trout",
            handlerId = CommandHandlerId.Slap
        ),
        CommandDefinition(
            command = "/sos",
            syntax = "[note]",
            description = "broadcast an SOS to everyone on the mesh (mesh only)",
            handlerId = CommandHandlerId.Sos,
            meshOnly = true
        ),
        CommandDefinition(
            command = "/transfer",
            syntax = "<nickname>",
            description = "transfer channel ownership (channel creator)",
            handlerId = CommandHandlerId.Transfer,
            channelOnly = true
        ),
        CommandDefinition(
            command = "/unblock",
            syntax = "<nickname>",
            description = "unblock a peer",
            handlerId = CommandHandlerId.Unblock
        ),
        CommandDefinition(
            command = "/w",
            aliases = listOf("/who"),
            description = "see who is online here",
            handlerId = CommandHandlerId.Who
        )
    )

    fun find(input: String): CommandDefinition? = definitions.firstOrNull { it.matches(input) }

    /** Commands offered in the current context, sorted for a stable autocomplete order. */
    fun available(context: CommandContext): List<CommandDefinition> =
        definitions.filter { it.availableIn(context) }.sortedBy { it.command }

    /**
     * `/help` output, generated from the same definitions the autocomplete shows, so it cannot
     * describe a command that does not exist or omit one that does.
     */
    fun helpText(context: CommandContext): String {
        val lines = available(context).map { it.helpLine() }
        return (listOf("commands:") + lines).joinToString("\n")
    }
}