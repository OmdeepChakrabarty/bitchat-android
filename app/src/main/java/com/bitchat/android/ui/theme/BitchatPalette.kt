package com.bitchat.android.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Bitchat-specific color tokens that do not have a faithful Material 3 semantic role.
 *
 * Standard backgrounds, surfaces, text, outlines, primary/secondary accents, and errors belong
 * to [androidx.compose.material3.MaterialTheme.colorScheme]. Keeping only the extra app semantics
 * here lets Material components inherit correct defaults without losing Bitchat's identity.
 */
@Immutable
data class BitchatPalette(
    // MARK: - Form controls
    /**
     * Resting border for text inputs. Deliberately a neutral grey rather than the green-tinted
     * Material outline: the composer is the one surface the user stares at while typing.
     */
    val inputOutline: Color,
    /** Border for a focused text input. A step brighter, still neutral. */
    val inputOutlineFocused: Color,
    /**
     * Fill for text inputs. Near-black / near-white and completely untinted, for the same reason
     * as [inputOutline] — and because the composer sits on top of a green-tinted scrim, so any
     * tint of its own compounds into something muddy.
     */
    val inputSurface: Color,
    /** Fill for a focused text input. A barely perceptible lift. */
    val inputSurfaceFocused: Color,
    /** Resting disc behind the composer's action glyphs. Neutral grey. */
    val inputButton: Color,

    // MARK: - Message bubbles
    /** Fill for your own outgoing bubbles. */
    val bubbleOutgoing: Color,
    /** Body text inside an outgoing bubble. */
    val bubbleOutgoingText: Color,
    /** Fill for incoming bubbles. */
    val bubbleIncoming: Color,
    /** Body text inside an incoming bubble. */
    val bubbleIncomingText: Color,

    // MARK: - Chrome
    /**
     * The single hairline that separates a bar from its content, and list rows from each
     * other. One token, so every divider in the app is the same weight and colour.
     */
    val separator: Color,
    /** Background behind the nav bar and the composer. */
    val barBackground: Color,
    /** Fill for the composer's rounded text field. */
    val composerField: Color,
    /** Tappable blue: nav-bar actions, links, the Send button when armed. */
    val tint: Color,
    /** The unread dot on a conversation row. */
    val unreadDot: Color,
    /** Full-bleed red behind a swiped-away list row. */
    val destructive: Color,

    // MARK: - Extra semantics
    /** Timestamps, placeholders, section labels, disabled states. */
    val textTertiary: Color,
    /** Self, mentions targeting you, unread DMs. */
    val accentOrange: Color,
    /** Nostr reachability. */
    val accentPurple: Color,

    // MARK: - Deterministic peer colors
    /**
     * Saturation/value applied after deriving a peer's stable hue. Swap this when adding a
     * new theme — see [PeerColorStyle] for contrast guidelines.
     */
    val peerColors: PeerColorStyle,
)

val DarkBitchatPalette = BitchatPalette(
    inputOutline = Color(0xFF3A3A3C),
    inputOutlineFocused = Color(0xFF48484A),
    inputSurface = Color(0xFF1C1C1E),
    inputSurfaceFocused = Color(0xFF2C2C2E),
    inputButton = Color(0xFF2C2C2E),
    bubbleOutgoing = Color(0xFF0A84FF),
    bubbleOutgoingText = Color(0xFFFFFFFF),
    bubbleIncoming = Color(0xFF26262A),
    bubbleIncomingText = Color(0xFFFFFFFF),
    separator = Color(0xFF38383A),
    barBackground = Color(0xFF1C1C1E),
    composerField = Color(0xFF2C2C2E),
    tint = Color(0xFF0A84FF),
    unreadDot = Color(0xFF0A84FF),
    destructive = Color(0xFFFF3B30),
    textTertiary = Color(0xFF98989D),
    accentOrange = Color(0xFFFF9F0A),
    accentPurple = Color(0xFFBF5AF2),
    peerColors = PeerColorStyle.Dark,
)

val LightBitchatPalette = BitchatPalette(
    inputOutline = Color(0xFFBFBFBF),
    inputOutlineFocused = Color(0xFF9A9A9A),
    inputSurface = Color(0xFFFFFFFF),
    inputSurfaceFocused = Color(0xFFF2F2F2),
    inputButton = Color(0xFFEDEDED),
    bubbleOutgoing = Color(0xFF1B8AFB),
    bubbleOutgoingText = Color(0xFFFFFFFF),
    bubbleIncoming = Color(0xFFE5E5EA),
    bubbleIncomingText = Color(0xFF000000),
    separator = Color(0xFFC8C7CC),
    barBackground = Color(0xFFF9F9F9),
    composerField = Color(0xFFFFFFFF),
    tint = Color(0xFF007AFF),
    unreadDot = Color(0xFF007AFF),
    destructive = Color(0xFFFF3B30),
    textTertiary = Color(0xFF8E8E93),
    accentOrange = Color(0xFFFF9500),
    accentPurple = Color(0xFFAF52DE),
    peerColors = PeerColorStyle.Light,
)

val LocalBitchatPalette = staticCompositionLocalOf { DarkBitchatPalette }

/**
 * Motion tokens. The redesign leans on short, snappy transitions: long durations read as
 * sluggish on a chat surface where the user is scanning quickly.
 */
object BitchatMotion {
    /** Icon tints, text colors, small fills. */
    const val QUICK_MS = 120

    /** Tab indicators, pill growth, chip reveals. */
    const val STANDARD_MS = 180

    /** Sheet-level fades and scroll-driven top bars. */
    const val EMPHASIZED_MS = 240
}
