package com.gravo.grabadora.ui.library

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gravo.grabadora.R
import com.gravo.grabadora.ui.components.BackButton
import com.gravo.grabadora.ui.components.TagChip
import com.gravo.grabadora.ui.theme.Accent
import com.gravo.grabadora.ui.theme.DmMono
import com.gravo.grabadora.ui.theme.DmSans
import com.gravo.grabadora.ui.theme.GrabadoraTheme
import com.gravo.grabadora.ui.theme.SpaceGrotesk
import com.gravo.grabadora.ui.theme.Yellow
import com.gravo.grabadora.util.RelativeDate
import com.gravo.grabadora.util.TimeFormat

@Composable
fun LibraryScreen(
    viewModel: LibraryViewModel,
    onBack: () -> Unit,
    onOpenRecording: (Long) -> Unit,
) {
    val colors = GrabadoraTheme.colors
    val items by viewModel.items.collectAsState()
    val query by viewModel.query.collectAsState()
    val tagFilter by viewModel.tagFilter.collectAsState()
    val sortBy by viewModel.sortBy.collectAsState()
    val usedTags by viewModel.usedTags.collectAsState()

    Column(Modifier.fillMaxSize().background(colors.bg).safeDrawingPadding()) {
        // cabecera
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 22.dp).padding(top = 14.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            BackButton(onBack)
            Text(
                stringResource(R.string.library_title),
                style = TextStyle(fontFamily = SpaceGrotesk, fontWeight = FontWeight.SemiBold, fontSize = 20.sp, color = colors.fg1),
            )
            Spacer(Modifier.weight(1f))
            Text(
                if (items.size == 1) {
                    stringResource(R.string.library_count_one)
                } else {
                    stringResource(R.string.library_count, items.size)
                },
                style = TextStyle(fontFamily = DmMono, fontSize = 12.sp, color = colors.fg3),
            )
        }

        // búsqueda
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp)
                .padding(top = 4.dp, bottom = 10.dp)
                .background(colors.sf2, RoundedCornerShape(12.dp))
                .border(1.dp, colors.bd3, RoundedCornerShape(12.dp))
                .padding(horizontal = 12.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(9.dp),
        ) {
            Icon(Icons.Outlined.Search, null, Modifier.size(16.dp), tint = colors.fg3)
            BasicTextField(
                value = query,
                onValueChange = { viewModel.query.value = it },
                singleLine = true,
                textStyle = TextStyle(fontFamily = DmSans, fontSize = 13.sp, color = colors.fg1),
                cursorBrush = SolidColor(Accent),
                modifier = Modifier.weight(1f),
                decorationBox = { inner ->
                    if (query.isEmpty()) {
                        Text(
                            stringResource(R.string.library_search_hint),
                            style = TextStyle(fontFamily = DmSans, fontSize = 13.sp, color = colors.fg3),
                        )
                    }
                    inner()
                },
            )
        }

        // orden
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 22.dp).padding(bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                stringResource(R.string.library_sort),
                style = TextStyle(fontFamily = DmSans, fontSize = 12.sp, color = colors.fg3),
                modifier = Modifier.padding(end = 2.dp),
            )
            for ((sort, label) in listOf(
                SortBy.RECENT to stringResource(R.string.library_sort_recent),
                SortBy.NAME to stringResource(R.string.library_sort_name),
                SortBy.DURATION to stringResource(R.string.library_sort_duration),
            )) {
                TagChip(label, sortBy == sort, { viewModel.sortBy.value = sort })
            }
        }

        // filtros por etiqueta
        Row(
            Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 22.dp)
                .padding(bottom = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            TagChip(stringResource(R.string.library_filter_all), tagFilter == LibraryFilter.ALL_TAG, {
                viewModel.tagFilter.value = LibraryFilter.ALL_TAG
            })
            for (tag in usedTags) {
                TagChip(tag, tagFilter == tag, { viewModel.tagFilter.value = tag })
            }
        }

        if (items.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    stringResource(R.string.library_empty),
                    style = TextStyle(fontFamily = DmSans, fontSize = 13.sp, color = colors.fg3),
                )
            }
        } else {
            val context = LocalContext.current
            LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(start = 16.dp, end = 16.dp, bottom = 20.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(items, key = { it.id }) { item ->
                    RecordingRow(item, onClick = { onOpenRecording(item.id) })
                }
            }
        }
    }
}

@Composable
private fun RecordingRow(item: LibraryItem, onClick: () -> Unit) {
    val colors = GrabadoraTheme.colors
    Row(
        Modifier
            .fillMaxWidth()
            .background(colors.sf1, RoundedCornerShape(14.dp))
            .border(1.dp, colors.bd1, RoundedCornerShape(14.dp))
            .clickable(remember { MutableInteractionSource() }, null, onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(
            Modifier.size(44.dp).background(Accent.copy(alpha = 0.12f), RoundedCornerShape(11.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Default.PlayArrow, stringResource(R.string.cd_play), Modifier.size(22.dp), tint = Accent)
        }
        Column(Modifier.weight(1f)) {
            Text(
                item.name,
                style = TextStyle(fontFamily = DmSans, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = colors.fg1),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Row(
                Modifier.padding(top = 3.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    RelativeDate.format(
                        item.createdAt,
                        System.currentTimeMillis(),
                        stringResource(R.string.date_today),
                        stringResource(R.string.date_yesterday),
                        stringResource(R.string.date_now),
                    ),
                    style = TextStyle(fontFamily = DmMono, fontSize = 11.sp, color = colors.fg3),
                )
                Text("·", style = TextStyle(fontFamily = DmMono, fontSize = 11.sp, color = colors.fg4))
                Text(
                    TimeFormat.mmss(item.durationMs / 1000),
                    style = TextStyle(fontFamily = DmMono, fontSize = 11.sp, color = colors.fg3),
                )
            }
            if (item.tags.isNotEmpty()) {
                Row(
                    Modifier.padding(top = 7.dp),
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    for (tag in item.tags) {
                        Text(
                            tag,
                            style = TextStyle(fontFamily = DmMono, fontWeight = FontWeight.Medium, fontSize = 10.sp, color = colors.fg2),
                            modifier = Modifier
                                .background(colors.bd2, RoundedCornerShape(5.dp))
                                .padding(horizontal = 8.dp, vertical = 2.dp),
                        )
                    }
                }
            }
        }
        Text(
            item.format,
            style = TextStyle(fontFamily = DmMono, fontWeight = FontWeight.Medium, fontSize = 10.sp, color = Yellow),
            modifier = Modifier
                .background(Yellow.copy(alpha = 0.1f), RoundedCornerShape(5.dp))
                .padding(horizontal = 7.dp, vertical = 3.dp),
        )
    }
}
