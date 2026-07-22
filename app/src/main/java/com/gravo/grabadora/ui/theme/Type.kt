package com.gravo.grabadora.ui.theme

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import com.gravo.grabadora.R

@OptIn(androidx.compose.ui.text.ExperimentalTextApi::class)
private fun variable(resId: Int, weight: FontWeight) = Font(
    resId = resId,
    weight = weight,
    variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight)),
)

val SpaceGrotesk = FontFamily(
    variable(R.font.space_grotesk, FontWeight.Normal),
    variable(R.font.space_grotesk, FontWeight.Medium),
    variable(R.font.space_grotesk, FontWeight.SemiBold),
    variable(R.font.space_grotesk, FontWeight.Bold),
)

val DmSans = FontFamily(
    variable(R.font.dm_sans, FontWeight.Normal),
    variable(R.font.dm_sans, FontWeight.Medium),
    variable(R.font.dm_sans, FontWeight.SemiBold),
    variable(R.font.dm_sans, FontWeight.Bold),
)

val DmMono = FontFamily(
    Font(R.font.dm_mono_light, FontWeight.Light),
    Font(R.font.dm_mono_regular, FontWeight.Normal),
    Font(R.font.dm_mono_medium, FontWeight.Medium),
)
