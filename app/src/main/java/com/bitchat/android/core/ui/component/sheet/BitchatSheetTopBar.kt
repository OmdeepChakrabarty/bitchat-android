package com.bitchat.android.core.ui.component.sheet

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bitchat.android.ui.ChatHeaderHeight
import com.bitchat.android.ui.HeaderInsetEnd
import com.bitchat.android.ui.HeaderInsetStart
import com.bitchat.android.ui.HeaderTapTarget
import com.bitchat.android.ui.HairlineThickness
import com.bitchat.android.ui.theme.BitchatFontFamily
import com.bitchat.android.ui.theme.LocalBitchatPalette
import com.bitchat.android.core.ui.component.button.CloseButton

/**
 * Compact sheet header.
 *
 * Built from a plain Row rather than `TopAppBar`, which imposes its own minimum height, a fixed
 * title inset and container colour. Everything the bar shows — height, insets, the rule beneath
 * it — is specified here so the sheet header and the chat header are the same object visually.
 */
@Composable
fun BitchatSheetTopBar(
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    backgroundAlpha: Float = 0.98f,
    title: @Composable () -> Unit,
    navigationIcon: (@Composable () -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {}
) {
    val palette = LocalBitchatPalette.current
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(palette.barBackground.copy(alpha = backgroundAlpha))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(ChatHeaderHeight)
                .padding(start = HeaderInsetStart, end = HeaderInsetEnd),
            verticalAlignment = Alignment.CenterVertically
        ) {
            navigationIcon?.let {
                Box(modifier = Modifier.size(HeaderTapTarget), contentAlignment = Alignment.Center) {
                    it()
                }
                Spacer(Modifier.width(4.dp))
            }
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                title()
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                content = actions
            )
            val dismiss = LocalSheetDismiss.current
            CloseButton(
                onClick = { dismiss?.invoke() ?: onClose() },
                modifier = Modifier.padding(start = 8.dp)
            )
        }
        HorizontalDivider(thickness = HairlineThickness, color = palette.separator)
    }
}
@Composable
fun BitchatSheetCenterTopBar(
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    backgroundAlpha: Float = 0.98f,
    title: @Composable () -> Unit,
    navigationIcon: (@Composable () -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {}
) {
    val palette = LocalBitchatPalette.current
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(palette.barBackground.copy(alpha = backgroundAlpha))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(ChatHeaderHeight)
                .padding(start = HeaderInsetStart, end = HeaderInsetEnd),
            contentAlignment = Alignment.Center
        ) {
            Box(Modifier.fillMaxWidth(0.5f), contentAlignment = Alignment.CenterStart) {
                navigationIcon?.invoke()
            }
            title()
            Row(
                modifier = Modifier.fillMaxWidth(0.5f),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
                content = actions
            )
            val dismiss = LocalSheetDismiss.current
            CloseButton(
                onClick = { dismiss?.invoke() ?: onClose() },
                modifier = Modifier.padding(start = 8.dp)
            )
        }
        HorizontalDivider(thickness = HairlineThickness, color = palette.separator)
    }
}

@Composable
fun BitchatSheetTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleLarge.copy(
            fontWeight = FontWeight.SemiBold,
            fontFamily = BitchatFontFamily
        ),
        color = MaterialTheme.colorScheme.onSurface,
        maxLines = 1
    )
}
