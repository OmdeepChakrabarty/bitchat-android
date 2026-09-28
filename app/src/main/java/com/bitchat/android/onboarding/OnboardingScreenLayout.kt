package com.bitchat.android.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.bitchat.android.ui.theme.BitchatFontFamily
import com.bitchat.android.ui.theme.ChatVisualTokens
import com.bitchat.android.ui.theme.LocalBitchatPalette

/**
 * The shell every permission and setup screen sits in.
 *
 * One flat white sheet: a title, a short explanation, and a primary action. No cards, no
 * elevation, no illustration-sized glyphs. The screens differ in wording and in which action
 * they offer, not in layout, so the layout is written once.
 */
@Composable
internal fun OnboardingScreenLayout(
    title: String,
    modifier: Modifier = Modifier,
    body: @Composable ColumnScope.() -> Unit = {},
    primaryAction: (@Composable () -> Unit)? = null,
) {
    val palette = LocalBitchatPalette.current
    val colorScheme = MaterialTheme.colorScheme

    Column(modifier = modifier.fillMaxSize().background(colorScheme.background)) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontFamily = BitchatFontFamily,
                color = colorScheme.onSurface,
            )
            Spacer(Modifier.height(10.dp))
            HorizontalDivider(thickness = ChatVisualTokens.Hairline, color = palette.separator)
            Spacer(Modifier.height(14.dp))
            body()
        }

        primaryAction?.let { action ->
            HorizontalDivider(thickness = ChatVisualTokens.Hairline, color = palette.separator)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) { action() }
        }
    }
}

/**
 * The one button style on these screens: a flat, full-width blue action.
 *
 * M3's default button carries elevation and a pill shape on some versions; this is a plain
 * rounded rectangle that sits on the hairline above it.
 */
@Composable
internal fun OnboardingPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val palette = LocalBitchatPalette.current
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(6.dp),
        elevation = ButtonDefaults.buttonElevation(0.dp, 0.dp, 0.dp, 0.dp, 0.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = palette.tint,
            contentColor = androidx.compose.ui.graphics.Color.White,
        ),
        modifier = modifier.heightIn(min = 44.dp),
    ) {
        Text(text = text, fontFamily = BitchatFontFamily)
    }
}

/** A blue text action, for the secondary "not now" path. */
@Composable
internal fun OnboardingTextButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = LocalBitchatPalette.current
    TextButton(onClick = onClick, modifier = modifier.heightIn(min = 44.dp)) {
        Text(text = text, fontFamily = BitchatFontFamily, color = palette.tint)
    }
}

/** Small grey body copy under a title. */
@Composable
internal fun OnboardingBody(
    text: String,
    modifier: Modifier = Modifier,
) {
    val palette = LocalBitchatPalette.current
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        fontFamily = BitchatFontFamily,
        color = palette.textTertiary,
        textAlign = TextAlign.Start,
        modifier = modifier.fillMaxWidth(),
    )
}

/** A small, quiet status glyph. Deliberately not an illustration-sized icon. */
@Composable
internal fun OnboardingSpinner(
    modifier: Modifier = Modifier,
    size: androidx.compose.ui.unit.Dp = 28.dp,
) {
    val palette = LocalBitchatPalette.current
    androidx.compose.material3.CircularProgressIndicator(
        modifier = modifier.size(size),
        color = palette.tint,
        strokeWidth = 2.dp,
    )
}

/** A row of status lines, ruled like a settings list. */
@Composable
internal fun OnboardingStatusRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: androidx.compose.ui.graphics.Color? = null,
) {
    val palette = LocalBitchatPalette.current
    val colorScheme = MaterialTheme.colorScheme
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 36.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontFamily = BitchatFontFamily,
            color = colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontFamily = BitchatFontFamily,
            color = valueColor ?: palette.textTertiary,
            textAlign = TextAlign.End,
        )
    }
}

/** A clipped group of status rows with hairlines between them. */
@Composable
internal fun OnboardingStatusGroup(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val palette = LocalBitchatPalette.current
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
    ) {
        HorizontalDivider(thickness = ChatVisualTokens.Hairline, color = palette.separator)
        content()
        HorizontalDivider(thickness = ChatVisualTokens.Hairline, color = palette.separator)
    }
}
