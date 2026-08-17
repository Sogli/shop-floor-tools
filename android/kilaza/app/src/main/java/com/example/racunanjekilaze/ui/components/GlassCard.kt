package com.example.racunanjekilaze.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.racunanjekilaze.ui.theme.CopperShadow
import com.example.racunanjekilaze.ui.theme.GlassBorder
import com.example.racunanjekilaze.ui.theme.Surface

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    bgColor: Color = Surface,
    borderColor: Color = GlassBorder,
    cornerRadius: Dp = 16.dp,
    elevation: Dp = 8.dp,
    content: @Composable BoxScope.() -> Unit
) {
    val shape = RoundedCornerShape(cornerRadius)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = elevation,
                shape = shape,
                ambientColor = CopperShadow,
                spotColor = CopperShadow
            )
            .clip(shape)
            .background(bgColor.copy(alpha = 0.95f))
            .border(
                width = 1.dp,
                color = borderColor.copy(alpha = 0.3f),
                shape = shape
            ),
        content = content
    )
}

@Composable
fun AccentGlassCard(
    modifier: Modifier = Modifier,
    bgColor: Color,
    cornerRadius: Dp = 16.dp,
    elevation: Dp = 10.dp,
    content: @Composable BoxScope.() -> Unit
) {
    val shape = RoundedCornerShape(cornerRadius)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = elevation,
                shape = shape,
                ambientColor = CopperShadow,
                spotColor = CopperShadow
            )
            .clip(shape)
            .background(bgColor),
        content = content
    )
}
