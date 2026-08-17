package com.example.racunanjekilaze.ui.sections

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.racunanjekilaze.R
import com.example.racunanjekilaze.ui.components.AutoSizeText
import com.example.racunanjekilaze.ui.theme.LayoutTokens
import com.example.racunanjekilaze.ui.theme.TextPrimary
import com.example.racunanjekilaze.ui.theme.TextSecondary

@Composable
fun HeaderSection(
    layout: LayoutTokens,
    modifier: Modifier = Modifier
) {
    val logoPainter = painterResource(id = R.drawable.logo)
    val logoMaxHeight = if (layout.isCompact) 68.dp else 84.dp
    val headerSpacing = if (layout.isCompact) 4.dp else 6.dp

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(headerSpacing),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painter = logoPainter,
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxWidth()
                .height(logoMaxHeight)
        )
        AutoSizeText(
            text = "Računanje kilaže",
            style = MaterialTheme.typography.titleLarge.copy(textAlign = TextAlign.Center),
            color = TextPrimary,
            maxFontSize = MaterialTheme.typography.titleLarge.fontSize,
            maxLines = 1,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = layout.screenPaddingHorizontal)
        )
        Text(
            text = "Kalkulator metraže i težine trake",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = layout.screenPaddingHorizontal)
        )
    }
}
