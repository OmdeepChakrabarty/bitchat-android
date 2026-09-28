package com.bitchat.android.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp

/**
 * The single definition of what a message bubble looks like.
 *
 * Text and media bubbles used to compute their own shape and fill independently, which meant
 * any change had to be made twice and the two drifted. Everything visual about a bubble now
 * comes from here: geometry, fill, and text tone.
 *
 * The shape is asymmetric — three generously rounded corners and one tightened corner on the
 * speaker's own side, which reads as the tail. Which corner is tight flips with the sender, so
 * outgoing bubbles point right and incoming ones point left.
 */
object MessageBubbleStyle {
    /** Shape for a bubble, with the tail on the speaker's side. */
    @Composable
    @ReadOnlyComposable
    fun shape(isSelf: Boolean): Shape = bubbleShape(
        corner = ChatVisualTokens.BubbleCornerRadius,
        tail = ChatVisualTokens.BubbleTailRadius,
        isSelf = isSelf,
    )

    /** Flat fill: your own brand blue, everyone else's neutral grey. */
    @Composable
    @ReadOnlyComposable
    fun fill(isSelf: Boolean): Color {
        val palette = LocalBitchatPalette.current
        return if (isSelf) palette.bubbleOutgoing else palette.bubbleIncoming
    }

    /** Body text reads white on your own bubbles, black on theirs. */
    @Composable
    @ReadOnlyComposable
    fun contentColor(isSelf: Boolean): Color {
        val palette = LocalBitchatPalette.current
        return if (isSelf) palette.bubbleOutgoingText else palette.bubbleIncomingText
    }
}

/** Shape-only form, so the layout maths below can compute geometry outside composition. */
internal fun bubbleShape(corner: Dp, tail: Dp, isSelf: Boolean): Shape =
    if (isSelf) {
        RoundedCornerShape(topStart = corner, topEnd = corner, bottomEnd = tail, bottomStart = corner)
    } else {
        RoundedCornerShape(topStart = corner, topEnd = corner, bottomEnd = corner, bottomStart = tail)
    }
