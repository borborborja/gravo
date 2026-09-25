package com.gravo.grabadora.ui.transcription

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
import com.gravo.grabadora.transcription.TranscriptionErrorKind
import com.gravo.grabadora.transcription.TranscriptionProviderId
import com.gravo.grabadora.ui.components.BackButton
import com.gravo.grabadora.ui.components.SegChip
import com.gravo.grabadora.ui.theme.Accent
import com.gravo.grabadora.ui.theme.DmMono
import com.gravo.grabadora.ui.theme.DmSans
import com.gravo.grabadora.ui.theme.GrabadoraTheme
import com.gravo.grabadora.ui.theme.RecordRed
import com.gravo.grabadora.ui.theme.SpaceGrotesk
import com.gravo.grabadora.ui.theme.WaveGreen
import com.gravo.grabadora.ui.theme.Yellow
import java.io.File

@Composable
fun TranscriptionScreen(
    viewModel: TranscriptionViewModel,
    onBack: () -> Unit,
    onShareText: (String) -> Unit,
) {
    val colors = GrabadoraTheme.colors
    val context = LocalContext.current
    val state by viewModel.state.collectAsState()
    val text by viewModel.text.collectAsState()
    val query by viewModel.query.collectAsState()
    val matchCount by viewModel.matchCount.collectAsState()
    val selectedLanguage by viewModel.selectedLanguage.collectAsState()
    val transcript by viewModel.transcript.collectAsState()
    val recording by viewModel.recording.collectAsState()
    val provider by viewModel.provider.collectAsState()

    // Precarga el texto del transcript guardado la primera vez que llega.
    LaunchedEffect(transcript) {
        val t = transcript
        if (state == TranscriptionViewModel.TranscriptionUiState.Idle && !t.isNullOrEmpty()) {
            viewModel.text.value = t
        }
    }

    val running = state == TranscriptionViewModel.TranscriptionUiState.Running

    Column(Modifier.fillMaxSize().background(colors.bg).safeDrawingPadding().verticalScroll(rememberScrollState())) {
        // cabecera
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 22.dp).padding(top = 14.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            BackButton(onBack)
            Text(
                stringResource(R.string.transcription_title),
                style = TextStyle(fontFamily = SpaceGrotesk, fontWeight = FontWeight.SemiBold, fontSize = 17.sp, color = colors.fg1),
            )
        }

        // selector de idioma
        Row(
            Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 18.dp)
                .padding(top = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            for ((code, label) in languageChips()) {
                SegChip(label, selectedLanguage == code, { viewModel.setLanguage(code) }, mono = false)
            }
        }
        if (provider == TranscriptionProviderId.GEMINI) {
            Text(
                stringResource(R.string.transcription_gemini_auto),
                style = TextStyle(fontFamily = DmSans, fontSize = 11.5.sp, color = colors.fg3),
                modifier = Modifier.padding(horizontal = 20.dp).padding(top = 8.dp),
            )
        }

        // transcribir
        Box(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp)
                .padding(top = 18.dp)
                .background(Accent, RoundedCornerShape(13.dp))
                .clickable(remember { MutableInteractionSource() }, null, enabled = !running) { viewModel.start() }
                .padding(15.dp),
            contentAlignment = Alignment.Center,
        ) {
            if (running) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                    CircularProgressIndicator(Modifier.size(18.dp), color = Color(0xFF08080C), strokeWidth = 2.dp)
                    Text(
                        stringResource(R.string.transcription_transcribing),
                        style = TextStyle(fontFamily = DmSans, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = Color(0xFF08080C)),
                    )
                }
            } else {
                Text(
                    stringResource(R.string.transcription_transcribe),
                    style = TextStyle(fontFamily = DmSans, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = Color(0xFF08080C)),
                )
            }
        }

        // estado
        when (val s = state) {
            is TranscriptionViewModel.TranscriptionUiState.Error -> {
                Text(
                    stringResource(R.string.transcription_error) + ": " + s.message,
                    style = TextStyle(fontFamily = DmSans, fontSize = 12.sp, color = errorColor(s.kind)),
                    modifier = Modifier.padding(horizontal = 20.dp).padding(top = 12.dp),
                )
            }
            TranscriptionViewModel.TranscriptionUiState.Done -> {
                Text(
                    stringResource(R.string.transcription_done),
                    style = TextStyle(fontFamily = DmSans, fontWeight = FontWeight.Medium, fontSize = 12.sp, color = WaveGreen),
                    modifier = Modifier.padding(horizontal = 20.dp).padding(top = 12.dp),
                )
            }
            else -> Unit
        }

        // editor de transcript
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp)
                .padding(top = 18.dp)
                .background(colors.sf1, RoundedCornerShape(16.dp))
                .border(1.dp, colors.bd2, RoundedCornerShape(16.dp))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(R.string.transcription_transcript).uppercase(),
                    style = TextStyle(fontFamily = DmMono, fontSize = 10.sp, letterSpacing = 1.sp, color = colors.fg3),
                )
                Box(
                    Modifier
                        .background(Accent, RoundedCornerShape(9.dp))
                        .clickable(remember { MutableInteractionSource() }, null) { viewModel.saveText() }
                        .padding(horizontal = 14.dp, vertical = 7.dp),
                ) {
                    Text(
                        stringResource(R.string.transcription_save),
                        style = TextStyle(fontFamily = DmSans, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = Color(0xFF08080C)),
                    )
                }
            }
            OutlinedTextField(
                value = text,
                onValueChange = { viewModel.text.value = it },
                modifier = Modifier.fillMaxWidth().heightIn(min = 180.dp),
                minLines = 6,
                textStyle = TextStyle(fontFamily = DmSans, fontSize = 14.sp, color = colors.fg1),
                placeholder = {
                    Text(
                        stringResource(R.string.transcription_text_hint),
                        style = TextStyle(fontFamily = DmSans, fontSize = 13.sp, color = colors.fg3),
                    )
                },
                colors = textFieldColors(colors),
            )
        }

        // buscador
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp)
                .padding(top = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = viewModel::setQuery,
                modifier = Modifier.weight(1f),
                singleLine = true,
                leadingIcon = { Icon(Icons.Outlined.Search, null, Modifier.size(18.dp), tint = colors.fg3) },
                textStyle = TextStyle(fontFamily = DmSans, fontSize = 13.sp, color = colors.fg1),
                placeholder = {
                    Text(
                        stringResource(R.string.transcription_search_hint),
                        style = TextStyle(fontFamily = DmSans, fontSize = 13.sp, color = colors.fg3),
                    )
                },
                colors = textFieldColors(colors),
            )
            Text(
                stringResource(R.string.transcription_matches, matchCount),
                style = TextStyle(fontFamily = DmMono, fontWeight = FontWeight.Medium, fontSize = 12.sp, color = if (matchCount > 0) Yellow else colors.fg3),
            )
        }

        // acciones
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 18.dp).padding(top = 18.dp, bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(9.dp),
        ) {
            ActionButton(
                icon = { Icon(Icons.Outlined.ContentCopy, null, Modifier.size(18.dp), tint = colors.fg2) },
                label = stringResource(R.string.transcription_copy),
                modifier = Modifier.weight(1f),
                enabled = text.isNotEmpty(),
            ) {
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(ClipData.newPlainText("transcript", viewModel.text.value))
            }
            ActionButton(
                icon = { Icon(Icons.Outlined.Share, null, Modifier.size(18.dp), tint = colors.fg2) },
                label = stringResource(R.string.transcription_share),
                modifier = Modifier.weight(1f),
                enabled = text.isNotEmpty(),
            ) {
                onShareText(viewModel.text.value)
            }
        }

        // exportar a .txt
        val exportLabel = stringResource(R.string.transcription_export_txt)
        ActionButton(
            icon = { Icon(Icons.Outlined.Download, null, Modifier.size(18.dp), tint = colors.fg2) },
            label = exportLabel,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp).padding(bottom = 26.dp),
            enabled = text.isNotBlank(),
        ) {
            val rec = recording?.recording
            val base = rec?.name?.takeIf { it.isNotBlank() } ?: (rec?.let { "transcripcion_${it.id}" } ?: "transcripcion")
            val name = base.trim()
                .replace(Regex("[^A-Za-z0-9._-]+"), "_")
                .trim('_', '.', '-')
                .ifBlank { "transcripcion" }
            val dir = File(context.cacheDir, "transcripts")
            dir.mkdirs()
            val file = File(dir, "$name.txt")
            file.writeText(viewModel.text.value)
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            context.startActivity(
                Intent.createChooser(
                    Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_STREAM, uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    },
                    exportLabel,
                ),
            )
        }
    }
}

@Composable
private fun ActionButton(
    icon: @Composable () -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    val colors = GrabadoraTheme.colors
    Row(
        modifier
            .background(colors.sf2, RoundedCornerShape(13.dp))
            .border(1.dp, colors.bd3, RoundedCornerShape(13.dp))
            .clickable(remember { MutableInteractionSource() }, null, enabled = enabled, onClick = onClick)
            .padding(vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        icon()
        Text(
            label,
            style = TextStyle(fontFamily = DmSans, fontWeight = FontWeight.Medium, fontSize = 12.sp, color = if (enabled) colors.fg2 else colors.fg4),
        )
    }
}

@Composable
private fun textFieldColors(colors: com.gravo.grabadora.ui.theme.GrabadoraColors) = OutlinedTextFieldDefaults.colors(
    focusedTextColor = colors.fg1,
    unfocusedTextColor = colors.fg1,
    cursorColor = Accent,
    focusedBorderColor = Accent,
    unfocusedBorderColor = colors.bd4,
    focusedContainerColor = colors.sf1,
    unfocusedContainerColor = colors.sf1,
    focusedPlaceholderColor = colors.fg3,
    unfocusedPlaceholderColor = colors.fg3,
)

@Composable
private fun languageChips(): List<Pair<String, String>> = listOf(
    "" to stringResource(R.string.transcription_language_auto),
    "es" to "ES",
    "en" to "EN",
    "fr" to "FR",
    "de" to "DE",
    "it" to "IT",
    "pt" to "PT",
)

private fun errorColor(kind: TranscriptionErrorKind): Color = when (kind) {
    TranscriptionErrorKind.AUTH, TranscriptionErrorKind.SERVER, TranscriptionErrorKind.NETWORK -> RecordRed
    else -> Yellow
}
