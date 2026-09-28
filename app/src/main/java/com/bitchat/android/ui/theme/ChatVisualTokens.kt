package com.bitchat.android.ui.theme

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * App-wide type family.
 *
 * Uses the platform sans-serif so no font binary ships in the APK and the app inherits the
 * device's native system face. This is the single source of truth: hundreds of call sites pass
 * `BitchatFontFamily` explicitly, so swapping the value here retunes all of them at once.
 */
internal val BitchatFontFamily: FontFamily = FontFamily.SansSerif

/**
 * Exact typography, spacing, and opacity values for the classic messenger surface.
 *
 * All chrome measurements live here so the conversation, the list, and the composer stay
 * dimensionally consistent — the whole design leans on a tight, flat, hairline-separated
 * layout, which only holds together if there is exactly one place that defines it.
 */
internal object ChatVisualTokens {
    // MARK: - Typography

    val MessageBodyFontSize: TextUnit = 15.sp
    val MessageBodyLineHeight: TextUnit = 19.sp
    val SenderFontSize: TextUnit = 12.sp
    val SenderLineHeight: TextUnit = 14.sp
    val SystemActionFontSize: TextUnit = 12.sp
    val SystemActionLineHeight: TextUnit = 15.sp
    val SystemTimeFontSize: TextUnit = 10.sp

    /** Secondary line under a list row / meta cluster. Small and grey, never louder. */
    val MetaFontSize: TextUnit = 12.sp
    val MetaLineHeight: TextUnit = 15.sp

    // MARK: - Transcript rhythm
    //
    // Consecutive messages from one sender sit tight together so a run reads as a single
    // group; a change of sender (or of day) opens a visible gap.

    /** Gap between consecutive messages from the same sender — a run reads as one block. */
    val MessageItemSpacing: Dp = 2.dp

    /** Gap above the first message of a new run, so separate thoughts visibly separate. */
    val NewGroupSpacing: Dp = 10.dp

    val SenderTopPadding: Dp = 8.dp
    val SenderToBodySpacing: Dp = 1.dp

    // MARK: - Bubble geometry

    /** Rounded corner on the three "free" corners of a message bubble. */
    val BubbleCornerRadius: Dp = 18.dp

    /** Tightened corner on the speaker's own side, giving the bubble its tail. */
    val BubbleTailRadius: Dp = 5.dp

    /** Padding inside a bubble, around the text. Deliberately snug. */
    val BubblePaddingHorizontal: Dp = 10.dp
    val BubblePaddingVertical: Dp = 6.dp

    /** A bubble never grows past this fraction of the list width, so long lines still wrap. */
    const val BubbleMaxWidthFraction: Float = 0.75f

    /** Breads are filled with a flat brand colour, not a tinted wash. */
    const val BubbleBackgroundAlpha: Float = 1.0f

    /** iOS bubbles carry no outline; the fill alone defines the shape. */
    const val BubbleBorderAlpha: Float = 0.0f

    /** Air under a bubble, reserved for the centered cluster timestamp. */
    val ClusterTimestampPadding: Dp = 6.dp

    const val SenderSuffixAlpha: Float = 0.60f
    const val HighlightAlpha: Float = 0.20f
    const val MutedTextAlpha: Float = 0.50f

    // MARK: - Chrome metrics
    //
    // A compact, fixed-height bar with a single hairline underneath — no elevation, no
    // shadow, no large title. These are shared by the nav bar and the composer so both
    // read as the same surface.

    /** The app's one separator weight: a true hairline, not a border. */
    val Hairline: Dp = 0.5.dp

    /** Height of the top navigation bar. */
    val NavBarHeight: Dp = 44.dp

    /** Height of the bottom composer. */
    val ComposerHeight: Dp = 40.dp

    /** Side gutter for list rows and the transcript. */
    val ScreenGutter: Dp = 12.dp

    /** The inset hairline at the left of a list divider. */
    val ListDividerInset: Dp = 52.dp

    /** Small leading glyph in a conversation row. */
    val ListRowGlyphSize: Dp = 34.dp

    /** Vertical padding inside a conversation row. */
    val ListRowVerticalPadding: Dp = 7.dp

    // MARK: - Composed styles

    val MessageBodyStyle = TextStyle(
        fontFamily = BitchatFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = MessageBodyFontSize,
        lineHeight = MessageBodyLineHeight,
    )

    /**
     * Sender name above a group. Small and semi-bold, in the tertiary grey rather than a
     * per-peer hue — colour-coding a person's name is not something the reference does, and
     * on a light bar it fights the row separators.
     */
    val SenderStyle = TextStyle(
        fontFamily = BitchatFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = SenderFontSize,
        lineHeight = SenderLineHeight,
    )

    val SystemActionStyle = TextStyle(
        fontFamily = BitchatFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = SystemActionFontSize,
        lineHeight = SystemActionLineHeight,
    )
}
