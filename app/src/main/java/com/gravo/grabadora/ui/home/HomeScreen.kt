package com.gravo.grabadora.ui.home

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.List
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.gravo.grabadora.R
import com.gravo.grabadora.audio.BitDepth
import com.gravo.grabadora.audio.LevelMeter
import com.gravo.grabadora.audio.RecStatus
import com.gravo.grabadora.audio.RecordFormat
import com.gravo.grabadora.ui.components.LevelMeterBars
import com.gravo.grabadora.ui.components.RecordButton
import com.gravo.grabadora.ui.components.SegChip
import com.gravo.grabadora.ui.theme.Accent
import com.gravo.grabadora.ui.theme.DmMono
import com.gravo.grabadora.ui.theme.DmSans
import com.gravo.grabadora.ui.theme.GrabadoraTheme
import com.gravo.grabadora.ui.theme.RecordRed
import com.gravo.grabadora.ui.theme.SpaceGrotesk
import com.gravo.grabadora.ui.theme.Yellow
import com.gravo.grabadora.util.TimeFormat
import kotlin.math.roundToInt

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onOpenLibrary: () -> Unit,
    onOpenSettings: () -> Unit,
    onRecordingSaved: (Long) -> Unit,
) {
    val colors = GrabadoraTheme.colors
    val context = LocalContext.current
    val settings by viewModel.settings.collectAsState()
    val status by viewModel.status.collectAsState()
    val elapsedMs by viewModel.elapsedMs.collectAsState()
    val levels by viewModel.levels.collectAsState()
    val peakDb by viewModel.peakDb.collectAsState()
    var pressing by remember { mutableStateOf(false) }

    var hasMicPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                android.content.pm.PackageManager.PERMISSION_GRANTED,
        )
    }
    var permissionDenied by remember { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { result ->
        hasMicPermission = result[Manifest.permission.RECORD_AUDIO] == true
        permissionDenied = !hasMicPermission
        if (hasMicPermission) viewModel.startMonitoring()
    }

    fun requestPermissions() {
        val perms = mutableListOf(Manifest.permission.RECORD_AUDIO)
        if (Build.VERSION.SDK_INT >= 33) perms += Manifest.permission.POST_NOTIFICATIONS
        permissionLauncher.launch(perms.toTypedArray())
    }

    LifecycleResumeEffect(hasMicPermission) {
        if (hasMicPermission) viewModel.startMonitoring()
        onPauseOrDispose { if (hasMicPermission) viewModel.stopMonitoring() }
    }

    val defaultNameTemplate = stringResource(R.string.recording_default_name)

    Column(
        Modifier
            .fillMaxSize()
            .background(colors.bg)
            .safeDrawingPadding()
            .padding(horizontal = 22.dp)
            .padding(top = 8.dp, bottom = 22.dp),
    ) {
        // cabecera
        Row(
            Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                Modifier.clickable(remember { MutableInteractionSource() }, null) { onOpenLibrary() }
                    .padding(vertical = 6.dp, horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(9.dp),
            ) {
                Icon(Icons.AutoMirrored.Outlined.List, null, Modifier.size(22.dp), tint = colors.fg1)
                Text(
                    stringResource(R.string.home_library),
                    style = TextStyle(fontFamily = DmSans, fontWeight = FontWeight.Medium, fontSize = 13.sp, color = colors.fg2),
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                IconSquare(
                    icon = { Icon(if (settings.darkTheme) Icons.Outlined.LightMode else Icons.Outlined.DarkMode, stringResource(R.string.cd_theme_toggle), Modifier.size(19.dp), tint = colors.fg2) },
                    onClick = { viewModel.toggleTheme() },
                )
                IconSquare(
                    icon = { Icon(Icons.Outlined.Settings, stringResource(R.string.cd_settings), Modifier.size(20.dp), tint = colors.fg2) },
                    onClick = onOpenSettings,
                )
            }
        }

        // estado + cronómetro
        Column(
            Modifier.fillMaxWidth().padding(top = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            val (label, color) = when {
                !hasMicPermission && permissionDenied -> stringResource(R.string.home_status_permission) to RecordRed
                status == RecStatus.RECORDING -> stringResource(R.string.home_status_recording) to RecordRed
                status == RecStatus.PAUSED -> stringResource(R.string.home_status_paused) to Yellow
                else -> stringResource(R.string.home_status_idle) to colors.fg3
            }
            Text(
                label.uppercase(),
                style = TextStyle(fontFamily = DmMono, fontSize = 11.sp, letterSpacing = 2.sp, color = color),
            )
            Text(
                TimeFormat.mmss(elapsedMs / 1000),
                style = TextStyle(
                    fontFamily = DmMono, fontWeight = FontWeight.Medium, fontSize = 52.sp,
                    letterSpacing = (-1).sp, color = colors.fg1,
                ),
            )
        }

        // audímetro
        Column(
            Modifier
                .fillMaxWidth()
                .padding(top = 20.dp)
                .background(colors.sf1, RoundedCornerShape(18.dp))
                .border(1.dp, colors.bd2, RoundedCornerShape(18.dp))
                .padding(horizontal = 18.dp)
                .padding(top = 20.dp, bottom = 16.dp),
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    stringResource(if (status == RecStatus.IDLE) R.string.home_meter_monitor else R.string.home_meter_title).uppercase(),
                    style = TextStyle(fontFamily = DmMono, fontSize = 10.sp, letterSpacing = 1.4.sp, color = colors.fg3),
                )
                Text(
                    "${peakDb.roundToInt()} dB",
                    style = TextStyle(
                        fontFamily = DmMono, fontWeight = FontWeight.Medium, fontSize = 11.sp,
                        color = if (status != RecStatus.IDLE && peakDb > -4f) RecordRed else colors.fg3,
                    ),
                )
            }
            LevelMeterBars(levels, Modifier.fillMaxWidth().height(84.dp).padding(top = 12.dp), muted = status == RecStatus.IDLE)
            Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                for (mark in listOf("-60", "-24", "-12", "-6")) {
                    Text(mark, style = TextStyle(fontFamily = DmMono, fontSize = 9.sp, color = colors.fg4))
                }
                Text("0dB", style = TextStyle(fontFamily = DmMono, fontSize = 9.sp, color = Accent))
            }
        }

        // controles rápidos
        Column(Modifier.fillMaxWidth().padding(top = 18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Column {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(
                        stringResource(R.string.home_gain),
                        style = TextStyle(fontFamily = DmSans, fontWeight = FontWeight.Medium, fontSize = 12.sp, color = colors.fg2),
                    )
                    val db = com.gravo.grabadora.audio.RecordingSpec.sliderToDb(settings.gainSlider)
                    Text(
                        (if (db > 0) "+" else "") + "${db.roundToInt()} dB",
                        style = TextStyle(fontFamily = DmMono, fontWeight = FontWeight.Medium, fontSize = 12.sp, color = Accent),
                    )
                }
                Slider(
                    value = settings.gainSlider.toFloat(),
                    onValueChange = { viewModel.setGainSlider(it.roundToInt()) },
                    valueRange = 0f..100f,
                    colors = SliderDefaults.colors(thumbColor = Accent, activeTrackColor = Accent, inactiveTrackColor = colors.bd4),
                )
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(Modifier.weight(1f)) {
                    Text(
                        stringResource(R.string.home_depth),
                        style = TextStyle(fontFamily = DmSans, fontWeight = FontWeight.Medium, fontSize = 12.sp, color = colors.fg2),
                        modifier = Modifier.padding(bottom = 8.dp),
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        for (depth in BitDepth.entries) {
                            SegChip(
                                label = "${depth.bits}b",
                                active = settings.depth == depth,
                                onClick = { viewModel.setDepth(depth) },
                                modifier = Modifier.weight(1f),
                                horizontalPadding = 4,
                                verticalPadding = 8,
                            )
                        }
                    }
                }
                Column {
                    Text(
                        stringResource(R.string.home_format),
                        style = TextStyle(fontFamily = DmSans, fontWeight = FontWeight.Medium, fontSize = 12.sp, color = colors.fg2),
                        modifier = Modifier.padding(bottom = 8.dp),
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        for (format in listOf(RecordFormat.WAV, RecordFormat.MP3, RecordFormat.M4A)) {
                            SegChip(
                                label = format.label,
                                active = settings.format == format,
                                onClick = { viewModel.setFormat(format) },
                                horizontalPadding = 10,
                                verticalPadding = 8,
                            )
                        }
                    }
                }
            }
        }

        Spacer(Modifier.weight(1f))

        // botón de grabar + hint
        Column(
            Modifier.fillMaxWidth().padding(top = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            RecordButton(
                status = status,
                onTap = {
                    if (!hasMicPermission) requestPermissions() else viewModel.onRecordTap()
                },
                onHoldComplete = { viewModel.onRecordHoldComplete(defaultNameTemplate, onRecordingSaved) },
                onPressingChange = { pressing = it },
            )
            val hint = when {
                !hasMicPermission && permissionDenied -> stringResource(R.string.home_permission_rationale)
                pressing -> stringResource(R.string.home_hint_releasing)
                status == RecStatus.RECORDING -> stringResource(R.string.home_hint_recording)
                status == RecStatus.PAUSED -> stringResource(R.string.home_hint_paused)
                else -> stringResource(R.string.home_hint_idle)
            }
            Text(
                hint,
                style = TextStyle(fontFamily = DmSans, fontSize = 11.sp, color = colors.fg3),
                textAlign = TextAlign.Center,
            )
            if (!hasMicPermission && permissionDenied) {
                Text(
                    stringResource(R.string.home_permission_open_settings),
                    style = TextStyle(fontFamily = DmSans, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = Accent),
                    modifier = Modifier.clickable {
                        context.startActivity(
                            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null)),
                        )
                    },
                )
            }
        }
    }
}

@Composable
private fun IconSquare(icon: @Composable () -> Unit, onClick: () -> Unit) {
    val colors = GrabadoraTheme.colors
    Box(
        Modifier
            .size(40.dp)
            .background(colors.sf2, RoundedCornerShape(12.dp))
            .border(1.dp, colors.bd3, RoundedCornerShape(12.dp))
            .clickable(remember { MutableInteractionSource() }, null, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { icon() }
}
