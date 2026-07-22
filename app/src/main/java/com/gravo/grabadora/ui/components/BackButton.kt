package com.gravo.grabadora.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.gravo.grabadora.R
import com.gravo.grabadora.ui.theme.GrabadoraTheme

@Composable
fun BackButton(onClick: () -> Unit, close: Boolean = false) {
    val colors = GrabadoraTheme.colors
    Box(
        Modifier
            .size(38.dp)
            .background(colors.sf2, RoundedCornerShape(11.dp))
            .border(1.dp, colors.bd3, RoundedCornerShape(11.dp))
            .clickable(remember { MutableInteractionSource() }, null, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            if (close) Icons.Default.Close else Icons.AutoMirrored.Filled.ArrowBack,
            stringResource(if (close) R.string.cd_close else R.string.cd_back),
            Modifier.size(20.dp),
            tint = colors.fg2,
        )
    }
}
