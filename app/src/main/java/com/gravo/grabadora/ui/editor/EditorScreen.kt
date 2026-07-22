package com.gravo.grabadora.ui.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.gestures.detectDragGestures
import com.gravo.grabadora.R
import com.gravo.grabadora.ui.components.BackButton
import com.gravo.grabadora.ui.components.EditorWaveform
import com.gravo.grabadora.ui.components.GravoSwitch
import com.gravo.grabadora.ui.components.SegChip
import com.gravo.grabadora.ui.theme.Accent
import com.gravo.grabadora.ui.theme.DmMono
import com.gravo.grabadora.ui.theme.DmSans
import com.gravo.grabadora.ui.theme.GrabadoraTheme
import com.gravo.grabadora.ui.theme.SpaceGrotesk
import com.gravo.grabadora.ui.theme.Yellow
import com.gravo.grabadora.util.TimeFormat
import kotlin.math.roundToInt

@Composable
fun EditorScreen(
    viewModel: EditorViewModel,
    onClose: () -> Unit,
) {
    val colors = GrabadoraTheme.colors
    val recording by viewModel.recording.collectAsState()
    val peaks by viewModel.peaks.collectAsState()
    val tool by viewModel.tool.collectAsState()
    val trimStart by viewModel.trimStart.collectAsState()
    val trimEnd by viewModel.trimEnd.collectAsState()
    val pointer by viewModel.pointer.collectAsState()
    val saving by viewModel.saving.collectAsState()
    val error by viewModel.error.collectAsState()

    val rec = recording ?: return
    val durationMs = rec.durationMs.coerceAtLeast(1)

    Column(Modifier.fillMaxSize().background(colors.bg)) {
        // cabecera
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 22.dp).padding(top = 14.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            BackButton(onClose, close = true)
            Text(
                stringResource(R.string.editor_title),
                style = TextStyle(fontFamily = SpaceGrotesk, fontWeight = FontWeight.SemiBold, fontSize = 17.sp, color = colors.fg1),
            )
            Spacer(Modifier.weight(1f))
            Box(
                Modifier
                    .background(Accent, RoundedCornerShape(10.dp))
                    .clickable(remember { MutableInteractionSource() }, null, enabled = !saving) {
                        viewModel.save(onClose)
                    }
                    .padding(horizontal = 18.dp, vertical = 9.dp),
            ) {
                if (saving) {
                    CircularProgressIndicator(Modifier.size(16.dp), color = Color(0xFF08080C), strokeWidth = 2.dp)
                } else {
                    Text(
                        stringResource(R.string.editor_save),
                        style = TextStyle(fontFamily = DmSans, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = Color(0xFF08080C)),
                    )
                }
            }
        }

        error?.let {
            Text(
                it,
                style = TextStyle(fontFamily = DmSans, fontSize = 12.sp, color = com.gravo.grabadora.ui.theme.RecordRed),
                modifier = Modifier.padding(horizontal = 22.dp, vertical = 4.dp),
            )
        }

        // tarjeta de onda con tiradores
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp)
                .padding(top = 14.dp)
                .background(colors.sf1, RoundedCornerShape(16.dp))
                .border(1.dp, colors.bd2, RoundedCornerShape(16.dp))
                .padding(horizontal = 14.dp)
                .padding(top = 20.dp, bottom = 16.dp),
        ) {
            TrimWaveform(
                peaks = peaks,
                trimStart = trimStart,
                trimEnd = trimEnd,
                pointer = pointer,
                onTrimStart = { viewModel.trimStart.value = it.coerceIn(0f, viewModel.trimEnd.value - 0.04f) },
                onTrimEnd = { viewModel.trimEnd.value = it.coerceIn(viewModel.trimStart.value + 0.04f, 1f) },
                onPointer = { viewModel.pointer.value = it.coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth().height(150.dp),
            )
            Row(Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    TimeFormat.mmss((trimStart * durationMs).toLong() / 1000),
                    style = TextStyle(fontFamily = DmMono, fontSize = 11.sp, color = colors.fg3),
                )
                Text(
                    stringResource(R.string.editor_selection, TimeFormat.mmss(((trimEnd - trimStart) * durationMs).toLong() / 1000)),
                    style = TextStyle(fontFamily = DmMono, fontSize = 11.sp, color = Yellow),
                )
                Text(
                    TimeFormat.mmss((trimEnd * durationMs).toLong() / 1000),
                    style = TextStyle(fontFamily = DmMono, fontSize = 11.sp, color = colors.fg3),
                )
            }
            // fila del puntero
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp)
                    .background(Yellow.copy(alpha = 0.06f), RoundedCornerShape(11.dp))
                    .border(1.dp, Yellow.copy(alpha = 0.18f), RoundedCornerShape(11.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    stringResource(R.string.editor_pointer).uppercase(),
                    style = TextStyle(fontFamily = DmMono, fontSize = 10.sp, letterSpacing = 1.sp, color = colors.fg3),
                )
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    for ((delta, label) in listOf(-1000L to "−1s", -100L to "−0,1s", 100L to "+0,1s", 1000L to "+1s")) {
                        Box(
                            Modifier
                                .background(colors.sf2, RoundedCornerShape(8.dp))
                                .border(1.dp, colors.bd4, RoundedCornerShape(8.dp))
                                .clickable(remember { MutableInteractionSource() }, null) { viewModel.nudgePointerMs(delta) }
                                .padding(horizontal = 9.dp, vertical = 6.dp),
                        ) {
                            Text(label, style = TextStyle(fontFamily = DmMono, fontWeight = FontWeight.SemiBold, fontSize = 11.sp, color = colors.fg2))
                        }
                    }
                    Text(
                        TimeFormat.fine((pointer * durationMs).toLong()),
                        style = TextStyle(fontFamily = DmMono, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = Yellow),
                        modifier = Modifier.width(60.dp),
                    )
                }
            }
        }

        // herramientas
        Row(
            Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 18.dp)
                .padding(top = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            for ((t, label) in listOf(
                EditorTool.TRIM to stringResource(R.string.editor_tool_trim),
                EditorTool.CUT to stringResource(R.string.editor_tool_cut),
                EditorTool.MARKERS to stringResource(R.string.editor_tool_markers),
                EditorTool.VOLUME to stringResource(R.string.editor_tool_volume),
                EditorTool.FADE to stringResource(R.string.editor_tool_fade),
            )) {
                SegChip(label, tool == t, { viewModel.tool.value = t }, mono = false)
            }
        }

        // panel de la herramienta
        Column(
            Modifier
                .fillMaxSize()
                .padding(horizontal = 18.dp)
                .padding(top = 16.dp, bottom = 22.dp)
                .background(colors.sf1, RoundedCornerShape(14.dp))
                .border(1.dp, colors.bd4, RoundedCornerShape(14.dp))
                .padding(18.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            when (tool) {
                EditorTool.TRIM -> ToolPanel(stringResource(R.string.editor_trim_title)) {
                    Hint(stringResource(R.string.editor_trim_hint))
                }
                EditorTool.CUT -> ToolPanel(stringResource(R.string.editor_cut_title)) {
                    Hint(stringResource(R.string.editor_cut_hint))
                    val cuts by viewModel.cuts.collectAsState()
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .background(Accent.copy(alpha = 0.1f), RoundedCornerShape(9.dp))
                            .border(1.dp, Accent.copy(alpha = 0.25f), RoundedCornerShape(9.dp))
                            .clickable(remember { MutableInteractionSource() }, null) { viewModel.addCutFromSelection() }
                            .padding(11.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            stringResource(R.string.editor_cut_apply),
                            style = TextStyle(fontFamily = DmSans, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = Accent),
                        )
                    }
                    for ((index, cut) in cuts.withIndex()) {
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .background(colors.sf2, RoundedCornerShape(9.dp))
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                TimeFormat.mmss((cut.start * durationMs).toLong() / 1000) + " — " +
                                    TimeFormat.mmss((cut.endInclusive * durationMs).toLong() / 1000),
                                style = TextStyle(fontFamily = DmMono, fontSize = 11.sp, color = Yellow),
                            )
                            Text(
                                "×",
                                style = TextStyle(fontFamily = DmSans, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = colors.fg3),
                                modifier = Modifier.clickable { viewModel.removeCut(index) },
                            )
                        }
                    }
                }
                EditorTool.MARKERS -> ToolPanel(stringResource(R.string.editor_markers_title)) {
                    val chapters by viewModel.chapters.collectAsState()
                    for (chapter in chapters) {
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .background(colors.sf2, RoundedCornerShape(9.dp))
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                TimeFormat.mmss(chapter.timeMs / 1000),
                                style = TextStyle(fontFamily = DmMono, fontWeight = FontWeight.Medium, fontSize = 11.sp, color = Yellow),
                            )
                            Text(
                                chapter.name,
                                style = TextStyle(fontFamily = DmSans, fontWeight = FontWeight.Medium, fontSize = 13.sp, color = colors.fg1),
                                modifier = Modifier.weight(1f),
                            )
                            Text(
                                "×",
                                style = TextStyle(fontFamily = DmSans, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = colors.fg3),
                                modifier = Modifier.clickable { viewModel.deleteChapter(chapter.id) },
                            )
                        }
                    }
                    val template = stringResource(R.string.editor_marker_default)
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .background(Accent.copy(alpha = 0.1f), RoundedCornerShape(9.dp))
                            .border(1.dp, Accent.copy(alpha = 0.35f), RoundedCornerShape(9.dp))
                            .clickable(remember { MutableInteractionSource() }, null) { viewModel.addChapterAtPointer(template) }
                            .padding(10.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            stringResource(R.string.editor_markers_add),
                            style = TextStyle(fontFamily = DmSans, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = Accent),
                        )
                    }
                }
                EditorTool.VOLUME -> ToolPanel(stringResource(R.string.editor_volume_title)) {
                    val gainDb by viewModel.gainDb.collectAsState()
                    val normalize by viewModel.normalize.collectAsState()
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(
                            stringResource(R.string.editor_volume_gain),
                            style = TextStyle(fontFamily = DmSans, fontWeight = FontWeight.Medium, fontSize = 12.sp, color = colors.fg2),
                        )
                        Text(
                            (if (gainDb > 0) "+" else "") + "%.1f dB".format(gainDb),
                            style = TextStyle(fontFamily = DmMono, fontSize = 12.sp, color = Accent),
                        )
                    }
                    Slider(
                        value = gainDb,
                        onValueChange = { viewModel.gainDb.value = (it * 2).roundToInt() / 2f },
                        valueRange = -12f..12f,
                        colors = SliderDefaults.colors(thumbColor = Accent, activeTrackColor = Accent, inactiveTrackColor = colors.bd4),
                    )
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .background(Yellow.copy(alpha = if (normalize) 0.25f else 0.12f), RoundedCornerShape(9.dp))
                            .border(1.dp, Yellow.copy(alpha = if (normalize) 0.7f else 0.3f), RoundedCornerShape(9.dp))
                            .clickable(remember { MutableInteractionSource() }, null) { viewModel.normalize.value = !normalize }
                            .padding(11.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            stringResource(R.string.editor_volume_normalize) + if (normalize) " ✓" else "",
                            style = TextStyle(fontFamily = DmSans, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = Yellow),
                        )
                    }
                }
                EditorTool.FADE -> ToolPanel(stringResource(R.string.editor_fade_title)) {
                    Hint(stringResource(R.string.editor_fade_hint))
                    val fadeIn by viewModel.fadeIn.collectAsState()
                    val fadeOut by viewModel.fadeOut.collectAsState()
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            stringResource(R.string.editor_fade_in),
                            style = TextStyle(fontFamily = DmSans, fontWeight = FontWeight.Medium, fontSize = 13.sp, color = colors.fg1),
                        )
                        GravoSwitch(fadeIn, { viewModel.fadeIn.value = !fadeIn })
                    }
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            stringResource(R.string.editor_fade_out),
                            style = TextStyle(fontFamily = DmSans, fontWeight = FontWeight.Medium, fontSize = 13.sp, color = colors.fg1),
                        )
                        GravoSwitch(fadeOut, { viewModel.fadeOut.value = !fadeOut })
                    }
                }
            }
        }
    }
}

@Composable
private fun ToolPanel(title: String, content: @Composable () -> Unit) {
    val colors = GrabadoraTheme.colors
    Text(
        title,
        style = TextStyle(fontFamily = DmSans, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = colors.fg1),
    )
    content()
}

@Composable
private fun Hint(text: String) {
    val colors = GrabadoraTheme.colors
    Text(
        text,
        style = TextStyle(fontFamily = DmSans, fontSize = 12.5.sp, lineHeight = 20.sp, color = colors.fg3),
    )
}

/** Onda del editor con tiradores de recorte arrastrables y puntero amarillo. */
@Composable
private fun TrimWaveform(
    peaks: FloatArray,
    trimStart: Float,
    trimEnd: Float,
    pointer: Float,
    onTrimStart: (Float) -> Unit,
    onTrimEnd: (Float) -> Unit,
    onPointer: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = GrabadoraTheme.colors
    BoxWithConstraints(modifier) {
        val widthPx = with(LocalDensity.current) { maxWidth.toPx() }
        EditorWaveform(
            peaks = peaks,
            dimColor = colors.dim,
            dimStart = trimStart,
            dimEnd = trimEnd,
            modifier = Modifier.fillMaxSize(),
        )
        // tirador de inicio
        DragHandle(
            fraction = trimStart,
            widthPx = widthPx,
            color = Accent,
            onDrag = onTrimStart,
        )
        // tirador de fin
        DragHandle(
            fraction = trimEnd,
            widthPx = widthPx,
            color = Accent,
            onDrag = onTrimEnd,
        )
        // puntero amarillo
        val currentPointer = androidx.compose.runtime.rememberUpdatedState(pointer)
        Box(
            Modifier
                .offset { IntOffset((pointer * widthPx - 9.dp.toPx()).roundToInt(), 0) }
                .width(18.dp)
                .fillMaxHeight()
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        onPointer(currentPointer.value + dragAmount.x / widthPx)
                    }
                },
            contentAlignment = Alignment.TopCenter,
        ) {
            Box(Modifier.width(2.dp).fillMaxHeight().background(Yellow))
            Box(
                Modifier
                    .size(13.dp)
                    .background(Yellow, CircleShape)
                    .border(3.dp, Yellow.copy(alpha = 0.2f), CircleShape),
            )
        }
    }
}

@Composable
private fun DragHandle(
    fraction: Float,
    widthPx: Float,
    color: Color,
    onDrag: (Float) -> Unit,
) {
    val currentFraction = androidx.compose.runtime.rememberUpdatedState(fraction)
    Box(
        Modifier
            .offset { IntOffset((fraction * widthPx - 7.dp.toPx()).roundToInt(), 0) }
            .width(14.dp)
            .fillMaxHeight()
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    onDrag(currentFraction.value + dragAmount.x / widthPx)
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Box(Modifier.width(3.dp).fillMaxHeight().background(color, RoundedCornerShape(2.dp)))
        Box(
            Modifier
                .align(Alignment.TopCenter)
                .size(width = 14.dp, height = 22.dp)
                .background(color, RoundedCornerShape(4.dp)),
        )
    }
}
