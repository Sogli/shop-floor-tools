package com.livnica

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.livnica.ui.components.AutoSizeText

@Composable
fun BackgroundLayer(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.fillMaxSize()) {
        drawRect(color = THEME.bgDark)
        drawOval(
            color = THEME.accentPrimary.copy(alpha = 0.13f),
            topLeft = Offset(-size.width * 0.25f, size.height * 0.35f),
            size = Size(size.width * 1.1f, size.height * 0.7f)
        )
        drawOval(
            color = THEME.accentSecondary.copy(alpha = 0.10f),
            topLeft = Offset(size.width * 0.15f, -size.height * 0.2f),
            size = Size(size.width * 1.0f, size.height * 0.8f)
        )
    }
}

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    bgColor: Color = THEME.bgCard,
    borderColor: Color = THEME.glassBorder,
    radius: Dp = UI.cardRadius.dp,
    content: @Composable BoxScope.() -> Unit
) {
    val shape = RoundedCornerShape(radius)
    val background = bgColor.copy(alpha = bgColor.alpha * 0.9f)
    Box(
        modifier = modifier
            .shadow(6.dp, shape, ambientColor = THEME.shadow.copy(alpha = 0.33f))
            .background(background, shape)
            .border(1.2.dp, borderColor.copy(alpha = 0.25f), shape)
    ) {
        content()
    }
}

@Composable
fun RoundedButton(
    text: String,
    modifier: Modifier = Modifier,
    bgColor: Color,
    textColor: Color,
    borderColor: Color = THEME.glassBorder,
    radius: Dp = 12.dp,
    textStyle: TextStyle = MaterialTheme.typography.labelMedium,
    bold: Boolean = false,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(radius)
    Box(
        modifier = modifier
            .clip(shape)
            .background(if (enabled) bgColor else bgColor.copy(alpha = 0.6f))
            .border(1.dp, borderColor.copy(alpha = 0.5f), shape)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        val resolvedStyle = if (bold) {
            textStyle.copy(fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        } else {
            textStyle.copy(textAlign = TextAlign.Center)
        }
        Text(
            text = text,
            color = if (enabled) textColor else textColor.copy(alpha = 0.6f),
            style = resolvedStyle,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 2.dp)
        )
    }
}

@Composable
fun GlowButton(
    text: String,
    modifier: Modifier = Modifier,
    bgColor: Color,
    textColor: Color,
    radius: Dp = 14.dp,
    textStyle: TextStyle = MaterialTheme.typography.labelLarge,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(radius)
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val glowAlpha by animateFloatAsState(if (pressed) 0.4f else 0.0f, label = "glow")
    val elevation by animateDpAsState(if (pressed) 12.dp else 0.dp, label = "elev")

    Box(
        modifier = modifier
            .shadow(
                elevation = elevation,
                shape = shape,
                ambientColor = bgColor.copy(alpha = glowAlpha),
                spotColor = bgColor.copy(alpha = glowAlpha)
            )
            .clip(shape)
            .background(bgColor)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = textColor,
            style = textStyle.copy(fontWeight = FontWeight.Bold, textAlign = TextAlign.Center),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 2.dp)
        )
    }
}

@Composable
fun IconButton(
    text: String,
    modifier: Modifier = Modifier,
    bgColor: Color,
    textColor: Color,
    onClick: () -> Unit
) {
    RoundedButton(
        text = text,
        modifier = modifier,
        bgColor = bgColor,
        textColor = textColor,
        textStyle = MaterialTheme.typography.titleLarge,
        bold = true,
        onClick = onClick
    )
}

@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        color = THEME.textSecondary,
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
        modifier = modifier.fillMaxWidth(),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
}

@Composable
fun StatCard(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    val cardModifier = if (onClick != null) {
        modifier
            .height(UI.statCardHeight.dp)
            .clickable(onClick = onClick)
    } else {
        modifier.height(UI.statCardHeight.dp)
    }
    GlassCard(modifier = cardModifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = UI.cardPadding.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AutoSizeText(
                text = value,
                color = color,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                maxFontSize = MaterialTheme.typography.titleLarge.fontSize,
                minFontSize = MaterialTheme.typography.titleMedium.fontSize,
                maxLines = 1
            )
            AutoSizeText(
                text = label,
                color = THEME.textMuted,
                style = MaterialTheme.typography.labelMedium.copy(textAlign = TextAlign.Center),
                maxFontSize = MaterialTheme.typography.labelMedium.fontSize,
                minFontSize = 8.sp,
                maxLines = 1,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModernTextField(
    value: String,
    onValueChange: (String) -> Unit,
    hintText: String,
    keyboardOptions: KeyboardOptions,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        singleLine = true,
        textStyle = MaterialTheme.typography.bodyLarge.copy(
            color = THEME.textPrimary
        ),
        placeholder = {
            Text(
                text = hintText,
                color = THEME.textMuted,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        keyboardOptions = keyboardOptions,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = THEME.accentPrimary.copy(alpha = 0.63f),
            unfocusedBorderColor = THEME.glassBorder.copy(alpha = 0.63f),
            cursorColor = THEME.accentPrimary,
            focusedTextColor = THEME.textPrimary,
            unfocusedTextColor = THEME.textPrimary
        )
    )
}

