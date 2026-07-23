package com.gravo.grabadora.ui.detail

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.gravo.grabadora.R
import com.gravo.grabadora.ui.components.BackButton
import com.gravo.grabadora.ui.components.TagChip
import com.gravo.grabadora.ui.components.WaveformView
import com.gravo.grabadora.ui.theme.Accent
import com.gravo.grabadora.ui.theme.DmMono
import com.gravo.grabadora.ui.theme.DmSans
import com.gravo.grabadora.ui.theme.GrabadoraTheme
import com.gravo.grabadora.ui.theme.SpaceGrotesk
import com.gravo.grabadora.ui.theme.Yellow
import com.gravo.grabadora.util.RelativeDate
import com.gravo.grabadora.util.TimeFormat

@Composable
fun DetailScreen(
    viewModel: DetailViewModel,
    onBack: () -> Unit,
    onOpenEditor: (Long) -> Unit,
) {
    val colors = GrabadoraTheme.colors
    val context = LocalContext.current
    val recording by viewModel.recording.collectAsState()
    val peaks by viewModel.peaks.collectAsState()
    val isPlaying by viewModel.player.isPlaying.collectAsState()
    val positionMs by viewModel.player.positionMs.collectAsState()
    val speed by viewModel.player.speed.collectAsState()
    var showRename by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    val rec = recording?.recording ?: return
    val durationMs = rec.durationMs.coerceAtLeast(1)
    val progress = (positionMs.toFloat() / durationMs).coerceIn(0f, 1f)

    Column(Modifier.fillMaxSize().background(colors.bg).safeDrawingPadding().verticalScroll(rememberScrollState())) {
        // cabecera
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 22.dp).padding(top = 14.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BackButton(onBack)
            Spacer(Modifier.weight(1f))
            Text(
                "${rec.format} · " + RelativeDate.format(
                    rec.createdAt, System.currentTimeMillis(),
                    stringResource(R.string.date_today), stringResource(R.string.date_yesterday),
                    stringResource(R.string.date_now),
                ),
                style = TextStyle(fontFamily = DmMono, fontSize = 12.sp, color = colors.fg3),
            )
        }

        // nombre + renombrar
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 22.dp).padding(top = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                rec.name,
                style = TextStyle(fontFamily = SpaceGrotesk, fontWeight = FontWeight.SemiBold, fontSize = 22.sp, color = colors.fg1),
                modifier = Modifier.weight(1f, fill = false),
            )
            Icon(
                Icons.Outlined.Edit, stringResource(R.string.detail_rename),
                Modifier.size(16.dp).clickable { showRename = true },
                tint = colors.fg3,
            )
        }

        // etiquetas maestras
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp)
                .padding(top = 12.dp)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            for (tag in viewModel.masterTags) {
                val active = recording?.tags?.any { it.name == tag } == true
                TagChip(tag, active, { viewModel.toggleTag(tag) })
            }
        }

        // tarjeta reproductor
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp)
                .padding(top = 24.dp)
                .background(colors.sf1, RoundedCornerShape(18.dp))
                .border(1.dp, colors.bd2, RoundedCornerShape(18.dp))
                .padding(horizontal = 18.dp, vertical = 24.dp),
        ) {
            WaveformView(
                peaks = peaks,
                progress = progress,
                modifier = Modifier.fillMaxWidth().height(120.dp),
                onScrub = { viewModel.player.seekTo((it * durationMs).toLong()) },
            )
            Row(Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    TimeFormat.fine(positionMs),
                    style = TextStyle(fontFamily = DmMono, fontWeight = FontWeight.Medium, fontSize = 12.sp, color = Accent),
                )
                Text(
                    TimeFormat.mmss(rec.durationMs / 1000),
                    style = TextStyle(fontFamily = DmMono, fontWeight = FontWeight.Medium, fontSize = 12.sp, color = colors.fg2),
                )
            }
            Slider(
                value = progress,
                onValueChange = { viewModel.player.seekTo((it * durationMs).toLong()) },
                colors = SliderDefaults.colors(thumbColor = Accent, activeTrackColor = Accent, inactiveTrackColor = colors.bd4),
                modifier = Modifier.padding(top = 4.dp),
            )
            // afinar
            Row(
                Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    stringResource(R.string.detail_fine).uppercase(),
                    style = TextStyle(fontFamily = DmMono, fontSize = 10.sp, letterSpacing = 1.sp, color = colors.fg3),
                )
                for ((delta, label) in listOf(-1000L to "−1s", -100L to "−0,1s", 100L to "+0,1s", 1000L to "+1s")) {
                    FineButton(label) { viewModel.player.seekBy(delta) }
                }
            }
        }

        // seek + play
        Row(
            Modifier.fillMaxWidth().padding(top = 22.dp, bottom = 0.dp).padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(9.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            for (d in listOf(-10, -5, -1)) SeekButton(TimeFormat.seekLabel(d)) { viewModel.player.seekBy(d * 1000L) }
            Box(
                Modifier
                    .size(68.dp)
                    .background(Accent, CircleShape)
                    .clickable(remember { MutableInteractionSource() }, null) { viewModel.player.togglePlay() },
                contentAlignment = Alignment.Center,
            ) {
                if (isPlaying) {
                    Row {
                        Box(Modifier.size(width = 6.dp, height = 24.dp).background(Color(0xFF08080C), RoundedCornerShape(2.dp)))
                        Box(Modifier.width(6.dp))
                        Box(Modifier.size(width = 6.dp, height = 24.dp).background(Color(0xFF08080C), RoundedCornerShape(2.dp)))
                    }
                } else {
                    Icon(Icons.Default.PlayArrow, stringResource(R.string.cd_play), Modifier.size(32.dp), tint = Color(0xFF08080C))
                }
            }
            for (d in listOf(1, 5, 10)) SeekButton(TimeFormat.seekLabel(d)) { viewModel.player.seekBy(d * 1000L) }
        }

        // velocidad
        Row(
            Modifier.fillMaxWidth().padding(top = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SpeedButton("−") { viewModel.player.stepSpeed(-1) }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    TimeFormat.speed(speed),
                    style = TextStyle(fontFamily = DmMono, fontWeight = FontWeight.SemiBold, fontSize = 19.sp, color = Yellow),
                )
                Text(
                    stringResource(R.string.detail_speed).uppercase(),
                    style = TextStyle(fontFamily = DmMono, fontSize = 9.sp, letterSpacing = 1.2.sp, color = colors.fg3),
                )
            }
            SpeedButton("+") { viewModel.player.stepSpeed(1) }
        }

        Spacer(Modifier.height(24.dp))

        // acciones
        Column(Modifier.fillMaxWidth().padding(horizontal = 18.dp).padding(bottom = 22.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(Yellow, RoundedCornerShape(13.dp))
                    .clickable(remember { MutableInteractionSource() }, null) { onOpenEditor(rec.id) }
                    .padding(15.dp),
                horizontalArrangement = Arrangement.spacedBy(9.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Outlined.Edit, null, Modifier.size(19.dp), tint = Color(0xFF08080C))
                Text(
                    stringResource(R.string.detail_edit),
                    style = TextStyle(fontFamily = DmSans, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = Color(0xFF08080C)),
                )
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                ActionCard(
                    icon = { Icon(Icons.Outlined.Share, null, Modifier.size(20.dp), tint = colors.fg2) },
                    label = stringResource(R.string.detail_share),
                    modifier = Modifier.weight(1f),
                ) {
                    viewModel.shareFile()?.let { file ->
                        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                        context.startActivity(
                            Intent.createChooser(
                                Intent(Intent.ACTION_SEND).apply {
                                    type = "audio/*"
                                    putExtra(Intent.EXTRA_STREAM, uri)
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                },
                                rec.name,
                            ),
                        )
                    }
                }
                ActionCard(
                    icon = { Icon(Icons.Outlined.Edit, null, Modifier.size(20.dp), tint = colors.fg2) },
                    label = stringResource(R.string.detail_rename),
                    modifier = Modifier.weight(1f),
                ) { showRename = true }
                ActionCard(
                    icon = { Icon(Icons.Outlined.Delete, null, Modifier.size(20.dp), tint = Accent) },
                    label = stringResource(R.string.detail_delete),
                    modifier = Modifier.weight(1f),
                    labelColor = Accent,
                ) { showDeleteConfirm = true }
            }
        }
    }

    if (showRename) {
        var name by remember { mutableStateOf(rec.name) }
        AlertDialog(
            onDismissRequest = { showRename = false },
            containerColor = if (colors.isDark) Color(0xFF16161C) else Color.White,
            title = {
                Text(
                    stringResource(R.string.detail_rename_title),
                    style = TextStyle(fontFamily = SpaceGrotesk, fontWeight = FontWeight.SemiBold, fontSize = 17.sp, color = colors.fg1),
                )
            },
            text = {
                OutlinedTextField(value = name, onValueChange = { name = it }, singleLine = true)
            },
            confirmButton = {
                TextButton(onClick = { viewModel.rename(name); showRename = false }) {
                    Text(stringResource(R.string.dialog_ok), color = Accent)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRename = false }) {
                    Text(stringResource(R.string.dialog_cancel), color = colors.fg3)
                }
            },
        )
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            containerColor = if (colors.isDark) Color(0xFF16161C) else Color.White,
            title = {
                Text(
                    stringResource(R.string.detail_delete_confirm_title),
                    style = TextStyle(fontFamily = SpaceGrotesk, fontWeight = FontWeight.SemiBold, fontSize = 17.sp, color = colors.fg1),
                )
            },
            text = {
                Text(
                    stringResource(R.string.detail_delete_confirm_msg),
                    style = TextStyle(fontFamily = DmSans, fontSize = 13.sp, color = colors.fg2),
                )
            },
            confirmButton = {
                TextButton(onClick = { showDeleteConfirm = false; viewModel.delete(onBack) }) {
                    Text(stringResource(R.string.detail_delete), color = Accent)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text(stringResource(R.string.dialog_cancel), color = colors.fg3)
                }
            },
        )
    }
}

@Composable
private fun FineButton(label: String, onClick: () -> Unit) {
    val colors = GrabadoraTheme.colors
    Box(
        Modifier
            .background(colors.sf2, RoundedCornerShape(8.dp))
            .border(1.dp, colors.bd4, RoundedCornerShape(8.dp))
            .clickable(remember { MutableInteractionSource() }, null, onClick = onClick)
            .padding(horizontal = 9.dp, vertical = 6.dp),
    ) {
        Text(label, style = TextStyle(fontFamily = DmMono, fontWeight = FontWeight.SemiBold, fontSize = 11.sp, color = colors.fg2))
    }
}

@Composable
private fun SeekButton(label: String, onClick: () -> Unit) {
    val colors = GrabadoraTheme.colors
    Box(
        Modifier
            .background(colors.sf3, RoundedCornerShape(12.dp))
            .border(1.dp, colors.bd3, RoundedCornerShape(12.dp))
            .clickable(remember { MutableInteractionSource() }, null, onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 11.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, style = TextStyle(fontFamily = DmMono, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = colors.fg2))
    }
}

@Composable
private fun SpeedButton(label: String, onClick: () -> Unit) {
    val colors = GrabadoraTheme.colors
    Box(
        Modifier
            .size(42.dp)
            .background(colors.sf3, RoundedCornerShape(12.dp))
            .border(1.dp, colors.bd3, RoundedCornerShape(12.dp))
            .clickable(remember { MutableInteractionSource() }, null, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, style = TextStyle(fontFamily = DmSans, fontWeight = FontWeight.SemiBold, fontSize = 18.sp, color = colors.fg2))
    }
}

@Composable
private fun ActionCard(
    icon: @Composable () -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    labelColor: Color = GrabadoraTheme.colors.fg2,
    onClick: () -> Unit,
) {
    val colors = GrabadoraTheme.colors
    Column(
        modifier
            .background(colors.sf2, RoundedCornerShape(13.dp))
            .border(1.dp, colors.bd3, RoundedCornerShape(13.dp))
            .clickable(remember { MutableInteractionSource() }, null, onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        icon()
        Text(label, style = TextStyle(fontFamily = DmSans, fontWeight = FontWeight.Medium, fontSize = 11.sp, color = labelColor))
    }
}
