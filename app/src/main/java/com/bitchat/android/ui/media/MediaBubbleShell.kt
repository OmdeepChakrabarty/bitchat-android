package com.bitchat.android.ui.media

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bitchat.android.core.ui.component.text.AnnotatedClickableText
import com.bitchat.android.model.BitchatMessage
import com.bitchat.android.ui.DeliveryStatusIcon
import com.bitchat.android.ui.PeerAvatar
import com.bitchat.android.ui.formatTextMessageMetadata
import com.bitchat.android.ui.formatTextMessageSender
import com.bitchat.android.ui.isFromSelf
import com.bitchat.android.ui.peerIdentityForMessage
import com.bitchat.android.ui.theme.BitchatFontFamily
import com.bitchat.android.ui.theme.ChatVisualTokens
import com.bitchat.android.ui.theme.LocalBitchatPalette
import com.bitchat.android.ui.theme.MessageBubbleStyle
import com.bitchat.android.ui.theme.MessageSenderTextStyle
import com.bitchat.android.ui.theme.colorForPeer
import java.text.SimpleDateFormat

/**
 * Shell for media messages (images, voice notes) so they sit in the conversation exactly the way
 * a text bubble does: the same flat fill and tail from [MessageBubbleStyle], the sender's name
 * and avatar above the first message of a run, and the timestamp — plus delivery ticks on your
 * own private messages — riding flush-right beneath the media.
 *
 * The shell is long-clickable so media bubbles always open the message action sheet, even when
 * no sender label is shown and the media itself consumes touches (voice-note player controls).
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MediaBubbleShell(
    message: BitchatMessage,
    currentUserNickname: String,
    myPeerID: String,
    showSender: Boolean,
    timeFormatter: SimpleDateFormat,
    onNicknameClick: ((String) -> Unit)?,
    onLongPress: (() -> Unit)?,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val palette = LocalBitchatPalette.current
    val haptic = LocalHapticFeedback.current
    val isSelf = message.isFromSelf(currentUserNickname, myPeerID)

    val bubbleShape = MessageBubbleStyle.shape(isSelf)
    val bubbleFill = MessageBubbleStyle.fill(isSelf)
    val bodyColor = MessageBubbleStyle.contentColor(isSelf)
    val accentColor = if (isSelf) bubbleFill else colorForPeer(peerIdentityForMessage(message), palette)

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = if (isSelf) Alignment.End else Alignment.Start,
    ) {
        if (showSender && !isSelf) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(start = 2.dp, bottom = 2.dp)
            ) {
                PeerAvatar(
                    name = message.sender,
                    color = accentColor,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(Modifier.width(6.dp))
                val senderText = remember(message, currentUserNickname, myPeerID, palette) {
                    formatTextMessageSender(
                        message = message,
                        currentUserNickname = currentUserNickname,
                        myPeerID = myPeerID,
                        palette = palette,
                    )
                }
                AnnotatedClickableText(
                    text = senderText,
                    annotationTags = listOf("nickname_click"),
                    onAnnotationClick = { tag, item ->
                        if (tag == "nickname_click" && onNicknameClick != null) {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onNicknameClick.invoke(item)
                            true
                        } else {
                            false
                        }
                    },
                    onLongPress = { onLongPress?.invoke() },
                    fontFamily = BitchatFontFamily,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis,
                    style = MessageSenderTextStyle.copy(color = palette.textTertiary),
                )
            }
        }

        Box(
            modifier = Modifier
                .background(color = bubbleFill, shape = bubbleShape)
                .combinedClickable(
                    enabled = onLongPress != null,
                    onClick = {},
                    onLongClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onLongPress?.invoke()
                    },
                )
                .padding(
                    horizontal = ChatVisualTokens.BubblePaddingHorizontal,
                    vertical = ChatVisualTokens.BubblePaddingVertical,
                )
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                content()

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.align(Alignment.End),
                ) {
                    Text(
                        text = formatTextMessageMetadata(message, timeFormatter),
                        fontFamily = BitchatFontFamily,
                        fontSize = 10.sp,
                        color = bodyColor.copy(alpha = 0.75f),
                    )
                    if (isSelf && message.isPrivate) {
                        message.deliveryStatus?.let { status ->
                            Spacer(Modifier.width(3.dp))
                            DeliveryStatusIcon(status = status, tint = bodyColor)
                        }
                    }
                }
            }
        }
    }
}
