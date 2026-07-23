package com.gravo.grabadora.ui.settings

import android.os.Build
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gravo.grabadora.R
import com.gravo.grabadora.audio.BitDepth
import com.gravo.grabadora.audio.MicSelector
import com.gravo.grabadora.audio.RecordFormat
import com.gravo.grabadora.audio.RecordingSpec
import com.gravo.grabadora.data.settings.SyncProtocol
import com.gravo.grabadora.ui.components.BackButton
import com.gravo.grabadora.ui.components.GravoSwitch
import com.gravo.grabadora.ui.components.SegChip
import com.gravo.grabadora.ui.theme.Accent
import com.gravo.grabadora.ui.theme.DmMono
import com.gravo.grabadora.ui.theme.DmSans
import com.gravo.grabadora.ui.theme.GrabadoraTheme
import com.gravo.grabadora.ui.theme.RecordRed
import com.gravo.grabadora.ui.theme.SpaceGrotesk
import com.gravo.grabadora.ui.theme.WaveGreen
import kotlin.math.roundToInt

@Composable
fun SettingsScreen(viewModel: SettingsViewModel, onBack: () -> Unit) {
    val colors = GrabadoraTheme.colors
    val settings by viewModel.settings.collectAsState()
    val syncTest by viewModel.syncTest.collectAsState()

    Column(Modifier.fillMaxSize().background(colors.bg).safeDrawingPadding()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 22.dp).padding(top = 14.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            BackButton(onBack)
            Text(
                stringResource(R.string.settings_title),
                style = TextStyle(fontFamily = SpaceGrotesk, fontWeight = FontWeight.SemiBold, fontSize = 20.sp, color = colors.fg1),
            )
        }

        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(top = 10.dp, bottom = 30.dp),
            verticalArrangement = Arrangement.spacedBy(22.dp),
        ) {
            // ============ GRABACIÓN ============
            Section(stringResource(R.string.settings_section_recording)) {
                SettingBlock(divider = true) {
                    Label(stringResource(R.string.settings_format))
                    Row(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        for (format in RecordFormat.entries) {
                            // OGG/Opus requiere API 29
                            if (format == RecordFormat.OGG && Build.VERSION.SDK_INT < 29) continue
                            SegChip(format.label, settings.format == format, { viewModel.setFormat(format) }, horizontalPadding = 11)
                        }
                    }
                }
                SettingBlock(divider = true) {
                    Label(stringResource(R.string.settings_depth))
                    if (settings.format.isLossy) {
                        Sub(stringResource(R.string.settings_depth_na))
                    } else {
                        Row(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            for (depth in BitDepth.entries) {
                                val effective = RecordingSpec.effectiveDepth(settings.format, depth)
                                SegChip(
                                    depth.label + if (effective != depth) " → ${effective.bits}" else "",
                                    settings.depth == depth,
                                    { viewModel.setDepth(depth) },
                                    modifier = Modifier.weight(1f),
                                    horizontalPadding = 4,
                                    verticalPadding = 10,
                                )
                            }
                        }
                    }
                }
                SettingBlock(divider = true) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Label(stringResource(R.string.settings_gain))
                        val db = RecordingSpec.sliderToDb(settings.gainSlider)
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
                var showMics by rememberSaveable { mutableStateOf(false) }
                SettingBlock(divider = true, onClick = { showMics = !showMics }) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Label(stringResource(R.string.settings_mic))
                            Sub(
                                viewModel.mics.firstOrNull { it.id == settings.micId }?.name
                                    ?: stringResource(R.string.settings_mic_builtin),
                            )
                        }
                        Icon(Icons.Default.ChevronRight, null, Modifier.size(18.dp), tint = colors.fg3)
                    }
                    if (showMics) {
                        Column(Modifier.padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            for (mic in listOf(com.gravo.grabadora.audio.MicOption(MicSelector.DEFAULT_ID, stringResource(R.string.settings_mic_builtin))) + viewModel.mics) {
                                Row(
                                    Modifier
                                        .fillMaxWidth()
                                        .background(
                                            if (settings.micId == mic.id) Accent.copy(alpha = 0.14f) else colors.sf2,
                                            RoundedCornerShape(8.dp),
                                        )
                                        .clickable { viewModel.setMicId(mic.id); showMics = false }
                                        .padding(10.dp),
                                ) {
                                    Text(
                                        mic.name,
                                        style = TextStyle(
                                            fontFamily = DmSans, fontSize = 12.sp,
                                            color = if (settings.micId == mic.id) Accent else colors.fg2,
                                        ),
                                    )
                                }
                            }
                        }
                    }
                }
                SettingBlock(divider = false, onClick = { viewModel.setStereo(!settings.stereo) }) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Label(stringResource(R.string.settings_stereo))
                            Sub(stringResource(R.string.settings_stereo_sub))
                        }
                        GravoSwitch(settings.stereo, { viewModel.setStereo(!settings.stereo) })
                    }
                }
            }

            // ============ INTERFAZ ============
            Section(stringResource(R.string.settings_section_interface)) {
                SettingBlock(divider = true) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Label(stringResource(R.string.settings_language))
                        Spacer(Modifier.weight(1f))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            SegChip("Español", settings.language == "es", { viewModel.setLanguage("es") }, mono = false)
                            SegChip("English", settings.language == "en", { viewModel.setLanguage("en") }, mono = false)
                        }
                    }
                }
                SettingBlock(divider = true) {
                    Label(stringResource(R.string.settings_theme))
                    Row(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        SegChip(stringResource(R.string.settings_theme_dark), settings.darkTheme, { viewModel.setDarkTheme(true) }, mono = false)
                        SegChip(stringResource(R.string.settings_theme_light), !settings.darkTheme, { viewModel.setDarkTheme(false) }, mono = false)
                    }
                }
                SettingBlock(divider = true, onClick = { viewModel.setHideNotification(!settings.hideNotification) }) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f).padding(end = 14.dp)) {
                            Label(stringResource(R.string.settings_hide_notification))
                            Sub(stringResource(R.string.settings_hide_notification_sub))
                        }
                        GravoSwitch(settings.hideNotification, { viewModel.setHideNotification(!settings.hideNotification) })
                    }
                }
                SettingBlock(divider = false, onClick = { viewModel.setAutoStart(!settings.autoStartRecording) }) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Label(stringResource(R.string.settings_autostart))
                            Sub(stringResource(R.string.settings_autostart_sub))
                        }
                        GravoSwitch(settings.autoStartRecording, { viewModel.setAutoStart(!settings.autoStartRecording) })
                    }
                }
            }

            // ============ SINCRONIZAR ============
            Column {
                Section(stringResource(R.string.settings_section_sync)) {
                    SettingBlock(divider = true) {
                        Label(stringResource(R.string.settings_sync_protocol))
                        Row(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            for (protocol in SyncProtocol.entries) {
                                SegChip(protocol.label, settings.syncProtocol == protocol, { viewModel.setSyncProtocol(protocol) }, horizontalPadding = 14)
                            }
                        }
                    }
                    SettingBlock(divider = true) {
                        var server by remember(settings.syncServer) { mutableStateOf(settings.syncServer) }
                        Label(stringResource(R.string.settings_sync_server))
                        SyncField(
                            value = server,
                            onValueChange = { server = it },
                            onDone = { viewModel.setSyncServer(server) },
                            placeholder = stringResource(R.string.settings_sync_server_hint),
                        )
                    }
                    SettingBlock(divider = true) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Column(Modifier.weight(1f)) {
                                var user by remember(settings.syncUser) { mutableStateOf(settings.syncUser) }
                                Label(stringResource(R.string.settings_sync_user))
                                SyncField(user, { user = it }, { viewModel.setSyncUser(user) })
                            }
                            Column(Modifier.weight(1f)) {
                                var pass by remember { mutableStateOf("") }
                                val hasPassword by viewModel.hasPassword.collectAsState()
                                Label(stringResource(R.string.settings_sync_password))
                                SyncField(
                                    pass, { pass = it }, { viewModel.setSyncPassword(pass) },
                                    password = true,
                                    placeholder = if (hasPassword) "••••••••" else "",
                                )
                            }
                        }
                    }
                    SettingBlock(divider = true) {
                        var folder by remember(settings.syncFolder) { mutableStateOf(settings.syncFolder) }
                        Label(stringResource(R.string.settings_sync_folder))
                        SyncField(folder, { folder = it }, { viewModel.setSyncFolder(folder) })
                    }
                    SettingBlock(divider = false, onClick = { viewModel.setAutoUpload(!settings.autoUpload) }) {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Label(stringResource(R.string.settings_sync_autoupload))
                                Sub(stringResource(R.string.settings_sync_autoupload_sub))
                            }
                            GravoSwitch(settings.autoUpload, { viewModel.setAutoUpload(!settings.autoUpload) })
                        }
                    }
                }
                // probar conexión
                Box(
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                        .background(Accent.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                        .border(1.dp, Accent.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
                        .clickable(remember { MutableInteractionSource() }, null) { viewModel.testConnection() }
                        .padding(14.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    val label = when (syncTest) {
                        SyncTestState.Testing -> stringResource(R.string.settings_sync_testing)
                        SyncTestState.Ok -> stringResource(R.string.settings_sync_ok)
                        is SyncTestState.Error -> stringResource(
                            R.string.settings_sync_error,
                            (syncTest as SyncTestState.Error).message,
                        )
                        else -> stringResource(R.string.settings_sync_test)
                    }
                    val color = when (syncTest) {
                        SyncTestState.Ok -> WaveGreen
                        is SyncTestState.Error -> RecordRed
                        else -> Accent
                    }
                    Text(
                        label,
                        style = TextStyle(fontFamily = DmSans, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = color),
                    )
                }
            }
        }
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    val colors = GrabadoraTheme.colors
    Column {
        Text(
            title.uppercase(),
            style = TextStyle(fontFamily = DmMono, fontSize = 10.sp, letterSpacing = 1.6.sp, color = Accent),
            modifier = Modifier.padding(bottom = 12.dp),
        )
        Column(
            Modifier
                .fillMaxWidth()
                .background(colors.sf1, RoundedCornerShape(14.dp))
                .border(1.dp, colors.bd2, RoundedCornerShape(14.dp)),
        ) {
            content()
        }
    }
}

@Composable
private fun SettingBlock(divider: Boolean, onClick: (() -> Unit)? = null, content: @Composable () -> Unit) {
    val colors = GrabadoraTheme.colors
    var modifier: Modifier = Modifier.fillMaxWidth()
    if (onClick != null) {
        modifier = modifier.clickable(remember { MutableInteractionSource() }, null, onClick = onClick)
    }
    Column(modifier.padding(horizontal = 16.dp, vertical = 14.dp)) { content() }
    if (divider) {
        Box(Modifier.fillMaxWidth().padding(0.dp).height(1.dp).background(colors.bd1))
    }
}

@Composable
private fun Label(text: String) {
    val colors = GrabadoraTheme.colors
    Text(text, style = TextStyle(fontFamily = DmSans, fontWeight = FontWeight.Medium, fontSize = 13.sp, color = colors.fg1))
}

@Composable
private fun Sub(text: String) {
    val colors = GrabadoraTheme.colors
    Text(
        text,
        style = TextStyle(fontFamily = DmSans, fontSize = 11.sp, lineHeight = 15.sp, color = colors.fg3),
        modifier = Modifier.padding(top = 2.dp),
    )
}

@Composable
private fun SyncField(
    value: String,
    onValueChange: (String) -> Unit,
    onDone: () -> Unit,
    password: Boolean = false,
    placeholder: String = "",
) {
    val colors = GrabadoraTheme.colors
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        visualTransformation = if (password) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
        keyboardOptions = KeyboardOptions(keyboardType = if (password) KeyboardType.Password else KeyboardType.Uri),
        placeholder = {
            Text(placeholder, style = TextStyle(fontFamily = DmMono, fontSize = 12.sp, color = colors.fg4))
        },
        textStyle = TextStyle(fontFamily = DmMono, fontSize = 12.sp, color = colors.fg1),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Accent,
            unfocusedBorderColor = colors.bd4,
            cursorColor = Accent,
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
            .onFocusChanged { if (!it.isFocused) onDone() },
    )
}
